/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
package fr.ird.osmose.process;

import fr.ird.osmose.util.ConnectivityMatrix;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class ConnectivityDistributionProcess extends AbstractProcess {

    private final MovementProcess movement;
    private final Species species;

    public ConnectivityDistributionProcess(int rank, Species species, MovementProcess parent) {
        super(rank);
        this.species = species;
        this.movement = parent;
    }

    @Override
    public void init() {
    }

    @Override
    public void run() {
        for (School school : getSchoolSet().getSchools(species)) {
            if (!movement.isOut(school)) {
                connectivityDistribution(school);
            } else {
                school.out();
            }
        }
    }

    private void connectivityDistribution(School school) {

        // loop over the schools of the species
        int age = school.getAgeDt();
        int i_step_year = getSimulation().getIndexTimeYear();
        int i_step_simu = getSimulation().getIndexTimeSimu();

        // Get current map and max probability of presence
        int indexMap = movement.getIndexMap(school);
        GridMap map = movement.getMap(indexMap);
        float tempMaxProbaPresence = movement.getMaxProbaPresence(indexMap);

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
                oldTime = getConfiguration().getNStepYear() - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            int previousIndexMap = movement.getIndexMap(school.getSpeciesIndex(), age - 1, oldTime);
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
            int nCells = getGrid().get_nx() * getGrid().get_ny();
            double proba;
            do {
                indexCell = (int) Math.round((nCells - 1) * Math.random());
                proba = movement.getMap(school).getValue(getGrid().getCell(indexCell));
            } while (proba <= 0 || proba < Math.random() * tempMaxProbaPresence);
            school.moveToCell(getGrid().getCell(indexCell));
        } else if (sameMap) {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(movement.randomDeal(movement.getAccessibleCells(school, map)));
        } else {
            connectivityMoveSchool(school, indexMap);
        }
    }

    private void connectivityMoveSchool(School school, int indexMap) {
        // get the connectivity matrix associated to object school
        // species i, cohort j and time step indexTime.
        ConnectivityMatrix matrix = movement.getMatrix(indexMap);
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
//public void writeAreaFileAsProp() {
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

