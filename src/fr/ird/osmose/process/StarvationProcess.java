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

    public StarvationProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {
        int nspec = getNSpecies();
        starvMaxRate = new float[nspec];
        criticalPredSuccess = new float[nspec];
        for (int i = 0; i < nspec; i++) {
            starvMaxRate[i] = getConfiguration().getFloat("mortality.starvation.rate.max.sp" + i);
            criticalPredSuccess[i] = getConfiguration().getFloat("predation.efficiency.critical.sp" + i);
        }
    }

    @Override
    public void run() {
        for (School school : getPopulation().getPresentSchools()) {
            double M = getStarvationMortalityRate(school, 1);
            double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-M));
            if (nDead > 0.d) {
                school.setNdeadStarvation(nDead);
            }
        }
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

        return mortalityRate / (getConfiguration().getNStepYear() * subdt);
    }
}
