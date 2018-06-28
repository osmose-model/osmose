
package fr.ird.osmose.output.spatial;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import fr.ird.osmose.output.IOutput;
import fr.ird.osmose.process.mortality.MortalityCause;
        
/**
 *
 * @author pverley
 */
public class SpatialYieldOutput extends AbstractSpatialOutput {

    public SpatialYieldOutput(int rank){
        super(rank);
    }
    
    @Override
    public String getVarName()
    {
        return "Yield";
    }
    
    @Override
    public String getDesc()
    {
        return "Catches, in tons, per species and per cell";
    }
    
    @Override
    public void update(){ 
           
        this.common_update();
     
        
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        if (cutoffEnabled && school.getAge() < cutoffAge[school.getSpeciesIndex()]) {
                            continue;
                        }
                        if (!school.isUnlocated()) {
                            int iSpec = school.getSpeciesIndex();
                            data[iSpec][j][i] += school.abd2biom(school.getNdead(MortalityCause.FISHING));
                        }
                    }
                }                
            }
        }
    }
}
