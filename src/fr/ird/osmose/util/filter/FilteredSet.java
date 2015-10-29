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
package fr.ird.osmose.util.filter;

import java.util.ArrayList;

/**
 *
 * @author pverley
 */
public class FilteredSet<E> extends ArrayList<E> {

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
