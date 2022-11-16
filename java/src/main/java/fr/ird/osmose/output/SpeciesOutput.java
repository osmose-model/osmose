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

import java.util.List;

import fr.ird.osmose.background.BackgroundSchool;

/**
 *
 * @author nbarrier
 */
public class SpeciesOutput extends AbstractOutput {

    protected double[][] value;
    private final String description;
    private final SchoolVariableGetter schoolVariable;
    public final boolean computeAverage;

    public SpeciesOutput(int rank, String subfolder, String name, String description, SchoolVariableGetter schoolVariable) {
        super(rank, subfolder, name);
        this.description = description;
        this.schoolVariable = schoolVariable;
        this.computeAverage = true;
    }

     public SpeciesOutput(int rank, String subfolder, String name, String description, SchoolVariableGetter schoolVariable, boolean computeAverage) {
        super(rank, subfolder, name);
        this.description = description;
        this.schoolVariable = schoolVariable;
        this.computeAverage = computeAverage;
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();
        value = new double[nSpecies][];
        for (int i = 0; i < nSpecies; i++) {
            value[i] = new double[getNOutputRegion()];
        }
    }

    @Override
    public void update() {

        // recover the values for the focal schools
        int timeStep = this.getSimulation().getIndexTimeSimu();
        getSchoolSet().getAliveSchools().stream()
                .forEach(school -> {
                    int irg = 0;
                    for (AbstractOutputRegion region : getOutputRegions()) {
                        if (region.contains(timeStep, school)) {
                            double select = region.getSelectivity(timeStep, school);
                            value[school.getSpeciesIndex()][irg] += select * schoolVariable.getVariable(school);
                        }
                        irg++;
                    }
                });

        // Init the biomass of background species by using the ResourceForcing class
        for (List<BackgroundSchool> bkgSchoolList : this.getBkgSchoolSet().getValues()) { // loop over the cells
            for (BackgroundSchool bkg : bkgSchoolList) { // loop over the resources
                int irg = 0;
                for (AbstractOutputRegion region : getOutputRegions()) {
                    if (region.contains(timeStep, bkg)) {
                        double select = region.getSelectivity(timeStep, bkg);
                        value[bkg.getSpeciesIndex()][irg] += select * schoolVariable.getVariable(bkg);
                    }
                    irg++;
                }
            }
        }
    }

    @Override
    public void write(float time) {

        double nsteps = getRecordFrequency();
        int nSpecies = this.getNSpecies() + this.getNBkgSpecies();

        // Loop over the output regions
        for (int irg = 0; irg < getNOutputRegion(); irg++) {
            double[] output = new double[nSpecies];
            for (int i = 0; i < nSpecies; i++) {
                output[i] = value[i][irg];
                if (this.computeAverage) {
                    // If the average should be computed, then divides by the number
                    // of time steps.
                    output[i] /= nsteps;
                }
            }
            writeVariable(irg, time, output);
        }
    }

    @Override
    public final String[] getHeaders() {
        String[] species = new String[getNSpecies() + getNBkgSpecies()];
        for (int cpt = 0; cpt < getNSpecies() + getNBkgSpecies(); cpt++) {
            species[cpt] = getISpecies(cpt).getName();
        }
        return species;
    }

    @Override
    String getDescription() {
        return description;
    }
}
