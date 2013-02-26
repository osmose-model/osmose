package fr.ird.osmose.process;

import fr.ird.osmose.ConnectivityMatrix;
import fr.ird.osmose.GridMap;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class ConnectProcessTemp extends AbstractProcess {

    private MovementProcess movementProcess;
    private Species species;
    int[][] numMaps;

    public ConnectProcessTemp(int replica, Species species) {
        super(replica);
        this.species = species;
    }

    @Override
    public void init() {
        numMaps = getOsmose().numMap[species.getIndex()];        
    }

    @Override
    public void run() {
        System.out.println("Connectivity distribution process " + getSimulation().getIndexTimeYear());
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
        if (movementProcess.isOut(school)) {
            school.setOffGrid();
            return;
        }

        // Get current map and max probability of presence
        int numMap = numMaps[age][i_step_year];
        GridMap map = getOsmose().getMap(numMap);
        float tempMaxProbaPresence = getOsmose().maxProbaPresence[numMap];

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
                oldTime = getOsmose().getNumberTimeStepsPerYear() - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            if (numMap == numMaps[age - 1][oldTime]) {
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
                proba = map.getValue(getGrid().getCell(indexCell));
            } while (proba <= 0 || proba < Math.random() * tempMaxProbaPresence);
            school.moveToCell(getGrid().getCell(indexCell));
        } else if (sameMap) {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(movementProcess.randomDeal(movementProcess.getAccessibleCells(school, map)));
            if (map.getValue(school.getCell()) <= 0) {
                System.out.println("Thomas a vu juste !!");
            }
        } else {
            connectivityMoveSchool(school, numMap);
        }
    }

    private void connectivityMoveSchool(School school, int numMap) {
        int i_step_year = getSimulation().getIndexTimeYear();
        boolean ok = false;
        int oldMapIndex = -1;
        if (school.getAgeDt() > 0) {
            int oldTime;
            if (i_step_year == 0) {
                oldTime = getOsmose().getNumberTimeStepsPerYear() - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            oldMapIndex = numMaps[school.getAgeDt() - 1][oldTime];
            GridMap oldMap = getOsmose().getMap(oldMapIndex);
            if (oldMap.getValue(school.getCell()) <= 0) {
                System.out.println("Je suis sortie de la carte precedente, pas bon");
            } else {
                ok = true;
            }
        }



        // get the connectivity matrix associated to object school
        // species i, cohort j and time step indexTime.
        ConnectivityMatrix matrix = getOsmose().connectivityMatrix[numMap];
        // get the connectivity of the cell where the school is
        // currently located
        int iCell = school.getCell().getIndex();
        ConnectivityMatrix.ConnectivityLine cline = matrix.clines.get(iCell);
        float[] sum = cumSum(cline.connectivity);
        if (sum[sum.length - 1] == 0) {
            System.out.println(ok + " " + school.getSpecies().getName() + " age " + school.getAgeDt() + " step " + i_step_year + " is in cell " + school.getCell().getIndex() + " not connected to any other one...");
            System.out.println("  i was in map " + oldMapIndex + " and connecting with mat " + numMap);
        }

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
//        for (int i = 0; i < cumSum.length; i++) {
//            //System.out.println(cumSum[i] + "------------------------------>"); 
//            cumSum[i] += cumSum[i] / cumSum[cumSum.length - 1];
//            //System.out.println(cumSum[i] + "------------------------------>");
//        } // FIN TD ~~ 24.10.2012  --> normalisation cumSum

        return cumSum;
    }
    
//     public void writeAreaFileAsProp() {
//        fr.ird.osmose.util.Properties properties = new fr.ird.osmose.util.Properties();
//        for (int indexMap = 0; indexMap < maps.length; indexMap++) {
//            String map = "map[" + indexMap + "]";
//            String value;
//            StringBuilder key = new StringBuilder(map);
//            key.append(".species");
//            value = nameSpecMatrix[numSerie][areasNumSpForMap[indexMap]];
//            properties.setProperty(key.toString(), value);
//            key = new StringBuilder(map);
//            key.append(".agemin");
//            value = String.valueOf(areasTempAge[indexMap][0]);
//            properties.setProperty(key.toString(), value);
//            key = new StringBuilder(map);
//            key.append(".agemax");
//            value = String.valueOf(areasTempAge[indexMap][areasTempAge[indexMap].length - 1] + 1);
//            properties.setProperty(key.toString(), value);
//            key = new StringBuilder(map);
//            key.append(".season");
//            value = arrayIntToString(areasTempDt[indexMap]);
//            properties.setProperty(key.toString(), value);
//            key = new StringBuilder(map);
//            key.append(".csv");
//            value = mapFile[indexMap];
//            properties.setProperty(key.toString(), value);
//            if (spatialDistribution[areasNumSpForMap[indexMap]] == SpatialDistribution.CONNECTIVITY) {
//                key = new StringBuilder(map);
//                key.append(".connectivity");
//                value = connectivityFile[indexMap];
//                properties.setProperty(key.toString(), value);
//            }
//        }
//        String fileName = "area.cfg";
//        try {
//            properties.store(new FileWriter(resolveFile(fileName)), fileSeparator);
//        } catch (IOException ex) {
//            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.exit(0);
//    }
//
//    private String arrayIntToString(int[] array) {
//        if (null == array) {
//            return null;
//        }
//        StringBuilder str = new StringBuilder();
//        str.append(array[0]);
//        for (int i = 1; i < array.length; i++) {
//            str.append(", ");
//            str.append(array[i]);
//        }
//        return str.toString();
//    }
}
