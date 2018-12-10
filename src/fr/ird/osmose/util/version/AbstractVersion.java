/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.version;

import fr.ird.osmose.util.OsmoseLinker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 *
 * @author pverley
 */
public abstract class AbstractVersion extends OsmoseLinker implements Comparable<AbstractVersion> {

    final private int number;

    final private int update;

    private int release;

    final private String date;

    final private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

    abstract void updateParameters();
    
    AbstractVersion(int number, int update, int release, int year, int month, int day) {
        this(number, update, year, month, day);
        this.release = release;
    }

    AbstractVersion(int number, int update, int year, int month, int day) {
        this.release = 0;
        this.number = number;
        this.update = update;
        Calendar cld = Calendar.getInstance();
        cld.set(year, month, day);
        sdf.setCalendar(cld);
        date = sdf.format(cld.getTime());
    }

    void updateConfiguration() {

        info("Updating configuration file to " + toString() + "...");

        // Update parameters
        updateParameters();

        // Update version
        updateValue("osmose.version", toString());
    }

    public int getNumber() {
        return number;
    }

    public int getUpdate() {
        return update;
    }
    
    public int getRelease() {
        return release;
    }

    public String getDate() {
        return date;
    }

    /**
     * Return Osmose version as a formatted String.
     * <br />
     * Osmose {version_number} Update {update-number} ({release_date})<br />
     * Example: Osmose 3 Update 1 (2014/06/01)
     *
     * @return Osmose version as a String.
     */
    @Override
    public String toString() {
        StringBuilder version = new StringBuilder("Osmose ");
        version.append(number);

        version.append(" Update ");
        version.append(update);
        
        version.append(" Release ");
        version.append(release);

        version.append(" (");
        version.append(date);
        version.append(")");
        return version.toString();
    }

    /**
     * Return Osmose version as a condensed String.
     * <br />
     * o{version_number}u{update-number}<br />
     * Example: o3u1, for Osmose 3 Update 1
     *
     * @return Osmose condensed version as a String.
     */
    public String toShortString() {
        StringBuilder version = new StringBuilder("o");
        version.append(number);
        version.append("u");
        version.append(update);
        version.append("r");
        version.append(release);
        return version.toString();
    }

    @Override
    public int compareTo(AbstractVersion otherVersion) {

        // Compare version number
        if (number != otherVersion.getNumber()) {
            return Integer.compare(number, otherVersion.getNumber());
        }

        // Same version number, compare update number
        if (update != otherVersion.getUpdate()) {
            return Integer.compare(update, otherVersion.getUpdate());
        }
        
        // Same version/update number, compare release number
        if (release != otherVersion.getRelease()) {
            return Integer.compare(release, otherVersion.getRelease());
        }

        // Same version number and same update number, versions are equal
        return 0;
    }

    /**
     * Add a new parameter (key + value) in the current configuration. The
     * function tries to find the most appropriate configuration file by looking
     * for similar parameters in the set of files. The main configuration file
     * is used by default if the guess is unsuccessful. The function uses the
     * default parameter separator between the key and the value.
     * {@link fr.ird.osmose.Configuration#getDefaultSeparator()}
     *
     * @param key, the key of the parameter
     * @param value, the value of the parameter
     */
    protected void addParameter(String key, String value) {

        // Return if the parameter already exists
        if (getConfiguration().canFind(key)) {
            StringBuilder warning = new StringBuilder();
            warning.append("  Did not add parameter ");
            warning.append(key);
            warning.append(" as it is already defined in file ");
            warning.append(getConfiguration().getSource(key));
            info(warning.toString());
            return;
        }
        // Find the best source file by looking for similar
        // parameters in the set of configuration files
        // Main configuration file by default
        String pattern = key;
        String source = getConfiguration().getMainFile();
        while (pattern.contains(".")) {
            pattern = pattern.substring(0, pattern.lastIndexOf(".")) + "*";
            List<String> keys = getConfiguration().findKeys(pattern);
            if (!keys.isEmpty()) {
                source = getConfiguration().getSource(keys.get(0));
                break;
            }
        }
        // Backup the source
        backup(source, VersionManager.getInstance().getConfigurationVersion());
        // Build the parameter as key + separator + value
        StringBuilder parameter = new StringBuilder();
        parameter.append(key);
        parameter.append(getConfiguration().getDefaultSeparator());
        parameter.append(value);
        // Extract the list of parameters
        ArrayList<String> parameters = getLines(source);
        // Add comment 
        StringBuilder msg = new StringBuilder();
        msg.append(toString());
        msg.append(" Added parameter ");
        msg.append(parameter);
        parameters.add("# " + msg.toString());
        // Add parameter
        parameters.add(parameter.toString());
        // Print the change in the console
        msg.append(" (");
        msg.append(source);
        msg.append(")");
        info(" " + msg.substring(msg.indexOf(")") + 1));
        // Write the updated configuration file
        write(source, parameters);
    }

    private int findLine(String key, ArrayList<String> parameters) {
        int iline = -1;
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).startsWith(key)) {
                iline = i;
                break;
            }
        }
        return iline;
    }

    protected void deprecateParameter(String key) {
        commentParameter(key, "Deprecated parameter " + key);
    }

    protected void commentParameter(String key, String comment) {

        // Check whether the parameter exists in the current configuration
        if (!getConfiguration().canFind(key)) {
            return;
        }
        // Backup the source file
        String source = getConfiguration().getSource(key);
        backup(source, VersionManager.getInstance().getConfigurationVersion());
        // Extract the list of parameters
        ArrayList<String> parameters = getLines(source);
        // Find the line of parameter defined by the key
        int iline = findLine(key, parameters);
        // Comment the deprecated parameter
        String deprecated = "# " + parameters.get(iline);
        parameters.set(iline, deprecated);
        // Add comment
        StringBuilder msg = new StringBuilder();
        msg.append(toString());
        msg.append(" ");
        msg.append(comment);
        parameters.add(iline, "# " + msg);
        // Print the change in the console
        msg.insert(0, "  ");
        msg.append(" (");
        msg.append(source);
        msg.append(")");
        info(" " + msg.substring(msg.indexOf(")") + 1));
        // Write the updated configuration file
        write(source, parameters);
    }

    protected void updateKey(String key, String newKey) {

        // Check whether the parameter newKey exists already
        if (getConfiguration().canFind(newKey)) {
            StringBuilder msg = new StringBuilder();
            msg.append("  Did not rename ");
            msg.append(key);
            msg.append(" into ");
            msg.append(newKey);
            msg.append(" as it is already defined in file");
            msg.append(getConfiguration().getSource(newKey));
            info(msg.toString());
            commentParameter(key, msg.toString());
            return;
        }
        
        // Check whether the parameter exists in the current configuration
        if (!getConfiguration().canFind(key)) {
            StringBuilder warning = new StringBuilder();
            warning.append("  Did not rename ");
            warning.append(key);
            warning.append(" into ");
            warning.append(newKey);
            warning.append(" as it is not defined in your configuration.");
            info(warning.toString());
            return;
        }
        
        // Parameter exists and can be renamed safely
        // Backup the source file
        String source = getConfiguration().getSource(key);
        backup(source, VersionManager.getInstance().getConfigurationVersion());
        // Extract the list of parameters
        ArrayList<String> parameters = getLines(source);
        // Find the line of parameter defined by the key
        int iline = findLine(key, parameters);
        // Update the name of the key
        String updatedParameter = parameters.get(iline).replace(key, newKey);
        parameters.set(iline, updatedParameter);
        // Add comment
        StringBuilder msg = new StringBuilder();
        msg.append(toString());
        msg.append(" Renamed parameter ");
        msg.append(key);
        msg.append(" into ");
        msg.append(newKey);
        parameters.add(iline, "# " + msg);
        // Print the change in the console
        msg.insert(0, "  ");
        msg.append(" (");
        msg.append(source);
        msg.append(")");
        info(" " + msg.substring(msg.indexOf(")") + 1));
        // Write the updated configuration file
        write(source, parameters);
    }

    protected void updateValue(String key, String newValue) {

        // Check whether the parameter exists in the current configuration
        // If not add it
        if (!getConfiguration().canFind(key)) {
            addParameter(key, newValue);
            return;
        }
        // Check whether the new value is similar to the current value
        if (getConfiguration().getString(key).matches(newValue)) {
            StringBuilder warning = new StringBuilder();
            warning.append("  Did not update ");
            warning.append(key);
            warning.append(" value as it is already up-to-date.");
            info(warning.toString());
            return;
        }
        // Backup the source file
        String source = getConfiguration().getSource(key);
        backup(source, VersionManager.getInstance().getConfigurationVersion());
        // Extract the list of parameters
        ArrayList<String> parameters = getLines(source);
        // Find the line of parameter defined by the key
        int iline = findLine(key, parameters);
        // Update the value of the parameter
        String value = getConfiguration().getString(key);
        String updatedParameter = parameters.get(iline).replace(value, newValue);
        parameters.set(iline, updatedParameter);
        // Add comment
        StringBuilder msg = new StringBuilder();
        msg.append(toString());
        msg.append(" Updated parameter ");
        msg.append(key);
        msg.append(" to ");
        msg.append(newValue);
        parameters.add(iline, "# " + msg);
        // Print the change in the console
        msg.insert(0, "  ");
        msg.append(" (");
        msg.append(source);
        msg.append(")");
        info(" " + msg.substring(msg.indexOf(")") + 1));
        // Write the updated configuration file
        write(source, parameters);
    }

    protected void write(String file, ArrayList<String> lines) {
        BufferedWriter bfr = null;
        try {
            bfr = new BufferedWriter(new FileWriter(file));
            for (String line : lines) {
                bfr.append(line);
                bfr.newLine();
            }
            bfr.close();
        } catch (IOException ex) {
            error("Error writing configuration file " + file, ex);
        } finally {
            try {
                bfr.flush();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    protected ArrayList<String> getLines(String file) {
        ArrayList<String> lines = new ArrayList();
        try {
            BufferedReader bfr = new BufferedReader(new FileReader(file));
            String line = null;
            int iline = 1;
            try {
                while ((line = bfr.readLine()) != null) {
                    lines.add(line.trim());
                    iline++;
                }
            } catch (IOException ex) {
                error("Error loading parameters from file " + file + " at line " + iline + " " + line, ex);
            }
        } catch (FileNotFoundException ex) {
            error("Could not fing Osmose configuration file: " + file, ex);
        }
        return lines;
    }
    
    protected String backup(String src, AbstractVersion srcVersion) {

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
        formatter.setCalendar(calendar);
        StringBuilder bak = new StringBuilder(src);
        bak.append(".backup-");
        bak.append(srcVersion.toShortString());
        bak.append('-');
        bak.append(formatter.format(calendar.getTime()));
        // If the backup file already exist, no need to backup again
        // It means it has been saved withing the last minute
        if (!new File(bak.toString()).exists()) {
            try {
                copyFile(new File(src), new File(bak.toString()));
                return bak.toString();
            } catch (IOException ex) {
                error("Failed to backup configuration file " + src, ex);
            }
        }
        return null;
    }

    private void copyFile(File src, File dest) throws IOException {

        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);

        java.nio.channels.FileChannel channelSrc = fis.getChannel();
        java.nio.channels.FileChannel channelDest = fos.getChannel();

        channelSrc.transferTo(0, channelSrc.size(), channelDest);

        fis.close();
        fos.close();
    }

}
