package traveller;

import db.MySQLLink;

import java.sql.Connection;

public class Economy {

    static MySQLLink dbLink = new MySQLLink();

    public static void calculateBudgets(int year) {
        // we cycle through all Worlds and calculate their budgets
        // first load in the worlds from MySQL
        Connection connection = dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT id FROM worlds WHERE Population > 0");
            while (result.next()) {
                World world = new World(result.getInt("id"));

                int resources = world.getAvailableResources();

                double gwp = resources * 0.1 * world.techLevel * world.popMantissa * Math.pow(10, world.popExponent - 6) / (world.culture + 1);

                world.budget = gwp * 0.4;

                double expenses = (world.culture * 2 + world.infrastructure) * 0.01 * world.budget;

                world.treasury += world.budget - expenses;

                // then population increases by 1% per year (to be changed later)
                world.popMantissa *= 1.01;
                if (world.popMantissa >= 10) {
                    world.popMantissa /= 10;
                    world.popExponent++;
                }

                world.write();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void developmentOrders(int year) {
        // we create new development orders
        // for now we'll just upgrade Starport up to D as first priority
        // then tech uplift and infrastructure

        Connection connection = dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT id FROM worlds WHERE Population > 0");
            while (result.next()) {
                World world = new World(result.getInt("id"));
                double budget = world.budget + world.treasury;

                // we then need to check to see what orders are already in progress for the world
                var orders = connection.createStatement().executeQuery("SELECT * FROM orders WHERE World = " + world.id + " AND Status = 'IN_PROGRESS'");
                boolean starportInProgress = false;
                boolean infraInProgress = false;
                while (orders.next()) {
                    Order order = new Order(orders.getInt("id"));
                    if (order.type == Order.OrderType.STARPORT) {
                        starportInProgress = true;
                    } else if (order.type == Order.OrderType.INFRASTRUCTURE) {
                        infraInProgress = true;
                    }
                }

                int infraTime = 2 * (world.size + atmosphericModifier(world.atmosphere));
                double infraCost = infraTime * (world.infrastructure + 1) * 0.1;

                Order order = null;
                if (!starportInProgress && world.starport.equals("X")) {
                    order = new Order(Order.OrderType.STARPORT, world.id, -1, year, year + 1, 0);
                    order.description = "Upgrade Starport to E";
                } else if (!starportInProgress && world.starport.equals("E") && budget >= 0.6) {
                    order = new Order(Order.OrderType.STARPORT, world.id, -1, year, year + 2, 1.2);
                    order.description = "Upgrade Starport to D";
                } else if (!infraInProgress) {
                    order = new Order(Order.OrderType.INFRASTRUCTURE, world.id, -1, year, year + infraTime, infraCost);
                    order.description = "Improve Infrastructure to " + (world.infrastructure + 1);
                }
                if (order != null)
                    order.write();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void progressDevelopment(int year) {
        // we iterate through all orders and progress them
        Connection connection = dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT * FROM orders WHERE Status = 'IN_PROGRESS'");
            while (result.next()) {
                Order order = new Order(result.getInt("id"));
                World world = new World(order.worldId);
                // first we pay any costs
                double cost = order.totalCost / (order.endYear - order.startYear);
                world.budget -= cost;

                // then we progress the order if we have reached the end year
                if (year == order.endYear) {
                    if (order.type == Order.OrderType.STARPORT) {
                        if (world.starport.equals("X")) {
                            world.starport = "E";
                        } else if (world.starport.equals("E")) {
                            world.starport = "D";
                        }
                    } else if (order.type == Order.OrderType.INFRASTRUCTURE) {
                        world.infrastructure++;
                    }
                    order.status = Constants.Status.COMPLETED;
                    order.write();
                    world.write();
                }
                order.write();
                world.write();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void endOfYear(int year) {
        // we run through all worlds and update their treasury
        Connection connection = dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT id FROM worlds WHERE Population > 0");
            while (result.next()) {
                World world = new World(result.getInt("id"));
                world.treasury += world.budget;
                world.write();
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public static int atmosphericModifier(int atmosphere) {
        return switch (atmosphere) {
            case 0, 11, 12 -> 8;
            case 1, 2, 3, 4, 5 -> 7 - atmosphere;
            case 6, 8 -> 0;
            case 7, 9 -> 3;
            case 10 -> 6;
            case 13, 14 -> 1;
            case 15 -> 2;
            default -> 10;
        };
    }
}
