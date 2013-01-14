package fr.ird.osmose;

/**
 * ******************************************************************************
 * <p>Titre : Species class</p>
 *
 * <p>Description : groups species specificities - biological processes </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * *******************************************************************************
 */

public class Species {

    final static public float TL_EGG = 3f;
///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /*
     * ******************************
     * * Description of the species * ******************************
     */
    /*
     * Index of the species [0 : numberTotalSpecies - 1]
     */
    private int index;
    /*
     * Name of the species
     */
    private String name;
    /*
     * Longevity expressed in number of time steps
     */
    private int longevity;
    /*
     * ***************************
     * * Life history parameters * ***************************
     */
    float D;//D0;		//mortality rates year-1
    float[] fishingRates;
    
    float lInf, K, t0, c, bPower;	//von bertalanffy growth parameters
    float alpha;  		//nb of eggs per gram of mature female
    float sizeMat;
    int recruitAge;            //year
    /*
     * Age from which the species biomass-0 is calculated, expressed in dt
     */
    int indexAgeClass0;
    float recruitSize;
    float[] larvalMortalityRates;
    float[] seasonSpawning; //according to nbDt
    float sexRatio, eggSize, eggWeight, growthAgeThreshold,
            predationRate, criticalPredSuccess, starvMaxRate;
    float[] predPreySizesMax, predPreySizesMin;
    int nbFeedingStages;  // stage indirectly correponds to size classes:
    float[] sizeFeeding;
    int nbAccessStages;
    float[] ageStagesTab;
    int nbDietStages;
    float[] dietStagesTab;
    private boolean reproduceLocally;
    float biomassFluxIn;
    float meanLengthIn;
    int ageMeanIn;
    // Migration
    private float[][] outOfZoneMortality;
    private boolean[][] outOfZoneCohort;
    
    /**
     * Create a new species
     *
     * @param number, an integer, the number of the species {1 : nbTotSpecies}
     */
    public Species(int number) {
        index = number - 1;
    }
    
    public Species(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /*
     * Initialize the parameters of the species
     */
    public void init() {

        int numSerie = getOsmose().numSerie;

        // INITIALISATION of PARAM
        this.name = getOsmose().nameSpecMatrix[numSerie][index];
        this.D = getOsmose().DMatrix[numSerie][index];
        this.fishingRates = getOsmose().fishingRates[index];
        this.lInf = getOsmose().lInfMatrix[numSerie][index];
        this.K = getOsmose().KMatrix[numSerie][index];
        this.t0 = getOsmose().t0Matrix[numSerie][index];
        this.c = getOsmose().cMatrix[numSerie][index];
        this.bPower = getOsmose().bPowerMatrix[numSerie][index];
        this.alpha = getOsmose().alphaMatrix[numSerie][index];
        this.sizeMat = getOsmose().sizeMatMatrix[numSerie][index];
        this.nbFeedingStages = getOsmose().nbStagesMatrix[numSerie][index];
        this.sizeFeeding = getOsmose().sizeFeedingMatrix[numSerie][index];
        this.recruitAge = Math.round(getOsmose().recruitAgeMatrix[numSerie][index] * getSimulation().getNbTimeStepsPerYear());
        this.recruitSize = getOsmose().recruitSizeMatrix[numSerie][index];
        this.seasonSpawning = getOsmose().seasonSpawningMatrix[numSerie][index];
        this.indexAgeClass0 = (int) Math.ceil(getOsmose().supAgeOfClass0Matrix[numSerie][index] * getSimulation().getNbTimeStepsPerYear());      // index of supAgeOfClass0 used in tabCohorts table
        this.larvalMortalityRates = new float[getSimulation().getNbTimeStepsPerYear() * getOsmose().simulationTimeTab[numSerie]];
        int t = 0;
        for (int iStep = 0; iStep < larvalMortalityRates.length; iStep++) {
            if (t > getOsmose().larvalMortalityRates[index].length - 1) {
                t = 0;
            }
            larvalMortalityRates[iStep] = getOsmose().larvalMortalityRates[index][t];
            t++;
        }
        this.sexRatio = getOsmose().sexRatioMatrix[numSerie][index];
        this.eggSize = getOsmose().eggSizeMatrix[numSerie][index];
        this.eggWeight = getOsmose().eggWeightMatrix[numSerie][index];
        this.growthAgeThreshold = getOsmose().growthAgeThresholdMatrix[numSerie][index];

        this.predationRate = getOsmose().predationRateMatrix[numSerie][index];
        this.predPreySizesMax = getOsmose().predPreySizesMaxMatrix[numSerie][index];
        this.predPreySizesMin = getOsmose().predPreySizesMinMatrix[numSerie][index];
        this.criticalPredSuccess = getOsmose().criticalPredSuccessMatrix[numSerie][index];
        this.starvMaxRate = getOsmose().starvMaxRateMatrix[numSerie][index];
        this.nbAccessStages = getOsmose().nbAccessStage[index];
        this.ageStagesTab = getOsmose().accessStageThreshold[index];
        if (getOsmose().dietsOutputMatrix[getOsmose().numSerie]) {
            this.dietStagesTab = getOsmose().dietStageThreshold[numSerie][index];
            this.nbDietStages = getOsmose().nbDietsStages[numSerie][index];
        }
        /*
         * phv 2011/11/21 Added new parameters for species reproducing outside
         * the simulated area.
         */
        this.reproduceLocally = getOsmose().reproduceLocallyTab[numSerie][index];
        this.biomassFluxIn = getOsmose().biomassFluxInTab[numSerie][index];
        this.meanLengthIn = getOsmose().meanLengthFishInTab[numSerie][index];
        this.ageMeanIn = (int) Math.round(getOsmose().meanAgeFishInTab[numSerie][index] * getSimulation().getNbTimeStepsPerYear());
        //System.out.println(name + " reproLocal ? " + reproduceLocally + " biomassIn: " + biomassFluxIn + " lengthIn: " + meanLengthIn + " ageIn: " + ageMeanIn);

        // START INITIALISATION of COHORTS
        longevity = (int) Math.round((getOsmose().longevityMatrix[numSerie][index]) * getSimulation().getNbTimeStepsPerYear());

        // migration
        outOfZoneMortality = new float[longevity][getSimulation().getNbTimeStepsPerYear()];
        outOfZoneCohort = new boolean[longevity][getSimulation().getNbTimeStepsPerYear()];
    }
    
    public float[] getMeanLength() {

        // INITIALISATION of TAB for LENGTH and MINMAX of DELTA LENGTH
        float[] meanLength = new float[longevity];

        float decimalAge;
        meanLength[0] = eggSize;

        for (int i = 1; i < getLongevity(); i++) {
            decimalAge = i / (float) getSimulation().getNbTimeStepsPerYear();
            if (decimalAge < growthAgeThreshold) {
                float lengthAtAgePart = (float) (lInf * (1 - Math.exp(-K * (growthAgeThreshold - t0))));
                if (lengthAtAgePart < eggSize) {
                    lengthAtAgePart = eggSize;
                }
                meanLength[i] = decimalAge * (float) (lengthAtAgePart - eggSize) + eggSize;    // linear growth for the 1st year as von Bertalanffy is not well adapted for the 1st year
            } else {
                meanLength[i] = (float) (lInf * (1 - Math.exp(-K * (decimalAge - t0))));   // von Bertalnffy growth after the first year
            }
        }
        return meanLength;
    }

    public float[] getMeanWeight(float[] tabMeanLength) {

        float[] meanWeight = new float[longevity];
        meanWeight[0] = eggWeight;
        for (int i = 1; i < longevity; i++) {
            meanWeight[i] = (float) (c * (Math.pow(tabMeanLength[i], bPower)));
            if (meanWeight[i] < eggWeight) {
                meanWeight[i] = eggWeight;
            }
        }
        return meanWeight;
    }

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    public int getLongevity() {
        return longevity;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public boolean isReproduceLocally() {
        return reproduceLocally;
    }

    public boolean isOut(int age, int indexTime) {
        return outOfZoneCohort[age][indexTime];
    }

    public void setOut(int age, int indexTime, boolean isOut) {
        outOfZoneCohort[age][indexTime] = isOut;
    }

    public float getOutMortality(int age, int indexTime) {
        return outOfZoneMortality[age][indexTime];
    }

    public void setOutMortality(int age, int indexTime, float mortality) {
        outOfZoneMortality[age][indexTime] = mortality;
    }
}
