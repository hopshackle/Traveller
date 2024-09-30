package traveller;

import db.MySQLLink;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.toHexString;

public class World {

    int id = -1;
    String name;
    Hex location;
    String sector;
    int size;
    int popExponent;
    int atmosphere;
    int hydrographics;
    int techLevel;

    int infrastructure;
    int baseResources;
    int culture;
    int preTech;
    int gasGiantCount, beltCount;
    int empire;
    double popMantissa, treasury, gwp;
    String starport;
    int starportRank;
    int military;
    int progression, advancement, growth, planning, militancy, unity, tolerance;

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

    public void changePopulation(double change) {
        double totalPop = popMantissa * Math.pow(10, popExponent) + change;
        popExponent = (int) Math.log10(totalPop);
        popMantissa = totalPop / Math.pow(10, popExponent);
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
            this.starportRank = switch (starport) {
                case "A" -> 5;
                case "B" -> 4;
                case "C" -> 3;
                case "D" -> 2;
                case "E" -> 1;
                default -> 0;
            };
            this.size = result.getInt("Size");
            this.atmosphere = result.getInt("Atmosphere");
            this.hydrographics = result.getInt("Hydrographics");
            this.popExponent = result.getInt("Population");
            this.techLevel = result.getInt("Tech");
            this.popMantissa = result.getDouble("PopDigit");
            this.infrastructure = result.getInt("Infrastructure");
            this.baseResources = result.getInt("BaseResources");
            this.culture = result.getInt("Culture");
            this.progression = result.getInt("Progression");
            this.advancement = result.getInt("Advancement");
            this.growth = result.getInt("Growth");
            this.planning = result.getInt("Planning");
            this.militancy = result.getInt("Militancy");
            this.unity = result.getInt("Unity");
            this.tolerance = result.getInt("Tolerance");
            this.preTech = result.getInt("PreTech");
            this.gasGiantCount = result.getInt("GasGiants");
            this.beltCount = result.getInt("Belts");
            this.treasury = result.getDouble("Treasury");
            this.gwp = result.getDouble("GWP");
            this.empire = result.getInt("Empire");
            this.military = result.getInt("Military");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int write() {
        // get a connection, and write the system to the database
        Connection connection = dbLink.getConnection();
        String query = "";
        try {
            if (id == -1) { // need to insert
                query = "INSERT INTO systems (name, x, y, sector) " +
                        "VALUES ('" + name + "'," + location.x + "," + location.y + ", '" + sector + "')";
                connection.createStatement().executeUpdate(query);
                var result = connection.createStatement().executeQuery("SELECT LAST_INSERT_ID()");
                result.next();
                id = result.getInt(1);

                // write the world data
                query = "INSERT INTO worlds (id, Starport, Size, Atmosphere, Hydrographics, Population, Tech, " +
                        "PopDigit, Infrastructure, BaseResources, Culture, PreTech, GasGiants, Belts, Treasury, GWP, " +
                        " Empire, Military) " +
                        "VALUES (" + id + ", '" + starport + "', " + size + ", " + atmosphere + ", " +
                        hydrographics + ", " + popExponent + ", " + techLevel + ", " + popMantissa + ", " +
                        infrastructure + ", " + baseResources + ", " + culture + ", " + preTech + ", " +
                        gasGiantCount + ", " + beltCount + "," + treasury + "," + gwp + ", " + empire + ", " + military + ")";
                connection.createStatement().executeUpdate(query);
            } else {  // need to update
                query = "UPDATE worlds SET " +
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
                        "GWP = " + gwp + ", " +
                        "Empire = " + empire + ", " +
                        "Military = " + military + " " +
                        "WHERE id = " + id;
                connection.createStatement().executeUpdate(query);
            }
            return id;
        } catch (Exception e) {
            System.out.println(query);
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

    public String keywordDescription() {
        // we return a string with 6 components (Progression through Unity)
        StringBuilder sb = new StringBuilder();
        sb.append(switch (progression) {
            case 0, 1, 2, 3 -> "Radical";
            case 4, 5, 6, 7 -> "Progressive";
            case 8, 9, 10, 11 -> "Conservative";
            default -> "Reactionary";
        });
        sb.append(", ").append(switch (advancement) {
            case -3, -2, -1  -> "Mercurial";
            case 0, 1, 2, 3 -> "Enterprising";
            case 4, 5, 6, 7 -> "Balanced";
            case 8, 9, 10, 11 -> "Careful";
            case 12, 13, 14 -> "Stagnant";
            default -> "Decaying";
        });
        sb.append(", ").append(switch (growth) {
            case -3, -2, -1  -> "Imperialist";
            case 0, 1, 2, 3 -> "Expansionist";
            case 4, 5, 6, 7 -> "Competitive";
            case 8, 9, 10, 11 -> "Stable";
            case 12, 13, 14 -> "Passive";
            default -> "Shrinking";
        });
        sb.append(", ").append(switch (planning) {
            case -3, -2, -1  -> "Chaotic";
            case 0, 1, 2, 3 -> "Short-term thinkers";
            case 4, 5, 6, 7 -> "Organised";
            case 8, 9, 10, 11 -> "Strategic";
            case 12, 13, 14 -> "Long-term thinkers";
            default -> "Very long-term thinkers";
        });
        sb.append(", ").append(switch (militancy) {
            case -3, -2, -1  -> "Belligerent";
            case 0, 1, 2, 3 -> "Militant";
            case 4, 5, 6, 7 -> "Neutral";
            case 8, 9, 10, 11 -> "Peaceful";
            case 12, 13, 14 -> "Conciliatory";
            default -> "Pacifist";
        });
        sb.append(", ").append(switch (tolerance) {
            case -3, -2, -1  -> "Xenophilic";
            case 0, 1, 2 -> "Friendly";
            case 3, 4, 5-> "Open";
            case 6, 7, 8 -> "Reserved";
            case 9, 10, 11 -> "Isolationist";
            default -> "Xenophobic";
        });
        sb.append(", and ").append(switch (unity) {
            case 1, 2  -> "Monolithic";
            case 3, 4, 5 -> "Homogeneous";
            case 6, 7 -> "Harmonious";
            case 8, 9  -> "Discordant";
            case 10, 11 -> "Fragmented";
            default -> "Anarchic";
        });
        return sb.toString();
    }

    public int culturalDistanceTo(World other) {
        return (int) Math.round((Math.abs(progression - other.progression) +
                        Math.abs(advancement - other.advancement) +
                        Math.abs(growth - other.growth) +
                        Math.abs(planning - other.planning) +
                        Math.abs(militancy - other.militancy) +
                        Math.abs(unity - other.unity) +
                        Math.abs(tolerance - other.tolerance) +
                        Math.abs(culture - other.culture)) / 8.0);
    }

    public boolean isAgricultural() {
        return atmosphere >= 4 && atmosphere <= 9 &&
                hydrographics >= 4 && hydrographics <= 8 &&
                popExponent >= 5 && popExponent <= 7;
    }

    public static List<World> getAllWorlds() {
        try {
            Connection connection = dbLink.getConnection();
            var result = connection.createStatement().executeQuery("SELECT id FROM worlds");
            List<World> worlds = new ArrayList<>();
            while (result.next()) {
                worlds.add(new World(result.getInt("id")));
            }
            return worlds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public int getSize() {
        return size;
    }

    public int getPopExponent() {
        return popExponent;
    }

    public int getAtmosphere() {
        return atmosphere;
    }

    public int getHydrographics() {
        return hydrographics;
    }

    public int getTechLevel() {
        return techLevel;
    }

    public int getInfrastructure() {
        return infrastructure;
    }

    public int getCulture() {
        return culture;
    }

    public int getEmpire() {
        return empire;
    }

    public int getStarportRank() {
        return starportRank;
    }

    public String getStarport() {
        return starport;
    }
}
