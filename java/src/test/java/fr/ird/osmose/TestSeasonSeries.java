package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import fr.ird.osmose.util.timeseries.SeasonTimeSeries;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSeasonSeries {
    
    private Configuration cfg;
    private SeasonTimeSeries ts1;
    private SeasonTimeSeries ts2;
    private SeasonTimeSeries ts3;
        
    @BeforeAll
    public void prepareData() throws Exception {

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        
        // first test: null values
        cmd.put("first.test.season.sp0", "null");
        
        // second test: providing seasonal values
        cmd.put("second.test.season.sp0", "0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,1.1,1.2,1.3,1.4,1.5,1.6,1.7,1.8,1.9,2.0,2.1,2.2,2.3,2.4");
        
        String filename = this.getClass().getClassLoader().getResource("osmose-eec/reproduction/reproduction-seasonality-sp0.csv").getFile();
        cmd.put("third.test.season.file.sp0", filename);

        // Test the standard configuration
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();
        
        ts1 = new SeasonTimeSeries("first.test.season", "sp0");
        ts1.init();
        
        ts2 = new SeasonTimeSeries("second.test.season", "sp0");
        ts2.init();
        
        ts3 = new SeasonTimeSeries("third.test.season", "sp0");
        ts3.init();

    }
    
    @Test
    public void testSeries1() { 
        double[] actual = ts1.getValues();   
        double[] expected = new double[cfg.getNStep()];
        for(int i = 0; i < expected.length; i++) {
            expected[i] = 1.0;
            //expected[i] = 1.0 / cfg.getNStepYear();
        }
        
        assertArrayEquals(expected, actual);
        
    }
    
    @Test
    public void testSeries2() {
        double[] actual = ts2.getValues();
        double[] expected = new double[cfg.getNStep()];
        double[] temp = new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6,
                1.7, 1.8, 1.9, 2.0, 2.1, 2.2, 2.3, 2.4 };
        for (int i = 0; i < expected.length; i++) {
            expected[i] = temp[i % cfg.getNStepYear()];
        }

        assertArrayEquals(expected, actual);

    }
    
    @Test
    public void testSeries3() {
        double[] actual = ts3.getValues();
        double[] expected = new double[cfg.getNStep()];
        double[] temp = new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 0.0417, 0.0417, 0.1875, 0.1875, 0.1875, 0.1875, 0.0417,
                0.0417, 0.0417, 0.0417, 0, 0, 0, 0, 0, 0 };
 
        for (int i = 0; i < expected.length; i++) {
            expected[i] = temp[i % cfg.getNStepYear()];
        }

        assertArrayEquals(expected, actual);

    }
    
    
    
    
}
