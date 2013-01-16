package fr.ird.osmose;

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
    
    float[][] dietTemp;
    private float sumDiet;
    //
    double nDeadFishing;
    double nDeadPredation;
    double nDeadStarvation;
    double nDeadNatural;
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
    boolean hasPredated;

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

        // initialize school variables
        init();
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /*
     * Initialize the school state variables
     */
    private void init() {
        alive = true;
        catchable = true;
        trophicLevel = Species.TL_EGG;

        /*
         * Initialisation of stage
         */
        updateFeedingStage(species.sizeFeeding, species.nbFeedingStages);
        updateAccessStage(species.ageStagesTab, species.nbAccessStages);
        updateDietOutputStage(species.dietStagesTab, species.nbDietStages);

        dietTemp = new float[getSimulation().getNbSpecies() + getSimulation().getForcing().getNbPlanktonGroups()][];
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            dietTemp[i] = new float[getSimulation().getSpecies(i).nbDietStages];
        }
        for (int i = getSimulation().getNbSpecies(); i < getSimulation().getNbSpecies() + getSimulation().getForcing().getNbPlanktonGroups(); i++) {
            dietTemp[i] = new float[]{0.f};
        }
        sumDiet = 0;

        /*
         * phv 2011/11/22 This booleans means that the scool has been created
         * but not located anywhere in the grid. unlocated will be set to false
         * when we first call communicatePosition
         * phv 2013/01/08 Replaced the boolean by negative coordinates for the
         * school.
         */
        setOffGrid();
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

    public void updateDietVariables() {
        if ((getOsmose().isDietOuput()) && (getSimulation().getYear() >= getOsmose().timeSeriesStart)) {
            for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                    sumDiet += dietTemp[i][s];
                }
            }
            for (int i = getSimulation().getNbSpecies(); i < getSimulation().getNbSpecies() + getSimulation().getForcing().getNbPlanktonGroups(); i++) {
                sumDiet += dietTemp[i][0];
            }
        }
    }

    public void resetDietVariables() {
        sumDiet = 0;
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                dietTemp[i][s] = 0;
            }
        }
        for (int i = getSimulation().getNbSpecies(); i < getSimulation().getNbSpecies() + getSimulation().getForcing().getNbPlanktonGroups(); i++) {
            dietTemp[i][0] = 0;
        }
    }

    /**
     * @return the abundance
     */
    public double getAbundance() {
        return abundance;
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

    public float getSumDiet() {
        return sumDiet;
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("School");
        str.append( "\n  Species: ");
        str.append(getSpecies().getName());
        str.append("\n  Cohort: ");
        float ageInYear = getAgeDt() / (float) getSimulation().getNumberTimeStepsPerYear();
        str.append(ageInYear);
        str.append(" [year]");
        str.append("\n  Cell: ");
        str.append(getCell().getIndex());
        return str.toString();
    }
}
