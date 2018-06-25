
package fr.ird.osmose.process.mortality.fisheries;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.process.mortality.FisheriesMortality;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.logging.OLogger;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * 
 * @todo Eventually Move the selectivity into Interface, with three different classes (Step, Gaussian and Sigmo) 
 * @author nbarrier
 */
public abstract class SizeSelectivity extends OsmoseLinker {
    
    /**
     * Type of the selectivity variable (age or size).
     */
    protected SizeSelectivity.Variable variable;
    
    /** L50 size. Used in all three types of selectivities. */
    protected double l50;
    
     /** Tiny size. Size below which no fish is captured. */
    protected double tiny = 1.e-6;
    
    /** Pointer to the fisheries mortality array. 
     * Allows to recover the fisheries index and the MPI rank.
     */
    protected final SingleFisheriesMortality mort;
    
    /**
     * Public constructor. Initialize the FisheriesMortality pointer.
     *
     * @param fmort
     */
    public SizeSelectivity(SingleFisheriesMortality fmort) {
        this.mort = fmort;
        this.init_var();
    }
    
    /** Initializes the selectivity class. The selectivity parameters
     * are initialized from the configuration file.
     * The number of parameters depends on the selectivity curve.
     */
    public final void init_var() {
        
        int index = mort.getFIndex();
        
        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        // Initialize the selectivity variable 
        String var = cfg.getString("fisheries.select.var.fis" + index);
        if (var.equals("age")) {
            this.variable = SizeSelectivity.Variable.AGE;
        } else {
            this.variable = SizeSelectivity.Variable.LEN;
        }
        
        // Init the l50 variable
        this.l50 = cfg.getFloat("fisheries.select.l50.fis" + index);

        // if tiny parameter exists, set tiny. Else, use default
        if(cfg.canFind("fisheries.select.tiny.fis" + index)) {
            this.tiny  = cfg.getFloat("fisheries.select.tiny.fis" + index);
        }

    }
    
    /**
     * Returns a selectivity value. It depends on the size of the specieand
     * on the selectivity curve and parameters. Output value is 
     * between 0 and 1.
     * @param size Specie size
     * @return A selectivity value (0<output<1)
     */
    public abstract double getSelectivity(double size);
    
    
    /** Abstract init method. */
    public abstract void init();
   
    /** Returns the selectivity variable. */
    public Variable getVariable() {
        return this.variable;
    }
    
    public enum  Variable {
        LEN,
        AGE,
    }
    
}
