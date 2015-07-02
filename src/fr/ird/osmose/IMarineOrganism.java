/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public interface IMarineOrganism {
    
    public int getSpeciesIndex();
    
    public float getAge();

    public float getLength();

    public float getTrophicLevel();
    
    public float getWeight();
    
}
