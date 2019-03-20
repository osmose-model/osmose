/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
     * Array containing the fields to read. Dimensions should be (time, 3, lat,
     * lon) with k=0 => surface, k=1: column average, k=2: last ocean cell.
     */
    private double[][][][] values;

    public PhysicalData(int rank, String var_name) {
        super(rank);
        this.variable_name = var_name;
    }

    public void init() throws IOException {

        String key;

        // Recovering the name of the NetCDF variable (temperature.varname)
        key = String.format("%s.varname", this.variable_name);
        this.netcdf_variable_name = getConfiguration().getString(key);

        // Recovering the conversion factors and offsets (temperature.factor, temperature.offset)
        key = String.format("%s.factor", this.variable_name);
        if (getConfiguration().canFind(key)) {
            factor = getConfiguration().getDouble(key);
        }

        key = String.format("%s.offset", this.variable_name);
        if (getConfiguration().canFind(key)) {
            offset = getConfiguration().getDouble(key);
        }

        // Recovering the key temperature.filename
        key = String.format("%s.filename", this.variable_name);
        String filename = getConfiguration().getFile(key);

        if (!new File(filename).exists()) {
            error("Error reading LTLForcing parameters.", new FileNotFoundException("LTL NetCDF file " + filename + " does not exist."));
        }

        try {
            
        
            NetcdfFile nc = NetcdfFile.open(filename);
            // count the number of time steps
            int ntime = nc.findVariable(netcdf_variable_name).getDimension(0).getLength();
            values = new double[ntime][3][getGrid().get_ny()][getGrid().get_nx()];;

            Array netcdf_value = nc.findVariable(netcdf_variable_name).read();
            
            Index index = netcdf_value.getIndex();

            for (int iTime = 0; iTime < ntime; iTime++) {
                for (int k = 0; k < 3; k++) {
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

            nc.close();

        } catch (IOException ex) {
            error("File " + filename + " and variable " + netcdf_variable_name + "cannot be read", new IOException());

        }  // end of try catch
    }  // end of init method
    
    /** Recovers the value of a physicald dataset providing the depth index.
     * Value is converted as follows: output = factor * (offset + value)
     * 
     * @param index Depth index (0 = surface, 1=mean, 2=bottom)
     * @param cell Cell index
     * @return 
     */
    public double getValue(int index, Cell cell) {
        int ltlTimeStep = getSimulation().getIndexTimeSimu() % this.values.length;
        return values[ltlTimeStep][index][cell.get_jgrid()][cell.get_igrid()];
    }
    
        /** Recovers the value of a physicald dataset providing the depth index.
     * Value is converted as follows: output = factor * (offset + value)
     * 
     * @param index Depth index (0 = surface, 1=mean, 2=bottom)
     * @param cell Cell index
     * @return 
     */
    public double getValue(School school) {
        Cell cell = school.getCell();
        int index = school.getSpecies().getDepthLayer();
        return this.getValue(index, cell);
    }
    
    
}  // end of class
