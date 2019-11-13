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
import fr.ird.osmose.util.io.IOTools;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * @author pverley
 */
abstract public class AbstractOutput_Netcdf extends SimulationLinker implements IOutput {

    private boolean cutoffEnabled;
    private int recordFrequency;

    /**
     * List of dimensions of the variable to write out.
     */
    private Dimension timeDim;

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    private NetcdfFileWriteable nc;

    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;
    private final String separator;
    private Dimension[] outDims;

    /**
     * Returns NetCdf file
     */
    abstract String getFilename();

    /**
     * Returns output variable description
     */
    abstract String getDescription();

    /* Return output units. */
    abstract String getUnits();

    abstract String getVarname();

    AbstractOutput_Netcdf(int rank) {
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

        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            String filename = getFilename();
            System.out.println(filename);
            IOTools.makeDirectories(filename);
            nc.setLocation(filename);
        } catch (IOException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Add time dim and variable (common to all files)
        timeDim = nc.addUnlimitedDimension("time");
        nc.addVariable("time", DataType.DOUBLE, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "days since 0-1-1 0:0:0");
        nc.addVariableAttribute("time", "calendar", "360_day");
        nc.addVariableAttribute("time", "description", "time ellapsed, in days, since the beginning of the simulation");

        // Init NC dimensions and coords (in define mode)
        this.init_nc_dims_coords();

        // Create output variable
        nc.addVariable(getVarname(), DataType.FLOAT, this.getNcDims());
        nc.addVariableAttribute(getVarname(), "units", getUnits());
        nc.addVariableAttribute(getVarname(), "description", getDescription());
        nc.addVariableAttribute(getVarname(), "_FillValue", getFillValue());

        try {
            // Validates the structure of the NetCDF file.
            nc.create();
        } catch (IOException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Write NetCDF coords (for instance species, stage, etc.)
        this.write_nc_coords();

    }

    boolean includeClassZero() {
        return !cutoffEnabled;
    }

    boolean include(School school) {
        return ((!cutoffEnabled) || (school.getAge() >= cutoffAge[school.getSpeciesIndex()]));
    }

    @Override
    public void close() {
        try {
            nc.close();
            String strFilePart = nc.getLocation();
            String strFileBase = strFilePart.substring(0, strFilePart.indexOf(".part"));
            File filePart = new File(strFilePart);
            File fileBase = new File(strFileBase);
            filePart.renameTo(fileBase);
        } catch (IOException ex) {
            warning("Problem closing the NetCDF spatial output file | {0}", ex.toString());
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

    /**
     * Function to create a species attribute.
     */
    public String createSpeciesAttr() {

        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < this.getNSpecies(); i++) {
            String attrname = String.format("species%d", i);
            String attval = this.getSpecies(i).getName();
            nc.addVariableAttribute("species", attrname, attval);
        }

        return bld.toString();

    }

    public int getTimeIndex() {
        return this.nc.getUnlimitedDimension().getLength();
    }

    public Dimension getTimeDim() {
        return timeDim;
    }

    public void writeVariable(double time, double array[]) {

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        ArrayFloat.D2 arrAbund = new ArrayFloat.D2(1, array.length);
        arrTime.set(0, time);

        for (int i = 0; i < array.length; i++) {
            arrAbund.set(0, i, (float) array[i]);
        }

        int index = this.getTimeIndex();
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write(this.getVarname(), new int[]{index, 0}, arrAbund);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public float getFillValue() {
        return this.FILLVALUE;
    }

    public Dimension[] getNcDims() {
        return this.outDims;
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    void init_nc_dims_coords() {

        Dimension speciesDim = nc.addDimension("species", getNSpecies());
        nc.addVariable("species", DataType.INT, new Dimension[]{speciesDim});
        this.createSpeciesAttr();
        outDims = new Dimension[]{timeDim, speciesDim};

    }

    public void write_nc_coords() {
        try {

            // Writes variable trait (trait names) and species (species names)
            ArrayInt.D1 arrSpecies = new ArrayInt.D1(this.getNSpecies());

            for (int i = 0; i < this.getNSpecies(); i++) {
                arrSpecies.set(i, i);
            }

            nc.write("species", arrSpecies);

        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbundanceOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public NetcdfFileWriteable getNc() {
        return this.nc;
    }

    public void setDims(Dimension[] dims) {
        this.outDims = dims;
    }

    public void writeVariable(double time, double array[][]) {

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        arrTime.set(0, time);
        int nrows = array.length;  // number of classes
        int ncol = array[0].length;  // number of species

        ArrayFloat.D3 arrAbund = new ArrayFloat.D3(1, nrows, ncol);

        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncol; j++) {
                arrAbund.set(0, i, j, (float) array[i][j]);
            }
        }

        int index = this.getTimeIndex();
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write(this.getVarname(), new int[]{index, 0, 0}, arrAbund);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public StringBuilder initFileName() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        return filename;
    }

}
