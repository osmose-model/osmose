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
 * Gompertz growth function.
 *
 * @author P. Verley
 */
public class GompertzGrowth extends AbstractGrowth {

    private double m0;
    private double g1;
    private double mInf;
    private double gamma;
    private double growthAgeThreshold;

    public GompertzGrowth(int rank, Species species) {
        super(rank, species);
    }

    @Override
    void init() {

        int iSpec = getIndexSpecies();
        m0 = getConfiguration().getDouble("growth.gompertz.m0.sp" + iSpec);
        g1 = getConfiguration().getDouble("growth.gompertz.g1.sp" + iSpec);
        mInf = getConfiguration().getDouble("growth.gompertz.minf.sp" + iSpec);
        gamma = getConfiguration().getDouble("growth.gompertz.gamma.sp" + iSpec);
        growthAgeThreshold = getConfiguration().getDouble("growth.gompertz.threshold.age.sp" + iSpec);
    }

    @Override
    public double ageToLength(double age) {

        double mantleLength;
        double eggSize = getSpecies().getEggSize();
        double ageInDay = age * 365.d;
        if (age == 0) {
            // Egg size for first age class
            mantleLength = eggSize;
        } else if (age < growthAgeThreshold) {
            // Exponential growth
            mantleLength = m0 * Math.exp(g1 * ageInDay);
            if (mantleLength < eggSize) {
                mantleLength = eggSize;
            }
        } else {
            // Gompertz growth
            mantleLength = mInf * Math.exp(-gamma * Math.exp(-g1 * ageInDay));
        }

        return mantleLength;
    }

    @Override
    public double lengthToAge(double length) {

        double ageInDay;
        double lengthAtThreshold = mInf * Math.exp(-gamma * Math.exp(-g1 * (growthAgeThreshold * 365.d)));
        if (length > lengthAtThreshold) {
            if (length < mInf) {
                ageInDay = -Math.log(-Math.log(length / mInf) / gamma) / g1;
            } else {
                // express lifespan in number of days
                ageInDay = getSpecies().getLifespanDt() / getConfiguration().getNStepYear() * 365.d;
            }
        } else {
            ageInDay = Math.log(length / m0) / g1;
        }
        // return age in year
        return (ageInDay / 365.d);
    }
}
