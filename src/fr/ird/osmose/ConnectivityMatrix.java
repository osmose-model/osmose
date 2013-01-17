/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class ConnectivityMatrix {

    private int indexMatrix;
    public HashMap<Integer, ConnectivityLine> clines;

    public ConnectivityMatrix(int indexMatrix, String csvFile) {
        this.indexMatrix = indexMatrix;
        readConnectivityFile(csvFile);
    }

    public int getIndexMatrix() {
        return indexMatrix;
    }

    private void readConnectivityFile(String csvFile) {

        clines = new HashMap();

        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            try {
                String[] line;
                int index = 0;
                while ((line = reader.readNext()) != null) {
                    Cell cell = getOsmose().getGrid().getCell(index);
                    if (!cell.isLand()) {
                        clines.put(cell.getIndex(), new ConnectivityLine(cell, line));
                    }
                    index++;
                }

            } catch (IOException ex) {
                Logger.getLogger(ConnectivityMatrix.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }

    public class ConnectivityLine {

        int indexCell;
        public int[] indexCells;
        public float[] connectivity;

        public ConnectivityLine(Cell cell, String[] line) {

            int nCells = 0;
            // Récupérer le nombre de cellules avec connectivité non nulle sur une ligne (pour une cellule donnée)
            for (String str : line) {
                float val = Float.valueOf(str);
                // if (val > 0) { // TD --
                nCells++;
                // } // TD --
            }
            indexCells = new int[nCells];
            connectivity = new float[nCells];

            // Charger en mémoire les valeurs de connectivité
            int i = 0;
            for (int iCell = 0; iCell < line.length; iCell++) {
                float val = Float.valueOf(line[iCell]);
                //if (val > 0) {  // TD --
                indexCells[i] = iCell;
                connectivity[i] = val;
                i++;
                //}  // TD --
            }
        }
//        private void cumSum() {
//            cumSum = new float[connectivity.length];
//            for (int i = 1; i < cumSum.length; i++) {
//                cumSum[i] += cumSum[i - 1];
//            }
//        }
    }
}