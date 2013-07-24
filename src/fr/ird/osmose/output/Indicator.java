
package fr.ird.osmose.output;

/**
 *
 * @author pverley
 */
public interface Indicator {
    
    /**
     * This function will be called at the beginning of every time step, before
     * any process occurred.
     * Indeed for some indicators it might be necessary to know the state of
     * the system just after the reproduction and before the following step.
     */
    public void initStep();

    /**
     * Reset the indicator after a saving step has been written in output file.
     * It will be automatically called after the write(time) function
     */
    public void reset();

    /**
     * The function is called every time step, at the end of the step,
     * usually before the reproduction process.
     */
    public void update();

    /**
     * Whether or not the indicator is activated in the current simulation.
     *
     * @return true is the indicator should be saved
     */
    public boolean isEnabled();

    /**
     * Write the indicator in output file at specified time
     *
     * @param time, expressed in year
     */
    public void write(float time);
    
    /**
     * Whether the parameter should be written at specified time step.
     * @param iStepSimu, the current step of the simulation.
     * @return true if the parameter should be written in the file at the
     * specified time step, false otherwise.
     */
    public boolean isTimeToWrite(int iStepSimu);
    
    /**
     * Initializes the indicator. Load parameters and create the file.
     */
    public void init();
    
    /**
     * Closes the file
     */
    public void close();
    
}
