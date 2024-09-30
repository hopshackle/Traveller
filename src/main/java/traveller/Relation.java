package traveller;

import java.sql.Connection;

public class Relation {

    enum Level {
        AT_WAR(1), COLD_WAR(2), RIVALS(3), NEUTRAL(4), COMMUNICATING(5), CORDIAL(6), TRADE_PARTNERS(7),
        FRIENDLY(8), RESEARCH_PARTNERS(9), DEFENSIVE_ALLIANCE(10), ALLIES(11), FULL_ALLIANCE(12),
        LEGAL_UNION(13), ECONOMIC_UNION(14), FULLY_INTEGRATED(15);

        int value;
        Level(int value) {
            this.value = value;
        }

        public static Level fromInt(int value) {
            for (Level level : Level.values()) {
                if (level.value == value) {
                    return level;
                }
            }
            return null;
        }
    }

    World one;
    World two;
    Level value;
    int id = -1;

    public Relation(World one, World two, Level value) {
        this.one = one;
        this.two = two;
        this.value = value;
    }

    public Relation(int id) {
        // pull in from database
        Connection connection = World.dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT * FROM relations WHERE id = " + id);
            if (result.next()) {
                one = new World(result.getInt("Empire1"));
                two = new World(result.getInt("Empire2"));
                value = Level.fromInt(result.getInt("Value"));
            }
            result.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void write() {
        Connection connection = World.dbLink.getConnection();
        try {
            if (id == -1) {
                connection.createStatement().executeUpdate("INSERT INTO relations (Empire1, Empire2, Value) VALUES (" + one.getId() + ", " + two.getId() + ", " + value.value + ")");
                var result = connection.createStatement().executeQuery("SELECT LAST_INSERT_ID()");
                id = result.next() ? result.getInt(1) : -1;
                result.close();
            } else {
                connection.createStatement().executeUpdate("UPDATE relations SET Value = " + value.value + " WHERE id = " + id);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return one + " - " + two + " : " + value;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Relation) {
            Relation other = (Relation) obj;
            return one.equals(other.one) && two.equals(other.two) && value == other.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 2489 - one.hashCode() + two.hashCode() * 31 + value.ordinal() * 255;
    }

}
