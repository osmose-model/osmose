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
package fr.ird.osmose.netcdf;

import fr.ird.osmose.IAggregation;
import fr.ird.osmose.process.mortality.FishingGear;
import java.io.IOException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author nbarrier
 */
public class FisheryAccessMatrixNetcdf extends AbstractInputNetcdf {

    /**
     * Species name. One for prey, one for pred. One for each class
     */
    private String[] namesFish, namesGear;

    public FisheryAccessMatrixNetcdf(String filename, String varname) {
        super(filename, varname);
    }

    @Override
    public void init() throws IOException {

        this.openNcFile();

        try (NetcdfFile nc = this.getNcFile()) {
            String varname = this.getVarname();
            
            // Recovers the number of time records.
            Dimension timeDim = nc.findDimension("time");
            this.setNRecords(timeDim.getLength());
            
            // Recovers the variable to read
            Variable netcdfVar = nc.findVariable(varname);
            varArray = (double[][][]) netcdfVar.read().copyToNDJavaArray();
            
            // Reads the species names for the prey and predators
            Variable speciesVar = nc.findVariable("speciesName");
            namesFish = this.getStringVar((char[][]) speciesVar.read().copyTo1DJavaArray());
            
            Variable gearVar = nc.findVariable("fishingGearName");
            namesGear = this.getStringVar((char[][]) gearVar.read().copyTo1DJavaArray());
        }

    } // end of init

    /**
     * Finds the index that match the name and the class within 2 lists.
     *
     * @param name Name of the species to match (either predator or prey)
     * @param listNames List of species names
     * @return
     * @throws java.lang.Exception
     */
    public int findIndex(String name, String[] listNames) throws Exception {

        for (int i = 0; i < listNames.length; i++) {
            // Loop over the list of names. 
            if (listNames[i].equals(name)) {
                return (i);
            }

        }

        return -1;

    }

    /**
     * Finds the accessibility coefficient for a given time step.
     *
     * @param timeStep Simulation time step
     * @param predator Predator (should be of type School).
     * @param gear
     * @param fisheryIndex
     * @return
     * @throws Exception
     */
    public double getAccessibility(int timeStep, IAggregation predator, FishingGear gear) throws Exception {

        this.setNcIndex(timeStep);
        
        
        // finds the index (i.e. column index) for the predator
        int indexSpecies = this.findIndex(predator.getSpeciesName(), this.namesFish);
        
        // fins the index of the fishing gear
        int fisheryIndex = this.findIndex(gear.getName(), this.namesGear);
        
        double output =  varArray[this.getNcIndex()][fisheryIndex][indexSpecies];

        return (output);

    }
}
