package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.process.mortality.additional.AnnualAdditionalMortality;
import fr.ird.osmose.process.mortality.additional.AnnualLarvaMortality;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestAdditionalMortality {

    private Configuration cfg;
    private AnnualAdditionalMortality mort1;
    private AnnualLarvaMortality mort1larva;
    private AnnualAdditionalMortality mort2;
    private AnnualLarvaMortality mort2larva;

    @BeforeAll
    public void prepareData() throws Exception {

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv")
                .getFile();

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();

        // first test: old param (yearly values)
        cmd.put("mortality.additional.rate.sp0", "5");
        cmd.put("mortality.additional.larva.rate.sp0", "3");

        // second test: old param + multiplier
        cmd.put("mortality.additional.rate.sp1", "3");
        cmd.put("mortality.additional.rate.multiplier.sp1", "10");
        cmd.put("mortality.additional.larva.rate.sp1", "5");
        cmd.put("mortality.additional.larva.rate.multiplier.sp1", "20");

        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();

        // Setting values for species 0
        Species spec = osmose.getConfiguration().getSpecies(0);
        mort1 = new AnnualAdditionalMortality(0, spec);
        mort1.init();

        mort1larva = new AnnualLarvaMortality(0, spec);
        mort1larva.init();

        // Setting values for species 1
        spec = osmose.getConfiguration().getSpecies(1);
        mort2 = new AnnualAdditionalMortality(0, spec);
        mort2.init();

        mort2larva = new AnnualLarvaMortality(0, spec);
        mort2larva.init();

    }

    @Test
    public void testMort1() {
        double[] actual = mort1.getRates();
        double[] expected = new double[cfg.getNStep()];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 5. / cfg.getNStepYear();
        }

        assertArrayEquals(expected, actual, 1e-5);

    }

    @Test
    public void testMortLarva1() {
        double[] actual = mort1larva.getRates();
        double[] expected = new double[cfg.getNStep()];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 3.;
        }

        assertArrayEquals(expected, actual, 1e-5);

    }

    @Test
    public void testMort2() {
        double[] actual = mort2.getRates();
        double[] expected = new double[cfg.getNStep()];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 30. / cfg.getNStepYear();
        }

        assertArrayEquals(expected, actual, 1e-5);

    }

    @Test
    public void testMortLarva2() {
        double[] actual = mort2larva.getRates();
        double[] expected = new double[cfg.getNStep()];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 100.;
        }

        assertArrayEquals(expected, actual, 1e-5);

    }

}
