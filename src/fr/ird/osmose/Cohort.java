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
    /*
     * Abundance of the cohort, number of individuals
     */
    private double abundance;
    /*
     * Biomass [ton] of the cohort
     */
    private double biomass;
    
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
        this.abundance = abundance;
        this.biomass = biomass;
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

    public void upperAgeClass(Cohort upperAgerCohort) {
        clear();
        addAll(upperAgerCohort);
        abundance = upperAgerCohort.getAbundance();
        biomass = upperAgerCohort.getBiomass();
    }

    /**
     * @return the abundance
     */
    public double getAbundance() {
        return abundance;
    }

    public void incrementAbundance(double abundance) {
        this.abundance += abundance;
    }

    /**
     * @param abundance the abundance to set
     */
    public void setAbundance(double abundance) {
        this.abundance = abundance;
    }

    /**
     * @return the biomass
     */
    public double getBiomass() {
        return biomass;
    }

    public void incrementBiomass(double biomass) {
        this.biomass += biomass;
    }

    /**
     * @param biomass the biomass to set
     */
    public void setBiomass(double biomass) {
        this.biomass = biomass;
    }
}
