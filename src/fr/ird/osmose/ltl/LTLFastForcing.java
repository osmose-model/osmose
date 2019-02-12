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
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;

/**
 * This class loads in memory all LTL data from a NetCDF file at the beginning
 * of the simulation, conversely to other LTL plug-ins that load the data at
 * each time step. It was coded in order to speed up configurations that loop
 * over the same year of LTL data during all the simulation. The class reads the
 * data from a NetCDF file with the following characteristics: (i) the LTL grid
 * is the same than the Osmose grid ; (ii) the biomass is provided in a variable
 * called ltl_biomass(time, ltl, ny, nx), with time the number of time steps
 * (this value must be a multiple of the number of time step per year in Osmose.
 * It means the user can provide either one year, 5 years or 50 years of LTL
 * data and Osmose will loop over it until the end of the simulation), ltl the
 * number of plankton groups ordered the same way as in the configuration (e.g.
 * plankton.name.plk0, plankton.name.plk1, etc.), ny the number of lines, nx the
 * number of columns. The biomass is provided in tonnes. The NetCDF file is
 * designated by parameter 'ltl.netcdf.file'.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 */
public class LTLFastForcing extends AbstractLTLForcing {

    /**
     * The LTL biomass [TIME][PLANKTON][NY][NX]
     */
    private double[][][][] biomass;

    public LTLFastForcing(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        //String ncFile = getConfiguration().getFile("ltl.netcdf.file");
        //if (!new File(ncFile).exists()) {
        //    error("Error reading LTLForcing parameters.", new FileNotFoundException("LTL NetCDF file " + ncFile + " does not exist."));
        //}
        // Read number of LTL steps
        int nLTLStep = getConfiguration().getInt("ltl.nstep");
        int nPlk = getConfiguration().getNPlankton();

        // Initialises biomass variable
        biomass = new double[nLTLStep][nPlk][getGrid().get_ny()][getGrid().get_nx()];

        // Read LTL data from NetCDF
        loadData();

        // Uniform biomass. Check AbstractLTLForcing Javadoc for details.
        // In LTLFastForcing the constant LTL groups must be defined last, after
        // the other LTL groups whose biomass is provided in the NetCDF file.
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {
            if (!getConfiguration().isNull("plankton.biomass.total.plk" + iPlk)) {
                double uBiomass = getConfiguration().getDouble("plankton.biomass.total.plk" + iPlk) / getGrid().getNOceanCell();
                for (int iTime = 0; iTime < nLTLStep; iTime++) {
                    for (Cell cell : getGrid().getCells()) {
                        if (!cell.isLand()) {
                            biomass[iTime][iPlk][cell.get_jgrid()][cell.get_igrid()] = uBiomass;
                        }
                    }
                }
            }
        }

        // Biomass multiplier. Check Javadoc of AbstractLTLForcing for details.
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {
            if (!getConfiguration().isNull("plankton.multiplier.plk" + iPlk)) {
                double multiplier = getConfiguration().getFloat("plankton.multiplier.plk" + iPlk);
                if (multiplier != 1.) {
                    for (int iTime = 0; iTime < nLTLStep; iTime++) {
                        for (Cell cell : getGrid().getCells()) {
                            if (!cell.isLand()) {
                                biomass[iTime][iPlk][cell.get_jgrid()][cell.get_igrid()] *= multiplier;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Read the LTL biomass for every LTL groups from a NetCDF file. Refer to
     * the class Javadoc for details about the NetCDF file format.
     *
     * @param ncFile, the path of the NetCDF file
     */
    private void loadData() {
        info("Loading plankton data...");
        //debug("Forcing file {0}", ncFile);
        //NetcdfFile nc = NetcdfFile.open(ncFile);
        // We do not load directly ltlbiomass[][][][] variable in
        // biomass[][][][] variable because there might be more LTL groups
        // defined in the Osmose configuration with uniform biomass
        // (ltl.biomass.total.plk#).
        //Array ltlbiomass = nc.findVariable("ltl_biomass").read();
        // nLTL the number of LTL groups in the NetCDF file <= nLTL groups
        // of the Osmose configuration
        //int nPlk = ltlbiomass.getShape()[1];
        // NetCDF file may contain more time steps than number of time steps
        // to be considered, as defined by 'ltl.nstep'
        int nTime = biomass.length;
        int nPlk = getConfiguration().getNPlankton();

        //Index index = ltlbiomass.getIndex();
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {

            if (getConfiguration().isNull("plankton.file.plk" + iPlk)) {
                if (!getConfiguration().canFind("plankton.biomass.total.plk" + iPlk)) {
                    error("No input file is provided for plankton " + getConfiguration().getString("plankton.name.plk" + iPlk), new Exception("Cannot initialize LTL class " + iPlk));
                }
                continue;
            }

            String name = getConfiguration().getString("plankton.name.plk" + iPlk);
            String ncFile = getConfiguration().getFile("plankton.file.plk" + iPlk);

            if (!new File(ncFile).exists()) {
                error("Error reading LTLForcing parameters.", new FileNotFoundException("LTL NetCDF file " + ncFile + " does not exist."));
            }

            try {
                NetcdfFile nc = NetcdfFile.open(ncFile);
                Array ltlbiomass = nc.findVariable(name).read();
                Index index = ltlbiomass.getIndex();

                for (int iTime = 0; iTime < nTime; iTime++) {
                    for (Cell cell : getGrid().getCells()) {
                        if (!cell.isLand()) {
                            int i = cell.get_igrid();
                            int j = cell.get_jgrid();
                            index.set(iTime, j, i);
                            biomass[iTime][iPlk][j][i] = ltlbiomass.getDouble(index);
                        }
                    }
                }

                nc.close();

            } catch (IOException ex) {
                error("File " + ncFile + " and variable " + name + "cannot be read", new IOException());

            }
        }
    }

    @Override
    public double getBiomass(int iPlk, Cell cell) {
        int ltlTimeStep = getSimulation().getIndexTimeSimu() % biomass.length;
        return biomass[ltlTimeStep][iPlk][cell.get_jgrid()][cell.get_igrid()];
    }

    /**
     * In the case of LTLFastForcing, and conversely to other classes extending
     * AbstractLTLForcing, there is no LTL data to update as it is already
     * loaded in memory since the beginning of the simulation.
     *
     * @param iStepSimu, the current time step of the simulation
     */
    @Override
    public void update(int iStepSimu) {
        // Do nothing
    }

    @Override
    float[][][] getRawBiomass(int iPlk, int iStepSimu) {
        throw new UnsupportedOperationException("LTLFastForcing assumes that LTL biomass is already provided in tonne in each cell.");
    }

    @Override
    void readParameters() {
        // Do nothing. Done directly in init() function for LTLFastForcing
    }

    @Override
    void initLTL() {
        throw new UnsupportedOperationException("LTLFastForcing assumes that LTL data is provided on Osmose grid.");
    }

    @Override
    float[] getDepthLevel(int iLTL, int jLTL) {
        throw new UnsupportedOperationException("LTLFastForcing assumes that LTL data is provided on 2D grid.");
    }

    @Override
    List<Point> getLTLCells(Cell cell) {
        throw new UnsupportedOperationException("LTLFastForcing assumes that LTL data is provided on Osmose grid.");
    }
}
