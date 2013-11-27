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
public class MeanTrophicLevelAgeOutput extends AbstractOutput {

    private double[][] meanTL;
    private double[][] biomass;

     public MeanTrophicLevelAgeOutput(int rank, String keyEnabled) {
        super(rank, keyEnabled);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanTLPerAge_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean Trophic Level of fish species by age class.";
    }

    @Override
    String[] getHeaders() {

        int classmax = 0;
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            classmax = (int) Math.max(Math.ceil(getConfiguration().getFloat("species.lifespan.sp" + iSpecies)) , classmax);
        }
        String[] headers = new String[classmax + 1];
        headers[0] = "Species index";
        for (int i = 0; i < classmax; i++) {
            headers[i + 1] = "Age class " + i;
        }
        return headers;
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        meanTL = new double[getNSpecies()][];
        biomass = new double[getNSpecies()][];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            meanTL[iSpecies] = new double[(int) Math.ceil(getConfiguration().getFloat("species.lifespan.sp" + iSpecies))];
            biomass[iSpecies] = new double[(int) Math.ceil(getConfiguration().getFloat("species.lifespan.sp" + iSpecies))];
        }
    }

    @Override
    public void update() {
        int nstep = getConfiguration().getNStepYear();
        for (School school : getSchoolSet().getAliveSchools()) {
            int i = school.getSpeciesIndex();
            double biom = school.getInstantaneousBiomass();
            int ageClass = school.getAgeDt() / nstep;
            meanTL[i][ageClass] += biom * school.getTrophicLevel();
            biomass[i][ageClass] += biom;
        }
    }

    @Override
    public void write(float time) {

        double[][] values = new double[getNSpecies()][];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            values[iSpecies] = new double[meanTL[iSpecies].length + 1];
            values[iSpecies][0] = iSpecies;
            for (int ageClass = 0; ageClass < meanTL[iSpecies].length; ageClass++) {
                if (biomass[iSpecies][ageClass] > 0.d) {
                    meanTL[iSpecies][ageClass] = (float) (meanTL[iSpecies][ageClass] / biomass[iSpecies][ageClass]);
                } else {
                    meanTL[iSpecies][ageClass] = Double.NaN;
                }
                values[iSpecies][ageClass + 1] = meanTL[iSpecies][ageClass];
            }
        }
        
        writeVariable(time, values);
    }
}
