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
import fr.ird.osmose.process.mortality.MortalityCause;
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
public class SpatialOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFormatWriter nc;
    private NetcdfFormatWriter.Builder bNc;

    // spatial indicators
    private float[][][] biomass;
    private float[][][] mean_size;
    private float[][][] tl;
    private float[][][] ltlbiomass;
    private float[][][] abundance;
    private float[][][] yield;
    private int index;

    private Variable timevar, lonvar, latvar, biomassVar, abundanceVar, yieldVar, meanSizeVar, tlVar, ltlbiomVar;

    public SpatialOutput(int rank) {
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
        Dimension ltlDim = bNc.addDimension("ltl", getConfiguration().getNRscSpecies());
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

        Variable.Builder<?> biomassVarBuilder = bNc.addVariable("biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, linesDim, columnsDim)));
        biomassVarBuilder.addAttribute(new Attribute("units", "ton"));
        biomassVarBuilder.addAttribute(new Attribute("description", "biomass, in tons, per species and per cell"));
        biomassVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?> abundanceVarBuilder = bNc.addVariable("abundance", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, linesDim, columnsDim)));
        abundanceVarBuilder.addAttribute(new Attribute("units", "number of fish"));
        abundanceVarBuilder.addAttribute(new Attribute("description", "Number of fish per species and per cell"));
        abundanceVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?> yieldVarBuilder = bNc.addVariable("yield", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, linesDim, columnsDim)));
        yieldVarBuilder.addAttribute(new Attribute("units", "ton"));
        yieldVarBuilder.addAttribute(new Attribute("description", "Catches, in tons, per species and per cell"));
        yieldVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?> meanSizeVarBuilder = bNc.addVariable("mean_size", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, linesDim, columnsDim)));
        meanSizeVarBuilder.addAttribute(new Attribute("units", "centimeter"));
        meanSizeVarBuilder.addAttribute(new Attribute("description", "mean size, in centimeter, per species and per cell"));
        meanSizeVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?>  tlVarBuilder = bNc.addVariable("trophic_level", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, linesDim, columnsDim)));
        tlVarBuilder.addAttribute(new Attribute("units", "scalar"));
        tlVarBuilder.addAttribute(new Attribute("description", "trophic level per species and per cell"));
        tlVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?>  ltlbiomVarBuilder = bNc.addVariable("ltl_biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, ltlDim, linesDim, columnsDim)));
        ltlbiomVarBuilder.addAttribute(new Attribute("units", "ton/km2"));
        ltlbiomVarBuilder.addAttribute(new Attribute("description", "resource biomass, in tons per km2 integrated on water column, per group and per cell"));
        ltlbiomVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        Variable.Builder<?>  latvarBuilder = bNc.addVariable("latitude", DataType.FLOAT, new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        latvarBuilder.addAttribute(new Attribute("units", "degree"));
        latvarBuilder.addAttribute(new Attribute("description", "latitude of the center of the cell"));

        Variable.Builder<?>  lonvarBuilder = bNc.addVariable("longitude", DataType.FLOAT, new ArrayList<>(Arrays.asList(linesDim, columnsDim)));
        lonvarBuilder.addAttribute(new Attribute("units", "degree"));
        lonvarBuilder.addAttribute(new Attribute("description", "longitude of the center of the cell"));
        /*
         * Add global attributes
         */
        bNc.addAttribute(new Attribute("dimension_step", "step=0 before predation, step=1 after predation"));
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getConfiguration().getNRscSpecies(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getConfiguration().getResourceSpecies(kltl));
            str.append(" ");
        }
        bNc.addAttribute(new Attribute("dimension_ltl", str.toString()));
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
            nc.write(lonvar, arrLon);
            nc.write(latvar, arrLat);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
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

        int nSpecies = getNSpecies();
        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();
        biomass = new float[nSpecies][ny][nx];
        mean_size = new float[nSpecies][ny][nx];
        tl = new float[nSpecies][ny][nx];
        ltlbiomass = new float[getConfiguration().getNRscSpecies()][ny][nx];
        abundance = new float[nSpecies][ny][nx];
        yield = new float[nSpecies][ny][nx];
    }

    @Override
    public void update() {
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        if (getConfiguration().isCutoffEnabled() && ((school.getAge() < getConfiguration().getCutoffAge()[school.getSpeciesIndex()]) || school.getLength() < getConfiguration().getCutoffLength()[school.getSpeciesIndex()])) {
                            continue;
                        }
                        if (!school.isUnlocated()) {
                            int iSpec = school.getSpeciesIndex();
                            biomass[iSpec][j][i] += school.getInstantaneousBiomass();
                            abundance[iSpec][j][i] += school.getInstantaneousAbundance();
                            mean_size[iSpec][j][i] += school.getLength() * school.getInstantaneousAbundance();
                            tl[iSpec][j][i] += school.getTrophicLevel() * school.getInstantaneousBiomass();
                            yield[iSpec][j][i] += school.abd2biom(school.getNdead(MortalityCause.FISHING));
                        }
                    }
                }

                int offset = this.getNBkgSpecies();
                for (int iRsc = 0; iRsc < getConfiguration().getNRscSpecies(); iRsc++) {
                    ltlbiomass[iRsc][j][i] = (float) getSimulation().getResourceForcing(iRsc + offset).getBiomass(cell);
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
                for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                    biomass[ispec][j][i] = FILLVALUE;
                    abundance[ispec][j][i] = FILLVALUE;
                    mean_size[ispec][j][i] = FILLVALUE;
                    tl[ispec][j][i] = FILLVALUE;
                    yield[ispec][j][i] = FILLVALUE;
                }
                for (int iltl = 0; iltl < getConfiguration().getNRscSpecies(); iltl++) {
                    ltlbiomass[iltl][j][i] = FILLVALUE;
                }
            } else {
                // Weight mean size with abundance
                // Weight mean size with biomass
                for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                    if (abundance[ispec][j][i] > 0) {
                        mean_size[ispec][j][i] /= abundance[ispec][j][i];
                        tl[ispec][j][i] /= biomass[ispec][j][i];
                    }
                }
            }
        }

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrAbundance = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrYield = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrSize = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrTL = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        ArrayFloat.D4 arrLTL = new ArrayFloat.D4(1, getConfiguration().getNRscSpecies(), getGrid().get_ny(), getGrid().get_nx());
        for (int kspec = 0; kspec < nSpecies; kspec++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrBiomass.set(0, kspec, j, i, biomass[kspec][j][i]);
                    arrAbundance.set(0, kspec, j, i, abundance[kspec][j][i]);
                    arrSize.set(0, kspec, j, i, mean_size[kspec][j][i]);
                    arrTL.set(0, kspec, j, i, tl[kspec][j][i]);
                    arrYield.set(0, kspec, j, i, yield[kspec][j][i]);
                }
            }
        }
        for (int kltl = 0; kltl < getConfiguration().getNRscSpecies(); kltl++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrLTL.set(0, kltl, j, i, ltlbiomass[kltl][j][i]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(timevar, new int[]{index}, arrTime);
            nc.write(this.biomassVar, new int[]{index, 0, 0, 0}, arrBiomass);
            nc.write(this.abundanceVar, new int[]{index, 0, 0, 0}, arrAbundance);
            nc.write(this.yieldVar, new int[]{index, 0, 0, 0}, arrYield);
            nc.write(this.meanSizeVar, new int[]{index, 0, 0, 0}, arrSize);
            nc.write(this.tlVar, new int[]{index, 0, 0, 0}, arrTL);
            nc.write(this.ltlbiomVar, new int[]{index, 0, 0, 0, 0}, arrLTL);
            index++;
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_spatialized_Simu");
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
