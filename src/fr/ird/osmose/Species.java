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
/////////////////////////////
// Description of the species
/////////////////////////////
    /*
     * Index of the species [0 : numberTotalSpecies - 1]
     */
    private int index;
    /*
     * Name of the species
     */
    private String name;
//////////////////////////
// Life history parameters
//////////////////////////
    /*
     * Longevity expressed in number of time steps
     */
    private int longevity;
    /*
     * Natural mortality rates year-1
     */
    public float D;
    /*
     * Von bertalanffy growth parameters
     */
    private float lInf, K, t0;
    /*
     * Allometric parameters
     */
    private float c, bPower;
    public float sizeMat;
    public int recruitAge;            //year
    /*
     * Age from which the species biomass-0 is calculated, expressed in dt
     */
    public int indexAgeClass0;
    float recruitSize;
    public float[] seasonSpawning; //according to nbDt
    public float eggSize, eggWeight, growthAgeThreshold;
    int nbFeedingStages;  // stage indirectly correponds to size classes:
    float[] sizeFeeding;
    int nbAccessStages;
    float[] ageStagesTab;
    public int nbDietStages;
    public float[] dietStagesTab;

    /**
     * Create a new species
     *
     * @param index, an integer, the index of the species {0 : nbTotSpecies - 1}
     */
    public Species(int index) {
        this.index = index;
    }

    public Species(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /*
     * Initialize the parameters of the species
     */
    public void init() {

        // INITIALISATION of PARAM
        this.name = getOsmose().nameSpecMatrix[index];
        this.D = getOsmose().DMatrix[index];
        this.lInf = getOsmose().lInfMatrix[index];
        this.K = getOsmose().KMatrix[index];
        this.t0 = getOsmose().t0Matrix[index];
        this.c = getOsmose().cMatrix[index];
        this.bPower = getOsmose().bPowerMatrix[index];
        this.sizeMat = getOsmose().sizeMatMatrix[index];
        this.nbFeedingStages = getOsmose().nbStagesMatrix[index];
        this.sizeFeeding = getOsmose().sizeFeedingMatrix[index];
        this.recruitAge = Math.round(getOsmose().recruitAgeMatrix[index] * getSimulation().getNumberTimeStepsPerYear());
        this.recruitSize = getOsmose().recruitSizeMatrix[index];
        this.seasonSpawning = getOsmose().seasonSpawningMatrix[index];
        this.indexAgeClass0 = (int) Math.ceil(getOsmose().supAgeOfClass0Matrix[index] * getSimulation().getNumberTimeStepsPerYear());      // index of supAgeOfClass0 used in tabCohorts table
        this.eggSize = getOsmose().eggSizeMatrix[index];
        this.eggWeight = getOsmose().eggWeightMatrix[index];
        this.growthAgeThreshold = getOsmose().growthAgeThresholdMatrix[index];

        this.nbAccessStages = getOsmose().nbAccessStage[index];
        this.ageStagesTab = getOsmose().accessStageThreshold[index];
        if (getOsmose().dietsOutputMatrix) {
            this.dietStagesTab = getOsmose().dietStageThreshold[index];
            this.nbDietStages = getOsmose().nbDietsStages[index];
        }

        // START INITIALISATION of COHORTS
        longevity = (int) Math.round((getOsmose().longevityMatrix[index]) * getSimulation().getNumberTimeStepsPerYear());
    }

    public float[] getMeanLength() {

        // INITIALISATION of TAB for LENGTH and MINMAX of DELTA LENGTH
        float[] meanLength = new float[longevity];

        float decimalAge;
        meanLength[0] = eggSize;

        for (int i = 1; i < getLongevity(); i++) {
            decimalAge = i / (float) getSimulation().getNumberTimeStepsPerYear();
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
            meanWeight[i] = (float) computeWeight(tabMeanLength[i]);
            if (meanWeight[i] < eggWeight) {
                meanWeight[i] = eggWeight;
            }
        }
        return meanWeight;
    }

    public float computeWeight(float length) {
        return (float) (c * (Math.pow(length, bPower)));
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
}
