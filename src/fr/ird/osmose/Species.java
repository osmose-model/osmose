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
     * List of the cohorts
     */
    private Cohort[] tabCohorts;
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
    /*
     * Mortality rates Stages: 1. eggs & larvae 2. Pre-recruits 3. Recruits
     * Mortality causes: 1. predation 2. starvation 3. natural 4. fishing
     */
    double[][] nDead, mortalityRate;
    double[] abundanceStage;
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
        tabCohorts = new Cohort[nbCohorts];

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

    public Cohort getCohort(int classAge) {
        return tabCohorts[classAge];
    }

    public void setCohort(int classAge, Cohort cohort) {
        tabCohorts[classAge] = cohort;
    }

    public int getNumberCohorts() {
        return nbCohorts;
    }

    public School getSchool(int classAge, int indexSchool) {
        return tabCohorts[classAge].get(indexSchool);
    }

    public List<School> getSchools() {
        List<School> schools = new ArrayList();
        for (Cohort cohort : tabCohorts) {
            schools.addAll(cohort);
        }
        return schools;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public void updateAbundancePerStages() {
        abundanceStage = new double[3];
        // Eggs & larvae
        for (School school : tabCohorts[0]) {
            abundanceStage[0] += school.getAbundance();
        }
        // Pre-recruits
        int indexRecruitAge = Math.round(recruitAge * getSimulation().getNbTimeStepsPerYear());
        for (int j = 1; j < indexRecruitAge; j++) {
            for (School school : tabCohorts[j]) {
                abundanceStage[1] += school.getAbundance();
            }
        }
        // Recruits
        for (int j = indexRecruitAge; j < tabCohorts.length; j++) {
            for (School school : tabCohorts[j]) {
                abundanceStage[2] += school.getAbundance();
            }
        }
    }

    public void computeMortalityRates() {
        mortalityRate = new double[4][3];
        nDead = new double[4][3];
        // Update number od deads
        // Eggs & larvae
        for (School school : tabCohorts[0]) {
            nDead[0][0] += school.nDeadPredation;
            nDead[1][0] += school.nDeadStarvation;
            nDead[2][0] += school.nDeadNatural;
            nDead[3][0] += school.nDeadFishing;
        }
        // Pre-recruits
        int indexRecruitAge = Math.round(recruitAge * getSimulation().getNbTimeStepsPerYear());
        for (int j = 1; j < indexRecruitAge; j++) {
            for (School school : tabCohorts[j]) {
                nDead[0][1] += school.nDeadPredation;
                nDead[1][1] += school.nDeadStarvation;
                nDead[2][1] += school.nDeadNatural;
                nDead[3][1] += school.nDeadFishing;
            }
        }
        // Recruits
        for (int j = indexRecruitAge; j < tabCohorts.length; j++) {
            for (School school : tabCohorts[j]) {
                nDead[0][2] += school.nDeadPredation;
                nDead[1][2] += school.nDeadStarvation;
                nDead[2][2] += school.nDeadNatural;
                nDead[3][2] += school.nDeadFishing;
            }
        }

        // Compute total mortality rate
        for (int iStage = 0; iStage < 3; iStage++) {
            double nDeadTot = 0;
            for (int iDeath = 0; iDeath < 4; iDeath++) {
                nDeadTot += nDead[iDeath][iStage];
            }
            double Ftot = Math.log(abundanceStage[iStage] / (abundanceStage[iStage] - nDeadTot));
            for (int iDeath = 0; iDeath < 4; iDeath++) {
                mortalityRate[iDeath][iStage] = Ftot * nDead[iDeath][iStage] / ((1 - Math.exp(-Ftot)) * abundanceStage[iStage]);
            }
        }
    }

    public boolean isReproduceLocally() {
        return reproduceLocally;
    }

    /*
     * phv 2011/11/22 Created new function for modeling incoming flux of biomass
     * for species that do not reproduce in the simulated domain.
     */
    public void incomingFlux() {

        /*
         * Making cohorts going up to the upper age class
         */
        for (int i = nbCohorts - 1; i > ageMeanIn; i--) {
            getCohort(i).clear();
            getCohort(i).addAll(tabCohorts[i - 1]);
            for (int j = 0; j < getCohort(i).size(); j++) {
                getSchool(i, j).age += 1;
            }
        }
        /*
         * Reset all cohorts younger than ageMeanIn
         */
        for (int i = ageMeanIn - 1; i > 0; i--) {
            tabCohorts[i].clear();
        }
        /*
         * Incoming flux
         */
        double biomassIn = biomassFluxIn * seasonSpawning[getSimulation().getIndexTimeYear()];
        float meanWeigthIn = (float) (c * Math.pow(meanLengthIn, bPower));
        long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
        tabCohorts[ageMeanIn].clear();
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (abundanceIn > 0 && abundanceIn < nbSchools) {
            tabCohorts[ageMeanIn].add(new School(this, abundanceIn, meanLengthIn, meanWeigthIn, ageMeanIn));
        } else if (abundanceIn >= nbSchools) {
            int mod = (int) (abundanceIn % nbSchools);
            int abdSchool = (int) (abundanceIn / nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                abdSchool += (i < mod) ? 1 : 0;
                tabCohorts[ageMeanIn].add(new School(this, abdSchool, meanLengthIn, meanWeigthIn, ageMeanIn));
            }
        }
        //System.out.println(name + " incoming flux " + biomassIn + " [tons] + ageIn: " + ageMeanIn);
    }

    public void reproduce() {
        //CALCULATION of Spawning Stock Biomass (SSB)
        double SSB = 0;
        float tempTL = 0f;
        int indexMin = 0;
        List<School> tabSchoolsRanked = sortSchoolsByLength();
        while ((indexMin < tabSchoolsRanked.size())
                && (tabSchoolsRanked.get(indexMin).getLength() < sizeMat)) {
            indexMin++;
        }
        for (int i = indexMin; i < tabSchoolsRanked.size(); i++) {
            SSB += tabSchoolsRanked.get(i).getBiomass();
            tempTL += tabSchoolsRanked.get(i).trophicLevel[tabSchoolsRanked.get(i).getAgeDt()] * tabSchoolsRanked.get(i).getBiomass();
        }

        double season = seasonSpawning.length > getSimulation().getNbTimeStepsPerYear()
                ? seasonSpawning[getSimulation().getIndexTimeSimu()]
                : seasonSpawning[getSimulation().getIndexTimeYear()];
        double nbEggs = sexRatio * alpha * season * SSB * 1000000;

        //MAKING COHORTS GOING UP to the UPPER AGE CLASS
        //species, age, caseLeftUpAireCoh, tabCasesAireCoh do not change
        for (int i = nbCohorts - 1; i > 0; i--) {
            getCohort(i).clear();
            getCohort(i).addAll(tabCohorts[i - 1]);
            for (int j = 0; j < getCohort(i).size(); j++) {
                getSchool(i, j).age += 1;
            }
        }

        //UPDATE AGE CLASS 0
        Cohort coh0 = tabCohorts[0];
        coh0.clear();
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (nbEggs == 0.d) {
            // do nothing, zero school
        } else if (nbEggs < nbSchools) {
            coh0.add(new School(this, nbEggs, eggSize, eggWeight, 0));
        } else if (nbEggs >= nbSchools) {
            coh0.ensureCapacity(nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                coh0.add(new School(this, nbEggs / nbSchools, eggSize, eggWeight, 0));
            }
        }
        coh0.trimToSize();
        for (int i = 0; i < coh0.size(); i++) {
            ((School) coh0.get(i)).trophicLevel[0] = TL_EGG; //tempTL/(float)SSB;
        }
    }

    public void update() {

        for (int i = 0; i < nbCohorts; i++) {
            for (int j = 0; j < getCohort(i).size(); j++) {
                getSchool(i, j).updateFeedingStage(sizeFeeding, nbFeedingStages);
                getSchool(i, j).updateAccessStage(getOsmose().accessStageThreshold[index], getOsmose().nbAccessStage[index]);
            }
        }

        if (!(nbCohorts == (int) Math.round((longevity) * getSimulation().getNbTimeStepsPerYear()))) {
            nbCohorts = (int) Math.round((longevity + 1) * getSimulation().getNbTimeStepsPerYear());
            System.out.println("PB of number of cohorts at the update stage: not equals to the initial number of cohorts");
            // Morgane 03-2007
        }
    }

    /**
     * Sort all the schools of this species according to their length.
     *
     * @return a List of the Schools of this species sorted by length.
     */
    public List<School> sortSchoolsByLength() {

        List<School> schools = new ArrayList();
        for (Cohort cohort : tabCohorts) {
            schools.addAll(cohort);
        }
        Collections.sort(schools, new SchoolLengthComparator());
        return schools;
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
