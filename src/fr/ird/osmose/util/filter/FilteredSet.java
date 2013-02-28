/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.filter;

import fr.ird.osmose.util.filter.IFilter;
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
}
