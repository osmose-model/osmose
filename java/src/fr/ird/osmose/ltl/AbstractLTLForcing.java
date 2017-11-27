/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2015
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
import java.awt.Point;
import java.util.List;

/**
 * This abstract class indicates how Osmose manages and retrieves the biomass of
 * the low trophic levels compartments of the ecosystem. The LTL groups are an
 * input of the model, they are forcing the model biomass as they provide food
 * to the higher trophic levels.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public abstract class AbstractLTLForcing extends SimulationLinker implements LTLForcing {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Factor for converting biomass from plankton unit to wet weight in
     * tonne/m3. (e.g. mmolN/m3 to tonne/m3)
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
    /**
     * The constant biomass, in tonne, in a cell of the model. Parameter
     * 'plankton.biomass.total.plk#' provides the total biomass of a given
     * plankton group in the system for every time step. This feature allows to
     * consider a plankton group with a constant biomass uniformly distributed
     * over the grid of the model and over time. This feature has been added as
     * a quick patch for a configuration that seems to lack a food compartment
     * and as a result cannot reach any biomass equilibrium. It provides to the
     * system a constant pool of biomass throughout time. It should only be used
     * for "debugging" a configuration.
     */
    private double[] uBiomass;

    /**
     * Multiplier of the plankton biomass. Parameter 'plankton.multiplier.plk#'
     * for virtually increasing or decreasing plankton biomass.
     */
    private double[] multiplier;

//////////////
// Constructor
//////////////
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
     * Get the 3D biomass of the specified plankton group on the LTL grid for a
     * given time step. The biomass is expressed in the same unit as it is in
     * the forcing file. Dimensions must be ordered as [jLTL][iLTL][zLTL]
     *
     * @param iPlankton, the index of a plankton group
     * @param iStepSimu, the current step of the simulation
     * @return an array of same dimension and unit than the LTL grid
     */
    abstract float[][][] getRawBiomass(int iPlankton, int iStepSimu);

    /**
     * List the LTL cells that overlap a given Osmose cell. The LTL cells are
     * returned as a list of Point(x, y) with x, y integer coordinates of the
     * cell on the LTL grid. These are the LTL cells that are taken into account
     * for interpolating the LTL biomass on an Osmose cell. The list may or may
     * not be empty for land cells. The list must only return LTL ocean cells
     * (so that land LTL cells are not included in the spatial integration).
     */
    abstract List<Point> getLTLCells(Cell cell);

    /**
     * Read LTL parameters in the Osmose configuration file.
     */
    abstract void readParameters();

    /**
     * This function must be used for initialising the LTL plug-ins inheriting
     * from this abstract class. It should be the place, for instance, for
     * linking the LTL grid cells to Osmose grid cells (that are be returned by
     * the getLTLCells function), and for (pre)loading any stuff or NetCDF
     * variables that you deem useful for making getRawBiomass() as efficient as
     * possible.
     */
    abstract void initLTL();

    /**
     *
     * Get the depths of the vertical levels at a given location on the LTL
     * grid. depth[zLTL]
     *
     * @param iLTL the i-coordinate of the cell in the LTL grid
     * @param jLTL the j-coordinate of the cell in the LTL grid
     * @return an array that contains the depth, in metre, of the vertical
     * levels at LTL cell(iLTL, jLTL)
     */
    abstract float[] getDepthLevel(int iLTL, int jLTL);

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
                warning("Parameter plankton.conversion2tons.plk{0} not found (or set to null). Osmose assumes that LTL data for plankton group {1} is already expressed in tonne/m2 (or tonne/m3 for 3D dataset)", new Object[]{iPlk, getConfiguration().getPlankton(iPlk).getName()});
                conversionFactor[iPlk] = 1.d;
            }
        }

        // Read LTL parameters
        readParameters();

        // Initializes LTL
        initLTL();

        // Plankton multplier
        multiplier = new double[nPlk];
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {
            if (!getConfiguration().isNull("plankton.multiplier.plk" + iPlk)) {
                multiplier[iPlk] = getConfiguration().getFloat("plankton.multiplier.plk" + iPlk);
                warning("Plankton biomass for plankton group " + iPlk + " will be multiplied by " + multiplier[iPlk] + " accordingly to parameter " + getConfiguration().printParameter("plankton.multiplier.plk" + iPlk));
            } else {
                multiplier[iPlk] = 1.d;
            }
        }

        // Uniform biomass
        uBiomass = new double[nPlk];
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {
            if (!getConfiguration().isNull("plankton.biomass.total.plk" + iPlk)) {
                uBiomass[iPlk] = getConfiguration().getDouble("plankton.biomass.total.plk" + iPlk) / getGrid().getNOceanCell();
            } else {
                uBiomass[iPlk] = -1.d;
            }
        }
    }

    /**
     * Updates the biomass of the LTL groups at the current time step of the
     * simulation. Get the raw biomass of the LTL groups on their original grid
     * and with their original unit and interpolates and integrates it on the
     * Osmose grid in order to get tonnes of wet weight.
     *
     * @param iStepSimu, the current step of the simulation
     */
    @Override
    public void update(int iStepSimu) {

        // Reset biomass matrix
        biomass = new double[getConfiguration().getNPlankton()][getGrid().get_ny()][getGrid().get_nx()];

        for (int iPlk = 0; iPlk < getConfiguration().getNPlankton(); iPlk++) {
            biomass[iPlk] = new double[getGrid().get_ny()][getGrid().get_nx()];
            // Check whether the biomass is read from NetCDF file or uniform
            if (uBiomass[iPlk] < 0) {
                // From NetCDF, rawBiomass[jLTL][iLTL][kLTL]
                float[][][] rawBiomass = getRawBiomass(iPlk, iStepSimu);
                float maxDepth = getConfiguration().getFloat("ltl.integration.depth");
                for (Cell cell : getGrid().getCells()) {
                    if (!cell.isLand()) {
                        double area = 1.111e5d * getGrid().getdLat() * 1.111e5d * Math.cos(cell.getLat() * Math.PI / (90.d * 2.d)) * getGrid().getdLong();
                        int i = cell.get_igrid();
                        int j = cell.get_jgrid();
                        List<Point> ltlCells = getLTLCells(cell);
                        int nCells = ltlCells.size();
                        for (Point ltlCell : ltlCells) {
                            double bm = zIntegration(rawBiomass[ltlCell.y][ltlCell.x], getDepthLevel(ltlCell.x, ltlCell.y), maxDepth);
                            biomass[iPlk][j][i] += area * convertToTonneWWPerM3(iPlk, bm) / nCells;
                        }
                    }
                }
            } else {
                // Uniform plankton biomass
                for (Cell cell : getGrid().getCells()) {
                    if (!cell.isLand()) {
                        biomass[iPlk][cell.get_jgrid()][cell.get_igrid()] = uBiomass[iPlk];
                    }
                }
            }
        }
    }

    @Override
    public double getBiomass(int iPlk, Cell cell) {
        return multiplier[iPlk] * biomass[iPlk][cell.get_jgrid()][cell.get_igrid()];
    }

    /**
     * Converts plankton biomass (usually from mmolN/m3) to tonne of wet weight
     * per cubic metre (tonneWW/m3).
     *
     * @param iPlk, the index of the plankton group
     * @param concentration of the plankton biomass in the same unit as in the
     * LTL files
     * @return concentration of plankton biomass in tonneWW/m3
     */
    private double convertToTonneWWPerM3(int iPlk, double concentration) {
        return concentration * conversionFactor[iPlk];
    }

    /**
     * Vertical integration of LTL data on the water column, up to a given
     * depth.
     *
     * @param zdata, an array of the LTL data in the water column
     * @param zlevel, an array of the depth levels (metre) in the water column
     * @param maxDepth, the maximum depth (metre) to be taken into account for
     * the vertical integration. Make sure the sign of the depth is consistent
     * between the depth of the LTL grid and the maximum depth.
     * @return the raw LTL data vertically integrated
     */
    private double zIntegration(float[] zdata, float[] zlevel, float maxDepth) {

        int nz = zdata.length;
        double integratedData = 0.d;
        for (int k = 0; k < nz - 1; k++) {
            if (zlevel[k] > maxDepth) {
                if (zdata[k] >= 0 && zdata[k + 1] >= 0) {
                    integratedData += (Math.abs(zlevel[k] - zlevel[k + 1])) * ((zdata[k] + zdata[k + 1]) / 2.d);
                }
            }
        }
        return integratedData;
    }
}
