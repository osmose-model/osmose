package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class SchoolBasedIndicator extends SimulationLinker implements Indicator {

    /**
     * The function details how this school contribute to the corresponding
     * indicator.
     *
     * @param school, an alive school from the population.
     */
    abstract public void update(School school);

    /**
     * The function is called every time step, at the end of the step, usually
     * before the reproduction process.
     */
    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            update(school);
        }
    }
}
