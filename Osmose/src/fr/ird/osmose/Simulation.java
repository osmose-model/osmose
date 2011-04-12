package fr.ird.osmose;

/*******************************************************************************
 * <p>Titre : Simulation class</p>
 *
 * <p>Description : </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 ******************************************************************************* 
 */
import java.io.*;
import java.util.*;

public class Simulation {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/08 phv
     * Deleted the constructor. Parameters are now loaded in the init() method.
     * 2011/04/07 phv
     * Deleted variable Osmose. Must be called using Osmose.getInstance()
     */
    Coupling couple;
    int numSerie, nbDt, savingDt;
    boolean randomDistribution = true;
    int t, dt, dtCount; // years, time steps, for saving
    int nbSpecies, nbSpeciesIni;
    Species[] species;
    School[] tabSchoolsRandom;
    String recruitMetric;
    //spectrum
//	float[] spectrumAbd, spectrumBiom;
    float[][] spectrumSpeciesAbd; //output of size spectrum per species - Stage MORGANE 07-2004
    float[][][] distribTL;  //output of TL distribution per species
    //tables for output of additional data, cf Morgane
    //float[] lMat;
    //table grouping the characteristics of schools caught by species
    float[][] tabSizeCatch, tabNbCatch;
    float[] tabTLCatch;
    long abdTot = 0;
    double biomTot = 0;
    long nbDeadTot, nbDeadTotDd, nbDeadTotFf, nbDeadTotPp, nbDeadTotSs;//(for ages>=1)
    long nbDeadTot0, nbDeadTotDd0, nbDeadTotPp0, nbDeadTotSs0, nbDeadTotFf0;
    // for saving
    long[] abdTemp, abdTempWithout0, savingNbYield;
    double[] biomTemp, biomTempWithout0;
    float[] savingYield, meanSizeTemp, meanSizeCatchTemp, meanTLtemp;
    float[][][] spectrumTemp;
    float[][] meanTLperAgeTemp;
    int[][] countTemp;
    double[][] biomPerStage;
    float tempMaxProbaPresence;
    float[][] accessibilityMatrix;
    int[] nbAccessibilityStages;
    float[][][][] dietsMatrix, predatorsPressureMatrix;
    long[][] nbStomachs;
    // initialisation param for species abd in function of an input size spectrum
    double a, b;	    //coeff of the relation nb=length^a * expb
    //in Rice : a=-5.8; b=35.5*/
    Vector[] specInSizeClass10;	//tab of vectors of species belonging to [0-10[....[140-150[
    long[] abdGapSizeClass10;	//tab of abd to fill in 10cm size class
    long abdIniMin;			//initial min abd of last age class of a species
    boolean targetFishing;
    double RS;		//ratio between mpa and total grid surfaces, RS for Relative Size of MPA
    FileOutputStream dietTime, biomTime, abdTime, TLDistTime, yieldTime, nbYieldTime, meanSizeTime, meanTLTime, SSperSpTime;
    Runtime r = Runtime.getRuntime();
    long freeMem;
//	*******Trophodynamics indicators
    boolean TLoutput;
    boolean TLDistriboutput;
    boolean dietsOutput;
    String dietMetric;
//	*******Size-based indicators
    boolean meanSizeOutput;
    boolean sizeSpectrumOutput;
    boolean sizeSpectrumPerSpeOutput;
//	*******Mortalities data
    boolean planktonMortalityOutput;
    boolean outputClass0;
    boolean calibration;
    boolean isForcing;

    public void init() {

        t = 0;
        dt = 0;
        dtCount = 1;
        getOsmose().simInitialized = true;
        this.numSerie = getOsmose().numSerie;
        this.nbDt = getOsmose().nbDtMatrix[numSerie];
        this.savingDt = getOsmose().savingDtMatrix[numSerie];
        this.nbSpecies = getOsmose().nbSpeciesTab[numSerie];
        this.recruitMetric = getOsmose().recruitMetricMatrix[numSerie];
        nbSpeciesIni = getOsmose().nbSpeciesTab[numSerie];

        this.calibration = getOsmose().calibrationMatrix[numSerie];

        this.TLoutput = getOsmose().TLoutputMatrix[numSerie];
        this.TLDistriboutput = getOsmose().TLDistriboutputMatrix[numSerie];
        this.dietsOutput = getOsmose().dietsOutputMatrix[numSerie];
        this.dietMetric = getOsmose().dietOutputMetrics[numSerie];
        this.meanSizeOutput = getOsmose().meanSizeOutputMatrix[numSerie];
        this.sizeSpectrumOutput = getOsmose().sizeSpectrumOutputMatrix[numSerie];
        this.sizeSpectrumPerSpeOutput = getOsmose().sizeSpectrumPerSpeOutputMatrix[numSerie];
        this.planktonMortalityOutput = getOsmose().planktonMortalityOutputMatrix[numSerie];
        this.outputClass0 = getOsmose().outputClass0Matrix[numSerie];

        // Initialise plankton matrix
        this.isForcing = getOsmose().isForcing[numSerie];
        iniPlanktonField(isForcing);

        //CREATION of the SPECIES
        species = new Species[nbSpecies];
        for (int i = 0; i < nbSpecies; i++) {
            species[i] = new Species(i + 1);
            species[i].init();
        }

        // determine if fishing is species-based or similar for all species
        targetFishing = false;
        for (int i = 1; i < nbSpecies; i++) {
            if (species[i].F != species[0].F) {
                targetFishing = true;
            }
        }

        //INITIALISATION of SPECIES ABD ACCORDING TO SIZE SPECTRUM
        if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("biomass")) {
            iniBySpeciesBiomass();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("spectrum")) {
            iniBySizeSpectrum();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("random")) {
            iniRandomly();
        }

        for (int i = 0; i < nbSpecies; i++) {
            abdTot += species[i].getAbundance();
            biomTot += species[i].getBiomass();
        }

        // Initialize all the tables required for saving output
        initSaving();

        //Initialisation indicators
        tabSizeCatch = new float[species.length][];
        tabNbCatch = new float[species.length][];
        if (sizeSpectrumPerSpeOutput) {
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        ((School) species[i].getCohort(j).getSchool(k)).rankSize(getOsmose().tabSizes, getOsmose().spectrumMaxSize);
                    }
                }
            }
        }
    }

    private Grid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    public void step() {
        // screen display to check the period already simulated
        if (t % 5 == 0) {
            System.out.println("t" + t + " -> " + new Date());   // t is annual
        } else {
            System.out.println("t" + t);
        }

        // calculation of relative size of MPA
        if ((getOsmose().thereIsMPATab[numSerie]) && (t == getOsmose().MPAtStartTab[numSerie])) {
            RS = getOsmose().tabMPAiMatrix[numSerie].length / ((getGrid().getNbLines()) * getGrid().getNbColumns());
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(true);
            }
        } else if ((!getOsmose().thereIsMPATab[numSerie]) || (t > getOsmose().MPAtEndTab[numSerie])) {
            RS = 0;
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(false);
            }
        }


        while (dt < nbDt) // for each time step dt of the year t
        {
            // clear tables
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    species[i].getCohort(j).setNbDeadPp(0);
                    species[i].getCohort(j).setNbDeadSs(0);
                    species[i].getCohort(j).setNbDeadDd(0);
                    species[i].getCohort(j).setNbDeadFf(0);
                }
            }
            // update stages
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        ((School) species[i].getCohort(j).getSchool(k)).updateFeedingStage(species[i].sizeFeeding, species[i].nbFeedingStages);
                        ((School) species[i].getCohort(j).getSchool(k)).updateAccessStage(getOsmose().accessStageThreshold[i], getOsmose().nbAccessStage[i]);
                        ((School) species[i].getCohort(j).getSchool(k)).updateDietOutputStage(species[i].dietStagesTab, species[i].nbDietStages);
                    }
                }
            }

            // ***** SPATIAL DISTRIBUTION of the species *****

            if (!((dt == 0) && (t == 0))) // because distributeSpeciesIni() used at initialisation
            {
                for (int i = 0; i < getGrid().getNbLines(); i++) // remove all the schools because of the last age class
                {
                    for (int j = 0; j < getGrid().getNbColumns(); j++) {
                        getGrid().getCell(i, j).clear();
                    }
                }
                distributeSpecies();      // update distribution
            }

            // ***** ADDITIONAL MORTALITY D *****
            for (int i = 0; i < species.length; i++) {
                //for all species, D is due to other predators (seals, seabirds)
                //for migrating species, we add mortality because absents during a time step
                //so they don't undergo mortalities due to predation and starvation
                //Additional mortalities for ages 0: no-fecundation of eggs, starvation more pronounced
                //than for sup ages (rel. to CC), predation by other species are not explicit
                if (species[i].getCohort(0).getAbundance() != 0) {
                    species[i].getCohort(0).surviveD(species[i].larvalSurvival + (species[i].getCohort(0).getOutOfZoneMortality()[dt] / (float) nbDt));     //additional larval mortality
                }
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    if (species[i].getCohort(j).getAbundance() != 0) {
                        species[i].getCohort(j).surviveD((species[i].D + species[i].getCohort(j).getOutOfZoneMortality()[dt]) / (float) nbDt);
                    }
                }
            }

            // ***** UPDATE LTL DATA *****
            r.gc();
            freeMem = r.freeMemory();

            if ((!couple.isForcing) && (t >= couple.startLTLModel)) // if LTL model to be run, run it
            {
                System.out.print(". " + new Date() + "       Free mem = " + freeMem);
                couple.runLTLModel();
                System.out.println("      -> OK ");
            }
            couple.updatePlankton(dt);     // update plankton fields either from LTL run or from data

            // *** PREDATION ***
            randomOrder();
            rankSchoolsSizes();

            if (t >= getOsmose().timeSeriesStart) // save fish biomass before predation process for diets data
            {
                for (int i = 0; i < nbSpecies; i++) {
                    for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                        for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                            biomPerStage[i][((School) species[i].getCohort(j).getSchool(k)).dietOutputStage] += ((School) species[i].getCohort(j).getSchool(k)).getBiomass();
                        }
                    }
                }
            }
            if ((t >= couple.startLTLModel - 1)) // save grid of plankton biomass one year before coupling so forcing mode is also saved
            {
                couple.savePlanktonBiomass();
            }

            for (int i = 0; i < tabSchoolsRandom.length; i++) {
                if (!tabSchoolsRandom[i].willDisappear()) {
                    if (!(((Cohort) tabSchoolsRandom[i].getCohort()).getAgeNbDt() == 0)) // eggs do not predate other organisms
                    {
                        tabSchoolsRandom[i].predation();
                    }
                }
            }

            if ((!couple.isForcing) && (t >= couple.startLTLModel)) {
                couple.calculPlanktonMortality();
            }


            // *** STARVATION MORTALITY ***
            for (int i = 0; i < tabSchoolsRandom.length; i++) {
                if (!tabSchoolsRandom[i].willDisappear()) {
                    tabSchoolsRandom[i].surviveP();
                }
            }

            // *** UPDATE of disappeared schools ***
            assessNbSchools();
            assessPresentSchools();
            assessAbdCohSpec();

            // *** GROWTH ***
            for (int i = 0; i < species.length; i++) {
                if (species[i].getAbundance() != 0) {
                    species[i].growth();
                }
            }

            // *** FISHING ***
            assessCatchableSchools();

            //Initialisation of indicators tables
            for (int i = 0; i < species.length; i++) {
                tabSizeCatch[i] = new float[species[i].nbSchoolsTotCatch];
                tabNbCatch[i] = new float[species[i].nbSchoolsTotCatch];
            }
            for (int i = 0; i < species.length; i++) {
                for (int s = 0; s < species[i].nbSchoolsTotCatch; s++) {
                    tabSizeCatch[i][s] = 0;
                    tabNbCatch[i][s] = 0;
                }
            }
            for (int i = 0; i < species.length; i++) {
                if ((species[i].getAbundance() != 0) && (species[i].seasonFishing[dt] != 0) && (species[i].F != 0)) {
                    species[i].fishingA();
                }
            }


            // *** UPDATE ***
            nbSpecies = nbSpeciesIni;
            abdTot = 0;
            biomTot = 0;

            for (int i = 0; i < species.length; i++) {
                species[i].update();
                abdTot += species[i].getAbundance();
                biomTot += species[i].getBiomass();
                if (species[i].getAbundance() == 0) {
                    nbSpecies--;
                }
            }

            //update spectra
            if (sizeSpectrumPerSpeOutput) {
                for (int i = 0; i < getOsmose().nbSizeClass; i++) {
                    //spectrumAbd[i]=0;
                    //spectrumBiom[i]=0;
                    for (int j = 0; j < species.length; j++) {
                        spectrumSpeciesAbd[j][i] = 0;
                    }
                }
            }

            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        if (sizeSpectrumPerSpeOutput) {
                            ((School) species[i].getCohort(j).getSchool(k)).rankSize(getOsmose().tabSizes, getOsmose().spectrumMaxSize);
                        }
                        if ((t >= getOsmose().timeSeriesStart) && ((TLoutput) || (TLDistriboutput))) {
                            ((School) species[i].getCohort(j).getSchool(k)).rankTL(getOsmose().tabTL);
                        }
                    }
                }
            }
            //Stage Morgane - 07-2004  output of indicators
            for (int i = 0; i < species.length; i++) {
                if (meanSizeOutput) {
                    species[i].calculSizes();
                    species[i].calculSizesCatch();
                }
                if ((TLoutput) || (TLDistriboutput)) {
                    species[i].calculTL();
                }
            }

            // *** SAVE THE TIME STEP ***
            saveStep();

            // *** REPRODUCTION ***
            for (int i = 0; i < species.length; i++) {
                species[i].reproduce();
            }

            if (t >= getOsmose().timeSeriesStart) {
                dtCount++; // for saving
            }
            dt++;
        }
        dt = 0;  //end of the year
        t++; // go to following year
    }

    public void iniBySizeSpectrum() //************************************* A VERIFIER : � adapter eu nouveau pas de temps si besoin**************************
    //initialisation according to a spectrum [10cm], from 0 to 200cm
    {
        long[] tempSpectrumAbd = new long[20];
        specInSizeClass10 = new Vector[20];    //20 classes size 0 a 200
        for (int i = 0; i < specInSizeClass10.length; i++) {
            specInSizeClass10[i] = new Vector(nbSpeciesIni);
        }
        abdIniMin = 100;
        //a=-5.8;
        //b=35.5;
        a = getOsmose().SSslope[numSerie];
        b = getOsmose().SSintercept[numSerie];
        //Calculation of abd lacking in each size class
        //calculation apart for first size class because minSize=0.05 (and not 0)
        tempSpectrumAbd[0] = Math.round(Math.pow(5., a) * Math.exp(b));
        for (int i = 1; i < 20; i++) {
            tempSpectrumAbd[i] = Math.round(Math.pow((i * getOsmose().classRange) + 5., a) * Math.exp(b));
        }
        //tabSizes10[i]+5 is mean length of [tabSizes10[i],tabSizes10[i+1][
        //Sort the Lmax of each species in each size class
        for (int i = 0; i < nbSpeciesIni; i++) {
            int index1 = tempSpectrumAbd.length - 1;
            while (species[i].tabMeanLength[species[i].getNumberCohorts() - 1] < (index1 * getOsmose().classRange)) {
                index1--;
            }
            specInSizeClass10[index1].addElement(species[i]);
        }
        //calculate spectrumMaxIndex
        int spectrumMaxIndex = specInSizeClass10.length - 1;
        while (specInSizeClass10[spectrumMaxIndex].size() == 0) {
            spectrumMaxIndex--;
        }

        //Calculate abd species and cohorts
        for (int i = spectrumMaxIndex; i >= 0; i--) {
            for (int j = 0; j < specInSizeClass10[i].size(); j++) {
                Species speciesj = ((Species) specInSizeClass10[i].elementAt(j));
                speciesj.tabAbdIni[speciesj.getNumberCohorts() - 1] = Math.round(((double) tempSpectrumAbd[i])
                        / specInSizeClass10[i].size());
                speciesj.tabBiomIni[speciesj.getNumberCohorts() - 1] = ((double) speciesj.tabAbdIni[speciesj.getNumberCohorts() - 1]) * speciesj.tabMeanWeight[speciesj.getNumberCohorts() - 1] / 1000000.;
                //we consider that D0->1 = 10 for the first age class (month or year, whatever nbDt), D0-1year->2 = 1 and D=0.4 otherwise
                //we calculate abd & biom of coh, and in parallel abd & biom of species & we create cohorts
                speciesj.resetAbundance();
                speciesj.resetBiomass();
                speciesj.incrementAbundance(speciesj.tabAbdIni[speciesj.getNumberCohorts() - 1]);
                speciesj.incrementBiomass(speciesj.tabBiomIni[speciesj.getNumberCohorts() - 1]);

                for (int k = speciesj.getNumberCohorts() - 2; k >= (2 * nbDt); k--) {
                    speciesj.tabAbdIni[k] = Math.round(speciesj.tabAbdIni[k + 1] * Math.exp((0.5 / (float) nbDt)));
                    speciesj.tabBiomIni[k] = ((double) speciesj.tabAbdIni[k]) * speciesj.tabMeanWeight[k] / 1000000.;
                    speciesj.incrementAbundance(speciesj.tabAbdIni[k]);
                    speciesj.incrementBiomass(speciesj.tabBiomIni[k]);
                }
                int kTemp;
                if (speciesj.longevity <= 1) {
                    kTemp = speciesj.getNumberCohorts() - 2;
                } else {
                    kTemp = (2 * nbDt) - 1;
                }

                for (int k = kTemp; k >= 1; k--) {
                    speciesj.tabAbdIni[k] = Math.round(speciesj.tabAbdIni[k + 1] * Math.exp((1. / (float) nbDt)));
                    speciesj.tabBiomIni[k] = ((double) speciesj.tabAbdIni[k]) * speciesj.tabMeanWeight[k] / 1000000.;
                    speciesj.incrementAbundance(speciesj.tabAbdIni[k]);
                    speciesj.incrementBiomass(speciesj.tabBiomIni[k]);
                }

                speciesj.tabAbdIni[0] = Math.round(speciesj.tabAbdIni[1] * Math.exp(10.));
                speciesj.tabBiomIni[0] = ((double) speciesj.tabAbdIni[0]) * speciesj.tabMeanWeight[0] / 1000000.;
                /*
                 * 2011/04/11 phv : commented line since nbEggs is only used in
                 * Species.reproduce as local variable.
                 */
                //speciesj.nbEggs = speciesj.tabAbdIni[0];
                speciesj.incrementAbundance(speciesj.tabAbdIni[0]);
                speciesj.incrementBiomass(speciesj.tabBiomIni[0]);
                //creation of the cohorts
                for (int k = 0; k < speciesj.getNumberCohorts(); k++) {
                    speciesj.setCohort(k, new Cohort(speciesj, k, speciesj.tabAbdIni[k], speciesj.tabBiomIni[k], speciesj.tabMeanLength[k], speciesj.tabMeanWeight[k]));
                }
            }
        }
    }

    public void iniRandomly() //************************** Nouvelle option : A faire
    {
    }

    public void iniBySpeciesBiomass() {
        float correctingFactor;
        double abdIni;

        for (int i = 0; i < nbSpeciesIni; i++) {
            //We calculate abd & biom ini of cohorts, and in parallel biom of species
            Species speci = species[i];
            speci.resetAbundance();
            speci.resetBiomass();
            double sumExp = 0;
            abdIni = getOsmose().spBiomIniTab[numSerie][i] / (speci.tabMeanWeight[(int) Math.round(speci.getNumberCohorts() / 2)] / 1000000);

            for (int j = speci.indexAgeClass0; j < speci.getNumberCohorts(); j++) {
                sumExp += Math.exp(-(j * (speci.D + speci.F + 0.5f) / (float) nbDt)); //0.5 = approximation of average natural mortality (by predation, senecence...)
            }
            speci.tabAbdIni[0] = (long) ((abdIni) / (Math.exp(-speci.larvalSurvival / (float) nbDt) * (1 + sumExp)));
            speci.tabBiomIni[0] = ((double) speci.tabAbdIni[0]) * speci.tabMeanWeight[0] / 1000000.;
            if (speci.indexAgeClass0 <= 0) {
                speci.incrementBiomass(speci.tabBiomIni[0]);
            }

            speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbDt));
            speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
            if (speci.indexAgeClass0 <= 1) {
                speci.incrementBiomass(speci.tabBiomIni[1]);
            }

            for (int j = 2; j < speci.getNumberCohorts(); j++) {
                speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbDt));
                speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                if (speci.indexAgeClass0 <= j) {
                    speci.incrementBiomass(speci.tabBiomIni[j]);
                }
            }
            correctingFactor = (float) (getOsmose().spBiomIniTab[numSerie][i] / speci.getBiomass());

            // we make corrections on initial abundance to fit the input biomass
            speci.resetBiomass();

            speci.tabAbdIni[0] = (long) ((abdIni * correctingFactor) / (Math.exp(-speci.larvalSurvival / (float) nbDt) * (1 + sumExp)));
            speci.tabBiomIni[0] = ((double) speci.tabAbdIni[0]) * speci.tabMeanWeight[0] / 1000000.;
            speci.incrementAbundance(speci.tabAbdIni[0]);
            speci.incrementBiomass(speci.tabBiomIni[0]);
            /*
             * 2011/04/11 phv : commented line since nbEggs is only used in
             * Species.reproduce as local variable.
             */
            //speci.nbEggs = speci.tabAbdIni[0];

            speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbDt));
            speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
            speci.incrementAbundance(speci.tabAbdIni[1]);
            speci.incrementBiomass(speci.tabBiomIni[1]);

            for (int j = 2; j < speci.getNumberCohorts(); j++) {
                speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbDt));
                speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                speci.incrementAbundance(speci.tabAbdIni[j]);
                speci.incrementBiomass(speci.tabBiomIni[j]);
            }
            // and we create the cohorts
            for (int j = 0; j < speci.getNumberCohorts(); j++) {
                speci.setCohort(j, new Cohort(speci, j, speci.tabAbdIni[j], speci.tabBiomIni[j], speci.tabMeanLength[j], speci.tabMeanWeight[j]));
            }
        }
    }

    public void iniPlanktonField(boolean isForcing) {
        couple = new Coupling(isForcing);
        couple.iniCouplingReading(getOsmose().planktonStructureFileNameTab[numSerie]);
        couple.readInputPlanktonFiles(getOsmose().planktonFileNameTab[numSerie]);
        couple.initPlanktonMap();
    }

    public void randomOrder() {
        //schools are sorted randomly for predation  ---- ALL SCHOOLS
        int capaIni = 30 * 10 * species.length;
        Vector vectSchoolsRandom = new Vector(capaIni, 1);
        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                if (!species[i].getCohort(j).getOutOfZoneCohort()[dt]) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        vectSchoolsRandom.addElement(species[i].getCohort(j).getSchool(k));
                    }
                }
            }
        }

        vectSchoolsRandom.trimToSize();
        tabSchoolsRandom = new School[vectSchoolsRandom.size()];
        int z = 0;
        while (z < tabSchoolsRandom.length) {
            int random = (int) Math.round((vectSchoolsRandom.size() - 1) * Math.random());
            tabSchoolsRandom[z] = (School) vectSchoolsRandom.elementAt(random);
            vectSchoolsRandom.removeElementAt(random);
            z++;
        }
        vectSchoolsRandom.removeAllElements();
        vectSchoolsRandom.trimToSize();
    }

    public void rankSchoolsSizes() {
        Grid grid = getGrid();
        int dummy;
        for (int i = 0; i < grid.getNbLines(); i++) {
            for (int j = 0; j < grid.getNbColumns(); j++) {
                int[] indexSchoolsSizes = new int[grid.getCell(i, j).size()];
                for (int k = 0; k < grid.getCell(i, j).size(); k++) {
                    indexSchoolsSizes[k] = k;
                }
                for (int k1 = 0; k1 < grid.getCell(i, j).size(); k1++) {
                    for (int k2 = k1 + 1; k2 < grid.getCell(i, j).size(); k2++) {
                        if (((School) grid.getCell(i, j).get(indexSchoolsSizes[k1])).getLength()
                                > ((School) grid.getCell(i, j).get(indexSchoolsSizes[k2])).getLength()) {
                            dummy = indexSchoolsSizes[k1];
                            indexSchoolsSizes[k1] = indexSchoolsSizes[k2];
                            indexSchoolsSizes[k2] = dummy;
                        }
                    }
                }
                School[] tabSchoolsTemp = new School[grid.getCell(i, j).size()];
                for (int k = 0; k < tabSchoolsTemp.length; k++) {
                    tabSchoolsTemp[k] = (School) grid.getCell(i, j).get(indexSchoolsSizes[k]);
                }
                grid.getCell(i, j).clear();
                for (int k = 0; k < tabSchoolsTemp.length; k++) {
                    grid.getCell(i, j).add(tabSchoolsTemp[k]);
                }
            }
        }
    }

    //update remaining schools in coh.vectSchools & vectPresentSchools(according to disappears)
    public void assessNbSchools() {
        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                for (int k = species[i].getCohort(j).size() - 1; k >= 0; k--) {
                    if (((School) species[i].getCohort(j).getSchool(k)).willDisappear()) {
                        species[i].getCohort(j).remove(k);
                    }
                }
                species[i].getCohort(j).trimToSize();
            }
        }
    }

    public void assessPresentSchools() {
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                for (int k = getGrid().getCell(i, j).size() - 1; k >= 0; k--) {
                    if (((School) getGrid().getCell(i, j).getSchool(k)).willDisappear()) {
                        getGrid().getCell(i, j).remove(k);
                    }
                }
                getGrid().getCell(i, j).trimToSize();
            }
        }
    }

    public void assessAbdCohSpec() {
        //update abd cohort and abd species
        for (int i = 0; i < species.length; i++) {
            species[i].resetAbundance();
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                species[i].getCohort(j).setAbundance(0);
            }
        }
        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                    species[i].getCohort(j).setAbundance(species[i].getCohort(j).getAbundance() + ((School) species[i].getCohort(j).getSchool(k)).getAbundance());
                }
                species[i].incrementAbundance(species[i].getCohort(j).getAbundance());
            }
        }
    }

    public void distributeSpeciesIni() //***NEW: correspond to distributeSpecies for initialization
    {
        if (randomDistribution) {
            for (int i = 0; i < species.length; i++) {
                Vector vectCells = new Vector(getOsmose().randomAreaCoordi[i].length);
                for (int m = 0; m < getOsmose().randomAreaCoordi[i].length; m++) {
                    vectCells.addElement(getGrid().getCell(getOsmose().randomAreaCoordi[i][m], getOsmose().randomAreaCoordj[i][m]));
                }
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        ((School) species[i].getCohort(j).getSchool(k)).randomDeal(vectCells);
                        ((School) species[i].getCohort(j).getSchool(k)).communicatePosition();
                    }
                }
            }
        } else//species areas given by file
        {
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    if (!species[i].getCohort(j).getOutOfZoneCohort()[0]) // 0=at the first time step
                    {
                        Vector vectCells = new Vector(getOsmose().mapCoordi[getOsmose().numMap[i][j][0]].length);
                        tempMaxProbaPresence = 0;
                        for (int m = 0; m < getOsmose().mapCoordi[getOsmose().numMap[i][j][0]].length; m++) {
                            vectCells.addElement(getGrid().getCell(getOsmose().mapCoordi[getOsmose().numMap[i][j][0]][m], getOsmose().mapCoordj[getOsmose().numMap[i][j][0]][m]));
                            tempMaxProbaPresence = Math.max(tempMaxProbaPresence, (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][0]][m]));
                        }
                        for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                            School thisSchool = (School) species[i].getCohort(j).getSchool(k);
                            thisSchool.setOutOfZoneSchool(true);
                        }

                        for (int k = 0; k < Math.round((float) species[i].getCohort(j).size() * (1 - species[i].getCohort(j).getOutOfZonePercentage()[0] / 100)); k++) {
                            // proba of presence: loop while to check if proba of presence> random proba
                            School thisSchool = (School) species[i].getCohort(j).getSchool(k);
                            thisSchool.randomDeal(vectCells);
                            thisSchool.setOutOfZoneSchool(false);
                            while ((float) getOsmose().mapProbaPresence[getOsmose().numMap[i][j][0]][thisSchool.indexij] < (float) Math.random() * tempMaxProbaPresence) {
                                thisSchool.randomDeal(vectCells);
                            }
                            thisSchool.communicatePosition();
                        }
                    }
                }
            }
        }//end file areas
    }

    public void distributeSpecies() {
        if (randomDistribution) {
            //distribute coh 0 & commPosition for 0 only, the others stay in the same cell
            for (int i = 0; i < species.length; i++) {
                Vector vectCells = new Vector(getOsmose().randomAreaCoordi[i].length);
                for (int m = 0; m < getOsmose().randomAreaCoordi[i].length; m++) {
                    vectCells.addElement(getGrid().getCell(getOsmose().randomAreaCoordi[i][m], getOsmose().randomAreaCoordj[i][m]));
                }
                for (int k = 0; k < species[i].getCohort(0).size(); k++) {
                    ((School) species[i].getCohort(0).getSchool(k)).randomDeal(vectCells);
                    ((School) species[i].getCohort(0).getSchool(k)).communicatePosition();
                }
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 1; k < species[i].getCohort(j).size(); k++) {
                        School thisSchool = (School) (species[i].getCohort(j).getSchool(k));
                        //  ((QSchool)species[i].getCohort(j).getSchool(k)).communicatePosition();
                        thisSchool.randomWalk();
                        thisSchool.communicatePosition();
                    }
                }
            }
        } else//species areas given by file
        {
            for (int i = 0; i < species.length; i++) {
                //		if(!species[i].getCohort(0).outOfZoneCohort[dt])  //distribute coh 0
                //		{
                Vector vectCellsCoh0 = new Vector();
                tempMaxProbaPresence = 0;
                for (int j = 0; j < getOsmose().mapCoordi[(getOsmose().numMap[i][0][dt])].length; j++) {
                    vectCellsCoh0.addElement(getGrid().getCell(getOsmose().mapCoordi[(getOsmose().numMap[i][0][dt])][j], getOsmose().mapCoordj[(getOsmose().numMap[i][0][dt])][j]));
                    tempMaxProbaPresence = Math.max(tempMaxProbaPresence, (getOsmose().mapProbaPresence[getOsmose().numMap[i][0][dt]][j]));
                }


                for (int k = 0; k < species[i].getCohort(0).size(); k++) {
                    School thisSchool = (School) species[i].getCohort(0).getSchool(k);
                    thisSchool.setOutOfZoneSchool(true);
                }
                for (int k = 0; k < Math.round((float) species[i].getCohort(0).size() * (1f - (species[i].getCohort(0).getOutOfZonePercentage()[dt] / 100f))); k++) {
                    School thisSchool = (School) species[i].getCohort(0).getSchool(k);
                    thisSchool.randomDeal(vectCellsCoh0);
                    thisSchool.setOutOfZoneSchool(false);
                    while ((float) getOsmose().mapProbaPresence[getOsmose().numMap[i][0][dt]][thisSchool.indexij] < (float) Math.random() * tempMaxProbaPresence) {
                        thisSchool.randomDeal(vectCellsCoh0);
                    }
                    thisSchool.communicatePosition();
                }
                //		}

                //compare areas (ages to end): age a, sem2 with age a+1, sem 1
                // if diff, distribute
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    int oldTime;
                    if (dt == 0) {
                        oldTime = nbDt - 1;
                    } else {
                        oldTime = dt - 1;
                    }

                    boolean idem = false;
                    if (getOsmose().numMap[i][j][dt] == getOsmose().numMap[i][j - 1][oldTime]) {
                        idem = true;
                    }

                    if (!idem)// distribute in new area
                    {
                        //		if(!species[i].getCohort(j).outOfZoneCohort[dt])      //if is in the area during the time step
                        //		{
                        Vector vectCellsCoh = new Vector();
                        tempMaxProbaPresence = 0;
                        for (int m = 0; m < getOsmose().mapCoordi[(getOsmose().numMap[i][j][dt])].length; m++) {
                            vectCellsCoh.addElement(getGrid().getCell(getOsmose().mapCoordi[(getOsmose().numMap[i][j][dt])][m], getOsmose().mapCoordj[(getOsmose().numMap[i][j][dt])][m]));
                            tempMaxProbaPresence = Math.max(tempMaxProbaPresence, (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][dt]][m]));
                        }
                        for (int k = 0; k < species[i].getCohort(j).size(); k++) // Loop to initialize outOfZoneSchool= true
                        {
                            ((School) species[i].getCohort(j).getSchool(k)).setOutOfZoneSchool(true);
                        }

                        for (int k = 0; k < Math.round((float) species[i].getCohort(j).size() * (100f - species[i].getCohort(j).getOutOfZonePercentage()[dt]) / 100f); k++) {
                            School thisSchool = (School) (species[i].getCohort(j).getSchool(k));
                            thisSchool.setOutOfZoneSchool(false);
                            thisSchool.randomDeal(vectCellsCoh);
                            while (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][dt]][thisSchool.indexij] < Math.random() * tempMaxProbaPresence) {
                                thisSchool.randomDeal(vectCellsCoh);
                            }
                            thisSchool.communicatePosition();
                        }

                        //		}

                    } else // stay in the same map
                    {
                        for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                            School thisSchool = (School) (species[i].getCohort(j).getSchool(k));
                            if (!thisSchool.isOutOfZoneSchool()) {
                                thisSchool.randomWalk();

                                boolean stillInMap = false;
                                for (int p = 0; p < thisSchool.getCell().getNbMapsConcerned(); p++) {
                                    if (((Integer) thisSchool.getCell().numMapsConcerned.elementAt(p)).intValue() == getOsmose().numMap[i][j][dt]) {
                                        stillInMap = true;
                                    }
                                }

                                if (!stillInMap) {
                                    Vector vectCellsCoh = new Vector();
                                    tempMaxProbaPresence = 0;
                                    for (int m = 0; m < getOsmose().mapCoordi[(getOsmose().numMap[i][j][dt])].length; m++) {
                                        vectCellsCoh.addElement(getGrid().getCell(getOsmose().mapCoordi[(getOsmose().numMap[i][j][dt])][m], getOsmose().mapCoordj[(getOsmose().numMap[i][j][dt])][m]));
                                        tempMaxProbaPresence = Math.max(tempMaxProbaPresence, (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][dt]][m]));
                                    }

                                    while (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][dt]][thisSchool.indexij] < Math.random() * tempMaxProbaPresence) {
                                        thisSchool.randomDeal(vectCellsCoh);
                                    }
                                }
                                thisSchool.communicatePosition();
                            }
                        }
                    }
                }//end loop cohort
            }//end loop species
        }//end file areas
    }

    public void assessCatchableSchools() {

        if ((!getOsmose().thereIsMPATab[numSerie])
                || (t < getOsmose().MPAtStartTab[numSerie])
                || (t >= getOsmose().MPAtEndTab[numSerie]))// case where no MPA
        {
            for (int i = 0; i < species.length; i++) {
                species[i].nbSchoolsTotCatch = 0;
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    Cohort cohij = species[i].getCohort(j);
                    cohij.setNbSchoolsCatchable(cohij.size());
                    //cohij.schoolsCatchable = new Vector(cohij.nbSchoolsCatchable);
                    cohij.setAbundanceCatchable(0);
                    for (int k = 0; k < cohij.size(); k++) {
                        School schoolk = (School) cohij.getSchool(k);
                        //cohij.schoolsCatchable.addElement(schoolk);
                        cohij.setAbundanceCatchable(cohij.getAbundanceCatchable() + schoolk.getAbundance());
                        schoolk.setCatchable(true);
                    }
                }
                species[i].cumulCatch[0] = 0;
                species[i].cumulCatch[0] = species[i].getCohort(0).getNbSchoolsCatchable();
                species[i].nbSchoolsTotCatch += species[i].getCohort(0).getNbSchoolsCatchable();
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    species[i].cumulCatch[j] = 0;
                    species[i].cumulCatch[j] = species[i].cumulCatch[j - 1] + species[i].getCohort(j).getNbSchoolsCatchable();
                    species[i].nbSchoolsTotCatch += species[i].getCohort(j).getNbSchoolsCatchable();
                }
            }
        } else//case MPA
        {
            for (int i = 0; i < species.length; i++) {
                species[i].nbSchoolsTotCatch = 0;
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    Cohort cohij = species[i].getCohort(j);
                    cohij.setNbSchoolsCatchable(0);
                    //cohij.schoolsCatchable = new Vector(getOsmose().nbSchools[numSerie]);
                    cohij.setAbundanceCatchable(0);
                    for (int k = 0; k < cohij.size(); k++) {
                        School schoolk = (School) cohij.getSchool(k);
                        if (schoolk.getCell().isMPA()) {
                            schoolk.setCatchable(false);
                        } else {
                            schoolk.setCatchable(true);
                            cohij.setNbSchoolsCatchable(cohij.getNbSchoolsCatchable() + 1);
                            //cohij.schoolsCatchable.addElement(schoolk);
                            cohij.setAbundanceCatchable(cohij.getAbundanceCatchable() + schoolk.getAbundance());
                        }
                    }
                    //cohij.schoolsCatchable.trimToSize();
                }
                species[i].cumulCatch[0] = 0;
                species[i].cumulCatch[0] = species[i].getCohort(0).getNbSchoolsCatchable();
                species[i].nbSchoolsTotCatch += species[i].getCohort(0).getNbSchoolsCatchable();
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    species[i].cumulCatch[j] = 0;
                    species[i].cumulCatch[j] = species[i].cumulCatch[j - 1] + species[i].getCohort(j).getNbSchoolsCatchable();
                    species[i].nbSchoolsTotCatch += species[i].getCohort(j).getNbSchoolsCatchable();
                }
            }
        }
    }

    public void initSaving() {
        abdTemp = new long[species.length];
        abdTempWithout0 = new long[species.length];
        biomTemp = new double[species.length];
        biomTempWithout0 = new double[species.length];
        savingYield = new float[species.length];
        savingNbYield = new long[species.length];
        tabTLCatch = new float[species.length];
        biomPerStage = new double[nbSpecies + couple.nbPlankton][];

        if (meanSizeOutput) {
            meanSizeTemp = new float[species.length];
            meanSizeCatchTemp = new float[species.length];
        }
        if (TLoutput) {
            meanTLtemp = new float[species.length];
            for (int i = 0; i < species.length; i++) {
                tabTLCatch[i] = 0;
            }
        }


        for (int i = 0; i < species.length; i++) {
            abdTemp[i] = 0;
            abdTempWithout0[i] = 0;
            biomTemp[i] = 0;
            biomTempWithout0[i] = 0;
            savingYield[i] = 0;
            savingNbYield[i] = 0;
            biomPerStage[i] = new double[species[i].nbDietStages];
            for (int j = 0; j < species[i].nbDietStages; j++) {
                biomPerStage[i][j] = 0;
            }
            if (meanSizeOutput) {
                meanSizeTemp[i] = 0f;
                meanSizeCatchTemp[i] = 0f;
            }
            if (TLoutput) {
                meanTLtemp[i] = 0f;
            }
        }
        for (int i = species.length; i < species.length + couple.nbPlankton; i++) {
            biomPerStage[i] = new double[1];                  // only & stage per plankton group
            biomPerStage[i][0] = 0;
        }


        if (dietsOutput) {
            nbStomachs = new long[species.length][];
            dietsMatrix = new float[species.length][][][];
            predatorsPressureMatrix = new float[species.length][][][];
            for (int i = 0; i < species.length; i++) {
                nbStomachs[i] = new long[species[i].nbDietStages];
                dietsMatrix[i] = new float[species[i].nbDietStages][][];
                predatorsPressureMatrix[i] = new float[species[i].nbDietStages][][];
                for (int s = 0; s < species[i].nbDietStages; s++) {
                    nbStomachs[i][s] = 0;
                    dietsMatrix[i][s] = new float[species.length + couple.nbPlankton][];
                    predatorsPressureMatrix[i][s] = new float[species.length + couple.nbPlankton][];
                    for (int j = 0; j < species.length; j++) {
                        dietsMatrix[i][s][j] = new float[species[j].nbDietStages];
                        predatorsPressureMatrix[i][s][j] = new float[species[j].nbDietStages];
                        for (int st = 0; st < species[j].nbDietStages; st++) {
                            dietsMatrix[i][s][j][st] = 0f;
                            predatorsPressureMatrix[i][s][j][st] = 0f;
                        }
                    }
                    for (int j = species.length; j < species.length + couple.nbPlankton; j++) {
                        dietsMatrix[i][s][j] = new float[1];
                        dietsMatrix[i][s][j][0] = 0f;
                        predatorsPressureMatrix[i][s][j] = new float[1];
                        predatorsPressureMatrix[i][s][j][0] = 0f;
                    }
                }
            }
        }

        countTemp = new int[species.length][];
        for (int i = 0; i < species.length; i++) {
            countTemp[i] = new int[species[i].getNumberCohorts()];
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                countTemp[i][j] = 0;
            }
        }

        if (TLoutput) {
            meanTLperAgeTemp = new float[species.length][];
            for (int i = 0; i < species.length; i++) {
                meanTLperAgeTemp[i] = new float[species[i].getNumberCohorts()];
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    meanTLperAgeTemp[i][j] = 0;
                }
            }
        }
        if (TLDistriboutput) {
            distribTL = new float[species.length][][];
            for (int i = 0; i < species.length; i++) {
                distribTL[i] = new float[2][];
                distribTL[i][0] = new float[getOsmose().nbTLClass];    // age 0
                distribTL[i][1] = new float[getOsmose().nbTLClass];    // without age 0
                for (int j = 0; j < getOsmose().nbTLClass; j++) {
                    distribTL[i][0][j] = 0;
                    distribTL[i][1][j] = 0;
                }
            }
        }
        //ORGANIZING SIZE CLASSES of the spectrum at INITIALIZATION
        //spectrumAbd = new float[getOsmose().nbSizeClass];
        //spectrumBiom = new float[getOsmose().nbSizeClass];

        if (sizeSpectrumPerSpeOutput) {
            spectrumSpeciesAbd = new float[species.length][];
            spectrumTemp = new float[2][][];
            spectrumTemp[0] = new float[species.length][];
            spectrumTemp[1] = new float[species.length][];
            for (int i = 0; i < species.length; i++) {
                spectrumSpeciesAbd[i] = new float[getOsmose().nbSizeClass];
                spectrumTemp[0][i] = new float[getOsmose().nbSizeClass];
                spectrumTemp[1][i] = new float[getOsmose().nbSizeClass];
            }
            //calculation of spectrum values
            for (int i = 0; i < getOsmose().nbSizeClass; i++) {
                //   spectrumAbd[i]=0;
                //   spectrumBiom[i]=0;
                for (int j = 0; j < species.length; j++) {
                    spectrumSpeciesAbd[j][i] = 0;
                    spectrumTemp[0][j][i] = 0;
                    spectrumTemp[1][j][i] = 0;
                }
            }
        }

        initAbdFile();
        initBiomFile();
        initYieldFile();
        initNbYieldFile();

        if (outputClass0) {
            initAbd0File();
            initBiom0File();
        }
        if (meanSizeOutput) {
            initMeanSizeFile();
            initMeanSizeCatchFile();
        }

        if (TLoutput) {
            initMeanTLFile();
            initMeanTLCatchFile();
        }
        if (dietsOutput) {
            initDietFile();
            initPredatorPressureFile();
        }
        if (TLDistriboutput) {
            initTLDistFile();
        }

        if (sizeSpectrumPerSpeOutput) {
            initSizeSpecPerSpFile();
            initSizeSpecPerSpCatchFile();
        }
        if (sizeSpectrumOutput) {
            initSizeSpecFile();
        }
    }

    public void saveStep() {

        int indexSaving;
        indexSaving = (int) dt / savingDt;
        double biomNo0;
        long abdNo0;

        if (t >= getOsmose().timeSeriesStart) {
            for (int i = 0; i < species.length; i++) {
                Species speci = species[i];

                abdTemp[i] += speci.getAbundance();
                biomTemp[i] += speci.getBiomass();

                if (sizeSpectrumPerSpeOutput) {
                    for (int j = 0; j < getOsmose().nbSizeClass; j++) {
                        spectrumTemp[0][i][j] += spectrumSpeciesAbd[i][j];
                    }
                }

                //biom per stage rempli avant la prédation
				/*               for (int j=0; j< speci.getNumberCohorts(); j++)
                for (int k=0; k<speci.getCohort(j).nbSchools; k++)
                biomPerStage[i][((QSchool)speci.getCohort(j).getSchool(k)).stage] += ((QSchool)speci.getCohort(j).getSchool(k)).biomass;
                 */
                abdNo0 = 0;
                biomNo0 = 0;
                for (int k = speci.indexAgeClass0; k < speci.getNumberCohorts(); k++) {
                    abdTempWithout0[i] += speci.getCohort(k).getAbundance();
                    biomTempWithout0[i] += speci.getCohort(k).getBiomass();
                    biomNo0 += speci.getCohort(k).getBiomass();
                    abdNo0 += speci.getCohort(k).getAbundance();

                }

                if (meanSizeOutput) {
                    meanSizeTemp[i] += speci.meanSizeSpe * (float) abdNo0;
                    meanSizeCatchTemp[i] += speci.meanSizeSpeCatch * (float) savingNbYield[i];
                }
                if (TLoutput) {
                    meanTLtemp[i] += speci.meanTLSpe * (float) biomNo0;
                    for (int j = 0; j < speci.getNumberCohorts(); j++) {
                        if (speci.meanTLperAge[j] != 0) {
                            meanTLperAgeTemp[i][j] += speci.meanTLperAge[j];
                            countTemp[i][j] += 1;
                        }
                    }
                }
            }

            if (dtCount == savingDt) {
                float timeSaving = (float) t + (dt + (savingDt / 2f) + 1f) / (float) nbDt;
                timeSaving = t + (dt + 1f) / (float) nbDt;
                saveABDperTime(timeSaving, abdTempWithout0);
                saveBIOMperTime(timeSaving, biomTempWithout0);

                if (outputClass0) {
                    saveABD0perTime(timeSaving, abdTemp);
                    saveBIOM0perTime(timeSaving, biomTemp);
                }
                saveYieldperTime(timeSaving, savingYield);
                saveNbYieldperTime(timeSaving, savingNbYield);
                if (meanSizeOutput) {
                    saveMeanSizeperTime(timeSaving, meanSizeTemp, abdTempWithout0);
                    saveMeanSizeCatchperTime(timeSaving, meanSizeCatchTemp, savingNbYield);
                }
                if (TLoutput) {
                    saveMeanTLperTime(timeSaving, meanTLtemp, biomTempWithout0);
                    saveMeanTLCatchperTime(timeSaving, tabTLCatch, savingYield);
                    saveMeanTLperAgeperTime(timeSaving, meanTLperAgeTemp, countTemp);
                    /*
                    if(getOsmose().TLoutput)
                    {
                    for (int j=0;j<species[i].getNumberCohorts();j++)
                    getOsmose().TLperAgeMatrix[getOsmose().numSimu][i][j][t-getOsmose().timeSeriesStart][indexSaving] = meanTLperAgeTemp[i][j]/countTemp[i][j];
                    }
                    if(getOsmose().TLoutput)
                    {
                    for (int j=0;j<species[i].getNumberCohorts();j++)
                    {
                    meanTLperAgeTemp[i][j]=0;
                    countTemp[i][j] = 0;
                    }
                    }
                     */
                }
                if (dietsOutput) {
                    saveDietperTime(timeSaving, dietsMatrix, nbStomachs);
                    savePredatorPressureperTime(timeSaving, predatorsPressureMatrix, biomPerStage);
                }
                if (TLDistriboutput) {
                    saveTLDistperTime(timeSaving, distribTL);
                }

                if (sizeSpectrumOutput) {
                    saveSizeSpecperTime(timeSaving, spectrumTemp[0]);
                }
                if (sizeSpectrumPerSpeOutput) {
                    saveSizeSpecPerSpperTime(timeSaving, spectrumTemp[0]);
                    saveSizeSpecPerSpperCatchTime(timeSaving, spectrumTemp[1]);
                }
                if (calibration) {
                    for (int i = 0; i < species.length; i++) {
                        getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][t - getOsmose().timeSeriesStart][indexSaving] = (float) biomTempWithout0[i] / savingDt;
                        getOsmose().BIOMQuadri[getOsmose().numSimu][i][1][t - getOsmose().timeSeriesStart][indexSaving] = (float) biomTemp[i] / savingDt;
                    }
                }
                for (int i = species.length; i < species.length + couple.nbPlankton; i++) {
                    if (calibration) {
                        getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][t - getOsmose().timeSeriesStart][indexSaving] = (float) biomPerStage[i][0] / savingDt;
                    }
                    biomPerStage[i][0] = 0;
                }

                // clear all saving tables
                for (int i = 0; i < species.length; i++) {
                    abdTemp[i] = 0;
                    abdTempWithout0[i] = 0;
                    biomTemp[i] = 0;
                    biomTempWithout0[i] = 0;
                    savingYield[i] = 0;
                    savingNbYield[i] = 0;
                    tabTLCatch[i] = 0;

                    if (meanSizeOutput) {
                        meanSizeTemp[i] = 0;
                        meanSizeCatchTemp[i] = 0;
                    }

                    if (TLoutput) {
                        meanTLtemp[i] = 0;
                        for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                            meanTLperAgeTemp[i][j] = 0;
                            countTemp[i][j] = 0;
                        }
                    }

                    if (dietsOutput) {
                        for (int s = 0; s < species[i].nbDietStages; s++) {
                            nbStomachs[i][s] = 0;
                            biomPerStage[i][s] = 0;
                            for (int j = 0; j < species.length; j++) {
                                for (int st = 0; st < species[j].nbDietStages; st++) {
                                    dietsMatrix[i][s][j][st] = 0f;
                                    predatorsPressureMatrix[i][s][j][st] = 0f;
                                }
                            }
                            for (int j = species.length; j < species.length + couple.nbPlankton; j++) {
                                dietsMatrix[i][s][j][0] = 0f;
                                predatorsPressureMatrix[i][s][j][0] = 0f;
                            }
                        }
                    }
                    if (TLDistriboutput) {
                        for (int j = 0; j < getOsmose().nbTLClass; j++) {
                            distribTL[i][0][j] = 0;
                            distribTL[i][1][j] = 0;
                        }
                    }
                    if (sizeSpectrumPerSpeOutput) {
                        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
                            spectrumTemp[0][i][j] = 0;
                            spectrumTemp[1][i][j] = 0;
                        }
                    }
                } // end clearing loop over species

                dtCount = 0;

            }   // end loop dtcount=dtSaving
        }
    }

    public void initPredatorPressureFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_predatorPressureMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(dietTime, true);
        pr.print("Time");
        pr.print(';');
        pr.print("Prey");
        for (int i = 0; i < species.length; i++) {
            for (int s = 0; s < species[i].nbDietStages; s++) {
                pr.print(";");
                if (species[i].nbDietStages == 1) {
                    pr.print(species[i].getName());    // Name predators
                } else {
                    if (s == 0) {
                        pr.print(species[i].getName() + " < " + species[i].dietStagesTab[s]);    // Name predators
                    } else {
                        pr.print(species[i].getName() + " >" + species[i].dietStagesTab[s - 1]);    // Name predators
                    }
                }
            }
        }
        pr.print(";");
        pr.print("Biomass");
        pr.println();
        pr.close();
    }

    public void initDietFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_dietMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(dietTime, true);
        pr.print("Time");
        pr.print(';');
        pr.print("Prey");
        for (int i = 0; i < species.length; i++) {
            for (int s = 0; s < species[i].nbDietStages; s++) {
                pr.print(";");
                if (species[i].nbDietStages == 1) {
                    pr.print(species[i].getName());    // Name predators
                } else {
                    if (s == 0) {
                        pr.print(species[i].getName() + " < " + species[i].dietStagesTab[s]);    // Name predators
                    } else {
                        pr.print(species[i].getName() + " >" + species[i].dietStagesTab[s - 1]);    // Name predators
                    }
                }
            }
        }
        pr.print(";");
        pr.print("nbStomachs");
        pr.println();
        pr.close();
    }

    public void savePredatorPressureperTime(float time, float[][][][] diets, double[][] biom) {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_predatorPressureMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(dietTime, true);


        for (int j = 0; j < species.length; j++) {
            for (int st = 0; st < species[j].nbDietStages; st++) {
                pr.print(time);
                pr.print(';');
                if (species[j].nbDietStages == 1) {
                    pr.print(species[j].getName());    // Name predators
                } else {
                    if (st == 0) {
                        pr.print(species[j].getName() + " < " + species[j].dietStagesTab[st]);    // Name predators
                    } else {
                        pr.print(species[j].getName() + " >" + species[j].dietStagesTab[st - 1]);    // Name predators
                    }
                }
                pr.print(";");
                for (int i = 0; i < species.length; i++) {
                    for (int s = 0; s < species[i].nbDietStages; s++) {
                        pr.print(diets[i][s][j][st] / savingDt);
                        pr.print(";");
                    }
                }
                pr.print(biom[j][st] / savingDt);
                pr.println();
            }
        }
        for (int j = species.length; j < (species.length + couple.nbPlankton); j++) {
            pr.print(time);
            pr.print(";");
            pr.print(couple.planktonNames[j - species.length]);
            pr.print(";");
            for (int i = 0; i < species.length; i++) {
                for (int s = 0; s < species[i].nbDietStages; s++) // 4 Stages
                {
                    pr.print(diets[i][s][j][0] / savingDt);
                    pr.print(";");
                }
            }
            pr.print(biom[j][0] / savingDt);
            pr.println();
        }
        pr.close();
    }

    public void saveDietperTime(float time, float[][][][] diets, long[][] nbStomachs) {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_dietMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(dietTime, true);


        for (int j = 0; j < species.length; j++) {
            for (int st = 0; st < species[j].nbDietStages; st++) {
                pr.print(time);
                pr.print(';');
                if (species[j].nbDietStages == 1) {
                    pr.print(species[j].getName());    // Name predators
                } else {
                    if (st == 0) {
                        pr.print(species[j].getName() + " < " + species[j].dietStagesTab[st]);    // Name predators
                    } else {
                        pr.print(species[j].getName() + " >" + species[j].dietStagesTab[st - 1]);    // Name predators
                    }
                }
                pr.print(";");
                for (int i = 0; i < species.length; i++) {
                    for (int s = 0; s < species[i].nbDietStages; s++) {
                        if (nbStomachs[i][s] != 0) {
                            pr.print(diets[i][s][j][st] / (float) nbStomachs[i][s]);
                        } else {
                            pr.print("NaN");
                        }
                        pr.print(";");
                    }
                }
                pr.print(nbStomachs[j][st]);
                pr.println();
            }
        }
        for (int j = species.length; j < (species.length + couple.nbPlankton); j++) {
            pr.print(time);
            pr.print(";");
            pr.print(couple.planktonNames[j - species.length]);
            pr.print(";");
            for (int i = 0; i < species.length; i++) {
                for (int s = 0; s < species[i].nbDietStages; s++) {
                    if (nbStomachs[i][s] != 0) {
                        pr.print(diets[i][s][j][0] / (float) nbStomachs[i][s]);
                    } else {
                        pr.print("NaN");
                    }
                    pr.print(";");
                }
            }
            pr.println();
        }
        pr.close();
    }

    public void initBiomFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String biomFile = getOsmose().outputFileNameTab[numSerie] + "_biomass_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, biomFile);
            biomTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(biomTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        /*        for (int i=0;i<couple.nbPlankton;i++)
        {
        pr.print(";");
        pr.print(couple.planktonList[i].name);
        }*/
        pr.println();
        pr.close();
    }

    public void initBiom0File() {
        File targetPath, targetFile;
        PrintWriter pr;
        String biomFile = getOsmose().outputFileNameTab[numSerie] + "_biomassClass0_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, biomFile);
            biomTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(biomTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }

        pr.println();
        pr.close();
    }

    public void initAbdFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String abdFile = getOsmose().outputFileNameTab[numSerie] + "_abundance_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, abdFile);
            abdTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(abdTime, true);

        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initAbd0File() {
        File targetPath, targetFile;
        PrintWriter pr;
        String abdFile = getOsmose().outputFileNameTab[numSerie] + "_abundanceClass0_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, abdFile);
            abdTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(abdTime, true);

        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initYieldFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String yieldFile = getOsmose().outputFileNameTab[numSerie] + "_yield_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, yieldFile);
            yieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(yieldTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initNbYieldFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String nbYieldFile = getOsmose().outputFileNameTab[numSerie] + "_yieldNB_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, nbYieldFile);
            nbYieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(nbYieldTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initMeanSizeFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanSizeFile = getOsmose().outputFileNameTab[numSerie] + "_meanSize_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanSizeFile);
            meanSizeTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanSizeTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initMeanSizeCatchFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanSizeFile = getOsmose().outputFileNameTab[numSerie] + "_meanSizeCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanSizeFile);
            meanSizeTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanSizeTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initMeanTLFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTL_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initMeanTLCatchFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTLCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }

        pr.println();
        pr.close();
    }

    public void saveABDperTime(float time, long[] A) {
        File targetPath, targetFile;
        PrintWriter pr;
        String abdFile = getOsmose().outputFileNameTab[numSerie] + "_abundance_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, abdFile);
            abdTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(abdTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(A[i] / (float) savingDt);
        }
        pr.println();
        pr.close();
    }

    public void saveABD0perTime(float time, long[] A) {
        File targetPath, targetFile;
        PrintWriter pr;
        String abdFile = getOsmose().outputFileNameTab[numSerie] + "_abundanceClass0_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, abdFile);
            abdTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(abdTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(A[i] / (float) savingDt);
        }
        pr.println();
        pr.close();
    }

    public void saveBIOMperTime(float time, double[] B) {
        File targetPath, targetFile;
        PrintWriter pr;
        String biomFile = getOsmose().outputFileNameTab[numSerie] + "_biomass_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();
        try {
            targetFile = new File(targetPath, biomFile);
            biomTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(biomTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(B[i] / (float) savingDt);
        }
        pr.println();
        pr.close();
    }

    public void saveBIOM0perTime(float time, double[] B) {
        File targetPath, targetFile;
        PrintWriter pr;
        String biomFile = getOsmose().outputFileNameTab[numSerie] + "_biomassClass0_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();
        try {
            targetFile = new File(targetPath, biomFile);
            biomTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(biomTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(B[i] / (float) savingDt);
        }
        pr.println();
        pr.close();
    }

    public void saveYieldperTime(float time, float[] Y) {
        File targetPath, targetFile;
        PrintWriter pr;
        String yieldFile = getOsmose().outputFileNameTab[numSerie] + "_yield_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, yieldFile);
            yieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(yieldTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(Y[i]);
        }
        pr.println();
        pr.close();
    }

    public void saveNbYieldperTime(float time, long[] nY) {
        File targetPath, targetFile;
        PrintWriter pr;
        String nbYieldFile = getOsmose().outputFileNameTab[numSerie] + "_yieldNB_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, nbYieldFile);
            nbYieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(nbYieldTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(nY[i]);
        }
        pr.println();
        pr.close();
    }

    public void saveMeanSizeperTime(float time, float[] mL, long[] abd) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanSizeFile = getOsmose().outputFileNameTab[numSerie] + "_meanSize_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanSizeFile);
            meanSizeTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanSizeTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            if (abd[i] != 0) {
                pr.print((mL[i] / (float) abd[i]));
            } else {
                pr.print("NaN");
            }
        }
        pr.println();
        pr.close();
    }

    public void saveMeanSizeCatchperTime(float time, float[] mLY, long[] abd) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanSizeFile = getOsmose().outputFileNameTab[numSerie] + "_meanSizeCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanSizeFile);
            meanSizeTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanSizeTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            if (abd[i] != 0) {
                pr.print((mLY[i] / (float) abd[i]));
            } else {
                pr.print("NaN");
            }
        }
        pr.println();
        pr.close();
    }

    public void saveMeanTLperTime(float time, float[] mTL, double[] biom) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTL_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            if (biom[i] != 0) {
                pr.print((mTL[i] / (float) biom[i]));
            } else {
                pr.print("NaN");
            }
        }
        pr.println();
        pr.close();
    }

    public void saveMeanTLCatchperTime(float time, float[] mTL, float[] biom) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTLCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            if (biom[i] != 0) {
                pr.print((mTL[i] / (float) biom[i]));
            } else {
                pr.print("NaN");
            }
        }
        pr.println();
        pr.close();
    }

    public void saveMeanTLperAgeperTime(float time, float[][] mTL, int[][] nb) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTLperAge_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);

        pr.println(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                pr.print(";");
                if (nb[i][j] != 0) {
                    pr.print((mTL[i][j] / (float) nb[i][j]));
                } else {
                    pr.print("NaN");
                }
            }
            pr.println();
        }
        pr.println();
        pr.close();
    }

    public void initTLDistFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String TLDistFile = getOsmose().outputFileNameTab[numSerie] + "_TLDistrib_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, TLDistFile);
            TLDistTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(TLDistTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("TL");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName() + " -0");
            pr.print(';');
            pr.print(species[i].getName() + " -1+");
            pr.print(';');
        }
        pr.println();
        pr.close();
    }

    public void saveTLDistperTime(float time, float[][][] TLdist) {
        File targetPath, targetFile;
        PrintWriter pr;
        String TLDistFile = getOsmose().outputFileNameTab[numSerie] + "_TLDistrib_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, TLDistFile);
            TLDistTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(TLDistTime, true);

        for (int j = 0; j < getOsmose().nbTLClass; j++) {
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabTL[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(TLdist[i][0][j] / (float) savingDt);
                pr.print(';');
                pr.print(TLdist[i][1][j] / (float) savingDt);
                pr.print(';');
            }
            pr.println();
        }
        pr.close();
    }

    public void initSizeSpecPerSpFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpecies_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("size");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            pr.print(';');
        }
        pr.print("LN(size)");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            pr.print(';');
        }
        pr.println();
        pr.close();
    }

    public void saveSizeSpecPerSpperTime(float time, float[][] abdSize) {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpecies_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabSizes[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(abdSize[i][j] / (float) savingDt);
                pr.print(';');
            }
            pr.print((getOsmose().tabSizesLn[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(Math.log(abdSize[i][j] / (float) savingDt));
                pr.print(';');
            }
            pr.println();
        }
        pr.close();
    }

    public void initSizeSpecPerSpCatchFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpeciesCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("size");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            pr.print(';');
        }
        pr.println();
        pr.close();
    }

    public void saveSizeSpecPerSpperCatchTime(float time, float[][] abdSize) {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpeciesCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabSizes[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(abdSize[i][j] / (float) savingDt);
                pr.print(';');
            }
            pr.println();
        }
        pr.close();
    }

    public void initSizeSpecFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrum_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("size");
        pr.print(';');
        pr.print("Abundance");
        pr.print(';');
        pr.print("LN(size)");
        pr.print(';');
        pr.print("LN(Abd)");
        pr.print(';');
        pr.println();
        pr.close();
    }

    public void saveSizeSpecperTime(float time, float[][] abdSize) {
        float sum;
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrum_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
            sum = 0f;
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabSizes[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                sum += abdSize[i][j] / (float) savingDt;
            }
            pr.print(sum);
            pr.print(';');
            pr.print((getOsmose().tabSizesLn[j]));
            pr.print(';');
            pr.print(Math.log(sum));
            pr.println();
        }
        pr.close();
    }
}
