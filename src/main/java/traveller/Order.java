package traveller;

import java.sql.Connection;
import java.sql.SQLException;

public class Order {


    public enum OrderType {
        STARPORT, RESEARCH, INFRASTRUCTURE, UPLIFT, COLONISE_LOW, COLONISE_MID, COLONISE_HIGH, ATTACK
    }

    int id = -1;
    OrderType type;
    int worldId;
    int targetId;
    Constants.Status status;
    int startYear;
    int endYear;
    int minDuration;
    double totalCost;
    double remainingCost;
    String description;

    /** Assumes that the calling code will write() anything to database
     *
     * @param year
     * @param world
     * @return Message for logging
     */
    public String complete(int year, World world) {
        if (status != Constants.Status.IN_PROGRESS) {
            throw new RuntimeException("Order already completed");
        }
        if (remainingCost > 0) {
            throw new RuntimeException("Order not fully paid");
        }
        if (world.id != worldId) {
            throw new RuntimeException("Order not for this world");
        }
        status = Constants.Status.COMPLETED;
        endYear = year;
        remainingCost = 0;

        switch (type) {
            case STARPORT -> {
                if (world.starport.equals("X")) {
                    world.starport = "E";
                } else if (world.starport.equals("E")) {
                    world.starport = "D";
                } else if (world.starport.equals("D")) {
                    world.starport = "C";
                } else if (world.starport.equals("C")) {
                    world.starport = "B";
                } else if (world.starport.equals("B")) {
                    world.starport = "A";
                }
                return "Starport upgraded to " + world.starport;
            }
            case INFRASTRUCTURE -> {
                world.infrastructure++;
                return "Infrastructure increased to " + world.infrastructure;
            }
            case RESEARCH -> {
                world.techLevel++;
                return "Tech level increased to " + world.techLevel;
            }
            case COLONISE_HIGH, COLONISE_MID, COLONISE_LOW -> {

                World target = new World(targetId);
                target.popMantissa = 1;
                target.popExponent = switch (type) {
                    case COLONISE_LOW -> 3;
                    case COLONISE_MID -> 4;
                    case COLONISE_HIGH -> 5;
                    default -> throw new RuntimeException("Invalid colonisation order");
                };
                target.techLevel = world.techLevel - 2;
                target.infrastructure = switch (type) {
                    case COLONISE_LOW -> 1;
                    case COLONISE_MID -> 2;
                    case COLONISE_HIGH -> 3;
                    default -> throw new RuntimeException("Invalid colonisation order");
                };
                target.culture = world.culture;
                target.starport = type == Order.OrderType.COLONISE_LOW ? "E" : "D";
                target.empire = world.empire;
                target.write();
                world.changePopulation(-Math.pow(10, target.popExponent) * target.popMantissa);
                return "Colonisation of " + target.name + " complete";
            }
            default -> throw new RuntimeException("Order type not implemented");
        }
    }


    public Order(OrderType type, int worldId, int targetId, int startYear, int minDuration, double totalCost) {
        this.type = type;
        this.worldId = worldId;
        this.targetId = targetId;
        this.startYear = startYear;
        this.endYear = -1;
        this.minDuration = minDuration;
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
            this.minDuration = result.getInt("MinDuration");
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
                    String update = "INSERT INTO orders (Type, World, Target, Start, MinDuration, TotalCost, RemainingCost, Status, Description) " +
                            "VALUES ('" + type + "'," + worldId + "," + targetId + ", " + startYear + ", " + minDuration +
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
