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

package fr.ird.osmose.process.bioen;

import fr.ird.osmose.process.mortality.*;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class BioenPredationMortality extends PredationMortality {

    private final OxygenFunction bioen_ingest;

    /**
     * Maximum ingestion rate
     */
    private double[] predationRateBioen;

    /**
     * Maximum ingestion rate use to calcul max ingestion for larvae.
     */
    private double[] larvaePredationRateBioen;

    /**
     * Mean enet rate for larvae.
     */
    private double[] c_rateBioen;

    public BioenPredationMortality(int rank) throws IOException {

        super(rank);

        // Initialisation of the ingestion method
        bioen_ingest = new OxygenFunction(rank);
        bioen_ingest.init();
    }

    @Override
    public void init() {

        // Initialisation of the PredationMortality class.
        super.init();
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
        predationRateBioen = new double[nSpecies];
        larvaePredationRateBioen = new double[nSpecies];
        c_rateBioen = new double[nSpecies];

        // Recovers the max predation rate for bioen config (not the same unit as in
        // the standard code
        int cpt = 0;
        for (int i : this.getConfiguration().getPredatorIndex()) {
            predationRateBioen[cpt] = getConfiguration().getDouble("predation.ingestion.rate.max.bioen.sp" + i);
            larvaePredationRateBioen[cpt] = getConfiguration().getDouble("predation.ingestion.rate.max.larvae.bioen.sp" + i);
            c_rateBioen[cpt] = getConfiguration().getDouble("predation.c.bioen.sp" + i);
            cpt++;
        }

    }

    /**
     * Computes the biomass preyed by predator upon the list of preys. The
     * function considers instantaneous biomass for both preys and predator.
     *
     * @param predator
     * @param preys
     * @param accessibility
     * @param subdt
     * @return the array of biomass preyed by the predator upon the preys
     */
    @Override
    public double[] computePredation(IAggregation predator, List<IAggregation> preys, double[] accessibility, int subdt) {

        double[] preyUpon = new double[preys.size()];
        double cumPreyUpon = 0.d;
        // egg do not predate
        if (predator.getAgeDt() >= predator.getFirstFeedingAgeDt()) {
            // Compute accessible biomass
            // 1. from preys
            double[] accessibleBiomass = new double[preys.size()];
            for (int i = 0; i < preys.size(); i++) {
                accessibleBiomass[i] = accessibility[i] * preys.get(i).getInstantaneousBiomass();
            }

            // this is the biomass which is accessible to all predators
            // in ton.  this should be the P(w) values.
            double biomAccessibleTot = sum(accessibleBiomass);

            // this is the maximum biomass that the predator can ingest.
            // to be modified to fit equation (1).
            // max = predation rate * biomass^beta 
            // maximum biomass that a single fish can eat during the time step subdt
            // barrier.n: weight is converted into g here
            // this is the (Imax * w^beta) variable 
            double maxBiomassToPredate = getMaxPredationRate(predator) * Math.pow(predator.getWeight() * 1e6f, predator.getBetaBioen()) / subdt;

            // multiply the biomass eaten by one fish by the number of fishes to get the maximum biomass that the
            // entire school can eat
            // barrier.n: converted back into ton.
            maxBiomassToPredate *= predator.getInstantaneousAbundance() * 1e-6f;

            // By default the predator will eat as much as it can
            // this is the fP(P(W)) variable.
            double biomassToPredate = maxBiomassToPredate;

            // Distribute the predation over the preys
            if (biomAccessibleTot != 0) {
                // There is less prey available than the predator can
                // potentially prey upon. Predator will feed upon the total
                // accessible biomass
                if (biomAccessibleTot <= biomassToPredate) {
                    // Force fP(P(W)) to Imax * w^beta
                    biomassToPredate = biomAccessibleTot;
                }

                // Assess the loss for the preys caused by this predator
                for (int i = 0; i < preys.size(); i++) {
                    // ratio of prey i (among available preys) preyed upon by predator
                    double ratio = accessibleBiomass[i] / biomAccessibleTot;
                    preyUpon[i] = ratio * biomassToPredate;
                    cumPreyUpon += preyUpon[i];   // biomass in ton.
                }
                // Increments the total ingestion of preys within the system
                predator.incrementIngestion(cumPreyUpon);

            } else {
                // Case 2: there is no prey available
                for (int i = 0; i < preys.size(); i++) {
                    preyUpon[i] = 0;   // no need to do it since initialization already set it to zero?
                }                // Predation success rate is zero for this subdt
            }
        }
        // Return the array of biomass preyed by the predator
        return preyUpon;
    }

    /**
     * Gets the maximum predation rate of a predator per time step.
     *
     * @param predator
     * @return
     */
    @Override
    public double getMaxPredationRate(IAggregation predator) {

        // recovers the species index
        int speciesIndex = predator.getSpeciesIndex();
        
        if(speciesIndex >= this.getNSpecies()) {
            // If species is a background one, return parameter
            // to check with Alaia
            return  predationRateBioen[speciesIndex];
        }

        // recovers the thresshold age (stored on Dt)
        int thresAge = this.getSpecies(speciesIndex).getFirstFeedingAgeDt();

        double factor = 1;

        try {
            factor = (predator.getAgeDt() < thresAge) ? larvaePredationRateBioen[speciesIndex] : 1;
        } catch (NullPointerException ex) {
            String message = "Cannot find larvaePredationRateBioen for species " + speciesIndex;
            error(message, ex);
        }

        double output = 0.d;

        if (predator instanceof School) {
            String key = "imax";
            try {
                output = ((School) predator).existsTrait(key) ? ((School) predator).getTrait(key) : predationRateBioen[speciesIndex];
            } catch (Exception ex) {
                Logger.getLogger(BioenPredationMortality.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            output = predationRateBioen[speciesIndex];
        }

        return ((output + (factor - 1) * c_rateBioen[speciesIndex]) / getConfiguration().getNStepYear());
    }
}
