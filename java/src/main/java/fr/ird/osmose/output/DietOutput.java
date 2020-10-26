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
import fr.ird.osmose.Prey;
import fr.ird.osmose.stage.DietOutputStage;
import fr.ird.osmose.stage.IStage;
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
public class DietOutput extends SimulationLinker implements IOutput {

    // IO
    private FileOutputStream fos;
    private PrintWriter prw;
    private int recordFrequency;
    //
    private double[][][][] diet;
    private double[][] abundanceStage;
    // Diet output stage
    private IStage dietOutputStage;

    private final String separator;

    public DietOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
    }

    @Override
    public void initStep() {
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies() + this.getConfiguration().getNBkgSpecies();
        int nPrey = nSpec + getConfiguration().getNRscSpecies() + this.getConfiguration().getNBkgSpecies();
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
                }  /// end of loop over preys
            }
        }  // end of loop over predators
    }

    @Override
    public void update() {

        for (School school : getSchoolSet().getPresentSchools()) {
            double preyedBiomass = school.getPreyedBiomass();
            int iSpec = school.getGlobalSpeciesIndex();
            if (preyedBiomass > 0) {
                abundanceStage[iSpec][dietOutputStage.getStage(school)] += school.getAbundance();
                for (Prey prey : school.getPreys()) {
                    int iPrey = prey.getGlobalSpeciesIndex();
                    diet[iSpec][dietOutputStage.getStage(school)][iPrey][dietOutputStage.getStage(prey)] += school.getAbundance() * prey.getBiomass() / preyedBiomass;
                }
            }
        }
    }

    @Override
    public void write(float time) {

        int nSpec = getConfiguration().getNSpecies() + this.getNBkgSpecies();
//        double[][] sum = new double[getNSpecies()][];
//        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
//            sum[iSpec] = new double[nDietStage[iSpec]];
//        }

        // Write the step in the file
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            String name = getSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStagePred = dietOutputStage.getNStage(iSpec);
            for (int st = 0; st < nStagePred; st++) {
                prw.print(time);
                prw.print(separator);
                if (nStagePred == 1) {
                    prw.print(name);    // Name predators
                } else {
                    if (st == 0) {
                        prw.print(quote(name + " < " + threshold[st]));    // Name predators
                    } else {
                        prw.print(quote(name + " >=" + threshold[st - 1]));    // Name predators
                    }
                }
                prw.print(separator);
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
                            prw.print(separator);
                        }
                    }
                }  // end of loop over focal/bkg species as preds.
                prw.println();
            }
        }  // loop of focal/background species as prey. 
        
        // Loop over the resource species, only as prey
        for (int j = 0; j < getConfiguration().getNRscSpecies(); j++) {
            prw.print(time);
            prw.print(separator);
            prw.print(getConfiguration().getResourceSpecies(j).getName());
            prw.print(separator);
            for (int i = 0; i < nSpec; i++) {
                int nStagePred = dietOutputStage.getNStage(i);
                for (int s = 0; s < nStagePred; s++) {
                    if (abundanceStage[i][s] > 0) {
                        float val = (float) (100.d * diet[i][s][j][0] / abundanceStage[i][s]);
                        String sval = Float.isInfinite(val)
                                ? "Inf"
                                : Float.toString(val);
                        prw.print(sval);
                    } else {
                        prw.print("NaN");
                    }
                    //sum[i][s] += diet[i][s][j][0];
                    if (i < nSpec - 1 || s < nStagePred - 1) {
                        prw.print(separator);
                    }
                }
            }  // loop over background + focal species as pred.
            prw.println();
        }
//        prw.print(";sum;");
//        for (int i = 0; i < nSpec; i++) {
//            for (int s = 0; s < nDietStage[i]; s++) {
//                prw.print((float) (100.d * sum[i][s] / abundanceStage[i][s]));
//                prw.print(separator);
//            }
//        }
//        prw.println();
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
        filename.append("_dietMatrix_Simu");
        filename.append(getRank());
        filename.append(".csv");
        File file = new File(path, filename.toString());
        boolean fileExists = file.exists();
        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DietOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);
        if (!fileExists) {
            prw.println(quote("% of prey species (in rows) in the diet of predator species (in col)"));
            prw.print(quote("Time"));
            prw.print(separator);
            prw.print(quote("Prey"));
            int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
            for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                String name = getISpecies(iSpec).getName();
                float[] threshold = dietOutputStage.getThresholds(iSpec);
                int nStage = dietOutputStage.getNStage(iSpec);
                for (int iStage = 0; iStage < nStage; iStage++) {
                    prw.print(separator);
                    if (nStage == 1) {
                        prw.print(name);    // Name predators
                    } else {
                        if (iStage == 0) {
                            prw.print(quote(name + " < " + threshold[iStage]));    // Name predators
                        } else {
                            prw.print(quote(name + " >=" + threshold[iStage - 1]));    // Name predators
                        }
                    }
                }   // end of loop over stage
            }  // loop over predators (focal + bkg)
            prw.println();
        }  // end of file existence test
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
                Logger.getLogger(DietOutput.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }
}
