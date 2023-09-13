package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
public class TestNetcdfFishingMapSet {

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
        cmd.put("fisheries.movement.file.map10", dirMaps + "maps/movements_horseMackerel_initialAge_.*.nc");
        cmd.put("fisheries.movement.variable.map10", "movements");
        cmd.put("fisheries.movement.nsteps.year.map10", "24");
        cmd.put("fisheries.movement.netcdf.enabled", "true");
                
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
        assertEquals(24 * 3, mapSet.getNMap());       
    }
    
    @Test
    public void testIndexShape() {
        int nStep = cfg.getNStep();
        assertEquals(nStep, mapSet.getIndexMap().length);       
    }
    
    @Test
    public void testIndexValues() {

        int[] expected = new int[cfg.getNStep()];
        int s;
        
        for (s = 0; s < cfg.getNStep(); s++) {
            expected[s] = s % (24 * 3);
        }
      
        assertArrayEquals(expected, mapSet.getIndexMap());
        
    }
    
    
}
