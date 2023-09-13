
package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.stage.SchoolStage;
import ucar.ma2.InvalidRangeException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSchoolStage {

    private static Configuration cfg;
    private SchoolStage lengthStage;

    @BeforeAll
    public void prepareData() {

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        cmd.put("output.diet.stage.threshold.sp1", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.structure.sp2", "weight");
        cmd.put("output.diet.stage.structure.sp3", "age");
        cmd.put("output.diet.stage.threshold.sp3", "2;4;6");  // overwrites 5, 10, 30

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

        lengthStage = new SchoolStage("output.diet.stage");
        lengthStage.init();

    }


    @Test
    public void testSizeStageSp0() {

        School school1 = createSchool(0, 3.f, 0.5f, 1.f, 1);
        School school2 = createSchool(0, 7.f, 0.5f, 1.f, 1);
        School school3 = createSchool(0, 12.f, 0.5f, 1.f, 1);
        School school4 = createSchool(0, 35.f, 0.5f, 1.f, 1);
        School school5 = createSchool(0, 5.f, 0.5f, 1.f, 1);
        School school6 = createSchool(0, 10.f, 0.5f, 1.f, 1);
        School school7 = createSchool(0, 30.f, 0.5f, 1.f, 1);

        int stage1 = lengthStage.getStage(school1);
        assertEquals(0, stage1);

        int stage2 = lengthStage.getStage(school2);
        assertEquals(1, stage2);

        int stage3 = lengthStage.getStage(school3);
        assertEquals(2, stage3);

        int stage4 = lengthStage.getStage(school4);
        assertEquals(3, stage4);

        // Asserts classes that are on the edges
        int stage5 = lengthStage.getStage(school5);
        assertEquals(1, stage5);

        int stage6 = lengthStage.getStage(school6);
        assertEquals(2, stage6);

        int stage7 = lengthStage.getStage(school7);
        assertEquals(3, stage7);

    }

    @Test
    public void testSizeStageSp1() {

        School school1 = createSchool(1, 3.f, 0.5f, 1.f, 1);
        School school2 = createSchool(1, 7.f, 0.5f, 1.f, 1);
        School school3 = createSchool(1, 12.f, 0.5f, 3.f, 1);
        School school4 = createSchool(1, 35.f, 0.5f, 3.f, 1);
        School school5 = createSchool(1, 5.f, 0.5f, 5.f, 1);
        School school6 = createSchool(1, 10.f, 0.5f, 5.f, 1);
        School school7 = createSchool(1, 30.f, 0.5f, 5.f, 1);

        int stage1 = lengthStage.getStage(school1);
        assertEquals(0, stage1);

        int stage2 = lengthStage.getStage(school2);
        assertEquals(0, stage2);

        int stage3 = lengthStage.getStage(school3);
        assertEquals(0, stage3);

        int stage4 = lengthStage.getStage(school4);
        assertEquals(0, stage4);

        // Asserts classes that are on the edges
        int stage5 = lengthStage.getStage(school5);
        assertEquals(0, stage5);

        int stage6 = lengthStage.getStage(school6);
        assertEquals(0, stage6);

        int stage7 = lengthStage.getStage(school7);
        assertEquals(0, stage7);

    }

    @Test
    public void testSizeStageSp2() {

        // For species 2, control should be driven by weight
        School school1 = createSchool(2, 3.f, 0.5f, 1.f, 1);
        School school2 = createSchool(2, 7.f, 0.5f, 1.f, 1);
        School school3 = createSchool(2, 12.f, 6.f, 1.f, 1);
        School school4 = createSchool(2, 35.f, 6.f, 1.f, 1);
        School school5 = createSchool(2, 5.f, 12f, 1.f, 1);
        School school6 = createSchool(2, 10.f, 12f, 1.f, 1);
        School school7 = createSchool(2, 30.f, 35f, 1.f, 1);

        int stage1 = lengthStage.getStage(school1);
        assertEquals(0, stage1);

        int stage2 = lengthStage.getStage(school2);
        assertEquals(0, stage2);

        int stage3 = lengthStage.getStage(school3);
        assertEquals(1, stage3);

        int stage4 = lengthStage.getStage(school4);
        assertEquals(1, stage4);

        // Asserts classes that are on the edges
        int stage5 = lengthStage.getStage(school5);
        assertEquals(2, stage5);

        int stage6 = lengthStage.getStage(school6);
        assertEquals(2, stage6);

        int stage7 = lengthStage.getStage(school7);
        assertEquals(3, stage7);

    }

    @Test
    public void testSizeStageSp3() {

        // For species 2, control should be driven by weight
        School school1 = createSchool(3, 1.f, 0.5f, 1.f, 1);
        School school2 = createSchool(3, 1.f, 0.5f, 1.f, 1);
        School school3 = createSchool(3, 1.f, 0.6f, 3.f, 1);
        School school4 = createSchool(3, 1.f, 0.6f, 3.f, 1);
        School school5 = createSchool(3, 1.f, 0.12f, 5.f, 1);
        School school6 = createSchool(3, 1.f, 0.12f, 5.f, 1);
        School school7 = createSchool(3, 1.f, 0.35f, 7.f, 1);

        int stage1 = lengthStage.getStage(school1);
        assertEquals(0, stage1);

        int stage2 = lengthStage.getStage(school2);
        assertEquals(0, stage2);

        int stage3 = lengthStage.getStage(school3);
        assertEquals(1, stage3);

        int stage4 = lengthStage.getStage(school4);
        assertEquals(1, stage4);

        // Asserts classes that are on the edges
        int stage5 = lengthStage.getStage(school5);
        assertEquals(2, stage5);

        int stage6 = lengthStage.getStage(school6);
        assertEquals(2, stage6);

        int stage7 = lengthStage.getStage(school7);
        assertEquals(3, stage7);

    }


    private School createSchool(int speciesIndex, float length, float weight, float age, float trophicLevel) {
        Species species = cfg.getSpecies(speciesIndex);
        int ageDt = (int) age * cfg.getNStepYear();
        School school = new School(species, -1, -1, 1, length, weight, ageDt, trophicLevel);
        return school;
    }


}
