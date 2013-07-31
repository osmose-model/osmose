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
        if (!getConfiguration().isNull("flux.incoming.season.file")) {
            readFluxSeason(getConfiguration().getFile("flux.incoming.season.file"));
        } else {
            seasonFlux = new double[nSpecies][0];
        }

        for (int i = 0; i < nSpecies; i++) {
            double sum = 0;
            for (double d : seasonFlux[i]) {
                sum += d;
            }
            if (sum > 0.d) {
                biomassFluxIn[i] = getConfiguration().getFloat("flux.incoming.annual.biomass.sp" + i);
                meanLengthIn[i] = getConfiguration().getFloat("flux.incoming.size.sp" + i);
                ageMeanIn[i] = (int) Math.round(getConfiguration().getFloat("flux.incoming.age.sp" + i) * getConfiguration().getNStepYear());
            }
        }
    }

    private void readFluxSeason(String filename) {

        try {
            // 1. Open the CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();

            // 2. Read the seasonality
            int nTimeSerie = lines.size() - 1;
            int nStepYear = getConfiguration().getNStepYear();
            int nStepSimu = getConfiguration().getNYear() * nStepYear;
            int nspecies = getNSpecies();
            seasonFlux = new double[nspecies][nStepSimu];
            for (int t = 0; t < nTimeSerie; t++) {
                String[] line = lines.get(t + 1);
                for (int i = 0; i < nspecies; i++) {
                    seasonFlux[i][t] = Double.valueOf(line[i + 1]);
                }
            }

            // 3. Check the length of the time serie
            if (nTimeSerie % nStepYear != 0) {
                // Either the time serie is less than a year or it is not a 
                // multiple of number of time step per year.
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must be a multiple of the number of time steps per year.");
            } else if (nTimeSerie < nStepSimu) {
                // There is less season in the file than number of years of the
                // simulation.
                int t = nTimeSerie;
                while (t < nStepSimu) {
                    for (int k = 0; k < nTimeSerie; k++) {
                        for (int i = 0; i < nspecies; i++) {
                            seasonFlux[i][t] = seasonFlux[i][k];
                        }
                        t++;
                        if (t == nStepSimu) {
                            break;
                        }
                    }
                }
                getLogger().log(Level.WARNING, "Time serie in file {0} only contains {1} steps out of {2}. Osmose will loop over it.", new Object[]{filename, nTimeSerie, nStepSimu});
            } else if (nTimeSerie > nStepSimu) {
                getLogger().log(Level.WARNING, "Time serie in file {0} contains {1} steps out of {2}. Osmose will ignore the exceeding years.", new Object[]{filename, nTimeSerie, nStepSimu});
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading CSV file " + filename, ex);
            System.exit(1);
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
            int nSchool = getConfiguration().getSeed(i);
            if (abundanceIn > 0 && abundanceIn < nSchool) {
                getSchoolSet().add(new School(species, abundanceIn, meanLengthIn[i], meanWeigthIn, ageMeanIn[i]));
            } else if (abundanceIn >= nSchool) {
                int mod = (int) (abundanceIn % nSchool);
                int abdSchool = (int) (abundanceIn / nSchool);
                for (int s = 0; s < nSchool; s++) {
                    abdSchool += (s < mod) ? 1 : 0;
                    getSchoolSet().add(new School(species, abdSchool, meanLengthIn[i], meanWeigthIn, ageMeanIn[i]));
                }
            }
        }
    }

    private double getSeason(int iStepSimu, Species species) {
        int iSpec = species.getIndex();
        return seasonFlux[iSpec][iStepSimu];
    }
}
