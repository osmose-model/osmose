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
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;

/**
 * Class that is dedicated to the reading of Physical datasets used in the
 * bioenegertics module.
 *
 * @author nbarrier
 */
public class PhysicalData extends SimulationLinker {

    /**
     * Name of the variable to read, and from which parameters will be
     * extracted.
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

    /**
     * Array containing the fields to read. Dimensions should be (time, layer,
     * lat, lon) with layer the index of the vertical layer.
     */
    private double[][][][] values;

    public PhysicalData(int rank, String var_name) {
        super(rank);
        this.variable_name = var_name;
    }

    public void init() throws IOException {

        String key;

        // Recovering the key temperature.filename
        key = String.format("%s.value", this.variable_name);
        if (getConfiguration().canFind(key)) {
            // if a constant value is provided, then set the value
            // and force boolean to true
            this.useConstantVal = true;
            this.constantVal = getConfiguration().getDouble(key);
        } else {
            // if no constant value is provided, ask for a netcdf file.
            this.useConstantVal = false;
            
            // Recovering the conversion factors and offsets (temperature.factor, temperature.offset)
            key = String.format("%s.factor", this.variable_name);
            if (getConfiguration().canFind(key)) {
                factor = getConfiguration().getDouble(key);
            }

            key = String.format("%s.offset", this.variable_name);
            if (getConfiguration().canFind(key)) {
                offset = getConfiguration().getDouble(key);
            }

            // Recovering the name of the NetCDF variable (temperature.varname)
            key = String.format("%s.varname", this.variable_name);
            this.netcdf_variable_name = getConfiguration().getString(key);

            // Recovering the key temperature.filename
            key = String.format("%s.filename", this.variable_name);
            String filename = getConfiguration().getFile(key);

            if (!new File(filename).exists()) {
                error("Error reading PhysicalDataset parameters.", new FileNotFoundException("LTL NetCDF file " + filename + " does not exist."));
            }

            // count the number of time steps
            try (NetcdfFile nc = NetcdfFile.open(filename)) {
                // count the number of time steps
                int ntime = nc.findVariable(netcdf_variable_name).getDimension(0).getLength();

                // count the number of layers (i.e. depth levels) within the physical variable
                int nlayers = nc.findVariable(netcdf_variable_name).getDimension(1).getLength();
                values = new double[ntime][nlayers][getGrid().get_ny()][getGrid().get_nx()];;

                Array netcdf_value = nc.findVariable(netcdf_variable_name).read();

                Index index = netcdf_value.getIndex();

                for (int iTime = 0; iTime < ntime; iTime++) {
                    for (int k = 0; k < nlayers; k++) {
                        for (Cell cell : getGrid().getCells()) {
                            if (!cell.isLand()) {
                                int i = cell.get_igrid();
                                int j = cell.get_jgrid();
                                index.set(iTime, k, j, i);
                                values[iTime][k][j][i] = factor * (offset + netcdf_value.getDouble(index));
                            }  // end of if
                        }  // end of cell
                    }  // end of k
                }   // end of time
            } // end of try
        }  // end of canfind constant value.
    }  // end of init method

    /**
     * Recovers the value of a physicald dataset providing the depth index.
     * Value is converted as follows: output = factor * (offset + value)
     *
     * @param index Depth index (0 = surface, 1=mean, 2=bottom)
     * @param cell Cell index
     * @return
     */
    public double getValue(int index, Cell cell) {
        if (this.useConstantVal) {
            return this.constantVal;
        } else {
            int ltlTimeStep = getSimulation().getIndexTimeSimu() % this.values.length;
            return values[ltlTimeStep][index][cell.get_jgrid()][cell.get_igrid()];
        }
    }

    /**
     * Recovers the value of a physicald dataset providing the depth index.
     * Value is converted as follows: output = factor * (offset + value)
     *
     * @param school
     * @return
     */
    public double getValue(School school) {
        Cell cell = school.getCell();
        int index = school.getSpecies().getDepthLayer();
        return this.getValue(index, cell);
    }

}  // end of class
