/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import fr.ird.osmose.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

    Configuration(String mainFilename, String outputPathname) {
        this.mainFilename = mainFilename;
        inputPathname = new File(mainFilename).getParentFile().getAbsolutePath();

        cfg = new Properties();
        loadProperties(mainFilename);

        if (null != outputPathname) {
            this.outputPathname = outputPathname;
        } else {
            this.outputPathname = resolvePath(getValue("output.path"));
        }
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
                if (!line.startsWith("#") & !(line.length() < 1)) {
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
            getValue(key);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    final public String getValue(String key) {

        String lkey = key.toLowerCase();
        if (cfg.containsKey(lkey)) {
            String value = cfg.getProperty(lkey);
            if (value.equalsIgnoreCase("null")) {
                throw new NullPointerException("No value for parameter " + key);
            }
            return value.trim();
        } else {
            throw new NullPointerException("Could not find parameter " + key);
        }
    }

    public String[] getValues(String key) {
        String value = getValue(key);
        return value.split(";");
    }

    public int getInt(String key) {
        String s = getValue(key);
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to integer " + s);
        }
    }
    
    public float getFloat(String key) {
        String s = getValue(key);
        try {
            return Float.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to float " + s);
        }
    }
    
    public double getDouble(String key) {
        String s = getValue(key);
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to double " + s);
        }
    }
    
    public int[] getArrayInt(String key) {
        String[] as = getValues(key);
        try {
            int[] ai = new int[as.length];
            for (int i = 0; i < ai.length; i++) {
                ai[i] = Integer.valueOf(as[i]);
            }
            return ai;
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to array of integer " + getValue(key));
        }
    }
    
    public float[] getArrayFloat(String key) {
        String[] as = getValues(key);
        try {
            float[] af = new float[as.length];
            for (int i = 0; i < af.length; i++) {
                af[i] = Float.valueOf(as[i]);
            }
            return af;
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to array of float " + getValue(key));
        }
    }
    
    public double[] getArrayDouble(String key) {
        String[] as = getValues(key);
        try {
            double[] ad = new double[as.length];
            for (int i = 0; i < ad.length; i++) {
                ad[i] = Float.valueOf(as[i]);
            }
            return ad;
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Could not convert parameter " + key + " to array of double " + getValue(key));
        }
    }

    private String resolvePath(String path) {
        String pathname = resolveFile(path);
        if (!pathname.endsWith(File.separator)) {
            pathname += File.separator;
        }
        return pathname;
    }

    public String resolveFile(String filename) {
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
                getLogger().log(Level.WARNING, "No value found for parameter {0}", key);
            }
        }
    }
//    public static void main(String[] args) {
//
//        Osmose.getInstance().init(args);
//        Configuration cfg = new Configuration("/home/pverley/osmose/dev/config/ben/ben_meso_trunk/benguela_all-parameters.csv", null);
//        cfg.getLogger().info(cfg.getValue("predation.accessibility.file"));
//        cfg.getLogger().info(cfg.getValue("grid.lowright.lat"));
//        cfg.getLogger().info(cfg.getValue("fishing.rate.sp3"));
//        cfg.getArrayFloat("fishing.rate.sp3");
//        cfg.getLogger().log(Level.INFO, "{0}", cfg.getFloat("grid.lowright.lat"));
//
//    }
}
