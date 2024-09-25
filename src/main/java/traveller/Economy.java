package traveller;

import db.MySQLLink;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static traveller.Constants.Status.IN_PROGRESS;
import static traveller.Constants.costPerMilitaryUnit;
import static traveller.Constants.maintenancePerMilitaryUnit;

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

            double gwp = resources * 0.1 * infrastructure * world.techLevel * world.popMantissa * Math.pow(10, world.popExponent - 8) / (world.culture + 1);

            // then trade modifier
            Empire empire = new Empire(world.empire);
            double tradeAccess = empire.tradeAccess + empire.tradeValue - world.starportRank;
            double tradeModifier = Math.pow(world.starportRank, 2.5) / 100.0 * (1 - 1 / Math.sqrt(5 * Math.pow(6.0/world.starportRank, 5)  + tradeAccess));

            if (tradeModifier < 1.0) tradeModifier = 1.0;
            world.gwp = gwp * tradeModifier;

            double expenses = (world.culture * 2 + world.infrastructure) * 0.01 * gwp * 0.4;
            double militaryMaintenance = world.military * maintenancePerMilitaryUnit;

            world.treasury += gwp * 0.4 - expenses - militaryMaintenance;

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
                double fraction = budget / maxSpend;
                double evenSplit = budget / orderList.size();

                // loop through orders and spend the available money
                for (Order order : orderList) {
                    double maxSpendPerYear = order.totalCost / order.minDuration;
                    double spend = fraction >= 1.0 ? maxSpendPerYear : Math.min(maxSpendPerYear, evenSplit);
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
                    newOrders(year, world, orderList);

                world.write();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void newOrders(int year, World world, List<Order> currentOrders) {
        // we create new development orders, this does not update world in any way
        try {
            Connection connection = dbLink.getConnection();

            double budget = Math.min(world.treasury, world.gwp * 0.5);
            boolean starportInProgress = world.techLevel < 7; // can't build starport without spacefaring tech
            boolean infraInProgress = world.techLevel <= world.infrastructure;  // tech is limit on infra
            boolean techInProgress = false;
            boolean militaryUnderConstruction = false;
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
                } else if (order.type == Order.OrderType.MILITARY) {
                    militaryUnderConstruction = true;
                }
            }

            int infraTime = 2 * (world.size + atmosphericModifier(world.atmosphere));
            double infraCost = infraTime * (world.infrastructure + 1) * 0.1 * Math.sqrt(world.popMantissa * Math.pow(10, world.popExponent - 6));
            // This should really be related to the population in some way, not just the size of the world
            // if we assume that this base is for a world with about 1 million people, then we can scale it
            // but we do so as the square root of the population, as the infrastructure is not linearly related
            // denser population density makes it relatively cheaper

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
                // to account for the ability to colonise via other worlds, we batch process and the
                // results are held in the colonisable table (updated at the end of the year)
                var result2 = connection.createStatement().executeQuery("SELECT * FROM colonisable " +
                        "WHERE empire =  " + world.empire);
                boolean colonisationTargetFound = false;
                while (result2.next() && !colonisationTargetFound) {
                    World target = new World(result2.getInt("world"));
                    if (target.popExponent == 0) { // we check this in case the target has been colonised in the meantime
                        colonisationTargetFound = true;
                    } else {
                        continue; // try next one
                    }
                    if (budget < 10) {
                        order = new Order(Order.OrderType.COLONISE_LOW, world.id, target.id, year, 1, 2);
                    } else if (budget < 100) {
                        order = new Order(Order.OrderType.COLONISE_MID, world.id, target.id, year, 2, 20);
                    } else {
                        order = new Order(Order.OrderType.COLONISE_HIGH, world.id, target.id, year, 3, 200);
                    }
                    order.description = "Colonise " + target.name;

                    // and we update the population on the target world to stop a second attempt while this one is in progress
                    target.popMantissa = 1;
                    target.popExponent = 1;
                    target.write();
                }
                result2.close();
            } else if (!militaryUnderConstruction) {
                // we consider building a military force
                // we're going for a constant RU of 500 per military unit
                int maxUnitsPerYear = world.techLevel + Math.min(world.infrastructure, world.getAvailableResources());
                double possibleUnits = budget / 500;
                if (possibleUnits < 1.0) {
                    order = new Order(Order.OrderType.MILITARY, world.id, -1, year, 1, costPerMilitaryUnit);
                } else {
                    int unitsToBuild = (int) Math.min(possibleUnits, maxUnitsPerYear);
                    order = new Order(Order.OrderType.MILITARY, world.id, -1, year, 1, costPerMilitaryUnit * unitsToBuild);
                }
                order.description = String.format("Build %d military units", (int) (order.totalCost / costPerMilitaryUnit));
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
        try (Statement stmt = dbLink.getConnection().createStatement()) {
            stmt.executeUpdate("INSERT INTO messages (year, world, message) VALUES (" + year + ", " + w.id + ", '" + message + "')");
        } catch (Exception e) {
            throw new RuntimeException(message);
        }
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
                    world.military = Math.max(0, world.military - 1);
                    world.infrastructure = Math.max(0, world.infrastructure - 1);
                    if (debug) logMessage(year, world, "Bankruptcy! Infrastructure and Military reduced by 1");
                    // we also cancel any Infrastructure or Military orders
                    try (Statement stmt = dbLink.getConnection().createStatement()) {
                        stmt.executeUpdate("UPDATE orders SET Status = 'FAILED' WHERE World = " + world.id +
                                " AND (Type = 'INFRASTRUCTURE' OR Type = 'MILITARY') AND Status = 'IN_PROGRESS'");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            world.write();
        }

        Connection connection = dbLink.getConnection();
        // Then we do Empire maintenance
        // Run through all Empires and update their number of worlds and trade value
        try (Statement result = connection.createStatement()) {
            ResultSet rs = result.executeQuery("SELECT * FROM empires WHERE Collapsed = -1");
            while (rs.next()) {
                int empire = rs.getInt("id");
                String sqlQuery = "SELECT * FROM worlds WHERE Empire = " + empire;
                ResultSet rs2 = connection.createStatement().executeQuery(sqlQuery);
                double tradeValue = 0.0;
                int empireSize = 0;
                while (rs2.next()) {
                    empireSize++;
                    World w = new World(rs2.getInt("id"));
                    tradeValue += w.starportRank;
                }
                rs2.close();

                connection.createStatement().executeUpdate("UPDATE empires SET worlds = " + empireSize +
                        ", tradeValue = " + tradeValue +
                        " WHERE id = " + empire);
            }
            rs.close();

            // Now we get all Empires with which we have trading relations
            // We first extract information for all Empires for the sum of the tradeValue of other
            // Empires with which we have trading relations
            String sqlQuery = "SELECT Empire1, SUM(tradeValue) AS tradeValue FROM relations " +
                    "JOIN empires ON Empire2 = empires.id " +
                    "WHERE relations.Value > 6 " +
                    "GROUP BY Empire1";
            rs = connection.createStatement().executeQuery(sqlQuery);
            while (rs.next()) {
                int empire = rs.getInt("Empire1");
                double tradeValue = rs.getDouble("tradeValue");
                connection.createStatement().executeUpdate("UPDATE empires SET tradeAccess = " + tradeValue +
                        " WHERE id = " + empire);
            }

            rs.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // then update the global year

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
