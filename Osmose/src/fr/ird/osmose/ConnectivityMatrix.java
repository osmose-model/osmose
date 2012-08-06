/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class ConnectivityMatrix {

    private int indexMatrix;
    ConnectivityLine[] clines;

    public ConnectivityMatrix(int indexMatrix, String csvFile) {
        this.indexMatrix = indexMatrix;
        readConnectivityFile(csvFile);
    }
    
    public int getIndexMatrix() {
        return indexMatrix;
    }    

    private void readConnectivityFile(String csvFile) {
        
        int ni = getOsmose().getGrid().getNbColumns();
        int nj = getOsmose().getGrid().getNbLines();
        clines = new ConnectivityLine[ni * nj];

        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            try {
                List<String[]> lines = reader.readAll();
                for (int index = 0; index < lines.size(); index++) {
                    String[] line = lines.get(index);
                    int j = index / nj;
                    int i = index - j * nj;
                    Cell cell = getOsmose().getGrid().getCell(i, j);
                    if (cell.isLand()) {
                        clines[index] = null;
                    } else {
                        clines[index] = new ConnectivityLine(cell, line);
                    }
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

        Cell cell;
        int[] indexCells;
        float[] connectivity;
        float[] cumSum;

        public ConnectivityLine(Cell cell, String[] line) {

            this.cell = cell;
            int nCells = 0;
            // Récupérer le nombre de cellules avec connectivité non nulle sur une ligne (pour une cellule donnée)
            for (String str : line) {
                float val = Float.valueOf(str);
                if (val > 0) {
                    nCells++;
                }
            }
            indexCells = new int[nCells];
            connectivity = new float[nCells];

            // Charger en mémoire les valeurs de connectivité
            int i = 0;
            for (int iCell = 0; iCell < line.length; iCell++) {
                float val = Float.valueOf(line[iCell]);
                if (val > 0) {
                    indexCells[i] = iCell;
                    connectivity[i] = val;
                }
            }
            cumSum();
        }

        private void cumSum() {
            cumSum = new float[connectivity.length];
            for (int i = 1; i < cumSum.length; i++) {
                cumSum[i] += cumSum[i - 1];
            }
        }
    }
}
