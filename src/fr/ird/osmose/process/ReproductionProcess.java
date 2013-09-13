/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

    public ReproductionProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        int nSpecies = getNSpecies();
        sexRatio = new double[nSpecies];
        alpha = new double[nSpecies];
        readSpawningSeason(getConfiguration().getFile("reproduction.season.file"));

        for (int i = 0; i < nSpecies; i++) {
            float sum = 0;
            for (double d : seasonSpawning[i]) {
                sum += d;
            }
            if (sum > 0) {
                sexRatio[i] = getConfiguration().getDouble("species.sexratio.sp" + i);
                alpha[i] = getConfiguration().getDouble("species.relativefecundity.sp" + i);
            }
        }
    }

    private void readSpawningSeason(String filename) {

        try {
            // 1. Open the CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();

            // 2. Read the reproduction seasonality
            int nTimeSerie = lines.size() - 1;
            int nStepYear = getConfiguration().getNStepYear();
            int nStepSimu = getConfiguration().getNYear() * nStepYear;
            int nspecies = getNSpecies();
            seasonSpawning = new double[nspecies][nStepSimu];
            for (int t = 0; t < nTimeSerie; t++) {
                String[] line = lines.get(t + 1);
                for (int i = 0; i < nspecies; i++) {
                    seasonSpawning[i][t] = Double.valueOf(line[i + 1]);
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
                            seasonSpawning[i][t] = seasonSpawning[i][k];
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
            double nEgg = 0.d;
            Species species = getSpecies(i);
            List<School> schools = getSchoolSet().getSchools(species);
            if (sexRatio[i] > 0.d && alpha[i] > 0.d) {
                double SSB = 0;
                for (School school : schools) {
                    if (school.getLength() >= species.getSizeMaturity()) {
                        SSB += school.getInstantaneousBiomass();
                    }
                }
                double season = getSeason(getSimulation().getIndexTimeSimu(), species);
                nEgg = sexRatio[i] * alpha[i] * season * SSB * 1000000;
            }

            /*
             * Making cohorts going up to the upper age class
             */
            for (School school : schools) {
                school.incrementAge();
            }

            //UPDATE AGE CLASS 0
            int nSchool = getConfiguration().getSeed(i);
            if (nEgg == 0.d) {
                // do nothing, zero school
            } else if (nEgg < nSchool) {
                School school0 = new School(species, nEgg);
                getSchoolSet().add(school0);
            } else if (nEgg >= nSchool) {
                for (int s = 0; s < nSchool; s++) {
                    School school0 = new School(species, nEgg / nSchool);
                    getSchoolSet().add(school0);
                }
            }
        }
    }

    private double getSeason(int iStepSimu, Species species) {
        int iSpec = species.getIndex();
        int iStep = seasonSpawning[iSpec].length > getConfiguration().getNStepYear()
                ? iStepSimu
                : getSimulation().getIndexTimeYear();
        return seasonSpawning[iSpec][iStep];
    }
}
