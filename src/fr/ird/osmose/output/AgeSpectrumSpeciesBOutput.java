/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import java.io.File;

/**
 *
 * @author pverley
 */
public class AgeSpectrumSpeciesBOutput extends AbstractSpectrumOutput {

    public AgeSpectrumSpeciesBOutput(int rank, String keyEnabled) {
        super(rank, keyEnabled, Type.AGE);
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
        StringBuilder filename = new StringBuilder("AgeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_AgeSpectrumSpeciesB_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();

    }

    @Override
    String getDescription() {
        return "Distribution of fish species biomass in age classes (year). For age class i, the biomass of fish in [i,i+1[ is reported.";
    }

    @Override
    public void initStep() {
        // nothing to do
    }
}
