package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
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
public class TestMapSets {
    
    private static Configuration cfg;
    private MapSet mapSet;
    
    Species species;
    
    @BeforeAll
    public void prepareData() {
    
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        osmose.readConfiguration(configurationFile);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }
        
        species = osmose.getConfiguration().getSpecies(9);
        
        mapSet = new MapSet(species.getFileSpeciesIndex(), species.getSpeciesIndex(), "movement");
        try {
            mapSet.init();
        } catch (IOException | InvalidRangeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }   
    }
    
    /** Testing the number of maps */
    @Test
    public void testMapNumber() {
        assertEquals(5, mapSet.getNMap());
    }
    
    /** Testing the shape of the index Matrix */ 
    @Test
    public void testIndexDimensions() {
        int[][] indexMaps = mapSet.getIndexMap();
        int lifeSpan = species.getLifespanDt();
        int nStep = cfg.getNStep();
        assertEquals(lifeSpan, indexMaps.length);
        for(int i = 0; i<lifeSpan; i++) { 
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
    
    /** Testing the filenames */
    @Test
    public void testFileNames() { 
    
        String files[] = new String[] { "maps/10chinchard_0.csv", "maps/10chinchard_1plus.csv", null,
                "maps/10chinchard_spawning.csv", "maps/10chinchard_1plus.csv" };
        String expected[] = new String[5];
        for (int i = 0; i < 5; i++) {
            if (files[i] == null) {
                expected[i] = null;
            } else {
                expected[i] = this.getClass().getClassLoader().getResource("osmose-eec/" + files[i]).getFile();
            }
        }
                
        assertArrayEquals(expected, mapSet.getFileNames());
    }
    
    
    
    
    
    
    
}
