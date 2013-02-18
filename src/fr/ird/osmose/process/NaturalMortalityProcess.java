package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class NaturalMortalityProcess extends AbstractProcess {

    private static float[][] larvalMortalityRates;

    @Override
    public void init() {
        larvalMortalityRates = new float[getOsmose().getNumberSpecies()][getOsmose().getNumberTimeStepsPerYear() * getOsmose().getNumberYears()];
        for (int iSpec = 0; iSpec < getOsmose().getNumberSpecies(); iSpec++) {
            int t = 0;
            for (int iStep = 0; iStep < larvalMortalityRates[iSpec].length; iStep++) {
                if (t > getOsmose().larvalMortalityRates[iSpec].length - 1) {
                    t = 0;
                }
                larvalMortalityRates[iSpec][iStep] = getOsmose().larvalMortalityRates[iSpec][t];
                t++;
            }
        }
    }

    @Override
    public void run() {
        // Natural mortality (due to other predators)
        for (School school : getPopulation()) {
            double nDead = computeNaturalMortality(school, 1);
            school.setAbundance(school.getAbundance() - nDead);
            if (school.getAbundance() < 1.d) {
                //nDead = school.getAbundance();
                school.setAbundance(0.d);
            }
            //school.nDeadNatural = nDead;
        }
    }

    /**
     * For all species, D is due to other predators (seals, seabirds) for
     * migrating species, we add mortality because absents during a time step so
     * they don't undergo mortalities due to predation and starvation Additional
     * mortalities for ages 0: no-fecundation of eggs, starvation more
     * pronounced than for sup ages (rel to CC), predation by other species are
     * not explicit.
     */
    static public double getNaturalMortalityRate(School school, int subdt) {
        double D;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            D = (larvalMortalityRates[spec.getIndex()][getSimulation().getIndexTimeSimu()] + MovementProcess.getOutMortality(school)) / (float) subdt;
        } else {
            D = (spec.D + MovementProcess.getOutMortality(school)) / (float) (getOsmose().getNumberTimeStepsPerYear() * subdt);
        }
        return D;
    }

    static public double computeNaturalMortality(School school, int subdt) {

        double D = getNaturalMortalityRate(school, subdt);
        return school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the larval mortality rates over the years.
     */
    static public double getLarvalMortalityRate(Species species) {

        double rate = 0.d;
        int iSpec = species.getIndex();
        for (int iStep = 0; iStep < larvalMortalityRates[iSpec].length; iStep++) {
            rate += larvalMortalityRates[iSpec][iStep];
        }
        rate /= larvalMortalityRates[iSpec].length;
        return rate;
    }
}
