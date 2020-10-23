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

import fr.ird.osmose.Species;
import fr.ird.osmose.output.distribution.AbstractDistribution;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class DietDistribOutput extends AbstractDistribOutput {

    private final Species species;

    public DietDistribOutput(int rank, Species species, AbstractDistribution distrib) {
        super(rank, "Trophic", "dietMatrix", species, distrib);
        this.species = species;
    }

    @Override
    public void reset() {
        values.clear();
        for (int i : getConfiguration().getAllIndex()) {
            values.put(i, new double[getNClass()]);
        }
    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Distribution of the biomass (tonne) of prey species (in columns) in the diet of ");
        description.append(species.getName());
        description.append(" by ");
        description.append(getType().getDescription());
        description.append(". For class i, the preyed biomass in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + getConfiguration().getNRscSpecies() + getConfiguration().getNBkgSpecies() + 1];  
        headers[0] = getType().toString();
        int cpt = 1;
        
        for (int i : getConfiguration().getFocalIndex()) {
            headers[cpt] = getSpecies(i).getName();
            cpt++;
        }
        
        for (int i : getConfiguration().getBkgIndex()) {
            headers[cpt] = this.getBkgSpecies(i).getName();
            cpt++;
        }
        
        for (int i : getConfiguration().getRscIndex()) {
            headers[cpt] = getConfiguration().getResourceSpecies(i).getName();
            cpt++;
        }
        return headers;
    }

    @Override
    public void update() {

        getSchoolSet().getSchools(species, false).stream()
                .filter(predator -> predator.getPreyedBiomass() > 0)
                .forEach(predator -> {
                    predator.getPreys().forEach(prey -> {
                        int classPredator = getClass(predator);
                        if (classPredator >= 0) {
                            values.get(prey.getSpeciesIndex())[classPredator] += prey.getBiomass();
                        }
                    });
                });
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void write(float time) {
        // values = new double[getNSpecies() + getConfiguration().getNResource()][getNClass()];
        int nClass = this.getNClass();
        double[][] array = new double[nClass][getNSpecies() + getConfiguration().getNRscSpecies() + getConfiguration().getNBkgSpecies() + 1];
        for (int iClass = 0; iClass < nClass; iClass++) {
            int cpt = 0;
            array[iClass][cpt++] = this.getClassThreshold(iClass);
            for (int iSpec : this.getConfiguration().getAllIndex()) {
                array[iClass][cpt++] = values.get(iSpec)[iClass] / getRecordFrequency();
            }
        }
        writeVariable(time, array);
    }
}
