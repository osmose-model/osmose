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
        
        int index = getFileSpeciesIndex();
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
                age = getSpecies().getLifespanDt() / getConfiguration().getNStepYear();
            }
        } else {
            age = growthAgeThreshold * (length - eggSize) / (lengthAtAgePart - eggSize);
        }
        return age;
    }
}
