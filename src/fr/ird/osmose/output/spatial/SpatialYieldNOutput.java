
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
 * @author Nicolas Barrier
 */
public class SpatialYieldNOutput extends AbstractSpatialOutput {

    public SpatialYieldNOutput(int rank){
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
                            data[iSpec][j][i] += school.getNdead(MortalityCause.FISHING);
                        }
                    }
                }                
            }
        }
    }
    
    @Override
    public void write(float time) {

        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                    data[ispec][j][i] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        for (int kspec = 0; kspec < nSpecies; kspec++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrBiomass.set(0, kspec, j, i, data[kspec][j][i]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) this.timeOut * 360 / (float) this.counter);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write(this.getVarName(), new int[]{index, 0, 0, 0}, arrBiomass);
        } catch (IOException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}
