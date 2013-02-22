/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Simulation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author pverley
 */
public class PredationProcess extends AbstractProcess {
    
    private static float[][] predPreySizesMax, predPreySizesMin;
    private static float[] predationRate;

    @Override
    public void init() {
        predPreySizesMax = getOsmose().predPreySizesMaxMatrix;
        predPreySizesMin = getOsmose().predPreySizesMinMatrix;
        predationRate = getOsmose().predationRateMatrix;
    }

    @Override
    public void run() {
        for (Cell cell : getGrid().getCells()) {
                List<School> schools = getPopulation().getSchools(cell);
                Collections.shuffle(schools);
                int ns = schools.size();
                if (!(cell.isLand() || schools.isEmpty())) {
                    double[] nDeadPredation = new double[ns];
                    // Compute predation
                    for (School predator : schools) {
                        double[] preyUpon = computePredation(predator, 1);
                        for (int ipr = 0; ipr < ns; ipr++) {
                            if (ipr < ns) {
                                School prey = schools.get(ipr);
                                nDeadPredation[ipr] += prey.biom2abd(preyUpon[ipr]);
                                prey.nDeadPredation += prey.biom2abd(preyUpon[ipr]);
                            }
                        }
                        predator.preyedBiomass = sum(preyUpon);
                    }
                    // Apply predation mortality
                    for (int is = 0; is < ns; is++) {
                        School school = schools.get(is);
                        school.nDeadPredation = 0;
                        school.predSuccessRate = computePredSuccessRate(school, school.preyedBiomass);
                        school.setAbundance(school.getAbundance() - nDeadPredation[is]);
                        if (school.getAbundance() < 1.d) {
                            school.setAbundance(0.d);
                        }
                    }
                }
            }

    }
    
    public static double[] computePredation(School predator, int subdt) {

        Cell cell = predator.getCell();
        List<School> schools = getPopulation().getSchools(predator.getCell());
        int nFish = schools.size();
        double[] preyUpon = new double[schools.size() + getOsmose().getNumberLTLGroups()];
        // egg do not predate
        if (predator.getAgeDt() == 0) {
            return preyUpon;
        }
        // find the preys
        int[] indexPreys = findPreys(predator);

        // Compute accessible biomass
        // 1. from preys
        double biomAccessibleTot = 0.d;
        for (int iPrey : indexPreys) {
            biomAccessibleTot += getAccessibleBiomass(predator, schools.get(iPrey));
        }
        // 2. from plankton
        float[] percentPlankton = getPercentPlankton(predator);
        for (int i = 0; i < getOsmose().getNumberLTLGroups(); i++) {
            float tempAccess = getOsmose().accessibilityMatrix[getOsmose().getNumberSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
            biomAccessibleTot += percentPlankton[i] * tempAccess * getSimulation().getPlankton(i).getAccessibleBiomass(cell);
        }

        // Compute the potential biomass that predators could prey upon
        double biomassToPredate = computeBiomassToPredate(predator, subdt);
        /*
         * phv 20121219 - this is just a way to stick to what is done in
         * Osmose version SCHOOL2012 and previous version.
         * Tbe biomassToPredate variable of the predator is update on the fly.
         * Should check how it is done in version WS2009 and make sure that it
         * is equivalent to what is done here. It might have some consequences
         * for School.predSuccessRate which influences growth and starvation.
         * phv 20130222 deleted variable school.biomassToPredate
         */
        if (Simulation.VERSION.equals(Simulation.Version.SCHOOL2012_BIOM) || Simulation.VERSION.equals(Simulation.Version.SCHOOL2012_PROD)) {
            //predator.biomassToPredate = biomassToPredate;
        }

        // Distribute the predation over the preys
        if (biomAccessibleTot != 0) {
            // There is less prey available than the predator can
            // potentially prey upon. Predator will feed upon the total
            // accessible biomass
            if (biomAccessibleTot <= biomassToPredate) {
                biomassToPredate = biomAccessibleTot;
            }

            // Assess the loss for the preys caused by this predator
            // Assess the gain for the predator from preys
            for (int iPrey : indexPreys) {
                double ratio = getAccessibleBiomass(predator, schools.get(iPrey)) / biomAccessibleTot;
                preyUpon[iPrey] = ratio * biomassToPredate;
            }
            // Assess the gain for the predator from plankton
            // Assess the loss for the plankton caused by the predator
            for (int i = 0; i < getOsmose().getNumberLTLGroups(); i++) {
                float tempAccess = getOsmose().accessibilityMatrix[getOsmose().getNumberSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
                double ratio = percentPlankton[i] * tempAccess * getSimulation().getPlankton(i).getAccessibleBiomass(cell) / biomAccessibleTot;
                preyUpon[nFish + i] = ratio * biomassToPredate;
            }

        } else {
            // Case 2: there is no prey available
            // No loss !
        }
        return preyUpon;
    }

    private double sum(double[] array) {
        double sum = 0.d;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    public static double[][] computePredationMatrix(Cell cell, int subdt) {

        List<School> schools = getPopulation().getSchools(cell);
        double[][] preyUpon = new double[schools.size() + getOsmose().getNumberLTLGroups()][schools.size() + getOsmose().getNumberLTLGroups()];
        // Loop over the schools of the cell
        for (School school : schools) {
            school.nDeadPredation = 0;
        }
        for (int iPred = 0; iPred < schools.size(); iPred++) {
            preyUpon[iPred] = computePredation(schools.get(iPred), subdt);
        }
        return preyUpon;
    }
    
    /**
     * Compute the rate of predation success.
     * @param biomassToPredate, the max biomass [ton] that a school can prey.
     * @param preyedBiomass, the biomass [ton] effectively preyed.
     * @return 
     */
    public static float computePredSuccessRate(double biomassToPredate, double preyedBiomass) {

        // Compute the predation success rate
        return Math.min((float) (preyedBiomass / biomassToPredate), 1.f);
    }
    
    public static float computePredSuccessRate(School school, double preyedBiomass) {

        // Compute the predation success rate
        double biomassToPredate = computeBiomassToPredate(school, 1);
        return computePredSuccessRate(biomassToPredate, preyedBiomass);
    }
    
    private static float[] getPercentPlankton(School predator) {
        float[] percentPlankton = new float[getOsmose().getNumberLTLGroups()];
        int iPred = predator.getSpeciesIndex();
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][predator.getFeedingStage()];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][predator.getFeedingStage()];
        for (int i = 0; i < getOsmose().getNumberLTLGroups(); i++) {
            if ((preySizeMin > getSimulation().getPlankton(i).getSizeMax()) || (preySizeMax < getSimulation().getPlankton(i).getSizeMin())) {
                percentPlankton[i] = 0.0f;
            } else {
                percentPlankton[i] = getSimulation().getPlankton(i).calculPercent(preySizeMin, preySizeMax);
            }
        }
        return percentPlankton;
    }
    
    /*
     * Get the accessible biomass that predator can feed on prey
     */
    private static double getAccessibleBiomass(School predator, School prey) {
        return getAccessibility(predator, prey) * prey.getInstantaneousBiomass();
    }
    
    /**
     * Returns a list of preys for a given predator.
     *
     * @param predator
     * @return the list of preys for this predator
     */
    private static int[] findPreys(School predator) {

        int iPred = predator.getSpeciesIndex();
        List<School> schoolsInCell = getPopulation().getSchools(predator.getCell());
        //schoolsInCell.remove(predator);
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][predator.getFeedingStage()];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][predator.getFeedingStage()];
        List<Integer> indexPreys = new ArrayList();
        for (int iPrey = 0; iPrey < schoolsInCell.size(); iPrey++) {
            School prey = schoolsInCell.get(iPrey);
            if (prey.equals(predator)) {
                continue;
            }
            if (prey.getLength() >= preySizeMin && prey.getLength() < preySizeMax) {
                indexPreys.add(iPrey);
            }
        }
        int[] index = new int[indexPreys.size()];
        for (int iPrey = 0; iPrey < indexPreys.size(); iPrey++) {
            index[iPrey] = indexPreys.get(iPrey);
        }
        return index;
    }
    
    public static double computeBiomassToPredate(School predator, int subdt) {
        return predator.getInstantaneousBiomass() * predationRate[predator.getSpeciesIndex()] / (double) (getOsmose().getNumberTimeStepsPerYear() * subdt);
    }
    
    /*
     * Get the accessible biomass that predator can feed on prey
     */
    private static double getAccessibility(School predator, School prey) {
        return getOsmose().accessibilityMatrix[prey.getSpeciesIndex()][prey.getAccessibilityStage()][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
    } 
}
