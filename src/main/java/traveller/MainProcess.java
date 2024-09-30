package traveller;

import db.JumpLinks;

import java.sql.Connection;
import java.sql.SQLException;

public class MainProcess {

    static int year = 0;

    public static void main(String[] args) {
        int timeToRun = 15;

        // first get the current year
        Connection connection = World.dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT MAX(year) FROM global");
            year = result.next() ? result.getInt(1) + 1 : 0;
            result.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < timeToRun; i++) {
            oneYear();

            JumpLinks.empireRelationsAndTargets(year);
        }
    }

    public final static void oneYear() {

        // we read in all the worlds between each step
        Economy.calculateBudgets(year, World.getAllWorlds());

        Economy.progressDevelopment(year, World.getAllWorlds());

        Economy.endOfYear(year, World.getAllWorlds());

        year++;
    }
}
