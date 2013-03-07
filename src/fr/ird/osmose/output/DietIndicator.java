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
    /**
     * Number of diet stages.
     */
    private int[] nDietStage;
    /**
     * Threshold age (year) or size (cm) between the diet stages.
     */
    private float[][] dietStageThreshold;
    
     public DietIndicator(int replica) {
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
        diet = new double[nSpec][][][];
        nbStomachs = new double[nSpec][];
        biomassStage = new double[nPrey][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            biomassStage[iSpec] = new double[nDietStage[iSpec]];
            diet[iSpec] = new double[nDietStage[iSpec]][][];
            nbStomachs[iSpec] = new double[nDietStage[iSpec]];
            for (int iStage = 0; iStage < nDietStage[iSpec]; iStage++) {
                diet[iSpec][iStage] = new double[nPrey][];

                for (int iPrey = 0; iPrey < nPrey; iPrey++) {
                    if (iPrey < nSpec) {
                        diet[iSpec][iStage][iPrey] = new double[nDietStage[iPrey]];
                    } else {
                        diet[iSpec][iStage][iPrey] = new double[1];
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
            for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
                for (int s = 0; s < nDietStage[i]; s++) {
                    if (sumDiet > 0) {
                        diet[iSpec][school.getDietOutputStage()][i][s] += school.getAbundance() * school.diet[i][s] / sumDiet;
                    }
                }
            }
            for (int i = getConfiguration().getNSpecies(); i < getConfiguration().getNSpecies() + getConfiguration().getNPlankton(); i++) {
                if (sumDiet > 0) {
                    diet[iSpec][school.getDietOutputStage()][i][0] += school.getAbundance() * school.diet[i][0] / sumDiet;
                }
            }
        }
    }

    private double computeSumDiet(School school) {
        double sumDiet = 0.d;
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            for (int s = 0; s < nDietStage[i]; s++) {
                sumDiet += school.diet[i][s];
            }
        }
        for (int i = getConfiguration().getNSpecies(); i < getConfiguration().getNSpecies() + getConfiguration().getNPlankton(); i++) {
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

        int nSpec = getConfiguration().getNSpecies();

        // Write the step in the file
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            Species species = getSimulation().getSpecies(iSpec);
            for (int st = 0; st < nDietStage[iSpec]; st++) {
                prw.print(time);
                prw.print(';');
                if (nDietStage[iSpec] == 1) {
                    prw.print(species.getName());    // Name predators
                } else {
                    if (st == 0) {
                        prw.print(species.getName() + " < " + dietStageThreshold[iSpec][st]);    // Name predators
                    } else {
                        prw.print(species.getName() + " >" + dietStageThreshold[iSpec][st - 1]);    // Name predators
                    }
                }
                prw.print(";");
                for (int i = 0; i < nSpec; i++) {
                    for (int s = 0; s < nDietStage[i]; s++) {
                        if (nbStomachs[i][s] >= 1) {
                            prw.print((float) (diet[i][s][iSpec][st] / nbStomachs[i][s]));
                        } else {
                            prw.print("NaN");
                        }
                        prw.print(";");
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
                for (int s = 0; s < nDietStage[i]; s++) {
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
        
        // Read diet stages
        nDietStage = getConfiguration().nDietStage;
        dietStageThreshold = getConfiguration().dietStageThreshold;
        
        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname() + getConfiguration().getOutputFolder());
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getOutputPrefix());
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
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSimulation().getSpecies(iSpec);
            for (int iStage = 0; iStage < nDietStage[iSpec]; iStage++) {
                prw.print(";");
                if (nDietStage[iSpec] == 1) {
                    prw.print(species.getName());    // Name predators
                } else {
                    if (iStage == 0) {
                        prw.print(species.getName() + " < " + dietStageThreshold[iSpec][iStage]);    // Name predators
                    } else {
                        prw.print(species.getName() + " >" + dietStageThreshold[iSpec][iStage - 1]);    // Name predators
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
