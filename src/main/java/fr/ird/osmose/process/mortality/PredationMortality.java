/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import fr.ird.osmose.stage.IStage;
import fr.ird.osmose.stage.PredPreyStage;
import fr.ird.osmose.util.Matrix;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author pverley
 */
public class PredationMortality extends AbstractMortality {

    /**
     * Predator/prey size ratio
     */
    private HashMap<Integer, double[]> predPreySizesMax, predPreySizesMin;
    /**
     * Maximum ingestion rate
     */
    private HashMap<Integer, Double> predationRate;

    /*
     * Feeding stages
     */
    private IStage predPreyStage;

    private PredationAccessibility predationAccess;

    public PredationMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        predationAccess = new PredationAccessibility(this.getRank(), "predation.accessibility", "acc");
        predationAccess.init();

        int nsp = getNSpecies();
        int nrsc = getConfiguration().getNRscSpecies();
        int nbkg = getConfiguration().getNBkgSpecies();
        predPreySizesMax = new HashMap();
        predPreySizesMin = new HashMap();
        predationRate = new HashMap();

        for (int i : getConfiguration().getFishIndex()) {
            predPreySizesMax.put(i, getConfiguration().getArrayDouble("predation.predPrey.sizeRatio.max.sp" + i));
            predPreySizesMin.put(i, getConfiguration().getArrayDouble("predation.predPrey.sizeRatio.min.sp" + i));
            if (!getConfiguration().isBioenEnabled()) {
                predationRate.put(i, getConfiguration().getDouble("predation.ingestion.rate.max.sp" + i));
            }
        }

        // Feeding stages (i.e. classes for Lmin/LMax ratios
        predPreyStage = new PredPreyStage();
        predPreyStage.init();

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
            double biomAccessibleTot = sum(accessibleBiomass);

            // Compute the maximum biomass that the predator could prey upon
            double maxBiomassToPredate = getMaxPredationRate(predator) * predator.getInstantaneousBiomass() / subdt;
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
                // Update predation success rate
                // The predation success rate at the end of the time step is the
                // average of the predation success rate for every subdt
                float success = computePredSuccessRate(maxBiomassToPredate, cumPreyUpon);
                predator.incrementPredSuccessRate(success / subdt);
            } else {
                // Case 2: there is no prey available
                // preyUpon[i] = 0; no need to do it since initialization already set it to zero
                // Predation success rate is zero for this subdt
            }
        }
        // Return the array of biomass preyed by the predator
        return preyUpon;
    }

    protected double sum(double[] array) {
        double sum = 0.d;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    /**
     * Compute the rate of predation success.
     *
     * @param biomassToPredate, the max biomass [ton] that a school can prey.
     * @param preyedBiomass, the biomass [ton] effectively preyed.
     * @return
     */
    public float computePredSuccessRate(double biomassToPredate, double preyedBiomass) {

        // Compute the predation success rate
        return Math.min((float) (preyedBiomass / biomassToPredate), 1.f);
    }

    private HashMap<Integer, Double> getPercentResource(IAggregation predator) {
        HashMap<Integer, Double> percentResource = new HashMap();
        int iPred = predator.getSpeciesIndex();
        int iStage = predPreyStage.getStage(predator);
        double preySizeMax = predator.getLength() / predPreySizesMax.get(iPred)[iStage];
        double preySizeMin = predator.getLength() / predPreySizesMin.get(iPred)[iStage];
        for (int i : getConfiguration().getRscIndex()) {
            if ((preySizeMin > getConfiguration().getResourceSpecies(i).getSizeMax()) || (preySizeMax < getConfiguration().getResourceSpecies(i).getSizeMin())) {
                percentResource.put(i, 0.0d);
            } else {
                percentResource.put(i, getConfiguration().getResourceSpecies(i).computePercent(preySizeMin, preySizeMax));
            }
        }
        return percentResource;
    }

    /**
     * Gets the maximum predation rate of a predator per time step
     *
     * @param predator
     * @return
     */
    public double getMaxPredationRate(IAggregation predator) {
        if (getConfiguration().isBioenEnabled()) {
            error("The getMaxPredationRate method of PredationMortality not suitable in Osmose-PHYSIO", new Exception());
        }
        return predationRate.get(predator.getSpeciesIndex()) / getConfiguration().getNStepYear();
    }

    /**
     * Get the accessibility of a list of preys for a given predator. Zero means
     * that the prey is not accessible to this predator. Accessibility ranges
     * from zero to one.
     *
     * @param predator, the predator in a cell
     * @param preys a list of preys that are in the same cell that the predator
     * @return an array of accessibility of the preys to this predator.
     */
    public double[] getAccessibility(IAggregation predator, List<IAggregation> preys) {

        AccessMatrix accessibilityMatrix = (AccessMatrix) predationAccess.getAccessMatrix();
        int iAccessPred = accessibilityMatrix.getIndexPred(predator);

        double[] accessibility = new double[preys.size()];
        int iSpecPred = predator.getSpeciesIndex();
        int iPredPreyStage = predPreyStage.getStage(predator);
        double preySizeMax = predator.getLength() / predPreySizesMax.get(iSpecPred)[iPredPreyStage];
        double preySizeMin = predator.getLength() / predPreySizesMin.get(iSpecPred)[iPredPreyStage];
        HashMap<Integer, Double> percentResource = getPercentResource(predator);

        for (int iPrey = 0; iPrey < preys.size(); iPrey++) {
            int iSpecPrey = preys.get(iPrey).getSpeciesIndex();
            IAggregation prey = (IAggregation) preys.get(iPrey);
            int iAccessPrey = accessibilityMatrix.getIndexPrey(prey);
            // The prey is an other school
            if (preys.get(iPrey) instanceof AbstractSchool) {
                if (prey.equals(predator)) {
                    continue;
                }
                if (prey.getLength() >= preySizeMin && prey.getLength() < preySizeMax) {
                    accessibility[iPrey] = accessibilityMatrix.getValue(iAccessPrey, iAccessPred);
                } else {
                    accessibility[iPrey] = 0.d; //no need to do it since initialization already set it to zero
                }
            } else {
                // The prey is a resource group
                accessibility[iPrey] = accessibilityMatrix.getValue(iAccessPrey, iAccessPred)
                        * percentResource.get(iSpecPrey);
            }
        }
        return accessibility;
    }

    @Override
    public double getRate(School school) {
        throw new UnsupportedOperationException("Predation mortality is handled explicitly in Osmose.");
    }
}
