package typeusage.miner;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DatabaseTypeUsageCollector extends TypeUsageCollector {

    private final Connection databaseConnection;

    // statements for type hierarchy
    private final PreparedStatement checkTypesStatement;
    private final PreparedStatement getTypeStatement;
    private final PreparedStatement addTypeStatement;

    // statements for methods
    private final PreparedStatement addMethodStatement;

    public DatabaseTypeUsageCollector(String databaseLocation) {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:" + databaseLocation, "SA", "");
            checkTypesStatement = databaseConnection.prepareStatement("SELECT COUNT(*) FROM type WHERE typeName IN( UNNEST(?) )");
            getTypeStatement = databaseConnection.prepareStatement("SELECT typeId FROM type WHERE typeName = ?");
            addTypeStatement = databaseConnection.prepareStatement("INSERT INTO type (typeId, parentId, typeName) VALUES(DEFAULT, ?, ?)");
            addMethodStatement = databaseConnection.prepareStatement(
                "MERGE INTO method USING (VALUES( ? )) AS vals(methodName) " +
                    "ON method.typeId = ? AND method.methodName = vals.methodName " +
                    "WHEN NOT MATCHED THEN INSERT VALUES (DEFAULT, ?, vals.methodName)");
        } catch (SQLException e) {
            e.printStackTrace();
            // rethrow to enable setting final variables in try-catch block
            throw new RuntimeException(e);
        }
    }

    @Override
    public DatabaseTypeUsageCollector run() {
        super.run();
        try {
            databaseConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void receive(TypeUsage t) {
        System.out.println(t);

        try {
            addTypeHierarchy(t);

            addMethodCalls(t);

            // add the actual type usage

            // add the actual methodcalls called on this TU

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /** Check if all types along the hierarchy of this typeusage are already in the DB and add them if not */
    private void addTypeHierarchy(TypeUsage t) throws SQLException {
        List<String> typeHierarchy = t.getTypeHierarchy();
        Collections.reverse(typeHierarchy);
        debug("Adding TypeHierarchy to db: %s\n", typeHierarchy);

        //TODO replace this complicated stuff with a MERGE statement?! http://hsqldb.org/doc/2.0/guide/dataaccess-chapt.html#dac_merge_statement
        // especially because right now it's not actually thread safe anymore...
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

        // check all types starting from the topmost parent and add them if necessary
        Integer parentId = null;
        ResultSet currentType;
        for (String type : typeHierarchy) {
            synchronized (getTypeStatement) {
                getTypeStatement.clearParameters();
                getTypeStatement.setString(1, type);
                currentType = getTypeStatement.executeQuery();
            }

            if (!currentType.next()) {
                synchronized (addTypeStatement) {
                    addMethodStatement.clearParameters();
                    if (parentId != null) {
                        addTypeStatement.setInt(1, parentId);
                    } else {
                        addTypeStatement.setNull(1, Types.INTEGER);
                    }
                    addTypeStatement.setString(2, type);
                    addTypeStatement.execute();
                }

                // get current ID, set parameter again since we don't now if another thread used this statement in the meantime
                synchronized (getTypeStatement) {
                    getTypeStatement.clearParameters();
                    getTypeStatement.setString(1, type);
                    currentType = getTypeStatement.executeQuery();
                }
                currentType.next();
            }
            // set parentId for next iteration
            parentId = currentType.getInt(1);
        }
    }

    /** Add all method calls of this TU into the DB **/
    private void addMethodCalls(TypeUsage t) throws SQLException {
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
        debug("Saving methodcalls to db: %s\n", methodsCalls);
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
    }

    @Override
    public void debug(String format, Object... args) {
        System.out.printf(format, args);
    }

}
