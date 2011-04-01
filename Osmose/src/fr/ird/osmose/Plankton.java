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

    Coupling coupling;
    float trophicLevel;        // trophic level of the plankton group
    float sizeMin, sizeMax;      // min and max size of the group (uniform distribution)
    String name;     // e.g.   phytoplankton, diatoms, copepods...
    float accessibilityCoeff; // percentage of plankton biomass available for fish
    float conversionFactor;   // factor to be used in order to transform biomass from plankton unit (eg mmolN/m2) towards wet weight (tons/km2)
    float prodBiomFactor;   // factor to be used to transform biomass into production (per year)
    float[][][] dataInit;      // table of intial values in 3D -> Dimensions are case-specific
    float[][] biomass, accessibleBiomass, iniBiomass, integratedData;         // table of transformed values in 2D
    float[][] mortalityRate;     // table for output values in 2D

    public Plankton(Coupling coupling, String name, float sizeMin, float sizeMax, float trophicLevel, float conversionFactor, float prodBiomFactor, float accessCoeff) {
        this.name = name;
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;
        this.trophicLevel = trophicLevel;
        this.conversionFactor = conversionFactor;
        this.prodBiomFactor = prodBiomFactor;
        this.coupling = coupling;
        this.accessibilityCoeff = accessCoeff;

        // Initialization matrix corresponding to plankton grid, eg ROMS curvilinear grid
        dataInit = new float[coupling.planktonDimX][][];
        integratedData = new float[coupling.planktonDimX][];

        for (int i = 0; i < coupling.planktonDimX; i++) {
            integratedData[i] = new float[coupling.planktonDimY];
            dataInit[i] = new float[coupling.planktonDimY][];
            for (int j = 0; j < coupling.planktonDimY; j++) {
                dataInit[i][j] = new float[coupling.planktonDimZ];
            }
        }

        // Initialization matrix corresponding to osmose grid
        mortalityRate = new float[coupling.grid.nbLines][];
        biomass = new float[coupling.grid.nbLines][];
        accessibleBiomass = new float[coupling.grid.nbLines][];
        iniBiomass = new float[coupling.grid.nbLines][];
        for (int i = 0; i < coupling.grid.nbLines; i++) {
            biomass[i] = new float[coupling.grid.nbColumns];
            accessibleBiomass[i] = new float[coupling.grid.nbColumns];
            iniBiomass[i] = new float[coupling.grid.nbColumns];
            mortalityRate[i] = new float[coupling.grid.nbColumns];
            for (int j = 0; j < coupling.grid.nbColumns; j++) {
                biomass[i][j] = 0;
                accessibleBiomass[i][j] = 0;
                iniBiomass[i][j] = 0;
                mortalityRate[i][j] = (accessibilityCoeff / 2f) * coupling.simulation.nbDt / 365f; // default = median of the max mortality rate (in day-1)
            }
        }
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
                        integr += (Math.abs(depthLayer[i][j][k] - depthLayer[i][j][k + 1])) * ((dataInit[i][j][k] + dataInit[i][j][k + 1]) / 2f);
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
        area = 111f * coupling.grid.dLat * 111f * (float) Math.cos(coupling.grid.matrix[i][j].getLat() * Math.PI / (90f * 2f)) * coupling.grid.dLong;

        biomass[i][j] += area * unitConversion(integratedData[x][y] / (float) nb);
        iniBiomass[i][j] += area * unitConversion(integratedData[x][y] / (float) nb);
        accessibleBiomass[i][j] += area * unitConversion(accessibilityCoeff * integratedData[x][y] / (float) nb);
    }

    public float unitConversion(float concentration) {
        float biom = 0.f;
        biom = biomToProd(concentration * conversionFactor);     //   (from mmolN/m2 to tons/km2)
        return biom;
    }

    public float biomToProd(float biomass) {
        float prod = 0f;
        prod = biomass * prodBiomFactor / (float) coupling.simulation.nbDt;
        return prod;
    }

    public void clearPlankton() // clear the matrix for the next time step
    {
        for (int i = 0; i < coupling.grid.nbLines; i++) {
            for (int j = 0; j < coupling.grid.nbColumns; j++) {
                biomass[i][j] = 0;
                accessibleBiomass[i][j] = 0;
                iniBiomass[i][j] = 0;
            }
        }
    }

    public float calculPercent(float CritMin, float CritMax) // used during the predation process : compute the percentage of plankton size range available to a predator (according to its size)
    {
        float tempPercent;
        tempPercent = (Math.min(sizeMax, CritMax) - Math.max(sizeMin, CritMin)) / (sizeMax - sizeMin);
        return tempPercent;
    }
}
