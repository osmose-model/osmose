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

public class School {

///////////////////////////////
// Declaration of the variables
///////////////////////////////

    /*
     * The Cell where is located the School
     */
    private Cell cell;
    /*
     * Cohort of the School
     */
    private Cohort cohort;
    /*
     * Species of the school
     */
    private Species species;
    /*
     * length of the individuals in the school in centimeters
     */
    private float length;
    /*
     * weight of individual of the school in grams
     */
    private float weight;
    /*
     * Table of the TL of this school at each time step
     */
    float[] trophicLevel;
    /*
     * Number of individuals in the school
     */
    private double abundance;
    /*
     * Predation success rate
     */
    float predSuccessRate;
    /*
     * whether the school will disappear at next time step
     */
    private boolean disappears;
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
    int dietOutputStage;
    float[][] dietTemp;
    private float sumDiet;
    private boolean unlocated;
    double catches;
    //
    double nDeadFishing;
    double nDeadPredation;
    double nDeadStarvation;
    double nDeadNatural;
    //
    double preyedBiomass;
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
    public School(Cohort cohort, double abundance, float length, float weight) {
        this.cohort = cohort;
        this.abundance = abundance;
        this.length = length;
        this.weight = weight;

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
        species = cohort.getSpecies();
        disappears = false;
        catchable = true;
        /*
         * Initialisation TLs trophicLevel = table of the TL of this school at
         * each time step
         */
        trophicLevel = new float[species.getNumberCohorts()];

        /*
         * Initialisation of the previous age because predation is based on TL
         * at the previous time step
         */
        if (cohort.getAgeNbDt() == 0) {
            /*
             * Egg stage
             */
            trophicLevel[0] = species.TLeggs;
        }
        if (cohort.getAgeNbDt() != 0) {
            /*
             * 2011/04/11 phv : do not understand since TLeggs = 3 as well...
             */
            trophicLevel[cohort.getAgeNbDt() - 1] = 3f;
        }
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
         */
        unlocated = true;
    }
    /*
     * Get the current Osmose instance
     */

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    /*
     * Get current Simulation
     */
    private Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    /**
     * Gets the current location of the school
     *
     * @return the cell where is located the school
     */
    public Cell getCell() {
        return cell;
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
    
    public void moveToCell(Cell cell) {
        this.cell = cell;
    }

    /**
     * Link the school and the cell where it is located.
     */
    public void communicatePosition() {
        cell.add(this);
        unlocated = false;
    }

    public boolean isUnlocated() {
        return unlocated;
    }

    /*
     * This method should be called when a school is momentarily out of the
     * simulated area.
     */
    public void moveOut() {
        if (!unlocated) {
            cell.remove(this);
            unlocated = true;
        }
    }

    public void updateAccessStage(float[] ageStages, int nbAccessStages) {
        accessibilityStage = 0;
        for (int i = 1; i < nbAccessStages; i++) {
            if (getCohort().getAgeNbDt() >= ageStages[i - 1]) {
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

    public void growth(float minDelta, float maxDelta, float c, float bPower) {

        //calculation of lengths according to predation efficiency
        if (predSuccessRate >= getCohort().getSpecies().criticalPredSuccess) {
            setLength(getLength() + minDelta + (maxDelta - minDelta) * ((predSuccessRate - getCohort().getSpecies().criticalPredSuccess) / (1 - getCohort().getSpecies().criticalPredSuccess)));
            setWeight((float) (c * Math.pow(getLength(), bPower)));
        }

        //		updateTL(previousW,weight);
        //updateTLbis();
    }

//    public void updateTL(float previousW, float W) {
//        float previousTL, newTL;
//        if (getCohort().getAgeNbDt() != 0) {
//            previousTL = trophicLevel[getCohort().getAgeNbDt() - 1];
//        } else {
//            previousTL = getCohort().getSpecies().TLeggs;
//        }
//
//        newTL = ((previousW * previousTL) + ((W - previousW) * trophicLevel[getCohort().getAgeNbDt()])) / (W);   // weighting of new TL according to increase of weight dut to prey ingestion
//        trophicLevel[getCohort().getAgeNbDt()] = newTL;
//    }
    public void updateTLbis() {
        float[] TLproie = new float[4];
        float[] deltaW = new float[4];
        if (getCohort().getAgeNbDt() == 0) {
            deltaW[0] = 0;
            deltaW[1] = 0;
            deltaW[2] = 0;
            deltaW[3] = predSuccessRate;
            TLproie[0] = 0;
            TLproie[1] = 0;
            TLproie[2] = 0;
            TLproie[3] = trophicLevel[getCohort().getAgeNbDt()];
        } else {
            if (getCohort().getAgeNbDt() == 1) {
                deltaW[0] = 0;
                deltaW[1] = 0;
                deltaW[2] = deltaW[3];
                deltaW[3] = predSuccessRate;
                TLproie[0] = 0;
                TLproie[1] = 0;
                TLproie[2] = TLproie[3];
                TLproie[3] = trophicLevel[getCohort().getAgeNbDt()];
            } else {
                if (getCohort().getAgeNbDt() == 2) {
                    deltaW[0] = 0;
                    deltaW[1] = deltaW[2];
                    deltaW[2] = deltaW[3];
                    deltaW[3] = predSuccessRate;
                    TLproie[0] = 0;
                    TLproie[1] = TLproie[2];
                    TLproie[2] = TLproie[3];
                    TLproie[3] = trophicLevel[getCohort().getAgeNbDt()];
                } else {
                    deltaW[0] = deltaW[1];
                    deltaW[1] = deltaW[2];
                    deltaW[2] = deltaW[3];
                    deltaW[3] = predSuccessRate;
                    TLproie[0] = TLproie[1];
                    TLproie[1] = TLproie[2];
                    TLproie[2] = TLproie[3];
                    TLproie[3] = trophicLevel[getCohort().getAgeNbDt()];
                }
            }
        }
        if ((deltaW[3] + deltaW[2] + deltaW[1] + deltaW[0]) != 0) {
            trophicLevel[getCohort().getAgeNbDt()] = (deltaW[3] * TLproie[3] + deltaW[2] * TLproie[2] + deltaW[1] * TLproie[1] + deltaW[0] * TLproie[0]) / (deltaW[3] + deltaW[2] + deltaW[1] + deltaW[0]);
        } else {
            if (getCohort().getAgeNbDt() != 0) {
                trophicLevel[getCohort().getAgeNbDt()] = trophicLevel[getCohort().getAgeNbDt() - 1];
            } else {
                trophicLevel[getCohort().getAgeNbDt()] = getCohort().getSpecies().TLeggs;
            }
        }
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
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(float weight) {
        this.weight = weight;
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
     * @param trophicLevel the trophicLevel to set
     */
    public void setTrophicLevel(float[] trophicLevel) {
        this.trophicLevel = trophicLevel;
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

    /**
     * @return the cohort
     */
    public Cohort getCohort() {
        return cohort;
    }

    public void setCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    /**
     * @return whether the shcool will disappear at next time step
     */
    public boolean willDisappear() {
        return disappears;
    }

    /**
     * Tag the school as about to disappear
     */
    public void tagForRemoval() {
        disappears = true;
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

    public float getSumDiet() {
        return sumDiet;
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("School");
        str.append( "\n  Species: ");
        str.append(getCohort().getSpecies().getName());
        str.append("\n  Cohort: ");
        float ageInYear = getCohort().getAgeNbDt() / (float) getSimulation().getNbTimeStepsPerYear();
        str.append(ageInYear);
        str.append(" [year]");
        str.append("\n  Cell: ");
        str.append(cell.getIndex());
        return str.toString();
    }
}
