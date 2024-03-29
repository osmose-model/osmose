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

package fr.ird.osmose.populator;

import fr.ird.osmose.School;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;

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

            nc = NetcdfDatasets.openDataset(ncfile);
        } catch (IOException ex) {
            error("Failed to open restart file " + ncfile, ex);
        }
    }

    @Override
    public void populate() {

        boolean useGenetic = this.getConfiguration().isGeneticEnabled();
        boolean useBioen = this.getConfiguration().isBioenEnabled();
        Variable genetVar = null; // variable containing the genotype
        Variable traitVarVar = null;  // variable containing the env. noise
        Variable gonadVar = null;
        Variable maturityVar = null;
        ArrayFloat.D4 genotype = null;   // data array containing the Netcdf genotype array
        ArrayFloat.D2 traitNoise = null;   // data array containing the Netcdf genotype array
        ArrayFloat.D1 gonadWeight = null;
        ArrayInt.D1 maturity = null;

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

            if(useBioen) {
                gonadVar = nc.findVariable("gonadWeight");
                gonadWeight = (ArrayFloat.D1) gonadVar.read();
                maturityVar = nc.findVariable("maturity");
                maturity = (ArrayInt.D1) maturityVar.read();
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
                school.instance_genotype(this.getRank());
                if (useGenetic) {
                    school.restartGenotype(this.getRank(), s, genotype, traitNoise);
                }
                if(useBioen) {
                    // Weight is saved in g in netcdf, so must be provided converted in tons.
                    school.setGonadWeight(gonadWeight.get(s) * 1e-6f);
                    school.setIsMature(maturity.get(s) == 1);
                }
                getSchoolSet().add(school);
            }
            nc.close();
        } catch (IOException ex) {
            error("Error reading restart file " + nc.getLocation(), ex);
        }
    }
}
