/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose.filter;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
 public class SpeciesFilter implements IFilter<School> {

        final private int speciesIndex;

        public SpeciesFilter(int speciesIndex) {
            this.speciesIndex = speciesIndex;
        }

        @Override
        public boolean accept(School school) {
            return speciesIndex == school.getSpeciesIndex();
        }
    }
