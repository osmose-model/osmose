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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLFastForcing extends SimulationLinker implements LTLForcing {

    private float[][][][] biomass;
    /**
     * Number of time step in the LTL time series inputed to Osmose. This value
     * must be a multiple of the number of time step per year in Osmose. It
     * means the user can provide either one year, 5 years or 50 years of LTL
     * data and Osmose will loop over it (if necessary) until the end of the
     * simulation.
     */
    private int nLTLStep;
    /**
     * Current LTL time step
     */
    private int iLTLStep;

    public LTLFastForcing(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        String ncFile = getConfiguration().getFile("ltl.netcdf.file");
        if (!new File(ncFile).exists()) {
            error("Error reading LTLForcing parameters.", new FileNotFoundException("LTL NetCDF file " + ncFile + " does not exist."));
        }
        
        // Read number of LTL steps
        nLTLStep = getConfiguration().getInt("ltl.nstep");
        iLTLStep = 0;

        loadData(ncFile);
    }

    private void loadData(String ncFile) {
        try {
            info("Loading plankton data...");
            debug("Forcing file {0}", ncFile);

            NetcdfFile nc = NetcdfFile.open(ncFile);
            biomass = (float[][][][]) nc.findVariable("ltl_biomass").read().copyToNDJavaArray();
        } catch (IOException ex) {
            error("Error while loading LTL biomass from file " + ncFile, ex);
        }
    }

    /**
     *
     * @param iPlankton
     * @param cell
     * @return
     */
    @Override
    public float getBiomass(int iPlankton, Cell cell) {
        return biomass[iLTLStep][iPlankton][cell.get_jgrid()][cell.get_igrid()];
    }

    @Override
    public void update(int iStepSimu) {
        
        // Update the LTL time step
        iLTLStep = getSimulation().getIndexTimeSimu() % nLTLStep;
    }
}
