/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.genet;

import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.SimulationLinker;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author nbarrier
 */
public class Trait extends OsmoseLinker {

    /**
     * Number of locus that code the traits. One value for each species
     */
    private int[] n_locus;

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
    public Trait(String prefix) {

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
}
