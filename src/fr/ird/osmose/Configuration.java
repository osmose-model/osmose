/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.util.GridMap;
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
    private int[] seed;
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
        seed = new int[nSpecies];
        if (findKeys("simulation.nschool.sp*").size() == nSpecies) {
            for (int i = 0; i < nSpecies; i++) {
                seed[i] = getInt("simulation.nschool.sp" + i);
            }
        } else if (canFind("simulation.nschool")) {
            int n = getInt("simulation.nschool");
            for (int i = 0; i < nSpecies; i++) {
                seed[i] = n;
            }
        } else {
            for (int i = 0; i < nSpecies; i++) {
                seed[i] = 10;
            }
        }

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

    /**
     * Check if parameters 'key' has 'null' value.
     *
     * @param key
     * @return true if parameter has 'null' value or does not exist
     */
    public boolean isNull(String key) {
        try {
            return (null == getString(key));
        } catch (Exception ex) {
            return true;
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

    final public String getString(String key) {

        String lkey = key.toLowerCase();
        if (cfg.containsKey(lkey)) {
            String value = cfg.getProperty(lkey);
            if (value.equalsIgnoreCase("null")) {
                return null;
                //throw new NullPointerException("Null value for parameter " + key);
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
        String[] values = value.split(guessSeparator(value, Separator.SEMICOLON).toString());
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
    public int getSeed(int iSpecies) {
        return seed[iSpecies];
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

    public GridMap readCSVMap(String csvFile) {

        GridMap map = null;
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            /*
             * Initialize the map
             */
            map = new GridMap();
            /*
             * Read the map
             */
            int ny = getGrid().get_ny();
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val > 0.f) {
                        map.setValue(i, j, val);
                    }
                }
            }
            reader.close();
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading CSV map " + csvFile, ex);
        }
        return map;
    }

    public float[][] readTimeSeriesSpecies(String filename) {

        int nStepSimu = nStepYear * nYear;
        float[][] rates = new float[nSpecies][nStepSimu];
        try {
            // 1. Open CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();
            // 2. Read natural mortality rates
            int nTimeSerie = lines.size() - 1;
            for (int t = 0; t < nTimeSerie; t++) {
                String[] line = lines.get(t + 1);
                for (int i = 0; i < nSpecies; i++) {
                    rates[i][t] = Float.valueOf(line[i + 1]);
                }
            }
            // 3. Check the length of the time serie
            if (nTimeSerie % nStepYear != 0) {
                // Either the time serie is less than a year or it is not a 
                // multiple of number of time step per year.
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must be a multiple of the number of time steps per year.");
            } else if (nTimeSerie < nStepSimu) {
                // There is less season in the file than number of years of the
                // simulation.
                int t = nTimeSerie;
                while (t < nStepSimu) {
                    for (int k = 0; k < nTimeSerie; k++) {
                        for (int i = 0; i < nSpecies; i++) {
                            rates[i][t] = rates[i][k];
                        }
                        t++;
                        if (t == nStepSimu) {
                            break;
                        }
                    }
                }
                getLogger().log(Level.WARNING, "Time serie in file {0} only contains {1} steps out of {2}. Osmose will loop over it.", new Object[]{filename, nTimeSerie, nStepSimu});
            } else if (nTimeSerie > nStepSimu) {
                getLogger().log(Level.WARNING, "Time serie in file {0} contains {1} steps out of {2}. Osmose will ignore the exceeding years.", new Object[]{filename, nTimeSerie, nStepSimu});
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading CSV file " + filename, ex);
            System.exit(1);
        }

        return rates;
    }
    
    public float[] readTimeSeries(String filename) {
        return readTimeSeries(filename, nStepYear, nStepYear * nYear);
    }

    public float[] readTimeSeries(String filename, int nMin, int nMax) {

        int nStepSimu = nStepYear * nYear;
        float[] rates = new float[nStepSimu];
        try {
            // 1. Open CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();
            // 2. Check the length of the time serie and inform the user about potential problems or inconsistencies
            int nTimeSerie = lines.size() - 1;
            if (nTimeSerie < nMin) {
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must contain at least " + nMin + " time steps.");
            }
            if (nTimeSerie % nStepYear != 0) {
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must be a multiple of the number of time steps per year.");
            }
            if (nTimeSerie > nMax) {
                getLogger().log(Level.WARNING, "Time serie in file {0} contains {1} steps out of {2}. Osmose will ignore the exceeding years.", new Object[]{filename, nTimeSerie, nMax});
            }
            nTimeSerie = Math.min(nTimeSerie, nMax);
            // 3. Read the time serie
            for (int t = 0; t < nTimeSerie; t++) {
                String[] line = lines.get(t + 1);
                rates[t] = Float.valueOf(line[1]);
            }
            // 4. Fill up the time serie if necessary
            if (nTimeSerie < nStepSimu) {
                // There is less season in the file than number of years of the
                // simulation.
                int t = nTimeSerie;
                while (t < nStepSimu) {
                    for (int k = 0; k < nTimeSerie; k++) {
                        rates[t] = rates[k];
                        t++;
                        if (t == nStepSimu) {
                            break;
                        }
                    }
                }
                getLogger().log(Level.WARNING, "Time serie in file {0} only contains {1} steps out of {2}. Osmose will loop over it.", new Object[]{filename, nTimeSerie, nMax});
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading CSV file " + filename, ex);
            System.exit(1);
        }

        return rates;
    }

    /**
     * This function tries to guess what is the separator in the given string
     * assuming that it is an array of at least two values. It will look for
     * separators '=' '\t' ';' and ',' in this order. If none of this separator
     * are found then it will return the fallback separator given as a
     * parameter.
     *
     * @param string, the string you assume to be an array of values
     * @param fallbackSeparator, the fallback separator if
     * @return
     */
    private Separator guessSeparator(String string, Separator fallback) {

        for (Separator separator : Separator.values()) {
            if (string.contains(separator.toString()) && string.split(separator.toString()).length >= 1) {
                return separator;
            }
        }
        return fallback;
    }

    private enum Separator {

        EQUALS('='),
        SEMICOLON(';'),
        COMA(','),
        COLON(':'),
        TAB('\t'),
        BLANK(' ');
        private String separator;

        private Separator(char separator) {
            this.separator = Character.toString(separator);
        }

        @Override
        public String toString() {
            return separator;
        }
    }

    private class Entry {

        private String key;
        private String value;
        private String keySeparator;
        private String valueSeparator;

        Entry(String line) {
            process(line);
        }

        private void process(String line) {
            key = value = null;
            keySeparator = guessSeparator(line, Separator.EQUALS).toString();
            parse(line);
            valueSeparator = guessSeparator(value, Separator.SEMICOLON).toString();
            value = clean(value);
        }

        private String clean(String value) {
            String cleanedValue = value.trim();
            if (cleanedValue.endsWith(valueSeparator)) {
                cleanedValue = cleanedValue.substring(0, cleanedValue.lastIndexOf(valueSeparator));
                return clean(cleanedValue);
            } else {
                return cleanedValue;
            }
        }

        private void parse(String line) {

            // make sure the line contains at least one semi-colon (key;value)
            if (!line.contains(keySeparator)) {
                getLogger().log(Level.WARNING, "Failed to parse line {0} as key{1}value", new Object[]{line, keySeparator});
                // key = value = null by default
                return;
            }
            // extract the key
            key = line.substring(0, line.indexOf(keySeparator)).toLowerCase();
            // extract the value
            try {
                value = line.substring(line.indexOf(keySeparator) + 1);
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
