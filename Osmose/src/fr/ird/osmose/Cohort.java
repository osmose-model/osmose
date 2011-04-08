package fr.ird.osmose;

/*******************************************************************************
 * <p>Titre : Cohort class</p>
 *
 * <p>Description : groups the super-individuals (School). represents a cohort (not annual but for a tims step)</p>
 *
 * <p>Copyright : Copyright (c) may 2009 </p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 ********************************************************************************
 */
import java.util.*;

public class Cohort extends ArrayList<School> {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/08 phv
     * Deleted vector vectCatchableSchools. It is faster to loop on the whole
     * list of schools and check wether they are catchable or not.
     * 2011/04/07 phv
     * Osmose and Simulation are called with Osmose.getInstance()
     * and Osmose.getInstance().getSimulation()
     * Deleted vector presentSchools. Cohort now extends ArrayList<School>
     * Deleted variable nbSchools. Replaced by this.size()
     * ***
     */
    Species species;
    int numSerie;
    int ageNbDt;    //age in nbDt
    long abundance, oldAbundance;
    double biomass;		//biomass in tonnes
    float meanLength;
    float meanWeight;
    float[] outOfZoneMortality;
    boolean[] outOfZoneCohort;
    float[] outOfZonePercentage;
    float Z, Dd, Ff, Pp, Ss;   //effective mortalities tot(Z),div(D),fishing(F),preda(P),starva(S)
    long nbDead, nbDeadDd, nbDeadFf, nbDeadPp, nbDeadSs;
    int nbSchoolsCatchable;
    long abundanceCatchable;

    public Cohort(Species species, int ageNbDt, long abundance, double biomass,
            float iniLength, float iniWeight) {
        this.species = species;
        this.numSerie = species.numSerie;
        this.ageNbDt = ageNbDt;

        outOfZoneMortality = new float[getSimulation().nbDt];
        outOfZoneCohort = new boolean[getSimulation().nbDt];
        outOfZonePercentage = new float[getSimulation().nbDt];

        for (int i = 0; i < getSimulation().nbDt; i++) {
            // initialization by default
            outOfZoneMortality[i] = 0;
            outOfZoneCohort[i] = false;
            outOfZonePercentage[i] = 0;
        }

        this.abundance = abundance;
        this.oldAbundance = abundance;
        this.biomass = biomass;

//		nbSchools = getOsmose().nbSchools[numSerie];

        int nbSchools = (int) (1 + 10 / (species.longevity + 1)) * getOsmose().nbSchools[numSerie];
        ensureCapacity(nbSchools);
        for (int i = 0; i < nbSchools; i++) {
            add(new School(this, Math.round(((double) abundance) / nbSchools), iniLength, iniWeight));
        }

        int surplus = (int) abundance % nbSchools;
        get(0).setAbundance(get(0).getAbundance() + surplus);
        calculMeanGrowth();
    }

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    public void surviveD(float D) {
        long oldAbd = abundance;
        abundance = Math.round(oldAbd * Math.exp(-D));      // D is already divided by the time step in Qsimulation
        long nbDeadTemp = oldAbd - abundance;
        nbDeadDd += nbDeadTemp;

        int nbSchools = size();
        long nbSurplusDead = nbDeadTemp % nbSchools;

        //NB of DEAD FISH are DISTRIBUTED UNIFORMLY
        for (int i = 0; i < nbSchools; i++) {
            if (((School) get(i)).getAbundance() > Math.round(((double) nbDeadTemp) / nbSchools)) {
                ((School) get(i)).setAbundance(((School) get(i)).getAbundance() - Math.round(((double) nbDeadTemp) / nbSchools));
            } else {
                nbSurplusDead += Math.round(((double) nbDeadTemp) / nbSchools)
                        - ((School) get(i)).getAbundance();
                ((School) get(i)).setAbundance(0);
                ((School) get(i)).tagForRemoval();
            }
        }

        //SURPLUS of DEAD are DISTRIBUTED
        int index = 0;
        while ((nbSurplusDead != 0) && (index < size())) {
            if (((School) get(index)).getAbundance() > nbSurplusDead) {
                ((School) get(index)).setAbundance(((School) get(index)).getAbundance() - nbSurplusDead);
                nbSurplusDead = 0;
            } else {
                nbSurplusDead -= ((School) get(index)).getAbundance();
                ((School) get(index)).tagForRemoval();
                ((School) get(index)).setAbundance(0);
            }
            index++;
        }

        //REMOVING DEAD SCHOOLS FROM VECTBANCS and VECTPRESENTSCHOOLS
        List<School> schoolsToRemove = new ArrayList();
        for (School school : this) {
            if (school.willDisappear()) {
                // cohorts in the area during the time step
                if (!(outOfZoneCohort[getSimulation().dt])) {
                    school.getCell().remove(school);
                }
                schoolsToRemove.add(school);
            }
        }
        removeAll(schoolsToRemove);

        //UPDATE biomass of schools & cohort
        biomass = 0;
        for (int i = 0; i < size(); i++) {
            ((School) get(i)).setBiomass(((double) ((School) get(i)).getAbundance()) * ((School) get(i)).getWeight() / 1000000.);
            biomass += ((School) get(i)).getBiomass();
        }
    }

    public long fishing1(float F) // indicator to be checked
    {
        long oldAbdCatch1 = abundance;
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
            if (school.isCatchable()) {
                // Vector of the length of fish of schools caught -> indicator Morgane 07-2004
                getSimulation().tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = school.getLength();

                if (school.getAbundance() > Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable)) //case: enough fish in the school
                {
                    Yi += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) * ((double) school.getWeight()) / 1000000.;
                    school.setAbundance(school.getAbundance() - Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable));
                    // Vector of the number of fish of schools caught  -> indicator  MORGANE 07-2004
                    getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable);
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * Math.round(((float) nbDeadFfTheo) / nbSchoolsCatchable) * ((float) school.getWeight()) / 1000000f;
                    }
                } else //case: not enough fish in the school
                {
                    nbSurplusDead += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) - school.getAbundance();
                    Yi += ((double) school.getAbundance()) * school.getWeight() / 1000000.;
                    // Vector of the number of fish of schools caught   -> indicator   MORGANE 07-2004
                    getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) school.getAbundance();
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) school.getAbundance()) * school.getWeight() / 1000000f;
                    }
                    school.setAbundance(0);
                    school.tagForRemoval();
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
                    Yi += ((double) nbSurplusDead) * school.getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) nbSurplusDead) * school.getWeight() / 1000000f;
                    }
                    nbSurplusDead = 0;
                } else {
                    nbSurplusDead -= school.getAbundance();
                    Yi += ((double) school.getAbundance()) * school.getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) school.getAbundance();
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) school.getAbundance()) * school.getWeight() / 1000000f;
                    }
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
                if (!outOfZoneCohort[getSimulation().dt]) {
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
            school.setBiomass(((double) school.getAbundance()) * school.getWeight() / 1000000.);
            abundance += school.getAbundance();
            if (school.isCatchable()) {
                abundanceCatchable += school.getAbundance();
            }
        }
        nbDeadFf = oldAbdCatch1 - abundance;

        // save of annual yield
        if ((getSimulation().t) >= getOsmose().timeSeriesStart) {
            getSimulation().savingYield[species.number - 1] += Yi;
            //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            for (k = 0; k < nbSchoolsCatchable; k++) {
                getSimulation().savingNbYield[species.number - 1] += getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]];
            }
        }
        return nbSurplusDead;
    }

    public void fishing2(float Freal) // Same F per species   + indicator to be checked
    {
        double Yi = 0;
        if (nbSchoolsCatchable != 0) {
            nbDeadFf = Math.round(abundanceCatchable * (1 - Math.exp(-Freal)));  //time step taken in account through seasonnality
            long nbSurplusDead;
            nbSurplusDead = (long) (nbDeadFf % nbSchoolsCatchable);

            //FIRST, NB DEAD are DISTRIBUTED UNIFORMLY
            int k = 0;
            for (School school : this) {
                if (school.isCatchable()) {
                    //MORGANE 07-2004	    // Vector of the length of fish of schools caught
                    getSimulation().tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = school.getLength();
                    if (school.getAbundance() > Math.round(((double) nbDeadFf) / nbSchoolsCatchable)) {
                        Yi += Math.round(((double) nbDeadFf) / nbSchoolsCatchable) * ((double) school.getWeight()) / 1000000.;
                        school.setAbundance(school.getAbundance() - Math.round(((double) nbDeadFf) / nbSchoolsCatchable));
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) (nbDeadFf / nbSchoolsCatchable);
                        if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                            getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * Math.round(((float) nbDeadFf) / nbSchoolsCatchable) * ((float) school.getWeight()) / 1000000f;
                        }
                    } else {
                        nbSurplusDead += Math.round(((double) nbDeadFf) / nbSchoolsCatchable) - school.getAbundance();
                        Yi += ((double) school.getAbundance()) * school.getWeight() / 1000000.;
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) school.getAbundance();
                        if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                            getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) school.getAbundance()) * school.getWeight() / 1000000f;
                        }
                        school.setAbundance(0);
                        school.tagForRemoval();
                    }
                    k++;
                }
            }
            //SURPLUS of DEAD is DISTRIBUTED
            int index = 0;
            Iterator<School> iterator = iterator();
            while ((nbSurplusDead != 0) && iterator.hasNext()) {
                School school = iterator.next();
                if (school.isCatchable()) {
                    if (school.getAbundance() > nbSurplusDead) {
                        school.setAbundance(school.getAbundance() - nbSurplusDead);
                        Yi += ((double) nbSurplusDead) * school.getWeight() / 1000000.;
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                        if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                            getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) nbSurplusDead) * school.getWeight() / 1000000f;
                        }
                        nbSurplusDead = 0;
                    } else {
                        nbSurplusDead -= school.getAbundance();
                        Yi += ((double) school.getAbundance())
                                * school.getWeight() / 1000000.;
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) school.getAbundance();
                        getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += school.getTrophicLevel()[ageNbDt] * ((float) school.getAbundance()) * school.getWeight() / 1000000f;
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
                    if (!outOfZoneCohort[getSimulation().dt]) {
                        school.getCell().remove(school);
                    }
                    schoolsToRemove.add(school);
                    nbSchoolsCatchable--;
                }
            }
            removeAll(schoolsToRemove);

            //UPDATE schools biomasss & cohort abd
            abundance -= nbDeadFf;
            abundanceCatchable -= nbDeadFf;
            for (School school : this) {
                if (school.isCatchable()) {
                    school.setBiomass(((double) school.getAbundance()) * school.getWeight() / 1000000.);
                }
            }
        }
        if ((getSimulation().t) >= getOsmose().timeSeriesStart) {
            getSimulation().savingYield[species.number - 1] += Yi;
            //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            for (int k = 0; k < nbSchoolsCatchable; k++) {
                getSimulation().savingNbYield[species.number - 1] += getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]];
            }

        }
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
                    //MORGANE 07-2004	    // Vector of the length of fish of schools caught
                    getSimulation().tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = school.getLength();
                    if (school.getAbundance() > Math.round(((double) abdToCatch) / nbSchoolsCatchable)) {
                        school.setAbundance(school.getAbundance() - Math.round(((double) abdToCatch) / nbSchoolsCatchable));
                        Yi += Math.round(((double) abdToCatch) / nbSchoolsCatchable)
                                * ((double) school.getWeight()) / 1000000.;
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) abdToCatch / nbSchoolsCatchable;
                        if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                            getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * Math.round(((float) abdToCatch) / nbSchoolsCatchable)
                                    * ((float) school.getWeight()) / 1000000f;
                        }
                    } else {
                        nbSurplusDead += Math.round(((double) abdToCatch) / nbSchoolsCatchable) - school.getAbundance();
                        Yi += ((double) school.getAbundance()) * school.getWeight() / 1000000.;
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) school.getAbundance();
                        if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                            getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) school.getAbundance()) * school.getWeight() / 1000000f;
                        }
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
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                        if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                            getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) nbSurplusDead) * school.getWeight() / 1000000f;
                        }
                        nbSurplusDead = 0;
                    } else {
                        nbSurplusDead -= school.getAbundance();
                        Yi += ((double) school.getAbundance())
                                * school.getWeight() / 1000000.;
                        //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                        getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) school.getAbundance();
                        if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                            getSimulation().tabTLCatch[species.number - 1] += school.getTrophicLevel()[ageNbDt] * ((float) school.getAbundance())
                                    * school.getWeight() / 1000000f;
                        }
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
                    if (!outOfZoneCohort[getSimulation().dt]) {
                        school.getCell().remove(school);
                    }
                    schoolsToRemove.add(school);
                    nbSchoolsCatchable--;
                }
            }
            removeAll(schoolsToRemove);

            //UPDATE schools biomass & cohort abd
            abundance = 0;
            for (School school : this) {
                school.setBiomass(((double) school.getAbundance()) * school.getWeight() / 1000000.);
                abundance += school.getAbundance();
            }
            nbDeadFf += abdToCatch;
        }
        if ((getSimulation().t) >= getOsmose().timeSeriesStart) {
            getSimulation().savingYield[species.number - 1] += Yi;                                       // ********saving of indicators to be done

            for (int k = 0; k < nbSchoolsCatchable; k++) //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            {
                getSimulation().savingNbYield[species.number - 1] += getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]];
            }

        }
    }

    public void growth(float minDelta, float maxDelta, float c, float bPower) {
        for (int i = 0; i < size(); i++) {
            ((School) get(i)).growth(minDelta, maxDelta, c, bPower);
        }
    }

    public void calculMeanGrowth() {
        double sumLengths = 0;
        double sumWeights = 0;
        long count = 0;
        for (int i = 0; i < size(); i++) {
            School schooli = (School) get(i);
            sumLengths += ((double) schooli.getLength()) * schooli.getAbundance();
            sumWeights += ((double) schooli.getWeight()) * schooli.getAbundance();
            count += schooli.getAbundance();
        }
        meanLength = (float) sumLengths / count;
        meanWeight = (float) sumWeights / count;
    }
}
