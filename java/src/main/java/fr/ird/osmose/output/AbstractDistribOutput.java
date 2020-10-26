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

package fr.ird.osmose.output;

import fr.ird.osmose.IMarineOrganism;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.output.distribution.DistributionType;
import java.util.HashMap;

/**
 *
 * @author pverley
 */
public abstract class AbstractDistribOutput extends AbstractOutput {

    // Output values distributed by species and by class
    protected double[][] values;
    
    // Distribution 
    private final AbstractDistribution distrib;

    public AbstractDistribOutput(int rank, String subfolder, String name, Species species, AbstractDistribution distrib) {
        super(rank, subfolder, name + "DistribBy" + distrib.getType() + (null != species ? "-" + species.getName() : ""));
        this.distrib = distrib;
    }
    
    public AbstractDistribOutput(int rank, String subfolder, String name, AbstractDistribution distrib) {
        this(rank, subfolder, name, null, distrib);
    }

    @Override
    public void reset() {
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
        values = new double[nSpecies][];
        for (int i = 0; i < nSpecies; i++) {
            values[i] = new double[distrib.getNClass()];
        }
    }

    int getClass(IMarineOrganism school) {
        return distrib.getClass(school);
    }

    @Override
    public void write(float time) {

        int nClass = distrib.getNClass();
        double[][] array = new double[nClass][getNSpecies() + this.getNBkgSpecies() +  1];
        for (int iClass = 0; iClass < nClass; iClass++) {
            int cpt = 0;
            array[iClass][cpt++] = distrib.getThreshold(iClass);
            for (int iSpec = 0; iSpec < this.getNBkgSpecies() + this.getNSpecies(); iSpec++) {
                array[iClass][cpt++] = values[iSpec][iClass] / getRecordFrequency();
            }
        }
        writeVariable(time, array);
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + 1];
        headers[0] = distrib.getType().toString();
        for (int i = 0; i < this.getNSpecies() + this.getNBkgSpecies(); i++) {
            headers[i + 1] = getISpecies(i).getName();
        }
        return headers;
    }

    float getClassThreshold(int iClass) {
        return distrib.getThreshold(iClass);
    }

    int getNClass() {
        return distrib.getNClass();
    }

    DistributionType getType() {
        return distrib.getType();
    }
}
