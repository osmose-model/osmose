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
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * Class that manages the input of fisheries data.
 *
 * @author Nicolas Barrier
 */
public class FisheryOutput extends SimulationLinker implements IOutput {

    /*
     * Number of fisheries.
     */
    private int nFishery;
    /*
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriter nc;
    /*
     * NetCDF time and biomass variables.
     */
    private Variable timeVar, biomassVar, discardsVar, accessibleBiomassVar;
    /*
     * NetCDF time index.
     */
    private int index = 0;
    /*
     * Array containing the fisheries catches by species and by fisheries.
     * Output has (species, fisheries) dimensions.
     */
    private double[][] biomass;
    private double[][] discards;
    private double[][] accessibleBiomass;

    public FisheryOutput(int rank) {
        super(rank);

    }

    @Override
    public void init() {

        // initializes the number of fisheries
        nFishery = getConfiguration().getNFishery();
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
        biomass = new double[nSpecies][nFishery];
        discards = new double[nSpecies][nFishery];
        accessibleBiomass = new double[nSpecies][nFishery];

        /*
         * Create NetCDF file
         */
        try {
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc = NetcdfFileWriter.createNew(getConfiguration().getNcOutVersion(), filename);
        } catch (IOException ex) {
            Logger.getLogger(FisheryOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*
         * Create dimensions
         */
        Dimension speciesDim = nc.addDimension(null, "species", getNSpecies() + this.getNBkgSpecies());
        Dimension fisheriesDim = nc.addDimension(null, "fishery", nFishery);
        Dimension timeDim = nc.addUnlimitedDimension("time");

        String attr = this.getSpeciesNames();
        String fisheryNames = this.getFisheriesNames();

        /*
         * Add variables
         */
        timeVar = nc.addVariable(null, "time", DataType.FLOAT, "time");
        timeVar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        timeVar.addAttribute(new Attribute("calendar", "360_day"));
        timeVar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        biomassVar = nc.addVariable(null, "landings", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim)));
        biomassVar.addAttribute(new Attribute("units", "ton"));
        biomassVar.addAttribute(new Attribute("description", "landings, in tons, by species and by fishery"));
        biomassVar.addAttribute(new Attribute("_FillValue", -99.f));
        biomassVar.addAttribute(new Attribute("species_names", attr));
        biomassVar.addAttribute(new Attribute("fisheries_names", fisheryNames));

        discardsVar = nc.addVariable(null, "discards", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim)));
        discardsVar.addAttribute(new Attribute("units", "ton"));
        discardsVar.addAttribute(new Attribute("description", "discards, in tons, by species and by fishery"));
        discardsVar.addAttribute(new Attribute("_FillValue", -99.f));
        discardsVar.addAttribute(new Attribute("species_names", attr));
        discardsVar.addAttribute(new Attribute("fisheries_names", fisheryNames));

        accessibleBiomassVar = nc.addVariable(null, "accessible_biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim)));
        accessibleBiomassVar.addAttribute(new Attribute("units", "ton"));
        accessibleBiomassVar.addAttribute(new Attribute("description", "accessible biomass, in tons, by species and by fishery"));
        accessibleBiomassVar.addAttribute(new Attribute("_FillValue", -99.f));
        accessibleBiomassVar.addAttribute(new Attribute("species_names", attr));
        accessibleBiomassVar.addAttribute(new Attribute("fisheries_names", fisheryNames));


        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();

        } catch (IOException ex) {
            Logger.getLogger(FisheryOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
        biomass = new double[nSpecies][nFishery];
        discards = new double[nSpecies][nFishery];
        accessibleBiomass = new double[nSpecies][nFishery];
    }

    @Override
    public void update() {

        getSchoolSet().getAliveSchools().forEach((school) -> {
            int iSpecies = school.getSpeciesIndex();
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                biomass[iSpecies][iFishery] += school.getFishedBiomass(iFishery);
                discards[iSpecies][iFishery] += school.getDiscardedBiomass(iFishery);
                accessibleBiomass[iSpecies][iFishery] += school.getAccessibleBiomass(iFishery);
            }
        });


        this.getBkgSchoolSet().getAllSchools().forEach((bkgSch) -> {
            int iSpecies = bkgSch.getSpeciesIndex();
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                biomass[iSpecies][iFishery] += bkgSch.getFishedBiomass(iFishery);
                discards[iSpecies][iFishery] += bkgSch.getDiscardedBiomass(iFishery);
                accessibleBiomass[iSpecies][iFishery] += bkgSch.getAccessibleBiomass(iFishery);
            }
        }
        );

    }

    @Override
    public void write(float time) {

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        int nBackground = this.getNBkgSpecies();
        ArrayFloat.D3 arrBiomass = new ArrayFloat.D3(1, nSpecies + nBackground, nFishery);
        ArrayFloat.D3 arrDiscards = new ArrayFloat.D3(1, nSpecies + nBackground, nFishery);
        ArrayFloat.D3 arrAccessBiomass = new ArrayFloat.D3(1, nSpecies + nBackground, nFishery);
        for (int iSpecies = 0; iSpecies < nSpecies + nBackground; iSpecies++) {
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                arrBiomass.set(0, iSpecies, iFishery, (float) biomass[iSpecies][iFishery]);
                arrDiscards.set(0, iSpecies, iFishery, (float) discards[iSpecies][iFishery]);
                arrAccessBiomass.set(0, iSpecies, iFishery, (float) accessibleBiomass[iSpecies][iFishery]);
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(timeVar, new int[]{index}, arrTime);
            nc.write(biomassVar, new int[]{index, 0, 0}, arrBiomass);
            nc.write(discardsVar, new int[]{index, 0, 0}, arrDiscards);
            nc.write(accessibleBiomassVar, new int[]{index, 0, 0}, arrAccessBiomass);
            index++;
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(FisheryOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_yieldByFishery_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        // Always true, every time step should be written in the NetCDF file.
        return true;
    }

    /**
     * Get species names for attributes.
     *
     * @return
     */
    private String getSpeciesNames() {
        StringBuilder strBuild = new StringBuilder();

        int cpt = 0;
        for (cpt = 0; cpt < this.getNSpecies(); cpt++) {
            strBuild.append(getSpecies(cpt).getName());
            strBuild.append(", ");
        }

        cpt = 0;
        for (cpt = 0; cpt < this.getNBkgSpecies(); cpt++) {
            strBuild.append(getBkgSpecies(cpt).getName());
            strBuild.append(", ");
        }

        String output = strBuild.toString().trim();
        if (output.endsWith(",")) {
            int comIndex = output.lastIndexOf(",");
            output = output.substring(0, comIndex);
        }

        return output;

    }

    /**
     * Get species names for attributes.
     *
     * @return
     */
    private String getFisheriesNames() {

        StringBuilder strBuild = new StringBuilder();

        // Recovers the index of fisheries
        int[] fisheryIndex = this.getConfiguration().findKeys("fisheries.name.fsh*").stream()
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).sorted().toArray();

        for(int cpt = 0; cpt < fisheryIndex.length; cpt++) {
            int fileFisheryIndex = fisheryIndex[cpt];
            String fisheryName = this.getConfiguration().getString("fisheries.name.fsh" + fileFisheryIndex);
            strBuild.append(fisheryName);
            strBuild.append(", ");
        }

        String output = strBuild.toString().trim();
        if (output.endsWith(",")) {
            int comIndex = output.lastIndexOf(",");
            output = output.substring(0, comIndex);
        }

        return output;

    }


}
