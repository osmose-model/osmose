/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.Prey.MortalityCause;
import fr.ird.osmose.util.SimulationLinker;
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
public class MortalityOutput extends SimulationLinker implements IOutput {

    // IO
    private FileOutputStream[] fos;
    private PrintWriter[] prw;
    private int recordFrequency;
    /*
     * Mortality rates Stages: 1. eggs & larvae 2. Pre-recruits 3. Recruits
     */
    final private int STAGES = 3;
    final private int EGG = 0;
    final private int PRE_RECRUIT = 1;
    final private int RECRUIT = 2;
    /*
     * Mortality rates array [SPECIES][CAUSES][STAGES]
     */
    private double[][][] mortalityRates;
    /*
     * Abundance per stages [SPECIES][STAGES]
     */
    private double[][] abundanceStage;
    /**
     * Whether the indicator should be enabled or not.
     */
    private final boolean enabled;
    /**
     * Age of recruitment (expressed in number of time steps) [SPECIES]
     */
    private int[] recruitmentAge;
    /**
     * CSV separator
     */
    private final String separator;

    public MortalityOutput(int rank, String keyEnabled) {
        super(rank);
        enabled = getConfiguration().getBoolean(keyEnabled);
        if (!getConfiguration().isNull("output.csv.separator")) {
            separator = getConfiguration().getString("output.csv.separator");
        } else {
            separator = OutputManager.SEPARATOR;
        }
    }

    @Override
    public void initStep() {

        // Reset the nDead array used to compute the mortality rates of current
        // time step
        abundanceStage = new double[getNSpecies()][STAGES];

        // save abundance at the beginning of the time step
        for (School school : getSchoolSet().getAliveSchools()) {
            int iStage;
            if (school.getAgeDt() == 0) {
                // Eggss
                iStage = EGG;
            } else if (school.getAgeDt() < recruitmentAge[school.getSpeciesIndex()]) {
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
        mortalityRates = new double[getNSpecies()][MortalityCause.values().length][STAGES];
    }

    @Override
    public void update() {
        int iStage;
        int nCause = MortalityCause.values().length;
        double[][][] nDead = new double[getNSpecies()][nCause][STAGES];
        for (School school : getSchoolSet().getAliveSchools()) {
            if (school.getAgeDt() == 0) {
                iStage = EGG;
            } else if (school.getAgeDt() < recruitmentAge[school.getSpeciesIndex()]) {
                // Pre-recruits
                iStage = PRE_RECRUIT;
            } else {
                // Recruits
                iStage = RECRUIT;
            }
            int iSpecies = school.getSpeciesIndex();
            // Update number of deads
            for (MortalityCause cause : MortalityCause.values()) {
                nDead[iSpecies][cause.index][iStage] += school.getNdead(cause);
            }
        }
        // Cumulate the mortality rates
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            for (iStage = 0; iStage < STAGES; iStage++) {
                double nDeadTot = 0;
                for (int iDeath = 0; iDeath < nCause; iDeath++) {
                    nDeadTot += nDead[iSpecies][iDeath][iStage];
                }
                double Ftot = Math.log(abundanceStage[iSpecies][iStage] / (abundanceStage[iSpecies][iStage] - nDeadTot));
                for (int iDeath = 0; iDeath < nCause; iDeath++) {
                    mortalityRates[iSpecies][iDeath][iStage] += Ftot * nDead[iSpecies][iDeath][iStage] / ((1 - Math.exp(-Ftot)) * abundanceStage[iSpecies][iStage]);
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void write(float time) {

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            prw[iSpecies].print(time);
            prw[iSpecies].print(separator);
            for (int iDeath = 0; iDeath < MortalityCause.values().length; iDeath++) {
                for (int iStage = 0; iStage < STAGES; iStage++) {
                    if (iDeath == MortalityCause.NATURAL.index && iStage == EGG) {
                        // instantenous mortality rate for eggs natural mortality 
                        prw[iSpecies].print(mortalityRates[iSpecies][iDeath][iStage] / recordFrequency);
                    } else {
                        prw[iSpecies].print(mortalityRates[iSpecies][iDeath][iStage]);
                    }
                    prw[iSpecies].print(separator);
                }
            }
            prw[iSpecies].println();
        }
    }

    @Override
    public void init() {

        // Record frequency
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        fos = new FileOutputStream[getNSpecies()];
        prw = new PrintWriter[getNSpecies()];
        recruitmentAge = new int[getNSpecies()];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("Mortality");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_mortalityRate-");
            filename.append(getSimulation().getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getRank());
            filename.append(".csv");
            File file = new File(path, filename.toString());
            boolean fileExists = file.exists();
            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos[iSpecies] = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MortalityOutput.class.getName()).log(Level.SEVERE, null, ex);
            }
            prw[iSpecies] = new PrintWriter(fos[iSpecies], true);
            if (!fileExists) {
                // Write headers
                prw[iSpecies].println(quote("Predation (Mpred), Starvation (Mstarv), Other Natural mortality (Mnat), Fishing (F) & Out-of-domain (Z) mortality rates per time step of saving, except for Mnat Eggs that is expressed in osmose time step. Z is the total mortality for migratory fish outside the simulation grid. To get annual mortality rates, sum the mortality rates within one year."));
                prw[iSpecies].print(quote("Time"));
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Mpred"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Mstarv"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Mnat"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("F"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Z"));
                }
                prw[iSpecies].println();
                for (MortalityCause cause : MortalityCause.values()) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print("Eggs");
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print("Pre-recruits");
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print("Recruits");
                }
                prw[iSpecies].println();
            }

            // Get the age of recruitment
            if (!getConfiguration().isNull("mortality.fishing.recruitment.age.sp" + iSpecies)) {
                float age = getConfiguration().getFloat("mortality.fishing.recruitment.age.sp" + iSpecies);
                recruitmentAge[iSpecies] = Math.round(age * getConfiguration().getNStepYear());
            } else if (!getConfiguration().isNull("mortality.fishing.recruitment.size.sp" + iSpecies)) {
                float recruitmentSize = getConfiguration().getFloat("mortality.fishing.recruitment.size.sp" + iSpecies);
                recruitmentAge[iSpecies] = getSpecies(iSpecies).computeMeanAge(recruitmentSize);
            } else {
                getSimulation().warning("Could not find parameters mortality.fishing.recruitment.age/size.sp{0}. Osmose assumes it is one year.", new Object[]{iSpecies});
                recruitmentAge[iSpecies] = getConfiguration().getNStepYear();
            }
        }
    }
    
    private String quote(String str) {
        return "\"" + str + "\"";
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
                    // do nothing
                }
            }
        }
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }
}
