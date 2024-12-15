package traveller;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static traveller.Economy.endOfYear;

public class EmpireReports {

    @Test
    public void empireNeighbours() {
        List<World> allWorlds = World.getAllWorlds();
        //     endOfYear(25, allWorlds);


        for (World w : allWorlds) {
            int empireId = w.getEmpire();
            if (empireId != w.id) {
                continue; // subject of empire
            }
            Empire empire = new Empire(empireId);
            if (w.techLevel < 9) {
                continue; // not space-faring
            }
            System.out.println(empire.fullDetails());
            empire.getWorlds().forEach(world -> {
                System.out.println(world.fullDescription());
            });
        }
    }
}
