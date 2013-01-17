/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.filter;

import fr.ird.osmose.Osmose;
import fr.ird.osmose.filter.IFilter;
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
     * @param filter
     * @return
     */
    public static <T> FilteredSet<T> subset(FilteredSet<T> set, IFilter<? super T>[] filters) {

        FilteredSet<T> subset = new FilteredSet(set, filters);
        refresh(subset);
        return subset;
    }

    public static <T> FilteredSet<T> subset(FilteredSet<T> set, IFilter<? super T> filter) {
        return subset(set, new IFilter[]{filter});
    }

    public static <T> void refresh(FilteredSet<T> set) {
        if (null != set.getParent()) {
            set.clear();
            IFilter<? super T>[] filters = set.getFilters();
            for (T member : set.getParent()) {
                boolean accept = true;
                if (filters != null) {
                    for (IFilter<? super T> filter : filters) {
                        accept = accept && filter.accept(member);
                        if (!accept) {
                            break;
                        }
                    }
                    if (accept) {
                        set.add(member);
                    }
                }
            }
        } else {
            //throw new NullPointerException("Community's parent is null");
        }
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

    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }
}
