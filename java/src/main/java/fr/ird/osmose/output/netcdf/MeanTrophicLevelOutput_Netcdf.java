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
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanTrophicLevelOutput_Netcdf extends AbstractOutput_Netcdf {

    private double[] meanTL;
    private double[] biomass;

    public MeanTrophicLevelOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        meanTL = new double[getNSpecies()];
        biomass = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            if (include(school)) {
                int i = school.getSpeciesIndex();
                meanTL[i] += school.getInstantaneousBiomass() * school.getTrophicLevel();
                biomass[i] += school.getInstantaneousBiomass();
            }
        }
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (biomass[i] > 0.d) {
                meanTL[i] = (float) (meanTL[i] / biomass[i]);
            } else {
                meanTL[i] = Double.NaN;
            }
        }
        writeVariable(time, meanTL);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanTL_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder str = new StringBuilder("Mean Trophic Level of fish species, weighted by fish biomass, and ");
        if (includeClassZero()) {
            str.append("including ");
        } else {
            str.append("excluding ");
        }
        str.append("first ages specified in input");
        return str.toString();
    }

    @Override
    String getUnits() {
        return (""); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return ("trophic_level");
    }
}
