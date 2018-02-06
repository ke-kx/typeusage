
DROP SCHEMA intermediate CASCADE;
DROP SCHEMA display CASCADE;

CREATE SCHEMA intermediate;
CREATE SCHEMA display;

/* Show types with all methods which are called on them */
CREATE VIEW display.typeWithMethods (typeName, methods) AS
  SELECT type.typeName, GROUP_CONCAT(method.methodName SEPARATOR ', ')
  FROM type JOIN method
      ON type.id = method.typeId
  GROUP BY type.typeName
;

/* A list of calls for each type usage */
CREATE VIEW display.typeusageCalls (id, methodCalls) AS
  SELECT tu.id, GROUP_CONCAT(method.methodName SEPARATOR ', ')
  FROM typeusage tu
    JOIN callList cl ON tu.id = cl.typeusageId
    JOIN method ON cl.methodId = method.id
  GROUP BY tu.id /* ORDER BY cl.position */
;

/* Show complete typeusages as they would be printed -  location, lineNr, methodContext, type, methodCalls, (_extends) */
CREATE VIEW display.typeusageComplete (id, class, lineNr, context, typeName, methodCalls) AS
  SELECT tu.id, tu.class, tu.lineNr, tu.context, type.typeName, tuc.methodCalls
  FROM typeusage tu, type, display.typeusageCalls tuc
  WHERE tu.typeId = type.id AND tu.id = tuc.id
;

/* Find all typeusages which are exactly equal to another tu */
CREATE TABLE intermediate.equal (id, eqId) AS
  (SELECT ta.id, tb.id
  FROM typeusage ta JOIN typeusage tb ON ta.typeId = tb.typeId AND ta.context = tb.context
  WHERE NOT EXISTS (
  -- Neither a has more methodcalls than b
      (SELECT cla.methodId FROM callList cla WHERE ta.id = cla.typeusageId
       EXCEPT
       Select clb.methodId FROM callList clb WHERE tb.id = clb.typeusageId)
      UNION
      -- Nor has b more methodcalls than a
      (Select clb.methodId FROM callList clb WHERE tb.id = clb.typeusageId
       EXCEPT
       SELECT cla.methodId FROM callList cla WHERE ta.id = cla.typeusageId)))
WITH DATA
;
CREATE INDEX in_equal ON intermediate.equal (id, eqId);

-- debug view to check if equals is working as intended
CREATE VIEW display.equals (class, B_class, typeId, B_typeId, context, B_context, methodCalls, B_methodCalls) AS
  SELECT ta.class, tb.class, ta.typeId, tb.typeId, ta.context, tb.context, tca.methodCalls, tcb.methodCalls
  FROM typeusage ta JOIN display.typeusageCalls tca ON ta.id = tca.id
    JOIN intermediate.equal e ON ta.id = e.id
    JOIN typeusage tb ON tb.id = e.eqId
    JOIN display.typeusageCalls tcb ON tb.id = tcb.id
;

-- Shows the size of the calllist of B without the calls from A
CREATE TABLE intermediate.callListDifferences (leftId, rightId, difference) AS
  (SELECT ta.id, tb.id,
         (SELECT  COUNT(*) FROM (SELECT clc.methodId FROM callList clc WHERE tb.id = clc.typeusageId
                                 EXCEPT SELECT cld.methodId FROM callList cld WHERE ta.id = cld.typeusageId) AS calls)
  FROM typeusage ta JOIN typeusage tb ON ta.typeId = tb.typeId AND ta.context = tb.context)
WITH DATA
;
CREATE INDEX in_callListDifferences ON intermediate.callListDifferences (leftId, rightId);
CREATE INDEX in_callListDifferencesLeft ON intermediate.callListDifferences (leftId);
CREATE INDEX in_callListDifferencesRight ON intermediate.callListDifferences (rightId);

-- all ids on the right (bId) are almost equal to the ones on the left (have one more method call)
CREATE TABLE intermediate.almostEqual (id, aeqId) AS
  (SELECT a.leftId, a.rightId
  FROM intermediate.callListDifferences a JOIN intermediate.callListDifferences b ON a.leftId = b.rightId AND a.rightId = b.leftId
  WHERE
    --- call list of right side without callList of left side should have size of 1
    -- difference < 0 solves a weird bug where otherwise nothingg is displayed
    a.difference < 0 OR a.difference = 1
                        -- call list of left side without call list of right side should be empty
                        AND b.difference = 0)
WITH DATA
;
CREATE INDEX in_almostEqualId ON intermediate.almostEqual (id);
CREATE INDEX in_almostEqualAeqId ON intermediate.almostEqual (aeqId);

-- debug view to check if almostEqual does the right thing
CREATE VIEW display.almostEquals (class, B_class, typeId, B_typeId, context, B_context, methodCalls, B_methodCalls) AS
  SELECT ta.class, tb.class, ta.typeId, tb.typeId, ta.context, tb.context, tca.methodCalls, tcb.methodCalls
  FROM typeusage ta JOIN display.typeusageCalls tca ON ta.id = tca.id
    JOIN intermediate.almostEqual ae ON ta.id = ae.id
    JOIN typeusage tb ON tb.id = ae.aeqId
    JOIN display.typeusageCalls tcb ON tb.id = tcb.id
;

-- strangeness score is calculated as S(x) = 1 - (|E(x)| / (|E(x)| + |A(x)|))
CREATE TABLE intermediate.strangeness (id, score, equalCount, almostEqualCount) AS
  (WITH equalCount as (SELECT id, CAST(COUNT (eqID) AS FLOAT) AS count FROM intermediate.equal GROUP BY id),
      almostEqualCount as (SELECT id, CAST(COUNT (aeqId) AS FLOAT) AS count FROM intermediate.almostEqual GROUP BY id)
  SELECT tu.id, ISNULL(1.0 - (ec.count / (ec.count + aec.count)), 0.0), ec.count, ISNULL(aec.count, 0.0)
    FROM typeusage tu JOIN equalCount ec ON tu.id = ec.id
    LEFT JOIN almostEqualCount aec ON tu.id = aec.id)
WITH DATA
;
ALTER TABLE intermediate.strangeness ADD PRIMARY KEY (ID);

-- show a list of potentially missing method calls + their frequency?!
CREATE TABLE intermediate.missingMethodCount (id, methodId, count) AS
  (SELECT ae.id,
    (SELECT clb.methodId FROM callList clb WHERE ae.aeqId = clb.typeusageId
     EXCEPT SELECT cla.methodId FROM callList cla WHERE ae.id = cla.typeusageId) AS methodId,
    COUNT(1)
  FROM intermediate.almostEqual ae
  GROUP BY ae.id, methodId)
WITH DATA
;
CREATE INDEX in_mmcId ON intermediate.missingMethodCount (id);
CREATE INDEX in_mmcMethodId ON intermediate.missingMethodCount (methodId);

CREATE TABLE intermediate.missingMethodPercentage (id, methodId, percentage) AS
  (SELECT mmc.id, mmc.methodId, CAST(mmc.count AS FLOAT) / CAST(s.almostEqualCount AS FLOAT)
  FROM intermediate.missingMethodCount mmc JOIN intermediate.strangeness s ON mmc.id = s.id
  )
WITH DATA
;
CREATE INDEX in_mmp ON intermediate.missingMethodPercentage (id, methodId);

-- should show strangeness score, complete typeusage and the missing calls + their frequency
CREATE TABLE display.missingMethodCalls (score, equalCount, almostEqualCount, class, lineNr, context, typeName, methodCalls, missingCall, percentage) AS
  (SELECT s.score, s.equalCount, s.almostEqualCount, tuc.class, tuc.lineNr, tuc.context, tuc.typeName, tuc.methodCalls, m.methodName, mmp.percentage
  FROM intermediate.strangeness s JOIN display.typeusageComplete tuc ON s.id = tuc.id
    JOIN intermediate.missingMethodPercentage mmp ON tuc.id = mmp.id
    JOIN method m ON mmp.methodId = m.id)
WITH DATA
;
