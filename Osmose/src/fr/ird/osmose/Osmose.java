package fr.ird.osmose;

/**
 * *****************************************************************************
 * <p>Titre : Osmose </p>
 *
 * <p>Description : Main class of the Osmose model - reads the input files and
 * initialize the series and simulations - save the biomass file in case of
 * calibration - run the simulations </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * ******************************************************************************
 */
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.util.IOTools;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.nc2.NetcdfFileWriteable;

public class Osmose {

    /*
     * ********
     * * Logs * ******** 2011/04/07 phv Created static instance of the Osmose
     * object. Added a getGrid() method that other classes will call through the
     * Osmose instance. Added a getSimulation() method for other classes to get
     * the simulation instance. public void distribRandom(). Recoded the random
     * sorting of the neighbor cells using method Grid.getNeighborCells since
     * Cell.neighbors[] has been deleted. ***
     */

    /*
     * Static instance of Osmose
     */
    private static Osmose osmose = new Osmose();
    /*
     *
     */
    final String fileSeparator = System.getProperty("file.separator");
    String inputPathName, outputPathName, inputTxtName;
    /*
     *
     */
    private Simulation simulation;
    private IGrid grid;
    private int nbSeriesSimus;	// nb of series
    int[] nbLoopTab;	// nb of simulations per serie
    int numSerie, numSimu;
    // TABLES OF INPUT FILES NAMES (one entry per serie)
    //for 1 serie of simus, same species parameters and options
    String[] configFileNameTab, speciesFileNameTab, predationFileNameTab, fishingFileNameTab,
            planktonFileNameTab, planktonStructureFileNameTab, calibrationFileNameTab, outputFileNameTab,
            indicatorsFileNameTab, outputPrefix;
    // Tables of Optional Input Files names (with entries as file name or "default")
    String[] size0FileNameTab, migrationFileNameTab, accessibilitiesFileNameTab,
            reproductionFileNameTab, fishingSeasonFileNameTab, /*
             * � creer
             */
            couplingFileNameTab;
    boolean[] isForcing;
    private String[] lowTLClassNameTab;
    /*
     * SPECIES PARAMETERS FILE
     */
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
    boolean[][] reproduceLocallyTab;
    float[][] biomassFluxInTab;
    float[][] meanLengthFishInTab;
    float[][] meanAgeFishInTab;
    /*
     * FISHING
     */
    String[] globalOrCohortTab;
    float[][] FMatrix, recruitSizeMatrix;
    float[][][] seasonFishingMatrix;
    /*
     * PLANKTON groups
     */
    int[] nbPlanktonGroupsTab;
    String[][] planktonNamesTab;
    /*
     * CONFIG & OPTIONS
     */
    String[] gridClassNameTab;
    String[] gridFileTab, lonFieldTab, latFieldTab, maskFieldTab;
    int[] strideTab;
    int[] gridLinesTab, gridColumnsTab;
    float[] upLeftLatTab, lowRightLatTab, upLeftLongTab, lowRightLongTab;
    int[] simulationTimeTab, nbDtMatrix, savingDtMatrix, nbDtSavePerYear;
    /*
     * Parameters
     */
    int[] nbSchools;
    float[] planktonAccessCoeffMatrix;
    int[] nbAccessStage;
    float[][] accessStageThreshold;
    float[][][][] accessibilityMatrix;
    /*
     * SPECIES AREAS FILE
     */
    String[] areasFileNameTab;	              //choice between "Random" or fileName
    int[][] speciesAreasSizeTab;	    //used only for Qsimulation.iniRepartitionAleat() ie for random distribution
    int[][] randomAreaCoordi, randomAreaCoordj;//species areas in random cases [species][cell]
    int[][][] numMap;        //gives a number of map for[species][cohort][dt]
    int[][] mapCoordi, mapCoordj;      //    coord of maps [numMap][cell]
    float[][] mapProbaPresence; // Probability of presence of a cohort [numMap][cell]
    boolean densityMaps;
    ConnectivityMatrix[] connectivityMatrix;
    /*
     * COASTLINE
     */
    String[] coastFileNameTab;	              //choice between "None" or fileName
    int[][] tabCoastiMatrix, tabCoastjMatrix;   //coordinates of the cells representing land
    int[] nbCellsCoastTab;
    /*
     * MPAs coordinates
     */
    String[] mpaFileNameTab;
    int[][] tabMPAiMatrix, tabMPAjMatrix;     //coord i et j of the matrix delimiting a mpa
    boolean[] thereIsMPATab;		      //signify that 1 mpa is implemented even if t<tStart
    int[] MPAtStartTab, MPAtEndTab;	      //start and end of MPA in years
    //tables for output storage by series of simulations
    //4 dimensions : simu, species,val(total OR total-0), step t
    //for mortalities, 3 dim, the last is for the mean on the simulation period
    float[][][][][] BIOMQuadri;   //[numSimu][species][with or without age 0][t][dt]
    float[][] iniBiomass; //used for saving the biomass after initialization
    /*
     * INDICATORS OUTPUT
     */
    boolean[] TLoutputMatrix, TLDistriboutputMatrix, dietsOutputMatrix, meanSizeOutputMatrix,
            sizeSpectrumOutputMatrix, sizeSpectrumPerSpeOutputMatrix,
            planktonMortalityOutputMatrix, calibrationMatrix, outputClass0Matrix, spatializedOutputs;
    boolean[] planktonBiomassOutputMatrix;
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
    // migration
    float[][] migrationTempMortality;
    int[][] migrationTempAge, migrationTempDt;
    // distribution
    int[][] areasTempAge;
    int[][] areasTempDt;
    int[] areasNumSpForMap;
    SpatialDistribution[] spatialDistribution;
    /**
     * Object for creating/writing netCDF files.
     */
    private static NetcdfFileWriteable ncOut;
    
    public void initSimulation() {
        
        readInputFile();	// read the first file containing the file names of all other input files
        for (int x = 0; x < nbSeriesSimus; x++) {
            readAllInputFiles(x);
        }
        initializeSizeAndTLSpectrum();
    }
    
    public void loadMPAs() {
        for (int index = 0; index < tabMPAiMatrix[numSerie].length; index++) {
            getGrid().getCell(tabMPAiMatrix[numSerie][index], tabMPAjMatrix[numSerie][index]).setMPA(true);
        }
    }
    
    public void initializeSizeAndTLSpectrum() {
        if (sizeSpectrumOutputMatrix[numSerie] || sizeSpectrumPerSpeOutputMatrix[numSerie]) {
            //initialisation of the size spectrum features
            nbSizeClass = (int) Math.ceil(spectrumMaxSize / classRange);//size classes of 5 cm

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
             * if(x==0)	// for the first serie : all input files are read and
             * kept in memory readAllInputFiles(x); else	// for the following
             * series, only the files that differ from the previous one are
             * read, other input are already in memory { int previousSerie =
             * x-1; checkInputFilesChange(x, previousSerie); }
             */
            for (int xx = 0; xx < nbLoopTab[x]; xx++) {
                numSimu = xx;
                r.gc();
                freeMem = r.freeMemory();
                System.out.println("Simulation " + xx + "        **** FREE MEMORY = " + freeMem);
                /*
                 * Delete older output directory and all its content
                 */
                File targetPath = new File(outputPathName + outputFileNameTab[numSerie]);
                if ((numSimu == 0) && targetPath.exists()) {
//                    if (targetPath.list() != null) {
//                        System.out.println("Output folder " + targetPath + " already contains some files and/or folders.");
//                        System.out.println("Should we delete them (y=yes, n=no, c=cancel) ?");
//                        try {
//                            char answer = (char) System.in.read();
//                            switch (answer) {
//                                case 'y':
//                                    IOTools.deleteDirectory(targetPath);
//                                    break;
//                                case 'n':
//                                    break;
//                                default:
//                                    System.exit(0);
//
//                            }
//                        } catch (IOException ex) {
//                            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
//                            System.exit(0);
//                        }
//                    }
                    IOTools.deleteDirectory(targetPath);
                }
                if (spatializedOutputs[numSerie]) {
                    createNCFile(numSerie);
                }
                if (numSimu == 0) {
                    initializeOptions();
                    System.out.println("options initialized");
                    
                    simulation = new Simulation();
                    simulation.init();
                    System.out.println("simulation initialized");
                    
                    readMigrationFile();
                    System.out.println("migration caracteristics initialized");
                    
                    readAreaFile();
                    System.out.println("areas initialized");

                    //in initialiserSpeciesareas, save in tabTemp the areas by cohort
                    //do not distribute the species in simulation()
                    initializeOutputData();
                    System.out.println("output data initialized");
                    
                } else {
                    try {
                        grid = (IGrid) Class.forName(gridClassNameTab[numSerie]).newInstance();
                        grid.init();
                    } catch (InstantiationException ex) {
                        Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    if (!(coastFileNameTab[numSerie].equalsIgnoreCase("None") || coastFileNameTab[numSerie].equalsIgnoreCase("default"))) {
                        updateCoastCells(numSerie);
                    }
                    
                    simulation = new Simulation();
                    simulation.init();
                    
                    readMigrationFile();
                    readAreaFile();
                    System.out.println();
                }
                
                runSimulation();
                if (spatializedOutputs[numSerie]) {
                    closeNCFile();
                }
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
        readOutputConfigurationFile(indicatorsFileNameTab[numSerie], numSerie);
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
            readOutputConfigurationFile(indicatorsFileNameTab[newNumSerie], newNumSerie);
        }
        
        if (dietsOutputMatrix[newNumSerie]) {
            if (!(dietsConfigFileName[newNumSerie].equals(dietsConfigFileName[previousNumSerie]))) {
                readDietsOutputFile(dietsConfigFileName[newNumSerie], newNumSerie);
            }
        }
        
        
    }
    
    public String readPathFile() // read the file situated within the source code directory
    {
        FileInputStream pathFile = null;
        try {
            pathFile = new FileInputStream(new File("filePath.txt"));
        } catch (FileNotFoundException ex) {
            System.err.println("initial path file doesn't exist");
        }
        
        Reader r = new BufferedReader(new InputStreamReader(pathFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');
        
        System.out.println("1. Reading the filePath.txt");
        try {
            st.nextToken();
            File inputFile = new File(st.sval);
            return inputFile.getAbsolutePath();
        } catch (IOException ex) {
            System.err.println("Reading error of path file");
            return null;
        }
    }

    // read the first file, that should be named INPUT.txt, situated at the path given by filePath.txt
    public void readInputFile() {
        System.out.println("2. Reading file INPUT.txt");
        FileInputStream inputFile;
        try {
            inputFile = new FileInputStream(resolveFile(inputTxtName));
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
            System.out.println("  Number of series = " + nbSeriesSimus);
        } catch (IOException ex) {
            System.out.println("  Reading error of INPUT file");
            return;
        }

        // give the right dimension to tables according to the number of series specified
        initializeNbSeriesInTables(nbSeriesSimus);
        
        try {
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                nbLoopTab[x] = (new Integer(st.sval)).intValue();
                System.out.println("  Number of replicated simulations per serie = " + nbLoopTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                configFileNameTab[x] = st.sval;
                System.out.println("  Configuration file = " + configFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                speciesFileNameTab[x] = st.sval;
                System.out.println("  Species file = " + speciesFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                predationFileNameTab[x] = st.sval;
                System.out.println("  Predation file = " + predationFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                fishingFileNameTab[x] = st.sval;
                System.out.println("  Fishing file = " + fishingFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                areasFileNameTab[x] = st.sval;
                System.out.println("  Areas file = " + areasFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                planktonStructureFileNameTab[x] = st.sval;
                System.out.println("  Plankton groups file = " + planktonStructureFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                planktonFileNameTab[x] = st.sval;
                System.out.println("  Plankton data file = " + planktonFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                calibrationFileNameTab[x] = st.sval;
                System.out.println("  Calibration file = " + calibrationFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                indicatorsFileNameTab[x] = st.sval;
                System.out.println("  Indicators file = " + indicatorsFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                accessibilitiesFileNameTab[x] = st.sval;
                System.out.println("  Accessibilities file = " + accessibilitiesFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                size0FileNameTab[x] = st.sval;
                System.out.println("  Size age-class zero file = " + size0FileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                reproductionFileNameTab[x] = st.sval;
                System.out.println("  Reproduction file = " + reproductionFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                fishingSeasonFileNameTab[x] = st.sval;
                System.out.println("  Fishing season file = " + fishingSeasonFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                migrationFileNameTab[x] = st.sval;
                System.out.println("  Migration file = " + migrationFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                coastFileNameTab[x] = st.sval;
                System.out.println("  Coast file = " + coastFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                mpaFileNameTab[x] = st.sval;
                System.out.println("  MPA file = " + mpaFileNameTab[x]);
            }
            for (int x = 0; x < nbSeriesSimus; x++) {
                st.nextToken();
                couplingFileNameTab[x] = st.sval;
                System.out.println("  Coupling file = " + couplingFileNameTab[x]);
            }
            inputFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of INPUT file");
            return;
        }
        System.out.println("EOF for step 2. Reading file INPUT.txt");
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
        outputPrefix = new String[nbSeriesSimus];
        planktonFileNameTab = new String[nbSeriesSimus];
        planktonStructureFileNameTab = new String[nbSeriesSimus];
        indicatorsFileNameTab = new String[nbSeriesSimus];
        couplingFileNameTab = new String[nbSeriesSimus];
        accessibilitiesFileNameTab = new String[nbSeriesSimus];
        fishingSeasonFileNameTab = new String[nbSeriesSimus];

        //--- CONFIGURATION file---
        gridClassNameTab = new String[nbSeriesSimus];
        gridFileTab = new String[nbSeriesSimus];
        lonFieldTab = new String[nbSeriesSimus];
        latFieldTab = new String[nbSeriesSimus];
        maskFieldTab = new String[nbSeriesSimus];
        strideTab = new int[nbSeriesSimus];
        gridLinesTab = new int[nbSeriesSimus];
        gridColumnsTab = new int[nbSeriesSimus];
        simulationTimeTab = new int[nbSeriesSimus];
        nbDtMatrix = new int[nbSeriesSimus];
        savingDtMatrix = new int[nbSeriesSimus];
        nbDtSavePerYear = new int[nbSeriesSimus];
        startingSavingTimeTab = new int[nbSeriesSimus];
        nbSpeciesTab = new int[nbSeriesSimus];
        isForcing = new boolean[nbSeriesSimus];
        lowTLClassNameTab = new String[nbSeriesSimus];

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
        reproduceLocallyTab = new boolean[nbSeriesSimus][];
        biomassFluxInTab = new float[nbSeriesSimus][];
        meanLengthFishInTab = new float[nbSeriesSimus][];
        meanAgeFishInTab = new float[nbSeriesSimus][];

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
        speciesAreasSizeTab = new int[nbSeriesSimus][];
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
        spatializedOutputs = new boolean[nbSeriesSimus];
        planktonBiomassOutputMatrix = new boolean[nbSeriesSimus];
        dietsConfigFileName = new String[nbSeriesSimus];
        dietOutputMetrics = new String[nbSeriesSimus];
        nbDietsStages = new int[nbSeriesSimus][];
        dietStageThreshold = new float[nbSeriesSimus][][];
    }
    
    public void readSpeciesFile(String speciesFileName, int numSerie) {
        FileInputStream speciesFile;
        try {
            speciesFile = new FileInputStream(resolveFile(speciesFileName));
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
                /*
                 * phv 2011/11/21 Determine wether reproduction occurs locally
                 * or outside the simulated domain. Then for species reproducing
                 * outside it reads the incoming biomass caracteristics.
                 */
                reproduceLocallyTab[numSerie] = new boolean[nbSpeciesTab[numSerie]];
                biomassFluxInTab[numSerie] = new float[nbSpeciesTab[numSerie]];
                meanLengthFishInTab[numSerie] = new float[nbSpeciesTab[numSerie]];
                meanAgeFishInTab[numSerie] = new float[nbSpeciesTab[numSerie]];
                st.nextToken();
                int nbReproOut = 0;
                if (null != st.sval) {
                    for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                        reproduceLocallyTab[numSerie][i] = st.sval.matches("in");
                        if (!reproduceLocallyTab[numSerie][i]) {
                            nbReproOut++;
                        }
                        st.nextToken();
                    }
                    for (int i = 0; i < nbReproOut; i++) {
                        int indexSpecies = new Integer(st.sval).intValue() - 1;
                        st.nextToken();
                        biomassFluxInTab[numSerie][indexSpecies] = new Float(st.sval).floatValue();
                        st.nextToken();
                        meanLengthFishInTab[numSerie][indexSpecies] = new Float(st.sval).floatValue();
                        st.nextToken();
                        meanAgeFishInTab[numSerie][indexSpecies] = new Float(st.sval).floatValue();
                        st.nextToken();
                    }
                } else {
                    for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                        reproduceLocallyTab[numSerie][i] = true;
                    }
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
                reproductionFile = new FileInputStream(resolveFile(reproductionFileName));
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
                fishingFile = new FileInputStream(resolveFile(fishingFileName));
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
            fishingFile = new FileInputStream(resolveFile(fishingFileName));
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
            calibFile = new FileInputStream(resolveFile(calibrationFileName));
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
            configFile = new FileInputStream(resolveFile(configFileName));
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

            /*
             * addition phv 2011/08/02 since Coupling.java (renamed as
             * LTLCouplingRomsPisces) is model specific, I had to code new
             * classes for ECO3M (Danial for GL) and BFM (Camille Adriatic). So
             * in the config file I added a parameter 3.12 to identify the Java
             * class that implements the forcing/coupling.
             */
            st.nextToken();
            lowTLClassNameTab[numSerie] = st.sval;
            
            st.nextToken();
            nbSchools[numSerie] = 1 + Math.round(((new Integer(st.sval)).intValue()) / nbDtMatrix[numSerie]);

            /*
             * Additional parameters to read the grid from NetCDF file
             */
            st.nextToken();
            if (null != st.sval) {
                gridClassNameTab[numSerie] = st.sval;
                st.nextToken();
                String filename = resolveFile(st.sval).toString();
                gridFileTab[numSerie] = filename;
                st.nextToken();
                lonFieldTab[numSerie] = st.sval;
                st.nextToken();
                latFieldTab[numSerie] = st.sval;
                st.nextToken();
                maskFieldTab[numSerie] = st.sval;
                st.nextToken();
                strideTab[numSerie] = Integer.valueOf(st.sval);
            } else {
                gridClassNameTab[numSerie] = OriginalGrid.class.getCanonicalName();
            }
            
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
                optionFile = new FileInputStream(resolveFile(optionFileName));
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
            predationFile = new FileInputStream(resolveFile(predationFileName));
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
    
    public void readOutputConfigurationFile(String indicatorsFileName, int numSerie) {
        FileInputStream indicFile;
        try {
            indicFile = new FileInputStream(resolveFile(indicatorsFileName));
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
            String[] split = st.sval.split("/");
            outputPrefix[numSerie] = split[split.length - 1];
            
            st.nextToken();
            calibrationMatrix[numSerie] = false;
            if (st.sval.equalsIgnoreCase("calibration")) {
                calibrationMatrix[numSerie] = true;
            }
            
            st.nextToken();
            TLoutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            TLDistriboutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            dietsOutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            dietsConfigFileName[numSerie] = st.sval;
            st.nextToken();
            meanSizeOutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            sizeSpectrumOutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            sizeSpectrumPerSpeOutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            if (sizeSpectrumOutputMatrix[numSerie] || sizeSpectrumPerSpeOutputMatrix[numSerie]) {
                st.nextToken();
                spectrumMinSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                spectrumMaxSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                classRange = (new Float(st.sval)).floatValue();
            }
            st.nextToken();
            planktonMortalityOutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            outputClass0Matrix[numSerie] = (new Boolean(st.sval)).booleanValue();
            try {
                /*
                 * phv 2011/06/30 Read additional parameters "spatialized
                 * outputs" Since it might not exist in most configurations I
                 * catch any exception and set it as false by default.
                 */
                st.nextToken();
                spatializedOutputs[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            } catch (Exception ex) {
                spatializedOutputs[numSerie] = false;
            }
            try {
                /*
                 * phv 2012/05/29 Read additional parameters "palnkton biomass"
                 * Since it might not exist in most configurations I catch any
                 * exception and set it as false by default.
                 */
                st.nextToken();
                planktonBiomassOutputMatrix[numSerie] = (Boolean.valueOf(st.sval)).booleanValue();
            } catch (Exception ex) {
                planktonBiomassOutputMatrix[numSerie] = false;
            }
            
            indicFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of output config file");
            return;
        }
    }
    
    public void initializeOptions() {
        try {
            System.out.println("Initialize grid: " + gridClassNameTab[numSerie]);
            grid = (IGrid) Class.forName(gridClassNameTab[numSerie]).newInstance();
            grid.init();
            if (coastFileNameTab[numSerie].equalsIgnoreCase("None")) {
                nbCellsCoastTab[numSerie] = 0;
            } else {
                initializeCoast();
            }
            initializeMPA();
        } catch (InstantiationException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readCoastAsCSV(String csvFile) {
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            int nbCells = 0;
            for (String[] line : lines) {
                for (String str : line) {
                    if (Integer.valueOf(str) <= 0) {
                        nbCells++;
                    }
                }
            }
            nbCellsCoastTab[numSerie] = nbCells;

            /*
             * Identify the coordinates
             */
            int indexCell = 0;
            tabCoastiMatrix[numSerie] = new int[nbCells];
            tabCoastjMatrix[numSerie] = new int[nbCells];
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val <= 0.f) {
                        tabCoastiMatrix[numSerie][indexCell] = i;
                        tabCoastjMatrix[numSerie][indexCell] = j;
                        indexCell++;
                    }
                }
            }
            for (int k = 0; k < tabCoastiMatrix[numSerie].length; k++) {
                grid.getCell(tabCoastiMatrix[numSerie][k], tabCoastjMatrix[numSerie][k]).setLand(true);
            }
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readCoastCoordinates(String coastFilename) {
        FileInputStream coastFile;
        
        try {
            coastFile = new FileInputStream(coastFilename);
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
        /*
         * structure of file: nb lines; nb columns; nb Cells for coast(land); in
         * lines coord i of coast; in lines coord j of coast;
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
        }
    }
    
    public void initializeCoast() {
        if (coastFileNameTab[numSerie].equalsIgnoreCase("default")) {
            System.out.println("No coast in the grid (default)");
        } else {
            
            String filename = resolveFile(coastFileNameTab[numSerie]);
            if (filename.endsWith("csv")) {
                /*
                 * load coast cell from CSV file
                 */
                readCoastAsCSV(filename);
            } else {

                /*
                 * load coast from old osmose coast file
                 */
                readCoastCoordinates(filename);
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
                mpaFile = new FileInputStream(resolveFile(mpaFileNameTab[numSerie]));
            } catch (FileNotFoundException ex) {
                System.out.println("Error while opening mpaFile");
                return;
            }
            Reader r = new BufferedReader(new InputStreamReader(mpaFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');
            /*
             * structure of file: (diff with osmose: MPAtStart et MPAtEnd in
             * addition) nb lines; nb columns; nbCells of mpa;
             * MPAtstart;MPAtEnd; in lines, coord i of mpa; in lines coord j of
             * mpa;
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
                        st.nextToken();
                        /*
                         * Check whether the next value is an integer ==> MPA is
                         * defined directly in the file, or a string==> MPA is
                         * defined in separate CSV file.
                         */
                        boolean mpaFromCSV = false;
                        try {
                            Integer.parseInt(st.sval);
                        } catch (NumberFormatException ex) {
                            mpaFromCSV = true;
                        }
                        if (mpaFromCSV) {
                            /*
                             * Case CSV
                             */
                            readMPA(resolveFile(st.sval));
                        } else {
                            /*
                             * Case coordinates
                             */
                            for (int i = 0; i < tabi.length; i++) {
                                tabi[i] = new Integer(st.sval).intValue();
                                st.nextToken();
                            }
                            for (int j = 0; j < tabj.length; j++) {
                                tabj[j] = new Integer(st.sval).intValue();
                                st.nextToken();
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
    
    public void writeMPAasCSV() {
        
        String fileName = "mpa.csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(fileName)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                String[] entries = new String[getGrid().getNbColumns()];
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    if (getGrid().getCell(i, j).isLand()) {
                        entries[j] = String.valueOf(-99);
                    } else if (getGrid().getCell(i, j).isMPA()) {
                        entries[j] = String.valueOf(1);
                    } else {
                        entries[j] = String.valueOf(0);
                    }
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeMapsAsCSV() {
        
        int numberMaps = mapCoordi.length;
        for (int k = 0; k < numberMaps; k++) {
            try {
                List<Cell> map = getMap(k);
                String fileName = "maps/map" + numMapToString(k + 1, numberMaps) + "-" + getSimulation().getSpecies(areasNumSpForMap[k]).getName().trim() + ".csv";
                CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(fileName)), ';', CSVWriter.NO_QUOTE_CHARACTER);
                // Golf of Lyons
                /*
                 * for (int j = getGrid().getNbColumns() - 1; j >= 0; j--) {
                 * String[] entries = new String[getGrid().getNbLines()]; for
                 * (int i = 0; i < getGrid().getNbLines(); i++) { if
                 * (getGrid().getCell(i, j).isLand()) { entries[i] =
                 * String.valueOf(-99); } else if
                 * (map.contains(getGrid().getCell(i, j))) { entries[i] =
                 * String.valueOf(1); } else { entries[i] = String.valueOf(0); }
                 * } writer.writeNext(entries); }
                 */
                // Benguela
                for (int i = 0; i < getGrid().getNbLines(); i++) {
                    String[] entries = new String[getGrid().getNbColumns()];
                    for (int j = 0; j < getGrid().getNbColumns(); j++) {
                        if (getGrid().getCell(i, j).isLand()) {
                            entries[j] = String.valueOf(-99);
                        } else if (map.contains(getGrid().getCell(i, j))) {
                            entries[j] = String.valueOf(1);
                        } else {
                            entries[j] = String.valueOf(0);
                        }
                    }
                    writer.writeNext(entries);
                }
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
        }
    }
    
    public void writeGlobalDistributionMap() {
        try {
            int numberMaps = mapCoordi.length;
            String fileName = "map-all.csv";
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(fileName)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            
            int[][] area = new int[getGrid().getNbLines()][getGrid().getNbColumns()];
            for (int k = 0; k < numberMaps; k++) {
                
                if (areasNumSpForMap[k] == 1 || areasNumSpForMap[k] == 5 || areasNumSpForMap[k] == 6 || areasTempAge[k][0] == 0) {
                    System.out.println(getSimulation().getSpecies(areasNumSpForMap[k]).getName() + " " + areasTempAge[k][0]);
                    continue;
                }
                
                List<Cell> map = getMap(k);
                for (Cell cell : map) {
                    area[cell.get_igrid()][cell.get_jgrid()] = 1;
                }
            }
            
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                String[] entries = new String[getGrid().getNbColumns()];
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    if (getGrid().getCell(i, j).isLand()) {
                        entries[j] = String.valueOf(-99);
                    } else {
                        entries[j] = String.valueOf(area[i][j]);
                    }
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("########## map finished !");
    }
    
    private String numMapToString(int numMap, int numberMaps) {
        
        String snumMap = String.valueOf(numMap);
        int lengthNumberMaps = String.valueOf(numberMaps).length();
        while (snumMap.length() < lengthNumberMaps) {
            snumMap = "0" + snumMap;
        }
        return snumMap;
    }

    public void readAreaFile() {

        /*
         * Open areas-maps configuration file
         */
        FileInputStream areasFile;
        try {
            areasFile = new FileInputStream(resolveFile(areasFileNameTab[numSerie]));
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening areasFile");
            return;
        }
        /*
         * Initialize the reader
         */
        Reader r = new BufferedReader(new InputStreamReader(areasFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');
        spatialDistribution = new SpatialDistribution[nbSpeciesTab[numSerie]];
        speciesAreasSizeTab[numSerie] = new int[nbSpeciesTab[numSerie]];
        randomAreaCoordi = new int[nbSpeciesTab[numSerie]][];
        randomAreaCoordj = new int[nbSpeciesTab[numSerie]][];
        try {
            
            for (int i = 0; i < nbSpeciesTab[numSerie]; i++) {
                st.nextToken();
                String areaDistribMethod = st.sval;
                if (areaDistribMethod.equalsIgnoreCase("random")) {
                    spatialDistribution[i] = SpatialDistribution.RANDOM;
                } else if (areaDistribMethod.equalsIgnoreCase("maps")) {
                    spatialDistribution[i] = SpatialDistribution.MAPS;
                } else if (areaDistribMethod.equalsIgnoreCase("connectivity")) {
                    spatialDistribution[i] = SpatialDistribution.CONNECTIVITY;
                } else {
                    throw new UnsupportedOperationException("Distribution method is either 'random' | 'maps' | 'connectivity'");
                }
            }
            /*
             * get number of maps
             */
            st.nextToken();
            int nbMaps = new Integer(st.sval).intValue();
            mapCoordi = new int[nbMaps][];
            mapCoordj = new int[nbMaps][];
            connectivityMatrix = new ConnectivityMatrix[nbMaps];
            mapProbaPresence = new float[nbMaps][];
            areasNumSpForMap = new int[nbMaps];
            areasTempAge = new int[nbMaps][];
            areasTempDt = new int[nbMaps][];
            numMap = new int[nbSpeciesTab[numSerie]][][];
            for (int iSpec = 0; iSpec < nbSpeciesTab[numSerie]; iSpec++) {
                Species speci = simulation.getSpecies(iSpec);
                numMap[iSpec] = new int[speci.getNumberCohorts()][];
                for (int j = 0; j < speci.getNumberCohorts(); j++) {
                    numMap[iSpec][j] = new int[nbDtMatrix[numSerie]];
                }
            }

            /**
             * Loop over the maps
             */
            for (int indexMap = 0; indexMap < nbMaps; indexMap++) {
                /*
                 * read species number
                 */
                st.nextToken();
                int iSpec = new Integer(st.sval).intValue() - 1;
                switch (spatialDistribution[iSpec]) {
                    case RANDOM:
                        st.nextToken();
                        speciesAreasSizeTab[numSerie][iSpec] = (new Integer(st.sval)).intValue();
                        distribRandom(iSpec);
                        break;
                    case MAPS:
                        readAreaCSV(st, iSpec, indexMap);
                        break;
                    case CONNECTIVITY:
                        readConnectivity(st, iSpec, indexMap);
                        speciesAreasSizeTab[numSerie][iSpec] = (int) (0.75 * getGrid().getNumberAvailableCells());
                        distribRandom(iSpec);
                        break;
                }
            }
            
            
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                r.close();
            } catch (IOException ex) {
                Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        /*
         * Initial distribution of the scools
         */
        simulation.distributeSpeciesIni();
    }
    
    private void readConnectivity(StreamTokenizer st, int iSpec, int indexMap) throws IOException {
        
        System.out.println("Reading connectivity matric for " + getSimulation().getSpecies(iSpec).getName() + " map " + indexMap);
        
        areasNumSpForMap[indexMap] = iSpec;
        /*
         * read number of class ages concerned by this map
         */
        st.nextToken();
        int nbAgePerMap = new Integer(st.sval).intValue();
        areasTempAge[indexMap] = new int[nbAgePerMap];
        /*
         * read number of time step over the year concerned by this map
         */
        st.nextToken();
        int nbDtPerMap = new Integer(st.sval).intValue();
        areasTempDt[indexMap] = new int[nbDtPerMap];
        /*
         * read the age classes concerned by this map
         */
        for (int k = 0; k < nbAgePerMap; k++) {
            st.nextToken();
            areasTempAge[indexMap][k] = new Integer(st.sval).intValue();
        }
        /*
         * read the time steps over the year concerned by this map
         */
        for (int k = 0; k < nbDtPerMap; k++) {
            st.nextToken();
            areasTempDt[indexMap][k] = new Integer(st.sval).intValue() - 1;
        }
        /*
         * Read the name of the connectivity file and load the matrix If
         * name = "null" it means the species is out of the simulated domain
         * at these age-class and time-step
         */
        st.nextToken();
        if (!"null".equals(st.sval)) {
            String csvFile = resolveFile(st.sval);
            connectivityMatrix[indexMap] = new ConnectivityMatrix(indexMap, csvFile);
            /*
             * Set the numero of maps per species, age class and time step
             */
            for (int m = 0; m < areasTempAge[indexMap].length; m++) {
                for (int n = 0; n < areasTempDt[indexMap].length; n++) {
                    for (int h = 0; h < nbDtMatrix[numSerie]; h++) {
                        if ((areasTempAge[indexMap][m] * nbDtMatrix[numSerie] + h) < simulation.getSpecies(areasNumSpForMap[indexMap]).getNumberCohorts()) {
                            numMap[areasNumSpForMap[indexMap]][areasTempAge[indexMap][m] * nbDtMatrix[numSerie] + h][areasTempDt[indexMap][n]] = indexMap;
                        }
                    }
                }
            }
        }
        System.out.println("Connectivity matrix loaded");
    }
    
    private void readAreaCSV(StreamTokenizer st, int iSpec, int indexMap) throws IOException {
        
        areasNumSpForMap[indexMap] = iSpec;   //because species number between 1 and nbSpecies
            /*
         * read number of class ages concerned by this map
         */
        st.nextToken();
        int nbAgePerMap = new Integer(st.sval).intValue();
        areasTempAge[indexMap] = new int[nbAgePerMap];
        /*
         * read number of time step over the year concerned by this map
         */
        st.nextToken();
        int nbDtPerMap = new Integer(st.sval).intValue();
        areasTempDt[indexMap] = new int[nbDtPerMap];
        /*
         * read the age classes concerned by this map
         */
        for (int k = 0; k < nbAgePerMap; k++) {
            st.nextToken();
            areasTempAge[indexMap][k] = new Integer(st.sval).intValue();
        }
        /*
         * read the time steps over the year concerned by this map
         */
        for (int k = 0; k < nbDtPerMap; k++) {
            st.nextToken();
            areasTempDt[indexMap][k] = new Integer(st.sval).intValue() - 1;
        }
        /*
         * read the name of the CSV file and load the map if name = "null"
         * it means the species is out of the simulated domain at these
         * age-class and time-step
         */
        st.nextToken();
        if (!"null".equals(st.sval)) {
            String csvFile = resolveFile(st.sval);
            readCSVMap(csvFile, indexMap);
        }
    }
    
    private void readMPA(String csvFile) {
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            int nbCells = 0;
            for (String[] line : lines) {
                for (String str : line) {
                    if (Integer.valueOf(str) > 0) {
                        nbCells++;
                    }
                }
            }

            /*
             * Identify the coordinates
             */
            int indexCell = 0;
            tabMPAiMatrix[numSerie] = new int[nbCells];
            tabMPAjMatrix[numSerie] = new int[nbCells];
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val > 0.f) {
                        tabMPAiMatrix[numSerie][indexCell] = i;
                        tabMPAjMatrix[numSerie][indexCell] = j;
                        indexCell++;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readCSVMap(String csvFile, int indexMap) {
        
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            /*
             * Get the number of cells for the map phv 28 march 2012 - it is a
             * loss of time to do twice the loop over the CSV file but no choice
             * at the moment without changing mapCoordi, mapCoordj
             */
            int nbCells = 0;
            for (String[] line : lines) {
                for (String str : line) {
                    //if (Integer.valueOf(str) > 0) {
                    if (Float.valueOf(str) > 0.f) {
                        nbCells++;
                    }
                }
            }
            /*
             * Initialize the arrays
             */
            mapCoordi[indexMap] = new int[nbCells];
            mapCoordj[indexMap] = new int[nbCells];
            mapProbaPresence[indexMap] = new float[nbCells];
            /*
             * Set the numero of maps per species, age class and time step
             */
            for (int m = 0; m < areasTempAge[indexMap].length; m++) {
                for (int n = 0; n < areasTempDt[indexMap].length; n++) {
                    for (int h = 0; h < nbDtMatrix[numSerie]; h++) {
                        if ((areasTempAge[indexMap][m] * nbDtMatrix[numSerie] + h) < simulation.getSpecies(areasNumSpForMap[indexMap]).getNumberCohorts()) {
                            numMap[areasNumSpForMap[indexMap]][areasTempAge[indexMap][m] * nbDtMatrix[numSerie] + h][areasTempDt[indexMap][n]] = indexMap;
                            //System.out.println("NumMap: " + areasNumSpForMap[indexMap] + " " + (areasTempAge[indexMap][m] * nbDtMatrix[numSerie] + h) + " " + (areasTempDt[indexMap][n]) + " " + indexMap);
                        }
                        if (mapCoordi[indexMap].length == 0) {
                            if (!simulation.getSpecies(areasNumSpForMap[indexMap]).getCohort((areasTempAge[indexMap][m] * nbDtMatrix[numSerie]) + h).isOut(areasTempDt[indexMap][n])) {
                                System.out.println("Match error between species areas and migration file for " + simulation.getSpecies(areasNumSpForMap[indexMap]).getName());
                            }
                        }
                    }
                }
            }
            /*
             * Identify the coordinates of the cells and set the probability
             */
            int indexCell = 0;
            float invNbCells = 1.f / nbCells;
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val > 0.f) {
                        mapCoordi[indexMap][indexCell] = i;
                        mapCoordj[indexMap][indexCell] = j;
                        if (val < 1.f) {
                            /*
                             * value provided is directly a probability
                             */
                            mapProbaPresence[indexMap][indexCell] = val;
                        } else if (val == 1.f) {
                            /*
                             * map is presence/absence so equal probability of
                             * presence among cells
                             */
                            mapProbaPresence[indexMap][indexCell] = invNbCells;
                            /*
                             * else mapProbaPresence[indexMap][indexCell] = 0
                             * default value at initialization of the array
                             */
                        }
                        indexCell++;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("Read CSV file " + csvFile + " [OK]");
    }
    
    public List<Cell> getMap(int numMap) {
        
        int length = mapCoordi[numMap].length;
        List<Cell> list = new ArrayList(length);
        for (int k = 0; k < length; k++) {
            list.add(getGrid().getCell(mapCoordi[numMap][k], mapCoordj[numMap][k]));
        }
        return list;
    }
    
    public String getMapDetails(int numMap) {
        StringBuilder str = new StringBuilder();
        str.append("Map: ");
        str.append(numMap);
        str.append(" - Species: ");
        str.append(getSimulation().getSpecies(areasNumSpForMap[numMap]).getName());
        str.append(" - Age class: ");
        for (int k = 0; k < areasTempAge[numMap].length; k++) {
            str.append(areasTempAge[numMap][k]);
            str.append(" ");
        }
        str.append("- Time step: ");
        for (int k = 0; k < areasTempDt[numMap].length; k++) {
            str.append(areasTempDt[numMap][k]);
            str.append(" ");
        }
        return str.toString();
    }
    
    public void distribRandom(int iSpec) {

        //int nbCasesDispos = ((int) (gridLinesTab[numSerie] * gridColumnsTab[numSerie])) - nbCellsCoastTab[numSerie];
        int nbCasesDispos = grid.getNumberAvailableCells();

        //Case where random distribution on the whole (grid-coast)
        if (speciesAreasSizeTab[numSerie][iSpec] >= nbCasesDispos) {
            speciesAreasSizeTab[numSerie][iSpec] = nbCasesDispos;
            
            randomAreaCoordi[iSpec] = new int[speciesAreasSizeTab[numSerie][iSpec]];
            randomAreaCoordj[iSpec] = new int[speciesAreasSizeTab[numSerie][iSpec]];
            
            int index = 0;
            for (int l = 0; l < grid.getNbLines(); l++) {
                for (int m = 0; m < grid.getNbColumns(); m++) {
                    if (!grid.getCell(l, m).isLand()) {
                        
                        randomAreaCoordi[iSpec][index] = grid.getCell(l, m).get_igrid();
                        randomAreaCoordj[iSpec][index] = grid.getCell(l, m).get_jgrid();
                        
                        index++;
                    }
                }
            }
        } //case where random disribution on speciesAreasSize cells
        //random sorting of connex cells for each species
        else {
            randomAreaCoordi[iSpec] = new int[speciesAreasSizeTab[numSerie][iSpec]];
            randomAreaCoordj[iSpec] = new int[speciesAreasSizeTab[numSerie][iSpec]];
            for (int l = 0; l < grid.getNbLines(); l++) {
                for (int m = 0; m < grid.getNbColumns(); m++) {
                    grid.getCell(l, m).setAlreadyChosen(false);
                }
            }
            Cell[] tabCellsArea = new Cell[speciesAreasSizeTab[numSerie][iSpec]];
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
             * From initial cell, successive random sorting of the
             * adjacent cells until tabCellsArea is full
             */
            int iFirstSorted = 0;
            int iLastSorted = 0;
            int index = 0;
            while (index < (tabCellsArea.length - 1)) {
                for (int iCell = iFirstSorted; iCell <= iLastSorted; iCell++) {
                    ArrayList<Cell> neigbors = grid.getNeighborCells(tabCellsArea[iCell]);
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
                randomAreaCoordi[iSpec][m] = tabCellsArea[m].get_igrid();
                randomAreaCoordj[iSpec][m] = tabCellsArea[m].get_jgrid();
            }
        }
    }
    
    public void readMigrationFile() {
        if (migrationFileNameTab[numSerie].equalsIgnoreCase("default")) {
            System.out.println("No migrations implemented (default)");
        } else {
            /*
             * Structure of file seasonalityFile (IN CAPITAL default options)
             * ;nb species migrating; ;num of species ;number of ages; ;number
             * of time steps ;ages ;time steps ;mortality ... * nb species
             * migrating
             */
            
            FileInputStream outOfZoneFile;
            try {
                outOfZoneFile = new FileInputStream(resolveFile(migrationFileNameTab[numSerie]));
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
            migrationTempAge = new int[nbSpeciesTab[numSerie]][];
            migrationTempDt = new int[nbSpeciesTab[numSerie]][];
            migrationTempMortality = new float[nbSpeciesTab[numSerie]][];
            
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
                        st.nextToken();
                        nbDtPerCase = new Integer(st.sval).intValue();
                        migrationTempAge[numSpOutOfZone - 1] = new int[nbAgePerCase];
                        migrationTempDt[numSpOutOfZone - 1] = new int[nbDtPerCase];
                        migrationTempMortality[numSpOutOfZone - 1] = new float[nbAgePerCase];
                        
                        for (int k = 0; k < nbAgePerCase; k++) {
                            st.nextToken();
                            migrationTempAge[numSpOutOfZone - 1][k] = new Integer(st.sval).intValue();
                        }
                        for (int k = 0; k < nbDtPerCase; k++) {
                            st.nextToken();
                            migrationTempDt[numSpOutOfZone - 1][k] = new Integer(st.sval).intValue() - 1;
                        }
                        
                        for (int m = 0; m < nbAgePerCase; m++) {
                            st.nextToken();
                            migrationTempMortality[numSpOutOfZone - 1][m] = (new Float(st.sval)).floatValue();
                            for (int n = 0; n < nbDtPerCase; n++) {
                                for (int h = 0; h < nbDtMatrix[numSerie]; h++) {
                                    simulation.getSpecies(numSpOutOfZone - 1).getCohort(migrationTempAge[numSpOutOfZone - 1][m] * nbDtMatrix[numSerie] + h).setOut(migrationTempDt[numSpOutOfZone - 1][n], true);
                                    simulation.getSpecies(numSpOutOfZone - 1).getCohort(migrationTempAge[numSpOutOfZone - 1][m] * nbDtMatrix[numSerie] + h).setOutMortality(migrationTempDt[numSpOutOfZone - 1][n], migrationTempMortality[numSpOutOfZone - 1][m]);
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
                    /*
                     * phv 2011/11/18 set default access matrix to 0.8 for being
                     * compatible with Osmose 2.0
                     */
                    accessibilityMatrix[i][0][j][0] = 0.8f;
                }
            }
            for (int i = nbSpeciesTab[numSerie]; i < nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]; i++) {
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nbSpeciesTab[numSerie]][];
                for (int j = 0; j < nbSpeciesTab[numSerie]; j++) {
                    accessibilityMatrix[i][0][j] = new float[1];
                    accessibilityMatrix[i][0][j][0] = 0.8f;
                }
            }
        } else // in case of an accessibility file specified
        {
            FileInputStream accessFile;
            try {
                accessFile = new FileInputStream(resolveFile(accessFileName));
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
                for (int j = simulation.getSpecies(i).indexAgeClass0; j < simulation.getSpecies(i).getNumberCohorts(); j++) {
                    iniBiomass[x][i] += (float) simulation.getSpecies(i).getCohort(j).getBiomass();
                }
            }
        }
        
        
        for (int xx = 0; xx < nbLoopTab[numSerie]; xx++) {
            if (calibrationMatrix[numSerie]) {
                BIOMQuadri[xx] = new float[nbSpeciesTab[numSerie] + nbPlanktonGroupsTab[numSerie]][][][];
            }

            /*
             * iniBiomass[xx] = new float[nbSpeciesTab[numSerie]]; for(int
             * i=0;i<nbSpeciesTab[numSerie];i++) iniBiomass[xx][i] = 0; for(int
             * i=0;i<nbSpeciesTab[numSerie];i++) for(int
             * j=simulation.getSpecies(i).indexAgeClass0;j<simulation.getSpecies(i).nbCohorts;j++)
             * iniBiomass[xx][i] += (float)
             * simulation.getSpecies(i).tabCohorts[j].biomass;
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
                dietConfigFile = new FileInputStream(resolveFile(dietsConfigFileName));
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
    
    private void createNCFile(int nSerie) {
        try {
            ncOut = NetcdfFileWriteable.createNew("");
            ncOut.setLocation(makeFileLocation(nSerie));
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Closes the NetCDF file.
     */
    private void closeNCFile() {
        try {
            ncOut.close();
            String strFilePart = ncOut.getLocation();
            String strFileBase = strFilePart.substring(0, strFilePart.indexOf(".part"));
            File filePart = new File(strFilePart);
            File fileBase = new File(strFileBase);
            filePart.renameTo(fileBase);
        } catch (Exception ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.WARNING, "Problem closing the NetCDF output file ==> " + ex.toString());
        }
    }
    
    public NetcdfFileWriteable getNCOut() {
        return ncOut;
    }
    
    private String makeFileLocation(int nSerie) throws IOException {
        
        StringBuilder filename = new StringBuilder();
        filename.append(outputPathName);
        filename.append(outputFileNameTab[nSerie]);
        filename.append(fileSeparator);
        filename.append(outputPrefix[nSerie]);
        filename.append("_spatialized_Simu");
        filename.append(nSerie);
        filename.append(".nc");
        File file = new File(filename.toString());
        try {
            IOTools.makeDirectories(file.getAbsolutePath());
            file.createNewFile();
            file.delete();
        } catch (Exception ex) {
            IOException ioex = new IOException("{Ouput} Failed to create NetCDF file " + filename + " ==> " + ex.getMessage());
            ioex.setStackTrace(ex.getStackTrace());
            throw ioex;
        }
        return filename + ".part";
    }
    
    public void saveSerieSimulations(int nSerie) // ************************** seuls les fichiers biomasses, abundances, yield,
    //**************************size, mortalites et size spectrum per species sont OK � 100%
    {
        //save in output files
        File targetPath;
        
        String inputFileName = outputPrefix[nSerie] + "_I";
        String biomFileName = outputPrefix[nSerie] + "_B.csv";
        
        targetPath = new File(outputPathName + outputFileNameTab[nSerie]);
        targetPath.mkdirs();

        //saveInputParameters(targetPath, inputFileName, nSerie);
        if (calibrationMatrix[nSerie]) {
            saveBIOMData(targetPath, inputFileName, biomFileName);
        }
        
    }

    /*
     * phv 2011/12/07 obsolete function. Parameters and options added by Morgane
     * have not been included here.
     */
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
        /*
         * if(areasFileNameTab[nSerie].equalsIgnoreCase("Random")) {
         * pw.print("//Species areas
         * size");pw.print(';');pw.println(speciesAreasSizeTab[nSerie]); }
         * pw.print("//Coast
         * file");pw.print(';');pw.println(coastFileNameTab[nSerie]);
         * if(!coastFileNameTab[nSerie].equalsIgnoreCase("default")) {
         * pw.print("//Coast
         * size");pw.print(';');pw.println(tabCoastiMatrix[nSerie].length); }
         * pw.print("//MPA
         * file");pw.print(';');pw.println(mpaFileNameTab[nSerie]);
         * if(!mpaFileNameTab[nSerie].equalsIgnoreCase("default")) {
         * pw.print("//MPA
         * size");pw.print(';');pw.println(tabMPAiMatrix[nSerie].length); }
         */ pw.println();
        pw.println("// ----- BIOLOGICAL PARAMETERS -----   ");
        pw.print("Nb species ");
        pw.print(';');
        pw.print(nbSpeciesTab[nSerie]);
        pw.println(';');
        pw.print("Name ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).getName());
        }
        pw.println();
        pw.print("D ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).D);
        }
        pw.println();
        pw.print("F ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).F);
        }
        pw.println();
        pw.print("recruitment age ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).recruitAge);
        }
        pw.println();
        pw.print("longevity ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).longevity);
        }
        pw.println();
        pw.print("lInf ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).lInf);
        }
        pw.println();
        pw.print("K ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).K);
        }
        pw.println();
        pw.print("t0 ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).t0);
        }
        pw.println();
        pw.print("c ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).c);
        }
        pw.println();
        pw.print("b ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).bPower);
        }
        pw.println();
        
        pw.print("relFecund ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).alpha);
        }
        pw.println();
        pw.print("sizeMat ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).sizeMat);
        }
        pw.println();
        pw.print("eggSize ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).eggSize);
        }
        pw.println();
        pw.print("eggWeight ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).eggWeight);
        }
        pw.println();
        pw.print("sex ratio ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).sexRatio);
        }
        pw.println();
        
        pw.println("// -----  INITIALISATION ---- ");
        pw.println();
        
        pw.print("Initial biomass for serie : ");
        
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(iniBiomass[nSerie][i]);
        }
        pw.println();
        pw.println("// -----  CALIBRATION ---- ");
        pw.print("Larval mortality ");
        for (int i = 0; i < simulation.getNbSpecies(); i++) {
            pw.print(';');
            pw.print(simulation.getSpecies(i).larvalSurvival);
        }
        pw.println();
        pw.print("accessibility coefficient ");
        for (int i = 0; i < simulation.getForcing().getNbPlanktonGroups(); i++) {
            pw.print(';');
            pw.print(simulation.getForcing().getPlankton(i).getAccessibilityCoeff());
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
            /*
             * for(int i=nbSpeciesTab[numSerie];
             * i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab[numSerie]; i++) {
             * pw.print("Plankton
             * Gr"+(i-nbSpeciesTab[numSerie]+1));pw.print(';'); for(int
             * t=0;t<timeSeriesLength;t++) for(int
             * dt=0;dt<nbDtSavePerYear[numSerie];dt++) { pw.print(
             * BIOMQuadri[xx][i][0][t][dt] ); pw.print(';'); }
             * tabMean[i][0][xx]=mean2(BIOMQuadri[xx][i][0]);
             * std=std2(BIOMQuadri[xx][i][0]);
             * tabCv[i][0][xx]=std/tabMean[i][0][xx];
             * pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[i][0][xx]);pw.print(';');
             * pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
             * pw.print("cv-0");pw.print(';');pw.print(tabCv[i][0][xx]);pw.print(';');
             * pw.println();
             *
             * }
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
                /*
                 * for(int i=nbSpeciesTab[numSerie];
                 * i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab[numSerie]; i++)
                 * { pw.print("Plankton
                 * Gr"+(i-nbSpeciesTab[numSerie]+1));pw.print(';'); for(int
                 * t=0;t<timeSeriesLength;t++) for(int
                 * dt=0;dt<nbDtSavePerYear[numSerie];dt++) { pw.print(
                 * BIOMQuadri[xx][i][0][t][dt] ); pw.print(';'); }
                 * tabMean[i][0][xx]=mean2(BIOMQuadri[xx][i][0]);
                 * std=std2(BIOMQuadri[xx][i][0]);
                 * tabCv[i][0][xx]=std/tabMean[i][0][xx];
                 * pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[i][0][xx]);pw.print(';');
                 * pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
                 * pw.print("cv-0");pw.print(';');pw.print(tabCv[i][0][xx]);pw.print(';');
                 * pw.println();
                 *
                 * }
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
        /*
         * for(int
         * i=nbSpeciesTab[numSerie];i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab;i++)
         * { MEAN = mean(tabMean[i][0]); STD = std(tabMean[i][0]); CV =
         * STD/MEAN; pw.print("Plankton group
         * "+(i+1-nbSpeciesTab[numSerie]));pw.print(';');pw.print(MEAN);pw.print(';');
         * pw.print(STD);pw.print(';');
         * pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
         * }
         *
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
            /*
             * for(int
             * i=nbSpeciesTab[numSerie];i<nbSpeciesTab[numSerie]+nbPlanktonGroupsTab;i++)
             * { MEAN = mean(tabMean[i][0]); STD = std(tabMean[i][0]); CV =
             * STD/MEAN; pw.print("Plankton group
             * "+(i+1-nbSpeciesTab[numSerie]));pw.print(';');pw.print(MEAN);pw.print(';');
             * pw.print(STD);pw.print(';');
             * pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
             * }
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
        
        while (simulation.getYear() < simulationTimeTab[numSerie]) {
            simulation.step();
        }
    }
    
    public String getLTLClassName() {
        return lowTLClassNameTab[numSerie];
    }

    /*
     * Function for dealing with command line arguments From David K. for the GA
     */
    public void loadArgs(String[] args) {
        // Get command line arguments
        if (args.length > 0) {
            inputPathName = args[0];
        } else {
            // This will not have trailing file separator - no idea if this is a problem
            inputPathName = readPathFile();
            System.out.println("Input path ==> " + inputPathName);
        }
        
        if (args.length > 1) {
            outputPathName = args[1];
        } else {
            outputPathName = inputPathName + fileSeparator + "output" + fileSeparator;
        }
        
        if (args.length > 2) {
            inputTxtName = args[2];
        } else {
            inputTxtName = "INPUT.txt";
        }
        
    }

    /*
     * Point d'entrée du programme
     */
    public static void main(String... args) {
        System.out.println(new Date());
        osmose.loadArgs(args);
        osmose.initSimulation();
        osmose.runSeriesSimulations();
        System.out.println(new Date());
    }
    
    public static Osmose getInstance() {
        return osmose;
    }
    
    public IGrid getGrid() {
        return grid;
    }
    
    public Simulation getSimulation() {
        return simulation;
    }
    
    public String resolvePath(String path) {
        String pathname = resolveFile(path);
        if (!pathname.endsWith(File.separator)) {
            pathname += File.separator;
        }
        return pathname;
    }
    
    public String resolveFile(String filename) {
        try {
            File file = new File(inputPathName);
            String pathname = new File(file.toURI().resolve(filename)).getCanonicalPath();
            return pathname;
        } catch (Exception e) {
            return filename;
        }
    }
    
    public void initAll(String args[]) {
        
        loadArgs(args);
        initSimulation();
        initializeOptions();
        loadMPAs();
        initializeOptions();
        if (spatializedOutputs[numSerie]) {
            createNCFile(numSerie);
        }
        simulation = new Simulation();
        simulation.init();
        readMigrationFile();
        readAreaFile();
        initializeOutputData();
        readMigrationFile();
    }
    
    public int getRecordFrequency() {
        return savingDtMatrix[numSerie];
    }
    
    public boolean isCalibrationOutput() {
        return calibrationMatrix[numSerie];
    }
    
    public boolean isTLOutput() {
        return TLoutputMatrix[numSerie];
    }
    
    public boolean isTLDistribOutput() {
        return TLDistriboutputMatrix[numSerie];
    }
    
    public boolean isDietOuput() {
        return dietsOutputMatrix[numSerie];
    }
    
    public boolean isMeanSizeOutput() {
        return meanSizeOutputMatrix[numSerie];
    }
    
    public boolean isSizeSpectrumOutput() {
        return sizeSpectrumOutputMatrix[numSerie];
    }
    
    public boolean isSizeSpectrumSpeciesOutput() {
        return sizeSpectrumPerSpeOutputMatrix[numSerie];
    }
    
    public boolean isPlanktonMortalityOutput() {
        return planktonMortalityOutputMatrix[numSerie];
    }
    
    public boolean isIncludeClassZero() {
        return outputClass0Matrix[numSerie];
    }
    
    public String getDietOutputMetric() {
        return dietOutputMetrics[numSerie];
    }
    
    public enum SpatialDistribution {
        
        RANDOM,
        MAPS,
        CONNECTIVITY;
    }
}
