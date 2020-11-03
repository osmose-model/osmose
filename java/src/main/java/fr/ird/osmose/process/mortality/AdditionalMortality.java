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

package fr.ird.osmose.process.mortality;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.additional.AnnualAdditionalMortality;
import fr.ird.osmose.process.mortality.additional.ByDtByClassAdditionalMortality;
import fr.ird.osmose.process.mortality.additional.ByDtLarvaMortality;
import fr.ird.osmose.process.mortality.additional.ByDtAdditionalMortality;
import fr.ird.osmose.process.mortality.additional.ConstantLarvaMortality;
import fr.ird.osmose.util.GridMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author pverley
 */
public class AdditionalMortality extends AbstractMortality {

    private AbstractMortalitySpecies[] larvaAdditionalMortality;
    private AbstractMortalitySpecies[] additionalMortality;
    /**
     * Spatial factor for additional mortality [0, 1]
     */
    private GridMap[] spatialD;

    public AdditionalMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int rank = getRank();
        int nSpecies = this.getConfiguration().getNSpecies();
        larvaAdditionalMortality = new AbstractMortalitySpecies[nSpecies];
        additionalMortality = new AbstractMortalitySpecies[nSpecies];

        // Find and initialises larva and adult Additional Mortality scenario for every species
        int cpt = 0;
        for (int iSpec : getConfiguration().getFocalIndex()) {
            Species species = getSpecies(cpt);
            // Find Larva Additional Mortality scenario
            ScenarioLarva scenarioLarva = findScenarioLarva(iSpec);
            debug("Larva Additional Mortality scenario for " + species.getName() + " set to " + scenarioLarva.toString());
            switch (scenarioLarva) {
                case CONSTANT:
                    larvaAdditionalMortality[cpt] = new ConstantLarvaMortality(rank, species);
                    break;
                case BY_DT:
                    larvaAdditionalMortality[cpt] = new ByDtLarvaMortality(rank, species);
                    break;
            }
            // Initialises Larva Additional Mortality scenario
            larvaAdditionalMortality[cpt].init();
            
            // Find Additional Mortality scenario
            Scenario scenario = findScenario(iSpec);
            debug("Additional Mortality scenario for " + species.getName() + " set to " + scenario.toString());
            switch (scenario) {
                case ANNUAL:
                    additionalMortality[cpt] = new AnnualAdditionalMortality(rank, species);
                    break;
                case BY_DT:
                    additionalMortality[cpt] = new ByDtAdditionalMortality(rank, species);
                    break;
                case BY_DT_BY_AGE:
                    additionalMortality[cpt] = new ByDtByClassAdditionalMortality(rank, species);
                    break;
                case BY_DT_BY_SIZE:
                    additionalMortality[cpt] = new ByDtByClassAdditionalMortality(rank, species);
                    break;
            }
            // Initialises Additional Mortality scenario
            additionalMortality[cpt].init();
            
            cpt++;
            
        }  // end of loop over focal species

        // Patch for Ricardo to include space variability in additional mortality
        // Need to think of a better parametrization before including it
        // formally in Osmose
        spatialD = new GridMap[nSpecies];
        List<String> keys = getConfiguration().findKeys("mortality.additional.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
             cpt = 0;
            for (int iSpec : getConfiguration().getFocalIndex()) {
                if (!getConfiguration().isNull("mortality.additional.spatial.distrib.file.sp" + iSpec)) {
                    spatialD[cpt] = new GridMap(getConfiguration().getFile("mortality.additional.spatial.distrib.file.sp" + iSpec));
                }
                cpt++;
            }
        }
    }

    /**
     * Additional mortality rate due to other predators (seals, sea birds, etc.)
     * or disease. For school of age 0 it returns the egg mortality rate at
     * current time step.
     *
     * @param school, a school of the system
     * @return the additional mortality rate for the current time step, and the
     * larva mortality rate for school of age 0.
     */
    @Override
    public double getRate(School school) {
        double D;
        Species spec = school.getSpecies();
        int ispec = school.getSpeciesIndex();
        if (school.isLarva()) {
            // Egg stage
            D = larvaAdditionalMortality[ispec].getRate(school);
        } else {
            if (null != spatialD[ispec] && !school.isUnlocated()) {
                D = (spatialD[ispec].getValue(school.getCell()) * additionalMortality[ispec].getRate(school));
            } else {
                D = additionalMortality[ispec].getRate(school);
            }
        }
        return D;
    }

    /**
     * Find the Additional Mortality scenario defined for the given species.
     * Osmose accepts exactly one scenario. The function throws an error if no
     * scenario or several scenarios are defined.
     *
     * @param iSpecies, the index of the species
     * @return the Additional Mortality scenario for this species
     */
    private Scenario findScenario(int iSpecies) {

        List<Scenario> scenarios = new ArrayList();
        // List the scenarios listed in the current configuration file
        for (Scenario scenario : Scenario.values()) {
            if (!getConfiguration().isNull(scenario.key + iSpecies)) {
                scenarios.add(scenario);
            }
        }

        // No scenario has been defined
        if (scenarios.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Set an Additional Mortality scenario among ");
            msg.append(Arrays.toString(Scenario.values()));
            error("No Additional Mortality scenario has been defined for species " + getSpecies(iSpecies).getName(), new NullPointerException(msg.toString()));
        }

        // Several scenarios have been defined
        if (scenarios.size() > 1) {
            StringBuilder msg = new StringBuilder();
            msg.append("Osmose found several Additional Mortality scenarios defined for species ");
            msg.append(getSpecies(iSpecies).getName());
            msg.append(": ");
            msg.append(Arrays.toString(scenarios.toArray()));
            error(msg.toString(), new UnsupportedOperationException("Only one Additional Mortality scenario per species can be defined."));
        }

        // return the scenario
        return scenarios.get(0);
    }

    /**
     * Find the Larva Additional Mortality scenario defined for the given
     * species. Osmose accepts exactly one scenario. The function throws an
     * error if no scenario or several scenarios are defined.
     *
     * @param iSpecies, the index of the species
     * @return the Larva Additional Mortality scenario for this species
     */
    private ScenarioLarva findScenarioLarva(int iSpecies) {

        List<ScenarioLarva> scenarios = new ArrayList();
        // List the scenarios listed in the current configuration file
        for (ScenarioLarva scenario : ScenarioLarva.values()) {
            if (!getConfiguration().isNull(scenario.key + iSpecies)) {
                scenarios.add(scenario);
            }
        }

        // No scenario has been defined
        if (scenarios.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Set a Larva Additional Mortality scenario among ");
            msg.append(Arrays.toString(Scenario.values()));
            error("No Larva Additional Mortality scenario has been defined for species " + getSpecies(iSpecies).getName(), new NullPointerException(msg.toString()));
        }

        // Several scenarios have been defined
        if (scenarios.size() > 1) {
            StringBuilder msg = new StringBuilder();
            msg.append("Osmose found several Larva Additional Mortality scenarios defined for species ");
            msg.append(getSpecies(iSpecies).getName());
            msg.append(": ");
            msg.append(Arrays.toString(scenarios.toArray()));
            error(msg.toString(), new UnsupportedOperationException("Only one Larva Additional Mortality scenario per species can be defined."));
        }

        // return the scenario
        return scenarios.get(0);
    }

    /**
     * Additional Mortality scenario
     */
    public enum Scenario {

        ANNUAL("mortality.additional.rate.sp"),
        BY_DT("mortality.additional.rate.bytDt.file.sp"),
        BY_DT_BY_AGE("mortality.additional.rate.byDt.byAge.file.sp"),
        BY_DT_BY_SIZE("mortality.additional.rate.byDt.bySize.file.sp");

        private final String key;

        private Scenario(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key + "#";
        }
    }

    /**
     * Larva Additional Mortality scenario
     */
    public enum ScenarioLarva {

        CONSTANT("mortality.additional.larva.rate.sp"),
        BY_DT("mortality.additional.larva.rate.bytDt.file.sp");

        private final String key;

        private ScenarioLarva(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key + "#";
        }
    }
}
