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
     * Age of the cohort expressed in number of time steps.
     */
    private int ageNbDt;
    /*
     * Abundance of the cohort, number of individuals
     */
    private double abundance;
    /*
     * Biomass [ton] of the cohort
     */
    private double biomass;
    /*
     * Mean length [cm] of the schools in the cohort
     */
    private float meanLength;
    /*
     * Mean weight [g] of the schools in the cohort
     */
    private float meanWeight;
    /*
     *
     */
    private float[] outOfZoneMortality;
    private boolean[] outOfZoneCohort;
    private float[] outOfZonePercentage;
    private float Z;
    private float Dd;
    private float Ff;
    private float Pp;
    private float Ss;
    private long nbDead;
    long nbDeadDd;
    long nbDeadFf;
    private long nbDeadPp;
    private long nbDeadSs;
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
        this.ageNbDt = ageNbDt;

        outOfZoneMortality = new float[getSimulation().getNbTimeStepsPerYear()];
        outOfZoneCohort = new boolean[getSimulation().getNbTimeStepsPerYear()];
        outOfZonePercentage = new float[getSimulation().getNbTimeStepsPerYear()];

        for (int i = 0; i < getSimulation().getNbTimeStepsPerYear(); i++) {
            // initialization by default
            outOfZoneMortality[i] = 0;
            outOfZoneCohort[i] = false;
            outOfZonePercentage[i] = 0;
        }

        this.abundance = abundance;
        this.biomass = biomass;
        if (biomass > 0.d) {
            int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
            ensureCapacity(nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                add(new School(this, abundance / nbSchools, iniLength, iniWeight));
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

    public void calculMeanGrowth() {
        double sumLengths = 0;
        double sumWeights = 0;
        long count = 0;
        for (int i = 0; i < size(); i++) {
            School schooli = getSchool(i);
            sumLengths += ((double) schooli.getLength()) * schooli.getAbundance();
            sumWeights += ((double) schooli.getWeight()) * schooli.getAbundance();
            count += schooli.getAbundance();
        }
        meanLength = (float) sumLengths / count;
        meanWeight = (float) sumWeights / count;
    }

    public void upperAgeClass(Cohort upperAgerCohort) {
        clear();
        addAll(upperAgerCohort);
        abundance = upperAgerCohort.getAbundance();
        biomass = upperAgerCohort.getBiomass();
        nbDead = upperAgerCohort.getNbDead();
        nbDeadDd = upperAgerCohort.getNbDeadDd();
        nbDeadPp = upperAgerCohort.getNbDeadPp();
        nbDeadSs = upperAgerCohort.getNbDeadSs();
        nbDeadFf = upperAgerCohort.getNbDeadFf();
        Z = upperAgerCohort.getZ();
        Dd = upperAgerCohort.getDd();
        Pp = upperAgerCohort.getPp();
        Ss = upperAgerCohort.getSs();
        Ff = upperAgerCohort.getFf();
    }

    /**
     * @return the species
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * @return the ageNbDt
     */
    public int getAgeNbDt() {
        return ageNbDt;
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
     * @return the outOfZonePercentage
     */
    public float[] getOutOfZonePercentage() {
        return outOfZonePercentage;
    }

    /**
     * @return the Z
     */
    public float getZ() {
        return Z;
    }

    /**
     * @return the Dd
     */
    public float getDd() {
        return Dd;
    }

    /**
     * @return the Ff
     */
    public float getFf() {
        return Ff;
    }

    /**
     * @return the Pp
     */
    public float getPp() {
        return Pp;
    }

    /**
     * @return the Ss
     */
    public float getSs() {
        return Ss;
    }

    /**
     * @return the nbDead
     */
    public long getNbDead() {
        return nbDead;
    }

    /**
     * @return the nbDeadDd
     */
    public long getNbDeadDd() {
        return nbDeadDd;
    }

    /**
     * @param nbDeadDd the nbDeadDd to set
     */
    public void setNbDeadDd(long nbDeadDd) {
        this.nbDeadDd = nbDeadDd;
    }

    /**
     * @return the nbDeadFf
     */
    public long getNbDeadFf() {
        return nbDeadFf;
    }

    /**
     * @param nbDeadFf the nbDeadFf to set
     */
    public void setNbDeadFf(long nbDeadFf) {
        this.nbDeadFf = nbDeadFf;
    }

    /**
     * @return the nbDeadPp
     */
    public long getNbDeadPp() {
        return nbDeadPp;
    }

    /**
     * @param nbDeadPp the nbDeadPp to set
     */
    public void setNbDeadPp(long nbDeadPp) {
        this.nbDeadPp = nbDeadPp;
    }

    /**
     * @return the nbDeadSs
     */
    public long getNbDeadSs() {
        return nbDeadSs;
    }

    /**
     * @param nbDeadSs the nbDeadSs to set
     */
    public void setNbDeadSs(long nbDeadSs) {
        this.nbDeadSs = nbDeadSs;
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
