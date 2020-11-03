/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
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
                        int iSpec = school.getSpeciesIndex();
                        if (cutoffEnabled && school.getAge() < cutoffAge[iSpec]) {
                            continue;
                        }
                        if (!school.isUnlocated()) {
                            // here, data is TK weighted by the biomass
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
