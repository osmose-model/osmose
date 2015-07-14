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

    private double lStart;
    private double Ke;
    private double Kg;
    private double lInf;
    private double tg;
    private double ageThrExp, ageThrGom;

    public GompertzGrowth(int rank, Species species) {
        super(rank, species);
    }

    @Override
    void init() {

        int iSpec = getIndexSpecies();
        lStart = getConfiguration().getDouble("growth.exponential.lstart.sp" + iSpec);
        Ke = getConfiguration().getDouble("growth.exponential.ke.sp" + iSpec);
        lInf = getConfiguration().getDouble("growth.gompertz.linf.sp" + iSpec);
        Kg = getConfiguration().getDouble("growth.gompertz.kg.sp" + iSpec);
        ageThrExp = getConfiguration().getDouble("growth.exponential.thr.age.sp" + iSpec);
        ageThrGom = getConfiguration().getDouble("growth.gompertz.thr.age.sp" + iSpec);
    }

    @Override
    public double ageToLength(double age) {

        double length;
        double eggSize = getSpecies().getEggSize();
        if (age == 0) {
            // Egg size for first age class
            length = eggSize;
        } else if (age < ageThrExp) {
            // Exponential growth for age < 80 days
            length = lStart * Math.exp(Ke * age);
            if (length < eggSize) {
                length = eggSize;
            }
        } else if (age < ageThrGom) {
            // Linear growth between 80 and 120 days
            double lExpMax = lStart * Math.exp(Ke * ageThrExp);
            double lGomMin = lInf * Math.exp(-Math.exp(-Kg * (ageThrGom - tg)));
            length = lExpMax + ((lGomMin - lExpMax) / (ageThrGom - ageThrExp)) * (age - ageThrExp);
        } else {
            // Gompertz growth for age > 120 days
            length = lInf * Math.exp(-Math.exp(-Kg * (age - tg)));
        }

        return length;
    }

    @Override
    public double lengthToAge(double length) {

        double age;
        double lExpMax = lStart * Math.exp(Ke * ageThrExp);
        double lGomMin = lInf * Math.exp(-Math.exp(-Kg * (ageThrGom - tg)));
        if (length > lGomMin) {
            // Gompertz
            if (length < lInf) {
                age = tg - Math.log(-Math.log(length / lInf)) / Kg;
            } else {
                // express lifespan in years
                age = getSpecies().getLifespanDt() / getConfiguration().getNStepYear();
            }
        } else if (length > lExpMax) {
            // Linear growth
            age = ageThrExp + ((ageThrGom - ageThrExp) / (lGomMin - lExpMax)) * (length - lExpMax);
        } else {
            // Exponential growth
            age = Math.log(length / lStart) / Ke;
        }
        // return age in year
        return age;
    }
}
