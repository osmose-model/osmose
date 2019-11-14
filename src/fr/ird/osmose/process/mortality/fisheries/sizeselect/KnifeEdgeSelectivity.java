package fr.ird.osmose.process.mortality.fisheries.sizeselect;

import fr.ird.osmose.process.mortality.fisheries.SingleFisheriesMortality;
import fr.ird.osmose.process.mortality.fisheries.SizeSelectivity;

/**
 *
 * @todo Eventually Move the selectivity into Interface, with three different
 * classes (Step, Gaussian and Sigmo)
 * @author nbarrier
 */
public class KnifeEdgeSelectivity extends SizeSelectivity {

    /**
     * Public constructor. Initialize the FisheriesMortality pointer.
     *
     * @param fmort
     */
    public KnifeEdgeSelectivity(SingleFisheriesMortality fmort) {
        super(fmort);
    }

    /**
     * Initializes the selectivity class. For step, only L50 is used. Hence
     * no more init is used.
     * 
     */
    @Override
    public void init() {

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

        double output = size < l50 ? 0 : 1;
        return output;
    }
}
