
package fr.ird.osmose.process.mortality.fisheries;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.OsmoseLinker;

/**
 *
 *
 * @author nbarrier
 */
public class TimeVariability extends OsmoseLinker {

    /**
     * Pointer to the fisheries mortality array. Allows to recover the fisheries
     * index and the MPI rank.
     */
    private final SingleFisheriesMortality mort;

    private TimeVariability.FishBase fishBase;
    
    private double[] timeArr;
    
    /** Number of time steps per year. */
    private int ndt;
    
    /** Number of years. */
    private int nyear;
    
    /**
     * Public constructor. Initialize the FisheriesMortality pointer.
     * @param fmort
     */
    public TimeVariability(SingleFisheriesMortality fmort) {
        this.mort = fmort;
    }

    /** Initialize the time varying index. */
    public void init() {
        int index = mort.getFIndex(); 
        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        this.ndt = cfg.getNStepYear();
        this.nyear = cfg.getNStep() / ndt;
        this.timeArr = new double[nyear * ndt];
        
        String type = cfg.getString("fisheries.rate.method.fis" + index);
        switch (type) {
            case "constant":
                this.fishBase = FishBase.CONS;
                this.getFBaseConstant();
                break;
            case "byregime":
                this.fishBase = FishBase.BYREGIME;
                this.getFBaseRegime();
                break;
            case "linear":
                this.fishBase = FishBase.LINEAR;
                this.getFBaseLinear();
                break;
            case "byyear":
                this.fishBase = FishBase.BYYEAR;
                this.getFBaseByYear();
                break;
            case "bydt":
                this.fishBase = FishBase.BYDT;
                 this.getFBaseByDt();
                break;
            default:
                error("Fishing base " + type + "is not implemented.", new Exception());
        }
    }
    
     /**
     * Initialize the time array from dt values. 
     * Here, the rate array is repeated cyclically. 
     * For instance, if rate = [1, 3 , 7, 8, 20] and
     * there is 8 values to fill, the timeArr array will be
     * [1, 3, 7, 8, 20, 1, 3, 7]
     *
     */
    private void getFBaseByDt() {
        Configuration cfg = Osmose.getInstance().getConfiguration();
        double[] value = cfg.getArrayDouble("fisheries.rate.bydt.rate.fis" + mort.getFIndex());
        for (int i=0; i< ndt * nyear; i++)
        {
            int k = i % value.length;
            this.timeArr[i] = value[k];
        }
        
    }
    
    
    /**
     * Initialize the time array from yearly values. If there is 2 years to fill
     * with 12 time steps, and peryear=4, the rate array should have 4 values.
     * If rate = [a, b, c, d], then 
     * timeArr = [
     * a, a, a, b, b, b, c, c, c, d, d, d, (year 1)
     * a, a, a, b, b, b, c, c, c, d, d, d, (year 2)
     *           ] 
     */
    private void getFBaseByYear() {
        Configuration cfg = Osmose.getInstance().getConfiguration();
        double[] value = cfg.getArrayDouble("fisheries.rate.byyear.rate.fis" + mort.getFIndex());

        int freq = this.getFishingFrequency();    // Number of fishing values to provide per year. Equals ndt / peryear
        if (freq != value.length) {
            error("The number of rate values must be " + freq, new Exception());
        }

        // Converts the yearly rates into seasonal rates
        int nStepYear = getConfiguration().getNStepYear();
        
        int cpt = 0;
        for (int i = 0; i < nyear; i++) {
            for (int j = 0; j < ndt; j++) {
                timeArr[cpt] = value[j / freq] / nStepYear;   // converts yearly rates into seasonal rates
                cpt++;
            }
        }
    }

    /** Initialize the time array from constant yearly mortality rate. */
    private void getFBaseConstant() {
        Configuration cfg = Osmose.getInstance().getConfiguration();
        double value = cfg.getDouble("fisheries.rate.const.rate.fis" + mort.getFIndex());
        
        int nStepYear = getConfiguration().getNStepYear();

        for (int i = 0; i < ndt * nyear; i++) {
            timeArr[i] = value / nStepYear;   // convert from yearly mortality rate to seasonal mortality rate
        }
    }
    
    /**
     * Initialize the time array from linear value.
     */
    private void getFBaseLinear() {
        Configuration cfg = Osmose.getInstance().getConfiguration();
        double rate = cfg.getDouble("fisheries.rate.linear.rate.fis" + mort.getFIndex());
        double slope = cfg.getDouble("fisheries.rate.linear.slope.fis" + mort.getFIndex());

        int freq = this.getFishingFrequency();
       
        for (int i = 0; i < ndt * nyear; i++) {   
            int x = i / freq;
            float time = (x * freq) / ((float) ndt);
            timeArr[i] = rate * (1 + slope * time);
        }

    }

    /**
     * Initialize the time array from regime shift values.
     */
    private void getFBaseRegime() {
        Configuration cfg = Osmose.getInstance().getConfiguration();
        int index = mort.getFIndex();
        
        /** Recover the shift array from file, and remove values for which 
         * shift > T * ndt;
         */
        int tempshifts[] = cfg.getArrayInt("fisheries.rate.regime.shifts.fis" + index);
        
        // Count the number of good shift values
        int nRegimes = 0;
        for (int i = 0; i < tempshifts.length; i++) {
            if (tempshifts[i] < ndt * nyear) {
                nRegimes++;
            }
        }
        
        // Initialize the shift values 
        int[] shifts = new int[nRegimes];
        int cpt = 0;
        for (int i = 0; i < tempshifts.length; i++) {
            if (tempshifts[i] < ndt * nyear) {
                shifts[cpt] = tempshifts[i];
                cpt++;
            }
        }
        
        double[] rates = cfg.getArrayDouble("fisheries.rate.regime.rates.fis" + index);
        if (rates.length != nRegimes) {
            error("You must provide " + nRegimes + " fishing rates.", new Exception());
        }

        nRegimes++;

        int irate = 0;   // current index in the rate array.
        int ishift = 0;  // current index in the shift array.
        int sh = shifts[ishift];   // sets the current shift array
        for (int i = 0; i < ndt * nyear; i++) {
           
            // if the current array index is greater than shift,
            // we update the ishift and irate array.
            if (i >= sh) {
                ishift++;
                irate++;

                // if the shift index is greater than bound array
                // the last shift value is set as equal to nyear*ndt
                sh = (ishift < shifts.length) ? shifts[ishift] : nyear * ndt;

            }
            
            timeArr[i] = rates[irate];
            
        }
            
    }

    /**
     * Returns the number of time steps with constant fishing data.
     * If ndt=12 and periodperyear = 4, then we have 
     * fishing data which is something like
     * a,a,a,b,b,b,c,c,c,d,d,d
     * 
     * @return 
     */
    private int getFishingFrequency() {

        Configuration cfg = Osmose.getInstance().getConfiguration();
        int periods;
        if (cfg.canFind("fisheries.rate.periodsperyear.fis" + mort.getFIndex())) {
            periods = cfg.getInt("fisheries.rate.periodsperyear.fis" + mort.getFIndex());
        } else {
            periods = 1;
        }

        // Checks that the number of fishing period is a multiple of ndt
        if (ndt % periods != 0) {
            error("The number of fishing period must be a multiple of ndt", new Exception());
        }

        int freq = ndt / periods;

        return freq;
    }

    /** Recovers the value of the timeArr array for a given index.
     * 
     * @param idt
     * @return 
     */
    public double getTimeVar(int idt)
    {
        return timeArr[idt];
    }

    public enum FishBase
    {
        CONS,
        BYREGIME,
        LINEAR,
        BYYEAR,
        BYDT;
    }
    
}
