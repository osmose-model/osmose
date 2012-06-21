/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class BFMGrid extends AbstractGrid {

    /*
     * Pathname of the BFM NetCDF grid file
     */
    private String gridFile;
    /*
     * Names of the variables in the BFM grid file.
     */
    private String strLon, strLat, strMask;
    /*
     * Stride for loading the BFM grid.
     */
    private int stride;

////////////////////////////
// Definition of the methods
////////////////////////////
    @Override
    public void readParameters() {
        int numSerie = getOsmose().numSerie;
        this.gridFile = getOsmose().gridFileTab[numSerie];
        this.strLat = getOsmose().latFieldTab[numSerie];
        this.strLon = getOsmose().lonFieldTab[numSerie];
        this.strMask = getOsmose().maskFieldTab[numSerie];
        this.stride = getOsmose().strideTab[numSerie];
    }

    /*
     * Open the BFM Netcdf grid file.
     */
    private NetcdfFile openNetcdfFile(String gridFile) {

        try {
            return NetcdfFile.open(gridFile, null);
        } catch (IOException ex) {
            Logger.getLogger(BFMGrid.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /*
     * Load a Netcdf variable from the BFM grid file.
     * It is assumed that the variables are two-dimensional double arrays.
     * Stride is ignored so far, returns the whole variable.
     */
    private float[][] readVariable(NetcdfFile nc, String varname) {
        try {
            return (float[][]) nc.findVariable(varname).read().copyToNDJavaArray();
        } catch (IOException ex) {
            Logger.getLogger(BFMGrid.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private float[][] makeStriddenVariable(float[][] var, int stride) {


        int jmstr = var.length / stride;
        int imstr = var[0].length / stride; // not clean since it assumes the array is square
        float[][] striddenVar = new float[jmstr][imstr];

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
        float[][] lon = makeStriddenVariable(readVariable(ncGrid, strLon), stride);
        float[][] lat = makeStriddenVariable(readVariable(ncGrid, strLat), stride);

        /*
         * Special calculation for the mask.
         * First average the mask values within an osmose cell.
         * If averaged mask > 0.5 then stridden cell = ocean
         * else stridden cell = land
         */
        float[][][] mask = null;
        try {
            mask = (float[][][]) ncGrid.findVariable(strMask).read().copyToNDJavaArray();
        } catch (IOException ex) {
            Logger.getLogger(BFMGrid.class.getName()).log(Level.SEVERE, null, ex);
        }
        int jmstr = lon.length;
        int imstr = lon[0].length;
        Cell[][] grid = new Cell[jmstr][imstr];
        for (int i = 0; i < imstr; i++) {
            for (int j = 0; j < jmstr; j++) {
                float fmask = 0.f;
                for (int ii = 0; ii < stride; ii++) {
                    for (int jj = 0; jj < stride; jj++) {
                        fmask += mask[0][j * stride + jj][i * stride + ii];
                    }
                }
                fmask = fmask / (stride * stride);
                boolean land = (fmask > 0.5) ? false : true;
                grid[jmstr - j - 1][i] = new Cell(jmstr - j - 1, i, (float) lat[j][i], (float) lon[j][i], land);
            }
        }

        return grid;
    }

    @Override
    public int getStride() {
        return stride;
    }

}
