/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import fr.ird.osmose.process.mortality.MortalityCause;

/**
 *
 * @author pverley
 */
public interface IAggregation extends IMarineOrganism {

    public double getBiomass();
    
    public double getInstantaneousBiomass();
    
    public double getAbundance();
    
    public double getInstantaneousAbundance();
    
    public void incrementNdead(MortalityCause cause, double nDead);
    
    public double biom2abd(double biomass);

}
