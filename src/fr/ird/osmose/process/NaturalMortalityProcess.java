package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class NaturalMortalityProcess extends AbstractProcess {

    private float[][] larvalMortalityRates;
    // migration process
    private MigrationProcess migration;

    public NaturalMortalityProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        larvalMortalityRates = new float[getNSpecies()][getOsmose().getNumberTimeStepsPerYear() * getOsmose().getNumberYears()];
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
        
        // Migration
        migration = new MigrationProcess(getReplica());
    }

    @Override
    public void run() {
        // Natural mortality (due to other predators)
        for (School school : getPopulation()) {
            double D = getNaturalMortalityRate(school, 1);
            double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
            if (nDead > 0.d) {
                school.setNdeadNatural(nDead);
            }
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
    public double getNaturalMortalityRate(School school, int subdt) {
        double D;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            D = (larvalMortalityRates[spec.getIndex()][getSimulation().getIndexTimeSimu()] + migration.getOutMortality(school)) / (float) subdt;
        } else {
            D = (spec.D + migration.getOutMortality(school)) / (float) (getOsmose().getNumberTimeStepsPerYear() * subdt);
        }
        return D;
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the larval mortality rates over the years.
     */
    public double getLarvalMortalityRate(Species species) {

        double rate = 0.d;
        int iSpec = species.getIndex();
        for (int iStep = 0; iStep < larvalMortalityRates[iSpec].length; iStep++) {
            rate += larvalMortalityRates[iSpec][iStep];
        }
        rate /= larvalMortalityRates[iSpec].length;
        return rate;
    }
}
