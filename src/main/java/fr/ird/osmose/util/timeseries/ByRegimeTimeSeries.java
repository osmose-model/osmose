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
 * processes of fish life cycle (growth, explicit predation, additional and
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
package fr.ird.osmose.util.timeseries;

import fr.ird.osmose.process.mortality.fishery.*;
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
     * @param keyVal  Key for data values
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

        // Initialize the final shift values, i.e after removing out of range ones.
        int shifts[] = new int[nShift];
        int cpt = 0;
        for (int i = 0; i < tempshifts.length; i++) {
            if (tempshifts[i] < nStep) {
                shifts[cpt] = tempshifts[i];
                cpt++;
            }
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
