package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.util.AccessibilityManager;
import fr.ird.osmose.util.Matrix;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestPredMatrix {
    
    private Configuration cfg;
    
    private Matrix accessMatrix;
    
    @Test
    public void Test1() {
        
        int nPreys = accessMatrix.getNPrey();
        int nPred =  accessMatrix.getNPred();
        
        // testing the number of preds (i.e. columns)
        assertEquals(25, nPred);
        
        // testing the number of rows (i.e. rows)
        assertEquals(35, nPreys);
        
    }
    
    
    @BeforeAll
    public void prepareData() throws Exception {
        
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String dirIn = System.getenv("OSMOSE_TEST_DIR");
        String fileIn = System.getenv("OSMOSE_TEST_FILE");
        String configurationFile = new File(dirIn, fileIn).getAbsolutePath();
        
        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        
        // Test the standard configuration
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();
        AccessibilityManager access = new AccessibilityManager(0, "predation.accessibility", ".acc", (school -> (0)), false);
        access.init();
        
        accessMatrix = access.getMatrix().get(-1);
        
    }
    
    
    
}
