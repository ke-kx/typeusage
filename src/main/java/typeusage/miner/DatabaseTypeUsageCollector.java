package typeusage.miner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseTypeUsageCollector extends TypeUsageCollector {

    private Connection databaseConnection;

    public DatabaseTypeUsageCollector(String databaseLocation) {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:" + databaseLocation, "SA", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(TypeUsage t) {
        System.out.println(t);

    }
}
