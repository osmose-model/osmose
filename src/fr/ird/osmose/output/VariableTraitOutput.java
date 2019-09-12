/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
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
import fr.ird.osmose.Species;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.ma2.ArrayString;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayInt;

/**
 *
 * @author pverley
 */
public class VariableTraitOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    
    private double timeOut;
    private int counter;
    
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;
    
    // spatial indicators
    private float[][] trait_mean;
    private float[][] trait_var;
    
    private int recordFrequency;

    public VariableTraitOutput(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");
        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc.setLocation(filename);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension speciesDim = nc.addDimension("species", getNSpecies());
        Dimension traitDim = nc.addDimension("trait", this.getSimulation().getNEvolvingTraits());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "days since 0-1-1 0:0:0");
        nc.addVariableAttribute("time", "calendar", "360_day");
        nc.addVariableAttribute("time", "description", "time ellapsed, in days, since the beginning of the simulation");
      
        nc.addVariable("trait_mean", DataType.FLOAT, new Dimension[]{timeDim, traitDim, speciesDim});
        nc.addVariableAttribute("trait_mean", "units", "");
        nc.addVariableAttribute("trait_mean", "description", "Mean value of the trait");
        nc.addVariableAttribute("trait_mean", "_FillValue", -99.f);
        
        nc.addVariable("trait_var", DataType.FLOAT, new Dimension[]{timeDim, traitDim, speciesDim});
        nc.addVariableAttribute("trait_var", "units", "");
        nc.addVariableAttribute("trait_var", "description", "Variance of the trait");
        nc.addVariableAttribute("trait_var", "_FillValue", -99.f);
        
        nc.addVariable("trait", DataType.INT, new Dimension[]{traitDim});
        nc.addVariable("species", DataType.INT, new Dimension[]{speciesDim});
        
        StringBuilder bld = new StringBuilder();
        for(int i = 0; i<this.getNSpecies(); i++) {
            bld.append(String.format("Species%d=, name=%s\n", i, this.getSpecies(i).getName()));
        }
        nc.addVariableAttribute("species", "description", bld.toString());
        
        bld = new StringBuilder();
        for (int i = 0; i < this.getNEvolvingTraits(); i++) {
            String attr = String.format("Trait%d=, name=%s\n", i, this.getEvolvingTrait(i).getName());
            bld.append(attr);
        }
        nc.addVariableAttribute("trait", "description", bld.toString());
        
          
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
            
            for(int i = 0; i < this.getNSpecies(); i++) { 
                arrSpecies.set(i, i);
            }
            
            for (int i = 0; i < this.getSimulation().getNEvolvingTraits(); i++) {
                arrTrait.set(i, i);
            }
            
            nc.write("species", arrSpecies);
            nc.write("trait", arrTrait);

        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() {
        try {
            nc.close();
            String strFilePart = nc.getLocation();
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
        this.trait_mean = new float[this.getSimulation().getNEvolvingTraits()][this.getNSpecies()];
        this.trait_var = new float[this.getSimulation().getNEvolvingTraits()][this.getNSpecies()];
    }

    @Override
    public void update() {

        for (int i = 0; i < this.getNSpecies(); i++) {

            // recovering the species object
            Species species = this.getSpecies(i);

            // listing all the schools that belong to the given species
            List<School> listSchool = this.getSchoolSet().getSchools(species);
            int nschool = listSchool.size();
            double[] temp = new double[nschool];   // one value per individual in the school 
            
            // Loop over all the traits
            for (int itrait = 0; itrait < this.getNEvolvingTraits(); itrait++) {
                String traitName = this.getEvolvingTrait(itrait).getName();
                for (int jschool = 0; jschool < nschool; jschool++) {
                    try {
                        temp[jschool] = listSchool.get(jschool).getTrait(traitName);
                    } catch (Exception ex) {
                        Logger.getLogger(VariableTraitOutput.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                // mean value of the trait for the given species
                double tmean = this.computeMean(temp);
                double tvar = this.computeVar(temp, tmean);
                this.trait_mean[itrait][i] = (float) tmean;
                this.trait_var[itrait][i] = (float) tvar;

            }

        }  // end of species loop
        
        this.write_step();
        this.reset();
        
    }   // end of method

    public void write_step() {

        int iStepSimu = this.getSimulation().getIndexTimeSimu();
        float time = (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
        
        ArrayFloat.D3 arrMean = new ArrayFloat.D3(1, this.getNEvolvingTraits(), this.getNSpecies());
        ArrayFloat.D3 arrVar = new ArrayFloat.D3(1, this.getNEvolvingTraits(), this.getNSpecies());

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        int nTraits = this.getNEvolvingTraits();

        for (int j = 0; j < nTraits; j++) {
            for (int i = 0; i < nSpecies; i++) {
                arrMean.set(0, j, i, trait_mean[j][i]);
                arrVar.set(0, j, i, trait_var[j][i]);
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) this.timeOut * 360 / (float) this.counter);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("time", new int[]{index}, arrTime);
            nc.write("trait_mean", new int[]{index, 0, 0}, arrMean);
            nc.write("trait_var", new int[]{index, 0, 0}, arrVar);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    
    
    private double computeMean(double[] input) { 
        double output = 0.d;
        int N = input.length;
        for (int i = 0; i < N; i++) { 
            output += input[i];
        }
        output /= N;
        return output;
    }
    
    private double computeVar(double[] input, double vmean) {
        double output = 0.d;
        int N = input.length;
        for (int i = 0; i < N; i++) {
            output += Math.pow(input[i] - vmean, 2);
        }
        output /= N;
        return output;
    }

    @Override
    public void write(float time) {
        
    }
    
}
