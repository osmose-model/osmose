
package fr.ird.osmose.process.mortality.fisheries;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.process.mortality.FisheriesMortality;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.logging.OLogger;

/**
 *
 * @author nbarrier
 */
public class SizeSelectivity extends OsmoseLinker {
    
    /** Type of the selectivity curve. */
    private SizeSelectivity.Curve curve;
    
    /**
     * Type of the selectivity curve.
     */
    private SizeSelectivity.Variable variable;
    
    /** L50 size. Used in all three types of selectivities. */
    private double l50;
    
    /** Exponential factors. Used only in SIGMO and GAUSS selectivities. */
    private double b;
    
    /** Multiplication factor. Used only for SIGMO selectivities. */
    private double a;
    
    /** Pointer to the fisheries mortality array. 
     * Allows to recover the fisheries index and the MPI rank.
     */
    private final SingleFisheriesMortality mort;
    
    /**
     * Public constructor. Initialize the FisheriesMortality pointer.
     *
     * @param fmort
     */
    public SizeSelectivity(SingleFisheriesMortality fmort) {
        this.mort = fmort;
    }
    
    /** Initializes the selectivity class. The selectivity parameters
     * are initialized from the configuration file.
     * The number of parameters depends on the selectivity curve.
     */
    public void init() {
        int index = mort.getFIndex();
        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        String var = cfg.getString("fisheries.select.var.fis" + index);
        if (var.equals("age")) {
            this.variable = SizeSelectivity.Variable.AGE;
        } else {
            this.variable = SizeSelectivity.Variable.LEN;
        }

        String type = cfg.getString("fisheries.select.curve.fis" + index);
        if (type.equals("step")) {
            this.curve = SizeSelectivity.Curve.STEP;
            this.l50 = cfg.getFloat("fisheries.select.l50.fis" + index);
        } else if (type.equals("gauss")) {
            this.curve = SizeSelectivity.Curve.GAUSS;
            this.l50 = cfg.getFloat("fisheries.select.l50.fis" + index);
            this.b = cfg.getFloat("fisheries.select.b.fis" + index);
        } else if (type.equals("sigmo")) {
            this.curve = SizeSelectivity.Curve.GAUSS;
            this.l50 = cfg.getFloat("fisheries.select.l50.fis" + index);
            this.b = cfg.getFloat("fisheries.select.b.fis" + index);
            this.a = cfg.getFloat("fisheries.select.a.fis" + index);
        }
        else {
            error("Selectivity curve " + type + "is not implemented. Choose 'step', 'gauss' or 'sigmo'.", new Exception());
        }
    }
    
    /**
     * Returns a selectivity value. It depends on the size of the specieand
     * on the selectivity curve and parameters. Output value is 
     * between 0 and 1.
     * @param size Specie size
     * @return A selectivity value (0<output<1)
     */
    public double getSelectivity(double size) {
        double output = 0.0;
        switch (this.curve) {
            case STEP:
                // Returns the step function
                if (size < this.l50) {
                    output = 0;
                } else {
                    output = 1;
                }
                break;
            case GAUSS:
                // Sel = exp (-b*(x-l50)**2)
                output = Math.exp(-this.b * Math.pow(size - this.l50, 2));
                break;
            case SIGMO:
                // Sel = 1 / (1 + a*exp(-b(x-l50)))
                output = 1 / (1 + this.a * Math.exp(-this.b * (size - this.l50)));
                break;
            default:
                error("Selectivity curve is not implemented.", new Exception());
                break;
        }

        return output;

    }
   
    public Curve getCurve() {
        return this.curve;
    }

    public Variable getVariable() {
        return this.variable;
    }
    
    public enum Curve
    {
        STEP,
        GAUSS,
        SIGMO;
    }
    
    public enum  Variable {
        LEN,
        AGE,
    }
    
}
