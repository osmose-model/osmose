/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.genet;

import fr.ird.osmose.util.SimulationLinker;

/**
 *
 * @author nbarrier
 */
public class Locus extends SimulationLinker {

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
    
    /**
     * Locus constructor.
     *
     * @param rank
     * @param index
     * @param trait
     */
    public Locus(int rank, int index, Trait trait) {

        super(rank);
        this.index = index;
        this.trait = trait;
        value = new double[N];

    }
    
    /**
     * Random draft of an allele among the n_allele possible.
     */
    public void init_random_draft() {

        // value index in the trait diversity array.
        int valindex;
         
        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (Math.random() * this.trait.getNValues());
        value[0] = this.trait.getDiv(index, valindex);
        
        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (Math.random() * this.trait.getNValues());
        value[1] = this.trait.getDiv(index, valindex);
        
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

    public double getValue(int i) {
        return this.value[i];
    }

    public double sum() {
        return (this.getValue(0) + this.getValue(1));
    }

}
