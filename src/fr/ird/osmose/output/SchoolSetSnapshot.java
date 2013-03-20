/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public SchoolSetSnapshot(int iSimulation) {
        super(iSimulation);
    }

    public void makeSnapshot(int iStepSimu) {

        NetcdfFileWriteable nc = createNCFile(iStepSimu);
        int nSchool = getSchoolSet().size();
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
        for (School school : getSchoolSet()) {
            species.set(s, school.getSpeciesIndex());
            x.set(s, school.getX());
            y.set(s, school.getY());
            abundance.set(s, school.getInstantaneousAbundance());
            age.set(s, (float) school.getAgeDt() / getConfiguration().getNStepYear());
            length.set(s, school.getLength());
            weight.set(s, school.getWeight());
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
            getLogger().log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            getLogger().log(Level.SEVERE, null, ex);
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
            getLogger().log(Level.SEVERE, "Could not create NetCDF file " + nc.getLocation(), ex);
        }
        /*
         * Create dimensions
         */
        Dimension nSchool = nc.addDimension("nschool", getSchoolSet().size());
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
            str.append(getSimulation().getSpecies(i).getName());
            str.append(" ");
        }
        nc.addGlobalAttribute("species", str.toString());
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();

        } catch (IOException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return nc;
    }

    /**
     * Closes the NetCDF file.
     */
    private void close(NetcdfFileWriteable nc) {
        try {
            nc.close();
            String strFilePart = nc.getLocation();
            String strFileBase = strFilePart.substring(0, strFilePart.indexOf(".part"));
            File filePart = new File(strFilePart);
            File fileBase = new File(strFileBase);
            filePart.renameTo(fileBase);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Error closing NetCDF restart file " + nc.getLocation(), ex);
        }
    }

    String getFilename(int iStepSimu) {
        StringBuilder filename = new StringBuilder();
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_snapshot");
        filename.append(iStepSimu);
        filename.append("_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".nc");
        return filename.toString();
    }
}
