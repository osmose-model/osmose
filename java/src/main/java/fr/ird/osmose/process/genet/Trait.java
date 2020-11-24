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

import fr.ird.osmose.util.SimulationLinker;
import java.util.Random;

/**
 *
 * @author nbarrier
 */
public class Trait extends SimulationLinker {

    /**
     * Number of locus that code the traits. One value for each species
     */
    private int[] n_locus;
    
    /** Variance of the trait expressed due to env. One value per species. */
    private double[] env_var;

    /**
     * Number of possible values that the locus can take. One value for each
     * species
     */
    private int[] n_val;

    /**
     * Mean and variance of trait. One value for each species
     */
    private double[] xmean, xvar;

    /**
     * Name of the trait.
     */
    private final String prefix;

    /**
     * Diversity matrix, used to initialize genotypes. One matrix for each
     * species
     */
    private double[][][] diversity;

    /**
     * Locus constructor.
     *
     * @param rank
     * @param prefix
     */
    public Trait(int rank, String prefix) {

        super(rank);
        // Trait eyecolor = new Trait(rank, "eyecol")
        this.prefix = prefix;

    }

    public void init() {

        String key;

        int nspecies = this.getNSpecies();

        // look for the mean value of the trait
        key = String.format("%s.trait.mean", prefix);
        xmean = this.getConfiguration().getArrayDouble(key);
        if (xmean.length != nspecies) {
            String errorMsg = String.format("The xmean value for trait %s should have a size of %d. Size %d provided", prefix, nspecies, xmean.length);
            error(errorMsg, new Exception());
        }
        
        key = String.format("%s.trait.envvar", prefix);
        env_var = this.getConfiguration().getArrayDouble(key);
        if (env_var.length != nspecies) {
            String errorMsg = String.format("The env_var value for trait %s should have a size of %d. Size %d provided", prefix, nspecies, xvar.length);
            error(errorMsg, new Exception());
        }

        // look for the variance (sigma^2) of the trait
        key = String.format("%s.trait.var", prefix);
        xvar = this.getConfiguration().getArrayDouble(key);
        if (xvar.length != nspecies) {
            String errorMsg = String.format("The xvar value for trait %s should have a size of %d. Size %d provided", prefix, nspecies, xvar.length);
            error(errorMsg, new Exception());
        }

        // number of locus that code the trait
        key = String.format("%s.trait.nlocus", prefix);
        n_locus = this.getConfiguration().getArrayInt(key);
        if (n_locus.length != nspecies) {
            String errorMsg = String.format("The n_locus value for trait %s should have a size of %d. Size %d provided", prefix, nspecies, n_locus.length);
            error(errorMsg, new Exception());
        }

        // number of values that the locus can take
        key = String.format("%s.trait.nval", prefix);
        n_val = this.getConfiguration().getArrayInt(key);
        if (n_val.length != nspecies) {
            String errorMsg = String.format("The n_val value for trait %s should have a size of %d. Size %d provided", prefix, nspecies, n_val.length);
            error(errorMsg, new Exception());
        }

        diversity = new double[nspecies][][];

        for (int ispec = 0; ispec < nspecies; ispec++) {

            diversity[ispec] = new double[n_locus[ispec]][n_val[ispec]];
            // Convert the trait variance to "Loci" standard deviation.
            // variance is xvar / (2 * Lc)
            // hence standard deviation is sqrt(xvar / (2 * Lc))
            double stddev = Math.sqrt(xvar[ispec] / (2 * n_locus[ispec]));

            // initialisation of the "diversity" matrix, which is
            // the array of possible values for each of the locis
            // that code the trait
            Random gaussian_gen = new Random();
            for (int i = 0; i < n_locus[ispec]; i++) {
                for (int k = 0; k < n_val[ispec]; k++) {  // k = 0, 1
                    diversity[ispec][i][k] = gaussian_gen.nextGaussian() * stddev;
                }
            }
        }  // end of species loop
    }

    /**
     * Returns the number of locus that codes the trait.
     *
     * @param index Species index
     * @return
     */
    public int getNLocus(int index) {
        return this.n_locus[index];
    }

    /**
     * Returns the number of values a locus can take for the trait.
     *
     * @param index Species index
     * @return
     */
    public int getNValues(int index) {
        return this.n_val[index];
    }

    /**
     * Returns the variance of the trait.
     *
     * @param index Species index
     * @return
     */
    public double getVar(int index) {
        return this.xvar[index];
    }

    /**
     * Returns the diversity matrix of the trait.
     *
     * @param spec_index Species index
     * @param loc_index Locus index (L_c)
     * @param val_index Value index (V)
     * @return
     */
    public double getDiv(int spec_index, int loc_index, int val_index) {
        return this.diversity[spec_index][loc_index][val_index];
    }

    /**
     * Returns the mean value of the trait.
     *
     * @param spec_index
     * @return
     */
    public double getMean(int spec_index) {
        return this.xmean[spec_index];
    }

    /** Get the name of the variable trait. 
     * 
     * @return Name of the trait. 
     */
    public String getName() {
        return this.prefix;
    }
    
    /**
     * Add some environmental noise to the trait "expression". Trait expressed is due 
     * to genotype + some noise (sigma_e^2). Species dependent.
     * @param index
     * @return
     */
    public double addTraitNoise(int index) {
        Random random = new Random();
        double std = Math.sqrt(this.env_var[index]);
        double val = random.nextGaussian() * std;
        return val;
    }
    
}
