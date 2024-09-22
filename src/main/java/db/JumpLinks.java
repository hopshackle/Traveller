package db;

import traveller.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class JumpLinks {

    // the aim of this class is to provide a way to link worlds together
    // we read in the worlds table, and then create a list of links between them
    // which is written to the database

    public static void main(String[] args) {
        // get a connection
        MySQLLink dbLink = new MySQLLink();
        var connection = dbLink.getConnection();

        List<World> worlds = new ArrayList<>();
        // get all the worlds

        try {
            String query = "SELECT id FROM worlds";
            ResultSet result = null;
            result = connection.createStatement().executeQuery(query);

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
}
