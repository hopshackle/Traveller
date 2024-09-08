package traveller;

import db.MySQLLink;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;

public class SystemTests {

    @BeforeAll
    public static void setup() {
        MySQLLink dbLink = new MySQLLink("travellertest");
        System.setDbLink(dbLink);
        System.initialiseTable(true);
    }

    @Test
    public void createNewSystem() {
        System system = new System("Test System", 1, 1, "Test Sector");
        int new_id = system.write();

        // now check this exists on the table
        System new_system = new System(new_id);
        assert new_system.name.equals("Test System");
        assert new_system.location.x == 1;
        assert new_system.location.y == 1;
        assert new_system.sector.equals("Test Sector");

        assert new_system.equals(system);
    }

    @Test
    public void updateSystem() {
        MySQLLink dbLink = new MySQLLink("travellertest");
        System.setDbLink(dbLink);
        System.initialiseTable(true);
        System system = new System("Test System", 1, 1, "Test Sector");
        int new_id = system.write();

        system.name = "Updated System";
        system.location = new Point(2, 2);
        system.write();

        System updated_system = new System(new_id);
        assert updated_system.name.equals("Updated System");
        assert updated_system.location.x == 2;
        assert updated_system.location.y == 2;
        assert updated_system.sector.equals("Test Sector");
    }

}
