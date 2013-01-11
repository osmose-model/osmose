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
import fr.ird.osmose.util.SchoolLengthComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Species extends ArrayList<School> {

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
     * Number of the cohorts = (int) Math.round((longevity + 1) *
     * simulation.getNbTimeSteps()
     */
    private int nbCohorts;
    /*
     * ***************************
     * * Life history parameters * ***************************
     */
    float D;//D0;		//mortality rates year-1
    float[] fishingRates;
    float F;
    float longevity;              //in years
    float lInf, K, t0, c, bPower;	//von bertalanffy growth parameters
    float alpha;  		//nb of eggs per gram of mature female
    float sizeMat;
    float recruitAge;            //year
    float supAgeOfClass0;        // year
    int indexAgeClass0;          // index for the table tabCohort, in nbDt
    float recruitSize;
    float larvalSurvival;
    float[] larvalMortalityRates;
    float[] seasonSpawning; //according to nbDt
    float sexRatio, eggSize, eggWeight, growthAgeThreshold,
            predationRate, criticalPredSuccess, starvMaxRate;
    float[] predPreySizesMax, predPreySizesMin;
    float[] tabMeanWeight;      //tab of mean weights at nbDt*ages
    float[] tabMeanLength;	//tab of mean lengths at nbDt*ages
    float[] minDelta;
    float[] maxDelta;
    float[] deltaMeanLength;
    long[] tabAbdIni;	//tab of abd for initializing the simulation nbDt*age
    double[] tabBiomIni;
    int nbFeedingStages;  // stage indirectly correponds to size classes:
    float[] sizeFeeding;
    int nbAccessStages;
    float[] ageStagesTab;
    int nbDietStages;
    float[] dietStagesTab;
    private boolean reproduceLocally;
    private float biomassFluxIn;
    private float meanLengthIn;
    private int ageMeanIn;
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

    /*
     * Initialize the parameters of the species
     */
    public void init() {

        int numSerie = getOsmose().numSerie;

        // INITIALISATION of PARAM
        this.name = getOsmose().nameSpecMatrix[numSerie][index];
        this.D = getOsmose().DMatrix[numSerie][index];
        this.fishingRates = getOsmose().fishingRates[index];
        this.longevity = getOsmose().longevityMatrix[numSerie][index];
        this.lInf = getOsmose().lInfMatrix[numSerie][index];
        this.K = getOsmose().KMatrix[numSerie][index];
        this.t0 = getOsmose().t0Matrix[numSerie][index];
        this.c = getOsmose().cMatrix[numSerie][index];
        this.bPower = getOsmose().bPowerMatrix[numSerie][index];
        this.alpha = getOsmose().alphaMatrix[numSerie][index];
        this.sizeMat = getOsmose().sizeMatMatrix[numSerie][index];
        this.nbFeedingStages = getOsmose().nbStagesMatrix[numSerie][index];
        this.sizeFeeding = getOsmose().sizeFeedingMatrix[numSerie][index];
        this.recruitAge = getOsmose().recruitAgeMatrix[numSerie][index];
        this.recruitSize = getOsmose().recruitSizeMatrix[numSerie][index];
        this.seasonSpawning = getOsmose().seasonSpawningMatrix[numSerie][index];
        this.supAgeOfClass0 = getOsmose().supAgeOfClass0Matrix[numSerie][index];//age from which the species biomass-0 is calculated
        this.indexAgeClass0 = (int) Math.ceil(supAgeOfClass0 * getSimulation().getNbTimeStepsPerYear());      // index of supAgeOfClass0 used in tabCohorts table
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
        nbCohorts = (int) Math.round((longevity) * getSimulation().getNbTimeStepsPerYear());

        tabAbdIni = new long[nbCohorts];
        tabBiomIni = new double[nbCohorts];

        // INITIALISATION of TAB for LENGTH and MINMAX of DELTA LENGTH
        tabMeanLength = new float[nbCohorts];
        tabMeanWeight = new float[nbCohorts];

        float decimalAge;
        tabMeanLength[0] = eggSize;
        tabMeanWeight[0] = eggWeight;

        for (int i = 1; i < nbCohorts; i++) {
            decimalAge = i / (float) getSimulation().getNbTimeStepsPerYear();
            if (decimalAge < growthAgeThreshold) {
                float lengthAtAgePart = (float) (lInf * (1 - Math.exp(-K * (growthAgeThreshold - t0))));
                if (lengthAtAgePart < eggSize) {
                    lengthAtAgePart = eggSize;
                }
                tabMeanLength[i] = decimalAge * (float) (lengthAtAgePart - eggSize) + eggSize;    // linear growth for the 1st year as von Bertalanffy is not well adapted for the 1st year
            } else {
                tabMeanLength[i] = (float) (lInf * (1 - Math.exp(-K * (decimalAge - t0))));   // von Bertalnffy growth after the first year
            }
            tabMeanWeight[i] = (float) (c * (Math.pow(tabMeanLength[i], bPower)));
            if (tabMeanWeight[i] < eggWeight) {
                tabMeanWeight[i] = eggWeight;
            }
        }

        minDelta = new float[nbCohorts];
        maxDelta = new float[nbCohorts];
        deltaMeanLength = new float[nbCohorts];

        for (int i = 0; i < nbCohorts - 1; i++) {
            deltaMeanLength[i] = tabMeanLength[i + 1] - tabMeanLength[i];

            minDelta[i] = deltaMeanLength[i] - deltaMeanLength[i];
            maxDelta[i] = deltaMeanLength[i] + deltaMeanLength[i];
        }

        /*
         * phv 2012/11/08 - Careful, F the annual mortality rate is calculated
         * as the annual average of the fishing rates over the years.
         * F is still used in Simulation.iniBySpeciesBiomass
         */
        for (int iStep = 0; iStep < fishingRates.length; iStep++) {
            F += fishingRates[iStep];
        }
        if (getSimulation().isFishingInterannual) {
            F /= getOsmose().simulationTimeTab[numSerie];
        }

        /*
         * phv 2012/11/08 - Careful, larvalSurvival the annual mortality rate
         * is calculated as the annual average of the larval mortality rates
         * over the years.
         * larvalSurvival is still used in Simulation.iniBySpeciesBiomass
         */
        for (int iStep = 0; iStep < larvalMortalityRates.length; iStep++) {
            larvalSurvival += larvalMortalityRates[iStep];
        }
        larvalSurvival /= larvalMortalityRates.length;
        //System.out.println("Species " + name + " larval mortality " + larvalSurvival);

        // migration
        outOfZoneMortality = new float[nbCohorts][getSimulation().getNbTimeStepsPerYear()];
        outOfZoneCohort = new boolean[nbCohorts][getSimulation().getNbTimeStepsPerYear()];
    }

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    public int getNumberCohorts() {
        return nbCohorts;
    }

    public List<School> getSchools() {
        return this;
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

    public void removeDeadSchools() {
        List<School> schoolsToRemove = new ArrayList();
        for (School school : this) {
            if (school.willDisappear()) {
                if (!school.isUnlocated()) {
                    school.getCell().remove(school);
                }
                schoolsToRemove.add(school);
            }
        }
        removeAll(schoolsToRemove);
    }

    /*
     * phv 2011/11/22 Created new function for modeling incoming flux of biomass
     * for species that do not reproduce in the simulated domain.
     */
    public void incomingFlux() {

        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : this) {
            school.age += 1;
            if (school.getAgeDt() > (nbCohorts - 1)) {
                school.tagForRemoval();
            }
        }
        removeDeadSchools();

        /*
         * Incoming flux
         */
        double biomassIn = biomassFluxIn * seasonSpawning[getSimulation().getIndexTimeYear()];
        float meanWeigthIn = (float) (c * Math.pow(meanLengthIn, bPower));
        long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (abundanceIn > 0 && abundanceIn < nbSchools) {
            add(new School(this, abundanceIn, meanLengthIn, meanWeigthIn, ageMeanIn));
        } else if (abundanceIn >= nbSchools) {
            int mod = (int) (abundanceIn % nbSchools);
            int abdSchool = (int) (abundanceIn / nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                abdSchool += (i < mod) ? 1 : 0;
                add(new School(this, abdSchool, meanLengthIn, meanWeigthIn, ageMeanIn));
            }
        }
        //System.out.println(name + " incoming flux " + biomassIn + " [tons] + ageIn: " + ageMeanIn);
    }

    public void reproduce() {
        //CALCULATION of Spawning Stock Biomass (SSB)
        double SSB = 0;
        float tempTL = 0f;
        int indexMin = 0;
        Collections.sort(this, new SchoolLengthComparator());
        while ((indexMin < size())
                && (get(indexMin).getLength() < sizeMat)) {
            indexMin++;
        }
        for (int i = indexMin; i < size(); i++) {
            SSB += get(i).getBiomass();
            tempTL += get(i).trophicLevel * get(i).getBiomass();
        }

        double season = seasonSpawning.length > getSimulation().getNbTimeStepsPerYear()
                ? seasonSpawning[getSimulation().getIndexTimeSimu()]
                : seasonSpawning[getSimulation().getIndexTimeYear()];
        double nbEggs = sexRatio * alpha * season * SSB * 1000000;

        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : this) {
            school.age += 1;
            if (school.getAgeDt() > (nbCohorts - 1)) {
                school.tagForRemoval();
            }
        }
        removeDeadSchools();

        //UPDATE AGE CLASS 0
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (nbEggs == 0.d) {
            // do nothing, zero school
        } else if (nbEggs < nbSchools) {
            School school0 = new School(this, nbEggs, eggSize, eggWeight, 0);
            add(school0);
        } else if (nbEggs >= nbSchools) {
            for (int i = 0; i < nbSchools; i++) {
                School school0 = new School(this, nbEggs / nbSchools, eggSize, eggWeight, 0);
                add(school0);
            }
        }
    }

    public void update() {
        for (School school : this) {
            school.trophicLevel = school.tmpTL;
            school.updateFeedingStage(sizeFeeding, nbFeedingStages);
            school.updateAccessStage(getOsmose().accessStageThreshold[index], getOsmose().nbAccessStage[index]);
        }
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
