/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
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
package fr.ird.osmose.util.version;

import java.util.Calendar;

/**
 *
 * @author pverley
 */
public class Version3Update1 extends AbstractVersion {

    public Version3Update1() {
        super(3, 1, 2014, Calendar.JUNE, 1);
    }

    @Override
    void updateParameters() {
      
        // Renamed simulation.restart.recordfrequency.ndt into output.restart.recordfrequency.ndt
        updateKey("simulation.restart.recordfrequency.ndt", "output.restart.recordfrequency.ndt");
        
        // Renamed simulation.restart.spinup into output.restart.spinup
        updateKey("simulation.restart.spinup", "output.restart.spinup");

        // Deleted output output.size.perSpecies.enabled
        deprecateParameter("output.size.perSpecies.enabled");

        // Deleted output output.size.spectrum.perSpecies.B.enabled
        deprecateParameter("output.size.spectrum.perSpecies.B.enabled");

        // Deleted output output.size.spectrum.perSpecies.N.enabled
        deprecateParameter("output.size.spectrum.perSpecies.N.enabled");

        // Renamed output.size.spectrum.enabled into output.abundance.bySize.enabled
        updateKey("output.size.spectrum.enabled", "output.abundance.bySize.enabled");

        // Renamed output.size.spectrum.size.min into output.distrib.bySize.min
        updateKey("output.size.spectrum.size.min", "output.distrib.bySize.min");

        // Renamed output.size.spectrum.size.max into output.distrib.bySize.max
        updateKey("output.size.spectrum.size.max", "output.distrib.bySize.max");

        // Renamed output.size.spectrum.size.range into output.distrib.bySize.incr
        updateKey("output.size.spectrum.size.range", "output.distrib.bySize.incr");

        // Renamed output.TL.perAge.enabled into output.meanTL.byAge.enabled
        updateKey("output.TL.perAge.enabled", "output.meanTL.byAge.enabled");

        // Renamed output.TL.perSize.enabled into output.meanTL.bySize.enabled
        updateKey("output.TL.perSize.enabled", "output.meanTL.bySize.enabled");

        // Renamed output.TL.spectrum.enabled into output.biomass.byTL.enabled
        updateKey("output.TL.spectrum.enabled", "output.biomass.byTL.enabled");
    }
}
