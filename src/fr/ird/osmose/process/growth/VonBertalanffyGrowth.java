/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright IRD (Institut de Recherche pour le Développement) 2015
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
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
package fr.ird.osmose.process.growth;

import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class VonBertalanffyGrowth extends AbstractGrowth {

    /**
     * Von Bertalanffy growth parameters. Parameters <i>species.linf.sp#</i>,
     * <i>species.k.sp#</i> and
     * <i>species.t0.sp#</i>
     */
    private double lInf, K, t0;
    
    /**
     * Threshold age (year) for applying Von Bertalanffy growth model. Parameter
     * <i>species.vonbertalanffy.threshold.age.sp#</i>
     */
    private double growthAgeThreshold;

    public VonBertalanffyGrowth(int rank, Species species) {
        super(rank, species);
    }
    
    @Override
    public void init() {
        
        int index = getIndexSpecies();
        lInf = getConfiguration().getDouble("species.linf.sp" + index);
        K = getConfiguration().getDouble("species.k.sp" + index);
        t0 = getConfiguration().getDouble("species.t0.sp" + index);
        if (!getConfiguration().isNull("species.vonbertalanffy.threshold.age.sp" + index)) {
            growthAgeThreshold = getConfiguration().getDouble("species.vonbertalanffy.threshold.age.sp" + index);
        } else {
            // by default, von Bertalanffy model considered valid after 1 year old, linear growth from 0 to 1 year
            growthAgeThreshold = 1.d;
        }
    }
    
    @Override
    public double ageToLength(double age) {

        double length;
        double eggSize = getSpecies().getEggSize();
        if (age == 0) {
            // Egg size for first time step
            length = eggSize;
        } else if (age < growthAgeThreshold) {
            // Linear growth
            double lengthAtAgePart = lInf * (1 - Math.exp(-K * (growthAgeThreshold - t0)));
            if (lengthAtAgePart < eggSize) {
                lengthAtAgePart = eggSize;
            }
            length = (age / growthAgeThreshold) * (lengthAtAgePart - getSpecies().getEggSize()) + eggSize;
        } else {
            // Von Bertalnffy growth
            length = lInf * (1 - Math.exp(-K * (age - t0)));
        }

        return length;
    }
    
    @Override
    public double lengthToAge(double length) {

        double age;
        double eggSize = getSpecies().getEggSize();
        double lengthAtAgePart = lInf * (1 - Math.exp(-K * (growthAgeThreshold - t0)));
        if (length > lengthAtAgePart) {
            if (length < lInf) {
                age = t0 - (Math.log(1 - length / lInf) / K);
            } else {
                age = getSpecies().getLifespanDt();
            }
        } else {
            age = growthAgeThreshold * (length - eggSize) / (lengthAtAgePart - eggSize);
        }
        return age;
    }
}
