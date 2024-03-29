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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.stage.SchoolStage;
import fr.ird.osmose.util.SimulationLinker;

/**
 *
 * Class for writting the accessible biomass for fisheries. Outputs is by
 * species (one file
 * per species) and columns provide the accessible biomass for each fishing
 * gear.
 *
 * @author Nicolas Barrier
 */
public class FishingAccessBiomassOutput extends SimulationLinker implements IOutput {

    private FileOutputStream fos[];
    private PrintWriter prw[];
    private int recordFrequency;
    private double output[][][];
    private int nFisheries;
    private SchoolStage sizeClasses;

    /**
     * CSV separator
     */
    private final String separator;

    FishingAccessBiomassOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
    }

    @Override
    public void initStep() {
        // TODO Auto-generated method stub

    }

    @Override
    public void reset() {
        // initialisation of the accessible biomass
        output = new double[getNSpecies()][nFisheries][];
        for (int i = 0; i < getNSpecies(); i++) {
            int nClass = this.sizeClasses.getNStage(i);
            for (int j = 0; j < nFisheries; j++) {
                output[i][j] = new double[nClass];
            }
        }
    }

    @Override
    public void update() {
        // get accessible biomass (nfisheries, nspecies)
        for (int iFishery = 0; iFishery < nFisheries; iFishery++) {
            for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
                for(int iClass=0; iClass < this.sizeClasses.getNStage(iSpecies); iClass++) {
                    output[iSpecies][iFishery][iClass] += getSimulation().getEconomicModule().getAccessibleBiomass(iFishery, iSpecies, iClass);
                }
            }
        }
    }

    @Override
    public void write(float time) {

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            for (int iClass = 0; iClass < this.sizeClasses.getNStage(iSpecies); iClass++) {
                prw[iSpecies].print(time);
                prw[iSpecies].print(separator);
                prw[iSpecies].print(iClass == 0 ? 0: this.sizeClasses.getThresholds(iSpecies, iClass - 1));
                prw[iSpecies].print(separator);
                for (int iFishery = 0; iFishery < nFisheries - 1; iFishery++) {
                    // instantenous mortality rate for eggs additional mortality
                    prw[iSpecies].print(output[iSpecies][iFishery][iClass]);
                    prw[iSpecies].print(separator);
                }

                int iFishery = nFisheries - 1;
                prw[iSpecies].print(output[iSpecies][iFishery][iClass]);
                prw[iSpecies].println();
            }
        }
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }

    @Override
    public void init() {

        this.sizeClasses = new SchoolStage("economic.output.stage");
        this.sizeClasses.init();

        fos = new FileOutputStream[getNSpecies()];
        prw = new PrintWriter[getNSpecies()];
        nFisheries = getConfiguration().getNFisheries();
        String[] namesFisheries = getConfiguration().getFisheriesNames();

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("Econ");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_accessBiomassBy");
            filename.append(sizeClasses.getType(iSpecies));
            filename.append("-");
            filename.append(getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getRank());
            filename.append(".csv");
            File file = new File(path, filename.toString());

            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos[iSpecies] = new FileOutputStream(file, false);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MortalityOutput.class.getName()).log(Level.SEVERE, null, ex);
            }

            prw[iSpecies] = new PrintWriter(fos[iSpecies], true);
            // Write headers
            prw[iSpecies].print(quote("Time"));
            prw[iSpecies].print(separator);
            prw[iSpecies].print(quote("Class"));
            prw[iSpecies].print(separator);
            for (int iFishery = 0; iFishery < nFisheries - 1; iFishery++) {
                String fishingName = namesFisheries[iFishery];
                prw[iSpecies].print(quote(fishingName));
                prw[iSpecies].print(separator);
            }
            int iFishery = nFisheries - 1;
            String fishingName = namesFisheries[iFishery];
            prw[iSpecies].print(quote(fishingName));
            prw[iSpecies].println();

        }

        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

    }

    @Override
    public void close() {
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            if (null != prw) {
                prw[iSpecies].close();
            }
            if (null != fos) {
                try {
                    fos[iSpecies].close();
                } catch (IOException ex) {
                    // do nothing
                }
            }
        }

    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }
}