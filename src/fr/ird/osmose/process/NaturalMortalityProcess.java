package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class NaturalMortalityProcess extends AbstractProcess {

    /**
     * Natural mortality rates year-1.
     */
    private float D[];
    /**
     * Larval mortality rates, timestep-1.
     */
    private float[][] larvalMortalityRates;
    // migration process
    private MigrationProcess migration;

    public NaturalMortalityProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {
        
        D = getConfiguration().D;

        larvalMortalityRates = new float[getNSpecies()][getConfiguration().getNumberTimeStepsPerYear() * getConfiguration().getNYear()];
        for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
            int t = 0;
            for (int iStep = 0; iStep < larvalMortalityRates[iSpec].length; iStep++) {
                if (t > getConfiguration().larvalMortalityRates[iSpec].length - 1) {
                    t = 0;
                }
                larvalMortalityRates[iSpec][iStep] = getConfiguration().larvalMortalityRates[iSpec][t];
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
        double M;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            M = (larvalMortalityRates[spec.getIndex()][getSimulation().getIndexTimeSimu()] + migration.getOutMortality(school)) / (float) subdt;
        } else {
            M = (D[spec.getIndex()] + migration.getOutMortality(school)) / (float) (getConfiguration().getNumberTimeStepsPerYear() * subdt);
        }
        return M;
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
    
    public double getNaturalMortalityRate(Species species) {
        return D[species.getIndex()];
    }
}
