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

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Prey;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import ucar.ma2.ArrayFloat;
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
public class ResourceOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriter nc;
    /**
     * Resource biomass array at the beginning of the time step.
     */
    private HashMap<Integer, double[][]> rscBiomass0;
    /**
     * Resource biomass array after predation process.
     */
    private HashMap<Integer, double[][]> rscBiomass1;
    
    private int index;
    private Variable timeVar, rscBiomVar, rscBiomPredVar, lonVar, latVar;

    public ResourceOutput(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        this.rscBiomass0 = new HashMap<>();
        this.rscBiomass1 = new HashMap<>();
        String filename = getFilename();
        IOTools.makeDirectories(filename);
        createNCFile(filename);
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
        rscBiomass0.clear();
        rscBiomass1.clear();
        for (int i : getConfiguration().getResourceIndex()) {
            rscBiomass0.put(i, new double[ny][nx]);
            rscBiomass1.put(i, new double[ny][nx]);
        }
    }

    @Override
    public void update() {

        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!(cell.isLand())) {
                // Preyed biomass for every resource group in current cell
                HashMap<Integer, Double> preyedResources = new HashMap();
                for(int i : this.getConfiguration().getResourceIndex()) { 
                    preyedResources.put(i, 0.0);
                }
                
                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        for (Prey prey : school.getPreys()) {
                            int iRsc = prey.getSpeciesIndex();                           
                            if (ArrayUtils.contains(this.getConfiguration().getResourceIndex(), iRsc)) {
                                double val = (null ==  preyedResources.get(iRsc)) ? prey.getBiomass() : preyedResources.get(iRsc) + prey.getBiomass();
                                preyedResources.put(iRsc, val);
                            }
                        }
                    }
                }
                
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                for (int iRsc : getConfiguration().getResourceIndex()) {
                    // rscBiomass0 is the resource biomass at the beginning of the time step
                    rscBiomass0.get(iRsc)[j][i] = getSimulation().getResourceForcing(iRsc).getBiomass(cell);
                    // rscBiomass1 is the resource biomass remaining in the water column after the predation process
                    rscBiomass1.get(iRsc)[j][i] = rscBiomass0.get(iRsc)[j][i] - preyedResources.get(iRsc);
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
                for (int iRsc = 0; iRsc < getConfiguration().getNRscSpecies(); iRsc++) {
                    int iRscFinal = this.getConfiguration().getRscIndex(iRsc);
                    rscBiomass0.get(iRscFinal)[j][i] = FILLVALUE;
                    rscBiomass1.get(iRscFinal)[j][i] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        int cpt = 0;
        ArrayFloat.D4 arrRsc0 = new ArrayFloat.D4(1, getConfiguration().getNRscSpecies(), getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrRsc1 = new ArrayFloat.D4(1, getConfiguration().getNRscSpecies(), getGrid().get_ny(), getGrid().get_nx());
        int nl = getGrid().get_ny() - 1;
        for (int iRsc = 0; iRsc < getConfiguration().getNRscSpecies(); iRsc++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    int iRscFinal = this.getConfiguration().getRscIndex(iRsc);
                    arrRsc0.set(0, cpt, j, i, (float) rscBiomass0.get(iRscFinal)[j][i]);
                    arrRsc1.set(0, cpt, j, i, (float) rscBiomass1.get(iRscFinal)[j][i]);
                }
            }
            cpt++;
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(rscBiomVar, new int[]{index, 0, 0, 0}, arrRsc0);
            nc.write(timeVar, new int[]{index}, arrTime);
            nc.write(this.rscBiomPredVar, new int[]{index, 0, 0, 0}, arrRsc1);
            this.index++;
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(ResourceOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createNCFile(String ncfile) {
        try {
            /*
             * Create NetCDF file
             */
            nc = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, ncfile);
        } catch (IOException ex) {
            Logger.getLogger(ResourceOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
 
        /*
         * Create dimensions
         */
        Dimension rscDim = nc.addDimension(null, "rsc", getConfiguration().getNRscSpecies());
        Dimension nxDim = nc.addDimension(null, "nx", getGrid().get_nx());
        Dimension nyDim = nc.addDimension(null, "ny", getGrid().get_ny());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        timeVar = nc.addVariable(null, "time", DataType.FLOAT, "time");
        timeVar.addAttribute(new Attribute("units", "days since 1-1-1 0:0:0"));
        timeVar.addAttribute(new Attribute("calendar", "360_day"));
        timeVar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));
        
        rscBiomVar = nc.addVariable(null, "rsc_biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, rscDim, nyDim, nxDim)));
        rscBiomVar.addAttribute(new Attribute("units", "tons per cell"));
        rscBiomVar.addAttribute(new Attribute("description", "resource biomass in osmose cell, in tons integrated on water column, per group and per cell"));
        rscBiomVar.addAttribute(new Attribute("_FillValue", -99.f));
        
        rscBiomPredVar = nc.addVariable(null, "rsc_biomass_pred", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, rscDim, nyDim, nxDim)));
        rscBiomPredVar.addAttribute(new Attribute("units", "tons per cell"));
        rscBiomPredVar.addAttribute(new Attribute("description", "resource biomass after predation process in osmose cell, in tons integrated on water column, per group and per cell"));
        rscBiomPredVar.addAttribute(new Attribute("_FillValue", -99.f));
        
        latVar = nc.addVariable(null, "latitude", DataType.FLOAT,  new ArrayList<>(Arrays.asList(nyDim, nxDim)));
        latVar.addAttribute(new Attribute( "units", "degree"));
        latVar.addAttribute(new Attribute("description", "latitude of the center of the cell"));
        
        lonVar = nc.addVariable(null, "longitude", DataType.FLOAT,  new ArrayList<>(Arrays.asList(nyDim, nxDim)));
        lonVar.addAttribute(new Attribute("units", "degree"));
        lonVar.addAttribute(new Attribute("description", "longitude of the center of the cell"));
        /*
         * Add global attributes
         */
        StringBuilder str = new StringBuilder();
        for (int kRsc = 0; kRsc < getConfiguration().getNRscSpecies(); kRsc++) {
            str.append(kRsc);
            str.append("=");
            str.append(getConfiguration().getResourceSpecies(kRsc));
            str.append(" ");
        }
        nc.addGroupAttribute(null, new Attribute("dimension_rsc", str.toString()));
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
            nc.write(lonVar, arrLon);
            nc.write(latVar, arrLat);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(ResourceOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append("resourceBiomass");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_rscbiomass_integrated_");
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
