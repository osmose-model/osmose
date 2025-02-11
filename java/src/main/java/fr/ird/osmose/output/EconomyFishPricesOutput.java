/*
 *OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 *http://www.osmose-model.org
 *
 *Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-today
 *
 *Osmose is a computer program whose purpose is to simulate fish
 *populations and their interactions with their biotic and abiotic environment.
 *OSMOSE is a spatial, multispecies and individual-based model which assumes
 *size-based opportunistic predation based on spatio-temporal co-occurrence
 *and size adequacy between a predator and its prey. It represents fish
 *individuals grouped into schools, which are characterized by their size,
 *weight, age, taxonomy and geographical location, and which undergo major
 *processes of fish life cycle (growth, explicit predation, additional and
 *starvation mortalities, reproduction and migration) and fishing mortalities
 *(Shin and Cury 2001, 2004).
 *
 *Contributor(s):
 *Yunne SHIN (yunne.shin@ird.fr),
 *Morgane TRAVERS (morgane.travers@ifremer.fr)
 *Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 *Philippe VERLEY (philippe.verley@ird.fr)
 *Laure VELEZ (laure.velez@ird.fr)
 *Nicolas Barrier (nicolas.barrier@ird.fr)
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation (version 3 of the License). Full description
 *is provided on the LICENSE file.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package fr.ird.osmose.output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.stage.SchoolStage;
import fr.ird.osmose.util.SimulationLinker;

public class EconomyFishPricesOutput extends SimulationLinker implements IOutput {

    /**
     * Output is the price of a fish species per size class. Dim =
     * [Species][Size-class]
     */
    private double[][] output;
    private FileOutputStream fos[];
    private PrintWriter prw[];
    private int recordFrequency;
    private SchoolStage sizeClasses;

    /**
     * CSV separator
     */
    private final String separator;

    public EconomyFishPricesOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
    }

    @Override
    public void initStep() {
        // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
        // initialisation of the prices
        output = new double[getNSpecies()][];
        for (int i = 0; i < getNSpecies(); i++) {
            int iClass = this.sizeClasses.getNStage(i);
            output[i] = new double[iClass];
        }
    }

    @Override
    public void update() {
        // get fish prices (ispecies)(size-class)
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            for (int iClass = 0; iClass < this.sizeClasses.getNStage(iSpecies); iClass++) {
                output[iSpecies][iClass] += getSimulation().getEconomicModule().getPrices(iSpecies, iClass);
            }
        }
    }

    @Override
    public void write(float time) {
        for (int iSpecies = 0; iSpecies < getConfiguration().getNSpecies(); iSpecies++) {
            for (int iClass = 0; iClass < this.sizeClasses.getNStage(iSpecies); iClass++) {
                prw[iSpecies].print(time);
                prw[iSpecies].print(separator);
                prw[iSpecies].print(iClass == 0 ? 0 : this.sizeClasses.getThresholds(iSpecies, iClass - 1));
                prw[iSpecies].print(separator);
                prw[iSpecies].print(output[iSpecies][iClass]);
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

        fos = new FileOutputStream[getNSpecies()];
        prw = new PrintWriter[getNSpecies()];

        this.sizeClasses = new SchoolStage("economic.output.stage");
        this.sizeClasses.init();

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("Econ");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_FishPrices");
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
            String name = getISpecies(iSpecies).getName();
            prw[iSpecies].print(quote(name));
            prw[iSpecies].print(separator);
            prw[iSpecies].println();
        }

        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }
}
