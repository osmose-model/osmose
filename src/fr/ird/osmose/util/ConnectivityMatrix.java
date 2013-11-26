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
package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Osmose;
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