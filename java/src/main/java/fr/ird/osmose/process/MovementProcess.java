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

package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.process.movement.AbstractSpatialDistribution;
import fr.ird.osmose.process.movement.MapDistribution;
import fr.ird.osmose.process.movement.RandomDistribution;

/**
 *
 * @author pverley
 */
public class MovementProcess extends AbstractProcess {

    private AbstractSpatialDistribution[] spatialDistribution;

    public MovementProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        // init distribution
        spatialDistribution = new AbstractSpatialDistribution[nSpecies];
        int cpt = 0;
        for (int iSpecies : this.getFocalIndex()) {
            String distributionMethod = getConfiguration().getString("movement.distribution.method.sp" + iSpecies);
            if (distributionMethod.equalsIgnoreCase("random")) {
                spatialDistribution[cpt] = new RandomDistribution(iSpecies, cpt);
            } else if (distributionMethod.equalsIgnoreCase("maps")) {
                spatialDistribution[cpt] = new MapDistribution(iSpecies, cpt);
            } else {
                throw new UnsupportedOperationException("Distribution method is either 'random' or 'maps'");
            }
            spatialDistribution[cpt].init();
            cpt++;
        }
    }

    @Override
    public void run() {
        for (School school : getSchoolSet().getSchools()) {
            spatialDistribution[school.getSpeciesIndex()].move(school, getSimulation().getIndexTimeSimu());
        }

        getSchoolSet().updateSchoolMap();
    }

    public enum SpatialDistribution {

        RANDOM,
        MAPS;
        // phv 20140116 temporarily disabled connectivity
        //CONNECTIVITY;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
