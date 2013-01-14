/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright IRD (Institut de Recherche pour le Développement) 2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 *
 * This software is a computer program whose purpose is to simulate trophic
 * interactions between fish species. This model assumes opportunistic
 * predation based on spatial co-occurrence and size adequacy between a
 * predator and its prey (size-based opportunistic predation).
 * It represents fish individuals grouped into schools, which are
 * characterized by their size, weight, age, taxonomy and geographical
 * location (2D model), and which undergo major processes of fish life cycl
 * (growth, explicit predation, natural and starvation mortalities,
 * reproduction and migration) and a fishing mortality distinct for each
 * species (Shin and Cury 2001, 2004).
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
package fr.ird.osmose;

public class Fish extends GridPoint {

    /*
     * Species of the school
     */
    Species species;
    /*
     * Name of the species
     */
    String speciesName;
    /*
     * Age of the fish expressed in number of time steps.
     */
    int age;
    /*
     * length of the individuals in the school in centimeters
     */
    float length;
    /*
     * TL of this fish, amd its variation in a time-step
     */
    float trophicLevel, tmpTL;
    /*
     * Predation success rate
     */
    float predSuccessRate;
    /*
     * whether the school will disappear at next time step
     */
    boolean alive;
    /*
     * Available biomass [ton] of the school for predation by other schools
     */
    double biomassToPredate;
    /*
     * Maximum prey size [cm]
     */
    float critPreySizeMax;
    /*
     * Minimum prey size [cm]
     */
    float critPreySizeMin;
    /*
     * Whether the school is catchable for fishing
     */
    boolean catchable;
    /*
     * Correspond to feeding length-stage
     */
    int feedingStage;
    /*
     * Correspond to the age-stage used for accessibilities between species
     */
    int accessibilityStage;
    /*
     *
     */
    int dietOutputStage;

    public Fish(Species species) {
        this.species = species;
    }

    public Species getSpecies() {
        return species;
    }

    public int getSpeciesIndex() {
        return species.getIndex();
    }

    public int getAgeDt() {
        return age;
    }

    /**
     * @return the length
     */
    public float getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(float length) {
        this.length = length;
    }

    /**
     * @return the weight
     */
    public float getWeight() {
        return age == 0
                ? species.eggWeight
                : species.computeWeight(length);
    }

    /**
     * @return the trophicLevel
     */
    public int getFeedingStage() {
        return feedingStage;
    }

    /**
     * @return the trophicLevel
     */
    public int getAccessibilityStage() {
        return accessibilityStage;
    }

    /**
     * @return whether the shcool will disappear at next time step
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Tag the school as about to disappear
     */
    public void kill() {
        alive = false;
    }

    /**
     * @return whether the school is catchable for fishing
     */
    public boolean isCatchable() {
        return catchable;
    }

    /**
     * @param sets whether the school is catchable for fishing
     */
    public void setCatchable(boolean catchable) {
        this.catchable = catchable;
    }

    public void updateAccessStage(float[] ageStages, int nbAccessStages) {
        accessibilityStage = 0;
        for (int i = 1; i < nbAccessStages; i++) {
            if (getAgeDt() >= ageStages[i - 1]) {
                accessibilityStage++;
            }
        }
    }

    public void updateFeedingStage(float[] sizeStages, int nbStages) {
        feedingStage = 0;
        for (int i = 1; i < nbStages; i++) {
            if (getLength() >= sizeStages[i - 1]) {
                feedingStage++;
            }
        }
    }

    public void updateDietOutputStage(float[] thresholdTab, int nbStages) {

        if (!getOsmose().dietsOutputMatrix[getOsmose().numSerie]) {
            return;
        }

        dietOutputStage = 0;

        if (getOsmose().getDietOutputMetric().equalsIgnoreCase("size")) {
            for (int i = 1; i < nbStages; i++) {
                if (getLength() >= thresholdTab[i - 1]) {
                    dietOutputStage++;
                }
            }
        } else if (getOsmose().getDietOutputMetric().equalsIgnoreCase("age")) {
            for (int i = 1; i < nbStages; i++) {
                int tempAge = Math.round(thresholdTab[i - 1] * getSimulation().getNbTimeStepsPerYear());
                if (getLength() >= tempAge) {
                    dietOutputStage++;
                }
            }
        }
    }
}
