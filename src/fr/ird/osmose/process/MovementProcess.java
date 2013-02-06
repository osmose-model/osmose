
package fr.ird.osmose.process;

import fr.ird.osmose.Cell;
import fr.ird.osmose.GridMap;
import fr.ird.osmose.School;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author pverley
 */
public class MovementProcess extends AbstractProcess {
    
    private final static float ONE_DEG_LATITUDE_IN_METER = 111138.f;
    private AbstractProcess[] movements;
    /*
     * Ranges of movement in cell during one Osmose time step
     */
    private static int[] range;
    /*
     * Migration
     */
    private static float[][][] outOfZoneMortality;
    private static boolean[][][] outOfZoneCohort;

    @Override
    public void init() {
        int nSpecies = getSimulation().getNumberSpecies();
        // init migration
        outOfZoneMortality = new float[nSpecies][][];
        outOfZoneCohort = new boolean[nSpecies][][];
        for (int index = 0; index < nSpecies; index++) {
            int longevity = getSpecies(index).getLongevity();
            outOfZoneMortality[index] = new float[longevity][getSimulation().getNumberTimeStepsPerYear()];
            outOfZoneCohort[index] = new boolean[longevity][getSimulation().getNumberTimeStepsPerYear()];
            if (null != getOsmose().migrationTempAge[index]) {
                int nbStepYear = getSimulation().getNumberTimeStepsPerYear();
                for (int m = 0; m < getOsmose().migrationTempAge[index].length; m++) {
                    for (int n = 0; n < getOsmose().migrationTempDt[index].length; n++) {
                        for (int h = 0; h < nbStepYear; h++) {
                            outOfZoneCohort[index][getOsmose().migrationTempAge[index][m] * nbStepYear + h][getOsmose().migrationTempDt[index][n]] = true;
                            outOfZoneMortality[index][getOsmose().migrationTempAge[index][m] * nbStepYear + h][getOsmose().migrationTempDt[index][n]] = getOsmose().migrationTempMortality[index][m];
                        }
                    }
                }
            }
        }
        // init distribution
        range = getOsmose().range[getOsmose().numSerie];
        movements = new AbstractProcess[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            range[i] = 0;
            switch (getOsmose().spatialDistribution[i]) {
                case RANDOM:
                    movements[i] = new RandomDistributionProcess(getSimulation().getSpecies(i));
                    break;
                case MAPS:
                    movements[i] = new MapDistributionProcess(getSimulation().getSpecies(i));
                    break;
                case CONNECTIVITY:
                    movements[i] = new ConnectivityDistributionProcess(getSimulation().getSpecies(i));
                    break;
            }
            movements[i].init();
        }
    }

    @Override
    public void run() {
        for (AbstractProcess movement : movements) {
            movement.run();
        }
        getPopulation().updateSchoolMap();
    }
    
    /**
     * Get the adjacent cells of a given school that are contained in the given
     * map.
     *
     * @param school
     * @param map
     * @return
     */
    public static List<Cell> getAccessibleCells(School school, GridMap map) {

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
        Iterator<Cell> neighbours = getGrid().getNeighbourCells(cell, range[school.getSpeciesIndex()]).iterator();
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
    public static Cell randomDeal(List<Cell> cells) {
        int index = (int) Math.round((cells.size() - 1) * Math.random());
        return cells.get(index);
    }
    
    /**
     * Create a list of the accessible cells for a given cell: neighbour cells
     * that are not in land + current cell
     *
     * @param school
     * @return the list of cells accessible to the school
     */
    public static List<Cell> getAccessibleCells(School school) {

        Cell cell = school.getCell();
        List<Cell> accessibleCells = new ArrayList();
        Iterator<Cell> neighbors = getGrid().getNeighbourCells(cell, range[school.getSpeciesIndex()]).iterator();
        while (neighbors.hasNext()) {
            Cell neighbor = neighbors.next();
            if (!neighbor.isLand()) {
                accessibleCells.add(neighbor);
            }
        }
        return accessibleCells;
    }
    
    public static boolean isOut(School school) {
        return outOfZoneCohort[school.getSpeciesIndex()][school.getAgeDt()][getSimulation().getIndexTimeYear()];
    }

    public static float getOutMortality(School school) {
        return outOfZoneMortality[school.getSpeciesIndex()][school.getAgeDt()][getSimulation().getIndexTimeYear()];
    }
}
