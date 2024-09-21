package traveller;

import db.MySQLLink;

import java.sql.Connection;
import static java.lang.Integer.toHexString;

public class World {

    int id = -1;
    String name;
    Hex location;
    String sector;
    int size, popExponent, atmosphere, hydrographics, techLevel, infrastructure, baseResources, culture, preTech;
    int gasGiantCount, beltCount;
    double popMantissa, treasury, gwp;
    String starport;
    static MySQLLink dbLink = new MySQLLink();

    public World(String name, int x, int y, String sector) {
        this.name = name;
        this.location = new Hex(x, y);
        this.sector = sector;
    }

    public int getAvailableResources() {
        int resources = baseResources;
        if (!(starport.equals("X") || starport.equals("E") || techLevel < 8)) {
            resources += gasGiantCount + beltCount;
        }
        if (starport.equals("A"))
            resources += 2;
        if (starport.equals("B"))
            resources += 1;
        if (popExponent > 8)
            resources += 1;
        if (isAgricultural())
            resources += 1;
        if (atmosphere == 0)
            resources -= 1;
        if (atmosphere == 6 || atmosphere == 8)
            resources += 1;
        if (hydrographics == 0 || hydrographics == 10)
            resources -= 1;
        return resources;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Hex getLocation() {
        return location;
    }

    public String getSector() {
        return sector;
    }

    public static void setDbLink(MySQLLink dbLink) {
        World.dbLink = dbLink;
    }

    public World(int id) {
        Connection connection = dbLink.getConnection();
        try {
            String query = "SELECT * FROM systems, worlds WHERE systems.id = " + id + " AND worlds.id = " + id;
            var result = connection.createStatement().executeQuery(query);
            result.next();
            this.id = id;
            this.name = result.getString("Name");
            this.location = new Hex(result.getInt("x"), result.getInt("y"));
            this.sector = result.getString("Sector");
            this.starport = result.getString("Starport");
            this.size = result.getInt("Size");
            this.atmosphere = result.getInt("Atmosphere");
            this.hydrographics = result.getInt("Hydrographics");
            this.popExponent = result.getInt("Population");
            this.techLevel = result.getInt("Tech");
            this.popMantissa = result.getDouble("PopDigit");
            this.infrastructure = result.getInt("Infrastructure");
            this.baseResources = result.getInt("BaseResources");
            this.culture = result.getInt("Culture");
            this.preTech = result.getInt("PreTech");
            this.gasGiantCount = result.getInt("GasGiants");
            this.beltCount = result.getInt("Belts");
            this.treasury = result.getDouble("Treasury");
            this.gwp = result.getDouble("GWP");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void initialiseTable(boolean drop) {
        Connection connection = dbLink.getConnection();
        try {
            if (drop) {
                String update = "DROP TABLE IF EXISTS worlds";
                connection.createStatement().executeUpdate(update);
            }

            String update = "CREATE TABLE IF NOT EXISTS systems (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "Name VARCHAR(45) NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "Sector VARCHAR(45) NOT NULL)" +
                    "Starport VARCHAR(1) NOT NULL," +
                    "Size INT NOT NULL," +
                    "Atmosphere INT NOT NULL," +
                    "Hydrographics INT NOT NULL," +
                    "Population INT NOT NULL," +
                    "Tech INT NOT NULL," +
                    "PopDigit DOUBLE NOT NULL," +
                    "Infrastructure INT NOT NULL," +
                    "BaseResources INT NOT NULL," +
                    "Culture INT NOT NULL," +
                    "PreTech INT NOT NULL," +
                    "GasGiants INT NOT NULL," +
                    "Belts INT NOT NULL," +
                    "Treasury DOUBLE NOT NULL, " +
                    "GWP DOUBLE NOT NULL";

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
                        "VALUES ('" + name + "'," + location.x + "," + location.y + ", '" + sector + "')";
                connection.createStatement().executeUpdate(update);
                var result = connection.createStatement().executeQuery("SELECT LAST_INSERT_ID()");
                result.next();
                id = result.getInt(1);

                // write the world data
                update = "INSERT INTO worlds (id, Starport, Size, Atmosphere, Hydrographics, Population, Tech, " +
                        "PopDigit, Infrastructure, BaseResources, Culture, PreTech, GasGiants, Belts, Treasury, GWP) " +
                        "VALUES (" + id + ", '" + starport + "', " + size + ", " + atmosphere + ", " +
                        hydrographics + ", " + popExponent + ", " + techLevel + ", " + popMantissa + ", " +
                        infrastructure + ", " + baseResources + ", " + culture + ", " + preTech + ", " +
                        gasGiantCount + ", " + beltCount + "," + treasury + "," + gwp + ")";
                connection.createStatement().executeUpdate(update);
            } else {  // need to update
                String update = "UPDATE worlds SET " +
                        "Starport = '" + starport + "', " +
                        "Size = " + size + ", " +
                        "Atmosphere = " + atmosphere + ", " +
                        "Hydrographics = " + hydrographics + ", " +
                        "Population = " + popExponent + ", " +
                        "Tech = " + techLevel + ", " +
                        "PopDigit = " + popMantissa + ", " +
                        "Infrastructure = " + infrastructure + ", " +
                        "BaseResources = " + baseResources + ", " +
                        "Culture = " + culture + ", " +
                        "PreTech = " + preTech + ", " +
                        "GasGiants = " + gasGiantCount + ", " +
                        "Belts = " + beltCount + ", " +
                        "Treasury = " + treasury + ", " +
                        "GWP = " + gwp + " " +
                        "WHERE id = " + id;
                connection.createStatement().executeUpdate(update);
            }
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof World other) {
            return other.id == id;

        }
        return false;
    }

    @Override
    public int hashCode() {
        return id + 8675309;
    }

    @Override
    public String toString() {
        return "System{ " +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", sector='" + sector + '\'' +
                ". UWP=" + String.format("%s%s%s%s-%s", toHexString(size), toHexString(atmosphere),
                toHexString(hydrographics), toHexString(popExponent), toHexString(techLevel)) +
                " }";
    }

    public boolean isAgricultural() {
        return atmosphere >= 4 && atmosphere <= 9 &&
                hydrographics >= 4 && hydrographics <= 8 &&
                popExponent >= 5 && popExponent <= 7;
    }
}
