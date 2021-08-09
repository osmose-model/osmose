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
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import fr.ird.osmose.stage.ClassGetter;
import fr.ird.osmose.stage.IStage;
import fr.ird.osmose.stage.PredPreyStage;
import fr.ird.osmose.util.AccessibilityManager;
import fr.ird.osmose.util.Matrix;
import java.util.List;

/**
 *
 * @author pverley
 */
public class PredationMortality extends AbstractMortality {

    /**
     * Predator/prey size ratio
     */
    private double[][] predPreySizesMax, predPreySizesMin;
    /**
     * Maximum ingestion rate
     */
    private double[] predationRate;

    /*
     * Feeding stages
     */
    private IStage predPreyStage;

    private AccessibilityManager predationAccess;

    public PredationMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        String prefix = "predation.accessibility";

        Configuration conf = this.getConfiguration();
        String metrics = conf.getString(prefix + ".stage.structure");

        if (!(metrics.equalsIgnoreCase("size") || metrics.equalsIgnoreCase("age"))) {
            metrics = null;
        }

        if (metrics == null) {
            String message = String.format("The %s parameter must either be \"age\" or \"size\". ",
                    prefix + ".stage.structure");
            error(message, new IllegalArgumentException());
        }

        ClassGetter classGetter = null;

        if (metrics.equalsIgnoreCase("size")) {
            classGetter = (school -> school.getLength());
        } else if (metrics.equalsIgnoreCase("age")) {
            classGetter = (school -> school.getAge());
        }

        predationAccess = new AccessibilityManager(this.getRank(), "predation.accessibility", "acc", classGetter);
        predationAccess.init();

        int nSpecies = getConfiguration().getNSpecies();
        int nBackground = getConfiguration().getNBkgSpecies();
        predPreySizesMax = new double[nSpecies + nBackground][];
        predPreySizesMin = new double[nSpecies + nBackground][];
        predationRate = new double[nSpecies + nBackground];

        int cpt = 0;
        for (int fileSpeciesIndex : getConfiguration().getPredatorIndex()) {
            predPreySizesMax[cpt] = getConfiguration()
                    .getArrayDouble("predation.predPrey.sizeRatio.max.sp" + fileSpeciesIndex);
            predPreySizesMin[cpt] = getConfiguration()
                    .getArrayDouble("predation.predPrey.sizeRatio.min.sp" + fileSpeciesIndex);
            if (!getConfiguration().isBioenEnabled()) {
                predationRate[cpt] = getConfiguration().getDouble("predation.ingestion.rate.max.sp" + fileSpeciesIndex);
            }
            cpt++;
        }

        // Feeding stages (i.e. classes for Lmin/LMax ratios
        predPreyStage = new PredPreyStage();
        predPreyStage.init();

    }

    /**
     * Computes the biomass preyed by predator upon the list of preys. The function
     * considers instantaneous biomass for both preys and predator.
     *
     * @param predator
     * @param preys
     * @param accessibility
     * @param subdt
     * @return the array of biomass preyed by the predator upon the preys
     */
    public double[] computePredation(IAggregation predator, List<IAggregation> preys, double[] accessibility,
            int subdt) {

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
     * @param preyedBiomass,    the biomass [ton] effectively preyed.
     * @return
     */
    public float computePredSuccessRate(double biomassToPredate, double preyedBiomass) {

        // Compute the predation success rate
        return Math.min((float) (preyedBiomass / biomassToPredate), 1.f);
    }

    private double[] getPercentResource(IAggregation predator) {
        double[] percentResource = new double[this.getConfiguration().getNRscSpecies()];
        int iPred = predator.getSpeciesIndex();
        int iStage = predPreyStage.getStage(predator);
        double preySizeMax = predator.getLength() / predPreySizesMax[iPred][iStage];
        double preySizeMin = predator.getLength() / predPreySizesMin[iPred][iStage];
        for (int resourceIndex = 0; resourceIndex < this.getConfiguration().getNRscSpecies(); resourceIndex++) {
            if ((preySizeMin > getConfiguration().getResourceSpecies(resourceIndex).getSizeMax())
                    || (preySizeMax < getConfiguration().getResourceSpecies(resourceIndex).getSizeMin())) {
                percentResource[resourceIndex] = 0.0d;
            } else {
                percentResource[resourceIndex] = getConfiguration().getResourceSpecies(resourceIndex)
                        .computePercent(preySizeMin, preySizeMax);
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
            error("The getMaxPredationRate method of PredationMortality not suitable in Osmose-PHYSIO",
                    new Exception());
        }
        return predationRate[predator.getSpeciesIndex()] / getConfiguration().getNStepYear();
    }

    /**
     * Get the accessibility of a list of preys for a given predator. Zero means
     * that the prey is not accessible to this predator. Accessibility ranges from
     * zero to one.
     *
     * @param predator, the predator in a cell
     * @param preys     a list of preys that are in the same cell that the predator
     * @return an array of accessibility of the preys to this predator.
     */
    public double[] getAccessibility(IAggregation predator, List<IAggregation> preys) {

        int year = this.getSimulation().getYear();
        int season = this.getSimulation().getIndexTimeYear();
        
        Matrix accessibilityMatrix = predationAccess.getMatrix(year, season);
        int iAccessPred = accessibilityMatrix.getIndexPred(predator);

        // Number of predators species. Used to offeset resource percentage index
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();

        double[] accessibility = new double[preys.size()];
        int iSpecPred = predator.getSpeciesIndex();
        int iPredPreyStage = predPreyStage.getStage(predator);
        double preySizeMax = predator.getLength() / predPreySizesMax[iSpecPred][iPredPreyStage];
        double preySizeMin = predator.getLength() / predPreySizesMin[iSpecPred][iPredPreyStage];
        double[] percentResource = getPercentResource(predator);

        for (int iPrey = 0; iPrey < preys.size(); iPrey++) {
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
                    accessibility[iPrey] = 0.d; // no need to do it since initialization already set it to zero
                }
            } else {
                int iSpecPrey = preys.get(iPrey).getSpeciesIndex(); // get species index with offset
                // The prey is a resource group
                accessibility[iPrey] = accessibilityMatrix.getValue(iAccessPrey, iAccessPred)
                        * percentResource[iSpecPrey - nSpecies];
            }
        }
        return accessibility;
    }

    @Override
    public double getRate(School school) {
        throw new UnsupportedOperationException("Predation mortality is handled explicitly in Osmose.");
    }
}
