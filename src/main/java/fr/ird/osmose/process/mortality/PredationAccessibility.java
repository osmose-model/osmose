/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
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
package fr.ird.osmose.process.mortality;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import fr.ird.osmose.stage.AccessibilityStage;
import fr.ird.osmose.stage.IStage;
import fr.ird.osmose.stage.PredPreyStage;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.Separator;
import fr.ird.osmose.util.SimulationLinker;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Nicolas Barrier
 */
public class PredationAccessibility extends SimulationLinker {

    /*
     * Accessibility stages
     */
    private IStage accessStage;

    /**
     * HashMaps of accessibility matrixes. -1 is when only one matrix is used.
     */
    private HashMap<Integer, AccessMatrix> matrixAccess;

    /**
     * Provides the accessibility matrix to use as a function of the time-step.
     */
    private int[][] indexAccess;

    public PredationAccessibility(int rank) {
        super(rank);
    }

    public void init() {

        int nseason = getConfiguration().getNStepYear();
        int nyear = (int) Math.ceil(this.getConfiguration().getNStep() / (float) nseason);

        // Mapping of the year and season value
        indexAccess = new int[nyear][nseason];
        for (int i = 0; i < nyear; i++) {
            for (int j = 0; j < nseason; j++) {
                indexAccess[i][j] = -1;
            }
        }

        // Accessibility stages
        accessStage = new AccessibilityStage();
        accessStage.init();

        matrixAccess = new HashMap<>();

        Configuration conf = this.getConfiguration();

        // If only one file is provided (old way)
        if (!getConfiguration().isNull("predation.accessibility.file")) {
            // accessibility matrix
            String filename = getConfiguration().getFile("predation.accessibility.file");
            AccessMatrix temp = new AccessMatrix(filename);
            matrixAccess.put(-1, temp);
        } else {
            // If several access files are defined.
            // recovers the indexes of the accessibility matrixes.
            int[] index = this.getConfiguration().findKeys("predation.accessibility.file.acc*").stream().mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".acc") + 4))).toArray();
            for (int i : index) {
                String filename = getConfiguration().getFile("predation.accessibility.file.acc" + i);
                AccessMatrix temp = new AccessMatrix(filename);
                matrixAccess.put(i, temp);

                String key;
                int ymax, ymin;
                int season[];

                key = "predation.accessibility.ymin.acc" + i;
                if (conf.canFind(key)) {
                    ymin = conf.getInt(key);
                } else {
                    ymin = 0;
                }

                key = "predation.accessibility.ymax.acc" + i;
                if (conf.canFind(key)) {
                    ymax = conf.getInt(key);
                } else {
                    ymax = nyear;
                }

                key = "predation.accessibility.season.acc" + i;
                if (conf.canFind(key)) {
                    season = conf.getArrayInt(key);
                } else {
                    season = new int[nseason];
                    for (int s = 0; s < nseason; s++) {
                        season[s] = s;
                    }
                }

                for (int y = ymin; y < ymax; y++) {
                    for (int s : season) {
                        indexAccess[y][s] = i;
                    }
                }
            }  // end of loop on access files

            for (int y = 0; y < nyear; y++) {
                for (int s = 0; s < nseason; s++) {
                    if (indexAccess[y][s] == - 1) {
                        error("Missing accessibility indexation for year " + y + " and season " + s, null);
                    }
                }
            }

            this.eliminateTwinAccess();

        }  // end of test for multiple access files

    }  // end of init

    private void eliminateTwinAccess() {

        // recover the sorted indexes of the access. objects;
        int[] index = (int[]) this.matrixAccess.keySet().stream().sorted().mapToInt(key -> key).toArray();
        int nmaps = index.length;

        int[] mapIndexNoTwin = new int[nmaps];

        for (int k = 0; k < nmaps; k++) {
            String file = this.matrixAccess.get(index[k]).getFile();
            mapIndexNoTwin[k] = k;
            for (int l = k - 1; l >= 0; l--) {
                if (file.equals(this.matrixAccess.get(index[l]).getFile())) {
                    mapIndexNoTwin[k] = mapIndexNoTwin[l];
                    // Delete twin maps
                    this.matrixAccess.remove(k);
                    break;
                }
            }
        }

        int nseason = getConfiguration().getNStepYear();
        int nyear = (int) Math.ceil(this.getConfiguration().getNStep() / (float) nseason);

        for (int y = 0; y < nyear; y++) {
            for (int s = 0; s < nseason; s++) {
                int indexMap = indexAccess[y][s];
                indexAccess[y][s] = mapIndexNoTwin[indexMap];
            }
        }
    }

    public IStage getStage() {
        return this.accessStage;
    }

    public double[][][][] getAccessMatrix() {

        int year = this.getSimulation().getYear();
        int season = this.getSimulation().getIndexTimeYear();
        int mapIndex = this.indexAccess[year][season];
        return this.matrixAccess.get(mapIndex).getAccessMatrix();

    }

    private class AccessMatrix {

        private double[][][][] accessibilityMatrix;
        private final String filename;

        private AccessMatrix(String filename) {
            this.filename = filename;
            this.read();
        }

        private double[][][][] getAccessMatrix() {
            return this.accessibilityMatrix;
        }

        private void read() {

            int nsp = getNSpecies();
            int nrsc = getConfiguration().getNRscSpecies();
            int nbkg = getConfiguration().getNBkgSpecies();

            try (CSVReader reader = new CSVReader(new FileReader(filename), Separator.guess(filename).getSeparator())) {
                List<String[]> lines = reader.readAll();
                int l = 1;
                accessibilityMatrix = new double[nsp + nbkg + nrsc][][][];   // lines are preys (order: focal, bkg, rsc)
                for (int i = 0; i < nsp + nrsc + nbkg; i++) {
                    int nStagePrey = accessStage.getNStage(i);
                    accessibilityMatrix[i] = new double[nStagePrey][][];
                    for (int j = 0; j < nStagePrey; j++) {
                        String[] line = lines.get(l);
                        int ll = 1;
                        accessibilityMatrix[i][j] = new double[nsp + nbkg][];      // columns are preds (order: focal, bkg)
                        for (int k = 0; k < nsp + nbkg; k++) {
                            int nStagePred = accessStage.getNStage(k);
                            accessibilityMatrix[i][j][k] = new double[nStagePred];
                            for (int m = 0; m < nStagePred; m++) {
                                double value = Double.valueOf(line[ll]);
                                accessibilityMatrix[i][j][k][m] = value;
                                ll++;
                            }
                        }
                        l++;
                    }
                }
            } catch (IOException ex) {
                error("Error loading accessibility matrix from file " + filename, ex);
            }
        }

        private String getFile() {
            return this.filename;
        }

    }
}
