/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.MortalityCause;
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
     * Age of recruitment (expressed in number of time steps) [SPECIES]
     */
    private int[] recruitmentAge;
    /**
     * Size of recruitment (expressed in centimetre) [SPECIES]
     */
    private float[] recruitmentSize;
    /**
     * CSV separator
     */
    private final String separator;
    
    /** Stage of the schools at the beginning of the time step. */
    private int[] stage_init;

    public MortalityOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
    }

    @Override
    public void initStep() {

        // Reset the nDead array used to compute the mortality rates of current
        // time step
        abundanceStage = new double[getNSpecies()][STAGES];

        stage_init = new int[getSchoolSet().getSchools().size()];

        int cpt = 0;

        // save abundance at the beginning of the time step
        for (School school : getSchoolSet().getSchools()) {
            int stage = getStage(school);
            stage_init[cpt] = stage;
            abundanceStage[school.getSpeciesIndex()][stage] += school.getAbundance();
            cpt += 1;
        }
    }

    @Override
    public void reset() {

        // Reset mortality rates
        mortalityRates = new double[getNSpecies()][MortalityCause.values().length][STAGES];
    }

    @Override
    public void update() {
        int iStage, cpt = 0;
        int nCause = MortalityCause.values().length;
        double[][][] nDead = new double[getNSpecies()][nCause][STAGES];
        for (School school : getSchoolSet().getSchools()) {
            //iStage = getStage(school);
            iStage = stage_init[cpt];
            int iSpecies = school.getSpeciesIndex();
            // Update number of deads
            for (MortalityCause cause : MortalityCause.values()) {
                nDead[iSpecies][cause.index][iStage] += school.getNdead(cause);
            }
            cpt += 1;
        }
        // Cumulate the mortality rates
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            for (iStage = 0; iStage < STAGES; iStage++) {
                if (abundanceStage[iSpecies][iStage] > 0) {
                    double nDeadTot = 0;
                    for (int iDeath = 0; iDeath < nCause; iDeath++) {
                        nDeadTot += nDead[iSpecies][iDeath][iStage];
                    }
                    // total mortality rate for the species/class based on  D = A (1 - e^{-F})
                    double Ftot = Math.log(abundanceStage[iSpecies][iStage] / (abundanceStage[iSpecies][iStage] - nDeadTot));

                    // Mortality rates is just the proportion Fi = Ftot * Di / D, with D the number of dead.
                    if (Ftot != 0) {
                        for (int iDeath = 0; iDeath < nCause; iDeath++) {
                            mortalityRates[iSpecies][iDeath][iStage] += Ftot * nDead[iSpecies][iDeath][iStage] / ((1 - Math.exp(-Ftot)) * abundanceStage[iSpecies][iStage]);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void write(float time) {

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            prw[iSpecies].print(time);
            prw[iSpecies].print(separator);
            for (int iDeath = 0; iDeath < MortalityCause.values().length; iDeath++) {
                for (int iStage = 0; iStage < STAGES; iStage++) {
                    if (iDeath == MortalityCause.ADDITIONAL.index && iStage == EGG) {
                        // instantenous mortality rate for eggs additional mortality 
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
        recruitmentSize = new float[getNSpecies()];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("Mortality");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_mortalityRate-");
            filename.append(getSpecies(iSpecies).getName());
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
                prw[iSpecies].println(quote("Predation (Mpred), Starvation (Mstarv), Additional mortality (Madd), Fishing (F) & Out-of-domain (Zout) mortality rates per time step of saving, except for Madd Eggs that is expressed in osmose time step. Z is the total mortality for migratory fish outside the simulation grid. To get annual mortality rates, sum the mortality rates within one year."));
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
                    prw[iSpecies].print(quote("Madd"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("F"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Zout"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Mfor"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Mdis"));
                }
                for (int i = 0; i < STAGES; i++) {
                    prw[iSpecies].print(separator);
                    prw[iSpecies].print(quote("Mage"));
                }
                prw[iSpecies].println();
                for (int cpt = 0; cpt < MortalityCause.values().length; cpt++) {
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
                recruitmentSize[iSpecies] = -1.f;
            } else if (!getConfiguration().isNull("mortality.fishing.recruitment.size.sp" + iSpecies)) {
                recruitmentSize[iSpecies] = getConfiguration().getFloat("mortality.fishing.recruitment.size.sp" + iSpecies);
                recruitmentAge[iSpecies] = -1;
            } else {
                warning("Could not find parameters mortality.fishing.recruitment.age/size.sp{0}. Osmose assumes it is one year.", new Object[]{iSpecies});
                recruitmentAge[iSpecies] = getConfiguration().getNStepYear();
                recruitmentSize[iSpecies] = -1.f;
                
            }
        }
    }
    
    private int getStage(School school) {
        
        int iStage;
        
        if(this.getConfiguration().isBioenEnabled()) {
    
            if (school.isEgg()) {
                // Eggss
                iStage = EGG;

            } else if (!school.isMature()) {
                // Pre-recruits
                iStage = PRE_RECRUIT;

            } else {
                // Recruits
                iStage = RECRUIT;
            }
            
        } else {
        
            if (school.isEgg()) {
                // Eggss
                iStage = EGG;

            } else if (school.getAgeDt() < recruitmentAge[school.getSpeciesIndex()]
                    || school.getLength() < recruitmentSize[school.getSpeciesIndex()]) {
                // Pre-recruits
                iStage = PRE_RECRUIT;

            } else {
                // Recruits
                iStage = RECRUIT;
            }
        }
        return iStage;
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
