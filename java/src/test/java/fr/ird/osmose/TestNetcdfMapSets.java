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
        cmd.put("netcdfmovements.lastAge.map29", "1");
        cmd.put("netcdfmovements.initialAge.map29", "0");
        cmd.put("netcdfmovements.file.map29", "maps/10chinchard_0.nc");
        cmd.put("netcdfmovements.steps.map29", "0;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23");
        cmd.put("netcdfmovements.species.map29", "horseMackerel");
        cmd.put("netcdfmovements.lastAge.map30", "4");
        cmd.put("netcdfmovements.initialAge.map30", "1");
        cmd.put("netcdfmovements.file.map30", "maps/10chinchard_1plus.nc");
        cmd.put("netcdfmovements.steps.map30", "0;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23");
        cmd.put("netcdfmovements.species.map30", "horseMackerel");
        cmd.put("netcdfmovements.lastAge.map31", "16");
        cmd.put("netcdfmovements.initialAge.map31", "4");
        cmd.put("netcdfmovements.file.map31", "null");
        cmd.put("netcdfmovements.steps.map31", "12;13;14;15;16;17");
        cmd.put("netcdfmovements.species.map31", "horseMackerel");
        cmd.put("netcdfmovements.lastAge.map32", "16");
        cmd.put("netcdfmovements.initialAge.map32", "4");
        cmd.put("netcdfmovements.file.map32", "maps/10chinchard_spawning.nc");
        cmd.put("netcdfmovements.steps.map32", "4;5;6;7;8;9;10;11;");
        cmd.put("netcdfmovements.species.map32", "horseMackerel");
        cmd.put("netcdfmovements.lastAge.map33", "16");
        cmd.put("netcdfmovements.initialAge.map33", "4");
        cmd.put("netcdfmovements.file.map33", "maps/10chinchard_1plus.nc");
        cmd.put("netcdfmovements.steps.map33", "0;1;2;3;18;19;20;21;22;23;");
        cmd.put("netcdfmovements.species.map33", "horseMackerel");
        cmd.put("netcdfmovements.species.map33", "horseMackerel");
        cmd.put("netcdfmovements.netcdf.enabled", "true");
        
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }
        
        species = osmose.getConfiguration().getSpecies(9);
        
        mapSet = new MapSet(species.getFileSpeciesIndex(), species.getSpeciesIndex(), "netcdfmovements");
        try {
            mapSet.init();
        } catch (IOException | InvalidRangeException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Test
    public void testIndexMaps() { 
        assertEquals(5, mapSet.getNMap());   
    }
    
}
