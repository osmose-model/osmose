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

package fr.ird.osmose.process.mortality.additional;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.AbstractMortalitySpecies;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

/**
 * Larva mortality varies over time. Osmose can handle values for 1. one year 2.
 * for a cycle of k years (with k smaller than nyear), in that case Osmose will
 * loop over the cycle until the end of the simulation ; 3. nyear, for a full
 * inter annual parametrization.
 *
 * @author P. Verley
 */
public class ByDtLarvaMortality extends AbstractMortalitySpecies {

    // Larval mortality rate expressed in [year^-1]
    private double[] mortRate, a;
    private double referenceDensity;

    public ByDtLarvaMortality(int rank, Species species) {
        super(rank, species);
    }

    @Override
    public void init() {

        // check if the parameter reference density exists
        // if not, set a value to -999
        String keyDens = String.format("mortality.additional.larva.referencedensity.sp%s", getFileSpeciesIndex());
        if (getConfiguration().isNull(keyDens)) {
            referenceDensity = -999;
        } else {
            referenceDensity = getConfiguration().getDouble(keyDens);
        }

        SingleTimeSeries ts = new SingleTimeSeries();
        ts.read(getConfiguration().getFile("mortality.additional.larva.rate.bytDt.file.sp" + getFileSpeciesIndex()));
        mortRate = ts.getValues();

        // convert the larva mortality from year-1 to byDt-1
        for (int t = 0; t < getConfiguration().getNStep(); t++) {
            mortRate[t] /= getConfiguration().getNStepYear();
        }

        a = new double[getConfiguration().getNStep()];

        if (referenceDensity < 0) {
            // if reference density does not exist, set the value of a to 1
            for (int t = 0; t < getConfiguration().getNStep(); t++) {
                a[t] = 0;
            }
        } else {
            // if the reference density exits, set the value of a to log(2)/L
            for (int t = 0; t < getConfiguration().getNStep(); t++) {
                a[t] = Math.log(2) / mortRate[t];
            }
        }
    }

    @Override
    public double getRate(School school) {
        int timeIndex = getSimulation().getIndexTimeSimu();
        double correctedMortRate = mortRate[timeIndex] * (1 + a[timeIndex] * getSimulation().getSSB(school) / referenceDensity);
        return correctedMortRate;
    }
}
