package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class IncomingFluxProcess extends AbstractProcess {

    /**
     * Distribution of the spawning throughout the year
     */
    private double[][] seasonFlux;
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

    public IncomingFluxProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        biomassFluxIn = new double[nSpecies];
        meanLengthIn = new float[nSpecies];
        ageMeanIn = new int[nSpecies];
        readFluxSeason(getConfiguration().getFile("flux.incoming.season.file"));

        for (int i = 0; i < nSpecies; i++) {
            float sum = 0;
            for (double d : seasonFlux[i]) {
                sum += d;
            }
            if (sum > 0) {
                biomassFluxIn[i] = getConfiguration().getFloat("flux.incoming.biomass.sp" + i);
                meanLengthIn[i] = getConfiguration().getFloat("flux.incoming.size.sp" + i);
                ageMeanIn[i] = (int) Math.round(getConfiguration().getFloat("flux.incoming.age.sp" + i) * getConfiguration().getNStepYear());
            }
        }
    }
    
    private void readFluxSeason(String filename) {

        int nStepYear = getConfiguration().getNStepYear();
        try {
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();
            if ((lines.size() - 1) % nStepYear != 0) {
                // @TODO throw error
            }
            int nstep = lines.size() - 1;
            int nspecies = getNSpecies();
            seasonFlux = new double[nspecies][nstep];
            for (int t = 0; t < nstep; t++) {
                String[] line = lines.get(t + 1);
                for (int i = 0; i < nspecies; i++) {
                    seasonFlux[i][t] = Double.valueOf(line[i + 1]) / 100.d;
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
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
        int iStep = seasonFlux[iSpec].length > getConfiguration().getNStepYear()
                ? iStepSimu
                : getSimulation().getIndexTimeYear();
        return seasonFlux[iSpec][iStep];
    }
}
