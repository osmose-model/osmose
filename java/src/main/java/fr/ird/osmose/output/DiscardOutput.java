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
 * Class that manages the input of fisheries data.
 *
 * @author Nicolas Barrier
 */
public class DiscardOutput extends SimulationLinker implements IOutput {

    /*
     * Number of fisheries.
     */
    private int nFishery;
    /*
     * Object for creating/writing netCDF files.
     */
    private NetcdfFormatWriter nc;
    private NetcdfFormatWriter.Builder bNc;
    /*
     * NetCDF time and biomass variables.
     */
    private Variable timeVar, biomassVar;
    /*
     * NetCDF time index.
     */
    private int index = 0;
    /*
     * Array containing the fisheries catches by species and by fisheries.
     * Output has (species, fisheries) dimensions.
     */
    private float[][] biomass;

    public DiscardOutput(int rank) {
        super(rank);

    }

    @Override
    public void init() {

        // initializes the number of fisheries
        nFishery = getConfiguration().getNFishery();

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
        Dimension fisheriesDim = bNc.addDimension("fishery", nFishery);
        Dimension timeDim = bNc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        Variable.Builder<?> timeVarBuilder = bNc.addVariable("time", DataType.FLOAT, "time");
        timeVarBuilder.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        timeVarBuilder.addAttribute(new Attribute("calendar", "360_day"));
        timeVarBuilder.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Variable.Builder<?> biomassVarBuilder = bNc.addVariable("biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim)));
        biomassVarBuilder.addAttribute(new Attribute("units", "ton"));
        biomassVarBuilder.addAttribute(new Attribute("description", "discared biomass, in tons, per species and per cell"));
        biomassVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));

        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            this.nc = this.bNc.build();

        } catch (IOException ex) {
            Logger.getLogger(DiscardOutput.class.getName()).log(Level.SEVERE, null, ex);

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

    /**
     * Nothing to be done here.
     */
    @Override
    public void initStep() {
    }

    /**
     * Reset the biomass array. This is done at each time step.
     */
    @Override
    public void reset() {
        biomass = new float[getNSpecies()][nFishery];
    }

    @Override
    public void update() {

        getSchoolSet().getAliveSchools().forEach((school) -> {
            int iSpecies = school.getSpeciesIndex();
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                biomass[iSpecies][iFishery] += school.getDiscardedBiomass(iFishery);
            }
        });

    }

    @Override
    public void write(float time) {

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D3 arrBiomass = new ArrayFloat.D3(1, nSpecies, nFishery);
        for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                arrBiomass.set(0, iSpecies, iFishery, biomass[iSpecies][iFishery]);
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(timeVar, new int[]{index}, arrTime);
            nc.write(biomassVar, new int[]{index, 0, 0}, arrBiomass);
            index++;
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DiscardOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_discardOutput_Simu");
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
