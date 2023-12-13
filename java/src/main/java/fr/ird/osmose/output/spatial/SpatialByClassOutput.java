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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import fr.ird.osmose.Cell;
import fr.ird.osmose.IMarineOrganism;
import fr.ird.osmose.School;
import fr.ird.osmose.output.SchoolVariableGetter;
import fr.ird.osmose.output.distribution.DistributionType;
import fr.ird.osmose.output.distribution.OutputDistribution;
import fr.ird.osmose.util.io.IOTools;
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
public class SpatialByClassOutput extends AbstractSpatialOutput {

    private final String description;
    private final int speciesIndex;
    private final String variableName;
    private final OutputDistribution distribution;

    private interface SchoolSetMethod {
        public Stream<School> getSchoolSet();
    }

    SchoolSetMethod schoolSetMethod;

    SchoolVariableGetter variable;
    boolean computeAverage;
    boolean includeOnlyAlive;

    public SpatialByClassOutput(int rank, String variableName, String description, int indexSpecies,
            DistributionType type, SchoolVariableGetter variable, boolean computeAverage, boolean includeOnlyAlive) {
        super(rank);
        this.description = description;
        this.speciesIndex = indexSpecies;
        this.variableName = variableName;
        this.distribution = new OutputDistribution("output.distribution", type, speciesIndex);
        this.variable = variable;
        this.computeAverage = computeAverage;
        this.includeOnlyAlive = includeOnlyAlive;
        if (this.includeOnlyAlive) {
            schoolSetMethod = () -> getAliveOutputSchoolStream();
        } else {
            schoolSetMethod = () -> getAllOutputSchoolStream();
        }
    }

    public Stream<School> getAllOutputSchoolStream() {
        return this.getSchoolSet().getSchools().stream();
    }

    public Stream<School> getAliveOutputSchoolStream() {
        return this.getSchoolSet().getAliveSchools().stream();
    }

    public Stream<School> getOutputSchoolStream() {
        return schoolSetMethod.getSchoolSet();
    }

    public String getVarName() {
        return variableName;
    }

    public String getDesc() {
        return description;
    }

    @Override
    public void init() {

        this.distribution.init();
        DistributionType type = this.distribution.getType();

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
        Dimension classDim = bNc.addDimension(type.name(), this.distribution.getNClass());
        Dimension columnsDim = bNc.addDimension("nx", getGrid().get_nx());
        Dimension linesDim = bNc.addDimension("ny", getGrid().get_ny());
        Dimension timeDim = bNc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        Variable.Builder<?> timeVarBuilder = bNc.addVariable("time", DataType.FLOAT, "time");
        timeVarBuilder.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        timeVarBuilder.addAttribute(new Attribute("calendar", "360_day"));
        timeVarBuilder.addAttribute(
                new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Variable.Builder<?> outVarBuilder = bNc.addVariable(this.getVarName(), DataType.FLOAT,
                new ArrayList<>(Arrays.asList(timeDim, classDim, linesDim, columnsDim)));
        outVarBuilder.addAttribute(new Attribute("units", "number of fish"));
        outVarBuilder.addAttribute(new Attribute("_FillValue", FILLVALUE));

        Variable.Builder<?> latVarBuilder = bNc.addVariable("latitude", DataType.FLOAT,
                new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        latVarBuilder.addAttribute(new Attribute("units", "degree"));
        latVarBuilder.addAttribute(new Attribute("description", "latitude of the center of the cell"));

        Variable.Builder<?> lonVarBuilder = bNc.addVariable("longitude", DataType.FLOAT,
                new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        lonVarBuilder.addAttribute(new Attribute("units", "degree"));
        lonVarBuilder.addAttribute(new Attribute("description", "longitude of the center of the cell"));
        /*
         * Add global attributes
         */

        StringBuilder attribute = new StringBuilder();
        attribute.append("[0, ");
        for (Float thres : distribution.getThresholds()) {
            attribute.append(thres).append(",");
        }
        attribute.append(Float.MAX_VALUE).append(",");
        bNc.addAttribute(new Attribute("distribution_thresholds", attribute.toString()));
        bNc.addAttribute(new Attribute("distribution_type", type.name()));

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
    public void reset() {
        this.data = new float[distribution.getNClass()][getGrid().get_ny()][getGrid().get_nx()];
    }

    @Override
    public void update() {

        this.getOutputSchoolStream().forEach(school -> {

            int j = (int) school.getY();
            int i = (int) school.getX();
            int classSchool = getClass(school);

            if ((classSchool >= 0) & (i >= 0) & (j >= 0)) {
                float var = (float) variable.getVariable(school);
                data[classSchool][j][i] += var;
            }
        });
    }

    int getClass(IMarineOrganism school) {
        return distribution.getClass(school);
    }

    @Override
    public String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append("Spatial");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_spatial_").append(variableName);
        filename.append("by").append(distribution.getType());
        filename.append("-");
        filename.append(getConfiguration().getSpecies(speciesIndex).getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }


    @Override
    public void write(float time) {

        int nClass = this.distribution.getNClass();

        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int ispec = 0; ispec < nClass; ispec++) {
                    data[ispec][j][i] = FILLVALUE;
                }
            }
        }

        float denominator;
        if (this.computeAverage) {
            denominator = this.getConfiguration().getRecordFrequency();
        } else {
            denominator = 1f;
        }

        // Write into NetCDF file
        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, nClass, getGrid().get_ny(), getGrid().get_nx());
        for (int kspec = 0; kspec < nClass; kspec++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrBiomass.set(0, kspec, j, i, data[kspec][j][i] / denominator);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) this.timeOut * 360 / (float) this.counter);

        int index = this.getNetcdfIndex();
        try {
            nc.write(this.getTimeVar(), new int[]{index}, arrTime);
            nc.write(this.getOutVar(), new int[]{index, 0, 0, 0}, arrBiomass);
            this.incrementIndex();
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }




}
