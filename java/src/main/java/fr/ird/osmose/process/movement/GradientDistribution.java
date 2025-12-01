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
public class GradientDistribution extends AbstractSpatialDistribution {

    private final int iSpecies;
    private final int iSpeciesFile;
    private Random rd1;
    private MapSet maps;
    private float[] maxProbaPresence;
    private float randomWalkProba;
    private int rank;
    private int baseSearchRadius;
    private double randomWalkCoef;
    private static int radiusExpansions[] = new int[] { 0, 1, 2, 3, 5, 10 };

    /*
     * Ranges of movement in cell during one Osmose time step
     */
    private int range;

    public GradientDistribution(int iSpeciesFile, int iSpecies, int rank) {
        this.iSpeciesFile = iSpeciesFile;
        this.iSpecies = iSpecies;
        this.rank = rank;
    }

    @Override
    public void init() {

        boolean fixedSeed = false;
        if (!getConfiguration().isNull("simulation.fixedseed.enabled")) {
            fixedSeed = getConfiguration().getBoolean("simulation.fixedseed.enabled");
        }
        if (fixedSeed) {
            rd1 = new Random((13L ^ iSpecies) * (rank + 1));
            warning("Parameter 'simulation.fixedseed.enabled' is set to true. It means that two simulations with strictly identical initial school distribution will lead to same movement.");
        } else {
            rd1 = new Random();
        }

        // List of probability maps
        maps = new MapSet(iSpeciesFile, iSpecies, "gradient.movement");
        try {
            maps.init();
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(GradientDistribution.class.getName()).log(Level.SEVERE, null, ex);
        }

        baseSearchRadius = getConfiguration().getInt("movement.base.search.radius.sp" + iSpeciesFile);
        randomWalkCoef = getConfiguration().getInt("movement.random.walk.coef.sp" + iSpeciesFile);

    }

    @Override
    public void move(School school, int iStepSimu) {
        gradientDistribution(school, iStepSimu);
    }

    // private boolean isOut(School school, int iStepSimu) {
    // return (null == maps.getMap(school, iStepSimu));
    // }

    private void gradientDistribution(School school, int iStepSimu) {

        int age = school.getAgeDt();

        // Get current map and max probability of presence
        int indexMap = maps.getIndexMap(school.getAgeDt(), iStepSimu);
        GridMap map = maps.getMap(indexMap);

        /*
         * Check whether the map has changed from previous cohort and time-step. For
         * cohort zero and first time-step of the simulation we can assert sameMap =
         * false;
         */
        boolean sameMap = false;
        if (age > 0 && iStepSimu > 0) {
            int previousIndexMap = maps.getIndexMap(age - 1, iStepSimu - 1);
            if (indexMap == previousIndexMap) {
                sameMap = true;
            }
        }

        // Normalize probabilities
        List<Cell> listOfCells = this.getAccessibleCells(school, map);
        List<Float> listOfProba = new ArrayList<>();

        float probaTot = 0;
        for (Cell cell: listOfCells) {
            listOfProba.add(map.getValue(cell));
            probaTot += map.getValue(cell);
        }

        for (int k = 0; k < listOfProba.size(); k++) {
            listOfProba.set(k, listOfProba.get(k) / probaTot);
        }


        // Move the school
        if (!sameMap || school.isUnlocated()) {
            /*
             * Random distribution in a map, either because the map has changed from
             * previous cohort and time-step, or because the school was unlocated due to
             * migration.
             */
            int maxIter = 10000;
            int nIter = -1;
            int indexCell;
            int nCells = getGrid().get_nx() * getGrid().get_ny();
            double proba;
            do {
                nIter++;
                if (nIter >= maxIter) {
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
            } while (proba <= 0.d || proba < rd2.nextDouble() * maxProbaPresence[indexMap] || Double.isNaN(proba)
                    || getGrid().getCell(indexCell).isLand());
            school.moveToCell(getGrid().getCell(indexCell));
        } else {

            double testMove = rd1.nextDouble(); // valeur entre 0 et 1
            if (testMove >= randomWalkProba) {
                // Random move in adjacent cells contained in the map.
                List<Cell> accessibleCells = getAccessibleCells(school, map);
                Cell destinationCell = randomDeal(accessibleCells, rd3);
                school.moveToCell(destinationCell);
            }
        }
    }

    /**
     * Get the adjacent cells of a given school that are contained in the given map.
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

        // List of all accessible cells.
        // Starts searching from base radius and incrementing using the cell iterator
        List<Cell> accessibleCells = new ArrayList<>();
        for (int iterator = 0; iterator < radiusExpansions.length; iterator++) {
            // List of all accessible cells, i.e. the cells for which probability is >0
            accessibleCells.clear();
            int range = baseSearchRadius + radiusExpansions[iterator];
            Iterator<Cell> neighbours = getGrid().getNeighbourCells(cell, range).iterator();
            while (neighbours.hasNext()) {
                Cell neighbour = neighbours.next();
                // 2. Eliminate cell that is on land or for which probability is 0
                if (!neighbour.isLand() && (map.getValue(neighbour) > 0) && (!Double.isNaN(map.getValue(neighbour)))) {
                    accessibleCells.add(neighbour);
                }
            }

            // If the number of accessible cells > 0
            // then we leave the loop
            if (accessibleCells.size() > 0) {
                break;
            }

        } // end of loop on radius iterator

        if (accessibleCells.isEmpty()) {
            accessibleCells.add(cell);
        }

        return accessibleCells;
    }

    /**
     * Randomly choose a cell among the given list of cells.
     *
     * @param cells,
     *            a list of cells
     * @param rd,
     *            a random generator
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
