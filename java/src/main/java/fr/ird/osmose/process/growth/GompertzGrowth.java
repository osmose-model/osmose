/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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
    public void init() {

        int iSpec = getIndexSpecies();
        lStart = getConfiguration().getDouble("growth.exponential.lstart.sp" + iSpec);
        Ke = getConfiguration().getDouble("growth.exponential.ke.sp" + iSpec);
        lInf = getConfiguration().getDouble("growth.gompertz.linf.sp" + iSpec);
        Kg = getConfiguration().getDouble("growth.gompertz.kg.sp" + iSpec);
        tg = getConfiguration().getDouble("growth.gompertz.tg.sp" + iSpec);
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
