/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.output.distribution.AbstractDistribution;
import java.io.File;

/**
 *
 * @author pverley
 */
public class BiomassDistribOutput_Netcdf extends AbstractDistribOutput_Netcdf {

    public BiomassDistribOutput_Netcdf(int rank, AbstractDistribution distrib) {
        super(rank, distrib);
    }
    
    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int classSchool = getClass(school);
            if (classSchool >= 0) {
                values[school.getSpeciesIndex()][classSchool] += school.getInstantaneousBiomass();
            }
        }
    }

    @Override
    String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getType().toString());
        filename.append("Indicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_biomassDistribBy");
        filename.append(getType().toString());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");;
        return filename.toString();

    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Distribution of fish species biomass (tonne) by ");
        description.append(getType().getDescription());
        description.append(". For class i, the biomass of fish in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    String getUnits() {
        return("tons");
    }

    @Override
    String getVarname() {
       return("biomass");
    }
    
}
