/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.version;

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
public abstract class AbstractVersion extends OLogger implements Comparable<AbstractVersion> {

    final private int number;

    final private int update;

    final private String date;

    final private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

    abstract void update();

    AbstractVersion(int number, int update, int year, int month, int day) {
        this.number = number;
        this.update = update;
        Calendar cld = Calendar.getInstance();
        cld.set(year, month, day);
        sdf.setCalendar(cld);
        date = sdf.format(cld.getTime());
    }

    void updateConfiguration() {

        // Update parameters
        update();

        // Update version
        if (!getConfiguration().isNull("osmose.version")) {
            String source = getConfiguration().getSource("osmose.version");
            backup(source);
            ArrayList<String> cfgfile = getLines(source);
            cfgfile = replace("osmose.version", "osmose.version;" + toString(), cfgfile, "Version " + toString() + " - Updated version number");
            write(source, cfgfile);
        } else {
            String source = getConfiguration().getMainFile();
            backup(source);
            ArrayList<String> cfgfile = getLines(source);
            cfgfile = add("osmose.version;" + toString(), cfgfile, "Version " + toString() + " - Added version number");
            write(source, cfgfile);
        }
    }

    public int getNumber() {
        return number;
    }

    public int getUpdate() {
        return update;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        StringBuilder version = new StringBuilder();
        version.append(number);
        if (update > 0) {
            version.append(" update ");
            version.append(update);
        }
        version.append(" (");
        version.append(date);
        version.append(")");
        return version.toString();
    }

    public boolean priorTo(AbstractVersion otherVersion) {
        return compareTo(otherVersion) < 0;
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

        // Same version number and same update number, versions are equal
        return 0;
    }

    protected ArrayList<String> add(String parameter, ArrayList<String> parameters, String msg) {
        ArrayList<String> newList = new ArrayList(parameters);
        newList.add("# " + msg);
        newList.add(parameter);
        return newList;
    }

    protected ArrayList<String> deprecate(String key, ArrayList<String> parameters, String msg) {

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

    protected ArrayList<String> rename(String key, String newKey, ArrayList<String> parameters, String msg) {

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

    protected ArrayList<String> replace(String key, String parameter, ArrayList<String> parameters, String msg) {

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

    protected Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    protected String backup(String src) {

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
        formatter.setCalendar(calendar);
        StringBuilder bak = new StringBuilder(src.toString());
        bak.append(".bak");
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
