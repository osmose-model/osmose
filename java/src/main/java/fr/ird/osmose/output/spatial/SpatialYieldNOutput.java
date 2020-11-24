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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import fr.ird.osmose.process.mortality.MortalityCause;

/**
 *
 * @author Nicolas Barrier
 */
public class SpatialYieldNOutput extends AbstractSpatialOutput {

    public SpatialYieldNOutput(int rank) {
        super(rank);
    }

    @Override
    public String getVarName() {
        return "Yield";
    }

    @Override
    public String getDesc() {
        return "Catches, in tons, per species and per cell";
    }

    @Override
    public void update() {

        this.common_update();

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
                            data[iSpec][j][i] += school.getNdead(MortalityCause.FISHING);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void write(float time) {

        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                    data[ispec][j][i] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D4 arrBiomass = new ArrayFloat.D4(1, nSpecies, getGrid().get_ny(), getGrid().get_nx());
        for (int kspec = 0; kspec < nSpecies; kspec++) {
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    arrBiomass.set(0, kspec, j, i, data[kspec][j][i]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) this.timeOut * 360 / (float) this.counter);

        int index = this.getNetcdfIndex();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write(this.getTimeVar(), new int[]{index}, arrTime);
            nc.write(this.getOutVar(), new int[]{index, 0, 0, 0}, arrBiomass);
            this.incrementIndex();
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbstractSpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
