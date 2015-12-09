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
abstract public class AbstractOutput extends SimulationLinker implements IOutput {

    private FileOutputStream fos;
    private PrintWriter prw;
    private boolean cutoffEnabled;
    private int recordFrequency;
    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;
    private final String separator;

    abstract String getFilename();

    abstract String getDescription();

    abstract String[] getHeaders();

    AbstractOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
    }

    @Override
    public void init() {

        // Cutoff
        cutoffEnabled = getConfiguration().getBoolean("output.cutoff.enabled");
        cutoffAge = new float[getNSpecies()];
        if (cutoffEnabled) {
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                cutoffAge[iSpec] = getConfiguration().getFloat("output.cutoff.age.sp" + iSpec);
            }
        }
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname());
        File file = new File(path, getFilename());
        boolean fileExists = file.exists();
        file.getParentFile().mkdirs();
        try {
            // Init stream
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException ex) {
            error("Failed to create output file " + file.getAbsolutePath(), ex);
        }
        prw = new PrintWriter(fos, true);

        if (!fileExists) {
            prw.println(quote(getDescription()));
            prw.print(quote("Time"));
            String[] headers = getHeaders();
            for (String header : headers) {
                prw.print(separator);
                prw.print(quote(header));
            }
            prw.println();
        }
    }

    boolean includeClassZero() {
        return !cutoffEnabled;
    }

    boolean include(School school) {
        return !cutoffEnabled || school.getAge() >= cutoffAge[school.getSpeciesIndex()];
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

    void writeVariable(float time, double[] variable) {

        prw.print(time);
        for (int i = 0; i < variable.length; i++) {
            prw.print(separator);
            String sval = Float.isInfinite((float) variable[i])
                    ? "Inf"
                    : Float.toString((float) variable[i]);
            prw.print(sval);
        }
        prw.println();
    }

    void writeVariable(float time, double[][] variable) {
        for (double[] row : variable) {
            prw.print(time);
            for (int j = 0; j < row.length; j++) {
                prw.print(separator);
                String sval = Float.isInfinite((float) row[j])
                        ? "Inf"
                        : Float.toString((float) row[j]);
                prw.print(sval);
            }
            prw.println();
        }
    }

    /**
     * @return the recordFrequency
     */
    public int getRecordFrequency() {
        return recordFrequency;
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }

    public String quote(String str) {
        return "\"" + str + "\"";
    }

    public String[] quote(String[] str) {
        String[] arr = new String[str.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = quote(str[i]);
        }
        return arr;
    }
}
