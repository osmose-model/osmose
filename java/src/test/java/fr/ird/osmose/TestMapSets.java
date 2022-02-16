package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.util.MapSet;
import ucar.jpeg.jj2000.j2k.wavelet.Subband;
import ucar.ma2.InvalidRangeException;
/**
 * Class for testing some basic parameters (number of species, number of
 * longitudes, latitudes, time-steps, etc.)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMapSets {
    
    private static Configuration cfg;
    private MapSet mapSet;
    private String mapsDirectory;
    
    Species species;
    
    @BeforeAll
    public void prepareData() {
    
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        mapsDirectory = this.getClass().getClassLoader().getResource("osmose-eec").getFile();
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
    
    /** Testing on the values of the
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
