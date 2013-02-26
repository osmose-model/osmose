package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class NaturalMortalityProcess extends AbstractProcess {

    private float[][] larvalMortalityRates;
    private static float[][][] outOfZoneMortality;
    
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

        outOfZoneMortality = new float[getNSpecies()][][];
        for (int index = 0; index < getNSpecies(); index++) {
            int longevity = getSpecies(index).getLongevity();
            outOfZoneMortality[index] = new float[longevity][getOsmose().getNumberTimeStepsPerYear()];
            if (null != getOsmose().migrationTempAge[index]) {
                int nbStepYear = getOsmose().getNumberTimeStepsPerYear();
                for (int m = 0; m < getOsmose().migrationTempAge[index].length; m++) {
                    for (int n = 0; n < getOsmose().migrationTempDt[index].length; n++) {
                        for (int h = 0; h < nbStepYear; h++) {
                            outOfZoneMortality[index][getOsmose().migrationTempAge[index][m] * nbStepYear + h][getOsmose().migrationTempDt[index][n]] = getOsmose().migrationTempMortality[index][m];
                        }
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        // Natural mortality (due to other predators)
        for (School school : getPopulation()) {
            double D = getNaturalMortalityRate(school, 1);
            double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
            school.setAbundance(school.getAbundance() - nDead);
            if (school.getAbundance() < 1.d) {
                //nDead = school.getAbundance();
                school.setAbundance(0.d);
            }
            //school.nDeadNatural = nDead;
        }
    }
    
    private float getOutMortality(School school) {
        return outOfZoneMortality[school.getSpeciesIndex()][school.getAgeDt()][getSimulation().getIndexTimeYear()];
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
            D = (larvalMortalityRates[spec.getIndex()][getSimulation().getIndexTimeSimu()] + getOutMortality(school)) / (float) subdt;
        } else {
            D = (spec.D + getOutMortality(school)) / (float) (getOsmose().getNumberTimeStepsPerYear() * subdt);
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
