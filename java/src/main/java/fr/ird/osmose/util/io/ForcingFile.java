/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
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

package fr.ird.osmose.util.io;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.OsmoseLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Class that helps managing input files (i.e. LTL or physical variables).
 * 
 * It is initialized by giving three variables:
 * - A file pattern
 * - A variable name
 * - A number of time-steps per year
 * 
 */
public class ForcingFile extends OsmoseLinker {

    /**
     * Array of double that stores the biomass on the Osmose grid for the
     * current time step of the simulation.
     * {@code variable[nline_osmose][ncolumn_osmose]}. The array is updated every
     * time step in the {@link #update(int)} function.
     */
    private double[][][] variable;

    /**
     * Cached variable arrays.
     */
    private HashMap<Integer, double[][][]> cachedVariable;

    /**
     * Caching mode.
     */
    private ForcingFileCaching caching = ForcingFileCaching.ALL;

    /**
     * Length of the NetCDF time series.
     */
    private int timeLength;

    /** List of resource file names. */
    private Object[] fileNames;

    /** Number of time-steps stored in each file. */
    private int[] nSteps;

    /** Number of files. */
    private int nFiles;

    /** Index of the file to read for each time step. */
    private int[] fileMapping;

    /**
     * Index of the time step to each for each simulated time step. **
     */
    private int[] stepMapping;

    /** Number of time steps within a year.*/
    private final int ncPerYear;
    
    /** Name of the variable to read. */
    private final String varName;
    
    /** File pattern to match. */
    private final String filePattern;
    
    private final double offset;
    private final double factor;
    
    private int previousNcStep = -1;
    
    //////////////
    // Constructor
    //////////////
    public ForcingFile(String varName, String filePattern, int ncPerYear, double offset, double factor, ForcingFileCaching caching) {
        this.varName = varName;
        this.filePattern = filePattern;
        this.ncPerYear = ncPerYear;
        this.caching = caching;
        this.factor = factor;
        this.offset = offset;
    }

    ////////////////////////////
    // Definition of the methods
    ////////////////////////////
    /**
     * Reads and checks parameters from configuration file.
     */
    public void init() throws IOException {

        String ncFile;
        List<String> listFiles = new ArrayList<>();

        // Recover the file pattern to match
        String pattern = new File(this.filePattern).getName();
        
        // Recover the directory that is used with the file pattern
        File directory = new File(this.filePattern).getParentFile();
        
        // Check whether the files match the pattern
        String[] fileList = directory.list();
        for (String f : fileList) {
            if (f.matches(pattern)) {
                File tempfile = new File(directory, f);
                listFiles.add(tempfile.getAbsolutePath());
            }
        }

        // Extract the files as a list of sorted String
        this.fileNames = listFiles.stream().sorted().toArray();
        this.nFiles = fileNames.length;
        
        if (this.nFiles == 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("No file has been found to match the patter " + this.filePattern + "\n");
            msg.append("The program will stop");
            error("Error reading resource file", new IOException(msg.toString()));
        }
        
        this.nSteps = new int[this.nFiles];
        int cpt = 0;

        // Loop over all the files 
        for (int iFile=0; iFile<this.nFiles; iFile++) {

            String temp = (String) fileNames[iFile];
            ncFile = new File(temp).getAbsolutePath();

            if (!new File(ncFile).exists()) {
                error("Error reading file " + ncFile,
                        new FileNotFoundException("NetCDF file " + ncFile + " does not exist."));
            }

            try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
                Variable variable = nc.findVariable(this.varName);
                int[] shape = variable.getShape();
                int nDims = shape.length;
                // check time length
                nSteps[cpt] = shape[0];
                cpt++;
                // check grid dimension
                if (getGrid().get_ny() != shape[nDims - 2] | getGrid().get_nx() != shape[nDims - 1]) {
                    throw new IOException("NetCDF grid dimensions of variable " + this.varName
                            + " does not match Osmose grid dimensions");
                }
            } catch (IOException ex) {
                error("NetCDF file " + ncFile + ", variable " + this.varName + "cannot be read", ex);
            }
        } // end of loop over files

        this.initTimeMapping();

        // prevent irrelevant caching mode : incremental caching requested but
        // NetCDF time series as long as simulation time
        if (caching.equals(ForcingFileCaching.INCREMENTAL) && (timeLength == getConfiguration().getNStep())) {
            caching = ForcingFileCaching.NONE;
        }

        // initializes the biomass cache
        if (!caching.equals(ForcingFileCaching.NONE)) {
            cachedVariable = new HashMap<>();
        }
    
        if(caching.equals(ForcingFileCaching.ALL)) { 
            this.loadCachedVariable();   
        }
    }
    
    private void loadCachedVariable() {
        // cache whole time series at first time step
        for (int iTime = 0; iTime < timeLength; iTime++) {
            debug("Caching from variable " + this.varName + " time step " + iTime);
            cachedVariable.put(iTime, readVariable(iTime));
        }
    }

    public void initTimeMapping() throws IOException {

        // Total number of time steps
        timeLength = 0;
        for (int c : this.nSteps) {
            timeLength += c;
        }

        /*
        if ((timeLength < getConfiguration().getNStep()) && (timeLength % getConfiguration().getNStepYear() != 0)) {
            throw new IOException("Time dimension of the NetCDF variale " + this.varName
                    + " must be a multiple of the number of time steps per year");
        }
        */

        // Recover the mapping between file / step
        // For exemple, for two files containing 6 time steps
        // fileMapping = 0 0 0 0 0 0 1 1 1 1 1 1
        // stepMapping = 0 1 2 3 4 5 0 1 2 3 4 5
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

    /**
     * Updates the biomass of the resource groups at the current time step of the
     * simulation.
     *
     * @param iStepSimu, the current step of the simulation
     */
    public void update(int iStepSimu) {

        int ndt = this.getConfiguration().getNStepYear();
        int iStepNc = (iStepSimu / (ndt / this.ncPerYear)) % timeLength;
        
        // If the Nc step to read has not changed, nothing is done.
        // Variable is kept unchanged.
        if(iStepNc == this.previousNcStep) { 
            return;
        }

        switch (this.caching) {
            case ALL:
                // retrieve biomass from cache
                this.variable = this.cachedVariable.get(iStepNc);
                break;
            case INCREMENTAL:
                if (!cachedVariable.containsKey(iStepNc)) {
                    // cache current netcdf time step
                    debug("Caching from variable " + this.varName + " time step " + iStepNc);
                    this.cachedVariable.put(iStepNc, readVariable(iStepNc));
                }
                // retrieve biomass from cache
                this.variable = cachedVariable.get(iStepNc);
                break;
            case NONE:
                // update biomass at current time step, no caching
                debug("Reading from variable " + this.varName + " time step " + iStepNc);
                this.variable = readVariable(iStepNc);
                break;
        }
        
        // Update previous step to the current step.
        this.previousNcStep = iStepNc;
        
    }

    public double getVariable(Cell cell) {
        return variable[0][cell.get_jgrid()][cell.get_igrid()];
    }
    
    public double getVariable(Cell cell, int layer) {
        return variable[layer][cell.get_jgrid()][cell.get_igrid()];
    }

    private double[][][] readVariable(int iStepNc) {

        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();

        int iFile = this.fileMapping[iStepNc];
        int iStep = this.stepMapping[iStepNc];

        // String ncFile = getConfiguration().resolve(this.fileNames[iFile],
        // getConfiguration().getMainFile());
        String ncFile = (String) this.fileNames[iFile];

        int nlayer;
        double[][][] output = null;

        // String ncFile = getConfiguration().getFile("species.file.sp" + index);
        try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
            // String message = String.format("Step=%d ==> Reading %s from %s, step=%d",
            // iStepNc, name, ncFile, iStep);
            // this.getLogger().info(message);
            Variable ncVariable = nc.findVariable(this.varName);
            Array ncArray;
            if (ncVariable.getShape().length == 3) {
                nlayer = 1;
                ncArray = ncVariable.read(new int[] { iStep, 0, 0 }, new int[] { 1, ny, nx }).reduce();
                Index ncindex = ncArray.getIndex();
                output = new double[1][ny][nx];
                for (Cell cell : getGrid().getCells()) {
                    if (cell.isLand()) {
                        continue;
                    }
                    int i = cell.get_igrid();
                    int j = cell.get_jgrid();
                    ncindex.set(j, i);
                    output[0][j][i] = this.factor * (ncArray.getDouble(ncindex) + this.offset);
                }
            } else if (ncVariable.getShape().length == 4) {
                nlayer = ncVariable.getShape()[1];
                ncArray = ncVariable.read(new int[] { iStep, 0, 0, 0 }, new int[] { 1, nlayer, ny, nx }).reduce();
                Index ncindex = ncArray.getIndex();
                output = new double[nlayer][ny][nx];
                for (Cell cell : getGrid().getCells()) { 
                    if (cell.isLand()) {
                        continue;
                    }   
                    int i = cell.get_igrid();
                    int j = cell.get_jgrid();
                    for (int k = 0; k < nlayer; k++) {
                        ncindex.set(k, j, i);
                        output[k][j][i] =  this.factor * (ncArray.getDouble(ncindex) + this.offset);
                    }
                };
            } else {
                String message = String.format(
                        "Variable %s must be 3D (time, lat, lon) or 4D (time, z, lat, lon). It currently has %d dimensions",
                        this.varName, ncVariable.getShape().length);
                error(message, new Exception());
            }
            
            
        } catch (IOException | InvalidRangeException ex) {
            error("File " + ncFile + ", variable " + this.varName + "cannot be read", ex);
        }
        
        return output;
    
    }
    
    public int getTimeLength() {
        return this.timeLength;
    }
    
    public int getNcStep(int iStepSimu) {  
        int ndt = this.getConfiguration().getNStepYear();
        int iStepNc = (iStepSimu / (ndt / this.ncPerYear)) % timeLength;
        return iStepNc;
    }
    
    public String getNcFileName(int iNcStep) {
        return (String) this.fileNames[this.fileMapping[iNcStep]];
    }
    
    public int getNcIndex(int iNcStep) {
        return this.stepMapping[iNcStep];
    }
    
    public HashMap<Integer, double[][][]> getCachedVariable() {
        return this.cachedVariable;
    }

}
