/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * This class indicates how Osmose manages and retrieves the biomass of the
 * resource compartments of the ecosystem. The resource groups are an input of
 * the model, they are forcing the model biomass as they provide food to the
 * focal species. The class reads the data from a NetCDF file with the following
 * characteristics: (i) the NetCDF grid is the same than the Osmose grid ; (ii)
 * the biomass is provided in variables of the same name than the resource group
 * (time, ny, nx), with time the number of time steps (this value must be a
 * multiple of the number of time step per year in Osmose, and ny, nx the grid
 * dimension. It means the user can provide either one year, 5 years or 50 years
 * of LTL data and Osmose will loop over it until the end of the simulation).
 * The biomass is provided in tonnes.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 4.2 2019/11/25
 */
public class AbstractLTLForcing extends SimulationLinker implements LTLForcing {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Array of float that stores the biomass, in tonne, of every RSC groups on
     * the Osmose grid for the current time step of the simulation.
     * {@code biomass[n_rsc_groups][nline_osmose][ncolumn_osmose]}. The array is
     * updated every time step in the {@link #update(int)} function.
     */
    private double[][][] biomass;
    /**
     * The constant biomass, in tonne, in a cell of the model. Parameter
     * 'resource.biomass.total.rsc#' provides the total biomass of a given
     * resource group in the system for every time step. This feature allows to
     * consider a resource group with a constant biomass uniformly distributed
     * over the grid of the model and over time. This feature has been added as
     * a quick patch for a configuration that seems to lack a food compartment
     * and as a result cannot reach any biomass equilibrium. It provides to the
     * system a constant pool of biomass throughout time. It should only be used
     * for "debugging" a configuration.
     */
    private double[] uBiomass;

    /**
     * Multiplier of the resource biomass. Parameter 'resource.multiplier.rsc#'
     * for virtually increasing or decreasing resource biomass.
     */
    private double[] multiplier;

    /**
     *
     */
    private int[] timeLength;

//////////////
// Constructor
//////////////
    /**
     * Creates a new ResourceForcing associated to a specified simulation.
     *
     * @param rank, the rank of the simulation
     */
    public AbstractLTLForcing(int rank) {
        super(rank);
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    @Override
    public void init() {

        // Read conversion factors
        int nRsc = getConfiguration().getNRscSpecies();

        // Uniform biomass
        uBiomass = new double[nRsc];
        // Resource multplier
        multiplier = new double[nRsc];
        // Time length
        timeLength = new int[nRsc];

        for (int iRsc = 0; iRsc < nRsc; iRsc++) {
            if (!getConfiguration().isNull("resource.biomass.total.rsc" + iRsc)) {
                uBiomass[iRsc] = getConfiguration().getDouble("resource.biomass.total.rsc" + iRsc) / getGrid().getNOceanCell();
                timeLength[iRsc] = getConfiguration().getNStepYear();
            } else if (!getConfiguration().isNull("resource.file.rsc" + iRsc)) {
                // set negative value to uniform biomass
                uBiomass[iRsc] = -1.d;

                // check resource is properly defined in the NetCDF file
                String name = getConfiguration().getString("resource.name.rsc" + iRsc);
                String ncFile = getConfiguration().getFile("resource.file.rsc" + iRsc);

                if (!new File(ncFile).exists()) {
                    error("Error reading LTLForcing parameters.", new FileNotFoundException("LTL NetCDF file " + ncFile + " does not exist."));
                }

                try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
                    Variable variable = nc.findVariable(name);
                    int[] shape = variable.getShape();
                    // check time length
                    if ((shape[0] < getConfiguration().getNStep()) && (shape[0] % getConfiguration().getNStepYear() != 0)) {
                        throw new IOException("Time dimension of the NetCDF resource group " + iRsc + " must be a multiple of the number of time steps per year");
                    }
                    timeLength[iRsc] = shape[0];
                    // check grid dimension
                    if (getGrid().get_ny() != shape[1] | getGrid().get_nx() != shape[2]) {
                        throw new IOException("NetCDF grid dimensions of resource group " + iRsc + " does not match Osmose grid dimensions");
                    }
                } catch (IOException ex) {
                    error("File " + ncFile + ", variable " + name + "cannot be read", ex);
                }
            } else {
                error("No input file is provided for resource " + getConfiguration().getString("resource.name.rsc" + iRsc), new IOException("Cannot initialize resource group " + iRsc));
            }

            if (!getConfiguration().isNull("resource.multiplier.rsc" + iRsc)) {
                multiplier[iRsc] = getConfiguration().getFloat("resource.multiplier.rsc" + iRsc);
                warning("Resource biomass for resource group " + iRsc + " will be multiplied by " + multiplier[iRsc] + " accordingly to parameter " + getConfiguration().printParameter("resource.multiplier.rsc" + iRsc));
            } else {
                multiplier[iRsc] = 1.d;
            }
        }
    }

    /**
     * Updates the biomass of the resource groups at the current time step of
     * the simulation.
     *
     * @param iStepSimu, the current step of the simulation
     */
    @Override
    public void update(int iStepSimu) {

        // Reset biomass matrix
        biomass = new double[getConfiguration().getNRscSpecies()][][];

        for (int iRsc = 0; iRsc < getConfiguration().getNRscSpecies(); iRsc++) {

            // Check whether the biomass is read from NetCDF file or uniform
            biomass[iRsc] = (uBiomass[iRsc] < 0)
                    ? readBiomass(iRsc, iStepSimu)
                    : fillBiomass(uBiomass[iRsc]);
        }
    }

    @Override
    public double getBiomass(int iRsc, Cell cell) {
        return multiplier[iRsc] * biomass[iRsc][cell.get_jgrid()][cell.get_igrid()];
    }

    double[][] readBiomass(int iRsc, int iStepSimu) {

        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();

        double[][] rscbiomass = new double[ny][nx];

        String name = getConfiguration().getString("resource.name.rsc" + iRsc);
        String ncFile = getConfiguration().getFile("resource.file.rsc" + iRsc);
        try (NetcdfFile nc = NetcdfFile.open(ncFile)) {
            Variable variable = nc.findVariable(name);
            int nTimeStep = variable.getShape(0);
            int iStepNc = iStepSimu % nTimeStep;
            Array ncbiomass = variable.read(new int[]{iStepNc, 0, 0}, new int[]{1, ny, nx}).reduce();
            Index index = ncbiomass.getIndex();
            for (Cell cell : getGrid().getCells()) {
                if (!cell.isLand()) {
                    int i = cell.get_igrid();
                    int j = cell.get_jgrid();
                    index.set(j, i);
                    rscbiomass[j][i] = ncbiomass.getDouble(index);
                }
            }
        } catch (IOException | InvalidRangeException ex) {
            error("File " + ncFile + ", variable " + name + "cannot be read", ex);
        }
        return rscbiomass;
    }

    int getTimeLength(int iRsc) {
        return timeLength[iRsc];
    }

    private double[][] fillBiomass(double b) {

        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();

        double[][] rscbiomass = new double[ny][nx];
        for (double[] row : rscbiomass) {
            Arrays.fill(row, b);
        }
        return rscbiomass;
    }
}
