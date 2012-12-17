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
import java.util.Iterator;
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
    
    public void surviveD(float D) {
        double oldAbd = abundance;
        abundance = Math.round(oldAbd * Math.exp(-D));      // D is already divided by the time step in Qsimulation
        double nbDeadTemp = oldAbd - abundance;
        nbDeadDd += nbDeadTemp;

        int nbSchools = size();
        double nbSurplusDead = Math.round(nbDeadTemp % nbSchools);

        //NB of DEAD FISH are DISTRIBUTED UNIFORMLY
        for (int i = 0; i < nbSchools; i++) {
            if (getSchool(i).getAbundance() > Math.round(((double) nbDeadTemp) / nbSchools)) {
                getSchool(i).setAbundance(getSchool(i).getAbundance() - Math.round(((double) nbDeadTemp) / nbSchools));
            } else {
                nbSurplusDead += Math.round(((double) nbDeadTemp) / nbSchools)
                        - getSchool(i).getAbundance();
                getSchool(i).setAbundance(0);
                getSchool(i).tagForRemoval();
            }
        }

        //SURPLUS of DEAD are DISTRIBUTED
        int index = 0;
        while ((nbSurplusDead != 0) && (index < size())) {
            if (getSchool(index).getAbundance() > nbSurplusDead) {
                getSchool(index).setAbundance(getSchool(index).getAbundance() - nbSurplusDead);
                nbSurplusDead = 0;
            } else {
                nbSurplusDead -= getSchool(index).getAbundance();
                getSchool(index).tagForRemoval();
                getSchool(index).setAbundance(0);
            }
            index++;
        }

        //REMOVING DEAD SCHOOLS FROM VECTBANCS and VECTPRESENTSCHOOLS
        removeDeadSchools();

        //UPDATE biomass of schools & cohort
        biomass = 0;
        abundance = 0;
        for (School school : this) {
            biomass += school.getBiomass();
            abundance += school.getAbundance();
        }
    }
    
    public long fishing1(float F) // indicator to be checked
    {
        double oldAbdCatch1 = abundance;
        long nbDeadFfTheo = Math.round(oldAbdCatch1 * (1 - Math.exp(-F)));   // F is already scaled to the time step trough seasonality
        //nbDeadFf = captures en nombre

        long nbSurplusDead;
        if (nbSchoolsCatchable == 0) {
            nbSurplusDead = nbDeadFfTheo;
        } else {
            nbSurplusDead = nbDeadFfTheo % nbSchoolsCatchable;
        }
        double Yi = 0;

        //----FIRST, DEAD FISH ARE DISTRIBUTED UNIFORMLY----
        int k = 0;
        for (School school : this) {
            school.catches = 0.f;
            if (school.isCatchable()) {

                if (school.getAbundance() > Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable)) //case: enough fish in the school
                {
                    Yi += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) * ((double) school.getWeight()) / 1000000.;
                    school.setAbundance(school.getAbundance() - Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable));
                    school.catches = Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable);
                } else //case: not enough fish in the school
                {
                    nbSurplusDead += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) - school.getAbundance();
                    Yi += ((double) school.getAbundance()) * school.getWeight() / 1000000.;
                    school.setAbundance(0);
                    school.tagForRemoval();
                    school.catches = school.getAbundance();
                }
                k++;
            }
        }

        //----SURPLUS of DEAD FISH ARE DISTRIBUTED----
        int index = 0;
        Iterator<School> iterator = iterator();
        while ((nbSurplusDead != 0) && iterator.hasNext()) {
            School school = iterator.next();
            if (school.isCatchable()) {
                if (school.getAbundance() > nbSurplusDead) {
                    school.setAbundance(school.getAbundance() - nbSurplusDead);
                    school.catches += nbSurplusDead;
                    Yi += ((double) nbSurplusDead) * school.getWeight() / 1000000.;
                    nbSurplusDead = 0;
                } else {
                    nbSurplusDead -= school.getAbundance();
                    Yi += ((double) school.getAbundance()) * school.getWeight() / 1000000.;
                    school.tagForRemoval();
                    school.setAbundance(0);
                    school.catches += school.getAbundance();
                }
                index++;
            }

        }

        //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
        List<School> schoolsToRemove = new ArrayList();
        for (School school : this) {
            if (school.isCatchable() && school.willDisappear()) {
                if (!outOfZoneCohort[getSimulation().getIndexTimeYear()]) {
                    school.getCell().remove(school);
                }
                schoolsToRemove.add(school);
                nbSchoolsCatchable--;
            }
        }
        removeAll(schoolsToRemove);

        //UPDATE biomass of schools & cohort abd
        abundance = 0;
        abundanceCatchable = 0;

        for (School school : this) {
            abundance += school.getAbundance();
            if (school.isCatchable()) {
                abundanceCatchable += school.getAbundance();
            }
        }
        nbDeadFf = (long) (oldAbdCatch1 - abundance);

        return nbSurplusDead;
    }
    
    public void fishingSurplus(long abdToCatch) //indicator to be checked
    {
        double Yi = 0;
        if (nbSchoolsCatchable != 0) {
            long nbSurplusDead;
            nbSurplusDead = abdToCatch % nbSchoolsCatchable;

            //FIRST, DEAD NB are UNIFORMLY DISTRIBUTED
            int k = 0;
            for (School school : this) {
                if (school.isCatchable()) {
                    if (school.getAbundance() > Math.round(((double) abdToCatch) / nbSchoolsCatchable)) {
                        school.setAbundance(school.getAbundance() - Math.round(((double) abdToCatch) / nbSchoolsCatchable));
                        Yi += Math.round(((double) abdToCatch) / nbSchoolsCatchable)
                                * ((double) school.getWeight()) / 1000000.;
                    } else {
                        nbSurplusDead += Math.round(((double) abdToCatch) / nbSchoolsCatchable) - school.getAbundance();
                        Yi += ((double) school.getAbundance()) * school.getWeight() / 1000000.;
                        school.setAbundance(0);
                        school.tagForRemoval();
                    }
                    k++;
                }
            }

            //SURPLUS DEAD are DISTRIBUTED
            int index = 0;
            Iterator<School> iterator = iterator();
            while ((nbSurplusDead != 0) && iterator.hasNext()) {
                School school = iterator.next();
                if (school.isCatchable()) {
                    if (school.getAbundance() > nbSurplusDead) {
                        school.setAbundance(school.getAbundance() - nbSurplusDead);
                        Yi += ((double) nbSurplusDead) * school.getWeight() / 1000000.;
                        nbSurplusDead = 0;
                    } else {
                        nbSurplusDead -= school.getAbundance();
                        Yi += ((double) school.getAbundance())
                                * school.getWeight() / 1000000.;
                        school.tagForRemoval();
                        school.setAbundance(0);
                    }
                    index++;
                }
            }
            //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
            List<School> schoolsToRemove = new ArrayList();
            for (School school : this) {
                if (school.isCatchable() && school.willDisappear()) {
                    if (!outOfZoneCohort[getSimulation().getIndexTimeYear()]) {
                        school.getCell().remove(school);
                    }
                    schoolsToRemove.add(school);
                    nbSchoolsCatchable--;
                }
            }
            removeAll(schoolsToRemove);

            //UPDATE cohort abd
            abundance = 0;
            for (School school : this) {
                abundance += school.getAbundance();
            }
            nbDeadFf += abdToCatch;
        }
    }
}
