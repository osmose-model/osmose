/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import java.io.File;

/**
 *
 * @author pverley
 */
public class BiomassDistribOutput extends AbstractSpectrumOutput {

    public BiomassDistribOutput(int rank, Type type) {
        super(rank, type);
    }
    
    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int classSchool = getClass(school);
            if (classSchool >= 0) {
                spectrum[school.getSpeciesIndex()][classSchool] += school.getInstantaneousBiomass();
            }
        }
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getType().toString());
        filename.append("Indicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_biomass-distrib-by");
        filename.append(getType().toString());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
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
    
}
