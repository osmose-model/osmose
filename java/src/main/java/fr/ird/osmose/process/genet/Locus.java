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

package fr.ird.osmose.process.genet;

import java.util.Random;

import fr.ird.osmose.util.OsmoseLinker;

/**
 *
 * @author nbarrier
 */
public class Locus extends OsmoseLinker {

    /**
     * Values of the two Locus (dim=N=2).
     */
    private final double value[];

    /**
     * Number of values for a given loci (alleles).
     */
    private final int N = 2;

    /** Trait that is associated with the given loci.
     This is a pointer to the trait associated with the given locus*/
    private final Trait trait;

    /** Locus index within the given trait. */
    private final int index;

    /** Species associated with the index. */
    private final int spec_index;

    Random generator;

    /**
     * Locus constructor.
     *
     * @param rank
     * @param index
     * @param spec_index
     * @param trait
     */
    public Locus(int index, Trait trait, int spec_index) {

        this.index = index;
        this.trait = trait;
        this.spec_index = spec_index;
        value = new double[N];

        if(getConfiguration().getBoolean("genetics.randomseed.fixed", false)) {
            generator = new Random(index);
        } else {
            generator = new Random();
        }

    }

    /**
     * Random draft of an allele among the n_allele possible.
     */
    public void init_random_draft() {

        // value index in the trait diversity array.
        int valindex;

        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (generator.nextDouble() * this.trait.getNValues(spec_index));
        value[0] = this.trait.getDiv(spec_index, index, valindex);

        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (generator.nextDouble() * this.trait.getNValues(spec_index));
        value[1] = this.trait.getDiv(spec_index, index, valindex);

    }

    /**
     * Set locus values from the parents locus (uniform draft of the parent's
     * locus).
     *
     * @param parent_a
     * @param parent_b
     */
    public void set_from_parents(Locus parent_a, Locus parent_b) {

        // Random draft of an allel (0 or 1) from parent a
        int i = (int) (2 * generator.nextDouble());
        value[0] = parent_a.getValue(i);

        // Random draft of an allel (0 or 1) from parent
        i = (int) (2 * generator.nextDouble());
        value[1] = parent_b.getValue(i);

    }

    /** Recovers the ith value of a loci par.
     *
     * @param i Loci pair index (0 or 1)
     * @return Loci pair value
     */
    public double getValue(int i) {
        if((i < 0) || (i>1)) {
            error("Locus index must be 0 or 1", new IllegalArgumentException());
        }
        return this.value[i];
    }

    /** Sets the value of a given loci.
     *
     * @param i Index of the loci (0 or 1)
     * @param val  Loci value
     */
    public void setValue(int i, double val) {
        if ((i < 0) || (i > 1)) {
            error("Locus index must be 0 or 1", new IllegalArgumentException());
        }
        this.value[i] = val;
    }

    /** Returns the sum of the two loci values.
     * @return Sum of the two loci values
     */
    public double sum() {
        return (this.getValue(0) + this.getValue(1));
    }

}
