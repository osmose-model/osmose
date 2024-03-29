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
import java.util.stream.Stream;

/**
 *
 * @author pverley
 */
abstract public class AbstractOutput extends SimulationLinker implements IOutput {

///////////////////////////////
// Declaration of the variables
///////////////////////////////

    /**
     * List of files where the given variable will be stored. prw[0] =
     * integrated over all the domain prw[1] = over the first region prw[2] =
     * over the second region. etc.
     */
    private PrintWriter[] prw;



    /**
     * CSV separator
     */
    private final String separator;

    private final String subfolder;

    private final String name;

    private final int nOutputRegion;

    private final boolean includeOnlyAlive;

///////////////////
// Abstract methods
///////////////////
    abstract String getDescription();

    public abstract String[] getHeaders();

    private interface FlushMethod {
        public void flushFile(PrintWriter prw);
    }

    FlushMethod flushMethod;

///////////////
// Constructors
///////////////

    /** Constructor in which alive schools are forced to be included */
    AbstractOutput(int rank, String subfolder, String name) {
        this(rank, subfolder, name, true);
    }

    /** Constructor with additional argument which specifies if only alive schools are to be included */
    AbstractOutput(int rank, String subfolder, String name, boolean includeOnlyAlive) {
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
        this.includeOnlyAlive = includeOnlyAlive;
    }

    public Stream<School> getOutputSchoolStream() {
        Stream<School> stream = this.includeOnlyAlive ? this.getSchoolSet().getAliveSchools().stream()
                : this.getSchoolSet().getSchools().stream();
        return stream;
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


        // Create parent directory
        File path = new File(getConfiguration().getOutputPathname());

        // Creation of the regional output files for each region.
        List<AbstractOutputRegion> regions = getConfiguration().getOutputRegions();
        prw = new PrintWriter[regions.size()];

        int i = 0;
        for (AbstractOutputRegion region : getOutputRegions()) {
            File file = new File(path, getFilename(region.getIndex(), region.getName()));
            // boolean fileExists = file.exists();
            file.getParentFile().mkdirs();
            try {
                // Init stream
                prw[i] = new PrintWriter(file);
            } catch (FileNotFoundException ex) {
                error("Failed to create output file " + file.getAbsolutePath(), ex);
            }

            prw[i].println(quote(getDescription()));
            prw[i].print(quote("Time"));
            String[] headers = getHeaders();
            for (String header : headers) {
                prw[i].print(separator);
                prw[i].print(quote(header));
            }
            prw[i].println();
            flushMethod.flushFile(prw[i]);

            i++;
        }

    }

    boolean include(School school) {
        return ((!getConfiguration().isCutoffEnabled()) || (school.getAge() >= getConfiguration().getCutoffAge()[school.getSpeciesIndex()]) & ((school.getLength() >= getConfiguration().getCutoffLength()[school.getSpeciesIndex()])));
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
        return getConfiguration().getRecordFrequency();
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % getRecordFrequency()) == 0);
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
