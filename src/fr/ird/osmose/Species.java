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

public class Species {

////////////
// Variables
////////////
    /**
     * Trophic level of eggs.
     */
    final static public float TL_EGG = 3f;
    /**
     * Index of the species. [0 : numberTotalSpecies - 1]
     */
    final private int index;
    /**
     * Name of the species.
     */
    final private String name;
    /**
     * Lifespan expressed in number of time steps.
     */
    final private int lifespan;
    /**
     * Von bertalanffy growth parameters.
     */
    final private float lInf, K, t0;
    /**
     * Allometric parameters.
     */
    final private float c, bPower;
    /**
     * Size (cm) at maturity.
     */
    final private float sizeMaturity;
    /**
     * Threshold age (year) for age class zero. It is the age from which target
     * biomass should be considered as eggs and larvae stages are generally not
     * considered.
     */
    final private int ageClassZero;
    /**
     * Size (cm) of eggs.
     */
    final private float eggSize;
    /**
     * Weight (gram) of eggs.
     */
    final private float eggWeight;
    /**
     * Threshold age (year) for applying Von Bertalanffy growth model.
     */
    final private float growthAgeThreshold;

//////////////
// Constructor
//////////////
    /**
     * Create a new species
     *
     * @param index, an integer, the index of the species {0 : nbTotSpecies - 1}
     */
    public Species(int index) {
        this.index = index;
        // INITIALISATION of PARAM
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

//        System.out.println("***********");
//        System.out.println("name " + name);
//        System.out.println("lInf " + lInf);
//        System.out.println("K " + K);
//        System.out.println("t0 " + t0);
//        System.out.println("c " + c);
//        System.out.println("bPower " + bPower);
//        System.out.println("sizeMaturity " + sizeMaturity);
//        System.out.println("recruitmentAge " + recruitmentAge);
//        System.out.println("ageClassZero " + ageClassZero);
//        System.out.println("eggSize " + eggSize);
//        System.out.println("eggWeight " + eggWeight);
//        System.out.println("growthAgeThreshold " + growthAgeThreshold);
//        System.out.println("lifespan " + lifespan);
    }

////////////
// Functions
////////////
    /**
     * Computes the mean length (cm) at a specific age.
     *
     * @param age, expressed in number of time steps.
     * @return the mean length (cm)
     */
    public float computeMeanLength(int age) {

        float length;
        if (age == 0) {
            length = eggSize;
        } else {
            float decimalAge = age / (float) getConfiguration().getNStepYear();
            if (decimalAge < growthAgeThreshold) {
                float lengthAtAgePart = (float) (lInf * (1 - Math.exp(-K * (growthAgeThreshold - t0))));
                if (lengthAtAgePart < eggSize) {
                    lengthAtAgePart = eggSize;
                }
                length = decimalAge * (float) (lengthAtAgePart - eggSize) + eggSize;    // linear growth for the 1st year as von Bertalanffy is not well adapted for the 1st year
            } else {
                length = (float) (lInf * (1 - Math.exp(-K * (decimalAge - t0))));   // von Bertalnffy growth after the first year
            }
        }

        return length;
    }

    /**
     * Compute the mean age (number of time steps) at a specific length (cm).
     * @param length (cm)
     * @return the mean age expressed in number of time steps
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
     * Computes the mean weight (gram) at a specific age.
     *
     * @param age, expressed in number of time steps.
     * @return the mean weight (gram)
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
     * Computes the weight corresponding to the given length.
     *
     * @param length (cm)
     * @return the weight (gram)
     */
    public float computeWeight(float length) {
        return (float) (c * (Math.pow(length, bPower)));
    }

    /**
     * @return the longevity, expressed in number of time steps.
     */
    public int getLifespanDt() {
        return lifespan;
    }

    /**
     * @return the index of the species.
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the name of the species.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the egg's size (cm)
     */
    public float getEggSize() {
        return eggSize;
    }

    /**
     * @return the egg's weight (gram)
     */
    public float getEggWeight() {
        return eggWeight;
    }

    /**
     * @return the threshold age of class zero, expressed in number of time
     * steps.
     */
    public int getAgeClassZero() {
        return ageClassZero;
    }

    /**
     * @return the size of maturity (cm)
     */
    public float getSizeMaturity() {
        return sizeMaturity;
    }

    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }
}
