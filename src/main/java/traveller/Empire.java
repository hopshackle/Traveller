package traveller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    public List<World> getWorlds() {
        List<World> worlds = new ArrayList<>();
        Connection conn = World.dbLink.getConnection();
        ResultSet rs = null;
    
        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM worlds WHERE empire = " + this.id);
    
            while (rs.next()) {
                World world = new World(rs.getInt("id"));
                worlds.add(world);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        return worlds;
    }
        
    public Map<Integer, Relation.Level> getRelations() {
        Map<Integer, Relation.Level> relations = new HashMap<>();
        Connection conn = World.dbLink.getConnection();
        ResultSet rs = null;
    
        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM relations WHERE Empire1 = " + this.id);
    
            while (rs.next()) {
                int targetEmpireId = rs.getInt("Empire2");
                int relationValue = rs.getInt("Value");
    
                relations.put(targetEmpireId, Relation.Level.fromInt(relationValue));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        return relations;
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

    @Override
    public String toString() {
        return name;
    }

    public String fullDetails() {
        World capitalWorld = new World(capital);
        StringBuffer sb = new StringBuffer();
        sb.append(name + " (" + size + " worlds, Tech Level " + capitalWorld.techLevel + ")\n");
        sb.append(description + "\n");
        sb.append("Worlds:\n");
        for (World world : getWorlds()) {
            sb.append("\t" + world.name + " : " + world.getUWP() + " : Tech Level " + world.techLevel + " : Pop " + world.popExponent + " : Fleet " + world.military + "\n");
        }
        Map<Integer, Relation.Level> neighbours = getRelations();
        if (!neighbours.isEmpty()) {
            sb.append("Neighbours:\n");
            for (Map.Entry<Integer, Relation.Level> link : neighbours.entrySet()) {
                Empire otherEmpire = new Empire(link.getKey());
                sb.append("\t" + otherEmpire.name + " : " + link.getValue() + "\n");
            }
        }
        sb.append("\n");

        return sb.toString();
    }
}
