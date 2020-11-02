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

import fr.ird.osmose.School;
import java.util.HashMap;
import java.util.function.Predicate;

/**
 *
 * @author pverley
 */
public class WeightedSpeciesOutput extends AbstractOutput {
    
    protected double[][] numerator;
    protected double[][] denumerator;
    
    private final String description;
    private final Predicate<School> predicate;
    private final SchoolVariableGetter variable;
    private final SchoolVariableGetter weight;

    public WeightedSpeciesOutput(int rank,
            String subfolder, String name, String description,
            Predicate<School> predicate,
            SchoolVariableGetter variable, SchoolVariableGetter weight) {
        super(rank, subfolder, name);
        this.description = description;
        this.predicate = predicate;
        this.variable = variable;
        this.weight = weight;
    }

    public WeightedSpeciesOutput(int rank,
            String subfolder, String name, String description,
            SchoolVariableGetter schoolVariable, SchoolVariableGetter weight) {
        this(rank, subfolder, name, description, school -> true, schoolVariable, weight);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        int nSpecies = getNSpecies();
        numerator = new double[nSpecies][];
        denumerator = new double[nSpecies][];
        for (int i = 0; i < nSpecies; i++) { 
            numerator[i] = new double[getNOutputRegion()];
            denumerator[i] = new double[getNOutputRegion()];
        }
    }

    @Override
    public void update() {

        int timeStep = this.getSimulation().getIndexTimeSimu();
        getSchoolSet().getAliveSchools().stream()
                .filter(predicate)
                .forEach(school -> {
                    double w = weight.getVariable(school);
                    double wvar = variable.getVariable(school) * w;
                    int irg = 0;
                    int iSpec = school.getGlobalSpeciesIndex();
                    for (AbstractOutputRegion region : getOutputRegions()) {
                        if (region.contains(timeStep, school)) {
                            numerator[iSpec][irg] += wvar;
                            denumerator[iSpec][irg] += w;
                        }
                        irg++;
                    }
                });
    }

    @Override
    public void write(float time) {

        int nSpecies = getNSpecies();
        for (int irg = 0; irg < getNOutputRegion(); irg++) {
            double[] result = new double[nSpecies];
            for (int isp = 0; isp < nSpecies; isp++) {
                result[isp] = (0 != denumerator[isp][irg])
                        ? numerator[isp][irg] / denumerator[isp][irg]
                        : Double.NaN;
    
            }
            writeVariable(irg, time, result);
        }
    }

    @Override
    final String[] getHeaders() {
        String[] species = new String[getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = getSpecies(i).getName();
        }
        return species;
    }

    @Override
    String getDescription() {
        return description;
    }
}

