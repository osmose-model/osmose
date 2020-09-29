/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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
