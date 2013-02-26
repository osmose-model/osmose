/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class StarvationProcess extends AbstractProcess {

    private float[] starvMaxRate;
    private float[] criticalPredSuccess;
    
    public StarvationProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {
        starvMaxRate = getOsmose().starvMaxRateMatrix;
        criticalPredSuccess = getOsmose().criticalPredSuccessMatrix;
    }

    @Override
    public void run() {
        for (School school : getPopulation().getPresentSchools()) {
            double nDead = computeStarvationMortality(school, 1);
            school.setAbundance(school.getAbundance() - nDead);
            if (school.getAbundance() < 1.d) {
                school.setAbundance(0.d);
            }
            //school.nDeadStarvation = nDead;
        }
    }

    public double computeStarvationMortality(School school, int subdt) {
        double M = getStarvationMortalityRate(school, subdt);
        return school.getInstantaneousAbundance() * (1 - Math.exp(-M));
    }

    public double getStarvationMortalityRate(School school, int subdt) {

        // no starvation for eggs
        if (school.getAgeDt() == 0) {
            return 0.d;
        }

        int iSpec = school.getSpeciesIndex();
        // Compute the predation mortality rate
        double mortalityRate = 0;
        if (school.predSuccessRate <= criticalPredSuccess[iSpec]) {
            mortalityRate = Math.max(starvMaxRate[iSpec] * (1 - school.predSuccessRate / criticalPredSuccess[iSpec]), 0.d);
        }

        return mortalityRate / (getOsmose().getNumberTimeStepsPerYear() * subdt);
    }
}
