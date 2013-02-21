
package fr.ird.osmose.process;

import fr.ird.osmose.GridMap;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class MapDistributionProcess extends AbstractProcess {
    
    private MovementProcess movement;
    private Species species;
    
    public MapDistributionProcess(Species species, MovementProcess parent) {
        this.species = species;
        this.movement = parent;
    }

    @Override
    public void init() {
    }

    @Override
    public void run() {
        for (School school : getPopulation().getSchools(species)) {
            mapsDistribution(school);
        }
    }
    
    private void mapsDistribution(School school) {

        int i_step_year = getSimulation().getIndexTimeYear();
        int age = school.getAgeDt();
        /*
         * Do not distribute cohorts that are presently out of
         * the simulated area.
         */
        if (movement.isOut(school)) {
            school.setOffGrid();
            return;
        }

        // Get current map and max probability of presence
        int indexMap = movement.getIndexMap(school);
        GridMap map = movement.getMap(indexMap);
        float tempMaxProbaPresence = movement.getMaxProbaPresence(indexMap);

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
                oldTime = getOsmose().getNumberTimeStepsPerYear() - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            int previousIndexMap = movement.getIndexMap(school.getSpeciesIndex(), age - 1, oldTime);
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
            int indexCell;
            int nCells = getGrid().getNbColumns() * getGrid().getNbLines();
            double proba;
            do {
                indexCell = (int) Math.round((nCells - 1) * Math.random());
                proba = movement.getMap(school).getValue(getGrid().getCell(indexCell));
            } while (proba <= 0 || proba < Math.random() * tempMaxProbaPresence);
            school.moveToCell(getGrid().getCell(indexCell));
        } else {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(movement.randomDeal(movement.getAccessibleCells(school, map)));
        }
    }
}
