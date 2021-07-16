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

package fr.ird.osmose.process.bioen;

import java.io.IOException;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.util.SimulationLinker;
import fr.ird.osmose.util.io.ForcingFile;
import fr.ird.osmose.util.io.ForcingFileCaching;

/**
 * Class that is dedicated to the reading of Physical datasets used in the
 * bioenegertics module.
 *
 * @author nbarrier
 */
public class PhysicalData extends SimulationLinker {

    /**
     * Name of the variable to read, and from which parameters will be extracted.
     *
     * @param rank
     */
    private final String variable_name;

    /**
     * Conversion factors to have the proper units.
     */
    private double offset = 0;
    private double factor = 1;

    /**
     * Name of the variable in the NetCDF file.
     */
    private String netcdf_variable_name;

    /**
     * True if a constant value should be used.
     */
    private boolean useConstantVal;
    private double constantVal;

    /** Number of time steps within a year. */
    private int ncPerYear;

    private ForcingFile forcingFile = null;

    private ForcingFileCaching caching = ForcingFileCaching.ALL;

    public PhysicalData(int rank, String var_name) {
        super(rank);
        this.variable_name = var_name;
    }

    public void init() throws IOException {

        String key;

        // Recovering the conversion factors and offsets (temperature.factor,
        // temperature.offset)
        key = String.format("%s.factor", this.variable_name);
        if (getConfiguration().canFind(key)) {
            factor = getConfiguration().getDouble(key);
        }

        key = String.format("%s.offset", this.variable_name);
        if (getConfiguration().canFind(key)) {
            offset = getConfiguration().getDouble(key);
        }

        // Recovering the key temperature.filename
        key = String.format("%s.value", this.variable_name);

        if (getConfiguration().canFind(key)) {
            // if a constant value is provided, then set the value
            // and force boolean to true
            this.useConstantVal = true;
            this.constantVal = this.factor * (getConfiguration().getDouble(key) + this.offset);

        } else {

            // if no constant value is provided, ask for a netcdf file.
            this.useConstantVal = false;

            key = String.format("%s.nsteps.year", this.variable_name);
            ncPerYear = getConfiguration().getInt(key);

            // Recovering the name of the NetCDF variable (temperature.varname)
            key = String.format("%s.varname", this.variable_name);
            this.netcdf_variable_name = getConfiguration().getString(key);

            // Recovering the key temperature.filename
            key = String.format("%s.filename", this.variable_name);
            String filename = getConfiguration().getFile(key);

            key = String.format("%s.caching", this.variable_name);
            if (!getConfiguration().isNull(key)) {
                caching = ForcingFileCaching.valueOf(getConfiguration().getString(key).toUpperCase());
            }

            this.forcingFile = new ForcingFile(netcdf_variable_name, filename, ncPerYear, this.offset, this.factor,
                    caching);
            this.forcingFile.init();

        }

    } // end of init method

    /**
     * Recovers the value of a physicald dataset providing the depth index. Value is
     * converted as follows: output = factor * (offset + value)
     *
     * @param index
     *            Depth index (0 = surface, 1=mean, 2=bottom)
     * @param cell
     *            Cell index
     * @return
     */
    public double getValue(int index, Cell cell) {
        if (this.useConstantVal) {
            return this.constantVal;
        } else {
            return this.forcingFile.getVariable(cell, index);
        }
    }

    /**
     * Recovers the value of a physicald dataset providing the depth index. Value is
     * converted as follows: output = factor * (offset + value)
     *
     * @param school
     * @return
     */
    public double getValue(School school) {
        Cell cell = school.getCell();
        int index = school.getSpecies().getDepthLayer();
        return this.getValue(index, cell);
    }
    
    /**
     * Updates the biomass of the resource groups at the current time step of the
     * simulation.
     *
     * @param iStepSimu, the current step of the simulation
     */
    public void update(int iStepSimu) {

        // uniform variable, nothing to update
        if (this.useConstantVal) {
            return;
        }

        this.forcingFile.update(iStepSimu);

    }

} // end of class
