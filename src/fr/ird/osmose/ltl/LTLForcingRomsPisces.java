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
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLForcingRomsPisces extends AbstractLTLForcing {

    String[] planktonFileListNetcdf;
    float[][][] depthLevel;
    String[] plktonNetcdfNames;
    private String gridFileName;
    private String strCs_r, strHC;
    private String strLon, strLat, strMask, strH;
    float[][] latitude, longitude, mask;
    /**
     * Dimension of the LTL grid along the x-axis.
     */
    private int nx;
    /**
     * Dimension of the LTL grid along the y-axis.
     */
    private int ny;
    /**
     * Dimension of the LTL grid along the z-axis.
     */
    private int nz;
    /**
     * List of LTL cells that are contained within Osmose cells. The map is
     * indexed by Osmose cell index.
     */
    private HashMap<Integer, List<Point>> ltlCells;

    public LTLForcingRomsPisces(int rank) {
        super(rank);
    }

    @Override
    public void readParameters() {

        plktonNetcdfNames = new String[getConfiguration().getNPlankton()];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            plktonNetcdfNames[i] = getConfiguration().getString("ltl.netcdf.var.plankton.plk" + i);
        }

        planktonFileListNetcdf = new String[getConfiguration().findKeys("ltl.netcdf.file.t*").size()];
        for (int i = 0; i < planktonFileListNetcdf.length; i++) {
            planktonFileListNetcdf[i] = getConfiguration().getFile("ltl.netcdf.file.t" + i);
        }

        gridFileName = getConfiguration().getFile("ltl.netcdf.grid.file");
        strLon = getConfiguration().getString("ltl.netcdf.var.lon");
        strLat = getConfiguration().getString("ltl.netcdf.var.lat");
        strMask = getConfiguration().getString("ltl.netcdf.var.mask");
        strH = getConfiguration().getString("ltl.netcdf.var.bathy");
        strCs_r = getConfiguration().getString("ltl.netcdf.var.csr");
        strHC = getConfiguration().getString("ltl.netcdf.var.hc");
    }

    @Override
    public void initLTL() {
        try {
            NetcdfFile ncIn = NetcdfFile.open(gridFileName, null);
            /*
             * read dimensions
             */
            int[] shape = ncIn.findVariable(strLon).getShape();
            nx = (shape[1]);
            ny = (shape[0]);
            nz = (getCs_r(ncIn).length);
            /*
             * Read lon & lat
             */
            Array arrLon = ncIn.findVariable(strLon).read();
            Array arrLat = ncIn.findVariable(strLat).read();
            Array arrMask = ncIn.findVariable(strMask).read();
            if (arrLon.getElementType() == float.class) {
                longitude = (float[][]) arrLon.copyToNDJavaArray();
                latitude = (float[][]) arrLat.copyToNDJavaArray();
                mask = (float[][]) arrMask.copyToNDJavaArray();
            } else {
                longitude = new float[ny][nx];
                latitude = new float[ny][nx];
                mask = new float[ny][nx];
                Index index = arrLon.getIndex();
                for (int j = 0; j < ny; j++) {
                    for (int i = 0; i < nx; i++) {
                        index.set(j, i);
                        longitude[j][i] = arrLon.getFloat(index);
                        latitude[j][i] = arrLat.getFloat(index);
                        mask[j][i] = arrMask.getFloat(index);
                    }
                }
            }
            /*
             * Compute vertical levels
             */
            depthLevel = computeDepthLevel(ncIn);
            /*
             * Determine cell overlap for spatial integration
             */
            findValidMapIndex();
        } catch (IOException ex) {
            error("Error while reading the LTL grid from file " + gridFileName, ex);
        }
    }

    // CASE SPECIFIC - uses easy relation between the grids Plume and Osmose
    private void findValidMapIndex() {

        ltlCells = new HashMap();

        // consider only the LTL cells included within the Osmose grid
        for (int iLTL = 0; iLTL < nx; iLTL++) {
            for (int jLTL = 0; jLTL < ny; jLTL++) {
                if ((latitude[jLTL][iLTL] >= getGrid().getLatMin()) && (latitude[jLTL][iLTL] <= getGrid().getLatMax()) && (longitude[jLTL][iLTL] >= getGrid().getLongMin()) && (longitude[jLTL][iLTL] <= getGrid().getLongMax())) {
                    // equations giving the position of ROMS cells within the Osmose getGrid(), avoiding to read the whole matrix
                    int j = (int) Math.floor((latitude[jLTL][iLTL] - getGrid().getLatMin()) / getGrid().getdLat());
                    int i = (int) Math.floor((longitude[jLTL][iLTL] - getGrid().getLongMin()) / getGrid().getdLong());
                    j = Math.min(j, getGrid().get_ny() - 1);
                    i = Math.min(i, getGrid().get_nx() - 1);
                    // attach each LTL cells to the right Osmose cell (several LTL cells per Osmose cell is allowed)
                    Cell cell = getGrid().getCell(i, j);
                    if (!cell.isLand()) {
                        //System.out.println("osmose cell " + posiTemp + " " + posjTemp + " contains roms cell " + i + " " + j);
                        if (!ltlCells.containsKey(cell.getIndex())) {
                            ltlCells.put(cell.getIndex(), new ArrayList());
                        }
                        // Only include ROMS ocean cells
                        if (mask[jLTL][iLTL] > 0) {
                            ltlCells.get(cell.getIndex()).add(new Point(iLTL, jLTL));
                        }
                    }
                }
            }
        }

        /*
         * If no LTL cell is associated with an osmose cell (because of
         * curvilinear grid of ROMS) then uses the neighbour cells to get the
         * average plankton biomass.
         */
        for (int j = 0; j < getGrid().get_ny(); j++) {
            for (int i = 0; i < getGrid().get_nx(); i++) {
                Cell cell = getGrid().getCell(i, j);
                int index = cell.getIndex();
                if (!cell.isLand()) {
                    if (!ltlCells.containsKey(index)) {
                        ltlCells.put(index, new ArrayList());
                    }
                    if (ltlCells.get(index).isEmpty()) {
                        List<Cell> neighbours = new ArrayList();
                        if (j > 0) {
                            neighbours.add(getGrid().getCell(i, j - 1));
                        }
                        if (i > 0) {
                            neighbours.add(getGrid().getCell(i - 1, j));
                        }
                        if (j < getGrid().get_ny() - 1) {
                            neighbours.add(getGrid().getCell(i, j + 1));
                        }
                        if (i < getGrid().get_nx() - 1) {
                            neighbours.add(getGrid().getCell(i + 1, j));
                        }
                        for (Cell neighbour : neighbours) {
                            if (!neighbour.isLand()) {
                                ltlCells.get(index).addAll(ltlCells.get(neighbour.getIndex()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    float[][][] getRawBiomass(int iPlankton, int iStepSimu) {

        // Get NetCDF file name for LTL group iLTL and time step iStepSimu
        String name = planktonFileListNetcdf[getIndexStepLTL(iStepSimu)];
        float[][][] rawBiomass = null;
        NetcdfFile nc = null;
        try {
            // Open NetCDF file
            nc = NetcdfFile.open(name);
            // Read LTL biomass in NetCDF array
            Array array = nc.findVariable(plktonNetcdfNames[iPlankton]).read().reduce();
            // Get the shape of the LTL variable
            int[] shape = array.getShape();
            // Permute the dimensions in order to have rawBiomass[j][i][k]
            rawBiomass = new float[shape[1]][shape[2]][shape[0]];
            // Fill up the rawBiomass variable
            Index index = array.getIndex();
            for (int iLTL = 0; iLTL < shape[2]; iLTL++) {
                for (int jLTL = 0; jLTL < shape[1]; jLTL++) {
                    for (int kLTL = 0; kLTL < shape[0]; kLTL++) {
                        rawBiomass[jLTL][iLTL][kLTL] = (float) array.getFloat(index.set(kLTL, jLTL, iLTL));
                    }
                }
            }
        } catch (IOException ex) {
            error("Error loading plankton variable " + plktonNetcdfNames[iPlankton] + " from file " + name, ex);
        } finally {
            // Close the NetCDF file
            if (null != nc) {
                try {
                    nc.close();
                } catch (IOException ex) {
                    // do nothing
                }
            }
        }

        return rawBiomass;
    }

    @Override
    public int getIndexStepLTL(int iStepSimu) {
        return iStepSimu % getConfiguration().getNStepYear();
    }

    /**
     * Computes the depth at sigma levels disregarding the free surface
     * elevation.
     */
    private float[][][] computeDepthLevel(NetcdfFile ncIn) throws IOException {

        double hc;
        double[] sc_r = new double[nz];
        double[] Cs_r;
        double[][] hRho;

        //-----------------------------------------------------------
        // Read h in the NetCDF file.
        Array arrH = ncIn.findVariable(strH).read();
        if (arrH.getElementType() == double.class) {
            hRho = (double[][]) arrH.copyToNDJavaArray();
        } else {
            hRho = new double[ny][nx];
            Index index = arrH.getIndex();
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    hRho[j][i] = arrH.getDouble(index.set(j, i));
                }
            }
        }

        //-----------------------------------------------------------
        // Read hc, Cs_r and Cs_w in the NetCDF file.
        hc = getHc(ncIn);
        Cs_r = getCs_r(ncIn);

        //-----------------------------------------------------------
        // Calculation of sc_r, the sigma levels
        for (int k = nz; k-- > 0;) {
            sc_r[k] = ((double) (k - nz) + .5d) / (double) nz;
        }

        //------------------------------------------------------------
        // Calculation of z_r
        float[][][] z_r = new float[ny][nx][nz];

        /* 2010 June: UCLA Roms version (but not AGRIF yet)
         * uses new formulation for computing the unperturbated depth.
         * It is specified in a ":VertCoordType" global attribute that takes
         * mainly two values : OLD / NEW
         * OLD: usual calculation ==> z_unperturbated = hc * (sc - Cs) + Cs * h
         * NEW: z_unperturbated = h * (sc * hc + Cs * h) / (h + hc)
         * https://www.myroms.org/forum/viewtopic.php?p=1664#p1664
         */
        switch (getVertCoordType(ncIn)) {
            // OLD: z_unperturbated = hc * (sc - Cs) + Cs * h
            case OLD:
                for (int i = nx; i-- > 0;) {
                    for (int j = ny; j-- > 0;) {
                        for (int k = nz; k-- > 0;) {
                            z_r[j][i][k] = (float) (hc * (sc_r[k] - Cs_r[k]) + Cs_r[k] * hRho[j][i]);
                        }
                    }
                }
                break;
            // NEW: z_unperturbated = h * (sc * hc + Cs * h) / (h + hc)
            case NEW:
                for (int i = nx; i-- > 0;) {
                    for (int j = ny; j-- > 0;) {
                        for (int k = nz; k-- > 0;) {
                            z_r[j][i][k] = (float) (hRho[j][i] * (sc_r[k] * hc + Cs_r[k] * hRho[j][i]) / (hc + hRho[j][i]));
                        }
                    }
                }
                break;
        }

        return z_r;
    }

    private double getHc(NetcdfFile ncIn) throws IOException {

        if (null != ncIn.findGlobalAttribute(strHC)) {
            /* supposedly UCLA */
            return ncIn.findGlobalAttribute(strHC).getNumericValue().floatValue();
        } else if (null != ncIn.findVariable(strHC)) {
            /* supposedly Rutgers */
            return ncIn.findVariable(strHC).readScalarFloat();
        } else {
            /* hc not found */
            throw new IOException("S-coordinate critical depth (hc) could not be found, neither among variables nor global attributes");
        }
    }

    private double[] getCs_r(NetcdfFile ncIn) throws IOException {
        if (null != ncIn.findGlobalAttribute(strCs_r)) {
            /* supposedly UCLA */
            Attribute attrib_cs_r = ncIn.findGlobalAttribute(strCs_r);
            double[] Cs_r = new double[attrib_cs_r.getLength()];
            for (int k = 0; k < Cs_r.length - 1; k++) {
                Cs_r[k] = attrib_cs_r.getNumericValue(k).floatValue();
            }
            return Cs_r;
        } else if (null != ncIn.findVariable(strCs_r)) {
            /* supposedly Rutgers */
            Array arr_cs_r = ncIn.findVariable(strCs_r).read();
            double[] Cs_r = new double[arr_cs_r.getShape()[0]];
            for (int k = 0; k < Cs_r.length - 1; k++) {
                Cs_r[k] = arr_cs_r.getFloat(k);
            }
            return Cs_r;
        } else {
            /* Cs_w not found */
            throw new IOException("S-coordinate stretching curves at Rho-points (Cs_r) could not be found, neither among variables nor global attributes");
        }
    }

    private VertCoordType getVertCoordType(NetcdfFile ncIn) {

        /*
         * UCLA - Attribute "VertCoordType" NEW / OLD
         */
        if (null != ncIn.findGlobalAttribute("VertCoordType")) {
            String strCoordType = ncIn.findGlobalAttribute("VertCoordType").getStringValue();
            if (strCoordType.toLowerCase().equals(VertCoordType.OLD.name().toLowerCase())) {
                return VertCoordType.NEW;
            }
        }
        /*
         * Rutgers - Variable "VTransform" 1 = OLD / 2 = NEW
         */
        if (null != ncIn.findVariable("Vtransform")) {
            try {
                int vTransform = ncIn.findVariable("Vtransform").readScalarInt();
                switch (vTransform) {
                    case 1:
                        return VertCoordType.OLD;
                    case 2:
                        return VertCoordType.NEW;
                }
            } catch (IOException ex) {
            }
        }
        /*
         * Nothing worked and eventually returned OLD type.
         */
        return VertCoordType.OLD;
    }

    @Override
    float[] getDepthLevel(int iLTL, int jLTL) {
        return depthLevel[jLTL][iLTL];
    }

    @Override
    List<Point> getLTLCells(Cell cell) {
        return ltlCells.get(cell.getIndex());
    }

    private enum VertCoordType {

        NEW,
        OLD;
    }
}
