package traveller;

import java.sql.Connection;

public class Relation {

    World one;
    World two;
    int value;
    int id = -1;

    public Relation(int id) {
        // pull in from database
        Connection connection = World.dbLink.getConnection();
        try {
            var result = connection.createStatement().executeQuery("SELECT * FROM relations WHERE id = " + id);
            if (result.next()) {
                id = result.getInt("id");
                one = new World(result.getInt("Empire1"));
                two = new World(result.getInt("Empire2"));
                value = result.getInt("Value");
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
                connection.createStatement().executeUpdate("INSERT INTO relations (Empire1, Empire2, Value) VALUES (" + one.getId() + ", " + two.getId() + ", " + value + ")");
                var result = connection.createStatement().executeQuery("SELECT LAST_INSERT_ID()");
                id = result.next() ? result.getInt(1) : -1;
                result.close();
            } else {
                connection.createStatement().executeUpdate("UPDATE relations SET Value = " + value + " WHERE id = " + id);
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
            return one.equals(other.one) && two.equals(other.two);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return one.hashCode() + two.hashCode();
    }

}
