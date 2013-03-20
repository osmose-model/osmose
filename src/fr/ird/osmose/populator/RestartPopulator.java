/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.populator;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.IOException;
import java.util.logging.Level;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class RestartPopulator extends AbstractPopulator {

    NetcdfFile nc;

    public RestartPopulator(int iSimulation) {
        super(iSimulation);
    }

    @Override
    public void init() {
        try {
            nc = NetcdfFile.open(getConfiguration().getFile("simulation.restart.file"));
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Failed to open restart file " + nc.getLocation(), ex);
        }
    }

    @Override
    public void populate() {

        int nSchool = nc.findDimension("nschool").getLength();
        try {
            int[] ispecies = (int[]) nc.findVariable("species").read().copyTo1DJavaArray();
            float[] x = (float[]) nc.findVariable("x").read().copyTo1DJavaArray();
            float[] y = (float[]) nc.findVariable("y").read().copyTo1DJavaArray();
            double[] abundance = (double[]) nc.findVariable("abundance").read().copyTo1DJavaArray();
            float[] length = (float[]) nc.findVariable("length").read().copyTo1DJavaArray();
            float[] weight = (float[]) nc.findVariable("weight").read().copyTo1DJavaArray();
            float[] age = (float[]) nc.findVariable("age").read().copyTo1DJavaArray();
            float[] trophiclevel = (float[]) nc.findVariable("trophiclevel").read().copyTo1DJavaArray();
            for (int s = 0; s < nSchool; s++) {
                Species species = getSimulation().getSpecies(ispecies[s]);
                School school = new School(
                        species,
                        x[s],
                        y[s],
                        abundance[s],
                        length[s],
                        weight[s],
                        Math.round(age[s] * getConfiguration().getNStepYear()),
                        trophiclevel[s]);
                getSchoolSet().add(school);
            }
            nc.close();
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading restart file " + nc.getLocation(), ex);
        }
    }
}
