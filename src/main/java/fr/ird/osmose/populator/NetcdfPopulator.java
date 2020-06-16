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
package fr.ird.osmose.populator;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class NetcdfPopulator extends AbstractPopulator {

    private NetcdfFile nc;
    private final String key;

    public NetcdfPopulator(int rank, String key) {
        super(rank);
        this.key = key;
    }

    @Override
    public void init() {

        String plainFilename = getConfiguration().getFile(key);
        String rankedFilename = plainFilename + "." + getRank();
        boolean plainFile = false, rankedFile = false;
        if (new File(plainFilename).exists()) {
            plainFile = true;
        }
        if (new File(rankedFilename).exists()) {
            rankedFile = true;
        };

        if (!plainFile && !rankedFile) {
            error("Could not find any NetCDF initialization file (check parameter " + key + ").", new FileNotFoundException("Neither file " + plainFilename + " nor " + rankedFilename + " exist."));
        } else if (plainFile && rankedFile) {
            warning("Found two suitable NetCDF initialization files: " + plainFilename + " and " + rankedFilename + ". Osmose will use the latest " + rankedFilename);
        }

        String ncfile = rankedFile ? rankedFilename : plainFilename;
        try {

            nc = NetcdfFile.open(ncfile);
        } catch (IOException ex) {
            error("Failed to open restart file " + ncfile, ex);
        }
    }

    @Override
    public void populate() {

        boolean useGenetic = this.getConfiguration().isGeneticEnabled();
        Variable genetVar = null; // variable containing the genotype
        Variable traitVarVar = null;  // variable containing the env. noise
        ArrayFloat.D4 genotype = null;   // data array containing the Netcdf genotype array
        ArrayFloat.D2 traitNoise = null;   // data array containing the Netcdf genotype array

        int nSchool = nc.findDimension("nschool").getLength();
        try {
            int[] ispecies = (int[]) nc.findVariable("species").read().copyTo1DJavaArray();
            float[] x = (float[]) nc.findVariable("x").read().copyTo1DJavaArray();
            float[] y = (float[]) nc.findVariable("y").read().copyTo1DJavaArray();
            double[] abundance = (double[]) nc.findVariable("abundance").read().copyTo1DJavaArray();
            float[] length = (float[]) nc.findVariable("length").read().copyTo1DJavaArray();
            float[] weight = (float[]) nc.findVariable("weight").read().copyTo1DJavaArray();
            float[] age = (float[]) nc.findVariable("age").read().copyTo1DJavaArray();
            float[] trophiclevel = (float[]) nc.findVariable("trophiclevel").read().copyTo1DJavaArray();
            if (useGenetic) {
                genetVar = nc.findVariable("genotype");
                genotype = (ArrayFloat.D4) genetVar.read();
                traitVarVar = nc.findVariable("trait_variance");
                traitNoise = (ArrayFloat.D2) traitVarVar.read();
            }

            for (int s = 0; s < nSchool; s++) {

                School school = new School(
                        getSpecies(ispecies[s]),
                        x[s],
                        y[s],
                        abundance[s],
                        length[s],
                        weight[s],
                        Math.round(age[s] * getConfiguration().getNStepYear()),
                        trophiclevel[s]);
                if (useGenetic) {
                    school.restartGenotype(this.getRank(), s, genotype, traitNoise);
                }
                getSchoolSet().add(school);
            }
            nc.close();
        } catch (IOException ex) {
            error("Error reading restart file " + nc.getLocation(), ex);
        }
    }
}
