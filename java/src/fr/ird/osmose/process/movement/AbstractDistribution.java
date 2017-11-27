/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.movement;

import fr.ird.osmose.School;
import fr.ird.osmose.util.OsmoseLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractDistribution extends OsmoseLinker {

    abstract public void init();
    
    abstract public void move(School school, int iStepSimu);
    
}
