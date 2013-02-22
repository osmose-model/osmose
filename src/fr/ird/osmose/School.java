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

import fr.ird.osmose.process.PredationProcess;

public class School extends GridPoint {

    /*
     * Species of the school
     */
    final private Species species;
    /*
     * Age of the fish expressed in number of time steps.
     */
    private int age;
    /*
     * length of the individuals in the school in centimeters
     */
    private float length;
    /*
     * weight of individual of the school in grams
     */
    private float weight;
    /*
     * TL of this fish
     */
    private float trophicLevel;
    /*
     * Whether the school is catchable for fishing
     */
    private boolean catchable;
    /*
     * Correspond to feeding length-stage
     */
    private int feedingStage;
    /*
     * Correspond to the age-stage used for accessibilities between species
     */
    private int accessibilityStage;
    /*
     *
     */
    private int dietOutputStage;
    /*
     * Number of individuals in the school
     */
    private double abundance;
    //
    public float[][] diet;
    //
    public double nDeadFishing;
    public double nDeadPredation;
    public double nDeadStarvation;
    public double nDeadNatural;
    //
    public double preyedBiomass;
    /*
     * Available biomass [ton] of the school for predation by other schools
     */
    public double biomassToPredate;
    /*
     * Predation success rate
     */
    public float predSuccessRate;
    //
    public boolean hasPredated;

//////////////
// Constructor
//////////////
    /**
     * Create a new school.
     *
     * @param cohort of the school
     * @param abundance, number of individuals in the school
     * @param length [cm] of the individual
     * @param weight [g] of the individual
     */
    public School(Species species, double abundance, float length, float weight, int age) {
        this.species = species;
        this.abundance = abundance;
        this.length = length;
        this.weight = weight;
        this.age = age;

        // Set initial trophic level to EGG
        trophicLevel = Species.TL_EGG;
        // Unlocated
        setOffGrid();
    }

////////////////////////////
// Definition of the methods
////////////////////////////

    /*
     * Reset school state variables
     */
    public void initStep() {
        // Update the stage
        updateFeedingStage(species.sizeFeeding, species.nbFeedingStages);
        updateAccessStage(species.ageStagesTab, species.nbAccessStages);
        updateDietOutputStage(species.dietStagesTab, species.nbDietStages);
        // Reset variables
        nDeadFishing = 0;
        nDeadNatural = 0;
        nDeadPredation = 0;
        nDeadStarvation = 0;
        biomassToPredate = PredationProcess.computeBiomassToPredate(this, 1);
        preyedBiomass = 0;
        catchable = true;
        // Reset diet variables
        diet = new float[getOsmose().getNumberSpecies() + getOsmose().getNumberLTLGroups()][];
        for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
            if (getOsmose().dietsOutputMatrix) {
                diet[i] = new float[getOsmose().nbDietsStages[i]];
            } else {
                diet[i] = new float[0];
            }
        }
        for (int i = getOsmose().getNumberSpecies(); i < getOsmose().getNumberSpecies() + getOsmose().getNumberLTLGroups(); i++) {
            diet[i] = new float[1];
        }
    }

    /**
     * Converts the specified biomass [tons] into abundance [scalar]
     */
    public double biom2abd(double biomass) {
        return 1.e6d * biomass / weight;
    }

    /**
     * Converts the specified abundance [scalar] into biomass [tons]
     */
    public double adb2biom(double abundance) {
        return abundance * weight / 1.e6d;
    }

    /**
     * @return the abundance
     */
    public double getAbundance() {
        return abundance;
    }

    public double getInstantaneousAbundance() {
        double nDeadTotal = nDeadPredation
                + nDeadStarvation
                + nDeadNatural
                + nDeadFishing;
        double abundanceTmp = abundance - nDeadTotal;
        //if (nDeadTotal > 0) System.out.println("Abundance changed " + " " + school.nDeadPredation + " " +  school.nDeadStarvation + " " + school.nDeadNatural + " " + school.nDeadFishing);
        return (abundanceTmp < 1)
                ? 0.d
                : abundanceTmp;
    }

    /**
     * @param abundance the abundance to set
     */
    public void setAbundance(double abundance) {
        this.abundance = abundance;
    }

    /**
     * @return the biomass
     */
    public double getBiomass() {
        return adb2biom(abundance);
    }

    public double getInstantaneousBiomass() {
        return adb2biom(getInstantaneousAbundance());
    }

    /**
     * @return whether the school is alive or not
     */
    public boolean isAlive() {
        return abundance > 0;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("School");
        str.append("\n  Species: ");
        str.append(getSpecies().getName());
        str.append("\n  Cohort: ");
        float ageInYear = getAgeDt() / (float) getOsmose().getNumberTimeStepsPerYear();
        str.append(ageInYear);
        str.append(" [year]");
        str.append("\n  Cell: ");
        str.append(getCell().getIndex());
        return str.toString();
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

    public void incrementAge() {
        age += 1;
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
    public void incrementLength(float dlength) {
        if (dlength != 0.f) {
            length += dlength;
            weight = species.computeWeight(length);
        }
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
     * @return the dietOutputStage
     */
    public int getDietOutputStage() {
        return dietOutputStage;
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
    public void notCatchable() {
        catchable = false;
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

        if (!getOsmose().dietsOutputMatrix) {
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
                int tempAge = Math.round(thresholdTab[i - 1] * getOsmose().getNumberTimeStepsPerYear());
                if (getLength() >= tempAge) {
                    dietOutputStage++;
                }
            }
        }
    }

    /*
     * Get the current Osmose instance
     */
    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }

    /**
     * @return the trophicLevel
     */
    public float getTrophicLevel() {
        return trophicLevel;
    }

    /**
     * @param trophicLevel the trophicLevel to set
     */
    public void setTrophicLevel(float trophicLevel) {
        this.trophicLevel = trophicLevel;
    }
}
