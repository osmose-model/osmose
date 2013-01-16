/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class NcGrid extends AbstractGrid {
    
    /*
     * Pathname of the ECO3M NetCDF grid file
     */
    private String gridFile;
    /*
     * Names of the variables in the ECO3M grid file.
     */
    private String strLon, strLat, strMask;

    @Override
    Cell[][] makeGrid() {
        
        NetcdfFile ncGrid = openNetcdfFile(gridFile);
        double[][] lon = readVariable(ncGrid, strLon);
        double[][] lat = readVariable(ncGrid, strLat);

        double[][] mask = readVariable(ncGrid, strMask);
        int im = lon.length;
        int jm = lon[0].length;
        Cell[][] grid = new Cell[im][jm];
        for (int i = 0; i < im; i++) {
            for (int j = 0; j < jm; j++) {
                boolean land = (mask[i][j] > 0) ? false : true;
                grid[i][j] = new Cell(i, j, (float) lat[i][j], (float) lon[i][j], land);
            }
        }

        return grid;
    }

    @Override
    void readParameters() {
        int numSerie = getOsmose().numSerie;
        this.gridFile = getOsmose().gridFileTab[numSerie];
        this.strLat = getOsmose().latFieldTab[numSerie];
        this.strLon = getOsmose().lonFieldTab[numSerie];
        this.strMask = getOsmose().maskFieldTab[numSerie];
    }

    @Override
    public int getStride() {
        return 1;
    }
    
    /*
     * Open the ECO3M Netcdf grid file.
     */
    private NetcdfFile openNetcdfFile(String gridFile) {

        try {
            return NetcdfFile.open(gridFile, null);
        } catch (IOException ex) {
            Logger.getLogger(ECO3MGrid.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /*
     * Load a Netcdf variable from the ECO3M grid file.
     * It is assumed that the variables are two-dimensional double arrays.
     * Stride is ignored so far, returns the whole variable.
     */
    private double[][] readVariable(NetcdfFile nc, String varname) {
        try {
            return (double[][]) nc.findVariable(varname).read().flip(0).copyToNDJavaArray();
        } catch (IOException ex) {
            Logger.getLogger(ECO3MGrid.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
}
