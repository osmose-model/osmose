/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class Configuration {

    final private Properties cfg;
    /**
     * Filename of the main configuration file.
     */
    final private String mainFilename;
    final private String outputPathname;
    final private String inputPathname;
    private int nCpu;
    private int nSpecies;
    private int nPlankton;
    private int nSimulation;
    private int nYear;
    private int nStepYear;
    private int seed;
    private int nLTLStep;
    private boolean dietEnabled;
    private IGrid grid;
    private LTLForcing forcing;

    Configuration(String mainFilename, String outputPathname) {
        this.mainFilename = mainFilename;
        inputPathname = new File(mainFilename).getParentFile().getAbsolutePath();

        cfg = new Properties();
        loadProperties(mainFilename);

        if (null != outputPathname) {
            this.outputPathname = outputPathname;
        } else {
            this.outputPathname = resolvePath(getString("output.dir.path"));
        }
    }

    public void init() {

        if (canFind("simulation.ncpu")) {
            nCpu = getInt("simulation.ncpu");
        } else {
            nCpu = Integer.MAX_VALUE;
        }
        nSpecies = getInt("simulation.nspecies");
        nPlankton = getInt("simulation.nplankton");
        nSimulation = getInt("simulation.nsimulation");
        nYear = getInt("simulation.time.nyear");
        nStepYear = getInt("simulation.time.ndtperyear");
        seed = getInt("simulation.nschool") / nStepYear;
        nLTLStep = getInt("ltl.nstep");
        dietEnabled = getBoolean("output.diet.pressure.enabled")
                || getBoolean("output.diet.composition.enabled");

        initGrid();
        initForcing();
    }

    public void initGrid() {

        String gridClassName = getString("grid.java.classname");
        try {
            getLogger().log(Level.INFO, "Initialize grid: {0}", gridClassName);
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

        String ltlClassName = getString("ltl.java.classname");
        try {
            forcing = (LTLForcing) Class.forName(ltlClassName).newInstance();
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        forcing.init();
    }

    private void loadProperties(String filename) {

        BufferedReader bfIn = null;
        // Open the buffer
        try {
            bfIn = new BufferedReader(new FileReader(resolveFile(filename)));
        } catch (FileNotFoundException ex) {
            String msg = "Could not fing Osmose configuration file: " + filename;
            getLogger().log(Level.SEVERE, msg, ex);
        }

        // Read it
        String line = null;
        try {
            while ((line = bfIn.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") & !line.startsWith("//") & !(line.length() < 1)) {
                    Entry entry = new Entry(line);
                    if (null != entry.key && null != entry.value) {
                        if (entry.key.startsWith("osmose.configuration")) {
                            loadProperties(entry.value);
                        } else {
                            cfg.setProperty(entry.key, entry.value);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error loading parameters from file " + filename + " around line " + line, ex);
        }
    }

    public boolean canFind(String key) {
        try {
            getString(key);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public List<String> findKeys(String filter) {
        return cfg.getKeys(filter);
    }

    private String clean(String value) {
        String cleanedValue = value.trim();
        if (cleanedValue.endsWith(";")) {
            cleanedValue = cleanedValue.substring(0, cleanedValue.lastIndexOf(";"));
            return clean(cleanedValue);
        } else {
            return cleanedValue;
        }
    }

    final public String getString(String key) {

        String lkey = key.toLowerCase();
        if (cfg.containsKey(lkey)) {
            String value = clean(cfg.getProperty(lkey));
            if (value.equalsIgnoreCase("null")) {
                throw new NullPointerException("Null value for parameter " + key);
            }
            return value.trim();
        } else {
            throw new NullPointerException("Could not find parameter " + key);
        }
    }

    public String getFile(String key) {
        return resolveFile(getString(key));
    }

    public String[] getArrayString(String key) {
        String value = getString(key);
        String[] values = value.split(";");
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        return values;
    }

    public int getInt(String key) {
        String s = getString(key);
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to integer " + s);
        }
    }

    public float getFloat(String key) {
        String s = getString(key);
        try {
            return Float.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to float " + s);
        }
    }

    public double getDouble(String key) {
        String s = getString(key);
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to double " + s);
        }
    }

    public boolean getBoolean(String key) {
        String s = getString(key);
        try {
            return Boolean.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to boolean " + s);
        }
    }

    public int[] getArrayInt(String key) {
        String[] as = getArrayString(key);
        try {
            int[] ai = new int[as.length];
            for (int i = 0; i < ai.length; i++) {
                ai[i] = Integer.valueOf(as[i]);
            }
            return ai;
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to array of integer " + getString(key));
        }
    }

    public float[] getArrayFloat(String key) {
        String[] as = getArrayString(key);
        try {
            float[] af = new float[as.length];
            for (int i = 0; i < af.length; i++) {
                af[i] = Float.valueOf(as[i]);
            }
            return af;
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to array of float " + getString(key));
        }
    }

    public double[] getArrayDouble(String key) {
        String[] as = getArrayString(key);
        try {
            double[] ad = new double[as.length];
            for (int i = 0; i < ad.length; i++) {
                ad[i] = Float.valueOf(as[i]);
            }
            return ad;
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to array of double " + getString(key));
        }
    }

    private String resolvePath(String path) {
        String pathname = resolveFile(path);
        if (!pathname.endsWith(File.separator)) {
            pathname += File.separator;
        }
        return pathname;
    }

    private String resolveFile(String filename) {
        try {
            File file = new File(inputPathname);
            String pathname = new File(file.toURI().resolve(filename)).getCanonicalPath();
            return pathname;
        } catch (Exception e) {
            return filename;
        }
    }

    private Logger getLogger() {
        return Osmose.getInstance().getLogger();
    }

    /**
     * @return the outputPathName
     */
    public String getOutputPathname() {
        return outputPathname;
    }

    /**
     * @return the nCpu
     */
    public int getNCpu() {
        return nCpu;
    }

    /**
     * @return the nSpecies
     */
    public int getNSpecies() {
        return nSpecies;
    }

    /**
     * @return the nPlankton
     */
    public int getNPlankton() {
        return nPlankton;
    }

    /**
     * @return the nSimulation
     */
    public int getNSimulation() {
        return nSimulation;
    }

    /**
     * @return the nYear
     */
    public int getNYear() {
        return nYear;
    }

    /**
     * @return the nStepYear
     */
    public int getNStepYear() {
        return nStepYear;
    }

    /**
     * @return the seed
     */
    public int getSeed() {
        return seed;
    }

    /**
     * @return the dietEnabled
     */
    public boolean isDietEnabled() {
        return dietEnabled;
    }

    public int getNumberLTLSteps() {
        return nLTLStep;
    }

    public IGrid getGrid() {
        return grid;
    }

    public LTLForcing getForcing() {
        return forcing;
    }

    private class Entry {

        private String key;
        private String value;

        Entry(String line) {
            key = value = null;
            parse(line);
        }

        private void parse(String line) {

            // make sure the line contains at least one semi-colon (key;value)
            if (!line.contains(";")) {
                getLogger().log(Level.WARNING, "Failed to parse line {0} as key;value", line);
                // key = value = null by default
                return;
            }
            // extract the key
            key = line.substring(0, line.indexOf(';')).toLowerCase();
            // extract the value
            try {
                value = line.substring(line.indexOf(';') + 1);
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
                getLogger().log(Level.FINE, "No value found for parameter {0}", key);
            }
        }
    }
}
