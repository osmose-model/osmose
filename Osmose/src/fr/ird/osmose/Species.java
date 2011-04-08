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
import java.util.*;

public class Species {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/08 phv
     * Added a getSchool(ageClass, index) instead of
     * tabCohorts[ageClass].getSchool(index)
     * Deleted the constructor with all the parameters. Parameters are now read
     * in a new method called init().
     * ***
     * 2011/04/07 phv
     * Deleted variable simulation. Osmose and Simulation are called with
     * Osmose.getInstance() and Osmose.getInstance().getSimulation()
     */
    //DESCRIPTORS
    int number;  		//num of the species [1...12]
    String name;			//name of the species
    long abundance;
    long nbEggs;
    double biomass;		//total biomass in tonnes
    private Cohort[] tabCohorts;       //[nb cohorts=(int)Math.round((longevity+1)*simulation.nbDt)
    int nbCohorts;              //=(int)Math.round((longevity+1)*simulation.nbDt
    double SSB;
    //MORGANE 07-2004
    //those attributes are used for the calculation of indicators
    int nbSchoolsTot;    //nb of schools per species
    School[] tabSchoolsRanked, schoolsRanked;  //schools sorted according to their size, dim = nbSchoolsTot
    int nbSchoolsTotCatch;	//nb of schools fished per species
    int[] cumulCatch;           //dim of 3 tables=nbSchoolsTotCatch
    float[] nbSchoolCatch;	//fished schools sorted according to their size
    float[] sizeSchoolCatch;
    float meanSizeSpe;	//mean size per species in the ecosystem
    float meanSizeSpeCatch;	//mean size per species in catches
    float meanTLSpe;
    float[] meanTLperAge;
    float TLeggs = 3f;
    //LIFE HISTORY PARAMETERS
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

    public Species(int number) {
        this.number = number;
    }

    public void init() {

        int numSerie = getSimulation().numSerie;

        // INITIALISATION of PARAM
        int iSpec = number - 1;
        this.name = getOsmose().nameSpecMatrix[numSerie][iSpec];
        this.D = getOsmose().DMatrix[numSerie][iSpec];
        this.F = getOsmose().FMatrix[numSerie][iSpec];
        this.longevity = getOsmose().longevityMatrix[numSerie][iSpec];
        this.lInf = getOsmose().lInfMatrix[numSerie][iSpec];
        this.K = getOsmose().KMatrix[numSerie][iSpec];
        this.t0 = getOsmose().t0Matrix[numSerie][iSpec];
        this.c = getOsmose().cMatrix[numSerie][iSpec];
        this.bPower = getOsmose().bPowerMatrix[numSerie][iSpec];
        this.alpha = getOsmose().alphaMatrix[numSerie][iSpec];
        this.sizeMat = getOsmose().sizeMatMatrix[numSerie][iSpec];
        this.nbFeedingStages = getOsmose().nbStagesMatrix[numSerie][iSpec];
        this.sizeFeeding = getOsmose().sizeFeedingMatrix[numSerie][iSpec];
        this.recruitAge = getOsmose().recruitAgeMatrix[numSerie][iSpec];
        this.recruitSize = getOsmose().recruitSizeMatrix[numSerie][iSpec];
        this.seasonFishing = getOsmose().seasonFishingMatrix[numSerie][iSpec];
        this.seasonSpawning = getOsmose().seasonSpawningMatrix[numSerie][iSpec];
        this.supAgeOfClass0 = getOsmose().supAgeOfClass0Matrix[numSerie][iSpec];//age from which the species biomass-0 is calculated
        this.indexAgeClass0 = (int) Math.ceil(supAgeOfClass0 * getSimulation().nbDt);      // index of supAgeOfClass0 used in tabCohorts table
        this.larvalSurvival = getOsmose().larvalSurvivalMatrix[numSerie][iSpec];
        this.sexRatio = getOsmose().sexRatioMatrix[numSerie][iSpec];
        this.eggSize = getOsmose().eggSizeMatrix[numSerie][iSpec];
        this.eggWeight = getOsmose().eggWeightMatrix[numSerie][iSpec];
        this.growthAgeThreshold = getOsmose().growthAgeThresholdMatrix[numSerie][iSpec];

        this.predationRate = getOsmose().predationRateMatrix[numSerie][iSpec];
        this.predPreySizesMax = getOsmose().predPreySizesMaxMatrix[numSerie][iSpec];
        this.predPreySizesMin = getOsmose().predPreySizesMinMatrix[numSerie][iSpec];
        this.criticalPredSuccess = getOsmose().criticalPredSuccessMatrix[numSerie][iSpec];
        this.starvMaxRate = getOsmose().starvMaxRateMatrix[numSerie][iSpec];
        this.nbAccessStages = getOsmose().nbAccessStage[iSpec];
        this.ageStagesTab = getOsmose().accessStageThreshold[iSpec];
        this.dietStagesTab = getOsmose().dietStageThreshold[numSerie][iSpec];
        this.nbDietStages = getOsmose().nbDietsStages[numSerie][iSpec];

        // START INITIALISATION of COHORTS
        nbCohorts = (int) Math.round((longevity) * getSimulation().nbDt);

        tabAbdIni = new long[nbCohorts];
        tabBiomIni = new double[nbCohorts];
        tabCohorts = new Cohort[nbCohorts];
        cumulCatch = new int[nbCohorts];

        // INITIALISATION of TAB for LENGTH and MINMAX of DELTA LENGTH
        tabMeanLength = new float[nbCohorts];
        tabMeanWeight = new float[nbCohorts];

        float decimalAge = 0.0f;
        tabMeanLength[0] = eggSize;
        tabMeanWeight[0] = eggWeight;

        for (int i = 1; i < nbCohorts; i++) {
            decimalAge = i / (float) getSimulation().nbDt;
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
            decimalAge = i / (float) getSimulation().nbDt;
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

    public int getNumberCohorts() {
        return nbCohorts;
    }

    public School getSchool(int classAge, int indexSchool) {
        return tabCohorts[classAge].getSchool(indexSchool);
    }

    public void growth() //****NEW: comes from growth1 and growth2
    {

        for (int i = 0; i < nbCohorts; i++) {
            //this had to be taken into account before the decision to take a straight line between eggs size and size at age 1
            if (i == 0) // eggs
            {
                for (int k = 0; k < tabCohorts[0].size(); k++) {
                    School school0k = getSchool(0, k);
                    school0k.setLength(school0k.getLength() + deltaMeanLength[i]);
                    school0k.setWeight((float) (c * Math.pow(school0k.getLength(), bPower)));
                    school0k.setBiomass(((double) school0k.getAbundance()) * school0k.getWeight() / 1000000.);
                }
            }

            if ((getCohort(i).getOutOfZoneCohort()[getSimulation().dt]))//||(i==0))
            {
                for (int k = 0; k < getCohort(i).size(); k++) {
                    School schoolk = getSchool(i, k);
                    schoolk.setLength(schoolk.getLength() + deltaMeanLength[i]);
                    schoolk.setWeight((float) (c * Math.pow(schoolk.getLength(), bPower)));
                    schoolk.setBiomass(((double) schoolk.getAbundance()) * schoolk.getWeight() / 1000000.);
                }
            } else {
                if (getCohort(i).getAbundance() != 0) {
                    getCohort(i).growth(minDelta[i], maxDelta[i], c, bPower);
                }
            }
        }
    }

    public void reproduce() {
        //CALCULATION of Spawning Stock Biomass (SSB) with an update of cohorts biomass
        for (int i = 0; i < nbCohorts; i++) {
            getCohort(i).setBiomass(0);
            for (int j = 0; j < getCohort(i).size(); j++) {
                getCohort(i).setBiomass(getCohort(i).getBiomass() + getSchool(i, j).getBiomass());
            }
        }
        SSB = 0;
        float tempTL = 0f;
        int indexMin = 0;
        while ((indexMin < tabSchoolsRanked.length)
                && (tabSchoolsRanked[indexMin].getLength() < sizeMat)) {
            indexMin++;
        }
        for (int i = indexMin; i < tabSchoolsRanked.length; i++) {
            SSB += tabSchoolsRanked[i].getBiomass();
            tempTL += tabSchoolsRanked[i].getTrophicLevel()[tabSchoolsRanked[i].getCohort().getAgeNbDt()] * tabSchoolsRanked[i].getBiomass();
        }

        nbEggs = Math.round(sexRatio * alpha * seasonSpawning[getSimulation().dt] * SSB * 1000000);
        if (nbEggs > 9100000000000000000l) {
            System.out.println("beyond long format for eggs numbers");
        }

        // REMOVING THE OLDEST COHORT from the system

        /*         long nbDeadSenescence = tabCohorts[nbCohorts-1].abundance;           //*************** Pas pris en compte pour l'instant
        //***************** pas ok
        QCell[][] matrix = getOsmose().grid.matrix;
        if(!tabCohorts[nbCohorts-1].outOfZoneCohort[getSimulation().dt])
        for(int k=0;k<tabCohorts[nbCohorts-1].nbSchools;k++)
        {
        QSchool oldSchoolk = (QSchool)tabCohorts[nbCohorts-1].getSchool(k);
        matrix[oldSchoolk.posi][oldSchoolk.get_jgrid()].vectPresentSchools.removeElement(oldSchoolk);
        matrix[oldSchoolk.posi][oldSchoolk.get_jgrid()].nbPresentSchools--;
        }

        for(int i=0;i<getOsmose().grid.nbLines;i++)
        for(int j=0;j<getOsmose().grid.nbColumns;j++)
        matrix[i][j].vectPresentSchools.trimToSize();

        tabCohorts[nbCohorts-1].vectSchools.removeAllElements();
        tabCohorts[nbCohorts-1].nbSchools=0;
        tabCohorts[nbCohorts-1].vectSchools.trimToSize();
         */
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
        coh0.setBiomass(((double) nbEggs) * eggWeight / 1000000.);
        coh0.clear();
        if (nbEggs == 0) {
            // do nothing, zero school
        } else if (nbEggs < getOsmose().nbSchools[getOsmose().numSerie]) {
            coh0.add(new School(coh0, nbEggs, eggSize, eggWeight));
            ((School) coh0.get(0)).setBiomass(coh0.getBiomass());
        } else if (nbEggs >= getOsmose().nbSchools[getOsmose().numSerie]) {
            int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
            coh0.ensureCapacity(nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                coh0.add(new School(coh0, Math.round(((double) coh0.getAbundance()) / (float) nbSchools), eggSize, eggWeight));
            }
            int surplus = (int) coh0.getAbundance() % coh0.size();
            ((School) (coh0.get(0))).setAbundance(((School) (coh0.get(0))).getAbundance() + surplus);
            for (int i = 0; i < coh0.size(); i++) {
                ((School) coh0.get(i)).setBiomass(((double) ((School) coh0.get(i)).getAbundance()) * eggWeight / 1000000.);
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

        /*	for(int i=0; i<nbCohorts;i++)
        {
        abundance+=getCohort(i).abundance;
        biomass+=getCohort(i).biomass;
        }
         */
        for (int i = 0; i < nbCohorts; i++) {
            for (int k = 0; k < getCohort(i).size(); k++) {
                abundance += getSchool(i, k).getAbundance();
                biomass += getSchool(i, k).getBiomass();
            }
        }



        if (abundance > 9100000000000000000l) {
            System.out.println("beyond long format for species abd num" + name);
        }

        for (int i = 0; i < nbCohorts; i++) {
            for (int j = 0; j < getCohort(i).size(); j++) {
                getSchool(i, j).updateFeedingStage(sizeFeeding, nbFeedingStages);
                getSchool(i, j).updateAccessStage(getOsmose().accessStageThreshold[number - 1], getOsmose().nbAccessStage[number - 1]);
            }
        }

        if (!(nbCohorts == (int) Math.round((longevity) * getSimulation().nbDt))) {
            nbCohorts = (int) Math.round((longevity + 1) * getSimulation().nbDt);
            System.out.println("PB of number of cohorts at the update stage: not equals to the initial number of cohorts");
            // Morgane 03-2007
        }
//		nbCohorts=(longevity+1)*getSimulation().nbDt;
//		for(int i=0;i<nbCohorts;i++)
//		if(getCohort(i).abundance==0)
//		this.nbCohorts--;


        // UPDATE LENGTHS and MEAN WEIGHTS of AGE CLASSES
        for (int i = 0; i < nbCohorts; i++) {
            getCohort(i).calculMeanGrowth();
        }
    }

    public void fishingA() // with recruit AGE as metric, calendar age
    {
        long nbSurplusDead = 0;
        int indexRecruitAge = Math.round(recruitAge * getSimulation().nbDt);

        if (getSimulation().targetFishing) //different F per species
        {
            for (int i = indexRecruitAge; i < nbCohorts; i++) {
                if (getCohort(i).getAbundance() != 0) {
                    nbSurplusDead += getCohort(i).fishing1(F * seasonFishing[getSimulation().dt]);
                }
            }

            if (nbSurplusDead != 0) //case not enough fish in a cohort -> surplus affected to other cohorts
            {
                Vector vectCohCatchable = new Vector(nbCohorts);
                long abdCatchableTot = 0;
                for (int i = indexRecruitAge; i < nbCohorts; i++) {
                    if (getCohort(i).getAbundance() != 0) {
                        vectCohCatchable.addElement(getCohort(i));
                        abdCatchableTot += getCohort(i).getAbundanceCatchable();
                    }
                }
                if (nbSurplusDead >= abdCatchableTot) // not enough fish even in other cohorts
                {
                    for (int i = indexRecruitAge; i < nbCohorts; i++) {
                        for (School school : getCohort(i)) {
                            if (school.isCatchable()) {
                                if ((getSimulation().t) >= getOsmose().timeSeriesStart) {
                                    getSimulation().savingYield[number - 1] +=
                                            ((float) (school.getAbundance() * school.getWeight() / 1000000));
                                    getSimulation().tabTLCatch[number - 1] += school.getTrophicLevel()[school.getCohort().getAgeNbDt()] * ((float) (school.getAbundance() * school.getWeight() / 1000000));
                                }
                                if (!(getCohort(i).getOutOfZoneCohort()[getSimulation().dt])) {
                                    school.getCell().remove(school);
                                }
                                getCohort(i).remove(school);
                            }
                        }
                        getCohort(i).setAbundance(getCohort(i).getAbundance() - getCohort(i).getAbundanceCatchable());
                        getCohort(i).setNbDeadFf(getCohort(i).getNbDeadFf() + getCohort(i).getAbundanceCatchable());
                    }

                } else // enough fish in other cohorts
                {
                    long abdToCatch;
                    for (int i = 0; i < vectCohCatchable.size(); i++) {
                        abdToCatch = Math.round(((double) nbSurplusDead)
                                * ((Cohort) vectCohCatchable.elementAt(i)).getAbundanceCatchable() / abdCatchableTot);
                        ((Cohort) vectCohCatchable.elementAt(i)).fishingSurplus(abdToCatch);
                    }
                }
            }
        } else //same F per species
        {
            for (int i = indexRecruitAge; i < nbCohorts; i++) {
                if ((getCohort(i).getAbundance() != 0) && (getSimulation().RS != 1.)) {
                    getCohort(i).fishing2((float) (F * seasonFishing[getSimulation().dt] / (1 - getSimulation().RS)));
                }
            }
        }
    }

    public void fishingL() {
//		with recruit LENGTH as metric   -> NEED FISHING1 and FISHING2 IN QSCHOOL (not in QCohort)
//		Not programmed for the moment. If recruitment size in input, transformed at initialisation into recruitment age
    }

    public void rankSchools() // NEW    morgane 02-2007
    {
        //Sort schools of all ages in the ecosystem according to their length
        nbSchoolsTot = 0;
        for (int j = 0; j < nbCohorts; j++) {
            nbSchoolsTot += getCohort(j).size();
        }
        tabSchoolsRanked = new School[nbSchoolsTot];
        int dummy;
        int index = 0;
        for (int j = 0; j < nbCohorts; j++) {
            for (int k = 0; k < getCohort(j).size(); k++) {
                tabSchoolsRanked[index] = getSchool(j, k);
                index++;
            }
        }
        int[] indexSchoolsSizes = new int[nbSchoolsTot];
        for (int k = 0; k < nbSchoolsTot; k++) {
            indexSchoolsSizes[k] = k;
        }
        for (int k1 = 0; k1 < nbSchoolsTot; k1++) {
            for (int k2 = k1 + 1; k2 < nbSchoolsTot; k2++) {
                if (((School) tabSchoolsRanked[indexSchoolsSizes[k1]]).getLength()
                        > ((School) tabSchoolsRanked[indexSchoolsSizes[k2]]).getLength()) {
                    dummy = indexSchoolsSizes[k1];
                    indexSchoolsSizes[k1] = indexSchoolsSizes[k2];
                    indexSchoolsSizes[k2] = dummy;
                }
            }
        }
        School[] tabSchoolsTemp = new School[nbSchoolsTot];
        for (int k = 0; k < tabSchoolsTemp.length; k++) {
            tabSchoolsTemp[k] = (School) tabSchoolsRanked[indexSchoolsSizes[k]];
        }
        for (int k = 0; k < tabSchoolsTemp.length; k++) {
            tabSchoolsRanked[k] = (School) tabSchoolsTemp[k];
        }
    }

    // ---------MORGANE 07-2004-----------
    public void calculSizes() // allows to calculate some SBIs in the ecosystem...   A verifier
    {
        /*	//Sort schools of age 1+ in the ecosystem according to their length
        nbSchoolsTot = 0;
        for(int j=indexAgeClass0;j<nbCohorts;j++)
        nbSchoolsTot += getCohort(j).nbSchools;
        schoolsRanked = new QSchool[nbSchoolsTot];
        int dummy;
        int index = 0;
        for(int j=1;j<nbCohorts;j++)
        for(int k=0;k<getCohort(j).nbSchools;k++)
        {
        schoolsRanked[index] = (QSchool)getSchool(j, k);
        index++;
        }
        int[] indexSchoolsSizes=new int[nbSchoolsTot];
        for(int k=0;k<nbSchoolsTot;k++)
        indexSchoolsSizes[k]=k;
        for(int k1=0;k1<nbSchoolsTot;k1++)
        for(int k2=k1+1;k2<nbSchoolsTot;k2++)
        if(((QSchool)schoolsRanked[indexSchoolsSizes[k1]]).length>
        ((QSchool)schoolsRanked[indexSchoolsSizes[k2]]).length)
        {
        dummy=indexSchoolsSizes[k1];
        indexSchoolsSizes[k1]=indexSchoolsSizes[k2];
        indexSchoolsSizes[k2]=dummy;
        }
        QSchool[] tabSchoolsTemp = new QSchool[nbSchoolsTot];
        for(int k=0;k<tabSchoolsTemp.length;k++)
        tabSchoolsTemp[k]=(QSchool)schoolsRanked[indexSchoolsSizes[k]];
        for(int k=0;k<tabSchoolsTemp.length;k++)
        schoolsRanked[k]=(QSchool)tabSchoolsTemp[k];
         */
        //Calculation of mean size per species
        float abdWithout0;
        meanSizeSpe = 0;
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
            meanSizeSpe = sum / abdWithout0;
        }

        //Calculation of max size (according to abd percentages) per species
		/*
        float limit = 0;
        for (int p=0;p<getOsmose().nbPercent;p++)
        {
        float sum2 = 0;
        int j= schoolsRanked.length;
        limit = (1- 0.99f + (p*0.01f))*abdWithout0;   //abd representing p% of the total abd without age0
        while ((sum2<limit)&&(j>0))
        {
        j--;
        sum2 += ((QSchool)schoolsRanked[j]).abundance;
        }
        limit = 0;
        if((getSimulation().t>=getOsmose().timeSeriesStart)&&(j<schoolsRanked.length))
        getOsmose().maxSize[getOsmose().numSimu][number-1][p][getSimulation().t-getOsmose().timeSeriesStart] = ((QSchool)schoolsRanked[j]).length;
        }
         */
        /*
        //Calculation of the proportion of mature fish
        float lMatTemp1 = 0;
        for (int i=0;i<getOsmose().nbPercent;i++)
        {
        float sumMat = 0;
        lMatTemp1 = (getOsmose().lMat[getSimulation().numSerie][number-1])*(1f+(i*0.1f));
        for(int j=0;j<nbSchoolsTot;j++)
        {
        if((((QSchool)schoolsRanked[j]).length) > lMatTemp1)
        {
        sumMat += ((QSchool)schoolsRanked[j]).abundance;
        }

        }
        lMatTemp1 = 0;
        if((getSimulation().t>=getOsmose().timeSeriesStart)&&(abdWithout0!=0))
        getOsmose().propLargeFish[getOsmose().numSimu][number-1][i][getSimulation().t-getOsmose().timeSeriesStart] = sumMat/abdWithout0;
        }
         */    }

    //---------------MORGANE  07-2004
    public void calculSizesCatch() //same as previously, but for fished schools
    {
        nbSchoolCatch = new float[nbSchoolsTotCatch];
        sizeSchoolCatch = new float[nbSchoolsTotCatch];
        int dummy;
        int index = 0;
        for (int k = 0; k < nbSchoolsTotCatch; k++) {
            nbSchoolCatch[index] = getSimulation().tabNbCatch[number - 1][k];
            sizeSchoolCatch[index] = getSimulation().tabSizeCatch[number - 1][k];
            index++;
        }



        /*	int[] indexSchoolsSizes=new int[nbSchoolsTotCatch];
        for(int k=0;k<nbSchoolsTotCatch;k++)
        indexSchoolsSizes[k]=k;
        for(int k1=0;k1<nbSchoolsTotCatch;k1++)
        for(int k2=k1+1;k2<nbSchoolsTotCatch;k2++)
        if(sizeSchoolCatch[k1]>sizeSchoolCatch[k2])
        {
        dummy=indexSchoolsSizes[k1];
        indexSchoolsSizes[k1]=indexSchoolsSizes[k2];
        indexSchoolsSizes[k2]=dummy;
        }
        float[][] tabSchoolsCatchTemp = new float[nbSchoolsTotCatch][];
        for (int i=0;i<nbSchoolsTotCatch;i++)
        tabSchoolsCatchTemp[i] = new float[2];

        for(int k=0;k<tabSchoolsCatchTemp.length;k++)
        {
        tabSchoolsCatchTemp[k][0]=nbSchoolCatch[indexSchoolsSizes[k]];
        tabSchoolsCatchTemp[k][1]=sizeSchoolCatch[indexSchoolsSizes[k]];
        }
        for(int k=0;k<tabSchoolsCatchTemp.length;k++)
        {
        nbSchoolCatch[k]=tabSchoolsCatchTemp[k][0];
        sizeSchoolCatch[k]=tabSchoolsCatchTemp[k][1];
        }
         */
        //Calculation of mean size per species
        float abdCatch;
        meanSizeSpeCatch = 0;
        float sumCatch = 0;
        abdCatch = 0;
        for (int j = 0; j < nbSchoolsTotCatch; j++) {
            sumCatch += nbSchoolCatch[j] * sizeSchoolCatch[j];
            abdCatch += nbSchoolCatch[j];
        }
        if (abdCatch != 0) {
            meanSizeSpeCatch = sumCatch / abdCatch;
        }

//		calcul of length frequency in catch
        if (getSimulation().sizeSpectrumPerSpeOutput) {
            int sizeTemp = 0;
            for (int j = 0; j < nbSchoolsTotCatch; j++) {
                if (sizeSchoolCatch[j] < getOsmose().spectrumMaxSize) {
                    sizeTemp = (int) Math.floor(sizeSchoolCatch[j] / getOsmose().classRange);
                    getSimulation().spectrumTemp[1][number - 1][sizeTemp] += nbSchoolCatch[j];
                }
            }
        }

        /*	//Calculation of max size (according to abd percentages) per species

        float limiteCatch = 0;
        for (int p=0;p<getOsmose().nbPercent;p++)
        {
        float sumCatch2 = 0;
        int j= nbSchoolsTotCatch;
        limiteCatch = (1 -0.99f +(p*0.01f))*abdCatch;
        while ((sumCatch2<limiteCatch)&&(j>0))
        {
        j--;
        sumCatch2 += nbSchoolCatch[j];
        }
        limiteCatch = 0;
        if((getSimulation().t>=getOsmose().timeSeriesStart)&&(j<nbSchoolsTotCatch))
        getOsmose().maxSizeCatch[getOsmose().numSimu][number-1][p][getSimulation().t-getOsmose().timeSeriesStart] = sizeSchoolCatch[j];
        }

        //Calculation of the proportion of mature fish

        float lMatTemp = 0;
        for (int i=0;i<getOsmose().nbPercent;i++)
        {
        float sumMatCatch = 0;
        lMatTemp = (getOsmose().lMat[getSimulation().numSerie][number-1])*(1+(i*0.1f));
        for(int j=0;j<nbSchoolsTotCatch;j++)
        {
        if((sizeSchoolCatch[j]) > lMatTemp)
        {
        sumMatCatch += nbSchoolCatch[j];
        }
        }
        lMatTemp =0;
        if((getSimulation().t>=getOsmose().timeSeriesStart)&&(abdCatch!=0))
        getOsmose().propLargeFishCatch[getOsmose().numSimu][number-1][i][getSimulation().t-getOsmose().timeSeriesStart] = sumMatCatch/abdCatch;
        }*/
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
