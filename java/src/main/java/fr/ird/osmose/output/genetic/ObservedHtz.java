package fr.ird.osmose.output.genetic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.AbstractOutput;
import fr.ird.osmose.output.IOutput;
import fr.ird.osmose.output.netcdf.AbstractOutput_Netcdf;
import fr.ird.osmose.output.netcdf.DietOutput_Netcdf;
import fr.ird.osmose.process.genet.Trait;
import fr.ird.osmose.process.genet.Genotype;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.NetcdfFormatWriter;

public class ObservedHtz extends AbstractOutput {

    private Species species;

    int species_index;

    // Number of traits
    int ntrait;

    // number of locus for each trait for this specific species
    int[] nlocus;

    // Number of values that the alleles can take (depends on the locus and on the
    // species
    int[] nvalues;

    int nvalue_max;
    int nlocus_max;

    private NetcdfFormatWriter observedHtzOutputnc;
    private NetcdfFormatWriter.Builder observedHtzOutputbNc;

    private Dimension timeDim;

    private int record_index;

    private double[][] number_of_occurrences;
    private double[][] normalization;

    public ObservedHtz(int rank, String subfolder, String name, boolean includeOnlyAlive, Species species) {
        super(rank, subfolder, name, includeOnlyAlive);
        this.species = species;
    }

    @Override
    public void initStep() {

    }

    @Override
    public void reset() {
        number_of_occurrences = new double[ntrait][nlocus_max];
    }

    @Override
    public void update() {
        // Loop over all the schools
        for (School school : getSchoolSet().getAliveSchools()) {

            // genotype of the school
            Genotype genotype = school.getGenotype();

            // Loop over all the traits
            for (int itrait = 0; itrait < ntrait; itrait++) {

                // Loop over all the locus that encode for the trait
                for (int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {

                    // get the values of both alleles
                    double value0 = genotype.getLocus(itrait, ilocus).getValue(0);
                    double value1 = genotype.getLocus(itrait, ilocus).getValue(1);

                    if (value0 != value1) {
                        this.number_of_occurrences[itrait][ilocus] += school.getAbundance();
                    }

                    normalization[itrait][ilocus] += school.getAbundance();

                }
            }
        }
    }

    @Override
    public void write(float time) {

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        ArrayFloat.D3 arrOut = new ArrayFloat.D3(1, this.ntrait, this.nlocus_max);

        arrTime.set(0, time);
        try {
            Variable tvar = observedHtzOutputnc.findVariable("time");
            observedHtzOutputnc.write(tvar, new int[] { this.record_index }, arrTime);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int itrait = 0; itrait < ntrait; itrait++) {
            for (int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {
                arrOut.set(0, itrait, ilocus,
                        (float) (number_of_occurrences[itrait][ilocus] / normalization[itrait][ilocus]));
            } // end of loop of resources as preys
        } // end of predator stage loop

        try {
            Variable outvar = observedHtzOutputnc.findVariable(this.getObservedHtzVarName());
            observedHtzOutputnc.write(outvar, new int[] { this.record_index, 0, 0 }, arrOut);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.record_index += 1;

    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method
        // 'getDescription'");
        return String.format("Probility of occurrence of a given allele for species %s. Depends on trait and locus",
                species.getName());
    }

    @Override
    public void init() {

        nvalue_max = Integer.MIN_VALUE;
        nlocus_max = Integer.MIN_VALUE;

        species_index = species.getSpeciesIndex();

        ntrait = this.getNEvolvingTraits();
        nlocus = new int[ntrait]; // nlocus depends on trait
        nvalues = new int[ntrait]; // nvalues depends on trait
        for (int indexTrait = 0; indexTrait < ntrait; indexTrait++) {
            Trait trait = this.getEvolvingTrait(indexTrait);
            nlocus[indexTrait] = trait.getNLocus(species.getSpeciesIndex());
            nvalues[indexTrait] = trait.getNValues(species.getSpeciesIndex());
            nvalue_max = Math.max(nvalue_max, nvalues[indexTrait]);
            nlocus_max = Math.max(nlocus_max, nlocus[indexTrait]);
        }

        this.createObservedHtzOutputFile();
    }

    private void createObservedHtzOutputFile() {

        Nc4Chunking chunker = getConfiguration().getChunker();

        /*
         * Create NetCDF file
         */
        String filename = getObservedHtzVarName();

        observedHtzOutputbNc = NetcdfFormatWriter.createNewNetcdf4(getConfiguration().getNcOutVersion(), filename, chunker);

        // Add time dim and variable (common to all files)
        timeDim = observedHtzOutputbNc.addUnlimitedDimension("time");

        Variable.Builder<?> tvar = observedHtzOutputbNc.addVariable("time", DataType.DOUBLE, "time");
        tvar.addAttribute(new Attribute("units", "days since 0-1-1 0:0:0"));
        tvar.addAttribute(new Attribute("calendar", "360_day"));
        tvar.addAttribute(
                new Attribute("description", "time ellapsed, in days, since the beginning of the simulation"));

        Dimension traitDim = observedHtzOutputbNc.addDimension("trait", ntrait);
        Dimension locusDim = observedHtzOutputbNc.addDimension("locus", nlocus_max);

        List<Dimension> outDims = new ArrayList<>();
        outDims.add(timeDim);
        outDims.add(traitDim);
        outDims.add(locusDim);

        // Create output variable
        Variable.Builder<?> outvar = observedHtzOutputbNc.addVariable(getObservedHtzVarName(), DataType.FLOAT, outDims);
        outvar.addAttribute(new Attribute("units", ""));
        outvar.addAttribute(new Attribute("description", getDescription()));
        outvar.addAttribute(new Attribute("_FillValue", -999));

        try {
            // Validates the structure of the NetCDF file.
            observedHtzOutputnc = observedHtzOutputbNc.build();
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
        // index.set(i);
        // arrSpecies.set(index, i);
        // }

        // Variable varspec = this.nc.findVariable("species");

        // try {
        // nc.write(varspec, arrSpecies);
        // } catch (IOException | InvalidRangeException ex) {
        // Logger.getLogger(AbstractOutput_Netcdf.class.getName()).log(Level.SEVERE,
        // null, ex);
        // }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    String getFilenameAlleleFrequency() {
        // Create parent directory
        StringBuilder filename = this.initFileName();
        filename.append("Genetic");
        filename.append(File.separatorChar);
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

    @Override
    public String[] getHeaders() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'getHeaders'");
        return new String[] { "" };
    }

    private String getObservedHtzVarName() {
        return "allele_occurrence_frequency";
    }

}