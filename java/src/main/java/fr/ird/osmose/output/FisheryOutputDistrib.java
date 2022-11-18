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
import fr.ird.osmose.output.distribution.OutputDistribution;
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
 * Class that manages the input of fisheries data.
 *
 * @author Nicolas Barrier
 */
public class FisheryOutputDistrib extends SimulationLinker implements IOutput {

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
     * NetCDF time index.
     */
    private int index = 0;

    private int nClass;

    OutputDistribution distrib;

    /*
     * Array containing the fisheries catches by species and by fisheries.
     * Output has (species, fisheries) dimensions.
     */
    private double[][][] biomass;
    private double[][][] discards;
    private double[][][] accessibleBiomass;

    public FisheryOutputDistrib(int rank, OutputDistribution distrib) {
        super(rank);
        this.distrib = distrib;
    }

    @Override
    public void init() {

        // initializes the number of fisheries
        nFishery = getConfiguration().getNFishery();
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
        int nClass = distrib.getNClass();

        biomass = new double[nSpecies][nFishery][nClass];
        discards = new double[nSpecies][nFishery][nClass];
        accessibleBiomass = new double[nSpecies][nFishery][nClass];

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
        Dimension speciesDim = bNc.addDimension("species", getNSpecies() + this.getNBkgSpecies());
        Dimension fisheriesDim = bNc.addDimension("fishery", nFishery);
        Dimension classDim = bNc.addDimension("class", nClass);

        Dimension timeDim = bNc.addUnlimitedDimension("time");

        String attr = this.getSpeciesNames();
        String fisheryNames = this.getFisheriesNames();
        String classNames = this.getClassNames();

        /*
         * Add variables
         */
        Variable.Builder<?> timeVarBuilder = bNc.addVariable("time", DataType.FLOAT, "time");
        timeVarBuilder.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        timeVarBuilder.addAttribute(new Attribute("calendar", "360_day"));
        timeVarBuilder.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Variable.Builder<?> biomassVarBuilder = bNc.addVariable("landings", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim, classDim)));
        biomassVarBuilder.addAttribute(new Attribute("units", "ton"));
        biomassVarBuilder.addAttribute(new Attribute("description", "landings, in tons, by species and by fishery"));
        biomassVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));
        biomassVarBuilder.addAttribute(new Attribute("species_names", attr));
        biomassVarBuilder.addAttribute(new Attribute("fisheries_names", fisheryNames));
        biomassVarBuilder.addAttribute(new Attribute("class_names", classNames));

        Variable.Builder<?> discardsVarBuilder = bNc.addVariable("discards", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim, classDim)));
        discardsVarBuilder.addAttribute(new Attribute("units", "ton"));
        discardsVarBuilder.addAttribute(new Attribute("description", "discards, in tons, by species and by fishery"));
        discardsVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));
        discardsVarBuilder.addAttribute(new Attribute("species_names", attr));
        discardsVarBuilder.addAttribute(new Attribute("fisheries_names", fisheryNames));
        discardsVarBuilder.addAttribute(new Attribute("class_names", classNames));

        Variable.Builder<?> accessibleBiomassVarBuilder = bNc.addVariable("accessible_biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim, classDim)));
        accessibleBiomassVarBuilder.addAttribute(new Attribute("units", "ton"));
        accessibleBiomassVarBuilder.addAttribute(new Attribute("description", "accessible biomass, in tons, by species and by fishery"));
        accessibleBiomassVarBuilder.addAttribute(new Attribute("_FillValue", -99.f));
        accessibleBiomassVarBuilder.addAttribute(new Attribute("species_names", attr));
        accessibleBiomassVarBuilder.addAttribute(new Attribute("fisheries_names", fisheryNames));
        accessibleBiomassVarBuilder.addAttribute(new Attribute("class_names", classNames));



        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc = this.bNc.build();

        } catch (IOException ex) {
            Logger.getLogger(FisheryOutputDistrib.class.getName()).log(Level.SEVERE, null, ex);
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
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
        biomass = new double[nSpecies][nFishery][nClass];
        discards = new double[nSpecies][nFishery][nClass];
        accessibleBiomass = new double[nSpecies][nFishery][nClass];
    }

    @Override
    public void update() {

        getSchoolSet().getAliveSchools().forEach((school) -> {
            int iSpecies = school.getSpeciesIndex();
            int iClass = distrib.getClass(school);
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                biomass[iSpecies][iFishery][iClass] += school.getFishedBiomass(iFishery);
                discards[iSpecies][iFishery][iClass] += school.getDiscardedBiomass(iFishery);
                accessibleBiomass[iSpecies][iFishery][iClass] += school.getAccessibleBiomass(iFishery);
            }
        });


        this.getBkgSchoolSet().getAllSchools().forEach((bkgSch) -> {
            int iSpecies = bkgSch.getSpeciesIndex();
            int iClass = distrib.getClass(bkgSch);
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                biomass[iSpecies][iFishery][iClass] += bkgSch.getFishedBiomass(iFishery);
                discards[iSpecies][iFishery][iClass] += bkgSch.getDiscardedBiomass(iFishery);
                accessibleBiomass[iSpecies][iFishery][iClass] += bkgSch.getAccessibleBiomass(iFishery);
            }
        }
        );

    }

    @Override
    public void write(float time) {

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        int nBackground = this.getNBkgSpecies();
        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, nSpecies + nBackground, nFishery, nClass);
        ArrayFloat.D4 arrDiscards = new ArrayFloat.D4(1, nSpecies + nBackground, nFishery, nClass);
        ArrayFloat.D4 arrAccessBiomass = new ArrayFloat.D4(1, nSpecies + nBackground, nFishery, nClass);
        for (int iSpecies = 0; iSpecies < nSpecies + nBackground; iSpecies++) {
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                for (int iClass = 0; iClass < nClass; iClass++) {
                    arrBiomass.set(0, iSpecies, iFishery, iClass, (float) biomass[iSpecies][iFishery][iClass]);
                    arrDiscards.set(0, iSpecies, iFishery, iClass, (float) discards[iSpecies][iFishery][iClass]);
                    arrAccessBiomass.set(0, iSpecies, iFishery, iClass,
                            (float) accessibleBiomass[iSpecies][iFishery][iClass]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time * 360);

        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(nc.findVariable("time"), new int[]{index}, arrTime);
            nc.write(nc.findVariable("landings"), new int[]{index, 0, 0, 0}, arrBiomass);
            nc.write(nc.findVariable("discards"), new int[]{index, 0, 0, 0}, arrDiscards);
            nc.write(nc.findVariable("accessible_biomass"), new int[]{index, 0, 0, 0}, arrAccessBiomass);
            index++;
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(FisheryOutputDistrib.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_yieldByFishery");
        filename.append("DistribBy").append(distrib.getType()). append("_Simu");
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


    private String getClassNames() {

        StringBuilder strBuild = new StringBuilder();
        strBuild.append(distrib.getType()).append(":");
        strBuild.append(0);
        strBuild.append(",");
        for(int i = 1; i < distrib.getNClass() - 1; i++) {
            strBuild.append(distrib.getThreshold(i - 1));
            strBuild.append(",");
        }
        strBuild.append(distrib.getThreshold(distrib.getNClass() - 1 - 1));

        return strBuild.toString();

    }


}
