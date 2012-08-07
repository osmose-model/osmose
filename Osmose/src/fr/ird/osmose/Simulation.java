package fr.ird.osmose;

/**
 * *****************************************************************************
 * <p>Titre : Simulation class</p>
 *
 * <p>Description : </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * ******************************************************************************
 */
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

public class Simulation {

///////////////////////////////
// Declaration of the constants
///////////////////////////////
    /*
     * Set the mortality algorithm. Mortality causes = {predation, starvation,
     * natural, fishing} CASE1: we assume every cause is independant and
     * concomitant. No stochasticity neither competition within predation
     * process: every predator sees preys as they are at current time-step.
     * CASE2: we assume every cause is independant and concomitant.
     * Stochasticity and competition within predation process: prey and predator
     * biomass are being updated on the fly. CASE3: we assume every cause
     * compete with each other. Stochasticity at both school and mortality
     * process levels.
     */
    public static final AlgoMortality ALGO_MORTALITY = AlgoMortality.CASE1;
    /*
     * Subdivise the main time step in smaller time steps for applying
     * mortality. Should only be 1 so far, still problems to fix.
     */
    public static final int SUB_DT = 1;
    /*
     * When true, it creates a nDead_Simu#.csv file that counts number of deads
     * from every mortality source and provides instantaneous mortality rates.
     */
    public static final boolean DEBUG_MORTALITY = false;
///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /*
     * Coupling with Biogeochimical model.
     */
    private LTLCoupling coupling;
    /*
     * Forcing with Biogeochimical model.
     */
    private LTLForcing forcing;
    /*
     * The number of the current scenario
     */
    private int numSerie;
    /*
     * Number of time-steps in one year
     */
    private int nbTimeStepsPerYear;
    /*
     * Record frequency
     */
    private int recordFrequency;
    /*
     * Random distribution of the schools in the grid
     */
    boolean randomDistribution = true;
    /*
     * Time of the simulation in [year]
     */
    private int year;
    /*
     * Time step of the simulation
     */
    private int indexTime;
    /*
     * Array of the species of the simulation
     */
    private Species[] species;
    /*
     * Characteristics of caught schools by species
     */
    float[][] tabSizeCatch, tabNbCatch;
    float tempMaxProbaPresence;
    float[][] accessibilityMatrix;
    int[] nbAccessibilityStages;
    // initialisation param for species abd in function of an input size spectrum
    double a, b;	    //coeff of the relation nb=length^a * expb
    //in Rice : a=-5.8; b=35.5*/
    long[] abdGapSizeClass10;	//tab of abd to fill in 10cm size class
    long abdIniMin;			//initial min abd of last age class of a species
    boolean targetFishing;
    double RS;		//ratio between mpa and total grid surfaces, RS for Relative Size of MPA
    FileOutputStream dietTime, biomTime, abdTime, TLDistTime, yieldTime, nbYieldTime, meanSizeTime, meanTLTime, SSperSpTime;
//	*******Trophodynamics indicators
    boolean TLoutput;
    boolean TLDistriboutput;
    boolean dietsOutput;
    String dietMetric;
//	*******Size-based indicators
    boolean meanSizeOutput;
    boolean sizeSpectrumOutput;
    boolean sizeSpectrumPerSpeOutput;
//	*******Mortalities data
    boolean planktonMortalityOutput;
    boolean outputClass0;
    boolean calibration;

    public void init() {

        year = 0;
        indexTime = 0;
        numSerie = getOsmose().numSerie;
        nbTimeStepsPerYear = getOsmose().nbDtMatrix[numSerie];
        recordFrequency = getOsmose().savingDtMatrix[numSerie];
        calibration = getOsmose().calibrationMatrix[numSerie];
        TLoutput = getOsmose().TLoutputMatrix[numSerie];
        TLDistriboutput = getOsmose().TLDistriboutputMatrix[numSerie];
        dietsOutput = getOsmose().dietsOutputMatrix[numSerie];
        dietMetric = getOsmose().dietOutputMetrics[numSerie];
        meanSizeOutput = getOsmose().meanSizeOutputMatrix[numSerie];
        sizeSpectrumOutput = getOsmose().sizeSpectrumOutputMatrix[numSerie];
        sizeSpectrumPerSpeOutput = getOsmose().sizeSpectrumPerSpeOutputMatrix[numSerie];
        planktonMortalityOutput = getOsmose().planktonMortalityOutputMatrix[numSerie];
        outputClass0 = getOsmose().outputClass0Matrix[numSerie];

        // Initialise plankton matrix
        iniPlanktonField(getOsmose().isForcing[numSerie]);

        //CREATION of the SPECIES
        species = new Species[getOsmose().nbSpeciesTab[numSerie]];
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(i + 1);
            species[i].init();
        }

        // determine if fishing is species-based or similar for all species
        targetFishing = false;
        for (int i = 1; i < species.length; i++) {
            if (species[i].F != species[0].F) {
                targetFishing = true;
            }
        }

        //INITIALISATION of SPECIES ABD ACCORDING TO SIZE SPECTRUM
        if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("biomass")) {
            iniBySpeciesBiomass();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("spectrum")) {
            iniBySizeSpectrum();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("random")) {
            iniRandomly();
        }
        updateBiomassAndAbundance();

        // Initialize all the tables required for saving output
        if (getOsmose().spatializedOutputs[numSerie]) {
            initSpatializedSaving();
        }

        //Initialisation indicators
        tabSizeCatch = new float[species.length][];
        tabNbCatch = new float[species.length][];
    }

    private IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private void printProgress() {
        // screen display to check the period already simulated
        if (year % 5 == 0) {
            System.out.println("year " + year + " | CPU time " + new Date());   // t is annual
        } else {
            System.out.println("year " + year);
        }
    }

    private int getRatioMPA() {
        int ratio = 0;
        if ((getOsmose().thereIsMPATab[numSerie]) && (year == getOsmose().MPAtStartTab[numSerie])) {
            ratio = getOsmose().tabMPAiMatrix[numSerie].length / ((getGrid().getNbLines()) * getGrid().getNbColumns());
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(true);
            }
        } else if ((!getOsmose().thereIsMPATab[numSerie]) || (year > getOsmose().MPAtEndTab[numSerie])) {
            ratio = 0;
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(false);
            }
        }
        return ratio;
    }

    private void clearNbDeadArrays() {
        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                species[i].getCohort(j).setNbDeadPp(0);
                species[i].getCohort(j).setNbDeadSs(0);
                species[i].getCohort(j).setNbDeadDd(0);
                species[i].getCohort(j).setNbDeadFf(0);
            }
        }
    }

    private void updateStages() {
        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                    ((School) species[i].getCohort(j).getSchool(k)).updateFeedingStage(species[i].sizeFeeding, species[i].nbFeedingStages);
                    ((School) species[i].getCohort(j).getSchool(k)).updateAccessStage(getOsmose().accessStageThreshold[i], getOsmose().nbAccessStage[i]);
                    ((School) species[i].getCohort(j).getSchool(k)).updateDietOutputStage(species[i].dietStagesTab, species[i].nbDietStages);
                }
            }
        }
    }

    /*
     * save fish biomass before any mortality process for diets data (last
     * column of predatorPressure output file in Diets/)
     */
    private void saveBiomassBeforeMortality() {

        // update biomass
        if (getOsmose().dietsOutputMatrix[getOsmose().numSerie] && (year >= getOsmose().timeSeriesStart)) {
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        Indicators.biomPerStage[i][((School) species[i].getCohort(j).getSchool(k)).dietOutputStage] += ((School) species[i].getCohort(j).getSchool(k)).getBiomass();
                    }
                }
            }
            getForcing().saveForDiet();
        }
        if (coupling != null && (year >= coupling.getStartYearLTLModel() - 1)) // save grid of plankton biomass one year before coupling so forcing mode is also saved
        {
            coupling.savePlanktonBiomass();
        } else if (getOsmose().planktonBiomassOutputMatrix[numSerie]) {
            forcing.savePlanktonBiomass();
        }
    }

    /**
     * For all species, D is due to other predators (seals, seabirds) for
     * migrating species, we add mortality because absents during a time step so
     * they don't undergo mortalities due to predation and starvation Additional
     * mortalities for ages 0: no-fecundation of eggs, starvation more
     * pronounced than for sup ages (rel to CC), predation by other species are
     * not explicit.
     */
    double getNaturalMortalityRate(School school, int subdt) {
        double D;
        Species spec = school.getCohort().getSpecies();
        if (school.getCohort().getAgeNbDt() == 0) {
            D = spec.larvalSurvival + (spec.getCohort(0).getOutMortality(indexTime) / (float) (nbTimeStepsPerYear * subdt));
        } else {
            D = (spec.D + school.getCohort().getOutMortality(indexTime)) / (float) (nbTimeStepsPerYear * subdt);
        }
        return D;
    }

    double computeNaturalMortality(School school, int subdt) {

        double D = getNaturalMortalityRate(school, subdt);
        return getAbundance(school) * (1.d - Math.exp(-D));
    }

    private double getFishingMortalityRate(School school, int subdt) {
        Species spec = school.getCohort().getSpecies();
        return spec.F * spec.seasonFishing[indexTime] / subdt;
    }

    private double computeFishingMortality(School school, int subdt) {

        Species spec = school.getCohort().getSpecies();
        int indexRecruitAge = Math.round(spec.recruitAge * nbTimeStepsPerYear);
        double F = getFishingMortalityRate(school, subdt);
        // Test whether fishing applies to this school
        // 1. F != 0
        // 2. School is recruited
        // 3. School is catchable (no MPA and no out of zone)
        boolean isFishable = (F != 0)
                && (school.getCohort().getAgeNbDt() >= indexRecruitAge)
                && school.isCatchable();
        double nDead = 0;
        if (isFishable) {
            nDead = getAbundance(school) * (1 - Math.exp(-F));
        }
        return nDead;
    }

    private double computeBiomassToPredate(School predator, int subdt) {
        return getBiomass(predator) * predator.getCohort().getSpecies().predationRate / (double) (nbTimeStepsPerYear * subdt);
    }

    private double getBiomass(School school) {
        return school.adb2biom(getAbundance(school));
    }

    private double getAbundance(School school) {
        double nDeadTotal = school.nDeadPredation
                + school.nDeadStarvation
                + school.nDeadNatural
                + school.nDeadFishing;
        double abundance = school.getAbundance() - nDeadTotal;
        //if (nDeadTotal > 0) System.out.println("Abundance changed " + " " + school.nDeadPredation + " " +  school.nDeadStarvation + " " + school.nDeadNatural + " " + school.nDeadFishing);
        return (abundance < 1)
                ? 0.d
                : abundance;
    }

    public double[] computePredation(School predator, int subdt) {

        Cell cell = predator.getCell();
        int nFish = cell.size();
        double[] preyUpon = new double[cell.size() + forcing.getNbPlanktonGroups()];
        Species predSpec = predator.getCohort().getSpecies();
        // find the preys
        int[] indexPreys = findPreys(predator);

        // Compute accessible biomass
        // 1. from preys
        double biomAccessibleTot = 0.d;
        for (int iPrey : indexPreys) {
            biomAccessibleTot += getAccessibleBiomass(predator, cell.get(iPrey));
        }
        // 2. from plankton
        float[] percentPlankton = getPercentPlankton(predator);
        for (int i = 0; i < forcing.getNbPlanktonGroups(); i++) {
            float tempAccess = getOsmose().accessibilityMatrix[getNbSpecies() + i][0][predSpec.getIndex()][predator.getAccessibilityStage()];
            biomAccessibleTot += percentPlankton[i] * tempAccess * forcing.getPlankton(i).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
        }

        // Compute the potential biomass that predators could prey upon
        double biomassToPredate = computeBiomassToPredate(predator, subdt);

        // Distribute the predation over the preys
        if (biomAccessibleTot != 0) {
            // There is less prey available than the predator can
            // potentially prey upon. Predator will feed upon the total
            // accessible biomass
            if (biomAccessibleTot <= biomassToPredate) {
                biomassToPredate = biomAccessibleTot;
            }

            // Assess the loss for the preys caused by this predator
            // Assess the gain for the predator from preys
            for (int iPrey : indexPreys) {
                double ratio = getAccessibleBiomass(predator, cell.get(iPrey)) / biomAccessibleTot;
                preyUpon[iPrey] = ratio * biomassToPredate;
            }
            // Assess the gain for the predator from plankton
            // Assess the loss for the plankton caused by the predator
            for (int i = 0; i < forcing.getNbPlanktonGroups(); i++) {
                float tempAccess = getOsmose().accessibilityMatrix[getNbSpecies() + i][0][predSpec.getIndex()][predator.getAccessibilityStage()];
                double ratio = percentPlankton[i] * tempAccess * forcing.getPlankton(i).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()] / biomAccessibleTot;
                preyUpon[nFish + i] = ratio * biomassToPredate;
            }

        } else {
            // Case 2: there is no prey available
            // No loss !
        }
        return preyUpon;
    }

    private double sum(double[] array) {
        double sum = 0.d;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    private double[][] computePredationMatrix(Cell cell, int subdt) {

        double[][] preyUpon = new double[cell.size() + forcing.getNbPlanktonGroups()][cell.size() + forcing.getNbPlanktonGroups()];
        // Loop over the schools of the cell
        for (School school : cell) {
            school.nDeadPredation = 0;
        }
        for (int iPred = 0; iPred < cell.size(); iPred++) {
            preyUpon[iPred] = computePredation(cell.get(iPred), subdt);
        }
        return preyUpon;
    }

    private double getStarvationMortalityRate(School school, int subdt) {
        Species spec = school.getCohort().getSpecies();

        // Compute the predation mortality rate
        double mortalityRate = 0;
        if (school.predSuccessRate <= school.getCohort().getSpecies().criticalPredSuccess) {
            mortalityRate = Math.max(spec.starvMaxRate * (1 - school.predSuccessRate / spec.criticalPredSuccess), 0.d);
        }

        return mortalityRate / (nbTimeStepsPerYear * subdt);
    }

    private float computePredSuccessRate(double biomassToPredate, double preyedBiomass) {

        // Compute the predation success rate
        return Math.min((float) (preyedBiomass / biomassToPredate), 1.f);
    }

    private double computeStarvationMortality(School school, int subdt) {
        double M = getStarvationMortalityRate(school, subdt);
        return getAbundance(school) * (1 - Math.exp(-M));
    }

    private float[] getPercentPlankton(School predator) {
        float[] percentPlankton = new float[forcing.getNbPlanktonGroups()];
        Species spec = predator.getCohort().getSpecies();
        float preySizeMax = predator.getLength() / spec.predPreySizesMax[predator.getFeedingStage()];
        float preySizeMin = predator.getLength() / spec.predPreySizesMin[predator.getFeedingStage()];
        for (int i = 0; i < forcing.getNbPlanktonGroups(); i++) {
            if ((preySizeMin > forcing.getPlankton(i).getSizeMax()) || (preySizeMax < forcing.getPlankton(i).getSizeMin())) {
                percentPlankton[i] = 0.0f;
            } else {
                percentPlankton[i] = forcing.getPlankton(i).calculPercent(preySizeMin, preySizeMax);
            }
        }
        return percentPlankton;
    }

    /*
     * Get the accessible biomass that predator can feed on prey
     */
    private double getAccessibleBiomass(School predator, School prey) {
        return getAccessibility(predator, prey) * getBiomass(prey);
    }

    /*
     * Get the accessible biomass that predator can feed on prey
     */
    private double getAccessibility(School predator, School prey) {
        return getOsmose().accessibilityMatrix[prey.getCohort().getSpecies().getIndex()][prey.getAccessibilityStage()][predator.getCohort().getSpecies().getIndex()][predator.getAccessibilityStage()];
    }

    /**
     * Returns a list of preys for a given predator.
     *
     * @param predator
     * @return the list of preys for this predator
     */
    private int[] findPreys(School predator) {

        Species spec = predator.getCohort().getSpecies();
        List<School> schoolsInCell = predator.getCell();
        //schoolsInCell.remove(predator);
        float preySizeMax = predator.getLength() / spec.predPreySizesMax[predator.getFeedingStage()];
        float preySizeMin = predator.getLength() / spec.predPreySizesMin[predator.getFeedingStage()];
        List<Integer> indexPreys = new ArrayList();
        for (int iPrey = 0; iPrey < schoolsInCell.size(); iPrey++) {
            School prey = schoolsInCell.get(iPrey);
            if (prey.equals(predator)) {
                continue;
            }
            if (prey.getLength() >= preySizeMin && prey.getLength() < preySizeMax) {
                indexPreys.add(iPrey);
            }
        }
        int[] index = new int[indexPreys.size()];
        for (int iPrey = 0; iPrey < indexPreys.size(); iPrey++) {
            index[iPrey] = indexPreys.get(iPrey);
        }
        return index;
    }

    private void clearCatchesIndicators() {
        //Initialisation of indicators tables
        for (int i = 0; i < species.length; i++) {
            tabSizeCatch[i] = new float[species[i].nbSchoolsTotCatch];
            tabNbCatch[i] = new float[species[i].nbSchoolsTotCatch];
        }
        for (int i = 0; i < species.length; i++) {
            for (int s = 0; s < species[i].nbSchoolsTotCatch; s++) {
                tabSizeCatch[i][s] = 0;
                tabNbCatch[i][s] = 0;
            }
        }
    }

    private void growth() {
        for (School school : getSchools()) {
            school.predSuccessRate = computePredSuccessRate(school.biomassToPredate, school.preyedBiomass);
//            if (school.getCohort().getSpecies().getIndex() == 0)
//                System.out.println(school.predSuccessRate);
        }
        for (int i = 0; i < species.length; i++) {
            if (species[i].getAbundance() != 0) {
                species[i].growth();
            }
        }
    }

    private void updateSpecies() {
        for (int i = 0; i < species.length; i++) {
            species[i].update();
            if (meanSizeOutput) {
                species[i].calculSizes();
                species[i].calculSizesCatch();
            }
            if ((TLoutput) || (TLDistriboutput)) {
                species[i].calculTL();
            }
        }
    }

    private void reproduction() {
        for (int i = 0; i < species.length; i++) {
            /*
             * phv 2011/11/22 Added species that can reproduce outside the
             * simulated domain and we only model an incoming flux of biomass.
             */
            if (species[i].isReproduceLocally()) {
                species[i].reproduce();
            } else {
                species[i].incomingFlux();
            }
        }
    }

    public void step() {

        // Print in console the period already simulated
        printProgress();
        Indicators.reset();

        // Calculation of relative size of MPA
        RS = getRatioMPA();

        // Loop over the year
        while (indexTime < nbTimeStepsPerYear) {

            // Clear some tables and update some stages at the begining of the step
            clearNbDeadArrays();
            updateStages();

            // Update LTL Data
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.runLTLModel();
            }
            forcing.updatePlankton(indexTime);

            // Spatial distribution (distributeSpeciesIni() for year0 & indexTime0)
            if (!((indexTime == 0) && (year == 0))) {
                distributeSpecies();
            }

            // Preliminary actions before mortality processes
            assessCatchableSchools();
            rankSchoolsSizes();
            saveBiomassBeforeMortality();
            clearCatchesIndicators();
            for (School school : getSchools()) {
                school.resetDietVariables();
            }

            // Compute mortality
            // (predation + fishing + natural mortality + starvation)
            for (School school : getSchools()) {
                school.nDeadFishing = 0;
                school.nDeadNatural = 0;
                school.nDeadPredation = 0;
                school.nDeadStarvation = 0;
                school.biomassToPredate = computeBiomassToPredate(school, 1);
                school.preyedBiomass = 0;
            }

            for (int t = 0; t < SUB_DT; t++) {
                computeMortality(SUB_DT, ALGO_MORTALITY);
                updatePopulation();
            }

            // Update of disappeared schools and plancton mortality
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.calculPlanktonMortality();
            }

            // Growth
            growth();

            // Save steps
            updateSpecies();
            if (getOsmose().spatializedOutputs[numSerie]) {
                saveSpatializedStep();
            }
            Indicators.updateAndWriteIndicators();

            // Reproduction
            reproduction();

            // Increment time step
            indexTime++;
        }
        indexTime = 0;  //end of the year
        year++; // go to following year

    }

    public double[][] computeMortality_case3(int subdt, Cell cell) {

        int ns = cell.size();
        int npl = forcing.getNbPlanktonGroups();
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];

        int[] seqPred = new int[ns];
        for (int i = 0; i < ns; i++) {
            cell.get(i).hasPredated = false;
            seqPred[i] = i;
        }
        int[] seqFish = Arrays.copyOf(seqPred, ns);
        int[] seqNat = Arrays.copyOf(seqPred, ns);
        int[] seqStarv = Arrays.copyOf(seqPred, ns);
        shuffleArray(seqPred);
        shuffleArray(seqFish);
        shuffleArray(seqNat);
        shuffleArray(seqStarv);
        int[] mortalitySource = new int[]{0, 1, 2, 3};
        // 0 = predation
        // 1 = starvation
        // 2 = natural
        // 3 = fishing

        for (int i = 0; i < ns; i++) {
            shuffleArray(mortalitySource);
            for (int j = 0; j < mortalitySource.length; j++) {
                School predator;
                switch (mortalitySource[j]) {
                    case 0:
                        // Predation mortality
                        predator = cell.get(seqPred[i]);
                        double[] preyUpon = computePredation(predator, subdt);
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School school = cell.get(ipr);
                                nDeadMatrix[ipr][seqPred[i]] = school.biom2abd(preyUpon[ipr]);
                                school.nDeadPredation += nDeadMatrix[ipr][seqPred[i]];
                            } else {
                                nDeadMatrix[ipr][seqPred[i]] = preyUpon[ipr];
                            }

                        }
                        predator.hasPredated = true;
                        predator.predSuccessRate = computePredSuccessRate(computeBiomassToPredate(predator, subdt), sum(preyUpon));
                        break;
                    case 1:
                        // Starvation mortality
                        predator = cell.get(seqStarv[i]);
                        if (predator.hasPredated) {
                            nDeadMatrix[seqStarv[i]][ns] = computeStarvationMortality(predator, subdt);
                            predator.nDeadStarvation = nDeadMatrix[seqStarv[i]][ns];
                        }
                        break;
                    case 2:
                        // Natural mortality
                        nDeadMatrix[seqNat[i]][ns + 1] = computeNaturalMortality(cell.get(seqNat[i]), subdt);
                        cell.get(seqNat[i]).nDeadNatural = nDeadMatrix[seqNat[i]][ns + 1];
                        break;
                    case 3:
                        // Fishing Mortality
                        nDeadMatrix[seqFish[i]][ns + 2] = computeFishingMortality(cell.get(seqFish[i]), subdt);
                        cell.get(seqFish[i]).nDeadFishing = nDeadMatrix[seqFish[i]][ns + 2];
                        break;
                }
            }
        }

        return nDeadMatrix;
    }

    public double[][] computeMortality_case2(int subdt, Cell cell) {

        int ns = cell.size();
        int npl = forcing.getNbPlanktonGroups();
        double[][] mortalityRateMatrix = new double[ns + npl][ns + 3];
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];
        double[] totalMortalityRate = new double[ns + npl];


        //
        // Assess all mortality independently from each other
        Collections.shuffle(cell);
        for (int ipd = 0; ipd < ns; ipd++) {
            // Predation mortality 
            School predator = cell.get(ipd);
            double[] preyUpon = computePredation(predator, subdt);
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                double predationMortalityRate;
                if (ipr < ns) {
                    School school = cell.get(ipr);
                    nDeadMatrix[ipr][ipd] = school.biom2abd(preyUpon[ipr]);
                    predationMortalityRate = Math.log(getAbundance(school) / (getAbundance(school) - nDeadMatrix[ipr][ipd]));
                    school.nDeadPredation += nDeadMatrix[ipr][ipd];
                } else {
                    nDeadMatrix[ipr][ipd] = preyUpon[ipr];
                    double planktonAbundance = forcing.getPlankton(ipr - ns).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
                    predationMortalityRate = Math.log(planktonAbundance / (planktonAbundance - preyUpon[ipr]));
                }
                mortalityRateMatrix[ipr][ipd] = predationMortalityRate;

            }
            predator.predSuccessRate = computePredSuccessRate(computeBiomassToPredate(predator, subdt), sum(preyUpon));
        }

        for (int is = 0; is < ns; is++) {
            School school = cell.get(is);
            school.nDeadPredation = 0.d;
            // 2. Starvation
            nDeadMatrix[is][ns] = computeStarvationMortality(school, subdt);
            mortalityRateMatrix[is][ns] = getStarvationMortalityRate(school, subdt);

            // 3. Natural mortality
            nDeadMatrix[is][ns + 1] = computeNaturalMortality(school, subdt);
            mortalityRateMatrix[is][ns + 1] = getNaturalMortalityRate(school, subdt);

            // 4. Fishing mortality
            nDeadMatrix[is][ns + 2] = computeFishingMortality(school, subdt);
            mortalityRateMatrix[is][ns + 2] = getFishingMortalityRate(school, subdt);
        }

        //
        // Compute total mortality rate for schools and plankton
        // Sum mortality rates from every source for every school and
        // every plankton group
        for (int ipr = 0; ipr < (ns + npl); ipr++) {
            for (int imort = 0; imort < (ns + 3); imort++) {
                totalMortalityRate[ipr] += mortalityRateMatrix[ipr][imort];
            }
        }

        //
        // Update number of deads
        for (int ipr = 0; ipr < (ns + npl); ipr++) {
            double abundance;
            if (ipr < ns) {
                abundance = cell.get(ipr).getAbundance();
            } else {
                abundance = forcing.getPlankton(ipr - ns).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
            }
            for (int imort = 0; imort < ns + 3; imort++) {
                if (totalMortalityRate[ipr] > 0) {
                    nDeadMatrix[ipr][imort] = (mortalityRateMatrix[ipr][imort] / totalMortalityRate[ipr]) * (1 - Math.exp(-totalMortalityRate[ipr])) * abundance;
                } else {
                    nDeadMatrix[ipr][imort] = 0.d;
                }
            }
        }

        return nDeadMatrix;
    }

    public double[][] computeMortality_case1(int subdt, Cell cell) {

        int ITER_MAX = 50;
        double ERR_MAX = 1.e-5d;

        int ns = cell.size();
        int npl = forcing.getNbPlanktonGroups();
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];
        double[][] mortalityRateMatrix = new double[ns + npl][ns + 3];
        double[] totalMortalityRate = new double[ns + npl];
        double[] correctionFactor = new double[ns];

        //
        // Initialize the number of deads and the mortality rates
        double[][] predationMatrix = computePredationMatrix(cell, subdt);
        for (int ipr = 0; ipr < (ns + npl); ipr++) {
            for (int ipd = 0; ipd < ns; ipd++) {
                double predationMortalityRate;
                if (ipr < ns) {
                    School school = cell.get(ipr);
                    nDeadMatrix[ipr][ipd] = school.biom2abd(predationMatrix[ipd][ipr]);
                    predationMortalityRate = Math.log(school.getAbundance() / (school.getAbundance() - nDeadMatrix[ipr][ipd]));
                } else {
                    nDeadMatrix[ipr][ipd] = predationMatrix[ipd][ipr];
                    double planktonAbundance = forcing.getPlankton(ipr - ns).biomass[cell.get_igrid()][cell.get_jgrid()];
                    if (planktonAbundance > 0) {
                        predationMortalityRate = Math.log(planktonAbundance / (planktonAbundance - predationMatrix[ipd][ipr]));
                    } else {
                        predationMortalityRate = 0;
                    }
                }
                mortalityRateMatrix[ipr][ipd] = predationMortalityRate;
            }
        }
        for (int is = 0; is < ns; is++) {
            School school = cell.get(is);
            // 2. Starvation
            // computes preyed biomass by school ipr
            double preyedBiomass = 0;
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                preyedBiomass += predationMatrix[is][ipr];
            }
            school.predSuccessRate = computePredSuccessRate(computeBiomassToPredate(school, subdt), preyedBiomass);
            mortalityRateMatrix[is][ns] = getStarvationMortalityRate(school, subdt);

            // 3. Natural mortality
            mortalityRateMatrix[is][ns + 1] = getNaturalMortalityRate(school, subdt);

            // 4. Fishing mortality
            mortalityRateMatrix[is][ns + 2] = getFishingMortalityRate(school, subdt);
        }

        //
        // Compute total mortality rate for schools and plankton
        // Sum mortality rates from every source for every school and
        // every plankton group
        for (int ipr = 0; ipr < (ns + npl); ipr++) {
            for (int imort = 0; imort < (ns + 3); imort++) {
                totalMortalityRate[ipr] += mortalityRateMatrix[ipr][imort];
            }
        }

        //
        // Begining of iteration
        int iteration = 0;
        double error = Double.MAX_VALUE;
        while ((iteration < ITER_MAX) && error > ERR_MAX) {

            // Update number of deads
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                double abundance;
                if (ipr < ns) {
                    abundance = cell.get(ipr).getAbundance();
                } else {
                    abundance = forcing.getPlankton(ipr - ns).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
                }
                for (int imort = 0; imort < ns + 3; imort++) {
                    if (totalMortalityRate[ipr] > 0) {
                        nDeadMatrix[ipr][imort] = (mortalityRateMatrix[ipr][imort] / totalMortalityRate[ipr]) * (1 - Math.exp(-totalMortalityRate[ipr])) * abundance;
                    } else {
                        nDeadMatrix[ipr][imort] = 0.d;
                    }
                }
            }

            // Compute correction factor
            for (int ipd = 0; ipd < ns; ipd++) {
                School predator = cell.getSchool(ipd);
                double preyedBiomass = 0;
                for (int ipr = 0; ipr < (ns + npl); ipr++) {
                    if (ipr < ns) {
                        preyedBiomass += cell.getSchool(ipr).adb2biom(nDeadMatrix[ipr][ipd]);
                    } else {
                        preyedBiomass += nDeadMatrix[ipr][ipd];
                    }
                    //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                }
                double biomassToPredate = computeBiomassToPredate(predator, subdt);
                predator.predSuccessRate = computePredSuccessRate(biomassToPredate, preyedBiomass);
                if (preyedBiomass > 0) {
                    correctionFactor[ipd] = Math.min(biomassToPredate / preyedBiomass, 1.d);
                } else {
                    correctionFactor[ipd] = 1;
                }
            }

            // Update mortality rates
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                // 1. Predation
                for (int ipd = 0; ipd < ns; ipd++) {
                    mortalityRateMatrix[ipr][ipd] *= correctionFactor[ipd];
                }
            }
            for (int ipr = 0; ipr < ns; ipr++) {
                School school = cell.get(ipr);
                // 2. Starvation
                // computes preyed biomass by school ipr
                mortalityRateMatrix[ipr][ns] = getStarvationMortalityRate(school, subdt);
                // 3. Natural mortality, unchanged
                // 4. Fishing, unchanged
            }

            // Convergence test
            double[] oldTotalMortalityRate = Arrays.copyOf(totalMortalityRate, totalMortalityRate.length);
            error = 0.d;
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                totalMortalityRate[ipr] = 0.d;
                for (int imort = 0; imort < (ns + 3); imort++) {
                    totalMortalityRate[ipr] += mortalityRateMatrix[ipr][imort];
                }
                error = Math.max(error, Math.abs(totalMortalityRate[ipr] - oldTotalMortalityRate[ipr]));
            }
            iteration++;
        }

        //
        // return the number of deads matrix
        return nDeadMatrix;
    }

    /**
     * New function that encompasses all kind of mortality faced by the schools:
     * natural mortality, predation, fishing and starvation. we assume all
     * mortality sources are independent, compete against each other but act
     * simultaneously.
     */
    public void computeMortality(int subdt, AlgoMortality mcase) {

        double[][] mortality = null;
        if (DEBUG_MORTALITY) {
            mortality = new double[getNbSpecies()][11];
            for (int i = 0; i < species.length; i++) {
                int indexRecruitAge = Math.round(species[i].recruitAge * nbTimeStepsPerYear);
                for (int j = indexRecruitAge; j < species[i].getNumberCohorts(); j++) {
                    mortality[i][10] += species[i].getCohort(j).getAbundance();
                }
            }
        }

        for (Cell cell : getGrid().getCells()) {
            if (!(cell.isLand() || cell.isEmpty())) {
                int ns = cell.size();
                int npl = forcing.getNbPlanktonGroups();

                // Reset nDeads
                for (School school : cell) {
                    school.nDeadPredation = 0;
                    school.nDeadStarvation = 0;
                    school.nDeadNatural = 0;
                    school.nDeadFishing = 0;
                }

                double[][] nDeadMatrix = null;
                switch (mcase) {
                    case CASE1:
                        nDeadMatrix = computeMortality_case1(subdt, cell);
                        break;
                    case CASE2:
                        nDeadMatrix = computeMortality_case2(subdt, cell);
                        break;
                    case CASE3:
                        nDeadMatrix = computeMortality_case3(subdt, cell);
                        break;
                }

                // Apply mortalities
                for (int is = 0; is < ns; is++) {
                    School school = cell.get(is);
                    // 1. Predation
                    school.nDeadPredation = 0.d;
                    double preyedBiomass = 0.d;
                    for (int ipd = 0; ipd < ns; ipd++) {
                        school.nDeadPredation += nDeadMatrix[is][ipd];
                    }
                    for (int ipr = 0; ipr < ns + npl; ipr++) {
                        if (ipr < ns) {
                            preyedBiomass += cell.get(ipr).adb2biom(nDeadMatrix[ipr][is]);
                        } else {
                            preyedBiomass += nDeadMatrix[ipr][is];
                        }
                    }
                    school.preyedBiomass += preyedBiomass;
                    // update TL
                    if (preyedBiomass > 0.d) {
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School prey = cell.getSchool(ipr);
                                double biomPrey = prey.adb2biom(nDeadMatrix[ipr][is]);
                                if (dietsOutput) {
                                    school.dietTemp[prey.getCohort().getSpecies().getIndex()][prey.dietOutputStage] += biomPrey;
                                }
                                float TLprey;
                                if ((prey.getCohort().getAgeNbDt() == 0) || (prey.getCohort().getAgeNbDt() == 1)) {
                                    TLprey = prey.getCohort().getSpecies().TLeggs;
                                } else {
                                    TLprey = prey.trophicLevel[prey.getCohort().getAgeNbDt() - 1];
                                }
                                school.trophicLevel[school.getCohort().getAgeNbDt()] += TLprey * biomPrey / preyedBiomass;
                            } else {
                                school.trophicLevel[school.getCohort().getAgeNbDt()] += forcing.getPlankton(ipr - ns).trophicLevel * nDeadMatrix[ipr][is] / preyedBiomass;
                                if (dietsOutput) {
                                    school.dietTemp[getNbSpecies() + (ipr - ns)][0] += nDeadMatrix[ipr][is];
                                }
                            }
                            //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                        }
                        school.trophicLevel[school.getCohort().getAgeNbDt()] += 1;
                    } else if (school.getCohort().getAgeNbDt() > 0) {
                        school.trophicLevel[school.getCohort().getAgeNbDt()] = school.trophicLevel[school.getCohort().getAgeNbDt() - 1];
                    }

                    // 2. Starvation
                    school.nDeadStarvation = nDeadMatrix[is][ns];
                    // 3. Natural mortality
                    school.nDeadNatural = nDeadMatrix[is][ns + 1];
                    // 4. Fishing
                    school.nDeadFishing = nDeadMatrix[is][ns + 2];

                    // Update abundance
                    double nDeadTotal = school.nDeadPredation
                            + school.nDeadStarvation
                            + school.nDeadNatural
                            + school.nDeadFishing;

                    if (DEBUG_MORTALITY) {
                        int i = school.getCohort().getSpecies().getIndex();
                        int indexRecruitAge = Math.round(species[i].recruitAge * nbTimeStepsPerYear);
                        if (school.getCohort().getAgeNbDt() >= indexRecruitAge) {
                            mortality[i][0] += (school.nDeadPredation);
                            mortality[i][2] += (school.nDeadStarvation);
                            mortality[i][4] += (school.nDeadNatural);
                            mortality[i][6] += (school.nDeadFishing);
                        }
                    }

                    school.setAbundance(school.getAbundance() - nDeadTotal);
                    if (school.getAbundance() < 1.d) {
                        school.setAbundance(0.d);
                        school.tagForRemoval();
                        school.getCohort().setNbSchoolsCatchable(school.getCohort().getNbSchoolsCatchable() - 1);
                    }
                }
            }
        }

        if (DEBUG_MORTALITY) {
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < 4; j++) {
                    // Total nDeads
                    mortality[i][8] += mortality[i][2 * j];
                }
                // Ftotal
                mortality[i][9] = Math.log(mortality[i][10] / (mortality[i][10] - mortality[i][8]));
                for (int j = 0; j < 4; j++) {
                    mortality[i][2 * j + 1] = mortality[i][9] * mortality[i][2 * j] / ((1 - Math.exp(-mortality[i][9])) * mortality[i][10]);
                }
            }
            String filename = "nDead_Simu" + getOsmose().numSimu + ".csv";
            String[] headers = new String[]{"Predation", "Fpred", "Starvation", "Fstarv", "Natural", "Fnat", "Fishing", "Ffish", "Total", "Ftotal", "Abundance"};
            Indicators.writeVariable(year + (indexTime + 1f) / (float) nbTimeStepsPerYear, mortality, filename, headers, "Instaneous number of deads and mortality rates");
        }
    }

    public void updateBiomassAndAbundance() {
        // Removing dead schools
        // Update biomass of schools & cohort & species
        for (int i = 0; i < species.length; i++) {
            species[i].resetAbundance();
            species[i].resetBiomass();
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                Cohort cohort = species[i].getCohort(j);
                cohort.removeDeadSchools();
                cohort.setAbundance(0.d);
                cohort.setBiomass(0.d);
                for (School school : cohort) {
                    cohort.incrementAbundance(school.getAbundance());
                    cohort.incrementBiomass(school.getBiomass());
                }
                species[i].incrementAbundance(cohort.getAbundance());
                species[i].incrementBiomass(cohort.getBiomass());
            }
        }
    }

    /*
     * Update biomass and abundance from School to Species, throught Cohort.
     */
    public void updatePopulation() {
        // Removing dead schools
        // Update biomass of schools & cohort & species
        for (int i = 0; i < species.length; i++) {
            int indexRecruitAge = Math.round(species[i].recruitAge * nbTimeStepsPerYear);
            species[i].resetAbundance();
            species[i].resetBiomass();
            species[i].yield = 0.d;
            species[i].yieldN = 0.d;
            species[i].tabTLCatch = 0.f;
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                Cohort cohort = species[i].getCohort(j);
                cohort.removeDeadSchools();
                cohort.setAbundance(0.d);
                cohort.setBiomass(0.d);
                cohort.setAbundanceCatchable(0);
                int iSchool = 0;
                for (School school : cohort) {
                    cohort.incrementAbundance(school.getAbundance());
                    cohort.incrementBiomass(school.getBiomass());
                    school.updateDietVariables();
                    if (school.isCatchable() && j >= indexRecruitAge) {
                        cohort.incrementAbundanceCatchable(school.getAbundance());
                        // update fihsing indicators
                        cohort.nbDeadFf += school.nDeadFishing;
                        tabNbCatch[i][iSchool + species[i].cumulCatch[cohort.getAgeNbDt() - 1]] += school.nDeadFishing;
                        tabSizeCatch[i][iSchool + species[i].cumulCatch[cohort.getAgeNbDt() - 1]] = school.getLength();
                        if ((getYear()) >= getOsmose().timeSeriesStart) {
                            species[i].yield += school.adb2biom(school.nDeadFishing);
                            species[i].yieldN += school.nDeadFishing;
                            if (TLoutput) {
                                species[i].tabTLCatch += school.trophicLevel[cohort.getAgeNbDt()] * school.adb2biom(school.nDeadFishing);
                            }
                        }
                        iSchool++;
                    }
                }
                species[i].incrementAbundance(cohort.getAbundance());
                species[i].incrementBiomass(cohort.getBiomass());
            }
        }
    }

    public void iniBySizeSpectrum() //************************************* A VERIFIER : � adapter eu nouveau pas de temps si besoin**************************
    //initialisation according to a spectrum [10cm], from 0 to 200cm
    {
        long[] tempSpectrumAbd = new long[20];
        /*
         * tab of vectors of species belonging to [0-10[....[140-150[
         */
        Vector[] specInSizeClass10 = new Vector[20];    //20 classes size 0 a 200
        for (int i = 0; i < specInSizeClass10.length; i++) {
            specInSizeClass10[i] = new Vector(species.length);
        }
        abdIniMin = 100;
        //a=-5.8;
        //b=35.5;
        a = getOsmose().SSslope[numSerie];
        b = getOsmose().SSintercept[numSerie];
        //Calculation of abd lacking in each size class
        //calculation apart for first size class because minSize=0.05 (and not 0)
        tempSpectrumAbd[0] = Math.round(Math.pow(5., a) * Math.exp(b));
        for (int i = 1; i < 20; i++) {
            tempSpectrumAbd[i] = Math.round(Math.pow((i * getOsmose().classRange) + 5., a) * Math.exp(b));
        }
        //tabSizes10[i]+5 is mean length of [tabSizes10[i],tabSizes10[i+1][
        //Sort the Lmax of each species in each size class
        for (int i = 0; i < species.length; i++) {
            int index1 = tempSpectrumAbd.length - 1;
            while (species[i].tabMeanLength[species[i].getNumberCohorts() - 1] < (index1 * getOsmose().classRange)) {
                index1--;
            }
            specInSizeClass10[index1].addElement(species[i]);
        }
        //calculate spectrumMaxIndex
        int spectrumMaxIndex = specInSizeClass10.length - 1;
        while (specInSizeClass10[spectrumMaxIndex].isEmpty()) {
            spectrumMaxIndex--;
        }

        //Calculate abd species and cohorts
        for (int i = spectrumMaxIndex; i >= 0; i--) {
            for (int j = 0; j < specInSizeClass10[i].size(); j++) {
                Species speciesj = ((Species) specInSizeClass10[i].elementAt(j));
                speciesj.tabAbdIni[speciesj.getNumberCohorts() - 1] = Math.round(((double) tempSpectrumAbd[i])
                        / specInSizeClass10[i].size());
                speciesj.tabBiomIni[speciesj.getNumberCohorts() - 1] = ((double) speciesj.tabAbdIni[speciesj.getNumberCohorts() - 1]) * speciesj.tabMeanWeight[speciesj.getNumberCohorts() - 1] / 1000000.;
                //we consider that D0->1 = 10 for the first age class (month or year, whatever nbDt), D0-1year->2 = 1 and D=0.4 otherwise
                //we calculate abd & biom of coh, and in parallel abd & biom of species & we create cohorts
                speciesj.resetAbundance();
                speciesj.resetBiomass();
                speciesj.incrementAbundance(speciesj.tabAbdIni[speciesj.getNumberCohorts() - 1]);
                speciesj.incrementBiomass(speciesj.tabBiomIni[speciesj.getNumberCohorts() - 1]);

                for (int k = speciesj.getNumberCohorts() - 2; k >= (2 * nbTimeStepsPerYear); k--) {
                    speciesj.tabAbdIni[k] = Math.round(speciesj.tabAbdIni[k + 1] * Math.exp((0.5 / (float) nbTimeStepsPerYear)));
                    speciesj.tabBiomIni[k] = ((double) speciesj.tabAbdIni[k]) * speciesj.tabMeanWeight[k] / 1000000.;
                    speciesj.incrementAbundance(speciesj.tabAbdIni[k]);
                    speciesj.incrementBiomass(speciesj.tabBiomIni[k]);
                }
                int kTemp;
                if (speciesj.longevity <= 1) {
                    kTemp = speciesj.getNumberCohorts() - 2;
                } else {
                    kTemp = (2 * nbTimeStepsPerYear) - 1;
                }

                for (int k = kTemp; k >= 1; k--) {
                    speciesj.tabAbdIni[k] = Math.round(speciesj.tabAbdIni[k + 1] * Math.exp((1. / (float) nbTimeStepsPerYear)));
                    speciesj.tabBiomIni[k] = ((double) speciesj.tabAbdIni[k]) * speciesj.tabMeanWeight[k] / 1000000.;
                    speciesj.incrementAbundance(speciesj.tabAbdIni[k]);
                    speciesj.incrementBiomass(speciesj.tabBiomIni[k]);
                }

                speciesj.tabAbdIni[0] = Math.round(speciesj.tabAbdIni[1] * Math.exp(10.));
                speciesj.tabBiomIni[0] = ((double) speciesj.tabAbdIni[0]) * speciesj.tabMeanWeight[0] / 1000000.;
                /*
                 * 2011/04/11 phv : commented line since nbEggs is only used in
                 * Species.reproduce as local variable.
                 */
                //speciesj.nbEggs = speciesj.tabAbdIni[0];
                speciesj.incrementAbundance(speciesj.tabAbdIni[0]);
                speciesj.incrementBiomass(speciesj.tabBiomIni[0]);
                //creation of the cohorts
                for (int k = 0; k < speciesj.getNumberCohorts(); k++) {
                    speciesj.setCohort(k, new Cohort(speciesj, k, speciesj.tabAbdIni[k], speciesj.tabBiomIni[k], speciesj.tabMeanLength[k], speciesj.tabMeanWeight[k]));
                }
            }
        }
    }

    public void iniRandomly() //************************** Nouvelle option : A faire
    {
    }

    public void iniBySpeciesBiomass() {
        float correctingFactor;
        double abdIni;

        for (int i = 0; i < species.length; i++) {
            //We calculate abd & biom ini of cohorts, and in parallel biom of species
            Species speci = species[i];
            speci.resetAbundance();
            speci.resetBiomass();
            double sumExp = 0;
            /*
             * phv 2011/11/24 For species that do not reproduce locally, initial
             * biomass is set to zero.
             */
            if (!speci.isReproduceLocally()) {
                for (int j = 0; j < speci.getCohorts().length; j++) {
                    speci.tabAbdIni[j] = 0;
                    speci.tabBiomIni[j] = 0;
                }
            } else {
                abdIni = getOsmose().spBiomIniTab[numSerie][i] / (speci.tabMeanWeight[(int) Math.round(speci.getNumberCohorts() / 2)] / 1000000);

                for (int j = speci.indexAgeClass0; j < speci.getNumberCohorts(); j++) {
                    sumExp += Math.exp(-(j * (speci.D + speci.F + 0.5f) / (float) nbTimeStepsPerYear)); //0.5 = approximation of average natural mortality (by predation, senecence...)
                }
                speci.tabAbdIni[0] = (long) ((abdIni) / (Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
                speci.tabBiomIni[0] = ((double) speci.tabAbdIni[0]) * speci.tabMeanWeight[0] / 1000000.;
                if (speci.indexAgeClass0 <= 0) {
                    speci.incrementBiomass(speci.tabBiomIni[0]);
                }

                speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear));
                speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
                if (speci.indexAgeClass0 <= 1) {
                    speci.incrementBiomass(speci.tabBiomIni[1]);
                }

                for (int j = 2; j < speci.getNumberCohorts(); j++) {
                    speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbTimeStepsPerYear));
                    speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                    if (speci.indexAgeClass0 <= j) {
                        speci.incrementBiomass(speci.tabBiomIni[j]);
                    }
                }
                correctingFactor = (float) (getOsmose().spBiomIniTab[numSerie][i] / speci.getBiomass());

                // we make corrections on initial abundance to fit the input biomass
                speci.resetBiomass();

                speci.tabAbdIni[0] = (long) ((abdIni * correctingFactor) / (Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
                speci.tabBiomIni[0] = ((double) speci.tabAbdIni[0]) * speci.tabMeanWeight[0] / 1000000.;
                speci.incrementAbundance(speci.tabAbdIni[0]);
                speci.incrementBiomass(speci.tabBiomIni[0]);
                /*
                 * 2011/04/11 phv : commented line since nbEggs is only used in
                 * Species.reproduce as local variable.
                 */
                //speci.nbEggs = speci.tabAbdIni[0];

                speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear));
                speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
                speci.incrementAbundance(speci.tabAbdIni[1]);
                speci.incrementBiomass(speci.tabBiomIni[1]);

                for (int j = 2; j < speci.getNumberCohorts(); j++) {
                    speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbTimeStepsPerYear));
                    speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                    speci.incrementAbundance(speci.tabAbdIni[j]);
                    speci.incrementBiomass(speci.tabBiomIni[j]);
                }
            }
            // and we create the cohorts
            for (int j = 0; j < speci.getNumberCohorts(); j++) {
                speci.setCohort(j, new Cohort(speci, j, speci.tabAbdIni[j], speci.tabBiomIni[j], speci.tabMeanLength[j], speci.tabMeanWeight[j]));
            }
        }
    }

    public void iniPlanktonField(boolean isForcing) {

        if (isForcing) {
            coupling = null;
            try {
                try {
                    forcing = (LTLForcing) Class.forName(getOsmose().getLTLClassName()).newInstance();
                } catch (InstantiationException ex) {
                    Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                try {
                    coupling = (LTLCoupling) Class.forName(getOsmose().getLTLClassName()).newInstance();
                    forcing = coupling;
                    coupling.readCouplingConfigFile(getOsmose().couplingFileNameTab[numSerie]);
                } catch (InstantiationException ex) {
                    Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        forcing.readLTLConfigFile1(getOsmose().planktonStructureFileNameTab[numSerie]);
        forcing.readLTLConfigFile2(getOsmose().planktonFileNameTab[numSerie]);
        forcing.initPlanktonMap();
    }

    public List<School> getSchools() {
        ArrayList<School> schools = new ArrayList();
        for (Species sp : species) {
            for (Cohort cohort : sp.getCohorts()) {
                if (!cohort.isOut(indexTime)) {
                    schools.addAll(cohort);
                }
            }
        }
        return schools;
    }

    public void rankSchoolsSizes() {

        Iterator<Cell> iterator = getGrid().getCells().iterator();
        while (iterator.hasNext()) {
            iterator.next().sortSchoolsByLength();
        }
    }

    //update remaining schools in coh.vectSchools & vectPresentSchools(according to disappears)
    public void assessNbSchools() {
        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                for (int k = species[i].getCohort(j).size() - 1; k >= 0; k--) {
                    if (((School) species[i].getCohort(j).getSchool(k)).willDisappear()) {
                        species[i].getCohort(j).remove(k);
                    }
                }
                species[i].getCohort(j).trimToSize();
            }
        }
    }

    public void assessPresentSchools() {
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                for (int k = getGrid().getCell(i, j).size() - 1; k >= 0; k--) {
                    if (((School) getGrid().getCell(i, j).getSchool(k)).willDisappear()) {
                        getGrid().getCell(i, j).remove(k);
                    }
                }
                getGrid().getCell(i, j).trimToSize();
            }
        }
    }

    public void assessAbdCohSpec() {
        //update abd cohort and abd species
        for (int i = 0; i < species.length; i++) {
            species[i].resetAbundance();
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                species[i].getCohort(j).setAbundance(0);
            }
        }
        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                    species[i].getCohort(j).incrementAbundance(species[i].getCohort(j).getSchool(k).getAbundance());
                }
                species[i].incrementAbundance(species[i].getCohort(j).getAbundance());
            }
        }
    }

    public void distributeSpeciesIni() //***NEW: correspond to distributeSpecies for initialization
    {
        if (randomDistribution) {
            for (int i = 0; i < species.length; i++) {
                List<Cell> cells = new ArrayList(getOsmose().randomAreaCoordi[i].length);
                for (int m = 0; m < getOsmose().randomAreaCoordi[i].length; m++) {
                    cells.add(getGrid().getCell(getOsmose().randomAreaCoordi[i][m], getOsmose().randomAreaCoordj[i][m]));
                }
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        ((School) species[i].getCohort(j).getSchool(k)).randomDeal(cells);
                        ((School) species[i].getCohort(j).getSchool(k)).communicatePosition();
                    }
                }
            }
        } else//species areas given by file
        {
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    if (!species[i].getCohort(j).isOut(0)) // 0=at the first time step
                    {
                        List<Cell> cells = new ArrayList(getOsmose().mapCoordi[getOsmose().numMap[i][j][0]].length);
                        tempMaxProbaPresence = 0;
                        for (int m = 0; m < getOsmose().mapCoordi[getOsmose().numMap[i][j][0]].length; m++) {
                            cells.add(getGrid().getCell(getOsmose().mapCoordi[getOsmose().numMap[i][j][0]][m], getOsmose().mapCoordj[getOsmose().numMap[i][j][0]][m]));
                            tempMaxProbaPresence = Math.max(tempMaxProbaPresence, (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][0]][m]));
                        }

                        for (int k = 0; k < Math.round((float) species[i].getCohort(j).size() * (1 - species[i].getCohort(j).getOutOfZonePercentage()[0] / 100)); k++) {
                            // proba of presence: loop while to check if proba of presence> random proba
                            School thisSchool = (School) species[i].getCohort(j).getSchool(k);
                            thisSchool.randomDeal(cells);
                            while ((float) getOsmose().mapProbaPresence[getOsmose().numMap[i][j][0]][thisSchool.indexij] < (float) Math.random() * tempMaxProbaPresence) {
                                thisSchool.randomDeal(cells);
                            }
                            thisSchool.communicatePosition();
                        }
                    }
                }
            }
        }//end file areas
    }

    public void distributeSpecies() {
        /*
         * Clear all cells (to make sure we remove the last age class)
         */
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                getGrid().getCell(i, j).clear();
            }
        }
        /*
         * Random distribution
         */
        if (randomDistribution) {
            for (int i = 0; i < species.length; i++) {
                List<Cell> cells = new ArrayList(getOsmose().randomAreaCoordi[i].length);
                for (int m = 0; m < getOsmose().randomAreaCoordi[i].length; m++) {
                    cells.add(getGrid().getCell(getOsmose().randomAreaCoordi[i][m], getOsmose().randomAreaCoordj[i][m]));
                }
                for (Cohort cohort : species[i].getCohorts()) {
                    for (School school : cohort) {
                        if (school.isUnlocated()) {
                            school.randomDeal(cells);
                        } else {
                            school.randomWalk();
                        }
                        school.communicatePosition();
                    }
                }
            }
        } else {
            /*
             * Species areas given by file
             */
            for (int i = 0; i < species.length; i++) {
                /*
                 * phv 2011/11/29 There is no reason to distribute species that
                 * are presently out of the simulated area.
                 */
                if (species[i].getCohort(0).isOut(indexTime)) {
                    for (School school : species[i].getCohort(0)) {
                        school.moveOut();
                    }
                    continue;
                }

                List<Cell> cellsCohort0 = new ArrayList(getOsmose().mapCoordi[(getOsmose().numMap[i][0][indexTime])].length);
                tempMaxProbaPresence = 0;
                for (int j = 0; j < getOsmose().mapCoordi[(getOsmose().numMap[i][0][indexTime])].length; j++) {
                    cellsCohort0.add(getGrid().getCell(getOsmose().mapCoordi[(getOsmose().numMap[i][0][indexTime])][j], getOsmose().mapCoordj[(getOsmose().numMap[i][0][indexTime])][j]));
                    tempMaxProbaPresence = Math.max(tempMaxProbaPresence, getOsmose().mapProbaPresence[getOsmose().numMap[i][0][indexTime]][j]);
                }

                for (int k = 0; k < Math.round((float) species[i].getCohort(0).size() * (1f - (species[i].getCohort(0).getOutOfZonePercentage()[indexTime] / 100f))); k++) {
                    School thisSchool = (School) species[i].getCohort(0).getSchool(k);
                    thisSchool.randomDeal(cellsCohort0);
                    while ((float) getOsmose().mapProbaPresence[getOsmose().numMap[i][0][indexTime]][thisSchool.indexij] < (float) Math.random() * tempMaxProbaPresence) {
                        thisSchool.randomDeal(cellsCohort0);
                    }
                    thisSchool.communicatePosition();
                }

                //compare areas (ages to end): age a, sem2 with age a+1, sem 1
                // if diff, distribute
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    /*
                     * phv 2011/11/29 There is no reason to distribute species
                     * that are presently out of the simulated area.
                     */
                    if (species[i].getCohort(j).isOut(indexTime)) {
                        for (School school : species[i].getCohort(j)) {
                            school.moveOut();
                        }
                        continue;
                    }

                    int oldTime;
                    if (indexTime == 0) {
                        oldTime = nbTimeStepsPerYear - 1;
                    } else {
                        oldTime = indexTime - 1;
                    }

                    boolean idem = false;
                    if (getOsmose().numMap[i][j][indexTime] == getOsmose().numMap[i][j - 1][oldTime]) {
                        idem = true;
                    }

                    if (!idem) {
                        // distribute in new area
                        List<Cell> cells = new ArrayList(getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])].length);
                        //System.out.println(species[i].getName() + " cohort " + species[i].getCohort(j).getAgeNbDt() + " step " + indexTime + " map " + getOsmose().numMap[i][j][indexTime] + " nbcells " + getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])].length);
                        tempMaxProbaPresence = 0;
                        for (int m = 0; m < getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])].length; m++) {
                            cells.add(getGrid().getCell(getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])][m], getOsmose().mapCoordj[(getOsmose().numMap[i][j][indexTime])][m]));
                            tempMaxProbaPresence = Math.max(tempMaxProbaPresence, (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][indexTime]][m]));
                        }

                        for (int k = 0; k < Math.round((float) species[i].getCohort(j).size() * (100f - species[i].getCohort(j).getOutOfZonePercentage()[indexTime]) / 100f); k++) {
                            School thisSchool = (School) (species[i].getCohort(j).getSchool(k));
                            thisSchool.randomDeal(cells);
                            while (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][indexTime]][thisSchool.indexij] < Math.random() * tempMaxProbaPresence) {
                                thisSchool.randomDeal(cells);
                            }
                            thisSchool.communicatePosition();
                        }

                        //		}

                    } else // stay in the same map
                    {
                        for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                            School thisSchool = (School) (species[i].getCohort(j).getSchool(k));
                            boolean stillInMap = false;
                            if (!thisSchool.isUnlocated()) {
                                thisSchool.randomWalk();

                                for (int p = 0; p < thisSchool.getCell().getNbMapsConcerned(); p++) {
                                    if (((Integer) thisSchool.getCell().numMapsConcerned.elementAt(p)).intValue() == getOsmose().numMap[i][j][indexTime]) {
                                        stillInMap = true;
                                    }
                                }
                            } else {
                                List<Cell> cells = new ArrayList(getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])].length);
                                for (int m = 0; m < getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])].length; m++) {
                                    cells.add(getGrid().getCell(getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])][m], getOsmose().mapCoordj[(getOsmose().numMap[i][j][indexTime])][m]));
                                }
                                thisSchool.randomDeal(cells);
                                stillInMap = false;
                            }

                            if (!stillInMap) {
                                List<Cell> cells = new ArrayList(getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])].length);
                                tempMaxProbaPresence = 0;
                                for (int m = 0; m < getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])].length; m++) {
                                    cells.add(getGrid().getCell(getOsmose().mapCoordi[(getOsmose().numMap[i][j][indexTime])][m], getOsmose().mapCoordj[(getOsmose().numMap[i][j][indexTime])][m]));
                                    tempMaxProbaPresence = Math.max(tempMaxProbaPresence, (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][indexTime]][m]));
                                }

                                while (getOsmose().mapProbaPresence[getOsmose().numMap[i][j][indexTime]][thisSchool.indexij] < Math.random() * tempMaxProbaPresence) {
                                    thisSchool.randomDeal(cells);
                                }
                            }
                            thisSchool.communicatePosition();
                        }
                    }
                }//end loop cohort
            }//end loop species
        }//end file areas
    }

    public void assessCatchableSchools() {

        if ((!getOsmose().thereIsMPATab[numSerie])
                || (year < getOsmose().MPAtStartTab[numSerie])
                || (year >= getOsmose().MPAtEndTab[numSerie]))// case where no MPA
        {
            for (int i = 0; i < species.length; i++) {
                species[i].nbSchoolsTotCatch = 0;
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    Cohort cohij = species[i].getCohort(j);
                    cohij.setNbSchoolsCatchable(cohij.size());
                    //cohij.schoolsCatchable = new Vector(cohij.nbSchoolsCatchable);
                    cohij.setAbundanceCatchable(0);
                    for (int k = 0; k < cohij.size(); k++) {
                        School schoolk = (School) cohij.getSchool(k);
                        //cohij.schoolsCatchable.addElement(schoolk);
                        cohij.setAbundanceCatchable(cohij.getAbundanceCatchable() + schoolk.getAbundance());
                        schoolk.setCatchable(true);
                    }
                }
                species[i].cumulCatch[0] = 0;
                species[i].cumulCatch[0] = species[i].getCohort(0).getNbSchoolsCatchable();
                species[i].nbSchoolsTotCatch += species[i].getCohort(0).getNbSchoolsCatchable();
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    species[i].cumulCatch[j] = 0;
                    species[i].cumulCatch[j] = species[i].cumulCatch[j - 1] + species[i].getCohort(j).getNbSchoolsCatchable();
                    species[i].nbSchoolsTotCatch += species[i].getCohort(j).getNbSchoolsCatchable();
                }
            }
        } else//case MPA
        {
            for (int i = 0; i < species.length; i++) {
                species[i].nbSchoolsTotCatch = 0;
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    Cohort cohij = species[i].getCohort(j);
                    cohij.setNbSchoolsCatchable(0);
                    //cohij.schoolsCatchable = new Vector(getOsmose().nbSchools[numSerie]);
                    cohij.setAbundanceCatchable(0);
                    for (int k = 0; k < cohij.size(); k++) {
                        School schoolk = (School) cohij.getSchool(k);
                        if (schoolk.isUnlocated() || schoolk.getCell().isMPA()) {
                            schoolk.setCatchable(false);
                        } else {
                            schoolk.setCatchable(true);
                            cohij.setNbSchoolsCatchable(cohij.getNbSchoolsCatchable() + 1);
                            //cohij.schoolsCatchable.addElement(schoolk);
                            cohij.setAbundanceCatchable(cohij.getAbundanceCatchable() + schoolk.getAbundance());
                        }
                    }
                    //cohij.schoolsCatchable.trimToSize();
                }
                species[i].cumulCatch[0] = 0;
                species[i].cumulCatch[0] = species[i].getCohort(0).getNbSchoolsCatchable();
                species[i].nbSchoolsTotCatch += species[i].getCohort(0).getNbSchoolsCatchable();
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    species[i].cumulCatch[j] = 0;
                    species[i].cumulCatch[j] = species[i].cumulCatch[j - 1] + species[i].getCohort(j).getNbSchoolsCatchable();
                    species[i].nbSchoolsTotCatch += species[i].getCohort(j).getNbSchoolsCatchable();
                }
            }
        }
    }

    public void initSpatializedSaving() {

        NetcdfFileWriteable nc = getOsmose().getNCOut();
        /*
         * Create dimensions
         */
        Dimension speciesDim = nc.addDimension("species", getNbSpecies());
        Dimension ltlDim = nc.addDimension("ltl", getForcing().getNbPlanktonGroups());
        Dimension columnsDim = nc.addDimension("columns", getGrid().getNbColumns());
        Dimension linesDim = nc.addDimension("lines", getGrid().getNbLines());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        Dimension stepDim = nc.addDimension("step", 2);
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "year");
        nc.addVariableAttribute("time", "description", "time ellapsed, in years, since the begining of the simulation");
        nc.addVariable("biomass", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("biomass", "units", "ton");
        nc.addVariableAttribute("biomass", "description", "biomass, in tons, per species and per cell");
        nc.addVariableAttribute("biomass", "_FillValue", -99.f);
        nc.addVariable("abundance", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("abundance", "units", "number of fish");
        nc.addVariableAttribute("abundance", "description", "Number of fish per species and per cell");
        nc.addVariableAttribute("abundance", "_FillValue", -99.f);
        nc.addVariable("yield", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("yield", "units", "ton");
        nc.addVariableAttribute("yield", "description", "Catches, in tons, per species and per cell");
        nc.addVariableAttribute("yield", "_FillValue", -99.f);
        nc.addVariable("mean_size", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("mean_size", "units", "centimeter");
        nc.addVariableAttribute("mean_size", "description", "mean size, in centimeter, per species and per cell");
        nc.addVariableAttribute("mean_size", "_FillValue", -99.f);
        nc.addVariable("trophic_level", DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("trophic_level", "units", "scalar");
        nc.addVariableAttribute("trophic_level", "description", "trophic level per species and per cell");
        nc.addVariableAttribute("trophic_level", "_FillValue", -99.f);
        nc.addVariable("ltl_biomass", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, stepDim, linesDim, columnsDim});
        nc.addVariableAttribute("ltl_biomass", "units", "ton/km2");
        nc.addVariableAttribute("ltl_biomass", "description", "plankton biomass, in tons per km2 integrated on water column, per group and per cell");
        nc.addVariableAttribute("ltl_biomass", "step", "step=0 before predation, step=1 after predation");
        nc.addVariableAttribute("ltl_biomass", "_FillValue", -99.f);
        nc.addVariable("latitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("latitude", "units", "degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        nc.addVariable("longitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("longitude", "units", "degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        /*
         * Add global attributes
         */
        nc.addGlobalAttribute("dimension_step", "step=0 before predation, step=1 after predation");
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getForcing().getNbPlanktonGroups(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getForcing().getPlanktonName(kltl));
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_ltl", str.toString());
        str = new StringBuilder();
        for (int ispec = 0; ispec < getNbSpecies(); ispec++) {
            str.append(ispec);
            str.append("=");
            str.append(getSpecies(ispec).getName());
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_species", str.toString());
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().getNbLines(), getGrid().getNbColumns());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().getNbLines(), getGrid().getNbColumns());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(getGrid().getNbLines() - cell.get_igrid() - 1, cell.get_jgrid(), cell.getLon());
                arrLat.set(getGrid().getNbLines() - cell.get_igrid() - 1, cell.get_jgrid(), cell.getLat());
            }
            nc.write("longitude", arrLon);
            nc.write("latitude", arrLat);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void saveSpatializedStep() {

        if (year < getOsmose().timeSeriesStart) {
            return;
        }

        float[][][] biomass = new float[this.getNbSpecies()][getGrid().getNbLines()][getGrid().getNbColumns()];
        float[][][] mean_size = new float[this.getNbSpecies()][getGrid().getNbLines()][getGrid().getNbColumns()];
        float[][][] tl = new float[this.getNbSpecies()][getGrid().getNbLines()][getGrid().getNbColumns()];
        float[][][][] ltlbiomass = new float[getForcing().getNbPlanktonGroups()][2][getGrid().getNbLines()][getGrid().getNbColumns()];
        float[][][] abundance = new float[this.getNbSpecies()][getGrid().getNbLines()][getGrid().getNbColumns()];
        float[][][] yield = new float[this.getNbSpecies()][getGrid().getNbLines()][getGrid().getNbColumns()];

        for (Cell cell : getGrid().getCells()) {
            int[] nbSchools = new int[getNbSpecies()];
            /*
             * Cell on land
             */
            if (cell.isLand()) {
                float fillValue = -99.f;
                for (int ispec = 0; ispec < getNbSpecies(); ispec++) {
                    biomass[ispec][cell.get_igrid()][cell.get_jgrid()] = fillValue;
                    abundance[ispec][cell.get_igrid()][cell.get_jgrid()] = fillValue;
                    mean_size[ispec][cell.get_igrid()][cell.get_jgrid()] = fillValue;
                    tl[ispec][cell.get_igrid()][cell.get_jgrid()] = fillValue;
                    yield[ispec][cell.get_igrid()][cell.get_jgrid()] = fillValue;
                }
                for (int iltl = 0; iltl < getForcing().getNbPlanktonGroups(); iltl++) {
                    ltlbiomass[iltl][0][cell.get_igrid()][cell.get_jgrid()] = fillValue;
                    ltlbiomass[iltl][1][cell.get_igrid()][cell.get_jgrid()] = fillValue;
                }
                continue;
            }
            /*
             * Cell in water
             */
            for (School school : cell) {
                if (school.getCohort().getAgeNbDt() > school.getCohort().getSpecies().indexAgeClass0 && !school.getCohort().isOut(indexTime)) {
                    nbSchools[school.getCohort().getSpecies().getIndex()] += 1;
                    biomass[school.getCohort().getSpecies().getIndex()][cell.get_igrid()][cell.get_jgrid()] += school.getBiomass();
                    abundance[school.getCohort().getSpecies().getIndex()][cell.get_igrid()][cell.get_jgrid()] += school.getAbundance();
                    mean_size[school.getCohort().getSpecies().getIndex()][cell.get_igrid()][cell.get_jgrid()] += school.getLength();
                    tl[school.getCohort().getSpecies().getIndex()][cell.get_igrid()][cell.get_jgrid()] += school.trophicLevel[indexTime];
                    //yield[school.getCohort().getSpecies().getIndex()][cell.get_igrid()][cell.get_jgrid()] += (school.catches * school.getWeight() / 1000000.d);
                }
            }
            for (int ispec = 0; ispec < getNbSpecies(); ispec++) {
                if (nbSchools[ispec] > 0) {
                    mean_size[ispec][cell.get_igrid()][cell.get_jgrid()] /= abundance[ispec][cell.get_igrid()][cell.get_jgrid()];
                    tl[ispec][cell.get_igrid()][cell.get_jgrid()] /= biomass[ispec][cell.get_igrid()][cell.get_jgrid()];
                }
            }
            for (int iltl = 0; iltl < getForcing().getNbPlanktonGroups(); iltl++) {
                ltlbiomass[iltl][0][cell.get_igrid()][cell.get_jgrid()] = getForcing().getPlankton(iltl).biomass[cell.get_igrid()][cell.get_jgrid()];
                ltlbiomass[iltl][1][cell.get_igrid()][cell.get_jgrid()] = getForcing().getPlankton(iltl).iniBiomass[cell.get_igrid()][cell.get_jgrid()];
            }
        }

        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, getNbSpecies(), getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrAbundance = new ArrayFloat.D4(1, getNbSpecies(), getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrYield = new ArrayFloat.D4(1, getNbSpecies(), getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrSize = new ArrayFloat.D4(1, getNbSpecies(), getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D4 arrTL = new ArrayFloat.D4(1, getNbSpecies(), getGrid().getNbLines(), getGrid().getNbColumns());
        ArrayFloat.D5 arrLTL = new ArrayFloat.D5(1, getForcing().getNbPlanktonGroups(), 2, getGrid().getNbLines(), getGrid().getNbColumns());
        int nl = getGrid().getNbLines() - 1;
        for (int kspec = 0; kspec < getNbSpecies(); kspec++) {
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    arrBiomass.set(0, kspec, nl - i, j, biomass[kspec][i][j]);
                    arrAbundance.set(0, kspec, nl - i, j, abundance[kspec][i][j]);
                    arrSize.set(0, kspec, nl - i, j, mean_size[kspec][i][j]);
                    arrTL.set(0, kspec, nl - i, j, tl[kspec][i][j]);
                    arrYield.set(0, kspec, nl - i, j, yield[kspec][i][j]);
                }
            }
        }
        for (int kltl = 0; kltl < getForcing().getNbPlanktonGroups(); kltl++) {
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    arrLTL.set(0, kltl, 0, nl - i, j, ltlbiomass[kltl][0][i][j]);
                    arrLTL.set(0, kltl, 1, nl - i, j, ltlbiomass[kltl][1][i][j]);
                }
            }
        }

        float timeSaving = year + (indexTime + 1f) / (float) nbTimeStepsPerYear;
        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, timeSaving);

        NetcdfFileWriteable nc = getOsmose().getNCOut();
        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + indexTime + " - " + timeSaving);
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write("biomass", new int[]{index, 0, 0, 0}, arrBiomass);
            nc.write("abundance", new int[]{index, 0, 0, 0}, arrAbundance);
            nc.write("yield", new int[]{index, 0, 0, 0}, arrYield);
            nc.write("mean_size", new int[]{index, 0, 0, 0}, arrSize);
            nc.write("trophic_level", new int[]{index, 0, 0, 0}, arrTL);
            nc.write("ltl_biomass", new int[]{index, 0, 0, 0, 0}, arrLTL);
        } catch (IOException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public int getNbSpecies() {
        return species.length;
    }

    /**
     * Get a species
     *
     * @param index, the index of the species
     * @return species[index]
     */
    public Species getSpecies(int index) {
        return species[index];
    }

    public LTLForcing getForcing() {
        return forcing;
    }

    public int getNbTimeStepsPerYear() {
        return nbTimeStepsPerYear;
    }

    public int getYear() {
        return year;
    }

    public int getIndexTime() {
        return indexTime;
    }

    public int getRecordFrequency() {
        return recordFrequency;
    }

    public static void shuffleArray(int[] a) {
        int n = a.length;
        Random random = new Random();
        random.nextInt();
        for (int i = 0; i < n; i++) {
            int change = i + random.nextInt(n - i);
            swap(a, i, change);
        }
    }

    private static void swap(int[] a, int i, int change) {
        int helper = a[i];
        a[i] = a[change];
        a[change] = helper;
    }

    public enum AlgoMortality {

        CASE1("Iteration algo proposed by Ricardo"),
        CASE2("Simultaneous processess but predation integrates competition between schools"),
        CASE3("All processes compete with each other");
        String description;

        AlgoMortality(String description) {
            this.description = description;
        }
    }
}
