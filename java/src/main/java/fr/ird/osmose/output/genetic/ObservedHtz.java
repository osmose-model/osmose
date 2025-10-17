package fr.ird.osmose.output.genetic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.IMarineOrganism;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.IOutput;
import fr.ird.osmose.output.distribution.OutputDistribution;
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

public class ObservedHtz extends SimulationLinker implements IOutput {

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
    private int numberOfClasses;

    private int record_index;
    private int recordFrequency;

    private double number_of_occurrences[][][];
    private double normalization[][][];

    private OutputDistribution distrib;

    private interface ClassMethod {
        int getClassIndex(IMarineOrganism school);
    }

    private ClassMethod classMethod;

    public ObservedHtz(int rank, Species species) {
        this(rank, species, null);
    }

    public ObservedHtz(int rank, Species species, OutputDistribution distrib) {
        super(rank);
        this.species = species;
        this.distrib = distrib;
    }

    @Override
    public void initStep() {

    }

    @Override
    public void reset() {
        number_of_occurrences = new double[numberOfClasses][ntrait][nlocus_max];
        normalization = new double[numberOfClasses][ntrait][nlocus_max];
    }

    int getClassIndex(IMarineOrganism school) {
        return distrib.getClass(school);
    }

    @Override
    public void update() {

        for(School school : getSchoolSet().getSchools(species)) {

            // genotype of the school
            Genotype genotype = school.getGenotype();

            int classIndex = classMethod.getClassIndex(school);

            // Loop over all the traits
            for (int itrait = 0; itrait < ntrait; itrait++) {

                // Loop over all the locus that encode for the trait
                for (int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {

                    // get the values of both alleles
                    double value0 = genotype.getLocus(itrait, ilocus).getValue(0);
                    double value1 = genotype.getLocus(itrait, ilocus).getValue(1);

                    if (value0 != value1) {
                        this.number_of_occurrences[classIndex][itrait][ilocus] += school.getAbundance();
                    }

                    normalization[classIndex][itrait][ilocus] += school.getAbundance();

                }
            }
        }
    }

    @Override
    public void write(float time) {

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        ArrayFloat.D4 arrOut = new ArrayFloat.D4(1, this.numberOfClasses, this.ntrait, this.nlocus_max);

        arrTime.set(0, time);
        try {
            Variable tvar = observedHtzOutputnc.findVariable("time");
            observedHtzOutputnc.write(tvar, new int[] { this.record_index }, arrTime);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int iclass = 0; iclass < this.numberOfClasses; iclass++) {
            for (int itrait = 0; itrait < ntrait; itrait++) {
                for (int ilocus = 0; ilocus < nlocus[itrait]; ilocus++) {
                    arrOut.set(0, iclass, itrait, ilocus,
                            (float) ((double) (number_of_occurrences[iclass][itrait][ilocus])
                                    / normalization[iclass][itrait][ilocus]));
                } // end of loop of resources as preys
            } // end of predator stage loop
        }

        try {
            Variable outvar = observedHtzOutputnc.findVariable(this.getObservedHtzVarName());
            observedHtzOutputnc.write(outvar, new int[] { this.record_index, 0, 0, 0}, arrOut);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.record_index += 1;

    }

    public String getDescription() {
        return String.format("Probility of occurrence of a given allele for species %s. Depends on trait and locus",
                species.getName());
    }

    @Override
    public void init() {

        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        // If distribution is null, then we use the update method without classes.
        if(distrib == null) {
            classMethod = (school) -> {return 0;};
            this.numberOfClasses = 1;
        } else {
            classMethod = (school) -> this.getClassIndex(school);
            this.numberOfClasses = distrib.getNClass();
        }

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
        String filename = getFilenameObservedHtz();

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
        Dimension classDim = observedHtzOutputbNc.addDimension("class", this.numberOfClasses);


        if(this.numberOfClasses > 1) {
            Dimension minimumClassDim = observedHtzOutputbNc.addDimension("class_thresholds", this.numberOfClasses - 1);
            List<Dimension> minimumClassDimList = new ArrayList<>();
            minimumClassDimList.add(minimumClassDim);
            Variable.Builder<?> minimumClassDimVariable = observedHtzOutputbNc.addVariable("class_thresholds", DataType.FLOAT, minimumClassDimList);
        }


        List<Dimension> outDims = new ArrayList<>();
        outDims.add(timeDim);
        outDims.add(classDim);
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

        if (this.numberOfClasses == 1) {
            return;
        }

        ArrayDouble.D1 arrClass = new ArrayDouble.D1(numberOfClasses - 1);
        for (int i = 0; i < this.numberOfClasses; i++) {
            arrClass.set(i, distrib.getThreshold(i));
        }

        Variable classVar = observedHtzOutputnc.findVariable("class_thresholds");
        try {
            observedHtzOutputnc.write(classVar, new int[] { 0 }, arrClass);
        } catch (IOException | InvalidRangeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void close() {
        try {
            this.observedHtzOutputnc.close();
            String strFilePart = this.getFilenameObservedHtz();
            String strFileBase = strFilePart.substring(0, strFilePart.indexOf(".part"));
            File filePart = new File(strFilePart);
            File fileBase = new File(strFileBase);
            filePart.renameTo(fileBase);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    String getFilenameObservedHtz() {
        // Create parent directory
        StringBuilder filename = this.initFileName();
        filename.append("Genetic");
        filename.append(File.separatorChar);

        IOTools.makeDirectories(filename.toString());

        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_observedHtz");

        if (this.numberOfClasses > 1) {
            filename.append("DistribBy" + distrib.getType());
        }

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

    private String getObservedHtzVarName() {
        return "observed_heterozygosity";
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }

}