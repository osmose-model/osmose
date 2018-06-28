
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
        
/**
 *
 * @author pverley
 */
public class SpatialTLOutput extends AbstractSpatialOutput {
    
    public SpatialTLOutput(int rank){
        super(rank);
    }
    
    @Override
    public String getVarName()
    {
        return "TL";
    }
    
    @Override
    public String getDesc()
    {
        return "trophic level per species and per cell";
    }
    
    @Override
    public void update() {
        
        // In this case, a weighted mean is applied on the TL, with
        // weights being provided by the biomass

        this.common_update();
        
        int nSpecies = getNSpecies();
        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();
        
        // temporary variable containing the total biomass within one cell
        float biomass[][][];
        biomass = new float[nSpecies][ny][nx];
        float temp[][][];
        temp = new float[nSpecies][ny][nx];

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
                            // here, data is TK weighted by the biomass
                            int iSpec = school.getSpeciesIndex();
                            temp[iSpec][j][i] += school.getTrophicLevel() * school.getInstantaneousBiomass();
                            biomass[iSpec][j][i] += school.getInstantaneousBiomass();
                        }
                    } 
                }      
            }
        }
        
        // Computation of the Weighted Mean for the TL
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            for (int j = 0; j < ny; j++) {
                    for (int i = 0; i < nx; i++) {
                    if (biomass[iSpec][j][i] > 0) {
                        temp[iSpec][j][i] /= biomass[iSpec][j][i];
                    }
                }
            }
        }
        
        
         // Update of the TL array
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    if (biomass[iSpec][j][i] > 0) {
                        data[iSpec][j][i] += temp[iSpec][j][i];
                    }
                }
            }
        }
        
    }
}
