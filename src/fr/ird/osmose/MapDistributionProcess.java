
package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class MapDistributionProcess extends AbstractProcess {
    
    private Species species;
    private GridMap[] maps;
    float[] maxProbaPresence;
    int[][] numMaps;
    int[][] ageClasses;
    int[][] periods;
    
    public MapDistributionProcess(Species species) {
        this.species = species;
    }

    @Override
    public void loadParameters() {
        numMaps = new int[species.getLongevity()][getSimulation().getNbTimeStepsPerYear()];
    }

    @Override
    public void run() {
        
    }
    
    private void mapsDistribution(School school) {

        int i_step_year = getSimulation().getIndexTimeYear();
        int age = school.getAgeDt();
        /*
         * Do not distribute cohorts that are presently out of
         * the simulated area.
         */
        if (species.isOut(age, i_step_year)) {
            school.setOffGrid();
            return;
        }

        // Get current map and max probability of presence
        int numMap = numMaps[age][i_step_year];
        GridMap map = getOsmose().getMap(numMap);
        float tempMaxProbaPresence = maxProbaPresence[numMap];

        /*
         * Check whether the map has changed from previous cohort
         * and time-step.
         * For cohort zero and first time-step of the simulation we can
         * assert sameMap = false;
         */
        boolean sameMap = false;
        if (age > 0 && getSimulation().getIndexTimeSimu() > 0) {
            int oldTime;
            if (i_step_year == 0) {
                oldTime = getSimulation().getNbTimeStepsPerYear() - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            if (numMap == numMaps[age - 1][oldTime]) {
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
            int indexCell;
            int nCells = getGrid().getNbColumns() * getGrid().getNbLines();
            double proba;
            do {
                indexCell = (int) Math.round((nCells - 1) * Math.random());
                proba = maps[numMaps[age][i_step_year]].getValue(getGrid().getCell(indexCell));
            } while (proba <= 0 || proba < Math.random() * tempMaxProbaPresence);
            school.moveToCell(getGrid().getCell(indexCell));
        } else {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(randomDeal(getAccessibleCells(school, map)));
        }
    }
    
    private void readCSVMap(String csvFile, int indexMap) {

        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            /*
             * Get the number of cells for the map phv 28 march 2012 - it is a
             * loss of time to do twice the loop over the CSV file but no choice
             * at the moment without changing mapCoordi, mapCoordj
             */
            int nbCells = 0;
            for (String[] line : lines) {
                for (String str : line) {
                    //if (Integer.valueOf(str) > 0) {
                    if (Float.valueOf(str) > 0.f) {
                        nbCells++;
                    }
                }
            }
            /*
             * Initialize the arrays
             */
            maps[indexMap] = new GridMap();
            /*
             * Set the numero of maps per species, age class and time step
             */
            int nDtYear = getOsmose().nbDtMatrix[getOsmose().numSerie];
            for (int m = 0; m < ageClasses[indexMap].length; m++) {
                for (int n = 0; n < periods[indexMap].length; n++) {
                    for (int h = 0; h < nDtYear; h++) {
                        if ((ageClasses[indexMap][m] * nDtYear + h) < species.getLongevity()) {
                            numMaps[ageClasses[indexMap][m] * nDtYear + h][periods[indexMap][n]] = indexMap;
                            //System.out.println("NumMap: " + areasNumSpForMap[indexMap] + " " + (areasTempAge[indexMap][m] * nbDtMatrix[numSerie] + h) + " " + (areasTempDt[indexMap][n]) + " " + indexMap);
                        }
                        if (nbCells == 0) {
                            if (!species.isOut((ageClasses[indexMap][m] * nDtYear) + h, periods[indexMap][n])) {
                                System.out.println("Match error between species areas and migration file for " + species.getName());
                            }
                        }
                    }
                }
            }
            /*
             * Identify the coordinates of the cells and set the probability
             */
            int indexCell = 0;
            float invNbCells = 1.f / nbCells;
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val > 0.f) {
                        if (val < 1.f) {
                            /*
                             * value provided is directly a probability
                             */
                            maps[indexMap].setValue(i, j, val);
                        } else if (val == 1.f) {
                            /*
                             * map is presence/absence so equal probability of
                             * presence among cells
                             */
                            maps[indexMap].setValue(i, j, invNbCells);
                            /*
                             * else mapProbaPresence[indexMap][indexCell] = 0
                             * default value at initialization of the array
                             */
                        }
                        indexCell++;
                    }
                }
            }
            maxProbaPresence[indexMap] = getMaxProbaPresence(indexMap);
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("Read CSV file " + csvFile + " [OK]");
    }
    
    private float getMaxProbaPresence(int indexMap) {
        float tempMaxProbaPresence = 0;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                tempMaxProbaPresence = Math.max(tempMaxProbaPresence, maps[indexMap].getValue(i, j));
            }
        }
        return tempMaxProbaPresence;
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
        if (map.getValue(cell) <= 0) {
            StringBuilder str = new StringBuilder("Inconsistency in moving ");
            str.append(school.toString());
            str.append("\n");
            str.append("It is not in the geographical area it is supposed to be...");
            System.out.println(str.toString());
        }
        List<Cell> accessibleCells = new ArrayList();
        // 1. Get all surrounding cells
        Iterator<Cell> neighbours = getGrid().getNeighbourCells(cell).iterator();
        while (neighbours.hasNext()) {
            Cell neighbour = neighbours.next();
            // 2. Eliminate cell that is on land
            // 3. Add the cell if it is within the current map of distribution 
            if (!neighbour.isLand() && map.getValue(neighbour) > 0) {
                accessibleCells.add(neighbour);
            }
        }
        return accessibleCells;
    }
    
    /**
     * Randomly choose a cell among the given list of cells.
     *
     * @param cells, a list of cells
     * @return a cell from the list of cells.
     */
    private Cell randomDeal(List<Cell> cells) {
        int index = (int) Math.round((cells.size() - 1) * Math.random());
        return cells.get(index);
    }
    
}
