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
import fr.ird.osmose.process.genet.Genotype;
import fr.ird.osmose.process.genet.Trait;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 *
 * @author pverley
 */
public class SchoolSetSnapshot extends SimulationLinker {

    private int nTrait=0;
    private int nMaxLoci;
    private File file;

    public SchoolSetSnapshot(int rank) {
        super(rank);
    }

    public void makeSnapshot(int iStepSimu) {
        boolean useGenet = this.getConfiguration().isGeneticEnabled();
        boolean useBioen = this.getConfiguration().isBioenEnabled();
        ArrayFloat.D1 gonadicWeight = null;
        ArrayFloat.D4 genotype = null;
        ArrayFloat.D2 traitnoise = null;
        ArrayInt.D1 maturity = null;
        NetcdfFormatWriter nc = createNCFile(iStepSimu);
        int nSchool = getSchoolSet().getSchools().size();
        ArrayInt.D1 species = new ArrayInt.D1(nSchool, false);
        ArrayFloat.D1 x = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 y = new ArrayFloat.D1(nSchool);
        ArrayDouble.D1 abundance = new ArrayDouble.D1(nSchool);
        ArrayFloat.D1 age = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 length = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 weight = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 trophiclevel = new ArrayFloat.D1(nSchool);
        if (useBioen) {
            gonadicWeight = new ArrayFloat.D1(nSchool);
            maturity = new ArrayInt.D1(nSchool, false);
        }

        // if use genetic, initialize the output of the genotype (nschool x ntrait x nloci x 2)
        if (useGenet) {
            genotype = new ArrayFloat.D4(nSchool, this.nTrait, this.nMaxLoci, 2);
            traitnoise = new ArrayFloat.D2(nSchool, this.nTrait);
        }

        int s = 0;
        // fill up the arrays
        for (School school : getSchoolSet().getSchools()) {
            species.set(s, school.getSpeciesIndex());
            x.set(s, school.getX());
            y.set(s, school.getY());
            abundance.set(s, school.getInstantaneousAbundance());
            age.set(s, (float) school.getAgeDt() / getConfiguration().getNStepYear());
            length.set(s, school.getLength());
            weight.set(s, school.getWeight() * 1e6f);
            trophiclevel.set(s, school.getTrophicLevel());

            if(useBioen) {
                gonadicWeight.set(s, school.getGonadWeight() * 1e6f);
                maturity.set(s, school.isMature() ? 1 : 0);
            }

            if(useGenet) {
                // If use genetic module, save the list of loci pairs for each trait.
                Genotype gen = school.getGenotype();
                for(int iTrait=0; iTrait<nTrait; iTrait++) {
                    traitnoise.set(s, iTrait, (float) gen.getEnvNoise(iTrait));
                    int nLoci = gen.getNLocus(iTrait);
                    for(int iLoci = 0; iLoci < nLoci; iLoci++) {
                       genotype.set(s, iTrait, iLoci, 0, (float) gen.getLocus(iTrait, iLoci).getValue(0));
                       genotype.set(s, iTrait, iLoci, 1, (float) gen.getLocus(iTrait, iLoci).getValue(1));
                    }
                }
            }
            // writes genotype for each school
            s++;
        }
        // write the arrays in the NetCDF file
        try {
            nc.write(nc.findVariable("species"), species);
            nc.write(nc.findVariable("x"), x);
            nc.write(nc.findVariable("y"), y);
            nc.write(nc.findVariable("abundance"), abundance);
            nc.write(nc.findVariable("age"), age);
            nc.write(nc.findVariable("length"), length);
            nc.write(nc.findVariable("weight"), weight);
            nc.write(nc.findVariable("trophiclevel"), trophiclevel);

            if(useGenet) {
                nc.write(nc.findVariable("genotype"), genotype);
                nc.write(nc.findVariable("trait_variance"), traitnoise);
            }

            if(useBioen) {
                nc.write(nc.findVariable("gonadWeight"), gonadicWeight);
                nc.write(nc.findVariable("maturity"), maturity);
            }

            nc.close();
            //close(nc);
        } catch (IOException ex) {
            error("Error writing snapshot " + file.getAbsolutePath(), ex);
        } catch (InvalidRangeException ex) {
            error("Error writing snapshot " + file.getAbsolutePath(), ex);
        }
    }

    private NetcdfFormatWriter createNCFile(int iStepSimu) {

        NetcdfFormatWriter nc = null;
        NetcdfFormatWriter.Builder bNc = null;
        this.file = null;

        /*
         * Create NetCDF file
         */
        File path = new File(getConfiguration().getOutputPathname());
        this.file = new File(path, getFilename(iStepSimu));
        this.file.getParentFile().mkdirs();

        Nc4Chunking chunker = Nc4ChunkingStrategy.factory(Nc4ChunkingStrategy.Strategy.none, 0, false);
        bNc = NetcdfFormatWriter.createNewNetcdf4(getConfiguration().getNcOutVersion(), file.getAbsolutePath(), chunker);

        /*
         * Create dimensions
         */
        Dimension schoolDim = bNc.addDimension("nschool", getSchoolSet().getSchools().size());
        /*
         * Add variables
         */
        Variable.Builder<?> specVarBuilder = bNc.addVariable("species", DataType.INT, "nschool");
        specVarBuilder.addAttribute(new Attribute("description", "index of the species"));

        Variable.Builder<?> xVarBuilder = bNc.addVariable("x", DataType.FLOAT, "nschool");
        xVarBuilder.addAttribute(new Attribute("units", "scalar"));
        xVarBuilder.addAttribute(new Attribute("description", "x-grid index of the school"));

        Variable.Builder<?> yVarBuilder = bNc.addVariable("y", DataType.FLOAT, "nschool");
        yVarBuilder.addAttribute(new Attribute("units", "scalar"));
        yVarBuilder.addAttribute(new Attribute("description", "y-grid index of the school"));

        Variable.Builder<?> abVarBuilder = bNc.addVariable("abundance", DataType.DOUBLE, "nschool");
        abVarBuilder.addAttribute(new Attribute("units", "scalar"));
        abVarBuilder.addAttribute(new Attribute("description", "number of fish in the school"));

        Variable.Builder<?>ageVarBuilder = bNc.addVariable("age", DataType.FLOAT, "nschool");
        ageVarBuilder.addAttribute(new Attribute("units", "year"));
        ageVarBuilder.addAttribute(new Attribute("description", "age of the school in year"));

        Variable.Builder<?>lengthVarBuilder = bNc.addVariable("length", DataType.FLOAT, "nschool");
        lengthVarBuilder.addAttribute(new Attribute("units", "cm"));
        lengthVarBuilder.addAttribute(new Attribute("description", "length of the fish in the school in centimeter"));

        Variable.Builder<?>weightVarBuilder = bNc.addVariable("weight", DataType.FLOAT, "nschool");
        weightVarBuilder.addAttribute(new Attribute("units", "g"));
        weightVarBuilder.addAttribute(new Attribute("description", "weight of the fish in the school in gram"));

        if (this.getConfiguration().isBioenEnabled()) {
            Variable.Builder<?> gonadWeightVaBuilder = bNc.addVariable("gonadWeight", DataType.FLOAT, "nschool");
            gonadWeightVaBuilder.addAttribute(new Attribute("units", "g"));
            gonadWeightVaBuilder.addAttribute(new Attribute("description", "gonad weight of the fish in the school in gram"));
            Variable.Builder<?>  maturityVarBuilder = bNc.addVariable("maturity", DataType.INT, "nschool");
            maturityVarBuilder.addAttribute(new Attribute("units", ""));
            maturityVarBuilder.addAttribute(new Attribute("description", "True if the school is mature"));
        }

        Variable.Builder<?> tlVarBuilder = bNc.addVariable("trophiclevel", DataType.FLOAT, "nschool");
        tlVarBuilder.addAttribute(new Attribute("units", "scalar"));
        tlVarBuilder.addAttribute(new Attribute("description", "trophiclevel of the fish in the school"));

        if(this.getConfiguration().isGeneticEnabled()) {
            // Determines the maximum number of Loci that codes a trait
            nTrait = this.getSimulation().getNEvolvingTraits();
            for (int iTrait = 0; iTrait < nTrait; iTrait++) {
                Trait trait = this.getEvolvingTrait(iTrait);
                for (int iSpec = 0; iSpec < this.getConfiguration().getNSpecies(); iSpec++) {
                    nMaxLoci = Math.max(trait.getNLocus(iSpec), nMaxLoci);
                }
            }

           //Dimension[] dimOut = new Dimension[3] {
           Dimension traitDim = bNc.addDimension("trait", nTrait);
           Dimension lociDim = bNc.addDimension("loci", nMaxLoci);
           Dimension locValDim = bNc.addDimension("loci_val",  2);

           bNc.addVariable("genotype", DataType.FLOAT, new ArrayList<>(Arrays.asList(schoolDim, traitDim, lociDim, locValDim)));
           bNc.addVariable("trait_variance", DataType.FLOAT, new ArrayList<>(Arrays.asList(schoolDim, traitDim)));

        }

        /*
         * Add global attributes
         */
        bNc.addAttribute(new Attribute("step", String.valueOf(iStepSimu)));
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            str.append(i);
            str.append("=");
            str.append(getSpecies(i).getName());
            str.append(" ");
        }

        bNc.addAttribute(new Attribute("species", str.toString()));

        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc = bNc.build();

        } catch (IOException ex) {
            error("Could not create snapshot file " + file.getAbsolutePath(), ex);
        }

        return nc;
    }

    String getFilename(int iStepSimu) {
        StringBuilder filename = new StringBuilder("restart");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_snapshot_step");
        filename.append(iStepSimu);
        filename.append(".nc.");
        filename.append(getRank());
        return filename.toString();
    }
}
