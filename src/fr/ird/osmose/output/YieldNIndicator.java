/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class YieldNIndicator extends AbstractIndicator {

    public double[] yieldN;
    
     public YieldNIndicator(int replica) {
        super(replica);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        yieldN = new double[getNSpecies()];

    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            yieldN[school.getSpeciesIndex()] += school.nDeadFishing;
        }
    }

    @Override
    public boolean isEnabled() {
        return !getOsmose().isCalibrationOutput();
    }

    @Override
    public void write(float time) {

        writeVariable(time, yieldN);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getOsmose().outputPrefix);
        filename.append("_yieldN_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "cumulative catch (number of fish caught per time step of saving). ex: if time step of saving is the year, then annual catches in fish numbers are saved";
    }

    @Override
    String[] getHeaders() {
        String[] species = new String[getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = getSimulation().getSpecies(i).getName();
        }
        return species;
    }
}
