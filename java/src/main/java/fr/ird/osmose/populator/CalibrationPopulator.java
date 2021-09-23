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

import fr.ird.osmose.Configuration;

/**
 *
 * @author pverley
 */
public class CalibrationPopulator extends AbstractPopulator {

    /** Initial biomass to release to the system. Dimension = [nSpecies]*/
    private double[] seedingBiomass;
    
    /** Biomass proportion among the size classes. Dimension = [nSpecies][nLenghts] */
    private double[][] biomassProportion;
    
    /** Size of the released schools. Dimensions = [nSpecies][nLenghts] */
    private double[][] size;
    
    /** Age of the released schools. Dimensions = [nSpecies][nLenghts] */
    private double[][] age;
    
    /** Size lengths of the released schools. Dimensions = [nSpecies][nLenghts] */
    private double[][] trophicLevels;
    
    private int[] nSize;
    
    public CalibrationPopulator(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        Configuration cfg = this.getConfiguration();
        int nSpecies = cfg.getNSpecies();
        
        int cpt;
        
        seedingBiomass = new double[nSpecies];
        cpt = 0;
        for(int i : this.getFocalIndex()) {
            seedingBiomass[cpt] = cfg.getDouble("population.initialization.biomass.sp" + i);
            cpt++;
        }
        
        nSize = new int[nSpecies];
        size = new double[nSpecies][];
        cpt = 0;
        for(int i : this.getFocalIndex()) {
            size[cpt] = cfg.getArrayDouble("population.initialization.size.sp" + i);
            nSize[cpt] = size[cpt].length;
            cpt++;
        }
        
        trophicLevels = new double[nSpecies][];
        cpt = 0;
        for(int i : this.getFocalIndex()) {
            trophicLevels[cpt] = cfg.getArrayDouble("population.initialization.tl.sp" + i);
            if(trophicLevels[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values", "population.initialization.tl.sp" + i, nSize[cpt]);   
                error(message, new Exception());
            }
            cpt++;
        }
        
        biomassProportion = new double[nSpecies][];
        cpt = 0;
        for(int i : this.getFocalIndex()) {
            biomassProportion[cpt] = cfg.getArrayDouble("population.initialization.relativebiomass.sp" + i);
            if(biomassProportion[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values", "population.initialization.relativebiomass.sp" + i, nSize[cpt]);   
                error(message, new Exception());
            }
            cpt++;
        }
        
        age = new double[nSpecies][];
        cpt = 0;
        for(int i : this.getFocalIndex()) {
            age[cpt] = cfg.getArrayDouble("population.initialization.age.sp" + i);
            if(age[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values", "population.initialization.relativebiomass.sp" + i, nSize[cpt]);   
                error(message, new Exception());
            }
            cpt++;
        }

    }

    @Override
    public void populate() {

        /*
        boolean useGenetic = this.getConfiguration().isGeneticEnabled();
        boolean useBioen = this.getConfiguration().isBioenEnabled();
        Variable genetVar = null; // variable containing the genotype
        Variable traitVarVar = null;  // variable containing the env. noise
        Variable gonadVar = null;
        ArrayFloat.D4 genotype = null;   // data array containing the Netcdf genotype array
        ArrayFloat.D2 traitNoise = null;   // data array containing the Netcdf genotype array
        ArrayFloat.D1 gonadWeight = null;

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
                gonadVar = nc.findVariable("genotype");
                gonadWeight = (ArrayFloat.D1) gonadVar.read();
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
                if(useBioen) { 
                    // Weight is saved in g in netcdf, so must be provided converted in tons.
                    school.setGonadWeight(gonadWeight.get(s) * 1e-6f);
                }
                getSchoolSet().add(school);
            }
            nc.close();
        } catch (IOException ex) {
            error("Error reading restart file " + nc.getLocation(), ex);
        }
        */
    }
}
