/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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

import fr.ird.osmose.util.Version;
import fr.ird.osmose.util.UpdateManager;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.util.Properties;
import fr.ird.osmose.util.logging.OLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * {@code Configuration}. It can be summarize <i>key separator1 value1
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
     * A {@link fr.ird.osmose.util.Properties} object that stores the
     * parameters.
     */
    final private Properties cfg;
    /**
     * A {@link fr.ird.osmose.util.Properties} object that stores the file of
     * origin of each parameter.
     */
    final private Properties source;
    /**
     * Name of the main configuration file.
     */
    final private String mainFilename;
    /**
     * Path of the output directory.
     */
    private String outputPathname;
    /**
     * Path of the directory of the main configuration file.
     */
    final private String inputPathname;
    /**
     * Number of CPUs allocated to {@code Osmose} for running the simulations
     * concurrently. Parameter <i>simulation.ncpu</i>
     */
    private int nCpu;
    /**
     * Number of species that are explicitly modeled. Parameter
     * <i>simulation.nspecies</i>
     */
    private int nSpecies;
    /**
     * Number of plankton groups that force the simulation. Parameter
     * <i>simulation.nplankton</i>
     */
    private int nPlankton;
    /**
     * Number of replicated simulations. It is the number of simulation to be
     * run with the same set of parameters. Parameter <i>simulation.nsimu</i>
     */
    private int nSimulation;
    /**
     * Number of years of the simulation. Parameter <i>simulation.nyear</i>
     */
    private int nYear;
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
     * The spatial grid of the simulation, {@link fr.ird.osmose.grid.IGrid}.
     */
    private IGrid grid;

///////////////
// Constructors
///////////////
    /**
     * Creates a new {@code Configuration}.
     *
     * @param mainFilename, the main configuration file
     * @param outputPathname, the path of the output directory
     */
    Configuration(String mainFilename, String outputPathname) {

        this.mainFilename = mainFilename;
        this.inputPathname = new File(mainFilename).getParentFile().getAbsolutePath();
        this.outputPathname = outputPathname;

        cfg = new Properties();
        source = new Properties();
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Initializes the current configuration. Loads the parameters from the
     * configuration file, sets the values of the main variables and creates the
     * grid.
     */
    public void init() {

        // Load the parameters from the main configuration file
        loadProperties(mainFilename, 0);

        // Check whether the path of the output folder is already set (by command line option)
        if (null == outputPathname) {
            // Output path read in the configuration file
            outputPathname = resolvePath(getString("output.dir.path"));
        }

        // Check whether the configuration file is up-to-date
        UpdateManager.getInstance().upgrade();

        if (!isNull("simulation.ncpu")) {
            nCpu = getInt("simulation.ncpu");
        } else {
            nCpu = Integer.MAX_VALUE;
        }
        nCpu = Math.max(nCpu, 1);
        nSpecies = getInt("simulation.nspecies");
        nPlankton = getInt("simulation.nplankton");
        nSimulation = getInt("simulation.nsimulation");
        nYear = getInt("simulation.time.nyear");
        nStepYear = getInt("simulation.time.ndtperyear");
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
            grid = (IGrid) Class.forName(gridClassName).newInstance();
        } catch (InstantiationException ex) {
            error("Failed to create new grid instance. " + ex.getMessage(), ex);
        } catch (IllegalAccessException ex) {
            error("Failed to create new grid instance. " + ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
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
    private void loadProperties(String filename, int depth) {

        BufferedReader bfIn = null;
        String path = resolveFile(filename);
        // Open the buffer
        try {
            bfIn = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException ex) {
            error("Could not fing Osmose configuration file: " + path, ex);
        }
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            msg.append("  ");
        }
        msg.append("Loading parameters from file ");
        msg.append(path);
        info(msg.toString());

        // Read it
        String line = null;
        int iline = 1;
        try {
            while ((line = bfIn.readLine()) != null) {
                line = line.trim();
                if (!startsWithSymbol(line) & !(line.length() <= 1)) {
                    Parameter entry = new Parameter(line, iline, path);
                    if (null != entry.key && null != entry.value) {
                        if (entry.key.startsWith("osmose.configuration")) {
                            source.setProperty(entry.key, path);
                            loadProperties(entry.value, depth + 1);
                        } else {
                            if (source.containsKey(entry.key)) {
                                warning("Parameter {0} has already been defined in file {1} with value {2}", new Object[]{entry.key, source.getProperty(entry.key), cfg.getProperty(entry.key)});
                                warning("Osmose will ignore parameter {0} in file {1} with value {2}", new Object[]{entry.key, path, entry.value});
                            } else {
                                cfg.setProperty(entry.key, entry.value);
                                source.setProperty(entry.key, path);
                            }
                        }
                    }
                }
                iline++;
            }
        } catch (IOException ex) {
            error("Error loading parameters from file " + filename + " at line " + iline + " " + line, ex);
        }
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
        try {
            return (null == getString(key));
        } catch (Exception ex) {
            return true;
        }
    }

    /**
     * Check whether the parameter exists, no matter what the value is.
     *
     * @param key, the key of the parameter
     * @return {@code true} if the parameter exists.
     */
    public boolean canFind(String key) {
        try {
            getString(key);
        } catch (Exception ex) {
            return false;
        }
        return true;
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
        return cfg.getKeys(filter);
    }

    /**
     * Returns the value of the specified parameter as a {@code String}
     *
     * @param key, the key of the parameter
     * @throws NullPointerException if the parameter is not found.
     * @return the value of the parameter as a {@code String}
     */
    final public String getString(String key) {

        String lkey = key.toLowerCase();
        if (cfg.containsKey(lkey)) {
            String value = cfg.getProperty(lkey);
            if (value.equalsIgnoreCase("null")) {
                return null;
            }
            return value.trim();
        } else {
            throw new NullPointerException("Could not find parameter " + key);
        }
    }

    /**
     * Returns the path of the configuration file that contains the specified
     * parameter.
     *
     * @param key, the key of the parameter
     * @return the path of the configuration file that contains the parameter.
     */
    final public String getSource(String key) {
        String lkey = key.toLowerCase();
        if (source.containsKey(lkey)) {
            return source.getProperty(key).trim();
        } else {
            return "null";
        }
    }

    /**
     * Returns the specified parameter as a path resolved again the main
     * configuration file.
     *
     * @see #resolveFile(java.lang.String)
     * @param key, the key of the parameter
     * @return, the parameter as a path resolved again the main configuration
     * file.
     */
    public String getFile(String key) {
        return resolveFile(getString(key));
    }

    /**
     * Returns the specified parameter as an array of strings, {@code String[]}.
     *
     * @param key, the key of the parameter
     * @return the parameter as a {@code String[]}
     */
    public String[] getArrayString(String key) {
        String value = getString(key);
        String[] values = value.split(guessSeparator(value, Separator.SEMICOLON).toString());
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
            error("Could not convert parameter " + key + " to integer " + s + " (from file " + getSource(key) + ")", ex);
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
            error("Could not convert parameter " + key + " to float " + s + " (from file " + getSource(key) + ")", ex);
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
            error("Could not convert parameter " + key + " to double " + s + " (from file " + getSource(key) + ")", ex);
        }
        return Double.NaN;
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
        try {
            String s = getString(key);
            try {
                return Boolean.valueOf(s);
            } catch (NumberFormatException ex) {
                error("Could not convert parameter " + key + " to boolean " + s + " (from file " + getSource(key) + ")", ex);
            }
        } catch (NullPointerException ex) {
            warning("Could not find boolean parameter " + key + ". Osmose assumes it is false.");
        }
        return false;
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
            error("Could not convert parameter " + key + " to array of integer " + getString(key) + " (from file " + getSource(key) + ")", ex);
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
            error("Could not convert parameter " + key + " to array of float " + getString(key) + " (from file " + getSource(key) + ")", ex);
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
                ad[i] = Float.valueOf(as[i]);
            }
            return ad;
        } catch (NumberFormatException ex) {
            error("Could not convert parameter " + key + " to array of double " + getString(key) + " (from file " + getSource(key) + ")", ex);
        }
        return null;
    }

    /**
     * Resolves a directory path against the the input path. Adds a trailing
     * file separator at the end of the resolved path.
     *
     * @param path, the path to resolve
     * @return the path resolved against the the input path.
     */
    private String resolvePath(String path) {
        String pathname = resolveFile(path);
        if (!pathname.endsWith(File.separator)) {
            pathname += File.separator;
        }
        return pathname;
    }

    /**
     * Resolves a file path against the the input path.
     *
     * @param filename, the file path to resolve
     * @return the file path resolved against the input path.
     */
    private String resolveFile(String filename) {
        try {
            File file = new File(inputPathname);
            String pathname = new File(file.toURI().resolve(filename)).getCanonicalPath();
            return pathname;
        } catch (Exception ex) {
            return filename;
        }
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
     * Returns the number of plankton groups. Parameter
     * <i>simulation.nplanton</i>
     *
     * @return the number of plankton groups.
     */
    public int getNPlankton() {
        return nPlankton;
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
     * Returns the number of years of the simulation. Parameter
     * <i>simulation.nyear</i>
     *
     * @return the number of years of the simulation
     */
    public int getNYear() {
        return nYear;
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
     * Returns the grid ({@link IGrid) of the model.
     *
     * @return the grid of the model.
     */
    public IGrid getGrid() {
        return grid;
    }

    /**
     * Returns the version of the configuration file. Parameter
     * <i>osmose.version</i>. If the parameter is not found or the value does
     * not match any listed Osmose version, {@code Configuration} assumes it is
     * version 3.0b {@link Version#v3_0_beta}
     *
     * @see fr.ird.osmose.util.Version
     * @return the version of the configuration file.
     */
    public Version getVersion() {
        if (!isNull("osmose.version")) {
            try {
                String version = getString("osmose.version");
                String number = version.split(" ")[0];
                String date = version.split(" ")[1];
                return new Version(number, date);
            } catch (Exception ex) {
                warning("Could not identify version of the configuration, parameter osmose.version = {0}. Osmose assumes it is {1}", new Object[]{getString("osmose.version"), Version.v3_0_beta.toString()});
            }
        }
        return Version.v3_0_beta;
    }

    /**
     * This function tries to guess what is the separator in the given string
     * assuming that it is an array of at least two values. It will look for
     * separators {@code = ; : \t} in this order. If none of these separators
     * are found then it will return the fallback separator given as a
     * parameter.
     *
     * @param string, the string you assume to be an array of strings separator
     * by one of the accepted {@link Separator}.
     * @param fallbackSeparator, the fallback separator returned by the function
     * if the guess fails
     * @see Separator
     * @return the separator contained in the {@code string}
     */
    private Separator guessSeparator(String string, Separator fallback) {

        for (Separator separator : Separator.values()) {
            if (string.contains(separator.toString()) && string.split(separator.toString()).length >= 1) {
                return separator;
            }
        }
        return fallback;
    }

    /**
     * List of accepted separators in a parameter <i>key separator value</i> and
     * in an array of values <i>key separator1 value1 separator2 value2
     * separator2 value3 separator2 value4</i>.
     * <ul>
     * <li>equals =</li>
     * <li>semicolon ;</li>
     * <li>coma ,</li>
     * <li>colon :</li>
     * <li>tab \t</li>
     * <ul>
     */
    private enum Separator {

        EQUALS('='),
        SEMICOLON(';'),
        COMA(','),
        COLON(':'),
        TAB('\t');
        private final String separator;

        private Separator(char separator) {
            this.separator = Character.toString(separator);
        }

        @Override
        public String toString() {
            return separator;
        }
    }

    /**
     * Inner class that represents a parameter in the configuration file.
     * {@code Configuration} parses the configuration file line by line. When
     * the line is not discarded (refer to function
     * {@link #loadProperties(java.lang.String, int)} for details about
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
         * Created a new parameter out of the given line.
         *
         * @param line, the {@code String} to be parsed as a parameter
         * @param iline, the line of the parameter in the configuration file
         * @param source, the path of the configuration file
         */
        Parameter(String line, int iline, String source) {
            this.iline = iline;
            this.source = source;
            parse(line);
            debug(key + "=" + value);
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
            keySeparator = guessSeparator(line, Separator.EQUALS).toString();
            split(line);
            valueSeparator = guessSeparator(value, Separator.SEMICOLON).toString();
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
                error("Failed to split line " + iline + " " + line + " as key" + keySeparator + "value (from file " + source + ")", null);
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
            // send a warning if the value is null
            if (value.equalsIgnoreCase("null")) {
                debug("No value found for parameter {0}", key);
            }
        }
    }
}
