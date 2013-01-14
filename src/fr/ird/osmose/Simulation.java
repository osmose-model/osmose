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
import fr.ird.osmose.ConnectivityMatrix.ConnectivityLine;
import fr.ird.osmose.filter.PresentSchoolFilter;
import fr.ird.osmose.filter.SpeciesFilter;
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
    public enum Version {
        /*
         * SCHOOL2012 stands for SCHOOLBASED processes, in sequential order
         * (just like in WS2009).
         * Similarily to WS2009 and conversely to SCHOOL2012_PROD, plankton
         * concentration are read like production.
         */

        SCHOOL2012_PROD,
        /*
         * SCHOOL2012 stands for SCHOOLBASED processes, in sequential order
         * (just like in WS2009).
         * Difference from WS2009 comes from plankton concentration that is read
         * directly as a biomass.
         */
        SCHOOL2012_BIOM,
        /*
         * CASE1: syncrhomous updating of biomass in between all processes
         * we assume every cause is independant and
         * concomitant. No stochasticity neither competition within predation
         * process: every predator sees preys as they are at current time-step
         */
        CASE1,
        /*
         * CASE2: asynchronous updating of biomass within predation mortality
         * and synchronous updating of biomass between all processes.
         * we assume every cause is independant and concomitant.
         * Stochasticity and competition within predation process: prey and predator
         * biomass are being updated on the fly.
         */
        CASE2,
        /*CASE3: asynchronous updating of biomasses in between all processes
         * we assume every cause
         * compete with each other. Stochasticity at both school and mortality
         * process levels.
         */
        CASE3;
    }
    /*
     * Choose the version of Osmose tu run.
     * @see enum Version for details.
     */
    public static final Version VERSION = Version.CASE3;
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
    private Population population;
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
     * Time of the simulation in [year]
     */
    private int year;
    /*
     * Time step of the current year
     */
    private int i_step_year;
    /*
     * Time step of the simulation
     */
    private int i_step_simu;
    /*
     * Array of the species of the simulation
     */
    private Species[] species;
    /*
     * Initialisation param for species abd in function of an input size
     * spectrum Coeff of the relation nb = length^a * exp(b) In Rice : a=-5.8;
     * b=35.5
     */
    double a, b;
    /*
     * Initial min abd of last age class of a species
     */
    long abdIniMin;
    /*
     * Ratio between MPA and total grid surfaces, RS for Relative Size of MPA
     */
    double RS;
    /*
     * Whether fishing rates are the same every year or change throughout the
     * years of simulation
     */
    boolean isFishingInterannual;
    /*
     * Random generator
     */
    private static Random random = new Random();
    private List<School>[][] schoolMap;

    public void init() {

        population = new Population();
        schoolMap = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];

        year = 0;
        i_step_year = 0;
        i_step_simu = 0;
        numSerie = getOsmose().numSerie;
        nbTimeStepsPerYear = getOsmose().nbDtMatrix[numSerie];

        // Initialise plankton matrix
        iniPlanktonField(getOsmose().isForcing[numSerie]);

        //CREATION of the SPECIES
        species = new Species[getOsmose().nbSpeciesTab[numSerie]];
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(i + 1);
            species[i].init();
        }

        //INITIALISATION of SPECIES ABD ACCORDING TO SIZE SPECTRUM
        if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("biomass")) {
            iniBySpeciesBiomass();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("spectrum")) {
            iniBySizeSpectrum();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("random")) {
            iniRandomly();
        }
        removeDeadSchools();

        // Initialize all the tables required for saving output
        if (getOsmose().spatializedOutputs[numSerie]) {
            initSpatializedSaving();
        }

        isFishingInterannual = getOsmose().fishingRates[0].length > nbTimeStepsPerYear;
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

    private void updateSchoolMap() {

        // reset the map
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    if (null == schoolMap[i][j]) {
                        schoolMap[i][j] = new ArrayList();
                    } else {
                        schoolMap[i][j].clear();
                    }
                }
            }
        }
        
        // fill up the map
        for (School school : population) {
            if (!school.isUnlocated()) {
                schoolMap[school.getCell().get_igrid()][school.getCell().get_jgrid()].add(school);
            }
        }
    }
    
    public List<School> getSchools(Cell cell) {
        return schoolMap[cell.get_igrid()][cell.get_jgrid()];
    }

    private void setupMPA() {
        if ((getOsmose().thereIsMPATab[numSerie]) && (year == getOsmose().MPAtStartTab[numSerie])) {
            RS = (double) getOsmose().tabMPAiMatrix[numSerie].length / ((getGrid().getNbLines()) * getGrid().getNbColumns());
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(true);
            }
        } else if ((!getOsmose().thereIsMPATab[numSerie]) || (year > getOsmose().MPAtEndTab[numSerie])) {
            RS = 0;
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(false);
            }
        }
    }

    private void updateStages() {
        for (School school : population) {
            int i = school.getSpeciesIndex();
            school.updateFeedingStage(species[i].sizeFeeding, species[i].nbFeedingStages);
            school.updateAccessStage(getOsmose().accessStageThreshold[i], getOsmose().nbAccessStage[i]);
            school.updateDietOutputStage(species[i].dietStagesTab, species[i].nbDietStages);
        }
    }

    /*
     * save fish biomass before any mortality process for diets data (last
     * column of predatorPressure output file in Diets/)
     */
    private void saveBiomassBeforeMortality() {

        // update biomass
        if (getOsmose().dietsOutputMatrix[getOsmose().numSerie] && (year >= getOsmose().timeSeriesStart)) {

            for (School school : getPresentSchools()) {
                Indicators.biomPerStage[school.getSpeciesIndex()][school.dietOutputStage] += school.getBiomass();
            }
            getForcing().saveForDiet();
        }
        if (coupling != null && (year >= coupling.getStartYearLTLModel() - 1)) // save grid of plankton biomass one year before coupling so forcing mode is also saved
        {
            coupling.savePlanktonBiomass("csv");
        } else {
            forcing.savePlanktonBiomass(getOsmose().planktonBiomassOutputMatrix[numSerie]);
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
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            D = (spec.larvalMortalityRates[i_step_simu] + (spec.getOutMortality(0, i_step_year) / (float) (nbTimeStepsPerYear))) / (float) subdt;
        } else {
            D = (spec.D + school.getSpecies().getOutMortality(school.getAgeDt(), i_step_year)) / (float) (nbTimeStepsPerYear * subdt);
        }
        return D;
    }

    double computeNaturalMortality(School school, int subdt) {

        double D = getNaturalMortalityRate(school, subdt);
        return getAbundance(school) * (1.d - Math.exp(-D));
    }

    public double getFishingMortalityRate(School school, int subdt) {
        if (isFishable(school)) {
            Species spec = school.getSpecies();
            return spec.fishingRates[isFishingInterannual ? i_step_simu : i_step_year] / subdt;
        } else {
            return 0.d;
        }
    }

    private boolean isFishable(School school) {
        // Test whether fishing applies to this school
        // 1. School is recruited
        // 2. School is catchable (no MPA and no out of zone)
        return (school.getAgeDt() >= school.getSpecies().recruitAge)
                && school.isCatchable();
    }

    private double computeFishingMortality(School school, int subdt) {

        double F = getFishingMortalityRate(school, subdt);
        double nDead = 0;
        if (F > 0) {
            nDead = getAbundance(school) * (1 - Math.exp(-F));
        }
        return nDead;
    }

    private double computeBiomassToPredate(School predator, int subdt) {
        return getBiomass(predator) * predator.getSpecies().predationRate / (double) (nbTimeStepsPerYear * subdt);
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
        List<School> schools = getSchools(predator.getCell());
        int nFish = schools.size();
        double[] preyUpon = new double[schools.size() + forcing.getNbPlanktonGroups()];
        // find the preys
        int[] indexPreys = findPreys(predator);

        // Compute accessible biomass
        // 1. from preys
        double biomAccessibleTot = 0.d;
        for (int iPrey : indexPreys) {
            biomAccessibleTot += getAccessibleBiomass(predator, schools.get(iPrey));
        }
        // 2. from plankton
        float[] percentPlankton = getPercentPlankton(predator);
        for (int i = 0; i < forcing.getNbPlanktonGroups(); i++) {
            float tempAccess = getOsmose().accessibilityMatrix[getNbSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
            biomAccessibleTot += percentPlankton[i] * tempAccess * forcing.getPlankton(i).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
        }

        // Compute the potential biomass that predators could prey upon
        double biomassToPredate = computeBiomassToPredate(predator, subdt);
        /*
         * phv 20121219 - this is just a way to stick to what is done in
         * Osmose version SCHOOL2012 and previous version.
         * Tbe biomassToPredate variable of the predator is update on the fly.
         * Should check how it is done in version WS2009 and make sure that it
         * is equivalent to what is done here. It might have some consequences
         * for School.predSuccessRate which influences growth and starvation.
         */
        if (VERSION.equals(Version.SCHOOL2012_BIOM) || VERSION.equals(Version.SCHOOL2012_PROD)) {
            predator.biomassToPredate = biomassToPredate;
        }

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
                double ratio = getAccessibleBiomass(predator, schools.get(iPrey)) / biomAccessibleTot;
                preyUpon[iPrey] = ratio * biomassToPredate;
            }
            // Assess the gain for the predator from plankton
            // Assess the loss for the plankton caused by the predator
            for (int i = 0; i < forcing.getNbPlanktonGroups(); i++) {
                float tempAccess = getOsmose().accessibilityMatrix[getNbSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
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

        List<School> schools = getSchools(cell);
        double[][] preyUpon = new double[schools.size() + forcing.getNbPlanktonGroups()][schools.size() + forcing.getNbPlanktonGroups()];
        // Loop over the schools of the cell
        for (School school : schools) {
            school.nDeadPredation = 0;
        }
        for (int iPred = 0; iPred < schools.size(); iPred++) {
            preyUpon[iPred] = computePredation(schools.get(iPred), subdt);
        }
        return preyUpon;
    }

    private double getStarvationMortalityRate(School school, int subdt) {
        Species spec = school.getSpecies();

        // Compute the predation mortality rate
        double mortalityRate = 0;
        if (school.predSuccessRate <= school.getSpecies().criticalPredSuccess) {
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
        Species spec = predator.getSpecies();
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
        return getOsmose().accessibilityMatrix[prey.getSpeciesIndex()][prey.getAccessibilityStage()][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
    }

    /**
     * Returns a list of preys for a given predator.
     *
     * @param predator
     * @return the list of preys for this predator
     */
    private int[] findPreys(School predator) {

        Species spec = predator.getSpecies();
        List<School> schoolsInCell = getSchools(predator.getCell());
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

    private void growth() {

        for (School school : getPresentSchools()) {
            school.predSuccessRate = computePredSuccessRate(school.biomassToPredate, school.preyedBiomass);
            Species spec = school.getSpecies();
            int j = school.getAgeDt();
            if ((j == 0) || spec.isOut(j, getIndexTimeYear())) {
                // Linear growth for eggs and migrating schools
                school.setLength(school.getLength() + spec.deltaMeanLength[j]);
                school.setWeight((float) (spec.c * Math.pow(school.getLength(), spec.bPower)));
            } else {
                // Growth based on predation success
                school.growth(spec.minDelta[j], spec.maxDelta[j], spec.c, spec.bPower);
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
                reproduce(species[i]);
            } else {
                incomingFlux(species[i]);
            }
        }
    }

    public void newStep() {

        // Print in console the period already simulated
        printProgress();
        Indicators.reset();

        // Calculation of relative size of MPA
        setupMPA();

        // Loop over the year
        while (i_step_year < nbTimeStepsPerYear) {

            // Update some stages at the begining of the step
            updateStages();

            // Update LTL Data
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.runLTLModel();
            }
            forcing.updatePlankton(i_step_year);

            // Spatial distribution (distributeSpeciesIni() for year0 & indexTime0)
            if (i_step_simu > 0) {
                distributeSpecies();
            }

            // Preliminary actions before mortality processes
            saveBiomassBeforeMortality();

            // Compute mortality
            // (predation + fishing + natural mortality + starvation)
            for (School school : population) {
                school.resetDietVariables();
                school.nDeadFishing = 0;
                school.nDeadNatural = 0;
                school.nDeadPredation = 0;
                school.nDeadStarvation = 0;
                school.biomassToPredate = computeBiomassToPredate(school, 1);
                school.preyedBiomass = 0;
            }

            computeMortality(1, VERSION);
            removeDeadSchools();

            // Update of disappeared schools and plancton mortality
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.calculPlanktonMortality();
            }

            // Growth
            growth();

            // Save steps
            if (getOsmose().spatializedOutputs[numSerie]) {
                saveSpatializedStep();
            }
            Indicators.updateAndWriteIndicators();

            // Reproduction
            reproduction();
            removeDeadSchools();

            // Increment time step
            i_step_year++;
            i_step_simu++;
        }
        i_step_year = 0;  //end of the year
        year++; // go to following year

    }

    public void oldStep() {

        // Print in console the period already simulated
        printProgress();
        Indicators.reset();

        // Calculation of relative size of MPA
        setupMPA();

        // Loop over the year
        while (i_step_year < nbTimeStepsPerYear) {

            // Update some stages at the begining of the step
            updateStages();

            // Spatial distribution (distributeSpeciesIni() for year0 & indexTime0)
            if (!((i_step_year == 0) && (year == 0))) {
                distributeSpecies();
            }

            // Natural mortality (due to other predators)
            for (School school : population) {
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
                //school.nDeadNatural = nDead;
            }
            removeDeadSchools();

            // Update LTL Data
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.runLTLModel();
            }
            forcing.updatePlankton(i_step_year);

            // Predation
            saveBiomassBeforeMortality();
            for (Cell cell : getGrid().getCells()) {
                List<School> schools = getSchools(cell);
                Collections.shuffle(schools);
                int ns = schools.size();
                if (!(cell.isLand() || schools.isEmpty())) {
                    double[] nDeadPredation = new double[ns];
                    // Compute predation
                    for (School predator : schools) {
                        double[] preyUpon = computePredation(predator, 1);
                        for (int ipr = 0; ipr < ns; ipr++) {
                            if (ipr < ns) {
                                School prey = schools.get(ipr);
                                nDeadPredation[ipr] += prey.biom2abd(preyUpon[ipr]);
                                prey.nDeadPredation += prey.biom2abd(preyUpon[ipr]);
                            }
                        }
                        predator.preyedBiomass = sum(preyUpon);
                    }
                    // Apply predation mortality
                    for (int is = 0; is < ns; is++) {
                        School school = schools.get(is);
                        school.nDeadPredation = 0;
                        school.predSuccessRate = computePredSuccessRate(school.biomassToPredate, school.preyedBiomass);
                        school.setAbundance(school.getAbundance() - nDeadPredation[is]);
                        if (school.getAbundance() < 1.d) {
                            school.setAbundance(0.d);
                            school.tagForRemoval();
                        }
                    }
                }
            }
            removeDeadSchools();

            // Compute Plankton Mortality
            if ((null != coupling) && (year >= coupling.getStartYearLTLModel())) {
                coupling.calculPlanktonMortality();
            }

            // Starvation
            for (School school : getPresentSchools()) {
                double nDead = computeStarvationMortality(school, 1);
                school.setAbundance(school.getAbundance() - nDead);
                if (school.getAbundance() < 1.d) {
                    school.setAbundance(0);
                    school.tagForRemoval();
                }
                //school.nDeadStarvation = nDead;
            }
            removeDeadSchools();

            // Growth
            growth();

            // Fishing
            for (School school : getPresentSchools()) {
                if (school.getAbundance() != 0.d) {
                    double nDead = computeFishingMortality(school, 1);
                    if (nDead != 0.d) {
                        school.setAbundance(school.getAbundance() - nDead);
                        if (school.getAbundance() < 1.d) {
                            nDead = school.getAbundance();
                            school.setAbundance(0);
                            school.tagForRemoval();
                        }
                        //school.nDeadFishing = nDead;
                    }
                }
            }
            removeDeadSchools();

            // Save steps
            Indicators.updateAndWriteIndicators();
            if (getOsmose().spatializedOutputs[numSerie]) {
                saveSpatializedStep();
            }

            // Reproduction
            reproduction();
            removeDeadSchools();

            // Increment time step
            i_step_year++;
            i_step_simu++;
        }
        i_step_year = 0;  //end of the year
        year++; // go to following year
    }

    public double[][] computeMortality_case3(int subdt, Cell cell) {

        List<School> schools = getSchools(cell);
        int ns = schools.size();
        int npl = forcing.getNbPlanktonGroups();
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];

        int[] seqPred = new int[ns];
        for (int i = 0; i < ns; i++) {
            schools.get(i).hasPredated = false;
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
                        predator = schools.get(seqPred[i]);
                        double[] preyUpon = computePredation(predator, subdt);
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School school = schools.get(ipr);
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
                        predator = schools.get(seqStarv[i]);
                        if (predator.hasPredated) {
                            nDeadMatrix[seqStarv[i]][ns] = computeStarvationMortality(predator, subdt);
                            predator.nDeadStarvation = nDeadMatrix[seqStarv[i]][ns];
                        }
                        break;
                    case 2:
                        // Natural mortality
                        nDeadMatrix[seqNat[i]][ns + 1] = computeNaturalMortality(schools.get(seqNat[i]), subdt);
                        schools.get(seqNat[i]).nDeadNatural = nDeadMatrix[seqNat[i]][ns + 1];
                        break;
                    case 3:
                        // Fishing Mortality
                        nDeadMatrix[seqFish[i]][ns + 2] = computeFishingMortality(schools.get(seqFish[i]), subdt);
                        schools.get(seqFish[i]).nDeadFishing = nDeadMatrix[seqFish[i]][ns + 2];
                        break;
                }
            }
        }

        return nDeadMatrix;
    }

    public double[][] computeMortality_case2(int subdt, Cell cell) {

        List<School> schools = getSchools(cell);
        int ns = schools.size();
        int npl = forcing.getNbPlanktonGroups();
        double[][] mortalityRateMatrix = new double[ns + npl][ns + 3];
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];
        double[] totalMortalityRate = new double[ns + npl];


        //
        // Assess all mortality independently from each other
        Collections.shuffle(schools);
        for (int ipd = 0; ipd < ns; ipd++) {
            // Predation mortality 
            School predator = schools.get(ipd);
            double[] preyUpon = computePredation(predator, subdt);
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                double predationMortalityRate;
                if (ipr < ns) {
                    School school = schools.get(ipr);
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
            School school = schools.get(is);
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
                abundance = schools.get(ipr).getAbundance();
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

        List<School> schools = getSchools(cell);
        int ns = schools.size();
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
                    School school = schools.get(ipr);
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
            School school = schools.get(is);
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
                    abundance = schools.get(ipr).getAbundance();
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
                School predator = schools.get(ipd);
                double preyedBiomass = 0;
                for (int ipr = 0; ipr < (ns + npl); ipr++) {
                    if (ipr < ns) {
                        preyedBiomass += schools.get(ipr).adb2biom(nDeadMatrix[ipr][ipd]);
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
                School school = schools.get(ipr);
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
    public void computeMortality(int subdt, Version version) {

        double[][] mortality = null;
        if (DEBUG_MORTALITY) {
            mortality = new double[getNbSpecies()][11];
            for (School school : population) {
                if (school.getAgeDt() >= school.getSpecies().recruitAge) {
                    mortality[school.getSpeciesIndex()][10] += school.getAbundance();
                }
            }
        }

        // Loop over cells
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getSchools(cell);
            if (!(cell.isLand() || schools.isEmpty())) {
                int ns = schools.size();
                int npl = forcing.getNbPlanktonGroups();

                // Reset nDeads
                for (School school : schools) {
                    school.nDeadPredation = 0;
                    school.nDeadStarvation = 0;
                    school.nDeadNatural = 0;
                    school.nDeadFishing = 0;
                }

                double[][] nDeadMatrix = null;
                switch (version) {
                    case CASE1:
                        nDeadMatrix = computeMortality_case1(subdt, cell);
                        break;
                    case CASE2:
                        nDeadMatrix = computeMortality_case2(subdt, cell);
                        break;
                    case CASE3:
                        nDeadMatrix = computeMortality_case3(subdt, cell);
                        break;
                    default:
                        throw new UnsupportedOperationException("Version " + version + " not supported in computeMortality() function.");
                }

                // Apply mortalities
                for (int is = 0; is < ns; is++) {
                    School school = schools.get(is);
                    // 1. Predation
                    school.nDeadPredation = 0.d;
                    double preyedBiomass = 0.d;
                    for (int ipd = 0; ipd < ns; ipd++) {
                        school.nDeadPredation += nDeadMatrix[is][ipd];
                    }
                    for (int ipr = 0; ipr < ns + npl; ipr++) {
                        if (ipr < ns) {
                            preyedBiomass += schools.get(ipr).adb2biom(nDeadMatrix[ipr][is]);
                        } else {
                            preyedBiomass += nDeadMatrix[ipr][is];
                        }
                    }
                    school.preyedBiomass += preyedBiomass;
                    // update TL
                    school.tmpTL = 0;
                    if (preyedBiomass > 0.d) {
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School prey = schools.get(ipr);
                                double biomPrey = prey.adb2biom(nDeadMatrix[ipr][is]);
                                if (getOsmose().isDietOuput()) {
                                    school.dietTemp[prey.getSpeciesIndex()][prey.dietOutputStage] += biomPrey;
                                }
                                float TLprey = (prey.getAgeDt() == 0) || (prey.getAgeDt() == 1)
                                        ? Species.TL_EGG
                                        : prey.trophicLevel;
                                school.tmpTL += TLprey * biomPrey / preyedBiomass;
                            } else {
                                school.tmpTL += forcing.getPlankton(ipr - ns).trophicLevel * nDeadMatrix[ipr][is] / preyedBiomass;
                                if (getOsmose().isDietOuput()) {
                                    school.dietTemp[getNbSpecies() + (ipr - ns)][0] += nDeadMatrix[ipr][is];
                                }
                            }
                            //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                        }
                        school.tmpTL += 1;
                    } else {
                        school.tmpTL = school.trophicLevel;
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
                        int i = school.getSpeciesIndex();
                        if (school.getAgeDt() >= species[i].recruitAge) {
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
                    }
                }
                for (School school : schools) {
                    school.trophicLevel = school.tmpTL;
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
            Indicators.writeVariable(year + (i_step_year + 1f) / (float) nbTimeStepsPerYear, mortality, filename, headers, "Instaneous number of deads and mortality rates");
        }
    }

    public void removeDeadSchools() {
        // Removing dead schools
        List<School> schoolsToRemove = new ArrayList();
        for (School school : population) {
            if (school.willDisappear()) {
                schoolsToRemove.add(school);
            }
        }
        population.removeAll(schoolsToRemove);
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

                for (int k = speciesj.getNumberCohorts() - 2; k >= (2 * nbTimeStepsPerYear); k--) {
                    speciesj.tabAbdIni[k] = Math.round(speciesj.tabAbdIni[k + 1] * Math.exp((0.5 / (float) nbTimeStepsPerYear)));
                    speciesj.tabBiomIni[k] = ((double) speciesj.tabAbdIni[k]) * speciesj.tabMeanWeight[k] / 1000000.;
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
                }

                speciesj.tabAbdIni[0] = Math.round(speciesj.tabAbdIni[1] * Math.exp(10.));
                speciesj.tabBiomIni[0] = ((double) speciesj.tabAbdIni[0]) * speciesj.tabMeanWeight[0] / 1000000.;

                //creation of the cohorts
                for (int k = 0; k < speciesj.getNumberCohorts(); k++) {
                    createCohort(speciesj, k, speciesj.tabAbdIni[k], speciesj.tabMeanLength[k], speciesj.tabMeanWeight[k]);
                }
            }
        }
    }

    private void createCohort(Species species, int age, long abundance, float iniLength, float iniWeight) {
        if (abundance > 0.d) {
            //System.out.println(species.getName() + " " + age);
            int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
            for (int i = 0; i < nbSchools; i++) {
                population.add(new School(species, abundance / nbSchools, iniLength, iniWeight, age));
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
            double biomass = 0;
            double sumExp = 0;
            /*
             * phv 2011/11/24 For species that do not reproduce locally, initial
             * biomass is set to zero.
             */
            if (!speci.isReproduceLocally()) {
                for (int j = 0; j < speci.getNumberCohorts(); j++) {
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
                    biomass += speci.tabBiomIni[0];
                }
                speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear));
                speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
                if (speci.indexAgeClass0 <= 1) {
                    biomass += speci.tabBiomIni[1];
                }
                for (int j = 2; j < speci.getNumberCohorts(); j++) {
                    speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbTimeStepsPerYear));
                    speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                    if (speci.indexAgeClass0 <= j) {
                        biomass += speci.tabBiomIni[j];
                    }
                }

                correctingFactor = (float) (getOsmose().spBiomIniTab[numSerie][i] / biomass);
                // we make corrections on initial abundance to fit the input biomass
                speci.tabAbdIni[0] = (long) ((abdIni * correctingFactor) / (Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
                speci.tabBiomIni[0] = ((double) speci.tabAbdIni[0]) * speci.tabMeanWeight[0] / 1000000.;
                speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear));
                speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
                for (int j = 2; j < speci.getNumberCohorts(); j++) {
                    speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbTimeStepsPerYear));
                    speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                }
            }

            // create the cohorts
            for (int j = 0; j < speci.getNumberCohorts(); j++) {
                createCohort(speci, j, speci.tabAbdIni[j], speci.tabMeanLength[j], speci.tabMeanWeight[j]);
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

    public List<School> getPresentSchools() {
        return FilteredSets.subset(population, new PresentSchoolFilter(i_step_year));
    }

    public Population getPopulation() {
        return population;
    }

    private void randomDistribution(School school) {
        if (school.isUnlocated()) {
            List<Cell> cells = getOsmose().randomMaps[school.getSpeciesIndex()];
            school.moveToCell(randomDeal(cells));
        } else {
            school.moveToCell(randomDeal(getAccessibleCells(school)));
        }
    }

    private void moveOut(School school) {
        school.setOffGrid();
    }

    private void mapsDistribution(School school) {

        int i = school.getSpeciesIndex();
        int j = school.getAgeDt();
        /*
         * Do not distribute cohorts that are presently out of
         * the simulated area.
         */
        if (species[i].isOut(j, i_step_year)) {
            moveOut(school);
            return;
        }

        // Get current map and max probability of presence
        int numMap = getOsmose().numMap[i][j][i_step_year];
        GridMap map = getOsmose().getMap(numMap);
        float tempMaxProbaPresence = getOsmose().maxProbaPresence[numMap];

        /*
         * Check whether the map has changed from previous cohort
         * and time-step.
         * For cohort zero and first time-step of the simulation we can
         * assert sameMap = false;
         */
        boolean sameMap = false;
        if (j > 0 && i_step_simu > 0) {
            int oldTime;
            if (i_step_year == 0) {
                oldTime = nbTimeStepsPerYear - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            if (numMap == getOsmose().numMap[i][j - 1][oldTime]) {
                sameMap = true;
            }
        }

        // Move the school
        if (!sameMap || school.isUnlocated()) {
            /*
             * Random distribution in a map, either because the map has
             * changed from previous cohort and time-step, or because the
             * school was unlocated due to migration.
             */
            int indexCell;
            int nCells = getGrid().getNbColumns() * getGrid().getNbLines();
            double proba;
            do {
                indexCell = (int) Math.round((nCells - 1) * Math.random());
                proba = getOsmose().maps[getOsmose().numMap[i][j][i_step_year]].getValue(getGrid().getCell(indexCell));
            } while (proba <= 0 || proba < Math.random() * tempMaxProbaPresence);
            school.moveToCell(getGrid().getCell(indexCell));
        } else {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(randomDeal(getAccessibleCells(school, map)));
        }
    }

    /**
     * Randomly choose a cell among the given list of cells.
     *
     * @param cells, a list of cells
     * @return a cell from the list of cells.
     */
    private Cell randomDeal(List<Cell> cells) {
        int index = (int) Math.round((cells.size() - 1) * Math.random());
        return cells.get(index);
    }

    /**
     * For debugging purpose, check whether the school is located in a cell that
     * belongs to the map number numMap
     *
     * @param school to be tested
     * @param numMap, the number of the map
     * @return true if school is located in a cell contained in map number
     * numMap and false otherwise
     */
    private boolean checkSchoolDistribution(School school, int numMap) {
        Cell cell = school.getCell();
        GridMap map = getOsmose().getMap(numMap);
        if (map.getValue(cell) > 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the adjacent cells of a given school that are contained in the given
     * map.
     *
     * @param school
     * @param map
     * @return
     */
    private List<Cell> getAccessibleCells(School school, GridMap map) {

        Cell cell = school.getCell();
        if (map.getValue(cell) <= 0) {
            StringBuilder str = new StringBuilder("Inconsistency in moving ");
            str.append(school.toString());
            str.append("\n");
            str.append("It is not in the geographical area it is supposed to be...");
            System.out.println(str.toString());
        }
        List<Cell> accessibleCells = new ArrayList();
        // 1. Get all surrounding cells
        Iterator<Cell> neighbours = getGrid().getNeighbourCells(cell).iterator();
        while (neighbours.hasNext()) {
            Cell neighbour = neighbours.next();
            // 2. Eliminate cell that is on land
            // 3. Add the cell if it is within the current map of distribution 
            if (!neighbour.isLand() && map.getValue(neighbour) > 0) {
                accessibleCells.add(neighbour);
            }
        }
        return accessibleCells;
    }

    /**
     * Create a list of the accessible cells for a given cell: neighbour cells
     * that are not in land + current cell
     *
     * @param school
     * @return the list of cells accessible to the school
     */
    private List<Cell> getAccessibleCells(School school) {

        Cell cell = school.getCell();
        List<Cell> accessibleCells = new ArrayList();
        Iterator<Cell> neighbors = getGrid().getNeighbourCells(cell).iterator();
        while (neighbors.hasNext()) {
            Cell neighbor = neighbors.next();
            if (!neighbor.isLand()) {
                accessibleCells.add(neighbor);
            }
        }
        return accessibleCells;
    }

    private void connectivityDistribution(School school) {

        // loop over the schools of the species
        int i = school.getSpeciesIndex();
        int j = school.getAgeDt();
        /*
         * Do not distribute cohorts that are presently out of
         * the simulated area.
         */
        if (species[i].isOut(j, i_step_year)) {
            moveOut(school);
            return;
        }

        // Get current map and max probability of presence
        int numMap = getOsmose().numMap[i][j][i_step_year];
        GridMap map = getOsmose().getMap(numMap);
        float tempMaxProbaPresence = getOsmose().maxProbaPresence[numMap];

        // init = true if either cohort zero or first time-step of the simulation
        boolean init = (j == 0) | (i_step_simu == 0);
        /*
         * boolean sameMap
         * Check whether the map has changed from previous cohort
         * and time-step.
         */
        boolean sameMap = false;
        if (j > 0 && i_step_simu > 0) {
            int oldTime;
            if (i_step_year == 0) {
                oldTime = nbTimeStepsPerYear - 1;
            } else {
                oldTime = i_step_year - 1;
            }
            if (numMap == getOsmose().numMap[i][j - 1][oldTime]) {
                sameMap = true;
            }
        }

        // Move the school
        if (init || school.isUnlocated()) {
            /*
             * Random distribution in a map, either because it is cohort
             * zero or first time-step or because the
             * school was unlocated due to migration.
             */
            int indexCell;
            int nCells = getGrid().getNbColumns() * getGrid().getNbLines();
            double proba;
            do {
                indexCell = (int) Math.round((nCells - 1) * Math.random());
                proba = getOsmose().maps[getOsmose().numMap[i][j][i_step_year]].getValue(getGrid().getCell(indexCell));
            } while (proba <= 0 || proba < Math.random() * tempMaxProbaPresence);
            school.moveToCell(getGrid().getCell(indexCell));
        } else if (sameMap) {
            // Random move in adjacent cells contained in the map.
            school.moveToCell(randomDeal(getAccessibleCells(school, map)));
        } else {
            connectivityMoveSchool(school, numMap);
        }
    }

    private void connectivityMoveSchool(School school, int numMap) {
        // get the connectivity matrix associated to object school
        // species i, cohort j and time step indexTime.
        ConnectivityMatrix matrix = getOsmose().connectivityMatrix[numMap];
        // get the connectivity of the cell where the school is
        // currently located
        int iCell = school.getCell().getIndex();
        ConnectivityLine cline = matrix.clines.get(iCell);

        if (!school.getCell().isLand() && null == cline) { // TD ~~
            //if (null == cline) { // TD ~~
            throw new NullPointerException("Could not find line associated to cell "
                    + iCell + " in connectivity matrix " + " ;isLand= " + school.getCell().isLand());
        }

        // TD CHANGE 23.10.12
        // Lines with only 0 come with length = 0
        // cumsum can't work with it
        // workaround: run cumsum only if length > 0 (otherwise keep initial 0 values)

        // computes the cumulative sum of this connectivity line   
        if (!school.getCell().isLand() && cline.connectivity.length > 0) { //++TD

            float[] cumSum = cumSum(cline.connectivity);

            //TD DEBUG 29.10.2012
            //if (indexTime >= (5 * nbTimeStepsPerYear) && school.getCell().isLand()) {
            if (i_step_year >= 1 && school.getCell().isLand()) {
                System.out.println("SCHOOL SWIMMING OUT OF THE POOL! <-----------------------------------");
            }
            //TD DEBUG 29.10.2012
            // choose the new cell
            // TD ~~ 24.10.2012
            //float random = (float) (Math.random() * cumSum[cumSum.length - 1]); // random 0-1 * plus grande valeur de cumsum (dernière valeur) --> ???
            //System.out.println("cumSum[cumSum.length - 1]: " + cumSum[cumSum.length - 1] + " random: " + random );
            // alternative : TD ~~
            float random = (float) (Math.random()); //TD ~~
            int iRandom = cumSum.length - 1; // on prend le dernier de la liste
            while (random < cumSum[iRandom] && iRandom > 0) { // et on redescend progressivement la liste jusqu'à trouver une valeur inférieure à random
                iRandom--;
            }
            school.moveToCell(getGrid().getCell(cline.indexCells[iRandom]));
        }
    }

    private float[] cumSum(float[] connectivity) {


        float[] cumSum = new float[connectivity.length];
        cumSum[0] = connectivity[0];

        for (int i = 1; i < cumSum.length; i++) {
            cumSum[i] += cumSum[i - 1] + connectivity[i];
        }

        // TD ~~ 24.10.2012  --> normalisation cumSum
        for (int i = 0; i < cumSum.length; i++) {
            //System.out.println(cumSum[i] + "------------------------------>"); 
            cumSum[i] += cumSum[i] / cumSum[cumSum.length - 1];
            //System.out.println(cumSum[i] + "------------------------------>");
        } // FIN TD ~~ 24.10.2012  --> normalisation cumSum

        return cumSum;
    }

    public void distributeSpecies() {
        
        for (School school : population) {
            switch (getOsmose().spatialDistribution[school.getSpeciesIndex()]) {
                case RANDOM:
                    randomDistribution(school);
                    break;
                case MAPS:
                    mapsDistribution(school);
                    break;
                case CONNECTIVITY:
                    connectivityDistribution(school);
                    break;
            }
        }
        updateSchoolMap();
    }

    public List<School> getSchools(Species species) {
        return FilteredSets.subset(population, new SpeciesFilter(species.getIndex()));
    }

    private void reproduce(Species species) {

        double SSB = 0;
        List<School> schools = getSchools(species);
        for (School school : schools) {
            if (school.getLength() >= species.sizeMat) {
                SSB += school.getBiomass();
            }
        }

        double season = species.seasonSpawning.length > getNbTimeStepsPerYear()
                ? species.seasonSpawning[getIndexTimeSimu()]
                : species.seasonSpawning[getIndexTimeYear()];
        double nbEggs = species.sexRatio * species.alpha * season * SSB * 1000000;

        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : schools) {
            school.age += 1;
            if (school.getAgeDt() > (species.getNumberCohorts() - 1)) {
                school.tagForRemoval();
            }
        }

        //UPDATE AGE CLASS 0
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (nbEggs == 0.d) {
            // do nothing, zero school
        } else if (nbEggs < nbSchools) {
            School school0 = new School(species, nbEggs, species.eggSize, species.eggWeight, 0);
            population.add(school0);
        } else if (nbEggs >= nbSchools) {
            for (int i = 0; i < nbSchools; i++) {
                School school0 = new School(species, nbEggs / nbSchools, species.eggSize, species.eggWeight, 0);
                population.add(school0);
            }
        }
    }

    /*
     * phv 2011/11/22 Created new function for modeling incoming flux of biomass
     * for species that do not reproduce in the simulated domain.
     */
    public void incomingFlux(Species species) {

        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : getSchools(species)) {
            school.age += 1;
            if (school.getAgeDt() > (species.getNumberCohorts() - 1)) {
                school.tagForRemoval();
            }
        }

        /*
         * Incoming flux
         */
        double biomassIn = species.biomassFluxIn * species.seasonSpawning[i_step_year];
        float meanWeigthIn = (float) (species.c * Math.pow(species.meanLengthIn, species.bPower));
        long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (abundanceIn > 0 && abundanceIn < nbSchools) {
            population.add(new School(species, abundanceIn, species.meanLengthIn, meanWeigthIn, species.ageMeanIn));
        } else if (abundanceIn >= nbSchools) {
            int mod = (int) (abundanceIn % nbSchools);
            int abdSchool = (int) (abundanceIn / nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                abdSchool += (i < mod) ? 1 : 0;
                population.add(new School(species, abdSchool, species.meanLengthIn, meanWeigthIn, species.ageMeanIn));
            }
        }
        //System.out.println(name + " incoming flux " + biomassIn + " [tons] + ageIn: " + ageMeanIn);
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
            for (School school : getSchools(cell)) {
                if (school.getAgeDt() > school.getSpecies().indexAgeClass0 && !school.getSpecies().isOut(school.getAgeDt(), i_step_year)) {
                    nbSchools[school.getSpeciesIndex()] += 1;
                    biomass[school.getSpeciesIndex()][cell.get_igrid()][cell.get_jgrid()] += school.getBiomass();
                    abundance[school.getSpeciesIndex()][cell.get_igrid()][cell.get_jgrid()] += school.getAbundance();
                    mean_size[school.getSpeciesIndex()][cell.get_igrid()][cell.get_jgrid()] += school.getLength();
                    tl[school.getSpeciesIndex()][cell.get_igrid()][cell.get_jgrid()] += school.trophicLevel;
                    //yield[school.getSpecies().getIndex()][cell.get_igrid()][cell.get_jgrid()] += (school.catches * school.getWeight() / 1000000.d);
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

        float timeSaving = year + (i_step_year + 1f) / (float) nbTimeStepsPerYear;
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

    public int getIndexTimeYear() {
        return i_step_year;
    }

    public int getIndexTimeSimu() {
        return i_step_simu;
    }

    public static void shuffleArray(int[] a) {
        // Shuffle array
        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, random.nextInt(i));
        }
    }

    private static void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }
}
