package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.process.mortality.fishery.FisheryMapSet;
import ucar.ma2.InvalidRangeException;

/**
 * Class for testing some basic parameters (number of species, number of
 * longitudes, latitudes, time-steps, etc.)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestFishingMapSet {

    private static Configuration cfg;
    private FisheryMapSet mapSet;

    Species species;

    @BeforeAll
    public void prepareData() {
    
        HashMap<String, String> cmd = new HashMap<>();
        
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        String dirMaps = this.getClass().getClassLoader().getResource("osmose-eec/").getFile();
        
        // // Overwrite some parameters
        cmd.put("fisheries.movement.fishery.map10", "gear0");
        cmd.put("fisheries.movement.file.map10", dirMaps + "maps/10chinchard_1plus.csv");
        cmd.put("fisheries.movement.years.map10", "0,1,2,3,4,5");
        
        cmd.put("fisheries.movement.fishery.map20", "gear0");
        cmd.put("fisheries.movement.file.map20", dirMaps + "maps/10chinchard_1plus.csv");
        cmd.put("fisheries.movement.years.map20", "6,7,8,9,10");
        
        cmd.put("fisheries.movement.fishery.map30", "gear0");
        cmd.put("fisheries.movement.file.map30", dirMaps + "maps/7callio_juillet.csv");
        cmd.put("fisheries.movement.initialYear.map30", "11");
        
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }
        
        species = osmose.getConfiguration().getSpecies(9);
        
        mapSet = new FisheryMapSet("gear0", "fisheries.movement", "fishery", false);
        mapSet.init();

    } 
    
    @Test
    public void testMapNumbers() {
        assertEquals(3, mapSet.getNMap());       
    }
    
}
