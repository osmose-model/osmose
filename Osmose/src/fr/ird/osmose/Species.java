package fr.ird.osmose;

/********************************************************************************
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
 ******************************************************************************** 
 */
import fr.ird.osmose.util.SchoolLengthComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Species {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /*
     * ******************************
     * * Description of the species *
     * ******************************
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
     * Number of individuals of the species
     */
    private double abundance;
    /*
     * Total biomass in tons
     */
    private double biomass;
    /*
     * List of the cohorts
     */
    private Cohort[] tabCohorts;
    /*
     * Number of the cohorts = (int) Math.round((longevity + 1) * simulation.getNbTimeSteps()
     */
    private int nbCohorts;
    /*
     * **************
     * * Indicators *
     * **************
     * MORGANE 07-2004
     * Those attributes are used for the calculation of indicators.
     */
    int nbSchoolsTotCatch;	//nb of schools fished per species
    int[] cumulCatch;           //dim of 3 tables=nbSchoolsTotCatch
    float[] nbSchoolCatch;	//fished schools sorted according to their size
    float[] sizeSchoolCatch;
    float meanSize;	//mean size per species in the ecosystem
    float meanSizeCatch;	//mean size per species in catches
    float meanTLSpe;
    float[] meanTLperAge;
    float TLeggs = 3f;
    /*
     * ***************************
     * * Life history parameters *
     * ***************************
     */
    float D;//D0;		//mortality rates year-1
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
    float[] seasonSpawning, seasonFishing; //according to nbDt
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
    // yield
    double yield, yieldN;

    /**
     * Create a new species
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
        this.F = getOsmose().FMatrix[numSerie][index];
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
        this.seasonFishing = getOsmose().seasonFishingMatrix[numSerie][index];
        this.seasonSpawning = getOsmose().seasonSpawningMatrix[numSerie][index];
        this.supAgeOfClass0 = getOsmose().supAgeOfClass0Matrix[numSerie][index];//age from which the species biomass-0 is calculated
        this.indexAgeClass0 = (int) Math.ceil(supAgeOfClass0 * getSimulation().getNbTimeStepsPerYear());      // index of supAgeOfClass0 used in tabCohorts table
        this.larvalSurvival = getOsmose().larvalSurvivalMatrix[numSerie][index];
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
         * phv 2011/11/21
         * Added new parameters for species reproducing outside the simulated
         * area.
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
        cumulCatch = new int[nbCohorts];

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

        meanTLperAge = new float[nbCohorts];
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

    public Cohort[] getCohorts() {
        return tabCohorts;
    }

    public int getNumberCohorts() {
        return nbCohorts;
    }

    public School getSchool(int classAge, int indexSchool) {
        return tabCohorts[classAge].getSchool(indexSchool);
    }
    
    public List<School> getSchools() {
        List<School> schools = new ArrayList();
        for (Cohort cohort : getCohorts()) {
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

    public double getAbundance() {
        return abundance;
    }

    public void resetAbundance() {
        abundance = 0;
    }

    public void incrementAbundance(double incr) {
        this.abundance += incr;
    }

    public double getBiomass() {
        return biomass;
    }

    public void resetBiomass() {
        biomass = 0.d;
    }

    public void incrementBiomass(double incr) {
        biomass += incr;
    }

    public void growth() {

        for (int j = 0; j < nbCohorts; j++) {
            Cohort cohort = tabCohorts[j];
            if ((j == 0) || cohort.isOut(getSimulation().getIndexTime())) {
                // Linear growth for eggs and migrating schools
                for (School school : cohort) {
                    school.setLength(school.getLength() + deltaMeanLength[j]);
                    school.setWeight((float) (c * Math.pow(school.getLength(), bPower)));
                }
            } else {
                // Growth based on predation success
                if (cohort.getAbundance() != 0) {
                    for (School school : cohort) {
                        school.growth(minDelta[j], maxDelta[j], c, bPower);
                    }
                }
            }
        }
    }

    public boolean isReproduceLocally() {
        return reproduceLocally;
    }

    /*
     * phv 2011/11/22
     * Created new function for modeling incoming flux of biomass for species
     * that do not reproduce in the simulated domain.
     */
    public void incomingFlux() {
        /*
         * Update Cohort biomass
         */
        for (int i = 0; i < nbCohorts; i++) {
            getCohort(i).setBiomass(0);
            for (int j = 0; j < getCohort(i).size(); j++) {
                getCohort(i).setBiomass(getCohort(i).getBiomass() + getSchool(i, j).getBiomass());
            }
        }
        /*
         * Making cohorts going up to the upper age class
         */
        for (int i = nbCohorts - 1; i > ageMeanIn; i--) {
            getCohort(i).upperAgeClass(tabCohorts[i - 1]);
            for (int j = 0; j < getCohort(i).size(); j++) {
                getSchool(i, j).setCohort(getCohort(i));
            }
        }
        /*
         * Reset all cohorts younger than ageMeanIn
         */
        for (int i = ageMeanIn - 1; i > 0; i--) {
            tabCohorts[i].setAbundance(0);
            tabCohorts[i].setBiomass(0);
            tabCohorts[i].clear();
        }
        /*
         * Incoming flux
         */
        double biomassIn = biomassFluxIn * seasonSpawning[getSimulation().getIndexTime()];
        float meanWeigthIn = (float) (c * Math.pow(meanLengthIn, bPower));
        long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
        tabCohorts[ageMeanIn].setAbundance(abundanceIn);
        tabCohorts[ageMeanIn].setBiomass(biomassIn);
        tabCohorts[ageMeanIn].clear();
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (abundanceIn > 0 && abundanceIn < nbSchools) {
            tabCohorts[ageMeanIn].add(new School(tabCohorts[ageMeanIn], abundanceIn, meanLengthIn, meanWeigthIn));
        } else if (abundanceIn >= nbSchools) {
            int mod = (int) (abundanceIn % nbSchools);
            int abdSchool = (int) (abundanceIn / nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                abdSchool += (i < mod) ? 1 : 0;
                tabCohorts[ageMeanIn].add(new School(tabCohorts[ageMeanIn], abdSchool, meanLengthIn, meanWeigthIn));
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
            tempTL += tabSchoolsRanked.get(i).getTrophicLevel()[tabSchoolsRanked.get(i).getCohort().getAgeNbDt()] * tabSchoolsRanked.get(i).getBiomass();
        }

        double nbEggs = sexRatio * alpha * seasonSpawning[getSimulation().getIndexTime()] * SSB * 1000000;
        
        //MAKING COHORTS GOING UP to the UPPER AGE CLASS
        //species, age, caseLeftUpAireCoh, tabCasesAireCoh do not change
        for (int i = nbCohorts - 1; i > 0; i--) {
            getCohort(i).upperAgeClass(tabCohorts[i - 1]);
            for (int j = 0; j < getCohort(i).size(); j++) {
                getSchool(i, j).setCohort(getCohort(i));
            }
        }

        //UPDATE AGE CLASS 0
        Cohort coh0 = tabCohorts[0];
        coh0.setAbundance(nbEggs);
        coh0.setBiomass(nbEggs * eggWeight / 1000000.);
        coh0.clear();
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (nbEggs == 0.d) {
            // do nothing, zero school
        } else if (nbEggs < nbSchools) {
            coh0.add(new School(coh0, nbEggs, eggSize, eggWeight));
        } else if (nbEggs >= nbSchools) {
            coh0.ensureCapacity(nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                coh0.add(new School(coh0, coh0.getAbundance() / nbSchools, eggSize, eggWeight));
            }
        }
        coh0.trimToSize();
        for (int i = 0; i < coh0.size(); i++) {
            ((School) coh0.get(i)).getTrophicLevel()[0] = TLeggs; //tempTL/(float)SSB;
        }
    }

    public void update() {
        
        //UPDATE ABD and BIOMASS of SPECIES
        abundance = 0;
        biomass = 0;
        for (int i = 0; i < nbCohorts; i++) {
            for (int k = 0; k < getCohort(i).size(); k++) {
                abundance += getSchool(i, k).getAbundance();
                biomass += getSchool(i, k).getBiomass();
            }
        }

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

        // UPDATE LENGTHS and MEAN WEIGHTS of AGE CLASSES
        for (int i = 0; i < nbCohorts; i++) {
            getCohort(i).calculMeanGrowth();
        }
    }

    /**
     * Sort all the schools of this species according to their length.
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

    // ---------MORGANE 07-2004-----------
    public void calculSizes() {

        //Calculation of mean size per species
        float abdWithout0;
        meanSize = 0;
        float sum = 0;
        abdWithout0 = 0;
        for (int j = indexAgeClass0; j < nbCohorts; j++) //we don't consider age class 0 in the calculation of the mean
        {
            for (int k = 0; k < getCohort(j).size(); k++) {
                sum += getSchool(j, k).getAbundance() * getSchool(j, k).getLength();
                abdWithout0 += getSchool(j, k).getAbundance();
            }
        }

        if (abdWithout0 != 0) {
            meanSize = sum / abdWithout0;
        }

    }

    //---------------MORGANE  07-2004
    //same as previously, but for fished schools
    public void calculSizesCatch() {
        nbSchoolCatch = new float[nbSchoolsTotCatch];
        sizeSchoolCatch = new float[nbSchoolsTotCatch];
        for (int k = 0; k < nbSchoolsTotCatch; k++) {
            nbSchoolCatch[k] = getSimulation().tabNbCatch[index][k];
            sizeSchoolCatch[k] = getSimulation().tabSizeCatch[index][k];
        }

        //Calculation of mean size per species
        float abdCatch;
        meanSizeCatch = 0;
        float sumCatch = 0;
        abdCatch = 0;
        for (int j = 0; j < nbSchoolsTotCatch; j++) {
            sumCatch += nbSchoolCatch[j] * sizeSchoolCatch[j];
            abdCatch += nbSchoolCatch[j];
        }
        if (abdCatch != 0) {
            meanSizeCatch = sumCatch / abdCatch;
        }
    }

    public void calculTL() {
        
        float biomWithout0 = 0;
        float abd = 0;
        float sum = 0;
        
        // ********** Calcul of mean trophic level of the species, without age 0

        meanTLSpe = 0;

        for (int j = indexAgeClass0; j < nbCohorts; j++) //we don't consider age class 0 in the calculation of the mean
        {
            for (int k = 0; k < getCohort(j).size(); k++) {
                if (getSchool(j, k).getTrophicLevel()[j] != 0) {
                    sum += getSchool(j, k).getBiomass() * getSchool(j, k).getTrophicLevel()[j];
                    biomWithout0 += getSchool(j, k).getBiomass();
                }
            }
        }

        if (biomWithout0 != 0) {
            meanTLSpe = sum / biomWithout0;
        }

        // ********* Calcul of mean trophic level per age

        for (int j = 0; j < nbCohorts; j++) {
            meanTLperAge[j] = 0;
        }

        for (int j = 0; j < nbCohorts; j++) {
            abd = 0;
            sum = 0;
            for (int k = 0; k < getCohort(j).size(); k++) {
                if (getSchool(j, k).getTrophicLevel()[j] != 0) {
                    sum += getSchool(j, k).getAbundance() * getSchool(j, k).getTrophicLevel()[j];
                    abd += getSchool(j, k).getAbundance();
                }
            }
            if (abd != 0) {
                meanTLperAge[j] = sum / abd;
            }
        }
        if (nbCohorts != nbCohorts) {
            System.out.println("nb= " + nbCohorts + " ,   length = " + nbCohorts);
        }
    }
}
