package fr.ird.osmose;

import fr.ird.osmose.grid.IGrid;

/**
 * ******************************************************************************
 * <p>Titre : Plankton class </p>
 *
 * <p>Description : Basic unit of the plankton part of the model - represents a
 * plankton group with size and maps specified </p>
 *
 * <p>Copyright : Copyright (c) may 2009 </p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * *******************************************************************************
 */
public class Plankton {

    private final int index;
    public float trophicLevel;        // trophic level of the plankton group
    private float sizeMin, sizeMax;
    private String name;     // e.g.   phytoplankton, diatoms, copepods...
    private float accessibilityCoeff; // percentage of plankton biomass available for fish
    private float conversionFactor;   // factor to be used in order to transform biomass from plankton unit (eg mmolN/m2) towards wet weight (tons/km2)
    private float prodBiomFactor;   // factor to be used to transform biomass into production (per year)
    public float[][] biomass, accessibleBiomass, iniBiomass;         // table of transformed values in 2D
    float[][] mortalityRate;     // table for output values in 2D

    public Plankton(int index, String name, float sizeMin, float sizeMax, float trophicLevel, float conversionFactor, float prodBiomFactor, float accessCoeff) {
        this.index = index;
        this.name = name;
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;
        this.trophicLevel = trophicLevel;
        this.conversionFactor = conversionFactor;
        this.prodBiomFactor = prodBiomFactor;
        this.accessibilityCoeff = accessCoeff;
    }

    public void init() {

        // Initialization matrix corresponding to osmose grid
        mortalityRate = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        biomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        accessibleBiomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        iniBiomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                mortalityRate[i][j] = (accessibilityCoeff / 2f) * getOsmose().getNumberTimeStepsPerYear() / 365f; // default = median of the max mortality rate (in day-1)
            }
        }
    }

    public void setBiomass(int i, int j, float value) {
        biomass[i][j] = value;
        iniBiomass[i][j] = value;
        accessibleBiomass[i][j] = getAccessibilityCoeff() * value;
    }

    public void updateBiomass(float[][] newBiomass) {
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                biomass[i][j] = newBiomass[i][j];
                iniBiomass[i][j] = newBiomass[i][j];
                accessibleBiomass[i][j] = getAccessibilityCoeff() * newBiomass[i][j];
            }
        }
    }

    /*
     * Converts plankton biomass (usually from mmolN/m2) to tons/km2
     */
    public float unitConversion(float concentration) {
        // @WS2009
        //return biomToProd(concentration * conversionFactor);

        // @SCHOOL2012
        return concentration * conversionFactor;
    }

    public float biomToProd(float biomass) {
        return biomass * prodBiomFactor / (float) getOsmose().getNumberTimeStepsPerYear();
    }

    /*
     * Clear matrices for next time step
     */
    public void clearPlankton() {

        biomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        accessibleBiomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        iniBiomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
    }

    public float calculPercent(float CritMin, float CritMax) // used during the predation process : compute the percentage of plankton size range available to a predator (according to its size)
    {
        float tempPercent;
        tempPercent = (Math.min(sizeMax, CritMax) - Math.max(sizeMin, CritMin)) / (sizeMax - sizeMin);
        return tempPercent;
    }

    /**
     * @return the sizeMax
     */
    public float getSizeMax() {
        return sizeMax;
    }

    /**
     * @return the sizeMin
     */
    public float getSizeMin() {
        return sizeMin;
    }

    /**
     * @return the accessibilityCoeff
     */
    public float getAccessibilityCoeff() {
        return accessibilityCoeff;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    private static Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private static IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }
}
