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
package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.logging.OLogger;
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

/**
 *
 * @author pverley
 */
public class UpdateManager extends OLogger {

    private static final UpdateManager updateManager = new UpdateManager();

    public static UpdateManager getInstance() {
        return updateManager;
    }

    /*
     * Upgrade the configuration file to the application version.
     */
    public void upgrade() {
        
        Version appVersion = Version.CURRENT;
        Version cfgVersion = getConfigurationVersion();

        // Check version
        if (cfgVersion.priorTo(appVersion)) {
            info("Configuration version {0} does not match Osmose version {1}. Your configuration file will be automatically updated.", new Object[]{cfgVersion, appVersion});
        } else {
            info("Configuration version {0} matches Osmose version. Nothing to do.", cfgVersion);
            return;
        }

        // Update version
        String source = getConfiguration().getSource("osmose.version");
        backupCfgFile(source);
        ArrayList<String> cfgfile = getLines(source);
        cfgfile = updateParam("osmose.version", "osmose.version;" + appVersion.toShortString(), cfgfile, "Version " + appVersion.toString() + " - Added version number");
        writeCfgFile(source, cfgfile);

    }

    private Version getConfigurationVersion() {
        return Osmose.getInstance().getConfiguration().getVersion();
    }

    private ArrayList<String> deprecateParam(String key, ArrayList<String> parameters, String msg) {

        ArrayList<String> newList = new ArrayList(parameters);
        int iline = -1;
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).startsWith(key)) {
                iline = i;
                break;
            }
        }
        String deprecated = "# " + parameters.get(iline);
        newList.set(iline, deprecated);
        newList.add(iline, "# " + msg);
        return newList;
    }

    private ArrayList<String> renameParam(String key, String newKey, ArrayList<String> parameters, String msg) {

        ArrayList<String> newList = new ArrayList(parameters);
        int iline = -1;
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).startsWith(key)) {
                iline = i;
                break;
            }
        }
        String newName = parameters.get(iline).replace(key, newKey);
        newList.set(iline, newName);
        newList.add(iline, "# " + msg);
        return newList;
    }

    private ArrayList<String> updateParam(String key, String parameter, ArrayList<String> parameters, String msg) {

        ArrayList<String> newList = new ArrayList(parameters);
        int iline = -1;
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).startsWith(key)) {
                iline = i;
                break;
            }
        }

        newList.set(iline, parameter);
        newList.add(iline, "# " + msg);
        return newList;
    }

    private void writeCfgFile(String file, ArrayList<String> lines) {
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

    private ArrayList<String> getLines(String file) {
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

    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    private String backupCfgFile(String src) {

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
        formatter.setCalendar(calendar);
        StringBuilder bak = new StringBuilder(src.toString());
        bak.append(".bak");
        bak.append(formatter.format(calendar.getTime()));
        try {
            copyFile(new File(src), new File(bak.toString()));
            return bak.toString();
        } catch (IOException ex) {
            error("Failed to backup configuration file " + src, ex);
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
