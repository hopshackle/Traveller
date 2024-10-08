package db;

import traveller.Economy;
import traveller.Empire;
import traveller.Relation;
import traveller.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class JumpLinks {

    // the aim of this class is to provide a way to link worlds together
    // we read in the worlds table, and then create a list of links between them
    // which is written to the database

    static int STARTING_LEVEL = 4;
    static Random rnd = new Random();

    public static void main(String[] args) {
        // get a connection
        MySQLLink dbLink = new MySQLLink();
        var connection = dbLink.getConnection();

        List<World> worlds = new ArrayList<>();
        // get all the worlds

        try {
            String query = "SELECT id FROM worlds";
            ResultSet result = connection.createStatement().executeQuery(query);

            // load the worlds into manageable format
            while (result.next()) {
                worlds.add(new World(result.getInt("id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Now we have all the worlds, we can create the links
        for (World world : worlds) {
            // get distances to all worlds
            Map<World, Integer> links = new HashMap<>();
            for (World other : worlds) {
                if (world.equals(other)) {
                    continue;
                }
                int distance = world.getLocation().distance(other.getLocation());
                if (distance > 6) {
                    continue;
                }
                links.put(other, distance);

            }
            // now we have the links, we can write them to the database
            for (World link : links.keySet()) {
                try {
                    String query = "INSERT INTO links (fromWorld, toWorld, distance) VALUES (" + world.getId() + ", " + link.getId() + ", " + links.get(link) + ")";
                    connection.createStatement().executeUpdate(query);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void empireRelationsAndTargets(int year) {
        // We run through all worlds, only worrying about those with a Starport of at least D (or C?)
        // we then check the jump technology of the Empire to which the world belongs, and check all links within range
        // if a world is no populated, then we add it to the list of colonisable targets
        // if a world is populated, then we add it to the list of contacted empires

        List<World> worlds = World.getAllWorlds();
        // get a connection
        MySQLLink dbLink = new MySQLLink();
        var connection = dbLink.getConnection();

        try{

            // first load in all current relations to avoid duplication
            String query = "SELECT Empire1, Empire2 FROM relations";
            ResultSet result = connection.createStatement().executeQuery(query);
            Set<String> relations = new HashSet<>();
            while (result.next()) {
                relations.add(result.getInt("Empire1") + "-" + result.getInt("Empire2"));
            }
            result.close();

            // then drop the colonisable table
            query = "DELETE FROM colonisable WHERE empire > 0";
            connection.createStatement().executeUpdate(query);

            for (World w : worlds) {
                if (w.getTechLevel() < 9) {
                    continue;
                }
                // get the empire
                int empireID = w.getEmpire();
                // get the jump technology
                int range = w.getTechLevel() - 9;
                if (range < 1) {
                    range = 1;
                }

                // get the links
                query = "SELECT toWorld FROM links WHERE fromWorld = " + w.getId() + " AND distance <= " + range;
                result = connection.createStatement().executeQuery(query);
                while (result.next()) {
                    World target = new World(result.getInt("toWorld"));
                    if (target.getEmpire() == empireID) {
                        continue;
                    }
                    int culturalDistance = w.culturalDistanceTo(target);
                    // we use this as a modifier in some way...but it can be pretty large
                    // 4 + d6 - culturalDistance
                    int startingRelationship = STARTING_LEVEL - culturalDistance + rnd.nextInt(6) + 1;
                    if (startingRelationship < 1) {
                        startingRelationship = 1;
                    }

                    if (target.getPopExponent() == 0) {
                        // add to colonisable targets if not already there
                        query = "SELECT * FROM colonisable WHERE world = " + target.getId() + " AND empire = " + empireID;
                        ResultSet colonisable = connection.createStatement().executeQuery(query);
                        if (colonisable.next()) {
                            continue;
                        }
                        query = "INSERT INTO colonisable (world, empire) VALUES (" + target.getId() + ", " + empireID + ")";
                        connection.createStatement().executeUpdate(query);
                    } else if (!relations.contains(empireID + "-" + target.getEmpire())) {
                        // add to contacted empires
                        query = "INSERT INTO relations (Empire1, Empire2, Value) VALUES (" + target.getId() + ", " + empireID + ", " + startingRelationship + " )";
                        connection.createStatement().executeUpdate(query);

                        // Insert both sides of the relation
                        query = "INSERT INTO relations (Empire1, Empire2, Value) VALUES (" + empireID + ", " + target.getId() + ", " + startingRelationship + " )";
                        connection.createStatement().executeUpdate(query);
                        // Add to list of known relations to avoid duplication
                        relations.add(empireID + "-" + target.getEmpire());
                        relations.add(target.getEmpire() + "-" + empireID);

                        Empire targetEmpire = new Empire(target.getEmpire());
                        Empire empire = new Empire(empireID);
                        String relString = Relation.Level.fromInt(startingRelationship).toString();
                        Economy.logMessage(year, w, ("Makes contact with " + targetEmpire.getName() + " : " + relString).trim());
                        Economy.logMessage(year, target, "Makes contact with " + empire.getName() + " : " + relString);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }

    }

}
