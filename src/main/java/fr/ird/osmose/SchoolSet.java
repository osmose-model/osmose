/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
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
package fr.ird.osmose;

import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.filter.AliveSchoolFilter;
import fr.ird.osmose.util.filter.FilteredSet;
import fr.ird.osmose.util.filter.FilteredSets;
import fr.ird.osmose.util.filter.IFilter;
import fr.ird.osmose.util.filter.OldSchoolFilter;
import fr.ird.osmose.util.filter.OutSchoolFilter;
import fr.ird.osmose.util.filter.PresentSchoolFilter;
import fr.ird.osmose.util.filter.RegionSchoolFilter;
import fr.ird.osmose.util.filter.SpeciesFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class is a set that contains all the school of the simulation. It
 * provides some functions to get subsets of specific schools, from a given
 * species, schools out of the simulated domain, alive schools, schools at a
 * given location, etc.
 *
 * @see fr.ird.osmose.util.filter.FilteredSet
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class SchoolSet extends OsmoseLinker {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Set of all the schools
     */
    private final FilteredSet<School> schoolset;
    /**
     * Snapshot of the distribution of the schools on the grid.
     */
    private final HashMap<Integer, List<School>> schoolByCell;
    /**
     * Snapshot of the distribution of the schools per species.
     */
    /**
     * Array of list of schools gathered by species.
     */
    private final HashMap<Integer, List<School>> schoolBySpecies;
    /**
     * Array of boolean that indicates whether the list of schools per species
     * has changed.
     */
    private final boolean[] hasSpeciesChanged;

    SchoolSet() {
        schoolset = new FilteredSet();
        schoolBySpecies = new HashMap();
        schoolByCell = new HashMap();
        hasSpeciesChanged = new boolean[getConfiguration().getNSpecies()];
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            hasSpeciesChanged[i] = true;
        }
    }

///////////////////////////////
// Definition of the functions
///////////////////////////////
    /**
     * Add a school to the school set.
     *
     * @param school, the school to add to the set.
     */
    public void add(School school) {
        schoolset.add(school);
    }

    /**
     * Remove all the schools.
     */
    public void clear() {
        schoolset.clear();
        schoolByCell.clear();
        schoolBySpecies.clear();
    }

    /**
     * Remove dead schools from the set
     */
    public void removeDeadSchools() {

        Iterator<School> it = schoolset.iterator();
        while (it.hasNext()) {
            if (!it.next().isAlive()) {
                it.remove();
            }
        }
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            hasSpeciesChanged[i] = true;
        }
    }

    /**
     * Returns all the schools.
     *
     * @return a list of all the schools
     */
    public List<School> getSchools() {
        return schoolset;
    }

    /**
     * Returns a list of schools of the given species.
     *
     * @param species, the species to select
     * @param update, if {@code false} returns the cached list of schools for
     * this species
     * @return a list of schools of this {@code species}
     */
    public List<School> getSchools(Species species, boolean update) {
        if (update || hasSpeciesChanged[species.getIndex()]) {
            schoolBySpecies.put(species.getIndex(), FilteredSets.subset(schoolset, new IFilter[]{new SpeciesFilter(species.getIndex()), new AliveSchoolFilter()}));
            hasSpeciesChanged[species.getIndex()] = false;
        }
        return schoolBySpecies.get(species.getIndex());
    }

    /**
     * Returns an up to date list of schools of the given species.
     *
     * @param species, the species to select
     * @return a list of schools of this {@code species}
     */
    public List<School> getSchools(Species species) {
        return getSchools(species, true);
    }

    /**
     * Returns a list of schools of the given species.
     *
     * @param species, the species to select
     * @param update, if {@code false} returns the cached list of schools for
     * this species
     * @return a list of schools of this {@code species}
     */
    public List<School> getSchoolsAll(Species species, boolean update) {
        if (update || hasSpeciesChanged[species.getIndex()]) {
            schoolBySpecies.put(species.getIndex(), FilteredSets.subset(schoolset, new IFilter[]{new SpeciesFilter(species.getIndex())}));
            hasSpeciesChanged[species.getIndex()] = false;
        }
        return schoolBySpecies.get(species.getIndex());
    }

    /**
     * Returns an up to date list of schools of the given species.
     *
     * @param species, the species to select
     * @return a list of schools of this {@code species}
     */
    public List<School> getSchoolsAll(Species species) {
        return getSchoolsAll(species, true);
    }

    /**
     * Get the list of the schools that are located in the given cell at current
     * time step.
     *
     * @param cell, a cell of the grid
     * @return the list of the schools located in the cell. The list might be
     * empty if it contains no school or null if the cell is on land.
     */
    public List<School> getSchools(Cell cell) {
        return schoolByCell.get(cell.getIndex());
    }

    /**
     * Get a list of the schools that are physically located on the grid at
     * current time step.
     *
     * @return a List of the schools present on the grid at current time step
     */
    public List<School> getPresentSchools() {
        return FilteredSets.subset(schoolset, new PresentSchoolFilter());
    }

    /**
     * Get a list of the schools that are out of the simulated domain at current
     * time step.
     *
     * @return a List of the schools out of the simulated domain at current time
     * step.
     */
    public List<School> getOutSchools() {
        return FilteredSets.subset(schoolset, new OutSchoolFilter());
    }

    /**
     * Get a list of the alive schools.
     *
     * @return a list of the alive schools
     */
    public List<School> getAliveSchools() {
        return FilteredSets.subset(schoolset, new AliveSchoolFilter());
    }

    /**
     * Get a list of the old school.
     *
     * @return a list of the dead schools because olderness
     */
    public List<School> getOldSchools() {
        return FilteredSets.subset(schoolset, new OldSchoolFilter());
    }

    /**
     * Take a snapshot of the distribution of the schools on the grid at current
     * time step.
     */
    public void updateSchoolMap() {

        // Clear the map
        for (List<School> schools : schoolByCell.values()) {
            schools.clear();
        }

        // Fill up the map
        for (School school : schoolset) {
            if (!school.isUnlocated()) {
                int iCell = school.getCell().getIndex();
                if (!schoolByCell.containsKey(iCell)) {
                    schoolByCell.put(iCell, new ArrayList());
                }
                schoolByCell.get(iCell).add(school);
            }
        }

        // Remove empty cells from the map
        Iterator<Integer> it = schoolByCell.keySet().iterator();
        while (it.hasNext()) {
            int iCell = it.next();
            if (schoolByCell.get(iCell).isEmpty()) {
                it.remove();
            }
        }
    }

    /**
     * Returns a list of alive schools within 
     * a given output domain.
     *
     * @param species, the species to select
     * @param update, if {@code false} returns the cached list of schools for
     * this species
     * @return a list of schools of this {@code species}
     */
    public List<School> getRegionSchools(int idom) {
        return FilteredSets.subset(schoolset, new IFilter[]{new AliveSchoolFilter(), new RegionSchoolFilter(idom)});
    }

}
