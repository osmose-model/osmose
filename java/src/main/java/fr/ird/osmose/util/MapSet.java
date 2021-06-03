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

package fr.ird.osmose.util;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

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
    protected final String prefix;

    /**
     * Sufix of the maps. Can be "sp" or "bkg".
     */
    protected final String suffix;

    /**
     * Index of the species.
     */
    protected final int iSpecies;
    protected final int iSpeciesFile;
    
    /**
     * Array of map indexes for every age class and simulation time step.
     * int[N_AGE_CLASSES][N_STEP_SIMU]
     */
    protected int[][] indexMaps;
    
    /**
     * List of the maps.
     */
    protected GridMap[] maps;
    
    /**
     * List of the pathnames of the CSV files.
     */
    protected String[] mapFile;

    public MapSet(int iSpeciesFile, int iSpecies, String prefix) {
        this(iSpeciesFile, iSpecies, prefix, "sp");
    }

    public MapSet(int iSpeciesFile, int iSpecies, String prefix, String suffix) {
        this.iSpeciesFile = iSpeciesFile;
        this.iSpecies = iSpecies;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public void init() throws IOException, InvalidRangeException {

        // Load the maps
        loadMaps();

    }

    public int getNMap() {
        return maps.length;
    }

    public GridMap getMap(int numMap) {
        return maps[numMap];
    }

    public GridMap[] getMaps() {
        return maps;
    }

    public GridMap getMap(School school, int iStepSimu) {
        return getMap(getIndexMap(school.getAgeDt(), iStepSimu));
    }

    /**
     * Recovers the map from the class index instead of age value.
     *
     * @param classIndex
     * @param iStepSimu
     * @return
     */
    public GridMap getMap(int classIndex, int iStepSimu) {
        return getMap(getIndexMap(classIndex, iStepSimu));
    }

    public String getMapFile(int numMap) {
        return mapFile[numMap];
    }

    public int getIndexMap(int iAge, int iStepSimu) {
        return indexMaps[iAge][iStepSimu];
    }

    public void loadMaps() throws IOException, InvalidRangeException {

        // Initialisation of the indexMaps array (valid for both NetCDF and CSV maps
        int nSteps = Math.max(getConfiguration().getNStep(), getConfiguration().getNStepYear());
        int lifespan = getSpecies(iSpecies).getLifespanDt();
        indexMaps = new int[lifespan][];
        for (int iAge = 0; iAge < lifespan; iAge++) {
            indexMaps[iAge] = new int[nSteps];
            for (int iStep = 0; iStep < nSteps; iStep++) {
                indexMaps[iAge][iStep] = -1;
            }
        }

        // If the movement.map.spX exists, maps are initialised by using NetCDF
        // else, classic definitions of maps.
        String key = prefix + ".map.file." + suffix + iSpeciesFile;
        if (!getConfiguration().isNull(key)) {
            info("Reding NetCDF file " + getConfiguration().getFile(key));
            if (!getConfiguration().isNull(prefix + ".map.file.variable." + suffix + iSpeciesFile)) {
                this.loadMapsNcMaps();
            } else {
                this.loadMapsNc();
            }
            // Check the map indexation
            if (!checkMapIndexation()) {
                error("Missing map indexation for species " + getSpecies(iSpecies).getName() + " in map series '" + prefix + ".map*'. Please refer to prior warning messages for details.", null);
            }

        } else {
            loadMapsCsv();
            // Check the map indexation
            if (!checkMapIndexation()) {
                error("Missing map indexation for species " + getSpecies(iSpecies).getName() + " in map series '" + prefix + ".map*'. Please refer to prior warning messages for details.", null);
            }

            // Get rid of redundant map definitions
            eliminateTwinMap();
        }

    }

    /**
     * Method to initialize MapSets from NetCDF file.
     *
     * @author Nicolas Barrier
     * @throws java.io.IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    public void loadMapsNc() throws IOException, InvalidRangeException {

        String key = prefix + ".map.file." + suffix + iSpeciesFile;
        String ncFile = getConfiguration().getFile(key);

        // Open the NetCDF file
        NetcdfFile nc = NetcdfFile.open(ncFile);

        // Counts the number of maps associated with the current species
        int nmapmax = nc.findDimension("m").getLength();

        // Initialize the total number of grid maps 
        maps = new GridMap[nmapmax];

        // Loop over all the Maps defined in the NetCDF.
        for (int i = 0; i < nmapmax; i++) {

            // Reads the GridMap by using NetCDF
            maps[i] = new GridMap();
            maps[i].read(nc, i);

            // If the map is set to 0 everywhere
            if (maps[i].count() == 0) {
                // add a warning here.
                maps[i] = null;
            }

            // Reads the NetCDF variables agemin and agemax
            // if unset, ageMin/age<ax should be set equal to -1
            int ageMin = nc.findVariable("agemin").read(new int[]{i}, new int[]{1}).getInt(0);
            int ageMax = nc.findVariable("agemax").read(new int[]{i}, new int[]{1}).getInt(0);

            // If ageMin is unset in the NetCDF files (values of -1),
            // default values are set.
            if (ageMin == -1) {
                ageMin = 0;
            }
            if (ageMax == -1) {
                ageMax = getSpecies(iSpecies).getLifespanDt() / getConfiguration().getNStepYear();
            }

            ageMin *= getConfiguration().getNStepYear();
            ageMax *= getConfiguration().getNStepYear();
            ageMax = Math.min(ageMax, getSpecies(iSpecies).getLifespanDt());

            // extracts the tempSeason array, with 0 if map should be use for current season and 1 if should be useed
            byte[] tempSeason = (byte[]) nc.findVariable("season").read(new int[]{i, 0}, new int[]{1, getConfiguration().getNStepYear()}).copyTo1DJavaArray();

            //  number of years
            int nyear = (int) Math.ceil(getConfiguration().getNStep() / (float) getConfiguration().getNStepYear());

            // Extracts the yearmin and yearmax
            int yearMin = nc.findVariable("yearmin").read(new int[]{i}, new int[]{1}).getInt(0);
            int yearMax = nc.findVariable("yearmax").read(new int[]{i}, new int[]{1}).getInt(0);
            if (yearMax == -1) {
                yearMax = nyear;
            }
            yearMax = Math.min(yearMax, nyear);
            yearMin = Math.max(yearMin, 0);

            int nStepYear = getConfiguration().getNStepYear();
            for (int iAge = ageMin; iAge < ageMax; iAge++) {
                for (int iYear = yearMin; iYear < yearMax; iYear++) {
                    int index = 0;    // index of the current season season in which we are
                    for (int iSeason : tempSeason) {  // loop over all the seasons
                        if (iSeason == 1) {
                            int iStep = iYear * nStepYear + index;
                            indexMaps[iAge][iStep] = i;
                        }
                        index++;
                    }
                }

            }
        }

        nc.close();
    }

    public void loadMapsCsv() {

        int nmapmax = getConfiguration().findKeys(prefix + ".species.map*").size();

        List<Integer> mapNumber = new ArrayList<>();
        int imap = 0;
        // Retrieve the index of the maps for this species
        for (int n = 0; n < nmapmax; n++) {
            while (!getConfiguration().canFind(prefix + ".species" + ".map" + imap)) {
                imap++;
            }
            String key = prefix + ".species" + ".map" + imap;
            Species species = getSpecies(getConfiguration().getString(key));
            if (null != species) {
                if (species.getSpeciesIndex() == iSpecies) {
                    mapNumber.add(imap);
                }
            } else {
                error("Wrong species name in spatial map series '" + prefix + ".map*'", new IOException("Parameter " + key + " = " + getConfiguration().getString(key) + " does not match any predefined species name."));
            }
            imap++;
        }

        maps = new GridMap[mapNumber.size()];
        mapFile = new String[mapNumber.size()];

        // Load the maps
        for (int n = 0; n < mapNumber.size(); n++) {
            imap = mapNumber.get(n);
            /*
             * read age min and age max concerned by this map
             */
            int ageMin = (int) Math.round(getConfiguration().getFloat(prefix + ".initialAge" + ".map" + imap) * getConfiguration().getNStepYear());
            int ageMax = (int) Math.round(getConfiguration().getFloat(prefix + ".lastAge" + ".map" + imap) * getConfiguration().getNStepYear());
            ageMax = Math.min(ageMax, getSpecies(iSpecies).getLifespanDt());
            
            /*
             * read the time steps over the year concerned by this map
             */
            StepParameters seasonParam = new StepParameters(prefix, "map" + imap);
            int[] mapSeason = seasonParam.getSeasons();
            
            /*
             * Read year min and max concerned by this map
             */
            YearParameters yearParam = new YearParameters(prefix, "map" + imap);            
            int[] mapYears = yearParam.getYears();
                                    
            /*
             * Assign number of maps to numMap array
             */
            int nStepYear = getConfiguration().getNStepYear();
            for (int iAge = ageMin; iAge < ageMax; iAge++) {
                for (int iYear : mapYears) {
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
            if (!getConfiguration().isNull(prefix + ".file" + ".map" + imap)) {
                String csvFile = getConfiguration().getFile(prefix + ".file" + ".map" + imap);
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
                    int year = iStep / nStepYear;
                    int step = iStep % nStepYear;
                    warning("No map assigned for {0} age (dt) {1} year {2} step {3}", new Object[]{getSpecies(iSpecies).getName(), iAge, year, step});
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
    
    
    
    /**
     * Method to initialize MapSets from NetCDF file.
     *
     * @author Nicolas Barrier
     * @throws java.io.IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    public void loadMapsNcMaps() throws IOException, InvalidRangeException {

        // Get filename as movements.map.file.spX
        String key = prefix + ".map.file." + suffix + iSpeciesFile;
        String ncFile = getConfiguration().getFile(key);

        // Open the NetCDF file
        NetcdfFile nc = NetcdfFile.open(ncFile);
        
        // Recovery of the variable name from movements.map.file.variable.spX
        String varName = getConfiguration().getString(prefix +  ".map.file.variable." + suffix + iSpeciesFile);
        
        // Recovery of the number of time steps per year in the file
        int ncPerYear = getConfiguration().getInt(prefix +  ".map.file.nsteps.year." + suffix + iSpeciesFile);
        
        Variable var = nc.findVariable(null, varName);
        if(var.getRank() != 3) { 
            error(varName + " must have 3 dimensions, " + var.getRank() + " provided", null);
        }
        
        int ntime = var.getShape()[0];

        // Load all the maps (one per time step)
        maps = new GridMap[ntime];
        
        for (int i = 0; i < ntime; i++) { 
            
            // Reads the GridMap by using NetCDF
            maps[i] = new GridMap();
            maps[i].read(nc, i, varName);

            // If the map is set to 0 everywhere
            if (maps[i].count() == 0) {
                // add a warning here.
                maps[i] = null;
            }
        } 

        // Init the indexMaps (same for each age)
        int ndt = this.getConfiguration().getNStepYear();
        int ageMin = 0;
        int ageMax = getSpecies(iSpecies).getLifespanDt();
        for (int iAge = ageMin; iAge < ageMax; iAge++) {
            for (int iStep = 0; iStep < getConfiguration().getNStep(); iStep++) {
                int iStepNc = (iStep / (ndt / ncPerYear)) % ntime;
                indexMaps[iAge][iStep] = iStepNc;
            }
        }
            
        nc.close();
    }
    
    
}
