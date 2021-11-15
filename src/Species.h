
#include <string.h>

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
};

const float Species::TL_EGG = 3.0;