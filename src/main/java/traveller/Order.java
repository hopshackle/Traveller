package traveller;

import java.sql.Connection;
import java.sql.SQLException;

public class Order {


    public enum OrderType {
        STARPORT, RESEARCH, INFRASTRUCTURE, UPLIFT, COLONISE, ATTACK
    }

    int id = -1;
    OrderType type;
    int worldId;
    int targetId;
    Constants.Status status;
    int startYear;
    int endYear;
    double totalCost;
    double remainingCost;
    String description;

    public Order(OrderType type, int worldId, int targetId, int startYear, int endYear, double totalCost) {
        this.type = type;
        this.worldId = worldId;
        this.targetId = targetId;
        this.startYear = startYear;
        this.endYear = endYear;
        this.totalCost = totalCost;
        this.remainingCost = totalCost;
        this.status = Constants.Status.IN_PROGRESS;
    }

    public Order(int id) {
        // get the order from the database
        Connection connection = World.dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT * FROM orders WHERE id = " + id);
            if (!result.next()) {
                throw new RuntimeException("Order not found: " + id);
            }
            this.id = id;
            this.type = OrderType.valueOf(result.getString("Type"));
            this.worldId = result.getInt("World");
            this.targetId = result.getInt("Target");
            this.startYear = result.getInt("Start");
            this.endYear = result.getInt("End");
            this.totalCost = result.getDouble("TotalCost");
            this.remainingCost = result.getDouble("RemainingCost");
            this.status = Constants.Status.valueOf(result.getString("Status"));
            this.description = result.getString("Description");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int write() {
            // get a connection, and write the system to the database
            Connection connection = World.dbLink.getConnection();
            try {
                if (id == -1) { // need to insert
                    String update = "INSERT INTO orders (Type, World, Target, Start, End, TotalCost, RemainingCost, Status, Description) " +
                            "VALUES ('" + type + "'," + worldId + "," + targetId + ", " + startYear + ", " + endYear +
                            ", " + totalCost + ", " + remainingCost + ", '" + status + "', '" + description + "')";
                    connection.createStatement().executeUpdate(update);
                    var result = connection.createStatement().executeQuery("SELECT LAST_INSERT_ID()");
                    result.next();
                    id = result.getInt(1);
                } else {
                    // exclude the fields that can't be changed
                    String update = "UPDATE orders SET " +
                            "End = " + endYear + ", " +
                            "RemainingCost = " + remainingCost + ", " +
                            "Status = '" + status + "', " +
                            "Description = '" + description + "' "+
                            "WHERE id = " + id;
                    connection.createStatement().executeUpdate(update);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return id;
    }
}
