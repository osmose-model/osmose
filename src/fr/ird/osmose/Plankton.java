package fr.ird.osmose;

import fr.ird.osmose.Simulation.Version;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;

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

    // constants
    final private int index;
    final private float trophicLevel;        // trophic level of the plankton group
    final private float sizeMin, sizeMax;
    final private String name;     // e.g.   phytoplankton, diatoms, copepods...
    final private float accessibilityCoeff; // percentage of plankton biomass available for fish
    final private float conversionFactor;   // factor to be used in order to transform biomass from plankton unit (eg mmolN/m2) towards wet weight (tons/km2)
    final private float prodBiomFactor;   // factor to be used to transform biomass into production (per year)
    // state variable
    private float[][] biomass;         // table of transformed values in 2D

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

    public void update(int iStepSimu) {
        biomass = getForcing().computeBiomass(this, iStepSimu);
    }

    public float getBiomass(Cell cell) {
        return biomass[cell.get_jgrid()][cell.get_igrid()];
    }

    public float getAccessibleBiomass(Cell cell) {
        return accessibilityCoeff * getBiomass(cell);
    }

    /**
     * Computes the biomass of the specified plankton over the domain.
     *
     * @return the cumulated biomass over the domain in tons
     */
    public double getBiomass() {
        double biomTot = 0.d;
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                biomTot += getBiomass(cell);
            }
        }
        return biomTot;
    }

    /*
     * Converts plankton biomass (usually from mmolN/m2) to tons/km2
     */
    public float convertToTonPerKm2(float concentration) {
        
        if (Simulation.VERSION == Version.SCHOOL2012_PROD) {
            return biomToProd(concentration * conversionFactor);
        } else {
            return concentration * conversionFactor;
        }
    }

    public float biomToProd(float biomass) {
        return biomass * prodBiomFactor / (float) getConfiguration().getNumberTimeStepsPerYear();
    }

    /*
     * Init biomass matrix
     */
    public void init() {
        //biomass = new float[getGrid().get_ny()][getGrid().get_nx()];
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
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the trophicLevel
     */
    public float getTrophicLevel() {
        return trophicLevel;
    }

    private static Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    private static IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    private static LTLForcing getForcing() {
        return Osmose.getInstance().getForcing();
    }
}
