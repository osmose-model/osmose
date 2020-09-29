/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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
import fr.ird.osmose.output.distribution.AbstractDistribution;
import java.io.File;

/**
 *
 * @author pverley
 */
public class BiomassDistribOutput_Netcdf extends AbstractDistribOutput_Netcdf {

    public BiomassDistribOutput_Netcdf(int rank, AbstractDistribution distrib) {
        super(rank, distrib);
    }
    
    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int classSchool = getClass(school);
            if (classSchool >= 0) {
                values[school.getSpeciesIndex()][classSchool] += school.getInstantaneousBiomass();
            }
        }
    }

    @Override
    String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getType().toString());
        filename.append("Indicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_biomassDistribBy");
        filename.append(getType().toString());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");;
        return filename.toString();

    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Distribution of fish species biomass (tonne) by ");
        description.append(getType().getDescription());
        description.append(". For class i, the biomass of fish in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    String getUnits() {
        return("tons");
    }

    @Override
    String getVarname() {
       return("biomass");
    }
    
}
