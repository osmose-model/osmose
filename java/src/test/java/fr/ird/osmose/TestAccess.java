package fr.ird.osmose;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import fr.ird.osmose.util.AccessibilityManager;

public class TestAccess {
    
    private Configuration cfg;
    private AccessibilityManager access;
    private AccessibilityManager access2;
    
    @Test
    public void testIndexNyears() {
        assertEquals(120, access.getMatrixIndex().length);
    }
    
    @Test
    public void testIndexSteps() {
        assertEquals(24, access.getMatrixIndex()[0].length);
    }
    
    @Before
    public void prepareData() throws Exception {

        Osmose osmose = Osmose.getInstance();
        String dirIn = System.getenv("OSMOSE_TEST_DIR");
        String fileIn = System.getenv("OSMOSE_TEST_FILE");
        String configurationFile = new File(dirIn, fileIn).getAbsolutePath();
        
        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();
        
        access = new AccessibilityManager(0, "predation.accessibility", ".acc", (school -> (0)));
        access.init();
        
    }
    
}
