/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
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
        larvaAdditionalMortality = new AbstractMortalitySpecies[getNSpecies()];
        additionalMortality = new AbstractMortalitySpecies[getNSpecies()];

        // Find and initialises larva and adult Additional Mortality scenario for every species
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSpecies(iSpec);
            // Find Larva Additional Mortality scenario
            ScenarioLarva scenarioLarva = findScenarioLarva(iSpec);
            debug("Larva Additional Mortality scenario for " + species.getName() + " set to " + scenarioLarva.toString());
            switch (scenarioLarva) {
                case CONSTANT:
                    larvaAdditionalMortality[iSpec] = new ConstantLarvaMortality(rank, species);
                    break;
                case BY_DT:
                    larvaAdditionalMortality[iSpec] = new ByDtLarvaMortality(rank, species);
                    break;
            }
            // Initialises Larva Additional Mortality scenario
            larvaAdditionalMortality[iSpec].init();
            
            // Find Additional Mortality scenario
            Scenario scenario = findScenario(iSpec);
            debug("Additional Mortality scenario for " + species.getName() + " set to " + scenario.toString());
            switch (scenario) {
                case ANNUAL:
                    additionalMortality[iSpec] = new AnnualAdditionalMortality(rank, species);
                    break;
                case BY_DT:
                    additionalMortality[iSpec] = new ByDtAdditionalMortality(rank, species);
                    break;
                case BY_DT_BY_AGE:
                    additionalMortality[iSpec] = new ByDtByClassAdditionalMortality(rank, species);
                    break;
                case BY_DT_BY_SIZE:
                    additionalMortality[iSpec] = new ByDtByClassAdditionalMortality(rank, species);
                    break;
            }
            // Initialises Additional Mortality scenario
            additionalMortality[iSpec].init();
        }

        // Patch for Ricardo to include space variability in additional mortality
        // Need to think of a better parametrization before including it
        // formally in Osmose
        spatialD = new GridMap[getNSpecies()];
        List<String> keys = getConfiguration().findKeys("mortality.additional.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
                if (!getConfiguration().isNull("mortality.additional.spatial.distrib.file.sp" + iSpec)) {
                    spatialD[iSpec] = new GridMap(getConfiguration().getFile("mortality.additional.spatial.distrib.file.sp" + iSpec));
                }
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
        if (school.isLarva()) {
            // Egg stage
            D = larvaAdditionalMortality[school.getSpeciesIndex()].getRate(school);
        } else {
            if (null != spatialD[spec.getIndex()] && !school.isUnlocated()) {
                D = (spatialD[spec.getIndex()].getValue(school.getCell()) * additionalMortality[school.getSpeciesIndex()].getRate(school));
            } else {
                D = additionalMortality[school.getSpeciesIndex()].getRate(school);
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
