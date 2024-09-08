package traveller;

import db.MySQLLink;

import java.awt.*;
import java.sql.Connection;

public class System {

    int id = -1;
    String name;
    Point location;
    String sector;
    static MySQLLink dbLink = new MySQLLink();

    public System(String name, int x, int y, String sector) {
        this.name = name;
        this.location = new Point(x, y);
        this.sector = sector;
    }

    public int getId() {
        return id;
    }

    public static void setDbLink(MySQLLink dbLink) {
        System.dbLink = dbLink;
    }

    public System(int id) {
        Connection connection = dbLink.getConnection();
        try {
            String query = "SELECT * FROM systems WHERE id = " + id;
            var result = connection.createStatement().executeQuery(query);
            result.next();
            this.id = id;
            this.name = result.getString("name");
            this.location = new Point(result.getInt("x"), result.getInt("y"));
            this.sector = result.getString("sector");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void initialiseTable(boolean drop) {
        Connection connection = dbLink.getConnection();
        try {
            if (drop) {
                String update = "DROP TABLE IF EXISTS systems";
                connection.createStatement().executeUpdate(update);
            }

            String update = "CREATE TABLE IF NOT EXISTS systems (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "sector VARCHAR(255) NOT NULL)";
            connection.createStatement().executeUpdate(update);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int write() {
        // get a connection, and write the system to the database
        Connection connection = dbLink.getConnection();
        try {
            if (id == -1) { // need to insert
                String update = "INSERT INTO systems (name, x, y, sector) " +
                        "VALUES ('" + name + "',"  + location.x + "," + location.y + ", '" + sector + "')";
                connection.createStatement().executeUpdate(update);
                var result = connection.createStatement().executeQuery("SELECT LAST_INSERT_ID()");
                result.next();
                id = result.getInt(1);
            } else {  // need to update
                String update = "UPDATE systems SET name = '" + name + "', x = " + location.x + ", y = " + location.y + ", sector = '" + sector + "' WHERE id = " + id;
                connection.createStatement().executeUpdate(update);
            }
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof System other) {
            return other.id == id && other.name.equals(name) && other.location.equals(location) && other.sector.equals(sector);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "System{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", sector='" + sector + '\'' +
                '}';
    }
}
