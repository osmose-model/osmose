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

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.Species;
import fr.ird.osmose.background.BackgroundSpecies;
import fr.ird.osmose.grid.AbstractGrid;
import fr.ird.osmose.util.logging.OLogger;
import java.util.Arrays;
import java.util.stream.IntStream;
import fr.ird.osmose.ISpecies;
import fr.ird.osmose.resource.ResourceSpecies;

/**
 *
 * @author pverley
 */
public class OsmoseLinker extends OLogger {

    final public Osmose getOsmose() {
        return Osmose.getInstance();
    }

    final public Configuration getConfiguration() {
        return getOsmose().getConfiguration();
    }

    final public Species getSpecies(int index) {
        return getConfiguration().getSpecies(index);
    }

    final public ResourceSpecies getRscSpecies(int index) {
        return getConfiguration().getResourceSpecies(index);
    }

    final public ISpecies getISpecies(int index) {
        int nSpecies = this.getNSpecies();
        int nBkg = this.getNBkgSpecies();
        if (index < nSpecies) {
            return this.getSpecies(index);
        } else if (index < nSpecies + nBkg) {
            return this.getBkgSpecies(index - nSpecies);
        } else {
            return this.getRscSpecies(index - nSpecies - nBkg);
        }

    }

    final public Species getSpecies(String name) {
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
    final public int getNSpecies() {
        return getConfiguration().getNSpecies();
    }

    /**
     * The number of simulated species
     *
     * @return the number of simulated species
     */
    final public int getNAllSpecies() {
        return getConfiguration().getNAllSpecies();
    }

    /**
     * The number of simulated species
     *
     * @return the number of simulated species
     */
    final public int getNPredSpecies() {
        return getConfiguration().getNPredSpecies();
    }


    final public AbstractGrid getGrid() {
        return getOsmose().getConfiguration().getGrid();
    }

    final public int getNBkgSpecies() {
        return getConfiguration().getNBkgSpecies();
    }

    final public int getNRscSpecies() {
        return getConfiguration().getNRscSpecies();
    }
    
    final public int[] getFocalIndex() {
        return getConfiguration().getFocalIndex();
    }

    final public int[] getBackgroundIndex() {
        return getConfiguration().getBackgroundIndex();
    }

    final public int[] getResourceIndex() {
        return getConfiguration().getResourceIndex();
    }
        
    final public BackgroundSpecies getBkgSpecies(int index) {
        return getConfiguration().getBkgSpecies(index);
    }

    final public BackgroundSpecies getBkgSpecies(String name) {
        for (int i = 0; i < getNBkgSpecies(); i++) {
            if (getBkgSpecies(i).getName().equalsIgnoreCase(name)) {
                return getBkgSpecies(i);
            }
        }
        return null;
    }

    /**
     * Converts from new species index to "old" species index.
     *
     * @param index New species index (can exceed nspecies)
     * @return The index in the old format.
     */
    final public int newToOldIndex(int index) {

        int output = 0;

        // If focal species, index is the position with FocalIndex
        if (IntStream.of(this.getConfiguration().getFocalIndex()).anyMatch(x -> x == index)) {
            output = IntStream.of(this.getConfiguration().getFocalIndex()).filter(x -> x == index).findFirst().getAsInt();
        } else if (IntStream.of(this.getConfiguration().getBackgroundIndex()).anyMatch(x -> x == index)) {
            output = IntStream.of(this.getConfiguration().getBackgroundIndex()).filter(x -> x == index).findFirst().getAsInt();
            output += this.getNSpecies();
        } else if (IntStream.of(this.getConfiguration().getResourceIndex()).anyMatch(x -> x == index)) {
            output = IntStream.of(this.getConfiguration().getResourceIndex()).filter(x -> x == index).findFirst().getAsInt();
            output += this.getNSpecies() + this.getNBkgSpecies();
        } else {
            StringBuilder mess = new StringBuilder();
            mess.append("Index ").append(index).append(" cannot be converted into saving index");
            error(mess.toString(), null);
        }

        return output;

    }

    /**
     * Converts from new species index to "old" species index.
     *
     * @param index New species index (can exceed nspecies)
     * @return The index in the old format.
     */
    final public int oldToNewIndex(int index) {

        if (index < this.getNSpecies()) {
            return this.getConfiguration().getFocalIndex(index);
        } else if (index < this.getNSpecies() + this.getNBkgSpecies()) {
            return this.getConfiguration().getBkgIndex(index - getNSpecies());
        } else {
            return this.getConfiguration().getRscIndex(index - this.getNSpecies() - this.getNBkgSpecies());
        }
    }

    // Function to find the index of an element 
    public static int findIndex(int arr[], int t) {
        int len = arr.length;
        return IntStream.range(0, len)
                .filter(i -> t == arr[i])
                .findFirst() // first occurrence 
                .orElse(-1); // No element found 
    }

}
