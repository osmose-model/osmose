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
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, msg, ex);
        }

        // Read it
        String line;
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
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    final public String getValue(String key) {

        if (cfg.containsKey(key)) {
            String value = cfg.getProperty(key);
            if (value.equalsIgnoreCase("null")) {
                throw new NullPointerException("No value for parameter: " + key);
            }
            return value;
        } else {
            throw new NullPointerException("Could not find parameter: " + key);
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
        return Logger.getLogger(Configuration.class.getName());
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
                getLogger().log(Level.WARNING, "Failed to parse line: {0} as key;value", line);
                // key = value = null by default
                return;
            }
            // extract the key
            key = line.substring(0, line.indexOf(';'));
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
                getLogger().log(Level.WARNING, "No value found for parameter: {0}", key);
            }
        }
    }

//    public static void main(String[] args) {
//
//        Configuration cfg = new Configuration("/home/pverley/osmose/dev/config/ben/ben_meso_trunk/benguela_all-parameters.csv", null);
//        cfg.getLogger().info(cfg.getValue("predation.accessibility.file"));
//        cfg.getLogger().info(cfg.getValue("grid.lowright.lat"));
//        cfg.getLogger().info(cfg.getValue("fishing.rate.sp3"));
//
//    }
}
