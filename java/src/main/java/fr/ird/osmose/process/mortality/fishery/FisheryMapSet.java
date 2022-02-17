/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */
package fr.ird.osmose.process.mortality.fishery;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.YearParameters;
import fr.ird.osmose.util.io.ForcingFile;
import fr.ird.osmose.util.io.ForcingFileCaching;
import ucar.ma2.InvalidRangeException;
import fr.ird.osmose.util.StepParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
public class FisheryMapSet extends OsmoseLinker {

    /**
     * Prefix of the series of maps in the configuration file. Parameter names
     * for maps will all be built the same way, for instance
     * '{$prefix}.map#.species' or '{$prefix}.map#.file', etc.
     */
    private final String prefix;

    private final String suffix;

    /**
     * Name of the fishery.
     */
    private final String fisheryName;

    /**
     * Array of map indexes for every time step. int[N_STEP_SIMU]
     */
    private int[] indexMaps;
    
    /**
     * List of the maps.
     */
    protected HashMap<Integer, GridMap> maps;

    /**
     * List of the pathnames of the CSV files.
     */
    private String[] mapFile;

    private final boolean removeDuplicate;
    
    public FisheryMapSet(String fisheryName, String prefix, String suffix, boolean removeDuplicate) {
        this.fisheryName = fisheryName;
        this.prefix = prefix;  // should be fishery.movement
        this.suffix = suffix;
        this.removeDuplicate = removeDuplicate;
    }
    
    public FisheryMapSet(String fisheryName, String prefix, String suffix) {
        this(fisheryName, prefix, suffix, true);
    }

    public void init() {

        // Load the maps
        String key = prefix + ".netcdf.enabled";
        if (!getConfiguration().getBoolean(key)) {
            this.loadMaps();

            // Check the map indexation
            if (!this.checkMapIndexation()) {
                error("Missing map indexation for Fishery index " + fisheryName + " in map series '" + prefix
                        + ".map*'. Please refer to prior warning messages for details.", null);
            }

            // Get rid of redundant map definitions
            if(this.removeDuplicate) { 
                this.eliminateTwinMap();
            }

        } else {
            try {
                this.loadMapsNcMaps();
            } catch (IOException | InvalidRangeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }   
        }

        // Normalize all maps
        this.normalizeAllMaps();

    }

    public int getNMap() {
        return maps.size();
    }

    public GridMap getMap(int numMap) {
        return maps.get(numMap);
    }

    public String getMapFile(int numMap) {
        return mapFile[numMap];
    }

    public int getIndexMap(int iStepSimu) {
        return indexMaps[iStepSimu];
    }

    public void loadMaps() {

        Configuration cfg = this.getConfiguration();

        // Count the total number of fishery maps by looking for the
        // number of "fisheries.movement.fiishery.map#" parameters 
        String key;

        key = String.format("%s.%s.map*", prefix, suffix);
        int nmapmax = cfg.findKeys(key).size();

        List<Integer> mapNumber = new ArrayList<>();
        int imap = 0;
        // Retrieve the index of the maps for this species
        for (int n = 0; n < nmapmax; n++) {

            key = String.format("%s.%s.map%d", prefix, suffix, imap);

// This is done if the indexing of fishering maps start with one for instance
            while (cfg.isNull(key)) {
                imap++;
                key = String.format("%s.%s.map%d", prefix, suffix, imap);
            }

            // Recovers the fisherie index associated with the current map.
            // If it matches the current fisherie, the map index is added to the list of
            // maps to be processed.
            String name = cfg.getString(key);

            if (name.equals(this.fisheryName)) {
                mapNumber.add(imap);
            }

            imap++;
        }  // end of nmapmax loop

        // Initialize NSTEP arrays of gridmaps, and initialize their index to -1
        maps = new HashMap<>();
        mapFile = new String[mapNumber.size()];
        int nSteps = Math.max(cfg.getNStep(), cfg.getNStepYear());
        indexMaps = new int[nSteps];

        // Initialize the array of fisheries map index to -1 (i.e. no fishery)
        for (int iStep = 0; iStep < nSteps; iStep++) {
            indexMaps[iStep] = -1;
        }

        // Load the maps to be loaded for the current fisherie
        for (int n = 0; n < mapNumber.size(); n++) {

            // Recovers the index of the map.
            imap = mapNumber.get(n);

            StepParameters seasonParam = new StepParameters(prefix, "map" + imap);
            int[] mapSeason = seasonParam.getSeasons();

            YearParameters yearParam = new YearParameters(prefix, "map" + imap);
            int[] listOfYears = yearParam.getYears();

            /*
             * Assign number of maps to numMap array.
             * indexMaps is the index of the map associated with the current fisherie
             * (i.e within all the maps with find of the current fisherie).
             */
            int nStepYear = cfg.getNStepYear();

            for (int iYear : listOfYears) {
                for (int iSeason : mapSeason) {
                    int iStep = iYear * nStepYear + iSeason;
                    if (iStep < indexMaps.length) {
                        indexMaps[iStep] = n;   // can be viewed as an array of size [year][ndt]
                    } else {
                        String msg = String.format("iStep=%d exceeed the given time period, %d", iStep, indexMaps.length);
                        throw new ArrayIndexOutOfBoundsException(msg);
                    }
                }
            }

            /*
             * read the name of the CSV file and load the map. If name = "null"
             * it means there is no map defined at these age-class and time-step
             */
            if (!cfg.isNull(prefix + ".file" + ".map" + imap)) {
                String csvFile = cfg.getFile(prefix + ".file" + ".map" + imap);
                mapFile[n] = csvFile;
                maps.put(n, new GridMap(csvFile));
            } else {
                mapFile[n] = null;
                maps.put(n, null);
            }

        } // end of loop on good map numbers
    }  // end of method

    /**
     * Checks the map indexation. In the fishery, if a map is not provided for
     * the current specie, it is assumed that fishing mortality is null.
     *
     * @return True or False
     */
    private boolean checkMapIndexation() {
        Configuration cfg = this.getConfiguration();
        int nSteps = cfg.getNStep();
        int nStepYear = cfg.getNStepYear();

        for (int iStep = 0; iStep < nSteps; iStep++) {
            if (indexMaps[iStep] < 0) {
                int year = iStep / nStepYear;
                int step = iStep % nStepYear;
                warning("No map assigned for fishery {0} year {1} step {2}", new Object[]{this.fisheryName, year, step});
                warning("Fishery {0} will therefore be deactivated during year {1} and step {2}", new Object[]{this.fisheryName, year, step});
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
                        maps.put(k, null);
                        break;
                    }
                }
            }
        }

        // Loop over all the time steps
        for (int iStep = 0; iStep < indexMaps.length; iStep++) {
            int indexMap = indexMaps[iStep];   // recover the old map index
            if (indexMap >= 0) {
                indexMaps[iStep] = mapIndexNoTwin[indexMap];    // replace it by the new one.
            }
        }
    }

    /**
     * Normalize the map values for each map. The sum of coeff * surf = 1
     */
    private void normalizeAllMaps() {
        // Loop over all the map files files (looping over map indexes).
        for (GridMap map : maps.values()) {
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
                mean += map.getValue(cell) * cell.getSurface();
                surftot += cell.getSurface();
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

    public double getValue(int idt, Cell cell) {

        int indexMap = this.getIndexMap(idt);
        if (indexMap < 0) {
            return 0;
        }

        GridMap map = this.getMap(indexMap);
        double spatialSelect;
        if (map != null) {
            spatialSelect = Math.max(0, map.getValue(cell));  // this is done because if value is -999, then no fishery is applied here.
        } else {
            spatialSelect = 0.0;
        }
        return spatialSelect;
    }

    
    /**
     * Method to initialize MapSets from NetCDF file.
     *
     * @author Nicolas Barrier
     * @throws java.io.IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    public void loadMapsNcMaps() throws IOException, InvalidRangeException {
        
        // Load config + nstepyear + nsteps
        Configuration cfg = getConfiguration();

        // Load the file prefix with names that are of type. movemement.map.species.mapX
        String key = String.format("%s.%s.map*", prefix, suffix);
        int nmapmax = getConfiguration().findKeys(key).size();
    
        List<Integer> mapNumber = new ArrayList<>();
        int imap = 0;
        // Retrieve the index of the maps for this species
        for (int n = 0; n < nmapmax; n++) {
            while (!getConfiguration().canFind(String.format("%s.%s.map%d", prefix, suffix, imap))) {
                imap++;
            }
            key = String.format("%s.%s.map%d", prefix, suffix, imap);
            String name = getConfiguration().getString(key);
            if (name.equals(fisheryName)) {
                mapNumber.add(imap);
            }
            imap++;
        }
        
        if(mapNumber.size() != 1) {
            getLogger().warning("One map should be provided for fishery");   
        }
    
        // Recover the first map number.
        imap = mapNumber.get(0);
        
        String ncFilePattern = cfg.getFile(prefix + ".file.map" + imap);
        String varName = cfg.getString(prefix + ".variable.map" + imap);
        int ncPerYear = cfg.getInt(prefix + ".steps.year.map" + imap);
      
        // Init a NetCDF file containing movements NetCDF forcings.
        ForcingFile forcingFile = new ForcingFile(varName, ncFilePattern, ncPerYear, 0, 1, ForcingFileCaching.ALL);
        forcingFile.init();
        
        // Define the indexMaps based on NetCDF index
        for(int iStep = 0; iStep < getConfiguration().getNStep(); iStep++) {
            int iStepNc = forcingFile.getNcStep(iStep);
            indexMaps[iStep] = iStepNc;            
        }
    
        // One map per timestep
        maps = new HashMap<>(); // dimension = [ntimeNc]
        HashMap<Integer, double[][][]> forcingValues = forcingFile.getCachedVariable();
        for(int i : forcingValues.keySet()) { 
           double[][] values = forcingValues.get(i)[0];
           maps.put(i, new GridMap(values));    
        }

    }  // end of method

    public int[] getIndexMap() { 
        return this.indexMaps;
    }
    
}
