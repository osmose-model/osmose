/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.version;

import java.util.Calendar;

/**
 *
 * @author pverley
 */
public class Version4Update2Release2 extends AbstractVersion {

    public Version4Update2Release2() {
        super(4, 2, 2, 2019, Calendar.OCTOBER, 11);
    }

    @Override
    void updateParameters() {
        
        updateKey("simulation.use.bioen", "simulation.bioen.enabled");
        
        // got rid of iterative mortality process, so only one mortality algorithm left
        deprecateParameter("mortality.algorithm");
        
    }
}
