package typeusage.miner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DatabaseTypeUsageCollector extends TypeUsageCollector {

    private static final Logger logger = LogManager.getLogger();

    private final Connection databaseConnection;

    /** Get count of types where name is in parameter array (1 = Array of typename)*/
    private final PreparedStatement checkTypesStatement;

    /** Get typeId of one type specified by its name (1 = Name) */
    private final PreparedStatement getTypeStatement;

    /** Add a type if it doesn't exist yet (1 = Name, 2 = ParentName) */
    private final PreparedStatement addTypeStatement;

    /** Insert method if there is none of the same name for the current ype (1 = methodName, 2 = typeId, 3 = typeId) */
    private final PreparedStatement addMethodStatement;

    /** Insert type usage (1 = typeName, 2 = class, 3 = lineNr, 4 = context) */
    private final PreparedStatement addTypeUsageStatement;

    /** Insert call into callList (1 = typeusageId, 2 = typeId, 3 = methodName, 4 = position) */
    private final PreparedStatement addCallStatement;

    private final int projectId;

    public DatabaseTypeUsageCollector(String databaseLocation, String projectName) {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:" + databaseLocation, "SA", "");

            // save project name to db + obtain project id (needs to be saved with every tu)
            PreparedStatement addProjectStmt = databaseConnection.prepareStatement("INSERT INTO project(id, name, analysisTime) " +
                    "VALUES (DEFAULT, ?, 0)", Statement.RETURN_GENERATED_KEYS);
            addProjectStmt.setString(1, projectName);
            addProjectStmt.execute();
            ResultSet res = addProjectStmt.getGeneratedKeys();
            if (res.next()) {
                projectId = res.getInt(1);
            } else {
                throw new RuntimeException("Couldn't insert project");
            }

            checkTypesStatement = databaseConnection.prepareStatement("SELECT COUNT(*) FROM type WHERE typeName IN( UNNEST(?) )");
            getTypeStatement = databaseConnection.prepareStatement("SELECT id FROM type WHERE typeName = ?");

            addTypeStatement = databaseConnection.prepareStatement(
                    "MERGE INTO type USING (VALUES( ? )) AS vals(typeName) " +
                            "ON type.typeName = vals.typeName " +
                            "WHEN NOT MATCHED THEN INSERT VALUES (DEFAULT, (SELECT id FROM type WHERE typeName = ?), vals.typeName)"
            );
            addMethodStatement = databaseConnection.prepareStatement(
                    "MERGE INTO method USING (VALUES( ? )) AS vals(methodName) " +
                            "ON method.typeId = ? AND method.methodName = vals.methodName " +
                            "WHEN NOT MATCHED THEN INSERT VALUES (DEFAULT, ?, vals.methodName)");
            addTypeUsageStatement = databaseConnection.prepareStatement(
                    "INSERT INTO typeusage(id, typeId, class, lineNr, context, projectId) " +
                            "VALUES( DEFAULT, (SELECT id FROM type WHERE typeName = ?), ?, ?, ?," + projectId +")", Statement.RETURN_GENERATED_KEYS);
            addCallStatement = databaseConnection.prepareStatement(
                    "INSERT INTO callList(typeusageId, methodId, position) " +
                            "VALUES( ?, (SELECT id FROM method WHERE typeId = ? AND methodName = ? ), ? )");
        } catch (SQLException e) {
            e.printStackTrace();
            // rethrow to enable setting final variables in try-catch block
            throw new RuntimeException(e);
        }
    }

    @Override
    public DatabaseTypeUsageCollector run() {
        long startTime = System.currentTimeMillis();

        super.run();

        // determine elapsed time and write to db
        long duration = System.currentTimeMillis() - startTime;
        try {
            Statement stmt = databaseConnection.createStatement();
            stmt.executeUpdate("UPDATE project SET analysisTime=" + duration + " WHERE id=" + projectId);
            databaseConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void receive(TypeUsage t) {
        logger.info(t);

        try {
            addTypeHierarchy(t);
            int typeId = addMethodCalls(t);
            int typeusageId = addTypeUsage(t);
            addCallList(typeusageId, typeId, t);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /** Check if all types along the hierarchy of this typeusage are already in the DB and add them if not */
    private void addTypeHierarchy(TypeUsage t) throws SQLException {
        List<String> typeHierarchy = t.getTypeHierarchy();
        Collections.reverse(typeHierarchy);
        logger.info("Adding TypeHierarchy to db: {}", typeHierarchy);

        // TODO does this offer any actual performance advantages or is it rather useless?
        ResultSet typeCount;
        synchronized (checkTypesStatement) {
            checkTypesStatement.clearParameters();
            Array array = databaseConnection.createArrayOf("VARCHAR", typeHierarchy.toArray());
            checkTypesStatement.setArray(1, array);
            typeCount = checkTypesStatement.executeQuery();
            array.free();
        }

        // all types are already in the database, no need to continue
        if (typeCount.next() && typeCount.getInt(1) == typeHierarchy.size()) return;

        // check all types starting from the topmost parent and merge them in batch
        String parentName = null;
        for (String typeName : typeHierarchy) {
            synchronized (addTypeStatement) {
                addTypeStatement.clearParameters();
                addTypeStatement.setString(1, typeName);
                if (parentName != null) {
                    addTypeStatement.setString(2, parentName);
                } else {
                    addTypeStatement.setNull(2, Types.VARCHAR);
                }
                addTypeStatement.execute();
            }
            parentName = typeName;
        }
    }

    /** Add all method calls of this TU into the DB **/
    private int addMethodCalls(TypeUsage t) throws SQLException {
        // determine typeID of type the method calls belong to
        Integer typeId;
        synchronized (getTypeStatement) {
            getTypeStatement.clearParameters();
            getTypeStatement.setString(1, t.type);
            ResultSet rs = getTypeStatement.executeQuery();
            if (!rs.next()) {
                throw new RuntimeException("Invalid state! Type doesn't exist in addMethodCalls() even though it should've been added by addTypeHierrachy()");
            }
            typeId = rs.getInt(1);
        }

        Set<String> methodsCalls = t.getMethodCalls();
        logger.info("Saving methodcalls to db: {}", methodsCalls);
        synchronized (addMethodStatement) {
            addMethodStatement.clearParameters();
            addMethodStatement.setInt(2, typeId);
            addMethodStatement.setInt(3, typeId);

            for (String call : methodsCalls) {
                addMethodStatement.setString(1, call);
                addMethodStatement.addBatch();
            }

            addMethodStatement.executeBatch();
        }

        return typeId;
    }

    /** Add the actual type usage to db */
    private int addTypeUsage(TypeUsage t) throws SQLException {
        ResultSet ret;
        synchronized (addTypeUsageStatement) {
            addTypeUsageStatement.clearParameters();
            addTypeUsageStatement.setString(1, t.type);

            addTypeUsageStatement.setString(2, t.getLocation());
            if (t.getLineNr() != null) {
                addTypeUsageStatement.setInt(3, t.getLineNr());
            } else {
                addTypeUsageStatement.setNull(3, Types.INTEGER);
            }

            addTypeUsageStatement.setString(4, t.getContext());
            addTypeUsageStatement.execute();

            ret = addTypeUsageStatement.getGeneratedKeys();
            if (ret.next()) {
                return ret.getInt(1);
            }
        }
        throw new RuntimeException("TypeUsage not succesfully inserted!");
    }

    /** Add the actual methodcalls called on this TU */
    private void addCallList(int typeusageId, Integer typeId, TypeUsage t) throws SQLException {
        synchronized (addCallStatement) {
            addCallStatement.clearParameters();
            addCallStatement.setInt(1, typeusageId);
            addCallStatement.setInt(2, typeId);

            int position = 0;
            //TODO really use "methodCallsInOrder"? seems to have a lot of problems regarding double use + then later equality analysis...
            for (String call : t.getMethodCalls()) {
                addCallStatement.setString(3, call);
                addCallStatement.setInt(4, position);
                addCallStatement.addBatch();
                position++;
            }
            addCallStatement.executeBatch();
        }
    }
}
