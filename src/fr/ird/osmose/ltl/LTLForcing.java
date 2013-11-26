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
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;

/**
 * This interface indicates how Osmose manages and retrieves the biomass of the
 * low trophic levels compartments of the ecosystem. The LTL groups are an input
 * of the model, they are forcing the model biomass as they provide food to the
 * higher trophic levels.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public interface LTLForcing {

    /**
     * Initializes the LTLForcing. Read configuration files; load the LTL grid
     * (as it may be different from the Osmose grid); ensure that the LTL
     * forcing files matches the description of the plankton groups; etc. Such
     * are the actions to be undertaken in this function.
     */
    public void init();

    /**
     * Update the biomass of the LTL groups for the current time step of the
     * simulation.
     *
     * @param iStepSimu, the current time step of the simulation
     */
    public void update(int iStepSimu);

    /**
     * Returns the biomass, in tonne, of a specified LTL group in a specified
     * cell at current time step of the simulation.
     *
     * @param iLTL, the index of the LTL group
     * @param cell, a {@code Cell} of the grid
     * @return the biomass, in tonne, of the LTL group at index {@code iLTL} in
     * this {@code cell}}
     */
    public float getBiomass(int iLTL, Cell cell);
}
