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
package fr.ird.osmose.output.spatial;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.output.IOutput;
import fr.ird.osmose.output.distribution.OutputDistribution;
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
import ucar.nc2.Variable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 *
 * @author pverley
 */
public class SpatialSizeSpeciesOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;

    /**
     * Size/Age distribution.
     */
    private final OutputDistribution distrib;

    private double timeOut;
    private int counter;

    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFormatWriter nc;
    private NetcdfFormatWriter.Builder bNc;


    // spatial indicators
    private float[][][][] abundance;

    private int ncindex;

    public SpatialSizeSpeciesOutput(int rank, OutputDistribution distrib) {
        super(rank);
        this.distrib = distrib;
    }

    @Override
    public void init() {

        Nc4Chunking chunker = getConfiguration().getChunker();

        /*
         * Create NetCDF file
         */
        String filename = getFilename();
        IOTools.makeDirectories(filename);
        bNc = NetcdfFormatWriter.createNewNetcdf4(getConfiguration().getNcOutVersion(), filename, chunker);

        /*
         * Create dimensions
         */
        Dimension speciesDim = bNc.addDimension("species", getNSpecies());
        Dimension ltlDim = bNc.addDimension("class", this.distrib.getNClass());
        Dimension columnsDim = bNc.addDimension("nx", getGrid().get_nx());
        Dimension linesDim = bNc.addDimension("ny", getGrid().get_ny());
        Dimension timeDim = bNc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        Variable.Builder<?> timeVarBuilder = bNc.addVariable("time", DataType.FLOAT, "time");
        timeVarBuilder.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        timeVarBuilder.addAttribute(new Attribute("calendar", "360_day"));
        timeVarBuilder.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Variable.Builder<?> abunVarBuilder = bNc.addVariable("abundance", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, ltlDim, speciesDim, linesDim, columnsDim)));
        abunVarBuilder.addAttribute(new Attribute("units", "number of fish"));
        abunVarBuilder.addAttribute(new Attribute("description", "Number of fish per species and per cell"));
        abunVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        bNc.addVariable("class", DataType.FLOAT, "class");
        /*nc.addVariableAttribute("class", "units", "number of fish");
        nc.addVariableAttribute("class", "description", "Number of fish per species and per cell");
        nc.addVariableAttribute("class", "_FillValue", -99.f);*/

        Variable.Builder<?> latVarBuilder = bNc.addVariable("latitude", DataType.FLOAT, new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        latVarBuilder.addAttribute(new Attribute("units", "degree"));
        latVarBuilder.addAttribute(new Attribute("description", "latitude of the center of the cell"));

        Variable.Builder<?> lonVarBuilder = bNc.addVariable("longitude", DataType.FLOAT, new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        lonVarBuilder.addAttribute(new Attribute("units", "degree"));
        lonVarBuilder.addAttribute(new Attribute("description", "longitude of the center of the cell"));

        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc = this.bNc.build();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(cell.get_jgrid(), cell.get_igrid(), cell.getLon());
                arrLat.set(cell.get_jgrid(), cell.get_igrid(), cell.getLat());
            }

            /* Writes out the class array. */
            ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.distrib.getNClass());
            arrClass.set(0, 0);
            for (int iclass = 1; iclass < this.distrib.getNClass(); iclass++) {
                arrClass.set(iclass, this.distrib.getThreshold(iclass - 1));
            }

            nc.write(nc.findVariable("class"), arrClass);
            nc.write(nc.findVariable("longitude"), arrLon);
            nc.write(nc.findVariable("latitude"), arrLat);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(SpatialSizeSpeciesOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() {
        try {
            nc.close();
            String strFilePart = this.getFilename();
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

    @Override
    public void update() {

        int iStepSimu = getSimulation().getIndexTimeSimu();
        if (this.isTimeToReset(iStepSimu)) {
            int nSpecies = getNSpecies();
            int nx = getGrid().get_nx();
            int ny = getGrid().get_ny();
            abundance = new float[getNClass()][nSpecies][ny][nx];
            this.counter = 0;
            this.timeOut = 0;
        }

        this.timeOut += (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
        this.counter += 1;

        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        int iSpec = school.getSpeciesIndex();
                        if (getConfiguration().isCutoffEnabled() && ((school.getAge() < getConfiguration().getCutoffAge()[iSpec]) || (school.getLength() < getConfiguration().getCutoffLength()[iSpec])))  {
                            //System.out.println("+++ cutoff ");
                            continue;
                        }
                        if (!school.isUnlocated()) {
                            //System.out.println("+++ unlocated ");
                            int classSchool = this.distrib.getClass(school);
                            if (classSchool >= 0) {
                                abundance[classSchool][iSpec][j][i] += school.getInstantaneousAbundance();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isTimeToReset(int iStepSimu) {
        return (((iStepSimu) % getConfiguration().getRecordFrequency()) == 0);
    }

    private int getNClass() {
        return this.distrib.getNClass();
    }

    @Override
    public void write(float time) {

        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int iclass = 0; iclass < this.distrib.getNClass(); iclass++) {
                    for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                        abundance[iclass][ispec][j][i] = FILLVALUE;
                    }
                }
            }
        }

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D5 arrAbundance = new ArrayFloat.D5(1, this.distrib.getNClass(), nSpecies, getGrid().get_ny(), getGrid().get_nx());

        for (int iclass = 0; iclass < this.distrib.getNClass(); iclass++) {
            for (int kspec = 0; kspec < nSpecies; kspec++) {
                for (int j = 0; j < getGrid().get_ny(); j++) {
                    for (int i = 0; i < getGrid().get_nx(); i++) {
                        if (!getGrid().getCell(i, j).isLand()) {
                            arrAbundance.set(0, iclass, kspec, j, i, abundance[iclass][kspec][j][i] / (float) this.counter);
                        } else {
                            arrAbundance.set(0, iclass, kspec, j, i, abundance[iclass][kspec][j][i]);
                        }
                    }
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) this.timeOut * 360 / (float) this.counter);

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(nc.findVariable("time"), new int[]{ncindex}, arrTime);
            nc.write(nc.findVariable("abundance"), new int[]{ncindex, 0, 0, 0, 0}, arrAbundance);
            ncindex++;
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(SpatialSizeSpeciesOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_spatialized").append(this.distrib.getType()).append("Species_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        // Always true, every time step should be written in the NetCDF file.
        return (((iStepSimu + 1) % getConfiguration().getRecordFrequency()) == 0);
    }
}
