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
import fr.ird.osmose.output.distribution.OutputDistribution;
import fr.ird.osmose.output.distribution.DistributionType;

/**
 *
 * @author pverley
 */
public class DistribOutput extends AbstractOutput {

    // Output values distributed by species and by class
    double[][][] values;

    // Distribution
    private final OutputDistribution distrib;
    // school variable getter
    protected final SchoolVariableGetter variable;
    // description
    private final String description;

    private final boolean computeAverage;

    /** Default constructor, in which temporal average is computed.
     *
     * @param rank
     * @param subfolder
     * @param name
     * @param description
     * @param species
     * @param variable
     * @param distrib
     */
    public DistribOutput(int rank, String subfolder,
            String name, String description,
            Species species,
            SchoolVariableGetter variable,
            OutputDistribution distrib) {

        this(rank, subfolder, name, description, species, variable, distrib, true);

    }

    /**
     * Full constructor, contains species and computeAverage arguments.
     *
     * @param rank
     * @param subfolder
     * @param name
     * @param description
     * @param species
     * @param variable
     * @param distrib
     * @param computeAverage
     */
    public DistribOutput(int rank, String subfolder,
            String name, String description,
            Species species,
            SchoolVariableGetter variable,
            OutputDistribution distrib, boolean computeAverage) {
        super(rank, subfolder, name + "DistribBy" + distrib.getType() + (null != species ? "-" + species.getName() : ""));
        this.distrib = distrib;
        this.variable = variable;
        this.description = description;
        this.computeAverage = computeAverage;
    }

    /** Constructor with compute average but not species.
     *
     * @param rank
     * @param subfolder
     * @param name
     * @param description
     * @param schoolVariable
     * @param distrib
     * @param computeAverage
     */
    public DistribOutput(int rank, String subfolder, String name, String description, SchoolVariableGetter schoolVariable, OutputDistribution distrib, boolean computeAverage) {
        this(rank, subfolder, name, description, null, schoolVariable, distrib, computeAverage);
    }

    /** Constructor without species and compute_average.
     *
     * @param rank
     * @param subfolder
     * @param name
     * @param description
     * @param schoolVariable
     * @param distrib
     */
    public DistribOutput(int rank, String subfolder, String name, String description, SchoolVariableGetter schoolVariable, OutputDistribution distrib) {
        this(rank, subfolder, name, description, null, schoolVariable, distrib, true);
    }

    @Override
    public void reset() {
        int nSpecies = this.getNSpecies();
        values = new double[nSpecies][][];
        for(int i = 0; i<nSpecies; i++) {
            values[i] = new double[getNOutputRegion()][distrib.getNClass()];
        }
    }

    @Override
    public void update() {
        int timeStep = this.getSimulation().getIndexTimeSimu();
        getSchoolSet().getAliveSchools().forEach(school -> {
            int classSchool = getClass(school);
            if (classSchool >= 0) {
                double var = variable.getVariable(school);
                int irg = 0;
                for (AbstractOutputRegion region : getOutputRegions()) {
                    if (region.contains(timeStep, school)) {
                        double sel = region.getSelectivity(timeStep, school);
                        values[school.getSpeciesIndex()][irg][getClass(school)] += sel * var;
                    }
                    irg++;
                }
            }
        });
    }

    @Override
    String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(description);
        sb.append(" by ").append(getType().getDescription());
        sb.append(". Class i designates interval [i,i+1[.");
        return sb.toString();
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    int getClass(IMarineOrganism school) {
        return distrib.getClass(school);
    }

    @Override
    public void write(float time) {

        int nSpecies = this.getNSpecies();
        int nClass = distrib.getNClass();
        double nsteps = getRecordFrequency();
        for (int irg = 0; irg < getNOutputRegion(); irg++) {
            double[][] array = new double[nClass][getNSpecies() + 1];
            for (int iClass = 0; iClass < nClass; iClass++) {
                int cpt = 0;
                array[iClass][cpt++] = distrib.getThreshold(iClass);
                if (this.computeAverage) {
                    for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                        array[iClass][cpt++] = values[iSpec][irg][iClass] / nsteps;
                    }
                } else {
                    for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                        array[iClass][cpt++] = values[iSpec][irg][iClass];
                    }
                }
            }
            writeVariable(irg, time, array);
        }
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + 1];
        int cpt = 0;
        headers[cpt++] = distrib.getType().toString();
        for (int i  = 0; i < getNSpecies(); i++) {
            headers[cpt++] = getSpecies(i).getName();
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
