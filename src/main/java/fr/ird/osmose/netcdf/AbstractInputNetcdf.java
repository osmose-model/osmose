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
package fr.ird.osmose.netcdf;

import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author nbarrier
 */
public class AbstractInputNetcdf {

    /** Input netcdf file. */
    private NetcdfFile nc;
    private final String varname;
    private final String filename;
    private int shape[];
    private ClassType classType;
    private double[] classPrey, classPred;
    private String[] namesPrey, namesPred;
    private ClassManager classManager;
    private double varArray[][];
    
    private interface ClassManager  {
        double getClass(IAggregation school);
    } 

    public AbstractInputNetcdf(String filename, String varname) {
        this.varname = varname;
        this.filename = filename;
        try {
            this.init();
        } catch (IOException ex) {
            Logger.getLogger(AbstractInputNetcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void init() throws IOException {

        nc = NetcdfFile.open(filename, null);

        // Recovers the variable to read
        Variable netcdfVar = nc.findVariable(varname);
        varArray = (double [][]) netcdfVar.read().copyToNDJavaArray();

        Variable preyClassVar = nc.findVariable("preyClass");
        classPrey = (double[]) preyClassVar.read().copyTo1DJavaArray();

        Variable predClassVar = nc.findVariable("predClass");
        classPred = (double[]) predClassVar.read().copyTo1DJavaArray();

        Variable preySpeciesVar = nc.findVariable("preySpeciesName");
        namesPrey = (String[]) preySpeciesVar.read().copyTo1DJavaArray();
 
        Variable predSpeciesVar = nc.findVariable("predSpeciesName");
        namesPred = (String[]) predSpeciesVar.read().copyTo1DJavaArray();
        
        Attribute attVar = preyClassVar.findAttribute("type");
        String type = (String) attVar.getValue(0);
        switch (type.toLowerCase()) {
            case ("age"):
                classType = ClassType.AGE;
                classManager = (x) -> x.getAge();
                break;
            case ("size"):
                classType = ClassType.SIZE;
                classManager = (x) -> x.getLength();
                break;
            default:
                classType = ClassType.SIZE;
        }
        
        nc.close();
        
    } // end of init

    public int findIndex(String name, double specClass, String[] listNames, double[] listClass) {
        int index = -1;
        for (int i = 0; i < listNames.length; i++) {
            if (this.namesPred[i].equals(name)) {
                if (specClass < this.classPred[i]) {
                    index = i;
                    return(index);
                }
            }
        }
        
        return index;
        
    }
    
    
    public double[] getAccessibility(IAggregation predator, List<IAggregation> preys) {
        int indexPred = this.findIndex(predator.getSpeciesName(), this.classManager.getClass(predator), namesPred, classPred);
        double[] output = new double[preys.size()];
        for(int iprey=0; iprey < preys.size(); iprey++) {
            IAggregation prey = preys.get(iprey);
            int indexPrey = this.findIndex(prey.getSpeciesName(), this.classManager.getClass(prey), namesPrey, classPrey);
            try {
                output[iprey] = varArray[indexPrey][indexPred];
            } catch (ArrayIndexOutOfBoundsException ex) {
                String errorMsg = String.format("Problem with indexes. indexPrey=%d and indexPred=%d", indexPrey, indexPred);
                System.out.println(errorMsg);
                throw(ex);
            }
        }       
        
        return(output);
        
    }

    private enum SpeciesType {
        PREY,
        PRED;
    }

    
}
