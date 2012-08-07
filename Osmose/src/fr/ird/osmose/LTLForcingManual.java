/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mtravers
 */
public class LTLForcingManual extends AbstractLTLForcing {

    private String[][] planktonFileList;
    private float[][][][] data;

    @Override
    public void readLTLConfigFile2(String planktonFileName) {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getOsmose().inputPathName, planktonFileName));
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
            planktonFileList = new String[getNbPlanktonGroups()][];
            for (int gr = 0; gr < getNbPlanktonGroups(); gr++) {
                planktonFileList[gr] = new String[getNbForcingDt()];
                for (int step = 0; step < getNbForcingDt(); step++) {
                    st.nextToken();
                    planktonFileList[gr][step] = st.sval;
                }
            }

        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            return;
        }
    }

    @Override
    public void initPlanktonMap() {
        /*
         * Associate osmose cells to BFM cells
         */
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                getGrid().getCell(i, j).icoordLTLGrid.addElement(i);
                getGrid().getCell(i, j).jcoordLTLGrid.addElement(j);
            }
        }
        /*
         * Initialize plankton list
         */
        initPlanktonList();
        loadData();
    }

    private void loadData() {

        System.out.println("Loading all plankton data, it might take a while...");

        data = new float[getSimulation().getNbTimeStepsPerYear()][getNbPlanktonGroups()][getPlanktonDimX()][getPlanktonDimY()];
        for (int t = 0; t < getSimulation().getNbTimeStepsPerYear(); t++) {
            for (int p = 0; p < getNbPlanktonGroups(); p++) {
                data[t][p] = getIntegratedData(getOsmose().resolveFile(planktonFileList[p][t]));
            }
        }

        System.out.println("All plankton data loaded !");
    }

    private void updateData(int dt) {

        for (int p = 0; p < getNbPlanktonGroups(); p++) {
            getPlankton(p).integratedData = data[dt][p];
        }
    }

    @Override
    public void updatePlankton(int dt) {
        for (int i = 0; i < getNbPlanktonGroups(); i++) {
            getPlanktonGroup(i).clearPlankton();      // put the biomass tables of plankton to 0
        }
        /*for (int p = 0; p < getNbPlanktonGroups(); p++) {
            String nameTemp = getOsmose().resolveFile(planktonFileList[p][dt]);
            readCSVFile(nameTemp, p);
        }*/
        updateData(dt);
        mapInterpolation();
    }

    @Override
    public void mapInterpolation() {
        int tempX, tempY;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    for (int k = 0; k < getGrid().getCell(i, j).getNbCellsLTLGrid(); k++) {
                        for (int p = 0; p < getNbPlanktonGroups(); p++) {
                            tempY = ((Integer) getGrid().getCell(i, j).icoordLTLGrid.elementAt(k)).intValue();
                            tempX = ((Integer) getGrid().getCell(i, j).jcoordLTLGrid.elementAt(k)).intValue();
                            /*if (p == 0) {
                            System.out.println("osmose cell (" + i + ", " + j + ") contains ECO3M cell (" + tempX + ", " + tempY + ")");
                            }*/
                            // interpolate the plankton concentrations from the LTL cells
                            getPlanktonGroup(p).addCell(i, j, tempX, tempY, getGrid().getCell(i, j).getNbCellsLTLGrid());
                        }
                    }
                }
            }
        }
    }

    private float[][] getIntegratedData(String nameOfFile) {
        //System.out.println("Reading " + nameOfFile);
        String name = nameOfFile;
        float[][] integratedData = new float[getPlanktonDimX()][getPlanktonDimY()];;
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(name), ';');
            List<String[]> lines = reader.readAll();

            if (!(lines.size() == getPlanktonDimY())) {
                System.out.println("No match between nb line of csv and dimY");
            }
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                if (!(line.length == getPlanktonDimX())) {
                    System.out.println("No match between nb column of csv and dimX");
                }

                for (int j = 0; j < line.length; j++) {
                    //          	getPlanktonGroup(planktonGroup).dataInit[j][i][0] = Float.valueOf(line[j]);
                    integratedData[j][i] = Float.valueOf(line[j]);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        return integratedData;
        //System.out.println("Read CSV file " + csvFile + " [OK]");
    }

    private void readCSVFile(String nameOfFile, int planktonGroup) {

        //System.out.println("Reading " + nameOfFile);
        String name = nameOfFile;
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(name), ';');
            List<String[]> lines = reader.readAll();

            if (!(lines.size() == getPlanktonDimY())) {
                System.out.println("No match between nb line of csv and dimY");
            }
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                if (!(line.length == getPlanktonDimX())) {
                    System.out.println("No match between nb column of csv and dimX");
                }

                for (int j = 0; j < line.length; j++) {
                    //          	getPlanktonGroup(planktonGroup).dataInit[j][i][0] = Float.valueOf(line[j]);
                    getPlanktonGroup(planktonGroup).integratedData[j][i] = Float.valueOf(line[j]);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("Read CSV file " + csvFile + " [OK]");
    }
}
