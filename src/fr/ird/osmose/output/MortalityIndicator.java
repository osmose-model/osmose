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

    @Override
    public void init() {
        
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
            nDead[iSpecies][PREDATION][iStage] += school.nDeadPredation;
            nDead[iSpecies][STARVATION][iStage] += school.nDeadStarvation;
            nDead[iSpecies][NATURAL][iStage] += school.nDeadNatural;
            nDead[iSpecies][FISHING][iStage] += school.nDeadFishing;
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
        return !getOsmose().isCalibrationOutput();
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            filename = new StringBuilder("Mortality");
            filename.append(File.separatorChar);
            filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
            filename.append("_mortalityRate_");
            filename.append(getSimulation().getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getOsmose().numSimu);
            filename.append(".csv");
            description = "Predation (Mpred), Starvation (Mstarv), Other Natural mortality (Mnat) & Fishing (F) mortality rates per time step of saving, except for Mnat Eggs that is expressed in osmose time step. To get annual mortality rates, sum the mortality rates within one year.";
            // Write the file
            File file = new File(path, filename.toString());
            file.getParentFile().mkdirs();
            boolean isNew = !file.exists();
            try {
                fos = new FileOutputStream(file, true);
                pr = new PrintWriter(fos, true);
                if (isNew) {
                    pr.print("// ");
                    pr.println(description);
                    pr.print("Time");
                    pr.print(';');
                    pr.print("Mpred;Mpred;Mpred;");
                    pr.print("Mstarv;Mstarv;Mstarv;");
                    pr.print("Mnat;Mnat;Mnat;");
                    pr.println("F;F;F");
                    pr.print(";");
                    for (int iDeath = 0; iDeath < 4; iDeath++) {
                        pr.print("Eggs;Pre-recruits;Recruits;");
                    }
                    pr.println();
                }
                pr.print(time);
                pr.print(";");
                for (int iDeath = 0; iDeath < CAUSES; iDeath++) {
                    for (int iStage = 0; iStage < STAGES; iStage++) {
                        if (iDeath == NATURAL && iStage == EGG) {
                            // instantenous mortality rate for eggs natural mortality 
                            pr.print(mortalityRates[iSpecies][iDeath][iStage] / getOsmose().savingDtMatrix[getOsmose().numSerie]);
                        } else {
                            pr.print(mortalityRates[iSpecies][iDeath][iStage]);
                        }
                        pr.print(";");
                    }
                }
                pr.println();
                pr.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Indicators.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(Indicators.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
