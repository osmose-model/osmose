
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
    
    private AbstractProcess[] movements;

    @Override
    public void init() {
        movements = new AbstractProcess[getSimulation().getNumberSpecies()];
        for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
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
        Iterator<Cell> neighbors = getGrid().getNeighbourCells(cell).iterator();
        while (neighbors.hasNext()) {
            Cell neighbor = neighbors.next();
            if (!neighbor.isLand()) {
                accessibleCells.add(neighbor);
            }
        }
        return accessibleCells;
    }
    
}
