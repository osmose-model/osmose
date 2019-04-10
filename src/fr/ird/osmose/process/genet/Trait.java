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
 *
 * @author nbarrier
 */
public class Trait extends SimulationLinker {

    /**
     * Number of alleles. This is the number of possible values for the current
     * locus.
     */
    public int n_locus;

    /**
     * List of locus that code the given trait.
     */
    List<Locus> list_locus;

    /**
     * Minimum and maximum values of the trait.
     */
    public double xmin, xmax;

    /**
     * Value of the trait.
     */
    public double x;

    private double u;

    String prefix;

    /**
     * Locus constructor.
     *
     * @param rank
     */
    public Trait(int rank, String prefix) {

        // Trait eyecolor = new Trait(rank, "eyecol")
        super(rank);
        this.prefix = prefix;

    }

    public void init() {

        n_locus = this.getConfiguration().getInt("genet.nlocus");
        list_locus = new ArrayList(n_locus);

        // look for param "eyecol.xmin"
        String key = String.format("%s.xmin", prefix);
        xmin = this.getConfiguration().getDouble(key);

        // look for param "eyecol.xmax"
        key = String.format("%s.xmax", prefix);
        xmax = this.getConfiguration().getDouble(key);

        if (xmin > xmax) {
            // Flip xmin and xmax
            warning("Xmin and Xmax have been flipped");
            double xtmp = xmax;
            xmax = xmin;
            xmin = xtmp;
        }

        int n_allele = this.getConfiguration().getInt("genet.nallele");
        u = (this.xmax - this.xmin) / ((n_allele - 1) * n_locus * 2);

        for (Locus l : list_locus) {
            l.init();
        }

    }

    public void random_draft() {

        for (Locus l : list_locus) {
            l.init_random_draft();
        }

        this.setTrait();

    }

    public void set_from_parents(Trait parentA, Trait parentB) {

        for (int i = 0; i < list_locus.size(); i++) {
            this.list_locus.get(i).set_from_parents(parentA.getLocus(i), parentB.getLocus(i));
        }

        this.setTrait();

    }

    public Locus getLocus(int i) {
        return list_locus.get(i);
    }

    public void setTrait() {
        
        this.x = 0;
        
        // Computation of (V1 + V1') + (V2 + V2') + (...)
        for (Locus l : list_locus) {
            this.x += l.sum();
        }

        // Multiplication by U + adding xmin
        this.x *= u;
        this.x += xmin;

    }

    public double getTrait() {
        return this.x;
    }

}
