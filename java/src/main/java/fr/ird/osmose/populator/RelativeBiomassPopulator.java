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


import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class RelativeBiomassPopulator extends AbstractPopulator {

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
    private double[][] size;

    /**
     * Age of the released schools. Dimensions = [nSpecies][nLenghts]. Units = dt,
     * but provided as years
     */
    private double[][] ageDt;

    /**
     * Age of the released schools. Dimensions = [nSpecies][nLenghts]. Units = [g]
     */
    private double[][] weight;

    /** Size lengths of the released schools. Dimensions = [nSpecies][nLenghts] */
    private double[][] trophicLevels;

    private int[] nSize;

    public RelativeBiomassPopulator(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        Configuration cfg = this.getConfiguration();
        int nSpecies = cfg.getNSpecies();

        int cpt;

        seedingBiomass = new double[nSpecies];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            seedingBiomass[cpt] = cfg.getDouble("population.initialization.biomass.sp" + i);
            cpt++;
        }

        nSize = new int[nSpecies];
        size = new double[nSpecies][];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            size[cpt] = cfg.getArrayDouble("population.initialization.size.sp" + i);
            nSize[cpt] = size[cpt].length;
            cpt++;
        }

        trophicLevels = new double[nSpecies][];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            trophicLevels[cpt] = cfg.getArrayDouble("population.initialization.tl.sp" + i);
            if (trophicLevels[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values",
                        "population.initialization.tl.sp" + i, nSize[cpt]);
                error(message, new Exception());
            }
            cpt++;
        }

        biomassProportion = new double[nSpecies][];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            biomassProportion[cpt] = cfg.getArrayDouble("population.initialization.relativebiomass.sp" + i);
            if (biomassProportion[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values",
                        "population.initialization.relativebiomass.sp" + i, nSize[cpt]);
                error(message, new Exception());
            }
            cpt++;
        }

        ageDt = new double[nSpecies][];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            ageDt[cpt] = cfg.getArrayDouble("population.initialization.age.sp" + i);
            if (ageDt[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values",
                        "population.initialization.relativebiomass.sp" + i, nSize[cpt]);
                error(message, new Exception());
            }
            cpt++;
        }

        // Conversion of the age in ageDt
        for (int i = 0; i < nSpecies; i++) {
            for (int l = 0; l < nSize[i]; l++) {
                ageDt[i][l] *= cfg.getNStepYear();
            }
        }

        weight = new double[nSpecies][];
        cpt = 0;
        for (int i : this.getFocalIndex()) {
            weight[cpt] = cfg.getArrayDouble("population.initialization.weight.sp" + i);
            if (weight[cpt].length != nSize[cpt]) {
                String message = String.format("Parameter %s must contain %d values",
                        "population.initialization.relativebiomass.sp" + i, nSize[cpt]);
                error(message, new Exception());
            }
            cpt++;
        }

    }

    @Override
    public void populate() {

        int nSpecies = this.getNSpecies();
        for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {

            Species species = getConfiguration().getSpecies(iSpecies);
            int nSchool = getConfiguration().getNSchool(iSpecies);

            for (int iLength = 0; iLength < nSize[iSpecies]; iLength++) {

                // Biomass in tons, converted into grams
                double biomass = this.seedingBiomass[iSpecies] * this.biomassProportion[iSpecies][iLength] * 1.0e6;

                // Computes the abundance based on weight ratio.
                double nEgg = biomass / this.weight[iSpecies][iLength];

                // Adding the schools to the system. However, in this case, they are not localized yet.
                // localized.
                if (nEgg == 0.d) {
                    // do nothing, zero school
                } else if (nEgg < nSchool) {
                    School school0 = new School(species, nEgg, (float) this.size[iSpecies][iLength],
                            (float) this.weight[iSpecies][iLength], (int) this.ageDt[iSpecies][iLength]);
                    getSchoolSet().add(school0);
                } else if (nEgg >= nSchool) {
                    for (int s = 0; s < nSchool; s++) {
                        School school0 = new School(species, nEgg / nSchool, (float) this.size[iSpecies][iLength],
                            (float) this.weight[iSpecies][iLength], (int) this.ageDt[iSpecies][iLength]);
                        getSchoolSet().add(school0);
                    }
                }
            }
        }
    }
}
