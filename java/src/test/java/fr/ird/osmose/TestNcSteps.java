package fr.ird.osmose;

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
    
    @Test
    public void TestNcSteps1() throws IOException, InvalidRangeException {
        
        // Resource files has 24 steps per years
        String filename = this.createSingleLtlFile("test1", "test1_ltl_", 24);
        ForcingFile forcingFile = new ForcingFile("ltl", filename, 24, 0.0, 1.0, ForcingFileCaching.NONE);
        forcingFile.init();
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
