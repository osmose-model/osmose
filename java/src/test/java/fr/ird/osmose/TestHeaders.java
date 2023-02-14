
package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.output.BiomassDietStageOutput;
import ucar.ma2.InvalidRangeException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestHeaders {

    private static Configuration cfg;

    @BeforeAll
    public void prepareData() {

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        //cmd.put("output.diet.stage.threshold.sp0", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp1", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp2", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp3", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp4", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp5", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp6", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp7", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp8", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp9", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp10", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp11", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp12", "null");  // overwrites 5, 10, 30
        cmd.put("output.diet.stage.threshold.sp13", "null");  // overwrites 5, 10, 30

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
    }

    @Test
    public void testBiomassDietHeader() {

        BiomassDietStageOutput test = new BiomassDietStageOutput(0);
        test.init();
        String[] headers = test.getHeaders();
        String[] expected = new String[] {
            String.format("lesserSpottedDogfish [%f, %f[", 0.0f, 5.0f),  // 0
            String.format("lesserSpottedDogfish [%f, %f[", 5.f, 10.0f),  // 0
            String.format("lesserSpottedDogfish [%f, %f[", 10.0f, 30.0f),  // 0
            String.format("lesserSpottedDogfish [%f, inf[", 30.0f),  // 0
            "redMullet", // 1
            "pouting", //2
            "whiting", //3
            "poorCod", //4
            "cod", //5
            "dragonet", //6
            "sole",  //7
            "plaice", //8
            "horseMackerel", //9
            "mackerel", //10
            "herring", //11
            "sardine", //12
            "squids", // 13
            "Dinoflagellates", //rsc, 14
            "Diatoms",  // rsc, 15
            "Microzoo", // rsc, 16
            "Mesozoo", // rsc 17
            "Macrozoo", // rsc 18
            "VSBVerySmallBenthos", // rsc 19
            "SmallBenthos", // rsc 20
            "MediumBenthos", // rsc 21
            "LargeBenthos", // rsc 22
            "VLBVeryLargeBenthos", // rsc 23
        };

        assertArrayEquals(expected, headers);



    }

}
