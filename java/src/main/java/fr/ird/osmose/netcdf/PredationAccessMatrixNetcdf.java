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
import java.io.IOException;
import java.util.List;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author nbarrier
 */
public class PredationAccessMatrixNetcdf extends AbstractInputNetcdf {

    /**
     * Upper bounds of the class. Either age or size. One for prey, one for pred
     */
    private double[] classPrey, classPred;

    /**
     * Species name. One for prey, one for pred. One for each class
     */
    private String[] namesPrey, namesPred;

    /**
     * Interface to recover the class of a given school. Pointer to getSize or
     * getAge school methods.
     */
    private ClassManager classManager;

    /**
     * Defines the interface to recover the class.
     */
    private interface ClassManager {

        double getClass(IAggregation school);
    }

    public PredationAccessMatrixNetcdf(String filename, String varname) {
        super(filename, varname);
    }

    @Override
    public void init() throws IOException {

        // Open the Netcdf file
        this.openNcFile();
        
        String varname = this.getVarname();
        
        // Recovers the number of time records.
        try (NetcdfFile nc = this.getNcFile()) {
            
            // Recovers the number of time records.
            Dimension timeDim = nc.findDimension("time");
            this.setNRecords(timeDim.getLength());
            
            // Recovers the variable to read
            Variable netcdfVar = nc.findVariable(varname);
            varArray = (double[][][]) netcdfVar.read().copyToNDJavaArray();
            
            // Reads the upper bounds of the predator/prey class
            Variable preyClassVar = nc.findVariable("preyClass");
            classPrey = (double[]) preyClassVar.read().copyTo1DJavaArray();
            
            Variable predClassVar = nc.findVariable("predClass");
            classPred = (double[]) predClassVar.read().copyTo1DJavaArray();
            
            // Reads the species names for the prey and predators
            Variable preySpeciesVar = nc.findVariable("preySpeciesName");
            namesPrey = this.getStringVar((char[][]) preySpeciesVar.read().copyTo1DJavaArray());
            
            Variable predSpeciesVar = nc.findVariable("predSpeciesName");
            namesPred = this.getStringVar((char[][]) predSpeciesVar.read().copyTo1DJavaArray());
            
            // Loop for the type attribute for the netcdfVar
            Attribute attVar = netcdfVar.findAttribute("class_type");
            if (attVar == null) {
                // If no attribute has been found, assumes that length is used.
                classManager = (x) -> x.getLength();
            } else {
                String type = (String) attVar.getValue(0);
                switch (type.toLowerCase()) {
                    case ("age"):
                        classManager = (x) -> x.getAge();
                        break;
                    case ("size"):
                        classManager = (x) -> x.getLength();
                        break;
                    default:
                        classManager = (x) -> x.getLength();
                }
            }
        } // end of try

    } // end of init

    /**
     * Finds the index that match the name and the class within 2 lists.
     *
     * @param name Name of the species to match (either predator or prey)
     * @param specClass Class of the species to match (either predator or prey,
     * age or size)
     * @param listNames List of species names
     * @param listClass List of species class upper bounds
     * @return
     */
    public int findIndex(String name, double specClass, String[] listNames, double[] listClass) throws Exception {
        int index = -1;

        if (listNames.length != listClass.length) {
            throw new Exception("The listNames and listClass don't have the same size");
        }

        for (int i = 0; i < listNames.length; i++) {
            // Loop over the list of names. 
            if (listNames[i].equals(name)) {
                // If the name matches, check for the class
                if (specClass < listClass[i]) {
                    index = i;
                    return (index);
                }
            }
        }

        return index;

    }

    /**
     * Finds the accessibility coefficient for a given time step.
     *
     * @param timeStep Simulation time step
     * @param predator Predator (should be of type School).
     * @param preys
     * @return
     * @throws Exception
     */
    public double[] getAccessibility(int timeStep, IAggregation predator, List<IAggregation> preys) throws Exception {

        this.setNcIndex(timeStep);
        
        // finds the index (i.e. column index) for the predator
        int indexPred = this.findIndex(predator.getSpeciesName(), this.classManager.getClass(predator), namesPred, classPred);

        double[] output = new double[preys.size()];
        for (int iprey = 0; iprey < preys.size(); iprey++) {
            IAggregation prey = preys.get(iprey);
            // finds the prey index (i.e. row index)
            int indexPrey = this.findIndex(prey.getSpeciesName(), this.classManager.getClass(prey), namesPrey, classPrey);
            try {
                output[iprey] = varArray[this.getNcIndex()][indexPrey][indexPred];
            } catch (ArrayIndexOutOfBoundsException ex) {
                String errorMsg = String.format("Problem with indexes. indexPrey=%d and indexPred=%d", indexPrey, indexPred);
                System.out.println(errorMsg);
                throw (ex);
            }
        }

        return (output);

    }
}
