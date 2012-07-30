package fr.ird.osmose;

/********************************************************************************
 * <p>Titre : Plankton class </p>
 *
 * <p>Description : Basic unit of the plankton part of the model - represents a plankton group with size and maps specified </p>
 *
 * <p>Copyright : Copyright (c) may 2009 </p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 ******************************************************************************** 
 */
public class Plankton {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/07 phv
     * Osmose and Simulation are called with Osmose.getInstance()
     * and Osmose.getInstance().getSimulation()
     */
    float trophicLevel;        // trophic level of the plankton group
    private float sizeMin, sizeMax;
    private String name;     // e.g.   phytoplankton, diatoms, copepods...
    private float accessibilityCoeff; // percentage of plankton biomass available for fish
    private float conversionFactor;   // factor to be used in order to transform biomass from plankton unit (eg mmolN/m2) towards wet weight (tons/km2)
    private float prodBiomFactor;   // factor to be used to transform biomass into production (per year)
    float[][][] dataInit;      // table of intial values in 3D -> Dimensions are case-specific
    float[][] biomass, accessibleBiomass, iniBiomass, integratedData;         // table of transformed values in 2D
    float[][] mortalityRate;     // table for output values in 2D

    public Plankton(String name, float sizeMin, float sizeMax, float trophicLevel, float conversionFactor, float prodBiomFactor, float accessCoeff) {
        this.name = name;
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;
        this.trophicLevel = trophicLevel;
        this.conversionFactor = conversionFactor;
        this.prodBiomFactor = prodBiomFactor;
        this.accessibilityCoeff = accessCoeff;

        LTLForcing forcing = Osmose.getInstance().getSimulation().getForcing();

        // Initialization matrix corresponding to plankton grid, eg ROMS curvilinear grid
        dataInit = new float[forcing.getPlanktonDimX()][forcing.getPlanktonDimY()][forcing.getPlanktonDimZ()];
        integratedData = new float[forcing.getPlanktonDimX()][forcing.getPlanktonDimY()];

        // Initialization matrix corresponding to osmose grid
        mortalityRate = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        biomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        accessibleBiomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        iniBiomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                mortalityRate[i][j] = (accessibilityCoeff / 2f) * getSimulation().getNbTimeStepsPerYear() / 365f; // default = median of the max mortality rate (in day-1)
            }
        }
    }

    private Simulation getSimulation() {
        return Osmose.getInstance().getSimulation();
    }

    public void verticalIntegration(float[][][] depthLayer, float maxDepth) // transforms dataInit into integratedData using the matrix depthLayer specifying the depth of each cell of the plankton grid
    // vertical integration is realized from 0 to maxDepth (eg -100) meters
    {
        float integr;
        for (int i = 0; i < depthLayer.length; i++) {
            for (int j = 0; j < depthLayer[i].length; j++) {
                integr = 0f;
                for (int k = 0; k < depthLayer[i][j].length - 1; k++) {
                    if (depthLayer[i][j][k] > maxDepth) {
                        if (dataInit[i][j][k] >= 0 && dataInit[i][j][k + 1] >= 0) {
                            integr += (Math.abs(depthLayer[i][j][k] - depthLayer[i][j][k + 1])) * ((dataInit[i][j][k] + dataInit[i][j][k + 1]) / 2f);
                        }
                    }
                }
                integratedData[i][j] = integr;
            }
        }
    }

    public void addCell(int i, int j, int x, int y, int nb) // i and j are for Osmose grid, x and y are for LTL grid
    // called during the spatial interpolation
    {
        float area;	// area of osmose cell in km^2
        area = 111f * getGrid().getdLat() * 111f * (float) Math.cos(getGrid().getCell(i, j).getLat() * Math.PI / (90f * 2f)) * getGrid().getdLong();

        biomass[i][j] += area * unitConversion(integratedData[x][y] / (float) nb);
        iniBiomass[i][j] += area * unitConversion(integratedData[x][y] / (float) nb);
        accessibleBiomass[i][j] += area * unitConversion(getAccessibilityCoeff() * integratedData[x][y] / (float) nb);
    }

    /*
     * Converts plankton biomass (usually from mmolN/m2) to tons/km2
     */
    public float unitConversion(float concentration) {
        return concentration * conversionFactor;
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

    private IGrid getGrid() {
        return Osmose.getInstance().getGrid();
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
}
