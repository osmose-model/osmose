package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.process.mortality.fishery.FisherySelectivity;

/**
 * Test the management of accessibility matrix, i.e. whether time-varying
 * matrixes are well defined. This is why the classVarGetter is defined as 0.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestFisherySelectivity {

    private Configuration cfg;
    private FisherySelectivity selectivity;

    /** Check that the matrix index contains 120 years. */
    @Test
    public void testSelectivity() {

        School school;
        Species species;

        double expected;
        double actual;

        species = cfg.getSpecies(0);
        school = new School(species, 10);
        school.setLength(16);
        actual = selectivity.getSelectivity(0, school);
        expected = 0.033872888050286126;
        assertEquals(expected, actual);

        school.setLength(0.2f);
        actual = selectivity.getSelectivity(0, school);
        expected = 0.0;
        assertEquals(expected, actual);

        school.setLength(40f);
        actual = selectivity.getSelectivity(0, school);
        expected = 0.9970751534686401;
        assertEquals(expected, actual);

        school.setLength(24.2f);
        actual = selectivity.getSelectivity(0, school);
        expected = 0.16968280913496425;
        assertEquals(expected, actual);

    }

    /** Prepare the input data for the test. */
    @BeforeAll
    public void prepareData() throws Exception {

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv")
                .getFile();

        // Update the URL for Windows computers
        configurationFile = configurationFile.replaceAll("%20", " ");

        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();

        cmd.put("fisheries.rate.base.fsh0", "0.0331359790275683");
        cmd.put("fisheries.structure.fsh0", "size");
        cmd.put("fisheries.threshold.fsh0",
                "0.5,1,1.5,2,2.5,3,3.5,4,4.5,5,5.5,6,6.5,7,7.5,8,8.5,9,9.5,10,10.5,11,11.5,12,12.5,13,13.5,14,14.5,15,15.5,16,16.5,17,17.5,18,18.5,19,19.5,20,20.5,21,21.5,22,22.5,23,23.5,24,24.5,25,25.5,26,26.5,27,27.5,28,28.5,29,29.5,30,30.5,31,31.5,32,32.5,33");
        cmd.put("fisheries.values.fsh0",
                "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.033872888050286126,0.07052588783535779,0.07605071712248221,0.08153112492305042,0.08701600168604848,0.09255173524150309,0.09818578597042463,0.10396364791035144,0.1099292956515963,0.1161267931637249,0.1225974634526747,0.12938385104078132,0.1365276364509772,0.14406758048407448,0.15204351649187034,0.16054610482553172,0.16968280913496425,0.17943855412023693,0.18971153681718822,0.2004069258430488,0.2114220244411718,0.22265654909461974,0.23402103117554002,0.2454211457835713,0.2567665602914564,0.2679696532424795,0.27893738083884045,0.28958450507045375,0.2977702439678086,0.3143679704338209,0.3891519452589055,0.519755227181493,0.7916855728605039,0.9970751534686401,0.9970751534686401");
        cmd.put("fisheries.type.fsh0", "9");

        // Test the standard configuration
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();

        selectivity = new FisherySelectivity(0, "fisheries", "fsh");
        selectivity.init();

    }



}
