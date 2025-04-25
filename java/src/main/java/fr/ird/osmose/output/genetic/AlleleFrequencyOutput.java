package fr.ird.osmose.output.genetic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.IOutput;
import fr.ird.osmose.output.netcdf.AbstractOutput_Netcdf;
import fr.ird.osmose.output.netcdf.DietOutput_Netcdf;
import fr.ird.osmose.process.genet.Genotype;
import fr.ird.osmose.process.genet.Trait;
import fr.ird.osmose.util.SimulationLinker;
import fr.ird.osmose.util.io.IOTools;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.NetcdfFormatWriter;

public class AlleleFrequencyOutput extends SimulationLinker implements IOutput {

    private Species species;

    int species_index;

    // Number of traits
    int ntrait;

    // number of locus for each trait for this specific species
    int[] nlocus;

    // Number of values that the alleles can take (depends on the locus and on the species
    int[] nvalues;

    int nvalue_max;
    int nlocus_max;

    private boolean expectedHtzOutputEnabled;
    private boolean alleleFrequencyOutputEnabled;

    private NetcdfFormatWriter alleleFrequencyOutputnc;
    private NetcdfFormatWriter.Builder alleleFrequencyOutputbNc;

    private NetcdfFormatWriter expectedHtzOutputnc;
    private NetcdfFormatWriter.Builder expectedHtzOutputbNc;

    private Dimension timeDim;

    private int record_index;
    int recordFrequency;

    private double[][][] number_of_occurrences;
    private double[][] normalization;

    public AlleleFrequencyOutput(int rank, Species species, boolean expectedHtzOutput, boolean alleleFrequecyOutput) {
        super(rank);
        this.species = species;
        this.expectedHtzOutputEnabled = expectedHtzOutput;
        this.alleleFrequencyOutputEnabled = alleleFrequecyOutput;
    }


    @Override
    public void initStep() {

    }

    @Override
    public void reset() {
        number_of_occurrences = new double[ntrait][nlocus_max][nvalue_max];
    }

    @Override
    public void update() {
        // Loop over all the schools
        for (School school : getSchoolSet().getSchools(species)) {

            // genotype of the school
            Genotype genotype = school.getGenotype();

            // Loop over all the traits
            for (int itrait = 0; itrait < ntrait; itrait++) {

                Trait trait = this.getEvolvingTrait(itrait);

                // Loop over all the locus that encode for the trait
                for (int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {

                    // get the values of both alleles
                    double value0 = genotype.getLocus(itrait, ilocus).getValue(0);
                    double value1 = genotype.getLocus(itrait, ilocus).getValue(1);

                    // Loop over all the possible values of the alleles
                    for (int ival = 0; ival < nvalues[itrait]; ival++) {
                        if (value0 == trait.getDiv(species_index, ilocus, ival)) {
                            this.number_of_occurrences[itrait][ilocus][ival] += school.getAbundance();
                        }
                        if (value1 == trait.getDiv(species_index, ilocus, ival)) {
                            this.number_of_occurrences[itrait][ilocus][ival] +=  school.getAbundance();
                        }
                    }

                    normalization[itrait][ilocus] += 2 * school.getAbundance();

                }
            }
        }
    }

    @Override
    public void write(float time) {

        if(this.alleleFrequencyOutputEnabled) {
            this.writeAlleleFrequency(time);
        }

        if(this.expectedHtzOutputEnabled) {
            this.writeExpectedHtz(time);
        }

        this.record_index += 1;
    }


    private void writeExpectedHtz(float time) {

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        ArrayFloat.D3 arrOut = new ArrayFloat.D3(1, this.ntrait, this.nlocus_max);

        arrTime.set(0, time);
        try {
            Variable tvar = expectedHtzOutputnc.findVariable("time");
            expectedHtzOutputnc.write(tvar, new int[] { this.record_index }, arrTime);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        double[][] expectedHtz = new double[this.ntrait][this.nlocus_max];
        for (int itrait = 0; itrait < ntrait; itrait++) {
            for(int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {
                double sum = 0;
                for(int ival = 0; ival < nvalues[itrait]; ival++) {
                    sum += Math.pow(number_of_occurrences[itrait][ilocus][ival] / normalization[itrait][ilocus], 2);
                }
                expectedHtz[itrait][ilocus] = 1 - sum;
            }
        }

        for (int itrait = 0; itrait < ntrait; itrait++) {
            for (int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {
                arrOut.set(0, itrait, ilocus, (float) expectedHtz[itrait][ilocus]);
            } // end of loop of resources as preys
        } // end of predator stage loop

        try {
            Variable outvar = expectedHtzOutputnc.findVariable(this.getExpectedHtzVarName());
            expectedHtzOutputnc.write(outvar, new int[] { this.record_index, 0, 0}, arrOut);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private void writeAlleleFrequency(float time) {

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        ArrayFloat.D4 arrOut = new ArrayFloat.D4(1, this.ntrait, this.nlocus_max, this.nvalue_max);

        arrTime.set(0, time);
        try {
            Variable tvar = alleleFrequencyOutputnc.findVariable("time");
            alleleFrequencyOutputnc.write(tvar, new int[] { this.record_index }, arrTime);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int itrait = 0; itrait < ntrait; itrait++) {
            for(int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {
                for(int ival = 0; ival < nvalues[itrait]; ival++) {
                    arrOut.set(0, itrait, ilocus, ival, (float) (100.d * number_of_occurrences[itrait][ilocus][ival] / normalization[itrait][ilocus]));
                } // end of loop of resources as preys
            } // end of predator stage loop
        } // end of predator species loop

        try {
            Variable outvar = alleleFrequencyOutputnc.findVariable(this.getAlleleFreqVarName());
            alleleFrequencyOutputnc.write(outvar, new int[] { this.record_index, 0, 0, 0 }, arrOut);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getAlleleFrequencyDescription() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
        return String.format("Probility of occurrence of a given allele for species %s. Depends on trait and locus", species.getName());
    }

    public String getExpectedHtzDescription() {
        return String.format("Expected heterozygosity for species %s.", species.getName());
    }

    @Override
    public void init() {

        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        nvalue_max = Integer.MIN_VALUE;
        nlocus_max = Integer.MIN_VALUE;

        species_index = species.getSpeciesIndex();

        ntrait = this.getNEvolvingTraits();
        nlocus = new int[ntrait];  // nlocus depends on trait
        nvalues = new int[ntrait];  // nvalues depends on trait
        for (int indexTrait = 0; indexTrait < ntrait; indexTrait++) {
            Trait trait = this.getEvolvingTrait(indexTrait);
            nlocus[indexTrait] = trait.getNLocus(species.getSpeciesIndex());
            nvalues[indexTrait] = trait.getNValues(species.getSpeciesIndex());
            nvalue_max = Math.max(nvalue_max, nvalues[indexTrait]);
            nlocus_max = Math.max(nlocus_max, nlocus[indexTrait]);
        }

        if(this.alleleFrequencyOutputEnabled) {
            this.createAlleleFrequencyOutputFile();
        }

        if(this.expectedHtzOutputEnabled) {
            this.createExpectedHtzOutputFile();
        }

    }


    private void createAlleleFrequencyOutputFile() {

        Nc4Chunking chunker = getConfiguration().getChunker();

        /*
         * Create NetCDF file
         */
        String filename = getFilenameAlleleFrequency();

        alleleFrequencyOutputbNc = NetcdfFormatWriter.createNewNetcdf4(getConfiguration().getNcOutVersion(), filename, chunker);

        // Add time dim and variable (common to all files)
        timeDim = alleleFrequencyOutputbNc.addUnlimitedDimension("time");

        Variable.Builder<?> tvar = alleleFrequencyOutputbNc.addVariable("time", DataType.DOUBLE, "time");
        tvar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        tvar.addAttribute(new Attribute("calendar", "360_day"));
        tvar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Dimension traitDim = alleleFrequencyOutputbNc.addDimension("trait", ntrait);
        Dimension locusDim = alleleFrequencyOutputbNc.addDimension("locus", nlocus_max);
        Dimension alleleDim = alleleFrequencyOutputbNc.addDimension("allele", nvalue_max);

        List<Dimension> outDims = new ArrayList<>();
        outDims.add(timeDim);
        outDims.add(traitDim);
        outDims.add(locusDim);
        outDims.add(alleleDim);

        // Create output variable
        Variable.Builder<?> outvar = alleleFrequencyOutputbNc.addVariable(getAlleleFreqVarName(), DataType.FLOAT, outDims);
        outvar.addAttribute(new Attribute("units", ""));
        outvar.addAttribute(new Attribute("description", getAlleleFrequencyDescription()));
        outvar.addAttribute(new Attribute("_FillValue", -999));

        try {
            // Validates the structure of the NetCDF file.
            alleleFrequencyOutputnc = alleleFrequencyOutputbNc.build();
        } catch (IOException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Write NetCDF coords (for instance species, stage, etc.)
        this.write_nc_coords();

    }

    private void createExpectedHtzOutputFile() {

        Nc4Chunking chunker = getConfiguration().getChunker();

        /*
         * Create NetCDF file
         */
        String filename = getFilenameExpectedHtz();

        expectedHtzOutputbNc = NetcdfFormatWriter.createNewNetcdf4(getConfiguration().getNcOutVersion(), filename, chunker);

        // Add time dim and variable (common to all files)
        timeDim = expectedHtzOutputbNc.addUnlimitedDimension("time");

        Variable.Builder<?> tvar = expectedHtzOutputbNc.addVariable("time", DataType.DOUBLE, "time");
        tvar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        tvar.addAttribute(new Attribute("calendar", "360_day"));
        tvar.addAttribute(new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Dimension traitDim = expectedHtzOutputbNc.addDimension("trait", ntrait);
        Dimension locusDim = expectedHtzOutputbNc.addDimension("locus", nlocus_max);

        List<Dimension> outDims = new ArrayList<>();
        outDims.add(timeDim);
        outDims.add(traitDim);
        outDims.add(locusDim);

        // Create output variable
        Variable.Builder<?> outvar = expectedHtzOutputbNc.addVariable(getExpectedHtzVarName(), DataType.FLOAT, outDims);
        outvar.addAttribute(new Attribute("units", ""));
        outvar.addAttribute(new Attribute("description", getExpectedHtzDescription()));
        outvar.addAttribute(new Attribute("_FillValue", -999));

        try {
            // Validates the structure of the NetCDF file.
            expectedHtzOutputnc = expectedHtzOutputbNc.build();
        } catch (IOException ex) {
            Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Write NetCDF coords (for instance species, stage, etc.)
        this.write_nc_coords();

    }

    private void write_nc_coords() {
        // // Writes variable trait (trait names) and species (species names)
        // ArrayInt arrSpecies = new ArrayInt(new int[] { this.getNSpecies() }, false);
        // Index index = arrSpecies.getIndex();

        // for (int i = 0; i < this.getNSpecies(); i++) {
        //     index.set(i);
        //     arrSpecies.set(index, i);
        // }

        // Variable varspec = this.nc.findVariable("species");

        // try {
        //     nc.write(varspec, arrSpecies);
        // } catch (IOException | InvalidRangeException ex) {
        //     Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        // }
    }

    @Override
    public void close() {
        if(this.alleleFrequencyOutputEnabled) {
            try {
                alleleFrequencyOutputnc.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if(this.expectedHtzOutputEnabled) {
            try {
                expectedHtzOutputnc.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    String getFilenameAlleleFrequency() {
        // Create parent directory
        StringBuilder filename = this.initFileName();
        filename.append("Genetic");
        filename.append(File.separatorChar);

        IOTools.makeDirectories(filename.toString());

        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_alleleFrequency");
        filename.append("-");
        filename.append(species.getName());
        filename.append("Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    String getFilenameExpectedHtz() {
        // Create parent directory
        StringBuilder filename = this.initFileName();
        filename.append("Genetic");
        filename.append(File.separatorChar);

        IOTools.makeDirectories(filename.toString());

        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_expectedHtz");
        filename.append("-");
        filename.append(species.getName());
        filename.append("Simu");
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

    private String getAlleleFreqVarName() {
        return "allele_occurrence_frequency";
    }


    private String getExpectedHtzVarName() {
        return "expected_heterozygosity";
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }

}
