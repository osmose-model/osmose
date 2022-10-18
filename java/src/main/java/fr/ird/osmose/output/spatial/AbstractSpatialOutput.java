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
import fr.ird.osmose.output.IOutput;
import java.util.ArrayList;
import java.util.Arrays;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 *
 * @author Nicolas Barrier
 */
public abstract class AbstractSpatialOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    protected final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    protected NetcdfFormatWriter nc;
    protected NetcdfFormatWriter.Builder bNc;

    // spatial indicators
    protected float[][][] data;

    public abstract String getVarName();

    public abstract String getDesc();

    public abstract void update();

    protected double timeOut;

    /**
     * Counter for saving.
     */
    protected int counter;

    private int index;

    public AbstractSpatialOutput(int rank) {
        super(rank);
    }

    private boolean includeClassZero() {
        return !getConfiguration().isCutoffEnabled();
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

        Variable.Builder<?> outVarBuilder = bNc.addVariable(this.getVarName(), DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, linesDim, columnsDim)));
        outVarBuilder.addAttribute(new Attribute("units", "number of fish"));
        outVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?> latVarBuilder = bNc.addVariable("latitude", DataType.FLOAT, new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        latVarBuilder.addAttribute(new Attribute("units", "degree"));
        latVarBuilder.addAttribute(new Attribute("description", "latitude of the center of the cell"));

        Variable.Builder<?> lonVarBuilder = bNc.addVariable("longitude", DataType.FLOAT, new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        lonVarBuilder.addAttribute(new Attribute("units", "degree"));
        lonVarBuilder.addAttribute(new Attribute("description", "longitude of the center of the cell"));
        /*
         * Add global attributes
         */
        bNc.addAttribute(new Attribute("dimension_step", "step=0 before predation, step=1 after predation"));
        StringBuilder str = new StringBuilder();
        for (int krsc = 0; krsc < getConfiguration().getNRscSpecies(); krsc++) {
            str.append(krsc);
            str.append("=");
            str.append(getConfiguration().getResourceSpecies(krsc));
            str.append(" ");
        }
        bNc.addAttribute(new Attribute("dimension_rsc", str.toString()));
        str = new StringBuilder();
        for (int ispec = 0; ispec < getConfiguration().getNSpecies(); ispec++) {
            str.append(ispec);
            str.append("=");
            str.append(getSpecies(ispec).getName());
            str.append(" ");
        }
        bNc.addAttribute(new Attribute("dimension_species", str.toString()));
        bNc.addAttribute(new Attribute("include_age_class_zero", Boolean.toString(includeClassZero())));
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            this.nc = this.bNc.build();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(cell.get_jgrid(), cell.get_igrid(), cell.getLon());
                arrLat.set(cell.get_jgrid(), cell.get_igrid(), cell.getLat());
            }
            try {
                nc.write(nc.findVariable("longitude"), arrLon);
                nc.write(nc.findVariable("latitude"), arrLat);
            } catch (InvalidRangeException ex) {
                Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
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

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(nc.findVariable("time"), new int[]{index}, arrTime);
            nc.write(nc.findVariable(this.getVarName()), new int[]{index, 0, 0, 0}, arrBiomass);
            this.incrementIndex();
        } catch (IOException | InvalidRangeException ex) {
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

    public boolean isTimeToReset(int iStepSimu) {
        return (((iStepSimu) % getConfiguration().getRecordFrequency()) == 0);
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        // Always true, every time step should be written in the NetCDF file.
        return (((iStepSimu + 1) % getConfiguration().getRecordFrequency()) == 0);
    }

    public void incrementIndex() {
        this.index++;
    }

    public int getNetcdfIndex() {
        return this.index;
    }

    public Variable getTimeVar() {
        return nc.findVariable("time");
    }

    public Variable getOutVar() {
        return nc.findVariable(this.getVarName());
    }

}
