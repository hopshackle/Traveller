package traveller;

import db.MySQLLink;

import java.sql.Connection;
import java.util.*;

import static traveller.Constants.Status.IN_PROGRESS;

public class Economy {

    static MySQLLink dbLink = new MySQLLink();

    static boolean debug = true;

    public static void calculateBudgets(int year, List<World> worlds) {
        // we cycle through all Worlds and calculate their budgets
        for (World world : worlds) {
            if (world.popExponent == 0) {
                // this is a world with no population, so we don't need to do anything
                continue;
            }
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
    }


    public static void progressDevelopment(int year, List<World> worlds) {
        // process existing orders
        Connection connection = dbLink.getConnection();
        try {
            for (World world : worlds) {
                double budget = Math.min(world.treasury, world.gwp);

                if (budget <= 0)
                    continue; // cannot do anything without any money to spend

                // we then need to check to see what orders are already in progress for the world
                var orders = connection.createStatement().executeQuery("SELECT * FROM orders WHERE World = " + world.id + " AND Status = 'IN_PROGRESS'");
                List<Order> orderList = new ArrayList<>();
                double maxSpend = 0.00;
                while (orders.next()) {
                    Order order = new Order(orders.getInt("id"));
                    orderList.add(order);
                    double maxSpendPerYear = order.totalCost / order.minDuration;
                    maxSpend += maxSpendPerYear;
                }
                double fraction = Math.min(1.0, budget / maxSpend);

                // loop through orders and spend the available money
                for (Order order : orderList) {
                    double maxSpendPerYear = order.totalCost / order.minDuration;
                    double spend = Math.min(maxSpendPerYear, order.totalCost / order.minDuration * fraction);
                    order.remainingCost -= spend;
                    world.treasury -= spend;
                    if (order.remainingCost <= 0) {
                        String message = order.complete(year, world);
                        if (debug) logMessage(year, world, message);
                    }
                    order.write();
                }

                // then if we have any money left, consider a new project
                if (world.treasury > 0.0)
                    developmentOrders(year, world, orderList);

                world.write();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void developmentOrders(int year, World world, List<Order> currentOrders) {
        // we create new development orders, this does not update world in any way
        try {
            Connection connection = dbLink.getConnection();

            double budget = Math.min(world.treasury, world.gwp * 0.5);
            boolean starportInProgress = world.techLevel < 7; // can't build starport without spacefaring tech
            boolean infraInProgress = world.techLevel <= world.infrastructure;  // tech is limit on infra
            boolean techInProgress = false;
            boolean colonisationInProgress = !(world.techLevel > 8 && world.starportRank > 2 && world.popExponent > 5);
            // don't replicate an existing project
            for (Order order : currentOrders) {
                if (order.status != IN_PROGRESS) continue;
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
                order = new Order(Order.OrderType.STARPORT, world.id, -1, year, 1, 0);
                order.description = "Upgrade Starport to E";
            } else if (!starportInProgress && world.starport.equals("E")) {
                order = new Order(Order.OrderType.STARPORT, world.id, -1, year, 2, 1.2);
                order.description = "Upgrade Starport to D";
            } else if (!infraInProgress) {
                order = new Order(Order.OrderType.INFRASTRUCTURE, world.id, -1, year, infraTime, infraCost);
                order.description = "Improve Infrastructure to " + (world.infrastructure + 1);
            } else if (!techInProgress) {
                double techCost = techCost(world.techLevel + 1);
                int techTime = techTime(world.techLevel + 1);
                if (world.preTech > world.techLevel + 1) {
                    techCost /= 5.0;
                    techTime /= 10;
                    if (techTime < 1) techTime = 1;
                }
                order = new Order(Order.OrderType.RESEARCH, world.id, -1, year, techTime, techCost);
                order.description = "Research new technologies to tech level " + (world.techLevel + 1);
            } else if (!starportInProgress) {
                if (world.starport.equals("D") && budget >= 5 && world.techLevel > 7) {
                    order = new Order(Order.OrderType.STARPORT, world.id, -1, year, 10, 120);
                    order.description = "Upgrade Starport to C";
                } else if (world.starport.equals("C") && budget >= 10 && world.techLevel > 8) {
                    order = new Order(Order.OrderType.STARPORT, world.id, -1, year, 20, 600);
                    order.description = "Upgrade Starport to B";
                } else if (world.starport.equals("B") && budget >= 20 && world.techLevel > 8) {
                    order = new Order(Order.OrderType.STARPORT, world.id, -1, year, 30, 2400);
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
                    if (budget < 10) {
                        order = new Order(Order.OrderType.COLONISE_LOW, world.id, result2.getInt("id"), year, 1, 2);
                    } else if (budget < 100) {
                        order = new Order(Order.OrderType.COLONISE_MID, world.id, result2.getInt("id"), year, 2, 20);
                    } else {
                        order = new Order(Order.OrderType.COLONISE_HIGH, world.id, result2.getInt("id"), year, 3, 200);
                    }
                    order.description = "Colonise " + result2.getString("Name");

                    // and we update the population on the target world to stop a second attempt while this one is in progress
                    World target = new World(order.targetId);
                    target.popMantissa = 1;
                    target.popExponent = 1;
                    target.write();
                }
            }
            if (order != null)
                order.write();

        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }


    static void logMessage(int year, World w, String message) {
        System.out.printf("%3d%20s%45s%n", year, w.name, message);
    }

    public static void endOfYear(int year, List<World> worlds) {
        // we run through all worlds and update their treasury

        for (World world : worlds) {
            if (world.popExponent == 0) {
                // this is a world with no population, so we don't need to do anything
                continue;
            }
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

        // then update the global year
        Connection connection = dbLink.getConnection();
        try {
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
