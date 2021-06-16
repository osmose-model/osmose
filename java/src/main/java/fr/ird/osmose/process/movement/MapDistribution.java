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

package fr.ird.osmose.process.movement;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.School;
import fr.ird.osmose.util.MapSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author pverley
 */
public class MapDistribution extends AbstractDistribution {

    private final int iSpecies;
    private final int iSpeciesFile;
    private Random rd1, rd2, rd3;
    private MapSet maps;
    private float[] maxProbaPresence;
    /*
     * Ranges of movement in cell during one Osmose time step
     */
    private int range;

    public MapDistribution(int iSpeciesFile, int iSpecies) {
        this.iSpeciesFile = iSpeciesFile;
        this.iSpecies = iSpecies;
    }

    @Override
    public void init() {

        boolean fixedSeed = false;
        if (!getConfiguration().isNull("movement.randomseed.fixed")) {
            fixedSeed = getConfiguration().getBoolean("movement.randomseed.fixed");
        }
        if (fixedSeed) {
            rd1 = new Random(13L ^ iSpecies);
            rd2 = new Random(5L ^ iSpecies);
            rd3 = new Random(1982L ^ iSpecies);
            warning("Parameter 'movement.randomseed.fixed' is set to true. It means that two simulations with strictly identical initial school distribution will lead to same movement.");
        } else {
            rd1 = new Random();
            rd2 = new Random();
            rd3 = new Random();
        }

        maps = new MapSet(iSpeciesFile, iSpecies, "movement");
        try {
            maps.init();
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MapDistribution.class.getName()).log(Level.SEVERE, null, ex);
        }
        maxProbaPresence = new float[maps.getNMap()];
        for (int imap = 0; imap < maxProbaPresence.length; imap++) {
            maxProbaPresence[imap] = computeMaxProbaPresence(imap);
            // Just a trick for absence/presence maps (and not probability)
            if (maxProbaPresence[imap] >= 1.f) {
                maxProbaPresence[imap] = 0.f;
            }
        }

        if (!getConfiguration().isNull("movement.randomwalk.range.sp" + iSpeciesFile)) {
            range = getConfiguration().getInt("movement.randomwalk.range.sp" + iSpeciesFile);
        } else {
            range = 1;
        }

    }

    @Override
    public void move(School school, int iStepSimu) {
        if (!isOut(school, iStepSimu)) {
            mapsDistribution(school, iStepSimu);
        } else {
            school.out();
        }
    }

    private boolean isOut(School school, int iStepSimu) {
        return (null == maps.getMap(school, iStepSimu));
    }

    private void mapsDistribution(School school, int iStepSimu) {

        int age = school.getAgeDt();

        // Get current map and max probability of presence
        int indexMap = maps.getIndexMap(school.getAgeDt(), iStepSimu);
        GridMap map = maps.getMap(indexMap);

        /*
         * Check whether the map has changed from previous cohort
         * and time-step.
         * For cohort zero and first time-step of the simulation we can
         * assert sameMap = false;
         */
        boolean sameMap = false;
        if (age > 0 && iStepSimu > 0) {
            int previousIndexMap = maps.getIndexMap(age - 1, iStepSimu - 1);
            if (indexMap == previousIndexMap) {
                sameMap = true;
            }
        }

        // Move the school
        if (!sameMap || school.isUnlocated()) {
            /*
             * Random distribution in a map, either because the map has
             * changed from previous cohort and time-step, or because the
             * school was unlocated due to migration.
             */
            int maxIter = 10000;
            int nIter = -1;
            int indexCell;
            int nCells = getGrid().get_nx() * getGrid().get_ny();
            double proba;
            do {
                nIter++;
                if(nIter >= maxIter) {
                    StringBuilder bld = new StringBuilder();
                    bld.append("The maximum number of iterations for map distribution has been reached\n");
                    String outFmt = String.format("Check movements for species %s", school.getSpecies().getName());
                    bld.append(outFmt);
                    outFmt = String.format(" and for age %f\n", (float) age / getConfiguration().getNStepYear());
                    bld.append(outFmt);
                    error(bld.toString(), new Exception());
                }
                indexCell = (int) Math.round((nCells - 1) * rd1.nextDouble());
                proba = map.getValue(getGrid().getCell(indexCell));
            } while (proba <= 0.d || proba < rd2.nextDouble() * maxProbaPresence[indexMap] || Double.isNaN(proba));
            school.moveToCell(getGrid().getCell(indexCell));
        } else {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(randomDeal(getAccessibleCells(school, map), rd3));
        }
    }

    /**
     * Get the adjacent cells of a given school that are contained in the given
     * map.
     *
     * @param school
     * @param map
     * @return
     */
    private List<Cell> getAccessibleCells(School school, GridMap map) {

        Cell cell = school.getCell();
        if (map.getValue(cell) <= 0.d) {
            StringBuilder str = new StringBuilder("Inconsistency in moving ");
            str.append(school.toString());
            str.append("\n");
            str.append("It is not in the geographical area it is supposed to be...");
            warning(str.toString());
        }
        List<Cell> accessibleCells = new ArrayList<>();
        // 1. Get all surrounding cells
        Iterator<Cell> neighbours = getGrid().getNeighbourCells(cell, range).iterator();
        while (neighbours.hasNext()) {
            Cell neighbour = neighbours.next();
            // 2. Eliminate cell that is on land
            // 3. Add the cell if it is within the current map of distribution
            if (!neighbour.isLand() && (map.getValue(neighbour) > 0) && (!Double.isNaN(map.getValue(neighbour)))) {
                accessibleCells.add(neighbour);
            }
        }
        return accessibleCells;
    }

    /**
     * Randomly choose a cell among the given list of cells.
     *
     * @param cells, a list of cells
     * @param rd, a random generator
     * @return a cell from the list of cells.
     */
    private Cell randomDeal(List<Cell> cells, Random rd) {
        int index = (int) Math.round((cells.size() - 1) * rd.nextDouble());
        return cells.get(index);
    }

    private float computeMaxProbaPresence(int numMap) {
        float tempMaxProbaPresence = 0;
        GridMap map = maps.getMap(numMap);
        if (null != map) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    tempMaxProbaPresence = Math.max(tempMaxProbaPresence, map.getValue(i, j));
                }
            }
        }
        return tempMaxProbaPresence;
    }
}
