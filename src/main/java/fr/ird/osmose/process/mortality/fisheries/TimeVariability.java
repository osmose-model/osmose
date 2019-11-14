/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.process.mortality.fisheries;

import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

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
    private final SingleFisheriesMortality fishery;

    private double[] timeArr;

    /**
     * Number of time steps per year.
     */
    private int ndt;

    /**
     * Number of years.
     */
    private int nyear;

    /**
     * Public constructor. Initialize the FisheriesMortality pointer.
     *
     * @param fishery
     */
    public TimeVariability(SingleFisheriesMortality fishery) {
        this.fishery = fishery;
    }

    /**
     * Initialize the time varying index.
     */
    public void init() {
        
        int index = fishery.getFIndex();
        this.ndt = getConfiguration().getNStepYear();
        this.nyear = Math.max(getConfiguration().getNStep() / ndt, 1);

        String type = getConfiguration().getString("fishery.fishing.rate.method.fsh" + index);
        switch (type) {
            case "constant":
                this.initFBaseConstant();
                break;
            case "byregime":
                this.initFBaseRegime();
                break;
            case "linear":
                this.initFBaseLinear();
                break;
            case "byyear":
                this.getFBaseByYear();
                break;
            case "bydt":
                this.getFBaseByDt();
                break;
            default:
                error("Fishing base " + type + "is not implemented.", new Exception());
        }
    }

    /**
     * Initialize the time array from dt values. Here, the rate array is
     * repeated cyclically. For instance, if rate = [1, 3 , 7, 8, 20] and there
     * is 8 values to fill, the timeArr array will be [1, 3, 7, 8, 20, 1, 3, 7]
     *
     */
    private void getFBaseByDt() {

        if (getConfiguration().canFind("fishery.fishing.rate.bydt.file.fsh" + fishery.getFIndex())) {
            // If a file parameter has been defined 
            SingleTimeSeries ts = new SingleTimeSeries();
            ts.read(getConfiguration().getFile("fishery.fishing.rate.bydt.file.fsh" + fishery.getFIndex()));
            this.timeArr = ts.getValues();
        } else {
            timeArr = new double[getConfiguration().getNStep()];
            double[] value = getConfiguration().getArrayDouble("fishery.fishing.rate.bydt.fsh" + fishery.getFIndex());
            // check F vector consistency
            if (timeArr.length != value.length && value.length % ndt != 0) {
                error("The length of the fishing rate vector (" + value.length + ") must be a multiple of the number of time steps per year " + ndt, new Exception("Fishery " + fishery.getFIndex()));
            }
            for (int i = 0; i < timeArr.length; i++) {
                int k = i % value.length;
                this.timeArr[i] = value[k];
            }
        }
    }

    /**
     * Initialize the time array with a vector of annual fishing rates. Vector
     * provides one F value per year. If there are less F values than number of
     * simulated years, Osmose will loop over it. If there are more F values
     * than number of simulated years, Osmose will ignore exceeding years.
     */
    private void getFBaseByYear() {

        double[] annualF = getConfiguration().getArrayDouble("fishery.fishing.rate.byYear.fsh" + fishery.getFIndex());
        if (annualF.length != nyear) {
            StringBuilder sb = new StringBuilder();
            sb.append("Length of fishery.fishing.rate.byYear.fsh").append(fishery.getFIndex()).append(" ").append(annualF.length);
            sb.append(", number of simulated years ").append(nyear).append(". ");
            sb.append(annualF.length > nyear
                    ? "Osmose will ignore exceeding values in the F vector."
                    : "Osmose will loop over the F vector.");
            warning(sb.toString());
        }

        // converts annual rates into rates per time step
        timeArr = new double[getConfiguration().getNStep()];
        for (int iStep = 0; iStep < timeArr.length; iStep++) {
            int iYear = (iStep / ndt) % annualF.length;
            timeArr[iStep] = annualF[iYear] / ndt;
        }

    }

    /**
     * Initialize the time array from yearly values. If there is 2 years to fill
     * with 12 time steps, and peryear=4, the rate array should have 4 values.
     * If rate = [a, b, c, d], then timeArr = [ a, a, a, b, b, b, c, c, c, d, d,
     * d, (year 1) a, a, a, b, b, b, c, c, c, d, d, d, (year 2) ]
     */
    private void getFBaseByPeriod() {

        // fishing rate vector
        double[] value = getConfiguration().getArrayDouble("fishery.fishing.rate.byPeriod.fsh" + fishery.getFIndex());

        // number of fishing values per year
        int nperiod = !getConfiguration().isNull("fishery.fishing.rate.nperiod.fsh" + fishery.getFIndex())
                ? getConfiguration().getInt("fishery.fishing.rate.nperiod.fsh" + fishery.getFIndex())
                : 1;
        if (value.length % nperiod != 0) {
            error("The number of rate values (" + value.length + ") must be a multiple of the number of periods " + nperiod, new Exception("Fishery " + fishery.getFIndex()));
        }
        int nyearF = value.length / nperiod;
        if (nyearF != nyear) {
            StringBuilder sb = new StringBuilder();
            sb.append("Length of fishery.fishing.rate.byPeriod.fsh").append(fishery.getFIndex())
                    .append(" ").append(value.length).append(" / nperiod=")
                    .append(nperiod).append(" = ").append(nyearF);
            sb.append(", number of simulated years ").append(nyear).append("\n");
            sb.append(nyearF > nyear
                    ? "Osmose will ignore exceeding values in the F vector."
                    : "Osmose will loop over the F vector.");
            warning(sb.toString());
        }

        // converts annual rates into seasonal rates
        timeArr = new double[getConfiguration().getNStep()];
        for (int iStep = 0; iStep < timeArr.length; iStep++) {
            int iRegime = (iStep * nperiod / ndt) % value.length;
            timeArr[iStep] = value[iRegime] / ndt;
        }
    }

    /**
     * Initialize the time array from constant annual mortality rate.
     */
    private void initFBaseConstant() {
        
        double F = getConfiguration().getDouble("fishery.fishing.rate.fsh" + fishery.getFIndex());

        int nStepYear = getConfiguration().getNStepYear();
        timeArr = new double[getConfiguration().getNStep()];
        for (int i = 0; i < timeArr.length; i++) {
            // convert from annual mortality rate to seasonal mortality rate
            timeArr[i] = F / nStepYear;
        }
    }

    /**
     * Initialize the time array from linear value.
     */
    private void initFBaseLinear() {
        
        double rate = getConfiguration().getDouble("fishery.fishing.rate.linear.fsh" + fishery.getFIndex());
        double slope = getConfiguration().getDouble("fishery.fishing.rate.linear.slope.fsh" + fishery.getFIndex());

        int freq = this.getFishingFrequency();

        for (int i = 0; i < ndt * nyear; i++) {
            int x = i / freq;
            float time = (x * freq) / ((float) ndt);
            timeArr[i] = rate * (1 + slope * time) / ndt;
        }

    }

    /**
     * Initialize the time array from regime shift values.
     */
    private void initFBaseRegime() {
        
        int index = fishery.getFIndex();

        /**
         * Recover the shift array from file, and remove values for which shift
         * > T * ndt;
         */
        int tempshifts[] = getConfiguration().getArrayInt("fishery.fishing.rate.byRegime.shifts.fsh" + index);

        int nStepYear = getConfiguration().getNStepYear();

        // Count the number of good shift values
        int nShift = 0;
        for (int i = 0; i < tempshifts.length; i++) {
            if (tempshifts[i] < ndt * nyear) {
                nShift++;
            }
        }

        // Initialize the shift values 
        int[] shifts = new int[nShift];
        int cpt = 0;
        for (int i = 0; i < tempshifts.length; i++) {
            if (tempshifts[i] < ndt * nyear) {
                shifts[cpt] = tempshifts[i];
                cpt++;
            }
        }

        // number of regimes is number of shifts + 1
        int nRegime = nShift + 1;

        double[] rates = getConfiguration().getArrayDouble("fishery.fishing.rate.byRegime.fsh" + index);
        if (rates.length != nRegime) {
            error("You must provide " + nRegime + " fishing rates.", new Exception());
        }

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

            timeArr[i] = rates[irate] / nStepYear;

        }

    }

    /**
     * Returns the number of time steps with constant fishing data. If ndt=12
     * and periodperyear = 4, then we have fishing data which is something like
     * a,a,a,b,b,b,c,c,c,d,d,d
     *
     * @return
     */
    private int getFishingFrequency() {

        int periods;
        if (getConfiguration().canFind("fishery.fishing.rate.nperiod.fsh" + fishery.getFIndex())) {
            periods = getConfiguration().getInt("fishery.fishing.rate.nperiod.fsh" + fishery.getFIndex());
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

    /**
     * Recovers the value of the timeArr array for a given index.
     *
     * @param idt
     * @return
     */
    public double getTimeVar(int idt) {
        return timeArr[idt];
    }

}
