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

package fr.ird.osmose.resource;

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
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 4.2 2019/11/25
 */
public class ResourceForcing extends OsmoseLinker {

    /**
     * Index of the resource group.
     */
    private final int index;

    /**
     * Array of double that stores the biomass, in tonne, on the Osmose grid for
     * the current time step of the simulation.
     * {@code biomass[nline_osmose][ncolumn_osmose]}. The array is updated every
     * time step in the {@link #update(int)} function.
     */
    private double[][] biomass;

    /**
     * Cached biomass arrays.
     */
    private HashMap<Integer, double[][]> cachedBiomass;

    /**
     * Caching mode.
     */
    private ResourceCaching caching = ResourceCaching.ALL;

    /**
     * The constant biomass, in tonne, in a cell of the model. Parameter
     * 'species.biomass.total.sp#' provides the total biomass of a given
     * resource group in the system for every time step. This feature allows to
     * consider a resource group with a constant biomass uniformly distributed
     * over the grid of the model and over time. This feature has been added as
     * a quick patch for a configuration that seems to lack a food compartment
     * and as a result cannot reach any biomass equilibrium. It provides to the
     * system a constant pool of biomass throughout time. It should only be used
     * for "debugging" a configuration.
     */
    private double uBiomass;

    /**
     * Multiplier of the resource biomass. Parameter 'species.multiplier.sp#'
     * for virtually increasing or decreasing resource biomass.
     */
    private double multiplier;

    /**
     * Length of the NetCDF time series.
     */
    private int timeLength;
    
    /** List of resource file names. */
    private String[] fileNames;
    
    /** Number of time-steps stored in each file. */
    private int[] nSteps;
    
    /** Number of files. */
    private int nFiles;
    
    /** Index of the file to read for each time step. */
    private int[] fileMapping;
    
    /** Index of the time step to each for each simulated time step. **
     *
     * @param index 
     */
    private int[] stepMapping;
    
//////////////
// Constructor
//////////////
    public ResourceForcing(int index) {
        this.index = index;
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Reads and checks parameters from configuration file.
     */
    public void init() throws IOException {

        List<String> listFiles = new ArrayList<>();

        if (!getConfiguration().isNull("species.biomass.total.sp" + index)) {
            // uniform biomass
            uBiomass = getConfiguration().getDouble("species.biomass.total.sp" + index) / getGrid().getNOceanCell();

        } else if (!getConfiguration().isNull("species.file.sp" + index)) {
            // biomass provided from NetCDF file
            // set negative value to uniform biomass
            uBiomass = -1.d;

            // check resource is properly defined in the NetCDF file
            String name = getConfiguration().getString("species.name.sp" + index);
            String ncFile = getConfiguration().getFile("species.file.sp" + index);

            // Recover the file pattern to match
            String pattern = new File(ncFile).getName();

            File directory = new File(ncFile).getParentFile();
            String[] fileList = directory.list();
            for (String f : fileList) {
                if (f.matches(pattern)) {
                    listFiles.add(f);
                }
            }
            
            Object[] tempObj = listFiles.stream().sorted().toArray();
            fileNames = new String[tempObj.length];
            for(int k=0; k<tempObj.length; k++) { 
                fileNames[k] = (String) tempObj[k];   
            }
            
            this.nFiles = fileNames.length;
            this.nSteps = new int[this.nFiles];
            int cpt = 0;

            for (String temp : fileNames) {

                ncFile = new File(directory, temp).getAbsolutePath();
                
                if (!new File(ncFile).exists()) {
                    error("Error reading forcing parameters for resource group " + index, new FileNotFoundException("NetCDF file " + ncFile + " does not exist."));
                }

                try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
                    Variable variable = nc.findVariable(name);
                    int[] shape = variable.getShape();
                    // check time length
                    nSteps[cpt] = shape[0];
                    cpt++;
                    // check grid dimension
                    if (getGrid().get_ny() != shape[1] | getGrid().get_nx() != shape[2]) {
                        throw new IOException("NetCDF grid dimensions of resource group " + index + " does not match Osmose grid dimensions");
                    }
                } catch (IOException ex) {
                    error("NetCDF file " + ncFile + ", variable " + name + "cannot be read", ex);
                }
            }

            // user-defined caching mode
            if (!getConfiguration().isNull("species.file.caching.sp" + index)) {
                caching = ResourceCaching.valueOf(getConfiguration().getString("species.file.caching.sp" + index).toUpperCase());
            }

        } else {
            error("No input file is provided for resource " + getConfiguration().getString("species.name.sp" + index), new IOException("Cannot initialize resource group " + index));
        }
                        
        this.initTimeMapping();

        // prevent irrelevant caching mode : incremental caching requested but 
        // NetCDF time series as long as simulation time
        if (caching.equals(ResourceCaching.INCREMENTAL) && (timeLength == getConfiguration().getNStep())) {
            caching = ResourceCaching.NONE;
        }

        // initializes the biomass cache
        if (!caching.equals(ResourceCaching.NONE) && (uBiomass < 0)) {
            cachedBiomass = new HashMap();
        }

        // biomass multiplier
        if (!getConfiguration().isNull("species.multiplier.sp" + index)) {
            multiplier = getConfiguration().getFloat("species.multiplier.sp" + index);
            warning("Resource biomass for resource group " + index + " will be multiplied by " + multiplier + " accordingly to parameter " + getConfiguration().printParameter("species.multiplier.sp" + index));
        } else {
            multiplier = 1.d;
        }       
    }
    
    public void initTimeMapping() throws IOException { 
        
        timeLength = 0;
        for (int c : this.nSteps) {
            timeLength += c;
        }

        if ((timeLength < getConfiguration().getNStep()) && (timeLength % getConfiguration().getNStepYear() != 0)) {
            throw new IOException("Time dimension of the NetCDF resource group " + index + " must be a multiple of the number of time steps per year");
        }
                     
        this.fileMapping = new int[timeLength];
        this.stepMapping = new int[timeLength];
        
        int cpt = 0;
        for (int ifile=0; ifile < this.nFiles; ifile++) {
            for(int istep=0; istep < this.nSteps[ifile]; istep++) {
                fileMapping[cpt] = ifile;
                stepMapping[cpt] = istep;
                cpt++;
            }
        }    
    }
    

    /**
     * Updates the biomass of the resource groups at the current time step of
     * the simulation.
     *
     * @param iStepSimu, the current step of the simulation
     */
    public void update(int iStepSimu) {

        // uniform biomass, nothing to update
        if (uBiomass >= 0.d) {
            return;
        }

        int iStepNc = iStepSimu % timeLength;

        switch (caching) {
            case ALL:
                if (iStepSimu == 0) {
                    // cache whole time series at first time step
                    for (int iTime = 0; iTime < timeLength; iTime++) {
                        debug("Caching biomass from resource group " + index + " time step " + iTime);
                        cachedBiomass.put(iTime, readBiomass(iTime));
                    }
                }
                // retrieve biomass from cache
                biomass = cachedBiomass.get(iStepNc);
                break;
            case INCREMENTAL:
                if (iStepSimu == iStepNc) {
                    // cache current time step
                    debug("Caching biomass from resource group " + index + " time step " + iStepNc);
                    cachedBiomass.put(iStepNc, readBiomass(iStepNc));
                }
                // retrieve biomass from cache
                biomass = cachedBiomass.get(iStepNc);
                break;
            case NONE:
                // update biomass at current time step, no caching
                debug("Reading biomass from resource group " + index + " time step " + iStepNc);
                biomass = readBiomass(iStepNc);
                break;
        }
    }

    public double getBiomass(Cell cell) {
        return multiplier * ((uBiomass >= 0) ? uBiomass : biomass[cell.get_jgrid()][cell.get_igrid()]);
    }

    private double[][] readBiomass(int iStepNc) {

        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();
        
        int iFile = this.fileMapping[iStepNc];
        int iStep = this.stepMapping[iStepNc];
       
        String ncFile = getConfiguration().resolve(this.fileNames[iFile], getConfiguration().getMainFile());

        double[][] rscbiomass = new double[ny][nx];

        String name = getConfiguration().getString("species.name.sp" + index);
        //String ncFile = getConfiguration().getFile("species.file.sp" + index);
        try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
            //String message = String.format("Step=%d ==> Reading %s from %s, step=%d", iStepNc, name, ncFile, iStep);
            //this.getLogger().info(message);
            Variable variable = nc.findVariable(name);
            Array ncbiomass = variable.read(new int[]{iStep, 0, 0}, new int[]{1, ny, nx}).reduce();
            Index index = ncbiomass.getIndex();
            getGrid().getCells().stream().filter((cell) -> (!cell.isLand())).forEach((cell) -> {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                index.set(j, i);
                rscbiomass[j][i] = ncbiomass.getDouble(index);
            });
        } catch (IOException | InvalidRangeException ex) {
            error("File " + ncFile + ", variable " + name + "cannot be read", ex);
        }
        return rscbiomass;
    }

}
