/*
This is supposed to create the database, we will see...
*/

/* delete complete db */
DROP SCHEMA PUBLIC CASCADE;

/* -------------------- Setup Tables -------------------- */

/* Holds all type information */
CREATE TABLE type (
	/* use the standard syntax and explicity declare a primary key identity column */
	typeId INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	parentId INTEGER, 
	typeName VARCHAR(1024) UNIQUE NOT NULL
);
COMMIT WORK;

/* All methods which are somewhere called on some type */
CREATE TABLE method (
	methodId INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	typeId INTEGER FOREIGN KEY REFERENCES type(typeId),
	methodName VARCHAR(1024) NOT NULL
);
COMMIT WORK;

/* Information for type usages which is not calculated and excluding the call list */
CREATE TABLE typeusage (
	typeusageId INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	typeId INTEGER FOREIGN KEY REFERENCES type(typeId),
	class VARCHAR(1024) NOT NULL,
	lineNr INTEGER,
	context VARCHAR(1024) NOT NULL
);
COMMIT WORK;

/* links method and type usages to a list of method calls */
CREATE TABLE callList (
	typeusageId INTEGER FOREIGN KEY REFERENCES typeusage(typeusageId),
	methodId INTEGER FOREIGN KEY REFERENCES method(methodId),
	position INTEGER
);
COMMIT WORK;

/* Show types with all methods which are called on them */
CREATE VIEW typeWithMethods AS
SELECT type.typeName, GROUP_CONCAT(method.methodName SEPARATOR ', ') AS methods
	FROM type JOIN method
	ON type.typeId = method.typeId
	GROUP BY type.typeName
;
COMMIT WORK;

/* A list of calls for each type usage */
CREATE VIEW typeusageCalls AS
SELECT tu.typeusageId, GROUP_CONCAT(method.methodName SEPARATOR ', ') AS methodCalls
	FROM typeusage tu
	JOIN callList cl ON tu.typeusageId = cl.typeusageId 
	JOIN method ON cl.methodId = method.methodId
	GROUP BY tu.typeusageId /* ORDER BY cl.position */
;
COMMIT WORK;

/* Show complete typeusages as they would be printed -  location, lineNr, methodContext, type, methodCalls, (_extends) */
CREATE VIEW showTypeusageComplete AS
SELECT tu.typeusageId, tu.class, tu.lineNr, tu.context, type.typeName, tuc.methodCalls
	FROM typeusage tu, type, typeusageCalls tuc
	WHERE tu.typeId = type.typeId AND tu.typeusageId = tuc.typeusageId
;
COMMIT WORK;

/* Find all typeusages which are exactly equal to another tu */
CREATE VIEW equal AS
SELECT ta.typeusageId, tb.typeusageId as eqId
	FROM typeusage ta JOIN typeusage tb ON ta.typeId = tb.typeId AND ta.context = tb.context
	WHERE NOT EXISTS (
		-- Neither a has more methodcalls than b
		(SELECT cla.methodId FROM callList cla WHERE ta.typeusageId = cla.typeusageId
		EXCEPT
		Select clb.methodId FROM callList clb WHERE tb.typeusageId = clb.typeusageId)
		UNION
		-- Nor has b more methodcalls than a
		(Select clb.methodId FROM callList clb WHERE tb.typeusageId = clb.typeusageId
		EXCEPT
		SELECT cla.methodId FROM callList cla WHERE ta.typeusageId = cla.typeusageId))
;
COMMIT WORK;

-- debug view to check if equals is working as intended
CREATE VIEW showEquals AS
SELECT ta.class, tb.class AS B_class, ta.typeId, tb.typeId AS B_typeId, ta.context, tb.context AS B_context, tca.methodCalls, tcb.methodCalls AS B_methodCalls
	FROM typeusage ta JOIN typeusageCalls tca ON ta.typeusageId = tca.typeusageId
	JOIN equal e ON ta.typeusageId = e.typeusageId
	JOIN typeusage tb ON tb.typeusageId = e.eqId
	JOIN typeusageCalls tcb ON tb.typeusageId = tcb.typeusageId
;
COMMIT WORK;

-- Shows the size of the calllist of B without the calls from A
CREATE VIEW callListDifferences AS
SELECT ta.typeusageId as aID, tb.typeusageId AS bID, 
	(SELECT  COUNT(*) FROM (SELECT clc.methodId FROM callList clc WHERE tb.typeusageId = clc.typeusageId
		EXCEPT
		SELECT cld.methodId FROM callList cld WHERE ta.typeusageId = cld.typeusageId)) AS difference
	FROM typeusage ta JOIN typeusage tb ON ta.typeId = tb.typeId AND ta.context = tb.context
;
COMMIT WORK;

/* */
CREATE VIEW almostEqual AS 
SELECT a.aID, a.bID
	FROM callListDifferences a JOIN callListDifferences b ON a.aID = b.bID AND a.bID = b.aID
	WHERE
		--- call list of right side without callList of left side should have size of 1
	 	a.difference < 0 OR a.difference = 1
	 	-- call list of left side without call list of right side should be empty
	 	AND b.difference = 0 
;
COMMIT WORK;

-- debug view to check if almostEqual does the right thing
CREATE VIEW showAlmostEquals AS
SELECT ta.class, tb.class AS B_class, ta.typeId, tb.typeId AS B_typeId, ta.context, tb.context AS B_context, tca.methodCalls, tcb.methodCalls AS B_methodCalls
	FROM typeusage ta JOIN typeusageCalls tca ON ta.typeusageId = tca.typeusageId
	JOIN almostEqual ae ON ta.typeusageId = ae.aID
	JOIN typeusage tb ON tb.typeusageId = ae.bID
	JOIN typeusageCalls tcb ON tb.typeusageId = tcb.typeusageId
;
COMMIT WORK;

/* Mapping from supertypes to all their children */
/*
CREATE VIEW children AS
WITH RECURSIVE rec(parentId,childId) AS (
    SELECT NULL, type.typeId FROM type
    WHERE type.parentId IS NULL -- this condition defines the ultimate ancestors in your chain, change it as appropriate
    UNION ALL
	    SELECT rec.typeId, t.typeId FROM type t, rec
		ON t.parentID = rec.typeId
)
SELECT  * FROM rec
;
*/

/*

todos:
understand views / those things I remember to be persistent
look into indices!

--- 
unfinished business:

VIEWS: (are those the ones which are kept persistently and only updated?)

CHILDREN (Parent type id, child type id) -> contains reverse, all children of one parent
EQUAL (tu id, tu id)
ALMOST_EQUAL (tu id, tu id)

Strangeness (tu id, strangeness score)

*/