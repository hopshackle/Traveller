package traveller;

import java.sql.Connection;
import java.sql.ResultSet;

public class Empire {

    int id = -1;
    String name;
    String description;
    int tradeValue;
    double tradeAccess;
    int size;
    int capital;

    public Empire(String name, int tradeValue, double tradeAccess, int size, int capital) {
        this.name = name;
        this.tradeValue = tradeValue;
        this.tradeAccess = tradeAccess;
        this.size = size;
        this.capital = capital;
    }

    public Empire(int id) {
        // extract from database
        Connection conn = World.dbLink.getConnection();
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM empires WHERE id = " + id);
            if (rs.next()) {
                this.id = id;
                this.name = rs.getString("name");
                this.description = rs.getString("description");
                this.tradeValue = rs.getInt("tradeValue");
                this.tradeAccess = rs.getDouble("tradeAccess");
                this.size = rs.getInt("worlds");
                this.capital = rs.getInt("capital");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public int getTradeValue() {
        return tradeValue;
    }
    public double getTradeAccess() {
        return tradeAccess;
    }
    public int getSize() {
        return size;
    }
    public int getCapital() {
        return capital;
    }
    public int getId() {
        return id;
    }
}
