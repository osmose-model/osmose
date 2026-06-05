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
 * Nicolas BARRIER (nicolas.barrier@ird.fr)
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

import java.io.IOException;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.io.ForcingFile;
import fr.ird.osmose.util.io.ForcingFileCaching;
import fr.ird.osmose.util.timeseries.ForcingTimeSeries;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 4.2 2019/11/25
 */
public class ResourceForcing extends OsmoseLinker {

    /**
     * Index of the resource group.
     */
    private final int fileindex;

    /**
     * Caching mode.
     */
    private ForcingFileCaching caching = ForcingFileCaching.ALL;

    /**
     * The constant biomass, in tonne, in a cell of the model. Parameter
     * 'species.biomass.total.sp#' provides the total biomass of a given resource
     * group in the system for every time step. This feature allows to consider a
     * resource group with a constant biomass uniformly distributed over the grid of
     * the model and over time. This feature has been added as a quick patch for a
     * configuration that seems to lack a food compartment and as a result cannot
     * reach any biomass equilibrium. It provides to the system a constant pool of
     * biomass throughout time. It should only be used for "debugging" a
     * configuration.
     */
    private double uBiomass[] = null;

    /**
     * The biomass, in tonne, in a cell of the model, considering time variability. Parameter
     * 'species.biomass.sp#' provides the total biomass of a given resource
     * group in the system in combination with species.biomass.nsteps.year.sp#.
     */
    private double currentBiomass;

    /**
     * Multiplier of the resource biomass. Parameter 'species.multiplier.sp#' for
     * virtually increasing or decreasing resource biomass.
     */
    private double multiplier;

    /**
     * Offset of the resource biomass. Parameter 'species.offset.sp#' for virtually
     * increasing or decreasing resource biomass.
     */
    private double offset;

    /**
     * Resource index. [0, nrsc -1]
     * for resource species.
     */
    private int index;

    private ForcingFile forcingFile = null;

    private enum ResourceForcingMode {
        UNIFORM_BIOMASS,
        NETCDF_BIOMASS,
        COMBINED_TS_NETCDF_BIOMASS;
    }

    @FunctionalInterface
    private interface UpdateInterface {
        void update(int iStepSimu);
    }

    @FunctionalInterface
    private interface BiomassInterface {
        double getBiomass(Cell cell);
    }

    private UpdateInterface updateInterface;
    private BiomassInterface biomassInterface;

    // Set the default value of resourceForcingMode to Netcdf
    private ResourceForcingMode resourceForcingMode = ResourceForcingMode.NETCDF_BIOMASS;

    //////////////
    // Constructor
    //////////////
    public ResourceForcing(int fileindex, int index) {
        this.fileindex = fileindex;
        this.index = index;
    }

    ////////////////////////////
    // Definition of the methods
    ////////////////////////////
    /**
     * Reads and checks parameters from configuration file.
     */
    public void init() throws IOException {

        // biomass multiplier
        if (!getConfiguration().isNull("species.multiplier.sp" + fileindex)) {
            multiplier = getConfiguration().getFloat("species.multiplier.sp" + fileindex);
            warning("Biomass for resource group " + fileindex + " will be multiplied by " + multiplier
                    + " accordingly to parameter "
                    + getConfiguration().printParameter("species.multiplier.sp" + fileindex));
        } else {
            multiplier = 1.d;
        }

        // biomass offset
        if (!getConfiguration().isNull("species.offset.sp" + fileindex)) {
            offset = getConfiguration().getFloat("species.offset.sp" + fileindex);
            warning("Biomass for resource group " + fileindex + " will be offseted by " + offset
                    + " accordingly to parameter "
                    + getConfiguration().printParameter("species.offset.sp" + fileindex));
        } else {
            offset = 0.d;
        }

        if (!getConfiguration().isNull("species.biomass.mode.sp" + fileindex)) {
            resourceForcingMode = ResourceForcingMode.valueOf(getConfiguration().getString("species.biomass.mode.sp" + fileindex).toUpperCase());
        }

        if (resourceForcingMode == ResourceForcingMode.UNIFORM_BIOMASS) {
            updateInterface = (fileindex -> updateUniform(fileindex));
            biomassInterface = (cell -> getBiomassUniform(cell));
        } else if (resourceForcingMode == ResourceForcingMode.NETCDF_BIOMASS) {
            updateInterface = (fileindex -> updateNetcdf(fileindex));
            biomassInterface = (cell -> getBiomassNetcdf(cell));
        } else if (resourceForcingMode == ResourceForcingMode.COMBINED_TS_NETCDF_BIOMASS) {
            updateInterface = (fileindex -> updateCombined(fileindex));
            biomassInterface = (cell -> getBiomassCombined(cell));
        }

        // If the resource forcing mode is either uniform biomass or combined, we need to load the uniform biomass time-series
        if ((resourceForcingMode == ResourceForcingMode.UNIFORM_BIOMASS) || (resourceForcingMode == ResourceForcingMode.COMBINED_TS_NETCDF_BIOMASS)){

            // initialize the uniform biomass time-series
            uBiomass = new double[getConfiguration().getNStep()];

            // If we have uniform biomass, two cases: constant biomass
            if (!getConfiguration().isNull("species.biomass.constant.sp" + fileindex)) {

                double constantuBiomass = getConfiguration().getDouble("species.biomass.constant.sp" + fileindex);
                constantuBiomass = multiplier * (constantuBiomass + offset) / getGrid().getNOceanCell();
                for (int t = 0; t < getConfiguration().getNStep(); t++) {
                    uBiomass[t] = constantuBiomass;
                }
            } else if (!getConfiguration().isNull("species.biomass.sp" + fileindex)) {

                String keyVal = "species.biomass.sp" + fileindex;
                String keyShift = "species.biomass.nsteps.year.sp" + fileindex;
                ForcingTimeSeries biomassSeries = new ForcingTimeSeries(keyShift, keyVal);
                biomassSeries.init();
                double fBiomass[] = biomassSeries.getValues();

                for (int t = 0; t < fBiomass.length; t++) {
                    uBiomass[t] = multiplier * (fBiomass[t] + offset) / getGrid().getNOceanCell();
                }

            } else {
                error("UNIFORM_BIOMASS resource forcing is used but parameters are missing", new Exception());
            } // end of check on parameter for uniform biomass

        } else if ((resourceForcingMode == ResourceForcingMode.NETCDF_BIOMASS) || (resourceForcingMode == ResourceForcingMode.COMBINED_TS_NETCDF_BIOMASS)) {

            String prefix = (resourceForcingMode == ResourceForcingMode.NETCDF_BIOMASS) ? "species.biomass" : "species.relative.biomass";

            if (!getConfiguration().isNull(prefix)) {

                // check resource is properly defined in the NetCDF file
                String name = getConfiguration().getString(prefix + ".varname.sp" + fileindex);
                String ncFile = getConfiguration().getFile(prefix + ".file.sp" + fileindex);

                int ncPerYear;
                if (!getConfiguration().isNull(prefix + ".nsteps.year.sp" + fileindex)) {
                    ncPerYear = getConfiguration().getInt(prefix + ".nsteps.year.sp" + fileindex);
                } else {
                    ncPerYear = getConfiguration().getInt(prefix + ".nsteps.year");
                }

                if (!getConfiguration().isNull(prefix + ".file.caching.sp" + fileindex)) {
                    caching = ForcingFileCaching
                            .valueOf(getConfiguration().getString(prefix + ".file.caching.sp" + fileindex).toUpperCase());
                }

                if (resourceForcingMode == ResourceForcingMode.COMBINED_TS_NETCDF_BIOMASS) {
                    // if combined mode, then force offset to 0 and factor to 1
                    // since it is applied to the uniform biomass
                    this.forcingFile = new ForcingFile(name, ncFile, ncPerYear, 0, 1, caching, true);
                } else {
                    this.forcingFile = new ForcingFile(name, ncFile, ncPerYear, offset, this.multiplier, caching);
                }
                this.forcingFile.init();

            } else {
                 error("NETCDF_BIOMASS resource forcing is used but parameters are missing", new Exception());
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
        updateInterface.update(iStepSimu);
    }

    /** Updates the uniform biomass time series */
    public void updateUniform(int iStepSimu) {
        currentBiomass = uBiomass[iStepSimu];
    }

    /**
     * Updates the NetCDF file in case of NetCDF biomass
     *
     */
    public void updateNetcdf(int iStepSimu) {
       this.forcingFile.update(iStepSimu);
    }

    /**
     * Updates the NetCDF file for relative biomass and the biomass time-series
     * in case of deconvoluted spatial and temporal dynamics.
     *
    */
    public void updateCombined(int iStepSimu) {
        this.forcingFile.update(iStepSimu);
        currentBiomass = uBiomass[iStepSimu];
    }

    /** Get the biomass using a functional interface */
    public double getBiomass(Cell cell) {
        return biomassInterface.getBiomass(cell);
    }

    /**
     * Get the uniform biomass value
     */
    public double getBiomassUniform(Cell cell) {
        return currentBiomass;
    }

    /**
     * Get the NetCDF biomass value
    */
    public double getBiomassNetcdf(Cell cell) {
        return this.forcingFile.getVariable(cell);
    }

    /**
     * Get the combined biomass value
    */
    public double getBiomassCombined(Cell cell) {
        return currentBiomass * this.forcingFile.getVariable(cell);
    }

    public int getIndex() {
        return this.index;
    }

    public int getFileIndex() {
        return this.fileindex;
    }

}
