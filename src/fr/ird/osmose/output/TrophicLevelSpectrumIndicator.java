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
public class TrophicLevelSpectrumIndicator extends AbstractIndicator {

    /*
     * Trophic level distribution [SPECIES][TL_SPECTRUM]
     */
    private double[][] trophicLevelSpectrum;
    
     public TrophicLevelSpectrumIndicator(int replica) {
        super(replica);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        trophicLevelSpectrum = new double[getNSpecies()][getConfiguration().tabTL.length];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            int ageClass1 = (int) Math.max(1, school.getSpecies().indexAgeClass0);
            if ((school.getBiomass() > 0) && (school.getAgeDt() >= ageClass1)) {
                trophicLevelSpectrum[school.getSpeciesIndex()][getTLRank(school)] += school.getBiomass();
            }
        }
    }

    private int getTLRank(School school) {

        int iTL = getConfiguration().tabTL.length - 1;
        while (school.getTrophicLevel() <= getConfiguration().tabTL[iTL] && (iTL > 0)) {
            iTL--;
        }
        return iTL;
    }

    @Override
    public boolean isEnabled() {
        return getConfiguration().isTLOutput();
    }

    @Override
    public void write(float time) {

        double[][] values = new double[getConfiguration().nbTLClass][getNSpecies() + 1];
        for (int iTL = 0; iTL < getConfiguration().nbTLClass; iTL++) {
            values[iTL][0] = getConfiguration().tabTL[iTL];
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                values[iTL][iSpec] = (trophicLevelSpectrum[iSpec][iTL] / getConfiguration().getRecordFrequency());
            }
        }
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getOutputPrefix());
        filename.append("_TLDistrib_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Distribution of species biomass (tons) by 0.1 TL class, and excluding first ages specified in input (in calibration file)";
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + 1];
        headers[0] = "TL";
        for (int i = 0; i < getNSpecies(); i++) {
            headers[i + 1] = getSimulation().getSpecies(i).getName();
        }
        return headers;
    }
}
