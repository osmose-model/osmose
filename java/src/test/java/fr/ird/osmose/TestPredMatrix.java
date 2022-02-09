package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

/** Class that tests the reading of accessibility matrix. */
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
    
    interface Index {
        int getIndex(School school);
    }
        
    String[] testNames = new String[] { "lesserSpottedDogfish", "lesserSpottedDogfish", "redMullet", "redMullet",
    "pouting", "pouting", "whiting", "whiting", "poorCod", "poorCod", "cod", "cod", "dragonet", "dragonet",
    "sole", "sole", "plaice", "plaice", "horseMackerel", "horseMackerel", "mackerel", "mackerel", "herring",
    "herring", "sardine", "sardine", "squids", "squids"};
    
    float[] testAges = new float[] { 0.3f, 0.46f, // lesser
            0.2f, 0.3f, // rm
            0.2f, 0.3f, // pouting
            0.2f, 0.3f, // whitting,
            0.2f, 0.3f, // poorcod,
            0.35f, 0.45f, // cod,
            0.2f, 0.3f, // drago,
            0.1f, 0.2f, // sole,
            0.2f, 0.3f, // plaice,
            0.3f, 0.5f, // hmack,
            0.3f, 0.5f, // mack,
            0.3f, 0.5f, // herring
            0.3f, 0.5f, // sard
            0.1f, 0.2f // squid
    };

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
    
    @Test
    public void TestPreyNames() {
        String[] expected = new String[] { "lesserSpottedDogfish", "lesserSpottedDogfish", "redMullet", "redMullet",
                "pouting", "pouting", "whiting", "whiting", "poorCod", "poorCod", "cod", "cod", "dragonet", "dragonet",
                "sole", "sole", "plaice", "plaice", "horseMackerel", "mackerel", "herring", "sardine", "squids",
                "squids", "Dinoflagellates", "Diatoms", "Microzoo", "Mesozoo", "Macrozoo", "VSBVerySmallBenthos",
                "SmallBenthos", "MediumBenthos", "LargeBenthos", "VLBVeryLargeBenthos", "backgroundSpecies" };
        assertArrayEquals(expected, this.accessMatrix.getPreyNames());
    }

    @Test
    public void TestPredNames() {
        String[] expected = new String[] { "lesserSpottedDogfish", "lesserSpottedDogfish", "redMullet", "redMullet",
                "pouting", "pouting", "whiting", "whiting", "poorCod", "poorCod", "cod", "cod", "dragonet", "dragonet",
                "sole", "sole", "plaice", "plaice", "horseMackerel", "mackerel", "herring", "sardine", "squids",
                "squids", "backgroundSpecies" };
        assertArrayEquals(expected, this.accessMatrix.getPredNames());
    }
    
    @Test
    public void TestPredClass() {
        float[] expected = new float[] { 0.45f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.25f,
                Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.4f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.15f,
                Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE, 0.125f, Float.MAX_VALUE, Float.MAX_VALUE };
        assertArrayEquals(expected, this.accessMatrix.getPredClasses());
    }

    @Test
    public void TestPreyClasses() {
        float[] expected = new float[] { 0.45f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.25f,
                Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.4f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.15f,
                Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE, 0.125f, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE };
        assertArrayEquals(expected, this.accessMatrix.getPreyClasses());
    }
    
    
    @Test
    public void TestSortedPreyNames() {
        String[] expected = new String[] { "backgroundSpecies", "cod", "cod", "Diatoms", "Dinoflagellates", "dragonet",
                "dragonet", "herring", "horseMackerel", "LargeBenthos", "lesserSpottedDogfish", "lesserSpottedDogfish",
                "mackerel", "Macrozoo", "MediumBenthos", "Mesozoo", "Microzoo", "plaice", "plaice", "poorCod",
                "poorCod", "pouting", "pouting", "redMullet", "redMullet", "sardine", "SmallBenthos", "sole", "sole",
                "squids", "squids", "VLBVeryLargeBenthos", "VSBVerySmallBenthos", "whiting", "whiting" };
        assertArrayEquals(expected, this.accessMatrix2.getPreyNames());
    }

    @Test
    public void TestSortedPredNames() {
        String[] expected = new String[] { "backgroundSpecies", "cod", "cod", "dragonet", "dragonet", "herring",
                "horseMackerel", "lesserSpottedDogfish", "lesserSpottedDogfish", "mackerel", "plaice", "plaice",
                "poorCod", "poorCod", "pouting", "pouting", "redMullet", "redMullet", "sardine", "sole", "sole",
                "squids", "squids", "whiting", "whiting" };
        assertArrayEquals(expected, this.accessMatrix2.getPredNames());
    }
    
    @Test
    public void TestSortedPredClass() {
        float[] expected = new float[] { Float.MAX_VALUE, 0.4f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE, 0.45f, Float.MAX_VALUE, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE,
                0.25f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, Float.MAX_VALUE, 0.15f,
                Float.MAX_VALUE, 0.125f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE };
        assertArrayEquals(expected, this.accessMatrix2.getPredClasses());
    }

    @Test
    public void TestSortedPreyClasses() {
        float[] expected = new float[] { Float.MAX_VALUE, 0.4f, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                0.25f, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 0.45f, Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 0.25f,
                Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE, 0.25f, Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE, 0.15f, Float.MAX_VALUE, 0.125f, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE, 0.25f, Float.MAX_VALUE };
        assertArrayEquals(expected, this.accessMatrix2.getPreyClasses());
    }
    
    /** Method that generates the expected index of the accessible matrix for the species and
     * age classes defined in the above. 
     */
    private int[] getIndexes(Index indexer) {
        
        List<Integer> values = new ArrayList<>();
        for(int i = 0; i < testNames.length; i++) {
            String speciesName = testNames[i];
            float age = testAges[i];
            Species species = this.getSpecies(speciesName);
            School school = this.getSchool(species, age);
            int index = indexer.getIndex(school);
            values.add(index);
        }
        
        int[] actual = values.stream().mapToInt(i -> i).toArray();
        return actual;
        
    }
    
    /** Test of the predator (column) index on unsorted array */
    @Test
    @Order(2)
    public void TestPredIndex() {
                
        int[] expected = new int[] { 0, 1, // less
                2, 3, // rm
                4, 5, // pout
                6, 7, // whit
                8, 9, // poorcod
                10, 11, // cod
                12, 13, // drago
                14, 15, // sole
                16, 17, // plaice
                18, 18, // hmack
                19, 19, // mack
                20, 20, // hering
                21, 21, // sard
                22, 23 };
            
        int[] actual = this.getIndexes(sch-> this.accessMatrix.getIndexPred(sch));
        
        assertArrayEquals(expected, actual);
            
    }
    
    /** Test of the predator (column) index on unsorted array */
    @Test
    public void SortedTestPredIndex() {

        int[] expected = new int[] { 7, 8, // less
                16, 17, // rm
                14, 15, // pout
                23, 24, // whit
                12, 13, // poorcod
                1, 2, // cod
                3, 4, // drago
                19, 20, // sole
                10, 11, // plaice
                6, 6, // hmack
                9, 9, // mack
                5, 5, // hering
                18, 18, // sard
                21, 22 };  // squids

        int[] actual = this.getIndexes(sch -> this.accessMatrix2.getIndexPred(sch));

        assertArrayEquals(expected, actual);

    }
    
    /** Test of the predator (column) index on unsorted array */
    @Test
    public void SortedTestPreYIndex() {

        int[] expected = new int[] { 10, 11, // less
                23, 24, // rm
                21, 22, // pout
                33, 34, // whit
                19, 20, // poorcod
                1, 2, // cod
                5, 6, // drago
                27, 28, // sole
                17, 18, // plaice
                8, 8, // hmack
                12, 12, // mack
                7, 7, // hering
                25, 25, // sard
                29, 30 };

        int[] actual = this.getIndexes(sch -> this.accessMatrix2.getIndexPrey(sch));

        assertArrayEquals(expected, actual);

    }
    

    /** Test on prey (row) for unsorted matrix **/
    @Test
    @Order(3)
    public void TestPreyIndex() {
        
        int[] expected = new int[] { 0, 1, // less
                2, 3, // rm
                4, 5, // pout
                6, 7, // whit
                8, 9, // poorcod
                10, 11, // cod
                12, 13, // drago
                14, 15, // sole
                16, 17, // plaice
                18, 18, // hmack
                19, 19, // mack
                20, 20, // hering
                21, 21, // sard
                22, 23 };
        
        int[] actual = this.getIndexes(sch -> this.accessMatrix.getIndexPrey(sch));
        
        assertArrayEquals(expected, actual);

    }

    @Test
    @Order(4)
    public void TestRscPreyIndex() {
        ResourceSpecies rscSpecies;
        Resource resource;
        Cell cell = Osmose.getInstance().getConfiguration().getGrid().getCell(0);
        for (int i = 0; i < Osmose.getInstance().getConfiguration().getNRscSpecies(); i++) {
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
    
    public Species getSpecies(String name) {
        return Osmose.getInstance().getConfiguration().getSpecies(name);
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
        
        // First, we read the accessibility matrix when the matrixes is not sorted.
        // i.e. we read the file as it is.
        AccessibilityManager access = new AccessibilityManager(0,"predation.accessibility",".acc",
                (school -> (school.getAge())), false);
        access.init();
        accessMatrix = access.getMatrix().get(-1);

        // Here, we read the file and we sort the columns and rows by first comparing names and then class.
        AccessibilityManager access2 = new AccessibilityManager(0,"predation.accessibility",".acc",
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
