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

package fr.ird.osmose.output.netcdf;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class PredSuccessOutput_Netcdf extends AbstractOutput_Netcdf {

    private double[] predSuccess;
    private double[] nschool;

    public PredSuccessOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        predSuccess = new double[getNSpecies()];
        nschool = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
//            if (school.getPredSuccessRate() >= 0.57) {
//                predSuccess[school.getFileSpeciesIndex()] += 1;
//            }
            predSuccess[school.getFileSpeciesIndex()] += school.getPredSuccessRate();
            nschool[school.getFileSpeciesIndex()] += 1;
        }
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < predSuccess.length; i++) {
            predSuccess[i] /= (nschool[i]);
        }
        writeVariable(time, predSuccess);
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
         filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_predsuccess_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Predation success rate per species.";
    }

    @Override
    String getUnits() {
        throw new UnsupportedOperationException(""); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        throw new UnsupportedOperationException("predation_success"); //To change body of generated methods, choose Tools | Templates.
    }

}
