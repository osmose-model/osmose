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
package fr.ird.osmose.output.spatial;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;

/**
 *
 * @author Nicolas Barrier
 */
public class SpatialTLOutput extends AbstractSpatialOutput {

    public SpatialTLOutput(int rank) {
        super(rank);
    }

    @Override
    public String getVarName() {
        return "TL";
    }

    @Override
    public String getDesc() {
        return "trophic level per species and per cell";
    }

    @Override
    public void update() {

        // In this case, a weighted mean is applied on the TL, with
        // weights being provided by the biomass
        this.common_update();

        int nSpecies = getNSpecies();
        int nx = getGrid().get_nx();
        int ny = getGrid().get_ny();

        // temporary variable containing the total biomass within one cell
        float biomass[][][];
        biomass = new float[nSpecies][ny][nx];
        float temp[][][];
        temp = new float[nSpecies][ny][nx];

        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        if (cutoffEnabled && school.getAge() < cutoffAge[school.getSpeciesIndex()]) {
                            continue;
                        }
                        if (!school.isUnlocated()) {
                            // here, data is TK weighted by the biomass
                            int iSpec = school.getSpeciesIndex();
                            temp[iSpec][j][i] += school.getTrophicLevel() * school.getInstantaneousBiomass();
                            biomass[iSpec][j][i] += school.getInstantaneousBiomass();
                        }
                    }
                }
            }
        }

        // Computation of the Weighted Mean for the TL
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    if (biomass[iSpec][j][i] > 0) {
                        temp[iSpec][j][i] /= biomass[iSpec][j][i];
                    }
                }
            }
        }

        // Update of the TL array
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    if (biomass[iSpec][j][i] > 0) {
                        data[iSpec][j][i] += temp[iSpec][j][i];
                    }
                }
            }
        }

    }
}
