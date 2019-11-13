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
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * @author pverley
 */
public class SchoolSetSnapshot extends SimulationLinker {

    public SchoolSetSnapshot(int rank) {
        super(rank);
    }

    public void makeSnapshot(int iStepSimu) {

        NetcdfFileWriteable nc = createNCFile(iStepSimu);
        int nSchool = getSchoolSet().getSchools().size();
        ArrayInt.D1 species = new ArrayInt.D1(nSchool);
        ArrayFloat.D1 x = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 y = new ArrayFloat.D1(nSchool);
        ArrayDouble.D1 abundance = new ArrayDouble.D1(nSchool);
        ArrayFloat.D1 age = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 length = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 weight = new ArrayFloat.D1(nSchool);
        ArrayFloat.D1 trophiclevel = new ArrayFloat.D1(nSchool);
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
            s++;
        }
        // write the arrays in the NetCDF file
        try {
            nc.write("species", species);
            nc.write("x", x);
            nc.write("y", y);
            nc.write("abundance", abundance);
            nc.write("age", age);
            nc.write("length", length);
            nc.write("weight", weight);
            nc.write("trophiclevel", trophiclevel);
            nc.close();
            //close(nc);
        } catch (IOException ex) {
            error("Error writing snapshot " + nc.getLocation(), ex);
        } catch (InvalidRangeException ex) {
            error("Error writing snapshot " + nc.getLocation(), ex);
        }
    }

    private NetcdfFileWriteable createNCFile(int iStepSimu) {

        NetcdfFileWriteable nc = null;
        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");

            File path = new File(getConfiguration().getOutputPathname());
            File file = new File(path, getFilename(iStepSimu));
            file.getParentFile().mkdirs();
            nc.setLocation(file.getAbsolutePath());
        } catch (IOException ex) {
            error("Could not create snapshot file " + nc.getLocation(), ex);
        }
        /*
         * Create dimensions
         */
        Dimension nSchool = nc.addDimension("nschool", getSchoolSet().getSchools().size());
        /*
         * Add variables
         */
        nc.addVariable("species", DataType.INT, new Dimension[]{nSchool});
        nc.addVariableAttribute("species", "units", "scalar");
        nc.addVariableAttribute("species", "description", "index of the species");

        nc.addVariable("x", DataType.FLOAT, new Dimension[]{nSchool});
        nc.addVariableAttribute("x", "units", "scalar");
        nc.addVariableAttribute("x", "description", "x-grid index of the school");

        nc.addVariable("y", DataType.FLOAT, new Dimension[]{nSchool});
        nc.addVariableAttribute("x", "units", "scalar");
        nc.addVariableAttribute("x", "description", "y-grid index of the school");

        nc.addVariable("abundance", DataType.DOUBLE, new Dimension[]{nSchool});
        nc.addVariableAttribute("abundance", "units", "scalar");
        nc.addVariableAttribute("abundance", "description", "number of fish in the school");

        nc.addVariable("age", DataType.FLOAT, new Dimension[]{nSchool});
        nc.addVariableAttribute("age", "units", "year");
        nc.addVariableAttribute("age", "description", "age of the school in year");

        nc.addVariable("length", DataType.FLOAT, new Dimension[]{nSchool});
        nc.addVariableAttribute("length", "units", "cm");
        nc.addVariableAttribute("length", "description", "length of the fish in the school in centimeter");

        nc.addVariable("weight", DataType.FLOAT, new Dimension[]{nSchool});
        nc.addVariableAttribute("weight", "units", "g");
        nc.addVariableAttribute("weight", "description", "weight of the fish in the school in gram");

        nc.addVariable("trophiclevel", DataType.FLOAT, new Dimension[]{nSchool});
        nc.addVariableAttribute("trophiclevel", "units", "scalar");
        nc.addVariableAttribute("trophiclevel", "description", "trophiclevel of the fish in the school");
        /*
         * Add global attributes
         */
        nc.addGlobalAttribute("step", String.valueOf(iStepSimu));
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            str.append(i);
            str.append("=");
            str.append(getSpecies(i).getName());
            str.append(" ");
        }
        nc.addGlobalAttribute("species", str.toString());
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();

        } catch (IOException ex) {
            error("Could not create snapshot file " + nc.getLocation(), ex);
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
