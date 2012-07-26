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
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

public class Simulation {

    /*
     * ********
     * * Logs * ******** 2011/04/19 phv Deleted, renamed and encapsulated
     * variables. 2011/04/18 phv Deleted variables tabSchoolsRandom,
     * specInSizeClass10 that only had local use. Replaced randomOrder()
     * function by shuffleSchools() function. Simplified the rankSchoolsSizes()
     * method. 2011/04/08 phv Deleted the constructor. Parameters are now loaded
     * in the init() method. 2011/04/07 phv Deleted variable Osmose. Must be
     * called using Osmose.getInstance()
     */
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
     * Output of size spectrum per species
     */
    float[][] spectrumSpeciesAbd;
    /*
     * Output of TL distribution per species
     */
    float[][][] distribTL;
    /*
     * Characteristics of caught schools by species
     */
    float[][] tabSizeCatch, tabNbCatch;
    float[] tabTLCatch;
    // for saving
    long[] savingNbYield;
    float[] savingYield, meanSizeCatchTemp;
    float[][][] spectrumTemp;
    float[][] meanTLperAgeTemp;
    int[][] countTemp;
    double[][] biomPerStage;
    float tempMaxProbaPresence;
    float[][] accessibilityMatrix;
    int[] nbAccessibilityStages;
    float[][][][] dietsMatrix, predatorsPressureMatrix;
    long[][] nbStomachs;
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
    final public static boolean SCHOOL_BASED = true;

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

        // Initialize all the tables required for saving output
        if (getOsmose().spatializedOutputs[numSerie]) {
            initSpatializedSaving();
        }
        initSaving();

        //Initialisation indicators
        tabSizeCatch = new float[species.length][];
        tabNbCatch = new float[species.length][];
        if (sizeSpectrumPerSpeOutput) {
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        ((School) species[i].getCohort(j).getSchool(k)).rankSize(getOsmose().tabSizes, getOsmose().spectrumMaxSize);
                    }
                }
            }
        }
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

    private void applyPredationMortality(List<School> randomSchools) {

        Iterator<School> randomIterator = randomSchools.iterator();
        // loop over the schools
        while (randomIterator.hasNext()) {
            School school = randomIterator.next();
            // age class 0 do not predate
            if (!school.willDisappear() && school.getCohort().getAgeNbDt() != 0) {
                // call the School.predation() function
                school.predation();
            }
        }
    }

    private void saveBiomassBeforePredation() {
        /*
         * save fish biomass before predation process for diets data (last
         * column of predatorPressure output file in Diets/)
         */
        if (getOsmose().dietsOutputMatrix[getOsmose().numSerie] && (year >= getOsmose().timeSeriesStart)) {
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                        biomPerStage[i][((School) species[i].getCohort(j).getSchool(k)).dietOutputStage] += ((School) species[i].getCohort(j).getSchool(k)).getBiomass();
                    }
                }
            }
        }
        if (coupling != null && (year >= coupling.getStartYearLTLModel() - 1)) // save grid of plankton biomass one year before coupling so forcing mode is also saved
        {
            coupling.savePlanktonBiomass();
        } else if (getOsmose().planktonBiomassOutputMatrix[numSerie]) {
            forcing.savePlanktonBiomass();
        }
    }

    private void applyStarvationMortality(List<School> schools) {
        for (School school : schools) {
            if (!school.willDisappear()) {
                school.surviveP();
            }
        }
    }

    /**
     * for all species, D is due to other predators (seals, seabirds) for
     * migrating species, we add mortality because absents during a time step so
     * they don't undergo mortalities due to predation and starvation Additional
     * mortalities for ages 0: no-fecundation of eggs, starvation more
     * pronounced than for sup ages (rel to CC), predation by other species are
     * not explicit.
     */
    private void applyNaturalMortality() {

        boolean DEBUG = false;
        double[] natDead = null, nSchools = null, abd = null;
        if (DEBUG) {
            natDead = new double[species.length];
            nSchools = new double[species.length];
            abd = new double[species.length];
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    abd[i] += species[i].getCohort(j).getAbundance();
                }
            }
        }

        if (!SCHOOL_BASED) {
            for (int i = 0; i < species.length; i++) {
                if (species[i].getCohort(0).getAbundance() != 0) {
                    species[i].getCohort(0).surviveD(species[i].larvalSurvival + (species[i].getCohort(0).getOutMortality(indexTime) / (float) nbTimeStepsPerYear));     //additional larval mortality
                    if (DEBUG) {
                        natDead[i] += species[i].getCohort(0).nbDeadDd;
                        nSchools[i] += species[i].getCohort(0).size();
                    }
                }
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    if (species[i].getCohort(j).getAbundance() != 0) {
                        species[i].getCohort(j).surviveD((species[i].D + species[i].getCohort(j).getOutMortality(indexTime)) / (float) nbTimeStepsPerYear);
                    }
                    if (DEBUG) {
                        nSchools[i] += species[i].getCohort(j).size();
                        natDead[i] += species[i].getCohort(j).nbDeadDd;
                    }
                }
            }
        } else {
            // apply natural mortality at school level
            for (School school : getSchools()) {
                double nDead = computeNaturalMortality(school, 1);
                school.setAbundance(school.getAbundance() - nDead);
                if (school.getAbundance() < 1.d) {
                    nDead = school.getAbundance();
                    school.setAbundance(0);
                    school.tagForRemoval();
                }
                school.getCohort().nbDeadDd += nDead;
            }

            // Removing dead schools
            // Update biomass of schools & cohort
            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    Cohort cohort = species[i].getCohort(j);
                    cohort.removeDeadSchools();
                    cohort.setAbundance(0.d);
                    cohort.setBiomass(0.d);
                    for (School school : cohort) {
                        cohort.incrementAbundance(school.getAbundance());
                        cohort.incrementBiomass(school.getBiomass());
                        if (DEBUG && cohort.getAgeNbDt() >= 0) {
                            nSchools[i] += 1;
                        }
                    }
                    if (DEBUG && cohort.getAgeNbDt() >= 0) {
                        natDead[i] += species[i].getCohort(j).nbDeadDd;
                    }
                }
            }
        }
        if (DEBUG) {
            double[] D = new double[species.length];
            for (int i = 0; i < species.length; i++) {
                D[i] = Math.log(abd[i] / (abd[i] - natDead[i])) * nbTimeStepsPerYear;
            }
            float time = getYear() + (getIndexTime() + 1f) / (float) nbTimeStepsPerYear;
            Indicators.writeVariable(time, natDead, "natural.csv");
            Indicators.writeVariable(time, nSchools, "schools.csv");
            Indicators.writeVariable(time, abd, "abundance.csv");
            Indicators.writeVariable(time, D, "natrate.csv");
        }

    }

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

    private void computeBiomassToPredate(School predator, int subdt) {
        predator.biomassToPredate = getBiomass(predator) * predator.getCohort().getSpecies().predationRate / (double) (nbTimeStepsPerYear * subdt);
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
        computeBiomassToPredate(predator, subdt);
        double biomassToPredate = predator.biomassToPredate;

        // Distribute the predation over the preys
        if (biomAccessibleTot != 0) {
            // There is less prey available than the predator can
            // potentially prey upon. Predator will feed upon the total
            // accessible biomass
            if (biomAccessibleTot <= predator.biomassToPredate) {
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

    private void computePredSuccessRate(School school, double preyedBiomass) {

        // Compute the predation success rate
        school.predSuccessRate = Math.min((float) (preyedBiomass / school.biomassToPredate), 1.f);
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

    /**
     * Get the accessible biomass that predator can feed on plankton
     */
    private double getAccessibleBiomass(School predator, int indexPlankton) {

        Species spec = predator.getCohort().getSpecies();
        Cell cell = predator.getCell();
        float preySizeMax = predator.getLength() / spec.predPreySizesMax[predator.getFeedingStage()];
        float preySizeMin = predator.getLength() / spec.predPreySizesMin[predator.getFeedingStage()];
        float tempAccess = getOsmose().accessibilityMatrix[getNbSpecies() + indexPlankton][0][spec.getIndex()][predator.getAccessibilityStage()];
        float percent = 0;
        if ((preySizeMin < forcing.getPlankton(indexPlankton).getSizeMax()) && (preySizeMax > forcing.getPlankton(indexPlankton).getSizeMin())) {
            percent = forcing.getPlankton(indexPlankton).calculPercent(preySizeMin, preySizeMax);
        }
        return percent * tempAccess * forcing.getPlankton(indexPlankton).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
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
        for (int i = 0; i < species.length; i++) {
            if (species[i].getAbundance() != 0) {
                species[i].growth();
            }
        }
    }

    private void applyFishingMortality() {

        boolean DEBUG = false;
        double[] fishDead = null, nSchools = null, abd = null;
        if (DEBUG) {
            fishDead = new double[species.length];
            nSchools = new double[species.length];
            abd = new double[species.length];
            for (int i = 0; i < species.length; i++) {
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    abd[i] += species[i].getCohort(j).getAbundance();
                }
            }
        }

        if (false) {
            for (int i = 0; i < species.length; i++) {
                if ((species[i].getAbundance() != 0) && (species[i].seasonFishing[indexTime] != 0) && (species[i].F != 0)) {
                    species[i].fishingA();
                }
                for (int j = 1; j < species[i].getNumberCohorts(); j++) {
                    fishDead[i] += species[i].getCohort(j).nbDeadFf;
                    nSchools[i] += species[i].getCohort(j).size();
                }
            }
            updateBiomassAndAbundance();
        } else {

            // Apply fishing mortality at school level
            for (School school : getSchools()) {
                if (school.getAbundance() != 0.d) {
                    double nDead = computeFishingMortality(school, 1);
                    if (nDead != 0.d) {
                        school.setAbundance(school.getAbundance() - nDead);
                        if (school.getAbundance() < 1.d) {
                            nDead = school.getAbundance();
                            school.setAbundance(0);
                            school.getCohort().setNbSchoolsCatchable(school.getCohort().getNbSchoolsCatchable() - 1);
                            school.tagForRemoval();
                        }
                        school.getCohort().nbDeadFf += nDead;
                    }
                    school.nDeadFishing = nDead;
                }
            }

            // Remove dead schools
            // Update biomass of schools & cohort
            // Update indicators
            for (int i = 0; i < species.length; i++) {
                int indexRecruitAge = Math.round(species[i].recruitAge * nbTimeStepsPerYear);
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    Cohort cohort = species[i].getCohort(j);
                    cohort.removeDeadSchools();
                    cohort.setAbundance(0.d);
                    cohort.setBiomass(0.d);
                    cohort.setAbundanceCatchable(0.d);
                    int iSchool = 0;
                    for (School school : cohort) {
                        cohort.incrementAbundance(school.getAbundance());
                        cohort.incrementBiomass(school.getBiomass());
                        if (school.isCatchable() && j >= indexRecruitAge) {
                            cohort.incrementAbundanceCatchable(school.getAbundance());
                            // update indicators
                            tabNbCatch[i][iSchool + species[i].cumulCatch[cohort.getAgeNbDt() - 1]] += school.nDeadFishing;
                            if ((getYear()) >= getOsmose().timeSeriesStart) {
                                savingYield[i] += school.adb2biom(school.nDeadFishing);
                                savingNbYield[i] += school.nDeadFishing;
                                if (TLoutput) {
                                    tabTLCatch[i] += school.getTrophicLevel()[cohort.getAgeNbDt()] * school.adb2biom(school.nDeadFishing);
                                }
                            }
                            iSchool++;
                        }
                        if (DEBUG) {
                            nSchools[i] += 1;
                        }
                    }
                    if (DEBUG) {
                        fishDead[i] += species[i].getCohort(j).nbDeadFf;
                    }
                }
            }
        }
        if (DEBUG) {
            double[] F = new double[species.length];
            for (int i = 0; i < species.length; i++) {
                F[i] = Math.log(abd[i] / (abd[i] - fishDead[i])) * nbTimeStepsPerYear;
            }
            float time = getYear() + (getIndexTime() + 1f) / (float) nbTimeStepsPerYear;
            Indicators.writeVariable(time, fishDead, "fishing.csv");
            Indicators.writeVariable(time, nSchools, "schools.csv");
            Indicators.writeVariable(time, abd, "abundance.csv");
            Indicators.writeVariable(time, F, "fishrate.csv");
        }
    }

    private void updateIndicators() {
        for (int i = 0; i < species.length; i++) {
            species[i].update();
            if (species[i].getAbundance() == 0) {
                /*
                 * 2011/04/19 phv There is no reason for doing such a thing.
                 * First, even though the current abundance of a species is
                 * zero, the Species object is not deleted from the species[]
                 * array. Secondly, in School.java, the array
                 * accessibilityMatrix relies on the total number of species and
                 * doest not seem to handle the fact that nbSpecies might
                 * change. What I did : deleted the nbSpecies variable. Replaced
                 * by getNbSpecies() function that returns species.length. I
                 * keep here a local nbSpecies variable as a reminder.
                 */
                //nbSpecies--; Do Nothing
            }
        }

        //update spectra
        if (sizeSpectrumPerSpeOutput) {
            for (int i = 0; i < getOsmose().nbSizeClass; i++) {
                //spectrumAbd[i]=0;
                //spectrumBiom[i]=0;
                for (int j = 0; j < species.length; j++) {
                    spectrumSpeciesAbd[j][i] = 0;
                }
            }
        }

        for (int i = 0; i < species.length; i++) {
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                for (int k = 0; k < species[i].getCohort(j).size(); k++) {
                    if (sizeSpectrumPerSpeOutput) {
                        ((School) species[i].getCohort(j).getSchool(k)).rankSize(getOsmose().tabSizes, getOsmose().spectrumMaxSize);
                    }
                    if ((year >= getOsmose().timeSeriesStart) && ((TLoutput) || (TLDistriboutput))) {
                        ((School) species[i].getCohort(j).getSchool(k)).rankTL(getOsmose().tabTL);
                    }
                }
            }
        }
        //Stage Morgane - 07-2004  output of indicators
        for (int i = 0; i < species.length; i++) {
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

            // Spatial distribution (distributeSpeciesIni() for year0 & indexTime0)
            if (!((indexTime == 0) && (year == 0))) {
                distributeSpecies();
            }
            List<School> schools = getSchools();

            // Natural mortality (due to other predators)
            applyNaturalMortality();

            // Update LTL Data
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.runLTLModel();
            }
            forcing.updatePlankton(indexTime);

            // Predation
            rankSchoolsSizes();
            saveBiomassBeforePredation();
            Collections.shuffle(schools);
            applyPredationMortality(schools);
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.calculPlanktonMortality();
            }

            // Starvation
            applyStarvationMortality(schools);

            // Update of disappeared schools
            assessNbSchools();
            assessPresentSchools();
            assessAbdCohSpec();

            // Growth
            growth();

            // Fishing
            assessCatchableSchools();
            clearCatchesIndicators();
            applyFishingMortality();

            // Save steps
            updateIndicators();
            if (getOsmose().spatializedOutputs[numSerie]) {
                saveSpatializedStep();
            }
            Indicators.updateAndWriteIndicators();
            saveStep();

            // Reproduction
            reproduction();

            // Increment time step
            indexTime++;
        }
        indexTime = 0;  //end of the year
        year++; // go to following year
    }

    public void phvStep() {

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

            // Spatial distribution (distributeSpeciesIni() for year0 & indexTime0)
            if (!((indexTime == 0) && (year == 0))) {
                distributeSpecies();
            }

            // Natural mortality (due to other predators)
            for (School school : getSchools()) {
                // reset indicators
                school.nDeadPredation = 0;
                school.nDeadStarvation = 0;
                school.nDeadNatural = 0;
                school.nDeadFishing = 0;
                school.preyedBiomass = 0;
                double nDead = computeNaturalMortality(school, 1);
                school.setAbundance(school.getAbundance() - nDead);
                if (school.getAbundance() < 1.d) {
                    nDead = school.getAbundance();
                    school.setAbundance(0);
                    school.tagForRemoval();
                }
                school.getCohort().nbDeadDd += nDead;
            }
            updateBiomassAndAbundance();

            // Update LTL Data
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.runLTLModel();
            }
            forcing.updatePlankton(indexTime);

            // Predation
            rankSchoolsSizes();
            saveBiomassBeforePredation();
            for (Cell cell : getGrid().getCells()) {
                Collections.shuffle(cell);
                int ns = cell.size();
                if (!(cell.isLand() || cell.isEmpty())) {
                    double[] nDeadPredation = new double[ns];
                    // Compute predation
                    for (School predator : cell) {
                        double[] preyUpon = computePredation(predator, 1);
                        for (int ipr = 0; ipr < ns; ipr++) {
                            if (ipr < ns) {
                                School prey = cell.get(ipr);
                                nDeadPredation[ipr] += prey.biom2abd(preyUpon[ipr]);
                                prey.nDeadPredation += nDeadPredation[ipr];
                            }
                        }
                        predator.preyedBiomass = sum(preyUpon);
                    }
                    // Apply predation mortality
                    for (int is = 0; is < ns; is++) {
                        School school = cell.get(is);
                        school.nDeadPredation = 0;
                        computePredSuccessRate(school, school.preyedBiomass);
                        school.setAbundance(school.getAbundance() - nDeadPredation[is]);
                        if (school.getAbundance() < 1.d) {
                            school.setAbundance(0.d);
                            school.tagForRemoval();
                            school.getCohort().setNbSchoolsCatchable(school.getCohort().getNbSchoolsCatchable() - 1);
                        }
                    }
                }
            }
            updateBiomassAndAbundance();

            // Compute Plankton Mortality
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.calculPlanktonMortality();
            }

            // Starvation
            for (School school : getSchools()) {
                double nDead = computeStarvationMortality(school, 1);
                school.setAbundance(school.getAbundance() - nDead);
                if (school.getAbundance() < 1.d) {
                    school.setAbundance(0);
                    school.tagForRemoval();
                }
            }
            updateBiomassAndAbundance();

            // Growth
            growth();

            // Fishing
            assessCatchableSchools();
            clearCatchesIndicators();
            for (School school : getSchools()) {
                if (school.getAbundance() != 0.d) {
                    double nDead = computeFishingMortality(school, 1);
                    if (nDead != 0.d) {
                        school.setAbundance(school.getAbundance() - nDead);
                        if (school.getAbundance() < 1.d) {
                            nDead = school.getAbundance();
                            school.setAbundance(0);
                            school.getCohort().setNbSchoolsCatchable(school.getCohort().getNbSchoolsCatchable() - 1);
                            school.tagForRemoval();
                        }
                        school.getCohort().nbDeadFf += nDead;
                        school.nDeadFishing = nDead;
                    }
                }
            }
            updatePopulation();

            // Save steps
            updateIndicators();
            if (getOsmose().spatializedOutputs[numSerie]) {
                saveSpatializedStep();
            }
            Indicators.updateAndWriteIndicators();
            saveStep();

            // Reproduction
            reproduction();

            // Increment time step
            indexTime++;
        }
        indexTime = 0;  //end of the year
        year++; // go to following year
    }

    public void newStep() {

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
            saveBiomassBeforePredation();
            clearCatchesIndicators();

            // Compute mortality
            // (predation + fishing + natural mortality + starvation)
            int subdt = 1;
            for (int t = 0; t < subdt; t++) {
                computeMortality(subdt, AlgoMortality.CASE3);
                updatePopulation();
            }

            // Update of disappeared schools and plancton mortality
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.calculPlanktonMortality();
            }

            // Growth
            growth();

            // Save steps
            updateIndicators();
            if (getOsmose().spatializedOutputs[numSerie]) {
                saveSpatializedStep();
            }
            saveStep();
            Indicators.updateAndWriteIndicators();

            // Reproduction
            reproduction();

            // Increment time step
            indexTime++;
        }
        indexTime = 0;  //end of the year
        year++; // go to following year

    }

    private double clean(double value, double cleanValue) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return cleanValue;
        } else {
            return value;
        }
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
                        predator.preyedBiomass = sum(preyUpon);
                        predator.hasPredated = true;
                        computePredSuccessRate(predator, predator.preyedBiomass);
                        break;
                    case 1:
                        // Starvation mortality
                        predator = cell.get(seqStarv[i]);
                        if (predator.hasPredated) {
                            computeBiomassToPredate(predator, subdt);
                            // here we recalculate the predation success rate
                            // since it might have changed if other mortality
                            // applied after predation
                            computePredSuccessRate(predator, predator.preyedBiomass);
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
            predator.preyedBiomass = sum(preyUpon);
        }

        for (int is = 0; is < ns; is++) {
            School school = cell.get(is);
            school.nDeadPredation = 0.d;
            // 2. Starvation
            computePredSuccessRate(school, school.preyedBiomass);
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
                nDeadMatrix[ipr][imort] = (mortalityRateMatrix[ipr][imort] / totalMortalityRate[ipr]) * (1 - Math.exp(-totalMortalityRate[ipr])) * abundance;
                nDeadMatrix[ipr][imort] = clean(nDeadMatrix[ipr][imort], 0.d);
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
                    double planktonAbundance = forcing.getPlankton(ipr - ns).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
                    predationMortalityRate = Math.log(planktonAbundance / (planktonAbundance - predationMatrix[ipd][ipr]));
                }
                mortalityRateMatrix[ipr][ipd] = clean(predationMortalityRate, 0.d);
            }
        }
        for (int is = 0; is < ns; is++) {
            School school = cell.get(is);
            // 2. Starvation
            // computes preyed biomass by school ipr
            school.preyedBiomass = 0;
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                school.preyedBiomass += predationMatrix[is][ipr];
            }
            computePredSuccessRate(school, school.preyedBiomass);
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
                    nDeadMatrix[ipr][imort] = (mortalityRateMatrix[ipr][imort] / totalMortalityRate[ipr]) * (1 - Math.exp(-totalMortalityRate[ipr])) * abundance;
                    nDeadMatrix[ipr][imort] = clean(nDeadMatrix[ipr][imort], 0.d);
                }
            }

            // Compute correction factor

            for (int ipd = 0; ipd < ns; ipd++) {
                School predator = cell.getSchool(ipd);
                predator.preyedBiomass = 0;
                for (int ipr = 0; ipr < (ns + npl); ipr++) {
                    if (ipr < ns) {
                        predator.preyedBiomass += cell.getSchool(ipr).adb2biom(nDeadMatrix[ipr][ipd]);
                    } else {
                        predator.preyedBiomass += nDeadMatrix[ipr][ipd];
                    }
                    //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                }
                predator.preyedBiomass = clean(predator.preyedBiomass, 0.d);
                correctionFactor[ipd] = clean(Math.min(predator.biomassToPredate / predator.preyedBiomass, 1.d), 1.d);
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
                computePredSuccessRate(school, school.preyedBiomass);
                mortalityRateMatrix[ipr][ns] = clean(getStarvationMortalityRate(school, subdt), 0);
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

        boolean DEBUG = false;
        //
        double[][] mortality = null;
        if (DEBUG) {
            mortality = new double[getNbSpecies()][5];
            for (int i = 0; i < species.length; i++) {
                int indexRecruitAge = Math.round(species[i].recruitAge * nbTimeStepsPerYear);
                for (int j = indexRecruitAge; j < species[i].getNumberCohorts(); j++) {
                    mortality[i][4] += species[i].getCohort(j).getAbundance();
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
                    school.preyedBiomass = 0;
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
                    for (int ipd = 0; ipd < ns; ipd++) {
                        school.nDeadPredation += nDeadMatrix[is][ipd];
                    }
                    // update TL
                    if (school.preyedBiomass > 0.d) {
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
                                school.trophicLevel[school.getCohort().getAgeNbDt()] += TLprey * biomPrey / school.preyedBiomass;
                            } else {
                                school.trophicLevel[school.getCohort().getAgeNbDt()] += forcing.getPlankton(ipr - ns).trophicLevel * nDeadMatrix[ipr][is] / school.preyedBiomass;
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

                    if (DEBUG) {
                        int i = school.getCohort().getSpecies().getIndex();
                        int indexRecruitAge = Math.round(species[i].recruitAge * nbTimeStepsPerYear);
                        if (school.getCohort().getAgeNbDt() >= indexRecruitAge) {
                            mortality[i][0] += (school.nDeadPredation);
                            mortality[i][1] += (school.nDeadStarvation);
                            mortality[i][2] += (school.nDeadNatural);
                            mortality[i][3] += (school.nDeadFishing);
                        }
                    }

                    //System.out.println(Math.round(school.getAbundance()) + " tot: " + nDeadTotal + " pred:" + school.nDeadPredation + " starv: " + school.nDeadStarvation + " nat: " + school.nDeadNatural + " fish: " + school.nDeadFishing);
//                    int pPred = (int) (100 * school.nDeadPredation / school.getAbundance());
//                    int pStar = (int) (100 * school.nDeadStarvation / school.getAbundance());
//                    int pNatu = (int) (100 * school.nDeadNatural / school.getAbundance());
//                    int pFish = (int) (100 * school.nDeadFishing / school.getAbundance());
//                    System.out.println(pPred + " " + pStar + " " + pNatu + " " + pFish);
//                    if (nDeadTotal > (school.getAbundance() - 1)) {
//                        nDeadTotal = school.getAbundance();
//                    }
                    school.setAbundance(school.getAbundance() - nDeadTotal);
                    if (school.getAbundance() < 1.d) {
                        school.setAbundance(0.d);
                        school.tagForRemoval();
                        school.getCohort().setNbSchoolsCatchable(school.getCohort().getNbSchoolsCatchable() - 1);
                    }
                }
            }
        }

        if (DEBUG) {
//            for (int i = 0; i < species.length; i++) {
//                for (int j = 0; j < 4; j++) {
//                    mortality[i][j] /= species[i].getBiomass();
//                }
//            }
            String filename = "nDead_Simu" + getOsmose().numSimu + ".csv";
            String[] headers = new String[]{"Predation", "Starvation", "Natural", "Fishing", "Abundance"};
            Indicators.writeVariable(year + (indexTime + 1f) / (float) nbTimeStepsPerYear, mortality, filename, headers);
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
                    school.updateDietIndicators();
                    if (school.isCatchable() && j >= indexRecruitAge) {
                        cohort.incrementAbundanceCatchable(school.getAbundance());
                        // update fihsing indicators
                        cohort.nbDeadFf += school.nDeadFishing;
                        tabNbCatch[i][iSchool + species[i].cumulCatch[cohort.getAgeNbDt() - 1]] += school.nDeadFishing;
                        if ((getYear()) >= getOsmose().timeSeriesStart) {
                            savingYield[i] += school.adb2biom(school.nDeadFishing);
                            savingNbYield[i] += school.nDeadFishing;
                            if (TLoutput) {
                                tabTLCatch[i] += school.getTrophicLevel()[cohort.getAgeNbDt()] * school.adb2biom(school.nDeadFishing);
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
                        school.breakaway();
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
                            school.breakaway();
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

    public void initSaving() {

        savingYield = new float[species.length];
        savingNbYield = new long[species.length];
        tabTLCatch = new float[species.length];
        biomPerStage = new double[species.length + forcing.getNbPlanktonGroups()][];

        if (meanSizeOutput) {
            meanSizeCatchTemp = new float[species.length];
        }
        if (TLoutput) {
            for (int i = 0; i < species.length; i++) {
                tabTLCatch[i] = 0;
            }
        }


        for (int i = 0; i < species.length; i++) {
            savingYield[i] = 0;
            savingNbYield[i] = 0;
            biomPerStage[i] = new double[species[i].nbDietStages];
            for (int j = 0; j < species[i].nbDietStages; j++) {
                biomPerStage[i][j] = 0;
            }
            if (meanSizeOutput) {
                meanSizeCatchTemp[i] = 0f;
            }
        }
        for (int i = species.length; i < species.length + forcing.getNbPlanktonGroups(); i++) {
            biomPerStage[i] = new double[1];                  // only & stage per plankton group
            biomPerStage[i][0] = 0;
        }


        if (dietsOutput) {
            nbStomachs = new long[species.length][];
            dietsMatrix = new float[species.length][][][];
            predatorsPressureMatrix = new float[species.length][][][];
            for (int i = 0; i < species.length; i++) {
                nbStomachs[i] = new long[species[i].nbDietStages];
                dietsMatrix[i] = new float[species[i].nbDietStages][][];
                predatorsPressureMatrix[i] = new float[species[i].nbDietStages][][];
                for (int s = 0; s < species[i].nbDietStages; s++) {
                    nbStomachs[i][s] = 0;
                    dietsMatrix[i][s] = new float[species.length + forcing.getNbPlanktonGroups()][];
                    predatorsPressureMatrix[i][s] = new float[species.length + forcing.getNbPlanktonGroups()][];
                    for (int j = 0; j < species.length; j++) {
                        dietsMatrix[i][s][j] = new float[species[j].nbDietStages];
                        predatorsPressureMatrix[i][s][j] = new float[species[j].nbDietStages];
                        for (int st = 0; st < species[j].nbDietStages; st++) {
                            dietsMatrix[i][s][j][st] = 0f;
                            predatorsPressureMatrix[i][s][j][st] = 0f;
                        }
                    }
                    for (int j = species.length; j < species.length + forcing.getNbPlanktonGroups(); j++) {
                        dietsMatrix[i][s][j] = new float[1];
                        dietsMatrix[i][s][j][0] = 0f;
                        predatorsPressureMatrix[i][s][j] = new float[1];
                        predatorsPressureMatrix[i][s][j][0] = 0f;
                    }
                }
            }
        }

        countTemp = new int[species.length][];
        for (int i = 0; i < species.length; i++) {
            countTemp[i] = new int[species[i].getNumberCohorts()];
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                countTemp[i][j] = 0;
            }
        }

        if (TLoutput) {
            meanTLperAgeTemp = new float[species.length][];
            for (int i = 0; i < species.length; i++) {
                meanTLperAgeTemp[i] = new float[species[i].getNumberCohorts()];
                for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                    meanTLperAgeTemp[i][j] = 0;
                }
            }
        }
        if (TLDistriboutput) {
            distribTL = new float[species.length][][];
            for (int i = 0; i < species.length; i++) {
                distribTL[i] = new float[2][];
                distribTL[i][0] = new float[getOsmose().nbTLClass];    // age 0
                distribTL[i][1] = new float[getOsmose().nbTLClass];    // without age 0
                for (int j = 0; j < getOsmose().nbTLClass; j++) {
                    distribTL[i][0][j] = 0;
                    distribTL[i][1][j] = 0;
                }
            }
        }
        //ORGANIZING SIZE CLASSES of the spectrum at INITIALIZATION
        //spectrumAbd = new float[getOsmose().nbSizeClass];
        //spectrumBiom = new float[getOsmose().nbSizeClass];

        if (sizeSpectrumPerSpeOutput) {
            spectrumSpeciesAbd = new float[species.length][];
            spectrumTemp = new float[2][][];
            spectrumTemp[0] = new float[species.length][];
            spectrumTemp[1] = new float[species.length][];
            for (int i = 0; i < species.length; i++) {
                spectrumSpeciesAbd[i] = new float[getOsmose().nbSizeClass];
                spectrumTemp[0][i] = new float[getOsmose().nbSizeClass];
                spectrumTemp[1][i] = new float[getOsmose().nbSizeClass];
            }
            //calculation of spectrum values
            for (int i = 0; i < getOsmose().nbSizeClass; i++) {
                //   spectrumAbd[i]=0;
                //   spectrumBiom[i]=0;
                for (int j = 0; j < species.length; j++) {
                    spectrumSpeciesAbd[j][i] = 0;
                    spectrumTemp[0][j][i] = 0;
                    spectrumTemp[1][j][i] = 0;
                }
            }
        }

        initYieldFile();
        initNbYieldFile();

        if (meanSizeOutput) {
            initMeanSizeCatchFile();
        }

        if (TLoutput) {
            initMeanTLCatchFile();
        }
        if (dietsOutput) {
            initDietFile();
            initPredatorPressureFile();
        }
        if (TLDistriboutput) {
            initTLDistFile();
        }

        if (sizeSpectrumPerSpeOutput) {
            initSizeSpecPerSpFile();
            initSizeSpecPerSpCatchFile();
        }
        if (sizeSpectrumOutput) {
            initSizeSpecFile();
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
                    tl[school.getCohort().getSpecies().getIndex()][cell.get_igrid()][cell.get_jgrid()] += school.getTrophicLevel()[indexTime];
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

    public void saveStep() {

        /*
         * Start saving for year >= timeSeriesStart
         */
        if (year < getOsmose().timeSeriesStart) {
            return;
        }

        int indexSaving;
        indexSaving = (int) indexTime / recordFrequency;
        double biomNo0;
        long abdNo0;

        if (year >= getOsmose().timeSeriesStart) {
            for (int i = 0; i < species.length; i++) {
                Species speci = species[i];

                if (sizeSpectrumPerSpeOutput) {
                    for (int j = 0; j < getOsmose().nbSizeClass; j++) {
                        spectrumTemp[0][i][j] += spectrumSpeciesAbd[i][j];
                    }
                }

                //biom per stage rempli avant la prédation
				/*
                 * for (int j=0; j< speci.getNumberCohorts(); j++) for (int k=0;
                 * k<speci.getCohort(j).nbSchools; k++)
                 * biomPerStage[i][((QSchool)speci.getCohort(j).getSchool(k)).stage]
                 * += ((QSchool)speci.getCohort(j).getSchool(k)).biomass;
                 */
                abdNo0 = 0;
                biomNo0 = 0;
                for (int k = speci.indexAgeClass0; k < speci.getNumberCohorts(); k++) {
                    biomNo0 += speci.getCohort(k).getBiomass();
                    abdNo0 += speci.getCohort(k).getAbundance();

                }

                if (meanSizeOutput) {
                    meanSizeCatchTemp[i] += speci.meanSizeSpeCatch * (float) savingNbYield[i];
                }
                if (TLoutput) {
                    for (int j = 0; j < speci.getNumberCohorts(); j++) {
                        if (speci.meanTLperAge[j] != 0) {
                            meanTLperAgeTemp[i][j] += speci.meanTLperAge[j];
                            countTemp[i][j] += 1;
                        }
                    }
                }
            }

            /*
             * Save every 'recordFrequency' steps
             */
            if (((indexTime + 1) % recordFrequency) == 0) {
                float timeSaving = (float) year + (indexTime + (recordFrequency / 2f) + 1f) / (float) nbTimeStepsPerYear;
                timeSaving = year + (indexTime + 1f) / (float) nbTimeStepsPerYear;

                saveYieldperTime(timeSaving, savingYield);
                saveNbYieldperTime(timeSaving, savingNbYield);
                if (meanSizeOutput) {
                    saveMeanSizeCatchperTime(timeSaving, meanSizeCatchTemp, savingNbYield);
                }
                if (TLoutput) {
                    saveMeanTLCatchperTime(timeSaving, tabTLCatch, savingYield);
                    saveMeanTLperAgeperTime(timeSaving, meanTLperAgeTemp, countTemp);
                    /*
                     * if(getOsmose().TLoutput) { for (int
                     * j=0;j<species[i].getNumberCohorts();j++)
                     * getOsmose().TLperAgeMatrix[getOsmose().numSimu][i][j][t-getOsmose().timeSeriesStart][indexSaving]
                     * = meanTLperAgeTemp[i][j]/countTemp[i][j]; }
                     * if(getOsmose().TLoutput) { for (int
                     * j=0;j<species[i].getNumberCohorts();j++) {
                     * meanTLperAgeTemp[i][j]=0; countTemp[i][j] = 0; } }
                     */
                }
                if (dietsOutput) {
                    saveDietperTime(timeSaving, dietsMatrix, nbStomachs);
                    savePredatorPressureperTime(timeSaving, predatorsPressureMatrix, biomPerStage);
                }
                if (TLDistriboutput) {
                    saveTLDistperTime(timeSaving, distribTL);
                }

                if (sizeSpectrumOutput) {
                    saveSizeSpecperTime(timeSaving, spectrumTemp[0]);
                }
                if (sizeSpectrumPerSpeOutput) {
                    saveSizeSpecPerSpperTime(timeSaving, spectrumTemp[0]);
                    saveSizeSpecPerSpperCatchTime(timeSaving, spectrumTemp[1]);
                }
                for (int i = species.length; i < species.length + forcing.getNbPlanktonGroups(); i++) {
                    if (calibration) {
                        getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomPerStage[i][0] / recordFrequency;
                    }
                    biomPerStage[i][0] = 0;
                }

                // clear all saving tables
                for (int i = 0; i < species.length; i++) {
                    savingYield[i] = 0;
                    savingNbYield[i] = 0;
                    tabTLCatch[i] = 0;

                    if (meanSizeOutput) {
                        meanSizeCatchTemp[i] = 0;
                    }

                    if (TLoutput) {
                        for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                            meanTLperAgeTemp[i][j] = 0;
                            countTemp[i][j] = 0;
                        }
                    }

                    if (dietsOutput) {
                        for (int s = 0; s < species[i].nbDietStages; s++) {
                            nbStomachs[i][s] = 0;
                            biomPerStage[i][s] = 0;
                            for (int j = 0; j < species.length; j++) {
                                for (int st = 0; st < species[j].nbDietStages; st++) {
                                    dietsMatrix[i][s][j][st] = 0f;
                                    predatorsPressureMatrix[i][s][j][st] = 0f;
                                }
                            }
                            for (int j = species.length; j < species.length + forcing.getNbPlanktonGroups(); j++) {
                                dietsMatrix[i][s][j][0] = 0f;
                                predatorsPressureMatrix[i][s][j][0] = 0f;
                            }
                        }
                    }
                    if (TLDistriboutput) {
                        for (int j = 0; j < getOsmose().nbTLClass; j++) {
                            distribTL[i][0][j] = 0;
                            distribTL[i][1][j] = 0;
                        }
                    }
                    if (sizeSpectrumPerSpeOutput) {
                        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
                            spectrumTemp[0][i][j] = 0;
                            spectrumTemp[1][i][j] = 0;
                        }
                    }
                } // end clearing loop over species


            }   // end loop dtcount=dtSaving
        }
    }

    public void initPredatorPressureFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_predatorPressureMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(dietTime, true);
        pr.print("Time");
        pr.print(';');
        pr.print("Prey");
        for (int i = 0; i < species.length; i++) {
            for (int s = 0; s < species[i].nbDietStages; s++) {
                pr.print(";");
                if (species[i].nbDietStages == 1) {
                    pr.print(species[i].getName());    // Name predators
                } else {
                    if (s == 0) {
                        pr.print(species[i].getName() + " < " + species[i].dietStagesTab[s]);    // Name predators
                    } else {
                        pr.print(species[i].getName() + " >" + species[i].dietStagesTab[s - 1]);    // Name predators
                    }
                }
            }
        }
        pr.print(";");
        pr.print("Biomass");
        pr.println();
        pr.close();
    }

    public void initDietFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_dietMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(dietTime, true);
        pr.print("Time");
        pr.print(';');
        pr.print("Prey");
        for (int i = 0; i < species.length; i++) {
            for (int s = 0; s < species[i].nbDietStages; s++) {
                pr.print(";");
                if (species[i].nbDietStages == 1) {
                    pr.print(species[i].getName());    // Name predators
                } else {
                    if (s == 0) {
                        pr.print(species[i].getName() + " < " + species[i].dietStagesTab[s]);    // Name predators
                    } else {
                        pr.print(species[i].getName() + " >" + species[i].dietStagesTab[s - 1]);    // Name predators
                    }
                }
            }
        }
        pr.print(";");
        pr.print("nbStomachs");
        pr.println();
        pr.close();
    }

    public void savePredatorPressureperTime(float time, float[][][][] diets, double[][] biom) {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_predatorPressureMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(dietTime, true);


        for (int j = 0; j < species.length; j++) {
            for (int st = 0; st < species[j].nbDietStages; st++) {
                pr.print(time);
                pr.print(';');
                if (species[j].nbDietStages == 1) {
                    pr.print(species[j].getName());    // Name predators
                } else {
                    if (st == 0) {
                        pr.print(species[j].getName() + " < " + species[j].dietStagesTab[st]);    // Name predators
                    } else {
                        pr.print(species[j].getName() + " >" + species[j].dietStagesTab[st - 1]);    // Name predators
                    }
                }
                pr.print(";");
                for (int i = 0; i < species.length; i++) {
                    for (int s = 0; s < species[i].nbDietStages; s++) {
                        pr.print(diets[i][s][j][st] / recordFrequency);
                        pr.print(";");
                    }
                }
                pr.print(biom[j][st] / recordFrequency);
                pr.println();
            }
        }
        for (int j = species.length; j < (species.length + forcing.getNbPlanktonGroups()); j++) {
            pr.print(time);
            pr.print(";");
            pr.print(forcing.getPlanktonName(j - species.length));
            pr.print(";");
            for (int i = 0; i < species.length; i++) {
                for (int s = 0; s < species[i].nbDietStages; s++) // 4 Stages
                {
                    pr.print(diets[i][s][j][0] / recordFrequency);
                    pr.print(";");
                }
            }
            pr.print(biom[j][0] / recordFrequency);
            pr.println();
        }
        pr.close();
    }

    public void saveDietperTime(float time, float[][][][] diets, long[][] nbStomachs) {
        File targetPath, targetFile;
        PrintWriter pr;
        String dietFile = getOsmose().outputFileNameTab[numSerie] + "_dietMatrix_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Diets");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, dietFile);
            dietTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(dietTime, true);


        for (int j = 0; j < species.length; j++) {
            for (int st = 0; st < species[j].nbDietStages; st++) {
                pr.print(time);
                pr.print(';');
                if (species[j].nbDietStages == 1) {
                    pr.print(species[j].getName());    // Name predators
                } else {
                    if (st == 0) {
                        pr.print(species[j].getName() + " < " + species[j].dietStagesTab[st]);    // Name predators
                    } else {
                        pr.print(species[j].getName() + " >" + species[j].dietStagesTab[st - 1]);    // Name predators
                    }
                }
                pr.print(";");
                for (int i = 0; i < species.length; i++) {
                    for (int s = 0; s < species[i].nbDietStages; s++) {
                        if (nbStomachs[i][s] != 0) {
                            pr.print(diets[i][s][j][st] / (float) nbStomachs[i][s]);
                        } else {
                            pr.print("NaN");
                        }
                        pr.print(";");
                    }
                }
                pr.print(nbStomachs[j][st]);
                pr.println();
            }
        }
        for (int j = species.length; j < (species.length + forcing.getNbPlanktonGroups()); j++) {
            pr.print(time);
            pr.print(";");
            pr.print(forcing.getPlanktonName(j - species.length));
            pr.print(";");
            for (int i = 0; i < species.length; i++) {
                for (int s = 0; s < species[i].nbDietStages; s++) {
                    if (nbStomachs[i][s] != 0) {
                        pr.print(diets[i][s][j][0] / (float) nbStomachs[i][s]);
                    } else {
                        pr.print("NaN");
                    }
                    pr.print(";");
                }
            }
            pr.println();
        }
        pr.close();
    }

    public void initYieldFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String yieldFile = getOsmose().outputFileNameTab[numSerie] + "_yield_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, yieldFile);
            yieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(yieldTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initNbYieldFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String nbYieldFile = getOsmose().outputFileNameTab[numSerie] + "_yieldNB_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, nbYieldFile);
            nbYieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(nbYieldTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initMeanSizeCatchFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanSizeFile = getOsmose().outputFileNameTab[numSerie] + "_meanSizeCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanSizeFile);
            meanSizeTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanSizeTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }
        pr.println();
        pr.close();
    }

    public void initMeanTLCatchFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTLCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);
        pr.print("Time");
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(species[i].getName());
        }

        pr.println();
        pr.close();
    }

    public void saveYieldperTime(float time, float[] Y) {
        File targetPath, targetFile;
        PrintWriter pr;
        String yieldFile = getOsmose().outputFileNameTab[numSerie] + "_yield_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, yieldFile);
            yieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(yieldTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(Y[i]);
        }
        pr.println();
        pr.close();
    }

    public void saveNbYieldperTime(float time, long[] nY) {
        File targetPath, targetFile;
        PrintWriter pr;
        String nbYieldFile = getOsmose().outputFileNameTab[numSerie] + "_yieldNB_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie]);
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, nbYieldFile);
            nbYieldTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }
        pr = new PrintWriter(nbYieldTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            pr.print(nY[i]);
        }
        pr.println();
        pr.close();
    }

    public void saveMeanSizeCatchperTime(float time, float[] mLY, long[] abd) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanSizeFile = getOsmose().outputFileNameTab[numSerie] + "_meanSizeCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanSizeFile);
            meanSizeTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanSizeTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            if (abd[i] != 0) {
                pr.print((mLY[i] / (float) abd[i]));
            } else {
                pr.print("NaN");
            }
        }
        pr.println();
        pr.close();
    }

    public void saveMeanTLCatchperTime(float time, float[] mTL, float[] biom) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTLCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);

        pr.print(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(";");
            if (biom[i] != 0) {
                pr.print((mTL[i] / (float) biom[i]));
            } else {
                pr.print("NaN");
            }
        }
        pr.println();
        pr.close();
    }

    public void saveMeanTLperAgeperTime(float time, float[][] mTL, int[][] nb) {
        File targetPath, targetFile;
        PrintWriter pr;
        String meanTLFile = getOsmose().outputFileNameTab[numSerie] + "_meanTLperAge_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, meanTLFile);
            meanTLTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(meanTLTime, true);

        pr.println(time);
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            for (int j = 0; j < species[i].getNumberCohorts(); j++) {
                pr.print(";");
                if (nb[i][j] != 0) {
                    pr.print((mTL[i][j] / (float) nb[i][j]));
                } else {
                    pr.print("NaN");
                }
            }
            pr.println();
        }
        pr.println();
        pr.close();
    }

    public void initTLDistFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String TLDistFile = getOsmose().outputFileNameTab[numSerie] + "_TLDistrib_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, TLDistFile);
            TLDistTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(TLDistTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("TL");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName() + " -0");
            pr.print(';');
            pr.print(species[i].getName() + " -1+");
            pr.print(';');
        }
        pr.println();
        pr.close();
    }

    public void saveTLDistperTime(float time, float[][][] TLdist) {
        File targetPath, targetFile;
        PrintWriter pr;
        String TLDistFile = getOsmose().outputFileNameTab[numSerie] + "_TLDistrib_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "Trophic");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, TLDistFile);
            TLDistTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(TLDistTime, true);

        for (int j = 0; j < getOsmose().nbTLClass; j++) {
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabTL[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(TLdist[i][0][j] / (float) recordFrequency);
                pr.print(';');
                pr.print(TLdist[i][1][j] / (float) recordFrequency);
                pr.print(';');
            }
            pr.println();
        }
        pr.close();
    }

    public void initSizeSpecPerSpFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpecies_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("size");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            pr.print(';');
        }
        pr.print("LN(size)");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            pr.print(';');
        }
        pr.println();
        pr.close();
    }

    public void saveSizeSpecPerSpperTime(float time, float[][] abdSize) {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpecies_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabSizes[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(abdSize[i][j] / (float) recordFrequency);
                pr.print(';');
            }
            pr.print((getOsmose().tabSizesLn[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(Math.log(abdSize[i][j] / (float) recordFrequency));
                pr.print(';');
            }
            pr.println();
        }
        pr.close();
    }

    public void initSizeSpecPerSpCatchFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpeciesCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("size");
        pr.print(';');
        for (int i = 0; i < species.length; i++) {
            pr.print(species[i].getName());
            pr.print(';');
        }
        pr.println();
        pr.close();
    }

    public void saveSizeSpecPerSpperCatchTime(float time, float[][] abdSize) {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrumPerSpeciesCatch_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabSizes[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                pr.print(abdSize[i][j] / (float) recordFrequency);
                pr.print(';');
            }
            pr.println();
        }
        pr.close();
    }

    public void initSizeSpecFile() {
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrum_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        pr.print("Time");
        pr.print(';');
        pr.print("size");
        pr.print(';');
        pr.print("Abundance");
        pr.print(';');
        pr.print("LN(size)");
        pr.print(';');
        pr.print("LN(Abd)");
        pr.print(';');
        pr.println();
        pr.close();
    }

    public void saveSizeSpecperTime(float time, float[][] abdSize) {
        float sum;
        File targetPath, targetFile;
        PrintWriter pr;
        String SSperSpFile = getOsmose().outputFileNameTab[numSerie] + "_SizeSpectrum_Simu" + getOsmose().numSimu + ".csv";
        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "SizeIndicators");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, SSperSpFile);
            SSperSpTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(SSperSpTime, true);

        for (int j = 0; j < getOsmose().nbSizeClass; j++) {
            sum = 0f;
            pr.print(time);
            pr.print(';');
            pr.print((getOsmose().tabSizes[j]));
            pr.print(';');
            for (int i = 0; i < species.length; i++) {
                sum += abdSize[i][j] / (float) recordFrequency;
            }
            pr.print(sum);
            pr.print(';');
            pr.print((getOsmose().tabSizesLn[j]));
            pr.print(';');
            pr.print(Math.log(sum));
            pr.println();
        }
        pr.close();
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

    class PredationOutcome {

        double predationLoss;
        double predationGain;
        double accessibleBiomass;

        PredationOutcome() {
            predationLoss = 0.d;
            predationGain = 0.d;
            accessibleBiomass = 0.d;
        }

        public double getLoss() {
            return predationLoss;
        }

        public double getGain() {
            return predationGain;
        }

        public void loss(double loss) {
            predationLoss += loss;
        }

        public void gain(double gain) {
            predationGain += gain;
        }

        public void incrementAccessibleBiomass(double biomass) {
            accessibleBiomass += biomass;
        }
    }

    public enum AlgoMortality {

        CASE1("Itertion algo proposed by Ricardo"),
        CASE2("Simultaneous processess but predation integrates competition between schools"),
        CASE3("All processes compete with each other");
        String description;

        AlgoMortality(String description) {
            this.description = description;
        }
    }
}
