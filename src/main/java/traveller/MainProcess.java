package traveller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainProcess {

    public static void main(String[] args) {
        // run the process for 10 years
        for (int i = 0; i < 25; i++) {
            oneYear();
        }
    }

    public final static void oneYear() {

        // first get the current year
        Connection connection = World.dbLink.getConnection();
        int year;
        try {
            var result = connection.createStatement().executeQuery("SELECT MAX(year) FROM global");
            year = result.next() ? result.getInt(1) + 1 : 0;
            result.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Economy.calculateBudgets(year);

        Economy.progressDevelopment(year);

        Economy.developmentOrders(year);

        Economy.endOfYear(year);
    }
}
