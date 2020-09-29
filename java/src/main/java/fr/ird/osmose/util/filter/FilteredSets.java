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

package fr.ird.osmose.util.filter;

import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author pverley
 */
public class FilteredSets {

    /**
     * Given a subset, the extract method creates a sub subset thanks to
     * the CommunityFilter.
     * 
     * @param <T>
     * @param set
     * @param filters
     * @return
     */
    public static <T> FilteredSet<T> subset(FilteredSet<T> set, IFilter<? super T>[] filters) {

        FilteredSet<T> subset = new FilteredSet(set, filters);
        subset.refresh();
        return subset;
    }

    public static <T> FilteredSet<T> subset(FilteredSet<T> set, IFilter<? super T> filter) {
        return subset(set, new IFilter[]{filter});
    }

    public static <T> FilteredSet<T> intersect(FilteredSet<T> subset1, FilteredSet<T> subset2) {

        HashSet<T> set = new HashSet();
        set.addAll(subset1);
        set.addAll(subset2);

        FilteredSet<T> merged = new FilteredSet();
        merged.addAll(set);

        HashSet<IFilter<? super T>> filters = new HashSet();
        filters.addAll(Arrays.asList(subset1.getFilters()));
        filters.addAll(Arrays.asList(subset2.getFilters()));

        return FilteredSets.subset(merged, filters.toArray(new IFilter[filters.size()]));
    }
}
