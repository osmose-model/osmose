/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
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
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
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
            double preyedBiomass = school.getPreyedBiomass();
            int iSpec = school.getSpeciesIndex();
            if (preyedBiomass > 0) {
                abundanceStage[iSpec][dietOutputStage.getStage(school)] += school.getAbundance();
                for (Prey prey : school.getPreys()) {
                    diet[iSpec][dietOutputStage.getStage(school)][prey.getSpeciesIndex()][dietOutputStage.getStage(prey)] += school.getAbundance() * prey.getBiomass() / preyedBiomass;
                }
            }
        }
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
                }
                prw.println();
            }
        }
        for (int j = nSpec; j < (nSpec + getConfiguration().getNPlankton()); j++) {
            prw.print(time);
            prw.print(separator);
            prw.print(getConfiguration().getPlankton(j - nSpec));
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
            }
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
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                String name = getSpecies(iSpec).getName();
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
