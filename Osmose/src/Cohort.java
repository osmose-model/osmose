
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

class Cohort {

    Species species;
    int numSerie;
    int ageNbDt;    //age in nbDt
    int nbSchools;
    Vector vectSchools;
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

        outOfZoneMortality = new float[species.simulation.nbDt];
        outOfZoneCohort = new boolean[species.simulation.nbDt];
        outOfZonePercentage = new float[species.simulation.nbDt];

        for (int i = 0; i < species.simulation.nbDt; i++) {
            // initialization by default
            outOfZoneMortality[i] = 0;
            outOfZoneCohort[i] = false;
            outOfZonePercentage[i] = 0;
        }

        this.abundance = abundance;
        this.oldAbundance = abundance;
        this.biomass = biomass;

//		nbSchools = species.simulation.osmose.nbSchools[numSerie];

        nbSchools = (int) (1 + 10 / (species.longevity + 1)) * species.simulation.osmose.nbSchools[numSerie];
        vectSchools = new Vector(nbSchools);
        for (int i = 0; i < nbSchools; i++) {
            vectSchools.addElement(new School(this, Math.round(((double) abundance) / nbSchools), iniLength, iniWeight));
        }

        int surplus = (int) abundance % nbSchools;
        ((School) (vectSchools.elementAt(0))).abundance += surplus;
        calculMeanGrowth();
    }

    public void surviveD(float D) {
        long oldAbd = abundance;
        abundance = Math.round(oldAbd * Math.exp(-D));      // D is already divided by the time step in Qsimulation
        long nbDeadTemp = oldAbd - abundance;
        nbDeadDd += nbDeadTemp;

        long nbSurplusDead = nbDeadTemp % nbSchools;

        //NB of DEAD FISH are DISTRIBUTED UNIFORMLY
        for (int i = 0; i < nbSchools; i++) {
            if (((School) vectSchools.elementAt(i)).abundance > Math.round(((double) nbDeadTemp) / nbSchools)) {
                ((School) vectSchools.elementAt(i)).abundance -= Math.round(((double) nbDeadTemp) / nbSchools);
            } else {
                nbSurplusDead += Math.round(((double) nbDeadTemp) / nbSchools)
                        - ((School) vectSchools.elementAt(i)).abundance;
                ((School) vectSchools.elementAt(i)).abundance = 0;
                ((School) vectSchools.elementAt(i)).disappears = true;
            }
        }

        //SURPLUS of DEAD are DISTRIBUTED
        int index = 0;
        while ((nbSurplusDead != 0) && (index < vectSchools.size())) {
            if (((School) vectSchools.elementAt(index)).abundance > nbSurplusDead) {
                ((School) vectSchools.elementAt(index)).abundance -= nbSurplusDead;
                nbSurplusDead = 0;
            } else {
                nbSurplusDead -= ((School) vectSchools.elementAt(index)).abundance;
                ((School) vectSchools.elementAt(index)).disappears = true;
                ((School) vectSchools.elementAt(index)).abundance = 0;
            }
            index++;
        }

        //REMOVING DEAD SCHOOLS FROM VECTBANCS and VECTPRESENTSCHOOLS
        Cell[][] matrix = species.simulation.osmose.grid.matrix;
        for (int i = vectSchools.size() - 1; i >= 0; i--) {
            if (((School) vectSchools.elementAt(i)).disappears) {
                if (!(outOfZoneCohort[species.simulation.dt]))// cohorts in the area during the time step
                {
                    matrix[((School) vectSchools.elementAt(i)).posi][((School) vectSchools.elementAt(i)).posj].vectPresentSchools.removeElement(vectSchools.elementAt(i));
                    matrix[((School) vectSchools.elementAt(i)).posi][((School) vectSchools.elementAt(i)).posj].nbPresentSchools--;
                }
                vectSchools.removeElementAt(i);
                nbSchools--;
            }
        }

        //UPDATE biomass of schools & cohort
        biomass = 0;
        for (int i = 0; i < vectSchools.size(); i++) {
            ((School) vectSchools.elementAt(i)).biomass =
                    ((double) ((School) vectSchools.elementAt(i)).abundance)
                    * ((School) vectSchools.elementAt(i)).weight / 1000000.;
            biomass += ((School) vectSchools.elementAt(i)).biomass;
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
            species.simulation.tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = schoolCatchk.length;

            if (schoolCatchk.abundance > Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable)) //case: enough fish in the school
            {
                Yi += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) * ((double) schoolCatchk.weight) / 1000000.;
                schoolCatchk.abundance -= Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable);
                // Vector of the number of fish of schools caught  -> indicator  MORGANE 07-2004
                species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable);
                if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                    species.simulation.tabTLCatch[species.number - 1] += schoolCatchk.trophicLevel[ageNbDt] * Math.round(((float) nbDeadFfTheo) / nbSchoolsCatchable) * ((float) schoolCatchk.weight) / 1000000f;
                }
            } else //case: not enough fish in the school
            {
                nbSurplusDead += Math.round(((double) nbDeadFfTheo) / nbSchoolsCatchable) - schoolCatchk.abundance;
                Yi += ((double) schoolCatchk.abundance) * schoolCatchk.weight / 1000000.;
                // Vector of the number of fish of schools caught   -> indicator   MORGANE 07-2004
                species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) schoolCatchk.abundance;
                if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                    species.simulation.tabTLCatch[species.number - 1] += schoolCatchk.trophicLevel[ageNbDt] * ((float) schoolCatchk.abundance) * schoolCatchk.weight / 1000000f;
                }
                schoolCatchk.abundance = 0;
                schoolCatchk.disappears = true;
            }
        }

        //----SURPLUS of DEAD FISH ARE DISTRIBUTED----
        int index = 0;
        while ((nbSurplusDead != 0) && (index < schoolsCatchable.size())) {
            if (((School) schoolsCatchable.elementAt(index)).abundance > nbSurplusDead) {
                ((School) schoolsCatchable.elementAt(index)).abundance -= nbSurplusDead;
                Yi += ((double) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000.;
                //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                species.simulation.tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                    species.simulation.tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).trophicLevel[ageNbDt] * ((float) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000f;
                }
                nbSurplusDead = 0;
            } else {
                nbSurplusDead -= ((School) schoolsCatchable.elementAt(index)).abundance;
                Yi += ((double) ((School) schoolsCatchable.elementAt(index)).abundance) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000.;
                //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                species.simulation.tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) ((School) schoolsCatchable.elementAt(index)).abundance;
                if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                    species.simulation.tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).trophicLevel[ageNbDt] * ((float) ((School) schoolsCatchable.elementAt(index)).abundance) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000f;
                }
                ((School) schoolsCatchable.elementAt(index)).disappears = true;
                ((School) schoolsCatchable.elementAt(index)).abundance = 0;
            }
            index++;
        }

        //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
        Cell[][] matrix = species.simulation.osmose.grid.matrix;
        for (int k = schoolsCatchable.size() - 1; k >= 0; k--) {
            School schoolCatchk = (School) schoolsCatchable.elementAt(k);
            if (schoolCatchk.disappears) {
                if (!outOfZoneCohort[species.simulation.dt]) {
                    matrix[schoolCatchk.posi][schoolCatchk.posj].vectPresentSchools.removeElement(schoolCatchk);
                    matrix[schoolCatchk.posi][schoolCatchk.posj].nbPresentSchools--;
                }
                vectSchools.removeElement(schoolCatchk);
                nbSchools--;
                schoolsCatchable.removeElementAt(k);
                nbSchoolsCatchable--;
            }
        }
        //UPDATE biomass of schools & cohort abd
        abundance = 0;
        abundanceCatchable = 0;
        for (int k = 0; k < vectSchools.size(); k++) {
            School schoolk = (School) vectSchools.elementAt(k);
            schoolk.biomass = ((double) schoolk.abundance) * schoolk.weight / 1000000.;
            abundance += schoolk.abundance;
            if (schoolk.isCatchable) {
                abundanceCatchable += schoolk.abundance;
            }
        }
        nbDeadFf = oldAbdCatch1 - abundance;

        // save of annual yield
        if ((species.simulation.t) >= species.simulation.osmose.timeSeriesStart) {
            species.simulation.savingYield[species.number - 1] += Yi;
            for (int k = 0; k < nbSchoolsCatchable; k++) //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            {
                species.simulation.savingNbYield[species.number - 1] += species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]];
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
                species.simulation.tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = schoolCatchk.length;
                if (schoolCatchk.abundance > Math.round(((double) nbDeadFf) / nbSchoolsCatchable)) {
                    Yi += Math.round(((double) nbDeadFf) / nbSchoolsCatchable) * ((double) schoolCatchk.weight) / 1000000.;
                    schoolCatchk.abundance -= Math.round(((double) nbDeadFf) / nbSchoolsCatchable);
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) (nbDeadFf / nbSchoolsCatchable);
                    if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                        species.simulation.tabTLCatch[species.number - 1] += schoolCatchk.trophicLevel[ageNbDt] * Math.round(((float) nbDeadFf) / nbSchoolsCatchable) * ((float) schoolCatchk.weight) / 1000000f;
                    }
                } else {
                    nbSurplusDead += Math.round(((double) nbDeadFf) / nbSchoolsCatchable) - schoolCatchk.abundance;
                    Yi += ((double) schoolCatchk.abundance) * schoolCatchk.weight / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) schoolCatchk.abundance;
                    if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                        species.simulation.tabTLCatch[species.number - 1] += schoolCatchk.trophicLevel[ageNbDt] * ((float) schoolCatchk.abundance) * schoolCatchk.weight / 1000000f;
                    }
                    schoolCatchk.abundance = 0;
                    schoolCatchk.disappears = true;
                }
            }
            //SURPLUS of DEAD is DISTRIBUTED
            int index = 0;
            while ((nbSurplusDead != 0) && (index < schoolsCatchable.size())) {
                if (((School) schoolsCatchable.elementAt(index)).abundance > nbSurplusDead) {
                    ((School) schoolsCatchable.elementAt(index)).abundance -= nbSurplusDead;
                    Yi += ((double) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                    if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                        species.simulation.tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).trophicLevel[ageNbDt] * ((float) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000f;
                    }
                    nbSurplusDead = 0;
                } else {
                    nbSurplusDead -= ((School) schoolsCatchable.elementAt(index)).abundance;
                    Yi += ((double) ((School) schoolsCatchable.elementAt(index)).abundance)
                            * ((School) schoolsCatchable.elementAt(index)).weight / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) ((School) schoolsCatchable.elementAt(index)).abundance;
                    species.simulation.tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += ((School) schoolsCatchable.elementAt(index)).trophicLevel[ageNbDt] * ((float) ((School) schoolsCatchable.elementAt(index)).abundance)
                            * ((School) schoolsCatchable.elementAt(index)).weight / 1000000f;
                    ((School) schoolsCatchable.elementAt(index)).disappears = true;
                    ((School) schoolsCatchable.elementAt(index)).abundance = 0;
                }
                index++;
            }
            //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
            Cell[][] matrix = species.simulation.osmose.grid.matrix;
            for (int k = schoolsCatchable.size() - 1; k >= 0; k--) {
                School schoolCatchk = (School) schoolsCatchable.elementAt(k);
                if (schoolCatchk.disappears) {
                    if (!outOfZoneCohort[species.simulation.dt]) {
                        matrix[schoolCatchk.posi][schoolCatchk.posj].vectPresentSchools.removeElement(schoolCatchk);
                        matrix[schoolCatchk.posi][schoolCatchk.posj].nbPresentSchools--;
                    }
                    vectSchools.removeElement(schoolCatchk);
                    nbSchools--;
                    schoolsCatchable.removeElementAt(k);
                    nbSchoolsCatchable--;
                }
            }
            //UPDATE schools biomasss & cohort abd
            abundance -= nbDeadFf;
            abundanceCatchable -= nbDeadFf;
            for (int i = 0; i < schoolsCatchable.size(); i++) {
                ((School) schoolsCatchable.elementAt(i)).biomass =
                        ((double) ((School) schoolsCatchable.elementAt(i)).abundance)
                        * ((School) schoolsCatchable.elementAt(i)).weight / 1000000.;
            }
        }
        if ((species.simulation.t) >= species.simulation.osmose.timeSeriesStart) {
            species.simulation.savingYield[species.number - 1] += Yi;
            for (int k = 0; k < nbSchoolsCatchable; k++) //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            {
                species.simulation.savingNbYield[species.number - 1] += species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]];
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
                species.simulation.tabSizeCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] = schoolCatchk.length;
                if (schoolCatchk.abundance > Math.round(((double) abdToCatch) / nbSchoolsCatchable)) {
                    schoolCatchk.abundance -= Math.round(((double) abdToCatch) / nbSchoolsCatchable);
                    Yi += Math.round(((double) abdToCatch) / nbSchoolsCatchable)
                            * ((double) schoolCatchk.weight) / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) abdToCatch / nbSchoolsCatchable;
                    if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                        species.simulation.tabTLCatch[species.number - 1] += schoolCatchk.trophicLevel[ageNbDt] * Math.round(((float) abdToCatch) / nbSchoolsCatchable)
                                * ((float) schoolCatchk.weight) / 1000000f;
                    }
                } else {
                    nbSurplusDead += Math.round(((double) abdToCatch) / nbSchoolsCatchable) - schoolCatchk.abundance;
                    Yi += ((double) schoolCatchk.abundance) * schoolCatchk.weight / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]] += (float) schoolCatchk.abundance;
                    if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                        species.simulation.tabTLCatch[species.number - 1] += schoolCatchk.trophicLevel[ageNbDt] * ((float) schoolCatchk.abundance) * schoolCatchk.weight / 1000000f;
                    }
                    schoolCatchk.abundance = 0;
                    schoolCatchk.disappears = true;
                }
            }
            //SURPLUS DEAD are DISTRIBUTED
            int index = 0;
            while ((nbSurplusDead != 0) && (index < schoolsCatchable.size())) {
                if (((School) schoolsCatchable.elementAt(index)).abundance > nbSurplusDead) {
                    ((School) schoolsCatchable.elementAt(index)).abundance -= nbSurplusDead;
                    Yi += ((double) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) nbSurplusDead;
                    if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                        species.simulation.tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).trophicLevel[ageNbDt] * ((float) nbSurplusDead) * ((School) schoolsCatchable.elementAt(index)).weight / 1000000f;
                    }
                    nbSurplusDead = 0;
                } else {
                    nbSurplusDead -= ((School) schoolsCatchable.elementAt(index)).abundance;
                    Yi += ((double) ((School) schoolsCatchable.elementAt(index)).abundance)
                            * ((School) schoolsCatchable.elementAt(index)).weight / 1000000.;
                    //MORGANE 07-2004	    // Vector of the number of fish of schools caught
                    species.simulation.tabNbCatch[species.number - 1][index + species.cumulCatch[ageNbDt - 1]] += (float) ((School) schoolsCatchable.elementAt(index)).abundance;
                    if ((species.simulation.TLoutput) && (species.simulation.t >= species.simulation.osmose.timeSeriesStart)) {
                        species.simulation.tabTLCatch[species.number - 1] += ((School) schoolsCatchable.elementAt(index)).trophicLevel[ageNbDt] * ((float) ((School) schoolsCatchable.elementAt(index)).abundance)
                                * ((School) schoolsCatchable.elementAt(index)).weight / 1000000f;
                    }
                    ((School) schoolsCatchable.elementAt(index)).disappears = true;
                    ((School) schoolsCatchable.elementAt(index)).abundance = 0;
                }
                index++;
            }
            //REMOVE DEAD SCHOOLS FROM VECTBANCS & VECTPRESENTSCHOOLS
            Cell[][] matrix = species.simulation.osmose.grid.matrix;
            for (int k = schoolsCatchable.size() - 1; k >= 0; k--) {
                School schoolCatchk = (School) schoolsCatchable.elementAt(k);
                if (schoolCatchk.disappears) {
                    if (!outOfZoneCohort[species.simulation.dt]) {
                        matrix[schoolCatchk.posi][schoolCatchk.posj].vectPresentSchools.removeElement(schoolCatchk);
                        matrix[schoolCatchk.posi][schoolCatchk.posj].nbPresentSchools--;
                    }
                    vectSchools.removeElement(schoolCatchk);
                    nbSchools--;
                }
            }
            //UPDATE schools biomass & cohort abd
            abundance = 0;
            for (int i = 0; i < vectSchools.size(); i++) {
                ((School) vectSchools.elementAt(i)).biomass =
                        ((double) ((School) vectSchools.elementAt(i)).abundance)
                        * ((School) vectSchools.elementAt(i)).weight / 1000000.;
                abundance += ((School) vectSchools.elementAt(i)).abundance;
            }
            nbDeadFf += abdToCatch;
        }
        if ((species.simulation.t) >= species.simulation.osmose.timeSeriesStart) {
            species.simulation.savingYield[species.number - 1] += Yi;                                       // ********saving of indicators to be done

            for (int k = 0; k < nbSchoolsCatchable; k++) //MORGANE 07-2004	    // Vector of the number of fish of schools caught
            {
                species.simulation.savingNbYield[species.number - 1] += species.simulation.tabNbCatch[species.number - 1][k + species.cumulCatch[ageNbDt - 1]];
            }

        }
    }

    public void growth(float minDelta, float maxDelta, float c, float bPower) {
        for (int i = 0; i < nbSchools; i++) {
            ((School) vectSchools.elementAt(i)).growth(minDelta, maxDelta, c, bPower);
        }
    }

    public void calculMeanGrowth() {
        double sumLengths = 0;
        double sumWeights = 0;
        long count = 0;
        for (int i = 0; i < vectSchools.size(); i++) {
            School schooli = (School) vectSchools.elementAt(i);
            sumLengths += ((double) schooli.length) * schooli.abundance;
            sumWeights += ((double) schooli.weight) * schooli.abundance;
            count += schooli.abundance;
        }
        meanLength = (float) sumLengths / count;
        meanWeight = (float) sumWeights / count;
    }
}
