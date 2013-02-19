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
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.grid.OriginalGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.util.IOTools;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Osmose {

    /*
     * Static instance of Osmose
     */
    private static Osmose osmose = new Osmose();
    // phv 20130208
    // Temporary flag to be able to read new format for HABITAT configuration
    // file with agemin and agemax specified.
    public final static boolean NEW_AREA_FILE = false;
    /*
     *
     */
    public final String fileSeparator = System.getProperty("file.separator");
    public String inputPathName, outputPathName, inputTxtName;
    /*
     *
     */
    private Simulation simulation;
    private IGrid grid;
    /*
     * Forcing with Biogeochimical model.
     */
    private LTLForcing forcing;
    int nbLoopTab;	// nb of simulations per serie
    // TABLES OF INPUT FILES NAMES (one entry per serie)
    //for 1 serie of simus, same species parameters and options
    public String configFileNameTab, speciesFileNameTab, predationFileNameTab, fishingFileNameTab,
            planktonFileNameTab, planktonStructureFileNameTab, calibrationFileNameTab, outputFileNameTab,
            indicatorsFileNameTab, outputPrefix;
    // Tables of Optional Input Files names (with entries as file name or "default")
    String size0FileNameTab, migrationFileNameTab, accessibilitiesFileNameTab,
            reproductionFileNameTab, fishingSeasonFileNameTab, /*
             * � creer
             */
            couplingFileNameTab;
    boolean isForcing;
    private String lowTLClassNameTab;
    /*
     * SPECIES PARAMETERS FILE
     */
    int nbSpeciesTab;
    String[] nameSpecMatrix, maturityMetricMatrix;
    String recruitMetricMatrix;
    public float[] DMatrix, starvMaxRateMatrix;
    public float[][] larvalMortalityRates;
    float[] longevityMatrix;
    float[] lInfMatrix, KMatrix, t0Matrix, cMatrix, bPowerMatrix;
    public float[] alphaMatrix, sizeMatMatrix;
    float[][] seasonSpawningMatrix;
    public float[][] sizeFeedingMatrix, predPreySizesMinMatrix, predPreySizesMaxMatrix;
    int[] nbStagesMatrix;
    float[] recruitAgeMatrix;
    public float[] predationRateMatrix, criticalPredSuccessMatrix;
    public float[] eggSizeMatrix, eggWeightMatrix, sexRatioMatrix, growthAgeThresholdMatrix;
    float[] supAgeOfClass0Matrix;
    public boolean[] reproduceLocallyTab;
    public float[] biomassFluxInTab;
    public float[] meanLengthFishInTab;
    public float[] meanAgeFishInTab;
    public int[] range;
    /*
     * FISHING
     */
    float[] recruitSizeMatrix;
    public float[][] fishingRates;
    /*
     * PLANKTON groups
     */
    public int nbPlanktonGroupsTab;
    public String[] planktonNamesTab;
    /*
     * CONFIG & OPTIONS
     */
    String gridClassNameTab;
    public String gridFileTab, lonFieldTab, latFieldTab, maskFieldTab;
    public int strideTab;
    public int gridLinesTab, gridColumnsTab;
    public float upLeftLatTab, lowRightLatTab, upLeftLongTab, lowRightLongTab;
    public int nYears, nStepYear, savingDtMatrix, nbDtSavePerYear;
    /*
     * Parameters
     */
    public int nbSchools;
    public float[] planktonAccessCoeffMatrix;
    int[] nbAccessStage;
    float[][] accessStageThreshold;
    public float[][][][] accessibilityMatrix;
    /*
     * SPECIES AREAS FILE
     */
    public String areasFileNameTab;	              //choice between "Random" or fileName
    public int[] speciesAreasSizeTab;	    //used only for Qsimulation.iniRepartitionAleat() ie for random distribution
    public int[][][] numMap;        //gives a number of map for[species][cohort][dt]
    public GridMap[] maps;
    public float[] maxProbaPresence;
    boolean densityMaps;
    public ConnectivityMatrix[] connectivityMatrix;
    /*
     * COASTLINE
     */
    String coastFileNameTab;	              //choice between "None" or fileName
    int[] tabCoastiMatrix, tabCoastjMatrix;   //coordinates of the cells representing land
    int nbCellsCoastTab;
    /*
     * MPAs coordinates
     */
    String mpaFileNameTab;
    int[] tabMPAiMatrix, tabMPAjMatrix;     //coord i et j of the matrix delimiting a mpa
    boolean thereIsMPATab;		      //signify that 1 mpa is implemented even if t<tStart
    int MPAtStartTab, MPAtEndTab;	      //start and end of MPA in years
    //tables for output storage by series of simulations
    //4 dimensions : simu, species,val(total OR total-0), step t
    //for mortalities, 3 dim, the last is for the mean on the simulation period
    public float[][][][][] BIOMQuadri;   //[numSimu][species][with or without age 0][t][dt]
    /*
     * INDICATORS OUTPUT
     */
    public boolean TLoutputMatrix, TLDistriboutputMatrix, dietsOutputMatrix, meanSizeOutputMatrix,
            sizeSpectrumOutputMatrix, sizeSpectrumPerSpeOutputMatrix,
            planktonMortalityOutputMatrix, calibrationMatrix, outputClass0Matrix, spatializedOutputs;
    public String planktonBiomassOutputMatrix;
    String dietsConfigFileName, dietOutputMetrics;
    int[] nbDietsStages;
    float[][] dietStageThreshold;
    //Initial abundances
    public String calibrationMethod;
    public double[] spBiomIniTab;
    public double SSslope, SSintercept;
    //size spectrum
    public float spectrumMinSize, spectrumMaxSize, classRange;
    public float[] tabSizes;//size classes of 10cm are used for ini and output
    public float[] tabSizesLn;//for output
    public int nbSizeClass;
    // TL distrib
    public int nbTLClass;
    float minTL;
    float maxTL;
    public float[] tabTL;
    //param allowing to adapt the size of output tables depending on simulation time
    public int timeSeriesLength, timeSeriesStart;
    int startingSavingTimeTab;
    boolean timeSeriesIsShortened;
    // migration
    public float[][] migrationTempMortality;
    public int[][] migrationTempAge, migrationTempDt;
    // distribution
    int[][] areasTempAge;
    int[][] areasTempDt;
    int[] areasNumSpForMap;
    private String[] mapFile;
    private int[] mapIndexNoTwin;
    public SpatialDistribution[] spatialDistribution;

    public void init() {

        readInputFile();	// read the first file containing the file names of all other input files
        readAllInputFiles(0);
        initializeSizeAndTLSpectrum();
    }

    public void loadMPAs() {
        for (int index = 0; index < tabMPAiMatrix.length; index++) {
            getGrid().getCell(tabMPAiMatrix[index], tabMPAjMatrix[index]).setMPA(true);
        }
    }

    public void initializeSizeAndTLSpectrum() {
        if (sizeSpectrumOutputMatrix || sizeSpectrumPerSpeOutputMatrix) {
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

    public void run() {

        // Delete existing output directory
        File targetPath = new File(outputPathName + outputFileNameTab);
        if (targetPath.exists()) {
            IOTools.deleteDirectory(targetPath);
        }
        // Loop over the number of replica
        for (int replica = 0; replica < nbLoopTab; replica++) {
            long begin = System.currentTimeMillis();
            System.out.println();
            System.out.println("Replicate " + replica + "...");
            simulation = new Simulation(replica);
            simulation.init();
            simulation.run();
            int time = (int) ((System.currentTimeMillis() - begin) / 1000);
            System.out.println("Replicate " + replica + " [OK] (time ellapsed:  " + time + " seconds)");
        }
        // Save summary for calibration
        if (calibrationMatrix) {
            saveSerieSimulations();
        }
    }

    public void readAllInputFiles(int numSerie) {
        readConfigurationFile(configFileNameTab, numSerie);
        readSpeciesFile(speciesFileNameTab, numSerie);
        readPredationFile(predationFileNameTab, numSerie);
        readFishingFile(fishingFileNameTab, numSerie);
        readCalibrationFile(calibrationFileNameTab, numSerie);
        readSeasonalityReproFile(reproductionFileNameTab, numSerie);
        readsize0File(size0FileNameTab, numSerie);
        readOutputConfigurationFile(indicatorsFileNameTab, numSerie);
        if (dietsOutputMatrix) {
            readDietsOutputFile(dietsConfigFileName, numSerie);
        }
        readAccessibilitiesFile(accessibilitiesFileNameTab, numSerie);
        readMigrationFile();
        readCoastFile();
        initGrid();
        if (!NEW_AREA_FILE) {
            readAreaFile();
        }
        readMPAFile();
        initializeOutputData();
        initForcing();
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
        FileInputStream inputFile = null;
        try {
            inputFile = new FileInputStream(resolveFile(inputTxtName));
        } catch (FileNotFoundException ex) {
            System.out.println("INPUT file doesn't exist");
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(inputFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
        } catch (IOException ex) {
            System.out.println("  Reading error of INPUT file");
            System.exit(1);
        }

        try {
            st.nextToken();
            nbLoopTab = (new Integer(st.sval)).intValue();
            System.out.println("  Number of replicated simulations = " + nbLoopTab);

            st.nextToken();
            configFileNameTab = st.sval;
            System.out.println("  Configuration file = " + configFileNameTab);

            st.nextToken();
            speciesFileNameTab = st.sval;
            System.out.println("  Species file = " + speciesFileNameTab);

            st.nextToken();
            predationFileNameTab = st.sval;
            System.out.println("  Predation file = " + predationFileNameTab);

            st.nextToken();
            fishingFileNameTab = st.sval;
            System.out.println("  Fishing file = " + fishingFileNameTab);

            st.nextToken();
            areasFileNameTab = st.sval;
            System.out.println("  Areas file = " + areasFileNameTab);

            st.nextToken();
            planktonStructureFileNameTab = st.sval;
            System.out.println("  Plankton groups file = " + planktonStructureFileNameTab);

            st.nextToken();
            planktonFileNameTab = st.sval;
            System.out.println("  Plankton data file = " + planktonFileNameTab);

            st.nextToken();
            calibrationFileNameTab = st.sval;
            System.out.println("  Calibration file = " + calibrationFileNameTab);

            st.nextToken();
            indicatorsFileNameTab = st.sval;
            System.out.println("  Indicators file = " + indicatorsFileNameTab);

            st.nextToken();
            accessibilitiesFileNameTab = st.sval;
            System.out.println("  Accessibilities file = " + accessibilitiesFileNameTab);

            st.nextToken();
            size0FileNameTab = st.sval;
            System.out.println("  Size age-class zero file = " + size0FileNameTab);

            st.nextToken();
            reproductionFileNameTab = st.sval;
            System.out.println("  Reproduction file = " + reproductionFileNameTab);

            st.nextToken();
            fishingSeasonFileNameTab = st.sval;
            System.out.println("  Fishing season file = " + fishingSeasonFileNameTab);

            st.nextToken();
            migrationFileNameTab = st.sval;
            System.out.println("  Migration file = " + migrationFileNameTab);

            st.nextToken();
            coastFileNameTab = st.sval;
            System.out.println("  Coast file = " + coastFileNameTab);

            st.nextToken();
            mpaFileNameTab = st.sval;
            System.out.println("  MPA file = " + mpaFileNameTab);

            st.nextToken();
            couplingFileNameTab = st.sval;
            System.out.println("  Coupling file = " + couplingFileNameTab);

            inputFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of INPUT file");
            System.exit(1);
        }
        System.out.println("EOF for step 2. Reading file INPUT.txt");
    }

    public void readSpeciesFile(String speciesFileName, int numSerie) {
        FileInputStream speciesFile = null;
        try {
            speciesFile = new FileInputStream(resolveFile(speciesFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("Species file " + speciesFileName + " doesn't exist");
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(speciesFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            if (new Integer(st.sval).intValue() == nbSpeciesTab) {
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    nameSpecMatrix[i] = st.sval;
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    DMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    longevityMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    lInfMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    KMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    t0Matrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    cMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    bPowerMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    alphaMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    maturityMetricMatrix[i] = st.sval;
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    if (maturityMetricMatrix[i].equalsIgnoreCase("age")) //conversion from maturity age to maturity size through von Bertalanffy equation
                    {
                        st.nextToken();
                        sizeMatMatrix[i] = lInfMatrix[i] * (float) (1 - Math.exp(-KMatrix[i]
                                * ((new Float(st.sval).floatValue()) - t0Matrix[i])));             //***** to checked if not too big fish (same test than for recruit age)
                    } else {
                        st.nextToken();
                        sizeMatMatrix[i] = (new Float(st.sval)).floatValue();
                    }
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    sexRatioMatrix[i] = (new Float(st.sval)).floatValue();
                }
                /*
                 * phv 2011/11/21 Determine wether reproduction occurs locally
                 * or outside the simulated domain. Then for species reproducing
                 * outside it reads the incoming biomass caracteristics.
                 */
                reproduceLocallyTab = new boolean[nbSpeciesTab];
                biomassFluxInTab = new float[nbSpeciesTab];
                meanLengthFishInTab = new float[nbSpeciesTab];
                meanAgeFishInTab = new float[nbSpeciesTab];
                range = new int[nbSpeciesTab];
                st.nextToken();
                int nbReproOut = 0;
                if (null != st.sval) {
                    if (Character.isDigit(st.sval.charAt(0))) {
                        for (int i = 0; i < nbSpeciesTab; i++) {
                            range[i] = Integer.getInteger(st.sval);
                            st.nextToken();
                        }
                    } else {
                        for (int i = 0; i < nbSpeciesTab; i++) {
                            reproduceLocallyTab[i] = st.sval.matches("in");
                            if (!reproduceLocallyTab[i]) {
                                nbReproOut++;
                            }
                            st.nextToken();
                        }
                        for (int i = 0; i < nbReproOut; i++) {
                            int indexSpecies = new Integer(st.sval).intValue() - 1;
                            st.nextToken();
                            biomassFluxInTab[indexSpecies] = new Float(st.sval).floatValue();
                            st.nextToken();
                            meanLengthFishInTab[indexSpecies] = new Float(st.sval).floatValue();
                            st.nextToken();
                            meanAgeFishInTab[indexSpecies] = new Float(st.sval).floatValue();
                            st.nextToken();
                        }
                        if (null != st.sval) {
                            for (int i = 0; i < nbSpeciesTab; i++) {
                                range[i] = Integer.valueOf(st.sval);
                                st.nextToken();
                            }
                        } else {
                            for (int i = 0; i < nbSpeciesTab; i++) {
                                range[i] = 1;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < nbSpeciesTab; i++) {
                        reproduceLocallyTab[i] = true;
                        range[i] = 1;
                    }
                }
            } else {
                System.out.println("Uncorrect number of species in species file");
                System.exit(1);
            }
            speciesFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of species file");
            System.exit(1);
        }
    }

    private void readReproductionFile(String csvFile) throws IOException {

        CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
        List<String[]> lines = reader.readAll();
        /*
         * Check dimensions of the file
         * We expect nb_columns = nb_species + 1 (1st column for time step)
         * and nb_rows = nb_steps_per_year  + 1 (1st row is the header)
         * This case allows annual variability for reproduction.
         * or nb_rows = nb_steps_per_year * nb_years + 1 (1st row is the header)
         * This case allows interannual variability for reproduction.
         */
        int nb_rows = lines.size() - 1;
        if (nb_rows != nStepYear & nb_rows != nStepYear * nYears) {
            System.out.println("Reproduction seasonality file " + csvFile + " contains " + nb_rows + " rows. Should be either " + nStepYear + " or " + (nStepYear * nYears));
            System.exit(1);
        }
        int nb_columns = lines.get(0).length - 1;
        if (nb_columns != nbSpeciesTab) {
            System.out.println("Wrong number of species in reproduction seasonality file " + csvFile);
            System.exit(1);
        }
        /*
         * Reads the larval mortality rates
         */
        seasonSpawningMatrix = new float[nbSpeciesTab][nb_rows];
        for (int step = 0; step < nb_rows; step++) {
            String[] line = lines.get(step + 1); // skip header on 1st line
            for (int iSpec = 0; iSpec < nbSpeciesTab; iSpec++) {
                seasonSpawningMatrix[iSpec][step] = Float.valueOf(line[iSpec + 1]) / 100.f; // skip 1st column, the time step
            }
        }
        /*
         * Check that sum of % == 100% every year for every species
         */
        for (int iSpec = 0; iSpec < nbSpeciesTab; iSpec++) {
            double sum = 0;
            for (int iStep = 0; iStep < seasonSpawningMatrix[iSpec].length; iStep++) {
                sum += seasonSpawningMatrix[iSpec][iStep];
                if ((iStep + 1) % nStepYear == 0) {
                    if (!((sum > 0.99f) && (sum < 1.01f))) {
                        int year = (iStep + 1) / nStepYear;
                        System.out.println("ERROR: sum of percents does not equal 100% in spawning seasonality file " + csvFile + " for species " + nameSpecMatrix[iSpec] + " in year " + year);
                        System.exit(1);
                    }
                    sum = 0;
                }
            }
        }

    }

    public void readSeasonalityReproFile(String reproductionFileName, int numSerie) {
        if (reproductionFileName.equalsIgnoreCase("default")) {
            for (int i = 0; i < nbSpeciesTab; i++) {
                seasonSpawningMatrix[i] = new float[nStepYear];
                for (int j = 0; j < nStepYear; j++) {
                    seasonSpawningMatrix[i][j] = (float) 1 / (float) nStepYear;
                }
            }
            System.out.println("Reproduction is set constant over the year (default)");
        } else if (reproductionFileName.endsWith(".csv")) {
            try {
                readReproductionFile(resolveFile(reproductionFileName));
            } catch (IOException ex) {
                Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            FileInputStream reproductionFile = null;
            try {
                reproductionFile = new FileInputStream(resolveFile(reproductionFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("reproduction file doesn't exist: " + reproductionFileName);
                System.exit(1);
            }

            Reader r = new BufferedReader(new InputStreamReader(reproductionFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            float tempSum;

            try {
                st.nextToken();
                if (new Integer(st.sval).intValue() == nStepYear) {
                    for (int i = 0; i < nbSpeciesTab; i++) {
                        tempSum = 0;
                        seasonSpawningMatrix[i] = new float[nStepYear];
                        for (int j = 0; j < nStepYear; j++) {
                            st.nextToken();
                            seasonSpawningMatrix[i][j] = (new Float(st.sval)).floatValue() / 100; //percentage
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
                System.exit(1);
            }
        }
    }

    private void readFishingRates(String csvFile) {

        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            /*
             * Check dimensions of the file
             * We expect nb_columns = nb_species + 1 (1st column for time step)
             * and nb_rows = nb_steps_per_year  + 1 (1st row is the header)
             * This case does just the same as the previous combination of
             * one fishing rate per species + fishing seasonality
             * or nb_rows = nb_steps_per_year * nb_years + 1
             * This case is new and allows interannual variability for fishing
             * rate.
             */
            int nb_rows = lines.size() - 1;
            if (nb_rows != nStepYear & nb_rows != nStepYear * nYears) {
                System.out.println("Fishing rate file " + csvFile + " contains " + nb_rows + " rows. Should be either " + nStepYear + " or " + (nStepYear * nYears));
                System.exit(1);
            }
            int nb_columns = lines.get(0).length - 1;
            if (nb_columns != nbSpeciesTab) {
                System.out.println("Wrong number of species in fishing rate file " + csvFile);
                System.exit(1);
            }
            /*
             * Reads the fishing rates
             */
            fishingRates = new float[nbSpeciesTab][nb_rows];
            for (int step = 0; step < nb_rows; step++) {
                String[] line = lines.get(step + 1); // skip header on 1st line
                for (int iSpec = 0; iSpec < nbSpeciesTab; iSpec++) {
                    fishingRates[iSpec][step] = Float.valueOf(line[iSpec + 1]); // skip 1st column, the time step
                }
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void readFishingFile(String fishingFileName, int numSerie) {

        FileInputStream fishingFile = null;
        try {
            fishingFile = new FileInputStream(resolveFile(fishingFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("fishing file doesn't exist: " + fishingFileName);
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(fishingFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            String fishingRateFile = st.sval;
            readFishingRates(resolveFile(fishingRateFile));

            st.nextToken();
            recruitMetricMatrix = st.sval;

            if (recruitMetricMatrix.equalsIgnoreCase("age")) {
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    recruitAgeMatrix[i] = (new Float(st.sval)).floatValue();
                }
            } else {
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    recruitSizeMatrix[i] = (new Float(st.sval)).floatValue();
                    if (recruitSizeMatrix[i] < lInfMatrix[i]) {
                        recruitAgeMatrix[i] = (float) (-((Math.log(1 - (recruitSizeMatrix[i] / lInfMatrix[i]))) / KMatrix[i])) + t0Matrix[i];
                    } else {
                        recruitAgeMatrix[i] = longevityMatrix[i] + 1;
                    }
                    if (recruitAgeMatrix[i] < 0.6)//due to inverse von Bert transformation
                    {
                        recruitAgeMatrix[i] = (float) 0.6; // >0.5 to avoid Math.round() problems
                    }
                    if (recruitAgeMatrix[i] > longevityMatrix[i]) {
                        recruitAgeMatrix[i] = longevityMatrix[i] + 1;
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Reading error of fishing seasonality file");
        }
    }

    private void readLarvalMortalityRates(String csvFile) throws IOException {

        CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
        List<String[]> lines = reader.readAll();
        /*
         * Check dimensions of the file
         * We expect nb_columns = nb_species + 1 (1st column for time step)
         * and nb_rows = 2 (1st row is the header) for one larval mortality
         * rate per species.
         * or nb_rows = nb_steps_per_year  + 1 (1st row is the header)
         * This case allows annual variability for larval mortality rate.
         * or nb_rows = nb_steps_per_year * nb_years + 1 (1st row is the header)
         * This case allows interannual variability for larval mortality rate.
         */
        int nb_rows = lines.size() - 1;
        if (nb_rows != 1 & nb_rows != nStepYear & nb_rows != nStepYear * nYears) {
            System.out.println("Larval mortality rates file " + csvFile + " contains " + nb_rows + " rows. Should be either 1 or " + nStepYear + " or " + (nStepYear * nYears));
            System.exit(1);
        }
        int nb_columns = lines.get(0).length - 1;
        if (nb_columns != nbSpeciesTab) {
            System.out.println("Wrong number of species in larval mortality rates file " + csvFile);
            System.exit(1);
        }
        /*
         * Reads the larval mortality rates
         */
        larvalMortalityRates = new float[nbSpeciesTab][nb_rows];
        for (int step = 0; step < nb_rows; step++) {
            String[] line = lines.get(step + 1); // skip header on 1st line
            for (int iSpec = 0; iSpec < nbSpeciesTab; iSpec++) {
                larvalMortalityRates[iSpec][step] = Float.valueOf(line[iSpec + 1]); // skip 1st column, the time step
            }
        }
    }

    public void readCalibrationFile(String calibrationFileName, int numSerie) {
        FileInputStream calibFile = null;
        try {
            calibFile = new FileInputStream(resolveFile(calibrationFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("Calibration file doesn't exist: " + calibrationFileName);
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(calibFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            calibrationMethod = st.sval;
            if (calibrationMethod.equalsIgnoreCase("biomass")) {
                spBiomIniTab = new double[nbSpeciesTab];
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    spBiomIniTab[i] = (new Double(st.sval)).doubleValue();
                }
            } else if (calibrationMethod.equalsIgnoreCase("spectrum")) {
                st.nextToken();
                SSslope = (new Double(st.sval)).doubleValue();
                st.nextToken();
                SSintercept = (new Double(st.sval)).doubleValue();
            } else if (calibrationMethod.equalsIgnoreCase("random")) //*****************TO DETAIL****************************
            {
                System.out.println("The option initialisation per random method is not implemented yet");
            }

            supAgeOfClass0Matrix = new float[nbSpeciesTab];

            st.nextToken();
            /*
             * phv 2012/11/09
             * Check whether following value is a Float
             * YES: reads list a larval mortality rates directly in the
             * calibration file, one per species
             * NO: reads larval mortality rates from CSV file
             */
            try {
                Float.valueOf(st.sval);
                larvalMortalityRates = new float[nbSpeciesTab][1];
                for (int i = 0; i < nbSpeciesTab; i++) {
                    larvalMortalityRates[i][0] = (new Float(st.sval)).floatValue();
                    st.nextToken();
                }
            } catch (NumberFormatException ex) {
                String larvalMortalityFile = st.sval;
                readLarvalMortalityRates(resolveFile(larvalMortalityFile));
                st.nextToken();
            }

            for (int i = 0; i < nbSpeciesTab; i++) {
                supAgeOfClass0Matrix[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
            }
            for (int i = 0; i < nbPlanktonGroupsTab; i++) {
                planktonAccessCoeffMatrix[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
            }
            calibFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of calibration file");
            System.exit(1);
        }
    }

    public void readConfigurationFile(String configFileName, int numSerie) {
        FileInputStream configFile = null;
        try {
            configFile = new FileInputStream(resolveFile(configFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("configuration file doesn't exist: " + configFileName);
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(configFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            gridLinesTab = (new Integer(st.sval)).intValue();
            st.nextToken();
            gridColumnsTab = (new Integer(st.sval)).intValue();
            st.nextToken();
            upLeftLatTab = new Float(st.sval).floatValue();
            st.nextToken();
            lowRightLatTab = new Float(st.sval).floatValue();
            st.nextToken();
            upLeftLongTab = new Float(st.sval).floatValue();
            st.nextToken();
            lowRightLongTab = new Float(st.sval).floatValue();
            st.nextToken();
            nYears = (new Integer(st.sval)).intValue();
            st.nextToken();
            nStepYear = (new Integer(st.sval)).intValue();
            st.nextToken();
            savingDtMatrix = (new Integer(st.sval)).intValue();
            if (!((nStepYear % savingDtMatrix) == 0)) {
                System.out.println("The number of time steps per year is not a multiple of the number of time steps for saving");
                System.out.println("Thus, saving is realized at each time step");
                savingDtMatrix = 1;
            }
            nbDtSavePerYear = (int) nStepYear / savingDtMatrix;
            st.nextToken();
            startingSavingTimeTab = (new Integer(st.sval)).intValue();
            st.nextToken();
            nbSpeciesTab = (new Integer(st.sval)).intValue();
            st.nextToken();
            nbPlanktonGroupsTab = (new Integer(st.sval)).intValue();

            st.nextToken();
            if ((st.sval).equalsIgnoreCase("forcing")) {
                isForcing = true;
            } else if ((st.sval).equalsIgnoreCase("coupling")) {
                isForcing = false;
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
            lowTLClassNameTab = st.sval;

            st.nextToken();
            nbSchools = 1 + Math.round(((new Integer(st.sval)).intValue()) / nStepYear);

            /*
             * Additional parameters to read the grid from NetCDF file
             */
            st.nextToken();
            if (null != st.sval) {
                gridClassNameTab = st.sval;
                st.nextToken();
                String filename = resolveFile(st.sval).toString();
                gridFileTab = filename;
                st.nextToken();
                lonFieldTab = st.sval;
                st.nextToken();
                latFieldTab = st.sval;
                st.nextToken();
                maskFieldTab = st.sval;
                st.nextToken();
                strideTab = Integer.valueOf(st.sval);
            } else {
                gridClassNameTab = OriginalGrid.class.getCanonicalName();
            }

            configFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of configuration file");
            System.exit(1);
        }
        initializeNbLivingGroups(numSerie, nbSpeciesTab, nbPlanktonGroupsTab);
    }

    public void initializeNbLivingGroups(int numSerie, int nbSpeciesExplicit, int nbOtherFood) {

        // setting tables length
        //----OPTION file------
        eggSizeMatrix = new float[nbSpeciesExplicit];
        eggWeightMatrix = new float[nbSpeciesExplicit];
        growthAgeThresholdMatrix = new float[nbSpeciesExplicit];
        predationRateMatrix = new float[nbSpeciesExplicit];
        predPreySizesMaxMatrix = new float[nbSpeciesExplicit][];
        predPreySizesMinMatrix = new float[nbSpeciesExplicit][];
        criticalPredSuccessMatrix = new float[nbSpeciesExplicit];
        starvMaxRateMatrix = new float[nbSpeciesExplicit];
        //----SPECIES file------
        nameSpecMatrix = new String[nbSpeciesExplicit];
        DMatrix = new float[nbSpeciesExplicit];
        longevityMatrix = new float[nbSpeciesExplicit];
        lInfMatrix = new float[nbSpeciesExplicit];
        KMatrix = new float[nbSpeciesExplicit];
        t0Matrix = new float[nbSpeciesExplicit];
        cMatrix = new float[nbSpeciesExplicit];
        bPowerMatrix = new float[nbSpeciesExplicit];
        alphaMatrix = new float[nbSpeciesExplicit];
        maturityMetricMatrix = new String[nbSpeciesExplicit];
        sizeMatMatrix = new float[nbSpeciesExplicit];
        sexRatioMatrix = new float[nbSpeciesExplicit];
        sizeFeedingMatrix = new float[nbSpeciesExplicit][];
        nbStagesMatrix = new int[nbSpeciesExplicit];
        //----REPRODUCTION file------
        seasonSpawningMatrix = new float[nbSpeciesExplicit][];
        //----FISHING file------
        recruitSizeMatrix = new float[nbSpeciesExplicit];
        recruitAgeMatrix = new float[nbSpeciesExplicit];
        //--- ACCESSIBILITIES----
        planktonAccessCoeffMatrix = new float[nbOtherFood];
        nbAccessStage = new int[nbSpeciesExplicit];
        accessStageThreshold = new float[nbSpeciesExplicit][];
        accessibilityMatrix = new float[nbSpeciesExplicit + nbOtherFood][][][];

        planktonNamesTab = new String[nbOtherFood];
    }

    public void readsize0File(String optionFileName, int numSerie) {
        if (optionFileName.equalsIgnoreCase("default")) {
            for (int i = 0; i < nbSpeciesTab; i++) {
                eggSizeMatrix[i] = 0.1f;	// by default : egg diameter set to 1mm
                eggWeightMatrix[i] = 0.0005386f;	// weight of egg by default, considering a sphere with the water density
                growthAgeThresholdMatrix[i] = 1.0f;	// by default, von Bertalanffy model considered valid after 1 year old, linear growth from 0 to 1 year
            }
        } else {
            FileInputStream optionFile = null;
            try {
                optionFile = new FileInputStream(resolveFile(optionFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("option file doesn't exist: " + optionFileName);
                System.exit(1);
            }

            Reader r = new BufferedReader(new InputStreamReader(optionFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            try {
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    eggSizeMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    eggWeightMatrix[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nbSpeciesTab; i++) {
                    st.nextToken();
                    growthAgeThresholdMatrix[i] = (new Float(st.sval)).floatValue();
                }

                optionFile.close();
            } catch (IOException ex) {
                System.out.println("Reading error of option file");
                System.exit(1);
            }
        }
    }

    public void readPredationFile(String predationFileName, int numSerie) {
        FileInputStream predationFile = null;
        try {
            predationFile = new FileInputStream(resolveFile(predationFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("predation file doesn't exist: " + predationFileName);
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(predationFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            for (int i = 0; i < nbSpeciesTab; i++) {
                st.nextToken();
                predationRateMatrix[i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbSpeciesTab; i++) {
                st.nextToken();
                criticalPredSuccessMatrix[i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbSpeciesTab; i++) {
                st.nextToken();
                starvMaxRateMatrix[i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nbSpeciesTab; i++) {
                st.nextToken();
                nbStagesMatrix[i] = (new Integer(st.sval)).intValue();
                sizeFeedingMatrix[i] = new float[nbStagesMatrix[i] - 1];
                predPreySizesMinMatrix[i] = new float[nbStagesMatrix[i]];
                predPreySizesMaxMatrix[i] = new float[nbStagesMatrix[i]];
                for (int s = 0; s < nbStagesMatrix[i] - 1; s++) {
                    st.nextToken();
                    sizeFeedingMatrix[i][s] = (new Float(st.sval)).floatValue();
                }
                for (int s = 0; s < nbStagesMatrix[i]; s++) {
                    st.nextToken();
                    predPreySizesMinMatrix[i][s] = (new Float(st.sval)).floatValue();
                    st.nextToken();
                    predPreySizesMaxMatrix[i][s] = (new Float(st.sval)).floatValue();
                }
            }
            predationFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of predation file");
            System.exit(1);
        }
    }

    public void readOutputConfigurationFile(String indicatorsFileName, int numSerie) {
        FileInputStream indicFile = null;
        try {
            indicFile = new FileInputStream(resolveFile(indicatorsFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("output config file doesn't exist: " + indicatorsFileName);
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(indicFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {

            st.nextToken();
            outputFileNameTab = st.sval;
            String[] split = st.sval.split("/");
            outputPrefix = split[split.length - 1];

            st.nextToken();
            calibrationMatrix = false;
            if (st.sval.equalsIgnoreCase("calibration")) {
                calibrationMatrix = true;
            }

            st.nextToken();
            TLoutputMatrix = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            TLDistriboutputMatrix = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            dietsOutputMatrix = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            dietsConfigFileName = st.sval;
            st.nextToken();
            meanSizeOutputMatrix = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            sizeSpectrumOutputMatrix = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            sizeSpectrumPerSpeOutputMatrix = (Boolean.valueOf(st.sval)).booleanValue();
            if (sizeSpectrumOutputMatrix || sizeSpectrumPerSpeOutputMatrix) {
                st.nextToken();
                spectrumMinSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                spectrumMaxSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                classRange = (new Float(st.sval)).floatValue();
            }
            st.nextToken();
            planktonMortalityOutputMatrix = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            outputClass0Matrix = (Boolean.valueOf(st.sval)).booleanValue();
            try {
                /*
                 * phv 2011/06/30 Read additional parameters "spatialized
                 * outputs" Since it might not exist in most configurations I
                 * catch any exception and set it as false by default.
                 */
                st.nextToken();
                spatializedOutputs = (Boolean.valueOf(st.sval)).booleanValue();
            } catch (Exception ex) {
                spatializedOutputs = false;
            }
            try {
                /*
                 * phv 2012/05/29 Read additional parameters "plankton biomass"
                 * Since it might not exist in most configurations I catch any
                 * exception and set it as false by default.
                 * phv 2012/11/08 Parameter can be either 'csv' or 'netcdf'
                 * to choose the format.
                 */
                st.nextToken();
                planktonBiomassOutputMatrix = st.sval;
            } catch (Exception ex) {
                planktonBiomassOutputMatrix = "";
            }
            if (!planktonBiomassOutputMatrix.matches("csv") & !planktonBiomassOutputMatrix.matches("netcdf")) {
                planktonBiomassOutputMatrix = "";
            }

            indicFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of output config file");
            System.exit(1);
        }
    }

    public void initGrid() {
        try {
            System.out.println("Initialize grid: " + gridClassNameTab);
            grid = (IGrid) Class.forName(gridClassNameTab).newInstance();

        } catch (InstantiationException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Init the grid
        grid.init();
        // Apply mask
        if (null != tabCoastiMatrix) {
            for (int k = 0; k < tabCoastiMatrix.length; k++) {
                grid.getCell(tabCoastiMatrix[k], tabCoastjMatrix[k]).setLand(true);
            }
        }
    }

    public void initForcing() {
        try {
            forcing = (LTLForcing) Class.forName(getLTLClassName()).newInstance();
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        getForcing().init();
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
            nbCellsCoastTab = nbCells;

            /*
             * Identify the coordinates
             */
            int indexCell = 0;
            tabCoastiMatrix = new int[nbCells];
            tabCoastjMatrix = new int[nbCells];
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val <= 0.f) {
                        tabCoastiMatrix[indexCell] = i;
                        tabCoastjMatrix[indexCell] = j;
                        indexCell++;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void readCoastCoordinates(String coastFilename) {
        FileInputStream coastFile = null;

        try {
            coastFile = new FileInputStream(coastFilename);
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening coastFile");
            System.exit(1);
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
            if (new Integer(st.sval).intValue() == gridLinesTab) {
                st.nextToken();
                if (new Integer(st.sval).intValue() == gridColumnsTab) {

                    st.nextToken();
                    tabCoastiMatrix = new int[new Integer(st.sval).intValue()];
                    tabCoastjMatrix = new int[new Integer(st.sval).intValue()];

                    nbCellsCoastTab = new Integer(st.sval).intValue();

                    for (int i = 0; i < tabCoastiMatrix.length; i++) {

                        st.nextToken();
                        tabCoastiMatrix[i] = (new Integer(st.sval).intValue());
                    }

                    for (int i = 0; i < tabCoastjMatrix.length; i++) {

                        st.nextToken();
                        tabCoastjMatrix[i] = (new Integer(st.sval).intValue());
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

    public void readCoastFile() {
        if (coastFileNameTab.equalsIgnoreCase("default") || coastFileNameTab.equalsIgnoreCase("none")) {
            System.out.println("No coast in the grid (default)");
            nbCellsCoastTab = 0;
        } else {

            String filename = resolveFile(coastFileNameTab);
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

    public void readMPAFile() {
        if (mpaFileNameTab.equalsIgnoreCase("default")) {
            thereIsMPATab = false;
            tabMPAiMatrix = new int[0];
            tabMPAjMatrix = new int[0];
            MPAtStartTab = 0;
            MPAtEndTab = 0;
        } else {
            //read info in file mpa
            FileInputStream mpaFile = null;
            int[] tabi, tabj;
            try {
                mpaFile = new FileInputStream(resolveFile(mpaFileNameTab));
            } catch (FileNotFoundException ex) {
                System.out.println("Error while opening mpaFile");
                System.exit(1);
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
                if (new Integer(st.sval).intValue() == gridLinesTab) {
                    st.nextToken();
                    if (new Integer(st.sval).intValue() == gridColumnsTab) {
                        st.nextToken();
                        tabi = new int[new Integer(st.sval).intValue()];
                        tabj = new int[new Integer(st.sval).intValue()];
                        st.nextToken();
                        MPAtStartTab = new Integer(st.sval).intValue();
                        st.nextToken();
                        MPAtEndTab = new Integer(st.sval).intValue();
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

                            tabMPAiMatrix = new int[tabi.length];
                            tabMPAjMatrix = new int[tabj.length];
                            for (int i = 0; i < tabi.length; i++) {
                                tabMPAiMatrix[i] = tabi[i];
                                tabMPAjMatrix[i] = tabj[i];
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
                System.exit(1);
            }
            thereIsMPATab = true;
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

    public void readAreaFile() {

        /*
         * Open areas-maps configuration file
         */
        FileInputStream areasFile = null;
        try {
            areasFile = new FileInputStream(resolveFile(areasFileNameTab));
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening areasFile");
            System.exit(1);
        }
        /*
         * Initialize the reader
         */
        Reader r = new BufferedReader(new InputStreamReader(areasFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');
        spatialDistribution = new SpatialDistribution[nbSpeciesTab];
        speciesAreasSizeTab = new int[nbSpeciesTab];
        try {

            for (int i = 0; i < nbSpeciesTab; i++) {
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
            maps = new GridMap[nbMaps];
            connectivityMatrix = new ConnectivityMatrix[nbMaps];
            maxProbaPresence = new float[nbMaps];
            areasNumSpForMap = new int[nbMaps];
            mapFile = new String[nbMaps];
            mapIndexNoTwin = new int[nbMaps];
            areasTempAge = new int[nbMaps][];
            areasTempDt = new int[nbMaps][];
            numMap = new int[nbSpeciesTab][][];
            for (int iSpec = 0; iSpec < nbSpeciesTab; iSpec++) {
                int longevity = (int) Math.round((longevityMatrix[iSpec]) * nStepYear);
                numMap[iSpec] = new int[longevity][];
                for (int j = 0; j < longevity; j++) {
                    numMap[iSpec][j] = new int[nStepYear];
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
                        speciesAreasSizeTab[iSpec] = (new Integer(st.sval)).intValue();
                        //distribRandom(iSpec);
                        break;
                    case MAPS:
                        readAreaCSV(st, iSpec, indexMap);
                        break;
                    case CONNECTIVITY:
                        readConnectivity(st, iSpec, indexMap);
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

        eliminateTwinMap();
    }

    /**
     * This function eliminates twins in the list of maps of distribution
     */
    private void eliminateTwinMap() {
        mapIndexNoTwin = new int[mapFile.length];
        for (int k = 0; k < mapFile.length; k++) {
            String file = mapFile[k];
            mapIndexNoTwin[k] = k;
            for (int l = k - 1; l >= 0; l--) {
                if (file.matches(mapFile[l])) {
                    mapIndexNoTwin[k] = mapIndexNoTwin[l];
                    // Delete twin maps
                    maps[k] = null;
                    connectivityMatrix[k] = null;
                    break;
                }
            }
            //System.out.println("Map " + k + " has index " + mapIndexNoTwin[k] + " " + mapFile[k]);
        }

        for (int iSpec = 0; iSpec < numMap.length; iSpec++) {
            for (int iAge = 0; iAge < numMap[iSpec].length; iAge++) {
                for (int iStep = 0; iStep < numMap[iSpec][iAge].length; iStep++) {
                    int indexMap = numMap[iSpec][iAge][iStep];
                    numMap[iSpec][iAge][iStep] = mapIndexNoTwin[indexMap];
                }
            }
        }
    }

    private void readConnectivity(StreamTokenizer st, int iSpec, int indexMap) throws IOException {

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
         * read the name of the CSV file and load the map if name = "null"
         * it means the species is out of the simulated domain at these
         * age-class and time-step
         */
        st.nextToken();
        if (!"null".equals(st.sval)) {
            String csvFile = resolveFile(st.sval);
            mapFile[indexMap] = csvFile;
            readCSVMap(csvFile, indexMap);
            System.out.println("Loaded map " + indexMap + " " + csvFile);
        }
        /*
         * Read the name of the connectivity file and load the matrix If
         * name = "null" it means the species is out of the simulated domain
         * at these age-class and time-step or the map is not connected to any
         * other one so there is no need for a connectivity matrix
         */
        st.nextToken();
        if (!"null".equals(st.sval)) {
            System.out.println("Reading connectivity matric for " + nameSpecMatrix[iSpec] + " map " + indexMap);
            String csvFile = resolveFile(st.sval);
            connectivityMatrix[indexMap] = new ConnectivityMatrix(indexMap, csvFile);
            System.out.println("Connectivity matrix loaded");
        }
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
            mapFile[indexMap] = st.sval;
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
            tabMPAiMatrix = new int[nbCells];
            tabMPAjMatrix = new int[nbCells];
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val > 0.f) {
                        tabMPAiMatrix[indexCell] = i;
                        tabMPAjMatrix[indexCell] = j;
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
            maps[indexMap] = new GridMap();
            /*
             * Set the numero of maps per species, age class and time step
             */
            for (int m = 0; m < areasTempAge[indexMap].length; m++) {
                for (int n = 0; n < areasTempDt[indexMap].length; n++) {
                    for (int h = 0; h < nStepYear; h++) {
                        int longevity = (int) Math.round((longevityMatrix[areasNumSpForMap[indexMap]]) * nStepYear);
                        if ((areasTempAge[indexMap][m] * nStepYear + h) < longevity) {
                            numMap[areasNumSpForMap[indexMap]][areasTempAge[indexMap][m] * nStepYear + h][areasTempDt[indexMap][n]] = indexMap;
                            //System.out.println("NumMap: " + areasNumSpForMap[indexMap] + " " + (areasTempAge[indexMap][m] * nbDtMatrix + h) + " " + (areasTempDt[indexMap][n]) + " " + indexMap);
                        }
//                        if (nbCells == 0) {
//                            if (!simulation.getSpecies(areasNumSpForMap[indexMap]).isOut((areasTempAge[indexMap][m] * nbDtMatrix) + h, areasTempDt[indexMap][n])) {
//                                System.out.println("Match error between species areas and migration file for " + nameSpecMatrix[areasNumSpForMap[indexMap]]);
//                            }
//                        }
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
                        if (val < 1.f) {
                            /*
                             * value provided is directly a probability
                             */
                            maps[indexMap].setValue(i, j, val);
                        } else if (val == 1.f) {
                            /*
                             * map is presence/absence so equal probability of
                             * presence among cells
                             */
                            maps[indexMap].setValue(i, j, invNbCells);
                            /*
                             * else mapProbaPresence[indexMap][indexCell] = 0
                             * default value at initialization of the array
                             */
                        }
                        indexCell++;
                    }
                }
            }
            maxProbaPresence[indexMap] = getMaxProbaPresence(indexMap);
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("Read CSV file " + csvFile + " [OK]");
    }

    public GridMap getMap(int numMap) {
        return maps[numMap];
    }

    private float getMaxProbaPresence(int numMap) {
        float tempMaxProbaPresence = 0;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                tempMaxProbaPresence = Math.max(tempMaxProbaPresence, maps[numMap].getValue(i, j));
            }
        }
        return tempMaxProbaPresence;
    }

    public String getMapDetails(int numMap) {
        StringBuilder str = new StringBuilder();
        str.append("Map: ");
        str.append(numMap);
        str.append(" - Species: ");
        str.append(nameSpecMatrix[areasNumSpForMap[numMap]]);
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

    public void readMigrationFile() {
        if (migrationFileNameTab.equalsIgnoreCase("default")) {
            System.out.println("No migrations implemented (default)");
        } else {
            /*
             * Structure of file seasonalityFile (IN CAPITAL default options)
             * ;nb species migrating; ;num of species ;number of ages; ;number
             * of time steps ;ages ;time steps ;mortality ... * nb species
             * migrating
             */

            FileInputStream outOfZoneFile = null;
            try {
                outOfZoneFile = new FileInputStream(resolveFile(migrationFileNameTab));
            } catch (FileNotFoundException ex) {
                System.out.println("migration file doesn't exist: " + migrationFileNameTab);
                System.exit(1);
            }

            Reader r = new BufferedReader(new InputStreamReader(outOfZoneFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            int nbSpOutOfZone, iSpec;
            int nbAgePerCase, nbDtPerCase;
            migrationTempAge = new int[nbSpeciesTab][];
            migrationTempDt = new int[nbSpeciesTab][];
            migrationTempMortality = new float[nbSpeciesTab][];

            try {
                st.nextToken();
                nbSpOutOfZone = (new Integer(st.sval)).intValue();
                if (nbSpOutOfZone == 0) {
                    System.out.println("no species out of zone to be initialized");
                } else {
                    for (int i = 0; i < nbSpOutOfZone; i++) {
                        st.nextToken();
                        iSpec = (new Integer(st.sval)).intValue() - 1;
                        st.nextToken();
                        nbAgePerCase = new Integer(st.sval).intValue();
                        st.nextToken();
                        nbDtPerCase = new Integer(st.sval).intValue();
                        migrationTempAge[iSpec] = new int[nbAgePerCase];
                        migrationTempDt[iSpec] = new int[nbDtPerCase];
                        migrationTempMortality[iSpec] = new float[nbAgePerCase];

                        for (int k = 0; k < nbAgePerCase; k++) {
                            st.nextToken();
                            migrationTempAge[iSpec][k] = new Integer(st.sval).intValue();
                        }
                        for (int k = 0; k < nbDtPerCase; k++) {
                            st.nextToken();
                            migrationTempDt[iSpec][k] = new Integer(st.sval).intValue() - 1;
                        }

                        for (int m = 0; m < nbAgePerCase; m++) {
                            st.nextToken();
                            migrationTempMortality[iSpec][m] = (new Float(st.sval)).floatValue();
                        }
                    }
                }
                outOfZoneFile.close();
            } catch (IOException ex) {
                System.out.println("Reading error of out of zone species file");
                System.exit(1);
            }
        }
    }

    public void readAccessibilitiesFile(String accessFileName, int numSerie) {
        if (accessFileName.equalsIgnoreCase("default")) // if no accessiblity file is specified, all accessibilities are set to 1
        {
            for (int i = 0; i < nbSpeciesTab; i++) {
                nbAccessStage[i] = 1;
                accessStageThreshold[i] = new float[nbAccessStage[i] - 1];
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nbSpeciesTab][];
                for (int j = 0; j < nbSpeciesTab; j++) {
                    accessibilityMatrix[i][0][j] = new float[1];
                    /*
                     * phv 2011/11/18 set default access matrix to 0.8 for being
                     * compatible with Osmose 2.0
                     */
                    accessibilityMatrix[i][0][j][0] = 0.8f;
                }
            }
            for (int i = nbSpeciesTab; i < nbSpeciesTab + nbPlanktonGroupsTab; i++) {
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nbSpeciesTab][];
                for (int j = 0; j < nbSpeciesTab; j++) {
                    accessibilityMatrix[i][0][j] = new float[1];
                    accessibilityMatrix[i][0][j][0] = 0.8f;
                }
            }
        } else // in case of an accessibility file specified
        {
            FileInputStream accessFile = null;
            try {
                accessFile = new FileInputStream(resolveFile(accessFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("accessibility file doesn't exist: " + accessFileName);
                System.exit(1);
            }

            Reader r = new BufferedReader(new InputStreamReader(accessFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');
            try {
                st.nextToken();
                if (!((new Integer(st.sval)).intValue() == nbSpeciesTab)) {
                    System.out.println("nb of species in accessibilities file not valid");
                } else {
                    st.nextToken();
                    if (!((new Integer(st.sval)).intValue() == nbPlanktonGroupsTab)) {
                        System.out.println("nb of other food groups in accessibilities file not valid");
                    } else {
                        for (int i = 0; i < nbSpeciesTab; i++) {
                            st.nextToken();
                            nbAccessStage[i] = (new Integer(st.sval)).intValue();
                            accessStageThreshold[i] = new float[nbAccessStage[i] - 1];
                            for (int j = 0; j < nbAccessStage[i] - 1; j++) {
                                st.nextToken();
                                accessStageThreshold[i][j] = (new Float(st.sval)).floatValue();
                            }
                        }
                        for (int i = 0; i < nbSpeciesTab; i++) {
                            accessibilityMatrix[i] = new float[nbAccessStage[i]][][];
                            for (int j = 0; j < nbAccessStage[i]; j++) {
                                accessibilityMatrix[i][j] = new float[nbSpeciesTab][];
                                for (int k = 0; k < nbSpeciesTab; k++) {
                                    accessibilityMatrix[i][j][k] = new float[nbAccessStage[k]];
                                    for (int m = 0; m < nbAccessStage[k]; m++) {
                                        st.nextToken();
                                        accessibilityMatrix[i][j][k][m] = (new Float(st.sval)).floatValue();
                                    }
                                }
                            }
                        }
                        for (int i = nbSpeciesTab; i < nbSpeciesTab + nbPlanktonGroupsTab; i++) {
                            accessibilityMatrix[i] = new float[1][][];
                            accessibilityMatrix[i][0] = new float[nbSpeciesTab][];
                            for (int k = 0; k < nbSpeciesTab; k++) {
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
                System.exit(1);
            }
        }
    }

    public void initializeOutputData() {
        //these param are function of the length of the temporal series saved
        if (startingSavingTimeTab > nYears) {
            System.out.println("Starting time for saving higher than simulation time -> set to 0");
            startingSavingTimeTab = 0;
        }

        if (startingSavingTimeTab == 0) {
            timeSeriesLength = nYears;
            timeSeriesStart = 0;
            timeSeriesIsShortened = false;
        } else {
            timeSeriesStart = startingSavingTimeTab;
            timeSeriesLength = nYears - startingSavingTimeTab;
            timeSeriesIsShortened = true;
        }

        if (calibrationMatrix) {
            BIOMQuadri = new float[nbLoopTab][][][][];
        }

        for (int xx = 0; xx < nbLoopTab; xx++) {
            if (calibrationMatrix) {
                BIOMQuadri[xx] = new float[nbSpeciesTab + nbPlanktonGroupsTab][][][];
            }

            /*
             * iniBiomass[xx] = new float[nbSpeciesTab]; for(int
             * i=0;i<nbSpeciesTab;i++) iniBiomass[xx][i] = 0; for(int
             * i=0;i<nbSpeciesTab;i++) for(int
             * j=simulation.getSpecies(i).indexAgeClass0;j<simulation.getSpecies(i).nbCohorts;j++)
             * iniBiomass[xx][i] += (float)
             * simulation.getSpecies(i).tabCohorts[j].biomass;
             */
            int tempIndex = (int) nStepYear / savingDtMatrix;

            if (calibrationMatrix) {
                for (int i = 0; i < nbSpeciesTab; i++) {
                    BIOMQuadri[xx][i] = new float[2][][];
                    BIOMQuadri[xx][i][0] = new float[timeSeriesLength][];   //without age 0
                    BIOMQuadri[xx][i][1] = new float[timeSeriesLength][];   // with age 0

                    for (int tt = 0; tt < timeSeriesLength; tt++) {
                        BIOMQuadri[xx][i][0][tt] = new float[tempIndex];
                        BIOMQuadri[xx][i][1][tt] = new float[tempIndex];
                    }
                }

                for (int i = nbSpeciesTab; i < nbSpeciesTab + nbPlanktonGroupsTab; i++) {
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
        nbDietsStages = new int[nbSpeciesTab];
        dietStageThreshold = new float[nbSpeciesTab][];
        if (dietsConfigFileName.equalsIgnoreCase("byDefault")) {
            System.out.println("Diets output by default");
            dietOutputMetrics = "default";
            for (int i = 0; i < nbSpeciesTab; i++) {
                nbDietsStages[i] = 1;
                dietStageThreshold[i] = new float[0];
            }
        } else {
            FileInputStream dietConfigFile = null;
            try {
                dietConfigFile = new FileInputStream(resolveFile(dietsConfigFileName));
            } catch (FileNotFoundException ex) {
                System.out.println("diet configuration file doesn't exist: " + dietsConfigFileName);
                System.exit(1);
            }

            Reader r = new BufferedReader(new InputStreamReader(dietConfigFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            try {
                st.nextToken();
                if ((new Integer(st.sval)).intValue() == nbSpeciesTab) {
                    st.nextToken();
                    if ((new Integer(st.sval)).intValue() == nbPlanktonGroupsTab) {
                        st.nextToken();
                        if (st.sval.equalsIgnoreCase("age") || st.sval.equalsIgnoreCase("size")) {
                            dietOutputMetrics = st.sval;
                            for (int i = 0; i < nbSpeciesTab; i++) {
                                st.nextToken();
                                nbDietsStages[i] = (new Integer(st.sval)).intValue();
                                dietStageThreshold[i] = new float[nbDietsStages[i] - 1];
                                for (int j = 0; j < nbDietsStages[i] - 1; j++) {
                                    st.nextToken();
                                    dietStageThreshold[i][j] = (new Float(st.sval)).floatValue();
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
                System.exit(1);
            }
            System.out.println("diets config file read");
        }
    }

    public void saveSerieSimulations() {
        //save in output files
        File targetPath;
        String inputFileName = outputPrefix + "_I";
        String biomFileName = outputPrefix + "_B.csv";
        targetPath = new File(outputPathName + outputFileNameTab);
        targetPath.mkdirs();
        saveBIOMData(targetPath, inputFileName, biomFileName);
    }

    public void saveBIOMData(File targetPath, String inputFileName, String biomFileName) {

        FileOutputStream biomFile = null;
        File targetFile;
        PrintWriter pw;

        float[][][] tabMean, tabCv;
        tabMean = new float[nbSpeciesTab + 1 + nbPlanktonGroupsTab][][];
        tabCv = new float[nbSpeciesTab + 1 + nbPlanktonGroupsTab][][];

        for (int i = 0; i <= nbSpeciesTab + nbPlanktonGroupsTab; i++) {
            tabMean[i] = new float[2][];
            tabMean[i][0] = new float[nbLoopTab];
            tabMean[i][1] = new float[nbLoopTab];
            tabCv[i] = new float[2][];
            tabCv[i][0] = new float[nbLoopTab];
            tabCv[i][1] = new float[nbLoopTab];
        }

        float MEAN, STD, CV;//mean, std et cv des moyennes de chaque simu


        try {
            targetFile = new File(targetPath, biomFileName);
            biomFile = new FileOutputStream(targetFile);
        } catch (IOException e) {
            System.out.println("Error of biomass file creation");
            System.exit(1);
        }
        pw = new PrintWriter(biomFile, true);
        pw.println("//File containing the set of input parameters " + inputFileName);
        pw.println("//BIOMASS in tonnes");
        for (int xx = 0; xx < nbLoopTab; xx++) {
            float std;
            float[][] sum = new float[timeSeriesLength][];
            pw.println("SIMULATION n " + xx);
            pw.println();
            //results for tot-0
            for (int t = 0; t < timeSeriesLength; t++) {
                sum[t] = new float[nbDtSavePerYear];
                for (int dt = 0; dt < nbDtSavePerYear; dt++) {
                    sum[t][dt] = 0;
                }
            }
            pw.print("TOTAL-0");
            pw.println(';');
            pw.print("Time");
            pw.print(';');

            for (int t = timeSeriesStart; t < nYears; t++) {
                for (int dt = 0; dt < nbDtSavePerYear; dt++) {
                    pw.print((float) (t + dt / (float) nbDtSavePerYear));
                    pw.print(';');
                }
            }
            pw.println();

            for (int i = 0; i < nbSpeciesTab; i++) {
                pw.print(nameSpecMatrix[i]);
                pw.print(';');
                for (int t = 0; t < timeSeriesLength; t++) {
                    for (int dt = 0; dt < nbDtSavePerYear; dt++) {
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
             * for(int i=nbSpeciesTab;
             * i<nbSpeciesTab+nbPlanktonGroupsTab; i++) {
             * pw.print("Plankton
             * Gr"+(i-nbSpeciesTab+1));pw.print(';'); for(int
             * t=0;t<timeSeriesLength;t++) for(int
             * dt=0;dt<nbDtSavePerYear;dt++) { pw.print(
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
                for (int dt = 0; dt < nbDtSavePerYear; dt++) {
                    pw.print(sum[t][dt]);
                    pw.print(';');
                }
            }
            tabMean[nbSpeciesTab + nbPlanktonGroupsTab][0][xx] = mean2(sum);
            std = std2(sum);
            tabCv[nbSpeciesTab + nbPlanktonGroupsTab][0][xx] = std / tabMean[nbSpeciesTab + nbPlanktonGroupsTab][0][xx];
            pw.print(';');
            pw.print("mean-0");
            pw.print(';');
            pw.print(tabMean[nbSpeciesTab + nbPlanktonGroupsTab][0][xx]);
            pw.print(';');
            pw.print("std-0");
            pw.print(';');
            pw.print(std);
            pw.print(';');
            pw.print("cv-0");
            pw.print(';');
            pw.print(tabCv[nbSpeciesTab + nbPlanktonGroupsTab][0][xx]);
            pw.print(';');
            pw.println();
            pw.println();

            //bloc resultats pour total = with age 0
            if (outputClass0Matrix) {
                for (int t = 0; t < timeSeriesLength; t++) {
                    sum[t] = new float[nbDtSavePerYear];
                    for (int dt = 0; dt < nbDtSavePerYear; dt++) {
                        sum[t][dt] = 0;
                    }
                }
                pw.print("TOTAL");
                pw.println(';');
                pw.print("Time");
                pw.print(';');
                for (int t = timeSeriesStart; t < nYears; t++) {
                    for (int dt = 0; dt < nbDtSavePerYear; dt++) {
                        pw.print((float) (t + dt / (float) nbDtSavePerYear));
                        pw.print(';');
                    }
                }
                pw.println();

                for (int i = 0; i < nbSpeciesTab; i++) {
                    pw.print(nameSpecMatrix[i]);
                    pw.print(';');
                    for (int t = 0; t < timeSeriesLength; t++) {
                        for (int dt = 0; dt < nbDtSavePerYear; dt++) {
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
                 * for(int i=nbSpeciesTab;
                 * i<nbSpeciesTab+nbPlanktonGroupsTab; i++)
                 * { pw.print("Plankton
                 * Gr"+(i-nbSpeciesTab+1));pw.print(';'); for(int
                 * t=0;t<timeSeriesLength;t++) for(int
                 * dt=0;dt<nbDtSavePerYear;dt++) { pw.print(
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
                    for (int dt = 0; dt < nbDtSavePerYear; dt++) {
                        pw.print(sum[t][dt]);
                        pw.print(';');
                    }
                }
                tabMean[nbSpeciesTab + nbPlanktonGroupsTab][1][xx] = mean2(sum);
                std = std2(sum);
                tabCv[nbSpeciesTab + nbPlanktonGroupsTab][1][xx] = std / tabMean[nbSpeciesTab + nbPlanktonGroupsTab][1][xx];
                pw.print(';');
                pw.print("mean");
                pw.print(';');
                pw.print(tabMean[nbSpeciesTab + nbPlanktonGroupsTab][1][xx]);
                pw.print(';');
                pw.print("std");
                pw.print(';');
                pw.print(std);
                pw.print(';');
                pw.print("cv");
                pw.print(';');
                pw.print(tabCv[nbSpeciesTab + nbPlanktonGroupsTab][1][xx]);
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
        for (int i = 0; i < nbSpeciesTab; i++) {
            MEAN = mean(tabMean[i][0]);
            STD = std(tabMean[i][0]);
            CV = STD / MEAN;
            pw.print(nameSpecMatrix[i]);
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
         * i=nbSpeciesTab;i<nbSpeciesTab+nbPlanktonGroupsTab;i++)
         * { MEAN = mean(tabMean[i][0]); STD = std(tabMean[i][0]); CV =
         * STD/MEAN; pw.print("Plankton group
         * "+(i+1-nbSpeciesTab));pw.print(';');pw.print(MEAN);pw.print(';');
         * pw.print(STD);pw.print(';');
         * pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
         * }
         *
         */
        MEAN = mean(tabMean[nbSpeciesTab + nbPlanktonGroupsTab][0]);
        STD = std(tabMean[nbSpeciesTab + nbPlanktonGroupsTab][0]);
        CV = STD / MEAN;
        pw.print("SYS-0");
        pw.print(';');
        pw.print(MEAN);
        pw.print(';');
        pw.print(STD);
        pw.print(';');
        pw.print(CV);
        pw.print(';');
        pw.print(mean(tabCv[nbSpeciesTab + nbPlanktonGroupsTab][0]));
        pw.println(';');
        pw.println();

        //bloc tot
        if (outputClass0Matrix) {
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
            for (int i = 0; i < nbSpeciesTab; i++) {
                MEAN = mean(tabMean[i][1]);
                STD = std(tabMean[i][1]);
                CV = STD / MEAN;
                pw.print(nameSpecMatrix[i]);
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
             * i=nbSpeciesTab;i<nbSpeciesTab+nbPlanktonGroupsTab;i++)
             * { MEAN = mean(tabMean[i][0]); STD = std(tabMean[i][0]); CV =
             * STD/MEAN; pw.print("Plankton group
             * "+(i+1-nbSpeciesTab));pw.print(';');pw.print(MEAN);pw.print(';');
             * pw.print(STD);pw.print(';');
             * pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
             * }
             */
            MEAN = mean(tabMean[nbSpeciesTab + nbPlanktonGroupsTab][1]);
            STD = std(tabMean[nbSpeciesTab + nbPlanktonGroupsTab][1]);
            CV = STD / MEAN;
            pw.print("SYS");
            pw.print(';');
            pw.print(MEAN);
            pw.print(';');
            pw.print(STD);
            pw.print(';');
            pw.print(CV);
            pw.print(';');
            pw.print(mean(tabCv[nbSpeciesTab + nbPlanktonGroupsTab][1]));
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

    public String getLTLClassName() {
        return lowTLClassNameTab;
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
        System.out.println("*****************************************");
        System.out.println("*   Osmose v3.0b - Copyright 2013 IRD   *");
        System.out.println("*****************************************");
        System.out.println(new Date());
        System.out.println();
        osmose.loadArgs(args);
        osmose.init();
        osmose.run();
        System.out.println();
        System.out.println(new Date());
        System.out.println("*   Osmose v3.0b - Exit");
        System.out.println("*****************************************");
    }

    public static Osmose getInstance() {
        return osmose;
    }

    public IGrid getGrid() {
        return grid;
    }

    public LTLForcing getForcing() {
        return forcing;
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

    public int getRecordFrequency() {
        return savingDtMatrix;
    }

    public boolean isCalibrationOutput() {
        return calibrationMatrix;
    }

    public boolean isTLOutput() {
        return TLoutputMatrix;
    }

    public boolean isTLDistribOutput() {
        return TLDistriboutputMatrix;
    }

    public boolean isDietOuput() {
        return dietsOutputMatrix;
    }

    public boolean isMeanSizeOutput() {
        return meanSizeOutputMatrix;
    }

    public boolean isSizeSpectrumOutput() {
        return sizeSpectrumOutputMatrix;
    }

    public boolean isSizeSpectrumSpeciesOutput() {
        return sizeSpectrumPerSpeOutputMatrix;
    }

    public boolean isPlanktonMortalityOutput() {
        return planktonMortalityOutputMatrix;
    }

    public boolean isIncludeClassZero() {
        return outputClass0Matrix;
    }

    public String getDietOutputMetric() {
        return dietOutputMetrics;
    }

    public int getNumberTimeStepsPerYear() {
        return nStepYear;
    }

    public int getNumberYears() {
        return nYears;
    }

    public int getNumberSpecies() {
        return nbSpeciesTab;
    }

    public enum SpatialDistribution {

        RANDOM,
        MAPS,
        CONNECTIVITY;
    }
}
