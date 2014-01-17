/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import java.io.File;

/**
 *
 * @author pverley
 */
public class TrophicLevelSpectrumOutput extends AbstractOutput {

    /*
     * Trophic level distribution [SPECIES][TL_SPECTRUM]
     */
    private double[][] trophicLevelSpectrum;
    //
    private float[] tabTL;
    private int nTLClass;

     public TrophicLevelSpectrumOutput(int rank, String keyEnabled) {
        super(rank, keyEnabled);
        initializeTLSpectrum();
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
    }

    private void initializeTLSpectrum() {

        float minTL = 2.0f;
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
        for (School school : getSchoolSet().getAliveSchools()) {
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
    public void write(float time) {

        double[][] values = new double[nTLClass][getNSpecies() + 1];
        for (int iTL = 0; iTL < nTLClass; iTL++) {
            values[iTL][0] = tabTL[iTL];
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                values[iTL][iSpec + 1] = (trophicLevelSpectrum[iSpec][iTL] / getRecordFrequency());
            }
        }
        
        writeVariable(time, values);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_TLDistrib_Simu");
        filename.append(getRank());
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
