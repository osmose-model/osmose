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
import fr.ird.osmose.Prey;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.distribution.AbstractDistribution;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class DietDistribOutput_Netcdf extends AbstractDistribOutput_Netcdf {

    private final Species species;

    public DietDistribOutput_Netcdf(int rank, Species species, AbstractDistribution distrib) {
        super(rank, distrib);
        this.species = species;
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
    }

    @Override
    public void reset() {
        values = new double[getNSpecies() + getConfiguration().getNPlankton()][getNClass()];
    }

    @Override
    String getFilename() {

        StringBuilder filename = this.initFileName();
        filename.append("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_dietMatrixby");
        filename.append(getType().toString());
        filename.append("-");
        filename.append(species.getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Distribution of the biomass (tonne) of prey species (in columns) in the diet of ");
        description.append(species.getName());
        description.append(" by ");
        description.append(getType().getDescription());
        description.append(". For class i, the preyed biomass in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    public void update() {

        for (School predator : getSchoolSet().getSchools(species, false)) {
            double preyedBiomass = predator.getPreyedBiomass();
            if (preyedBiomass > 0) {
                for (Prey prey : predator.getPreys()) {
                    int classPredator = getClass(predator);
                    if (classPredator >= 0) {
                        values[prey.getSpeciesIndex()][classPredator] += prey.getBiomass();
                    }
                }
            }
        }
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    @Override
    void init_nc_dims_coords() {

        // Defines the prey dimension and coordinate. 
        Dimension preyDim = getNc().addDimension("prey_index", getNSpecies() + getConfiguration().getNPlankton());
        StringBuilder bld = new StringBuilder();

        getNc().addVariable("prey_index", DataType.FLOAT, new Dimension[]{preyDim});
        int k = 0;
        for (int i = 0; i < getNSpecies(); i++) {
            String name = String.format("prey%d", k);
            getNc().addVariableAttribute("prey_index", name, getSpecies(i).getName());
            k++;
        }

        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            String name = String.format("prey%d", k);
            getNc().addVariableAttribute("prey_index", name, getConfiguration().getPlankton(i).getName());
            k++;
        }

        // Defines the prey dimension and coordinate. 
        Dimension classDim = getNc().addDimension(this.getDisName(), this.getNClass());
        getNc().addVariable(this.getDisName(), DataType.FLOAT, new Dimension[]{classDim});
      
        this.setDims(new Dimension[]{getTimeDim(), classDim, preyDim});

        getNc().addGlobalAttribute("Species: ", this.species.getName());
        
    }
    
    @Override
    public void write_nc_coords() {
       
        try {
            // Writes variable trait (trait names) and species (species names)
            ArrayInt.D1 arrSpecies = new ArrayInt.D1(this.getNSpecies() + getConfiguration().getNPlankton());
            for (int i = 0; i < this.getNSpecies(); i++) {
                arrSpecies.set(i, i);
            }
            getNc().write("prey_index", arrSpecies);

            // Writes variable trait (trait names) and species (species names)
            ArrayDouble.D1 arrClass = new ArrayDouble.D1(this.getNClass());
            for (int i = 0; i < this.getNClass(); i++) {
                arrClass.set(i, this.getClassThreshold(i));
            }
            getNc().write(this.getDisName(), arrClass);
            
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietDistribOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    String getUnits() {
        return ("tons"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return ("prey_biomass"); //To change body of generated methods, choose Tools | Templates.
    }
}
