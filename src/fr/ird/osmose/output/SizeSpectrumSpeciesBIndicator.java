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
public class SizeSpectrumSpeciesBIndicator extends AbstractSizeSpectrumIndicator {

    public SizeSpectrumSpeciesBIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation, keyEnabled);
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            sizeSpectrum[school.getSpeciesIndex()][getSizeRank(school)] += school.getInstantaneousBiomass();
        }
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_SizeSpectrumSpeciesB_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();

    }

    @Override
    String getDescription() {
        return "Distribution of fish species biomass in size classes (cm). For size class i, the biomass of fish in [i,i+1[ is reported.";
    }
}
