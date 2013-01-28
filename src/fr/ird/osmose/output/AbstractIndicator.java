package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractIndicator extends SimulationLinker {

    /**
     * This function will be called at the beginning of every time step, before
     * any process occurred.
     * Indeed for some indicators it might be necessary to know the state of
     * the system just after the reproduction and before the following step.
     */
    abstract public void init();

    /**
     * Reset the indicator after a saving step has been written in output file.
     * It will be automatically called after the write(time) function
     */
    abstract public void reset();

    /**
     * The function is called every time step, at the end of the step,
     * usually before the reproduction process.
     *
     * @param school, an alive school from the population. The function details
     * how this school contribute to the corresponding indicator.
     */
    abstract public void update(School school);

    /**
     * Whether or not the indicator is activated in the current simulation.
     *
     * @return true is the indicator should be saved
     */
    abstract public boolean isEnabled();

    /**
     * Write the indicator in output file at specified time
     *
     * @param time, expressed in year
     */
    abstract public void write(float time);

    /**
     * The number of simulated species
     *
     * @return the number of simulated species
     */
    public int getNSpecies() {
        return getSimulation().getNumberSpecies();
    }
}
