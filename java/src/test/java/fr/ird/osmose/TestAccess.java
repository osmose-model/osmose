package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

/** Test the management of accessibility matrix, i.e.
 * whether time-varying matrixes are well defined. This
 * is why the classVarGetter is defined as 0. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestAccess {
    
    private Configuration cfg;
    private AccessibilityManager access;
    private AccessibilityManager access2;
    private AccessibilityManager access3;
    
    /** Check that the matrix index contains 120 years. */
    @Test
    public void testIndexNyears() {
        assertEquals(120, access.getMatrixIndex().length);
    }
    
    /** Check that the access matrix contains 24 time-steps. */
    @Test
    public void testIndexSteps() {
        assertEquals(24, access.getMatrixIndex()[0].length);
    }
    
    /** Check that in standard configuration, only 1 map is defined. */
    @Test
    public void testLenMaps() {
        assertEquals(1, access.getNMatrix());
    }
    
    /** Testing the indexing of accessible matrix. */
    @Test
    public void testIndex() {

        Osmose osmose = Osmose.getInstance();
        int nYears = osmose.getConfiguration().getNYears();
        int nSteps = osmose.getConfiguration().getNStepYear();
        int[][] expected = new int[nYears][nSteps];
        for (int i = 0; i < nYears; i++) {
            for (int j = 0; j < nSteps; j++) {
                expected[i][j] = -1;
            }
        }
        assertArrayEquals(expected, access.getMatrixIndex());
    }
    
    @Test
    public void testIndex2() {

        Osmose osmose = Osmose.getInstance();
        int nYears = osmose.getConfiguration().getNYears();
        int nSteps = osmose.getConfiguration().getNStepYear();
        int[][] expected = new int[nYears][nSteps];
        for (int i = 0; i <= 60; i++) {
            for (int j = 0; j < 12; j++) {
                expected[i][j] = 69;
            }
            for (int j = 12; j < nSteps; j++) {
                expected[i][j] = 50;
            }
        }

        for (int i = 61; i <= 119; i++) {
            for (int j = 0; j < nSteps; j++) {
                expected[i][j] = 80;
            }
            for (int j = 12; j < nSteps; j++) {
                expected[i][j] = 70;
            }
        }

        assertArrayEquals(expected, access2.getMatrixIndex());
    }
    
    @Test
    public void testIndex3() {

        Osmose osmose = Osmose.getInstance();
        int nYears = osmose.getConfiguration().getNYears();
        int nSteps = osmose.getConfiguration().getNStepYear();
        int[][] expected = new int[nYears][nSteps];
        for (int i = 0; i <= 60; i++) {
            for (int j = 0; j < 12; j++) {
                expected[i][j] = 69;
            }
            for (int j = 12; j < nSteps; j++) {
                expected[i][j] = 80;
            }
        }

        for (int i = 61; i <= 119; i++) {
            for (int j = 0; j < nSteps; j++) {
                expected[i][j] = 80;
            }
            for (int j = 12; j < nSteps; j++) {
                expected[i][j] = 69;
            }
        }

        assertArrayEquals(expected, access3.getMatrixIndex());
    }
    
    @Test
    public void testLenMaps2() {
        assertEquals(4, access2.getNMatrix());
    }
    
    
    @Test
    public void testLenMaps3() {
        assertEquals(2, access3.getNMatrix());
    }
    
    /** Prepare the input data for the test. */
    @BeforeAll
    public void prepareData() throws Exception {
        
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        
        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        
        // Test the standard configuration
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();
        access = new AccessibilityManager(0, "predation.accessibility", ".acc", (school -> (0)));
        access.init();
        
        // Test another configuration in four .acc files are defined
        // First, the original file is copied twice, in two different places (access1 and access2)
        File fileName = new File(cfg.getFile("predation.accessibility.file"));
        String strTmp = System.getProperty("java.io.tmpdir");
        File filename1 = new File(strTmp, "access1.csv");
        File filename2 = new File(strTmp, "access2.csv");
        File filename3 = new File(strTmp, "access3.csv");
        File filename4 = new File(strTmp, "access4.csv");
        Files.copy(fileName.toPath(), filename1.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(fileName.toPath(), filename2.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(fileName.toPath(), filename3.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(fileName.toPath(), filename4.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // Then, the original settings are overwritten, and a new Osmose configuration is defined.
        cmd.put("predation.accessibility.file", "null");
        cmd.put("predation.accessibility.file.acc69", filename1.getPath());
        cmd.put("predation.accessibility.initialYear.acc69", "0");
        cmd.put("predation.accessibility.lastYear.acc69", "60");
        cmd.put("predation.accessibility.steps.acc69", "0,1,2,3,4,5,6,7,8,9,10,11");
        
        cmd.put("predation.accessibility.file.acc80", filename2.getPath());
        cmd.put("predation.accessibility.initialYear.acc80", "61");
        cmd.put("predation.accessibility.lastYear.acc80", "119");
        cmd.put("predation.accessibility.steps.acc80", "0,1,2,3,4,5,6,7,8,9,10,11");
        
        cmd.put("predation.accessibility.file.acc50", filename3.getPath());
        cmd.put("predation.accessibility.initialYear.acc50", "0");
        cmd.put("predation.accessibility.lastYear.acc50", "60");
        cmd.put("predation.accessibility.steps.acc50", "12,13,14,15,16,17,18,19,20,21,22,23");
        
        cmd.put("predation.accessibility.file.acc70", filename4.getPath());
        cmd.put("predation.accessibility.initialYear.acc70", "61");
        cmd.put("predation.accessibility.lastYear.acc70", "119");
        cmd.put("predation.accessibility.steps.acc70", "12,13,14,15,16,17,18,19,20,21,22,23");
        
        osmose.readConfiguration(configurationFile, cmd);
        osmose.getConfiguration().init();
        
        access2 = new AccessibilityManager(0, "predation.accessibility", "acc", (school -> (0)));
        access2.init();
        
        // // Now we make make sure that the acc50 and acc70 point to the same files as acc69 and acc80
        cmd.put("predation.accessibility.file.acc70", filename1.getPath());
        cmd.put("predation.accessibility.file.acc50", filename2.getPath());
        osmose.readConfiguration(configurationFile, cmd);
        osmose.getConfiguration().init();
        
        access3 = new AccessibilityManager(0, "predation.accessibility", "acc", (school -> (0)));
        access3.init();
        
    }
    
}
