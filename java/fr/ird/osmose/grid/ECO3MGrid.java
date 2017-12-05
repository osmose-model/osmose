/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;
import java.io.IOException;
import ucar.nc2.NetcdfFile;

/**
 * This class creates an Osmose grid from an Eco3M NetCDF grid file. The Eco3M
 * (Ecological Modular Mechanistic Model) numerical platform for biogeochemical
 * modeling developed at LOPB (Baklouti et al., 2006a,b) is a modular tool that
 * handles multi-element multi-functional group biogeochemical models.<br>
 * The Osmose grid is aligned on the Eco3M grid. One Osmose cell is formed by
 * the regular aggregation of Eco3M cells (squares). The size of the aggregation
 * is given by the stride of the Osmose grid. One Osmose cell is made of
 * {@code stride * stride} Eco3M cells. An Osmose cell is marked as ocean if
 * more than 50% of the Eco3M cells in this cell are ocean.<br>
 * This class has been kept for backward compatibility with older configurations
 * of Osmose. Since Osmose version 3, {@link NcGrid} should preferably be used.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class ECO3MGrid extends AbstractGrid {

    /*
     * Pathname of the Eco3M NetCDF grid file
     */
    private String gridFile;
    /*
     * Names of the variables in the Eco3M grid file.
     */
    private String strLon, strLat, strMask;
    /*
     * Stride for loading the Eco3M grid.
     */
    private int stride;

////////////////////////////
// Definition of the methods
////////////////////////////
    @Override
    public void readParameters() {
        this.gridFile = getConfiguration().getFile("grid.netcdf.file");
        this.strLat = getConfiguration().getString("grid.var.lat");
        this.strLon = getConfiguration().getString("grid.var.lon");
        this.strMask = getConfiguration().getString("grid.var.mask");
        this.stride = getConfiguration().getInt("grid.stride");
    }

    /**
     * Open the Eco3M NetCDF grid file.
     *
     * @param gridFile, the path of the NetCDF grid file
     * @return a {@code NetcdfFile} object
     */
    private NetcdfFile openNetcdfFile(String gridFile) {

        try {
            return NetcdfFile.open(gridFile, null);
        } catch (IOException ex) {
            error("Failed to open NetCDF grid file " + gridFile, ex);
        }
        return null;
    }

    /**
     * Load a NetCDF variable from the Eco3M NetCDF grid file. The function
     * assumes that the variables is a two-dimensional double array.
     *
     * @param nc, the {@code NetcdfFile} that contains the variable
     * @param varname, the name of the variable in the NetCDF file
     * @return the values of the variable as a two-dimensional array of double
     */
    private double[][] readVariable(NetcdfFile nc, String varname) {
        try {
            return (double[][]) nc.findVariable(varname).read().copyToNDJavaArray();
        } catch (IOException ex) {
            error("Error while reading variable " + varname + " in NetCDF grid file " + nc.getLocation(), ex);
            return null;
        }
    }

    /**
     * Interpolates an Eco3M coordinate variable (either longitude or latitude)
     * on Osmose grid, with the given stride. It is assumed that the Eco3M
     * coordinate variable is a two dimensional array of double. Below is an
     * example of the problem to be solved for the interpolation. Here is an
     * Osmose cell, made of 5 * 5 Eco3M cells:<br>
     * | lon1 | lon2 | lon3 | lon4 | lon5 |<br>
     * | lon1 | lon2 | lon3 | lon4 | lon5 |<br>
     * | lon1 | lon2 | lon3 | lon4 | lon5 |<br>
     * | lon1 | lon2 | lon3 | lon4 | lon5 |<br>
     * | lon1 | lon2 | lon3 | lon4 | lon5 |<br>
     * What is the longitude of the Osmose cell ?
     * {@code lon = 0.5 * (lon1 + lon5)}. Same for latitude.
     *
     *
     * @param eco3mvar, the Eco3M two dimensional float array to interpolate on
     * Osmose grid
     * @param stride, the stride of the interpolation, such as
     * {@code osmose cell = stride * stride Eco3M cells}
     * @return an interpolated array of float on Osmose grid
     */
    private double[][] eco3m2osmose(double[][] eco3mvar, int stride) {

        int jmstr = eco3mvar.length / stride;
        int imstr = eco3mvar[0].length / stride;
        double[][] osmosevar = new double[jmstr][imstr];

        for (int i = 0; i < imstr; i++) {
            for (int j = 0; j < jmstr; j++) {
                // barrier.n: bug?  in the above example, we obtain 0.5(lon1 * lon1). But works for latitude
                // osmosevar[j][i] = 0.5f * (eco3mvar[(j + 1) * stride - 1][i * stride] + eco3mvar[j * stride][i * stride]);  
                // calculation using the index as follows:
                for (int ii = 0; ii < stride; ii++) {
                    for (int jj = 0; jj < stride; jj++) {
                        osmosevar[j][i] += eco3mvar[j * stride + jj][i * stride + ii] / (stride * stride)
            }
        }

        return osmosevar;
    }

    /**
     * Creates an Osmose grid based on the Eco3M grid, considering that one
     * Osmose cell is formed by the aggregation of {@code stride * stride} Eco3M
     * cells, with <i>stride</i> a user parameter. <br>
     * How does the function assign the value of the mask to an Osmose cell that
     * is made of both land and ocean Eco3M cells ? If (strictly) more than 50%
     * of the BFM cells constituting one Osmose cell are ocean, then the Osmose
     * cell is marked as ocean.<br>
     * | ocn | ocn | lnd | lnd | lnd |<br>
     * | ocn | ocn | lnd | lnd | lnd |<br>
     * | ocn | ocn | ocn | ocn | lnd |<br>
     * | ocn | ocn | ocn | ocn | ocn |<br>
     * | ocn | ocn | ocn | ocn | ocn |<br>
     * 25 Eco3M cells, 7 cells on land, 18 cells in ocean so the resulting
     * Osmose cell is marked as ocean.<br>
     * | lnd | lnd | lnd | lnd |<br>
     * | lnd | lnd | lnd | lnd |<br>
     * | ocn | ocn | ocn | ocn |<br>
     * | ocn | ocn | ocn | ocn |<br>
     * 16 Eco3M cells, 8 cells on land, 8 cells in ocean so the resulting Osmose
     * cell is marked as land.
     *
     * @return an array of {@code Cell} that represents the grid
     */
    @Override
    Cell[][] makeGrid() {

        NetcdfFile ncGrid = openNetcdfFile(gridFile);
        double[][] lon = eco3m2osmose(readVariable(ncGrid, strLon), stride);
        double[][] lat = eco3m2osmose(readVariable(ncGrid, strLat), stride);

        /*
         * Special calculation for the mask.
         * First average the mask values within an osmose cell.
         * If averaged mask > 0.5 then stridden cell = ocean
         * else stridden cell = land
         */
        double[][] mask = readVariable(ncGrid, strMask);
        int jmstr = lon.length;
        int imstr = lon[0].length;
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
                boolean land = (fmask <= 0.5);
                grid[j][i] = new Cell((j * imstr + i), i, j, (float) lat[j][i], (float) lon[j][i], land);
            }
        }

        return grid;
    }
}
