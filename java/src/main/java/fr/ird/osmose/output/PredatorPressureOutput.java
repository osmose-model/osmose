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
import fr.ird.osmose.stage.SchoolStage;
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
public class PredatorPressureOutput extends SimulationLinker implements IOutput {

    // IO
    private FileOutputStream fos;
    private PrintWriter prw;
    private int recordFrequency;
    //
    private double[][][][] predatorPressure;
    // Diet output stage
    private SchoolStage dietOutputStage;

    private final String separator;

    public PredatorPressureOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies() + this.getNBkgSpecies();;
        int nPrey = nSpec + getConfiguration().getNRscSpecies();
        predatorPressure = new double[nSpec][][][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            int nStage = dietOutputStage.getNStage(iSpec);
            predatorPressure[iSpec] = new double[nStage][][];
            for (int s = 0; s < nStage; s++) {
                predatorPressure[iSpec][s] = new double[nPrey][];
                for (int iPrey = 0; iPrey < nPrey; iPrey++) {
                    predatorPressure[iSpec][s][iPrey] = new double[dietOutputStage.getNStage(iPrey)];
                }
            } // end of species loop (preys)
        } // end of species loop (pred)
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int iSpec = school.getSpeciesIndex();
            int stage = dietOutputStage.getStage(school);
            for (Prey prey : school.getPreys()) {
                int iPrey = prey.getSpeciesIndex();
                predatorPressure[iSpec][stage][iPrey][dietOutputStage.getStage(prey)] += prey.getBiomass();
            }
        }
    }

    @Override
    public void write(float time) {

        int nSpec = this.getNSpecies();
        int nBkg = this.getNBkgSpecies();
        int nRsc = this.getNRscSpecies();

        int dtRecord = getConfiguration().getInt("output.recordfrequency.ndt");
        for (int iSpec = 0; iSpec < nSpec + nBkg; iSpec++) {
            // iSpec = species index as prey
            String name = getISpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStagePred = dietOutputStage.getNStage(iSpec);
            for (int iStage = 0; iStage < nStagePred; iStage++) {
                prw.print(time);
                prw.print(separator);
                if (nStagePred == 1) {
                    prw.print(name);    // Name predators
                } else {
                    if (iStage == 0) {
                        prw.print(quote(name + " < " + threshold[iStage]));    // Name predators
                    } else {
                        prw.print(quote(name + " >=" + threshold[iStage - 1]));    // Name predators
                    }
                }
                prw.print(separator);
                for (int i = 0; i < nSpec; i++) {
                    int nStage = dietOutputStage.getNStage(i);
                    for (int s = 0; s < nStage; s++) {
                        float val = (float) (predatorPressure[i][s][iSpec][iStage] / dtRecord);
                        String sval = Float.isInfinite(val)
                                ? "Inf"
                                : Float.toString(val);
                        prw.print(sval);
                        if (i < nSpec - 1 || s < nStage - 1) {
                            prw.print(separator);
                        }
                    }
                }
                prw.println();
            }
        }

        int offset = nSpec + nBkg;
        for (int j = 0; j < nRsc; j++) {
            prw.print(time);
            prw.print(separator);
            prw.print(getConfiguration().getResourceSpecies(j));
            prw.print(separator);
            for (int i = 0; i < nSpec; i++) {
                int nStage = dietOutputStage.getNStage(i);
                for (int s = 0; s < nStage; s++) {
                    prw.print((float) (predatorPressure[i][s][j + offset][0] / dtRecord));
                    if (i < nSpec - 1 || s < nStage - 1) {
                        prw.print(separator);
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
        dietOutputStage = new SchoolStage("output.diet.stage");
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
            fos = new FileOutputStream(file, false);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DietOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);
        if (!fileExists) {
            prw.println(quote("Biomass of prey species (in tons per time step of saving, in rows) eaten by a predator species (in col). The last column reports the biomass of prey at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)"));
            prw.print(quote("Time"));
            prw.print(separator);
            prw.print(quote("Prey"));
            for (int iSpec = 0; iSpec < getNSpecies() + getNBkgSpecies(); iSpec++) {
                String name = getISpecies(iSpec).getName();
                float[] threshold = dietOutputStage.getThresholds(iSpec);
                int nStage = dietOutputStage.getNStage(iSpec);
                for (int iStage = 0; iStage < nStage; iStage++) {
                    prw.print(separator);
                    if (nStage == 1) {
                        prw.print(quote(name));    // Name predators
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
                // do nothing
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
