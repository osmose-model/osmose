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
     * Number of locus that code the traits.
     */
    private int n_locus;
            
    /**
     * Number of possible values that the locus can take.
     */
    private int n_val;

    /** Mean and variance of trait. */
    private double xmean, xvar;
    
    /** Name of the trait. */
    private final String prefix;
    
    /** Diversity matrix, used to initialize genotypes. */
    private double[][] diversity;

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
        
        // look for the mean value of the trait
        key = String.format("%s.trait.mean", prefix);
        xmean = this.getConfiguration().getDouble(key);

        // look for the variance (sigma^2) of the trait
        key = String.format("%s.trait.var", prefix);
        xvar = this.getConfiguration().getDouble(key);

        // number of locus that code the trait
        key = String.format("%s.trait.nlocus", prefix);
        n_locus = this.getConfiguration().getInt(key);
        
        // number of values that the locus can take
        key = String.format("%s.trait.nval", prefix);
        n_val = this.getConfiguration().getInt(key);
        
        diversity = new double[n_locus][n_val];
        // Convert the trait variance to "Loci" standard deviation.
        // variance is xvar / (2 * Lc)
        // hence standard deviation is sqrt(xvar / (2 * Lc))
        double stddev = Math.sqrt(xvar / (2 * n_locus));
        
        // initialisation of the "diversity" matrix, which is
        // the array of possible values for each of the locis
        // that code the trait
        Random gaussian_gen = new Random();
        for (int i = 0; i < n_locus; i++) {
            for (int k = 0; k < n_val; k++) {  // k = 0, 1
                diversity[i][k] = gaussian_gen.nextGaussian() * stddev;
            }
        }
    }

   
    /** Get the value of a trait provided a list of locus.
     * @param list_locus */
    public double getTrait(List<Locus> list_locus) {
        
        double x = 0;
        
        // Computation of (V1 + V1') + (V2 + V2') + (...) (equation  of Alaia's document)
        //  Equation 34 from Alaia's document
        for (Locus l : list_locus) {
            x += l.sum();
        }

        // Multiplication by U + adding xmin
        //this.x *= u;
        x += this.xmean;
        
        return x;

    }
    
    public int getNLocus() {
        return this.n_locus;
    }
    
    public int getNValues() { 
        return this.n_val;
    }

    public double getVar() { 
        return this.xvar;
    }
    
    public double getDiv(int loc_index, int val_index) { 
        return this.diversity[loc_index][val_index];
    }
}
