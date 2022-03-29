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

package fr.ird.osmose.output.netcdf;

import fr.ird.osmose.School;
import fr.ird.osmose.output.IOutput;
import fr.ird.osmose.util.SimulationLinker;
import fr.ird.osmose.util.io.IOTools;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

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
    private NetcdfFileWriter nc;
    private int record_index;

    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;
    private List<Dimension> outDims;

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
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc = NetcdfFileWriter.createNew(getConfiguration().getNcOutVersion(), filename);
        } catch (IOException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Add time dim and variable (common to all files)
        timeDim = nc.addUnlimitedDimension("time");
        
        Variable tvar = nc.addVariable(null, "time", DataType.DOUBLE, "time");
        tvar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        tvar.addAttribute(new Attribute("calendar", "360_day"));
        tvar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        // Init NC dimensions and coords (in define mode)
        this.init_nc_dims_coords();

        // Create output variable
        Variable outvar = nc.addVariable(null, getVarname(), DataType.FLOAT, this.getNcDims());
        outvar.addAttribute(new Attribute("units", getUnits()));
        outvar.addAttribute(new Attribute("description", getDescription()));
        outvar.addAttribute(new Attribute("_FillValue", getFillValue()));

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
        return ((!cutoffEnabled) || (school.getAge() >= cutoffAge[school.getFileSpeciesIndex()]));
    }

    @Override
    public void close() {
        try {
            nc.close();
            String strFilePart = nc.getNetcdfFile().getLocation();
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
     * @return 
     */
    public String createSpeciesAttr() {

        Variable species = this.nc.findVariable("species");
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < this.getNSpecies(); i++) {
            String attrname = String.format("species%d", i);
            String attval = this.getSpecies(i).getName();
            species.addAttribute(new Attribute(attrname, attval));
        }

        return bld.toString();

    }

    public int getTimeIndex() {
        return this.record_index;
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
            Variable tvar = nc.findVariable("time");
            nc.write(tvar, new int[]{index}, arrTime);
            Variable outvar = nc.findVariable(this.getVarname());
            nc.write(outvar, new int[]{index, 0}, arrAbund);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.incrementIndex();    
    }

    public float getFillValue() {
        return this.FILLVALUE;
    }

    public List<Dimension> getNcDims() {
        return this.outDims;
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    void init_nc_dims_coords() {

        Dimension speciesDim = nc.addDimension(null, "species", getNSpecies());
        Variable species = nc.addVariable(null, "species", DataType.INT, "species");
        this.createSpeciesAttr();
        outDims = new ArrayList<>();
        outDims.add(timeDim);
        outDims.add(speciesDim);
                
    }
    
    public void write_nc_coords() {

        // Writes variable trait (trait names) and species (species names)
        ArrayInt.D1 arrSpecies = new ArrayInt.D1(this.getNSpecies());

        for (int i = 0; i < this.getNSpecies(); i++) {
            arrSpecies.set(i, i);
        }

        Variable varspec = this.nc.findVariable("species");

        try {
            nc.write(varspec, arrSpecies);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public NetcdfFileWriter getNc() {
        return this.nc;
    }

    public void setDims(List<Dimension> dims) {
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
            Variable tvar = nc.findVariable("time");
            nc.write(tvar, new int[]{index}, arrTime);
            Variable outvar = nc.findVariable(this.getVarname());
            nc.write(outvar, new int[]{index, 0, 0}, arrAbund);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.incrementIndex();
    }

    public StringBuilder initFileName() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        return filename;
    }
    
    public void incrementIndex() {
        this.record_index++;
    }

}
