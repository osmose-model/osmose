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
package fr.ird.osmose.process.mortality.fisheries;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.Separator;
import fr.ird.osmose.util.OsmoseLinker;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Nicolas Barrier (nicolas.barrier@ird.fr)
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class AccessMatrix extends OsmoseLinker {

    /**
     * Accessibility (in percentage). Dimensions = [species][fisheries]
     */
    private double[][] values;

    /**
     * Number of Fisheries.
     */
    private int nFisheries;

    /**
     * Reads the fisheries accessibility matrix. The return value is of
     * dimensions [nFisheries][nSpecies]
     *
     * @param filename
     */
    public void read(String filename) {

        Configuration cfg = Osmose.getInstance().getConfiguration();

        // recovers the number of species
        int nSpecies = getConfiguration().getNSpecies();

        // recovers the number of fisheries
        nFisheries = cfg.findKeys("fishery.selectivity.type.fsh*").size();

        read(filename, nFisheries, nSpecies);
    }

    /**
     * Reads the fisheries accessibility matrix. The return value is of
     * dimensions [nFisheries][nSpecies]
     *
     * @param filename
     * @param nFisheries
     * @param nSpecies
     */
    public void read(String filename, int nFisheries, int nSpecies) {

        try {
            // 1. Open the CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), Separator.guess(filename).getSeparator());
            List<String[]> lines = reader.readAll();

            // 2. Check the number of line and checks that it equal to nFisheries 
            int nTimeSerie = lines.size() - 1;
            if (nTimeSerie < nFisheries) {
                error("Found " + nTimeSerie + " lines in the file. It must contain " + nFisheries + " lines.", new Exception());
            }

            // 3. Read the time serie
            // Initialize the output array
            values = new double[nFisheries][nSpecies];

            // Loop over the lines (skip header)
            for (int t = 0; t < nFisheries; t++) {
                String[] line = lines.get(t + 1);

                // Checks that the matrix contains nSpecies column (skip column index)
                if (line.length != nSpecies + 1) {
                    error("The number of column must be " + nSpecies, new IOException());
                }

                // Loop over the nSpecies colums
                for (int k = 0; k < nSpecies; k++) {
                    values[t][k] = Double.valueOf(line[k + 1]) / 100. ;  // barrier.n: multiplication by 1/100. to have values into [0, 1]
                    // force the values between [0-1] 
                    values[t][k] = Math.max(values[t][k], 0);  
                    values[t][k] = Math.min(values[t][k], 1);  
                }
            }

        } catch (IOException ex) {
            error("Error reading CSV file " + filename, ex);
        }
    }

    /**
     * Returns the [nFisheries, nSpecies] accessibility matrix.
     * @return 
     */
    public double[][] getValues() {
        return values;
    }

    /**
     * Returns the [nSpecies] accessibility matrix for a given fisheries
     *
     * @param iFish Fisheries index
     * @return 
     */
    public double[] getValues(int iFish) {
        return values[iFish];
    }

    /**
     * Returns the accessibility matrix for a given fisheries and a given
     * specie.
     *
     * @param iFish Fisheries index
     * @param iSpec Species index
     * @return 
     */
    public double getValues(int iFish, int iSpec) {
        return values[iFish][iSpec];

    }

}
