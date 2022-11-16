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

package fr.ird.osmose.output.netcdf;

import fr.ird.osmose.output.distribution.OutputDistribution;

/**
 *
 * @author pverley
 */
public abstract class AbstractMeanDistribOutput_Netcdf extends AbstractDistribOutput_Netcdf {

    // Output values distributed by species and by class
    double[][] denominator;

    public AbstractMeanDistribOutput_Netcdf(int rank, OutputDistribution distrib) {
        super(rank, distrib);
    }

    @Override
    public void reset() {
        super.reset();
        denominator = new double[getNSpecies()][getNClass()];
    }

    @Override
    public void write(float time) {

        int nClass = getNClass();
        double[][] array = new double[nClass][getNSpecies() + 1];
        for (int iClass = 0; iClass < nClass; iClass++) {
            array[iClass][0] = getClassThreshold(iClass);
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                if (denominator[iSpec][iClass] != 0) {
                    array[iClass][iSpec + 1] = values[iSpec][iClass] / denominator[iSpec][iClass];
                } else {
                    array[iClass][iSpec + 1] = Double.NaN;
                }
            }
        }
        writeVariable(time, array);
    }

}
