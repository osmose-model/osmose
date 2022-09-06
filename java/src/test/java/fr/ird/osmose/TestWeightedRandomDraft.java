package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.process.bioen.WeightedRandomDraft;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWeightedRandomDraft {

    private Configuration cfg;
    WeightedRandomDraft<School> weight_rand = new WeightedRandomDraft<>();

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
        weight_rand.init();


    }

    @Test
    public void testWeights() {

        Species species = cfg.getSpecies(0);
        School school1 = new School(species, 10000);
        School school2 = new School(species, 10000);
        School school3 = new School(species, 10000);

        weight_rand.reset();
        weight_rand.add(100., school1);
        weight_rand.add(1000., school2);
        weight_rand.add(10000., school3);

        double expected[] = new double[] {100, 100 + 1000, 100 + 1000 + 10000};
        assertArrayEquals(expected, weight_rand.getKeys());

        weight_rand.reset();
        weight_rand.add(100., school1);
        weight_rand.add(10000., school3);
        weight_rand.add(1000., school2);

        expected = new double[] {100, 10000 + 100, 100 + 10000 + 1000};
        assertArrayEquals(expected, weight_rand.getKeys());

    }

    @Test
    public void testEntrySorted() {

        Species species = cfg.getSpecies(0);
        School school1 = new School(species, 10000);
        School school2 = new School(species, 10000);
        School school3 = new School(species, 10000);

        weight_rand.reset();
        weight_rand.add(100., school1);
        weight_rand.add(1000., school2);
        weight_rand.add(10000., school3);

        Map.Entry<Double, School> entry = weight_rand.getMap().ceilingEntry(100.);
        double key = entry.getKey();
        assertEquals(key, 100);

        entry = weight_rand.getMap().ceilingEntry(100.1);
        key = entry.getKey();
        assertEquals(key, 1000 + 100);

        entry = weight_rand.getMap().ceilingEntry(1000. + 100.);
        key = entry.getKey();
        assertEquals(key, 1000 + 100);

        entry = weight_rand.getMap().ceilingEntry(10000.);
        key = entry.getKey();
        assertEquals(key, 1000 + 100 + 10000);

    }

    @Test
    public void testProbaSorted() {

        Species species = cfg.getSpecies(0);
        School school1 = new School(species, 10000);
        School school2 = new School(species, 10000);
        School school3 = new School(species, 10000);

        double w1 = 1000;
        double w2 = 100;
        double w3 = 10000;

        weight_rand.reset();
        weight_rand.add(w1, school2);
        weight_rand.add(w2, school1);
        weight_rand.add(w3, school3);

        int N = 100000000;
        double output[] = new double[N];
        double test[] = new double[3];
        for (int i = 0; i < N; i++) {
            School school = weight_rand.next();
            if(school.equals(school2)) {
                output[i] = 0;
                test[0]++;
            } else if (school.equals(school1)) {
                output[i] = 1;
                test[1]++;
            } else if (school.equals(school3)) {
                output[i] = 2;
                test[2]++;
            } else {
                output[i] = -999;
            }
        }

        for (int i = 0; i < 3; i++) {
            test[i] /= N;
        }

        double wtot = w1 + w2 + w3;
        double expected[] = new double[] {w1 / wtot, w2 / wtot, w3/wtot};
        assertArrayEquals(expected, test, 0.0001);

    }


    @Test
    public void testProbaWithZeros() {

        Species species = cfg.getSpecies(0);
        School school1 = new School(species, 10000);
        School school2 = new School(species, 10000);
        School school3 = new School(species, 10000);

        double w1 = 1000;
        double w2 = 0;
        double w3 = 10000;

        weight_rand.reset();
        weight_rand.add(w1, school2);
        weight_rand.add(w2, school1);
        weight_rand.add(w3, school3);

        int N = 100000000;
        double output[] = new double[N];
        double test[] = new double[3];
        for (int i = 0; i < N; i++) {
            School school = weight_rand.next();
            if(school.equals(school2)) {
                output[i] = 0;
                test[0]++;
            } else if (school.equals(school1)) {
                output[i] = 1;
                test[1]++;
            } else if (school.equals(school3)) {
                output[i] = 2;
                test[2]++;
            } else {
                output[i] = -999;
            }
        }

        for (int i = 0; i < 3; i++) {
            test[i] /= N;
        }

        double wtot = w1 + w2 + w3;
        double expected[] = new double[] {w1 / wtot, w2 / wtot, w3/wtot};
        assertArrayEquals(expected, test, 0.0001);

    }

}
