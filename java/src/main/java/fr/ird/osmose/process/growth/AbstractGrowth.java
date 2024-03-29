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

package fr.ird.osmose.process.growth;

import fr.ird.osmose.Species;
import fr.ird.osmose.util.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractGrowth extends SimulationLinker {

    final private Species species;

    public AbstractGrowth(int rank, Species species) {
        super(rank);
        this.species = species;
    }

    public Species getSpecies() {
        return species;
    }
    
    public int getSpeciesIndex() { 
        return this.species.getSpeciesIndex();
    }
    
    public int getFileSpeciesIndex() { 
        return this.species.getFileSpeciesIndex();
    }
    /**
     * Initialisation of the mortality. This function should loads the
     * appropriate parameters from the configuration files.
     */
    abstract public void init();

    /**
     * Compute the mean length, in centimetre, at a specific age, in year.
     *
     * @param age, an age in year.
     * @return the corresponding mean length
     */
    abstract public double ageToLength(double age);

    /**
     * Compute the mean age, in year, at a specific length, in
     * centimetre.
     *
     * @param length the length in centimetre
     * @return the corresponding mean age in year
     */
    abstract public double lengthToAge(double length);

}
