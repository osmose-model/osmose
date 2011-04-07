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
    Vector schoolsCatchable;
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
        for (int i = size() - 1; i >= 0; i--) {
            if (((School) get(i)).willDisappear()) {
                if (!(outOfZoneCohort[getSimulation().dt]))// cohorts in the area during the time step
                {
                    ((School) get(i)).getCell().remove(get(i));
                }
                remove(i);
                nbSchools--;
            }
        }

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

        for (int k = 0; k < nbSchoolsCatchable; k++) {
            School schoolCatchk = (School) schoolsCatchable.elementAt(k);
            // Vector of the length of fish of schools caught -> indicator Morgane 07-2004
            getSimulation().tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = schoolCatchk.getLength();

            if (schoolCatchk.getAbundance() > Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable)) //case: enough fish in the school
            {
                Yi += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) * ((double) schoolCatchk.getWeight()) / 1000000.;
                schoolCatchk.setAbundance(schoolCatchk.getAbundance() - Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable));
                // Vector of the number of fish of schools caught  -> indicator  MORGANE 07-2004
                getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable);
                if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                    getSimulation().tabTLCatch[species.number - 1] += schoolCatchk.getTrophicLevel()[ageNbDt] * Math.round(((float) nbDeadFfTheo) / nbSchoolsCatchable) * ((float) schoolCatchk.getWeight()) / 1000000f;
                }
            } else //case: not enough fish in the school
            {
                nbSurplusDead += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) - schoolCatchk.getAbundance();
                Yi += ((double) schoolCatchk.getAbundance()) * schoolCatchk.getWeight() / 1000000.;
                // Vector of the number of fish of schools caught   -> indicator   MORGANE 07-2004
                getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) schoolCatchk.getAbundance();
                if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                    getSimulation().tabTLCatch[species.number - 1] += schoolCatchk.getTrophicLevel()[ageNbDt] * ((float) schoolCatchk.getAbundance()) * schoolCatchk.getWeight() / 1000000f;
                }
                schoolCatchk.setAbundance(0);
                schoolCatchk.tagForRemoval();
            }
        }

        //----SURPLUS of DEAD FISH ARE DISTRIBUTED----
        int index = 0;
        while ((nbSurplusDead != 0) && (index < schoolsCatchable.size())) {
            if (((School) schoolsCatchable.elementAt(index)).getAbundance() > nbSurplusDead) {
                ((School) schoolsCatchable.elementAt(index)).setAbundance(((School) schoolsCatchable.elementAt(index)).getAbundance() - nbSurplusDead);
                Yi += ((double) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000.;
                //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                    getSimulation().tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).getTrophicLevel()[ageNbDt] * ((float) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000f;
                }
                nbSurplusDead = 0;
            } else {
                nbSurplusDead -= ((School) schoolsCatchable.elementAt(index)).getAbundance();
                Yi += ((double) ((School) schoolsCatchable.elementAt(index)).getAbundance()) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000.;
                //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) ((School) schoolsCatchable.elementAt(index)).getAbundance();
                if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                    getSimulation().tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).getTrophicLevel()[ageNbDt] * ((float) ((School) schoolsCatchable.elementAt(index)).getAbundance()) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000f;
                }
                ((School) schoolsCatchable.elementAt(index)).tagForRemoval();
                ((School) schoolsCatchable.elementAt(index)).setAbundance(0);
            }
            index++;
        }

        //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
        for (int k = schoolsCatchable.size() - 1; k >= 0; k--) {
            School schoolCatchk = (School) schoolsCatchable.elementAt(k);
            if (schoolCatchk.willDisappear()) {
                if (!outOfZoneCohort[getSimulation().dt]) {
                    schoolCatchk.getCell().remove(schoolCatchk);
                }
                remove(schoolCatchk);
                schoolsCatchable.removeElementAt(k);
                nbSchoolsCatchable--;
            }
        }
        //UPDATE biomass of schools & cohort abd
        abundance = 0;
        abundanceCatchable = 0;
        for (int k = 0; k < size(); k++) {
            School schoolk = (School) get(k);
            schoolk.setBiomass(((double) schoolk.getAbundance()) * schoolk.getWeight() / 1000000.);
            abundance += schoolk.getAbundance();
            if (schoolk.isCatchable()) {
                abundanceCatchable += schoolk.getAbundance();
            }
        }
        nbDeadFf = oldAbdCatch1 - abundance;

        // save of annual yield
        if ((getSimulation().t) >= getOsmose().timeSeriesStart) {
            getSimulation().savingYield[species.number - 1] += Yi;
            for (int k = 0; k < nbSchoolsCatchable; k++) //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            {
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
            for (int k = 0; k < nbSchoolsCatchable; k++) {
                School schoolCatchk = (School) schoolsCatchable.elementAt(k);
                //MORGANE 07-2004	    // Vector of the length of fish of schools caught
                getSimulation().tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = schoolCatchk.getLength();
                if (schoolCatchk.getAbundance() > Math.round(((double) nbDeadFf) / nbSchoolsCatchable)) {
                    Yi += Math.round(((double) nbDeadFf) / nbSchoolsCatchable) * ((double) schoolCatchk.getWeight()) / 1000000.;
                    schoolCatchk.setAbundance(schoolCatchk.getAbundance() - Math.round(((double) nbDeadFf) / nbSchoolsCatchable));
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) (nbDeadFf / nbSchoolsCatchable);
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += schoolCatchk.getTrophicLevel()[ageNbDt] * Math.round(((float) nbDeadFf) / nbSchoolsCatchable) * ((float) schoolCatchk.getWeight()) / 1000000f;
                    }
                } else {
                    nbSurplusDead += Math.round(((double) nbDeadFf) / nbSchoolsCatchable) - schoolCatchk.getAbundance();
                    Yi += ((double) schoolCatchk.getAbundance()) * schoolCatchk.getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) schoolCatchk.getAbundance();
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += schoolCatchk.getTrophicLevel()[ageNbDt] * ((float) schoolCatchk.getAbundance()) * schoolCatchk.getWeight() / 1000000f;
                    }
                    schoolCatchk.setAbundance(0);
                    schoolCatchk.tagForRemoval();
                }
            }
            //SURPLUS of DEAD is DISTRIBUTED
            int index = 0;
            while ((nbSurplusDead != 0) && (index < schoolsCatchable.size())) {
                if (((School) schoolsCatchable.elementAt(index)).getAbundance() > nbSurplusDead) {
                    ((School) schoolsCatchable.elementAt(index)).setAbundance(((School) schoolsCatchable.elementAt(index)).getAbundance() - nbSurplusDead);
                    Yi += ((double) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).getTrophicLevel()[ageNbDt] * ((float) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000f;
                    }
                    nbSurplusDead = 0;
                } else {
                    nbSurplusDead -= ((School) schoolsCatchable.elementAt(index)).getAbundance();
                    Yi += ((double) ((School) schoolsCatchable.elementAt(index)).getAbundance())
                            * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) ((School) schoolsCatchable.elementAt(index)).getAbundance();
                    getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += ((School) schoolsCatchable.elementAt(index)).getTrophicLevel()[ageNbDt] * ((float) ((School) schoolsCatchable.elementAt(index)).getAbundance())
                            * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000f;
                    ((School) schoolsCatchable.elementAt(index)).tagForRemoval();
                    ((School) schoolsCatchable.elementAt(index)).setAbundance(0);
                }
                index++;
            }
            //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
            for (int k = schoolsCatchable.size() - 1; k >= 0; k--) {
                School schoolCatchk = (School) schoolsCatchable.elementAt(k);
                if (schoolCatchk.willDisappear()) {
                    if (!outOfZoneCohort[getSimulation().dt]) {
                        schoolCatchk.getCell().remove(schoolCatchk);
                    }
                    remove(schoolCatchk);
                    schoolsCatchable.removeElementAt(k);
                    nbSchoolsCatchable--;
                }
            }
            //UPDATE schools biomasss & cohort abd
            abundance -= nbDeadFf;
            abundanceCatchable -= nbDeadFf;
            for (int i = 0; i < schoolsCatchable.size(); i++) {
                ((School) schoolsCatchable.elementAt(i)).setBiomass(((double) ((School) schoolsCatchable.elementAt(i)).getAbundance()) * ((School) schoolsCatchable.elementAt(i)).getWeight() / 1000000.);
            }
        }
        if ((getSimulation().t) >= getOsmose().timeSeriesStart) {
            getSimulation().savingYield[species.number - 1] += Yi;
            for (int k = 0; k < nbSchoolsCatchable; k++) //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            {
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
            for (int k = 0; k < nbSchoolsCatchable; k++) {
                School schoolCatchk = (School) schoolsCatchable.elementAt(k);
                //MORGANE 07-2004	    // Vector of the length of fish of schools caught
                getSimulation().tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = schoolCatchk.getLength();
                if (schoolCatchk.getAbundance() > Math.round(((double) abdToCatch) / nbSchoolsCatchable)) {
                    schoolCatchk.setAbundance(schoolCatchk.getAbundance() - Math.round(((double) abdToCatch) / nbSchoolsCatchable));
                    Yi += Math.round(((double) abdToCatch) / nbSchoolsCatchable)
                            * ((double) schoolCatchk.getWeight()) / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) abdToCatch / nbSchoolsCatchable;
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += schoolCatchk.getTrophicLevel()[ageNbDt] * Math.round(((float) abdToCatch) / nbSchoolsCatchable)
                                * ((float) schoolCatchk.getWeight()) / 1000000f;
                    }
                } else {
                    nbSurplusDead += Math.round(((double) abdToCatch) / nbSchoolsCatchable) - schoolCatchk.getAbundance();
                    Yi += ((double) schoolCatchk.getAbundance()) * schoolCatchk.getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) schoolCatchk.getAbundance();
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += schoolCatchk.getTrophicLevel()[ageNbDt] * ((float) schoolCatchk.getAbundance()) * schoolCatchk.getWeight() / 1000000f;
                    }
                    schoolCatchk.setAbundance(0);
                    schoolCatchk.tagForRemoval();
                }
            }
            //SURPLUS DEAD are DISTRIBUTED
            int index = 0;
            while ((nbSurplusDead != 0) && (index < schoolsCatchable.size())) {
                if (((School) schoolsCatchable.elementAt(index)).getAbundance() > nbSurplusDead) {
                    ((School) schoolsCatchable.elementAt(index)).setAbundance(((School) schoolsCatchable.elementAt(index)).getAbundance() - nbSurplusDead);
                    Yi += ((double) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).getTrophicLevel()[ageNbDt] * ((float) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000f;
                    }
                    nbSurplusDead = 0;
                } else {
                    nbSurplusDead -= ((School) schoolsCatchable.elementAt(index)).getAbundance();
                    Yi += ((double) ((School) schoolsCatchable.elementAt(index)).getAbundance())
                            * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    getSimulation().tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) ((School) schoolsCatchable.elementAt(index)).getAbundance();
                    if ((getSimulation().TLoutput) && (getSimulation().t >= getOsmose().timeSeriesStart)) {
                        getSimulation().tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).getTrophicLevel()[ageNbDt] * ((float) ((School) schoolsCatchable.elementAt(index)).getAbundance())
                                * ((School) schoolsCatchable.elementAt(index)).getWeight() / 1000000f;
                    }
                    ((School) schoolsCatchable.elementAt(index)).tagForRemoval();
                    ((School) schoolsCatchable.elementAt(index)).setAbundance(0);
                }
                index++;
            }
            //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
            for (int k = schoolsCatchable.size() - 1; k >= 0; k--) {
                School schoolCatchk = (School) schoolsCatchable.elementAt(k);
                if (schoolCatchk.willDisappear()) {
                    if (!outOfZoneCohort[getSimulation().dt]) {
                        schoolCatchk.getCell().remove(schoolCatchk);
                    }
                    remove(schoolCatchk);
                }
            }
            //UPDATE schools biomass & cohort abd
            abundance = 0;
            for (int i = 0; i < size(); i++) {
                ((School) get(i)).setBiomass(((double) ((School) get(i)).getAbundance()) * ((School) get(i)).getWeight() / 1000000.);
                abundance += ((School) get(i)).getAbundance();
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
