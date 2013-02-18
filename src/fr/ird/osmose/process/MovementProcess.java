package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import fr.ird.osmose.ConnectivityMatrix;
import fr.ird.osmose.GridMap;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.Osmose.SpatialDistribution;
import fr.ird.osmose.School;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static int[] range;
    /*
     * Migration
     */
    private static float[][][] outOfZoneMortality;
    private static boolean[][][] outOfZoneCohort;
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

    @Override
    public void init() {

        // read new format of area file, with agemin and agemax
        if (Osmose.NEW_AREA_FILE) {
            readConfigurationFile();
        }

        int nSpecies = getOsmose().getNumberSpecies();
        // init migration
        outOfZoneMortality = new float[nSpecies][][];
        outOfZoneCohort = new boolean[nSpecies][][];
        for (int index = 0; index < nSpecies; index++) {
            int longevity = getSpecies(index).getLongevity();
            outOfZoneMortality[index] = new float[longevity][getOsmose().getNumberTimeStepsPerYear()];
            outOfZoneCohort[index] = new boolean[longevity][getOsmose().getNumberTimeStepsPerYear()];
            if (null != getOsmose().migrationTempAge[index]) {
                int nbStepYear = getOsmose().getNumberTimeStepsPerYear();
                for (int m = 0; m < getOsmose().migrationTempAge[index].length; m++) {
                    for (int n = 0; n < getOsmose().migrationTempDt[index].length; n++) {
                        for (int h = 0; h < nbStepYear; h++) {
                            outOfZoneCohort[index][getOsmose().migrationTempAge[index][m] * nbStepYear + h][getOsmose().migrationTempDt[index][n]] = true;
                            outOfZoneMortality[index][getOsmose().migrationTempAge[index][m] * nbStepYear + h][getOsmose().migrationTempDt[index][n]] = getOsmose().migrationTempMortality[index][m];
                        }
                    }
                }
            }
        }
        // init distribution
        range = getOsmose().range;
        movements = new AbstractProcess[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            switch (getSpatialDistribution(i)) {
                case RANDOM:
                    movements[i] = new RandomDistributionProcess(getSimulation().getSpecies(i), this);
                    break;
                case MAPS:
                    movements[i] = new MapDistributionProcess(getSimulation().getSpecies(i), this);
                    break;
                case CONNECTIVITY:
                    movements[i] = new ConnectivityDistributionProcess(getSimulation().getSpecies(i), this);
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
        getPopulation().updateSchoolMap();
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
        if (map.getValue(cell) <= 0) {
            StringBuilder str = new StringBuilder("Inconsistency in moving ");
            str.append(school.toString());
            str.append("\n");
            str.append("It is not in the geographical area it is supposed to be...");
            System.out.println(str.toString());
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

    public static boolean isOut(School school) {
        return outOfZoneCohort[school.getSpeciesIndex()][school.getAgeDt()][getSimulation().getIndexTimeYear()];
    }

    public static float getOutMortality(School school) {
        return outOfZoneMortality[school.getSpeciesIndex()][school.getAgeDt()][getSimulation().getIndexTimeYear()];
    }

    private void readConfigurationFile() {

        /*
         * Open areas-maps configuration file
         */
        FileInputStream areasFile = null;
        try {
            areasFile = new FileInputStream(resolveFile(getOsmose().areasFileNameTab));
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening areasFile");
            System.exit(1);
        }
        /*
         * Initialize the reader
         */
        Reader r = new BufferedReader(new InputStreamReader(areasFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');
        int nSpecies = getOsmose().getNumberSpecies();
        spatialDistribution = new SpatialDistribution[nSpecies];
        sizeRandomMap = new int[nSpecies];

        try {

            for (int i = 0; i < nSpecies; i++) {
                st.nextToken();
                String areaDistribMethod = st.sval;
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
            st.nextToken();
            int nbMaps = new Integer(st.sval).intValue();
            maps = new GridMap[nbMaps];
            connectivityMatrix = new ConnectivityMatrix[nbMaps];
            maxProbaPresence = new float[nbMaps];
            mapFile = new String[nbMaps];
            mapIndexNoTwin = new int[nbMaps];
            indexMaps = new int[nSpecies][][];
            for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                int longevity = getSpecies(iSpec).getLongevity();
                indexMaps[iSpec] = new int[longevity][];
                for (int j = 0; j < longevity; j++) {
                    indexMaps[iSpec][j] = new int[getOsmose().getNumberTimeStepsPerYear()];
                }
            }

            /**
             * Loop over the maps
             */
            for (int indexMap = 0; indexMap < nbMaps; indexMap++) {
                /*
                 * read species number
                 */
                st.nextToken();
                int iSpec = new Integer(st.sval).intValue() - 1;
                if (spatialDistribution[iSpec] == SpatialDistribution.RANDOM) {
                    st.nextToken();
                    sizeRandomMap[iSpec] = (new Integer(st.sval)).intValue();
                } else {
                    /*
                     * read age min and age max concerned by this map
                     */
                    st.nextToken();
                    int ageMin = new Integer(st.sval).intValue() - 1;
                    st.nextToken();
                    int ageMax = new Integer(st.sval).intValue() - 1;
                    /*
                     * read number of time step over the year concerned by this map
                     */
                    st.nextToken();
                    int nbDtPerMap = new Integer(st.sval).intValue();
                    /*
                     * read the time steps over the year concerned by this map
                     */
                    int[] mapSeason = new int[nbDtPerMap];
                    for (int k = 0; k < nbDtPerMap; k++) {
                        st.nextToken();
                        mapSeason[k] = new Integer(st.sval).intValue() - 1;
                    }
                    /*
                     * Assign number of maps to numMap array
                     */
                    for (int iAge = ageMin; iAge <= ageMax; iAge++) {
                        for (int iStep : mapSeason) {
                            indexMaps[iSpec][iAge][iStep] = indexMap;
                        }
                    }
                    /*
                     * read the name of the CSV file and load the map if name = "null"
                     * it means the species is out of the simulated domain at these
                     * age-class and time-step
                     */
                    st.nextToken();
                    if (!"null".equals(st.sval)) {
                        String csvFile = resolveFile(st.sval);
                        mapFile[indexMap] = st.sval;
                        readCSVMap(csvFile, indexMap);
                    }
                    if (spatialDistribution[iSpec] == SpatialDistribution.CONNECTIVITY) {
                        /*
                         * Read the name of the connectivity file and load the matrix If
                         * name = "null" it means the species is out of the simulated domain
                         * at these age-class and time-step or the map is not connected to any
                         * other one so there is no need for a connectivity matrix
                         */
                        st.nextToken();
                        if (!"null".equals(st.sval)) {
                            System.out.println("Reading connectivity matrix for " + getSpecies(iSpec).getName() + " map " + indexMap);
                            String csvFile = resolveFile(st.sval);
                            connectivityMatrix[indexMap] = new ConnectivityMatrix(indexMap, csvFile);
                            System.out.println("Connectivity matrix loaded");
                        }
                    }
                }
            }


        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                r.close();
            } catch (IOException ex) {
                Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        eliminateTwinMap();
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
            int indexCell = 0;
            float invNbCells = 1.f / nbCells;
            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i);
                for (int j = 0; j < line.length; j++) {
                    float val = Float.valueOf(line[j]);
                    if (val > 0.f) {
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
                        indexCell++;
                    }
                }
            }
            maxProbaPresence[indexMap] = computeMaxProbaPresence(indexMap);
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("Read CSV file " + csvFile + " [OK]");
    }

    GridMap getMap(int numMap) {
        return (null == maps)
                ? getOsmose().maps[numMap]
                : maps[numMap];
    }

    GridMap getMap(School school) {
        return getMap(getIndexMap(school));
    }

    ConnectivityMatrix getMatrix(int numMap) {
        return (null == connectivityMatrix)
                ? getOsmose().connectivityMatrix[numMap]
                : connectivityMatrix[numMap];
    }

    SpatialDistribution getSpatialDistribution(int iSpec) {
        return (null == spatialDistribution)
                ? getOsmose().spatialDistribution[iSpec]
                : spatialDistribution[iSpec];
    }

    int getIndexMap(int iSpec, int iAge, int iStep) {
        return (null == indexMaps)
                ? getOsmose().numMap[iSpec][iAge][iStep]
                : indexMaps[iSpec][iAge][iStep];
    }

    int getIndexMap(School school) {
        return getIndexMap(school.getSpeciesIndex(), school.getAgeDt(), getSimulation().getIndexTimeYear());
    }

    float getMaxProbaPresence(int numMap) {
        return (null == maxProbaPresence)
                ? getOsmose().maxProbaPresence[numMap]
                : maxProbaPresence[numMap];
    }

    int getSizeRandomMap(int iSpec) {
        return (null == sizeRandomMap)
                ? getOsmose().speciesAreasSizeTab[iSpec]
                : sizeRandomMap[iSpec];
    }

    private float computeMaxProbaPresence(int numMap) {
        float tempMaxProbaPresence = 0;
        GridMap map = getMap(numMap);
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
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
            for (int l = k - 1; l >= 0; l--) {
                if (file.matches(mapFile[l])) {
                    mapIndexNoTwin[k] = mapIndexNoTwin[l];
                    // Delete twin maps
                    maps[k] = null;
                    connectivityMatrix[k] = null;
                    break;
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
}
