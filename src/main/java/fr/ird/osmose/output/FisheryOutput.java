/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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

    public FisheryOutput(int rank) {
        super(rank);
        
    }

    @Override
    public void init() {
        
        // initializes the number of fisheries
        nFishery = getConfiguration().getNFishery();

        /*
         * Create NetCDF file
         */
        try {
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filename);
        } catch (IOException ex) {
            Logger.getLogger(FisheryOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension speciesDim = nc.addDimension(null, "species", getNSpecies());
        Dimension fisheriesDim = nc.addDimension(null, "fishery", nFishery);
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        timeVar = nc.addVariable(null, "time", DataType.FLOAT, "time");
        timeVar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        timeVar.addAttribute(new Attribute("calendar", "360_day"));
        timeVar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        biomassVar = nc.addVariable(null, "biomass", DataType.FLOAT, new ArrayList<>(Arrays.asList(timeDim, speciesDim, fisheriesDim)));
        biomassVar.addAttribute(new Attribute("units", "ton"));
        biomassVar.addAttribute(new Attribute("description", "biomass, in tons, per species and per cell"));
        biomassVar.addAttribute(new Attribute("_FillValue", -99.f));

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
        biomass = new float[getNSpecies()][nFishery];
    }

    @Override
    public void update() {

        getSchoolSet().getAliveSchools().forEach((school) -> {
            int iSpecies = school.getSpeciesIndex();
            for (int iFishery = 0; iFishery < nFishery; iFishery++) {
                biomass[iSpecies][iFishery] += school.getFishedBiomass(iFishery);
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
            Logger.getLogger(FisheryOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_fisheryOutput_Simu");
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
