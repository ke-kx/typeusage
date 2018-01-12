package typeusage.miner;

import java.sql.*;
import java.util.Collections;
import java.util.List;

public class DatabaseTypeUsageCollector extends TypeUsageCollector {

    private Connection databaseConnection;
    private PreparedStatement checkTypesStatement;
    private PreparedStatement getTypeStatement;
    private PreparedStatement addTypeStatement;

    public DatabaseTypeUsageCollector(String databaseLocation) {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:" + databaseLocation, "SA", "");
            checkTypesStatement = databaseConnection.prepareStatement("SELECT COUNT(*) FROM \"type\" WHERE \"name\" IN( UNNEST(?) )");
            getTypeStatement = databaseConnection.prepareStatement("SELECT \"typeId\" FROM \"type\" WHERE \"name\" = ?");
            addTypeStatement = databaseConnection.prepareStatement("INSERT INTO \"type\" (\"typeId\", \"parentId\", \"name\") VALUES(DEFAULT, ?, ?)");
        } catch (SQLException e) {
            e.printStackTrace();
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

    // TODO check if type saftety is better or creating new statements for each thing
    /** Check if all types along the hierarchy of this typeusage are already in the DB and add them if not */
    private synchronized void addTypeHierarchy(TypeUsage t) throws SQLException {
        List<String> typeHierarchy = t.getTypeHierarchy();
        System.out.println(typeHierarchy);
        Array array = databaseConnection.createArrayOf("VARCHAR", typeHierarchy.toArray());
        checkTypesStatement.setArray(1, array);
        ResultSet typeCount = checkTypesStatement.executeQuery();

        // all types are already in the database
        if (typeCount.next() && typeCount.getInt(1) == typeHierarchy.size()) return;

        Integer parentId = null;
        ResultSet currentType;
        // check all types starting from the parent and add them if necessary
        Collections.reverse(typeHierarchy);
        for (String type : typeHierarchy) {
            getTypeStatement.setString(1, type);
            currentType = getTypeStatement.executeQuery();

            if (!currentType.next()) {
                if (parentId != null) {
                    addTypeStatement.setInt(1, parentId);
                } else {
                    addTypeStatement.setNull(1, Types.INTEGER);
                }
                addTypeStatement.setString(2, type);
                addTypeStatement.execute();

                // execute again to get current ID
                currentType = getTypeStatement.executeQuery();
                currentType.next();
            }
            // set parentId for next iteration
            parentId = currentType.getInt(1);
        }
    }
}
