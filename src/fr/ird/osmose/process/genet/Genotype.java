/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.genet;

import fr.ird.osmose.Species;
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
    private int ntraits;
    private int[] nlocus;
    private final int spec_index;

    /** Initialize a genotype for a given School.
     * 
     * @param rank
     * @param species 
     */
    public Genotype(int rank, Species species) {

        super(rank);
        genotype = new ArrayList<>();
        this.spec_index = species.getIndex();
    
    }
       
    public void init() {
        
        ntraits = this.getNEvolvingTraits();
        traits = new double[ntraits];
        nlocus = new int[ntraits];

        // Loop over the traits that may vary due to genetics
        for (int i = 0; i < ntraits; i++) {

            // Recover the trait from configuration list (assumes)
            Trait trait = this.getEvolvingTrait(i);

            nlocus[i] = trait.getNLocus(spec_index);

            // Init the list of locus for the given trait
            List<Locus> list_locus = new ArrayList<>();
            for (int j = 0; j < nlocus[i]; j++) {
                Locus l = new Locus(j, trait, spec_index);
                list_locus.add(l);
            }  // end of locus loop

            genotype.add(list_locus);

        }  // end of trait loop
    }  // end of constructor

    /**
     * Init, for each trait, all the locus by using random draft within Normal
     * distribution. When the initialisation of locus is done, update the trait
     * values
     */
    public void init_genotype() {
        
        for (int i = 0; i < ntraits; i++) {
            // recover the list of locus associated with the given trait
            for (int j = 0; j < nlocus[i]; j++) {
                // initialize each locus using a random draft
                this.getLocus(i, j).init_random_draft();
            }
        }  // end of trait loop
        this.update_traits();
    }  // end of method

    /**
     * Updates, for each trait, the locus values by using the genotypes of both
     * parents. When the initialisation of locus is done, update the trait
     * values
     *
     * @param parent_a
     * @param parent_b
     */
    public void transmit_genotype(Genotype parent_a, Genotype parent_b) {

        for (int i = 0; i < ntraits; i++) {
            for (int j = 0; j < nlocus[i]; j++) {
                // initialize each locus using a random draft
                this.getLocus(i, j).set_from_parents(parent_a.getLocus(i, j), parent_b.getLocus(i, j));
            }
        }  // end of trait loop
        this.update_traits();
    }  // end of method

    /**
     * Returns the locus for a given trait and locus index.
     *
     * @param itrait
     * @param ilocus
     * @return
     */
    public Locus getLocus(int itrait, int ilocus) {
        return genotype.get(itrait).get(ilocus);
    }

    /**
     * Returns the list of locus that are associated with a given trait.
     *
     * @param itrait
     * @return
     */
    public List<Locus> getLocusList(int itrait) {
        return genotype.get(itrait);
    }

    /**
     * Return the index of a trait providing it's name.
     *
     * @param name Name of the trait
     * @return The index of the trait
     * @throws java.lang.Exception
     */
    public int getTraitIndex(String name) throws Exception {

        for (int i = 0; i < ntraits; i++) {
            if (name.toLowerCase().compareTo(this.getSimulation().getEvolvingTrait(i).getName().toLowerCase()) == 0) {
                return i;
            }
        }

        // If the trait has not been found, return an exception.
        throw new Exception("The trait " + name + " cannot be found");

    }

    /**
     * Returns the value of the trait for a given genotype.
     *
     * @param name Name of the trait
     * @return Value of the trait for the given genotype
     * @throws java.lang.Exception
     */
    public double getTrait(String name) throws Exception {

        int index = this.getTraitIndex(name);
        return traits[index];

    }

    /**
     * Get the value of a trait provided a list of locus.
     *
     */
    public void update_traits() {

        for (int i = 0; i < ntraits; i++) {

            Trait trait = this.getSimulation().getEvolvingTrait(i);

            List<Locus> list_locus = this.getLocusList(i);

            double x = 0;

            // Computation of (V1 + V1') + (V2 + V2') + (...) (equation  of Alaia's document)
            //  Equation 34 from Alaia's document
            for (Locus l : list_locus) {
                x += l.sum();
            }

            // Multiplication by U + adding xmin
            //this.x *= u;
            x += trait.getMean(i);
            traits[i] = x;
            
            // adding noise to 
            traits[i] += trait.addTraitNoise(spec_index);
        }
    }
    
        /**
     * Return the index of a trait providing it's name.
     *
     * @param name Name of the trait
     * @return The index of the trait
     * @throws java.lang.Exception
     */
    public boolean existsTrait(String name) {

        for (int i = 0; i < ntraits; i++) {
            if (name.toLowerCase().compareTo(this.getSimulation().getEvolvingTrait(i).getName().toLowerCase()) == 0) {
                return true;
            }
        }

        return false;

    }
    

}  // end of class
