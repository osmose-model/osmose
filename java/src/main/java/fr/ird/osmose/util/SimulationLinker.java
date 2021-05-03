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

package fr.ird.osmose.util;

import fr.ird.osmose.SchoolSet;
import fr.ird.osmose.Simulation;
import fr.ird.osmose.process.genet.Trait;
import fr.ird.osmose.process.mortality.FishingGear;
import fr.ird.osmose.resource.ResourceForcing;
import fr.ird.osmose.background.BackgroundSchoolSet;

/**
 *
 * @author pverley
 */
public class SimulationLinker extends OsmoseLinker {

    private final int rank;

    public SimulationLinker(int rank) {
        this.rank = rank;
    }

    final public int getRank() {
        return rank;
    }

    public Simulation getSimulation() {
        return getOsmose().getSimulation(rank);
    }

    public SchoolSet getSchoolSet() {
        return getOsmose().getSimulation(rank).getSchoolSet();
    }
    
    public BackgroundSchoolSet getBkgSchoolSet() {
        return getOsmose().getSimulation(rank).getBkgSchoolSet();
    }

    public ResourceForcing getResourceForcing(int index) {
        return getOsmose().getSimulation(rank).getResourceForcing(index);
    }
    
    public ResourceForcing[] getResourceForcing() {
        return getOsmose().getSimulation(rank).getResourceForcing();
    }
    
    public FishingGear[] getFishingGear() { 
        return getOsmose().getSimulation(rank).getFishingGear();
    }
    
    
    /** Returns the ith trait for the given simulation.
     * @param i
     * @return 
     */
    public Trait getEvolvingTrait(int i) {
        return getOsmose().getSimulation(rank).getEvolvingTrait(i);
    }

    /** Returns the total number of evolving traits for the given simulation
     * 
     * @return 
     */
    public int getNEvolvingTraits() {
        return getOsmose().getSimulation(rank).getNEvolvingTraits();
    }
}
