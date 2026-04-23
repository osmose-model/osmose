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
 * Nicolas BARRIER (nicolas.barrier@ird.fr)
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
import fr.ird.osmose.util.timeseries.ByRegimeTimeSeries;
import fr.ird.osmose.util.timeseries.SeasonTimeSeries;

/**
 * Constant larval mortality rate over time.
 *
 * @author P. Verley
 */
public class AnnualLarvaMortality extends AbstractMortalitySpecies {

    // Larval mortality rate expressed in [time_step^-1]
    final private String stage0;
    private double[] mortRate;
    private double[] a;
    private double referenceDensity;

    public AnnualLarvaMortality(int rank, Species species, String stage0) {
        super(rank, species);
        this.stage0 = stage0;
    }

    // Adding constructor for making tests work
    // This mimics the default behavior
    public AnnualLarvaMortality(int rank, Species species) {
        this(rank, species, "larva");
    }

    @Override
    public void init() {

        if (stage0.equals("larva")) {
            // check if the parameter reference density exists
            // if not, set a value to -999
            String keyDens = String.format("mortality.additional.%s.referencedensity.sp%s", stage0, getFileSpeciesIndex());
            if (getConfiguration().isNull(keyDens)) {
                referenceDensity = -999;
            } else {
                referenceDensity = getConfiguration().getDouble(keyDens);
            }
        } else {
            referenceDensity = -999;
        }

        int nStepYear = getConfiguration().getNStepYear();
        // reading base mortality rate
        String keyShift = String.format("mortality.additional.%s.rate.shift.sp%d", this.stage0, getFileSpeciesIndex());
        String keyVal = String.format("mortality.additional.%s.rate.sp%d", this.stage0, getFileSpeciesIndex());
        //String keyValLog = String.format("mortality.additional.larva.rate.log.sp%d", getFileSpeciesIndex());

        //boolean useLog;
        ByRegimeTimeSeries mortRateSeries;
        mortRateSeries = new ByRegimeTimeSeries(keyShift, keyVal);
        mortRateSeries.init();
        double[] mortRateBase = mortRateSeries.getValues();

        // reading multiplier
        double multiplier;
        String keyMul = String.format("mortality.additional.%s.rate.multiplier.sp%d", this.stage0, getFileSpeciesIndex());
        if (getConfiguration().isNull(keyMul)) {
            multiplier = 1;
        } else {
            multiplier = getConfiguration().getDouble(keyMul);
        }

        // reading season
        String keySeason = String.format("mortality.additional.%s.rate.seasonality", this.stage0);
        SeasonTimeSeries season = new SeasonTimeSeries(keySeason, "sp" + getFileSpeciesIndex());
        season.init();
        double[] seasonValues = season.getValues();

        // computing final mortality rate
        mortRate = new double[getConfiguration().getNStep()];
        for(int i = 0; i < getConfiguration().getNStep(); i++) {
            mortRate[i] = multiplier * mortRateBase[i] * seasonValues[i] / nStepYear;
        }

        a = new double[getConfiguration().getNStep()];

        if (referenceDensity < 0) {
            // if reference density does not exist, set the value of a to 1
            for (int i = 0; i < getConfiguration().getNStep(); i++) {
                a[i] = 0;
            }
        } else {
            // if the reference density exits, set the value of a to log(2)/L
            for (int i = 0; i < getConfiguration().getNStep(); i++) {
                a[i] = Math.log(2) / mortRate[i];
            }
        }

    }

    @Override
    public double getRate(School school) {
        int timeIndex = getSimulation().getIndexTimeSimu();
        double correctedMortRate = mortRate[timeIndex] * (1 + a[timeIndex] * getSimulation().getSSB(school) / referenceDensity);
        return correctedMortRate;
    }

    public double[] getRates() {
        return mortRate;
    }
}
