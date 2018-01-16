package typeusage.miner;

import java.sql.*;
import java.util.Collections;
import java.util.List;

public class DatabaseTypeUsageCollector extends TypeUsageCollector {

    private final Connection databaseConnection;
    private final PreparedStatement checkTypesStatement;
    private final PreparedStatement getTypeStatement;
    private final PreparedStatement addTypeStatement;

    public DatabaseTypeUsageCollector(String databaseLocation) {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:" + databaseLocation, "SA", "");
            checkTypesStatement = databaseConnection.prepareStatement("SELECT COUNT(*) FROM \"type\" WHERE \"name\" IN( UNNEST(?) )");
            getTypeStatement = databaseConnection.prepareStatement("SELECT \"typeId\" FROM \"type\" WHERE \"name\" = ?");
            addTypeStatement = databaseConnection.prepareStatement("INSERT INTO \"type\" (\"typeId\", \"parentId\", \"name\") VALUES(DEFAULT, ?, ?)");
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

            // check if the methods called in this TU are already in the DB and add them if not

            // add the actual type usage

            // add the actual methodcalls called on this TU

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public void debug(String format, Object... args) {
        System.out.printf(format, args);
    }

    /** Check if all types along the hierarchy of this typeusage are already in the DB and add them if not */
    private void addTypeHierarchy(TypeUsage t) throws SQLException {
        List<String> typeHierarchy = t.getTypeHierarchy();
        Collections.reverse(typeHierarchy);
        debug("Adding TypeHierarchy to db: %s\n", typeHierarchy);

        ResultSet typeCount;
        synchronized (checkTypesStatement) {
            checkTypesStatement.clearParameters();
            Array array = databaseConnection.createArrayOf("VARCHAR", typeHierarchy.toArray());
            checkTypesStatement.setArray(1, array);
            typeCount = checkTypesStatement.executeQuery();
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
}
