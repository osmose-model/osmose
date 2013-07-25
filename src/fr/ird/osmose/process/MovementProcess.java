package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.util.ConnectivityMatrix;
import fr.ird.osmose.util.GridMap;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class MovementProcess extends AbstractProcess {

    private final static float ONE_DEG_LATITUDE_IN_METER = 111138.f;
    private AbstractProcess[] movements;
    /*
     * Ranges of movement in cell during one Osmose time step
     */
    private int[] range;

    /*
     * Type of spatial distribution
     */
    private SpatialDistribution[] spatialDistribution;
    /*
     * Distribution
     */
    private int[] sizeRandomMap;	    //used only for Qsimulation.iniRepartitionAleat() ie for random distribution
    private int[][][] indexMaps;        //gives a number of map for[species][cohort][dt]
    private GridMap[] maps;
    private float[] maxProbaPresence;
    private ConnectivityMatrix[] connectivityMatrix;
    private String[] mapFile;
    private int[] mapIndexNoTwin;

    public MovementProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        loadParameters();

        int nSpecies = getConfiguration().getNSpecies();
        // init distribution
        range = new int[nSpecies];
        movements = new AbstractProcess[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            range[i] = getConfiguration().getInt("movement.randomwalk.range.sp" + i);
            switch (getSpatialDistribution(i)) {
                case RANDOM:
                    movements[i] = new RandomDistributionProcess(getIndexSimulation(), getSimulation().getSpecies(i), this);
                    break;
                case MAPS:
                    movements[i] = new MapDistributionProcess(getIndexSimulation(), getSimulation().getSpecies(i), this);
                    break;
                case CONNECTIVITY:
                    movements[i] = new ConnectivityDistributionProcess(getIndexSimulation(), getSimulation().getSpecies(i), this);
                    break;
            }
            movements[i].init();
        }
    }

    @Override
    public void run() {
        for (AbstractProcess movement : movements) {
            movement.run();
        }
        getSchoolSet().updateSchoolMap();
    }

    /**
     * Get the adjacent cells of a given school that are contained in the given
     * map.
     *
     * @param school
     * @param map
     * @return
     */
    List<Cell> getAccessibleCells(School school, GridMap map) {

        Cell cell = school.getCell();
        if (map.getValue(cell) <= 0.d) {
            StringBuilder str = new StringBuilder("Inconsistency in moving ");
            str.append(school.toString());
            str.append("\n");
            str.append("It is not in the geographical area it is supposed to be...");
            getLogger().warning(str.toString());
        }
        List<Cell> accessibleCells = new ArrayList();
        // 1. Get all surrounding cells
        Iterator<Cell> neighbours = getGrid().getNeighbourCells(cell, range[school.getSpeciesIndex()]).iterator();
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
     * Randomly choose a cell among the given list of cells.
     *
     * @param cells, a list of cells
     * @return a cell from the list of cells.
     */
    Cell randomDeal(List<Cell> cells) {
        int index = (int) Math.round((cells.size() - 1) * Math.random());
        return cells.get(index);
    }

    /**
     * Create a list of the accessible cells for a given cell: neighbour cells
     * that are not in land + current cell
     *
     * @param school
     * @return the list of cells accessible to the school
     */
    List<Cell> getAccessibleCells(School school) {

        Cell cell = school.getCell();
        List<Cell> accessibleCells = new ArrayList();
        Iterator<Cell> neighbors = getGrid().getNeighbourCells(cell, range[school.getSpeciesIndex()]).iterator();
        while (neighbors.hasNext()) {
            Cell neighbor = neighbors.next();
            if (!neighbor.isLand()) {
                accessibleCells.add(neighbor);
            }
        }
        return accessibleCells;
    }

    private void loadParameters() {


        int nSpecies = getConfiguration().getNSpecies();
        spatialDistribution = new SpatialDistribution[nSpecies];
        sizeRandomMap = new int[nSpecies];

        for (int i = 0; i < nSpecies; i++) {
            String areaDistribMethod = getConfiguration().getString("movement.distribution.method.sp" + i);
            if (areaDistribMethod.equalsIgnoreCase("random")) {
                spatialDistribution[i] = SpatialDistribution.RANDOM;
            } else if (areaDistribMethod.equalsIgnoreCase("maps")) {
                spatialDistribution[i] = SpatialDistribution.MAPS;
            } else if (areaDistribMethod.equalsIgnoreCase("connectivity")) {
                spatialDistribution[i] = SpatialDistribution.CONNECTIVITY;
            } else {
                throw new UnsupportedOperationException("Distribution method is either 'random' | 'maps' | 'connectivity'");
            }
        }
        /*
         * get number of maps
         */

        int nmap = getConfiguration().findKeys("movement.map*.species").size();
        maps = new GridMap[nmap];
        connectivityMatrix = new ConnectivityMatrix[nmap];
        maxProbaPresence = new float[nmap];
        mapFile = new String[nmap];
        mapIndexNoTwin = new int[nmap];
        indexMaps = new int[nSpecies][][];
        int nSteps = getConfiguration().getNStepYear() * getConfiguration().getNYear();
        for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
            int lifespan = getSpecies(iSpec).getLifespanDt();
            indexMaps[iSpec] = new int[lifespan][];
            for (int iAge = 0; iAge < lifespan; iAge++) {
                indexMaps[iSpec][iAge] = new int[nSteps];
                for (int iStep = 0; iStep < nSteps; iStep++) {
                    indexMaps[iSpec][iAge][iStep] = -1;
                }
            }
        }

        /**
         * Loop over the maps
         */
        int imap = 0;
        for (int indexMap = 0; indexMap < nmap; indexMap++) {
            while (!getConfiguration().canFind("movement.map" + imap + ".species")) {
                imap++;
            }
            /*
             * read species number
             */
            int iSpec = getSpecies(getConfiguration().getString("movement.map" + imap + ".species")).getIndex();

            if (spatialDistribution[iSpec] == SpatialDistribution.RANDOM) {
                sizeRandomMap[iSpec] = getConfiguration().getInt("movement.distribution.ncell.sp" + iSpec);
            } else {
                /*
                 * read age min and age max concerned by this map
                 */
                int ageMin = (int) Math.round(getConfiguration().getFloat("movement.map" + imap + ".age.min") * getConfiguration().getNStepYear());
                int ageMax = (int) Math.round(getConfiguration().getFloat("movement.map" + imap + ".age.max") * getConfiguration().getNStepYear());
                ageMax = Math.min(ageMax, getSpecies(iSpec).getLifespanDt());

                /*
                 * read the time steps over the year concerned by this map
                 */
                int[] mapSeason = getConfiguration().getArrayInt("movement.map" + imap + ".season");
                /*
                 * Read year min and max concerned by this map
                 */
                int yearMin = 0;
                int yearMax = getConfiguration().getNYear();
                if (!getConfiguration().isNull("movement.map" + imap + ".year.min")) {
                    yearMin = getConfiguration().getInt("movement.map" + imap + ".year.min");
                    yearMin = Math.max(yearMin, 0);
                }
                if (!getConfiguration().isNull("movement.map" + imap + ".year.max")) {
                    yearMax = getConfiguration().getInt("movement.map" + imap + ".year.max");
                    yearMax = Math.min(yearMax, getConfiguration().getNYear());
                }
                /*
                 * Assign number of maps to numMap array
                 */
                int nStepYear = getConfiguration().getNStepYear();
                for (int iAge = ageMin; iAge < ageMax; iAge++) {
                    for (int iYear = yearMin; iYear < yearMax; iYear++) {
                        for (int iStep : mapSeason) {
                            indexMaps[iSpec][iAge][iYear * nStepYear + iStep] = indexMap;
                        }
                    }
                }
                /*
                 * read the name of the CSV file and load the map if name = "null"
                 * it means the species is out of the simulated domain at these
                 * age-class and time-step
                 */
                if (getConfiguration().canFind("movement.map" + imap + ".file")) {
                    String csvFile = getConfiguration().getFile("movement.map" + imap + ".file");
                    mapFile[indexMap] = csvFile;
                    if (null != csvFile) {
                        readCSVMap(csvFile, indexMap);
                    } else {
                        maps[indexMap] = null;
                    }
                }
                if (spatialDistribution[iSpec] == SpatialDistribution.CONNECTIVITY) {
                    /*
                     * Read the name of the connectivity file and load the matrix If
                     * name = "null" it means the species is out of the simulated domain
                     * at these age-class and time-step or the map is not connected to any
                     * other one so there is no need for a connectivity matrix
                     */
                    if (!getConfiguration().isNull("movement.map" + imap + ".connectivity.file")) {
                        String csvFile = getConfiguration().getFile("movement.map" + imap + ".connectivity.file");
                        System.out.println("Reading connectivity matrix for " + getSpecies(iSpec).getName() + " map " + indexMap);
                        connectivityMatrix[indexMap] = new ConnectivityMatrix(indexMap, csvFile);
                        System.out.println("Connectivity matrix loaded");
                    }
                }
            }
            imap++;
        }
        checkMapIndexation();

        eliminateTwinMap();
    }

    private void checkMapIndexation() {

        int nSteps = getConfiguration().getNStepYear() * getConfiguration().getNYear();
        int nStepYear = getConfiguration().getNStepYear();
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            int lifespan = getSpecies(iSpec).getLifespanDt();
            for (int iAge = 0; iAge < lifespan; iAge++) {
                for (int iStep = 0; iStep < nSteps; iStep++) {
                    if (indexMaps[iSpec][iAge][iStep] < 0) {
                        float age = (float)iAge / nStepYear;
                        int year = iStep / nStepYear;
                        int step = iStep % nStepYear;
                        getLogger().log(Level.WARNING, "No map assigned for {0} age {1} year {2} step {3}", new Object[]{getSpecies(iSpec).getName(), age, year, step});
                    }
                }
            }
        }
    }

    private void readCSVMap(String csvFile, int indexMap) {

        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            /*
             * Get the number of cells for the map phv 28 march 2012 - it is a
             * loss of time to do twice the loop over the CSV file but no choice
             * at the moment without changing mapCoordi, mapCoordj
             */
            int nbCells = 0;
            for (String[] line : lines) {
                for (String str : line) {
                    //if (Integer.valueOf(str) > 0) {
                    if (Float.valueOf(str) > 0.f) {
                        nbCells++;
                    }
                }
            }
            /*
             * Initialize the arrays
             */
            maps[indexMap] = new GridMap();
            /*
             * Identify the coordinates of the cells and set the probability
             */
            float invNbCells = 1.f / nbCells;
            int ny = getGrid().get_ny();
            boolean error = false;
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val > 0.f) {
                        if (getGrid().getCell(i, j).isLand()) {
                            getLogger().log(Level.SEVERE, "Error loading map {0}. Found value > 0 in {1}", new Object[]{csvFile, getGrid().getCell(i, j).toString()});
                            error = true;
                        }
                        if (val < 1.f) {
                            /*
                             * value provided is directly a probability
                             */
                            maps[indexMap].setValue(i, j, val);
                        } else if (val == 1.f) {
                            /*
                             * map is presence/absence so equal probability of
                             * presence among cells
                             */
                            maps[indexMap].setValue(i, j, invNbCells);
                            /*
                             * else mapProbaPresence[indexMap][indexCell] = 0
                             * default value at initialization of the array
                             */
                        }
                    }
                }
            }
            if (error) {
                System.exit(1);
            }
            maxProbaPresence[indexMap] = computeMaxProbaPresence(indexMap);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading map " + csvFile, ex);
        }
        //System.out.println("Read CSV file " + csvFile + " [OK]");
    }

    boolean isOut(School school) {
        return (null == getMap(school));
    }

    public GridMap getMap(int numMap) {
        return maps[numMap];
    }

    GridMap getMap(School school) {
        return getMap(getIndexMap(school));
    }

    ConnectivityMatrix getMatrix(int numMap) {
        return connectivityMatrix[numMap];
    }

    SpatialDistribution getSpatialDistribution(int iSpec) {
        return spatialDistribution[iSpec];
    }

    int getIndexMap(int iSpec, int iAge, int iStep) {
        return indexMaps[iSpec][iAge][iStep];
    }

    int getIndexMap(School school) {
        return getIndexMap(school.getSpeciesIndex(), school.getAgeDt(), getSimulation().getIndexTimeSimu());
    }

    float getMaxProbaPresence(int numMap) {
        return maxProbaPresence[numMap];
    }

    int getSizeRandomMap(int iSpec) {
        return sizeRandomMap[iSpec];
    }

    private float computeMaxProbaPresence(int numMap) {
        float tempMaxProbaPresence = 0;
        GridMap map = getMap(numMap);
        for (int j = 0; j < getGrid().get_ny(); j++) {
            for (int i = 0; i < getGrid().get_nx(); i++) {
                tempMaxProbaPresence = Math.max(tempMaxProbaPresence, map.getValue(i, j));
            }
        }
        return tempMaxProbaPresence;
    }

    /**
     * This function eliminates twins in the list of maps of distribution
     */
    private void eliminateTwinMap() {
        mapIndexNoTwin = new int[mapFile.length];
        for (int k = 0; k < mapFile.length; k++) {
            String file = mapFile[k];
            mapIndexNoTwin[k] = k;
            if (null != file) {
                for (int l = k - 1; l >= 0; l--) {
                    if (file.equals(mapFile[l])) {
                        mapIndexNoTwin[k] = mapIndexNoTwin[l];
                        // Delete twin maps
                        maps[k] = null;
                        connectivityMatrix[k] = null;
                        break;
                    }
                }
            }
            //System.out.println("Map " + k + " has index " + mapIndexNoTwin[k] + " " + mapFile[k]);
        }

        for (int iSpec = 0; iSpec < indexMaps.length; iSpec++) {
            for (int iAge = 0; iAge < indexMaps[iSpec].length; iAge++) {
                for (int iStep = 0; iStep < indexMaps[iSpec][iAge].length; iStep++) {
                    int indexMap = indexMaps[iSpec][iAge][iStep];
                    indexMaps[iSpec][iAge][iStep] = mapIndexNoTwin[indexMap];
//                    if (iSpec == 3 && iStep < 24 && (iAge == 23)) {
//                        System.out.println("Species " + iSpec + " ageYear " + iAge + " iStep " + iStep + " numMap " + mapIndexNoTwin[indexMap]);
//                    }
                }
            }
        }
    }

    public String getMapDetails(int numMap) {
        StringBuilder str = new StringBuilder();
        str.append("Map: ");
        str.append(numMap);
//        str.append(" - Species: ");
//        str.append(speciesName[speciesMap[numMap]]);
//        str.append(" - Age class: ");
//        for (int k = 0; k < agesMap[numMap].length; k++) {
//            str.append(agesMap[numMap][k]);
//            str.append(" ");
//        }
//        str.append("- Time step: ");
//        for (int k = 0; k < seasonMap[numMap].length; k++) {
//            str.append(seasonMap[numMap][k]);
//            str.append(" ");
//        }
        return str.toString();
    }

    public enum SpatialDistribution {

        RANDOM,
        MAPS,
        CONNECTIVITY;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
