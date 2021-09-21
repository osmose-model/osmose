package fr.ird.osmose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
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
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

@TestInstance(Lifecycle.PER_CLASS)
public class TestNcSteps {
    
    private Configuration cfg;
    private final NetcdfFileWriter.Version NC_VERSION = NetcdfFileWriter.Version.netcdf3;
    
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
        String filename = this.createSingleLtlFile("test1", "test1_ltl_", 24);
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
        boolean deleted = tempFile.delete();
        
    }

        /** Test of a seasonal file, which contains one value per osmose dt **/
        @Test
        public void TestNcSteps2() throws IOException, InvalidRangeException {
            
            // Resource files has 24 steps per years
            String filename = this.createSingleLtlFile("test2", "test2_ltl_", 12);
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
            boolean deleted = tempFile.delete();
            
        }
        
        /** Test of a seasonal file, which contains 5 years of monthly values **/
        @Test
        public void TestNcSteps3() throws IOException, InvalidRangeException {
            
            // Resource files has 24 steps per years
            String filename = this.createSingleLtlFile("test3", "test3_ltl_", 5*12);
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
            boolean deleted = tempFile.delete();
            
        }
        
     /** Test of a combination of three files with different number of time-steps **/
     @Test
     public void TestNcSteps4() throws IOException, InvalidRangeException {
         
         // Resource files has 24 steps per years
         String filename1 = this.createSingleLtlFile("test4", "test4_a_ltl_", 12);
         String filename2 = this.createSingleLtlFile("test4", "test4_b_ltl_", 2 * 12);
         String filename3 = this.createSingleLtlFile("test4", "test4_c_ltl_", 3 * 12);
         
         String directory = new File(filename1).getParent();
         String pattern = 
         
         System.out.println(directory);
          
         //ForcingFile forcingFile = new ForcingFile("ltl", filename, 12, 0.0, 1.0, ForcingFileCaching.NONE);
         //forcingFile.init();
         
        //  // Check the proper conversion from Osmose time-step to NetCDf time step
        //  assertEquals(0, forcingFile.getNcStep(0));
        //  assertEquals(0, forcingFile.getNcStep(1));

        //  assertEquals(16, forcingFile.getNcStep(32));
        //  assertEquals(6, forcingFile.getNcStep(613));
        //  assertEquals(3, forcingFile.getNcStep(1807));
         
        //  // Check the proper conversion from **global** netcdf index to individual ones
        //  assertEquals(0, forcingFile.getNcIndex(0));
        //  assertEquals(3, forcingFile.getNcIndex(3));
        //  assertEquals(8, forcingFile.getNcIndex(8));
        //  assertEquals(49, forcingFile.getNcIndex(49));
         
        //  // Check the proper conversion from **global** netcdf index to individual ones
        //  assertEquals(filename, forcingFile.getNcFileName(0));
        //  assertEquals(filename, forcingFile.getNcFileName(3));
        //  assertEquals(filename, forcingFile.getNcFileName(8));
         
        //  File tempFile = new File(filename);
        //  boolean deleted = tempFile.delete();
         
     }   
        
        
        
    private String createSingleLtlFile(String directory, String prefix, int nSteps) throws IOException, InvalidRangeException {
         
        String fileName = File.createTempFile(prefix, ".nc").getAbsolutePath();
        NetcdfFileWriter nc = NetcdfFileWriter.createNew(NC_VERSION, fileName);
        
        ArrayList<Dimension> outDims = new ArrayList<>();
        
        // Add time dim and variable (common to all files)
        Dimension timeDim = nc.addUnlimitedDimension("time");
        Dimension xDim = nc.addDimension(null, "x", cfg.getGrid().get_nx());
        Dimension yDim = nc.addDimension(null, "y", cfg.getGrid().get_ny());
        outDims.add(timeDim);
        outDims.add(yDim);
        outDims.add(xDim);
        
        Variable timeVar = nc.addVariable(null, "time", DataType.DOUBLE, "time");
        Variable ltlVar = nc.addVariable(null, "ltl", DataType.DOUBLE, outDims);
        
        nc.create();
        
        ArrayDouble.D3 arrLtl = new ArrayDouble.D3(nSteps, cfg.getGrid().get_ny(), cfg.getGrid().get_nx());
        nc.write(ltlVar, arrLtl);
        
        ArrayDouble.D1 arrTime = new ArrayDouble.D1(nSteps);
        nc.write(timeVar, arrTime);
        
        nc.close();
        
        return fileName;
        
    }
}
