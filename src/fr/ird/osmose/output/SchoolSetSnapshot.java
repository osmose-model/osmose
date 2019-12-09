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
 * processes of fish life cycle (growth, explicit predation, natural and
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
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class SchoolSetSnapshot extends SimulationLinker {
    
    private Variable xVar, yVar, abVar, ageVar, lengthVar, weightVar, tlVar, specVar;
    private Variable genetVar;
    private int nTrait=0;
    private int nMaxLoci;
    
    public SchoolSetSnapshot(int rank) {
        super(rank);
    }

    public void makeSnapshot(int iStepSimu) {

        boolean useGenet = this.getConfiguration().useGenetic();
        ArrayFloat.D4 genotype = null;
        NetcdfFileWriter nc = createNCFile(iStepSimu);
        int nSchool = getSchoolSet().getSchools().size();
        ArrayInt.D1 species = new ArrayInt.D1(nSchool);
        ArrayFloat.D1 x = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 y = new ArrayFloat.D1(nSchool);
        ArrayDouble.D1 abundance = new ArrayDouble.D1(nSchool);
        ArrayFloat.D1 age = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 length = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 weight = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 trophiclevel = new ArrayFloat.D1(nSchool);
        
        // if use genetic, initialize the output of the genotype (nschool x ntrait x nloci x 2)
        if (useGenet) {
            genotype = new ArrayFloat.D4(nSchool, this.nTrait, this.nMaxLoci, 2);
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
            
            if(useGenet) {
                // If use genetic module, save the list of loci pairs for each trait.
                Genotype gen = school.getGenotype();
                for(int iTrait=0; iTrait<nTrait; iTrait++) {
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
            nc.write(this.specVar, species);
            nc.write(this.xVar, x);
            nc.write(this.yVar, y);
            nc.write(this.abVar, abundance);
            nc.write(this.ageVar, age);
            nc.write(this.lengthVar, length);
            nc.write(this.weightVar, weight);
            nc.write(this.tlVar, trophiclevel);
            if(useGenet) { 
                nc.write(this.genetVar, genotype);
            }
            nc.close();
            //close(nc);
        } catch (IOException ex) {
            error("Error writing snapshot " + nc.getNetcdfFile().getLocation(), ex);
        } catch (InvalidRangeException ex) {
            error("Error writing snapshot " + nc.getNetcdfFile().getLocation(), ex);
        }
    }

    private NetcdfFileWriter createNCFile(int iStepSimu) {

        NetcdfFileWriter nc = null;
        File file = null;
        
        /*
         * Create NetCDF file
         */
        try {
          
            File path = new File(getConfiguration().getOutputPathname());
            file = new File(path, getFilename(iStepSimu));
            file.getParentFile().mkdirs();
            nc = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, file.getAbsolutePath());
        } catch (IOException ex) {
            error("Could not create snapshot file " + file.getAbsolutePath(), ex);
        }
        /*
         * Create dimensions
         */
        Dimension nSchool = nc.addDimension(null, "nschool", getSchoolSet().getSchools().size());
        /*
         * Add variables
         */
        specVar = nc.addVariable(null, "species", DataType.INT, "nschool");
        specVar.addAttribute(new Attribute("description", "index of the species"));

        xVar = nc.addVariable(null, "x", DataType.FLOAT, "nschool");
        xVar.addAttribute(new Attribute("units", "scalar"));
        xVar.addAttribute(new Attribute("description", "x-grid index of the school"));

        yVar = nc.addVariable(null, "y", DataType.FLOAT, "nschool");
        yVar.addAttribute(new Attribute("units", "scalar"));
        yVar.addAttribute(new Attribute("description", "y-grid index of the school"));

        abVar = nc.addVariable(null, "abundance", DataType.DOUBLE, "nschool");
        abVar.addAttribute(new Attribute("units", "scalar"));
        abVar.addAttribute(new Attribute("description", "number of fish in the school"));

        ageVar = nc.addVariable(null, "age", DataType.FLOAT, "nschool");
        ageVar.addAttribute(new Attribute("units", "year"));
        ageVar.addAttribute(new Attribute("description", "age of the school in year"));

        lengthVar = nc.addVariable(null, "length", DataType.FLOAT, "nschool");
        lengthVar.addAttribute(new Attribute("units", "cm"));
        lengthVar.addAttribute(new Attribute("description", "length of the fish in the school in centimeter"));

        weightVar = nc.addVariable(null, "weight", DataType.FLOAT, "nschool");
        weightVar.addAttribute(new Attribute("units", "g"));
        weightVar.addAttribute(new Attribute("description", "weight of the fish in the school in gram"));

        tlVar = nc.addVariable(null, "trophiclevel", DataType.FLOAT, "nschool");
        tlVar.addAttribute(new Attribute("units", "scalar"));
        tlVar.addAttribute(new Attribute("description", "trophiclevel of the fish in the school"));
        
        if(this.getConfiguration().useGenetic()) {
            // Determines the maximum number of Loci that codes a trait
            nTrait = this.getSimulation().getNEvolvingTraits();
            for (int iTrait = 0; iTrait < nTrait; iTrait++) {
                Trait trait = this.getEvolvingTrait(iTrait);
                for (int iSpec = 0; iSpec < this.getConfiguration().getNSpecies(); iSpec++) {
                    nMaxLoci = Math.max(trait.getNLocus(iSpec), nMaxLoci);
                }   
            }
            
           //Dimension[] dimOut = new Dimension[3] {
           Dimension traitDim = new Dimension(null, nTrait);
           Dimension lociDim = new Dimension(null, nMaxLoci);
           Dimension schoolDim = new Dimension(null, getSchoolSet().getSchools().size());
           genetVar = nc.addVariable(null, "genotyope", DataType.FLOAT, new ArrayList(Arrays.asList(schoolDim, traitDim, lociDim)));
        }
        
        /*
         * Add global attributes
         */
        nc.addGroupAttribute(null, new Attribute("step", String.valueOf(iStepSimu)));
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            str.append(i);
            str.append("=");
            str.append(getSpecies(i).getName());
            str.append(" ");
        }
        nc.addGroupAttribute(null, new Attribute("species", str.toString()));
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();

        } catch (IOException ex) {
            error("Could not create snapshot file " + nc.getNetcdfFile().getLocation(), ex);
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
