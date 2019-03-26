/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.process.mortality.*;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author pverley
 */
public class BioenPredationMortality extends PredationMortality {

    private double[] alpha;
    
    private final OxygenFunction bioen_ingest;
    
    /**
     * Maximum ingestion rate
     */
    private double[] predationRateBioen;
    
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
        
        int nBack = this.getNBkgSpecies();
        int nspec = this.getNSpecies();
       
        // Recovers the alpha coefficient for focal + background species
        alpha = new double[nspec + nBack];
        for (int i = 0; i < this.getNSpecies(); i++) {
            String key = String.format("species.alpha.sp%d", i);
            alpha[i] = this.getConfiguration().getDouble(key);
        }

        for (int i = 0; i < nBack; i++) {
            String key = String.format("species.alpha.bkg%d", i);
            alpha[i + nspec] = this.getConfiguration().getDouble(key);
        }      
        
        // Recovers the max predation rate for bioen config (not the same unit as in
        // the standard code
        predationRateBioen = new double[nspec + nBack];

        for (int i = 0; i < nspec; i++) {
            predationRateBioen[i] = getConfiguration().getDouble("predation.ingestion.rate.max.bioen.sp" + i);
        }

        // Recovering predation parameters for background species
        for (int i = 0; i < nBack; i++) {
            predationRateBioen[i + nspec] = getConfiguration().getDouble("predation.ingestion.rate.max.bioen.bkg" + i);
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
        if (predator.getAgeDt() > 0) {
            // Compute accessible biomass
            // 1. from preys
            double[] accessibleBiomass = new double[preys.size()];
            for (int i = 0; i < preys.size(); i++) {
                accessibleBiomass[i] = accessibility[i] * preys.get(i).getInstantaneousBiomass();
            }
            
            // this is the biomass which is accessible to all predators
            // in ton.
            double biomAccessibleTot = sum(accessibleBiomass);

            // this is the maximum biomass that the predator can ingest.
            // to be modified to fit equation (1).
            // max = predation rate * biomass^alpha 
            double fo2 = (predator instanceof School) ? this.bioen_ingest.compute_fO2((School) predator) : 1;
            // barrier.n: @@@@@@@@@@@@@@@@@@@@@@@ fo2 is at this time undefined for bkg species. to see how this can be done later on.
            
            // maximum biomass that a single fish can eat during the time step subdt
            double maxBiomassToPredate = getMaxPredationRate(predator) * Math.pow(predator.getWeight(), alpha[predator.getSpeciesIndex()]) * fo2 / subdt;
            
            // multiply the biomass eaten by one fish by the number of fishes to get the maximum biomass that the
            // entire school can eat
            maxBiomassToPredate *= predator.getAbundance();
            
            // By default the predator will eat as much as it can
            double biomassToPredate = maxBiomassToPredate;

            // Distribute the predation over the preys
            if (biomAccessibleTot != 0) {
                // There is less prey available than the predator can
                // potentially prey upon. Predator will feed upon the total
                // accessible biomass
                if (biomAccessibleTot <= biomassToPredate) {
                    biomassToPredate = biomAccessibleTot;
                }

                // Assess the loss for the preys caused by this predator
                for (int i = 0; i < preys.size(); i++) {
                    // ratio of prey i (among available preys) preyed upon by predator
                    double ratio = accessibleBiomass[i] / biomAccessibleTot;
                    preyUpon[i] = ratio * biomassToPredate;
                    cumPreyUpon += preyUpon[i];
                }
                // Increments the total ingestion of preys within the system
                predator.incrementIngestion(cumPreyUpon);
                
            } else {
                // Case 2: there is no prey available
                for (int i = 0; i < preys.size(); i++) preyUpon[i] = 0;   // no need to do it since initialization already set it to zero?
                // Predation success rate is zero for this subdt
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
        return predationRateBioen[predator.getSpeciesIndex()] / getConfiguration().getNStepYear();
    }

}
