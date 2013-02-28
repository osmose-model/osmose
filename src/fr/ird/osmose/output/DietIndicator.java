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
public class DietIndicator extends SimulationLinker implements Indicator {

    // IO
    private FileOutputStream fos;
    private PrintWriter prw;
    //
    private double[][][][] diet;
    private double[][] nbStomachs;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private double[][] biomassStage;
    
     public DietIndicator(int replica) {
        super(replica);
    }

    @Override
    public void initStep() {
        for (School school : getPopulation().getPresentSchools()) {
            biomassStage[school.getSpeciesIndex()][school.getDietOutputStage()] += school.getBiomass();
        }
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNumberLTLGroups();
        for (int i = nSpec; i < nPrey; i++) {
            int iPlankton = i - nSpec;
            biomassStage[i][0] += getSimulation().getPlankton(iPlankton).getBiomass();
        }
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNumberLTLGroups();
        diet = new double[nSpec][][][];
        nbStomachs = new double[nSpec][];
        biomassStage = new double[nPrey][];
        for (int i = 0; i < nSpec; i++) {
            biomassStage[i] = new double[getSimulation().getSpecies(i).nbDietStages];
            diet[i] = new double[getSimulation().getSpecies(i).nbDietStages][][];
            nbStomachs[i] = new double[getSimulation().getSpecies(i).nbDietStages];
            for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                diet[i][s] = new double[nPrey][];

                for (int ipr = 0; ipr < nPrey; ipr++) {
                    if (ipr < nSpec) {
                        diet[i][s][ipr] = new double[getSimulation().getSpecies(ipr).nbDietStages];
                    } else {
                        diet[i][s][ipr] = new double[1];
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
            double sumDiet = computeSumDiet(school);
            int iSpec = school.getSpeciesIndex();
            nbStomachs[iSpec][school.getDietOutputStage()] += school.getAbundance();
            for (int i = 0; i < getConfiguration().getNumberSpecies(); i++) {
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                    if (sumDiet > 0) {
                        diet[iSpec][school.getDietOutputStage()][i][s] += school.getAbundance() * school.diet[i][s] / sumDiet;
                    }
                }
            }
            for (int i = getConfiguration().getNumberSpecies(); i < getConfiguration().getNumberSpecies() + getConfiguration().getNumberLTLGroups(); i++) {
                if (sumDiet > 0) {
                    diet[iSpec][school.getDietOutputStage()][i][0] += school.getAbundance() * school.diet[i][0] / sumDiet;
                }
            }
        }
    }

    private double computeSumDiet(School school) {
        double sumDiet = 0.d;
        for (int i = 0; i < getConfiguration().getNumberSpecies(); i++) {
            for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                sumDiet += school.diet[i][s];
            }
        }
        for (int i = getConfiguration().getNumberSpecies(); i < getConfiguration().getNumberSpecies() + getConfiguration().getNumberLTLGroups(); i++) {
            sumDiet += school.diet[i][0];
        }
        return sumDiet;
    }

    @Override
    public boolean isEnabled() {
        return getConfiguration().isDietOuput();
    }

    @Override
    public void write(float time) {

        int nSpec = getConfiguration().getNumberSpecies();

        // Write the step in the file
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
                        if (nbStomachs[i][s] >= 1) {
                            prw.print((float) (diet[i][s][j][st] / nbStomachs[i][s]));
                        } else {
                            prw.print("NaN");
                        }
                        prw.print(";");
                    }
                }
                prw.println();
            }
        }
        for (int j = nSpec; j < (nSpec + getConfiguration().getNumberLTLGroups()); j++) {
            prw.print(time);
            prw.print(";");
            prw.print(getSimulation().getPlankton(j - nSpec));
            prw.print(";");
            for (int i = 0; i < nSpec; i++) {
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                    if (nbStomachs[i][s] >= 1) {
                        prw.print((float) (diet[i][s][j][0] / nbStomachs[i][s]));
                    } else {
                        prw.print("NaN");
                    }
                    prw.print(";");
                }
            }
            prw.println();
        }
    }

    @Override
    public void init() {
        // Create parent directory
        File path = new File(getConfiguration().outputPathName + getConfiguration().outputFileNameTab);
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().outputPrefix);
        filename.append("_dietMatrix_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        File file = new File(path, filename.toString());
        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AbstractIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);
        prw.print("\"");
        prw.print("% of prey species (in rows) in the diet of predator species (in col)");
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
