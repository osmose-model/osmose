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

package fr.ird.osmose.background;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.OsmoseLinker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author barrier
 */
public class BackgroundSchoolSet extends OsmoseLinker {

    /**
     * The set of background species schools. Structure is (cell index, list of
     * schools). The list contains one school per species per size class.
     *
     */
    private HashMap<Integer, List<BackgroundSchool>> bkgSet;

    public void init() {
        bkgSet = new HashMap();
    }

    /**
     * Return the whole list of background schools at the given cell.
     *
     * If the HashMap is empty at the given cell, the HashMap is updated from a
     * list of new schools.
     *
     * @param cell
     * @return
     */
    public List<BackgroundSchool> getBackgroundSchool(Cell cell) {
        if (!bkgSet.containsKey(cell.getIndex())) {
            List<BackgroundSchool> bkgSchools = new ArrayList();  // list of all the background species within the cell
            for (int ibkg : this.getConfiguration().getBkgIndex()) {
                int nClass = this.getConfiguration().getBkgSpecies(ibkg).getNClass();
                for (int cl = 0; cl < nClass; cl++) {
                    bkgSchools.add(new BackgroundSchool(getConfiguration().getBkgSpecies(ibkg), cl, cell));
                }
            }
            bkgSet.put(cell.getIndex(), bkgSchools);
        }
        return bkgSet.get(cell.getIndex());
    }

    /**
     * Returns the list of list of background schools.
     *
     * @return
     */
    public Collection<List<BackgroundSchool>> getValues() {
        return this.bkgSet.values();
    }

    /**
     * Returns all the background schools. From all cell, all species and all
     * size classes.
     *
     * @return
     */
    public List<BackgroundSchool> getAllSchools() {
        ArrayList<BackgroundSchool> list = new ArrayList();
        if (!bkgSet.isEmpty()) {
            this.bkgSet.values().forEach(l -> list.addAll(l));
        }
        return list;
    }
}
