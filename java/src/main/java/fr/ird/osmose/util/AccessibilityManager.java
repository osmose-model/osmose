/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
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
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.stage.ClassGetter;
import fr.ird.osmose.util.Matrix;
import fr.ird.osmose.util.StepParameters;
import fr.ird.osmose.util.SimulationLinker;
import fr.ird.osmose.util.YearParameters;
import java.io.IOException;

import java.util.HashMap;

/**
 * Class that manages the time-variability of accessibility matrix.
 *
 * It is strongly inspired on the way species movements are parameterized.
 *
 * @author Nicolas Barrier
 */
public class AccessibilityManager extends SimulationLinker {

    /**
     * HashMaps of accessibility matrixes. -1 is when only one matrix is used.
     */
    private HashMap<Integer, Matrix> matrixAccess;
    
    private final String prefix;
    private final String suffix;
    
    private final ClassGetter classGetter;
    
    /**
     * Provides the accessibility matrix to use as a function of the time-step.
     */
    private int[][] indexAccess;

    public AccessibilityManager(int rank, String prefix, String suffix, ClassGetter classGetter) {
        super(rank);
        this.prefix = prefix;
        this.suffix = suffix;
        this.classGetter = classGetter;
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

        matrixAccess = new HashMap<>();

        // If only one file is provided (old way)
        
        if (!getConfiguration().isNull(this.prefix + ".file")) {
            // accessibility matrix
            String filename = getConfiguration().getFile(this.prefix + ".file");
            Matrix temp = new Matrix(filename, classGetter);   
            matrixAccess.put(-1, temp);
        } else {
            // If several access files are defined.
            // recovers the indexes of the accessibility matrixes.
            int[] index = this.getConfiguration().findKeys(this.prefix + ".file." + this.suffix + "*").stream().mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".acc") + 4))).toArray();
            for (int i : index) {

                String filename = getConfiguration().getFile(this.prefix + ".file." + this.suffix +  + i);
                Matrix temp = new Matrix(filename, classGetter);
                matrixAccess.put(i, temp);

                // Reconstruct the years to be used with this map
                YearParameters yearParam = new YearParameters(this.prefix, this.suffix + i);
                int[] years = yearParam.getYears();

                // Reconstruct the steps to be used with this map
                StepParameters seasonParam = new StepParameters(this.prefix, this.suffix + i);
                int[] season = seasonParam.getSeasons();

                for (int y : years) {
                    for (int s : season) {
                        indexAccess[y][s] = i;
                    }
                }
            }  // end of loop on access files

            for (int y = 0; y < nyear; y++) {
                for (int s = 0; s < nseason; s++) {
                    if (indexAccess[y][s] == - 1) {
                        error("Missing accessibility indexation for year " + y + " and season " + s, new IOException());
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

    /**
     * Returns the accesibility matrix for the given time-step.
     *
     * @return
     */
    public Matrix getMatrix() {

        int year = this.getSimulation().getYear();
        int season = this.getSimulation().getIndexTimeYear();
        int mapIndex = this.indexAccess[year][season];
        return this.matrixAccess.get(mapIndex);

    }
}
