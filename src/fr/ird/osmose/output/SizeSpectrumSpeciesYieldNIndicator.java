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
public class SizeSpectrumSpeciesYieldNIndicator extends AbstractSizeSpectrumIndicator {

    public SizeSpectrumSpeciesYieldNIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation, keyEnabled);
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            sizeSpectrum[school.getSpeciesIndex()][getSizeRank(school)] += school.getNdeadFishing();
        }
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_SizeSpectrumSpeciesYieldN_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();

    }

    @Override
    String getDescription() {
        return "Distribution of cumulative catch (number of fish caught per time step of saving) in size classes (cm). For size class i, the yield in [i,i+1[ is reported.";
    }
}