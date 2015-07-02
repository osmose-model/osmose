/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.SimulationLinker;
import java.util.List;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public abstract class AbstractLTLForcing extends SimulationLinker implements LTLForcing {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * List of the i-coordinates of the LTL cells that overlap an Osmose cell.
     * These are the LTL cells that are taken into account for interpolating the
     * LTL biomass on an Osmose cell. It is an array of the size of the Osmose
     * grid so that it provides of list of i-coordinates for every (ocean) cell
     * of the Osmose grid.
     *
     */
    List<Integer>[][] icoordLTLGrid;
    /**
     * List of the j-coordinates of the LTL cells that overlap an Osmose cell.
     * These are the LTL cells that are taken into account for interpolating the
     * LTL biomass on an Osmose cell. It is an array of the size of the Osmose
     * grid so that it provides of list of j-coordinates for every (ocean) cell
     * of the Osmose grid.
     *
     */
    List<Integer>[][] jcoordLTLGrid;
    /**
     * Factor for converting biomass from plankton unit to wet weight in
     * tonne/km2. (e.g. mmolN/m2 to tonne/km2)
     */
    double[] conversionFactor;
    /**
     * Number of time step in the LTL time series inputed to Osmose. This value
     * must be a multiple of the number of time step per year in Osmose. It
     * means the user can provide either one year, 5 years or 50 years of LTL
     * data and Osmose will loop over it (if necessary) until the end of the
     * simulation.
     */
    private int nLTLStep;
    /**
     * Array of float that stores the biomass, in tonne, of every LTL groups
     * interpolated on the Osmose grid for the current time step of the
     * simulation. {@code biomass[n_LTL_groups][nline_osmose][ncolumn_osmose]}.
     * The array is updated every time step in the {@link #update(int)}
     * function.
     */
    private double[][][] biomass;

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Creates a new LTLForcing associated to a specified simulation.
     *
     * @param rank, the rank of the simulation
     */
    AbstractLTLForcing(int rank) {
        super(rank);
    }

/////////////////////////////////////
// Definition of the abstract methods
/////////////////////////////////////
    /**
     * Get the biomass of the specified plankton group on the LTL grid and
     * vertically integrated, for a given time step. The biomass is expressed in
     * the same unit as it is in the forcing file except that it is vertically
     * integrated (concentration of plankton per surface unit).
     *
     * @param iPlankton, the index of a plankton group
     * @param iStepSimu, the current step of the simulation
     * @return an array of dimension of the LTL grid with biomass vertically
     * integrated.
     */
    abstract double[][] getRawBiomass(int iPlankton, int iStepSimu);

    /**
     * Read LTL parameters in the Osmose configuration file.
     */
    abstract void readParameters();

    /**
     * This function loads the LTL grid. It loads longitude, latitude and depth
     * of vertical levels. It creates the index map between osmose cells and LTL
     * grid cells (fills up variables icoordLTLGrid & jcoordLTLGrid)
     */
    abstract void initLTLGrid();

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Converts the current time step of the simulation into the corresponding
     * time step of the LTL data.
     *
     * @param iStepSimu, the current step of the simulation.
     * @return the corresponding time step of the LTL data.
     */
    public int getIndexStepLTL(int iStepSimu) {
        return iStepSimu % nLTLStep;
    }

    @Override
    public void init() {

        // Read number of LTL steps
        nLTLStep = getConfiguration().getInt("ltl.nstep");

        // Read conversion factors
        int nPlk = getConfiguration().getNPlankton();
        conversionFactor = new double[nPlk];
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {
            if (!getConfiguration().isNull("plankton.conversion2tons.plk" + iPlk)) {
                conversionFactor[iPlk] = getConfiguration().getDouble("plankton.conversion2tons.plk" + iPlk);
            } else {
                warning("Paramter plankton.conversion2tons.plk{0} not found (or set to null). Osmose assumes that LTL data for plankton group {1} is already expressed in tonne/km2 (or tonne/km3 for 3D dataset)", new Object[]{iPlk, getSimulation().getPlankton(iPlk).getName()});
                conversionFactor[iPlk] = 1.d;
            }
        }

        // Read LTL parameters
        readParameters();

        // Initializes LTL grid
        initLTLGrid();

        // Initializes biomass matrix
        biomass = new double[nPlk][getGrid().get_ny()][getGrid().get_nx()];
    }

    /**
     *
     * @param data3d, the raw LTL data with a vertical dimension
     * {@code data3d[nz][ny][nx]}
     * @param depthLayer, an array of float that provides the depth of every
     * cell of the LTL grid. {@code depthLayer[nz][ny][nx]}
     * @param maxDepth, the maximum depth to be taken into account for the
     * vertical integration. Make sure the sign of the depth is consistent
     * between the depth of the LTL grid and the maximum depth.
     * @return the raw LTL data vertically integrated, in concentration of
     * plankton per surface unit.
     */
    public double[][] verticalIntegration(float[][][] data3d, float[][][] depthLayer, float maxDepth) {
        int nx = data3d[0][0].length;
        int ny = data3d[0].length;
        int nz = data3d.length;
        double[][] integratedData = new double[ny][nx];
        double integr;
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                integr = 0.d;
                for (int k = 0; k < nz - 1; k++) {
                    if (depthLayer[k][j][i] > maxDepth) {
                        if (data3d[k][j][i] >= 0 && data3d[k + 1][j][i] >= 0) {
                            integr += (Math.abs(depthLayer[k][j][i] - depthLayer[k + 1][j][i])) * ((data3d[k][j][i] + data3d[k + 1][j][i]) / 2.d);
                        }
                    }
                }
                integratedData[j][i] = integr;
            }
        }
        return integratedData;
    }

    /**
     * Updates the biomass of the LTL groups at the current time step of the
     * simulation. Get the "raw" biomass of the LTL groups on their original
     * grid and with their original unit and interpolates it on the Osmose grid
     * and converts it to tonne/km2.
     *
     * @param iStepSimu, the current step of the simulation
     */
    @Override
    public void update(int iStepSimu) {
        
        // Reset biomass matrix
        biomass = new double[getConfiguration().getNPlankton()][getGrid().get_ny()][getGrid().get_nx()];

        for (int iPlk = 0; iPlk < getConfiguration().getNPlankton(); iPlk++) {
            biomass[iPlk] = new double[getGrid().get_ny()][getGrid().get_nx()];
            double[][] rawBiomass = getRawBiomass(iPlk, iStepSimu);
            for (Cell cell : getGrid().getCells()) {
                if (!cell.isLand()) {
                    double area = 111.d * getGrid().getdLat() * 111.d * Math.cos(cell.getLat() * Math.PI / (90.d * 2.d)) * getGrid().getdLong();
                    int i = cell.get_igrid();
                    int j = cell.get_jgrid();
                    int nCells = icoordLTLGrid[j][i].size();
                    for (int k = 0; k < nCells; k++) {
                        biomass[iPlk][j][i] += area * convertToTonnePerKm2(iPlk, rawBiomass[jcoordLTLGrid[j][i].get(k)][icoordLTLGrid[j][i].get(k)]) / (double) nCells;
                    }
                }
            }
        }
    }

    @Override
    public double getBiomass(int iPlankton, Cell cell) {
        return biomass[iPlankton][cell.get_jgrid()][cell.get_igrid()];
    }

    /**
     * Converts plankton biomass (usually from mmolN/m2) to tonne/km2
     *
     * @param iPlankton, the index of the plankton group
     * @param concentration of the plankton biomass in the same unit as in the
     * LTL files
     * @return concentration of plankton biomass in tonne/km2
     */
    private double convertToTonnePerKm2(int iPlankton, double concentration) {
        return concentration * conversionFactor[iPlankton];
    }
}
