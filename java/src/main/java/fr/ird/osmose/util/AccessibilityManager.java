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
