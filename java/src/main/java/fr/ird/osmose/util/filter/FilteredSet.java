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

import java.util.LinkedList;

/**
 *
 * @author pverley
 * @param <E>
 */
public class FilteredSet<E> extends LinkedList<E> {

    final private FilteredSet<E> parent;
    final private IFilter<E>[] filters;
    final private int level;

    public FilteredSet(FilteredSet<E> parent, IFilter<E>[] filters, int level) {
        this.parent = parent;
        this.filters = filters;
        this.level = level;
    }

     public FilteredSet(FilteredSet<E> parent, IFilter<E> filter, int level) {
        this(parent, new IFilter[]{filter}, level);
    }

    public FilteredSet(FilteredSet<E> parent, IFilter<E>[] filters) {
        this(parent, filters, parent.getLevel());
    }

    public FilteredSet(FilteredSet<E> parent, IFilter<E> filter) {
        this(parent, new IFilter[]{filter});
    }

    public FilteredSet() {
        this.parent = null;
        this.filters = null;
        level = 0;
    }

    public FilteredSet<E> getParent() {
        return parent;
    }

    public IFilter<E>[] getFilters() {
        return filters;
    }

    public int getLevel() {
        return level;
    }
    
    void refresh() {
        if (null != parent) {
            clear();
            for (E member : parent) {
                boolean accept = true;
                if (filters != null) {
                    for (IFilter<? super E> filter : filters) {
                        accept = accept && filter.accept(member);
                        if (!accept) {
                            break;
                        }
                    }
                    if (accept) {
                        add(member);
                    }
                }
            }
        } else {
            //throw new NullPointerException("Community's parent is null");
        }
    }
}
