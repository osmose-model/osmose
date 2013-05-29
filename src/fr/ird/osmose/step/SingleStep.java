/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

/**
 *
 * @author pverley
 */
public class SingleStep extends ConcomitantMortalityStep {
    
    public SingleStep(int indexSimulation) {
        super(indexSimulation);
    }
    
    @Override
    public void step(int iStepSimu) {
        super.step(iStepSimu);
        indicators.close();
        getSimulation().makeSnapshot();
        System.exit(0);
    }
    
}
