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

import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.util.MapSet;
import java.util.Random;

/**
 *
 * @author pverley
 */
public class MapDistributionProcess extends AbstractProcess {

    private final MovementProcess parent;
    private final Species species;
    private Random rd1, rd2, rd3;
    private MapSet maps;
    private float[] maxProbaPresence;

    public MapDistributionProcess(int rank, Species species, MovementProcess parent) {
        super(rank);
        this.species = species;
        this.parent = parent;
    }

    @Override
    public void init() {

        boolean fixedSeed = false;
        if (!getConfiguration().isNull("movement.randomseed.fixed")) {
            fixedSeed = getConfiguration().getBoolean("movement.randomseed.fixed");
        }
        if (fixedSeed) {
            rd1 = new Random(13L ^ species.getIndex());
            rd2 = new Random(5L ^ species.getIndex());
            rd3 = new Random(1982L ^ species.getIndex());
            warning("Parameter 'movement.randomseed.fixed' is set to true. It means that two simulations with strictly identical initial school distribution will lead to same movement.");
        } else {
            rd1 = new Random();
            rd2 = new Random();
            rd3 = new Random();
        }

        maps = new MapSet(getRank(), species.getIndex(), "movement");
        maps.init();
        maxProbaPresence = new float[maps.getNMap()];
        for (int imap = 0; imap < maxProbaPresence.length; imap++) {
            maxProbaPresence[imap] = computeMaxProbaPresence(imap);
            // Just a trick for absence/presence maps (and not probability)
            if (maxProbaPresence[imap] >= 1.f) {
                maxProbaPresence[imap] = 0.f;
            }
        }
    }

    @Override
    public void run() {
        for (School school : getSchoolSet().getSchools(species)) {
            if (!isOut(school)) {
                mapsDistribution(school);
            } else {
                school.out();
            }
        }
    }

    private boolean isOut(School school) {
        return (null == maps.getMap(school));
    }

    private void mapsDistribution(School school) {

        int i_step_year = getSimulation().getIndexTimeYear();
        int age = school.getAgeDt();

        // Get current map and max probability of presence
        int indexMap = maps.getIndexMap(school);
        GridMap map = maps.getMap(indexMap);

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
                oldTime = getConfiguration().getNStepYear() - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            int previousIndexMap = maps.getIndexMap(age - 1, oldTime);
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
            int nCells = getGrid().get_nx() * getGrid().get_ny();
            double proba;
            do {
                indexCell = (int) Math.round((nCells - 1) * rd1.nextDouble());
                proba = map.getValue(getGrid().getCell(indexCell));
            } while (proba <= 0.d || proba < rd2.nextDouble() * maxProbaPresence[indexMap]);
            school.moveToCell(getGrid().getCell(indexCell));
        } else {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(parent.randomDeal(parent.getAccessibleCells(school, map), rd3));
        }
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
