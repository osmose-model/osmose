package fr.ird.osmose;

import fr.ird.osmose.process.PredationProcess;

/**
 * ******************************************************************************
 * <p>Titre : School class</p>
 *
 * <p>Description : Basic unit of Osmose model - represents a super-individual
 * </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * *******************************************************************************
 */
public class School extends Fish {

///////////////////////////////
// Declaration of the variables
///////////////////////////////

    /*
     * Number of individuals in the school
     */
    private double abundance;
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
        super(species);
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
}
