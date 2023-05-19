
package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.output.distribution.DistributionType;
import fr.ird.osmose.output.distribution.OutputDistribution;
import ucar.ma2.InvalidRangeException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSchoolDistribution {

    private static Configuration cfg;
    private OutputDistribution lengthDistribution;

    @BeforeAll
    public void prepareData() {

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        cmd.put("output.distrib.bySize.min", "0");
        cmd.put("output.distrib.bySize.max", "30");
        cmd.put("output.distrib.bySize.incr", "5");


        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();

        // cmd.put("test.distribution.sp0", "lesser_Spotted_Dogfish");
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }

        // output.distrib.bySize.max;200;;
        // output.distrib.bySize.min;0;;
        // output.distrib.bySize.incr;10;;
        lengthDistribution = new OutputDistribution(DistributionType.SIZE);
        lengthDistribution.init();

    }

    @Test
    public void testSizeDistributionThresholds() {

        float actual[] = lengthDistribution.getThresholds();
        float expected[] = new float[] {5.f, 10.f, 15.f, 20.f, 25.f, 30.f};
        assertArrayEquals(expected, actual);

        int nClass = lengthDistribution.getNClass();
        assertEquals(7, nClass);

        int nThres = lengthDistribution.getThresholds().length;
        assertEquals(6, nThres);

    }

    @Test
    public void testSizeStageSp0() {

        // 5, 10, 15, 20, 25, 30, inf
        School school1 = createSchool(0, 3.f, 0.5f, 1.f, 1);
        School school2 = createSchool(0, 7.f, 0.5f, 1.f, 1);
        School school3 = createSchool(0, 12.f, 0.5f, 1.f, 1);
        School school4 = createSchool(0, 35.f, 0.5f, 1.f, 1);
        School school5 = createSchool(0, 5.f, 0.5f, 1.f, 1);
        School school6 = createSchool(0, 27.f, 0.5f, 1.f, 1);
        School school7 = createSchool(0, 17.f, 0.5f, 1.f, 1);
        School school8 = createSchool(0, 21.f, 0.5f, 1.f, 1);
        School school9 = createSchool(0, 5.f, 0.5f, 1.f, 1);
        School school10 = createSchool(0, 15.f, 0.5f, 1.f, 1);

        int stage1 = lengthDistribution.getClass(school1);
        assertEquals(0, stage1);

        int stage2 = lengthDistribution.getClass(school2);
        assertEquals(1, stage2);

        int stage3 = lengthDistribution.getClass(school3);
        assertEquals(2, stage3);

        int stage4 = lengthDistribution.getClass(school4);
        assertEquals(6, stage4);

        // Asserts classes that are on the edges
        int stage5 = lengthDistribution.getClass(school5);
        assertEquals(1, stage5);

        int stage6 = lengthDistribution.getClass(school6);
        assertEquals(5, stage6);

        int stage7 = lengthDistribution.getClass(school7);
        assertEquals(3, stage7);

        int stage8 = lengthDistribution.getClass(school8);
        assertEquals(4, stage8);

        int stage9 = lengthDistribution.getClass(school9);
        assertEquals(1, stage9);

        int stage10 = lengthDistribution.getClass(school10);
        assertEquals(3, stage10);

    }

    // @Test
    // public void testSizeStageSp1() {

    //     School school1 = createSchool(1, 3.f, 0.5f, 1.f, 1);
    //     School school2 = createSchool(1, 7.f, 0.5f, 1.f, 1);
    //     School school3 = createSchool(1, 12.f, 0.5f, 3.f, 1);
    //     School school4 = createSchool(1, 35.f, 0.5f, 3.f, 1);
    //     School school5 = createSchool(1, 5.f, 0.5f, 5.f, 1);
    //     School school6 = createSchool(1, 10.f, 0.5f, 5.f, 1);
    //     School school7 = createSchool(1, 30.f, 0.5f, 5.f, 1);

    //     int stage1 = lengthDistribution.getClass(school1);
    //     assertEquals(0, stage1);

    //     int stage2 = lengthDistribution.getClass(school2);
    //     assertEquals(0, stage2);

    //     int stage3 = lengthDistribution.getClass(school3);
    //     assertEquals(0, stage3);

    //     int stage4 = lengthDistribution.getClass(school4);
    //     assertEquals(0, stage4);

    //     // Asserts classes that are on the edges
    //     int stage5 = lengthDistribution.getClass(school5);
    //     assertEquals(0, stage5);

    //     int stage6 = lengthDistribution.getClass(school6);
    //     assertEquals(0, stage6);

    //     int stage7 = lengthDistribution.getClass(school7);
    //     assertEquals(0, stage7);

    // }

    // @Test
    // public void testSizeStageSp2() {

    //     // For species 2, control should be driven by weight
    //     School school1 = createSchool(2, 3.f, 0.5f, 1.f, 1);
    //     School school2 = createSchool(2, 7.f, 0.5f, 1.f, 1);
    //     School school3 = createSchool(2, 12.f, 6.f, 1.f, 1);
    //     School school4 = createSchool(2, 35.f, 6.f, 1.f, 1);
    //     School school5 = createSchool(2, 5.f, 12f, 1.f, 1);
    //     School school6 = createSchool(2, 10.f, 12f, 1.f, 1);
    //     School school7 = createSchool(2, 30.f, 35f, 1.f, 1);

    //     int stage1 = lengthDistribution.getClass(school1);
    //     assertEquals(0, stage1);

    //     int stage2 = lengthDistribution.getClass(school2);
    //     assertEquals(0, stage2);

    //     int stage3 = lengthDistribution.getClass(school3);
    //     assertEquals(1, stage3);

    //     int stage4 = lengthDistribution.getClass(school4);
    //     assertEquals(1, stage4);

    //     // Asserts classes that are on the edges
    //     int stage5 = lengthDistribution.getClass(school5);
    //     assertEquals(2, stage5);

    //     int stage6 = lengthDistribution.getClass(school6);
    //     assertEquals(2, stage6);

    //     int stage7 = lengthDistribution.getClass(school7);
    //     assertEquals(3, stage7);

    // }

    // @Test
    // public void testSizeStageSp3() {

    //     // For species 2, control should be driven by weight
    //     School school1 = createSchool(3, 1.f, 0.5f, 1.f, 1);
    //     School school2 = createSchool(3, 1.f, 0.5f, 1.f, 1);
    //     School school3 = createSchool(3, 1.f, 0.6f, 3.f, 1);
    //     School school4 = createSchool(3, 1.f, 0.6f, 3.f, 1);
    //     School school5 = createSchool(3, 1.f, 0.12f, 5.f, 1);
    //     School school6 = createSchool(3, 1.f, 0.12f, 5.f, 1);
    //     School school7 = createSchool(3, 1.f, 0.35f, 7.f, 1);

    //     int stage1 = lengthDistribution.getClass(school1);
    //     assertEquals(0, stage1);

    //     int stage2 = lengthDistribution.getClass(school2);
    //     assertEquals(0, stage2);

    //     int stage3 = lengthDistribution.getClass(school3);
    //     assertEquals(1, stage3);

    //     int stage4 = lengthDistribution.getClass(school4);
    //     assertEquals(1, stage4);

    //     // Asserts classes that are on the edges
    //     int stage5 = lengthDistribution.getClass(school5);
    //     assertEquals(2, stage5);

    //     int stage6 = lengthDistribution.getClass(school6);
    //     assertEquals(2, stage6);

    //     int stage7 = lengthDistribution.getClass(school7);
    //     assertEquals(3, stage7);

    // }


    private School createSchool(int speciesIndex, float length, float weight, float age, float trophicLevel) {
        Species species = cfg.getSpecies(speciesIndex);
        int ageDt = (int) age * cfg.getNStepYear();
        School school = new School(species, -1, -1, 1, length, weight, ageDt, trophicLevel);
        return school;
    }


}
