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
        this.name = getConfiguration().speciesName[index];
        this.D = getConfiguration().D[index];
        this.lInf = getConfiguration().lInf[index];
        this.K = getConfiguration().K[index];
        this.t0 = getConfiguration().t0[index];
        this.c = getConfiguration().c[index];
        this.bPower = getConfiguration().bPower[index];
        this.sizeMat = getConfiguration().sizeMaturity[index];
        this.nbFeedingStages = getConfiguration().nFeedingStage[index];
        this.sizeFeeding = getConfiguration().feedingStageThreshold[index];
        this.recruitAge = Math.round(getConfiguration().recruitmentAge[index] * getConfiguration().getNumberTimeStepsPerYear());
        this.seasonSpawning = getConfiguration().seasonSpawning[index];
        this.indexAgeClass0 = (int) Math.ceil(getConfiguration().supAgeOfClass0Matrix[index] * getConfiguration().getNumberTimeStepsPerYear());      // index of supAgeOfClass0 used in tabCohorts table
        this.eggSize = getConfiguration().eggSize[index];
        this.eggWeight = getConfiguration().eggWeight[index];
        this.growthAgeThreshold = getConfiguration().growthAgeThreshold[index];

        this.nbAccessStages = getConfiguration().nAccessStage[index];
        this.ageStagesTab = getConfiguration().accessStageThreshold[index];
        if (getConfiguration().outputDiet) {
            this.dietStagesTab = getConfiguration().dietStageThreshold[index];
            this.nbDietStages = getConfiguration().nDietStage[index];
        }

        // START INITIALISATION of COHORTS
        longevity = (int) Math.round((getConfiguration().speciesLongevity[index]) * getConfiguration().getNumberTimeStepsPerYear());
    }

    public float[] getMeanLength() {

        // INITIALISATION of TAB for LENGTH and MINMAX of DELTA LENGTH
        float[] meanLength = new float[longevity];

        float decimalAge;
        meanLength[0] = eggSize;

        for (int i = 1; i < getLongevity(); i++) {
            decimalAge = i / (float) getConfiguration().getNumberTimeStepsPerYear();
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

    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
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
