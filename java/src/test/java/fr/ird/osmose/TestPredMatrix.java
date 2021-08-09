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
    private float x = 0.f;
    private float y = 0.f;
    private double abundance = 100;
    private float length = 20;
    private float weight = 20;
    private float tl = 2;
    
    private int nStepYears;
    
    @Test
    public void TestArrayDim() {
        
        int nPreys = accessMatrix.getNPrey();
        int nPred =  accessMatrix.getNPred();
        
        // testing the number of preds (i.e. columns)
        assertEquals(25, nPred);
        
        // testing the number of rows (i.e. rows)
        assertEquals(35, nPreys);
        
    }
    
    @Test
    public void TestPredIndex() {
        
        double age;
        Species species;
        School school;
        System.out.println("+++++++++++++++++++++ ");
        for (int i = 0; i < Osmose.getInstance().getConfiguration().getNSpecies(); i++) {
            System.out.println(this.getSpecies(i).getName());
        }
        
        // testing species 0=lesserSpottedDogfish;
        species = this.getSpecies(0);
        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(0, this.accessMatrix.getIndexPred(school));

        age = 0.45;
        school = this.getSchool(species, age);
        assertEquals(1, this.accessMatrix.getIndexPred(school));
        
        age = 0.46;
        school = this.getSchool(species, age);
        assertEquals(1, this.accessMatrix.getIndexPred(school));
        
        // testing for horseMackerel 18
        species = this.getSpecies(9);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(18, this.accessMatrix.getIndexPred(school));
        
        age = 1e6f;
        school = this.getSchool(species, age);
        assertEquals(18, this.accessMatrix.getIndexPred(school));

        // testing for species= (squids) 22
        species = this.getSpecies(13);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(22, this.accessMatrix.getIndexPred(school));
        
        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(23, this.accessMatrix.getIndexPred(school));
        
    }
    
    public School getSchool(Species species, double age) {
        int ageDt = (int) Math.round(age * this.nStepYears);
        School school = new School(species, x, y, abundance, length, weight, ageDt, tl, 0);
        return school;
    }
    
    public Species getSpecies(int index) {
        return Osmose.getInstance().getConfiguration().getSpecies(index); 
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
        AccessibilityManager access = new AccessibilityManager(0, "predation.accessibility", ".acc", (school -> (school.getAge())), false);
        access.init();
        
        accessMatrix = access.getMatrix().get(-1);
        
        this.nStepYears = cfg.getNStepYear();
        
    }
    
    
    
}
