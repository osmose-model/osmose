/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose.stage;

import fr.ird.osmose.ISchool;

/**
 *
 * @author pverley
 */
public interface IStage {
    
    public void init();

    public int getStage(ISchool school);

    public int getNStage(int iSpecies);

    public float[] getThresholds(int iSpecies);
    
}
