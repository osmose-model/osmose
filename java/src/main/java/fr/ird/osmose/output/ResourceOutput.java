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
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.NetcdfFormatWriter;
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
    private NetcdfFormatWriter nc;
    private NetcdfFormatWriter.Builder bNc;
    /**
     * Resource biomass array at the beginning of the time step.
     */
    private double[][][] rscBiomass0;
    /**
     * Resource biomass array after predation process.
     */
    private double[][][] rscBiomass1;

    private int index;

    public ResourceOutput(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        int nRsc = this.getNRscSpecies();
        this.rscBiomass0 = new double[nRsc][][];
        this.rscBiomass1 = new double[nRsc][][];
        String filename = getFilename();
        IOTools.makeDirectories(filename);
        createNCFile(filename);
    }

    @Override
    public void close() {
        try {
            nc.close();
            String strFilePart = getFilename();
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
        int nRsc = this.getNRscSpecies();
        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();
        this.rscBiomass0 = new double[nRsc][][];
        this.rscBiomass1 = new double[nRsc][][];
        for (int cpt = 0; cpt < nRsc; cpt++) {
            rscBiomass0[cpt] = new double[ny][nx];
            rscBiomass1[cpt] = new double[ny][nx];
        }
    }

    @Override
    public void update() {
        int nRsc = this.getNRscSpecies();
        int nSpecies = this.getNSpecies();
        int nBkg = this.getNBkgSpecies();
        int offset = nBkg + nSpecies;
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!(cell.isLand())) {
                // Preyed biomass for every resource group in current cell
                double[] preyedResources = new double[nRsc];

                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        for (Prey prey : school.getPreys()) {
                            int iRsc = prey.getSpeciesIndex();
                            if (iRsc >= offset) {
                                // check that we are attacking a resource species
                                preyedResources[iRsc - offset] += prey.getBiomass();
                            }
                        }
                    }
                }

                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                int nBackground = this.getNBkgSpecies();
                for (int cpt = 0; cpt < this.getNRscSpecies(); cpt++) {
                    // rscBiomass0 is the resource biomass at the beginning of the time step
                    // adding an offset, since resourceForcing starts with bkgSpecies
                    rscBiomass0[cpt][j][i] = getSimulation().getResourceForcing(cpt + nBackground).getBiomass(cell);
                    // rscBiomass1 is the resource biomass remaining in the water column after the predation process
                    rscBiomass1[cpt][j][i] = rscBiomass0[cpt][j][i] - preyedResources[cpt];
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
                    rscBiomass0[iRsc][j][i] = FILLVALUE;
                    rscBiomass1[iRsc][j][i] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        ArrayFloat.D4 arrRsc0 = new ArrayFloat.D4(1, getConfiguration().getNRscSpecies(), getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrRsc1 = new ArrayFloat.D4(1, getConfiguration().getNRscSpecies(), getGrid().get_ny(), getGrid().get_nx());
        for (int iRsc = 0; iRsc < getConfiguration().getNRscSpecies(); iRsc++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrRsc0.set(0, iRsc, j, i, (float) rscBiomass0[iRsc][j][i]);
                    arrRsc1.set(0, iRsc, j, i, (float) rscBiomass1[iRsc][j][i]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(nc.findVariable("rsc_biomass"), new int[]{index, 0, 0, 0}, arrRsc0);
            nc.write(nc.findVariable("time"), new int[]{index}, arrTime);
            nc.write(nc.findVariable("rsc_biomass_pred"), new int[]{index, 0, 0, 0}, arrRsc1);
            this.index++;
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(ResourceOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createNCFile(String ncfile) {

        Nc4Chunking chunker = getConfiguration().getChunker();

        bNc = NetcdfFormatWriter.createNewNetcdf4(getConfiguration().getNcOutVersion(), ncfile, chunker);

        /*
         * Create dimensions
         */
        Dimension rscDim = bNc.addDimension("rsc", getConfiguration().getNRscSpecies());
        Dimension nxDim = bNc.addDimension("nx", getGrid().get_nx());
        Dimension nyDim = bNc.addDimension("ny", getGrid().get_ny());
        Dimension timeDim = bNc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        Variable.Builder<?> timeVarBuilder = bNc.addVariable("time", DataType.FLOAT, "time");
        timeVarBuilder.addAttribute(new Attribute("units", "days since 1-1-1 0:0:0"));
        timeVarBuilder.addAttribute(new Attribute("calendar", "360_day"));
        timeVarBuilder.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Variable.Builder<?> rscBiomVarBuilder = bNc.addVariable("rsc_biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, rscDim, nyDim, nxDim)));
        rscBiomVarBuilder.addAttribute(new Attribute("units", "tons per cell"));
        rscBiomVarBuilder.addAttribute(new Attribute("description", "resource biomass in osmose cell, in tons integrated on water column, per group and per cell"));
        rscBiomVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?> rscBiomPredVarBuilder = bNc.addVariable("rsc_biomass_pred", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, rscDim, nyDim, nxDim)));
        rscBiomPredVarBuilder.addAttribute(new Attribute("units", "tons per cell"));
        rscBiomPredVarBuilder.addAttribute(new Attribute("description", "resource biomass after predation process in osmose cell, in tons integrated on water column, per group and per cell"));
        rscBiomPredVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?> latVarBuilder = bNc.addVariable("latitude", DataType.FLOAT,  new ArrayList<>(Arrays.asList(nyDim, nxDim)));
        latVarBuilder.addAttribute(new Attribute( "units", "degree"));
        latVarBuilder.addAttribute(new Attribute("description", "latitude of the center of the cell"));

        Variable.Builder<?> lonVarBuilder = bNc.addVariable("longitude", DataType.FLOAT,  new ArrayList<>(Arrays.asList(nyDim, nxDim)));
        lonVarBuilder.addAttribute(new Attribute("units", "degree"));
        lonVarBuilder.addAttribute(new Attribute("description", "longitude of the center of the cell"));
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
        bNc.addAttribute(new Attribute("dimension_rsc", str.toString()));
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc = bNc.build();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(cell.get_jgrid(), cell.get_igrid(), cell.getLon());
                arrLat.set(cell.get_jgrid(), cell.get_igrid(), cell.getLat());
            }
            nc.write(nc.findVariable("longitude"), arrLon);
            nc.write(nc.findVariable("latitude"), arrLat);
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
