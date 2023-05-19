package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import fr.ird.osmose.util.io.ForcingFile;
import fr.ird.osmose.util.io.ForcingFileCaching;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

@TestInstance(Lifecycle.PER_CLASS)
public class TestNcSteps {

    private Configuration cfg;
    private final NetcdfFileFormat NC_VERSION = NetcdfFileFormat.NETCDF3;

    /** Prepare the Osmose object for the tests. */
    @BeforeAll
    public void prepareData() {

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        osmose.readConfiguration(configurationFile);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }
    }

    /** Test of a seasonal file, which contains one value per osmose dt **/
    @Test
    public void TestNcSteps1() throws IOException, InvalidRangeException {

        // Resource files has 24 steps per years
        String filename = this.createSingleLtlFile("test1_ltl_", 24);
        ForcingFile forcingFile = new ForcingFile("ltl", filename, 24, 0.0, 1.0, ForcingFileCaching.NONE);
        forcingFile.init();

        // Check the proper conversion from Osmose time-step to NetCDf time step
        assertEquals(0, forcingFile.getNcStep(0));
        assertEquals(1, forcingFile.getNcStep(1));
        assertEquals(0, forcingFile.getNcStep(24));
        assertEquals(0, forcingFile.getNcStep(48));

        assertEquals(3, forcingFile.getNcStep(1203));
        assertEquals(8, forcingFile.getNcStep(2648));

        // Check the proper conversion from **global** netcdf index to individual ones
        assertEquals(0, forcingFile.getNcIndex(0));
        assertEquals(3, forcingFile.getNcIndex(3));
        assertEquals(8, forcingFile.getNcIndex(8));

        // Check the proper conversion from **global** netcdf index to individual ones
        assertEquals(filename, forcingFile.getNcFileName(0));
        assertEquals(filename, forcingFile.getNcFileName(3));
        assertEquals(filename, forcingFile.getNcFileName(8));

        File tempFile = new File(filename);
        tempFile.delete();

    }

        /** Test of a seasonal file, which contains two values per osmose dt (i.e
         * one every 30 days) **/
        @Test
        public void TestNcSteps2() throws IOException, InvalidRangeException {

            // Resource files has 24 steps per years
            String filename = this.createSingleLtlFile("test2_ltl_", 12);
            ForcingFile forcingFile = new ForcingFile("ltl", filename, 12, 0.0, 1.0, ForcingFileCaching.NONE);
            forcingFile.init();

            // Check the proper conversion from Osmose time-step to NetCDf time step
            assertEquals(0, forcingFile.getNcStep(0));
            assertEquals(0, forcingFile.getNcStep(1));
            assertEquals(0, forcingFile.getNcStep(24));
            assertEquals(0, forcingFile.getNcStep(48));

            assertEquals(1, forcingFile.getNcStep(1203));
            assertEquals(4, forcingFile.getNcStep(2648));

            // Check the proper conversion from **global** netcdf index to individual ones
            assertEquals(0, forcingFile.getNcIndex(0));
            assertEquals(3, forcingFile.getNcIndex(3));
            assertEquals(8, forcingFile.getNcIndex(8));

            // Check the proper conversion from **global** netcdf index to individual ones
            assertEquals(filename, forcingFile.getNcFileName(0));
            assertEquals(filename, forcingFile.getNcFileName(3));
            assertEquals(filename, forcingFile.getNcFileName(8));

            File tempFile = new File(filename);
            tempFile.delete();

        }

        /** Test of a seasonal file, which contains 5 years of monthly values **/
        @Test
        public void TestNcSteps3() throws IOException, InvalidRangeException {

            // Resource files has 24 steps per years
            String filename = this.createSingleLtlFile("test3_ltl_", 5*12);
            ForcingFile forcingFile = new ForcingFile("ltl", filename, 12, 0.0, 1.0, ForcingFileCaching.NONE);
            forcingFile.init();

            // Check the proper conversion from Osmose time-step to NetCDf time step
            assertEquals(0, forcingFile.getNcStep(0));
            assertEquals(0, forcingFile.getNcStep(1));

            assertEquals(16, forcingFile.getNcStep(32));
            assertEquals(6, forcingFile.getNcStep(613));
            assertEquals(3, forcingFile.getNcStep(1807));

            // Check the proper conversion from **global** netcdf index to individual ones
            assertEquals(0, forcingFile.getNcIndex(0));
            assertEquals(3, forcingFile.getNcIndex(3));
            assertEquals(8, forcingFile.getNcIndex(8));
            assertEquals(49, forcingFile.getNcIndex(49));

            // Check the proper conversion from **global** netcdf index to individual ones
            assertEquals(filename, forcingFile.getNcFileName(0));
            assertEquals(filename, forcingFile.getNcFileName(3));
            assertEquals(filename, forcingFile.getNcFileName(8));

            File tempFile = new File(filename);
            tempFile.delete();

        }

     /** Test of a combination of three files with different number of time-steps **/
     @Test
     public void TestNcSteps4() throws IOException, InvalidRangeException {

         // Resource files has 24 steps per years
         String filename1 = this.createSingleLtlFile("test4_a_ltl_", 12);
         String filename2 = this.createSingleLtlFile("test4_b_ltl_", 2 * 12);
         String filename3 = this.createSingleLtlFile("test4_c_ltl_", 3 * 12);

         String directory = new File(filename1).getParent();
         String pattern = new File(directory, "test4_[abc]_ltl_.*").getAbsolutePath();

         ForcingFile forcingFile = new ForcingFile("ltl", pattern, 12, 0.0, 1.0, ForcingFileCaching.NONE);
         forcingFile.init();

         // We remove the files, in order to make sure that for the next running of tests,
         // only the newly created files are used.
         File tempFile;

         tempFile = new File(filename1);
         tempFile.delete();

         tempFile = new File(filename2);
         tempFile.delete();

         tempFile = new File(filename3);
         tempFile.delete();

         // test on the total number of time steps
         assertEquals(72, forcingFile.getTimeLength());

         // testing on the name of the file that is accessed depending on the Nc time step
         assertEquals(filename1, forcingFile.getNcFileName(0));
         assertEquals(filename1, forcingFile.getNcFileName(5));
         assertEquals(filename1, forcingFile.getNcFileName(11));

         assertEquals(filename2, forcingFile.getNcFileName(12));
         assertEquals(filename2, forcingFile.getNcFileName(27));
         assertEquals(filename2, forcingFile.getNcFileName(33));

         assertEquals(filename3, forcingFile.getNcFileName(36));
         assertEquals(filename3, forcingFile.getNcFileName(50));
         assertEquals(filename3, forcingFile.getNcFileName(71));

         // testing on the time-step that will be used on the local file
         assertEquals(0, forcingFile.getNcIndex(0));
         assertEquals(5, forcingFile.getNcIndex(5));
         assertEquals(11, forcingFile.getNcIndex(11));

         assertEquals(0, forcingFile.getNcIndex(12));
         assertEquals(15, forcingFile.getNcIndex(27));
         assertEquals(21, forcingFile.getNcIndex(33));

         assertEquals(0, forcingFile.getNcIndex(36));
         assertEquals(14, forcingFile.getNcIndex(50));
         assertEquals(35, forcingFile.getNcIndex(71));

         // Check the proper conversion from Osmose time-step to NetCDf time step
         assertEquals(5, forcingFile.getNcStep(10));
         assertEquals(34, forcingFile.getNcStep(69));

         // check for the cyclicity, when the first 6 years have been read
         assertEquals(5, forcingFile.getNcStep(10 + 6 * 24));
         assertEquals(34, forcingFile.getNcStep(69 + 6 * 24));

         assertEquals(5, forcingFile.getNcStep(10 + 48 * 24));
         assertEquals(34, forcingFile.getNcStep(69 + 48 * 24));

     }


     /**
      * Create a temporary LTL file used in the different tests.
      *
      * @param prefix
      *            File prefix
      * @param nSteps
      *            Number of time steps in the file.
      */
    private String createSingleLtlFile(String prefix, int nSteps) throws IOException, InvalidRangeException {

        String fileName = Files.createTempFile(prefix, ".nc").toString();
        NetcdfFormatWriter.Builder bNc = NetcdfFormatWriter.createNewNetcdf4(NC_VERSION, fileName, null);

        ArrayList<Dimension> outDims = new ArrayList<>();

        // Add time dim and variable (common to all files)
        Dimension timeDim = bNc.addUnlimitedDimension("time");
        Dimension xDim = bNc.addDimension("x", cfg.getGrid().get_nx());
        Dimension yDim = bNc.addDimension("y", cfg.getGrid().get_ny());
        outDims.add(timeDim);
        outDims.add(yDim);
        outDims.add(xDim);

        bNc.addVariable("time", DataType.DOUBLE, "time");
        bNc.addVariable("ltl", DataType.DOUBLE, outDims);

        NetcdfFormatWriter nc = null;

        nc = bNc.build();

        ArrayDouble.D3 arrLtl = new ArrayDouble.D3(nSteps, cfg.getGrid().get_ny(), cfg.getGrid().get_nx());
        nc.write(nc.findVariable("ltl"), arrLtl);

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(nSteps);
        nc.write(nc.findVariable("time"), arrTime);

        nc.close();

        return fileName;

    }
}
