/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.process.mortality.fishery;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
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
public class FisheryMapSet extends OsmoseLinker {

    /**
     * Prefix of the series of maps in the configuration file. Parameter names
     * for maps will all be built the same way, for instance
     * '{$prefix}.map#.species' or '{$prefix}.map#.file', etc.
     */
    private final String prefix;

    /**
     * Name of the fishery.
     */
    private final String fisheryName;

    /**
     * Array of map indexes for every time step.
     * int[N_STEP_SIMU]
     */
    private int[] indexMaps;
    
    /**
     * List of the maps.
     */
    private GridMap[] maps;
    
    /**
     * List of the pathnames of the CSV files.
     */
    private String[] mapFile;

    public FisheryMapSet(String fisheryName, String prefix) {
        this.fisheryName = fisheryName;
        this.prefix = prefix;  // should be fishery.movement
    }
    
    public void init() {

        // Load the maps
        this.loadMaps();

        // Check the map indexation
        if (!this.checkMapIndexation()) {
            error("Missing map indexation for Fishery index " + fisheryName + " in map series '" + prefix + ".map*'. Please refer to prior warning messages for details.", null);
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

        Configuration cfg = this.getConfiguration();
        
        // Count the total number of fishery maps by looking for the
        // number of "fishery.map.index.map#" parameters 
        int nmapmax = getConfiguration().findKeys(prefix + ".fishery.map*").size();
  
        List<Integer> mapNumber = new ArrayList();
        int imap = 0;
        // Retrieve the index of the maps for this species
        for (int n = 0; n < nmapmax; n++) {

            // This is done if the indexing of fishering maps start with one for instance
            while (getConfiguration().isNull(prefix + ".fishery.map" + imap)) {
                imap++;
            }

            // Recovers the fisherie index associated with the current map.
            // If it matches the current fisherie, the map index is added to the list of
            // maps to be processed.
            String key = prefix + ".fishery.map" + imap;
            String fisheryName = getConfiguration().getString(key);

            if (fisheryName.equals(this.fisheryName)) {
                mapNumber.add(imap);
            }

            imap++;
        }  // end of nmapmax loop

        // Initialize NSTEP arrays of gridmaps, and initialize their index to -1
        maps = new GridMap[mapNumber.size()];
        mapFile = new String[mapNumber.size()];
        int nSteps = Math.max(getConfiguration().getNStep(), getConfiguration().getNStepYear());
        indexMaps = new int[nSteps];

        // Initialize the array of fisheries map index to -1 (i.e. no fishery)
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
            if (!getConfiguration().isNull(key)) {
                mapSeason = getConfiguration().getArrayInt(key);
            } else {
                mapSeason = new int[getConfiguration().getNStepYear()];
                for (int iStep = 0; iStep < getConfiguration().getNStepYear(); iStep++) {
                    mapSeason[iStep] = iStep;
                }
            }
            
            int[] listOfYears;
            
            key = String.format("%s.years.map%d", prefix, imap);
            if (!this.getConfiguration().isNull(key)) {
                listOfYears = cfg.getArrayInt(key);
            } else {
                int yearMin = 0;
                int nyear = (int) Math.ceil(getConfiguration().getNStep() / (float) getConfiguration().getNStepYear());
                int yearMax = nyear;
                if (!getConfiguration().isNull(prefix + ".initialYear.map" + imap)) {
                    yearMin = getConfiguration().getInt(prefix + ".initialYear.map" + imap);
                    yearMin = Math.max(yearMin, 0);
                }
                if (!getConfiguration().isNull(prefix + ".finalYear" + ".map" + imap)) {
                    yearMax = getConfiguration().getInt(prefix + ".finalYear.map" + imap);
                    yearMax = Math.min(yearMax, nyear);
                }

                int nyears = yearMax - yearMin;
                listOfYears = new int[nyears];
                int cpt = 0;
                for (int y = yearMin; y < yearMax; y++) {
                    listOfYears[cpt] = y;
                    cpt++;
                }
            }

            /*
             * Assign number of maps to numMap array.
             * indexMaps is the index of the map associated with the current fisherie
             * (i.e within all the maps with find of the current fisherie).
             */
            int nStepYear = getConfiguration().getNStepYear();

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
            if (!getConfiguration().isNull(prefix + ".file" + ".map" + imap)) {
                String csvFile = getConfiguration().getFile(prefix + ".file" + ".map" + imap);
                mapFile[n] = csvFile;
                maps[n] = new GridMap(csvFile);
            } else {
                mapFile[n] = null;
                maps[n] = null;
            }
            
        } // end of loop on good map numbers
    }  // end of method

    /**
     * Checks the map indexation. In the fishery,
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

}
