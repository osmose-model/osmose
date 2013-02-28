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
 * Grid built by reading the ECO3M NetCDF grid file.
 * User can specify a stride to create a grid with lower resolution that still
 * overlaps the ECO3M grid. Stride = 2 means one osmose cell = 4 ECO3M cells,
 * stride = 5 means one osmose cell = 25 ECO3M cells, etc.
 *
 * @author phv 2011/08/05
 */
public class ECO3MGrid extends AbstractGrid {

    /*
     * Pathname of the ECO3M NetCDF grid file
     */
    private String gridFile;
    /*
     * Names of the variables in the ECO3M grid file.
     */
    private String strLon, strLat, strMask;
    /*
     * Stride for loading the ECO3M grid.
     */
    private int stride;

////////////////////////////
// Definition of the methods
////////////////////////////
    @Override
    public void readParameters() {
        this.gridFile = getConfiguration().gridFileTab;
        this.strLat = getConfiguration().latField;
        this.strLon = getConfiguration().lonField;
        this.strMask = getConfiguration().maskField;
        this.stride = getConfiguration().stride;
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

    private double[][] makeStriddenVariable(double[][] var, int stride) {


        int jmstr = var.length / stride;
        int imstr = var[0].length / stride; // not clean since it assumes the array is square
        double[][] striddenVar = new double[jmstr][imstr];

        for (int i = 0; i < imstr; i++) {
            for (int j = 0; j < jmstr; j++) {
                striddenVar[j][i] = 0.5f * (var[(j + 1) * stride - 1][i * stride] + var[j * stride][i * stride]);
            }
        }

        return striddenVar;
    }

    @Override
    Cell[][] makeGrid() {

        NetcdfFile ncGrid = openNetcdfFile(gridFile);
        double[][] lon = makeStriddenVariable(readVariable(ncGrid, strLon), stride);
        double[][] lat = makeStriddenVariable(readVariable(ncGrid, strLat), stride);

        /*
         * Special calculation for the mask.
         * First average the mask values within an osmose cell.
         * If averaged mask > 0.5 then stridden cell = ocean
         * else stridden cell = land
         */
        double[][] mask = readVariable(ncGrid, strMask);
        int jmstr = lon.length;
        int imstr = lon[0].length;
        //Cell[][] grid = new Cell[imstr][jmstr];
        Cell[][] grid = new Cell[jmstr][imstr];
        for (int i = 0; i < imstr; i++) {
            for (int j = 0; j < jmstr; j++) {
                float fmask = 0.f;
                for (int ii = 0; ii < stride; ii++) {
                    for (int jj = 0; jj < stride; jj++) {
                        fmask += mask[j * stride + jj][i * stride + ii];
                    }
                }
                fmask = fmask / (stride * stride);
                boolean land = (fmask > 0.5) ? false : true;
                //grid[i][j] = new Cell(i, j, (float) lat[j][i], (float) lon[j][i], land);
                grid[j][i] = new Cell(j, i, (float) lat[j][i], (float) lon[j][i], land);
            }
        }

        return grid;
    }

    @Override
    public int getStride() {
        return stride;
    }
}
