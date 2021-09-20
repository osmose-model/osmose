package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import fr.ird.osmose.resource.Resource;
import fr.ird.osmose.resource.ResourceSpecies;
import fr.ird.osmose.util.AccessibilityManager;
import fr.ird.osmose.util.Matrix;

@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestPredMatrix {

    private Configuration cfg;
    private Matrix accessMatrix;
    private Matrix accessMatrix2;
    private float x = 0.f;
    private float y = 0.f;
    private double abundance = 100;
    private float length = 20;
    private float weight = 20;
    private float tl = 2;
    private ArrayList<Integer> predIndexSorted = new ArrayList<>();
    private ArrayList<Integer> predIndex = new ArrayList<>();
    private ArrayList<Integer> preyIndexSorted = new ArrayList<>();
    private ArrayList<Integer> preyIndex = new ArrayList<>();

    private int nStepYears;

    /** Test of the predation dimension array **/
    @Test
    @Order(1)
    public void TestArrayDim() {

        int nPreys = accessMatrix.getNPrey();
        int nPred = accessMatrix.getNPred();

        // testing the number of preds (i.e. columns)
        assertEquals(25, nPred);

        // testing the number of rows (i.e. rows)
        assertEquals(35, nPreys);

    }

    /** Test of the predator (column) index on unsorted array */
    @Test
    @Order(2)
    public void TestPredIndex() {

        double age;
        Species species;
        School school;

        // testing species 0=lesserSpottedDogfish;
        species = this.getSpecies(0);
        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(0, this.accessMatrix.getIndexPred(school));
        predIndex.add(0);

        age = 0.45;
        school = this.getSchool(species, age);
        assertEquals(1, this.accessMatrix.getIndexPred(school));
        predIndex.add(1);

        age = 0.46;
        school = this.getSchool(species, age);
        assertEquals(1, this.accessMatrix.getIndexPred(school));
        predIndex.add(1);

        // testing for horseMackerel 18
        species = this.getSpecies(9);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(18, this.accessMatrix.getIndexPred(school));
        predIndex.add(18);

        age = 1e6f;
        school = this.getSchool(species, age);
        assertEquals(18, this.accessMatrix.getIndexPred(school));
        predIndex.add(18);

        // testing for species= (squids) 22
        species = this.getSpecies(13);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(22, this.accessMatrix.getIndexPred(school));
        predIndex.add(22);

        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(23, this.accessMatrix.getIndexPred(school));
        predIndex.add(23);

    }

    /** Test on prey (row) for unsorted matrix **/
    @Test
    @Order(3)
    public void TestPreyIndex() {

        System.out.println(":::::::");
        double age;
        Species species;
        School school;

        // testing species 0=lesserSpottedDogfish;
        species = this.getSpecies(0);
        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(0, this.accessMatrix.getIndexPrey(school));
        preyIndex.add(0);

        age = 0.45;
        school = this.getSchool(species, age);
        assertEquals(1, this.accessMatrix.getIndexPrey(school));
        preyIndex.add(1);

        age = 0.46;
        school = this.getSchool(species, age);
        assertEquals(1, this.accessMatrix.getIndexPrey(school));
        preyIndex.add(1);

        // testing for horseMackerel 18
        species = this.getSpecies(9);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(18, this.accessMatrix.getIndexPrey(school));
        preyIndex.add(18);

        age = 1e6f;
        school = this.getSchool(species, age);
        assertEquals(18, this.accessMatrix.getIndexPrey(school));
        preyIndex.add(18);

        // testing for species= (squids) 22
        species = this.getSpecies(13);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(22, this.accessMatrix.getIndexPrey(school));
        preyIndex.add(22);

        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(23, this.accessMatrix.getIndexPrey(school));
        preyIndex.add(23);

    }

    @Test
    @Order(4)
    public void TestRscPreyIndex() {
        System.out.println("RSC");
        ResourceSpecies rscSpecies;
        Resource resource;
        Cell cell = Osmose.getInstance().getConfiguration().getGrid().getCell(0);

        for (int i = 0; i < 10; i++) {
            rscSpecies = this.getRscSpecies(i);
            resource = new Resource(rscSpecies, cell);
            assertEquals(24 + i, this.accessMatrix.getIndexPrey(resource));
            preyIndex.add(24 + i);
        }

    }

    public School getSchool(Species species, double age) {
        int ageDt = (int) Math.round(age * this.nStepYears);
        School school = new School(species, x, y, abundance, length, weight, ageDt, tl, 0);
        return school;
    }

    public Species getSpecies(int index) {
        return Osmose.getInstance().getConfiguration().getSpecies(index);
    }

    public ResourceSpecies getRscSpecies(int index) {
        return Osmose.getInstance().getConfiguration().getResourceSpecies(index);
    }

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
        AccessibilityManager access = new AccessibilityManager(0, "predation.accessibility", ".acc",
                (school -> (school.getAge())), false);
        access.init();
        accessMatrix = access.getMatrix().get(-1);

        AccessibilityManager access2 = new AccessibilityManager(0, "predation.accessibility", ".acc",
                (school -> (school.getAge())), true);
        access2.init();
        accessMatrix2 = access2.getMatrix().get(-1);

        this.nStepYears = cfg.getNStepYear();

    }

    /** Test of the predator (column) index on unsorted array */
    @Test
    @Order(5)
    public void TestPredIndexSorted() {

        double age;
        Species species;
        School school;

        // testing species 0=lesserSpottedDogfish;
        species = this.getSpecies(0);
        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(7, this.accessMatrix2.getIndexPred(school));
        predIndexSorted.add(7);

        age = 0.45;
        school = this.getSchool(species, age);
        assertEquals(8, this.accessMatrix2.getIndexPred(school));
        predIndexSorted.add(8);

        age = 0.46;
        school = this.getSchool(species, age);
        assertEquals(8, this.accessMatrix2.getIndexPred(school));
        predIndexSorted.add(8);

        // testing for horseMackerel 18
        species = this.getSpecies(9);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(6, this.accessMatrix2.getIndexPred(school));
        predIndexSorted.add(9);

        age = 1e6f;
        school = this.getSchool(species, age);
        assertEquals(6, this.accessMatrix2.getIndexPred(school));
        predIndexSorted.add(6);

        // testing for species= (squids) 22
        species = this.getSpecies(13);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(21, this.accessMatrix2.getIndexPred(school));
        predIndexSorted.add(21);

        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(22, this.accessMatrix2.getIndexPred(school));
        predIndexSorted.add(22);

    }

    /** Test on prey (row) for unsorted matrix **/
    @Test
    @Order(6)
    public void TestPreyIndexSorted() {

        double age;
        Species species;
        School school;

        // testing species 0=lesserSpottedDogfish;
        species = this.getSpecies(0);
        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(10, this.accessMatrix2.getIndexPrey(school));
        preyIndexSorted.add(10);

        age = 0.45;
        school = this.getSchool(species, age);
        assertEquals(11, this.accessMatrix2.getIndexPrey(school));
        preyIndexSorted.add(11);

        age = 0.46;
        school = this.getSchool(species, age);
        assertEquals(11, this.accessMatrix2.getIndexPrey(school));
        preyIndexSorted.add(11);

        // testing for horseMackerel 18
        species = this.getSpecies(9);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(8, this.accessMatrix2.getIndexPrey(school));
        preyIndexSorted.add(8);

        age = 1e6f;
        school = this.getSchool(species, age);
        assertEquals(8, this.accessMatrix2.getIndexPrey(school));
        preyIndexSorted.add(8);

        // testing for species= (squids) 22
        species = this.getSpecies(13);
        age = 0.1;
        school = this.getSchool(species, age);
        assertEquals(29, this.accessMatrix2.getIndexPrey(school));
        preyIndexSorted.add(29);

        age = 0.2;
        school = this.getSchool(species, age);
        assertEquals(30, this.accessMatrix2.getIndexPrey(school));
        preyIndexSorted.add(30);

    }

    @Test
    @Order(7)
    public void TestRscPreyIndexSorted() {

        int[] expected = new int[] { 4, 3, 16, 15, 13, 32, 26, 14, 9, 31 };
        ResourceSpecies rscSpecies;
        Resource resource;
        Cell cell = Osmose.getInstance().getConfiguration().getGrid().getCell(0);

        for (int i = 0; i < 10; i++) {
            rscSpecies = this.getRscSpecies(i);
            resource = new Resource(rscSpecies, cell);
            assertEquals(expected[i], this.accessMatrix2.getIndexPrey(resource));
            preyIndexSorted.add(expected[i]);
        }
    }

    /**
     * Test some values of the accessibility matrix for both the sorted and unsorted
     * accessibility matrix
     */
    @AfterAll
    public void TestValues() {

        double[] expected = new double[] { 0.05, 0.0, 0.0, 0.0, 0.0, 0.05, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 0.0, 0.8, 0.8, 0.4, 0.4, 0.0, 0.4, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.0, 0.8,
                0.8, 0.4, 0.4, 0.0, 0.4, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.0, 0.4, 0.4, 0.8, 0.8, 0.0,
                0.8, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.4, 0.4, 0.8, 0.8, 0.0, 0.8, 1.0, 1.0, 1.0,
                1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.05, 0.0, 0.0, 0.0, 0.0, 0.05, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 0.0, 0.4, 0.4, 0.8, 0.8, 0.0, 0.8, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
                0.0 };

        int cpt = 0;
        for (int iPred : this.predIndex) {
            for (int iPrey : this.preyIndex) {
                assertEquals(expected[cpt], this.accessMatrix.getValue(iPrey, iPred));
                cpt++;
            }
        }

        cpt = 0;
        for (int iPred : this.predIndexSorted) {
            for (int iPrey : this.preyIndexSorted) {
                assertEquals(expected[cpt], this.accessMatrix2.getValue(iPrey, iPred));
                cpt++;
            }
        }

    }

}
