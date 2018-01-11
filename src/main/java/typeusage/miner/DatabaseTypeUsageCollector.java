package typeusage.miner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseTypeUsageCollector extends TypeUsageCollector {

    private Connection databaseConnection;

    public DatabaseTypeUsageCollector() {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:output/testdb", "SA", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(TypeUsage t) {
        System.out.println(t);

    }
}
