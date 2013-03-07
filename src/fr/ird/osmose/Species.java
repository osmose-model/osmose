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
     * Age of recruitment (year)
     */
    final private int recruitmentAge;
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
        this.name = getConfiguration().speciesName[index];
        this.lInf = getConfiguration().lInf[index];
        this.K = getConfiguration().K[index];
        this.t0 = getConfiguration().t0[index];
        this.c = getConfiguration().c[index];
        this.bPower = getConfiguration().bPower[index];
        this.sizeMaturity = getConfiguration().sizeMaturity[index];
        this.recruitmentAge = Math.round(getConfiguration().recruitmentAge[index] * getConfiguration().getNumberTimeStepsPerYear());
        this.ageClassZero = (int) Math.ceil(getConfiguration().supAgeOfClass0Matrix[index] * getConfiguration().getNumberTimeStepsPerYear());      // index of supAgeOfClass0 used in tabCohorts table
        this.eggSize = getConfiguration().eggSize[index];
        this.eggWeight = getConfiguration().eggWeight[index];
        this.growthAgeThreshold = getConfiguration().growthAgeThreshold[index];
        lifespan = (int) Math.round(getConfiguration().speciesLifespan[index] * getConfiguration().getNumberTimeStepsPerYear());
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
            float decimalAge = age / (float) getConfiguration().getNumberTimeStepsPerYear();
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

    /**
     * @return the age of recruitment, expressed in number of time steps.
     */
    public int getRecruitmentAge() {
        return recruitmentAge;
    }

    private OldConfiguration getConfiguration() {
        return Osmose.getInstance().getOldConfiguration();
    }
}
