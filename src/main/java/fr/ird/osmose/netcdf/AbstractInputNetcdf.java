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
import fr.ird.osmose.School;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author nbarrier
 */
public abstract class AbstractInputNetcdf {

    /**
     * Input netcdf file.
     */
    private NetcdfFile nc;
    /**
     * Variable to read. For instance, accessibility.
     */
    private final String varname;
    /**
     * Netcdf filename.
     */
    private final String filename;

    /** Number of records (i.e. time steps) in the Netcdf file. */
    private int nRecords;
    
    /** Abstract method for class initialization.
     * @throws java.io.IOException */
    public abstract void init() throws IOException;
    
    /**
     * Variable array to read. Should be of dims (time, prey, pred).
     */
    protected double varArray[][][];  // accessibility matrix: (time, prey, pred)
    
    
    /** Returns the Netcdf filename.
     * @return Filename  */
    public String getFilename() {
        return this.filename;
    }
    
    /** Returns the Netcdf variable name. */
    public String getVarname() {
        return this.varname;
    }
    
    /** Returns the NetcdfFile object.
     * @return 
     */
    public NetcdfFile getNcFile() {
        return this.nc;
    }
    
    /** Open the NetcdfFile object.
     * 
     * @throws IOException 
     */
    public void openNcFile() throws IOException {
        nc = NetcdfFile.open(filename, null);
    }

    /** Public constructor. 
     * 
     * @param filename
     * @param varname 
     */
    public AbstractInputNetcdf(String filename, String varname) {
        this.varname = varname;
        this.filename = filename;
    }
    
    /** Get the number of time steps in the file.
     * 
     * @return 
     */
    public int getNRecords() {
        return this.nRecords;
    }
    
    /** Sets the number of time steps in the file.
     * 
     * @param nRecords 
     */
    public void setNRecords(int nRecords) {
        this.nRecords = nRecords;
    }
    
    /** Converts char array ([][]) into  string array ([]).
     * @param input
     * @return  */
    public String[] getStringVar(char[][] input) {
        int nPrey = input.length;
        String output[] = new String[nPrey];
        for (int itmp = 0; itmp < nPrey; itmp++) {
            output[itmp] = String.valueOf(input[itmp]);
        } 
        return output;    
    }
    
    public int getNcIndex(int timeStep) { 
        return (timeStep % this.getNRecords());
    }
    
}
