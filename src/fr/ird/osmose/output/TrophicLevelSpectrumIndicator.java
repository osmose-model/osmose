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
    //
    private float[] tabTL;
    private int nTLClass;

    public TrophicLevelSpectrumIndicator(int replica) {
        super(replica);
        initializeTLSpectrum();
    }

    private void initializeTLSpectrum() {

        float minTL = 1.0f;
        float maxTL = 6.0f;
        nTLClass = (int) (1 + ((maxTL - minTL) / 0.1f));   // TL classes of 0.1, from 1 to 6
        tabTL = new float[nTLClass];
        tabTL[0] = minTL;
        for (int i = 1; i < nTLClass; i++) {
            tabTL[i] = minTL + i * 0.1f;
        }
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        trophicLevelSpectrum = new double[getNSpecies()][tabTL.length];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            int ageClass1 = (int) Math.max(1, school.getSpecies().getAgeClassZero());
            if (!includeClassZero() && (school.getAgeDt() < ageClass1)) {
                continue;
            }
            double biomass = school.getInstantaneousBiomass();
            if (biomass > 0) {
                trophicLevelSpectrum[school.getSpeciesIndex()][getTLRank(school)] += biomass;
            }
        }
    }

    private int getTLRank(School school) {

        int iTL = tabTL.length - 1;
        while (school.getTrophicLevel() <= tabTL[iTL] && (iTL > 0)) {
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

        double[][] values = new double[nTLClass][getNSpecies() + 1];
        for (int iTL = 0; iTL < nTLClass; iTL++) {
            values[iTL][0] = tabTL[iTL];
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
        StringBuilder str = new StringBuilder("Distribution of species biomass (tons) by 0.1 TL class, and ");
        if (includeClassZero()) {
            str.append("including ");
        } else {
            str.append("excluding ");
        }
        str.append("first ages specified in input");
        return str.toString();
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
