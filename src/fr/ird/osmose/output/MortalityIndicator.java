/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.SimulationLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class MortalityIndicator extends SimulationLinker implements Indicator {

    // IO
    private FileOutputStream[] fos;
    private PrintWriter[] prw;
    /*
     * Mortality rates Stages: 1. eggs & larvae 2. Pre-recruits 3. Recruits
     */
    final private int STAGES = 3;
    final private int EGG = 0;
    final private int PRE_RECRUIT = 1;
    final private int RECRUIT = 2;
    /*
     * Mortality causes: 1. predation 2. starvation 3. natural 4. fishing
     */
    final private int CAUSES = 4;
    final private int PREDATION = 0;
    final private int STARVATION = 1;
    final private int NATURAL = 2;
    final private int FISHING = 3;
    /*
     * Mortality rates array [SPECIES][CAUSES][STAGES]
     */
    private double[][][] mortalityRates;
    /*
     * Abundance per stages [SPECIES][STAGES]
     */
    private double[][] abundanceStage;
    
     public MortalityIndicator(int replica) {
        super(replica);
    }

    @Override
    public void initStep() {

        // Reset the nDead array used to compute the mortality rates of current
        // time step
        abundanceStage = new double[getNSpecies()][STAGES];

        // save abundance at the beginning of the time step
        for (School school : getPopulation().getAliveSchools()) {
            int iStage;
            if (school.getAgeDt() == 0) {
                // Eggss
                iStage = EGG;
            } else if (school.getAgeDt() < school.getSpecies().recruitAge) {
                // Pre-recruits
                iStage = PRE_RECRUIT;
            } else {
                // Recruits
                iStage = RECRUIT;
            }
            abundanceStage[school.getSpeciesIndex()][iStage] += school.getAbundance();
        }
    }

    @Override
    public void reset() {

        // Reset mortality rates
        mortalityRates = new double[getNSpecies()][CAUSES][STAGES];
    }

    @Override
    public void update() {
        int iStage;
        double[][][] nDead = new double[getNSpecies()][CAUSES][STAGES];
        for (School school : getPopulation().getAliveSchools()) {
            if (school.getAgeDt() == 0) {
                iStage = EGG;
            } else if (school.getAgeDt() < school.getSpecies().recruitAge) {
                // Pre-recruits
                iStage = PRE_RECRUIT;
            } else {
                // Recruits
                iStage = RECRUIT;
            }
            int iSpecies = school.getSpeciesIndex();
            // Update number of deads
            nDead[iSpecies][PREDATION][iStage] += school.getNdeadPredation();
            nDead[iSpecies][STARVATION][iStage] += school.getNdeadStarvation();
            nDead[iSpecies][NATURAL][iStage] += school.getNdeadNatural();
            nDead[iSpecies][FISHING][iStage] += school.getNdeadFishing();
        }
        // Cumulate the mortality rates
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            for (iStage = 0; iStage < STAGES; iStage++) {
                double nDeadTot = 0;
                for (int iDeath = 0; iDeath < CAUSES; iDeath++) {
                    nDeadTot += nDead[iSpecies][iDeath][iStage];
                }
                double Ftot = Math.log(abundanceStage[iSpecies][iStage] / (abundanceStage[iSpecies][iStage] - nDeadTot));
                for (int iDeath = 0; iDeath < CAUSES; iDeath++) {
                    mortalityRates[iSpecies][iDeath][iStage] += Ftot * nDead[iSpecies][iDeath][iStage] / ((1 - Math.exp(-Ftot)) * abundanceStage[iSpecies][iStage]);
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return !getConfiguration().isCalibrationOutput();
    }

    @Override
    public void write(float time) {

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            prw[iSpecies].print(time);
            prw[iSpecies].print(";");
            for (int iDeath = 0; iDeath < CAUSES; iDeath++) {
                for (int iStage = 0; iStage < STAGES; iStage++) {
                    if (iDeath == NATURAL && iStage == EGG) {
                        // instantenous mortality rate for eggs natural mortality 
                        prw[iSpecies].print(mortalityRates[iSpecies][iDeath][iStage] / getConfiguration().savingDtMatrix);
                    } else {
                        prw[iSpecies].print(mortalityRates[iSpecies][iDeath][iStage]);
                    }
                    prw[iSpecies].print(";");
                }
            }
            prw[iSpecies].println();
        }
    }

    @Override
    public void init() {

        fos = new FileOutputStream[getNSpecies()];
        prw = new PrintWriter[getNSpecies()];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().outputPathName + getConfiguration().outputFileNameTab);
            StringBuilder filename = new StringBuilder("Mortality");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().outputPrefix);
            filename.append("_mortalityRate-");
            filename.append(getSimulation().getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getSimulation().getReplica());
            filename.append(".csv");
            File file = new File(path, filename.toString());
            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos[iSpecies] = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MortalityIndicator.class.getName()).log(Level.SEVERE, null, ex);
            }
            prw[iSpecies] = new PrintWriter(fos[iSpecies], true);
            // Write headers
            prw[iSpecies].print("\"");
            prw[iSpecies].print("Predation (Mpred), Starvation (Mstarv), Other Natural mortality (Mnat) & Fishing (F) mortality rates per time step of saving, except for Mnat Eggs that is expressed in osmose time step. To get annual mortality rates, sum the mortality rates within one year.");
            prw[iSpecies].println("\"");
            prw[iSpecies].print("Time");
            prw[iSpecies].print(';');
            prw[iSpecies].print("Mpred;Mpred;Mpred;");
            prw[iSpecies].print("Mstarv;Mstarv;Mstarv;");
            prw[iSpecies].print("Mnat;Mnat;Mnat;");
            prw[iSpecies].println("F;F;F");
            prw[iSpecies].print(";");
            for (int iDeath = 0; iDeath < 4; iDeath++) {
                prw[iSpecies].print("Eggs;Pre-recruits;Recruits;");
            }
            prw[iSpecies].println();
        }
    }

    @Override
    public void close() {
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            if (null != prw) {
                prw[iSpecies].close();
            }
            if (null != fos) {
                try {
                    fos[iSpecies].close();
                } catch (IOException ex) {
                    Logger.getLogger(MortalityIndicator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
