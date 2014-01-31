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
package fr.ird.osmose;

/**
 * This class represents a species. It is characterized by the following
 * variables:
 * <ul>
 * <li>name</li>
 * <li>lifespan</li>
 * <li>Von Bertalanffy growth parameters</li>
 * <li>Threshold age for applying Von Bertalanffy growth model</li>
 * <li>Allometric parameters</li>
 * <li>Egg weight and size</li>
 * <li>Size at maturity</li>
 * <li>Threshold age for age class zero</li>
 * <ul>
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Species {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Trophic level of an egg.
     */
    final static public float TL_EGG = 3f;
    /**
     * Index of the species. [0 : numberTotalSpecies - 1]
     */
    final private int index;
    /**
     * Name of the species. Parameter <i>species.name.sp#</i>
     */
    final private String name;
    /**
     * Lifespan expressed in number of time step. A lifespan of 5 years means
     * that a fish will die as soon as it turns 5 years old. Parameter
     * <i>species.lifespan.sp#</i>
     */
    final private int lifespan;
    /**
     * Von Bertalanffy growth parameters. Parameters <i>species.linf.sp#</i>,
     * <i>species.k.sp#</i> and
     * <i>species.t0.sp#</i>
     */
    final private float lInf, K, t0;
    /**
     * Allometric parameters. Parameters
     * <i>species.length2weight.condition.factor.sp#</i> and
     * <i>species.length2weight.allometric.power.sp#</i>
     */
    final private float c, bPower;
    /**
     * Size (cm) at maturity. Parameter <i>species.maturity.age.sp#</i>
     */
    final private float sizeMaturity;
    /**
     * Threshold age (year) for age class zero. It is the age from which target
     * biomass should be considered as eggs and larvae stages are generally not
     * considered. Parameter <i>output.cutoff.age.sp#</i>
     */
    final private int ageClassZero;
    /**
     * Size (cm) of eggs. Parameter <i>species.egg.size.sp#</i>
     */
    final private float eggSize;
    /**
     * Weight (gram) of eggs. Parameter <i>species.egg.weight.sp#</i>
     */
    final private float eggWeight;
    /**
     * Threshold age (year) for applying Von Bertalanffy growth model. Parameter
     * <i>species.vonbertalanffy.threshold.age.sp#</i>
     */
    final private float growthAgeThreshold;

    final private float conversionToTotalLength;

//////////////
// Constructor
//////////////
    /**
     * Create a new species
     *
     * @param index, an integer, the index of the species
     * {@code [0, nbTotSpecies - 1]}
     */
    public Species(int index) {

        this.index = index;
        // Initialization of parameters
        name = getConfiguration().getString("species.name.sp" + index);
        lInf = getConfiguration().getFloat("species.linf.sp" + index);
        K = getConfiguration().getFloat("species.k.sp" + index);
        t0 = getConfiguration().getFloat("species.t0.sp" + index);
        c = getConfiguration().getFloat("species.length2weight.condition.factor.sp" + index);
        bPower = getConfiguration().getFloat("species.length2weight.allometric.power.sp" + index);
        if (!getConfiguration().isNull("species.maturity.size.sp" + index)) {
            sizeMaturity = getConfiguration().getFloat("species.maturity.size.sp" + index);
        } else {
            float ageMaturity = getConfiguration().getFloat("species.maturity.age.sp" + index);
            sizeMaturity = lInf * (float) (1 - Math.exp(-K * (ageMaturity - t0)));
        }
        float age0 = getConfiguration().getFloat("output.cutoff.age.sp" + index);
        ageClassZero = (int) Math.ceil(age0 * getConfiguration().getNStepYear());
        eggSize = getConfiguration().getFloat("species.egg.size.sp" + index);
        eggWeight = getConfiguration().getFloat("species.egg.weight.sp" + index);
        if (!getConfiguration().isNull("species.vonbertalanffy.threshold.age.sp" + index)) {
            growthAgeThreshold = getConfiguration().getFloat("species.vonbertalanffy.threshold.age.sp" + index);
        } else {
            // by default, von Bertalanffy model considered valid after 1 year old, linear growth from 0 to 1 year
            growthAgeThreshold = 1.f;
        }
        float agemax = getConfiguration().getFloat("species.lifespan.sp" + index);
        lifespan = (int) Math.round(agemax * getConfiguration().getNStepYear());
        if (!getConfiguration().isNull("species.conversion2totallength.sp" + index)) {
            conversionToTotalLength = getConfiguration().getFloat("species.conversion2totallength.sp" + index);
        } else {
            conversionToTotalLength = 1.f;
        }
    }

//////////////////////////////
// Definition of the functions
//////////////////////////////
    /**
     * Computes the mean length, in centimeter, at a specific age.
     *
     * @param age, an age in number of time step.
     * @return the mean length, in centimeter, at this {@code age}
     */
    public float computeMeanLength(int age) {

        float length;
        float decimalAge = age / (float) getConfiguration().getNStepYear();
        if (age == 0) {
            // Egg size for first time step
            length = eggSize;
        } else if (decimalAge < growthAgeThreshold) {
            // Linear growth
            float lengthAtAgePart = (float) (lInf * (1 - Math.exp(-K * (growthAgeThreshold - t0))));
            if (lengthAtAgePart < eggSize) {
                lengthAtAgePart = eggSize;
            }
            length = (decimalAge / growthAgeThreshold) * (float) (lengthAtAgePart - eggSize) + eggSize;
        } else {
            // Von Bertalnffy growth
            length = (float) (lInf * (1 - Math.exp(-K * (decimalAge - t0))));
        }

        return length;
    }

    /**
     * Compute the mean age, in number of time step, at a specific length, in
     * centimeter.
     *
     * @param length the length in centimeter
     * @return the mean age in number of time step for this {@code length}
     */
    public int computeMeanAge(float length) {

        float age;
        float lengthAtAgePart = (float) (lInf * (1 - Math.exp(-K * (growthAgeThreshold - t0))));
        if (length > lengthAtAgePart) {
            if (length < lInf) {
                age = (float) (-((Math.log(1 - (length / lInf))) / K)) + t0;
            } else {
                age = lifespan;
            }
        } else {
            age = growthAgeThreshold * (length - eggSize) / (lengthAtAgePart - eggSize);
        }
        return Math.round(age * getConfiguration().getNStepYear());

    }

    /**
     * Computes the mean weight, in gram, at a specific age, in number of time
     * step.
     *
     * @param age, the age in number of time step
     * @return the mean weight in gram at this {@code age}
     */
    public float computeMeanWeight(int age) {

        float weight;
        if (age == 0) {
            weight = eggWeight;
        } else {
            weight = computeWeight(computeMeanLength(age));
            if (weight < eggWeight) {
                weight = eggWeight;
            }
        }
        return weight;
    }

    /**
     * Computes the weight, in gram, corresponding to the given length, in
     * centimeter.
     *
     * @param length, the length in centimeter
     * @return the weight in gram for this {@code length}
     */
    public float computeWeight(float length) {
        return (float) (c * (Math.pow(length, bPower)));
    }

    /**
     * Converts physiological length into total length. Default length that
     * characterizes the species should be the "physiological" length (e.g. the
     * fork length or the cephalo-thorax length, the standard length, etc.).
     * Whereas in the predation process Osmose must consider the total length as
     * a proxy of the diameter of the prey. Conversion is done with a
     * multiplying parameter.
     *
     * @param length, the physiological length of the fish
     * @return the total length of the fish
     */
    public float toTotalLength(float length) {
        return conversionToTotalLength * length;
    }

    /**
     * Returns the lifespan of the species. Parameter
     * <i>species.lifespan.sp#</i>
     *
     * @return the lifespa, in number of time step
     */
    public int getLifespanDt() {
        return lifespan;
    }

    /**
     * Returns the index of the species.
     *
     * @return the index of the species
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the name of the species. Parameter <i>species.name.sp#</i>
     *
     * @return the name of the species
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the size of an egg. Parameter <i>species.egg.size.sp#</i>
     *
     * @return the size of an egg in centimeter
     */
    public float getEggSize() {
        return eggSize;
    }

    /**
     * Returns the weight of an egg in gram. Parameter
     * <i>species.egg.weight.sp#</i>
     *
     * @return the weight of an egg in gram
     */
    public float getEggWeight() {
        return eggWeight;
    }

    /**
     * Returns the threshold of age class zero, in number of time step. This
     * parameter allows to discard schools younger that this threshold in the
     * calculation of the indicators when parameter <i>output.cutoff.enabled</i>
     * is set to {@code true}. Parameter <i>output.cutoff.age.sp#</i>
     *
     * @return the threshold age of class zero, in number of time step
     */
    public int getAgeClassZero() {
        return ageClassZero;
    }

    /**
     * Return the size, in centimeter, at (sexual) maturity. Parameter
     * <i>species.maturity.age.sp#</i>
     *
     * @return the size at maturity in centimeter
     */
    public float getSizeMaturity() {
        return sizeMaturity;
    }

    /**
     * Just keep it as a reminder for a future vulnerability function
     *
     * @param biomass
     * @return
     */
    private boolean isVulnerable(double biomass) {
        double Bv = 0.d;
        double Sv = 1.d;
        return (Math.random() > (1.d / (1.d + Math.exp(Sv * (Bv - biomass)))));
    }

    /**
     * Returns an instance of the {@code Configuration}.
     *
     * @return an instance of the {@code Configuration}
     */
    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }
}
