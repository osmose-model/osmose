/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.util.List;

/**
 *
 * @author pverley
 */
public class ReproductionProcess extends AbstractProcess {

    /**
     * Distribution of the spawning throughout the year
     */
    private double[][] seasonSpawning;
    /*
     * Percentage of female in the population
     */
    private double[] sexRatio;
    /*
     * Number of eggs per gram of mature female
     */
    private double[] alpha;

    public ReproductionProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        seasonSpawning = new double[nSpecies][];
        sexRatio = new double[nSpecies];
        alpha = new double[nSpecies];

        for (int i = 0; i < nSpecies; i++) {
            seasonSpawning[i] = getConfiguration().getArrayDouble("reproduction.season.sp" + i);
            float sum = 0;
            for (double d : seasonSpawning[i]) {
                sum += d;
            }
            if (sum > 0) {
                sexRatio[i] = getConfiguration().getDouble("species.sexratio.sp" + i);
                alpha[i] = getConfiguration().getDouble("species.relativefecundity.sp" + i);
                for (int t = 0; t < seasonSpawning[i].length; t++) {
                    seasonSpawning[i][t] = seasonSpawning[i][t] / 100.f;
                }
            }
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (sexRatio[i] == 0.d || alpha[i] == 0.d) {
                continue;
            }
            Species species = getSpecies(i);
            double SSB = 0;
            List<School> schools = getPopulation().getSchools(species);
            for (School school : schools) {
                if (school.getLength() >= species.getSizeMaturity()) {
                    SSB += school.getInstantaneousBiomass();
                }
            }

            double season = getSeason(getSimulation().getIndexTimeSimu(), species);
            double nEgg = sexRatio[i] * alpha[i] * season * SSB * 1000000;

            /*
             * Making cohorts going up to the upper age class
             */
            for (School school : schools) {
                school.incrementAge();
            }

            //UPDATE AGE CLASS 0
            int nSchool = getConfiguration().getSeed();
            if (nEgg == 0.d) {
                // do nothing, zero school
            } else if (nEgg < nSchool) {
                School school0 = new School(species, nEgg);
                getPopulation().add(school0);
            } else if (nEgg >= nSchool) {
                for (int s = 0; s < nSchool; s++) {
                    School school0 = new School(species, nEgg / nSchool);
                    getPopulation().add(school0);
                }
            }
        }
    }

    private double getSeason(int iStepSimu, Species species) {
        int iSpec = species.getIndex();
        int iStep = seasonSpawning[iSpec].length > getConfiguration().getNumberTimeStepsPerYear()
                ? iStepSimu
                : getSimulation().getIndexTimeYear();
        return seasonSpawning[iSpec][iStep];
    }
}
