/*
 *
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 *
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 *
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 *
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package fr.ird.osmose;

import fr.ird.osmose.resource.ResourceSpecies;
import fr.ird.osmose.background.BackgroundSpecies;
import fr.ird.osmose.util.version.VersionManager;
import fr.ird.osmose.grid.AbstractGrid;
import fr.ird.osmose.grid.NcGrid;
import fr.ird.osmose.output.AbstractOutputRegion;
import fr.ird.osmose.output.OutputRegion;
import fr.ird.osmose.output.Surveys;
import fr.ird.osmose.output.OutputWholeRegion;
import fr.ird.osmose.util.Separator;
import fr.ird.osmose.util.logging.OLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import ucar.ma2.InvalidRangeException;
import org.apache.commons.lang3.ArrayUtils;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;

/**
 * This class handles the Osmose configuration. It knows how to read Osmose
 * configuration files and to returns values of the parameters as String,
 * integer, float, double, path or arrays of these previous types. An Osmose
 * configuration file is a text based file. The extension of the file does not
 * matter. {@code Configuration} scans every line of the file looking for
 * parameters. Some lines are automatically discarded:
 * <ul>
 * <li>empty lines (regardless of blank or tab characters).</li>
 * <li>lines that start with a punctuation character, using
 * {@link java.util.regex.Pattern} {@code \p{Punct}} Punctuation: One of
 * {@code !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~}</li>
 * </ul>
 * For comments, it is recommended to start the line with {@code #} or
 * {@code //}.<br>
 * A parameter is formed by the juxtaposition of three elements: <b>key</b>,
 * <b>separator</b> and <b>value</b>.<br>
 * The key can be any sequence of characters, without blank or any special
 * characters (dot, hyphen and underscore are accepted). Example of keys:
 * <i>simulation.ncpu</i> or <i>predation.ingestion.rate.max.sp6</i>. Osmose
 * makes no difference between upper and lower case: <i>simulation.ncpu</i>,
 * <i>simulation.Ncpu</i>, <i>Simulation.nCPU</i>, <i>SIMULATION.NCPU</i>
 * designate the same key. Keys starting with <i>osmose.configuration.*</i> (*
 * being any sequence of characters that follow the same rules than any other
 * key) has a special meaning to {@code Configuration}. It means the value of
 * this parameter is the path of an other Osmose configuration file and the
 * parameters of this file are to be loaded in the current configuration. That
 * way, instead of having one big configuration file with all the parameters, it
 * is possible to split the parameters in as many files as the user wishes. This
 * process works recursively: one file contains one or several parameters
 * <i>osmose.configuration.*</i> that point to configuration files that may
 * contains one or several parameters <i>osmose.configuration.*</i>, and so on.
 * The <b>main configuration file</b> designates the one that is listed in the
 * file <i>filePath.txt</i> or given to {@code Osmose} as an input argument.
 * <br>
 * The separator can be any of the following characters:
 * <ul>
 * <li>equals =</li>
 * <li>semicolon ;</li>
 * <li>coma ,</li>
 * <li>colon :</li>
 * <li>tab \t</li>
 * </ul>
 * Parameters in the same configuration file can have different separators.
 * {@code Configuration} finds out what is the separator for each parameter.<br>
 * The value can be any sequence of characters (even empty).
 * {@code Configuration} does not try to interpret the value when it loads the
 * configuration files, it merely stores it in a {@code String} object. A value
 * can be served by {@code Configuration} as
 * <ul>
 * <li>a string</li>
 * <li>an integer</li>
 * <li>a float</li>
 * <li>a double</li>
 * <li>a boolean</li>
 * <li>an array of strings, {@code String[]}</li>
 * <li>an array of integers, {@code int[]}</li>
 * <li>an array of floats, {@code float[]}</li>
 * <li>an array of doubles, {@code double[]}</li>
 * <li>a resolved path</li>
 * <li></li>
 * </ul>
 * An array of values is a sequence of values with a separator in between:
 * <i>value1 separator value2 separator value3 separator value4</i>. Accepted
 * separators for an array of values are the same characters listed above. The
 * separator for an array of values can either be the same or distinct from the
 * separator between the key and the value. The following examples are valid
 * entries <i>movement.map0.season;0;1;2;3;4;5</i>
 * <i>movement.map0.season=0;1;2;3;4;5</i> <i>movement.map0.season = 0, 1, 2, 3,
 * 4, 5</i> <i>movement.map0.season : 0 ; 1 ; 2;3;4;5</i> and are equivalent for
 * {@code Configuration}. It can be summarise <i>key separator1 value1
 * separator2 value2 separator2 value3 separator2 value4</i> with separator1
 * either equal or different from separator2. *
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Configuration extends OLogger {

    ///////////////////////////////
    // Declaration of the variables
    ///////////////////////////////
    /**
     * List of all the parameters
     */
    private final HashMap<String, Parameter> parameters;
    /**
     * Name of the main configuration file.
     */
    final private String mainFilename;
    /**
     * Default parameter separator between key and value
     */
    private Separator defaultSeparator;
    /**
     * Path of the output directory.
     */
    private String outputPathname;
    /**
     * Output CSV separator
     *
     * @see fr.ird.osmose.util.Separator
     */
    private String outputSeparator;

    /** True if output files should be flushed any time they are written. */
    private boolean flushEnabled;

    /**
     * Number of CPUs allocated to {@code Osmose} for running the simulations
     * concurrently. Parameter <i>simulation.ncpu</i>
     */
    private int nCpu;
    /**
     * Number of species that are explicitly modelled. Parameter
     * <i>simulation.nspecies</i>
     */
    private int nSpecies;
    /**
     * Number of resource groups that force the simulation. Parameter
     * <i>simulation.nresource</i>
     */
    private int nResource;
    /**
     * Number of replicated simulations. It is the number of simulation to be run
     * with the same set of parameters. Parameter <i>simulation.nsimu</i>
     */
    private int nSimulation;
    /**
     * Number of time steps of the simulation. Parameter
     * <i>simulation.time.nstep</i>
     */
    private int nStep;
    /**
     * Number of time steps in one year. Time step {@code dt = 1. / nStepYear}
     * [year^-1]. Parameter <i>simulation.ndtPerYear</i>
     */
    private int nStepYear;
    /**
     * The number of new schools, per species, that are created every reproductive
     * event. {@code nSchool = new int[nSpecies]}. Parameter
     * <i>simulation.nschool</i> or <i>simulation.nschool.sp#</i>. This is a key
     * parameter of the IBM as it controls the number of individuals in the
     * population for each species. The bigger the better but the bigger the most
     * time consuming is the simulation.
     */
    private int[] nSchool;
    /**
     * The spatial grid of the simulation, {@link fr.ird.osmose.grid.AbstractGrid}.
     */
    private AbstractGrid grid;
    /**
     * Array of the species of the simulation.
     */
    private Species[] species;
    /**
     * Array of the resource species of the simulation.
     */
    private ResourceSpecies[] rscSpecies;

    /**
     * Number of species that are not explicitely modelled. Parameter
     * <i>simulation.nbackground</i>
     */
    private int nBackground;

    /**
     * Array of background species.
     */
    private BackgroundSpecies[] bkgSpecies; // barrier.n

    /**
     * True if the bioenergetic module should be activated.
     */
    private boolean bioenEnabled = false;

    /**
     * True if the genetic module should be activated.
     */
    private boolean geneticEnabled = false;

    /**
     * True if incoming fluxes should be used.
     */
    private boolean incomingFluxEnabled = false;
    /**
     * Number of fisheries.
     */
    private int nFishery;

    private List<AbstractOutputRegion> outputRegions;

    /**
     * Species index for focal, background and resource indexes.
     */
    private int[] focalIndex, bkgIndex, rscIndex;

    private boolean isEconomyEnabled;

    private NetcdfFileFormat ncOutVersion;

    /** True if fishing Mortality (v3 or v4) is on. */
    private boolean fishingMortalityEnabled = true;

    private boolean cutoffEnabled;
    private int recordFrequency;

    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;

    /**
     * Threshold size (cm) for age class zero. This parameter allows to discard
     * schools smaller that this threshold in the calculation of the indicators when
     * parameter <i>output.cutoff.enabled</i> is set to {@code true}. Parameter
     * <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffLength;

    /**
     * Year to start writing the outputs
     */
    private int yearOutput;

    /**
     * Whether the restart files should be written or not
     */
    private boolean writeRestart;

    /**
     * Record frequency for writing restart files, in number of time step.
     */
    private int restartFrequency;
    /**
     * Indicates whether the simulation starts from a restart file.
     */
    private boolean restart;
    /**
     * Number of years before writing restart files.
     */
    private int spinupRestart;

    /**
     * Whether to keep track of prey records during the simulation
     */
    private boolean preyRecord;

    private Nc4Chunking chunker;

    private int nFisheries;

    private String[] namesFisheries;

    ///////////////
    // Constructors
    ///////////////
    /**
     * Creates a new {@code Configuration}.
     *
     * @param mainFilename, the main configuration file
     * @param cmd,          the list of options/parameters set in command line
     */
    Configuration(String mainFilename, HashMap<String, String> cmd) {

        this.mainFilename = new File(mainFilename).getAbsolutePath();

        parameters = new HashMap<>();

        if (null != cmd) {
            // Add the parameters from the command line
            debug("Loading parameters from command line");
            for (Entry<String, String> argument : cmd.entrySet()) {
                Parameter parameter = new Parameter(argument.getKey(), argument.getValue());
                parameters.put(argument.getKey().toLowerCase(), parameter);
                debug(". " + parameter.toString());
            }
        }
    }

    /**
     * Creates a new {@code Configuration}.
     *
     * @param mainFilename, the main configuration file
     */
    Configuration(String mainFilename) {
        this(mainFilename, null);
    }

    ////////////////////////////
    // Definition of the methods
    ////////////////////////////
    /**
     * Load the parameters from the main configuration file and check whether the
     * configuration is up to date.
     *
     * @return {@code TRUE} if the configuration is up to date.
     */
    public boolean load() {
        // Load the parameters from the main configuration file
        loadParameters(mainFilename, 0);
        info("Configuration version: " + VersionManager.getInstance().getConfigurationVersion());

        // Check what is the default separator
        defaultSeparator = guessDefaultSeparator();

        return VersionManager.getInstance().isConfigurationUpToDate();
    }

    public boolean isPreyRecordEnabled() {
        return this.preyRecord;
    }

    private void checkPreyRecord() {

        preyRecord = false;
        List<String> outputList = this.findKeys("output.diet*enabled*");
        for(String param : outputList) {
            preyRecord = preyRecord || getBoolean(param);
        }

        outputList = this.findKeys("output.*tl*enabled");
        for(String param : outputList) {
            preyRecord = preyRecord || getBoolean(param);
        }

    }

    /**
     * Initialises the current configuration. Sets the values of the main variables
     * and creates the grid.
     *
     * @throws java.io.IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    public void init() throws IOException, InvalidRangeException {

        // barrier.n: reads the parameter that defines whether
        // the bioen module should be used.
        // String keybioen = "simulation.use.bioen";
        String keybioen = "simulation.bioen.enabled";
        this.bioenEnabled = this.getBoolean(keybioen);

        this.checkPreyRecord();

        this.geneticEnabled = false;
        if (this.bioenEnabled) {
            String key = "simulation.genetic.enabled";
            this.geneticEnabled = this.getBoolean(key);
        }

        String keyincom = "simulation.incoming.flux.enabled";
        this.incomingFluxEnabled = this.getBoolean(keyincom);

        // Output path
        outputPathname = getFile("output.dir.path");

        this.flushEnabled = getBoolean("output.flush.enabled");

        writeRestart = true;
        if (!this.isNull("output.restart.enabled")) {
            writeRestart = this.getBoolean("output.restart.enabled");
        } else {
            warning("Could not find parameter 'output.restart.enabled'. Osmose assumes it is true and a NetCDF restart file will be created at the end of the simulation (or more, depending on parameters 'simulation.restart.recordfrequency.ndt' and 'simulation.restart.spinup').");
        }

        restart = false;
        if (!this.isNull("simulation.restart.file")) {
            restart = true;
        }

        restartFrequency = Integer.MAX_VALUE;
        if (!this.isNull("output.restart.recordfrequency.ndt")) {
            restartFrequency = this.getInt("output.restart.recordfrequency.ndt");
        }

        spinupRestart = 0;
        if (!this.isNull("output.restart.spinup")) {
            spinupRestart = this.getInt("output.restart.spinup") - 1;
        }

        // Show the output folder
        info("Output folder set to " + outputPathname);

        // Read Output CSV separator
        Separator separator = Separator.COMA;
        if (!isNull("output.csv.separator")) {
            try {
                separator = Separator.valueOf(getString("output.csv.separator").toUpperCase());
            } catch (IllegalArgumentException ex) {
                warning("Failed to parse parameter output.csv.separator = " + getString("output.csv.separator")
                        + ". It must be either " + Separator.asList());
                separator = Separator.COMA;
            }
        }
        outputSeparator = separator.toString();
        info("Output CSV separator set to " + separator.name());

        // Number of CPUs allocated to this run
        if (!isNull("simulation.ncpu")) {
            nCpu = getInt("simulation.ncpu");
            // nCpu must range between 1 and system available processors
            nCpu = Math.min(Math.max(nCpu, 1), Runtime.getRuntime().availableProcessors());
        } else {
            nCpu = 1;
        }

        // Set the output NetCdf format for
        this.setOutputNcFormat();
        this.setChunker();

        // barrier.n: new way to count the number of species, resource and background
        // based on types.
        nSpecies = (int) this.findKeys("species.type.sp*").stream().filter((k) -> (getString(k).equals("focal")))
                .count();
        nResource = (int) this.findKeys("species.type.sp*").stream().filter(k -> getString(k).equals("resource"))
                .count();
        nBackground = (int) this.findKeys("species.type.sp*").stream().filter(k -> getString(k).equals("background"))
                .count();

        // Extract the species indexes for the focal, backgroud and resource species.
        this.focalIndex = this.findKeys("species.type.sp*").stream().filter(k -> getString(k).equals("focal"))
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".sp") + 3))).sorted().toArray();

        this.bkgIndex = this.findKeys("species.type.sp*").stream().filter(k -> getString(k).equals("background"))
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".sp") + 3))).sorted().toArray();

        this.rscIndex = this.findKeys("species.type.sp*").stream().filter(k -> getString(k).equals("resource"))
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".sp") + 3))).sorted().toArray();

        // Check that the number of focal species match the number of focal types
        int nSpecies_test = getInt("simulation.nspecies");
        if (nSpecies_test != nSpecies) {
            String errorMsg = String.format(
                    "Focal species may be badly defined. simulation.species=%d, number of focal types=%d",
                    nSpecies_test, nSpecies);
            error(errorMsg, null);
        }

        // Check that the number of resource species match the number of resource types
        int nResource_test = getInt("simulation.nresource");
        if (nResource_test != nResource) {
            String errorMsg = String.format(
                    "Resource species may be badly defined. simulation.nresource=%d, number of resource types=%d",
                    nResource_test, this.nResource);
            error(errorMsg, null);
        }

        nSimulation = getInt("simulation.nsimulation");

        nStepYear = getInt("simulation.time.ndtperyear");
        // PhV 20160203, new parameter simulation.time.nstep
        if (canFind("simulation.time.nstep")) {
            nStep = getInt("simulation.time.nstep");
        } else {
            // if simulation.time.nstep not defined, use old parameter simulation.time.nyear
            nStep = nStepYear * getInt("simulation.time.nyear");
        }

        nSchool = new int[nSpecies];
        if (findKeys("simulation.nschool.sp*").size() == nSpecies) {
            int cpt = 0;
            for (int i : this.focalIndex) {
                nSchool[cpt] = getInt("simulation.nschool.sp" + i);
                cpt++;
            }
        } else if (canFind("simulation.nschool")) {
            int n = getInt("simulation.nschool");
            for (int i = 0; i < nSpecies; i++) {
                nSchool[i] = n;
            }
        } else {
            for (int i = 0; i < nSpecies; i++) {
                nSchool[i] = 10;
            }
        }

        // Create the grid
        initGrid();

        // Create the species
        int cpt = 0;
        species = new Species[nSpecies];
        for (int fileIndex : this.focalIndex) {
            // Species are now instanciated from the fileIndex and the species index (cpt in
            // [0, nSpecies])
            species[cpt] = new Species(fileIndex, cpt);
            // Name must contain only alphanumerical characters
            if (!species[cpt].getName().matches("^[a-zA-Z0-9]*$")) {
                error("Species name must contain alphanumeric characters only. Please rename " + species[cpt].getName(),
                        null);
            }
            cpt++;
        }

        // Init resource groups
        rscSpecies = new ResourceSpecies[nResource];
        cpt = 0;
        for (int fileIndex : this.rscIndex) {
            // ResourceSpecies are now instanciated from the fileIndex (fileIndex) and the
            // species index (cpt in [0, nResources])
            rscSpecies[cpt] = new ResourceSpecies(fileIndex, cpt);
            // Name must contain only alphanumerical characters
            if (!rscSpecies[cpt].getName().matches("^[a-zA-Z0-9]*$")) {
                error("Resource name must contain alphanumeric characters only. Please rename "
                        + rscSpecies[cpt].getName(), null);
            }
            cpt++;
        }

        // barrier.n: add number of background species
        String key = "simulation.nbackground";
        int nBackground_test = 0;
        if (canFind(key)) {
            nBackground_test = getInt(key);
        }

        // check that the number of background species is consistent with the number of
        // background types
        if (nBackground_test != nBackground) {
            String errorMsg = String.format(
                    "Background species may be badly defined. simulation.nbackground=%d, number of background types=%d",
                    nBackground_test, nBackground);
            error(errorMsg, null);
        }

        // Initialisation of the Background array.
        cpt = 0;
        bkgSpecies = new BackgroundSpecies[nBackground];
        for (int fileIndex : this.bkgIndex) {
            // BackgroundSpecies are now instanciated from the fileIndex (fileIndex) and the
            // species index (cpt in [0, nResources])
            bkgSpecies[cpt] = new BackgroundSpecies(fileIndex, cpt);
            if (!bkgSpecies[cpt].getName().matches("^[a-zA-Z0-9]*$")) {
                error("Background species name must contain alphanumeric characters only. Please rename "
                        + bkgSpecies[cpt].getName(), null);
            }
            cpt++;
        }

        // Fisheries
        boolean fisheryEnabled = getBoolean("fisheries.enabled");
        this.isEconomyEnabled = getBoolean("economy.enabled");

        // true if fishingMortality is enabled or not (v3 or v4)
        if (!isNull("simulation.fishing.mortality.enabled")) {
            fishingMortalityEnabled = getBoolean("simulation.fishing.mortality.enabled");
        }

        // Init of fisheries at 0.
        nFishery = 0;

        if (fishingMortalityEnabled && fisheryEnabled) {

            // if fishery is on: read nfisheries
            nFishery = getInt("simulation.nfisheries");

            // compare with the number of fisheries names
            int nFisheryTest = findKeys("fisheries.name.fsh*").size();

            // Display warning message if new fishery enabled but no fishers
            if (nFishery != nFisheryTest) {
                warning("***************************************************");
                warning("The 'simulation.nfisheries' parameter is inconsistent "
                        + "with the number of 'fisheries.name.fsh*' parameters");
                warning("The program will stop");
                warning("***************************************************");
                System.exit(1);
            }
        }

        // output some output parameters
        this.initOutputParameters();

        // Output regions
        outputRegions = new ArrayList<>();
        // special case region0, the whole domain
        // in the following, we assume that if !canFind,
        // assumes that enabled by default.
        // If found, then we use its value to activate or not.
        if (!canFind("output.region.enabled.rg0") || getBoolean("output.region.enabled.rg0")) {
            if (findKeys("output.region.*.rg0").size() > 1) {
                error("Output region 0 corresponds to the whole grid and cannot be redefined.",
                        new IllegalArgumentException("Region0 cannot be overwritten"));
            }
            outputRegions.add(new OutputWholeRegion(0));
        }

        // list output regions
        HashSet<Integer> rg = new HashSet<>(findKeys("output.regions.*.rg*").stream()
                .map(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".rg") + 3)))
                .collect(Collectors.toList()));

        // remove rg0 (whole domain) that is handled separately
        rg.remove(0);

        // list output surveys
        HashSet<Integer> surveysIndex = new HashSet<>(findKeys("surveys.*.sr*").stream()
                .map(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".sr") + 3)))
                .collect(Collectors.toList()));

        // Do some test in order to ensure that surveys and output
        // regions have no duplicate indexes
        HashSet<Integer> total = new HashSet<>();
        total.addAll(rg);
        total.addAll(surveysIndex);
        if (total.size() != surveysIndex.size() + rg.size()) {
            String msg = String.format("The surveys and output regions must have different indexes.");
            throw new IllegalArgumentException(msg);
        }

        // Initialize output regions from indexes
        rg.forEach(index -> {
            if (!canFind("output.regions.enabled.rg" + index) || getBoolean("output.regions.enabled.rg" + index)) {
                outputRegions.add(new OutputRegion(index));
            }
        });

        // Initialize surveys regions from indexes
        surveysIndex.forEach(index -> {
            if (!canFind("surveys.enabled.sr" + index) || getBoolean("surveys.enabled.sr" + index)) {
                outputRegions.add(new Surveys(index));
            }
        });

        if (outputRegions.size() <= 0) {
            // phv 20191203, should throw an error instead?
            warning("No output region defined");
        }

        // init output regions
        outputRegions.forEach(region -> {
            region.init();
        });

        // Year to start writing the outputs
        yearOutput = this.getInt("output.start.year");

        this.initFisheries();

    }

    public int getRestartFrequency() {
        return this.restartFrequency;
    }

    public int getSpinupRestart() {
        return this.spinupRestart;
    }

    /**
     * Checks whether the simulation started from a restart file.
     *
     * @return {@code true} if the simulation started from a restart file
     */
    public boolean isRestart() {
        return restart;
    }

    public boolean isWriteRestartEnabled() {
        return this.writeRestart;
    }

    public int getYearOutput() {
        return this.yearOutput;
    }

    public List<AbstractOutputRegion> getOutputRegions() {
        return outputRegions;
    }

    public int getNFishery() {
        return nFishery;
    }

    public boolean isFisheryEnabled() {
        return nFishery > 0;
    }

    public boolean isFishingMortalityEnabled() {
        return this.fishingMortalityEnabled;
    }

    public boolean isEconomyEnabled() {
        return isEconomyEnabled;
    }

    /**
     * Returns {@code true} if Bioenergetics module is enabled.
     *
     * @return {@code true} if Bioenergetics module is enabled.
     */
    public boolean isBioenEnabled() {
        return this.bioenEnabled;
    }

    /**
     * Returns true of genetic module is used or not.
     *
     *
     * @return
     */
    public boolean isGeneticEnabled() {
        return this.geneticEnabled;
    }

    /**
     * Get a species
     *
     * @param index, the index of the species
     * @return the species at index {@code index}
     */
    public Species getSpecies(int index) {
        return species[index];
    }

    /**
     * Get a species
     *
     * @param name,
     *            the name of the species
     * @return the {@code name} species.
     */
    public Species getSpecies(String name) {
        for (int i = 0; i < getNSpecies(); i++) {
            if (getSpecies(i).getName().equalsIgnoreCase(name)) {
                return getSpecies(i);
            }
        }
        return null;
    }

    /**
     * Gets the specified resource group.
     *
     * @param index, the index of the resource group.
     * @return the resource group with given index
     */
    public ResourceSpecies getResourceSpecies(int index) {
        return rscSpecies[index];
    }

    /**
     * Creates a new grid.
     *
     * @see fr.ird.osmose.grid.IGrid
     */
    public void initGrid() {

        // barrier.n
        // Only NetCDF grid remaining
        grid = new NcGrid();
        grid.init();

    }

    /**
     * Loads recursively the parameters from the configuration file. The function
     * scans one by one the lines of the configuration file. A line is discarded
     * when it matches any of these criteria: it is empty, it contains only blank
     * and/or tab characters, it starts with a punctuation character (punctuation:
     * One of {@code !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~})<br>
     * Any other lines are expected to be parameters formed as <i>key separator
     * value</i>. Refer to the documentation at the beginning of the class for
     * details about the parameters.<br>
     * A parameter whose key start with <i>osmose.configuration.</i> means the value
     * designate an other configuration file that has to be loaded in the current
     * {@code Configuration}. The function {@code loadProperties} is called
     * recursively.
     *
     * @param filename, the configuration file to be loaded
     * @param depth,    an integer that reflects the level of recursivity of the
     *                  function. Zero for the main configuration file, one for a
     *                  file loaded from the main configuration file, etc.
     */
    private void loadParameters(String filename, int depth) {

        BufferedReader bfIn;
        // Open the buffer
        try {
            bfIn = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException ex) {
            error("Could not find Osmose configuration file: " + filename, ex);
            return;
        }
        StringBuilder msg = new StringBuilder();
        StringBuilder space = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            space.append(". ");
        }
        msg.append(space);
        msg.append("Loading parameters from ");
        msg.append(filename);
        info(msg.toString());
        space.append(". ");

        // Read it
        String line = null;
        int iline = 1;
        try {
            while ((line = bfIn.readLine()) != null) {
                line = line.trim();
                if (!startsWithSymbol(line) & !(line.length() <= 1)) {
                    Parameter entry = new Parameter(iline, filename);
                    entry.parse(line);
                    if (parameters.containsKey(entry.key)) {
                        Parameter existingParam = parameters.get(entry.key);
                        if ((existingParam.source.compareTo("command line") != 0) && (entry.key.contains("species.name") || entry.key.contains("species.type"))) {
                            String errorMsg = String.format("%s has already been defined.", entry.key);
                            error(errorMsg, null);
                        } else {
                            warning("{0}Osmose will ignore parameter {1}", new Object[] { space, entry });
                            warning("{0}Parameter already defined {1}",
                                    new Object[] { space, parameters.get(entry.key) });
                        } // end of test on parameter name
                    } else {
                        parameters.put(entry.key, entry);
                        debug(space + entry.toString());
                        if (entry.key.startsWith("osmose.configuration")) {
                            loadParameters(getFile(entry.key), depth + 1);
                        }
                    }
                }
                iline++;
            }
        } catch (IOException ex) {
            error("Error loading parameters from " + filename + " at line " + iline + " " + line, ex);
        }
    }

    public void refresh() {
        info("Reloading parameters...");
        // Clear current lists of parameters
        parameters.clear();
        // Reload parameters
        loadParameters(mainFilename, 0);
        info("Configuration version: " + VersionManager.getInstance().getConfigurationVersion());

    }

    /**
     * Checks whether a String start with a punctuation character. It uses
     * {@link java.util.regex.Pattern} {@code \p{Punct}} Punctuation: One of
     * {@code !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~}
     *
     * @param value, the {@code String} to be checked
     * @return true if {@code value} starts with one of
     *         {@code !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~}
     */
    private boolean startsWithSymbol(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        Pattern p = Pattern.compile("(^\\p{Punct})");
        Matcher m = p.matcher(value);
        return m.find();
    }

    /**
     * Check whether parameter 'key' has 'null' value. The function returns
     * {@code true} in several cases: the parameter does not exist, the value of the
     * parameter is empty or the value of the parameter is set to "null".
     *
     * @param key, the key of the parameter
     * @return {@code true} if the parameter is either null, empty or does not exist
     */
    public boolean isNull(String key) {
        Parameter param = parameters.get(key.toLowerCase());
        return (null == param) || param.value.isEmpty() || param.value.equalsIgnoreCase("null");
    }

    /**
     * Check whether the parameter exists, no matter what the value is.
     *
     * @param key, the key of the parameter
     * @return {@code true} if the parameter exists.
     */
    public final boolean canFind(String key) {
        return parameters.containsKey(key.toLowerCase());
    }

    /**
     * Find all the parameters whose key matches the filter given as argument. The
     * filter accepts the following meta-character: "?" for any single character and
     * "*" for any String.
     *
     * @see fr.ird.osmose.util.Properties#getKeys(java.lang.String) for details
     *      about how the filter works.
     * @param filter
     * @return
     */
    public List<String> findKeys(String filter) {

        // Add \Q \E around substrings of fileMask that are not meta-characters
        String regexpPattern = filter.replaceAll("[^\\*\\?]+", "\\\\Q$0\\\\E");
        // Replace all "*" by the corresponding java regex meta-characters
        regexpPattern = regexpPattern.replaceAll("\\*", ".*");
        // Replace all "?" by the corresponding java regex meta-characters
        regexpPattern = regexpPattern.replaceAll("\\?", ".");

        // List the keys and select the ones that match the filter
        List<String> filteredKeys = new ArrayList<>();
        for (String key : parameters.keySet()) {
            if (key.matches(regexpPattern)) {
                filteredKeys.add(key);
            }
        }
        return filteredKeys;
    }

    public String printParameter(String key) {
        return getParameter(key).toString();
    }

    /**
     * Returns the parameter designated by its key.
     *
     * @param key, the key of the parameter
     * @throws NullPointerException if the parameter is not found.
     * @return the parameter as a {@link Parameter}
     */
    private Parameter getParameter(String key) {
        String lkey = key.toLowerCase();
        if (parameters.containsKey(lkey)) {
            return parameters.get(lkey);
        } else {
            error("Could not find parameter " + key, new NullPointerException("Parameter " + key + " not found "));
        }
        return null;
    }

    /**
     * Returns the value of the specified parameter as a {@code String}
     *
     * @param key, the key of the parameter
     * @throws NullPointerException if the parameter is not found.
     * @return the value of the parameter as a {@code String}
     */
    final public String getString(String key) {
        return getParameter(key).value;
    }

    /**
     * Returns the path of the configuration file that contains the specified
     * parameter.
     *
     * @param key, the key of the parameter
     * @return the path of the configuration file that contains the parameter.
     */
    final public String getSource(String key) {
        return parameters.get(key.toLowerCase()).source;
    }

    /**
     * Returns the specified parameter as a path resolved again the path of the
     * configuration file that defined the parameter.
     *
     * @param key, the key of the parameter @return, the parameter as a path
     *             resolved again the path of the configuration file that defined
     *             the parameter.
     */
    public String getFile(String key) {
        return resolve(getString(key), getSource(key));
    }

    /**
     * Returns the specified parameter as an array of strings, {@code String[]}.
     *
     * @param key, the key of the parameter
     * @return the parameter as a {@code String[]}
     */
    public String[] getArrayString(String key) {
        String value = getString(key);
        String[] values = value.split(Separator.guess(value, Separator.SEMICOLON).toString());
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        return values;
    }

    /**
     * Returns the specified parameter as an integer.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the value of the parameter cannot be parsed
     *                               as an integer.
     * @return the parameter as an integer
     */
    public int getInt(String key) {
        String s = getString(key);
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            error("Could not convert to Integer parameter " + getParameter(key), ex);
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Returns the specified parameter as a float.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the value of the parameter cannot be parsed
     *                               as a float.
     * @return the parameter as a float
     */
    public float getFloat(String key) {
        String s = getString(key);
        try {
            return Float.valueOf(s);
        } catch (NumberFormatException ex) {
            error("Could not convert to Float parameter " + getParameter(key), ex);
        }
        return Float.NaN;
    }

    /**
     * Returns the specified parameter as a double.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the value of the parameter cannot be parsed
     *                               as a double.
     * @return the parameter as a double
     */
    public double getDouble(String key) {
        String s = getString(key);
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException ex) {
            error("Could not convert to Double parameter " + getParameter(key), ex);
        }
        return Double.NaN;
    }

    /**
     * Returns the specified parameter as a double.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the value of the parameter cannot be parsed
     *                               as a double.
     * @return the parameter as a double
     */
    public long getLong(String key) {
        String s = getString(key);
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException ex) {
            error("Could not convert to Double parameter " + getParameter(key), ex);
        }
        return Long.MIN_VALUE;
    }

    /**
     * Returns the specified parameter as a boolean.
     *
     * @param key,     the key of the parameter
     * @param warning, send a warning if the key cannot be found
     * @throws NumberFormatException if the value of the parameter cannot be parsed
     *                               as a boolean.
     * @return the parameter as a boolean
     */
    public boolean getBoolean(String key, boolean warning) {
        if (canFind(key)) {
            try {
                return Boolean.valueOf(getString(key));
            } catch (NumberFormatException ex) {
                error("Could not convert to Boolean parameter " + getParameter(key), ex);
            }
        } else if (warning) {
            warning("Could not find Boolean parameter " + key + ". Osmose assumes it is false.");
        }

        return false;
    }

    /**
     * Returns the specified parameter as a boolean.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the value of the parameter cannot be parsed
     *                               as a boolean.
     * @return the parameter as a boolean
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, true);
    }

    /**
     * Returns the specified parameter as an array of integers, {@code int[]}.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the values of the parameter cannot be parsed
     *                               as an integer.
     * @return the parameter as a {@code int[]}
     */
    public int[] getArrayInt(String key) {
        String[] as = getArrayString(key);
        try {
            int[] ai = new int[as.length];
            for (int i = 0; i < ai.length; i++) {
                ai[i] = Integer.valueOf(as[i]);
            }
            return ai;
        } catch (NumberFormatException ex) {
            error("Could not convert to array of Integer parameter " + getParameter(key), ex);
        }
        return null;
    }

    /**
     * Returns the specified parameter as an array of floats, {@code float[]}.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the values of the parameter cannot be parsed
     *                               as a float.
     * @return the parameter as a {@code float[]}
     */
    public float[] getArrayFloat(String key) {
        String[] as = getArrayString(key);
        try {
            float[] af = new float[as.length];
            for (int i = 0; i < af.length; i++) {
                af[i] = Float.valueOf(as[i]);
            }
            return af;
        } catch (NumberFormatException ex) {
            error("Could not convert to array of Float parameter " + getParameter(key), ex);
        }
        return null;
    }

    /**
     * Returns the specified parameter as an array of doubles, {@code double[]}.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the values of the parameter cannot be parsed
     *                               as a double.
     * @return the parameter as a {@code double[]}
     */
    public double[] getArrayDouble(String key) {
        String[] as = getArrayString(key);
        try {
            double[] ad = new double[as.length];
            for (int i = 0; i < ad.length; i++) {
                ad[i] = Double.valueOf(as[i]);
            }
            return ad;
        } catch (NumberFormatException ex) {
            error("Could not convert to array of Double parameter " + getParameter(key), ex);
        }
        return null;
    }

    /**
     * Resolves a file path against the the provided path. If filename is a
     * directory the function ensures the path ends with a separator.
     *
     * @param filename,   the file path to resolve
     * @param relativeTo, the path against the file must be resolved
     * @return the resolved file path
     */
    public String resolve(String filename, String relativeTo) {
        String pathname = filename;
        try {
            File file = new File(relativeTo);
            pathname = new File(file.toURI().resolve(filename)).getCanonicalPath();
        } catch (Exception ex) {
            // do nothing, just return the argument
        }
        if (new File(pathname).isDirectory() && !pathname.endsWith(File.separator)) {
            pathname += File.separator;
        }
        return pathname;
    }

    /**
     * Returns the output path. Parameter <i>output.dir.path</i> or second argument
     * of the command line option.
     *
     * @return the output path
     */
    public String getOutputPathname() {
        return outputPathname;
    }

    public String getOutputSeparator() {
        return outputSeparator;
    }

    public String getDefaultSeparator() {
        return defaultSeparator.toString();
    }

    /**
     * Returns the number of CPUs allocated for running the simulations
     * concurrently. Parameter<i>simulation.ncpu</i>
     *
     * @return the number of CPUs allocated for running the simulations concurrently
     */
    public int getNCpu() {
        return nCpu;
    }

    /**
     * Returns the number of species. Parameter <i>simulation.nspecies</i>
     *
     * @return the number of species.
     */
    public int getNSpecies() {
        return nSpecies;
    }

    /**
     * Returns the number of resource species. Parameter <i>simulation.nresource</i>
     *
     * @return the number of resource groups.
     */
    public int getNRscSpecies() {
        return nResource;
    }

    /**
     * Returns the number of replicated simulations. (i.e. simulations with same set
     * of parameters). Parameter <i>simulation.nsimu</i>
     *
     * @return the nSimulation
     */
    public int getNSimulation() {
        return nSimulation;
    }

    /**
     * Returns the number of time steps of the simulation. Parameter
     * <i>simulation.time.nstep</i>
     *
     * @return the number of years of the simulation
     */
    public int getNStep() {
        return nStep;
    }

    /**
     * Returns the number of time steps per year. Parameter
     * <i>simulation.ndtPerYear</i>
     *
     * @return the number of time steps per year
     */
    public int getNStepYear() {
        return nStepYear;
    }

    /**
     * Returns the number of new schools, for the specified species, that are
     * created every reproductive event. Parameter <i>simulation.nschool</i> or
     * <i>simulation.nschool.sp#</i>.
     *
     * @param iSpecies, the index of the species
     * @return the number of new schools that are created at every reproductive
     *         event for species at index {@code iSpecies}
     */
    public int getNSchool(int iSpecies) {
        return nSchool[iSpecies];
    }

    /**
     * Returns the grid of the model.
     *
     * @return the grid of the model.
     */
    public AbstractGrid getGrid() {
        return grid;
    }

    /**
     * Guess the default parameter separator (between key and value) in the set of
     * configuration files. Osmose takes as default separator the one that is the
     * most widely used in the current configuration. The function scans all the
     * parameters, counts the occurrences of every type of separator and returns the
     * one that with the highest count.
     *
     * @return the default parameter separator.
     */
    private Separator guessDefaultSeparator() {

        StringBuilder sbSeparators = new StringBuilder();
        for (Parameter parameter : parameters.values()) {
            sbSeparators.append(parameter.keySeparator);
        }
        String separators = sbSeparators.toString();

        Separator defaultSep = Separator.EQUALS;
        int nSep1 = 0;
        for (Separator separator : Separator.values()) {
            int nSep2 = separators.length() - separators.replaceAll(separator.toString(), "").length();
            if (nSep2 > nSep1) {
                nSep1 = nSep2;
                defaultSep = separator;
            }
        }

        return defaultSep;
    }

    private void initOutputParameters() {

        recordFrequency = getInt("output.recordfrequency.ndt");

        cutoffAge = new float[nSpecies];
        cutoffLength = new float[nSpecies];

        cutoffEnabled = getBoolean("output.cutoff.enabled");
        if (cutoffEnabled) {
            int cpt = 0;
            for (int iSpec : getFocalIndex()) {
                // If cutoff enabled, look for cutoff age
                if(!isNull("output.cutoff.age.sp" + iSpec)) {
                    cutoffAge[cpt] = getFloat("output.cutoff.age.sp" + iSpec);
                }

                // If cutoff enabled, look for cutoff size
                if(!isNull("output.cutoff.size.sp" + iSpec)) {
                    cutoffLength[cpt] = getFloat("output.cutoff.size.sp" + iSpec);
                }
                cpt++;
            }
        }
    }

    public String getMainFile() {
        return mainFilename;
    }

    /**
     * Returns the number of species. Parameter <i>simulation.nspecies</i>
     *
     * @return the number of species.
     */
    public int getNBkgSpecies() {
        return nBackground;
    }

    /**
     * Returns the number of all species. Parameter <i>simulation.nspecies</i>
     *
     * @return the number of species.
     */
    public int getNAllSpecies() {
        return this.nBackground + this.nSpecies + this.nResource;
    }

    /**
     * Returns the number of all species. Parameter <i>simulation.nspecies</i>
     *
     * @return the number of species.
     */
    public int getNPredSpecies() {
        return this.nBackground + this.nSpecies;
    }

    /**
     * Get a species
     *
     * @param index, the index of the species
     * @return the species at index {@code index}
     */
    public BackgroundSpecies getBkgSpecies(int index) {
        return bkgSpecies[index];
    }

    /**
     * Returns {@code true} if incoming fluxes is enabled.
     *
     * @return {@code true} if incoming fluxes is enabled.
     */
    public boolean isIncomingFluxEnabled() {
        return this.incomingFluxEnabled;
    }

    public float[] getCutoffLength() {
        return this.cutoffLength;
    }

    public float[] getCutoffAge() {
        return this.cutoffAge;
    }

    public boolean isCutoffEnabled() {
        return this.cutoffEnabled;
    }

    public int getRecordFrequency() {
        return this.recordFrequency;
    }

    /**
     * Inner class that represents a parameter in the configuration file.
     * {@code Configuration} parses the configuration file line by line. When the
     * line is not discarded (refer to function
     * {@link #loadParameters(java.lang.String, int)} for details about discarded
     * lines), it assumes it is a parameter (formed as <i>key separator value</i> or
     * <i>key separator1 value1 separator2 value2 separator2 value3 separator2
     * value4</i>) and creates a new {@code Parameter} object.
     */
    private class Parameter {

        /**
         * Path of the configuration file containing the parameter.
         */
        private final String source;
        /**
         * The line of the parameter in the configuration file.
         */
        private final int iline;
        /**
         * The key of the parameter.
         */
        private String key;
        /**
         * The value of the parameter.
         */
        private String value;
        /**
         * The separator between key and value. <i>key keySeparator value</i>
         */
        private String keySeparator;
        /**
         * The separator between the values of the parameter. <i>key keySeparator value1
         * valueSeparator value2 valueSeparator value3</i>
         */
        private String valueSeparator;

        /**
         * Create a new parameter out of the given line.
         *
         * @param iline,  the line of the parameter in the configuration file
         * @param source, the path of the configuration file
         */
        Parameter(int iline, String source) {
            this.iline = iline;
            this.source = source;
        }

        /**
         * Create a new parameter from the command line
         *
         * @param key,   the key of the parameter
         * @param value, the value of the parameter
         */
        Parameter(String key, String value) {
            this.key = key;
            this.value = value;
            this.source = "command line";
            this.iline = -1;
        }

        /**
         * Parse the line as a parameter. It follows the following steps: guess the
         * separator between key and value. Splits the line into a key and a value.
         * Guess the value separator in case it is actually an array of values.
         *
         * @param line, the line to be parsed as a parameter
         */
        private void parse(String line) {
            key = value = null;
            keySeparator = Separator.guess(line, Separator.EQUALS).toString();
            split(line);
            valueSeparator = Separator.guess(value, Separator.SEMICOLON).toString();
            value = clean(value);
        }

        /**
         * Cleans the value of the parameter. Trims the value (removes leading and
         * trailing blank characters), and removes any trailing separators.
         *
         * @param value, the value to be cleaned
         * @return a copy of the value, trimmed and without any trailing separator.
         */
        private String clean(String value) {
            String cleanedValue = value.trim();
            if (cleanedValue.endsWith(valueSeparator)) {
                cleanedValue = cleanedValue.substring(0, cleanedValue.lastIndexOf(valueSeparator));
                return clean(cleanedValue);
            } else {
                return cleanedValue;
            }
        }

        /**
         * Splits the given line into a key and a value, using the {@code keySeparator}.
         * Sends and error message if the line cannot be split.
         *
         * @param line, the line to be split into a key and a value.
         */
        private void split(String line) {

            // make sure the line contains at least one semi-colon (key;value)
            if (!line.contains(keySeparator)) {
                error("Failed to split line " + iline + " " + line + " as key" + keySeparator + "value (from " + source
                        + ")", null);
            }
            // extract the key
            key = line.substring(0, line.indexOf(keySeparator)).toLowerCase().trim();
            // extract the value
            try {
                value = line.substring(line.indexOf(keySeparator) + 1).trim();
            } catch (StringIndexOutOfBoundsException ex) {
                // set value to "null"
                value = "null";
            }
            // set empty value to "null"
            if (value.isEmpty()) {
                value = "null";
            }
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append(key);
            str.append(" = ");
            str.append(value);
            str.append(" (from ");
            str.append(source);
            if (iline >= 0) {
                str.append(" line ");
                str.append(iline);
            }
            str.append(")");
            return str.toString();
        }
    }

    /**
     * Returns the index of the focal species.
     *
     * @param i Index of the focal species
     * @return The species index of the ith focal species.
     */
    public int getFocalIndex(int i) {
        return this.focalIndex[i];
    }

    /**
     * Returns the index of the background species.
     *
     * @param i Index of the background species
     * @return The species index of the ith background species.
     */
    public int getBkgIndex(int i) {
        return this.bkgIndex[i];
    }

    /**
     * Returns the index of the resource species.
     *
     * @param i Index of the resource species
     * @return The species index of the ith resource species.
     */
    public int getRscIndex(int i) {
        return this.rscIndex[i];
    }

    /**
     * Returns the index of the focal species.
     *
     * @param i Index of the focal species
     * @return The species index of the ith focal species.
     */
    public int[] getFocalIndex() {
        return this.focalIndex;
    }

    public boolean isFlushEnabled() {
        return this.flushEnabled;
    }

    /**
     * Returns the index of the background species.
     *
     * @param i Index of the background species
     * @return The species index of the ith background species.
     */
    public int[] getBackgroundIndex() {
        return this.bkgIndex;
    }

    /**
     * Returns the index of the resource species.
     *
     * @param i Index of the resource species
     * @return The species index of the ith resource species.
     */
    public int[] getResourceIndex() {
        return this.rscIndex;
    }

    /**
     * Recovers the indexes of the species that can be fished.
     *
     * Returns the concatenated array of focal and background species indexes.
     *
     * @return
     */
    public int[] getPredatorIndex() {
        return (ArrayUtils.addAll(this.focalIndex, this.bkgIndex));
    }

    public int[] getPreyIndex() {
        return (ArrayUtils.addAll(this.bkgIndex, this.rscIndex));
    }

    public int[] getAllIndex() {
        return (ArrayUtils.addAll(ArrayUtils.addAll(this.focalIndex, this.bkgIndex), this.rscIndex));
    }

    /** Recover the output NetCDF version */
    public NetcdfFileFormat getNcOutVersion() {
        return ncOutVersion;
    }

    public int getNYears() {
        return this.getNStep() / this.getNStepYear();
    }

    private void setOutputNcFormat() {

        // Control of the NetCdf output version from a configuration file.
        // If not provided, NetCdf4 is used.
        ncOutVersion = NetcdfFileFormat.NETCDF4;
        if (!isNull("output.netcdf.format")) {
            String outputFormat = getString("output.netcdf.format");
            switch (outputFormat) {
                case "ncstream":
                    ncOutVersion = NetcdfFileFormat.NCSTREAM;
                break;
                case "netcdf3":
                    ncOutVersion = NetcdfFileFormat.NETCDF3;
                    break;
                case "netcdf3_64bit_data":
                    ncOutVersion = NetcdfFileFormat.NETCDF3_64BIT_DATA;
                    break;
                case "netcdf3_64bit_offset":
                    ncOutVersion = NetcdfFileFormat.NETCDF3_64BIT_OFFSET;
                    break;
                case "netcdf4":
                    ncOutVersion = NetcdfFileFormat.NETCDF4;
                    break;
                case "netcdf4_classic":
                    ncOutVersion = NetcdfFileFormat.NETCDF4_CLASSIC;
                    break;
                default:
                    ncOutVersion = NetcdfFileFormat.NETCDF3;
                    break;
            }
        }
    }

    public void setChunker() {

        int deflateLevel = 0;
        boolean shuffle = false;

        // if netcdf4 output, check if deflate level is set.
        String key = "output.netcdf.deflate.level";
        if (!this.isNull(key)) {
            deflateLevel = getInt(key);
        }

        key = "output.netcdf.shuffle";
        // we read whether shuffle parameter is on.
        if (!this.isNull(key)) {
            shuffle = getBoolean(key, false);
        }

        Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.none;
        key = "output.netcdf.chunk";
        if (!this.isNull(key)) {
            switch (getString(key)) {
                case "standard":
                    strategy = Nc4Chunking.Strategy.standard;
                    break;
                case "grib":
                    strategy = Nc4Chunking.Strategy.grib;
                    break;
                case "none":
                    strategy = Nc4Chunking.Strategy.none;
                    break;
                default:
                    strategy = Nc4Chunking.Strategy.none;
            }
        }

        this.chunker = Nc4ChunkingStrategy.factory(strategy, deflateLevel, shuffle);
    }

    public Nc4Chunking getChunker() {
        return this.chunker;
    }

    private void initFisheries() {

        if (this.isFisheryEnabled()) {
            nFisheries = this.getNFishery();
            namesFisheries = new String[nFisheries];
            for (int iFishery = 0; iFishery < nFisheries; iFishery++) {
                namesFisheries[iFishery] = String.format("fishery%03d", iFishery);
            }
        } else {
            nFisheries = this.getNSpecies();
            namesFisheries = new String[nFisheries];
            for (int iSpecies = 0; iSpecies < nFisheries; iSpecies++) {
                namesFisheries[iSpecies] = String.format("fishery%03d", iSpecies);
            }
        }
    }

    public String[] getFisheriesNames() {
        return this.namesFisheries;
    }

    public int getNFisheries() {
        return this.nFisheries;
    }

}
