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
    private double traitsEnvNoise[];

    /**
     * Initialize a genotype for a given School.
     *
     * @param rank
     * @param species
     */
    public Genotype(int rank, Species species) {

        super(rank);
        genotype = new ArrayList<>();
        this.spec_index = species.getSpeciesIndex();

    }

    public void init() {

        ntraits = this.getNEvolvingTraits();
        traits = new double[ntraits];
        traitsEnvNoise = new double[ntraits];
        nlocus = new int[ntraits];

        // Loop over the traits that may vary due to genetics
        for (int i = 0; i < ntraits; i++) {

            // Recover the trait from configuration list (assumes)
            Trait trait = this.getEvolvingTrait(i);

            nlocus[i] = trait.getNLocus(spec_index);

            // Init the list of locus for the given trait
            List<Locus> list_locus = new ArrayList<>();
            for (int j = 0; j < nlocus[i]; j++) {
                Locus l = new Locus(j, trait, spec_index, getRank());
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
        } // end of trait loop
        this.update_traits();
    } // end of method

    /**
     * Restart the value of the trait based on the restarts
     *
     */
    public void restartTrait(int i) {

        Trait trait = this.getSimulation().getEvolvingTrait(i);

        List<Locus> list_locus = this.getLocusList(i);

        double x = 0;

        // Computation of (V1 + V1') + (V2 + V2') + (...) (equation of Alaia's document)
        // Equation 34 from Alaia's document
        for (Locus l : list_locus) {
            x += l.sum();
        }

        // Multiplication by U + adding xmin
        // this.x *= u;
        x += trait.getMean(spec_index);
        traits[i] = x;

    }

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
     * @param name
     *            Name of the trait
     * @return Value of the trait for the given genotype
     * @throws java.lang.Exception
     */
    public double getTrait(String name) throws Exception {

        int index = this.getTraitIndex(name);
        return (traits[index] + this.traitsEnvNoise[index]);

    }

    public double getgenet_value(String name) throws Exception {
        int index = this.getTraitIndex(name);
        return (traits[index]);

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
            x += trait.getMean(spec_index);
            traits[i] = x;

            this.traitsEnvNoise[i] = trait.addTraitNoise(spec_index);
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

    public int getNLocus(int itrait) {
        return this.nlocus[itrait];
    }

    /**
     * Forces the value of a Loci pair. Used when restart with genetic.
     *
     * @param itrait Index of the variable trait
     * @param iloc Index of the loci
     * @param val0 First value of the loci
     * @param val1 Second value of the loci
     */
    public void setLocusVal(int itrait, int iloc, double val0, double val1) {
        this.getLocus(itrait, iloc).setValue(0, val0);
        this.getLocus(itrait, iloc).setValue(1, val1);
    }

    /**
     * Forces the value of a Loci pair. Used when restart with genetic.
     *
     * @param itrait Index of the variable trait
     * @param value Value fof the environmental noise
     */
    public void setEnvNoise(int itrait, double value) {
        this.traitsEnvNoise[itrait] = value;
    }

    public double getGeneticTrait(int index) throws Exception {
        return traits[index];
    }

    /**
     * Forces the value of a Loci pair. Used when restart with genetic.
     *
     * @param itrait Index of the variable trait
     */
    public double getEnvNoise(int itrait) {
        return this.traitsEnvNoise[itrait];
    }

}  // end of class
