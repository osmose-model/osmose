/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    float[][][] depthOfLayer;       // table of height of layers of ROMS model used in vertical integration
    String[] plktonNetcdfNames;
    private String gridFileName;
    private String strCs_r, strHC;
    private String strLon, strLat, strH;
    float[][] latitude, longitude;

    @Override
    public void readLTLForcingFile() {

        plktonNetcdfNames = new String[getConfiguration().getNPlankton()];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            plktonNetcdfNames[i] = getConfiguration().getString("ltl.netcdf.var.plankton.plk" + i);
        }

        planktonFileListNetcdf = new String[getConfiguration().findKeys("ltl.netcdf.file.t*").size()];
        for (int i = 0; i < planktonFileListNetcdf.length; i++) {
            planktonFileListNetcdf[i] = getConfiguration().getString("ltl.netcdf.file.t" + i);
        }

        gridFileName = getConfiguration().getString("ltl.netcdf.grid.file");
        strLon = getConfiguration().getString("ltl.netcdf.var.lon");
        strLat = getConfiguration().getString("ltl.netcdf.var.lat");
        strH = getConfiguration().getString("ltl.netcdf.var.bathy");
        strCs_r = getConfiguration().getString("ltl.netcdf.var.csr");
        strHC = getConfiguration().getString("ltl.netcdf.var.hc");
    }

    @Override
    public void initLTLGrid() {

        NetcdfFile ncIn = null;
        String ncpathname = getConfiguration().resolveFile(gridFileName);
        try {
            ncIn = NetcdfFile.open(ncpathname, null);
        } catch (IOException ex) {
            Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * read dimensions
         */
        try {
            int[] shape = ncIn.findVariable(strLon).getShape();
            setDimX(shape[1]);
            setDimY(shape[0]);
            setDimZ(getCs_r(ncIn).length);
        } catch (IOException ex) {
            Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Read lon & lat
         */
        try {
            int nx = get_nx();
            int ny = get_ny();
            Array arrLon = ncIn.findVariable(strLon).read();
            Array arrLat = ncIn.findVariable(strLat).read();
            if (arrLon.getElementType() == float.class) {
                longitude = (float[][]) arrLon.copyToNDJavaArray();
                latitude = (float[][]) arrLat.copyToNDJavaArray();
            } else {
                longitude = new float[ny][nx];
                latitude = new float[ny][nx];
                Index index = arrLon.getIndex();
                for (int j = 0; j < ny; j++) {
                    for (int i = 0; i < nx; i++) {
                        index.set(j, i);
                        longitude[j][i] = arrLon.getFloat(index);
                        latitude[j][i] = arrLat.getFloat(index);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Compute vertical levels
         */
        try {
            depthOfLayer = getCstSigLevels(ncIn);
        } catch (IOException ex) {
            Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Determine cell overlap for spatial integration
         */
        findValidMapIndex();
    }

    // CASE SPECIFIC - uses easy relation between the grids Plume and Osmose
    private void findValidMapIndex() {
        int jGrid, iGrid;
        icoordLTLGrid = new ArrayList[getGrid().get_ny()][getGrid().get_nx()];
        jcoordLTLGrid = new ArrayList[getGrid().get_ny()][getGrid().get_nx()];

        // consider only the LTL cells included within the Osmose grid
        for (int i = 0; i < get_nx(); i++) {
            for (int j = 0; j < get_ny(); j++) {
                if ((latitude[j][i] >= getGrid().getLatMin()) && (latitude[j][i] <= getGrid().getLatMax()) && (longitude[j][i] >= getGrid().getLongMin()) && (longitude[j][i] <= getGrid().getLongMax())) {
                    // equations giving the position of ROMS cells within the Osmose getGrid(), avoiding to read the whole matrix
                    jGrid = (int) Math.floor((latitude[j][i] - getGrid().getLatMin()) / getGrid().getdLat());
                    iGrid = (int) Math.floor((longitude[j][i] - getGrid().getLongMin()) / getGrid().getdLong());

                    // attach each LTL cells to the right Osmose cell (several LTL cells per Osmose cell is allowed)
                    if (!getGrid().getCell(iGrid, jGrid).isLand()) {
                        //System.out.println("osmose cell " + posiTemp + " " + posjTemp + " contains roms cell " + i + " " + j);
                        if (null == icoordLTLGrid[jGrid][iGrid]) {
                            icoordLTLGrid[jGrid][iGrid] = new ArrayList();
                            jcoordLTLGrid[jGrid][iGrid] = new ArrayList();
                        }
                        icoordLTLGrid[jGrid][iGrid].add(i);
                        jcoordLTLGrid[jGrid][iGrid].add(j);
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
                if (!getGrid().getCell(i, j).isLand()) {
                    if (getNbCellsLTLGrid(i, j) == 0) {
                        icoordLTLGrid[j][i] = new ArrayList();
                        jcoordLTLGrid[j][i] = new ArrayList();
                        Cell cell;
                        if (j > 0) {
                            cell = getGrid().getCell(i, j - 1);
                            if (!cell.isLand()) {
                                icoordLTLGrid[j][i].addAll(get_iLTL(cell));
                                jcoordLTLGrid[j][i].addAll(get_jLTL(cell));
                            }
                        }
                        if (i > 0) {
                            cell = getGrid().getCell(i - 1, j);
                            if (!cell.isLand()) {
                                icoordLTLGrid[j][i].addAll(get_iLTL(cell));
                                jcoordLTLGrid[j][i].addAll(get_jLTL(cell));
                            }
                        }
                        if (j < getGrid().get_ny() - 1) {
                            cell = getGrid().getCell(i, j + 1);
                            if (!cell.isLand()) {
                                icoordLTLGrid[j][i].addAll(get_iLTL(cell));
                                jcoordLTLGrid[j][i].addAll(get_jLTL(cell));
                            }
                        }
                        if (i < getGrid().get_nx() - 1) {
                            cell = getGrid().getCell(i + 1, j);
                            if (!cell.isLand()) {
                                icoordLTLGrid[j][i].addAll(get_iLTL(cell));
                                jcoordLTLGrid[j][i].addAll(get_jLTL(cell));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    float[][] getRawBiomass(int iPlankton, int iStepSimu) {

        String name = getConfiguration().resolveFile(planktonFileListNetcdf[getIndexStepLTL(iStepSimu)]);
        float[][][] data3d = null;

        try {
            NetcdfFile nc = NetcdfFile.open(name);
            // read data and put it local array
            data3d = (float[][][]) nc.findVariable(plktonNetcdfNames[iPlankton]).read().reduce().copyToNDJavaArray();
        } catch (IOException ex) {
            Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, ex);
        }

        return verticalIntegration(data3d, depthOfLayer, getConfiguration().getFloat("ltl.integration.depth"));
    }

    @Override
    public int getIndexStepLTL(int iStepSimu) {
        return iStepSimu % getConfiguration().getNumberTimeStepsPerYear();
    }

    /**
     * Computes the depth at sigma levels disregarding the free surface
     * elevation.
     */
    private float[][][] getCstSigLevels(NetcdfFile ncIn) throws IOException {

        int nz = get_nz();
        int nx = get_nx();
        int ny = get_ny();
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
        // Calculation of z_w , z_r
        float[][][] z_r = new float[nz][ny][nx];

        /* 2010 June: Recent UCLA Roms version (but not AGRIF yet)
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
                            z_r[k][j][i] = (float) (hc * (sc_r[k] - Cs_r[k]) + Cs_r[k] * hRho[j][i]);
                        }
                    }
                }
                break;
            // NEW: z_unperturbated = h * (sc * hc + Cs * h) / (h + hc)
            case NEW:
                for (int i = nx; i-- > 0;) {
                    for (int j = ny; j-- > 0;) {
                        for (int k = nz; k-- > 0;) {
                            z_r[k][j][i] = (float) (hRho[j][i] * (sc_r[k] * hc + Cs_r[k] * hRho[j][i]) / (hc + hRho[j][i]));
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
    public String[] getPlanktonFieldName() {
        return plktonNetcdfNames;
    }

    @Override
    public String[] getNetcdfFile() {
        return planktonFileListNetcdf;
    }

    private enum VertCoordType {

        NEW,
        OLD;
    }
}
