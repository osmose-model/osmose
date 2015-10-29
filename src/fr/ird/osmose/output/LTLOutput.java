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

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.PreyRecord;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * @author pverley
 */
public class LTLOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;
    /**
     * LTL biomass array at the beginning of the time step.
     */
    private double[][][] ltlbiomass0;
    /**
     * LTL biomass array after predation process.
     */
    private double[][][] ltlbiomass1;

    public LTLOutput(int rank) {
        super(rank);
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
    }

    @Override
    public void init() {
        String filename = getFilename();
        IOTools.makeDirectories(filename);
        createNCFile(filename);
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
            warning("Problem closing the NetCDF output file ==> {0}", ex.toString());
        }
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();
        ltlbiomass0 = new double[getConfiguration().getNPlankton()][ny][nx];
        ltlbiomass1 = new double[getConfiguration().getNPlankton()][ny][nx];
    }

    @Override
    public void update() {
        
        int nspec = getNSpecies();
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                List<School> schools = getSchoolSet().getSchools(cell);
                // Preyed biomass for every LTL group in current cell
                double[] preyedLTL = new double[getConfiguration().getNPlankton()];
                for (School school : schools) {
                    for (PreyRecord prey : school.getPreyRecords()) {
                        int iltl = prey.getSpeciesIndex() - nspec;
                        if (iltl >= 0) {
                            preyedLTL[iltl] += prey.getBiomass();
                        }
                    }
                }
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                for (int iltl = 0; iltl < getConfiguration().getNPlankton(); iltl++) {
                    // ltl_biomass is the plankton biomass at the beginning of the time step
                    ltlbiomass0[iltl][j][i] = getSimulation().getForcing().getBiomass(iltl, cell);
                    // ltl_biomass_pred is the plankton biomass remaining in the water column after the predation process
                    ltlbiomass1[iltl][j][i] = ltlbiomass0[iltl][j][i] - preyedLTL[iltl];
                }
            }
        }
    }

    @Override
    public void write(float time) {
        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int iltl = 0; iltl < getConfiguration().getNPlankton(); iltl++) {
                    ltlbiomass0[iltl][j][i] = FILLVALUE;
                    ltlbiomass1[iltl][j][i] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        ArrayFloat.D4 arrLTL0 = new ArrayFloat.D4(1, getConfiguration().getNPlankton(), getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrLTL1 = new ArrayFloat.D4(1, getConfiguration().getNPlankton(), getGrid().get_ny(), getGrid().get_nx());
        int nl = getGrid().get_ny() - 1;
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrLTL0.set(0, kltl, j, i, (float) ltlbiomass0[kltl][j][i]);
                    arrLTL1.set(0, kltl, j, i, (float) ltlbiomass1[kltl][j][i]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write("ltl_biomass", new int[]{index, 0, 0, 0}, arrLTL0);
            nc.write("ltl_biomass_pred", new int[]{index, 0, 0, 0}, arrLTL1);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createNCFile(String ncfile) {
        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            nc.setLocation(ncfile);
        } catch (IOException ex) {
            Logger.getLogger(LTLOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension ltlDim = nc.addDimension("ltl", getConfiguration().getNPlankton());
        Dimension nxDim = nc.addDimension("nx", getGrid().get_nx());
        Dimension nyDim = nc.addDimension("ny", getGrid().get_ny());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "days since 1-1-1 0:0:0");
        nc.addVariableAttribute("time", "calendar", "360_day");
        nc.addVariableAttribute("time", "description", "time ellapsed, in days, since the beginning of the simulation");
        nc.addVariable("ltl_biomass", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, nyDim, nxDim});
        nc.addVariableAttribute("ltl_biomass", "units", "tons per cell");
        nc.addVariableAttribute("ltl_biomass", "description", "plankton biomass in osmose cell, in tons integrated on water column, per group and per cell");
        nc.addVariableAttribute("ltl_biomass", "_FillValue", -99.f);
        nc.addVariable("ltl_biomass_pred", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, nyDim, nxDim});
        nc.addVariableAttribute("ltl_biomass_pred", "units", "tons per cell");
        nc.addVariableAttribute("ltl_biomass_pred", "description", "plankton biomass after predation process in osmose cell, in tons integrated on water column, per group and per cell");
        nc.addVariableAttribute("ltl_biomass_pred", "_FillValue", -99.f);
        nc.addVariable("latitude", DataType.FLOAT, new Dimension[]{nyDim, nxDim});
        nc.addVariableAttribute("latitude", "units", "degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        nc.addVariable("longitude", DataType.FLOAT, new Dimension[]{nyDim, nxDim});
        nc.addVariableAttribute("longitude", "units", "degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        /*
         * Add global attributes
         */
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getConfiguration().getPlankton(kltl));
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_ltl", str.toString());
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
            Logger.getLogger(LTLOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(LTLOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append("planktonBiomass");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_ltlbiomass_integrated_");
        filename.append("Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        // Always true, every time step should be written in the NetCDF file.
        return true;
    }
}
