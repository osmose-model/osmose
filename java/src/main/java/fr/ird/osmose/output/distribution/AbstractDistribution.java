/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.output.distribution;

import fr.ird.osmose.IMarineOrganism;
import fr.ird.osmose.util.OsmoseLinker;

/** Species belong to distribution (T) class s if 
 * 
 *   T(s) <=  L(s) < T(s + 1) 
 *  
 *
 * @author pverley
 */
public abstract class AbstractDistribution extends OsmoseLinker {

    // Distribution classes
    float[] classes;
    private final DistributionType type;
    private final int indexSpecies;

    abstract float getDefaultMin();

    abstract float getDefaultMax();

    abstract float getDefaultIncr();

    abstract float getValue(IMarineOrganism school);

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
            min = getConfiguration().getFloat(getKeyMin());
            max = getConfiguration().getFloat(getKeyMax());
            incr = getConfiguration().getFloat(getKeyIncr());
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
    
    public int getClass(IMarineOrganism school) {
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
}
