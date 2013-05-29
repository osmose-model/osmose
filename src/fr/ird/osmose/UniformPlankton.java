/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public class UniformPlankton extends Plankton {
    
    private float biomass;
    
    public UniformPlankton(int index, String name, float sizeMin, float sizeMax, float trophicLevel, float accessCoeff, double biomassTot) {
        super(index, name, sizeMin, sizeMax, trophicLevel, -99, -99, accessCoeff);
        int nCells = getGrid().getNumberAvailableCells();
        biomass = (float) (biomassTot / nCells);
    }
    
    @Override
    public void update(int iStepSimu) {
       // do nothing
    }

    @Override
    public float getBiomass(Cell cell) {
        return biomass;
    }
}
