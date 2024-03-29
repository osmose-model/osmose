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
        String dirMaps = this.getClass().getClassLoader().getResource("osmose-eec/").getFile();
        
        // Overwrite some parameters
        cmd.put("netcdfmovements.initialAge.map29", "0");
        cmd.put("netcdfmovements.lastAge.map29", "1");
        cmd.put("netcdfmovements.file.map29", dirMaps + "maps/movements_horseMackerel_initialAge_0_lastAge_1.nc");
        cmd.put("netcdfmovements.species.map29", "horseMackerel");
        cmd.put("netcdfmovements.variable.map29", "movements");
        cmd.put("netcdfmovements.nsteps.year.map29", "24");
        
        cmd.put("netcdfmovements.initialAge.map30", "1");
        cmd.put("netcdfmovements.lastAge.map30", "4");
        cmd.put("netcdfmovements.file.map30", dirMaps + "maps/movements_horseMackerel_initialAge_1_lastAge_4.nc");
        cmd.put("netcdfmovements.species.map30", "horseMackerel");
        cmd.put("netcdfmovements.variable.map30", "movements");
        cmd.put("netcdfmovements.nsteps.year.map30", "24");
        
        cmd.put("netcdfmovements.initialAge.map31", "4");
        cmd.put("netcdfmovements.lastAge.map31", "16");
        cmd.put("netcdfmovements.file.map31", dirMaps + "maps/movements_horseMackerel_initialAge_4_lastAge_16.nc");
        cmd.put("netcdfmovements.species.map31", "horseMackerel");
        cmd.put("netcdfmovements.variable.map31", "movements");
        cmd.put("netcdfmovements.nsteps.year.map31", "24");
        
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
        assertEquals(3 * 24, mapSet.getNMap());   
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

        float initialAge[] = new float[] { 0, 1, 4 };
        float lastAge[] = new float[] { 1, 4, 16 };

        int nYears = cfg.getNYears();
        int lifeSpanDt = species.getLifespanDt();
        
        
        int[][] expected = new int[lifeSpanDt][cfg.getNStep()];
        // for young ages, expected map index should be 0, 23
        // for intermediate aged, expected map index should be 24, 47
        // for old ages, expected map index should be 48, 71
        
        int dt = cfg.getNStepYear();
        for (int i = 0; i < 3; i++) {
            int ageMin = (int) Math.round(initialAge[i] * dt);
            int ageMax = (int) Math.round(lastAge[i] * dt);
            ageMax = Math.min(ageMax, lifeSpanDt - 1);
            for (int a = ageMin; a <= ageMax; a++) {
                for (int y = 0; y < nYears; y++) {
                    for (int s = 0; s < 24; s++) {
                        expected[a][y * dt + s] = i * 24 + s;
                    }
                }
            }
        }
        assertArrayEquals(expected, mapSet.getIndexMap());
           
    }
    
    
    
}
