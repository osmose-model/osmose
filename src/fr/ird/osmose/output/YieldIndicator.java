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
public class YieldIndicator extends AbstractIndicator {

    public double[] yield;

    public YieldIndicator(int replica, String keyEnabled) {
        super(replica, keyEnabled);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        yield = new double[getNSpecies()];

    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            yield[school.getSpeciesIndex()] += school.adb2biom(school.getNdeadFishing());
        }
    }

    @Override
    public void write(float time) {

        writeVariable(time, yield);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getString("output.file.prefix"));
        filename.append("_yield_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "cumulative catch (tons per time step of saving). ex: if time step of saving is the year, then annual catches are saved";
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
