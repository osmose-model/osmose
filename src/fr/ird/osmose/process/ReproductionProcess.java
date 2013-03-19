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

        int nStepYear = getConfiguration().getNStepYear();
        try {
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();
            if ((lines.size() - 1) % nStepYear != 0) {
                // @TODO throw error
            }
            int nstep = lines.size() - 1;
            int nspecies = getNSpecies();
            seasonSpawning = new double[nspecies][nstep];
            for (int t = 0; t < nstep; t++) {
                String[] line = lines.get(t + 1);
                for (int i = 0; i < nspecies; i++) {
                    seasonSpawning[i][t] = Double.valueOf(line[i + 1]) / 100.d;
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
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
            List<School> schools = getSchoolSet().getSchools(species);
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
