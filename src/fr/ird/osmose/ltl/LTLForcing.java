/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose.ltl;

import fr.ird.osmose.Plankton;

/**
 *
 * @author pverley
 */
public interface LTLForcing {

    public void readLTLConfigFile1(String planktonStructureFileName);
    public void readLTLConfigFile2(String planktonFileName);
    public void initPlanktonMap();
    public void updatePlankton(int dt);
    public void mapInterpolation();
    public double getBiomass(int iPlankton);
    public void savePlanktonBiomass(String fileFormat);
    public int getNbPlanktonGroups();
    public String getPlanktonName(int indexGroup);
    public Plankton getPlankton(int indexGroup);
    public int getPlanktonDimX();
    public int getPlanktonDimY();
    public int getPlanktonDimZ();
    public void createPlanktonGroups();


}
