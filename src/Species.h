#ifndef SPECIES_H
#define SPECIES_H

#include <string.h>
#include "School.h"

using namespace std;

class Species {

    ///////////////////////////////
    // Declaration of the variables
    ///////////////////////////////
  private:
    /**
     * Index of the species. From 0 to Nspecies - 1 for focal species
     */
    int index;
    /**
     * Index of the species in the configuration file.
     */
    int fileIndex;
    /**
     * Name of the species. Parameter <i>species.name.sp#</i>
     */
    string name;

    /**
     * Lifespan expressed in number of time step. A lifespan of 5 years means that a
     * fish will die as soon as it turns 5 years old. Parameter
     * <i>species.lifespan.sp#</i>
     */
    int lifespan;

    /**
     * Allometric parameters. Parameters
     * <i>species.length2weight.condition.factor.sp#</i> and
     * <i>species.length2weight.allometric.power.sp#</i>
     */
    float c, bPower;

    /**
     * Size (cm) at maturity. Parameter <i>species.maturity.size.sp#</i>
     */
    float sizeMaturity;
    /**
     * Age (year) at maturity. Parameter <i>species.maturity.age.sp#</i>
     */
    float ageMaturity;
    /**
     * Size (cm) of eggs. Parameter <i>species.egg.size.sp#</i>
     */
    float eggSize;
    /**
     * Weight (gram) of eggs. Parameter <i>species.egg.weight.sp#</i>
     */
    float eggWeight;

    /**
     * Threshold (number of time-steps) at which a species move from larva to
     * adults. Expressed in time steps.
     */
    int firstFeedingAgeDt;

    /** Threshold for moving from larvaeToAdults. */
    int larvaeToAdultsAgeDt;

    /** Index of the z layer to use when reading temperature or salinity */
    int zlayer = 0;

    /** Bioenergetic constant */
    double beta_bioen;

  public:
    /**
     * Trophic level of an egg.
     */
    static const float TL_EGG;
    
    /**
     * Create a new species
     *
     * @param fileIndex, an integer, the index of the species
     *                   {@code [0, nbTotSpecies - 1]}
     * @param index
     */
    Species(int fileIndex, int index);
    
    int getFirstFeedingAgeDt();
    double getBetaBioen();
    int getDepthLayer();
    int getLarvaeThresDt();
    /**
     * Computes the weight, in gram, corresponding to the given length, in
     * centimetre.
     *
     * @param length, the length in centimetre
     * @return the weight in gram for this {@code length}
     */
    float computeWeight(float length);

    /**
     * Computes the length, in centimeter, corresponding to the given weight, in
     * gram.
     *
     * @param weight, the weight in gram
     * @return the length in centimetre for this {@code weight}
     */
    float computeLength(float weight);
    
    /**
     * Returns the lifespan of the species. Parameter <i>species.lifespan.sp#</i>
     *
     * @return the lifespan, in number of time step
     */
    int getLifespanDt();
    
     /**
     * Returns the index of the species.
     *
     * @return the index of the species
     */
    int getFileSpeciesIndex();
    
     /**
     * Return the global index of the species.
     * 
     * Index between [0, Nspec - 1].
     * 
     * 
     * @return
     */
    int getSpeciesIndex();
    
     /**
     * Returns the name of the species. Parameter <i>species.name.sp#</i>
     *
     * @return the name of the species
     */
    string getName();
    
     /**
     * Returns the size of an egg. Parameter <i>species.egg.size.sp#</i>
     *
     * @return the size of an egg in centimeter
     */
    float getEggSize();
    
     /**
     * Returns the weight of an egg in gram. Parameter <i>species.egg.weight.sp#</i>
     *
     * @return the weight of an egg in gram
     */
    float getEggWeight();
    
    bool isSexuallyMature(School school);
    
};

const float Species::TL_EGG = 3.0;

#endif