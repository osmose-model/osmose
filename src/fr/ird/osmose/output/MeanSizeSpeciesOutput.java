/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
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
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author pverley
 */
public class MeanSizeSpeciesOutput extends SimulationLinker implements IOutput {

    // IO
    private FileOutputStream[] fos;
    private PrintWriter[] prw;
    private int recordFrequency;
    //
    private double[][] meanSize;
    private double[][] abundance;
    /**
     * Whether the indicator should be enabled or not.
     */
    private boolean enabled;

    public MeanSizeSpeciesOutput(int rank, String keyEnabled) {
        super(rank);
        enabled = getConfiguration().getBoolean(keyEnabled);
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {

        int nSpecies = getNSpecies();
        meanSize = new double[nSpecies][];
        abundance = new double[nSpecies][];
        for (int i = 0; i < nSpecies; i++) {
            meanSize[i] = new double[getSpecies(i).getLifespanDt()];
            abundance[i] = new double[getSpecies(i).getLifespanDt()];
        }
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int i = school.getSpeciesIndex();
            meanSize[i][school.getAgeDt()] += school.getInstantaneousAbundance() * school.getLength();
            abundance[i][school.getAgeDt()] += school.getInstantaneousAbundance();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void write(float time) {

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            prw[iSpecies].print(time);
            for (int iAge = 0; iAge < getSpecies(iSpecies).getLifespanDt(); iAge++) {
                prw[iSpecies].print(";");
                if (abundance[iSpecies][iAge] > 0) {
                    meanSize[iSpecies][iAge] = (float) (meanSize[iSpecies][iAge] / abundance[iSpecies][iAge]);
                } else {
                    meanSize[iSpecies][iAge] = Double.NaN;
                }
                prw[iSpecies].print((float) meanSize[iSpecies][iAge]);
            }
            prw[iSpecies].println();
        }
    }

    @Override
    public void init() {

        // Record frequency
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        fos = new FileOutputStream[getNSpecies()];
        prw = new PrintWriter[getNSpecies()];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("SizeIndicators");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_meanSize-");
            filename.append(getSimulation().getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getRank());
            filename.append(".csv");
            File file = new File(path, filename.toString());
            boolean fileExists = file.exists();
            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos[iSpecies] = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                getSimulation().warning("Failed to create indicator file {0}. Osmose will not write it.", file.getAbsolutePath());
                enabled = false;
            }
            prw[iSpecies] = new PrintWriter(fos[iSpecies], true);
            if (!fileExists) {
                // Write headers
                prw[iSpecies].print("\"");
                prw[iSpecies].print("Mean size of fish species by age class in cm, weighted by fish numbers");
                prw[iSpecies].println("\"");
                prw[iSpecies].print("Time");
                for (int iAge = 0; iAge < getSpecies(iSpecies).getLifespanDt(); iAge++) {
                    prw[iSpecies].print(";Age class ");
                    prw[iSpecies].print(iAge);
                }
                prw[iSpecies].println();
            }
        }
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

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }
}
