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

import java.util.Random;

import fr.ird.osmose.Configuration;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.growth.AbstractGrowth;

/**
 *
 * @author pverley
 */
public class RelativeBiomassPopulator extends AbstractPopulator {

    private Random rand;

    /**
     * Initial biomass to release to the system. Dimension = [nSpecies]. Units =
     * tons
     */
    private double[] seedingBiomass;

    /**
     * Biomass proportion among the size classes. Dimension = [nSpecies][nLenghts]
     * Units = [0, 1]
     */
    private double[][] biomassProportion;

    /**
     * Size of the released schools. Dimensions = [nSpecies][nLenghts]. Units = [cm]
     */
    private double[][] sizeMin;

    private double[][] sizeMax;

    /** Size lengths of the released schools. Dimensions = [nSpecies][nLenghts] */
    private double[][] trophicLevels;

    /** Age of the released schools. Dimensions = [nSpecies][nLenghts] */
    private int[][] ageDt;

    private int[] nSize;

    private GrowthProcess growthProcess;

    public RelativeBiomassPopulator(int rank) {
        super(rank);
        growthProcess = new GrowthProcess(rank);
    }

    @Override
    public void init() {

        growthProcess.init();

        Configuration cfg = this.getConfiguration();
        int nSpecies = cfg.getNSpecies();

        int cpt;

        // Init the random generator
        if (!cfg.isNull("population.initialization.seed")) {
            rand = new Random(cfg.getLong("population.initialization.seed"));
        } else {
            rand = new Random();
        }

        double[] lInf = new double[nSpecies];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            lInf[cpt] = getConfiguration().getDouble("species.linf.sp" + i);
            cpt++;
        }

        // Init the seeding biomass for each species (tons)
        seedingBiomass = new double[nSpecies];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            seedingBiomass[cpt] = cfg.getDouble("population.initialization.biomass.sp" + i);
            cpt++;
        }

        // Init the sizes (in cm) for each species and each size class
        nSize = new int[nSpecies];
        sizeMin = new double[nSpecies][];
        sizeMax = new double[nSpecies][];
        cpt = 0;
        for (int iSpeciesFiles : this.getFocalIndex()) {
            // Should be of size NClass + 1
            double sizeTemp[] = cfg.getArrayDouble("population.initialization.size.sp" + iSpeciesFiles);
            nSize[cpt] = sizeTemp.length - 1;
            for (int iClass = 0; iClass < nSize[cpt]; iClass++) {
                sizeMin[cpt][iClass] = sizeTemp[iClass];
                sizeMax[cpt][iClass] = sizeTemp[iClass + 1];
            }
   
            cpt++;
    
        }

        // Init the trophic levels for each species and each size class.
        trophicLevels = new double[nSpecies][];
        cpt = 0;
        for (int iSpeciesFile : this.getFocalIndex()) {
            trophicLevels[cpt] = cfg.getArrayDouble("population.initialization.tl.sp" + iSpeciesFile);
            if (trophicLevels[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values",
                        "population.initialization.tl.sp" + iSpeciesFile, nSize[cpt]);
                error(message, new Exception());
            }
            cpt++;
        }

        biomassProportion = new double[nSpecies][];
        cpt = 0;
        for (int iSpeciesFile : this.getFocalIndex()) {
            biomassProportion[cpt] = cfg.getArrayDouble("population.initialization.relativebiomass.sp" + iSpeciesFile);
            if (biomassProportion[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values",
                        "population.initialization.relativebiomass.sp" + iSpeciesFile, nSize[cpt]);
                error(message, new Exception());
            }
            cpt++;
        }
        
        // Init the age for each species and each size class.
        ageDt = new int[nSpecies][];
        cpt = 0;
        for (int iSpeciesFile : this.getFocalIndex()) {

            // load ages in years
            double temp[] = cfg.getArrayDouble("population.initialization.age.sp" + iSpeciesFile);

            if (temp.length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values",
                        "population.initialization.age.sp" + iSpeciesFile, nSize[cpt]);
                error(message, new Exception());
            }
            cpt++;

            ageDt[cpt] = new int[temp.length];
            for (int k = 0; k < temp.length; k++) {
                ageDt[cpt][k] = (int) (temp[k] * getConfiguration().getNStepYear());
            }

        }
        
    }

    @Override
    public void populate() {

        int nSpecies = this.getNSpecies();
        for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {

            int nSchool = getConfiguration().getNSchool(iSpecies);

            for (int iLength = 0; iLength < nSize[iSpecies]; iLength++) {

                double lengthMin = this.sizeMin[iSpecies][iLength];
                double lengthMax = this.sizeMax[iSpecies][iLength];
                int ageDt = this.ageDt[iSpecies][iLength];

                // Biomass in tons
                double biomass = this.seedingBiomass[iSpecies] * this.biomassProportion[iSpecies][iLength];
                if (biomass == 0) {
                    continue;
                }

                for (int s = 0; s < nSchool; s++) {
                    School school0 = this.generateSchool(biomass / nSchool, lengthMin, lengthMax, ageDt, iSpecies);
                    getSchoolSet().add(school0);
                    biomass -= school0.getBiomass();
                }
            }
        }        
    }

    private School generateSchool(double biomass, double lengthMin, double lengthMax, int ageDt, int iSpecies) {

        // Configuration cfg = getConfiguration();
        Species species = getSpecies(iSpecies);

        // Ramdom draft of length
        double length = lengthMin + rand.nextDouble() * (lengthMax - lengthMin);
        if (length == 0) {
            length = species.getEggSize();
        }

        // AbstractGrowth growth = growthProcess.getGrowth(iSpecies);
        // int ageDt;
        // if (length == species.getEggSize()) {
        //     ageDt = 0;
        // } else {
        //     ageDt = (int) Math.round(growth.lengthToAge(length) * cfg.getNStepYear());
        // }
        // ageDt = Math.min(ageDt, species.getLifespanDt() - 1);

        double weight;
        if (length == species.getEggSize()) {
            weight = species.getEggWeight();
        } else {
            weight = (double) species.computeWeight((float) length);
        }

        // Computes the abundance based on weight ratio. Weight is in g, so
        // it biomass is converted in grams to get abundance
        double nEgg = biomass * 1e6 / (weight); 
        
        // In school constructor, weight is provided in g.
        School school0 = new School(species, nEgg, (float) length, (float) weight, (int) ageDt);

        return school0;

    }

}
