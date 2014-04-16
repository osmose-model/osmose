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
package fr.ird.osmose.output.distribution;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.ISchool;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public abstract class AbstractDistribution {

    // Distribution classes
    float[] classes;
    private final DistributionType type;
    private final int indexSpecies;

    abstract float getDefaultMin();

    abstract float getDefaultMax();

    abstract float getDefaultIncr();

    abstract float getValue(ISchool school);

    AbstractDistribution(DistributionType type, int indexSpecies) {
        this.type = type;
        this.indexSpecies = indexSpecies;
    }

    AbstractDistribution(DistributionType type) {
        this(type, -1);
    }

    final public void init() {

        // Minimal class of the distribution
        float min;
        // Maximal class of the distribution
        float max;
        // Increment between two contiguous classes of the distribution
        float incr;

        if (indexSpecies >= 0 && speciesDistribExist()) {
            min = getConfiguration().getFloat(getKeyMinSpecies());
            max = getConfiguration().getFloat(getKeyMaxSpecies());
            incr = getConfiguration().getFloat(getKeyIncrSpecies());
        } else if (distribExist()) {
            min = getConfiguration().getFloat(getKeyMinSpecies());
            max = getConfiguration().getFloat(getKeyMaxSpecies());
            incr = getConfiguration().getFloat(getKeyIncrSpecies());
        } else {
            min = getDefaultMin();
            max = getDefaultMax();
            incr = getDefaultIncr();
        }

        // Number of classes 
        int nClass = (int) Math.ceil((max - min) / incr);
        classes = new float[nClass];
        classes[0] = min;
        for (int i = 1; i < nClass; i++) {
            classes[i] = min + i * incr;
        }
    }
    
    public int getClass(School school) {
        return getClass(getValue(school));
    }

    final public int getClass(float value) {
        int iClass = classes.length - 1;
        if (value <= classes[classes.length - 1]) {
            while ((iClass >= 0) && (value < classes[iClass])) {
                iClass--;
            }
        }
        return iClass;
    }
    
    public float getThreshold(int iClass) {
        return classes[iClass];
    }

    public int getNClass() {
        return classes.length;
    }

    public DistributionType getType() {
        return type;
    }

    private String getKeyMin() {
        return "output.distrib.by" + type.toString() + ".min";
    }

    private String getKeyMinSpecies() {
        return getKeyMin() + ".sp" + indexSpecies;
    }

    private String getKeyMax() {
        return "output.distrib.by" + type.toString() + ".max";
    }

    private String getKeyMaxSpecies() {
        return getKeyMax() + ".sp" + indexSpecies;
    }

    private String getKeyIncr() {
        return "output.distrib.by" + type.toString() + ".incr";
    }

    private String getKeyIncrSpecies() {
        return getKeyIncr() + ".sp" + indexSpecies;
    }

    private boolean speciesDistribExist() {
        return (!getConfiguration().isNull(getKeyMinSpecies()))
                && (!getConfiguration().isNull(getKeyMaxSpecies()))
                && (!getConfiguration().isNull(getKeyIncrSpecies()));
    }

    private boolean distribExist() {
        return (!getConfiguration().isNull(getKeyMin()))
                && (!getConfiguration().isNull(getKeyMax()))
                && (!getConfiguration().isNull(getKeyIncr()));
    }

    Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }
}
