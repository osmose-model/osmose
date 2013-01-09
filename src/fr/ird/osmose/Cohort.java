package fr.ird.osmose;

/**
 * *****************************************************************************
 * <p>Titre : Cohort class</p>
 *
 * <p>Description : groups the super-individuals (School). represents a cohort
 * (not annual but for a tims step)</p>
 *
 * <p>Copyright : Copyright (c) may 2009 </p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * *******************************************************************************
 */
import java.util.ArrayList;

public class Cohort extends ArrayList<School> {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
//////////////
// Constructor
//////////////
    /**
     *
     * @param species
     * @param age
     * @param abundance
     * @param biomass
     * @param iniLength
     * @param iniWeight
     */
    public Cohort(Species species, int age, long abundance, double biomass,
            float iniLength, float iniWeight) {
        if (biomass > 0.d) {
            int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
            ensureCapacity(nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                add(new School(species, abundance / nbSchools, iniLength, iniWeight, age));
            }
        }
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    public School getSchool(int index) {
        return get(index);
    }
}
