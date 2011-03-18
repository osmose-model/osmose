
/********************************************************************************
 * <p>Titre : School class</p>
 *
 * <p>Description : Basic unit of Osmose model - represents a super-individual </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 ******************************************************************************** 
 */
import java.util.*;

class School {

    Cohort cohort;
    int numSerie;
    int posi, posj;		//coordinates in the lattice (in cells)
    int indexij;               //index of the cell (i,j)
    boolean outOfZoneSchool;         // Tag for outOfZone schools
    float length;		//length of the individuals in the school in cm
    float weight;		//weight of ind of the school in g
    float[] trophicLevel;       // trophic level per age
    long abundance;
    double biomass;		//biomass in tonnes
    float predSuccessRate;
    Grid grid;
    Cell cellDestination;
    boolean disappears;
    double biomassToPredate;
    float critPreySizeMax, critPreySizeMin;
    boolean isCatchable;
    int feedingStage; // correspond to feeding length-stage
    int accessibilityStage;		// correspond to the age-stage used for accessibilities between species
    int dietOutputStage;
    float[][] dietTemp;
    float sumDiet;
    float[] TLproie;
    float[] deltaW;

    public School(Cohort cohort, long abd, float length, float weight) {
        this.cohort = cohort;
        this.numSerie = cohort.numSerie;
        this.abundance = abd;
        this.length = length;
        this.weight = weight;
        this.biomass = ((double) abundance) * weight / 1000000.;
        grid = cohort.species.simulation.osmose.grid;
        disappears = false;
        isCatchable = true;

        // initialisation TLs
        trophicLevel = new float[cohort.species.nbCohorts];    // table of the TL of this school at each time step
        for (int t = 0; t < cohort.species.nbCohorts; t++) {
            trophicLevel[t] = 0f;
        }
        if (cohort.ageNbDt == 0) {
            trophicLevel[cohort.ageNbDt] = cohort.species.TLeggs;   // initialisation of the previous age because predation is based on TL at the previous time step
        }
        if (cohort.ageNbDt != 0) {
            trophicLevel[cohort.ageNbDt - 1] = 3f;   // initialisation of the previous age because predation is based on TL at the previous time step
        }
        // initialisation of stage
        updateFeedingStage(cohort.species.sizeFeeding, cohort.species.nbFeedingStages);
        updateAccessStage(cohort.species.ageStagesTab, cohort.species.nbAccessStages);
        updateDietOutputStage(cohort.species.dietStagesTab, cohort.species.nbDietStages);

        this.outOfZoneSchool = false;

        dietTemp = new float[cohort.species.simulation.species.length + cohort.species.simulation.couple.nbPlankton][];
        for (int i = 0; i < cohort.species.simulation.species.length; i++) {
            dietTemp[i] = new float[cohort.species.simulation.species[i].nbDietStages];
            for (int s = 0; s < cohort.species.simulation.species[i].nbDietStages; s++) {
                dietTemp[i][s] = 0;
            }
        }
        for (int i = cohort.species.simulation.species.length; i < cohort.species.simulation.species.length + cohort.species.simulation.couple.nbPlankton; i++) {
            dietTemp[i] = new float[1];
            dietTemp[i][0] = 0;
        }
        sumDiet = 0;
        TLproie = new float[4];
        deltaW = new float[4];
    }

    public void distribute(Vector vectCells) // distribute school in one cell randomly chosen in vectCells
    {
        int nbCellsArea = vectCells.size();
        double rand = Math.random();
        // indexij to test the probability of presence
        indexij = (int) Math.round((nbCellsArea - 1) * rand);
        posi = ((Cell) vectCells.elementAt((int) (Math.round((nbCellsArea - 1) * rand)))).posi;
        posj = ((Cell) vectCells.elementAt((int) (Math.round((nbCellsArea - 1) * rand)))).posj;
    }

    public void communicatePosition() {
        //function called when update of schools present in cells after having
        //removed all elements from vectPresentSchools in all cells
        grid.matrix[posi][posj].vectPresentSchools.addElement(this);
        grid.matrix[posi][posj].nbPresentSchools++;
    }

    public void randomWalk() {
        Vector vectPossibleCells = new Vector();
        Cell[] neighbors = grid.matrix[posi][posj].neighbors;
        Cell oldCell = grid.matrix[posi][posj];

        for (int i = 0; i < neighbors.length; i++) {
            if (!neighbors[i].coast) {
                vectPossibleCells.addElement(neighbors[i]);
            }
        }
        vectPossibleCells.addElement(oldCell);
        distribute(vectPossibleCells);
    }

    public void updateAccessStage(float[] ageStages, int nbAccessStages) {
        accessibilityStage = 0;
        for (int i = 1; i < nbAccessStages; i++) {
            if (cohort.ageNbDt >= ageStages[i - 1]) {
                accessibilityStage++;
            }
        }
    }

    public void updateFeedingStage(float[] sizeStages, int nbStages) {
        feedingStage = 0;
        for (int i = 1; i < nbStages; i++) {
            if (length >= sizeStages[i - 1]) {
                feedingStage++;
            }
        }
    }

    public void updateDietOutputStage(float[] thresholdTab, int nbStages) {
        dietOutputStage = 0;

        if (cohort.species.simulation.dietMetric.equalsIgnoreCase("size")) {
            for (int i = 1; i < nbStages; i++) {
                if (length >= thresholdTab[i - 1]) {
                    dietOutputStage++;
                }
            }
        } else if (cohort.species.simulation.dietMetric.equalsIgnoreCase("age")) {
            for (int i = 1; i < nbStages; i++) {
                int tempAge = Math.round(thresholdTab[i - 1] * cohort.species.simulation.nbDt);
                if (length >= tempAge) {
                    dietOutputStage++;
                }
            }
        }
    }

    public void predation() {
        Cell myCell = grid.matrix[posi][posj];
        Coupling myCouple = cohort.species.simulation.couple;

        float[] percentPlankton;
        percentPlankton = new float[myCouple.nbPlankton];

        biomassToPredate = biomass * cohort.species.predationRate / (float) cohort.species.simulation.nbDt;
        double biomassToPredateIni = biomassToPredate;

        critPreySizeMax = length / cohort.species.predPreySizesMax[feedingStage];
        critPreySizeMin = length / cohort.species.predPreySizesMin[feedingStage];

        //************ Calculation of available fish******************

        int indexMax = 0;
        while ((indexMax < (myCell.vectPresentSchools.size() - 1))
                && (critPreySizeMax >= ((School) myCell.vectPresentSchools.elementAt(indexMax)).length)) {
            indexMax++;
        }
        //indexMax is the index of vectBP which corresponds to the sup limit of the possible prey sizes
        //for this school, vectPresentSchools(indexMax) is not a possible prey school.

        int indexMin = 0;
        while ((indexMin < (myCell.vectPresentSchools.size() - 1))
                && (critPreySizeMin > ((School) myCell.vectPresentSchools.elementAt(indexMin)).length)) {
            indexMin++;
        }
        //indexMin is the index of vectBP which corresponds to the inf limit of the possible prey sizes
        //for this school, vectPresentSchools(indexMin) IS a possible prey school.

        //summation of the accessible biom of school prey, for distributing predation mortality
        //cf third version further in the program, 21 january 2002
        double biomAccessibleTot = 0;
        for (int k = indexMin; k < indexMax; k++) {

            float tempAccess;
            School preySchool = (School) myCell.vectPresentSchools.elementAt(k);
            tempAccess = cohort.species.simulation.osmose.accessibilityMatrix[preySchool.cohort.species.number - 1][preySchool.accessibilityStage][cohort.species.number - 1][accessibilityStage];
            biomAccessibleTot += ((School) myCell.vectPresentSchools.elementAt(k)).biomass * tempAccess;
        }
        //************ Calculation of available plankton ***************
        for (int i = 0; i < myCouple.nbPlankton; i++) {
            if ((critPreySizeMin > myCouple.planktonList[i].sizeMax) || (critPreySizeMax < myCouple.planktonList[i].sizeMin)) {
                percentPlankton[i] = 0.0f;
            } else {
                float tempAccess;
                tempAccess = cohort.species.simulation.osmose.accessibilityMatrix[cohort.species.simulation.nbSpecies + i][0][cohort.species.number - 1][accessibilityStage];
                percentPlankton[i] = myCouple.planktonList[i].calculPercent(critPreySizeMin, critPreySizeMax);
                biomAccessibleTot += percentPlankton[i] * tempAccess * myCouple.planktonList[i].accessibleBiomass[posi][posj];
            }
        }


        int[] tabIndices = new int[indexMax - indexMin];

        Vector vectIndices = new Vector();
        for (int i = indexMin; i < indexMax; i++) {
            vectIndices.addElement(new Integer(i));
        }
        int z = 0;
        while (z < tabIndices.length) {
            int rand = (int) (Math.round((vectIndices.size() - 1) * Math.random()));
            tabIndices[z] = ((Integer) vectIndices.elementAt(rand)).intValue();
            vectIndices.removeElementAt(rand);
            z++;
        }

        /*-------code pour predation with inaccessible biom, of 10%
        calculated in simulation.step for each school before each phase of predation
        not quarter to quarter but directly according to options 21 january 2002*/

        if (biomAccessibleTot != 0) // if there is food
        {
            if (biomAccessibleTot <= biomassToPredate) // = NOT ENOUGH PREY --------------------------------
            {
                biomassToPredate -= biomAccessibleTot;
                // ***********Predation of fish*********************
                for (int k = 0; k < tabIndices.length; k++) {
                    School mySchoolk = (School) myCell.vectPresentSchools.elementAt(tabIndices[k]);
                    float tempAccess = cohort.species.simulation.osmose.accessibilityMatrix[mySchoolk.cohort.species.number - 1][mySchoolk.accessibilityStage][cohort.species.number - 1][accessibilityStage];

                    float TLprey;
                    if ((mySchoolk.cohort.ageNbDt == 0) || (mySchoolk.cohort.ageNbDt == 1)) // prey are eggs or were eggs at the previous time step
                    {
                        TLprey = cohort.species.TLeggs;
                    } else {
                        TLprey = mySchoolk.trophicLevel[mySchoolk.cohort.ageNbDt - 1];
                    }

                    trophicLevel[cohort.ageNbDt] += TLprey * (float) (mySchoolk.biomass * tempAccess / biomAccessibleTot);

                    if ((cohort.species.simulation.dietsOutput) && (cohort.species.simulation.t >= cohort.species.simulation.osmose.timeSeriesStart)) {
                        dietTemp[mySchoolk.cohort.species.number - 1][mySchoolk.dietOutputStage] += mySchoolk.biomass * tempAccess;
                    }
                    //
                    mySchoolk.cohort.nbDeadPp += Math.round((mySchoolk.biomass * tempAccess * 1000000.) / mySchoolk.weight);
                    mySchoolk.biomass -= mySchoolk.biomass * tempAccess;
                    mySchoolk.abundance -= Math.round((mySchoolk.biomass * tempAccess * 1000000.) / mySchoolk.weight);

                    if (mySchoolk.abundance <= 0) {
                        mySchoolk.abundance = 0;
                        mySchoolk.biomass = 0;
                        mySchoolk.disappears = true;
                    }

                }
                //**********Predation on plankton**************
                for (int i = 0; i < cohort.species.simulation.couple.nbPlankton; i++) {
                    if (percentPlankton[i] != 0.0f) {
                        float tempAccess;
                        tempAccess = cohort.species.simulation.osmose.accessibilityMatrix[cohort.species.simulation.nbSpecies + i][0][cohort.species.number - 1][accessibilityStage];

                        double tempBiomEaten = percentPlankton[i] * tempAccess * myCouple.planktonList[i].accessibleBiomass[posi][posj];

                        myCouple.planktonList[i].accessibleBiomass[posi][posj] -= tempBiomEaten;
                        myCouple.planktonList[i].biomass[posi][posj] -= tempBiomEaten;
                        trophicLevel[cohort.ageNbDt] += myCouple.planktonList[i].trophicLevel * (float) (tempBiomEaten / biomAccessibleTot);
                        if ((cohort.species.simulation.dietsOutput) && (cohort.species.simulation.t >= cohort.species.simulation.osmose.timeSeriesStart)) {
                            dietTemp[cohort.species.simulation.species.length + i][0] += tempBiomEaten;
                        }
                    }
                }
            } else // = ENOUGH PREY --------------------------------------------------------------------
            {
                // *********** Predation on fish ******************
                for (int k = 0; k < tabIndices.length; k++) {
                    School mySchoolk = (School) myCell.vectPresentSchools.elementAt(tabIndices[k]);
                    float tempAccess = cohort.species.simulation.osmose.accessibilityMatrix[mySchoolk.cohort.species.number - 1][mySchoolk.accessibilityStage][cohort.species.number - 1][accessibilityStage];

                    float TLprey;
                    if ((mySchoolk.cohort.ageNbDt == 0) || (mySchoolk.cohort.ageNbDt == 1)) // prey are eggs or were eggs at the previous time step
                    {
                        TLprey = cohort.species.TLeggs;
                    } else {
                        TLprey = mySchoolk.trophicLevel[mySchoolk.cohort.ageNbDt - 1];
                    }

                    trophicLevel[cohort.ageNbDt] += TLprey * (float) (mySchoolk.biomass * tempAccess / biomAccessibleTot);// TL of the prey at the previous time step because all schools hav'nt predate yet

                    if ((cohort.species.simulation.dietsOutput) && (cohort.species.simulation.t >= cohort.species.simulation.osmose.timeSeriesStart)) {
                        dietTemp[mySchoolk.cohort.species.number - 1][mySchoolk.dietOutputStage] += mySchoolk.biomass * tempAccess * biomassToPredate / biomAccessibleTot;
                    }

                    mySchoolk.cohort.nbDeadPp += Math.round((mySchoolk.biomass * tempAccess
                            * biomassToPredate * 1000000.) / (mySchoolk.weight * biomAccessibleTot));
                    mySchoolk.abundance -= Math.round((mySchoolk.biomass * tempAccess
                            * biomassToPredate * 1000000.) / (mySchoolk.weight * biomAccessibleTot));
                    mySchoolk.biomass -= mySchoolk.biomass * tempAccess * biomassToPredate / biomAccessibleTot;

                    if (mySchoolk.abundance <= 0) {
                        mySchoolk.abundance = 0;
                        mySchoolk.biomass = 0;
                        mySchoolk.disappears = true;
                    }

                }
                //************ Predation on plankton ***************
                for (int i = 0; i < cohort.species.simulation.couple.nbPlankton; i++) {
                    if (percentPlankton[i] != 0.0f) {
                        float tempAccess;
                        tempAccess = cohort.species.simulation.osmose.accessibilityMatrix[cohort.species.simulation.nbSpecies + i][0][cohort.species.number - 1][accessibilityStage];

                        double tempBiomEaten = percentPlankton[i] * tempAccess * myCouple.planktonList[i].accessibleBiomass[posi][posj] * biomassToPredate / biomAccessibleTot;

                        myCouple.planktonList[i].accessibleBiomass[posi][posj] -= tempBiomEaten;
                        myCouple.planktonList[i].biomass[posi][posj] -= tempBiomEaten;
                        trophicLevel[cohort.ageNbDt] += myCouple.planktonList[i].trophicLevel * (float) (tempBiomEaten / biomassToPredate);

                        if ((cohort.species.simulation.dietsOutput) && (cohort.species.simulation.t >= cohort.species.simulation.osmose.timeSeriesStart)) {
                            dietTemp[cohort.species.simulation.species.length + i][0] += tempBiomEaten;
                        }
                    }
                }

                biomassToPredate = 0;

            }//end else (case of enough prey)

            trophicLevel[cohort.ageNbDt] += 1;
        } else // biomAccessibleTot = 0, i.e. No food
        {
            trophicLevel[cohort.ageNbDt] = trophicLevel[cohort.ageNbDt - 1];     // no food available, so current TL = previous TL
        }
        predSuccessRate = (float) (1 - biomassToPredate / biomassToPredateIni);     // = predSuccessRate for the current time step
        if ((cohort.species.simulation.dietsOutput) && (cohort.species.simulation.t >= cohort.species.simulation.osmose.timeSeriesStart)) {
            for (int i = 0; i < cohort.species.simulation.species.length; i++) {
                for (int s = 0; s < cohort.species.simulation.species[i].nbDietStages; s++) {
                    sumDiet += dietTemp[i][s];
                    cohort.species.simulation.predatorsPressureMatrix[cohort.species.number - 1][dietOutputStage][i][s] += dietTemp[i][s];
                }
            }
            for (int i = cohort.species.simulation.species.length; i < cohort.species.simulation.species.length + cohort.species.simulation.couple.nbPlankton; i++) {
                sumDiet += dietTemp[i][0];
                cohort.species.simulation.predatorsPressureMatrix[cohort.species.number - 1][dietOutputStage][i][0] += dietTemp[i][0];
            }

            if (sumDiet != 0) {
                cohort.species.simulation.nbStomachs[cohort.species.number - 1][dietOutputStage] += abundance;
                for (int i = 0; i < cohort.species.simulation.species.length; i++) {
                    for (int s = 0; s < cohort.species.simulation.species[i].nbDietStages; s++) {
                        cohort.species.simulation.dietsMatrix[cohort.species.number - 1][dietOutputStage][i][s] += abundance * dietTemp[i][s] / sumDiet;
                    }
                }
                for (int i = cohort.species.simulation.species.length; i < cohort.species.simulation.species.length + cohort.species.simulation.couple.nbPlankton; i++) {
                    cohort.species.simulation.dietsMatrix[cohort.species.number - 1][dietOutputStage][i][0] += abundance * dietTemp[i][0] / sumDiet;
                }
                sumDiet = 0;
            }
            for (int i = 0; i < cohort.species.simulation.species.length; i++) {
                for (int s = 0; s < cohort.species.simulation.species[i].nbDietStages; s++) {
                    dietTemp[i][s] = 0;
                }
            }
            for (int i = cohort.species.simulation.species.length; i < cohort.species.simulation.species.length + cohort.species.simulation.couple.nbPlankton; i++) {
                dietTemp[i][0] = 0;
            }

        }
    }

    public void surviveP() {
        if (predSuccessRate <= cohort.species.criticalPredSuccess) {
            long nbDead;
            double mortalityRate;

            mortalityRate = -(cohort.species.starvMaxRate * predSuccessRate) / cohort.species.criticalPredSuccess + cohort.species.starvMaxRate;
            if (mortalityRate < 0) {
                mortalityRate = 0;
                System.out.print("starvation bug ");
            }
            nbDead = (long) (Math.round((double) abundance) * (1 - Math.exp(-mortalityRate / (float) cohort.species.simulation.nbDt)));

            abundance -= nbDead;

            this.cohort.nbDeadSs += nbDead;
            if (abundance <= 0) {
                disappears = true;
                abundance = 0;
            }
        }
    }

    public void growth(float minDelta, float maxDelta, float c, float bPower) {
        float previousW = (float) (c * Math.pow(length, bPower));
        //calculation of lengths according to predation efficiency
        if (predSuccessRate >= cohort.species.criticalPredSuccess) {
            length += minDelta + (maxDelta - minDelta) * ((predSuccessRate - cohort.species.criticalPredSuccess) / (1 - cohort.species.criticalPredSuccess));
            weight = (float) (c * Math.pow(length, bPower));
        }
        biomass = ((double) abundance) * weight / 1000000.;

        //		updateTL(previousW,weight);
        updateTLbis();
    }

    public void updateTL(float previousW, float W) {
        float previousTL, newTL;
        if (cohort.ageNbDt != 0) {
            previousTL = trophicLevel[cohort.ageNbDt - 1];
        } else {
            previousTL = cohort.species.TLeggs;
        }

        newTL = ((previousW * previousTL) + ((W - previousW) * trophicLevel[cohort.ageNbDt])) / (W);   // weighting of new TL according to increase of weight dut to prey ingestion
        trophicLevel[cohort.ageNbDt] = newTL;
    }

    public void updateTLbis() {
        if (cohort.ageNbDt == 0) {
            deltaW[0] = 0;
            deltaW[1] = 0;
            deltaW[2] = 0;
            deltaW[3] = predSuccessRate;
            TLproie[0] = 0;
            TLproie[1] = 0;
            TLproie[2] = 0;
            TLproie[3] = trophicLevel[cohort.ageNbDt];
        } else {
            if (cohort.ageNbDt == 1) {
                deltaW[0] = 0;
                deltaW[1] = 0;
                deltaW[2] = deltaW[3];
                deltaW[3] = predSuccessRate;
                TLproie[0] = 0;
                TLproie[1] = 0;
                TLproie[2] = TLproie[3];
                TLproie[3] = trophicLevel[cohort.ageNbDt];
            } else {
                if (cohort.ageNbDt == 2) {
                    deltaW[0] = 0;
                    deltaW[1] = deltaW[2];
                    deltaW[2] = deltaW[3];
                    deltaW[3] = predSuccessRate;
                    TLproie[0] = 0;
                    TLproie[1] = TLproie[2];
                    TLproie[2] = TLproie[3];
                    TLproie[3] = trophicLevel[cohort.ageNbDt];
                } else {
                    deltaW[0] = deltaW[1];
                    deltaW[1] = deltaW[2];
                    deltaW[2] = deltaW[3];
                    deltaW[3] = predSuccessRate;
                    TLproie[0] = TLproie[1];
                    TLproie[1] = TLproie[2];
                    TLproie[2] = TLproie[3];
                    TLproie[3] = trophicLevel[cohort.ageNbDt];
                }
            }
        }
        if ((deltaW[3] + deltaW[2] + deltaW[1] + deltaW[0]) != 0) {
            trophicLevel[cohort.ageNbDt] = (deltaW[3] * TLproie[3] + deltaW[2] * TLproie[2] + deltaW[1] * TLproie[1] + deltaW[0] * TLproie[0]) / (deltaW[3] + deltaW[2] + deltaW[1] + deltaW[0]);
        } else {
            if (cohort.ageNbDt != 0) {
                trophicLevel[cohort.ageNbDt] = trophicLevel[cohort.ageNbDt - 1];
            } else {
                trophicLevel[cohort.ageNbDt] = cohort.species.TLeggs;
            }
        }
    }

    public void rankSize(float[] tabSizes, float sizeMax) // for size spectrum output
    {
        int indexMax = tabSizes.length - 1;
        if (length <= sizeMax) {
            while (length < tabSizes[indexMax]) {
                indexMax--;
            }

            //cohort.species.simulation.spectrumAbd[indexMax] += this.abundance;
            //cohort.species.simulation.spectrumBiom[indexMax] += this.biomass;

            //MORGANE 07-2004
            // Size spectrum per species
            cohort.species.simulation.spectrumSpeciesAbd[cohort.species.number - 1][indexMax] += this.abundance;
        }
    }

    public void rankTL(float[] tabTL) // for TL distribution output
    {
        if ((trophicLevel[cohort.ageNbDt] >= 1) && (biomass != 0)) {
            int indexMax = tabTL.length - 1;
            while ((trophicLevel[cohort.ageNbDt] <= tabTL[indexMax]) && (indexMax > 0)) {
                indexMax--;
            }

            if (cohort.ageNbDt < cohort.species.supAgeOfClass0) {
                cohort.species.simulation.distribTL[cohort.species.number - 1][0][indexMax] += this.biomass;   // inferior to ageSupAgeClass0
            } else {
                cohort.species.simulation.distribTL[cohort.species.number - 1][1][indexMax] += this.biomass;   // superior to ageSupAgeClass0
            }

        }
    }
}
