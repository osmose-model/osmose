
package fr.ird.osmose.process.mortality.fisheries.sizeselect;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.process.mortality.fisheries.SingleFisheriesMortality;
import fr.ird.osmose.process.mortality.fisheries.SizeSelectivity;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * 
 * @todo Eventually Move the selectivity into Interface, with three different classes (Step, Gaussian and Sigmo) 
 * @author nbarrier
 */
public class GaussSelectivity extends SizeSelectivity {
    
    /** L75 size. If < 0, consider that the old formulation is used */
    private double l75 = -999;
       
    /** Exponential factors. Used only in GAUSS selectivities. */
    private double b;
    
    /** Maximum value. Use only in the case of guaussian distribution for normalisation purposes */
    private NormalDistribution distrib;
    
    /**
     * Public constructor. Initialize the FisheriesMortality pointer.
     *
     * @param fmort
     */
    public GaussSelectivity(SingleFisheriesMortality fmort) {
        super(fmort);
    }
    
    /** Initializes the selectivity class. The selectivity parameters
     * are initialized from the configuration file.
     * The number of parameters depends on the selectivity curve.
     */
    @Override
    public void init() {
        
        int index = mort.getFIndex();
        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        // If L75 is found, Ricardo formulae is used
        if (cfg.canFind("fishery.selectivity.l75.fsh" + index)) {
            this.l75 = cfg.getFloat("fishery.selectivity.l75.fsh" + index);
            // Normal distribution for init qnorm(0.75)
            NormalDistribution norm = new NormalDistribution();
            double sd = (this.l75 - this.l50) / norm.inverseCumulativeProbability(0.75);  // this is the qnorm function
            // initialisation of the distribution used in selectity calculation
            this.distrib = new NormalDistribution(this.l50, sd);
        } else {
            this.b = cfg.getFloat("fishery.selectivity.b.fsh" + index);
        }
    }

    /**
     * Returns a selectivity value. It depends on the size of the specieand on
     * the selectivity curve and parameters. Output value is between 0 and 1.
     *
     * @param size Specie size
     * @return A selectivity value (0<output<1)
     */
    @Override
    public double getSelectivity(double size) {
        
        double output; 
        // calculation of selectivity. Normalisation by the maximum value 
        // (i.e. the value computed with x = mean).
        if (this.l75 > 0) {
            // If L75 > 0, assumes Ricardo Formulation should be used
            output = this.distrib.density(size) / this.distrib.density(this.l50);
        } else {
            output = Math.exp(-this.b * Math.pow(size - this.l50, 2));
        }

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }
}
