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
     * Values of the two Locus (dim=2).
     */
    private int value[];

    /**
     * Number of alleles. This is the number of possible values for the current
     * locus.
     */
    public int n_allele;

    /** Number of values for a given loci. */
    private final int N = 2;

    /**
     * Locus constructor.
     * @param rank
     */
    public Locus(int rank) {

        super(rank);
        value = new int[2];

    }

    public void init() {

        n_allele = this.getConfiguration().getInt("genet.nallele");

    }

    /**
     * Random draft of an allele among the n_allele possible.
     */
    public void init_random_draft() {
        for (int k = 0; k < N; k++) {
            double i = Math.random() * this.n_allele;
            int n = (int) i;
            value[k] = n;
        }
    }
    
    /** Set locus values from the parents locus (uniform draft of the 
     * parent's locus). 
     * @param parent_a
     * @param parent_b 
     */
    public void set_from_parents(Locus parent_a, Locus parent_b) {
        
        int i = (int) (2 * Math.random());
        value[0] = parent_a.getValue()[i];
                        
        i = (int) (2 * Math.random());
        value[1] = parent_b.getValue()[i];
        
    }
    
    public int[] getValue() {
        return this.value;
    }
    
    public int sum() {
        return (value[0] + value[1]);
    }
            
    

}
