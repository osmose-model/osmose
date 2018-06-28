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
package fr.ird.osmose.output.spatial;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import fr.ird.osmose.output.IOutput;
        
/**
 *
 * @author pverley
 */
public abstract class AbstractSpatialOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;

    // spatial indicators
    protected float[][][] data;

    protected boolean cutoffEnabled;
    
    public abstract String getVarName();
    public abstract String getDesc();
    public abstract void update();
    
    protected double timeOut;

            
    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    protected float[] cutoffAge;
    
    /** Record frequency. */
    private int recordFrequency;
    
    /** Counter for saving. */
    protected int counter;
    
    public AbstractSpatialOutput(int rank) {
        super(rank);
    }

    private boolean includeClassZero() {
        return !cutoffEnabled;
    }

    @Override
    public void init() {
        
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        // cutoff for egg, larvae and juveniles
        cutoffEnabled = getConfiguration().getBoolean("output.cutoff.enabled");
        cutoffAge = new float[getNSpecies()];
        if (cutoffEnabled) {
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                cutoffAge[iSpec] = getConfiguration().getFloat("output.cutoff.age.sp" + iSpec);
            }
        }

        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc.setLocation(filename);
        } catch (IOException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension speciesDim = nc.addDimension("species", getNSpecies());
        Dimension columnsDim = nc.addDimension("nx", getGrid().get_nx());
        Dimension linesDim = nc.addDimension("ny", getGrid().get_ny());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "days since 0-1-1 0:0:0");
        nc.addVariableAttribute("time", "calendar", "360_day");
        nc.addVariableAttribute("time", "description", "time ellapsed, in days, since the beginning of the simulation");
        
        nc.addVariable(this.getVarName(), DataType.FLOAT, new Dimension[]{timeDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute(this.getVarName(), "units", "number of fish");
        nc.addVariableAttribute(this.getVarName(), "_FillValue", -99.f);
        
        nc.addVariable("latitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("latitude", "units", "degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        
        nc.addVariable("longitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("longitude", "units", "degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        /*
         * Add global attributes
         */
        nc.addGlobalAttribute("dimension_step", "step=0 before predation, step=1 after predation");
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getConfiguration().getPlankton(kltl));
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_ltl", str.toString());
        str = new StringBuilder();
        for (int ispec = 0; ispec < getConfiguration().getNSpecies(); ispec++) {
            str.append(ispec);
            str.append("=");
            str.append(getSpecies(ispec).getName());
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_species", str.toString());
        nc.addGlobalAttribute("include_age_class_zero", Boolean.toString(includeClassZero()));
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(cell.get_jgrid(), cell.get_igrid(), cell.getLon());
                arrLat.set(cell.get_jgrid(), cell.get_igrid(), cell.getLat());
            }
            nc.write("longitude", arrLon);
            nc.write("latitude", arrLat);
        } catch (IOException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    @Override
    public void initStep() {
    }

    @Override
    public void reset() {

    }
    
    protected void common_update() {
        int iStepSimu = getSimulation().getIndexTimeSimu();
        if (this.isTimeToReset(iStepSimu)) {
            int nSpecies = getNSpecies();
            int nx = getGrid().get_nx();
            int ny = getGrid().get_ny();
            data = new float[nSpecies][ny][nx];
            this.counter = 0;
            this.timeOut = 0;
        }

        this.timeOut += (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
        this.counter += 1;
    }
    
    @Override
    public void write(float time) {

        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                    data[ispec][j][i] = FILLVALUE;
                }
            } 
        }

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        for (int kspec = 0; kspec < nSpecies; kspec++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    if (!getGrid().getCell(i, j).isLand()) {
                        arrBiomass.set(0, kspec, j, i, (data[kspec][j][i] / ((float) this.counter)));
                    } else {
                        arrBiomass.set(0, kspec, j, i, data[kspec][j][i]);
                    }
                }
            }
        }
        
        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) this.timeOut * 360 / (float) this.counter);
        
        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write(this.getVarName(), new int[]{index, 0, 0, 0}, arrBiomass);
        } catch (IOException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_spatialized").append(this.getVarName()).append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    public boolean isTimeToReset(int iStepSimu)
    {
        return (((iStepSimu) % recordFrequency) == 0);
    }
    
    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        // Always true, every time step should be written in the NetCDF file.
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }
}
