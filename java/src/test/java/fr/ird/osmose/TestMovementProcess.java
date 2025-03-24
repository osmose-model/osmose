package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.process.MovementProcess;
import fr.ird.osmose.process.movement.AbstractSpatialDistribution;
import fr.ird.osmose.process.movement.MapDistribution;

/**
 * Test the management of accessibility matrix, i.e. whether time-varying
 * matrixes are well defined. This is why the classVarGetter is defined as 0.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMovementProcess {

    private Configuration cfg;
    School school;
    Species species;
    MovementProcess movementProcess;
    AbstractSpatialDistribution spatialDistribution;

    /** Check that the matrix index contains 120 years. */
    @Test
    public void testSelectivity() {



    }

    /** Prepare the input data for the test. */
    @BeforeAll
    public void prepareData() throws Exception {

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv")
                .getFile();

        // Update the URL for Windows computers
        configurationFile = configurationFile.replaceAll("%20", " ");

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();

        cmd.put("movement.distribution.method.sp0", "map_fixed");
        cmd.put("movement.initialAge.map0", "0");
        cmd.put("movement.lastAge.map0", "2");
        cmd.put("movement.file.map0", "maps/test1.csv");
        cmd.put("movement.steps.map0", "0;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23");
        cmd.put("movement.species.map0", "lesserSpottedDogfish");

        cmd.put("movement.initialAge.map1", "2");
        cmd.put("movement.lastAge.map1", "11");
        cmd.put("movement.file.map1", "maps/test2.csv");
        cmd.put("movement.steps.map1", "0;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23");
        cmd.put("movement.species.map1", "lesserSpottedDogfish");

        // Test the standard configuration
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();

        spatialDistribution = new MapDistribution(0, 0,  0);
        spatialDistribution.init();

        Species species;
        species = cfg.getSpecies(0);
        school = new School(species, 10);
        for (int i = 0; i < 2 * 24 - 1; i++) {
            school.incrementAge();
        }

        spatialDistribution.move(school, 0);
        school.incrementAge();
        spatialDistribution.move(school, 1);

    }
}
