
package fr.ird.osmose.process;

import fr.ird.osmose.ConnectivityMatrix;
import fr.ird.osmose.GridMap;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class ConnectivityDistributionProcess extends AbstractProcess {
    
    private MovementProcess parent;
    private Species species;
    
    public ConnectivityDistributionProcess(Species species, MovementProcess parent) {
        this.species = species;
        this.parent = parent;
    }

    @Override
    public void init() {
    }

    @Override
    public void run() {
        for (School school : getPopulation().getSchools(species)) {
            connectivityDistribution(school);
        }
    }
    
    private void connectivityDistribution(School school) {

        // loop over the schools of the species
        int age = school.getAgeDt();
        int i_step_year = getSimulation().getIndexTimeYear();
        int i_step_simu = getSimulation().getIndexTimeSimu();
        
        /*
         * Do not distribute cohorts that are presently out of
         * the simulated area.
         */
        if (MovementProcess.isOut(school)) {
            school.setOffGrid();
            return;
        }

        // Get current map and max probability of presence
        int indexMap = parent.getIndexMap(school);
        GridMap map = parent.getMap(indexMap);
        float tempMaxProbaPresence = parent.getMaxProbaPresence(indexMap);
        
        // init = true if either cohort zero or first time-step of the simulation
        boolean init = (age == 0) | (i_step_simu == 0);
        /*
         * boolean sameMap
         * Check whether the map has changed from previous cohort
         * and time-step.
         */
        boolean sameMap = false;
        if (age > 0 && i_step_simu > 0) {
            int oldTime;
            if (i_step_year == 0) {
                oldTime = getSimulation().getNumberTimeStepsPerYear() - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            int previousIndexMap = parent.getIndexMap(school.getSpeciesIndex(), age - 1, oldTime);
            if (indexMap == previousIndexMap) {
                sameMap = true;
            }
        }

        // Move the school
        if (init || school.isUnlocated()) {
            /*
             * Random distribution in a map, either because it is cohort
             * zero or first time-step or because the
             * school was unlocated due to migration.
             */
            int indexCell;
            int nCells = getGrid().getNbColumns() * getGrid().getNbLines();
            double proba;
            do {
                indexCell = (int) Math.round((nCells - 1) * Math.random());
                proba = parent.getMap(school).getValue(getGrid().getCell(indexCell));
            } while (proba <= 0 || proba < Math.random() * tempMaxProbaPresence);
            school.moveToCell(getGrid().getCell(indexCell));
        } else if (sameMap) {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(parent.randomDeal(parent.getAccessibleCells(school, map)));
        } else {
            connectivityMoveSchool(school, indexMap);
        }
    }

    private void connectivityMoveSchool(School school, int indexMap) {
        // get the connectivity matrix associated to object school
        // species i, cohort j and time step indexTime.
        ConnectivityMatrix matrix = parent.getMatrix(indexMap);
        // get the connectivity of the cell where the school is
        // currently located
        int iCell = school.getCell().getIndex();
        //System.out.println(indexMap + " " + (matrix == null) + " " + school.getAgeDt() + " " + getSimulation().getIndexTimeYear() + " " + school.isUnlocated());
        ConnectivityMatrix.ConnectivityLine cline = matrix.clines.get(iCell);

        if (!school.getCell().isLand() && null == cline) { // TD ~~
            //if (null == cline) { // TD ~~
            throw new NullPointerException("Could not find line associated to cell "
                    + iCell + " in connectivity matrix " + " ;isLand= " + school.getCell().isLand());
        }

        // TD CHANGE 23.10.12
        // Lines with only 0 come with length = 0
        // cumsum can't work with it
        // workaround: run cumsum only if length > 0 (otherwise keep initial 0 values)

        // computes the cumulative sum of this connectivity line   
        if (!school.getCell().isLand() && cline.connectivity.length > 0) { //++TD

            float[] cumSum = cumSum(cline.connectivity);

            //TD DEBUG 29.10.2012
            //if (indexTime >= (5 * nbTimeStepsPerYear) && school.getCell().isLand()) {
            if (getSimulation().getIndexTimeYear() >= 1 && school.getCell().isLand()) {
                System.out.println("SCHOOL SWIMMING OUT OF THE POOL! <-----------------------------------");
            }
            //TD DEBUG 29.10.2012
            // choose the new cell
            // TD ~~ 24.10.2012
            //float random = (float) (Math.random() * cumSum[cumSum.length - 1]); // random 0-1 * plus grande valeur de cumsum (dernière valeur) --> ???
            //System.out.println("cumSum[cumSum.length - 1]: " + cumSum[cumSum.length - 1] + " random: " + random );
            // alternative : TD ~~
            float random = (float) (Math.random()); //TD ~~
            int iRandom = cumSum.length - 1; // on prend le dernier de la liste
            while (random < cumSum[iRandom] && iRandom > 0) { // et on redescend progressivement la liste jusqu'à trouver une valeur inférieure à random
                iRandom--;
            }
            school.moveToCell(getGrid().getCell(cline.indexCells[iRandom]));
        }
    }

    private float[] cumSum(float[] connectivity) {

        float[] cumSum = new float[connectivity.length];
        cumSum[0] = connectivity[0];

        for (int i = 1; i < cumSum.length; i++) {
            cumSum[i] += cumSum[i - 1] + connectivity[i];
        }

        // TD ~~ 24.10.2012  --> normalisation cumSum
        for (int i = 0; i < cumSum.length; i++) {
            //System.out.println(cumSum[i] + "------------------------------>"); 
            cumSum[i] += cumSum[i] / cumSum[cumSum.length - 1];
            //System.out.println(cumSum[i] + "------------------------------>");
        } // FIN TD ~~ 24.10.2012  --> normalisation cumSum

        return cumSum;
    }
    
}
