/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
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
public class PredationIndicator extends SchoolBasedIndicator {

    private double[][][][] diet, predatorPressure;
    private double[][] nbStomachs;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private double[][] biomassStage;

    @Override
    public void init() {
        for (School school : getPopulation().getPresentSchools()) {
            biomassStage[school.getSpeciesIndex()][school.dietOutputStage] += school.getBiomass();
        }
        int nSpec = getNSpecies();
        int nPrey = nSpec + getForcing().getNbPlanktonGroups();
        for (int i = nSpec; i < nPrey; i++) {
            int iPlankton = i - nSpec;
            biomassStage[i][0] += getForcing().getBiomass(iPlankton);
        }
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getForcing().getNbPlanktonGroups();
        diet = new double[nSpec][][][];
        predatorPressure = new double[nSpec][][][];
        nbStomachs = new double[nSpec][];
        biomassStage = new double[nPrey][];
        for (int i = 0; i < nSpec; i++) {
            biomassStage[i] = new double[getSimulation().getSpecies(i).nbDietStages];
            diet[i] = new double[getSimulation().getSpecies(i).nbDietStages][][];
            predatorPressure[i] = new double[getSimulation().getSpecies(i).nbDietStages][][];
            nbStomachs[i] = new double[getSimulation().getSpecies(i).nbDietStages];
            for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                diet[i][s] = new double[nPrey][];
                predatorPressure[i][s] = new double[nPrey][];
                for (int ipr = 0; ipr < nPrey; ipr++) {
                    if (ipr < nSpec) {
                        diet[i][s][ipr] = new double[getSimulation().getSpecies(ipr).nbDietStages];
                        predatorPressure[i][s][ipr] = new double[getSimulation().getSpecies(ipr).nbDietStages];
                    } else {
                        diet[i][s][ipr] = new double[1];
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
    public void update(School school) {
        double sumDiet = computeSumDiet(school);
        int iSpec = school.getSpeciesIndex();
        nbStomachs[iSpec][school.dietOutputStage] += school.getAbundance();
        for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
            for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                predatorPressure[iSpec][school.dietOutputStage][i][s] += school.diet[i][s];
                if (sumDiet > 0) {
                    diet[iSpec][school.dietOutputStage][i][s] += school.getAbundance() * school.diet[i][s] / sumDiet;
                }
            }
        }
        for (int i = getSimulation().getNumberSpecies(); i < getSimulation().getNumberSpecies() + getForcing().getNbPlanktonGroups(); i++) {
            predatorPressure[iSpec][school.dietOutputStage][i][0] += school.diet[i][0];
            if (sumDiet > 0) {
                diet[iSpec][school.dietOutputStage][i][0] += school.getAbundance() * school.diet[i][0] / sumDiet;
            }
        }
    }
    
    private double computeSumDiet(School school) {
        double sumDiet = 0.d;
        for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                    sumDiet += school.diet[i][s];
                }
            }
            for (int i = getSimulation().getNumberSpecies(); i < getSimulation().getNumberSpecies() + getForcing().getNbPlanktonGroups(); i++) {
                sumDiet += school.diet[i][0];
            }
        return sumDiet;
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isDietOuput();
    }

    @Override
    public void write(float time) {
        writeDiet(time);
        writePredatorPressure(time);
    }

    public void writeDiet(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab);
        int nSpec = getSimulation().getNumberSpecies();

        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix);
        filename.append("_dietMatrix_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        description = "% of prey species (in rows) in the diet of predator species (in col)";
        // Write the file
        File file = new File(path, filename.toString());
        file.getParentFile().mkdirs();
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                pr.print("\"");
                pr.print(description);
                pr.println("\"");
                pr.print("Time");
                pr.print(';');
                pr.print("Prey");
                for (int i = 0; i < nSpec; i++) {
                    Species species = getSimulation().getSpecies(i);
                    for (int s = 0; s < species.nbDietStages; s++) {
                        pr.print(";");
                        if (species.nbDietStages == 1) {
                            pr.print(species.getName());    // Name predators
                        } else {
                            if (s == 0) {
                                pr.print(species.getName() + " < " + species.dietStagesTab[s]);    // Name predators
                            } else {
                                pr.print(species.getName() + " >" + species.dietStagesTab[s - 1]);    // Name predators
                            }
                        }
                    }
                }
                pr.println();
            }
            for (int j = 0; j < nSpec; j++) {
                Species species = getSimulation().getSpecies(j);
                for (int st = 0; st < species.nbDietStages; st++) {
                    pr.print(time);
                    pr.print(';');
                    if (species.nbDietStages == 1) {
                        pr.print(species.getName());    // Name predators
                    } else {
                        if (st == 0) {
                            pr.print(species.getName() + " < " + species.dietStagesTab[st]);    // Name predators
                        } else {
                            pr.print(species.getName() + " >" + species.dietStagesTab[st - 1]);    // Name predators
                        }
                    }
                    pr.print(";");
                    for (int i = 0; i < nSpec; i++) {
                        for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                            if (nbStomachs[i][s] >= 1) {
                                pr.print((float) (diet[i][s][j][st] / nbStomachs[i][s]));
                            } else {
                                pr.print("NaN");
                            }
                            pr.print(";");
                        }
                    }
                    pr.println();
                }
            }
            for (int j = nSpec; j < (nSpec + getForcing().getNbPlanktonGroups()); j++) {
                pr.print(time);
                pr.print(";");
                pr.print(getForcing().getPlanktonName(j - nSpec));
                pr.print(";");
                for (int i = 0; i < nSpec; i++) {
                    for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                        if (nbStomachs[i][s] >= 1) {
                            pr.print((float) (diet[i][s][j][0] / nbStomachs[i][s]));
                        } else {
                            pr.print("NaN");
                        }
                        pr.print(";");
                    }
                }
                pr.println();
            }
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

    public void writePredatorPressure(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab);
        int nSpec = getSimulation().getNumberSpecies();
        int dtRecord = getOsmose().getRecordFrequency();

        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix);
        filename.append("_predatorPressure_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        description = "Biomass of prey species (in tons per time step of saving, in rows) eaten by a predator species (in col). The last column reports the biomass of prey at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)";
        // Write the file
        File file = new File(path, filename.toString());
        file.getParentFile().mkdirs();
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                pr.print("\"");
                pr.print(description);
                pr.println("\"");
                pr.print("Time");
                pr.print(';');
                pr.print("Prey");
                for (int i = 0; i < nSpec; i++) {
                    Species species = getSimulation().getSpecies(i);
                    for (int s = 0; s < species.nbDietStages; s++) {
                        pr.print(";");
                        if (species.nbDietStages == 1) {
                            pr.print(species.getName());    // Name predators
                        } else {
                            if (s == 0) {
                                pr.print(species.getName() + " < " + species.dietStagesTab[s]);    // Name predators
                            } else {
                                pr.print(species.getName() + " >" + species.dietStagesTab[s - 1]);    // Name predators
                            }
                        }
                    }
                }
                pr.print(";");
                pr.print("Biomass");
                pr.println();
            }
            for (int j = 0; j < nSpec; j++) {
                Species species = getSimulation().getSpecies(j);
                for (int st = 0; st < species.nbDietStages; st++) {
                    pr.print(time);
                    pr.print(';');
                    if (species.nbDietStages == 1) {
                        pr.print(species.getName());    // Name predators
                    } else {
                        if (st == 0) {
                            pr.print(species.getName() + " < " + species.dietStagesTab[st]);    // Name predators
                        } else {
                            pr.print(species.getName() + " >" + species.dietStagesTab[st - 1]);    // Name predators
                        }
                    }
                    pr.print(";");
                    for (int i = 0; i < nSpec; i++) {
                        for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                            pr.print((float) (predatorPressure[i][s][j][st] / dtRecord));
                            pr.print(";");
                        }
                    }
                    pr.print((float) (biomassStage[j][st] / dtRecord));
                    pr.println();
                }
            }
            for (int j = nSpec; j < (nSpec + getForcing().getNbPlanktonGroups()); j++) {
                pr.print(time);
                pr.print(";");
                pr.print(getForcing().getPlanktonName(j - nSpec));
                pr.print(";");
                for (int i = 0; i < nSpec; i++) {
                    for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                        pr.print((float) (predatorPressure[i][s][j][0] / dtRecord));
                        pr.print(";");
                    }
                }
                pr.print(biomassStage[j][0] / dtRecord);
                pr.println();
            }
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
