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
public class DietIndicator extends SimulationLinker implements Indicator {

    // IO
    private FileOutputStream fos;
    private PrintWriter prw;
    //
    private double[][][][] diet;
    private double[][] abundanceStage;
    /**
     * Whether the indicator should be enabled or not.
     */
    private boolean enabled;
    // Diet output stage
    private AbstractStage dietOutputStage;

    public DietIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation);
        enabled = getConfiguration().getBoolean(keyEnabled);
    }

    @Override
    public void initStep() {
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        diet = new double[nSpec][][][];
        abundanceStage = new double[nSpec][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            int nStage = dietOutputStage.getNStage(iSpec);
            diet[iSpec] = new double[nStage][][];
            abundanceStage[iSpec] = new double[nStage];
            for (int iStage = 0; iStage < nStage; iStage++) {
                diet[iSpec][iStage] = new double[nPrey][];
                for (int iPrey = 0; iPrey < nPrey; iPrey++) {
                    if (iPrey < nSpec) {
                        diet[iSpec][iStage][iPrey] = new double[dietOutputStage.getNStage(iPrey)];
                    } else {
                        diet[iSpec][iStage][iPrey] = new double[1];
                    }
                }
            }
        }
    }

    @Override
    public void update() {

        for (School school : getSchoolSet().getPresentSchools()) {
            double sumDiet = computeSumDiet(school);
            int iSpec = school.getSpeciesIndex();
            if (sumDiet > 0) {
                abundanceStage[iSpec][dietOutputStage.getStage(school)] += school.getAbundance();
            }
            for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
                int nStage = dietOutputStage.getNStage(i);
                for (int s = 0; s < nStage; s++) {
                    if (sumDiet > 0) {
                        //System.out.println(nStage + " "  +school.diet[i].length + " " + s);
                        diet[iSpec][dietOutputStage.getStage(school)][i][s] += school.getAbundance() * school.diet[i][s] / sumDiet;
                    }
                }
            }
            for (int i = getConfiguration().getNSpecies(); i < getConfiguration().getNSpecies() + getConfiguration().getNPlankton(); i++) {
                if (sumDiet > 0) {
                    diet[iSpec][dietOutputStage.getStage(school)][i][0] += school.getAbundance() * school.diet[i][0] / sumDiet;
                }
            }
        }
    }

    private double computeSumDiet(School school) {
        double sumDiet = 0.d;
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            int nStage = dietOutputStage.getNStage(i);
            for (int s = 0; s < nStage; s++) {
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
        return enabled;
    }

    @Override
    public void write(float time) {

        int nSpec = getConfiguration().getNSpecies();
//        double[][] sum = new double[getNSpecies()][];
//        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
//            sum[iSpec] = new double[nDietStage[iSpec]];
//        }

        // Write the step in the file
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            String name = getSimulation().getSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStagePred = dietOutputStage.getNStage(iSpec);
            for (int st = 0; st < nStagePred; st++) {
                prw.print(time);
                prw.print(';');
                if (nStagePred == 1) {
                    prw.print(name);    // Name predators
                } else {
                    if (st == 0) {
                        prw.print(name + " < " + threshold[st]);    // Name predators
                    } else {
                        prw.print(name + " >=" + threshold[st - 1]);    // Name predators
                    }
                }
                prw.print(";");
                for (int i = 0; i < nSpec; i++) {
                    int nStagePrey = dietOutputStage.getNStage(i);
                    for (int s = 0; s < nStagePrey; s++) {
                        if (abundanceStage[i][s] > 0) {
                            prw.print((float) (100.d * diet[i][s][iSpec][st] / abundanceStage[i][s]));
                        } else {
                            prw.print("NaN");
                        }
                        //sum[i][s] += diet[i][s][iSpec][st];
                        if (i < nSpec - 1 || s < nStagePrey - 1) {
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
                int nStagePred = dietOutputStage.getNStage(i);
                for (int s = 0; s < nStagePred; s++) {
                    if (abundanceStage[i][s] > 0) {
                        prw.print((float) (100.d * diet[i][s][j][0] / abundanceStage[i][s]));
                    } else {
                        prw.print("NaN");
                    }
                    //sum[i][s] += diet[i][s][j][0];
                    if (i < nSpec - 1 || s < nStagePred - 1) {
                        prw.print(";");
                    }
                }
            }
            prw.println();
        }
//        prw.print(";sum;");
//        for (int i = 0; i < nSpec; i++) {
//            for (int s = 0; s < nDietStage[i]; s++) {
//                prw.print((float) (100.d * sum[i][s] / abundanceStage[i][s]));
//                prw.print(";");
//            }
//        }
//        prw.println();
    }

    @Override
    public void init() {

        // Init diet output stage
        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();

        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_dietMatrix_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        File file = new File(path, filename.toString());
        boolean fileExists = file.exists();
        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AbstractIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);
        if (!fileExists) {
            prw.print("\"");
            prw.print("% of prey species (in rows) in the diet of predator species (in col)");
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
                Logger.getLogger(DietIndicator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
