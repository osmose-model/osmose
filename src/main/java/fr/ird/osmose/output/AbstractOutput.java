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

///////////////////////////////
// Declaration of the variables
///////////////////////////////    
    private FileOutputStream fos;
    private boolean cutoffEnabled;
    private int recordFrequency;

    /**
     * List of files where the given variable will be stored. prw[0] =
     * integrated over all the domain prw[1] = over the first region prw[2] =
     * over the second region. etc.
     */
    private PrintWriter[] prw;

    /**
     * Boolean that defines whether the variable can be saved regionally.
     * Default is false.
     */
    private final boolean save_regional;

    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;
    /**
     * CSV separator
     */
    private final String separator;
    
    private final String subfolder;
    
    private final String name;

///////////////////
// Abstract methods
///////////////////    

    abstract String getDescription();

    abstract String[] getHeaders();

///////////////
// Constructors
///////////////    
    AbstractOutput(int rank, String subfolder, String name, boolean save_regional) {
        super(rank);
        this.subfolder = subfolder;
        this.name = name;
        this.save_regional = save_regional;
        separator = getConfiguration().getOutputSeparator();
    }

    AbstractOutput(int rank, String subfolder, String name) {
        this(rank, subfolder, name, false);
    }

////////////////////////////
// Definition of the methods
////////////////////////////

    final String getFilename() {
        StringBuilder filename = new StringBuilder();
        if (null != subfolder && !subfolder.isEmpty()) {
            filename.append(subfolder).append(File.separatorChar);
        }
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_").append(name).append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }
    
    final String getRegionalFilename(int idom) {
        StringBuilder filename = new StringBuilder(getConfiguration().getOutputPathname());
        filename.append(File.separatorChar);
        filename.append("Regional");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_");
        filename.append(Regions.getRegionName(idom));
        filename.append("_").append(name).append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }
    
    @Override
    public void init() {

        File file;
        boolean fileExists;

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

        // Creation of the regional output files for each region.
        int nregions = this.save_regional ? Regions.getNRegions() : 0;
        prw = new PrintWriter[nregions + 1];
        for (int i = 0; i < nregions + 1; i++) {
            if (i == 0) {
                file = new File(path, getFilename());
            } else {
                file = new File(path, getRegionalFilename(i - 1));
            }
            fileExists = file.exists();
            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                error("Failed to create output file " + file.getAbsolutePath(), ex);
            }
            prw[i] = new PrintWriter(fos, true);

            if (!fileExists) {
                prw[i].println(quote(getDescription()));
                prw[i].print(quote("Time"));
                String[] headers = getHeaders();
                for (String header : headers) {
                    prw[i].print(separator);
                    prw[i].print(quote(header));
                }
                prw[i].println();
            }
        }

    }

    boolean includeClassZero() {
        return !cutoffEnabled;
    }

    boolean include(School school) {
        return ((!cutoffEnabled) || (school.getAge() >= cutoffAge[school.getSpeciesIndex()]));
    }

    @Override
    public void close() {

        // Adding the closing of regional files
        for (int i = 0; i < prw.length; i++) {
            if (null != prw[i]) {
                prw[i].close();
            }
        }

        if (null != fos) {
            try {
                fos.close();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    /**
     * Write variables integrated over the entire region. It writes on the file
     * index 0.*
     */
    void writeVariable(float time, double[] variable) {
        writeVariable(0, time, variable);
    }

    /**
     * Write variables integrated over the entire region. It writes on the file
     * index 0.*
     */
    void writeVariable(float time, double[][] variable) {
        writeVariable(0, time, variable);
    }

    void writeVariable(int fileindex, float time, double[] variable) {

        prw[fileindex].print(time);
        for (int i = 0; i < variable.length; i++) {
            prw[fileindex].print(separator);
            String sval = Float.isInfinite((float) variable[i])
                    ? "Inf"
                    : Float.toString((float) variable[i]);
            prw[fileindex].print(sval);
        }
        prw[fileindex].println();
    }

    void writeVariable(int fileindex, float time, double[][] variable) {
        for (double[] row : variable) {
            prw[fileindex].print(time);
            for (int j = 0; j < row.length; j++) {
                prw[fileindex].print(separator);
                String sval = Float.isInfinite((float) row[j])
                        ? "Inf"
                        : Float.toString((float) row[j]);
                prw[fileindex].print(sval);
            }
            prw[fileindex].println();
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

    public boolean saveRegional() {
        return this.save_regional;
    }

}
