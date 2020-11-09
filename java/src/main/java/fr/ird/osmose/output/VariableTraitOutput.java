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

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.ma2.ArrayString;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayInt;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class VariableTraitOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;

    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriter nc;

    // spatial indicators
    private float[][] trait_mean;
    private float[] abundance;

    private int recordFrequency;

    // index of the record index (iterating at each write event)
    private int record_index;

    public VariableTraitOutput(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        record_index = 0;
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");
        /*
         * Create NetCDF file
         */
        try {
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4_classic, filename);
            //nc.setLocation(filename);
        } catch (IOException ex) {
            Logger.getLogger(VariableTraitOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions (time, species, trait)
         */
        Dimension speciesDim = nc.addDimension(null, "species", getNSpecies());
        Dimension traitDim = nc.addDimension(null, "trait", this.getSimulation().getNEvolvingTraits());
        Dimension timeDim = nc.addUnlimitedDimension("time");

        /*
         * Add variables
         */
        // Creating coordinate time variable 
        Variable tvar = nc.addVariable(null, "time", DataType.FLOAT, "time");
        tvar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        tvar.addAttribute(new Attribute("calendar", "360_day"));
        tvar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        // Creation of the output variable
        ArrayList<Dimension> outdim_l = new ArrayList<>();
        outdim_l.add(timeDim);
        outdim_l.add(traitDim);
        outdim_l.add(speciesDim);
        Variable trait_mean_var = nc.addVariable(null, "trait_mean", DataType.FLOAT, outdim_l);
        trait_mean_var.addAttribute(new Attribute("units", ""));
        trait_mean_var.addAttribute(new Attribute("description", "Mean value of the trait"));
        trait_mean_var.addAttribute(new Attribute("_FillValue", -99.f));

        // Writting coordinate species variable + attributes
        Variable specvar = nc.addVariable(null, "species", DataType.INT, "species");
        for (int i = 0; i < this.getNSpecies(); i++) {
            specvar.addAttribute(new Attribute(String.format("species%d", i), this.getSpecies(i).getName()));
        }

        // Writting coordinate trait variable + attributes
        Variable traitvar = nc.addVariable(null, "trait", DataType.INT, "trait");
        for (int i = 0; i < this.getNEvolvingTraits(); i++) {
            traitvar.addAttribute(new Attribute(String.format("trait%d", i), this.getEvolvingTrait(i).getName()));
        }

        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();
            /*
             * Writes variable trait (trait names) and species (species names)
             */
            ArrayInt.D1 arrTrait = new ArrayInt.D1(this.getSimulation().getNEvolvingTraits());
            ArrayInt.D1 arrSpecies = new ArrayInt.D1(this.getNSpecies());

            for (int i = 0; i < this.getNSpecies(); i++) {
                arrSpecies.set(i, i);
            }

            for (int i = 0; i < this.getSimulation().getNEvolvingTraits(); i++) {
                arrTrait.set(i, i);
            }

            Variable varspec = nc.findVariable("species");
            nc.write(varspec, arrSpecies);

            Variable vartrait = nc.findVariable("trait");
            nc.write(vartrait, arrTrait);

        } catch (IOException ex) {
            Logger.getLogger(VariableTraitOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(VariableTraitOutput.class.getName()).log(Level.SEVERE, null, ex);
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

    @Override
    public void initStep() {
    }

    @Override
    public void reset() {
        this.abundance = new float[this.getNSpecies()];   // initialize abundance
        this.trait_mean = new float[this.getSimulation().getNEvolvingTraits()][this.getNSpecies()];   // init. trait to 0
    }

    @Override
    public void update() {

        for (int i = 0; i < this.getNSpecies(); i++) {

            // recovering the species object
            Species species = this.getSpecies(i);

            // listing all the schools that belong to the given species
            List<School> listSchool = this.getSchoolSet().getSchools(species);

            // Loop over all the traits
            for (School sch : listSchool) {
                this.abundance[i] += sch.getInstantaneousAbundance();
                for (int itrait = 0; itrait < this.getNEvolvingTraits(); itrait++) {
                    String traitName = this.getEvolvingTrait(itrait).getName();
                    try {
                        this.trait_mean[itrait][i] += sch.getTrait(traitName) * sch.getInstantaneousAbundance();
                    } catch (Exception ex) {
                        Logger.getLogger(VariableTraitOutput.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }  // end of species loop        
    }   // end of method

    @Override
    public void write(float time) {

        ArrayFloat.D3 arrMean = new ArrayFloat.D3(1, this.getNEvolvingTraits(), this.getNSpecies());

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        int nTraits = this.getNEvolvingTraits();

        for (int j = 0; j < nTraits; j++) {
            for (int i = 0; i < nSpecies; i++) {
                arrMean.set(0, j, i, trait_mean[j][i] / this.abundance[i]);
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) time);

        try {
            Variable timevar = this.nc.findVariable("time");
            nc.write(timevar, new int[]{record_index}, arrTime);
            Variable tmeanvar = this.nc.findVariable("trait_mean");
            nc.write(tmeanvar, new int[]{record_index, 0, 0}, arrMean);
        } catch (InvalidRangeException | IOException ex) {
            Logger.getLogger(VariableTraitOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

        // increments the record index.
        record_index += 1;

    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_EvolvingTrait").append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        // Always true, every time step should be written in the NetCDF file.
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }
}
