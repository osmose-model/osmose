/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
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
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author barrier
 */
public class FileManager extends OsmoseLinker {

    /**
     * String pattern. Can be a regular expression.
     */
    private final String pattern;

    /**
     * Sorted list of filenames.
     */
    private String[] fileNames;

    /**
     * NetCDF varname.
     */
    private final String varname;

    /**
     * Number of time-steps stored in each file.
     */
    private int[] nSteps;

    /**
     * Number of files.
     */
    private int nFiles;

    /**
     * Index of the file to read for each time step.
     */
    private int[] fileMapping;

    /**
     * Index of the time step to each for each simulated time step. **
     *
     * @param index
     */
    private int[] stepMapping;

    private int timeLength;

    public FileManager(String pattern, String varname) {
        this.pattern = pattern;
        this.varname = varname;
    }

    /**
     * Initialize the file list.
     *
     * @throws java.io.IOException
     */
    public void init() throws IOException {

        List<String> listFiles = new ArrayList<>();

        // Recover the file pattern to match
        String fileName = new File(pattern).getName();

        // Extract the files that match the pattern
        File directory = new File(fileName).getParentFile();
        String[] fileList = directory.list();
        for (String f : fileList) {
            if (f.matches(fileName)) {
                listFiles.add(f);
            }
        }
        
        // Extract the file list as array of string
        Object[] tempObj = listFiles.stream().sorted().toArray();
        fileNames = new String[tempObj.length];
        for (int k = 0; k < tempObj.length; k++) {
            fileNames[k] = (String) tempObj[k];
        }

        this.nFiles = fileNames.length;
        this.nSteps = new int[this.nFiles];
        int cpt = 0;

        // Loop over the files to extract the time step.
        // Warning: time is the first dimension
        for (String temp : fileNames) {

            String ncFile = new File(directory, temp).getAbsolutePath();

            if (!new File(ncFile).exists()) {
                error("Error reading file " + ncFile, new FileNotFoundException("NetCDF file " + ncFile + " does not exist."));
            }

            try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
                Variable variable = nc.findVariable(this.varname);
                int[] shape = variable.getShape();
                // check time length
                nSteps[cpt] = shape[0];
                cpt++;
            } catch (IOException ex) {
                error("NetCDF file " + ncFile + ", variable " + varname + "cannot be read", ex);
            }
        }

        this.initTimeMapping();

    }

    /** Init the file mapping and step mapping for all the simulation steps. 
     * 
     * @throws IOException 
     */
    public void initTimeMapping() throws IOException {

        // Total number of time steps
        for (int c : this.nSteps) {
            timeLength += c;
        }

        if ((timeLength < getConfiguration().getNStep()) && (timeLength % getConfiguration().getNStepYear() != 0)) {
            throw new IOException("Time dimension for the " + this.pattern + " files must be a multiple of the number of time steps per year");
        }

        this.fileMapping = new int[timeLength];
        this.stepMapping = new int[timeLength];

        int cpt = 0;
        for (int ifile = 0; ifile < this.nFiles; ifile++) {
            for (int istep = 0; istep < this.nSteps[ifile]; istep++) {
                fileMapping[cpt] = ifile;
                stepMapping[cpt] = istep;
                cpt++;
            }
        }
    }

    /** Get the file to read at the given time step. 
     * 
     * @param iStepSimu
     * @return 
     */
    public String getFile(int iStepSimu) {
        int iStepNc = iStepSimu % timeLength;
        int fileindex = this.fileMapping[iStepNc];
        return this.fileNames[fileindex];
    }

    /** Get the step to read at the given step.
     * 
     * @param iStepSimu
     * @return 
     */
    public int getStep(int iStepSimu) {
        int iStepNc = iStepSimu % timeLength;
        return this.stepMapping[iStepNc];
    }

}
