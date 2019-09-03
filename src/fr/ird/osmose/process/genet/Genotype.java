/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.genet;

import fr.ird.osmose.util.SimulationLinker;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that manages the genotype for a given individual.
 *
 * @author nbarrier
 */
public class Genotype extends SimulationLinker {

    /**
     * Complete genotype for an individual. It is a list (one element per
     * varying trait) of locus list (the number of locus that code a trait may
     * vary).
     */
    private final List<List<Locus>> genotype;
    private double traits[];
    private final int ntraits;
    private final int[] nlocus;

    public Genotype(int rank) {

        super(rank);
        genotype = new ArrayList<>();

        ntraits = this.getConfiguration().getNEvolvingTraits();
        traits = new double[ntraits];
        nlocus = new int[ntraits];

        // Loop over the traits that may vary due to genetics
        for (int i = 0; i < ntraits; i++) {

            // Recover the trait from configuration list (assumes)
            Trait trait = this.getConfiguration().getEvolvingTrait(i);

            nlocus[i] = trait.getNLocus();

            // Init the list of locus for the given trait
            List<Locus> list_locus = new ArrayList<>();
            for (int j = 0; j < nlocus[i]; j++) {
                Locus l = new Locus(this.getRank(), j, trait);
                list_locus.add(l);
            }  // end of locus loop

            genotype.add(list_locus);

        }  // end of trait loop
    }  // end of constructor

    /**
     * Init genotype using random methods.
     */
    public void init_genotype() {

        for (int i = 0; i < ntraits; i++) {
            // recover the list of locus associated with the given trait
            for (int j = 0; j < nlocus[i]; j++) {
                // initialize each locus using a random draft
                this.getLocus(i, j).init_random_draft();
            }
        }  // end of trait loop
    }  // end of method

    public void transmit_genotype(Genotype parent_a, Genotype parent_b) {

        for (int i = 0; i < ntraits; i++) {
            for (int j = 0; j < nlocus[i]; j++) {
                // initialize each locus using a random draft
                this.getLocus(i, j).set_from_parents(parent_a.getLocus(i, j), parent_b.getLocus(i, j));
            }
        }  // end of trait loop
    }  // end of method

    public void update_traits() {
        for (int i = 0; i < ntraits; i++) {
            traits[i] = this.getConfiguration().getEvolvingTrait(i).getTrait(getLocusList(i));
        }
    }

    public Locus getLocus(int itrait, int ilocus) {
        return genotype.get(itrait).get(ilocus);
    }

    public List<Locus> getLocusList(int itrait) {
        return genotype.get(itrait);
    }

}  // end of class
