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

package fr.ird.osmose.resource;

import java.io.IOException;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.io.ForcingFile;
import fr.ird.osmose.util.io.ForcingFileCaching;

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
    private double uBiomass;

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
     * Resource index. [0, nbkg -1] for background species, [nbkg, nbkg + nrsc - 1]
     * for resource species.
     */
    private int index;

    private ForcingFile forcingFile = null;

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
            warning("Resource biomass for resource group " + fileindex + " will be multiplied by " + multiplier
                    + " accordingly to parameter "
                    + getConfiguration().printParameter("species.multiplier.sp" + fileindex));
        } else {
            multiplier = 1.d;
        }

        // biomass multiplier
        if (!getConfiguration().isNull("species.offset.sp" + fileindex)) {
            offset = getConfiguration().getFloat("species.offset.sp" + fileindex);
            warning("Resource biomass for resource group " + fileindex + " will be offseted by " + offset
                    + " accordingly to parameter "
                    + getConfiguration().printParameter("species.offset.sp" + fileindex));
        } else {
            offset = 0.d;
        }

        if (!getConfiguration().isNull("species.biomass.total.sp" + fileindex)) {
            // uniform biomass
            uBiomass = getConfiguration().getDouble("species.biomass.total.sp" + fileindex) / getGrid().getNOceanCell();
            uBiomass = multiplier * (uBiomass + offset);

        } else if (!getConfiguration().isNull("species.file.sp" + fileindex)) {
            // biomass provided from NetCDF file
            // set negative value to uniform biomass
            uBiomass = -1.d;

            // check resource is properly defined in the NetCDF file
            String name = getConfiguration().getString("species.name.sp" + fileindex);
            String ncFile = getConfiguration().getFile("species.file.sp" + fileindex);

            int ncPerYear;
            if (!getConfiguration().isNull("species.biomass.nsteps.year.sp" + fileindex)) {
                ncPerYear = getConfiguration().getInt("species.biomass.nsteps.year.sp" + fileindex);
            } else {
                ncPerYear = getConfiguration().getInt("species.biomass.nsteps.year");
            }

            if (!getConfiguration().isNull("species.file.caching.sp" + fileindex)) {
                caching = ForcingFileCaching
                        .valueOf(getConfiguration().getString("species.file.caching.sp" + fileindex).toUpperCase());
            }

            this.forcingFile = new ForcingFile(name, ncFile, ncPerYear, 0.0, this.multiplier, caching);
            this.forcingFile.init();
        }

    }

    /**
     * Updates the biomass of the resource groups at the current time step of the
     * simulation.
     *
     * @param iStepSimu, the current step of the simulation
     */
    public void update(int iStepSimu) {

        // uniform biomass, nothing to update
        if (uBiomass >= 0.d) {
            return;
        }

        this.forcingFile.update(iStepSimu);

    }

    public double getBiomass(Cell cell) {
        return (uBiomass >= 0) ? uBiomass : this.forcingFile.getVariable(cell);
    }

    public int getIndex() {
        return this.index;
    }

    public int getFileIndex() {
        return this.fileindex;
    }

}
