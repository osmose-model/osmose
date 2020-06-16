/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.genet;

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

    }
    
    /**
     * Random draft of an allele among the n_allele possible.
     */
    public void init_random_draft() {

        // value index in the trait diversity array.
        int valindex;
         
        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (Math.random() * this.trait.getNValues(spec_index));
        value[0] = this.trait.getDiv(spec_index, index, valindex);
        
        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (Math.random() * this.trait.getNValues(spec_index));
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
        int i = (int) (2 * Math.random());
        value[0] = parent_a.getValue(i);

        // Random draft of an allel (0 or 1) from parent 
        i = (int) (2 * Math.random());
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
