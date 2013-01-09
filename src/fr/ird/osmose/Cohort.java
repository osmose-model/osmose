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
import java.util.List;

public class Cohort extends ArrayList<School> {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /*
     * Species of the cohort
     */
    private Species species;
    /*
     * Abundance of the cohort, number of individuals
     */
    private double abundance;
    /*
     * Biomass [ton] of the cohort
     */
    private double biomass;
    /*
     *
     */
    private float[] outOfZoneMortality;
    private boolean[] outOfZoneCohort;
    /*
     * Number of catchable schools (~ not in MPA areas)
     */
    private int nbSchoolsCatchable;
    /*
     * Abundance of catchable schools
     */
    private double abundanceCatchable;

//////////////
// Constructor
//////////////
    /**
     *
     * @param species
     * @param ageNbDt
     * @param abundance
     * @param biomass
     * @param iniLength
     * @param iniWeight
     */
    public Cohort(Species species, int ageNbDt, long abundance, double biomass,
            float iniLength, float iniWeight) {
        this.species = species;

        outOfZoneMortality = new float[getSimulation().getNbTimeStepsPerYear()];
        outOfZoneCohort = new boolean[getSimulation().getNbTimeStepsPerYear()];

        for (int i = 0; i < getSimulation().getNbTimeStepsPerYear(); i++) {
            // initialization by default
            outOfZoneMortality[i] = 0;
            outOfZoneCohort[i] = false;
        }

        this.abundance = abundance;
        this.biomass = biomass;
        if (biomass > 0.d) {
            int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
            ensureCapacity(nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                add(new School(this, abundance / nbSchools, iniLength, iniWeight, ageNbDt));
            }
        }
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    public School getSchool(int index) {
        return get(index);
    }

    public void removeDeadSchools() {

        List<School> schoolsToRemove = new ArrayList();
        for (School school : this) {
            if (school.willDisappear()) {
                // cohorts in the area during the time step
                if (!(outOfZoneCohort[getSimulation().getIndexTimeYear()])) {
                    school.getCell().remove(school);
                }
                schoolsToRemove.add(school);
            }
        }
        removeAll(schoolsToRemove);
    }

    public void upperAgeClass(Cohort upperAgerCohort) {
        clear();
        addAll(upperAgerCohort);
        abundance = upperAgerCohort.getAbundance();
        biomass = upperAgerCohort.getBiomass();
    }

    /**
     * @return the species
     */
    public Species getSpecies() {
        return species;
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

    public float getOutMortality(int indexTime) {
        return outOfZoneMortality[indexTime];
    }

    public void setOutMortality(int indexTime, float mortality) {
        outOfZoneMortality[indexTime] = mortality;
    }

    public void setOut(int indexTime, boolean isOut) {
        outOfZoneCohort[indexTime] = isOut;
    }

    public boolean isOut(int indexTime) {
        return outOfZoneCohort[indexTime];
    }

    /**
     * @return the nbSchoolsCatchable
     */
    public int getNbSchoolsCatchable() {
        return nbSchoolsCatchable;
    }

    /**
     * @param nbSchoolsCatchable the nbSchoolsCatchable to set
     */
    public void setNbSchoolsCatchable(int nbSchoolsCatchable) {
        this.nbSchoolsCatchable = nbSchoolsCatchable;
    }

    /**
     * @param nbSchoolsCatchable the nbSchoolsCatchable to set
     */
    public void incrementAbundanceCatchable(double abundanceCatchable) {
        this.abundanceCatchable += abundanceCatchable;
    }

    /**
     * @return the abundanceCatchable
     */
    public double getAbundanceCatchable() {
        return abundanceCatchable;
    }

    /**
     * @param abundanceCatchable the abundanceCatchable to set
     */
    public void setAbundanceCatchable(double abundanceCatchable) {
        this.abundanceCatchable = abundanceCatchable;
    }
}
