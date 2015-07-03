/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
