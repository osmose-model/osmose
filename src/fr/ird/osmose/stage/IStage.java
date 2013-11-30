/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose.stage;

import fr.ird.osmose.Prey;
import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public interface IStage {
    
    public void init();

    public int getStage(School school);
    
    public int getStage(Prey prey);

    public int getNStage(int iSpecies);

    public float[] getThresholds(int iSpecies);
    
}
