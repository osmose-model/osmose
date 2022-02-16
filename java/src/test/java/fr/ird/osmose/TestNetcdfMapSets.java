package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.util.MapSet;
import ucar.ma2.InvalidRangeException;

/**
 * Class for testing some basic parameters (number of species, number of
 * longitudes, latitudes, time-steps, etc.)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestNetcdfMapSets {

    private static Configuration cfg;
    private MapSet mapSet;

    Species species;

    @BeforeAll
    public void prepareData() {
    
        HashMap<String, String> cmd = new HashMap<>();
        
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        
        // Overwrite some parameters
        cmd.put("netcdfmovements.initialAge.map29", "0");
        cmd.put("netcdfmovements.lastAge.map29", "1");
        cmd.put("netcdfmovements.file.map29", "maps/movements_horseMackerel_initialAge_0_lastAge_1.nc");
        cmd.put("netcdfmovements.species.map29", "horseMackerel");
        
        cmd.put("netcdfmovements.initialAge.map30", "1");
        cmd.put("netcdfmovements.lastAge.map30", "4");
        cmd.put("netcdfmovements.file.map30", "maps/movements_horseMackerel_initialAge_1_lastAge_4.nc");
        cmd.put("netcdfmovements.species.map30", "horseMackerel");
    
        cmd.put("netcdfmovements.initialAge.map31", "4");
        cmd.put("netcdfmovements.lastAge.map31", "16");
        cmd.put("netcdfmovements.file.map31", "maps/movements_horseMackerel_initialAge_4_lastAge_16.nc");
        cmd.put("netcdfmovements.species.map31", "horseMackerel");
        
        cmd.put("netcdfmovements.netcdf.enabled", "true");
        
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }
        
        species = osmose.getConfiguration().getSpecies(9);
        
        mapSet = new MapSet(species.getFileSpeciesIndex(), species.getSpeciesIndex(), "netcdfmovements", "map", false);
        try {
            mapSet.init();
        } catch (IOException | InvalidRangeException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Test
    public void testIndexMaps() { 
        assertEquals(3, mapSet.getNMap());   
    }
    
    /** Testing the shape of the index Matrix */
    @Test
    public void testIndexDimensions() {
        int[][] indexMaps = mapSet.getIndexMap();
        int lifeSpan = species.getLifespanDt();
        int nStep = cfg.getNStep();
        assertEquals(lifeSpan, indexMaps.length);
        for (int i = 0; i < lifeSpan; i++) {
            assertEquals(nStep, indexMaps[i].length);
        }
    }
    
    @Test
    public void testMatrixIndex() { 
        
        float initialAge[] = new float[] { 0, 1, 4, 4, 4 };
        float lastAge[] = new float[] { 1, 4, 16, 16, 16 };
        int dt = cfg.getNStepYear();
        int[][] steps = new int[][] {
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }, // 29
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 }, // 30
                { 12, 13, 14, 15, 16, 17 }, // 31
                { 4, 5, 6, 7, 8, 9, 10, 11 }, // 32
                { 0, 1, 2, 3, 18, 19, 20, 21, 22, 23 } }; // 33

        // expected maps, taken into account the duplicates
        int[] indexMaps = new int[] { 0, 1, 2, 3, 1 };
        
        int nYears = cfg.getNYears();
        int lifeSpanDt = species.getLifespanDt();
        int[][] expected = new int[lifeSpanDt][cfg.getNStep()];
                
        for (int i = 0; i < 5; i++) {
            int ageMin = (int) Math.round(initialAge[i] * dt);
            int ageMax = (int) Math.round(lastAge[i] * dt);
            ageMax = Math.min(ageMax, lifeSpanDt - 1);
            for (int a = ageMin; a <= ageMax; a++) {
                for (int y = 0; y < nYears; y++) {
                    for (int s : steps[i]) {
                        expected[a][y * dt + s] = indexMaps[i];
                    }
                }
            }
        }
        
        assertArrayEquals(expected, mapSet.getIndexMap());
            
    }
    
    
    
}
