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
import fr.ird.osmose.School.PreyRecord;
import fr.ird.osmose.stage.AbstractStage;
import fr.ird.osmose.stage.DietOutputStage;
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
public class PredatorPressureIndicator extends SimulationLinker implements Indicator {

    // IO
    private FileOutputStream fos;
    private PrintWriter prw;
    private int recordFrequency;
    //
    private double[][][][] predatorPressure;
    // Diet output stage
    private AbstractStage dietOutputStage;
    /**
     * Whether the indicator should be enabled or not.
     */
    private final boolean enabled;

    public PredatorPressureIndicator(int rank, String keyEnabled) {
        super(rank);
        enabled = getConfiguration().getBoolean(keyEnabled);
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        predatorPressure = new double[nSpec][][][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            int nStage = dietOutputStage.getNStage(iSpec);
            predatorPressure[iSpec] = new double[nStage][][];
            for (int s = 0; s < nStage; s++) {
                predatorPressure[iSpec][s] = new double[nPrey][];
                for (int iPrey = 0; iPrey < nPrey; iPrey++) {
                    if (iPrey < nSpec) {
                        predatorPressure[iSpec][s][iPrey] = new double[dietOutputStage.getNStage(iPrey)];
                    } else {
                        predatorPressure[iSpec][s][iPrey] = new double[1];
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int iSpec = school.getSpeciesIndex();
            int stage = dietOutputStage.getStage(school);
            for (PreyRecord prey : school.getPreyRecords()) {
                predatorPressure[iSpec][stage][prey.getIndex()][prey.getStage()] += prey.getBiomass();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void write(float time) {

        int nSpec = getNSpecies();
        int dtRecord = getConfiguration().getInt("output.recordfrequency.ndt");
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            String name = getSimulation().getSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStagePred = dietOutputStage.getNStage(iSpec);
            for (int iStage = 0; iStage < nStagePred; iStage++) {
                prw.print(time);
                prw.print(';');
                if (nStagePred == 1) {
                    prw.print(name);    // Name predators
                } else {
                    if (iStage == 0) {
                        prw.print(name + " < " + threshold[iStage]);    // Name predators
                    } else {
                        prw.print(name + " >=" + threshold[iStage - 1]);    // Name predators
                    }
                }
                prw.print(";");
                for (int i = 0; i < nSpec; i++) {
                    int nStage = dietOutputStage.getNStage(i);
                    for (int s = 0; s < nStage; s++) {
                        prw.print((float) (predatorPressure[i][s][iSpec][iStage] / dtRecord));
                        if (i < nSpec - 1 || s < nStage - 1) {
                            prw.print(";");
                        }
                    }
                }
                prw.println();
            }
        }
        for (int j = nSpec; j < (nSpec + getConfiguration().getNPlankton()); j++) {
            prw.print(time);
            prw.print(";");
            prw.print(getSimulation().getPlankton(j - nSpec));
            prw.print(";");
            for (int i = 0; i < nSpec; i++) {
                int nStage = dietOutputStage.getNStage(i);
                for (int s = 0; s < nStage; s++) {
                    prw.print((float) (predatorPressure[i][s][j][0] / dtRecord));
                    if (i < nSpec - 1 || s < nStage - 1) {
                        prw.print(";");
                    }
                }
            }
            prw.println();
        }
    }

    @Override
    public void init() {

        // Record frequency
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        // Init diet output stage
        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();

        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_predatorPressure_Simu");
        filename.append(getRank());
        filename.append(".csv");
        File file = new File(path, filename.toString());
        boolean fileExists = file.exists();
        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DietIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);
        if (!fileExists) {
            prw.print("\"");
            prw.print("Biomass of prey species (in tons per time step of saving, in rows) eaten by a predator species (in col). The last column reports the biomass of prey at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)");
            prw.println("\"");
            prw.print("Time");
            prw.print(';');
            prw.print("Prey");
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                String name = getSimulation().getSpecies(iSpec).getName();
                float[] threshold = dietOutputStage.getThresholds(iSpec);
                int nStage = dietOutputStage.getNStage(iSpec);
                for (int iStage = 0; iStage < nStage; iStage++) {
                    prw.print(";");
                    if (nStage == 1) {
                        prw.print(name);    // Name predators
                    } else {
                        if (iStage == 0) {
                            prw.print(name + " < " + threshold[iStage]);    // Name predators
                        } else {
                            prw.print(name + " >=" + threshold[iStage - 1]);    // Name predators
                        }
                    }
                }
            }
            prw.println();
        }
    }

    @Override
    public void close() {
        if (null != prw) {
            prw.close();
        }
        if (null != fos) {
            try {
                fos.close();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }
}
