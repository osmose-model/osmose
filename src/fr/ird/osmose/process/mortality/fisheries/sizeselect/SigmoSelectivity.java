
package fr.ird.osmose.process.mortality.fisheries.sizeselect;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.process.mortality.FisheriesMortality;
import fr.ird.osmose.process.mortality.fisheries.SingleFisheriesMortality;
import fr.ird.osmose.process.mortality.fisheries.SizeSelectivity;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.logging.OLogger;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * 
 * @todo Eventually Move the selectivity into Interface, with three different classes (Step, Gaussian and Sigmo) 
 * @author nbarrier
 */
public class SigmoSelectivity extends SizeSelectivity {
    
    /** L75 size. Used in all three types of selectivities. */
    private double l75 = -999;
    
    /** Exponential factors. Used only in SIGMO and GAUSS selectivities. */
    private double b;
    
    /** Multiplication factor. Used only for SIGMO selectivities. */
    private double a;
    
    private double s1;
    private double s2;
    
    /**
     * Public constructor. Initialize the FisheriesMortality pointer.
     *
     * @param fmort
     */
    public SigmoSelectivity(SingleFisheriesMortality fmort) {
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
        if (cfg.canFind("fisheries.select.l75.fis + index")) {
            this.l75 = cfg.getFloat("fisheries.select.l75.fis" + index);
            this.s1 = (this.l50 * Math.log(3)) / (this.l75 - this.l50);
            this.s2 = this.s1 / this.l50;
        } else {
            this.b = cfg.getFloat("fisheries.select.b.fis" + index);
            this.a = cfg.getFloat("fisheries.select.a.fis" + index);
        }
    }
    
    /**
     * Returns a selectivity value. It depends on the size of the specieand
     * on the selectivity curve and parameters. Output value is 
     * between 0 and 1.
     * @param size Specie size
     * @return A selectivity value (0<output<1)
     */
    @Override
    public double getSelectivity(double size) {
        double output;
        // If Ricardo formulation should be used.
        if (this.l75 > 0) {
            output = 1 / (1 + Math.exp(this.s1 - (this.s2 * size)));
        } else {
            // Sel = 1 / (1 + a*exp(-b(x-l50)))
            output = 1 / (1 + this.a * Math.exp(-this.b * (size - this.l50)));
        }

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }
}
