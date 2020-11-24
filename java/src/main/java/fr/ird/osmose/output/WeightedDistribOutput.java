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

import fr.ird.osmose.output.distribution.AbstractDistribution;

/**
 *
 * @author pverley
 */
public class WeightedDistribOutput extends DistribOutput {

    // Denumerator distributed by species and by class
    private double[][][] denominator;
    
    // school variable getter
    private final SchoolVariableGetter weight;

    public WeightedDistribOutput(int rank, String subfolder, String name, String description,
            SchoolVariableGetter variable, SchoolVariableGetter weight,
            AbstractDistribution distrib) {
        super(rank, subfolder, name, description, null, variable, distrib);
        this.weight = weight;
    }

    @Override
    public void update() {
        int timeStep = this.getSimulation().getIndexTimeSimu();
        getSchoolSet().getAliveSchools().forEach(school -> {
            int classSchool = getClass(school);
            int iSpec = school.getSpeciesIndex();
            if (classSchool >= 0) {
                double w = weight.getVariable(school);
                double wvar = w * variable.getVariable(school);
                int irg = 0;
                for (AbstractOutputRegion region : getOutputRegions()) {
                    if (region.contains(timeStep, school)) {
                        values[iSpec][irg][getClass(school)] += wvar;
                        denominator[iSpec][irg][classSchool] += w;
                    }
                    irg++;
                }
            }
        });
    }

    @Override
    public void reset() {
        super.reset();
        int nSpecies = this.getNSpecies();
        denominator = new double[nSpecies][][];
        for (int i = 0; i < nSpecies; i++) {
            denominator[i] = new double[getNOutputRegion()][this.getNClass()];
        }
    }

    @Override
    public void write(float time) {

        int nSpecies = this.getNSpecies();
        int nClass = getNClass();
        for (int irg = 0; irg < getNOutputRegion(); irg++) {
            double[][] array = new double[nClass][getNSpecies() + 1];
            for (int iClass = 0; iClass < nClass; iClass++) {
                int cpt = 0;
                array[iClass][cpt++] = getClassThreshold(iClass);
                for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                    if (denominator[iSpec][irg][iClass] != 0) {
                        array[iClass][cpt++] = values[iSpec][irg][iClass] / denominator[iSpec][irg][iClass];
                    } else {
                        array[iClass][cpt++] = Double.NaN;
                    }
                }
            }
            writeVariable(irg, time, array);
        }
    }

}
