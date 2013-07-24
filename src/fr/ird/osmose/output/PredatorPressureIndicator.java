/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
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
    private boolean enabled;

    public PredatorPressureIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation);
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
            for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
                int nStage = dietOutputStage.getNStage(i);
                for (int s = 0; s < nStage; s++) {
                    predatorPressure[iSpec][dietOutputStage.getStage(school)][i][s] += school.diet[i][s];
                }
            }
            for (int i = getConfiguration().getNSpecies(); i < getConfiguration().getNSpecies() + getConfiguration().getNPlankton(); i++) {
                predatorPressure[iSpec][dietOutputStage.getStage(school)][i][0] += school.diet[i][0];
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
        filename.append(getSimulation().getReplica());
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
                getLogger().log(Level.SEVERE, "Error closing output file PredatorPressure", ex);
            }
        }
    }
    
    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }
}
