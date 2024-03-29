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

/**
 *
 * @author pverley
 */
public class NewSchoolOutput extends AbstractOutput {

    private double[] biomass;
    private double[] egg;

    public NewSchoolOutput(int rank) {
        super(rank, null, "egg");
    }

    @Override
    public void initStep() {
        getSchoolSet().getAliveSchools().stream().forEach(school -> {
            if (school.isEgg()) {
                egg[school.getSpeciesIndex()] += school.getBiomass();
            }
            biomass[school.getSpeciesIndex()] += school.getBiomass();
        });
    }

    @Override
    public void reset() {
        biomass = new double[getNSpecies()];
        egg = new double[getNSpecies()];
    }

    @Override
    public void update() {
        // nothing to do
    }

    @Override
    public void write(float time) {

        double nsteps = getRecordFrequency();
        for (int i = 0; i < biomass.length; i++) {
            egg[i] = 100 * (egg[i] / biomass[i]) / nsteps;
        }
        writeVariable(time, egg);
    }

    @Override
    String getDescription() {
        return "Percentage of egg biomass for each species";
    }

    @Override
    public String[] getHeaders() {
        String[] species = new String[getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = getSpecies(i).getName();
        }
        return species;
    }

}
