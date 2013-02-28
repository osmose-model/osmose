package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.grid.OriginalGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.util.ConnectivityMatrix;
import fr.ird.osmose.util.GridMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class Configuration {

    //
    //// IO - INPUT
    // 
    /**
     * Path name of the input folder. The input folder contains at least the
     * main configuration file. It may also contains the whole set of
     * configuration files but not necessarily, as the main configuration file
     * can point to secondary configuration files stored in other places.
     */
    private String inputPathName;
    /**
     * Name of the main configuration file. Set by default to "INPUT.txt"
     */
    private String inputTxtName;
    /**
     * Filename (absolute or relative to input pathname) to secondary
     * configuration files.
     */
    private String configurationFilename,
            speciesFilename,
            predationFilename,
            fishingFilename,
            ltlForcingFilename,
            ltlBasisFilename,
            calibrationFilename,
            outputFilename,
            size0Filename,
            migrationFilename,
            accessibilityFilename,
            reproductionFilename,
            coastFilename,
            areasFilename,
            mpaFilename,
            dietFileName;
    //
    //// IO - OUTPUT 
    //
    /**
     * Pathname of the main output folder. It can be relative to the input
     * pathname.
     */
    private String outputPathName;
    /**
     * Output folder of the simulation, relative to the output pathname.
     */
    private String outputFolder;
    /**
     * Prefix of output files.
     */
    private String outputPrefix;
    //
    //// SIMULATION
    //
    /**
     * Number of replicated simulations.
     */
    private int nSimulation;
    /**
     * Number of schools per species per age class at initialization of the
     * population.
     */
    public int nSchool;
    /**
     * Number of simulated years.
     */
    private int nYear;
    /**
     * Number of time steps per year.
     */
    private int nStepYear;
    //
    //// SPECIES
    //
    /**
     * Number of simulated species.
     */
    private int nSpecies;
    /**
     * Names of the species. Array[nSpecies]
     */
    String[] speciesName;
    /**
     * Species longevity (year). Array[nSpecies]
     */
    float[] speciesLongevity;
    //
    //// GROWTH
    //
    /**
     * Von Bertalanffy growth parameters. Array[nSpecies]
     */
    float[] lInf, K, t0, c, bPower;
    /**
     * Threshold age (year) for applying Von Bertalanffy growth model.
     */
    public float[] growthAgeThreshold;
    //
    //// PREDATION / STARVATION
    //
    /**
     * Number of feeding stages. Array[nSpecies]
     */
    int[] nFeedingStage;
    /**
     * Threshold size (cm) of feeding stages. Array[nSpecies][nFeedingStage-1]
     */
    public float[][] feedingStageThreshold;
    /**
     * Minimal size ratio predator / prey. For a given predator, the smallest
     * accessible prey is: prey_size_min = predator.length /
     * predPreySizeRatioMin Array[nSpecies][nFeedingStage]
     */
    public float[][] predPreySizeRatioMin;
    /**
     * Maximal size ratio predator / prey. For a given predator, the biggest
     * accessible prey is: prey_size_max = predator.length /
     * predPreySizeRatioMax Array[nSpecies][nFeedingStage]
     */
    public float[][] predPreySizeRatioMax;
    /**
     * Maximal ingestion rate. In grams of food per gram of fish and per year.
     * Array[nSpecies]
     */
    public float[] maxPredationRate;
    /**
     * Critical predation efficiency. Ranging [0 1]. Array[nSpecies]
     */
    public float[] criticalPredSuccess;
    /**
     * Maximal starvation mortality rate. Array[nSpecies]
     */
    public float[] starvMaxRate;
    //
    //// ACCESSIBILITY
    //
    /**
     * Number of accessibility stages. Array[nSpecies]
     */
    int[] nAccessStage;
    /**
     * Threshold age (year) between accessibility stages.
     * Array[nSpecies][nAccessStage]
     */
    float[][] accessStageThreshold;
    /**
     * Accessibility matrix.
     * Array[nSpecies+nPlankton][nAccessStage][nSpecies][nAccessStage]
     */
    public float[][][][] accessibilityMatrix;
    //
    //// NATURAL MORTALITY
    //
    /**
     * Natural mortality rates. Array[nSpecies]
     */
    float[] D;
    /**
     * Larval mortality rates. Array[nSpecies]
     */
    public float[][] larvalMortalityRates;
    //
    //// REPRODUCTION
    //
    /**
     * Relative fecundity. Annual fecundity (number of eggs) per gram of mature
     * female. Array[nSpecies]
     */
    public float[] alpha;
    /**
     * Size (cm) at maturity. Array[nSpecies]
     */
    public float[] sizeMaturity;
    /**
     * Spawning seasonality. Array[nSpecies][nStepYear]
     */
    float[][] seasonSpawning;
    /**
     * Size of eggs (cm). Set to one millimeter by default. Array[nSpecies]
     */
    public float[] eggSize;
    /**
     * Weight of eggs (kg). Set to 0.538 gram by default, considering a sphere
     * with the water density by default. Array[nSpecies]
     */
    public float[] eggWeight;
    /**
     * Sex ratio, expressed as a percentage of females. Array[nSpecies]
     */
    public float[] sexRatio;
    /**
     * Whether a species reproduces within or outside the simulated domain.
     * Array[nSpecies]
     */
    public boolean[] reproduceLocally;
    /**
     * Annual flux of biomass (ton) for species that reproduce outside the
     * simulated domain. Array[nSpecies]
     */
    public float[] biomassFluxIn;
    /**
     * Mean length (cm) of fish flowing in for species that reproduce outside
     * the simulated domain. Array[nSpecies]
     */
    public float[] meanLengthFishIn;
    /**
     * Mean age (year) of fish flowing in for species that reproduce outside the
     * simulated domain. Array[nSpecies]
     */
    public float[] meanAgeFishIn;
    //
    //// FISHING
    //
    /**
     * Age of recruitment (year). Array[nSpecies]
     */
    float[] recruitmentAge;
    /**
     * Fishing mortality rates. Array[nSpecies][nStepYear|nStepSimu]
     */
    public float[][] fishingRates;
    //
    //// PLANKTON
    //
    /**
     * Number of plankton groups.
     */
    int nPlankton;
    /**
     * Accessibility plankton coefficients. Array[nPlankton]
     */
    public float[] planktonAccessibility;
    /**
     * Name of the plankton groups. Array[nPlankton]
     */
    String[] planktonName;
    /**
     * Trophic level of plankton groups. Array[nPlankton]
     */
    float[] ltlTrophicLevel;
    /**
     * Plankton minimal size (cm). Array[nPlankton]
     */
    float[] ltlMinSize;
    /**
     * Plankton maximal size (cm). Array[nPlankton]
     */
    float[] ltlMaxSize;
    /**
     * Conversion factor from plankton concentration to ton.
     */
    float[] ltlConversionFactor;
    /**
     * Conversion factor from biomass to production.
     */
    float[] ltlProdBiomFactor;
    /**
     * Number of time steps in the LTL model.
     */
    private int nLTLStep;
    /**
     * Integration depth (meter) of plankton biomass (from sea surface).
     */
    private float integrationDepth;
    /**
     * LTLForcing implementation for interfacing Osmose and LTL model.
     */
    private LTLForcing forcing;
    /**
     * Name of the Java Class name implementing LTLForcing.java.
     */
    private String ltlForcingClassName;
    //
    //// GRID
    //
    /**
     * Name of the Java Class name implementing IGrid.java.
     */
    private String gridClassName;
    /**
     * IGrid implementation of the grid.
     */
    private IGrid grid;
    /**
     * External filename for defining the grid.
     */
    public String gridFileTab;
    /**
     * Name of the longitude field in the external grid file.
     */
    public String lonField;
    /**
     * Name of the latitude field in the external grid file.
     */
    public String latField;
    /**
     * Name of the mask field in the external grid file.
     */
    public String maskField;
    /**
     * Stride for building the Osmose grid from the LTL grid. One osmose cell =
     * stride * stride LTL cells
     */
    public int stride;
    /**
     * Number of lines in the grid.
     */
    public int nLine;
    /**
     * Number of columns in the grid.
     */
    public int nColumn;
    /**
     * Latitude (North degree) of the upper left corner of the grid.
     */
    public float upLeftLat;
    /**
     * Latitude (North degree) of the lower right corner of the grid.
     */
    public float lowRightLat;
    /**
     * Longitude (East degree) of the upper left corner of the grid.
     */
    public float upLeftLon;
    /**
     * Longitude (East degree) of the lower right corner of the grid.
     */
    public float lowRightLon;
    /**
     * i-coordinate of the cells on land.
     */
    public int[] icoordLand;
    /**
     * j-coordinate of the cells on land.
     */
    public int[] jcoordLand;
    /**
     * Number of cells on land.
     */
    int nLandCell;
    //
    //// DISTRBUTION
    //
    /**
     * Number of cells for random distribution. Array[nSpecies]
     */
    public int[] randomAreaSize;
    /**
     * Indexes of maps per species, per age class and per time step.
     * Array[nSpecies][nAgeClass][nStepSimu]
     */
    public int[][][] numMap;
    /**
     * List of habitat maps. Array[nMap]
     */
    public GridMap[] maps;
    /**
     * Maximal probability of presence in a map. Array[nMap]
     */
    public float[] maxProbaPresence;
    /**
     * List of connectivity matrices. Array[nMap]
     */
    public ConnectivityMatrix[] connectivityMatrix;
    /**
     * Range of random displacement (cell). Array[nSpecies]
     */
    public int[] range;
    /**
     * Ages of species concerned by distribution maps.
     * Array[nMap][nYearConcerned]
     */
    int[][] agesMap;
    /**
     * Annual time step concerned by distribution maps.
     * Array[nMap][nStepConcerned]
     */
    int[][] seasonMap;
    /**
     * Species indexes concerned by distribution maps. Array[nMap]
     */
    int[] speciesMap;
    /**
     * List of the filenames of the distribution maps.
     */
    private String[] mapFile;
    /**
     * Index of the maps without twins.
     */
    private int[] mapIndexNoTwin;
    /**
     * Type of spatial distribution.
     */
    public SpatialDistribution[] spatialDistribution;
    //
    //// MPA
    //
    /**
     * i-coordinate of the MPA cells.
     */
    public int[] tabMPAiMatrix;
    /**
     * j-coordinate of the MPA cells.
     */
    public int[] tabMPAjMatrix;
    /**
     * Year the MPA is enabled.
     */
    public int yearStartMPA;
    /**
     * Year the MPA is disabled.
     */
    public int yearEndMPA;
    //
    //// INDICATORS
    //
    /**
     * Boolean for enabling indicator.
     */
    public boolean outputTL,
            outputTLSpectrum,
            outputDiet,
            outputMeanSize,
            outputSizeSpectrum,
            outputSizeSpectrumSpecies,
            outputCalibration,
            outputClass0,
            outputSpatialized,
            outputPlanktonBiomass;
    /**
     * Year to start saving the indicators.
     */
    public int yearStartSaving;
    /**
     * Record frequency, expressed as number of time steps.
     */
    private int recordFrequency;
    /**
     * Metrics used for splitting the stages (either age or size).
     */
    private String dietOutputMetrics;
    /**
     * Number of diet stages.
     */
    int[] nDietStage;
    /**
     * Threshold age (year) or size (cm) between the diet stages.
     */
    float[][] dietStageThreshold;
    /**
     * Minimal size (cm) of the size spectrum.
     */
    public float spectrumMinSize;
    /**
     * Maximal size (cm) of the size spectrum.
     */
    public float spectrumMaxSize;
    /**
     * Range (cm) of size classes.
     */
    public float classRange;
    public float[] tabSizes;
    public float[] tabSizesLn;
    public int nbSizeClass;
    // TL distrib
    public int nbTLClass;
    float minTL;
    float maxTL;
    public float[] tabTL;
    //
    //// POPULATION INITIALIZATION
    //
    /**
     * Calibration method. Either 'biomass' or 'spectrum'.
     */
    public String calibrationMethod;
    /**
     * Threshold age (year) for age class zero. It is the age from which target
     * biomass should be considered as eggs and larvae stages are generally not
     * considered. Array[nSpecies]
     */
    float[] supAgeOfClass0Matrix;
    /**
     * Initialization biomass (ton) per species. Array[nSpecies]
     */
    public double[] targetBiomass;
    /**
     * Slope of the size spectrum.
     */
    public double sizeSpectrumSlope;
    /**
     * Intercept of the size spectrum.
     */
    public double sizeSpectrumIntercept;
    //
    //// MIGRATION
    //
    /**
     * Mortality rates for migrating schools. Array[nSpecies][nAgeClass]
     */
    public float[][] migrationTempMortality;
    /**
     * Ages of species concerned by migration. Array[nSpecies][nYearConcerned]
     */
    public int[][] ageMigration;
    /**
     * Annual time step for species concerned by migration.
     * Array[nSpecies][nStepConcerned]
     */
    public int[][] seasonMigration;
    // phv 20130208
    // Temporary flag to be able to read new format for HABITAT configuration
    // file with agemin and agemax specified.
    public final static boolean NEW_AREA_FILE = false;

    Configuration(String inputPathName, String outputPathName, String inputTxtName) {
        this.inputPathName = inputPathName;
        this.outputPathName = outputPathName;
        this.inputTxtName = inputTxtName;
    }

    public void init() {

        readInputFile();	// read the first file containing the file names of all other input files
        readAllInputFiles();
        initializeSizeAndTLSpectrum();
    }

    public void initializeSizeAndTLSpectrum() {
        if (outputSizeSpectrum || outputSizeSpectrumSpecies) {
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

    public void readAllInputFiles() {
        readConfigurationFile(configurationFilename);
        readSpeciesFile(speciesFilename);
        readLTLBasisFile(ltlBasisFilename);
        readPredationFile(predationFilename);
        readFishingFile(fishingFilename);
        readCalibrationFile(calibrationFilename);
        readSeasonalityReproFile(reproductionFilename);
        readsize0File(size0Filename);
        readOutputConfigurationFile(outputFilename);
        if (outputDiet) {
            readDietsOutputFile(dietFileName);
        }
        readAccessibilitiesFile(accessibilityFilename);
        readMigrationFile();
        readCoastFile();
        initGrid();
        if (!NEW_AREA_FILE) {
            readAreaFile();
        }
        readMPAFile();
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
            nSimulation = (new Integer(st.sval)).intValue();
            System.out.println("  Number of replicated simulations = " + getNSimulation());

            st.nextToken();
            configurationFilename = st.sval;
            System.out.println("  Configuration file = " + configurationFilename);

            st.nextToken();
            speciesFilename = st.sval;
            System.out.println("  Species file = " + speciesFilename);

            st.nextToken();
            predationFilename = st.sval;
            System.out.println("  Predation file = " + predationFilename);

            st.nextToken();
            fishingFilename = st.sval;
            System.out.println("  Fishing file = " + fishingFilename);

            st.nextToken();
            areasFilename = st.sval;
            System.out.println("  Areas file = " + getAreasFilename());

            st.nextToken();
            ltlBasisFilename = st.sval;
            System.out.println("  Plankton groups file = " + ltlBasisFilename);

            st.nextToken();
            ltlForcingFilename = st.sval;
            System.out.println("  Plankton data file = " + getLtlForcingFilename());

            st.nextToken();
            calibrationFilename = st.sval;
            System.out.println("  Calibration file = " + calibrationFilename);

            st.nextToken();
            outputFilename = st.sval;
            System.out.println("  Indicators file = " + outputFilename);

            st.nextToken();
            accessibilityFilename = st.sval;
            System.out.println("  Accessibilities file = " + accessibilityFilename);

            st.nextToken();
            size0Filename = st.sval;
            System.out.println("  Size age-class zero file = " + size0Filename);

            st.nextToken();
            reproductionFilename = st.sval;
            System.out.println("  Reproduction file = " + reproductionFilename);

            st.nextToken();
            // fishingSeasonFilename not used anymore

            st.nextToken();
            migrationFilename = st.sval;
            System.out.println("  Migration file = " + migrationFilename);

            st.nextToken();
            coastFilename = st.sval;
            System.out.println("  Coast file = " + coastFilename);

            st.nextToken();
            mpaFilename = st.sval;
            System.out.println("  MPA file = " + mpaFilename);

            inputFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of INPUT file");
            System.exit(1);
        }
        System.out.println("EOF for step 2. Reading file INPUT.txt");
    }

    /**
     * Read LTL basic file with name of plankton, sizes, format of files...
     */
    private void readLTLBasisFile(String planktonStructureFileName) {

        FileInputStream LTLFile = null;
        try {
            LTLFile = new FileInputStream(new File(resolveFile(planktonStructureFileName)));
        } catch (FileNotFoundException ex) {
            System.out.println("LTL file " + planktonStructureFileName + " doesn't exist");
            System.exit(1);
        }

        Reader r = new BufferedReader(new InputStreamReader(LTLFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            int nPlanktonTmp = (new Integer(st.sval)).intValue();
            if (!(nPlanktonTmp == nPlankton)) {
                System.out.println("The number of plankton group in plankton structure file does not match the one from config file");
                System.exit(1);
            }

            st.nextToken();
            nLTLStep = (new Integer(st.sval)).intValue();
            if (nLTLStep % getNumberTimeStepsPerYear() > 0) {
                System.out.println("Number of LTL steps (found " + nLTLStep + ") should be a multiple of osmose number of steps per year (" + getNumberTimeStepsPerYear() + ")");
                System.exit(1);
            }

            // initializing tables
            planktonName = new String[nPlankton];
            ltlTrophicLevel = new float[nPlankton];
            ltlMinSize = new float[nPlankton];
            ltlMaxSize = new float[nPlankton];
            ltlConversionFactor = new float[nPlankton];
            ltlProdBiomFactor = new float[nPlankton];

            for (int i = 0; i < nPlankton; i++) {
                // filling tables
                st.nextToken();
                planktonName[i] = st.sval;
                planktonName[i] = st.sval;
                st.nextToken();
                ltlMinSize[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                ltlMaxSize[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                ltlTrophicLevel[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                ltlConversionFactor[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                ltlProdBiomFactor[i] = (new Float(st.sval)).floatValue();
            }

            st.nextToken();
            int nbDimensionsGrid = new Integer(st.sval).intValue();
            if ((nbDimensionsGrid > 3) || (nbDimensionsGrid < 2)) {
                System.out.println("The dimension " + nbDimensionsGrid + " cannot be consider - should be 2 or 3");
            }
            st.nextToken();
            // nx not used anymore
            st.nextToken();
            // ny not used anymore
            if (nbDimensionsGrid == 3) {
                st.nextToken();
                // nz not used anymore
                st.nextToken();
                integrationDepth = new Float(st.sval).floatValue();
            }
        } catch (IOException ex) {
            System.out.println("Reading error of LTL structure file");
            System.exit(1);
        }
    }

    public void readSpeciesFile(String speciesFileName) {
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
            if (new Integer(st.sval).intValue() == nSpecies) {
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    speciesName[i] = st.sval;
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    D[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    speciesLongevity[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    lInf[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    K[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    t0[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    c[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    bPower[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    alpha[i] = (new Float(st.sval)).floatValue();
                }
                String[] maturityMetric = new String[nSpecies];
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    maturityMetric[i] = st.sval;
                }
                for (int i = 0; i < nSpecies; i++) {
                    if (maturityMetric[i].equalsIgnoreCase("age")) //conversion from maturity age to maturity size through von Bertalanffy equation
                    {
                        st.nextToken();
                        sizeMaturity[i] = lInf[i] * (float) (1 - Math.exp(-K[i]
                                * ((new Float(st.sval).floatValue()) - t0[i])));             //***** to checked if not too big fish (same test than for recruit age)
                    } else {
                        st.nextToken();
                        sizeMaturity[i] = (new Float(st.sval)).floatValue();
                    }
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    sexRatio[i] = (new Float(st.sval)).floatValue();
                }
                /*
                 * phv 2011/11/21 Determine wether reproduction occurs locally
                 * or outside the simulated domain. Then for species reproducing
                 * outside it reads the incoming biomass caracteristics.
                 */
                reproduceLocally = new boolean[nSpecies];
                biomassFluxIn = new float[nSpecies];
                meanLengthFishIn = new float[nSpecies];
                meanAgeFishIn = new float[nSpecies];
                range = new int[nSpecies];
                st.nextToken();
                int nbReproOut = 0;
                if (null != st.sval) {
                    if (Character.isDigit(st.sval.charAt(0))) {
                        for (int i = 0; i < nSpecies; i++) {
                            range[i] = Integer.getInteger(st.sval);
                            st.nextToken();
                        }
                    } else {
                        for (int i = 0; i < nSpecies; i++) {
                            reproduceLocally[i] = st.sval.matches("in");
                            if (!reproduceLocally[i]) {
                                nbReproOut++;
                            }
                            st.nextToken();
                        }
                        for (int i = 0; i < nbReproOut; i++) {
                            int indexSpecies = new Integer(st.sval).intValue() - 1;
                            st.nextToken();
                            biomassFluxIn[indexSpecies] = new Float(st.sval).floatValue();
                            st.nextToken();
                            meanLengthFishIn[indexSpecies] = new Float(st.sval).floatValue();
                            st.nextToken();
                            meanAgeFishIn[indexSpecies] = new Float(st.sval).floatValue();
                            st.nextToken();
                        }
                        if (null != st.sval) {
                            for (int i = 0; i < nSpecies; i++) {
                                range[i] = Integer.valueOf(st.sval);
                                st.nextToken();
                            }
                        } else {
                            for (int i = 0; i < nSpecies; i++) {
                                range[i] = 1;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < nSpecies; i++) {
                        reproduceLocally[i] = true;
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
        if (nb_rows != nStepYear & nb_rows != nStepYear * nYear) {
            System.out.println("Reproduction seasonality file " + csvFile + " contains " + nb_rows + " rows. Should be either " + nStepYear + " or " + (nStepYear * nYear));
            System.exit(1);
        }
        int nb_columns = lines.get(0).length - 1;
        if (nb_columns != nSpecies) {
            System.out.println("Wrong number of species in reproduction seasonality file " + csvFile);
            System.exit(1);
        }
        /*
         * Reads the larval mortality rates
         */
        seasonSpawning = new float[nSpecies][nb_rows];
        for (int step = 0; step < nb_rows; step++) {
            String[] line = lines.get(step + 1); // skip header on 1st line
            for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                seasonSpawning[iSpec][step] = Float.valueOf(line[iSpec + 1]) / 100.f; // skip 1st column, the time step
            }
        }
        /*
         * Check that sum of % == 100% every year for every species
         */
        for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
            double sum = 0;
            for (int iStep = 0; iStep < seasonSpawning[iSpec].length; iStep++) {
                sum += seasonSpawning[iSpec][iStep];
                if ((iStep + 1) % nStepYear == 0) {
                    if (!((sum > 0.99f) && (sum < 1.01f))) {
                        int year = (iStep + 1) / nStepYear;
                        System.out.println("ERROR: sum of percents does not equal 100% in spawning seasonality file " + csvFile + " for species " + speciesName[iSpec] + " in year " + year);
                        System.exit(1);
                    }
                    sum = 0;
                }
            }
        }

    }

    public void readSeasonalityReproFile(String reproductionFileName) {
        if (reproductionFileName.equalsIgnoreCase("default")) {
            for (int i = 0; i < nSpecies; i++) {
                seasonSpawning[i] = new float[nStepYear];
                for (int j = 0; j < nStepYear; j++) {
                    seasonSpawning[i][j] = (float) 1 / (float) nStepYear;
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
                    for (int i = 0; i < nSpecies; i++) {
                        tempSum = 0;
                        seasonSpawning[i] = new float[nStepYear];
                        for (int j = 0; j < nStepYear; j++) {
                            st.nextToken();
                            seasonSpawning[i][j] = (new Float(st.sval)).floatValue() / 100; //percentage
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
            if (nb_rows != nStepYear & nb_rows != nStepYear * nYear) {
                System.out.println("Fishing rate file " + csvFile + " contains " + nb_rows + " rows. Should be either " + nStepYear + " or " + (nStepYear * nYear));
                System.exit(1);
            }
            int nb_columns = lines.get(0).length - 1;
            if (nb_columns != nSpecies) {
                System.out.println("Wrong number of species in fishing rate file " + csvFile);
                System.exit(1);
            }
            /*
             * Reads the fishing rates
             */
            fishingRates = new float[nSpecies][nb_rows];
            for (int step = 0; step < nb_rows; step++) {
                String[] line = lines.get(step + 1); // skip header on 1st line
                for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                    fishingRates[iSpec][step] = Float.valueOf(line[iSpec + 1]); // skip 1st column, the time step
                }
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void readFishingFile(String fishingFileName) {

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
            if (st.sval.equalsIgnoreCase("age")) {
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    recruitmentAge[i] = (new Float(st.sval)).floatValue();
                }
            } else {
                float[] recruitSizeMatrix = new float[nSpecies];
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    recruitSizeMatrix[i] = (new Float(st.sval)).floatValue();
                    if (recruitSizeMatrix[i] < lInf[i]) {
                        recruitmentAge[i] = (float) (-((Math.log(1 - (recruitSizeMatrix[i] / lInf[i]))) / K[i])) + t0[i];
                    } else {
                        recruitmentAge[i] = speciesLongevity[i] + 1;
                    }
                    if (recruitmentAge[i] < 0.6)//due to inverse von Bert transformation
                    {
                        recruitmentAge[i] = (float) 0.6; // >0.5 to avoid Math.round() problems
                    }
                    if (recruitmentAge[i] > speciesLongevity[i]) {
                        recruitmentAge[i] = speciesLongevity[i] + 1;
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
        if (nb_rows != 1 & nb_rows != nStepYear & nb_rows != nStepYear * nYear) {
            System.out.println("Larval mortality rates file " + csvFile + " contains " + nb_rows + " rows. Should be either 1 or " + nStepYear + " or " + (nStepYear * nYear));
            System.exit(1);
        }
        int nb_columns = lines.get(0).length - 1;
        if (nb_columns != nSpecies) {
            System.out.println("Wrong number of species in larval mortality rates file " + csvFile);
            System.exit(1);
        }
        /*
         * Reads the larval mortality rates
         */
        larvalMortalityRates = new float[nSpecies][nb_rows];
        for (int step = 0; step < nb_rows; step++) {
            String[] line = lines.get(step + 1); // skip header on 1st line
            for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                larvalMortalityRates[iSpec][step] = Float.valueOf(line[iSpec + 1]); // skip 1st column, the time step
            }
        }
    }

    public void readCalibrationFile(String calibrationFileName) {
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
                targetBiomass = new double[nSpecies];
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    targetBiomass[i] = (new Double(st.sval)).doubleValue();
                }
            } else if (calibrationMethod.equalsIgnoreCase("spectrum")) {
                st.nextToken();
                sizeSpectrumSlope = (new Double(st.sval)).doubleValue();
                st.nextToken();
                sizeSpectrumIntercept = (new Double(st.sval)).doubleValue();
            } else if (calibrationMethod.equalsIgnoreCase("random")) //*****************TO DETAIL****************************
            {
                System.out.println("The option initialisation per random method is not implemented yet");
            }

            supAgeOfClass0Matrix = new float[nSpecies];

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
                larvalMortalityRates = new float[nSpecies][1];
                for (int i = 0; i < nSpecies; i++) {
                    larvalMortalityRates[i][0] = (new Float(st.sval)).floatValue();
                    st.nextToken();
                }
            } catch (NumberFormatException ex) {
                String larvalMortalityFile = st.sval;
                readLarvalMortalityRates(resolveFile(larvalMortalityFile));
                st.nextToken();
            }

            for (int i = 0; i < nSpecies; i++) {
                supAgeOfClass0Matrix[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
            }
            for (int i = 0; i < nPlankton; i++) {
                planktonAccessibility[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
            }
            calibFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of calibration file");
            System.exit(1);
        }
    }

    public void readConfigurationFile(String configFileName) {
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
            nLine = (new Integer(st.sval)).intValue();
            st.nextToken();
            nColumn = (new Integer(st.sval)).intValue();
            st.nextToken();
            upLeftLat = new Float(st.sval).floatValue();
            st.nextToken();
            lowRightLat = new Float(st.sval).floatValue();
            st.nextToken();
            upLeftLon = new Float(st.sval).floatValue();
            st.nextToken();
            lowRightLon = new Float(st.sval).floatValue();
            st.nextToken();
            nYear = (new Integer(st.sval)).intValue();
            st.nextToken();
            nStepYear = (new Integer(st.sval)).intValue();
            st.nextToken();
            recordFrequency = (new Integer(st.sval)).intValue();
            st.nextToken();
            yearStartSaving = (new Integer(st.sval)).intValue();
            //these param are function of the length of the temporal series saved
            if (yearStartSaving > nYear) {
                System.out.println("Starting time for saving higher than simulation time -> set to 0");
                yearStartSaving = 0;
            }
            st.nextToken();
            nSpecies = (new Integer(st.sval)).intValue();
            st.nextToken();
            nPlankton = (new Integer(st.sval)).intValue();

            st.nextToken();
            // parameters for forcing/coupling not used anymore

            /*
             * addition phv 2011/08/02 since Coupling.java (renamed as
             * LTLCouplingRomsPisces) is model specific, I had to code new
             * classes for ECO3M (Danial for GL) and BFM (Camille Adriatic). So
             * in the config file I added a parameter 3.12 to identify the Java
             * class that implements the forcing/coupling.
             */
            st.nextToken();
            ltlForcingClassName = st.sval;

            st.nextToken();
            nSchool = 1 + Math.round(((new Integer(st.sval)).intValue()) / nStepYear);

            /*
             * Additional parameters to read the grid from NetCDF file
             */
            st.nextToken();
            if (null != st.sval) {
                gridClassName = st.sval;
                st.nextToken();
                String filename = resolveFile(st.sval).toString();
                gridFileTab = filename;
                st.nextToken();
                lonField = st.sval;
                st.nextToken();
                latField = st.sval;
                st.nextToken();
                maskField = st.sval;
                st.nextToken();
                stride = Integer.valueOf(st.sval);
            } else {
                gridClassName = OriginalGrid.class.getCanonicalName();
            }

            configFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of configuration file");
            System.exit(1);
        }
        initializeNbLivingGroups(nSpecies, nPlankton);
    }

    public void initializeNbLivingGroups(int nSpecies, int nPlankton) {

        // setting tables length
        //----OPTION file------
        eggSize = new float[nSpecies];
        eggWeight = new float[nSpecies];
        growthAgeThreshold = new float[nSpecies];
        maxPredationRate = new float[nSpecies];
        predPreySizeRatioMax = new float[nSpecies][];
        predPreySizeRatioMin = new float[nSpecies][];
        criticalPredSuccess = new float[nSpecies];
        starvMaxRate = new float[nSpecies];
        //----SPECIES file------
        speciesName = new String[nSpecies];
        D = new float[nSpecies];
        speciesLongevity = new float[nSpecies];
        lInf = new float[nSpecies];
        K = new float[nSpecies];
        t0 = new float[nSpecies];
        c = new float[nSpecies];
        bPower = new float[nSpecies];
        alpha = new float[nSpecies];
        sizeMaturity = new float[nSpecies];
        sexRatio = new float[nSpecies];
        feedingStageThreshold = new float[nSpecies][];
        nFeedingStage = new int[nSpecies];
        //----REPRODUCTION file------
        seasonSpawning = new float[nSpecies][];
        //----FISHING file------
        recruitmentAge = new float[nSpecies];
        //--- ACCESSIBILITIES----
        planktonAccessibility = new float[nPlankton];
        nAccessStage = new int[nSpecies];
        accessStageThreshold = new float[nSpecies][];
        accessibilityMatrix = new float[nSpecies + nPlankton][][][];

        planktonName = new String[nPlankton];
    }

    public void readsize0File(String optionFileName) {
        if (optionFileName.equalsIgnoreCase("default")) {
            for (int i = 0; i < nSpecies; i++) {
                eggSize[i] = 0.1f;	// by default : egg diameter set to 1mm
                eggWeight[i] = 0.0005386f;	// weight of egg by default, considering a sphere with the water density
                growthAgeThreshold[i] = 1.0f;	// by default, von Bertalanffy model considered valid after 1 year old, linear growth from 0 to 1 year
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
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    eggSize[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    eggWeight[i] = (new Float(st.sval)).floatValue();
                }
                for (int i = 0; i < nSpecies; i++) {
                    st.nextToken();
                    growthAgeThreshold[i] = (new Float(st.sval)).floatValue();
                }

                optionFile.close();
            } catch (IOException ex) {
                System.out.println("Reading error of option file");
                System.exit(1);
            }
        }
    }

    public void readPredationFile(String predationFileName) {
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
            for (int i = 0; i < nSpecies; i++) {
                st.nextToken();
                maxPredationRate[i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nSpecies; i++) {
                st.nextToken();
                criticalPredSuccess[i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nSpecies; i++) {
                st.nextToken();
                starvMaxRate[i] = (new Float(st.sval)).floatValue();
            }
            for (int i = 0; i < nSpecies; i++) {
                st.nextToken();
                nFeedingStage[i] = (new Integer(st.sval)).intValue();
                feedingStageThreshold[i] = new float[nFeedingStage[i] - 1];
                predPreySizeRatioMin[i] = new float[nFeedingStage[i]];
                predPreySizeRatioMax[i] = new float[nFeedingStage[i]];
                for (int s = 0; s < nFeedingStage[i] - 1; s++) {
                    st.nextToken();
                    feedingStageThreshold[i][s] = (new Float(st.sval)).floatValue();
                }
                for (int s = 0; s < nFeedingStage[i]; s++) {
                    st.nextToken();
                    predPreySizeRatioMin[i][s] = (new Float(st.sval)).floatValue();
                    st.nextToken();
                    predPreySizeRatioMax[i][s] = (new Float(st.sval)).floatValue();
                }
            }
            predationFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of predation file");
            System.exit(1);
        }
    }

    public void readOutputConfigurationFile(String indicatorsFileName) {
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
            outputFolder = st.sval;
            String[] split = st.sval.split("/");
            outputPrefix = split[split.length - 1];

            st.nextToken();
            outputCalibration = false;
            if (st.sval.equalsIgnoreCase("calibration")) {
                outputCalibration = true;
            }

            st.nextToken();
            outputTL = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            outputTLSpectrum = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            outputDiet = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            dietFileName = st.sval;
            st.nextToken();
            outputMeanSize = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            outputSizeSpectrum = (Boolean.valueOf(st.sval)).booleanValue();
            st.nextToken();
            outputSizeSpectrumSpecies = (Boolean.valueOf(st.sval)).booleanValue();
            if (outputSizeSpectrum || outputSizeSpectrumSpecies) {
                st.nextToken();
                spectrumMinSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                spectrumMaxSize = (new Float(st.sval)).floatValue();
                st.nextToken();
                classRange = (new Float(st.sval)).floatValue();
            }
            st.nextToken();
            // plankton mortality output, not used anymore
            st.nextToken();
            outputClass0 = (Boolean.valueOf(st.sval)).booleanValue();
            try {
                /*
                 * phv 2011/06/30 Read additional parameters "spatialized
                 * outputs" Since it might not exist in most configurations I
                 * catch any exception and set it as false by default.
                 */
                st.nextToken();
                outputSpatialized = (Boolean.valueOf(st.sval)).booleanValue();
            } catch (Exception ex) {
                outputSpatialized = false;
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
                outputPlanktonBiomass = (Boolean.valueOf(st.sval)).booleanValue();
            } catch (Exception ex) {
                outputPlanktonBiomass = false;
            }

            indicFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of output config file");
            System.exit(1);
        }
    }

    public void initGrid() {
        try {
            System.out.println("Initialize grid: " + gridClassName);
            grid = (IGrid) Class.forName(gridClassName).newInstance();

        } catch (InstantiationException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Init the grid
        grid.init();
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
        forcing.init();
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
            nLandCell = nbCells;

            /*
             * Identify the coordinates
             */
            int indexCell = 0;
            icoordLand = new int[nbCells];
            jcoordLand = new int[nbCells];
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val <= 0.f) {
                        icoordLand[indexCell] = i;
                        jcoordLand[indexCell] = j;
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
            if (new Integer(st.sval).intValue() == nLine) {
                st.nextToken();
                if (new Integer(st.sval).intValue() == nColumn) {

                    st.nextToken();
                    icoordLand = new int[new Integer(st.sval).intValue()];
                    jcoordLand = new int[new Integer(st.sval).intValue()];

                    nLandCell = new Integer(st.sval).intValue();

                    for (int i = 0; i < icoordLand.length; i++) {

                        st.nextToken();
                        icoordLand[i] = (new Integer(st.sval).intValue());
                    }

                    for (int i = 0; i < jcoordLand.length; i++) {

                        st.nextToken();
                        jcoordLand[i] = (new Integer(st.sval).intValue());
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
        if (coastFilename.equalsIgnoreCase("default") || coastFilename.equalsIgnoreCase("none")) {
            System.out.println("No coast in the grid (default)");
            nLandCell = 0;
        } else {

            String filename = resolveFile(coastFilename);
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
        if (mpaFilename.equalsIgnoreCase("default")) {
            tabMPAiMatrix = new int[0];
            tabMPAjMatrix = new int[0];
            yearStartMPA = 0;
            yearEndMPA = 0;
        } else {
            //read info in file mpa
            FileInputStream mpaFile = null;
            int[] tabi, tabj;
            try {
                mpaFile = new FileInputStream(resolveFile(mpaFilename));
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
                if (new Integer(st.sval).intValue() == nLine) {
                    st.nextToken();
                    if (new Integer(st.sval).intValue() == nColumn) {
                        st.nextToken();
                        tabi = new int[new Integer(st.sval).intValue()];
                        tabj = new int[new Integer(st.sval).intValue()];
                        st.nextToken();
                        yearStartMPA = new Integer(st.sval).intValue();
                        st.nextToken();
                        yearEndMPA = new Integer(st.sval).intValue();
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
        }
    }

    public void writeMPAasCSV() {

        String fileName = "mpa.csv";
//        try {
//            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(fileName)), ';', CSVWriter.NO_QUOTE_CHARACTER);
//            for (int i = 0; i < getGrid().getNbLines(); i++) {
//                String[] entries = new String[getGrid().getNbColumns()];
//                for (int j = 0; j < getGrid().getNbColumns(); j++) {
//                    if (getGrid().getCell(i, j).isLand()) {
//                        entries[j] = String.valueOf(-99);
//                    } else if (getGrid().getCell(i, j).isMPA()) {
//                        entries[j] = String.valueOf(1);
//                    } else {
//                        entries[j] = String.valueOf(0);
//                    }
//                }
//                writer.writeNext(entries);
//            }
//            writer.close();
//        } catch (IOException ex) {
//            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public void readAreaFile() {

        /*
         * Open areas-maps configuration file
         */
        FileInputStream areasFile = null;
        try {
            areasFile = new FileInputStream(resolveFile(getAreasFilename()));
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
        spatialDistribution = new SpatialDistribution[nSpecies];
        randomAreaSize = new int[nSpecies];
        try {

            for (int i = 0; i < nSpecies; i++) {
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
            speciesMap = new int[nbMaps];
            mapFile = new String[nbMaps];
            mapIndexNoTwin = new int[nbMaps];
            agesMap = new int[nbMaps][];
            seasonMap = new int[nbMaps][];
            numMap = new int[nSpecies][][];
            for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                int longevity = (int) Math.round((this.speciesLongevity[iSpec]) * nStepYear);
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
                        randomAreaSize[iSpec] = (new Integer(st.sval)).intValue();
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

        speciesMap[indexMap] = iSpec;
        /*
         * read number of class ages concerned by this map
         */
        st.nextToken();
        int nbAgePerMap = new Integer(st.sval).intValue();
        agesMap[indexMap] = new int[nbAgePerMap];
        /*
         * read number of time step over the year concerned by this map
         */
        st.nextToken();
        int nbDtPerMap = new Integer(st.sval).intValue();
        seasonMap[indexMap] = new int[nbDtPerMap];
        /*
         * read the age classes concerned by this map
         */
        for (int k = 0; k < nbAgePerMap; k++) {
            st.nextToken();
            agesMap[indexMap][k] = new Integer(st.sval).intValue();
        }
        /*
         * read the time steps over the year concerned by this map
         */
        for (int k = 0; k < nbDtPerMap; k++) {
            st.nextToken();
            seasonMap[indexMap][k] = new Integer(st.sval).intValue() - 1;
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
            System.out.println("Reading connectivity matric for " + speciesName[iSpec] + " map " + indexMap);
            String csvFile = resolveFile(st.sval);
            connectivityMatrix[indexMap] = new ConnectivityMatrix(indexMap, csvFile);
            System.out.println("Connectivity matrix loaded");
        }
    }

    private void readAreaCSV(StreamTokenizer st, int iSpec, int indexMap) throws IOException {

        speciesMap[indexMap] = iSpec;   //because species number between 1 and nbSpecies
            /*
         * read number of class ages concerned by this map
         */
        st.nextToken();
        int nbAgePerMap = new Integer(st.sval).intValue();
        agesMap[indexMap] = new int[nbAgePerMap];
        /*
         * read number of time step over the year concerned by this map
         */
        st.nextToken();
        int nbDtPerMap = new Integer(st.sval).intValue();
        seasonMap[indexMap] = new int[nbDtPerMap];
        /*
         * read the age classes concerned by this map
         */
        for (int k = 0; k < nbAgePerMap; k++) {
            st.nextToken();
            agesMap[indexMap][k] = new Integer(st.sval).intValue();
        }
        /*
         * read the time steps over the year concerned by this map
         */
        for (int k = 0; k < nbDtPerMap; k++) {
            st.nextToken();
            seasonMap[indexMap][k] = new Integer(st.sval).intValue() - 1;
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
            for (int m = 0; m < agesMap[indexMap].length; m++) {
                for (int n = 0; n < seasonMap[indexMap].length; n++) {
                    for (int h = 0; h < nStepYear; h++) {
                        int longevity = (int) Math.round((this.speciesLongevity[speciesMap[indexMap]]) * nStepYear);
                        if ((agesMap[indexMap][m] * nStepYear + h) < longevity) {
                            numMap[speciesMap[indexMap]][agesMap[indexMap][m] * nStepYear + h][seasonMap[indexMap][n]] = indexMap;
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
        for (int i = 0; i < grid.getNbLines(); i++) {
            for (int j = 0; j < grid.getNbColumns(); j++) {
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
        str.append(speciesName[speciesMap[numMap]]);
        str.append(" - Age class: ");
        for (int k = 0; k < agesMap[numMap].length; k++) {
            str.append(agesMap[numMap][k]);
            str.append(" ");
        }
        str.append("- Time step: ");
        for (int k = 0; k < seasonMap[numMap].length; k++) {
            str.append(seasonMap[numMap][k]);
            str.append(" ");
        }
        return str.toString();
    }

    public void readMigrationFile() {
        if (migrationFilename.equalsIgnoreCase("default")) {
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
                outOfZoneFile = new FileInputStream(resolveFile(migrationFilename));
            } catch (FileNotFoundException ex) {
                System.out.println("migration file doesn't exist: " + migrationFilename);
                System.exit(1);
            }

            Reader r = new BufferedReader(new InputStreamReader(outOfZoneFile));
            StreamTokenizer st = new StreamTokenizer(r);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.quoteChar(';');

            int nbSpOutOfZone, iSpec;
            int nbAgePerCase, nbDtPerCase;
            ageMigration = new int[nSpecies][];
            seasonMigration = new int[nSpecies][];
            migrationTempMortality = new float[nSpecies][];

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
                        ageMigration[iSpec] = new int[nbAgePerCase];
                        seasonMigration[iSpec] = new int[nbDtPerCase];
                        migrationTempMortality[iSpec] = new float[nbAgePerCase];

                        for (int k = 0; k < nbAgePerCase; k++) {
                            st.nextToken();
                            ageMigration[iSpec][k] = new Integer(st.sval).intValue();
                        }
                        for (int k = 0; k < nbDtPerCase; k++) {
                            st.nextToken();
                            seasonMigration[iSpec][k] = new Integer(st.sval).intValue() - 1;
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

    public void readAccessibilitiesFile(String accessFileName) {
        if (accessFileName.equalsIgnoreCase("default")) // if no accessiblity file is specified, all accessibilities are set to 1
        {
            for (int i = 0; i < nSpecies; i++) {
                nAccessStage[i] = 1;
                accessStageThreshold[i] = new float[nAccessStage[i] - 1];
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nSpecies][];
                for (int j = 0; j < nSpecies; j++) {
                    accessibilityMatrix[i][0][j] = new float[1];
                    /*
                     * phv 2011/11/18 set default access matrix to 0.8 for being
                     * compatible with Osmose 2.0
                     */
                    accessibilityMatrix[i][0][j][0] = 0.8f;
                }
            }
            for (int i = nSpecies; i < nSpecies + nPlankton; i++) {
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nSpecies][];
                for (int j = 0; j < nSpecies; j++) {
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
                if (!((new Integer(st.sval)).intValue() == nSpecies)) {
                    System.out.println("nb of species in accessibilities file not valid");
                } else {
                    st.nextToken();
                    if (!((new Integer(st.sval)).intValue() == nPlankton)) {
                        System.out.println("nb of other food groups in accessibilities file not valid");
                    } else {
                        for (int i = 0; i < nSpecies; i++) {
                            st.nextToken();
                            nAccessStage[i] = (new Integer(st.sval)).intValue();
                            accessStageThreshold[i] = new float[nAccessStage[i] - 1];
                            for (int j = 0; j < nAccessStage[i] - 1; j++) {
                                st.nextToken();
                                accessStageThreshold[i][j] = (new Float(st.sval)).floatValue();
                            }
                        }
                        for (int i = 0; i < nSpecies; i++) {
                            accessibilityMatrix[i] = new float[nAccessStage[i]][][];
                            for (int j = 0; j < nAccessStage[i]; j++) {
                                accessibilityMatrix[i][j] = new float[nSpecies][];
                                for (int k = 0; k < nSpecies; k++) {
                                    accessibilityMatrix[i][j][k] = new float[nAccessStage[k]];
                                    for (int m = 0; m < nAccessStage[k]; m++) {
                                        st.nextToken();
                                        accessibilityMatrix[i][j][k][m] = (new Float(st.sval)).floatValue();
                                    }
                                }
                            }
                        }
                        for (int i = nSpecies; i < nSpecies + nPlankton; i++) {
                            accessibilityMatrix[i] = new float[1][][];
                            accessibilityMatrix[i][0] = new float[nSpecies][];
                            for (int k = 0; k < nSpecies; k++) {
                                accessibilityMatrix[i][0][k] = new float[nAccessStage[k]];
                                for (int m = 0; m < nAccessStage[k]; m++) {
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

    public void readDietsOutputFile(String dietsConfigFileName) {
        nDietStage = new int[nSpecies];
        dietStageThreshold = new float[nSpecies][];
        if (dietsConfigFileName.equalsIgnoreCase("byDefault")) {
            System.out.println("Diets output by default");
            dietOutputMetrics = "default";
            for (int i = 0; i < nSpecies; i++) {
                nDietStage[i] = 1;
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
                if ((new Integer(st.sval)).intValue() == nSpecies) {
                    st.nextToken();
                    if ((new Integer(st.sval)).intValue() == nPlankton) {
                        st.nextToken();
                        if (st.sval.equalsIgnoreCase("age") || st.sval.equalsIgnoreCase("size")) {
                            dietOutputMetrics = st.sval;
                            for (int i = 0; i < nSpecies; i++) {
                                st.nextToken();
                                nDietStage[i] = (new Integer(st.sval)).intValue();
                                dietStageThreshold[i] = new float[nDietStage[i] - 1];
                                for (int j = 0; j < nDietStage[i] - 1; j++) {
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
        return ltlForcingClassName;
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
            String fileSeparator = System.getProperty("file.separator");
            outputPathName = inputPathName + fileSeparator + "output" + fileSeparator;
        }

        if (args.length > 2) {
            inputTxtName = args[2];
        } else {
            inputTxtName = "INPUT.txt";
        }

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
        return recordFrequency;
    }

    public boolean isCalibrationOutput() {
        return outputCalibration;
    }

    public boolean isTLOutput() {
        return outputTL;
    }

    public boolean isTLDistribOutput() {
        return outputTLSpectrum;
    }

    public boolean isDietOuput() {
        return outputDiet;
    }

    public boolean isMeanSizeOutput() {
        return outputMeanSize;
    }

    public boolean isSizeSpectrumOutput() {
        return outputSizeSpectrum;
    }

    public boolean isSizeSpectrumSpeciesOutput() {
        return outputSizeSpectrumSpecies;
    }

    public boolean isIncludeClassZero() {
        return outputClass0;
    }

    public String getDietOutputMetric() {
        return dietOutputMetrics;
    }

    public int getNumberTimeStepsPerYear() {
        return nStepYear;
    }

    public int getNYear() {
        return nYear;
    }

    public int getNSpecies() {
        return nSpecies;
    }

    public int getNPlankton() {
        return nPlankton;
    }

    public int getNumberLTLSteps() {
        return nLTLStep;
    }

    /**
     * @return the integrationDepth
     */
    public float getIntegrationDepth() {
        return integrationDepth;
    }

    public IGrid getGrid() {
        return grid;
    }

    public LTLForcing getForcing() {
        return forcing;
    }

    /**
     * @return the nSimulation
     */
    public int getNSimulation() {
        return nSimulation;
    }

    /**
     * @return the ltlForcingFilename
     */
    public String getLtlForcingFilename() {
        return ltlForcingFilename;
    }

    /**
     * @return the outputPathName
     */
    public String getOutputPathname() {
        return outputPathName;
    }

    /**
     * @return the outputFolder
     */
    public String getOutputFolder() {
        return outputFolder;
    }

    /**
     * @return the outputPrefix
     */
    public String getOutputPrefix() {
        return outputPrefix;
    }

    /**
     * @return the areasFilename
     */
    public String getAreasFilename() {
        return areasFilename;
    }

    public enum SpatialDistribution {

        RANDOM,
        MAPS,
        CONNECTIVITY;
    }
}
