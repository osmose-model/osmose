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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.School;
import fr.ird.osmose.util.SimulationLinker;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import fr.ird.osmose.util.io.IOTools;

public class MeanGenotypeOutput extends SimulationLinker implements IOutput {


    /** Array for storing outputs
     * Dimensions: nSpecies, nTraits
    */
    private double output[][];
    private double denominator[];
    int nSpecies;
    int nTraits;
    private List<Dimension> outDims;

    private final SchoolVariableGetter weight;
    private final Predicate<School> predicate;
    private final SchoolSetGetter schoolGetter;


    Variable outvar;
    private String prefix;


    /**
     * List of dimensions of the variable to write out.
     */
    private Dimension timeDim;
    /**
     * _FillValue attribute for cells on land
     */
    private NetcdfFileWriter nc;
    private int record_index = 0;

    private final String varname = "meanGenotype";
    private final String description;

    MeanGenotypeOutput(int rank, String prefix, SchoolVariableGetter weight, Predicate<School> predicate, SchoolSetGetter schoolGetter, String description) {
        super(rank);
        nSpecies = this.getConfiguration().getNSpecies();
        nTraits = this.getNEvolvingTraits();
        output = new double[nTraits][nSpecies];
        this.prefix = prefix;
        this.weight = weight;
        this.predicate = predicate;
        this.schoolGetter = schoolGetter;
        this.description = description;
    }

    private String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append("Genetic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_");
        filename.append(prefix);
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    public StringBuilder initFileName() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        return filename;
    }

    private String getVarname() {
        return this.varname;
    }

    @Override
    public void initStep() {
    }

    @Override
    public void reset() {
        output = new double[nSpecies][nTraits];
        denominator = new double[nSpecies];
    }

    @Override
    public void update() {
        for (School school : schoolGetter.getSet(getSchoolSet())) {
            if(predicate.test(school)) {
                int iSpecies = school.getSpeciesIndex();
                double w = this.weight.getVariable(school);
                denominator[iSpecies] += w;
                for(int iTrait = 0; iTrait < this.nTraits; iTrait++) {
                    try {
                        output[iSpecies][iTrait] += w * school.getGenotype().getGeneticTrait(iTrait);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void write(float time) {

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        arrTime.set(0, time);

        ArrayDouble.D3 arrAbund = new ArrayDouble.D3(1, nSpecies, nTraits);

        for (int i = 0; i < nSpecies; i++) {
            for (int j = 0; j < nTraits; j++) {
                double toWrite = denominator[i] > 0 ? (float) output[i][j] / denominator[i] : Double.NaN;
                arrAbund.set(0, i, j, toWrite);
            }
        }

        int index = this.getTimeIndex();
        try {
            Variable tvar = nc.findVariable("time");
            nc.write(tvar, new int[] { index }, arrTime);
            Variable outvar = nc.findVariable(this.getVarname());
            nc.write(outvar, new int[] { index, 0, 0 }, arrAbund);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MeanGenotypeOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.incrementIndex();
    }

    public int getTimeIndex() {
        return this.record_index;
    }

    public void incrementIndex() {
        this.record_index++;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return true;
    }

    @Override
    public void init() {

        /*
         * Create NetCDF file
         */
        try {
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc = NetcdfFileWriter.createNew(getConfiguration().getNcOutVersion(), filename);
        } catch (IOException ex) {
            Logger.getLogger(MeanGenotypeOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Add time dim and variable (common to all files)
        timeDim = nc.addUnlimitedDimension("time");

        Variable tvar = nc.addVariable(null, "time", DataType.DOUBLE, "time");
        tvar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        tvar.addAttribute(new Attribute("calendar", "360_day"));
        tvar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        // Init NC dimensions and coords (in define mode)
        this.init_nc_dims_coords();

        // Create output variable
        outvar = nc.addVariable(null, getVarname(), DataType.FLOAT, this.getNcDims());
        // outvar.addAttribute(new Attribute("units", getUnits()));
        outvar.addAttribute(new Attribute("description", getDescription()));
        // outvar.addAttribute(new Attribute("_FillValue", getFillValue()));

        try {
            // Validates the structure of the NetCDF file.
            nc.create();
        } catch (IOException ex) {
            Logger.getLogger(MeanGenotypeOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Write NetCDF coords (for instance species, stage, etc.)
        this.write_nc_coords();

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

    public List<Dimension> getNcDims() {
        return this.outDims;
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    void init_nc_dims_coords() {

        Dimension speciesDim = nc.addDimension(null, "species", getNSpecies());
        Dimension traitDim = nc.addDimension(null, "trait", getNEvolvingTraits());

        nc.addVariable(null, "species", DataType.INT, "species");
        this.createSpeciesAttr();

        nc.addVariable(null, "trait", DataType.INT, "trait");
        this.createTraitAttr();

        outDims = new ArrayList<>();
        outDims.add(timeDim);
        outDims.add(speciesDim);
        outDims.add(traitDim);

    }

    public void write_nc_coords() {

        // Writes variable trait (trait names) and species (species names)
        ArrayInt.D1 arrSpecies = new ArrayInt.D1(this.getNSpecies());
        for (int i = 0; i < this.getNSpecies(); i++) {
            arrSpecies.set(i, i);
        }

        Variable varspec = this.nc.findVariable("species");

        try {
            nc.write(varspec, arrSpecies);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MeanGenotypeOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Writes variable trait (trait names) and species (species names)
        ArrayInt.D1 arrTrait = new ArrayInt.D1(this.getNEvolvingTraits());
        for (int i = 0; i < this.getNEvolvingTraits(); i++) {
            arrSpecies.set(i, i);
        }

        Variable vartrait = this.nc.findVariable("trait");

        try {
            nc.write(vartrait, arrTrait);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MeanGenotypeOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Function to create a species attribute.
     * @return
     */
    public String createSpeciesAttr() {

        Variable species = this.nc.findVariable("species");
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < this.getNSpecies(); i++) {
            String attrname = String.format("species%d", i);
            String attval = this.getSpecies(i).getName();
            species.addAttribute(new Attribute(attrname, attval));
        }

        return bld.toString();

    }

        /**
     * Function to create a species attribute.
     * @return
     */
    public String createTraitAttr() {

        Variable trait = this.nc.findVariable("trait");
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < this.getNEvolvingTraits(); i++) {
            String attrname = String.format("trait%d", i);
            String attval = this.getEvolvingTrait(i).getName();
            trait.addAttribute(new Attribute(attrname, attval));
        }

        return bld.toString();

    }

}