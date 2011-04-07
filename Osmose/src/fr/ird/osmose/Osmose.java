package fr.ird.osmose;

/*******************************************************************************
 * <p>Titre : Osmose </p>
 *
 * <p>Description : Main class of the Osmose model - reads the input files and initialize the series and simulations
 * - save the biomass file in case of calibration - run the simulations </p>
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

public class Osmose {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/07 phv
     * public void distribRandom(). Recoded the random sorting of the adjacent
     * cells using method Grid.getAdjacentCells since Cell.neighbors[] has been
     * deleted.
     * ***
     */

    String OS_NAME = System.getProperty("os.name");
    String fileSeparator = System.getProperty("file.separator");
    String inputPathName, outputPathName;
    // Name of the initial file read by the program, where the path to all input files is specified.
    // This "filePath.txt" file must be located in the same folder than folder \src of the .java files
    String initialPathFile = "filePath.txt";

    /* ***************************** DECLARING VARIABLES ******************************** */
    Simulation simulation;
    Grid grid;
    int nbSeriesSimus;	// nb of series
    int[] nbLoopTab;	// nb of simulations per serie
    int numSerie, numSimu;
    // TABLES OF INPUT FILES NAMES (one entry per serie)
    //for 1 serie of simus, same species parameters and options
    String[] configFileNameTab, speciesFileNameTab, predationFileNameTab, fishingFileNameTab,
            planktonFileNameTab, planktonStructureFileNameTab, calibrationFileNameTab, outputFileNameTab,
            indicatorsFileNameTab;
    // Tables of Optional Input Files names (with entries as file name or "default")
    String[] size0FileNameTab, migrationFileNameTab, accessibilitiesFileNameTab,
            reproductionFileNameTab, fishingSeasonFileNameTab, /* � creer*/
            couplingFileNameTab;
    boolean simInitialized = false;
    boolean[] isForcing;
    //INFOS in SPECIES PARAMETERS FILE
    int[] nbSpeciesTab;
    String[][] nameSpecMatrix, maturityMetricMatrix;
    String[] recruitMetricMatrix;
    float[][] DMatrix, larvalSurvivalMatrix, starvMaxRateMatrix;
    float[][] longevityMatrix;
    float[][] lInfMatrix, KMatrix, t0Matrix, cMatrix, bPowerMatrix;
    float[][] alphaMatrix, sizeMatMatrix;
    float[][][] seasonSpawningMatrix;
    float[][][] sizeFeedingMatrix, predPreySizesMinMatrix, predPreySizesMaxMatrix;
    int[][] nbStagesMatrix;
    float[][] recruitAgeMatrix;
    float[][] predationRateMatrix, criticalPredSuccessMatrix;
    float[][] eggSizeMatrix, eggWeightMatrix, sexRatioMatrix, growthAgeThresholdMatrix;
    float[][] supAgeOfClass0Matrix;
    // INFOS for FISHING
    String[] globalOrCohortTab;
    float[][] FMatrix, recruitSizeMatrix;
    float[][][] seasonFishingMatrix;
    // INFOS for PLANKTON groups
    int[] nbPlanktonGroupsTab;
    String[][] planktonNamesTab;
    //INFOS for CONFIG & OPTIONS
    int[] gridLinesTab, gridColumnsTab;
    float[] upLeftLatTab, lowRightLatTab, upLeftLongTab, lowRightLongTab;
    int[] simulationTimeTab, nbDtMatrix, savingDtMatrix, nbDtSavePerYear;
    //Parameters
    int[] nbSchools;
    float[] planktonAccessCoeffMatrix;
    int[] nbAccessStage;
    float[][] accessStageThreshold;
    float[][][][] accessibilityMatrix;
    // SPECIES AREAS FILE
    String[] areasFileNameTab;	              //choice between "Random" or fileName
    int[] speciesAreasSizeTab;	    //used only for Qsimulation.iniRepartitionAleat() ie for random distribution
    int[][] randomAreaCoordi, randomAreaCoordj;//species areas in random cases [species][cell]
    int[][][] numMap;        //gives a number of map for[species][cohort][dt]
    int[][] mapCoordi, mapCoordj;      //    coord of maps [numMap][cell]
    float[][] mapProbaPresence; // Probability of presence of a cohort [numMap][cell]
    boolean densityMaps;
    //coastline
    String[] coastFileNameTab;	              //choice between "None" or fileName
    int[][] tabCoastiMatrix, tabCoastjMatrix;   //coordinates of the cells representing land
    int[] nbCellsCoastTab;
    //MPAs coordinates
    String[] mpaFileNameTab;
    int[][] tabMPAiMatrix, tabMPAjMatrix;     //coord i et j of the matrix delimiting a mpa
    boolean[] thereIsMPATab;		      //signify that 1 mpa is implemented even if t<tStart
    int[] MPAtStartTab, MPAtEndTab;	      //start and end of MPA in years
    //tables for output storage by series of simulations
    //4 dimensions : simu, species,val(total OR total-0), step t
    //for mortalities, 3 dim, the last is for the mean on the simulation period
    float[][][][][] BIOMQuadri;   //[numSimu][species][with or without age 0][t][dt]
    float[][] iniBiomass; //used for saving the biomass after initialization
    // FOR INDICATORS OUTPUT
    boolean[] TLoutputMatrix, TLDistriboutputMatrix, dietsOutputMatrix, meanSizeOutputMatrix,
            sizeSpectrumOutputMatrix, sizeSpectrumPerSpeOutputMatrix,
            planktonMortalityOutputMatrix, calibrationMatrix, outputClass0Matrix;
    String[] dietsConfigFileName, dietOutputMetrics;
    int[][] nbDietsStages;
    float[][][] dietStageThreshold;
    //Initial abundances
    String[] calibrationMethod;
    double[][] spBiomIniTab;
    double[] SSslope, SSintercept;
    //size spectrum
    float spectrumMinSize, spectrumMaxSize, classRange;
    float[] tabSizes;//size classes of 10cm are used for ini and output
    float[] tabSizesLn;//for output
    int nbSizeClass;
    // TL distrib
    int nbTLClass;
    float minTL;
    float maxTL;
    float[] tabTL;
    //param allowing to adapt the size of output tables depending on simulation time
    int timeSeriesLength, timeSeriesStart;
    int[] startingSavingTimeTab;
    boolean timeSeriesIsShortened;

    public Osmose() {
        readPathFile();		// read the path of input files, written in the file pathFile.txt

        // create output folder
        if (OS_NAME.startsWith("Linux")) {
            outputPathName = inputPathName + "/output/";
        } else if (OS_NAME.startsWith("Windows")) {
            outputPathName = inputPathName + ".\\output\\";
        }

        readInputFile();	// read the first file containing the file names of all other input files
        for (int x = 0; x < nbSeriesSimus; x++) {
            readAllInputFiles(x);
        }
        initializeSizeAndTLSpectrum();
    }

    public void initializeSizeAndTLSpectrum() {
        if (sizeSpectrumOutputMatrix[numSerie] || sizeSpectrumPerSpeOutputMatrix[numSerie]) {
            //initialisation of the size spectrum features
            nbSizeClass = (int) (spectrumMaxSize / classRange);//size classes of 5 cm

            tabSizes = new float[nbSizeClass];
            tabSizes[0] = spectrumMinSize;
            for (int i = 1; i < nbSizeClass; i++) {
                tabSizes[i] = i * classRange;
            }

            tabSizesLn = new float[nbSizeClass];
            tabSizesLn[0] = (float) (Math.log(classRange / 2f));

            for (int i = 1; i < nbSizeClass; i++) {
                tabSizesLn[i] = (float) (Math.log(tabSizes[i] + (classRange / 2f)));
            }

        }

        minTL = 1.0f;
        maxTL = 6.0f;
        nbTLClass = (int) (1 + ((maxTL - minTL) / 0.1f));   // TL classes of 0.1, from 1 to 6
        tabTL = new float[nbTLClass];
        tabTL[0] = minTL;
        for (int i = 1; i < nbTLClass; i++) {
            tabTL[i] = minTL + i * 0.1f;
        }
    }

    public void runSeriesSimulations() {
        Runtime r = Runtime.getRuntime();
        long freeMem = r.freeMemory();
        for (int x = 0; x < nbSeriesSimus; x++) {
            numSerie = x;
            System.out.println();
            System.out.println("SERIE " + x);
            /*
            if(x==0)	// for the first serie : all input files are read and kept in memory
            readAllInputFiles(x);
            else	// for the following series, only the files that differ from the previous one are read, other input are already in memory
            {
            int previousSerie = x-1;
            checkInputFilesChange(x, previousSerie);
            }
             */
            for (int xx = 0; xx < nbLoopTab[x]; xx++) {
                numSimu = xx;
                r.gc();
                freeMem = r.freeMemory();
                System.out.println("Simulation " + xx + "        **** FREE MEMORY = " + freeMem);

                if (numSimu == 0) {
                    initializeOptions();
                    System.out.println("options initialized");

                    simulation = new Simulation(this, nbDtMatrix[x], savingDtMatrix[x], nbSpeciesTab[x], nameSpecMatrix[x], DMatrix[x], FMatrix[x],
                            longevityMatrix[x], lInfMatrix[x], KMatrix[x], t0Matrix[x], cMatrix[x], bPowerMatrix[x], alphaMatrix[x],
                            sizeMatMatrix[x], nbStagesMatrix[x], sizeFeedingMatrix[x], recruitAgeMatrix[x], recruitSizeMatrix[x],
                            seasonFishingMatrix[x], recruitMetricMatrix[x], seasonSpawningMatrix[x], supAgeOfClass0Matrix[x],
                            larvalSurvivalMatrix[x], sexRatioMatrix[x], eggSizeMatrix[x], eggWeightMatrix[x], growthAgeThresholdMatrix[x],
                            predationRateMatrix[x], predPreySizesMaxMatrix[x], predPreySizesMinMatrix[x], criticalPredSuccessMatrix[x], starvMaxRateMatrix[x], nbAccessStage, accessStageThreshold,
                            TLoutputMatrix[x], TLDistriboutputMatrix[x], dietsOutputMatrix[x], dietOutputMetrics[x], nbDietsStages[x], dietStageThreshold[x], meanSizeOutputMatrix[x],
                            sizeSpectrumOutputMatrix[x], sizeSpectrumPerSpeOutputMatrix[x], planktonMortalityOutputMatrix[x], calibrationMatrix[x], outputClass0Matrix[x], isForcing[x]);

                    System.out.println("simulation initialized");

                    initializeOutOfZoneCarac();
                    System.out.println("migration caracteristics initialized");

                    initializeSpeciesAreas();
                    System.out.println("areas initialized");

                    //in initialiserSpeciesareas, save in tabTemp the areas by cohort
                    //do not distribute the species in simulation()
                    initializeOutputData();
                    System.out.println("output data initialized");

                } else {
                    grid = new Grid(gridLinesTab[numSerie], gridColumnsTab[numSerie], upLeftLatTab[numSerie],
                            lowRightLatTab[numSerie], upLeftLongTab[numSerie], lowRightLongTab[numSerie]);
                    if (!coastFileNameTab[numSerie].equalsIgnoreCase("None")) {
                        updateCoastCells(numSerie);
                    }

                    simulation = new Simulation(this, nbDtMatrix[x], savingDtMatrix[x], nbSpeciesTab[x], nameSpecMatrix[x], DMatrix[x], FMatrix[x],
                            longevityMatrix[x], lInfMatrix[x], KMatrix[x], t0Matrix[x], cMatrix[x], bPowerMatrix[x], alphaMatrix[x],
                            sizeMatMatrix[x], nbStagesMatrix[x], sizeFeedingMatrix[x], recruitAgeMatrix[x], recruitSizeMatrix[x],
                            seasonFishingMatrix[x], recruitMetricMatrix[x], seasonSpawningMatrix[x], supAgeOfClass0Matrix[x],
                            larvalSurvivalMatrix[x], sexRatioMatrix[x], eggSizeMatrix[x], eggWeightMatrix[x], growthAgeThresholdMatrix[x],
                            predationRateMatrix[x], predPreySizesMaxMatrix[x], predPreySizesMinMatrix[x], criticalPredSuccessMatrix[x], starvMaxRateMatrix[x], nbAccessStage, accessStageThreshold,
                            TLoutputMatrix[x], TLDistriboutputMatrix[x], dietsOutputMatrix[x], dietOutputMetrics[x], nbDietsStages[x], dietStageThreshold[x], meanSizeOutputMatrix[x],
                            sizeSpectrumOutputMatrix[x], sizeSpectrumPerSpeOutputMatrix[x],
                            planktonMortalityOutputMatrix[x], calibrationMatrix[x], outputClass0Matrix[x], isForcing[x]);

                    initializeOutOfZoneCarac();
                    initializeSpeciesAreas();
                    System.out.println();
                }

                runSimulation();
                System.out.print("simu " + numSimu + " end -> ");
                System.out.println(new Date());
            }

            saveSerieSimulations(numSerie);
            simulation = null;
            grid = null;
        }
    }

    public void readAllInputFiles(int numSerie) {
        readConfigurationFile(configFileNameTab[numSerie], numSerie);
        readSpeciesFile(speciesFileNameTab[numSerie], numSerie);
        readPredationFile(predationFileNameTab[numSerie], numSerie);
        readFishingFile(fishingFileNameTab[numSerie], numSerie);
        readCalibrationFile(calibrationFileNameTab[numSerie], numSerie);

        readSeasonalityReproFile(reproductionFileNameTab[numSerie], numSerie);
        readSeasonalityFishingFile(fishingSeasonFileNameTab[numSerie], numSerie);
        readsize0File(size0FileNameTab[numSerie], numSerie);
        readIndicatorsFile(indicatorsFileNameTab[numSerie], numSerie);
        if (dietsOutputMatrix[numSerie]) {
            readDietsOutputFile(dietsConfigFileName[numSerie], numSerie);
        }

        readAccessibilitiesFile(accessibilitiesFileNameTab[numSerie], numSerie);
    }

    public void checkInputFilesChange(int newNumSerie, int previousNumSerie) {
        if (!(configFileNameTab[newNumSerie].equals(configFileNameTab[previousNumSerie]))) {
            readConfigurationFile(configFileNameTab[newNumSerie], newNumSerie);
        }
        if (!(speciesFileNameTab[newNumSerie].equals(speciesFileNameTab[previousNumSerie]))) {
            readSpeciesFile(speciesFileNameTab[newNumSerie], newNumSerie);
        }
        if (!(predationFileNameTab[newNumSerie].equals(predationFileNameTab[previousNumSerie]))) {
            readPredationFile(predationFileNameTab[newNumSerie], newNumSerie);
        }
        if (!(fishingFileNameTab[newNumSerie].equals(fishingFileNameTab[previousNumSerie]))) {
            readFishingFile(fishingFileNameTab[newNumSerie], newNumSerie);
        }
        if (!(calibrationFileNameTab[newNumSerie].equals(calibrationFileNameTab[previousNumSerie]))) {
            readCalibrationFile(calibrationFileNameTab[newNumSerie], newNumSerie);
        }

        if (!(reproductionFileNameTab[newNumSerie].equals(reproductionFileNameTab[previousNumSerie]))) {
            readSeasonalityReproFile(reproductionFileNameTab[newNumSerie], newNumSerie);
        }
        if (!(fishingSeasonFileNameTab[newNumSerie].equals(fishingSeasonFileNameTab[previousNumSerie]))) {
            readSeasonalityFishingFile(fishingSeasonFileNameTab[newNumSerie], newNumSerie);
        }
        if (!(accessibilitiesFileNameTab[newNumSerie].equals(accessibilitiesFileNameTab[previousNumSerie]))) {
            readAccessibilitiesFile(accessibilitiesFileNameTab[newNumSerie], newNumSerie);
        }
        if (!(size0FileNameTab[newNumSerie].equals(size0FileNameTab[previousNumSerie]))) {
            readsize0File(size0FileNameTab[newNumSerie], newNumSerie);
        }
        if (!(indicatorsFileNameTab[newNumSerie].equals(indicatorsFileNameTab[previousNumSerie]))) {
            readIndicatorsFile(indicatorsFileNameTab[newNumSerie], newNumSerie);
        }

        if (dietsOutputMatrix[newNumSerie]) {
            if (!(dietsConfigFileName[newNumSerie].equals(dietsConfigFileName[previousNumSerie]))) {
                readDietsOutputFile(dietsConfigFileName[newNumSerie], newNumSerie);
            }
        }


    }

    public void readPathFile() // read the file situated within the source code directory
    {
        FileInputStream pathFile;
        try {
            pathFile = new FileInputStream(new File(initialPathFile));
        } catch (FileNotFoundException ex) {
            System.out.println("initial path file doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(pathFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            inputPathName = st.sval;
        } catch (IOException ex) {
            System.out.println("Reading error of path file");
            return;
        }
    }

    // read the first file, that should be named INPUT.txt, situated at the path given by filePath.txt
    public void readInputFile() {
        FileInputStream inputFile;
        try {
            inputFile = new FileInputStream(new File(inputPathName, "INPUT.txt"));
        } catch (FileNotFoundException ex) {
            System.out.println("INPUT file doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(inputFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            nbSeriesSimus = (new Integer(st.sval)).intValue();
        } catch (IOException ex) {
            System.out.println("Reading error of INPUT file");
            return;
        }

        // give the right dimension to tables according to the number of series specified
        initializeNbSeriesInTables(nbSeriesSimus);

        try {
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                nbLoopTab[x] = (new Integer(st.sval)).intValue();
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                configFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                speciesFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                predationFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                fishingFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                areasFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                planktonStructureFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                planktonFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                calibrationFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                indicatorsFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                accessibilitiesFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                size0FileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                reproductionFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                fishingSeasonFileNameTab[x] = st.sval;
            }


            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                migrationFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                coastFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                mpaFileNameTab[x] = st.sval;
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                couplingFileNameTab[x] = st.sval;
            }


            inputFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of INPUT file");
            return;
        }
    }

    public void initializeNbSeriesInTables(int nbSeriesSimus) {
        //****initialisation of tables dimensions****
        //--- INPUT file---
        nbLoopTab = new int[nbSeriesSimus];
        speciesFileNameTab = new String[nbSeriesSimus];
        configFileNameTab = new String[nbSeriesSimus];
        size0FileNameTab = new String[nbSeriesSimus];
        predationFileNameTab = new String[nbSeriesSimus];
        calibrationFileNameTab = new String[nbSeriesSimus];
        fishingFileNameTab = new String[nbSeriesSimus];
        reproductionFileNameTab = new String[nbSeriesSimus];
        areasFileNameTab = new String[nbSeriesSimus];
        migrationFileNameTab = new String[nbSeriesSimus];
        coastFileNameTab = new String[nbSeriesSimus];
        mpaFileNameTab = new String[nbSeriesSimus];
        outputFileNameTab = new String[nbSeriesSimus];
        planktonFileNameTab = new String[nbSeriesSimus];
        planktonStructureFileNameTab = new String[nbSeriesSimus];
        indicatorsFileNameTab = new String[nbSeriesSimus];
        couplingFileNameTab = new String[nbSeriesSimus];
        accessibilitiesFileNameTab = new String[nbSeriesSimus];
        fishingSeasonFileNameTab = new String[nbSeriesSimus];

        //--- CONFIGURATION file---
        gridLinesTab = new int[nbSeriesSimus];
        gridColumnsTab = new int[nbSeriesSimus];
        simulationTimeTab = new int[nbSeriesSimus];
        nbDtMatrix = new int[nbSeriesSimus];
        savingDtMatrix = new int[nbSeriesSimus];
        nbDtSavePerYear = new int[nbSeriesSimus];
        startingSavingTimeTab = new int[nbSeriesSimus];
        nbSpeciesTab = new int[nbSeriesSimus];
        isForcing = new boolean[nbSeriesSimus];

        //--- COAST file---
        upLeftLatTab = new float[nbSeriesSimus];
        lowRightLatTab = new float[nbSeriesSimus];
        upLeftLongTab = new float[nbSeriesSimus];
        lowRightLongTab = new float[nbSeriesSimus];

        //--- OPTION file---
        nbSchools = new int[nbSeriesSimus];
        eggSizeMatrix = new float[nbSeriesSimus][];
        eggWeightMatrix = new float[nbSeriesSimus][];
        growthAgeThresholdMatrix = new float[nbSeriesSimus][];
        predationRateMatrix = new float[nbSeriesSimus][];
        predPreySizesMinMatrix = new float[nbSeriesSimus][][];
        predPreySizesMaxMatrix = new float[nbSeriesSimus][][];
        criticalPredSuccessMatrix = new float[nbSeriesSimus][];
        starvMaxRateMatrix = new float[nbSeriesSimus][];

        //--- SPECIES file---
        nbPlanktonGroupsTab = new int[nbSeriesSimus];
        planktonNamesTab = new String[nbSeriesSimus][];
        nameSpecMatrix = new String[nbSeriesSimus][];
        DMatrix = new float[nbSeriesSimus][];
        longevityMatrix = new float[nbSeriesSimus][];
        lInfMatrix = new float[nbSeriesSimus][];
        KMatrix = new float[nbSeriesSimus][];
        t0Matrix = new float[nbSeriesSimus][];
        cMatrix = new float[nbSeriesSimus][];
        bPowerMatrix = new float[nbSeriesSimus][];
        alphaMatrix = new float[nbSeriesSimus][];
        maturityMetricMatrix = new String[nbSeriesSimus][];
        sizeMatMatrix = new float[nbSeriesSimus][];
        sexRatioMatrix = new float[nbSeriesSimus][];
        sizeFeedingMatrix = new float[nbSeriesSimus][][];
        nbStagesMatrix = new int[nbSeriesSimus][];

        //--- CALIBRATION file---
        calibrationMethod = new String[nbSeriesSimus];
        spBiomIniTab = new double[nbSeriesSimus][];
        larvalSurvivalMatrix = new float[nbSeriesSimus][];
        supAgeOfClass0Matrix = new float[nbSeriesSimus][];
        SSslope = new double[nbSeriesSimus];
        SSintercept = new double[nbSeriesSimus];

        //--- REPRODUCTION file---
        seasonSpawningMatrix = new float[nbSeriesSimus][][];

        //--- FISHING file---
        globalOrCohortTab = new String[nbSeriesSimus];
        FMatrix = new float[nbSeriesSimus][];
        recruitSizeMatrix = new float[nbSeriesSimus][];
        recruitAgeMatrix = new float[nbSeriesSimus][];
        recruitMetricMatrix = new String[nbSeriesSimus];
        seasonFishingMatrix = new float[nbSeriesSimus][][];

        //--- SPATIAL initialisations---
        speciesAreasSizeTab = new int[nbSeriesSimus];
        tabCoastiMatrix = new int[nbSeriesSimus][];
        tabCoastjMatrix = new int[nbSeriesSimus][];
        nbCellsCoastTab = new int[nbSeriesSimus];

        tabMPAiMatrix = new int[nbSeriesSimus][];
        tabMPAjMatrix = new int[nbSeriesSimus][];
        thereIsMPATab = new boolean[nbSeriesSimus];
        MPAtStartTab = new int[nbSeriesSimus];
        MPAtEndTab = new int[nbSeriesSimus];

        //--- OUTPUT INDICATORS ---

        TLoutputMatrix = new boolean[nbSeriesSimus];
        TLDistriboutputMatrix = new boolean[nbSeriesSimus];
        dietsOutputMatrix = new boolean[nbSeriesSimus];
        meanSizeOutputMatrix = new boolean[nbSeriesSimus];
        sizeSpectrumOutputMatrix = new boolean[nbSeriesSimus];
        sizeSpectrumPerSpeOutputMatrix = new boolean[nbSeriesSimus];
        planktonMortalityOutputMatrix = new boolean[nbSeriesSimus];
        calibrationMatrix = new boolean[nbSeriesSimus];
        outputClass0Matrix = new boolean[nbSeriesSimus];
        dietsConfigFileName = new String[nbSeriesSimus];
        dietOutputMetrics = new String[nbSeriesSimus];
        nbDietsStages = new int[nbSeriesSimus][];
        dietStageThreshold = new float[nbSeriesSimus][][];
    }

    public void readSpeciesFile(String speciesFileName, int numSerie) {
        FileInputStream speciesFile;
        try {
            speciesFile = new FileInputStream(new File(inputPathName, speciesFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("Species file " + speciesFileName + " doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(speciesFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            if (new Integer(st.sval).intValue() == nbSpeciesTab[numSerie]) {
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    nameSpecMatrix[numSerie][i] = st.sval;
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    DMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    longevityMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    lInfMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    KMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    t0Matrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    cMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    bPowerMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    alphaMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    maturityMetricMatrix[numSerie][i] = st.sval;
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    if (maturityMetricMatrix[numSerie][i].equalsIgnoreCase("age")) //conversion from maturity age to maturity size through von Bertalanffy equation
                    {
                        st.nextToken();
                        sizeMatMatrix[numSerie][i] = lInfMatrix[numSerie][i] * (float) (1 - Math.exp(-KMatrix[numSerie][i]
                                * ((new Float(st.sval).floatValue()) - t0Matrix[numSerie][i])));             //***** to checked if not too big fish (same test than for recruit age)
                    } else {
                        st.nextToken();
                        sizeMatMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                    }
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    sexRatioMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
            } else {
                System.out.println("Uncorrect number of species in species file");
                return;
            }
            speciesFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of species file");
            return;
        }
    }

    public void readSeasonalityReproFile(String reproductionFileName, int numSerie) {
        if (reproductionFileName.equalsIgnoreCase("default")) {
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                seasonSpawningMatrix[numSerie][i] = new float[nbDtMatrix[numSerie]];
                for (int j = 0; j < nbDtMatrix[numSerie]; j++) {
                    seasonSpawningMatrix[numSerie][i][j] = (float) 1 / (float) nbDtMatrix[numSerie];
                }
            }
            System.out.println("Reproduction is set constant over the year (default)");
        } else {
            FileInputStream reproductionFile;
            try {
                reproductionFile = new FileInputStream(new File(inputPathName, reproductionFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("reproduction file doesn't exist: " + reproductionFileName);
                return;
            }

            Reader r = new BufferedReader(new InputStreamReader(reproductionFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            float tempSum;

            try {
                st.nextToken();
                if (new Integer(st.sval).intValue() == nbDtMatrix[numSerie]) {
                    for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                        tempSum = 0;
                        seasonSpawningMatrix[numSerie][i] = new float[nbDtMatrix[numSerie]];
                        for (int j = 0; j < nbDtMatrix[numSerie]; j++) {
                            st.nextToken();
                            seasonSpawningMatrix[numSerie][i][j] = (new Float(st.sval)).floatValue() / 100; //percentage
                            tempSum += (new Float(st.sval)).floatValue();
                        }
                        if (!((tempSum > 99.f) && (tempSum < 101.f))) {
                            System.out.println("ERROR: sum of percents does not equal 100% in spawning seasonality file");
                        }
                    }
                } else {
                    System.out.println("Error in nb time steps defined in the reproduction seasonality file");
                }
            } catch (IOException ex) {
                System.out.println("Reading error of reproduction seasonality file");
                return;
            }
        }
    }

    public void readSeasonalityFishingFile(String fishingFileName, int numSerie) {
        if (fishingFileName.equalsIgnoreCase("default")) {
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                seasonFishingMatrix[numSerie][i] = new float[nbDtMatrix[numSerie]];
                for (int j = 0; j < nbDtMatrix[numSerie]; j++) {
                    seasonFishingMatrix[numSerie][i][j] = (float) 1 / (float) nbDtMatrix[numSerie];
                }
            }
            System.out.println("Fishing mortality is set constant over the year (default)");

        } else {
            FileInputStream fishingFile;
            try {
                fishingFile = new FileInputStream(new File(inputPathName, fishingFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("fishing file doesn't exist: " + fishingFileName);
                return;
            }

            Reader r = new BufferedReader(new InputStreamReader(fishingFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            float tempSum;

            try {
                st.nextToken();
                if (new Integer(st.sval).intValue() == nbDtMatrix[numSerie]) {
                    for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                        tempSum = 0;
                        seasonFishingMatrix[numSerie][i] = new float[nbDtMatrix[numSerie]];
                        for (int j = 0; j < nbDtMatrix[numSerie]; j++) {
                            st.nextToken();
                            seasonFishingMatrix[numSerie][i][j] = (new Float(st.sval)).floatValue() / 100;   //percentage
                            tempSum += (new Float(st.sval)).floatValue();
                        }
                        if (!((tempSum > 99.f) && (tempSum < 101.f))) {
                            System.out.println("ERROR: sum of percents does not equal 100% in fishing seasonality file");
                        }
                    }
                } else {
                    System.out.println("Error in nb time steps defined in the fishing seasonality file");
                }
            } catch (IOException ex) {
                System.out.println("Reading error of fishing seasonality file");
                return;
            }
        }
    }

    public void readFishingFile(String fishingFileName, int numSerie) {
        FileInputStream fishingFile;
        try {
            fishingFile = new FileInputStream(new File(inputPathName, fishingFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("fishing file doesn't exist: " + fishingFileName);
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(fishingFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            globalOrCohortTab[numSerie] = st.sval;
            if (globalOrCohortTab[numSerie].equalsIgnoreCase("global")) {
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    FMatrix[numSerie][i] = (new Float(st.sval)).floatValue();    //annual F mortality
                }
            }
            if (globalOrCohortTab[numSerie].equalsIgnoreCase("cohort")) //*******************TO DETAIL************************
            {
                System.out.println("The option F per cohort is not available now - please work with global F");
            }

            st.nextToken();
            recruitMetricMatrix[numSerie] = st.sval;

            if (recruitMetricMatrix[numSerie].equalsIgnoreCase("age")) {
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    recruitAgeMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
            } else {
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    recruitSizeMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                    if (recruitSizeMatrix[numSerie][i] < lInfMatrix[numSerie][i]) {
                        recruitAgeMatrix[numSerie][i] = (float) (-((Math.log(1 - (recruitSizeMatrix[numSerie][i] / lInfMatrix[numSerie][i]))) / KMatrix[numSerie][i])) + t0Matrix[numSerie][i];
                    } else {
                        recruitAgeMatrix[numSerie][i] = longevityMatrix[numSerie][i] + 1;
                    }
                    if (recruitAgeMatrix[numSerie][i] < 0.6)//due to inverse von Bert transformation
                    {
                        recruitAgeMatrix[numSerie][i] = (float) 0.6; // >0.5 to avoid Math.round() problems
                    }
                    if (recruitAgeMatrix[numSerie][i] > longevityMatrix[numSerie][i]) {
                        recruitAgeMatrix[numSerie][i] = longevityMatrix[numSerie][i] + 1;
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Reading error of fishing seasonality file");
            return;
        }
    }

    public void readCalibrationFile(String calibrationFileName, int numSerie) {
        FileInputStream calibFile;
        try {
            calibFile = new FileInputStream(new File(inputPathName, calibrationFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("abdIni file doesn't exist: " + calibrationFileName);
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(calibFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            calibrationMethod[numSerie] = st.sval;
            if (calibrationMethod[numSerie].equalsIgnoreCase("biomass")) {
                spBiomIniTab[numSerie] = new double[nbSpeciesTab[numSerie]];
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    spBiomIniTab[numSerie][i] = (new Double(st.sval)).doubleValue();
                }
            } else if (calibrationMethod[numSerie].equalsIgnoreCase("spectrum")) {
                st.nextToken();
                SSslope[numSerie] = (new Double(st.sval)).doubleValue();
                st.nextToken();
                SSintercept[numSerie] = (new Double(st.sval)).doubleValue();
            } else if (calibrationMethod[numSerie].equalsIgnoreCase("random")) //*****************TO DETAIL****************************
            {
                System.out.println("The option initialisation per random method is not implemented yet");
            }

            supAgeOfClass0Matrix[numSerie] = new float[nbSpeciesTab[numSerie]];
            larvalSurvivalMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];

            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                st.nextToken();
                larvalSurvivalMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                st.nextToken();
                supAgeOfClass0Matrix[numSerie][i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbPlanktonGroupsTab[numSerie]; i++) {
                st.nextToken();
                planktonAccessCoeffMatrix[i] = (new Float(st.sval)).floatValue();
            }
            calibFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of calibration file");
            return;
        }
    }

    public void readConfigurationFile(String configFileName, int numSerie) {
        FileInputStream configFile;
        try {
            configFile = new FileInputStream(new File(inputPathName, configFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("configuration file doesn't exist: " + configFileName);
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(configFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            gridLinesTab[numSerie] = (new Integer(st.sval)).intValue();
            st.nextToken();
            gridColumnsTab[numSerie] = (new Integer(st.sval)).intValue();
            st.nextToken();
            upLeftLatTab[numSerie] = new Float(st.sval).floatValue();
            st.nextToken();
            lowRightLatTab[numSerie] = new Float(st.sval).floatValue();
            st.nextToken();
            upLeftLongTab[numSerie] = new Float(st.sval).floatValue();
            st.nextToken();
            lowRightLongTab[numSerie] = new Float(st.sval).floatValue();
            st.nextToken();
            simulationTimeTab[numSerie] = (new Integer(st.sval)).intValue();
            st.nextToken();
            nbDtMatrix[numSerie] = (new Integer(st.sval)).intValue();
            st.nextToken();
            savingDtMatrix[numSerie] = (new Integer(st.sval)).intValue();
            if (!((nbDtMatrix[numSerie] % savingDtMatrix[numSerie]) == 0)) {
                System.out.println("The number of time steps per year is not a multiple of the number of time steps for saving");
                System.out.println("Thus, saving is realized at each time step");
                savingDtMatrix[numSerie] = 1;
            }
            nbDtSavePerYear[numSerie] = (int) nbDtMatrix[numSerie] / savingDtMatrix[numSerie];
            st.nextToken();
            startingSavingTimeTab[numSerie] = (new Integer(st.sval)).intValue();
            st.nextToken();
            nbSpeciesTab[numSerie] = (new Integer(st.sval)).intValue();
            st.nextToken();
            nbPlanktonGroupsTab[numSerie] = (new Integer(st.sval)).intValue();

            st.nextToken();
            if ((st.sval).equalsIgnoreCase("forcing")) {
                isForcing[numSerie] = true;
            } else if ((st.sval).equalsIgnoreCase("coupling")) {
                isForcing[numSerie] = false;
            } else {
                System.out.println("In configuration file you have to specify either COUPLING or FORCING");
            }

            st.nextToken();
            nbSchools[numSerie] = 1 + Math.round(((new Integer(st.sval)).intValue()) / nbDtMatrix[numSerie]);

            configFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of configuration file");
            return;
        }
        initializeNbLivingGroups(numSerie, nbSpeciesTab[numSerie], nbPlanktonGroupsTab[numSerie]);
    }

    public void initializeNbLivingGroups(int numSerie, int nbSpeciesExplicit, int nbOtherFood) {

        // setting tables length
        //----OPTION file------
        eggSizeMatrix[numSerie] = new float[nbSpeciesExplicit];
        eggWeightMatrix[numSerie] = new float[nbSpeciesExplicit];
        growthAgeThresholdMatrix[numSerie] = new float[nbSpeciesExplicit];
        predationRateMatrix[numSerie] = new float[nbSpeciesExplicit];
        predPreySizesMaxMatrix[numSerie] = new float[nbSpeciesExplicit][];
        predPreySizesMinMatrix[numSerie] = new float[nbSpeciesExplicit][];
        criticalPredSuccessMatrix[numSerie] = new float[nbSpeciesExplicit];
        starvMaxRateMatrix[numSerie] = new float[nbSpeciesExplicit];
        //----SPECIES file------
        nameSpecMatrix[numSerie] = new String[nbSpeciesExplicit];
        DMatrix[numSerie] = new float[nbSpeciesExplicit];
        longevityMatrix[numSerie] = new float[nbSpeciesExplicit];
        lInfMatrix[numSerie] = new float[nbSpeciesExplicit];
        KMatrix[numSerie] = new float[nbSpeciesExplicit];
        t0Matrix[numSerie] = new float[nbSpeciesExplicit];
        cMatrix[numSerie] = new float[nbSpeciesExplicit];
        bPowerMatrix[numSerie] = new float[nbSpeciesExplicit];
        alphaMatrix[numSerie] = new float[nbSpeciesExplicit];
        maturityMetricMatrix[numSerie] = new String[nbSpeciesExplicit];
        sizeMatMatrix[numSerie] = new float[nbSpeciesExplicit];
        sexRatioMatrix[numSerie] = new float[nbSpeciesExplicit];
        sizeFeedingMatrix[numSerie] = new float[nbSpeciesExplicit][];
        nbStagesMatrix[numSerie] = new int[nbSpeciesExplicit];
        //----REPRODUCTION file------
        seasonSpawningMatrix[numSerie] = new float[nbSpeciesExplicit][];
        //----FISHING file------
        recruitSizeMatrix[numSerie] = new float[nbSpeciesExplicit];
        recruitAgeMatrix[numSerie] = new float[nbSpeciesExplicit];
        seasonFishingMatrix[numSerie] = new float[nbSpeciesExplicit][];
        FMatrix[numSerie] = new float[nbSpeciesExplicit];
        //--- ACCESSIBILITIES----
        planktonAccessCoeffMatrix = new float[nbOtherFood];
        nbAccessStage = new int[nbSpeciesExplicit];
        accessStageThreshold = new float[nbSpeciesExplicit][];
        accessibilityMatrix = new float[nbSpeciesExplicit + nbOtherFood][][][];

        planktonNamesTab[numSerie] = new String[nbOtherFood];
    }

    public void readsize0File(String optionFileName, int numSerie) {
        if (optionFileName.equalsIgnoreCase("default")) {
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                eggSizeMatrix[numSerie][i] = 0.1f;	// by default : egg diameter set to 1mm
                eggWeightMatrix[numSerie][i] = 0.0005386f;	// weight of egg by default, considering a sphere with the water density
                growthAgeThresholdMatrix[numSerie][i] = 1.0f;	// by default, von Bertalanffy model considered valid after 1 year old, linear growth from 0 to 1 year
            }
        } else {
            FileInputStream optionFile;
            try {
                optionFile = new FileInputStream(new File(inputPathName, optionFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("option file doesn't exist: " + optionFileName);
                return;
            }

            Reader r = new BufferedReader(new InputStreamReader(optionFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            try {
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    eggSizeMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    eggWeightMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    st.nextToken();
                    growthAgeThresholdMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
                }

                optionFile.close();
            } catch (IOException ex) {
                System.out.println("Reading error of option file");
                return;
            }
        }
    }

    public void readPredationFile(String predationFileName, int numSerie) {
        FileInputStream predationFile;
        try {
            predationFile = new FileInputStream(new File(inputPathName, predationFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("predation file doesn't exist: " + predationFileName);
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(predationFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                st.nextToken();
                predationRateMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                st.nextToken();
                criticalPredSuccessMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                st.nextToken();
                starvMaxRateMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                st.nextToken();
                nbStagesMatrix[numSerie][i] = (new Integer(st.sval)).intValue();
                sizeFeedingMatrix[numSerie][i] = new float[nbStagesMatrix[numSerie][i] - 1];
                predPreySizesMinMatrix[numSerie][i] = new float[nbStagesMatrix[numSerie][i]];
                predPreySizesMaxMatrix[numSerie][i] = new float[nbStagesMatrix[numSerie][i]];
                for (int s = 0; s < nbStagesMatrix[numSerie][i] - 1; s++) {
                    st.nextToken();
                    sizeFeedingMatrix[numSerie][i][s] = (new Float(st.sval)).floatValue();
                }
                for (int s = 0; s < nbStagesMatrix[numSerie][i]; s++) {
                    st.nextToken();
                    predPreySizesMinMatrix[numSerie][i][s] = (new Float(st.sval)).floatValue();
                    st.nextToken();
                    predPreySizesMaxMatrix[numSerie][i][s] = (new Float(st.sval)).floatValue();
                }
            }
            predationFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of predation file");
            return;
        }
    }

    public void readIndicatorsFile(String indicatorsFileName, int numSerie) {
        FileInputStream indicFile;
        try {
            indicFile = new FileInputStream(new File(inputPathName, indicatorsFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("output config file doesn't exist: " + indicatorsFileName);
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(indicFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {

            st.nextToken();
            outputFileNameTab[numSerie] = st.sval;

            st.nextToken();
            calibrationMatrix[numSerie] = false;
            if (st.sval.equalsIgnoreCase("calibration")) {
                calibrationMatrix[numSerie] = true;
            }

            st.nextToken();
            TLoutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            st.nextToken();
            TLDistriboutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            st.nextToken();
            dietsOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            st.nextToken();
            dietsConfigFileName[numSerie] = st.sval;
            st.nextToken();
            meanSizeOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            st.nextToken();
            sizeSpectrumOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            st.nextToken();
            sizeSpectrumPerSpeOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            if (sizeSpectrumOutputMatrix[numSerie] || sizeSpectrumPerSpeOutputMatrix[numSerie]) {
                st.nextToken();
                spectrumMinSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                spectrumMaxSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                classRange = (new Float(st.sval)).floatValue();
            }
            st.nextToken();
            planktonMortalityOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            st.nextToken();
            outputClass0Matrix[numSerie] = (new Boolean(st.sval)).booleanValue();

            indicFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of output config file");
            return;
        }
    }

    public void initializeOptions() {
        grid = new Grid(gridLinesTab[numSerie], gridColumnsTab[numSerie], upLeftLatTab[numSerie],
                lowRightLatTab[numSerie], upLeftLongTab[numSerie], lowRightLongTab[numSerie]);
        if (coastFileNameTab[numSerie].equalsIgnoreCase("None")) {
            nbCellsCoastTab[numSerie] = 0;
        } else {
            initializeCoast();
        }

        initializeMPA();
    }

    public void initializeCoast() {
        if (coastFileNameTab[numSerie].equalsIgnoreCase("default")) {
            System.out.println("No coast in the grid (default)");
        } else {
            //read info in file coast
            FileInputStream coastFile;

            try {
                coastFile = new FileInputStream(new File(inputPathName, coastFileNameTab[numSerie]));
            } catch (FileNotFoundException ex) {
                System.out.println("Error while opening coastFile");
                return;
            }
            //read nb of cells and compare to options
            Reader r = new BufferedReader(new InputStreamReader(coastFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');
            /*structure of file:
            nb lines;
            nb columns;
            nb Cells for coast(land);
            in lines coord i of coast;
            in lines coord j of coast;
             */
            try {
                st.nextToken();
                if (new Integer(st.sval).intValue() == gridLinesTab[numSerie]) {
                    st.nextToken();
                    if (new Integer(st.sval).intValue() == gridColumnsTab[numSerie]) {

                        st.nextToken();
                        tabCoastiMatrix[numSerie] = new int[new Integer(st.sval).intValue()];
                        tabCoastjMatrix[numSerie] = new int[new Integer(st.sval).intValue()];

                        nbCellsCoastTab[numSerie] = new Integer(st.sval).intValue();

                        for (int i = 0; i < tabCoastiMatrix[numSerie].length; i++) {

                            st.nextToken();
                            tabCoastiMatrix[numSerie][i] = (new Integer(st.sval).intValue());
                        }

                        for (int i = 0; i < tabCoastjMatrix[numSerie].length; i++) {

                            st.nextToken();
                            tabCoastjMatrix[numSerie][i] = (new Integer(st.sval).intValue());
                        }
                        for (int i = 0; i < tabCoastiMatrix[numSerie].length; i++) {
                            grid.getCell(tabCoastiMatrix[numSerie][i], tabCoastjMatrix[numSerie][i]).setLand(true);
                        }
                    } else {
                        System.out.println("Error while reading coastFile for nb columns match");
                    }
                } else {
                    System.out.println("Error while reading coastFile for nb lines match");
                }

                coastFile.close();
            } catch (IOException ex) {
                System.out.println("Error while reading coastFile");
                return;
            }
        }
    }

    public void updateCoastCells(int numSerie) {
        for (int i = 0; i < tabCoastiMatrix[numSerie].length; i++) {
            grid.getCell(tabCoastiMatrix[numSerie][i], tabCoastjMatrix[numSerie][i]).setLand(true);
        }
    }

    public void initializeMPA() {
        if (mpaFileNameTab[numSerie].equalsIgnoreCase("default")) {
            thereIsMPATab[numSerie] = false;
            tabMPAiMatrix[numSerie] = new int[0];
            tabMPAjMatrix[numSerie] = new int[0];
            MPAtStartTab[numSerie] = 0;
            MPAtEndTab[numSerie] = 0;
        } else {
            //read info in file mpa
            FileInputStream mpaFile;
            int[] tabi, tabj;
            try {
                mpaFile = new FileInputStream(new File(inputPathName, mpaFileNameTab[numSerie]));
            } catch (FileNotFoundException ex) {
                System.out.println("Error while opening mpaFile");
                return;
            }
            Reader r = new BufferedReader(new InputStreamReader(mpaFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');
            /*structure of file: (diff with osmose: MPAtStart et MPAtEnd in addition)
            nb lines;
            nb columns;
            nbCells of mpa;
            MPAtstart;MPAtEnd;
            in lines, coord i of mpa;
            in lines coord j of mpa;
             */
            try {
                st.nextToken();
                if (new Integer(st.sval).intValue() == gridLinesTab[numSerie]) {
                    st.nextToken();
                    if (new Integer(st.sval).intValue() == gridColumnsTab[numSerie]) {
                        st.nextToken();
                        tabi = new int[new Integer(st.sval).intValue()];
                        tabj = new int[new Integer(st.sval).intValue()];
                        st.nextToken();
                        MPAtStartTab[numSerie] = new Integer(st.sval).intValue();
                        st.nextToken();
                        MPAtEndTab[numSerie] = new Integer(st.sval).intValue();
                        for (int i = 0; i < tabi.length; i++) {
                            st.nextToken();
                            tabi[i] = new Integer(st.sval).intValue();
                        }
                        for (int j = 0; j < tabj.length; j++) {
                            st.nextToken();
                            tabj[j] = new Integer(st.sval).intValue();
                        }
                        boolean okForCoast = true;
                        for (int i = 0; i < tabi.length; i++) {
                            if (grid.getCell(tabi[i], tabj[i]).isLand()) {
                                okForCoast = false;
                                break;
                            }
                        }
                        if (okForCoast) {
                            tabMPAiMatrix[numSerie] = new int[tabi.length];
                            tabMPAjMatrix[numSerie] = new int[tabj.length];
                            for (int i = 0; i < tabi.length; i++) {
                                tabMPAiMatrix[numSerie][i] = tabi[i];
                                tabMPAjMatrix[numSerie][i] = tabj[i];
                            }
                        } else {
                            System.out.println(
                                    "Error while reading mpaFile for coast cells");
                        }
                        mpaFile.close();
                    } else {
                        System.out.println("Error while reading mpaFile for nb columns match");
                    }
                } else {
                    System.out.println("Error while reading mpaFile for nb lines match");
                }
            } catch (IOException ex) {
                System.out.println("Error while reading mpaFile");
                return;
            }
            thereIsMPATab[numSerie] = true;
        }
    }

    public void initializeSpeciesAreas() //must be done after coast
    {
        //areas data are read from file areasFile
        FileInputStream areasFile;
        String distribMethod;

        try {
            areasFile = new FileInputStream(new File(inputPathName, areasFileNameTab[numSerie]));
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening areasFile");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(areasFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            distribMethod = st.sval;
            if (distribMethod.equalsIgnoreCase("random")) /* *******RANDOM CASE********* */ {
                st.nextToken();
                speciesAreasSizeTab[numSerie] = (new Integer(st.sval)).intValue();
                distribRandom();
            } else if (distribMethod.equalsIgnoreCase("maps"))/* *******CASE FILE AREAS - densities or presence/absence********* */ {
                if (numSimu == 0) {//1
                    //areas data are read from file areasFile

                    st.nextToken();
                    if (new Integer(st.sval).intValue() == gridLinesTab[numSerie]) {//3
                        st.nextToken();
                        if (new Integer(st.sval).intValue() == gridColumnsTab[numSerie]) {//3
                            st.nextToken();

                            if (new Integer(st.sval).intValue() == nbSpeciesTab[numSerie]) {//4
                                boolean okForLongevity = true;
                                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                                    st.nextToken();
                                    if (new Float(st.sval).floatValue() != longevityMatrix[numSerie][i]) {
                                        okForLongevity = false;
                                    }
                                }

                                if (okForLongevity) {//5
                                    numMap = new int[nbSpeciesTab[numSerie]][][];
                                    int nbMaps;
                                    st.nextToken();
                                    nbMaps = new Integer(st.sval).intValue();
                                    mapCoordi = new int[nbMaps][];
                                    mapCoordj = new int[nbMaps][];
                                    mapProbaPresence = new float[nbMaps][];

                                    st.nextToken();
                                    if ((st.sval).equalsIgnoreCase("density")) {
                                        densityMaps = true;
                                    } else {
                                        densityMaps = false;
                                    }

                                    int numSpForMap, nbAgePerMap, nbDtPerMap;
                                    int[] tempAge;
                                    int[] tempDt;


                                    for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                                        Species speci = simulation.species[i];
                                        numMap[i] = new int[speci.tabCohorts.length][];
                                        for (int j = 0; j < speci.tabCohorts.length; j++) {
                                            numMap[i][j] = new int[nbDtMatrix[numSerie]];
                                        }
                                    }

                                    for (int i = 0; i < nbMaps; i++) {
                                        st.nextToken();
                                        numSpForMap = new Integer(st.sval).intValue() - 1;   //because species number between 1 and nbSpecies
                                        st.nextToken();
                                        nbAgePerMap = new Integer(st.sval).intValue();
                                        tempAge = new int[nbAgePerMap];
                                        st.nextToken();
                                        nbDtPerMap = new Integer(st.sval).intValue();
                                        tempDt = new int[nbDtPerMap];
                                        for (int k = 0; k < nbAgePerMap; k++) {
                                            st.nextToken();
                                            tempAge[k] = new Integer(st.sval).intValue();
                                        }
                                        for (int k = 0; k < nbDtPerMap; k++) {
                                            st.nextToken();
                                            tempDt[k] = new Integer(st.sval).intValue() - 1;
                                        }
                                        st.nextToken();

                                        mapCoordi[i] = new int[(new Integer(st.sval).intValue())];
                                        mapCoordj[i] = new int[(new Integer(st.sval).intValue())];
                                        mapProbaPresence[i] = new float[new Integer(st.sval).intValue()];

                                        for (int m = 0; m < nbAgePerMap; m++) {
                                            for (int n = 0; n < nbDtPerMap; n++) {
                                                for (int h = 0; h < nbDtMatrix[numSerie]; h++) {
                                                    int tempo = tempAge[m] * nbDtMatrix[numSerie] + h;
                                                    if ((tempAge[m] * nbDtMatrix[numSerie] + h) < simulation.species[numSpForMap].tabCohorts.length) {
                                                        numMap[numSpForMap][tempAge[m] * nbDtMatrix[numSerie] + h][tempDt[n]] = i;
                                                    }
                                                    if (mapCoordi[i].length == 0) {
                                                        if (!simulation.species[numSpForMap].tabCohorts[(tempAge[m] * nbDtMatrix[numSerie]) + h].outOfZoneCohort[tempDt[n]]) {
                                                            System.out.println("Match error between species areas and migration file");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        for (int m = 0; m < mapCoordi[i].length; m++) {
                                            st.nextToken();
                                            mapCoordi[i][m] = (new Integer(st.sval).intValue());
                                        }
                                        for (int m = 0; m < mapCoordj[i].length; m++) {
                                            st.nextToken();
                                            mapCoordj[i][m] = (new Integer(st.sval).intValue());
                                        }
                                        for (int m = 0; m < mapCoordj[i].length; m++) {
                                            if (grid.getCell(mapCoordi[i][m], mapCoordj[i][m]).isLand()) {
                                                System.out.println("Problem of coast in species area file for map " + m + " - " + i + " - " + mapCoordi[i][m] + "  " + mapCoordj[i][m]);
                                            }
                                        }
                                        for (int m = 0; m < mapCoordj[i].length; m++) {
                                            grid.getCell(mapCoordi[i][m], mapCoordj[i][m]).numMapsConcerned.add(new Integer(m));
                                        }
                                        if (densityMaps) {
                                            for (int m = 0; m < mapProbaPresence[i].length; m++) {
                                                st.nextToken();
                                                mapProbaPresence[i][m] = new Float(st.sval).intValue();
                                            }
                                        } else {
                                            for (int m = 0; m < mapProbaPresence[i].length; m++) {
                                                mapProbaPresence[i][m] = 1f / (float) mapProbaPresence[i].length;
                                            }
                                        }

                                    }
                                    simulation.randomDistribution = false;

                                }//5
                                else {
                                    System.out.println("Error while reading the distribution file for longevities match");
                                }
                            }//4
                            else {
                                System.out.println("Spatial error while reading file for species number match");
                            }
                        }//3
                        else {
                            System.out.println("Error while reading the distribution file for nb columns match");
                        }
                    }//3
                    else {
                        System.out.println("Error while reading the distribution file for nb lines match");
                    }
                    areasFile.close();

                }//1
                if (numSimu > 0) {
                    simulation.randomDistribution = false;
                }

            } else {
                System.out.println("In distribution file, the first entry must be RANDOM or MAPS");
            }

        } catch (IOException ex) {
            System.out.println("Error while reading areasFile");
            return;
        }

        //################################################
        //## FOR ALL CASES/ sim0 or not, random or not
        //################################################
        simulation.distributeSpeciesIni();
    }

    public void distribRandom() {
        simulation.randomDistribution = true;
        if (numSimu == 0) {
            //creation of randomAreaCoordi and j
            randomAreaCoordi = new int[nbSpeciesTab[numSerie]][];
            randomAreaCoordj = new int[nbSpeciesTab[numSerie]][];
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                randomAreaCoordi[i] = new int[speciesAreasSizeTab[numSerie]];
                randomAreaCoordj[i] = new int[speciesAreasSizeTab[numSerie]];
            }
            //initialise random sorting of distribution areas

            int nbCasesDispos = ((int) (gridLinesTab[numSerie] * gridColumnsTab[numSerie]))
                    - nbCellsCoastTab[numSerie];

            //Case where random distribution on the whole (grid-coast)
            if (speciesAreasSizeTab[numSerie] >= nbCasesDispos) {
                speciesAreasSizeTab[numSerie] = nbCasesDispos;
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    randomAreaCoordi[i] = new int[speciesAreasSizeTab[numSerie]];
                    randomAreaCoordj[i] = new int[speciesAreasSizeTab[numSerie]];
                }
                int index = 0;
                for (int l = 0; l < grid.getNbLines(); l++) {
                    for (int m = 0; m < grid.getNbColumns(); m++) {
                        if (!grid.getCell(l, m).isLand()) {
                            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                                randomAreaCoordi[i][index] = grid.getCell(l, m).getI();
                                randomAreaCoordj[i][index] = grid.getCell(l, m).getJ();
                            }
                            index++;
                        }
                    }
                }
            } //case where random disribution on speciesAreasSize cells
            //random sorting of connex cells for each species
            else {
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    for (int l = 0; l < grid.getNbLines(); l++) {
                        for (int m = 0; m < grid.getNbColumns(); m++) {
                            grid.getCell(l, m).setAlreadyChosen(false);
                        }
                    }
                    Cell[] tabCellsArea = new Cell[speciesAreasSizeTab[numSerie]];
                    int coordi, coordj;
                    coordi = (int) Math.round(Math.random() * (grid.getNbLines() - 1));
                    coordj = (int) Math.round(Math.random() * (grid.getNbColumns() - 1));
                    while (grid.getCell(coordi, coordj).isLand()) {
                        coordi = (int) Math.round(Math.random() * (grid.getNbLines() - 1));
                        coordj = (int) Math.round(Math.random() * (grid.getNbColumns() - 1));
                    }
                    tabCellsArea[0] = grid.getCell(coordi, coordj);
                    grid.getCell(coordi, coordj).setAlreadyChosen(true);
                    /*
                     * From initial cell, successive random sorting of
                     * the adjacent cells until tabCellsArea is full
                     */
                    int iFirstSorted = 0;
                    int iLastSorted = 0;
                    int index = 0;
                    while (index < (tabCellsArea.length - 1)) {
                        for (int iCell = iFirstSorted; iCell <= iLastSorted; iCell++) {
                            ArrayList<Cell> neigbors = grid.getAdjacentCells(tabCellsArea[iCell]);
                            Iterator<Cell> iter = neigbors.iterator();
                            while ((index < (tabCellsArea.length - 1)) && iter.hasNext()) {
                                Cell cell = iter.next();
                                if (!cell.isLand() && !cell.isAlreadyChosen()) {
                                    index++;
                                    cell.setAlreadyChosen(true);
                                    tabCellsArea[index] = cell;
                                }
                            }
                        }
                        iFirstSorted = iLastSorted + 1;
                        iLastSorted = index;
                    }
                    for (int m = 0; m < tabCellsArea.length; m++) {
                        randomAreaCoordi[i][m] = tabCellsArea[m].getI();
                        randomAreaCoordj[i][m] = tabCellsArea[m].getJ();
                    }
                }
            }
        }
    }

    public void initializeOutOfZoneCarac() {
        if (migrationFileNameTab[numSerie].equalsIgnoreCase("default")) {
            System.out.println("No migrations implemented (default)");
        } else {
            /*Structure of file seasonalityFile (IN CAPITAL default options)
            ;nb species migrating;
            ;num of species
            ;number of ages;
            ;number of time steps
            ;ages
            ;time steps
            ;mortality
            ...  * nb species migrating
             */

            FileInputStream outOfZoneFile;
            try {
                outOfZoneFile = new FileInputStream(new File(inputPathName, migrationFileNameTab[numSerie]));
            } catch (FileNotFoundException ex) {
                System.out.println("migration file doesn't exist: " + migrationFileNameTab[numSerie]);
                return;
            }

            Reader r = new BufferedReader(new InputStreamReader(outOfZoneFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            int nbSpOutOfZone, numSpOutOfZone;
            int nbAgePerCase, nbDtPerCase;
            float tempMortality;
            int[] tempAge, tempDt;

            try {
                st.nextToken();
                nbSpOutOfZone = (new Integer(st.sval)).intValue();
                if (nbSpOutOfZone == 0) {
                    System.out.println("no species out of zone to be initialized");
                } else {
                    for (int i = 0; i < nbSpOutOfZone; i++) {
                        st.nextToken();
                        numSpOutOfZone = (new Integer(st.sval)).intValue();
                        st.nextToken();
                        nbAgePerCase = new Integer(st.sval).intValue();
                        tempAge = new int[nbAgePerCase];
                        st.nextToken();
                        nbDtPerCase = new Integer(st.sval).intValue();
                        tempDt = new int[nbDtPerCase];

                        for (int k = 0; k < nbAgePerCase; k++) {
                            st.nextToken();
                            tempAge[k] = new Integer(st.sval).intValue();
                        }
                        for (int k = 0; k < nbDtPerCase; k++) {
                            st.nextToken();
                            tempDt[k] = new Integer(st.sval).intValue() - 1;
                        }

                        for (int m = 0; m < nbAgePerCase; m++) {
                            st.nextToken();
                            tempMortality = (new Float(st.sval)).floatValue();
                            for (int n = 0; n < nbDtPerCase; n++) {
                                for (int h = 0; h < nbDtMatrix[numSerie]; h++) {
                                    simulation.species[numSpOutOfZone - 1].tabCohorts[tempAge[m] * nbDtMatrix[numSerie] + h].outOfZoneCohort[tempDt[n]] = true;
                                    simulation.species[numSpOutOfZone - 1].tabCohorts[tempAge[m] * nbDtMatrix[numSerie] + h].outOfZoneMortality[tempDt[n]] = tempMortality;
                                }
                            }
                        }
                    }
                }
                outOfZoneFile.close();
            } catch (IOException ex) {
                System.out.println("Reading error of out of zone species file");
                return;
            }
        }
    }

    public void readAccessibilitiesFile(String accessFileName, int numSerie) {
        if (accessFileName.equalsIgnoreCase("default")) // if no accessiblity file is specified, all accessibilities are set to 1
        {
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                nbAccessStage[i] = 1;
                accessStageThreshold[i] = new float[nbAccessStage[i] - 1];
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nbSpeciesTab[numSerie]][];
                for (int j = 0; j < nbSpeciesTab[numSerie]; j++) {
                    accessibilityMatrix[i][0][j] = new float[1];
                    accessibilityMatrix[i][0][j][0] = 1f;
                }
            }
            for (int i = nbSpeciesTab[numSerie]; i < nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]; i++) {
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nbSpeciesTab[numSerie]][];
                for (int j = 0; j < nbSpeciesTab[numSerie]; j++) {
                    accessibilityMatrix[i][0][j] = new float[1];
                    accessibilityMatrix[i][0][j][0] = 1f;
                }
            }
        } else // in case of an accessibility file specified
        {
            FileInputStream accessFile;
            try {
                accessFile = new FileInputStream(new File(inputPathName, accessFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("accessibility file doesn't exist: " + accessFileName);
                return;
            }

            Reader r = new BufferedReader(new InputStreamReader(accessFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');
            try {
                st.nextToken();
                if (!((new Integer(st.sval)).intValue() == nbSpeciesTab[numSerie])) {
                    System.out.println("nb of species in accessibilities file not valid");
                } else {
                    st.nextToken();
                    if (!((new Integer(st.sval)).intValue() == nbPlanktonGroupsTab[numSerie])) {
                        System.out.println("nb of other food groups in accessibilities file not valid");
                    } else {
                        for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                            st.nextToken();
                            nbAccessStage[i] = (new Integer(st.sval)).intValue();
                            accessStageThreshold[i] = new float[nbAccessStage[i] - 1];
                            for (int j = 0; j < nbAccessStage[i] - 1; j++) {
                                st.nextToken();
                                accessStageThreshold[i][j] = (new Float(st.sval)).floatValue();
                            }
                        }
                        for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                            accessibilityMatrix[i] = new float[nbAccessStage[i]][][];
                            for (int j = 0; j < nbAccessStage[i]; j++) {
                                accessibilityMatrix[i][j] = new float[nbSpeciesTab[numSerie]][];
                                for (int k = 0; k < nbSpeciesTab[numSerie]; k++) {
                                    accessibilityMatrix[i][j][k] = new float[nbAccessStage[k]];
                                    for (int m = 0; m < nbAccessStage[k]; m++) {
                                        st.nextToken();
                                        accessibilityMatrix[i][j][k][m] = (new Float(st.sval)).floatValue();
                                    }
                                }
                            }
                        }
                        for (int i = nbSpeciesTab[numSerie]; i < nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]; i++) {
                            accessibilityMatrix[i] = new float[1][][];
                            accessibilityMatrix[i][0] = new float[nbSpeciesTab[numSerie]][];
                            for (int k = 0; k < nbSpeciesTab[numSerie]; k++) {
                                accessibilityMatrix[i][0][k] = new float[nbAccessStage[k]];
                                for (int m = 0; m < nbAccessStage[k]; m++) {
                                    st.nextToken();
                                    accessibilityMatrix[i][0][k][m] = (new Float(st.sval)).floatValue();
                                }
                            }
                        }
                    }
                }
                accessFile.close();
            } catch (IOException ex) {
                System.out.println("Reading error of accessibilities file");
                return;
            }
        }
    }

    public void initializeOutputData() {
        //these param are function of the length of the temporal series saved
        if (startingSavingTimeTab[numSerie] > simulationTimeTab[numSerie]) {
            System.out.println("Starting time for saving higher than simulation time -> set to 0");
            startingSavingTimeTab[numSerie] = 0;
        }

        if (startingSavingTimeTab[numSerie] == 0) {
            timeSeriesLength = simulationTimeTab[numSerie];
            timeSeriesStart = 0;
            timeSeriesIsShortened = false;
        } else {
            timeSeriesStart = startingSavingTimeTab[numSerie];
            timeSeriesLength = simulationTimeTab[numSerie] - startingSavingTimeTab[numSerie];
            timeSeriesIsShortened = true;
        }

        if (calibrationMatrix[numSerie]) {
            BIOMQuadri = new float[nbLoopTab[numSerie]][][][][];
        }

        iniBiomass = new float[nbSeriesSimus][];
        for (int x = 0; x < nbSeriesSimus; x++) {
            iniBiomass[x] = new float[nbSpeciesTab[numSerie]];
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                iniBiomass[x][i] = 0;
            }
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                for (int j = simulation.species[i].indexAgeClass0; j < simulation.species[i].nbCohorts; j++) {
                    iniBiomass[x][i] += (float) simulation.species[i].tabCohorts[j].biomass;
                }
            }
        }


        for (int xx = 0; xx < nbLoopTab[numSerie]; xx++) {
            if (calibrationMatrix[numSerie]) {
                BIOMQuadri[xx] = new float[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][][][];
            }

            /*			iniBiomass[xx] = new float[nbSpeciesTab[numSerie]];
            for(int i=0;i<nbSpeciesTab[numSerie];i++)
            iniBiomass[xx][i] = 0;
            for(int i=0;i<nbSpeciesTab[numSerie];i++)
            for(int j=simulation.species[i].indexAgeClass0;j<simulation.species[i].nbCohorts;j++)
            iniBiomass[xx][i] += (float) simulation.species[i].tabCohorts[j].biomass;
             */
            int tempIndex = (int) nbDtMatrix[numSerie] / savingDtMatrix[numSerie];

            if (calibrationMatrix[numSerie]) {
                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    BIOMQuadri[xx][i] = new float[2][][];
                    BIOMQuadri[xx][i][0] = new float[timeSeriesLength][];   //without age 0
                    BIOMQuadri[xx][i][1] = new float[timeSeriesLength][];   // with age 0

                    for (int tt = 0; tt < timeSeriesLength; tt++) {
                        BIOMQuadri[xx][i][0][tt] = new float[tempIndex];
                        BIOMQuadri[xx][i][1][tt] = new float[tempIndex];
                    }
                }

                for (int i = nbSpeciesTab[numSerie]; i < nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]; i++) {
                    BIOMQuadri[xx][i] = new float[1][][];
                    BIOMQuadri[xx][i][0] = new float[timeSeriesLength][];   //without age 0
                    for (int tt = 0; tt < timeSeriesLength; tt++) {
                        BIOMQuadri[xx][i][0][tt] = new float[tempIndex];
                    }
                }
            }
        }
    }

    public void readDietsOutputFile(String dietsConfigFileName, int numSerie) {
        nbDietsStages[numSerie] = new int[nbSpeciesTab[numSerie]];
        dietStageThreshold[numSerie] = new float[nbSpeciesTab[numSerie]][];
        if (dietsConfigFileName.equalsIgnoreCase("byDefault")) {
            System.out.println("Diets output by default");
            dietOutputMetrics[numSerie] = "default";
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                nbDietsStages[numSerie][i] = 1;
                dietStageThreshold[numSerie][i] = new float[0];
            }
        } else {
            FileInputStream dietConfigFile;
            try {
                dietConfigFile = new FileInputStream(new File(inputPathName, dietsConfigFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("diet configuration file doesn't exist: " + dietsConfigFileName);
                return;
            }

            Reader r = new BufferedReader(new InputStreamReader(dietConfigFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            try {
                st.nextToken();
                if ((new Integer(st.sval)).intValue() == nbSpeciesTab[numSerie]) {
                    st.nextToken();
                    if ((new Integer(st.sval)).intValue() == nbPlanktonGroupsTab[numSerie]) {
                        st.nextToken();
                        if (st.sval.equalsIgnoreCase("age") || st.sval.equalsIgnoreCase("size")) {
                            dietOutputMetrics[numSerie] = st.sval;
                            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                                st.nextToken();
                                nbDietsStages[numSerie][i] = (new Integer(st.sval)).intValue();
                                dietStageThreshold[numSerie][i] = new float[nbDietsStages[numSerie][i] - 1];
                                for (int j = 0; j < nbDietsStages[numSerie][i] - 1; j++) {
                                    st.nextToken();
                                    dietStageThreshold[numSerie][i][j] = (new Float(st.sval)).floatValue();
                                }
                            }
                        } else {
                            System.out.println("metrics used is diets config file is not correct");
                        }
                    }
                }

                dietConfigFile.close();
            } catch (IOException ex) {
                System.out.println("Reading error of diets config file");
                return;
            }
            System.out.println("diets config file read");
        }
    }

    public void saveSerieSimulations(int nSerie) // ************************** seuls les fichiers biomasses, abundances, yield,
    //**************************size, mortalites et size spectrum per species sont OK � 100%
    {
        //save in output files
        File targetPath;

        String inputFileName = outputFileNameTab[nSerie] + "_I";
        String biomFileName = outputFileNameTab[nSerie] + "_B.csv";

        targetPath = new File(outputPathName + outputFileNameTab[nSerie]);
        targetPath.mkdirs();

        saveInputParameters(targetPath, inputFileName, nSerie);
        if (calibrationMatrix[nSerie]) {
            saveBIOMData(targetPath, inputFileName, biomFileName);
        }

    }

    public void saveInputParameters(File targetPath, String inputFileName, int nSerie) {

        FileOutputStream inputFile;
        File targetFile;
        PrintWriter pw;
        try {
            targetFile = new File(targetPath, inputFileName);
            inputFile = new FileOutputStream(targetFile);
        } catch (IOException e) {
            System.out.println("Error of input file creation");
            return;
        }
        pw = new PrintWriter(inputFile, true);

        pw.println("// ------SAVING------");
        pw.print("// Results saved every ");
        pw.print(savingDtMatrix[nSerie]);
        pw.println(" time step(s) ");
        pw.println("// Results averaged EXCEPT for yield (biomass and nb) where results are summed ");
        pw.println();

        pw.println("// ---- simulation OPTIONS ---- ");
        pw.print("// Lattice Size ");
        pw.print(';');
        pw.print(gridLinesTab[nSerie]);
        pw.print('*');
        pw.println(gridColumnsTab[nSerie]);
        pw.print("//Fish movement");
        pw.print(';');

        pw.print("//Species areas file");
        pw.print(';');
        pw.println(areasFileNameTab[nSerie]);
        /*		if(areasFileNameTab[nSerie].equalsIgnoreCase("Random"))
        {
        pw.print("//Species areas size");pw.print(';');pw.println(speciesAreasSizeTab[nSerie]);
        }
        pw.print("//Coast file");pw.print(';');pw.println(coastFileNameTab[nSerie]);
        if(!coastFileNameTab[nSerie].equalsIgnoreCase("default"))
        {
        pw.print("//Coast size");pw.print(';');pw.println(tabCoastiMatrix[nSerie].length);
        }
        pw.print("//MPA file");pw.print(';');pw.println(mpaFileNameTab[nSerie]);
        if(!mpaFileNameTab[nSerie].equalsIgnoreCase("default"))
        {
        pw.print("//MPA size");pw.print(';');pw.println(tabMPAiMatrix[nSerie].length);
        }
         */ pw.println();
        pw.println("// ----- BIOLOGICAL PARAMETERS -----   ");
        pw.print("Nb species ");
        pw.print(';');
        pw.print(nbSpeciesTab[nSerie]);
        pw.println(';');
        pw.print("Name ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].name);
        }
        pw.println();
        pw.print("D ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].D);
        }
        pw.println();
        pw.print("F ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].F);
        }
        pw.println();
        pw.print("recruitment age ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].recruitAge);
        }
        pw.println();
        pw.print("longevity ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].longevity);
        }
        pw.println();
        pw.print("lInf ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].lInf);
        }
        pw.println();
        pw.print("K ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].K);
        }
        pw.println();
        pw.print("t0 ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].t0);
        }
        pw.println();
        pw.print("c ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].c);
        }
        pw.println();
        pw.print("b ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].bPower);
        }
        pw.println();

        pw.print("relFecund ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].alpha);
        }
        pw.println();
        pw.print("sizeMat ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].sizeMat);
        }
        pw.println();
        pw.print("eggSize ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].eggSize);
        }
        pw.println();
        pw.print("eggWeight ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].eggWeight);
        }
        pw.println();
        pw.print("sex ratio ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].sexRatio);
        }
        pw.println();

        pw.println("// -----  INITIALISATION ---- ");
        pw.println();

        pw.print("Initial biomass for serie : ");

        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(iniBiomass[nSerie][i]);
        }
        pw.println();
        pw.println("// -----  CALIBRATION ---- ");
        pw.print("Larval mortality ");
        for (int i = 0; i < simulation.species.length; i++) {
            pw.print(';');
            pw.print(simulation.species[i].larvalSurvival);
        }
        pw.println();
        pw.print("accessibility coefficient ");
        for (int i = 0; i < simulation.couple.nbPlankton; i++) {
            pw.print(';');
            pw.print(simulation.couple.planktonList[i].accessibilityCoeff);
        }
        pw.close();
        System.out.println("Input data saved");
    }

    public void saveBIOMData(File targetPath, String inputFileName, String biomFileName) {

        FileOutputStream biomFile;
        File targetFile;
        PrintWriter pw;

        float[][][] tabMean, tabCv;
        tabMean = new float[nbSpeciesTab[numSerie] + 1 + nbPlanktonGroupsTab[numSerie]][][];
        tabCv = new float[nbSpeciesTab[numSerie] + 1 + nbPlanktonGroupsTab[numSerie]][][];

        for (int i = 0; i <= nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]; i++) {
            tabMean[i] = new float[2][];
            tabMean[i][0] = new float[nbLoopTab[numSerie]];
            tabMean[i][1] = new float[nbLoopTab[numSerie]];
            tabCv[i] = new float[2][];
            tabCv[i][0] = new float[nbLoopTab[numSerie]];
            tabCv[i][1] = new float[nbLoopTab[numSerie]];
        }

        float MEAN, STD, CV;//mean, std et cv des moyennes de chaque simu


        try {
            targetFile = new File(targetPath, biomFileName);
            biomFile = new FileOutputStream(targetFile);
        } catch (IOException e) {
            System.out.println("Error of biomass file creation");
            return;
        }
        pw = new PrintWriter(biomFile, true);
        pw.println("//File containing the set of input parameters " + inputFileName);
        pw.println("//BIOMASS in tonnes");
        for (int xx = 0; xx < nbLoopTab[numSerie]; xx++) {
            float std;
            float[][] sum = new float[timeSeriesLength][];
            pw.println("SIMULATION n " + xx);
            pw.println();
            //results for tot-0
            for (int t = 0; t < timeSeriesLength; t++) {
                sum[t] = new float[nbDtSavePerYear[numSerie]];
                for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                    sum[t][dt] = 0;
                }
            }
            pw.print("TOTAL-0");
            pw.println(';');
            pw.print("Time");
            pw.print(';');

            for (int t = timeSeriesStart; t < simulationTimeTab[numSerie]; t++) {
                for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                    pw.print((float) (t + dt / (float) nbDtSavePerYear[numSerie]));
                    pw.print(';');
                }
            }
            pw.println();

            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                pw.print(nameSpecMatrix[numSerie][i]);
                pw.print(';');
                for (int t = 0; t < timeSeriesLength; t++) {
                    for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                        sum[t][dt] += BIOMQuadri[xx][i][0][t][dt];
                        pw.print(BIOMQuadri[xx][i][0][t][dt]);
                        pw.print(';');
                    }
                }
                tabMean[i][0][xx] = mean2(BIOMQuadri[xx][i][0]);
                std = std2(BIOMQuadri[xx][i][0]);
                tabCv[i][0][xx] = std / tabMean[i][0][xx];
                pw.print(';');
                pw.print("mean-0");
                pw.print(';');
                pw.print(tabMean[i][0][xx]);
                pw.print(';');
                pw.print("std-0");
                pw.print(';');
                pw.print(std);
                pw.print(';');
                pw.print("cv-0");
                pw.print(';');
                pw.print(tabCv[i][0][xx]);
                pw.print(';');
                pw.println();
            }
            /*   for(int i=nbSpeciesTab[numSerie]; i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab[numSerie]; i++)
            {
            pw.print("Plankton Gr"+(i-nbSpeciesTab[numSerie]+1));pw.print(';');
            for(int t=0;t<timeSeriesLength;t++)
            for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
            {
            pw.print( BIOMQuadri[xx][i][0][t][dt] );
            pw.print(';');
            }
            tabMean[i][0][xx]=mean2(BIOMQuadri[xx][i][0]);
            std=std2(BIOMQuadri[xx][i][0]);
            tabCv[i][0][xx]=std/tabMean[i][0][xx];
            pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[i][0][xx]);pw.print(';');
            pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
            pw.print("cv-0");pw.print(';');pw.print(tabCv[i][0][xx]);pw.print(';');
            pw.println();

            }
             */
            pw.print("sys-0");
            pw.print(';');
            for (int t = 0; t < timeSeriesLength; t++) {
                for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                    pw.print(sum[t][dt]);
                    pw.print(';');
                }
            }
            tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0][xx] = mean2(sum);
            std = std2(sum);
            tabCv[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0][xx] = std / tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0][xx];
            pw.print(';');
            pw.print("mean-0");
            pw.print(';');
            pw.print(tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0][xx]);
            pw.print(';');
            pw.print("std-0");
            pw.print(';');
            pw.print(std);
            pw.print(';');
            pw.print("cv-0");
            pw.print(';');
            pw.print(tabCv[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0][xx]);
            pw.print(';');
            pw.println();
            pw.println();

            //bloc resultats pour total = with age 0
            if (outputClass0Matrix[numSerie]) {
                for (int t = 0; t < timeSeriesLength; t++) {
                    sum[t] = new float[nbDtSavePerYear[numSerie]];
                    for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                        sum[t][dt] = 0;
                    }
                }
                pw.print("TOTAL");
                pw.println(';');
                pw.print("Time");
                pw.print(';');
                for (int t = timeSeriesStart; t < simulationTimeTab[numSerie]; t++) {
                    for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                        pw.print((float) (t + dt / (float) nbDtSavePerYear[numSerie]));
                        pw.print(';');
                    }
                }
                pw.println();

                for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                    pw.print(nameSpecMatrix[numSerie][i]);
                    pw.print(';');
                    for (int t = 0; t < timeSeriesLength; t++) {
                        for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                            sum[t][dt] += BIOMQuadri[xx][i][1][t][dt];
                            pw.print(BIOMQuadri[xx][i][1][t][dt]);
                            pw.print(';');
                        }
                    }
                    tabMean[i][1][xx] = mean2(BIOMQuadri[xx][i][1]);
                    std = std2(BIOMQuadri[xx][i][1]);
                    tabCv[i][1][xx] = std / tabMean[i][1][xx];
                    pw.print(';');
                    pw.print("mean");
                    pw.print(';');
                    pw.print(tabMean[i][1][xx]);
                    pw.print(';');
                    pw.print("std");
                    pw.print(';');
                    pw.print(std);
                    pw.print(';');
                    pw.print("cv");
                    pw.print(';');
                    pw.print(tabCv[i][1][xx]);
                    pw.print(';');
                    pw.println();
                }
                /*   for(int i=nbSpeciesTab[numSerie]; i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab[numSerie]; i++)
                {
                pw.print("Plankton Gr"+(i-nbSpeciesTab[numSerie]+1));pw.print(';');
                for(int t=0;t<timeSeriesLength;t++)
                for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
                {
                pw.print( BIOMQuadri[xx][i][0][t][dt] );
                pw.print(';');
                }
                tabMean[i][0][xx]=mean2(BIOMQuadri[xx][i][0]);
                std=std2(BIOMQuadri[xx][i][0]);
                tabCv[i][0][xx]=std/tabMean[i][0][xx];
                pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[i][0][xx]);pw.print(';');
                pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
                pw.print("cv-0");pw.print(';');pw.print(tabCv[i][0][xx]);pw.print(';');
                pw.println();

                }
                 */
                pw.print("sys");
                pw.print(';');
                for (int t = 0; t < timeSeriesLength; t++) {
                    for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++) {
                        pw.print(sum[t][dt]);
                        pw.print(';');
                    }
                }
                tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1][xx] = mean2(sum);
                std = std2(sum);
                tabCv[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1][xx] = std / tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1][xx];
                pw.print(';');
                pw.print("mean");
                pw.print(';');
                pw.print(tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1][xx]);
                pw.print(';');
                pw.print("std");
                pw.print(';');
                pw.print(std);
                pw.print(';');
                pw.print("cv");
                pw.print(';');
                pw.print(tabCv[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1][xx]);
                pw.print(';');
                pw.println();
                pw.println();
            }
        }
        pw.println("SYNTHESIS");
        //tot-0
        pw.print("TOT-0");
        pw.print(';');
        pw.print("MEAN(mean-0)");
        pw.print(';');
        pw.print("STD(mean-0)");
        pw.print(';');
        pw.print("CV(mean-0)");
        pw.print(';');
        pw.print("MEAN(cv-0)");
        pw.println(';');
        for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
            MEAN = mean(tabMean[i][0]);
            STD = std(tabMean[i][0]);
            CV = STD / MEAN;
            pw.print(nameSpecMatrix[numSerie][i]);
            pw.print(';');
            pw.print(MEAN);
            pw.print(';');
            pw.print(STD);
            pw.print(';');
            pw.print(CV);
            pw.print(';');
            pw.print(mean(tabCv[i][0]));
            pw.println(';');
        }
        /*     for(int i=nbSpeciesTab[numSerie];i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab;i++)
        {
        MEAN = mean(tabMean[i][0]);
        STD = std(tabMean[i][0]);
        CV = STD/MEAN;
        pw.print("Plankton group "+(i+1-nbSpeciesTab[numSerie]));pw.print(';');pw.print(MEAN);pw.print(';');
        pw.print(STD);pw.print(';');
        pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
        }

         */
        MEAN = mean(tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0]);
        STD = std(tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0]);
        CV = STD / MEAN;
        pw.print("SYS-0");
        pw.print(';');
        pw.print(MEAN);
        pw.print(';');
        pw.print(STD);
        pw.print(';');
        pw.print(CV);
        pw.print(';');
        pw.print(mean(tabCv[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][0]));
        pw.println(';');
        pw.println();

        //bloc tot
        if (outputClass0Matrix[numSerie]) {
            pw.print("TOT");
            pw.print(';');
            pw.print("MEAN(mean)");
            pw.print(';');
            pw.print("STD(mean)");
            pw.print(';');
            pw.print("CV(mean)");
            pw.print(';');
            pw.print("MEAN(cv)");
            pw.println(';');
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                MEAN = mean(tabMean[i][1]);
                STD = std(tabMean[i][1]);
                CV = STD / MEAN;
                pw.print(nameSpecMatrix[numSerie][i]);
                pw.print(';');
                pw.print(MEAN);
                pw.print(';');
                pw.print(STD);
                pw.print(';');
                pw.print(CV);
                pw.print(';');
                pw.print(mean(tabCv[i][1]));
                pw.println(';');
            }
            /*     for(int i=nbSpeciesTab[numSerie];i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab;i++)
            {
            MEAN = mean(tabMean[i][0]);
            STD = std(tabMean[i][0]);
            CV = STD/MEAN;
            pw.print("Plankton group "+(i+1-nbSpeciesTab[numSerie]));pw.print(';');pw.print(MEAN);pw.print(';');
            pw.print(STD);pw.print(';');
            pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
            }
             */
            MEAN = mean(tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1]);
            STD = std(tabMean[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1]);
            CV = STD / MEAN;
            pw.print("SYS");
            pw.print(';');
            pw.print(MEAN);
            pw.print(';');
            pw.print(STD);
            pw.print(';');
            pw.print(CV);
            pw.print(';');
            pw.print(mean(tabCv[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][1]));
            pw.println(';');

        }
        pw.close();

        System.out.println("biom data saved");
    }

    public float mean(float[] val) {
        float mean;
        float sum = 0;
        int n = val.length;
        for (int i = 0; i < val.length; i++) {
            sum += val[i];
        }
        mean = sum / n;
        return mean;
    }

    public float mean2(float[][] val) {
        float mean;
        float sum = 0;
        int n = 0;
        for (int i = 0; i < val.length; i++) {
            for (int j = 0; j < val[i].length; j++) {
                sum += val[i][j];
                n++;
            }
        }
        mean = sum / n;
        return mean;
    }

    public float mean2d(double[][] val) {
        float mean;
        float sum = 0;
        int n = 0;
        for (int i = 0; i < val.length; i++) {
            for (int j = 0; j < val[i].length; j++) {
                sum += val[i][j];
                n++;
            }
        }
        mean = sum / n;
        return mean;
    }

    public float std(float[] val) {
        float std;
        double sum, sum2;
        sum = 0;
        sum2 = 0;
        int n = val.length;
        for (int i = 0; i < val.length; i++) {
            sum += (double) val[i];
            sum2 += (Math.pow((double) val[i], 2));
        }
        double numerator, denominator;
        numerator = n * sum2 - Math.pow(sum, 2);
        denominator = (double) n * (n - 1);
        std = (float) Math.sqrt(numerator / denominator);
        return std;
    }

    public float std2(float[][] val) {
        float std;
        double sum, sum2;
        sum = 0;
        sum2 = 0;
        int n = 0;
        for (int i = 0; i < val.length; i++) {
            for (int j = 0; j < val[i].length; j++) {
                sum += (double) val[i][j];
                sum2 += (Math.pow((double) val[i][j], 2));
                n++;
            }
        }
        double numerator, denominator;
        numerator = n * sum2 - Math.pow(sum, 2);
        denominator = (double) n * (n - 1);
        std = (float) Math.sqrt(numerator / denominator);
        return std;
    }

    //Methods of means and std for calculating SBIs, without null values
    public float meanWithout0(float[][] val, float[][] abd) {
        float mean;
        float sum = 0;
        int n = 0;
        for (int i = 0; i < val.length; i++) {
            for (int j = 0; j < val[i].length; j++) {
                if (abd[i][j] != 0) {
                    sum += val[i][j];
                    n++;
                }
            }
        }
        mean = sum / n;
        return mean;
    }

    public float stdWithout0(float[][] val, float[][] abd) {
        float std;
        double sum, sum2;
        sum = 0;
        sum2 = 0;
        int n = 0;
        for (int i = 0; i < val.length; i++) {
            for (int j = 0; j < val[i].length; j++) {
                if (abd[i][j] != 0) {
                    sum += (double) val[i][j];
                    sum2 += (Math.pow((double) val[i][j], 2));
                    n++;
                }
            }
        }
        double numerator, denominator;
        numerator = n * sum2 - Math.pow(sum, 2);
        denominator = (double) n * (n - 1);
        std = (float) Math.sqrt(numerator / denominator);
        return std;
    }

    public void runSimulation() {

        while (simulation.t < simulationTimeTab[numSerie]) {
            simulation.step();
        }
    }

    public static void main(String args[]) {
        System.out.println(new Date());
        Osmose osmose = new Osmose();
        osmose.runSeriesSimulations();
        System.out.println(new Date());
    }
}
