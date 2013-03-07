/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
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
    //
    private double[][][][] predatorPressure;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private double[][] biomassStage;
    /**
     * Number of diet stages.
     */
    private int[] nDietStage;
    /**
     * Threshold age (year) or size (cm) between the diet stages.
     */
    private float[][] dietStageThreshold;
    
     public PredatorPressureIndicator(int replica) {
        super(replica);
    }

    @Override
    public void initStep() {
        for (School school : getPopulation().getPresentSchools()) {
            biomassStage[school.getSpeciesIndex()][school.getDietOutputStage()] += school.getBiomass();
        }
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        for (int i = nSpec; i < nPrey; i++) {
            int iPlankton = i - nSpec;
            biomassStage[i][0] += getSimulation().getPlankton(iPlankton).getBiomass();
        }
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        predatorPressure = new double[nSpec][][][];
        biomassStage = new double[nPrey][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            biomassStage[iSpec] = new double[nDietStage[iSpec]];
            predatorPressure[iSpec] = new double[nDietStage[iSpec]][][];
            for (int s = 0; s < nDietStage[iSpec]; s++) {
                predatorPressure[iSpec][s] = new double[nPrey][];
                for (int ipr = 0; ipr < nPrey; ipr++) {
                    if (ipr < nSpec) {
                        predatorPressure[iSpec][s][ipr] = new double[nDietStage[ipr]];
                    } else {
                        predatorPressure[iSpec][s][ipr] = new double[1];
                    }
                }
            }
        }
        for (int i = nSpec; i < nPrey; i++) {
            // we consider just 1 stage per plankton group
            biomassStage[i] = new double[1];
        }
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            int iSpec = school.getSpeciesIndex();
            for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
                for (int s = 0; s < nDietStage[i]; s++) {
                    predatorPressure[iSpec][school.getDietOutputStage()][i][s] += school.diet[i][s];
                }
            }
            for (int i = getConfiguration().getNSpecies(); i < getConfiguration().getNSpecies() + getConfiguration().getNPlankton(); i++) {
                predatorPressure[iSpec][school.getDietOutputStage()][i][0] += school.diet[i][0];
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return getConfiguration().isDietOuput();
    }

    @Override
    public void write(float time) {

        int nSpec = getNSpecies();
        int dtRecord = getConfiguration().getRecordFrequency();
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            Species species = getSimulation().getSpecies(iSpec);
            for (int iStage = 0; iStage < nDietStage[iSpec]; iStage++) {
                prw.print(time);
                prw.print(';');
                if (nDietStage[iSpec] == 1) {
                    prw.print(species.getName());    // Name predators
                } else {
                    if (iStage == 0) {
                        prw.print(species.getName() + " < " + dietStageThreshold[iSpec][iStage]);    // Name predators
                    } else {
                        prw.print(species.getName() + " >" + dietStageThreshold[iSpec][iStage - 1]);    // Name predators
                    }
                }
                prw.print(";");
                for (int i = 0; i < nSpec; i++) {
                    for (int s = 0; s < nDietStage[i]; s++) {
                        prw.print((float) (predatorPressure[i][s][iSpec][iStage] / dtRecord));
                        prw.print(";");
                    }
                }
                prw.print((float) (biomassStage[iSpec][iStage] / dtRecord));
                prw.println();
            }
        }
        for (int j = nSpec; j < (nSpec + getConfiguration().getNPlankton()); j++) {
            prw.print(time);
            prw.print(";");
            prw.print(getSimulation().getPlankton(j - nSpec));
            prw.print(";");
            for (int i = 0; i < nSpec; i++) {
                for (int s = 0; s < nDietStage[i]; s++) {
                    prw.print((float) (predatorPressure[i][s][j][0] / dtRecord));
                    prw.print(";");
                }
            }
            prw.print(biomassStage[j][0] / dtRecord);
            prw.println();
        }
    }

    @Override
    public void init() {
        
        // Read diet stages
        nDietStage = getConfiguration().nDietStage;
        dietStageThreshold = getConfiguration().dietStageThreshold;
        
        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname() + getConfiguration().getOutputFolder());
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getOutputPrefix());
        filename.append("_predatorPressure_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        File file = new File(path, filename.toString());
        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DietIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);
        prw.print("\"");
        prw.print("Biomass of prey species (in tons per time step of saving, in rows) eaten by a predator species (in col). The last column reports the biomass of prey at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)");
        prw.println("\"");
        prw.print("Time");
        prw.print(';');
        prw.print("Prey");
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSimulation().getSpecies(iSpec);
            for (int s = 0; s < nDietStage[iSpec]; s++) {
                prw.print(";");
                if (nDietStage[iSpec] == 1) {
                    prw.print(species.getName());    // Name predators
                } else {
                    if (s == 0) {
                        prw.print(species.getName() + " < " + dietStageThreshold[iSpec][s]);    // Name predators
                    } else {
                        prw.print(species.getName() + " >" + dietStageThreshold[iSpec][s - 1]);    // Name predators
                    }
                }
            }
        }
        prw.print(";");
        prw.print("Biomass");
        prw.println();
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
                Logger.getLogger(DietIndicator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
