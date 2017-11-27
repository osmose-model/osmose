/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
package fr.ird.osmose.util;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.IOException;
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
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class MapSet extends OsmoseLinker {

    /**
     * Prefix of the series of maps in the configuration file. Parameter names
     * for maps will all be built the same way, for instance
     * '{$prefix}.map#.species' or '{$prefix}.map#.file', etc.
     */
    private final String prefix;
    /**
     * Index of the species.
     */
    private final int iSpecies;
    /**
     * Array of map indexes for every age class and simulation time step.
     * int[N_AGE_CLASSES][N_STEP_SIMU]
     */
    private int[][] indexMaps;
    /**
     * List of the maps.
     */
    private GridMap[] maps;
    /**
     * List of the pathnames of the CSV files.
     */
    private String[] mapFile;

    public MapSet(int iSpecies, String prefix) {
        this.iSpecies = iSpecies;
        this.prefix = prefix;
    }

    public void init() {

        // Load the maps
        loadMaps();

        // Check the map indexation
        if (!checkMapIndexation()) {
            error("Missing map indexation for species " + getSpecies(iSpecies).getName() + " in map series '" + prefix + ".map*'. Please refer to prior warning messages for details.", null);
        }

        // Get rid of redundant map definitions
        eliminateTwinMap();
    }

    public int getNMap() {
        return maps.length;
    }

    public GridMap getMap(int numMap) {
        return maps[numMap];
    }

    public GridMap getMap(School school, int iStepSimu) {
        return getMap(getIndexMap(school.getAgeDt(), iStepSimu));
    }

    public String getMapFile(int numMap) {
        return mapFile[numMap];
    }

    public int getIndexMap(int iAge, int iStepSimu) {
        return indexMaps[iAge][iStepSimu];
    }

    public void loadMaps() {

        int nmapmax = getConfiguration().findKeys(prefix + ".map*.species").size();
        List<Integer> mapNumber = new ArrayList();
        int imap = 0;
        // Retrieve the index of the maps for this species
        for (int n = 0; n < nmapmax; n++) {
            while (!getConfiguration().canFind(prefix + ".map" + imap + ".species")) {
                imap++;
            }
            String key = prefix + ".map" + imap + ".species";
            Species species = getSpecies(getConfiguration().getString(key));
            if (null != species) {
                if (species.getIndex() == iSpecies) {
                    mapNumber.add(imap);
                }
            } else {
                error("Wrong species name in spatial map series '" + prefix + ".map*'", new IOException("Parameter " + key + " = " + getConfiguration().getString(key) + " does not match any predefined species name."));
            }
            imap++;
        }

        // Initialize arrays
        maps = new GridMap[mapNumber.size()];
        mapFile = new String[mapNumber.size()];
        int nSteps = Math.max(getConfiguration().getNStep(), getConfiguration().getNStepYear());
        int lifespan = getSpecies(iSpecies).getLifespanDt();
        indexMaps = new int[lifespan][];
        for (int iAge = 0; iAge < lifespan; iAge++) {
            indexMaps[iAge] = new int[nSteps];
            for (int iStep = 0; iStep < nSteps; iStep++) {
                indexMaps[iAge][iStep] = -1;
            }
        }

        // Load the maps
        for (int n = 0; n < mapNumber.size(); n++) {
            imap = mapNumber.get(n);
            /*
             * read age min and age max concerned by this map
             */
            int ageMin = (int) Math.round(getConfiguration().getFloat(prefix + ".map" + imap + ".age.min") * getConfiguration().getNStepYear());
            int ageMax = (int) Math.round(getConfiguration().getFloat(prefix + ".map" + imap + ".age.max") * getConfiguration().getNStepYear());
            ageMax = Math.min(ageMax, getSpecies(iSpecies).getLifespanDt());

            /*
             * read the time steps over the year concerned by this map
             */
            int[] mapSeason = getConfiguration().getArrayInt(prefix + ".map" + imap + ".season");
            /*
             * Read year min and max concerned by this map
             */
            int yearMin = 0;
            int nyear = (int) Math.ceil(getConfiguration().getNStep() / (float) getConfiguration().getNStepYear());
            int yearMax = nyear;
            if (!getConfiguration().isNull(prefix + ".map" + imap + ".year.min")) {
                yearMin = getConfiguration().getInt(prefix + ".map" + imap + ".year.min");
                yearMin = Math.max(yearMin, 0);
            }
            if (!getConfiguration().isNull(prefix + ".map" + imap + ".year.max")) {
                yearMax = getConfiguration().getInt(prefix + ".map" + imap + ".year.max");
                yearMax = Math.min(yearMax, nyear);
            }
            /*
             * Assign number of maps to numMap array
             */
            int nStepYear = getConfiguration().getNStepYear();
            for (int iAge = ageMin; iAge < ageMax; iAge++) {
                for (int iYear = yearMin; iYear < yearMax; iYear++) {
                    for (int iSeason : mapSeason) {
                        int iStep = iYear * nStepYear + iSeason;
                        if (iStep < indexMaps[iAge].length) {
                            indexMaps[iAge][iYear * nStepYear + iSeason] = n;
                        } else {
                            break;
                        }
                    }
                }
            }
            /*
             * read the name of the CSV file and load the map. If name = "null"
             * it means there is no map defined at these age-class and time-step
             */
            if (!getConfiguration().isNull(prefix + ".map" + imap + ".file")) {
                String csvFile = getConfiguration().getFile(prefix + ".map" + imap + ".file");
                mapFile[n] = csvFile;
                maps[n] = new GridMap(csvFile);
            } else {
                maps[n] = null;
            }
        }
    }

    private boolean checkMapIndexation() {

        boolean isMapOK = true;
        int nSteps = getConfiguration().getNStep();
        int nStepYear = getConfiguration().getNStepYear();
        int lifespan = getSpecies(iSpecies).getLifespanDt();
        for (int iAge = 0; iAge < lifespan; iAge++) {
            for (int iStep = 0; iStep < nSteps; iStep++) {
                if (indexMaps[iAge][iStep] < 0) {
                    isMapOK = false;
                    float age = (float) iAge / nStepYear;
                    int year = iStep / nStepYear;
                    int step = iStep % nStepYear;
                    warning("No map assigned for {0} age {1} year {2} step {3}", new Object[]{getSpecies(iSpecies).getName(), age, year, step});
                }
            }
        }

        return isMapOK;
    }

    /**
     * This function eliminates twins in the list of maps
     */
    private void eliminateTwinMap() {

        int[] mapIndexNoTwin = new int[mapFile.length];
        for (int k = 0; k < mapFile.length; k++) {
            String file = mapFile[k];
            mapIndexNoTwin[k] = k;
            if (null != file) {
                for (int l = k - 1; l >= 0; l--) {
                    if (file.equals(mapFile[l])) {
                        mapIndexNoTwin[k] = mapIndexNoTwin[l];
                        // Delete twin maps
                        maps[k] = null;
                        break;
                    }
                }
            }
        }

        for (int iAge = 0; iAge < indexMaps.length; iAge++) {
            for (int iStep = 0; iStep < indexMaps[iAge].length; iStep++) {
                int indexMap = indexMaps[iAge][iStep];
                indexMaps[iAge][iStep] = mapIndexNoTwin[indexMap];
            }
        }

    }
}
