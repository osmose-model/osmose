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
import fr.ird.osmose.process.mortality.MortalityCause;
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanTrophicLevelCatchOutput_Netcdf extends AbstractOutput_Netcdf {

    private double[] meanTLCatch;
    private double[] yield;

    public MeanTrophicLevelCatchOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        meanTLCatch = new double[getNSpecies()];
        yield = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int i = school.getFileSpeciesIndex();
            meanTLCatch[i] += school.getTrophicLevel() * school.abd2biom(school.getNdead(MortalityCause.FISHING));
            yield[i] += school.abd2biom(school.getNdead(MortalityCause.FISHING));
        }
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (yield[i] > 0) {
                meanTLCatch[i] = meanTLCatch[i] / yield[i];
            } else {
                meanTLCatch[i] = Double.NaN;
            }
        }
        writeVariable(time, meanTLCatch);
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanTLCatch_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean Trophic Level of fish species, weighted by fish catch, and including first ages specified in input";
    }

    @Override
    String getUnits() {
        return(""); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return("trophic_level"); //To change body of generated methods, choose Tools | Templates.
    }
}
