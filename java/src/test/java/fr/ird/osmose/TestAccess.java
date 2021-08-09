package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.util.AccessibilityManager;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    
    @Test
    public void testLenMaps() {
        assertEquals(1, access.getNMatrix());
    }
    
    @Test
    public void testIndex() {
        assertEquals(-1, access.getMatrixIndex(0, 0));
        assertEquals(-1, access.getMatrixIndex(119, 23));
    }
    
    @Test
    public void testIndex2() {
        assertEquals(69, access2.getMatrixIndex(0, 0));
        assertEquals(80, access2.getMatrixIndex(119, 23));
    }
    
    @Test
    public void testLenMaps2() {
        assertEquals(2, access2.getNMatrix());
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
        access = new AccessibilityManager(0, "predation.accessibility", ".acc", (school -> (0)));
        access.init();
        
        // Test another configuration in two .acc files are defined
        
        // First, the original file is copied twice, in two different places (access1 and access2)
        File fileName = new File(cfg.getFile("predation.accessibility.file"));
        String strTmp = System.getProperty("java.io.tmpdir");
        File filename1 = new File(strTmp, "access1.csv");
        File filename2 = new File(strTmp, "access2.csv");
        Files.copy(fileName.toPath(), filename1.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(fileName.toPath(), filename2.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // Then, the original settings are overwritten.
        cmd.put("predation.accessibility.file", "null");
        cmd.put("predation.accessibility.file.acc69", filename1.getPath());
        cmd.put("predation.accessibility.initialYear.acc69", "0");
        cmd.put("predation.accessibility.lastYear.acc69", "60");
        cmd.put("predation.accessibility.file.acc80", filename2.getPath());
        cmd.put("predation.accessibility.initialYear.acc80", "61");
        cmd.put("predation.accessibility.lastYear.acc80", "119");
        
        osmose.readConfiguration(configurationFile, cmd);
        osmose.getConfiguration().init();
        
        access2 = new AccessibilityManager(0, "predation.accessibility", "acc", (school -> (0)));
        access2.init();
        
    }
    
}
