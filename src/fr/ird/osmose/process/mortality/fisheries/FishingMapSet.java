package fr.ird.osmose.process.mortality.fisheries;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.OsmoseLinker;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles a set of spatial maps for a given species. No matter the
 * process, spatial maps should always be inputed the same way. Here is the list
 * of parameters that the class will look for:
 * <ul>
 * <li>'{$prefix}.map#.species', the name of the species that must exactly match
 * predefined species name by parameters 'species.name.sp#'
 * <li>'{$prefix}.map#.season', a vector of time steps (within one year)
 * <li>'{$prefix}.map#.year.min', inclusive start year for this map. Zero by
 * default.
 * <li>'{$prefix}.map#.year.max', exclusive end year for this map. Number of
 * simulated year by default.
 * <li>'{$prefix}.map#.age.min', inclusive age min (in year) of the schools for
 * this map. Zero by default.
 * <li>'{$prefix}.map#.age.max', exclusive age max (in year) of the schools for
 * this map. Lifespan by default.
 * <li>'{$prefix}.map#.file', absolute or relative pathname of the CSV file.
 * </ul>
 * with {$prefix} any sequence of alpha-numeric characters (no blank or any
 * special characters allowed) and '#' standing for an integer, the index of the
 * map. Map index must be unique in the configuration for a given prefix. It
 * does not have to be defined sequentially (map0, map1, map2, map3, map4, map5,
 * etc.). For instance such indexation will be perfectly fine: map100, map101,
 * map102, map200, map201, map202, map300, etc.
 *
 *
 * @author Nicolas Barrier (nicolas.barrier@ird.fr)
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class FishingMapSet extends OsmoseLinker {

    /**
     * Prefix of the series of maps in the configuration file. Parameter names
     * for maps will all be built the same way, for instance
     * '{$prefix}.map#.species' or '{$prefix}.map#.file', etc.
     */
    private final String prefix;

    /**
     * Index of the species.
     */
    private final int iFisheries;

    /**
     * Array of map indexes for every age class and simulation time step.
     * int[N_STEP_SIMU]
     */
    private int[] indexMaps;
    /**
     * List of the maps.
     */
    private FisheriesGridMap[] maps;
    /**
     * List of the pathnames of the CSV files.
     */
    private String[] mapFile;

    public FishingMapSet(int iFisheries, String prefix) {
        this.iFisheries = iFisheries;
        this.prefix = prefix;  // should be fishery.movement
    }

    public void init() {

        // Load the maps
        this.loadMaps();

        // Check the map indexation
        if (!this.checkMapIndexation()) {
            error("Missing map indexation for Fisheries index " + iFisheries + " in map series '" + prefix + ".map*'. Please refer to prior warning messages for details.", null);
        }

        // Get rid of redundant map definitions
        this.eliminateTwinMap();

        // Normalize all maps
        this.normalize_all_maps();

    }

    public int getNMap() {
        return maps.length;
    }

    public GridMap getMap(int numMap) {
        return maps[numMap];
    }

    public String getMapFile(int numMap) {
        return mapFile[numMap];
    }

    public int getIndexMap(int iStepSimu) {
        return indexMaps[iStepSimu];
    }

    public void loadMaps() {

        // Count the total number of fisheries map by looking for the
        // number of "fishery.map.index.map#" parameters 
        int nmapmax = getConfiguration().findKeys(prefix + ".index.map*").size();
  
        List<Integer> mapNumber = new ArrayList();
        int imap = 0;
        // Retrieve the index of the maps for this species
        for (int n = 0; n < nmapmax; n++) {

            // This is done if the indexing of fishering maps start with one for instance
            while (!getConfiguration().canFind(prefix + ".index.map" + imap)) {
                imap++;
            }

            // Recovers the fisherie index associated with the current map.
            // If it matches the current fisherie, the map index is added to the list of
            // maps to be processed.
            String key = prefix + ".index.map" + imap;
            int fisheriesIndex = getConfiguration().getInt(key);

            if (fisheriesIndex == iFisheries) {
                mapNumber.add(imap);
            }

            imap++;
        }  // end of nmapmax loop

        // Initialize NSTEP arrays of gridmaps, and initialize their index to -1
        maps = new FisheriesGridMap[mapNumber.size()];
        mapFile = new String[mapNumber.size()];
        int nSteps = Math.max(getConfiguration().getNStep(), getConfiguration().getNStepYear());
        indexMaps = new int[nSteps];

        for (int iStep = 0; iStep < nSteps; iStep++) {
            indexMaps[iStep] = -1;
        }

        // Load the maps to be loaded for the current fisherie
        for (int n = 0; n < mapNumber.size(); n++) {

            // Recovers the index of the map.
            imap = mapNumber.get(n);

            /*
             * read the time steps over the year concerned by this map
             */
            int [] mapSeason;
            String key = prefix + ".season" + ".map" + imap;
            if (getConfiguration().canFind(key)) {
                mapSeason = getConfiguration().getArrayInt(key);
            } else {
                mapSeason = new int[getConfiguration().getNStepYear()];
                for (int iStep = 0; iStep < getConfiguration().getNStepYear(); iStep++) {
                    mapSeason[iStep] = iStep;
                }
            }
            /*
             * Read year min and max concerned by this map
             */
            int yearMin = 0;
            int nyear = (int) Math.ceil(getConfiguration().getNStep() / (float) getConfiguration().getNStepYear());
            int yearMax = nyear;
            if (!getConfiguration().isNull(prefix + ".year.min" + ".map" + imap)) {
                yearMin = getConfiguration().getInt(prefix + ".year.min" + ".map" + imap);
                yearMin = Math.max(yearMin, 0);
            }
            if (!getConfiguration().isNull(prefix + ".year.max" + ".map" + imap)) {
                yearMax = getConfiguration().getInt(prefix + ".year.max" + ".map" + imap);
                yearMax = Math.min(yearMax, nyear);
            }

            /*
             * Assign number of maps to numMap array.
             * indexMaps is the index of the map associated with the current fisherie
             * (i.e within all the maps with find of the current fisherie).
             */
            int nStepYear = getConfiguration().getNStepYear();

            for (int iYear = yearMin; iYear < yearMax; iYear++) {
                for (int iSeason : mapSeason) {
                    int iStep = iYear * nStepYear + iSeason;
                    if (iStep < indexMaps.length) {
                        indexMaps[iStep] = n;   // can be viewed as an array of size [year][ndt]
                    } else {
                        break;
                    }
                }
            }

            /*
             * read the name of the CSV file and load the map. If name = "null"
             * it means there is no map defined at these age-class and time-step
             */
            if (!getConfiguration().isNull(prefix + ".file" + ".map" + imap)) {
                String csvFile = getConfiguration().getFile(prefix + ".file" + ".map" + imap);
                mapFile[n] = csvFile;
                maps[n] = new FisheriesGridMap(csvFile);
            } else {
                maps[n] = null;
            }
        }
    }

    /**
     * Checks the map indexation. In the fisheries,
     * if a map is not provided for the current specie, it
     * is assumed that fishing mortality is null.
     *
     * @return True or False
     */
    private boolean checkMapIndexation() {

        int nSteps = getConfiguration().getNStep();
        int nStepYear = getConfiguration().getNStepYear();

        for (int iStep = 0; iStep < nSteps; iStep++) {
            if (indexMaps[iStep] < 0) {
                int year = iStep / nStepYear;
                int step = iStep % nStepYear;
                warning("No map assigned for fisheries {0} year {1} step {2}", new Object[]{iFisheries, year, step});
                warning("Fisheries {0} will therefore be deactivated during year {1} and step {2}", new Object[]{iFisheries, year, step});
            }
        }

        return true;
    }

    /**
     * This function eliminates twins in the list of maps. Redundant maps are
     * set equal to "null" in the maps array.
     */
    private void eliminateTwinMap() {

        int[] mapIndexNoTwin = new int[mapFile.length];

        // Loop over all the map files files (looping over map indexes).
        for (int k = 0; k < mapFile.length; k++) {
            String file = mapFile[k];
            mapIndexNoTwin[k] = k;

            // if the file is defined
            if (null != file) {
                for (int l = k - 1; l >= 0; l--) {   // loop over all the maps from index 0 to k
                    if (file.equals(mapFile[l])) {   // if the file of map k matches the map of map l.
                        mapIndexNoTwin[k] = mapIndexNoTwin[l];   // the k map is removed and the twin index is set equal to the twin map of l
                        // Delete twin maps
                        maps[k] = null;
                        break;
                    }
                }
            }
        }

        // Loop over all the time steps
        for (int iStep = 0; iStep < indexMaps.length; iStep++) {
            int indexMap = indexMaps[iStep];   // recover the old map index
            if(indexMap >=0) indexMaps[iStep] = mapIndexNoTwin[indexMap];    // replace it by the new one.
        }
    }

    /**
     * Normalize the map values for each map. The sum of coeff * surf = 1
     */
    private void normalize_all_maps() {
        // Loop over all the map files files (looping over map indexes).
        for (GridMap map : maps) {
            normalize_map(map);
        }
    }

    /**
     * Normalize the map values. The surface weighted mean of the coefficients
     * must match 1: sum(coeff * surf) / sum(surf) = 1, with coeff the final
     * coefficients. If coefo = old coefficients, let's define K = sum(coefo *
     * surf) / sum(surf): the weighted mean of the old coefficients
     *
     * If coeff = coefo / K Sum (coeff * surf) / sum(surf) = 1/K * sum(coefo *
     * surf) / sum(surf) = 1/K * K = 1.
     *
     * @param map
     */
    private void normalize_map(GridMap map) {
        
        // If the map is null, nothing is done.
        if (map == null) {
            return;
        }

        // Mean value
        float mean = 0;
        float surftot = 0;

        // Loop over all the cells of the current grid.
        for (Cell cell : getGrid().getCells()) {
            // If the cell is on water and if the value is ok, then 
            // the mean and surftot are updated.

            if ((!cell.isLand()) && (isValueOk(map.getValue(cell)))) {
                mean += map.getValue(cell) * getSurface(cell);
                surftot += getSurface(cell);
            }
        }
      
        float K = mean / surftot;
        
        // Loop over all the cells of the current grid
        // and corrects the fishing coefficients.
        for (Cell cell : getGrid().getCells()) {
            if ((!cell.isLand()) && (isValueOk(map.getValue(cell)))) {
                map.setValue(cell, map.getValue(cell) / K);
            }
        }

        // Checks that the final map value has a weighted mean of 1.
        // Loop over all the cells of the current grid
        // and corrects the fishing coefficients.
        float temp = 0;
        for (Cell cell : getGrid().getCells()) {
            if ((!cell.isLand()) && (isValueOk(map.getValue(cell)))) {
                temp += map.getValue(cell) * getSurface(cell);
            }
        }

        temp /= surftot;
               
        if (temp != 1) {
            warning("The temporal mean of the space factors is different from 1.0");
            warning("iFleet = " + this.iFisheries + ", mean = " + temp);
        }

    }

    /**
     * Return True if the value is different from 0 or NaN.
     *
     * @param value
     * @return
     */
    private boolean isValueOk(float value) {
        return (value >= 0) || (!Float.isNaN(value));
    }

    /**
     * Gets the surface of a given cell.
     *
     * @param cell
     * @return
     */
    private double getSurface(Cell cell) {
        float lat = cell.getLat();
        float dlat = getGrid().getdLat();
        float dlon = getGrid().getdLong();

        // Earth radius in m
        double Rt = 6371 * 1e3;

        double surf = Rt * deg2rad(dlat) * Rt * deg2rad(dlon) * Math.cos(deg2rad(lat));

        return surf;

    }

    /**
     * Converts a degree angle into radian.
     *
     * @param value
     * @return
     */
    private double deg2rad(double value) {
        return value * Math.PI / 180.;
    }

    /**
     * Converts a radian angle into degree.
     *
     * @param value
     * @return
     */
    private double rad2deg(double value) {
        return value * 180. / Math.PI;
    }

}
