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

/**
 * This class loads in memory all resource data from a NetCDF file at the
 * beginning of the simulation, conversely to LTLForcing class that loads the
 * data at each time step. It was coded in order to speed up configurations that
 * loop over the same year of NetCDF data during all the simulation.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 */
public class LTLFastForcing extends AbstractLTLForcing {

    /**
     * The LTL biomass [TIME][NRESOURCE][NY][NX]
     */
    private double[][][][] biomass;

    public LTLFastForcing(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        // initializes ResourceForcing
        super.init();

        // Read number of LTL steps
        int nLTLStep = getConfiguration().getInt("ltl.nstep");
        int nRsc = getConfiguration().getNRscSpecies();

        // Initialises biomass variable
        biomass = new double[nLTLStep][nRsc][getGrid().get_ny()][getGrid().get_nx()];
        for (int iTime = 0; iTime < nLTLStep; iTime++) {
            super.update(iTime);
            for (int iRsc = 0; iRsc < nRsc; iRsc++) {
                for (Cell cell : getGrid().getCells()) {
                    if (!cell.isLand()) {
                        int i = cell.get_igrid();
                        int j = cell.get_jgrid();
                        biomass[iTime][iRsc][j][i] = super.getBiomass(iRsc, cell);
                    }
                }
            }
        }
    }

    @Override
    public double getBiomass(int iRsc, Cell cell) {
        int ltlTimeStep = getSimulation().getIndexTimeSimu() % biomass.length;
        return biomass[ltlTimeStep][iRsc][cell.get_jgrid()][cell.get_igrid()];
    }

    /**
     * In the case of LTLFastForcing, and conversely to other classes extending
     * AbstractLTLForcing, there is no data to update as it is already loaded in
     * memory since the beginning of the simulation.
     *
     * @param iStepSimu, the current time step of the simulation
     */
    @Override
    public void update(int iStepSimu) {
        // Do nothing
    }
}
