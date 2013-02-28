/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Plankton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLFastForcingRomsPisces extends AbstractLTLForcing {

    private String[] planktonFileListNetcdf;
    private float[][][] depthOfLayer;       // table of height of layers of ROMS model used in vertical integration
    private String[] plktonNetcdfNames;
    private String gridFileName;
    private String strCs_r, strHC;
    private String strLon, strLat, strH;
    float[][] latitude, longitude;
    private float[][][][] data;

    @Override
    public void readLTLForcingFile(String planktonFileName) {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getConfiguration().resolveFile(planktonFileName)));
        } catch (FileNotFoundException ex) {
            System.out.println("LTL file " + planktonFileName + " doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(LTLFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            plktonNetcdfNames = new String[getConfiguration().getNumberLTLGroups()];
            for (int i = 0; i < getConfiguration().getNumberLTLGroups(); i++) {
                st.nextToken();
                plktonNetcdfNames[i] = st.sval;
            }

            planktonFileListNetcdf = new String[getConfiguration().getNumberLTLSteps()];
            for (int step = 0; step < getConfiguration().getNumberLTLSteps(); step++) {
                st.nextToken();
                planktonFileListNetcdf[step] = st.sval;
            }

            st.nextToken();
            gridFileName = st.sval;

            st.nextToken();
            strLon = st.sval;
            st.nextToken();
            strLat = st.sval;
            st.nextToken();
            strH = st.sval;
            st.nextToken();
            strCs_r = st.sval;
            st.nextToken();
            strHC = st.sval;


        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            System.exit(1);
        }
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
            setDimX(shape[0]);
            setDimY(shape[1]);
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
                longitude = new float[nx][ny];
                latitude = new float[nx][ny];
                Index index = arrLon.getIndex();
                for (int j = 0; j < ny; j++) {
                    for (int i = 0; i < nx; i++) {
                        index.set(i, j);
                        longitude[i][j] = arrLon.getFloat(index);
                        latitude[i][j] = arrLat.getFloat(index);
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

        loadData();
    }

    private void loadData() {

        System.out.println("Loading all plankton data, it might take a while...");

        data = new float[getConfiguration().nStepYear][getConfiguration().getNumberLTLGroups()][get_nx()][get_ny()];
        for (int t = 0; t < getConfiguration().nStepYear; t++) {
            data[t] = getIntegratedData(getConfiguration().resolveFile(planktonFileListNetcdf[t]));
        }

        System.out.println("All plankton data loaded !");
    }

    private float[][][] getIntegratedData(String nameOfFile) {

        float[][][] integratedData = new float[getConfiguration().getNumberLTLGroups()][get_nx()][get_ny()];
        float[][][] dataInit;

        NetcdfFile nc = null;
        String name = nameOfFile;
        ArrayFloat.D3 tempArray;

        try {
            nc = NetcdfFile.open(name);

            for (int p = 0; p < getConfiguration().getNumberLTLGroups(); p++) {
                // read data and put them in the local arrays
                tempArray = (ArrayFloat.D3) nc.findVariable(plktonNetcdfNames[p]).read().reduce();
                dataInit = new float[get_nx()][get_ny()][get_nz()];

                // fill dataInit of plankton classes from local arrays
                for (int i = 0; i < get_nx(); i++) {
                    for (int j = 0; j < get_ny(); j++) {
                        for (int k = 0; k < get_nz(); k++) {
                            dataInit[i][j][k] = tempArray.get(k, i, j);    // carreful, index not in the same order
                        }
                    }
                }

                // integrates vertically plankton biomass, using depth files
                integratedData[p] = verticalIntegration(dataInit, depthOfLayer, getConfiguration().getIntegrationDepth());
            }
        } catch (IOException e) {
            Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, ioe);
                }
            }
        }

        return integratedData;
    }

    // CASE SPECIFIC - uses easy relation between the grids Plume and Osmose
    private void findValidMapIndex() {
        int posiTemp, posjTemp;
        icoordLTLGrid = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];
        jcoordLTLGrid = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];

        for (int i = 0; i < get_nx(); i++) {
            for (int j = 0; j < get_ny(); j++) // consider only the LTL cells included within the Osmose grid
            {
                if ((latitude[i][j] >= getGrid().getLatMin()) && (latitude[i][j] <= getGrid().getLatMax()) && (longitude[i][j] >= getGrid().getLongMin()) && (longitude[i][j] <= getGrid().getLongMax())) {
                    // equations giving the position of ROMS cells within the Osmose getGrid(), avoiding to read the whole matrix
                    /*
                     * WARNING, phv, 2011/04/07
                     * I changed the calculation of posiTemp & posjTemp
                     * but it should be double checked.
                     * 2011/08/04 phv, looks like the change in positemp led to
                     * different outputs... so I reverted the change.
                     */
                    posiTemp = (int) Math.floor(-(latitude[i][j] - getGrid().getLatMax()) / getGrid().getdLat());    //************** Attention sign minus & latMax depend on the sign of lat and long
                    posjTemp = (int) Math.floor((longitude[i][j] - getGrid().getLongMin()) / getGrid().getdLong());

                    // attach each LTL cells to the right Osmose cell (several LTL cells per Osmose cell is allowed)
                    if (!getGrid().getCell(posiTemp, posjTemp).isLand()) {
                        //System.out.println("osmose cell " + posiTemp + " " + posjTemp + " contains roms cell " + i + " " + j);
                        if (null == icoordLTLGrid[posiTemp][posjTemp]) {
                            icoordLTLGrid[posiTemp][posjTemp] = new ArrayList();
                            jcoordLTLGrid[posiTemp][posjTemp] = new ArrayList();
                        }
                        icoordLTLGrid[posiTemp][posjTemp].add(i);
                        jcoordLTLGrid[posiTemp][posjTemp].add(j);
                    }
                }
            }
        }

        /*
         * If no LTL cell is associated with an osmose cell (because of
         * curvilinear grid of ROMS) then uses the neighbour cells to get the
         * average plankton biomass.
         */
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    if (getNbCellsLTLGrid(i, j) == 0) {
                        icoordLTLGrid[i][j] = new ArrayList();
                        jcoordLTLGrid[i][j] = new ArrayList();
                        Cell cell;
                        if (i > 0) {
                            cell = getGrid().getCell(i - 1, j);
                            if (!cell.isLand()) {
                                icoordLTLGrid[i][j].addAll(get_iLTL(cell));
                                jcoordLTLGrid[i][j].addAll(get_jLTL(cell));
                            }
                        }
                        if (j > 0) {
                            cell = getGrid().getCell(i, j - 1);
                            if (!cell.isLand()) {
                                icoordLTLGrid[i][j].addAll(get_iLTL(cell));
                                jcoordLTLGrid[i][j].addAll(get_jLTL(cell));
                            }
                        }
                        if (i < getGrid().getNbLines() - 1) {
                            cell = getGrid().getCell(i + 1, j);
                            if (!cell.isLand()) {
                                icoordLTLGrid[i][j].addAll(get_iLTL(cell));
                                jcoordLTLGrid[i][j].addAll(get_jLTL(cell));
                            }
                        }
                        if (j < getGrid().getNbColumns() - 1) {
                            cell = getGrid().getCell(i, j + 1);
                            if (!cell.isLand()) {
                                icoordLTLGrid[i][j].addAll(get_iLTL(cell));
                                jcoordLTLGrid[i][j].addAll(get_jLTL(cell));
                            }
                        }
                    }
                }
            }
        }
    }
    
     @Override
    float[][] getRawBiomass(Plankton plankton, int iStepSimu) {
        return data[getIndexStepLTL(iStepSimu)][plankton.getIndex()];
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
            hRho = new double[nx][ny];
            Index index = arrH.getIndex();
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    hRho[i][j] = arrH.getDouble(index.set(i, j));
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
        float[][][] z_r = new float[nx][ny][nz];

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
                            z_r[i][j][k] = (float) (hc * (sc_r[k] - Cs_r[k]) + Cs_r[k] * hRho[i][j]);
                        }
                    }
                }
                break;
            // NEW: z_unperturbated = h * (sc * hc + Cs * h) / (h + hc)
            case NEW:
                for (int i = nx; i-- > 0;) {
                    for (int j = ny; j-- > 0;) {
                        for (int k = nz; k-- > 0;) {
                            z_r[i][j][k] = (float) (hRho[i][j] * (sc_r[k] * hc + Cs_r[k] * hRho[i][j]) / (hc + hRho[i][j]));
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
            if (strCoordType.toLowerCase().matches(VertCoordType.OLD.name().toLowerCase())) {
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

    private enum VertCoordType {

        NEW,
        OLD;
    }
}
