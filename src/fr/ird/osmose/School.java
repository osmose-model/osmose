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

public class School extends GridPoint {
    
    final public static boolean INSTANTANEOUS_BIOMASS = true;
    final public static boolean INISTEP_BIOMASS = false;
    /**
     * Species of the school.
     */
    final private Species species;
    /**
     * Age of the fish expressed in number of time steps.
     */
    private int age;
    /**
     * Length of the individuals in the school in centimeters.
     */
    private float length;
    /**
     * Weight of individual of the school in tons.
     * The unit has been set to tons just because it saves computation time for
     * converting the biomass from grams to tons  
     */
    private float weight;
    /**
     * Trophic level of this school.
     */
    private float trophicLevel;
    /**
     * Whether the school is catchable for fishing.
     */
    private boolean catchable;
    /**
     * Correspond to feeding length-stage.
     */
    private int feedingStage;
    /**
     * Correspond to the age-stage used for accessibility between species
     */
    private int accessibilityStage;
    /**
     * Diet stage.
     */
    private int dietOutputStage;
    /**
     * Number of individuals in the school at beginning of the time step.
     */
    private double abundance;
    /**
     * Number of individuals in the school. instantaneousAbundance = abundance -
     * ndeads
     */
    private double instantaneousAbundance;
    /**
     * Matrix of diets in a time step. diet[NSPECIES+NPLANKTON][NDIETSTAGES]
     */
    public float[][] diet;
    /**
     * Number of dead individuals due to fishing.
     */
    private double ndeadFishing;
    /**
     * Number of dead individuals due to predation.
     */
    private double ndeadPredation;
    /**
     * Number of dead individuals due to starvation.
     */
    private double ndeadStarvation;
    /**
     * Number of dead individuals due to natural mortality.
     */
    private double ndeadNatural;
    /**
     * Predation success rate. (ratio of what is preyed on maximal ingestion).
     */
    public float predSuccessRate;
    /*
     * Monitor whether the number of deads has changed
     */
    private boolean ndeadHasChanged;

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
        instantaneousAbundance = abundance;
        this.length = length;
        this.weight = weight * 1.e-6f;
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
        catchable = true;
        // Reset diet variables
        diet = new float[getConfiguration().getNumberSpecies() + getConfiguration().getNumberLTLGroups()][];
        for (int i = 0; i < getConfiguration().getNumberSpecies(); i++) {
            if (getConfiguration().dietsOutputMatrix) {
                diet[i] = new float[getConfiguration().nbDietsStages[i]];
            } else {
                diet[i] = new float[0];
            }
        }
        for (int i = getConfiguration().getNumberSpecies(); i < getConfiguration().getNumberSpecies() + getConfiguration().getNumberLTLGroups(); i++) {
            diet[i] = new float[1];
        }
    }
    
    public void updateAbundance() {
        abundance = getInstantaneousAbundance();
        ndeadFishing = 0;
        ndeadNatural = 0;
        ndeadPredation = 0;
        ndeadStarvation = 0;
        ndeadHasChanged = false;
    }

    /**
     * Converts the specified biomass [tons] into abundance [scalar]
     */
    public double biom2abd(double biomass) {
        return biomass / weight;
    }

    /**
     * Converts the specified abundance [scalar] into biomass [tons]
     */
    public double adb2biom(double abundance) {
        return abundance * weight;
    }

    /**
     * Gets the abundance of the school at the beginning of the time step.
     *
     * @return the abundance of the school at the beginning of the time step
     */
    public double getAbundance() {
        return abundance;
    }

    /**
     * Evaluates the instantaneous abundance of the school.
     *
     * @return the instantaneous abundance of the school. instantaneous
     * abundance = abundance at the beginning of the time step minus the dead
     * individuals (due to either predation, starvation, natural or fishing) at
     * the moment the function is called. It is a snapshot of the abundance of
     * the school within the current time step.
     */
    public double getInstantaneousAbundance() {
        if (ndeadHasChanged) {
            double nDeadTotal = ndeadPredation
                    + ndeadStarvation
                    + ndeadNatural
                    + ndeadFishing;
            instantaneousAbundance = abundance - nDeadTotal;
            if (instantaneousAbundance < 1.d) {
                instantaneousAbundance = 0.d;
            }
            ndeadHasChanged = false;
        }
        return instantaneousAbundance;
    }

    /**
     * Gets the biomass of the school at the beginning of the time step.
     *
     * @return the biomass of the school at the beginning of the time step
     * expressed in ton.
     */
    public double getBiomass() {
        return adb2biom(abundance);
    }

    /**
     * Evaluates the instantaneous biomass of the school.
     *
     * @return the instantaneous biomass of the school. instantaneous biomass =
     * biomass at the beginning of the time step minus the dead biomass (due to
     * either predation, starvation, natural or fishing) at the moment the
     * function is called. It is a snapshot of the biomass of the school within
     * the current time step.
     */
    public double getInstantaneousBiomass() {
        return adb2biom(getInstantaneousAbundance());
    }

    /**
     * Checks whether the school is alive
     *
     * @return whether the school is alive or not
     */
    public boolean isAlive() {
        return (getInstantaneousAbundance() > 0) && (age <= species.getLongevity() - 1);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("School");
        str.append("\n  Species: ");
        str.append(getSpecies().getName());
        str.append("\n  Cohort: ");
        float ageInYear = getAgeDt() / (float) getConfiguration().getNumberTimeStepsPerYear();
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
            weight = species.computeWeight(length) * 1e-6f;
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

        if (!getConfiguration().dietsOutputMatrix) {
            return;
        }

        dietOutputStage = 0;

        if (getConfiguration().getDietOutputMetric().equalsIgnoreCase("size")) {
            for (int i = 1; i < nbStages; i++) {
                if (getLength() >= thresholdTab[i - 1]) {
                    dietOutputStage++;
                }
            }
        } else if (getConfiguration().getDietOutputMetric().equalsIgnoreCase("age")) {
            for (int i = 1; i < nbStages; i++) {
                int tempAge = Math.round(thresholdTab[i - 1] * getConfiguration().getNumberTimeStepsPerYear());
                if (getLength() >= tempAge) {
                    dietOutputStage++;
                }
            }
        }
    }

    /*
     * Get the current Osmose instance
     */
    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
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

    /**
     * @return the ndeadFishing
     */
    public double getNdeadFishing() {
        return ndeadFishing;
    }

    /**
     * @param ndeadFishing the nDeadFishing to set
     */
    public void setNdeadFishing(double ndeadFishing) {
        this.ndeadFishing = ndeadFishing;
        ndeadHasChanged = true;
    }

    /**
     * @return the ndeadPredation
     */
    public double getNdeadPredation() {
        return ndeadPredation;
    }

    /**
     * @param ndeadPredation the nDeadPredation to set
     */
    public void resetNdeadPredation() {
        ndeadPredation = 0;
        ndeadHasChanged = true;
    }
    
    public void incrementNdeadPredation(double ndead) {
        ndeadPredation += ndead;
        ndeadHasChanged = true;
    }

    /**
     * @return the ndeadStarvation
     */
    public double getNdeadStarvation() {
        return ndeadStarvation;
    }

    /**
     * @param ndeadStarvation the nDeadStarvation to set
     */
    public void setNdeadStarvation(double ndeadStarvation) {
        this.ndeadStarvation = ndeadStarvation;
        ndeadHasChanged = true;
    }

    /**
     * @return the ndeadNatural
     */
    public double getNdeadNatural() {
        return ndeadNatural;
    }

    /**
     * @param ndeadNatural the nDeadNatural to set
     */
    public void setNdeadNatural(double ndeadNatural) {
        this.ndeadNatural = ndeadNatural;
        ndeadHasChanged = true;
    }
}
