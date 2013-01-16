package fr.ird.osmose;

import fr.ird.osmose.ConnectivityMatrix.ConnectivityLine;
import fr.ird.osmose.filter.AliveSchoolFilter;
import fr.ird.osmose.filter.PresentSchoolFilter;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.PopulatingProcess;
import fr.ird.osmose.step.AbstractStep;
import fr.ird.osmose.step.ConcomitantMortalityStep;
import fr.ird.osmose.step.SequentialMortalityStep;
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
         * CASE1
         * > It is assumed that every cause is independant and concomitant.
         * > No stochasticity neither competition within predation process: every
         * predator sees preys as they are at the begining of the time-step.
         * > Synchromous updating of school biomass.
         */
        CASE1,
        /*
         * CASE2
         * > It is assumed that every cause is independant and concomitant.
         * > Stochasticity and competition within predation process: prey and
         * predator biomass are being updated on the fly virtually (indeed the
         * update is not effective outside the predation process,
         * it is just temporal).
         * > Synchronous updating of school biomass.
         */
        CASE2,
        /*
         * CASE3
         * > It is assumed that every cause compete with each other.
         * > Stochasticity and competition within predation process.
         * > Asynchronous updating of school biomass (it means biomass are updated
         * on the fly).
         */
        CASE3;
    }
    /*
     * Choose the version of Osmose tu run.
     * @see enum Version for details.
     */
    public static final Version VERSION = Version.CASE3;
///////////////////////////////
// Declaration of the variables
///////////////////////////////
    private Population population;
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
    private int nTimeStepsPerYear;
    /*
     * Number of years of simulation
     */
    private int nYear;
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
     * What should be done within one time step
     */
    private AbstractStep step;

///////////////////////////////
// Definition of the functions
///////////////////////////////
    /**
     * Initialize the simulation
     */
    public void init() {

        // Create a new population, empty at the moment
        population = new Population();

        // Reset time variables
        year = 0;
        i_step_year = 0;
        i_step_simu = 0;
        numSerie = getOsmose().numSerie;
        nTimeStepsPerYear = getOsmose().nbDtMatrix[numSerie];
        nYear = getOsmose().simulationTimeTab[numSerie];

        // Create the species
        species = new Species[getOsmose().nbSpeciesTab[numSerie]];
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(i);
            species[i].init();
        }

        // Instantiate the Step
        switch (VERSION) {
            case SCHOOL2012_PROD:
            case SCHOOL2012_BIOM:
                step = new SequentialMortalityStep();
                break;
            case CASE1:
            case CASE2:
            case CASE3:
                step = new ConcomitantMortalityStep();
        }
        step.init();

        // Initialize the population
        AbstractProcess populatingProcess = new PopulatingProcess();
        populatingProcess.init();
        populatingProcess.run();

        // Initialize spatialized outputs
        if (getOsmose().spatializedOutputs[numSerie]) {
            initSpatializedSaving();
        }
    }

    /**
     * Print the progress of the simulation in text console
     */
    private void progress() {
        // screen display to check the period already simulated
        if (year % 5 == 0) {
            System.out.println("year " + year + " | CPU time " + new Date());   // t is annual
        } else {
            System.out.println("year " + year);
        }
    }

    private void setupMPA() {
        if ((getOsmose().thereIsMPATab[numSerie]) && (year == getOsmose().MPAtStartTab[numSerie])) {
            //RS = (double) getOsmose().tabMPAiMatrix[numSerie].length / ((getGrid().getNbLines()) * getGrid().getNbColumns());
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(true);
            }
        } else if ((!getOsmose().thereIsMPATab[numSerie]) || (year > getOsmose().MPAtEndTab[numSerie])) {
            //RS = 0;
            for (int index = 0; index < getOsmose().tabMPAiMatrix[numSerie].length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[numSerie][index], getOsmose().tabMPAjMatrix[numSerie][index]).setMPA(false);
            }
        }
    }

    public void updateStages() {
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
    public void saveBiomassBeforeMortality() {

        // update biomass
        if (getOsmose().dietsOutputMatrix[getOsmose().numSerie] && (year >= getOsmose().timeSeriesStart)) {

            for (School school : getPresentSchools()) {
                Indicators.biomPerStage[school.getSpeciesIndex()][school.dietOutputStage] += school.getBiomass();
            }
            getForcing().saveForDiet();
        }
        forcing.savePlanktonBiomass(getOsmose().planktonBiomassOutputMatrix[numSerie]);
    }

    public double getAbundance(School school) {
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

    public void run() {

        while (year < nYear) {

            // Print progress in console
            progress();

            // Reset indicators
            Indicators.reset();

            // Calculate relative size of MPA
            setupMPA();

            // Loop over the year
            while (i_step_year < nTimeStepsPerYear) {
                // Run a new step
                step.step();
                // Increment time step
                i_step_year++;
                i_step_simu++;
            }
            // End of the year
            i_step_year = 0;
            // Go to following year
            year++;
        }
    }

    public List<School> getPresentSchools() {
        return FilteredSets.subset(population, new PresentSchoolFilter(i_step_year));
    }

    public List<School> getAliveSchools() {
        return FilteredSets.subset(population, new AliveSchoolFilter());
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
                oldTime = nTimeStepsPerYear - 1;
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
                oldTime = nTimeStepsPerYear - 1;
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
        population.updateSchoolMap();
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
        } catch (InvalidRangeException | IOException ex) {
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
            for (School school : getPopulation().getSchools(cell)) {
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

        float timeSaving = year + (i_step_year + 1f) / (float) nTimeStepsPerYear;
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
        } catch (IOException | InvalidRangeException ex) {
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
    
    public void setForcing(LTLForcing forcing) {
        this.forcing = forcing;
    }

    public int getNumberTimeStepsPerYear() {
        return nTimeStepsPerYear;
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

    public int getNumberYears() {
        return nYear;
    }
    
    private IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }
}
