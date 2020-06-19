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
package fr.ird.osmose.process.mortality.fishery;

import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.ByRegimeTimeSeries;
import java.io.IOException;

/**
 *
 *
 * @author nbarrier
 */
public class FisheryFBase extends OsmoseLinker {

    int fisheryIndex;
    public double[] fBase;

    /**
     * Public constructor. Initialize the FisheryMortality pointer.
     *
     * @param fishery
     */
    public FisheryFBase(int index) {
        this.fisheryIndex = index;
        fBase = new double[getConfiguration().getNStep()];
    }

    /**
     * Initialize the time varying index.
     */
    public void init() {

        int index = fisheryIndex;

        // If a fishing shift exists, take it to extract the fishing values
        String keyShift = String.format("fisheries.rate.base.shift.fsh%d", index);
        String keyVal = String.format("fisheries.rate.base.fsh%d", index);
        
        boolean useLog10 = getConfiguration().getBoolean("fisheries.rate.base.log.enabled.fsh" + index);

        ByRegimeTimeSeries ts = new ByRegimeTimeSeries(keyShift, keyVal);
        ts.init();

        fBase = ts.getValues();
        if(useLog10) {
            for (int i = 0; i < fBase.length; i++) {
                if(fBase[i] > 0) {
                    String message = String.format("Fishing mortality rate exponent for fishery %d is positive", index);
                    error(message, new IllegalArgumentException());
                }
                fBase[i] = Math.exp(fBase[i]);
            }
        }

    } // end of init method

    /**
     * Returns the value of the fbase array at a given time step.
     *
     * @param idt Time step
     * @return
     */
    public double getFBase(int idt) {
        return this.fBase[idt];
    }

} // end of class
