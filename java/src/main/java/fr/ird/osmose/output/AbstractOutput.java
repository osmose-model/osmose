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
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

/**
 *
 * @author pverley
 */
abstract public class AbstractOutput extends SimulationLinker implements IOutput {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    private boolean cutoffEnabled;
    private int recordFrequency;

    /**
     * List of files where the given variable will be stored. prw[0] =
     * integrated over all the domain prw[1] = over the first region prw[2] =
     * over the second region. etc.
     */
    private PrintWriter[] prw;

    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;

    /**
     * Threshold size (cm) for age class zero. This parameter allows to discard
     * schools smaller that this threshold in the calculation of the indicators when
     * parameter <i>output.cutoff.enabled</i> is set to {@code true}. Parameter
     * <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffSize;


    /**
     * CSV separator
     */
    private final String separator;

    private final String subfolder;

    private final String name;

    private final int nOutputRegion;

///////////////////
// Abstract methods
///////////////////
    abstract String getDescription();

    abstract String[] getHeaders();

    private interface FlushMethod {
        public void flushFile(PrintWriter prw);
    }

    FlushMethod flushMethod;

///////////////
// Constructors
///////////////
    AbstractOutput(int rank, String subfolder, String name) {
        super(rank);
        this.subfolder = subfolder;
        this.name = name;
        separator = getConfiguration().getOutputSeparator();
        nOutputRegion = getConfiguration().getOutputRegions().size();
        if(getConfiguration().isFlushEnabled()) {
            flushMethod = (prw) -> prw.flush();
        } else {
            flushMethod = (prw -> {});
        }
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    final String getFilename(int region, String regionName) {
        StringBuilder filename = new StringBuilder();
        if (null != subfolder && !subfolder.isEmpty()) {
            filename.append(subfolder).append(File.separatorChar);
        }
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_").append(name);
        if (region > 0) {
            filename.append("-").append(regionName);
        }
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    public int getNOutputRegion() {
        return nOutputRegion;
    }

    public List<AbstractOutputRegion> getOutputRegions() {
        return getConfiguration().getOutputRegions();
    }

    @Override
    public void init() {

        // Cutoff
        cutoffEnabled = getConfiguration().getBoolean("output.cutoff.enabled");
        cutoffAge = new float[getNSpecies()];
        if (cutoffEnabled) {
            int cpt = 0;
            for (int iSpec : getFocalIndex()) {
                // If cutoff enabled, look for cutoff age
                if(!getConfiguration().isNull("output.cutoff.age.sp" + iSpec)) {
                    cutoffAge[cpt] = getConfiguration().getFloat("output.cutoff.age.sp" + iSpec);
                } else {
                    cutoffAge[cpt] = Float.NEGATIVE_INFINITY;
                }

                // If cutoff enabled, look for cutoff size
                if(!getConfiguration().isNull("output.cutoff.size.sp" + iSpec)) {
                    cutoffSize[cpt] = getConfiguration().getFloat("output.cutoff.size.sp" + iSpec);
                } else {
                    cutoffSize[cpt] = Float.NEGATIVE_INFINITY;
                }
                cpt++;
            }
        }
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname());

        // Creation of the regional output files for each region.
        List<AbstractOutputRegion> regions = getConfiguration().getOutputRegions();
        prw = new PrintWriter[regions.size()];

        int i = 0;
        for (AbstractOutputRegion region : getOutputRegions()) {
            File file = new File(path, getFilename(region.getIndex(), region.getName()));
            boolean fileExists = file.exists();
            file.getParentFile().mkdirs();
            try {
                // Init stream
                prw[i] = new PrintWriter(file);
            } catch (FileNotFoundException ex) {
                error("Failed to create output file " + file.getAbsolutePath(), ex);
            }

            if (!fileExists) {
                prw[i].println(quote(getDescription()));
                prw[i].print(quote("Time"));
                String[] headers = getHeaders();
                for (String header : headers) {
                    prw[i].print(separator);
                    prw[i].print(quote(header));
                }
                prw[i].println();
                flushMethod.flushFile(prw[i]);
            }
            i++;
        }

    }

    boolean include(School school) {
        return ((!cutoffEnabled) || (school.getAge() >= cutoffAge[school.getSpeciesIndex()]) & ((school.getLength() >= cutoffSize[school.getSpeciesIndex()])));
    }

    @Override
    public void close() {

        // Adding the closing of regional files
        for (int i = 0; i < prw.length; i++) {
            if (null != prw[i]) {
                prw[i].close();
            }
        }
    }

    void writeVariable(int region, float time, double[] variable) {

        prw[region].print(time);
        for (int i = 0; i < variable.length; i++) {
            prw[region].print(separator);
            String sval = Float.isInfinite((float) variable[i])
                    ? "Inf"
                    : Float.toString((float) variable[i]);
            prw[region].print(sval);
        }
        prw[region].println();
        flushMethod.flushFile(prw[region]);
    }

    void writeVariable(int region, float time, double[][] variable) {
        for (double[] row : variable) {
            prw[region].print(time);
            for (int j = 0; j < row.length; j++) {
                prw[region].print(separator);
                String sval = Float.isInfinite((float) row[j])
                        ? "Inf"
                        : Float.toString((float) row[j]);
                prw[region].print(sval);
            }
            prw[region].println();
            flushMethod.flushFile(prw[region]);
        }
    }

    void writeVariable(float time, double[] variable) {
        writeVariable(0, time, variable);
    }

    void writeVariable(float time, double[][] variable) {
        writeVariable(0, time, variable);
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
