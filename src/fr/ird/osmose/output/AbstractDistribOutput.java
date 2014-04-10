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
import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.output.distribution.DistributionType;

/**
 *
 * @author pverley
 */
public abstract class AbstractDistribOutput extends AbstractOutput {

    // Output values distributed by species and by class
    double[][] values;
    // Distribution 
    private final AbstractDistribution distrib;

    public AbstractDistribOutput(int rank, AbstractDistribution distrib) {
        super(rank);
        this.distrib = distrib;
        distrib.init();
    }

    @Override
    public void reset() {
        values = new double[getNSpecies()][distrib.getNClass()];
    }

    int getClass(School school) {
        return distrib.getClass(school);
    }

    @Override
    public void write(float time) {

        int nClass = distrib.getNClass();
        double[][] array = new double[nClass][getNSpecies() + 1];
        for (int iClass = 0; iClass < nClass; iClass++) {
            array[iClass][0] = distrib.getThreshold(iClass);
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                array[iClass][iSpec + 1] = values[iSpec][iClass] / getRecordFrequency();
            }
        }
        writeVariable(time, array);
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + 1];
        headers[0] = distrib.getType().toString();
        for (int i = 0; i < getNSpecies(); i++) {
            headers[i + 1] = getSimulation().getSpecies(i).getName();
        }
        return headers;
    }

    float getClassThreshold(int iClass) {
        return distrib.getThreshold(iClass);
    }

    int getNClass() {
        return distrib.getNClass();
    }

    DistributionType getType() {
        return distrib.getType();
    }
}
