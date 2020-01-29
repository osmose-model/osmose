/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose;

import fr.ird.osmose.resource.ResourceSpecies;
import fr.ird.osmose.background.BackgroundSpecies;
import fr.ird.osmose.util.version.VersionManager;
import fr.ird.osmose.grid.AbstractGrid;
import fr.ird.osmose.output.OutputRegion;
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
 * A parameter is formed by the juxtaposition of three elements:
 * <b>key</b>,
 * <b>separator</b>
 * and <b>value</b>.<br>
 * The key can be any sequence of characters, without blank or any special
 * characters (dot, hyphen and underscore are accepted). Example of keys:
 * <i>simulation.ncpu</i> or <i>predation.ingestion.rate.max.sp6</i>. Osmose
 * makes no difference between upper and lower case:
 * <i>simulation.ncpu</i>, <i>simulation.Ncpu</i>,
 * <i>Simulation.nCPU</i>, <i>SIMULATION.NCPU</i> designate the same key. Keys
 * starting with <i>osmose.configuration.*</i> (* being any sequence of
 * characters that follow the same rules than any other key) has a special
 * meaning to {@code Configuration}. It means the value of this parameter is the
 * path of an other Osmose configuration file and the parameters of this file
 * are to be loaded in the current configuration. That way, instead of having
 * one big configuration file with all the parameters, it is possible to split
 * the parameters in as many files as the user wishes. This process works
 * recursively: one file contains one or several parameters
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
     * Number of replicated simulations. It is the number of simulation to be
     * run with the same set of parameters. Parameter <i>simulation.nsimu</i>
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
     * The number of new schools, per species, that are created every
     * reproductive event. {@code nSchool = new int[nSpecies]}. Parameter
     * <i>simulation.nschool</i> or <i>simulation.nschool.sp#</i>. This is a key
     * parameter of the IBM as it controls the number of individuals in the
     * population for each species. The bigger the better but the bigger the
     * most time consuming is the simulation.
     */
    private int[] nSchool;
    /**
     * The spatial grid of the simulation,
     * {@link fr.ird.osmose.grid.AbstractGrid}.
     */
    private AbstractGrid grid;
    /**
     * Array of the species of the simulation.
     */
    private HashMap<Integer, Species> species;
    /**
     * Array of the resource species of the simulation.
     */
    private HashMap<Integer, ResourceSpecies> rscSpecies;

    /**
     * Number of species that are not explicitely modelled. Parameter
     * <i>simulation.nbackground</i>
     */
    private int nBackground;

    /**
     * Array of background species.
     */
    private HashMap<Integer, BackgroundSpecies> bkgSpecies; // barrier.n

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

    private List<OutputRegion> outputRegions;

    /**
     * Species index for focal, background and resource indexes.
     */
    private int[] focalIndex, bkgIndex, rscIndex;

///////////////
// Constructors
///////////////
    /**
     * Creates a new {@code Configuration}.
     *
     * @param mainFilename, the main configuration file
     * @param cmd, the list of options/parameters set in command line
     */
    Configuration(String mainFilename, HashMap<String, String> cmd) {

        this.mainFilename = new File(mainFilename).getAbsolutePath();

        parameters = new HashMap();

        if (null != cmd) {
            // Add the parameters from the command line
            debug("Loading parameters from command line");
            for (Entry<String, String> argument : cmd.entrySet()) {
                Parameter parameter = new Parameter(argument.getKey(), argument.getValue());
                parameters.put(argument.getKey(), parameter);
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
     * Load the parameters from the main configuration file and check whether
     * the configuration is up to date.
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

    /**
     * Initialises the current configuration. Sets the values of the main
     * variables and creates the grid.
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

        String key = "simulation.genetic.enabled";
        geneticEnabled = this.getBoolean(key);

        String keyincom = "simulation.incoming.flux.enabled";
        this.incomingFluxEnabled = this.getBoolean(keyincom);

        // Output path
        outputPathname = getFile("output.dir.path");
        // barrier.n: outputPathname has been set unresolved,
        // so that outputs are not saved in the input directory.
        //outputPathname = getString("output.dir.path");
        info("Output folder set to " + outputPathname);

        // Read Output CSV separator
        Separator separator = Separator.COMA;
        if (!isNull("output.csv.separator")) {
            try {
                separator = Separator.valueOf(getString("output.csv.separator").toUpperCase());
            } catch (IllegalArgumentException ex) {
                warning("Failed to parse parameter output.csv.separator = " + getString("output.csv.separator") + ". It must be either " + Separator.asList());
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

        // barrier.n: new way to count the number of species, resource and background based on types.
        nSpecies = (int) this.findKeys("species.name.sp*").stream().count();
        nResource = (int) this.findKeys("species.name.rsc*").stream().count();
        nBackground = (int) this.findKeys("species.name.bkg*").stream().count();
        
        // Extract the species indexes for the the 
        this.focalIndex = this.findKeys("species.name.sp*").stream()
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".sp") + 3))).toArray();
        this.bkgIndex = this.findKeys("species.name.bkg*").stream()
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".bkg") + 4))).toArray();
        this.rscIndex = this.findKeys("species.type.rsc*").stream()
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".rsc") + 4))).toArray();
        
        nSpecies = getInt("simulation.nspecies");
        nResource = getInt("simulation.nresource");
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
            for (int i = 0; i < nSpecies; i++) {
                nSchool[i] = getInt("simulation.nschool.sp" + i);
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
        species = new HashMap();
        for (int i : this.focalIndex) {
            species.put(i, new Species(i));
            // Name must contain only alphanumerical characters
            if (!species.get(i).getName().matches("^[a-zA-Z0-9]*$")) {
                error("Species name must contain alphanumeric characters only. Please rename " + species.get(i).getName(), null);
            }
        }

        // Init resource groups
        //rscSpecies = new ResourceSpecies[nResource];
        rscSpecies = new HashMap();
        for (int rsc : this.rscIndex) {
            rscSpecies.put(rsc, new ResourceSpecies(rsc));
            // Name must contain only alphanumerical characters
            if (!rscSpecies.get(rsc).getName().matches("^[a-zA-Z0-9]*$")) {
                error("Resource name must contain alphanumeric characters only. Please rename " + rscSpecies.get(rsc).getName(), null);
            }
        }

        // barrier.n: add number of background species
        key = "simulation.nbackground";
        nBackground = 0;
        if (canFind(key)) {
            nBackground = getInt(key);
        }

        // Initialisation of the Background array.
        bkgSpecies = new HashMap();
        for (int p : this.bkgIndex) {
            bkgSpecies.put(p, new BackgroundSpecies(p));
             if (!bkgSpecies.get(p).getName().matches("^[a-zA-Z0-9]*$")) {
                error("Background species name must contain alphanumeric characters only. Please rename " + bkgSpecies.get(p).getName(), null);
            }
        }

        // Fisheries
        boolean fisheryEnabled = getBoolean("fishery.enabled");
        nFishery = fisheryEnabled ? findKeys("fishery.select.curve.fsh*").size() : 0;

        // Output regions
        outputRegions = new ArrayList();
        // special case region0, the whole domain
        if (!canFind("output.region.enabled.rg0")
                || getBoolean("output.region.enabled.rg0")) {
            if (findKeys("output.region.*.rg0").size() > 1) {
                error("Output region 0 corresponds to the whole grid and cannot be redefined.", new IllegalArgumentException("Region0 cannot be overwritten"));
            }
            outputRegions.add(new OutputWholeRegion(0));
        }
        // list output regions
        HashSet<Integer> rg = new HashSet(
                findKeys("output.region.*.rg*").stream()
                        .map(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".rg") + 3)))
                        .collect(Collectors.toList())
        );
        // remove rg0 (whole domain) that is handled separately
        rg.remove(0);
        rg.forEach(index -> {
            if (!canFind("output.region.enabled.rg" + index)
                    || getBoolean("output.region.enabled.rg" + index)) {
                outputRegions.add(new OutputRegion(index));
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

    }

    public List<OutputRegion> getOutputRegions() {
        return outputRegions;
    }

    public int getNFishery() {
        return nFishery;
    }

    public boolean isFisheryEnabled() {
        return nFishery > 0;
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
        return species.get(index);
    }

    /**
     * Gets the specified resource group.
     *
     * @param index, the index of the resource group.
     * @return the resource group with given index
     */
    public ResourceSpecies getResourceSpecies(int index) {
        return rscSpecies.get(index);
    }

    /**
     * Creates a new grid.
     *
     * @see fr.ird.osmose.grid.IGrid
     */
    public void initGrid() {

        String gridClassName = getString("grid.java.classname");
        try {
            info("Grid: " + gridClassName);
            grid = (AbstractGrid) Class.forName(gridClassName).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            error("Failed to create new grid instance. " + ex.getMessage(), ex);
        }
        // Init the grid
        grid.init();
    }

    /**
     * Loads recursively the parameters from the configuration file. The
     * function scans one by one the lines of the configuration file. A line is
     * discarded when it matches any of these criteria: it is empty, it contains
     * only blank and/or tab characters, it starts with a punctuation character
     * (punctuation: One of {@code !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~})<br>
     * Any other lines are expected to be parameters formed as <i>key separator
     * value</i>. Refer to the documentation at the beginning of the class for
     * details about the parameters.<br>
     * A parameter whose key start with <i>osmose.configuration.</i> means the
     * value designate an other configuration file that has to be loaded in the
     * current {@code Configuration}. The function {@code loadProperties} is
     * called recursively.
     *
     * @param filename, the configuration file to be loaded
     * @param depth, an integer that reflects the level of recursivity of the
     * function. Zero for the main configuration file, one for a file loaded
     * from the main configuration file, etc.
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
                        warning("{0}Osmose will ignore parameter {1}", new Object[]{space, entry});
                        warning("{0}Parameter already defined {1}", new Object[]{space, parameters.get(entry.key)});

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
     * {@code !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~}
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
     * {@code true} in several cases: the parameter does not exist, the value of
     * the parameter is empty or the value of the parameter is set to "null".
     *
     * @param key, the key of the parameter
     * @return {@code true} if the parameter is either null, empty or does not
     * exist
     */
    public boolean isNull(String key) {
        Parameter param = parameters.get(key.toLowerCase());
        return (null == param)
                || param.value.isEmpty()
                || param.value.equalsIgnoreCase("null");
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
     * Find all the parameters whose key matches the filter given as argument.
     * The filter accepts the following meta-character: "?" for any single
     * character and "*" for any String.
     *
     * @see fr.ird.osmose.util.Properties#getKeys(java.lang.String) for details
     * about how the filter works.
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
        List<String> filteredKeys = new ArrayList();
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
     * @param key, the key of the parameter
     * @return, the parameter as a path resolved again the path of the
     * configuration file that defined the parameter.
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
     * @throws NumberFormatException if the value of the parameter cannot be
     * parsed as an integer.
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
     * @throws NumberFormatException if the value of the parameter cannot be
     * parsed as a float.
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
     * @throws NumberFormatException if the value of the parameter cannot be
     * parsed as a double.
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
     * @throws NumberFormatException if the value of the parameter cannot be
     * parsed as a double.
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
     * @param key, the key of the parameter
     * @param warning, send a warning if the key cannot be found
     * @throws NumberFormatException if the value of the parameter cannot be
     * parsed as a boolean.
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
     * @throws NumberFormatException if the value of the parameter cannot be
     * parsed as a boolean.
     * @return the parameter as a boolean
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, true);
    }

    /**
     * Returns the specified parameter as an array of integers, {@code int[]}.
     *
     * @param key, the key of the parameter
     * @throws NumberFormatException if the values of the parameter cannot be
     * parsed as an integer.
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
     * @throws NumberFormatException if the values of the parameter cannot be
     * parsed as a float.
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
     * @throws NumberFormatException if the values of the parameter cannot be
     * parsed as a double.
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
     * @param filename, the file path to resolve
     * @param relativeTo, the path against the file must be resolved
     * @return the resolved file path
     */
    private String resolve(String filename, String relativeTo) {
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
     * Returns the output path. Parameter <i>output.dir.path</i> or second
     * argument of the command line option.
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
     * @return the number of CPUs allocated for running the simulations
     * concurrently
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
     * Returns the number of resource species. Parameter
     * <i>simulation.nresource</i>
     *
     * @return the number of resource groups.
     */
    public int getNRscSpecies() {
        return nResource;
    }

    /**
     * Returns the number of replicated simulations. (i.e. simulations with same
     * set of parameters). Parameter <i>simulation.nsimu</i>
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
     * event for species at index {@code iSpecies}
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
     * Guess the default parameter separator (between key and value) in the set
     * of configuration files. Osmose takes as default separator the one that is
     * the most widely used in the current configuration. The function scans all
     * the parameters, counts the occurrences of every type of separator and
     * returns the one that with the highest count.
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
     * Get a species
     *
     * @param index, the index of the species
     * @return the species at index {@code index}
     */
    public BackgroundSpecies getBkgSpecies(int index) {
        return bkgSpecies.get(index);
    }

    /**
     * Returns {@code true} if incoming fluxes is enabled.
     *
     * @return {@code true} if incoming fluxes is enabled.
     */
    public boolean isIncomingFluxEnabled() {
        return this.incomingFluxEnabled;
    }

    /**
     * Inner class that represents a parameter in the configuration file.
     * {@code Configuration} parses the configuration file line by line. When
     * the line is not discarded (refer to function
     * {@link #loadParameters(java.lang.String, int)} for details about
     * discarded lines), it assumes it is a parameter (formed as <i>key
     * separator value</i> or <i>key separator1 value1 separator2 value2
     * separator2 value3 separator2 value4</i>) and creates a new
     * {@code Parameter} object.
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
         * The separator between the values of the parameter. <i>key
         * keySeparator value1 valueSeparator value2 valueSeparator value3</i>
         */
        private String valueSeparator;

        /**
         * Create a new parameter out of the given line.
         *
         * @param iline, the line of the parameter in the configuration file
         * @param source, the path of the configuration file
         */
        Parameter(int iline, String source) {
            this.iline = iline;
            this.source = source;
        }

        /**
         * Create a new parameter from the command line
         *
         * @param key, the key of the parameter
         * @param value, the value of the parameter
         */
        Parameter(String key, String value) {
            this.key = key;
            this.value = value;
            this.source = "command line";
            this.iline = -1;
        }

        /**
         * Parse the line as a parameter. It follows the following steps: guess
         * the separator between key and value. Splits the line into a key and a
         * value. Guess the value separator in case it is actually an array of
         * values.
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
         * Cleans the value of the parameter. Trims the value (removes leading
         * and trailing blank characters), and removes any trailing separators.
         *
         * @param value, the value to be cleaned
         * @return a copy of the value, trimmed and without any trailing
         * separator.
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
         * Splits the given line into a key and a value, using the
         * {@code keySeparator}. Sends and error message if the line cannot be
         * split.
         *
         * @param line, the line to be split into a key and a value.
         */
        private void split(String line) {

            // make sure the line contains at least one semi-colon (key;value)
            if (!line.contains(keySeparator)) {
                error("Failed to split line " + iline + " " + line + " as key" + keySeparator + "value (from " + source + ")", null);
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

}
