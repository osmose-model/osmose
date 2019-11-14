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
public abstract class Release extends OsmoseLinker {

    final private VersionNumber versionNumber;

    abstract void updateParameters();
    
    Release(String versionNumber) {
        this.versionNumber = VersionNumber.valueOf(versionNumber);
    }

    void updateConfiguration() {

        info("Updating configuration file to " + versionNumber.toString() + "...");

        // Update parameters
        updateParameters();

        // Update version
        updateValue("osmose.version", versionNumber.toString());
    }

    public VersionNumber getVersionNumber() {
        return versionNumber;
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
            warning.append("  Did not add parameter ").append(key);
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
        msg.append("Osmose").append(versionNumber).append(" - ");
        msg.append("Added parameter ").append(parameter);
        parameters.add("# " + msg.toString());
        // Add parameter
        parameters.add(parameter.toString());
        // Print the change in the console
        msg.append(" (").append(source).append(")");
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
        msg.append("Osmose").append(versionNumber).append(" - ");
        msg.append(comment);
        parameters.add(iline, "# " + msg);
        // Print the change in the console
        msg.insert(0, "  ").append(" (").append(source).append(")");
        info(" " + msg.substring(msg.indexOf(")") + 1));
        // Write the updated configuration file
        write(source, parameters);
    }

    protected void updateKey(String key, String newKey) {

        // Check whether the parameter newKey exists already
        if (getConfiguration().canFind(newKey)) {
            StringBuilder msg = new StringBuilder();
            msg.append("  Did not rename ").append(key).append(" into ").append(newKey);
            msg.append(" as it is already defined in file").append(getConfiguration().getSource(newKey));
            info(msg.toString());
            commentParameter(key, msg.toString());
            return;
        }
        
        // Check whether the parameter exists in the current configuration
        if (!getConfiguration().canFind(key)) {
            StringBuilder warning = new StringBuilder();
            warning.append("  Did not rename ").append(key).append(" into ").append(newKey);
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
        msg.append("Osmose").append(versionNumber).append(" - ");
        msg.append("Renamed parameter ").append(key).append(" into ").append(newKey);
        parameters.add(iline, "# " + msg);
        // Print the change in the console
        msg.insert(0, "  ").append(" (").append(source).append(")");
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
            warning.append("  Did not update ").append(key).append(" value as it is already up-to-date.");
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
        msg.append("Osmose ").append(versionNumber).append(" - ");
        msg.append("Updated parameter ").append(key).append(" to ").append(newValue);
        parameters.add(iline, "# " + msg);
        // Print the change in the console
        msg.insert(0, "  ").append(" (").append(source).append(")");
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
    
    protected String backup(String src, VersionNumber srcVersion) {

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
        formatter.setCalendar(calendar);
        StringBuilder bak = new StringBuilder(src);
        bak.append(".backup-");
        bak.append(srcVersion);
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

        FileOutputStream fos;
        try (FileInputStream fis = new FileInputStream(src)) {
            fos = new FileOutputStream(dest);
            java.nio.channels.FileChannel channelSrc = fis.getChannel();
            java.nio.channels.FileChannel channelDest = fos.getChannel();
            channelSrc.transferTo(0, channelSrc.size(), channelDest);
        }
        fos.close();
    }

}
