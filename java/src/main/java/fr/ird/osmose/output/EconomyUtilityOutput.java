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
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.util.SimulationLinker;

public class EconomyUtilityOutput extends SimulationLinker implements IOutput {

    private double output;
    private PrintWriter prw;
    private FileOutputStream fos;
    private int recordFrequency;

    /**
     * CSV separator
     */
    private final String separator;

    public EconomyUtilityOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
    }

    @Override
    public void initStep() {
        // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
        output = 0;
    }

    @Override
    public void update() {
        output = getSimulation().getEconomicModule().getUtility();
    }

    @Override
    public void write(float time) {
        prw.print(time);
        prw.print(separator);
        prw.print(output);
        prw.println();
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }

    @Override
    public void init() {

        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder("Econ");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_Utility");
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        File file = new File(path, filename.toString());

        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, false);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MortalityOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        prw = new PrintWriter(fos, true);

        // Write headers
        prw.print(quote("Time"));
        prw.print(separator);
        prw.print(quote("Utility"));
        prw.print(separator);
        prw.println();

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