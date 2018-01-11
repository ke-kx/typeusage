package typeusage.miner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseTypeUsageCollector extends TypeUsageCollector {

    private Connection databaseConnection;
    private PreparedStatement statement;

    public DatabaseTypeUsageCollector(String databaseLocation) {
        try {
            databaseConnection = DriverManager.getConnection("jdbc:hsqldb:file:" + databaseLocation, "SA", "");
            statement = databaseConnection.prepareStatement("");
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
            statement.setBoolean(0, true);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
