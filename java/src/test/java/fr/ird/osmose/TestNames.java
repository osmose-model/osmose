
package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ucar.ma2.InvalidRangeException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestNames {


    private static Configuration cfg;
    Species spec1, spec2, spec3, spec4;

    @BeforeAll
    public void prepareData() {

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();

        cmd.put("species.name.sp0", "lesser_Spotted_Dogfish");
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }

        spec1 = cfg.getSpecies(0);

        cmd.put("species.name.sp0", "lesser-Spotted-Dogfish");
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }

        spec2 = cfg.getSpecies(0);

        cmd.put("species.name.sp0", "lesser_Spotted-Dogfish");
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }

        spec3 = cfg.getSpecies(0);

        cmd.put("species.name.sp0", "lesser-Spotted_Dogfish-2");
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }

        spec4 = cfg.getSpecies(0);

    }


    @Test
    public void testSpeciesNames() {

        String expected = "lesserSpottedDogfish";
        assertEquals(expected, spec1.getName());
        assertEquals(expected, spec2.getName());
        assertEquals(expected, spec3.getName());

        expected = "lesserSpottedDogfish2";
        assertEquals(expected, spec4.getName());

    }

}
