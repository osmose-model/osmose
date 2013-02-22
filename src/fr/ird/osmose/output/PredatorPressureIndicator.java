/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.SimulationLinker;
import fr.ird.osmose.Species;
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

    @Override
    public void initStep() {
        for (School school : getPopulation().getPresentSchools()) {
            biomassStage[school.getSpeciesIndex()][school.dietOutputStage] += school.getBiomass();
        }
        int nSpec = getNSpecies();
        int nPrey = nSpec + getOsmose().getNumberLTLGroups();
        for (int i = nSpec; i < nPrey; i++) {
            int iPlankton = i - nSpec;
            biomassStage[i][0] += getSimulation().getPlankton(iPlankton).getBiomass();
        }
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getOsmose().getNumberLTLGroups();
        predatorPressure = new double[nSpec][][][];
        biomassStage = new double[nPrey][];
        for (int i = 0; i < nSpec; i++) {
            biomassStage[i] = new double[getSimulation().getSpecies(i).nbDietStages];
            predatorPressure[i] = new double[getSimulation().getSpecies(i).nbDietStages][][];
            for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                predatorPressure[i][s] = new double[nPrey][];
                for (int ipr = 0; ipr < nPrey; ipr++) {
                    if (ipr < nSpec) {
                        predatorPressure[i][s][ipr] = new double[getSimulation().getSpecies(ipr).nbDietStages];
                    } else {
                        predatorPressure[i][s][ipr] = new double[1];
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
            for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                    predatorPressure[iSpec][school.dietOutputStage][i][s] += school.diet[i][s];
                }
            }
            for (int i = getOsmose().getNumberSpecies(); i < getOsmose().getNumberSpecies() + getOsmose().getNumberLTLGroups(); i++) {
                predatorPressure[iSpec][school.dietOutputStage][i][0] += school.diet[i][0];
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isDietOuput();
    }

    @Override
    public void write(float time) {

        int nSpec = getNSpecies();
        int dtRecord = getOsmose().getRecordFrequency();
        for (int j = 0; j < nSpec; j++) {
            Species species = getSimulation().getSpecies(j);
            for (int st = 0; st < species.nbDietStages; st++) {
                prw.print(time);
                prw.print(';');
                if (species.nbDietStages == 1) {
                    prw.print(species.getName());    // Name predators
                } else {
                    if (st == 0) {
                        prw.print(species.getName() + " < " + species.dietStagesTab[st]);    // Name predators
                    } else {
                        prw.print(species.getName() + " >" + species.dietStagesTab[st - 1]);    // Name predators
                    }
                }
                prw.print(";");
                for (int i = 0; i < nSpec; i++) {
                    for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                        prw.print((float) (predatorPressure[i][s][j][st] / dtRecord));
                        prw.print(";");
                    }
                }
                prw.print((float) (biomassStage[j][st] / dtRecord));
                prw.println();
            }
        }
        for (int j = nSpec; j < (nSpec + getOsmose().getNumberLTLGroups()); j++) {
            prw.print(time);
            prw.print(";");
            prw.print(getSimulation().getPlankton(j - nSpec));
            prw.print(";");
            for (int i = 0; i < nSpec; i++) {
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
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
        // Create parent directory
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab);
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix);
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
        for (int i = 0; i < getNSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            for (int s = 0; s < species.nbDietStages; s++) {
                prw.print(";");
                if (species.nbDietStages == 1) {
                    prw.print(species.getName());    // Name predators
                } else {
                    if (s == 0) {
                        prw.print(species.getName() + " < " + species.dietStagesTab[s]);    // Name predators
                    } else {
                        prw.print(species.getName() + " >" + species.dietStagesTab[s - 1]);    // Name predators
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
