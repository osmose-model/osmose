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

package fr.ird.osmose.process.genet;

import fr.ird.osmose.util.SimulationLinker;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author nbarrier
 */
public class Trait extends SimulationLinker {

    /**
     * Number of locus that code the traits. One value for each species
     */
    private int[] nLocus;

    /** Variance of the trait expressed due to env. One value per species. */
    private double[] envVar;

    /**
     * Number of possible values that the locus can take. One value for each
     * species
     */
    private int[] nVal;

    /**
     * Mean and variance of trait. One value for each species
     */
    private double[] xMean, xVar;

    /**
     * Name of the trait.
     */
    private final String prefix;

    /**
     * Diversity matrix, used to initialize genotypes. One matrix for each
     * species
     */
    private double diversity[][][];

    private Random generator;

    private int traitIndex;

    /**
     * Locus constructor.
     *
     * @param rank
     * @param prefix
     */
    public Trait(int rank, String prefix, int traitIndex) {

        super(rank);
        // Trait eyecolor = new Trait(rank, "eyecol")
        this.prefix = prefix;
        this.traitIndex = traitIndex;

        if(getConfiguration().getBoolean("simulation.fixed.seed.enabled", false)) {
            // Assumes a seed of dimension [trait][nSimu]
            long seed = traitIndex * getConfiguration().getNSimulation() + getRank();
            generator = new Random(seed);
        } else {
            generator = new Random();
        }
    }

    public void init() {

        String key;
        int cpt;
        int[] focalIndex = this.getConfiguration().getFocalIndex();

        int nspecies = this.getNSpecies();

        // look for the mean value of the trait
        xMean = new double[nspecies];
        key = String.format("%s.trait.mean", prefix);
        cpt = 0;
        for (int iSpeciesFile : focalIndex) {
            xMean[cpt] = this.getConfiguration().getDouble(key + ".sp" + iSpeciesFile);
            cpt++;
        }

        envVar = new double[nspecies];
        key = String.format("%s.trait.envvar", prefix);
        cpt = 0;
        for (int iSpeciesFile : focalIndex) {
            envVar[cpt] = this.getConfiguration().getDouble(key + ".sp" + iSpeciesFile);
            cpt++;
        }

        // look for the variance (sigma^2) of the trait
        xVar = new double[nspecies];
        key = String.format("%s.trait.var", prefix);
        cpt = 0;
        for (int iSpeciesFile : focalIndex) {
            xVar[cpt] = this.getConfiguration().getDouble(key + ".sp" + iSpeciesFile);
            cpt++;
        }

        // number of locus that code the trait
        nLocus = new int[nspecies];
        key = String.format("%s.trait.nlocus", prefix);
        cpt = 0;
        for (int iSpeciesFile : focalIndex) {
            nLocus[cpt] = this.getConfiguration().getInt(key + ".sp" + iSpeciesFile);
            cpt++;
        }

        // number of values that the locus can take
        nVal = new int[nspecies];
        key = String.format("%s.trait.nval", prefix);
        cpt = 0;
        for (int iSpeciesFile : focalIndex) {
            nVal[cpt] = this.getConfiguration().getInt(key + ".sp" + iSpeciesFile);
            cpt++;
        }

        // If restart is enabled, recover the diversity from restart file
        if(getConfiguration().isRestart()) {
            this.restartDiversity();
            return;
        }


        diversity = new double[nspecies][][];

        for (int ispec = 0; ispec < nspecies; ispec++) {

            diversity[ispec] = new double[nLocus[ispec]][nVal[ispec]];
            // Convert the trait variance to "Loci" standard deviation.
            // variance is xvar / (2 * Lc)
            // hence standard deviation is sqrt(xvar / (2 * Lc))
            double stddev = Math.sqrt(xVar[ispec] / (2 * nLocus[ispec]));

            // initialisation of the "diversity" matrix, which is
            // the array of possible values for each of the locis
            // that code the trait
            for (int i = 0; i < nLocus[ispec]; i++) {
                for (int k = 0; k < nVal[ispec]; k++) {  // k = 0, 1
                    diversity[ispec][i][k] = generator.nextGaussian() * stddev;
                }
            }
        }  // end of species loop
    }

    /**
     * Returns the number of locus that codes the trait.
     *
     * @param index Species index
     * @return
     */
    public int getNLocus(int index) {
        return this.nLocus[index];
    }

    /**
     * Returns the number of values a locus can take for the trait.
     *
     * @param index Species index
     * @return
     */
    public int getNValues(int index) {
        return this.nVal[index];
    }

    /**
     * Returns the variance of the trait.
     *
     * @param index Species index
     * @return
     */
    public double getVar(int index) {
        return this.xVar[index];
    }

    /**
     * Returns the diversity matrix of the trait.
     *
     * @param spec_index Species index
     * @param loc_index Locus index (L_c)
     * @param val_index Value index (V)
     * @return
     */
    public double getDiv(int spec_index, int loc_index, int val_index) {
        return this.diversity[spec_index][loc_index][val_index];
    }

    /**
     * Returns the mean value of the trait.
     *
     * @param spec_index
     * @return
     */
    public double getMean(int spec_index) {
        return this.xMean[spec_index];
    }

    /** Get the name of the variable trait.
     *
     * @return Name of the trait.
     */
    public String getName() {
        return this.prefix;
    }

    /**
     * Add some environmental noise to the trait "expression". Trait expressed is due
     * to genotype + some noise (sigma_e^2). Species dependent.
     * @param index
     * @return
     */
    public double addTraitNoise(int index) {
        double std = Math.sqrt(this.envVar[index]);
        double val = generator.nextGaussian() * std;
        return val;
    }


    public void restartDiversity() {

        NetcdfFile nc = null;
        String key = "simulation.restart.file";

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

        // Recover the list of species indexes from the restart
        ArrayInt.D1 species = null;
        try {
            species = (ArrayInt.D1) nc.findVariable("species").read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Variable genetVar = null;
        ArrayFloat.D4 genotype = null;   // data array containing the Netcdf genotype array
        genetVar = nc.findVariable("genotype");
        try {
            genotype = (ArrayFloat.D4) genetVar.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Recover the dimensions of the genotype variable
        int restart_nSchool = genetVar.getDimension(0).getLength();
        int restart_nTrait = genetVar.getDimension(1).getLength();
        int restart_nMaxLoci = genetVar.getDimension(2).getLength();
        int restart_nValue = genetVar.getDimension(3).getLength();

        int nSpecies = getNSpecies();

        //  genotype.set(s, iTrait, iLoci, 0, (float) gen.getLocus(iTrait, iLoci).getValue(0));

        // Initialize the list of biodiversity values for each species and locus
        // Create the list by species
        List<List<List<Double>>> values = new ArrayList<>();
        for(int ispec = 0; ispec < nSpecies; ispec++) {

            // Get the number of locus for the species
            int nLocus = this.nLocus[ispec];
            // Initialize the list of locus for the species
            List<List<Double>> list_locus = new ArrayList<>();
            for(int i  = 0; i < nLocus; i++) {
                // For each locus, initialize the list of values
                List<Double> list_values = new ArrayList<>();
                list_locus.add(list_values); // Initializes the list of values for the locus
            }
            values.add(list_locus);  // Initializes the list of locus for the species
        }

        // Loop over all the saved variables in restart (one value per school)
        for (int ischool = 0; ischool < restart_nSchool; ischool++) {
            int speciesIndex = species.get(ischool);
            // Loop over all the locus for the trait and the species index
            for (int locus = 0; locus < this.nLocus[speciesIndex]; locus++) {
                for (int k = 0; k < 2; k++) {
                    double value = genotype.get(ischool, traitIndex, locus, k);
                    if (!values.get(speciesIndex).get(locus).contains(value)) {
                        values.get(speciesIndex).get(locus).add(value);
                    }
                }
            }
        }

        diversity = new double[nSpecies][][];

        // Initialize the diversity array for the trait
        for(int indexSpecies = 0; indexSpecies < getNSpecies(); indexSpecies++) {
            int nLocus = this.nLocus[indexSpecies];
            diversity[indexSpecies] = new double[nLocus][];
            for(int locus = 0; locus < nLocus; locus++) {
                int nValues = values.get(indexSpecies).get(locus).size();
                diversity[indexSpecies][locus] = new double[nValues];
                for(int k = 0; k < nValues; k++) {
                    diversity[indexSpecies][locus][k] = values.get(indexSpecies).get(locus).get(k);
                }
            }
        }

        try {
            nc.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
