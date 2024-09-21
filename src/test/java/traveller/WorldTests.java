package traveller;

import db.MySQLLink;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WorldTests {

    @BeforeAll
    public static void setup() {
        MySQLLink dbLink = new MySQLLink("travellertest");
        World.setDbLink(dbLink);
        World.initialiseTable(true);
    }

    @Test
    public void createNewSystem() {
        World world = new World("Test System", 1, 1, "Test Sector");
        int new_id = world.write();

        // now check this exists on the table
        World new_world = new World(new_id);
        assert new_world.name.equals("Test System");
        assert new_world.location.x == 1;
        assert new_world.location.y == 1;
        assert new_world.sector.equals("Test Sector");

        assert new_world.equals(world);
    }

    @Test
    public void updateSystem() {
        MySQLLink dbLink = new MySQLLink("travellertest");
        World.setDbLink(dbLink);
        World.initialiseTable(true);
        World world = new World("Test System", 1, 1, "Test Sector");
        int new_id = world.write();

        world.name = "Updated System";
        world.location = new Hex(2, 2);
        world.write();

        World updated_world = new World(new_id);
        assert updated_world.name.equals("Updated System");
        assert updated_world.location.x == 2;
        assert updated_world.location.y == 2;
        assert updated_world.sector.equals("Test Sector");
    }

}
