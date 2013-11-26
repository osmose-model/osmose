/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright IRD (Institut de Recherche pour le Développement) 2013
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
package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.SchoolSet;
import fr.ird.osmose.Simulation;
import fr.ird.osmose.Species;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.util.logging.OLogger;

/**
 *
 * @author pverley
 */
public class SimulationLinker extends OLogger {

    private final int rank;

    public SimulationLinker(int rank) {
        super(rank);
        this.rank = rank;
    }

    final public int getRank() {
        return rank;
    }

    public Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    public IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    public Simulation getSimulation() {
        return Osmose.getInstance().getSimulation(rank);
    }

    public SchoolSet getSchoolSet() {
        return Osmose.getInstance().getSimulation(rank).getSchoolSet();
    }

    public LTLForcing getForcing() {
        return Osmose.getInstance().getSimulation(rank).getForcing();
    }

    public Species getSpecies(int index) {
        return Osmose.getInstance().getSimulation(rank).getSpecies(index);
    }

    public Species getSpecies(String name) {
        for (int i = 0; i < getNSpecies(); i++) {
            if (getSpecies(i).getName().equalsIgnoreCase(name)) {
                return getSpecies(i);
            }
        }
        return null;
    }

    /**
     * The number of simulated species
     *
     * @return the number of simulated species
     */
    public int getNSpecies() {
        return Osmose.getInstance().getConfiguration().getNSpecies();
    }
}
