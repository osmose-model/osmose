package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class IncomingFluxProcess extends AbstractProcess {

    /**
     * Distribution of the spawning throughout the year
     */
    private double[][] seasonSpawning;
    /*
     * Annual flux of incoming biomass in tons
     */
    private double[] biomassFluxIn;
    /*
     * Mean length of incomimg fish
     */
    private float[] meanLengthIn;
    /*
     * Mean weight of incoming fish
     */
    private int[] ageMeanIn;

    public IncomingFluxProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        seasonSpawning = new double[nSpecies][];
        biomassFluxIn = new double[nSpecies];
        meanLengthIn = new float[nSpecies];
        ageMeanIn = new int[nSpecies];

        for (int i = 0; i < nSpecies; i++) {
            seasonSpawning[i] = getConfiguration().getArrayDouble("flux.incoming.season.sp" + i);
            float sum = 0;
            for (double d : seasonSpawning[i]) {
                sum += d;
            }
            if (sum > 0) {
                biomassFluxIn[i] = getConfiguration().getFloat("flux.incoming.biomass.sp" + i);
                meanLengthIn[i] = getConfiguration().getFloat("flux.incoming.size.sp" + i);
                ageMeanIn[i] = (int) Math.round(getConfiguration().getFloat("flux.incoming.age.sp" + i) * getConfiguration().getNumberTimeStepsPerYear());
            }
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (biomassFluxIn[i] == 0.d) {
                continue;
            }
            Species species = getSpecies(i);
            /*
             * Incoming flux
             */
            double season = getSeason(getSimulation().getIndexTimeSimu(), species);
            double biomassIn = biomassFluxIn[i] * season;
            float meanWeigthIn = (float) species.computeWeight(meanLengthIn[i]);
            long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
            int nSchool = getConfiguration().getSeed();
            if (abundanceIn > 0 && abundanceIn < nSchool) {
                getPopulation().add(new School(species, abundanceIn, meanLengthIn[i], meanWeigthIn, ageMeanIn[i]));
            } else if (abundanceIn >= nSchool) {
                int mod = (int) (abundanceIn % nSchool);
                int abdSchool = (int) (abundanceIn / nSchool);
                for (int s = 0; s < nSchool; s++) {
                    abdSchool += (s < mod) ? 1 : 0;
                    getPopulation().add(new School(species, abdSchool, meanLengthIn[i], meanWeigthIn, ageMeanIn[i]));
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
