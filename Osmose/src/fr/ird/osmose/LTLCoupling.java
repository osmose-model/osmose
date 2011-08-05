/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public interface LTLCoupling extends LTLForcing {

    public void readCouplingConfigFile(String couplingFileName);
    public void runLTLModel();
    public void calculPlanktonMortality();
    public int getStartYearLTLModel();

}
