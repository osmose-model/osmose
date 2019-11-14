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
package fr.ird.osmose.background;

import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.MapSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;

/**
 * This class handles map sets for background species. Contrary to map sets for
 * focus species, maps are defined from class index and not age value.
 *
 */
public class BackgroundMapSet extends MapSet {

    // barrier.n: number of classes
    private final int nClass;

    /**
     * Public constructor.
     *
     * @param iSpecies Index of the background species
     * @param prefix Prefix (movement.bkgspecies)
     * @param suffix Suffix (bkg)
     * @param nClass Number of size classes
     */
    public BackgroundMapSet(int iSpecies, String prefix, String suffix, int nClass) {
        super(iSpecies, prefix, suffix);
        this.nClass = nClass;
    }

    /**
     * Loads the background maps. Contrary to the parent function, it considers
     * classes instead of age values
     *
     * @throws IOException
     * @throws InvalidRangeException
     * @todo Maybe there is a better way to do this from the MapSet parent
     * class.
     */
    @Override
    public void loadMaps() throws IOException, InvalidRangeException {

        // Initialisation of the indexMaps array (valid for both NetCDF and CSV maps
        int nSteps = Math.max(getConfiguration().getNStep(), getConfiguration().getNStepYear());
        indexMaps = new int[nClass][];
        for (int iAge = 0; iAge < nClass; iAge++) {
            indexMaps[iAge] = new int[nSteps];
            for (int iStep = 0; iStep < nSteps; iStep++) {
                indexMaps[iAge][iStep] = -1;
            }
        }

        // If the movement.map.spX exists, maps are initialised by using NetCDF
        // else, classic definitions of maps.
        String key = prefix + ".map." + suffix + iSpecies;
        if (getConfiguration().canFind(key)) {
            info("Reding NetCDF file " + getConfiguration().getFile(key));
            loadMapsNc();
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

        this.normalize();

    }

    /**
     * Method to initialize MapSets from NetCDF file.
     *
     * @author Nicolas Barrier
     * @throws java.io.IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    @Override
    public void loadMapsNc() throws IOException, InvalidRangeException {

        String key = prefix + ".map." + suffix + iSpecies;
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

            // If the map is set to everywhere
            if (maps[i].count() == 0) {
                maps[i] = null;
            }

            // Reads the NetCDF variables agemin and agemax
            int iClass = nc.findVariable("class").read(new int[]{i}, new int[]{1}).getInt(0);

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

            for (int iYear = yearMin; iYear < yearMax; iYear++) {
                int index = 0;
                for (int iSeason : tempSeason) {  // loop over all the seasons
                    if (iSeason == 1) {
                        int iStep = iYear * nStepYear + index;
                        indexMaps[iClass][iStep] = i;
                    }
                    index++;
                }
            }
        }

        nc.close();
    }

    @Override
    public void loadMapsCsv() {

        // recovers the number of params starting
        // by movement.bkgspecies.file.mapXX
        int nmapmax = getConfiguration().findKeys(prefix + ".file.map*").size();

        List<Integer> mapNumber = new ArrayList();
        int imap = 0;
        // Retrieve the index of the maps for this species
        for (int n = 0; n < nmapmax; n++) {
            while (!getConfiguration().canFind(prefix + ".file.map" + imap)) {
                imap++;
            }
            // recovering the name of the bkg species (movement.bkgspecies.species..mapX)
            String key = prefix + ".species.map" + imap;
            BackgroundSpecies species = getBkgSpecies(getConfiguration().getString(key));
            if (null != species) {
                if (species.getIndex() == iSpecies) {
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
            int iClass = (int) Math.round(getConfiguration().getFloat(prefix + ".class." + ".map" + imap) * getConfiguration().getNStepYear());

            /*
             * read the time steps over the year concerned by this map
             */
            int[] mapSeason = getConfiguration().getArrayInt(prefix + ".season" + ".map" + imap);
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
             * Assign number of maps to numMap array
             */
            int nStepYear = getConfiguration().getNStepYear();

            for (int iYear = yearMin; iYear < yearMax; iYear++) {
                for (int iSeason : mapSeason) {
                    int iStep = iYear * nStepYear + iSeason;
                    if (iStep < indexMaps[iClass].length) {
                        indexMaps[iClass][iYear * nStepYear + iSeason] = n;
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
        for (int iClass = 0; iClass < nClass; iClass++) {
            for (int iStep = 0; iStep < nSteps; iStep++) {
                if (indexMaps[iClass][iStep] < 0) {
                    isMapOK = false;
                    int year = iStep / nStepYear;
                    int step = iStep % nStepYear;
                    warning("No map assigned for {0} age {1} year {2} step {3}", new Object[]{getBkgSpecies(iSpecies).getName(), iClass, year, step});
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
     * Normalize maps so that they all have a mean of 1. This insures that
     * spatially integrated biomass is equal to the value provided in the time
     * serie.
     */
    private void normalize() {
        for (GridMap m : maps) {
            if (null != m) {
                float N = m.count();
                if (N > 0) {     // avoids division by 0
                    for (int j = 0; j < getGrid().get_ny(); j++) {
                        for (int i = 0; i < getGrid().get_nx(); i++) {
                            m.setValue(i, j, m.getValue(i, j) / N);
                        }   // end of i loop
                    }   // end of j j loop
                }   // end if N>0 check
            }   // end of if statement
        }   // end of loop over maps
    }

}
