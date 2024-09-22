package traveller;

import db.MySQLLink;

import java.sql.Connection;

public class Economy {

    static MySQLLink dbLink = new MySQLLink();

    static boolean debug = true;

    public static void calculateBudgets(int year) {
        // we cycle through all Worlds and calculate their budgets
        // first load in the worlds from MySQL
        Connection connection = dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT id FROM worlds WHERE Population > 0");
            while (result.next()) {
                World world = new World(result.getInt("id"));

                double resources = world.getAvailableResources();
                if (resources < 1) resources = 1.0 / (resources + 2);
                double infrastructure = world.infrastructure;
                if (infrastructure < 1) infrastructure = 1.0 / (infrastructure + 2);

                double gwp = resources * 0.1 * infrastructure * world.techLevel * world.popMantissa * Math.pow(10, world.popExponent - 6) / (world.culture + 1);

                world.gwp = gwp;

                double expenses = (world.culture * 2 + world.infrastructure) * 0.01 * gwp * 0.4;

                world.treasury += gwp * 0.4 - expenses;

                // then population increases by 1% per year (to be changed later)
                world.popMantissa *= 1.01;
                if (world.popMantissa >= 10) {
                    world.popMantissa /= 10;
                    world.popExponent++;
                    if (debug) logMessage(year, world, "Population increases to " + world.popExponent);
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
                double budget = Math.min(world.treasury, world.gwp);

                // we then need to check to see what orders are already in progress for the world
                var orders = connection.createStatement().executeQuery("SELECT * FROM orders WHERE World = " + world.id + " AND Status = 'IN_PROGRESS'");
                boolean starportInProgress = world.techLevel < 7; // can't build starport without spacefaring tech
                boolean infraInProgress = world.techLevel <= world.infrastructure;  // tech is limit on infra
                boolean techInProgress = false;
                boolean colonisationInProgress = !(world.techLevel > 8 && world.starportRank > 2 && world.popExponent > 5);
                while (orders.next()) {
                    Order order = new Order(orders.getInt("id"));
                    if (order.type == Order.OrderType.STARPORT) {
                        starportInProgress = true;
                    } else if (order.type == Order.OrderType.INFRASTRUCTURE) {
                        infraInProgress = true;
                    } else if (order.type == Order.OrderType.RESEARCH) {
                        techInProgress = true;
                    } else if (order.type == Order.OrderType.COLONISE_LOW || order.type == Order.OrderType.COLONISE_MID || order.type == Order.OrderType.COLONISE_HIGH) {
                        colonisationInProgress = true;
                    }
                }

                int infraTime = 2 * (world.size + atmosphericModifier(world.atmosphere));
                double infraCost = infraTime * (world.infrastructure + 1) * 0.1 * world.popExponent;

                Order order = null;
                if (!starportInProgress && world.starport.equals("X")) {
                    order = new Order(Order.OrderType.STARPORT, world.id, -1, year, year + 1, 0);
                    order.description = "Upgrade Starport to E";
                } else if (!starportInProgress && world.starport.equals("E") && budget >= 0.6) {
                    order = new Order(Order.OrderType.STARPORT, world.id, -1, year, year + 2, 1.2);
                    order.description = "Upgrade Starport to D";
                } else if (!infraInProgress && budget >= infraCost / infraTime) {
                    order = new Order(Order.OrderType.INFRASTRUCTURE, world.id, -1, year, year + infraTime, infraCost);
                    order.description = "Improve Infrastructure to " + (world.infrastructure + 1);
                } else if (!techInProgress) {
                    double techCost = techCost(world.techLevel + 1);
                    int techTime = techTime(world.techLevel + 1);
                    if (world.preTech > world.techLevel + 1) {
                        techCost /= 5.0;
                        techTime /= 10;
                        if (techTime < 1) techTime = 1;
                    }
                    if (budget >= techCost / techTime * 2) {
                        order = new Order(Order.OrderType.RESEARCH, world.id, -1, year, year + techTime, techCost);
                        order.description = "Research new technologies to tech level " + (world.techLevel + 1);
                    }
                } else if (!starportInProgress) {
                    if (world.starport.equals("D") && budget >= 24) {
                        order = new Order(Order.OrderType.STARPORT, world.id, -1, year, year + 10, 120);
                        order.description = "Upgrade Starport to C";
                    } else if (world.starport.equals("C") && budget >= 60) {
                        order = new Order(Order.OrderType.STARPORT, world.id, -1, year, year + 20, 600);
                        order.description = "Upgrade Starport to B";
                    } else if (world.starport.equals("B") && budget >= 160) {
                        order = new Order(Order.OrderType.STARPORT, world.id, -1, year, year + 30, 2400);
                        order.description = "Upgrade Starport to A";
                    }
                } else if (!colonisationInProgress) {
                    // we can colonise a world if there is one within range
                    int colonisationRange = world.techLevel - 9;
                    if (world.techLevel == 9) colonisationRange = 1;
                    var result2 = connection.createStatement().executeQuery("SELECT * FROM worlds, links" +
                            " WHERE Population = 0 AND " +
                            "worlds.id = links.toWorld AND links.fromWorld = " + world.id + " AND " +
                            "distance <= " + colonisationRange);
                    if (result2.next()) {
                        if (budget < 20) {
                            order = new Order(Order.OrderType.COLONISE_LOW, world.id, result2.getInt("id"), year, year + 1, 2);
                        } else if (budget < 200) {
                            order = new Order(Order.OrderType.COLONISE_MID, world.id, result2.getInt("id"), year, year + 1, 20);
                        } else {
                            order = new Order(Order.OrderType.COLONISE_HIGH, world.id, result2.getInt("id"), year, year + 1, 200);
                        }
                        order.description = "Colonise " + result2.getString("Name");
                    }
                }
                if (order != null)
                    order.write();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static void logMessage(int year, World w, String message) {
        System.out.printf("%3d%20s%45s%n", year, w.name, message);
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
                world.treasury -= cost;

                // then we progress the order if we have reached the end year
                if (year == order.endYear) {
                    if (order.type == Order.OrderType.STARPORT) {
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
                        if (debug) logMessage(year, world, "Starport upgraded to " + world.starport);
                    } else if (order.type == Order.OrderType.INFRASTRUCTURE) {
                        world.infrastructure++;
                        if (debug) logMessage(year, world, "Infrastructure increased to " + world.infrastructure);
                    } else if (order.type == Order.OrderType.RESEARCH) {
                        world.techLevel++;
                        if (debug) logMessage(year, world, "Tech level increased to " + world.techLevel);
                    } else if (order.type == Order.OrderType.COLONISE_LOW || order.type == Order.OrderType.COLONISE_MID || order.type == Order.OrderType.COLONISE_HIGH) {
                        World target = new World(order.targetId);
                        target.popMantissa = 1;
                        target.popExponent = switch (order.type) {
                            case COLONISE_LOW -> 3;
                            case COLONISE_MID -> 4;
                            case COLONISE_HIGH -> 5;
                            default -> throw new RuntimeException("Invalid colonisation order");
                        };
                        target.techLevel = world.techLevel - 2;
                        target.infrastructure = switch (order.type) {
                            case COLONISE_LOW -> 1;
                            case COLONISE_MID -> 2;
                            case COLONISE_HIGH -> 3;
                            default -> throw new RuntimeException("Invalid colonisation order");
                        };
                        target.culture = world.culture;
                        target.starport = order.type == Order.OrderType.COLONISE_LOW ? "E" : "D";
                        target.empire = world.empire;
                        target.write();
                        world.changePopulation(-Math.pow(10, target.popExponent) * target.popMantissa);
                        world.write();
                        if (debug) logMessage(year, world, "Colonisation of " + target.name + " complete");
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
                world.treasury = Math.min(world.treasury, world.gwp); // the max we can store
                if (world.treasury < 0.00) {
                    world.treasury *= 1.1; // we have to pay 10% interest on debt
                    if (world.treasury < -5 * world.gwp) {
                        // let's call this bankruptcy
                        world.treasury = 0.0;
                        if (world.infrastructure > 0)
                            world.infrastructure--;
                        if (debug) logMessage(year, world, "Bankruptcy! Infrastructure reduced by 1");
                    }
                }
                world.write();
            }
            result.close();

            // now update the year in the global table
            connection.createStatement().executeUpdate("INSERT INTO global (year) VALUES (" + year + ")");
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

    public static int techTime(int newLevel) {
        return switch (newLevel) {
            case 1, 2 -> 1;
            case 3 -> 300;
            case 4 -> 150;
            case 5, 6, 7, 8, 9, 10 -> 50;
            case 11 -> 100;
            case 12 -> 200;
            case 13 -> 400;
            default -> 500;
        };
    }

    public static double techCost(int newLevel) {
        return switch (newLevel) {
            case 1, 2, 3, 4 -> 0.0;
            case 5 -> 2;
            case 6 -> 3;
            case 7 -> 20.0;
            case 8 -> 50.0;
            case 9 -> 150.0;
            case 10 -> 300.0;
            case 11 -> 500.0;
            case 12 -> 2000.0;
            case 13 -> 2e4;
            case 14 -> 2e5;
            default -> newLevel * 2e6;
        };
    }
}
