/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.util.timeseries;

import fr.ird.osmose.util.OsmoseLinker;
import java.io.IOException;

/**
 *
 *
 * @author nbarrier
 */
public class ByRegimeTimeSeries extends OsmoseLinker {

    private double[] values;
    private final String keyShift;
    private final String keyVal;

    /**
     * Public constructor.Initialize the FisheryMortality pointer.
     *
     * @param keyShift Key for shift values
     * @param keyVal Key for data values
     */
    public ByRegimeTimeSeries(String keyShift, String keyVal) {
        this.keyShift = keyShift;
        this.keyVal = keyVal;
        values = new double[getConfiguration().getNStep()];
    }

    /**
     * Initialize the time varying index.
     */
    public void init() {

        int nStep = this.getConfiguration().getNStep();
        int nStepYear = this.getConfiguration().getNStepYear();
        
        if (this.getConfiguration().isNull(keyShift)) {
            //String warn = String.format("The %s argument was not found. Assumes one single value for all the simulation.", keyShift);
            //warning(warn);
            double cstVal = getConfiguration().getDouble(this.keyVal);
            for (int i = 0; i < nStep; i++) {
                values[i] = cstVal;
            }

        } else {

            // If a fishing shift exists, take it to extract the fishing values
            // Shift is provided in years in the file
            int[] tempshifts = getConfiguration().getArrayInt(this.keyShift);

            // converted into to time-step.
            for (int i = 0; i < tempshifts.length; i++) {
                tempshifts[i] *= nStepYear;
            }
            
            // Count the number of good shift values, i.e within simu. time period
            int nShift = 0;
            for (int i = 0; i < tempshifts.length; i++) {
                if (tempshifts[i] < nStep) {  // tempshifts here is converted from years to time-step
                    nShift++;
                }
            }

            int shifts[];
            if (nShift > 0) {
                // Initialize the final shift values, i.e after removing out of range ones.
                shifts = new int[nShift];
                int cpt = 0;
                for (int i = 0; i < tempshifts.length; i++) {
                    if (tempshifts[i] < nStep) {
                        shifts[cpt] = tempshifts[i];
                        cpt++;
                    }
                }
            } else { 
                shifts = new int[1];
                shifts[0] = nStep;
            }

            // number of regimes is number of shifts + 1
            int nRegime = nShift + 1;

            // Get the fishing rates.
            double[] rates = getConfiguration().getArrayDouble(this.keyVal);

            if (rates.length < nRegime) {
                error("You must provide at least " + nRegime + " fishing rates.", new IOException());
            }

            int irate = 0;   // current index in the rate array.
            int ishift = 0;  // current index of the next shift array.
            int sh = shifts[ishift];   // sets the value of the next shift (time step)
            for (int i = 0; i < nStep; i++) {

                // if the current array index is greater than shift,
                // we update the ishift and irate array.
                if (i >= sh) {
                    ishift++;
                    irate++;

                    // if the shift index is greater than bound array
                    // the last shift value is set as equal to nyear*ndt
                    sh = (ishift < shifts.length) ? shifts[ishift] : nStep;

                }

                values[i] = rates[irate];

            } // end of i loop
        }
    } // end of init method

    /**
     * Returns the value of the fbase array at a given time step.
     *
     * @param idt Time step
     * @return
     */
    public double[] getValues() {
        return this.values;
    }

} // end of class
