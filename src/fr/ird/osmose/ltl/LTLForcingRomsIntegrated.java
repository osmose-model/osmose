/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLForcingRomsIntegrated extends AbstractLTLForcing {

    private String[] planktonFileListNetcdf;
    private String[] plktonNetcdfNames;
    private float[][][][] data;

    @Override
    public void readLTLConfigFile2(String planktonFileName) {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getOsmose().resolveFile(planktonFileName)));
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
            plktonNetcdfNames = new String[getNbPlanktonGroups()];
            for (int i = 0; i < getNbPlanktonGroups(); i++) {
                st.nextToken();
                plktonNetcdfNames[i] = st.sval;
            }

            planktonFileListNetcdf = new String[getNbForcingDt()];
            for (int step = 0; step < getNbForcingDt(); step++) {
                st.nextToken();
                planktonFileListNetcdf[step] = st.sval;
            }


        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            return;
        }
    }

    @Override
    public void initPlanktonMap() {

        /*
         * set dimensions
         */
        setDimX(getGrid().getNbLines());
        setDimY(getGrid().getNbColumns());
        /*
         * Link LTL cells to Osmose cells, which is straighforward here
         */
        //findValidMapIndex();
        linkMapIndex();

        loadData();
    }

    @Override
    public void updatePlankton(int dt) {

        for (int i = 0; i < getNbPlanktonGroups(); i++) {
            getPlanktonGroup(i).clearPlankton();      // put the biomass tables of plankton to 0
        }
        updateData(dt);
        mapInterpolation();
    }

    private void updateData(int dt) {

        for (int p = 0; p < getNbPlanktonGroups(); p++) {
            getPlankton(p).integratedData = data[dt][p];
        }
    }

    private void loadData() {

        System.out.println("Loading all plankton data, it might take a while...");

        data = new float[getSimulation().getNumberTimeStepsPerYear()][getNbPlanktonGroups()][getPlanktonDimX()][getPlanktonDimY()];
        for (int t = 0; t < getSimulation().getNumberTimeStepsPerYear(); t++) {
            data[t] = getIntegratedData(getOsmose().resolveFile(planktonFileListNetcdf[t]));
        }

        System.out.println("All plankton data loaded !");
    }

    private float[][][] getIntegratedData(String nameOfFile) {

        float[][][] integratedData = new float[getNbPlanktonGroups()][getPlanktonDimX()][getPlanktonDimY()];

        NetcdfFile nc = null;
        String name = nameOfFile;
        ArrayDouble.D2 array;

        try {
            nc = NetcdfFile.open(name);

            for (int p = 0; p < getNbPlanktonGroups(); p++) {
                // read data and put them in the local arrays
                array = (ArrayDouble.D2) nc.findVariable(plktonNetcdfNames[p]).read().reduce();
                // fill dataInit of plankton classes from local arrays
                for (int i = 0; i < getPlanktonDimX(); i++) {
                    for (int j = 0; j < getPlanktonDimY(); j++) {
                        integratedData[p][i][j] = (float) array.get(i, j);    // carreful, index not in the same order
                    }
                }
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

    private void linkMapIndex() {
        for (int i = 0; i < getPlanktonDimX(); i++) {
            for (int j = 0; j < getPlanktonDimY(); j++) // consider only the LTL cells included within the Osmose grid
            {
                // attach each LTL cells to the right Osmose cell (several LTL cells per Osmose cell is allowed)
                if (!getGrid().getCell(i, j).isLand()) {
                    //System.out.println("osmose cell " + posiTemp + " " + posjTemp + " contains roms cell " + i + " " + j);
                    getGrid().getCell(i, j).icoordLTLGrid.addElement(new Integer(i));
                    getGrid().getCell(i, j).jcoordLTLGrid.addElement(new Integer(j));

                }
            }
        }
    }

    @Override
    public void mapInterpolation() {
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    for (int p = 0; p < getNbPlanktonGroups(); p++) {
                        getPlanktonGroup(p).addCell(i, j, i, j, 1);
                    }
                }
            }
        }
    }
}
