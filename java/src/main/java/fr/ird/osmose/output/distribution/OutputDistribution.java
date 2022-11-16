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
import fr.ird.osmose.stage.ClassGetter;
import fr.ird.osmose.util.OsmoseLinker;

/** Species belong to distribution (T) class s if
 *
 *   T(s) <=  L(s) < T(s + 1)
 *
 *
 * @author pverley
 */
public class OutputDistribution extends OsmoseLinker {

    // Distribution classes
    float[] thresholds;
    private final DistributionType type;
    private final int fileSpeciesIndex;

    private static float AGE_DEFAULT_MIN = 1.f;
    private static float AGE_DEFAULT_MAX = 25.f;
    private static float AGE_DEFAULT_INCR = 1.f;

    private static float SIZE_DEFAULT_MIN = 10.f;
    private static float SIZE_DEFAULT_MAX = 200.f;
    private static float SIZE_DEFAULT_INCR = 10.f;

    private static float TL_DEFAULT_MIN = 1.f;
    private static float TL_DEFAULT_MAX = 6.f;
    private static float TL_DEFAULT_INCR = 0.1f;

    private static float WEIGHT_DEFAULT_MIN = 25.f;
    private static float WEIGHT_DEFAULT_MAX = 2000.f;
    private static float WEIGHT_DEFAULT_INCR = 25.f;

    private final String key;
    private ClassGetter classGetter;

    public OutputDistribution(DistributionType type, int indexSpecies) {
        this.type = type;
        this.fileSpeciesIndex = indexSpecies;
        this.key = "output.distrib";
    }

    public OutputDistribution(DistributionType type) {
        this(type, -1);
    }

    final public void init() {

        switch (type) {
            case AGE:
                classGetter = (school -> school.getAge());
                break;
            case SIZE:
                classGetter = (school -> school.getLength());
                break;
            case WEIGHT:
                // converts getWeight (in tons) to grams
                classGetter = (school -> school.getWeight() * 1e6);
                break;
            case TL:
                classGetter = (school -> school.getTrophicLevel());
                break;
        }

        // Minimal class of the distribution
        float min;
        // Maximal class of the distribution
        float max;
        // Increment between two contiguous classes of the distribution
        float incr;

        if (fileSpeciesIndex >= 0 && speciesDistribExist()) {
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

        if(min == 0) {
            String message = "The minimum value was set equal to 0. It has been corrected to " + String.valueOf(incr);
            warning(message);
            min = incr;
        }

        // Number of classes
        // if thresholds are from 5 to 30 by 5,
        // thresholds class are [0, 5[, [5, 10[, .... [30, inf[
        int nClass = (int) Math.ceil((max - min) / incr) + 1;

        thresholds = new float[nClass];
        thresholds[0] = min;
        for (int i = 1; i < nClass; i++) {
            thresholds[i] = min + i * incr;
        }
    }

    public int getClass(IMarineOrganism school) {
        return getClass(classGetter.getVariable(school));
    }

    public int getClass(double value) {
        int stage = 0;
        for (float threshold : thresholds) {
            if (value < threshold) {
                break;
            }
            stage++;
        }
        return stage;
    }

    public float[] getThresholds() {
        return thresholds;
    }

    public float getThreshold(int iClass) {
        return thresholds[iClass];
    }

    public int getNClass() {
        return thresholds.length + 1;
    }

    public DistributionType getType() {
        return type;
    }

    private String getKeyMin() {
        return key + ".by" + type.toString() + ".min";
    }

    private String getKeyMinSpecies() {
        return getKeyMin() + ".sp" + fileSpeciesIndex;
    }

    private String getKeyMax() {
        return key + ".by" + type.toString() + ".max";
    }

    private String getKeyMaxSpecies() {
        return getKeyMax() + ".sp" + fileSpeciesIndex;
    }

    private String getKeyIncr() {
        return key + ".by" + type.toString() + ".incr";
    }

    private String getKeyIncrSpecies() {
        return getKeyIncr() + ".sp" + fileSpeciesIndex;
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

    private float getDefaultMin() {

        float output = -99;
        switch (type) {
            case AGE:
                output = AGE_DEFAULT_MIN;
                break;
            case SIZE:
                output = SIZE_DEFAULT_MIN;
                break;
            case WEIGHT:
                output = WEIGHT_DEFAULT_MIN;
                break;
            case TL:
                output = TL_DEFAULT_MIN;
                break;
        }

        if(output < 0) {
            error("Type " + type.toString() + "is not supported", null);
        }

        return output;

    }

    private float getDefaultMax() {

        float output = -999;
        switch (type) {
            case AGE:
                output = AGE_DEFAULT_MAX;
                break;
            case SIZE:
                output = SIZE_DEFAULT_MAX;
                break;
            case WEIGHT:
                output = WEIGHT_DEFAULT_MAX;
                break;
            case TL:
                output = TL_DEFAULT_MAX;
                break;
        }

        if(output < 0) {
            error("Type " + type.toString() + "is not supported", null);
        }

        return output;

    }

    private float getDefaultIncr() {

        float output = -999;
        switch (type) {
            case AGE:
                output = AGE_DEFAULT_INCR;
                break;
            case SIZE:
                output = SIZE_DEFAULT_INCR;
                break;
            case WEIGHT:
                output = WEIGHT_DEFAULT_INCR;
                break;
            case TL:
                output = TL_DEFAULT_INCR;
                break;
        }

        if(output < 0) {
            error("Type " + type.toString() + "is not supported", null);
        }

        return output;

    }


}
