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

import fr.ird.osmose.Configuration;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
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

    protected boolean checkMaps;
    
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
    protected HashMap<Integer, GridMap> maps;
    
    /**
     * List of the pathnames of the CSV files.
     */
    protected String[] mapFile;

    public MapSet(int iSpeciesFile, int iSpecies, String prefix) {
        this(iSpeciesFile, iSpecies, prefix, "map");
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
        return maps.size();
    }

    public GridMap getMap(int numMap) {
        return maps.get(numMap);
    }

    public HashMap<Integer, GridMap> getMaps() {
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

        this.checkMaps = getConfiguration().getBoolean("movement.checks.enabled");
        
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
        String key = prefix + ".netcdf.enabled";
        if (getConfiguration().getBoolean(key)) {
            this.loadMapsNcMaps();
            // Check the map indexation
            if (!checkMapIndexation()) {
                error("Missing map indexation for species " + getSpecies(iSpecies).getName() + " in map series '" + prefix + ".map*'. Please refer to prior warning messages for details.", null);
            }
            
            eliminateTwinMapNC();

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

    public void loadMapsCsv() throws FileNotFoundException {

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

        maps = new HashMap<>();
        mapFile = new String[mapNumber.size()];
        
        List<String> mapFileList = new ArrayList<>();
        List<Integer> mapIndexList = new ArrayList<>();
        
        // Load the maps
        for (int n = 0; n < mapNumber.size(); n++) {
            imap = mapNumber.get(n);

            /*
             * read age min and age max concerned by this map
             */
            int ageMin = (int) Math.round(getConfiguration().getFloat(prefix + ".initialAge" + ".map" + imap) * getConfiguration().getNStepYear());
            int ageMax = (int) Math.round(getConfiguration().getFloat(prefix + ".lastAge" + ".map" + imap) * getConfiguration().getNStepYear());
            ageMax = Math.min(ageMax, getSpecies(iSpecies).getLifespanDt() - 1);
                        
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
            for (int iAge = ageMin; iAge <= ageMax; iAge++) {
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
                maps.put(n, new GridMap(csvFile));
                mapFileList.add(csvFile);
            } else {
                maps.put(n, null);
                mapFileList.add("null");
            }
            
            mapIndexList.add(null);
            
        }
        
        if(this.checkMaps) { 
            this.writeMovementsChecks(mapFileList, mapIndexList);    
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
                        maps.put(k, null);
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
     * This function eliminates twins in the list of maps
     */
    private void eliminateTwinMapNC() {

        int[] mapIndexNoTwin = new int[maps.size()];
        for (int k = 0; k < maps.size(); k++) {
            GridMap mapK = maps.get(k);
            mapIndexNoTwin[k] = k;
            if (null != mapK) {
                for (int l = k - 1; l >= 0; l--) {
                    GridMap mapL = maps.get(l);
                    if ((mapL != null) && mapK.equals(mapL)) {
                        mapIndexNoTwin[k] = mapIndexNoTwin[l];
                        // Delete twin maps
                        maps.put(k, null);
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
        
        // Load config + nstepyear + nsteps
        Configuration cfg = getConfiguration();
        int dt = cfg.getNStepYear();

        // Load the file prefix with names that are of type. movemement.map.species.mapX
        int nmapmax = getConfiguration().findKeys(prefix + ".species.map*").size();
        List<Integer> mapNumber = new ArrayList<>();
        List<String> mapNcFiles = new ArrayList<>();
        List<Integer> mapNcSteps = new ArrayList<>();
        
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
            }
            imap++;
        }
        
        // One map per timestep and per age number.
        maps = new HashMap<>(); // dimension = [nMaps][ntime]
        int iii = 0;
        
        // Prepare time-indexation.
        int ndt = this.getConfiguration().getNStepYear();
        
        for (Integer im : mapNumber) {

            // Loop over the map indexes for the species.
            // note that it can contains two maps (for two size classes for instance)
            String ncFile = cfg.getFile(prefix + ".file." + suffix + im);
            String varName = cfg.getString(prefix + ".variable." + suffix + im);

            // Recovery of the number of time steps per year in the file
            int ncPerYear = getConfiguration().getInt(prefix + ".nsteps.year." + suffix + im);
            int ageMin = (int) Math.round(getConfiguration().getDouble(prefix + ".initialAge." + suffix + im) * dt);
            int ageMax = (int) Math.round(getConfiguration().getDouble(prefix + ".lastAge." + suffix + im) * dt);
            ageMax = Math.min(ageMax, getSpecies(iSpecies).getLifespanDt() - 1);
   
            // Open the NetCDF file
            NetcdfFile nc = NetcdfFile.open(ncFile);

            Variable var = nc.findVariable(null, varName);
            if ((var.getRank() != 3)) {
                error(varName + " must have 3 dimensions (time, lat, lon), " + var.getRank() + " provided", null);
            }

            // number of time steps in the NetCDF
            int ncTime = var.getShape()[0];

            // for each nctime, read the map.
            // map is indexed as [time, nmaps]
            for (int i = 0; i < ncTime; i++) {

                // Reads the GridMap by using NetCDF
                maps.put(iii, new GridMap());
                maps.get(iii).read(nc, i, varName);

                // If the map is set to 0 everywhere
                if (maps.get(iii).count() == 0) {
                    // add a warning here.
                    maps.put(iii, null);
                    // Set the file name and netcdf index to null
                    mapNcFiles.add("null");
                    mapNcSteps.add(null);
                } else {
                    // Add the file name and netcdf index
                    mapNcFiles.add(ncFile);
                    mapNcSteps.add(i);
                }

                for (int iAge = ageMin; iAge <= ageMax; iAge++) {
                    for (int iStep = 0; iStep < getConfiguration().getNStep(); iStep++) {
                        int iStepNc = (iStep / (ndt / ncPerYear)) % ncTime; // netcdf index to read, based on simulation time step
                        if (iStepNc == i) {
                            // if the nctime step associated with simulation time step matches nc index, add indexMaps
                            indexMaps[iAge][iStep] = iii;
                        }
                    }
                }
                
                
                iii++;

            } // end of nctime loop

            nc.close();
            
            if(this.checkMaps) {
                this.writeMovementsChecks(mapNcFiles, mapNcSteps);   
            }

        } // end of loop on map number
    }  // end of method
    
    
    /**
     * Method that allows to write in an ASCII file the association between the
     * time-step, the species age, the file that is being used and the time-step of
     * the NetCDF file. It allows to check whether the movements maps are properly configured.
     */
    public void writeMovementsChecks(List<String>mapNcFiles, List<Integer>mapNcSteps) throws FileNotFoundException {

        PrintWriter prw;
    
        // Create parent directory
        File file = new File(getFilename());
        file.getParentFile().mkdirs();
        try {
            // Init stream
            prw = new PrintWriter(file);
        } catch (FileNotFoundException ex) {
            error("Failed to create output file " + file.getAbsolutePath(), ex);
            throw (ex);
        }
    
        String separator = getConfiguration().getOutputSeparator();
        String[] headers = {"Age (dt)", "Time Step", "File", "Netcdf Index"};
    
        // Write headers
        for (int i = 0; i < headers.length - 1; i++) {
            prw.print(headers[i]);
            prw.print(separator);
        }
    
        prw.print(headers[headers.length - 1]);
        prw.println();
    
        for (int a = 0; a < this.getSpecies(this.iSpecies).getLifespanDt(); a++) {

            for (int i = 0; i < this.getConfiguration().getNStep(); i++) {
                
                int index = indexMaps[a][i];
                String fileName = mapNcFiles.get(index);
                Integer ncStep = mapNcSteps.get(index);
                
                prw.print(a);
                prw.print(separator);

                prw.print(i);
                prw.print(separator);

                prw.print(fileName);
                prw.print(separator);

                prw.print(ncStep);
                prw.print(separator);

                prw.println();
            }
        }
    
        prw.close();
    
    }
    
    final String getFilename() {
        StringBuilder filename = new StringBuilder();
        String subfolder = "movement_checks";
        String speciesName = getSpecies(this.iSpecies).getName();
        filename.append(subfolder).append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_").append("movement_checks_").append(speciesName);
        filename.append(".csv");
        return filename.toString();
    }
    
    
}
