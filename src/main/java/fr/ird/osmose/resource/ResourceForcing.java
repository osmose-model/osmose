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
 * processes of fish life cycle (growth, explicit predation, additional and
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
package fr.ird.osmose.resource;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.OsmoseLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
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
     * 'resource.biomass.total.rsc#' provides the total biomass of a given
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
     * Multiplier of the resource biomass. Parameter 'resource.multiplier.rsc#'
     * for virtually increasing or decreasing resource biomass.
     */
    private double multiplier;

    /**
     * Length of the NetCDF time series.
     */
    private int timeLength;

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
    public void init() {

        if (!getConfiguration().isNull("resource.biomass.total.rsc" + index)) {
            // uniform biomass
            uBiomass = getConfiguration().getDouble("resource.biomass.total.rsc" + index) / getGrid().getNOceanCell();

        } else if (!getConfiguration().isNull("resource.file.rsc" + index)) {
            // biomass provided from NetCDF file
            // set negative value to uniform biomass
            uBiomass = -1.d;

            // check resource is properly defined in the NetCDF file
            String name = getConfiguration().getString("resource.name.rsc" + index);
            String ncFile = getConfiguration().getFile("resource.file.rsc" + index);

            if (!new File(ncFile).exists()) {
                error("Error reading forcing parameters for resource group " + index, new FileNotFoundException("NetCDF file " + ncFile + " does not exist."));
            }

            try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
                Variable variable = nc.findVariable(name);
                int[] shape = variable.getShape();
                // check time length
                if ((shape[0] < getConfiguration().getNStep()) && (shape[0] % getConfiguration().getNStepYear() != 0)) {
                    throw new IOException("Time dimension of the NetCDF resource group " + index + " must be a multiple of the number of time steps per year");
                }
                timeLength = shape[0];
                // check grid dimension
                if (getGrid().get_ny() != shape[1] | getGrid().get_nx() != shape[2]) {
                    throw new IOException("NetCDF grid dimensions of resource group " + index + " does not match Osmose grid dimensions");
                }
            } catch (IOException ex) {
                error("NetCDF file " + ncFile + ", variable " + name + "cannot be read", ex);
            }

            // user-defined caching mode
            if (!getConfiguration().isNull("resource.file.caching.rsc" + index)) {
                caching = ResourceCaching.valueOf(getConfiguration().getString("resource.file.caching.rsc" + index).toUpperCase());
            }

        } else {
            error("No input file is provided for resource " + getConfiguration().getString("resource.name.rsc" + index), new IOException("Cannot initialize resource group " + index));
        }

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
        if (!getConfiguration().isNull("resource.multiplier.rsc" + index)) {
            multiplier = getConfiguration().getFloat("resource.multiplier.rsc" + index);
            warning("Resource biomass for resource group " + index + " will be multiplied by " + multiplier + " accordingly to parameter " + getConfiguration().printParameter("resource.multiplier.rsc" + index));
        } else {
            multiplier = 1.d;
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

        double[][] rscbiomass = new double[ny][nx];

        String name = getConfiguration().getString("resource.name.rsc" + index);
        String ncFile = getConfiguration().getFile("resource.file.rsc" + index);
        try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
            Variable variable = nc.findVariable(name);
            Array ncbiomass = variable.read(new int[]{iStepNc, 0, 0}, new int[]{1, ny, nx}).reduce();
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
