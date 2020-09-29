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
    
    private int ncIndex;
    
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
    
    public void closeNcFile() throws IOException {
        nc.close();
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
    
    public void setNcIndex(int timeStep) { 
        this.ncIndex = (timeStep % this.getNRecords());
    }
    
    public int getNcIndex() { 
        return this.ncIndex;
    }
    
}
