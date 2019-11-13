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

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.fishing.AbstractFishingMortality;
import fr.ird.osmose.process.mortality.fishing.RateByYearBySeasonFishingMortality;
import fr.ird.osmose.process.mortality.fishing.RateBySeasonFishingMortality;
import fr.ird.osmose.process.mortality.fishing.CatchesByDtByClassFishingMortality;
import fr.ird.osmose.process.mortality.fishing.CatchesBySeasonFishingMortality;
import fr.ird.osmose.process.mortality.fishing.CatchesByYearBySeasonFishingMortality;
import fr.ird.osmose.process.mortality.fishing.RateByDtByClassFishingMortality;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.MPA;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author pverley
 */
public class FishingMortality extends AbstractMortality {

    private AbstractFishingMortality[] fishingMortality;
    private List<MPA> mpas;
    private GridMap mpaFactor;
    private GridMap[] spatialFactor;
    private FishingMortality.Type[] fishingType;
    private List<School>[] arrSpecies;

    public FishingMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        fishingMortality = new AbstractFishingMortality[getNSpecies()];
        fishingType = new FishingMortality.Type[getNSpecies()];
        // Find type of fishing scenario

        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            int rank = getRank();
            Species species = getSpecies(iSpec);
            // Find fishing scenario
            Scenario scenario = findScenario(iSpec);
            debug("Fishing scenario for " + species.getName() + " set to " + scenario.toString());
            fishingType[iSpec] = scenario.type;
            switch (scenario) {
                case RATE_ANNUAL:
                    fishingMortality[iSpec] = new RateBySeasonFishingMortality(rank, species);
                    break;
                case RATE_BY_YEAR:
                    fishingMortality[iSpec] = new RateByYearBySeasonFishingMortality(rank, species);
                    break;
                case RATE_BY_DT_BY_AGE:
                    fishingMortality[iSpec] = new RateByDtByClassFishingMortality(rank, species);
                    break;
                case RATE_BY_DT_BY_SIZE:
                    fishingMortality[iSpec] = new RateByDtByClassFishingMortality(rank, species);
                    break;
                case CATCHES_ANNUAL:
                    fishingMortality[iSpec] = new CatchesBySeasonFishingMortality(rank, species);
                    break;
                case CATCHES_BY_YEAR:
                    fishingMortality[iSpec] = new CatchesByYearBySeasonFishingMortality(rank, species);
                    break;
                case CATCHES_BY_DT_BY_AGE:
                    fishingMortality[iSpec] = new CatchesByDtByClassFishingMortality(rank, species);
                    break;
                case CATCHES_BY_DT_BY_SIZE:
                    fishingMortality[iSpec] = new CatchesByDtByClassFishingMortality(rank, species);
                    break;
            }
            // Initialize fishing scenario
            fishingMortality[iSpec].init();
        }

        // Loads the MPAs
        int nMPA = getConfiguration().findKeys("mpa.file.mpa*").size();
        mpas = new ArrayList(nMPA);
        for (int iMPA = 0; iMPA < nMPA; iMPA++) {
            mpas.add(new MPA(getRank(), iMPA));
        }
        for (MPA mpa : mpas) {
            mpa.init();
        }
        // Initialize MPA correction factor
        mpaFactor = new GridMap(1);

        // Init array of species
        arrSpecies = new ArrayList[getNSpecies()];
        for (int i = 0; i < getNSpecies(); i++) {
            arrSpecies[i] = new ArrayList();
        }

        // Patch for Virginie to include space variability in fishing mortality
        // Need to think of a better way to include it to Osmose
        spatialFactor = new GridMap[getNSpecies()];
        List<String> keys = getConfiguration().findKeys("mortality.fishing.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
                if (!getConfiguration().isNull("mortality.fishing.spatial.distrib.file.sp" + iSpec)) {
                    spatialFactor[iSpec] = new GridMap(getConfiguration().getFile("mortality.fishing.spatial.distrib.file.sp" + iSpec));
                    // Make sure the sum of the values in ocean cells is equal to one
                    double sum = 0.d;
                    for (Cell cell : getGrid().getCells()) {
                        if (!cell.isLand()) {
                            sum += spatialFactor[iSpec].getValue(cell);
                        }
                    }
                    if (Math.abs(sum - 1.d) > 1e-2) {
                        StringBuilder msg = new StringBuilder();
                        msg.append("The sum of the factors in spatial fishing distribution file ");
                        msg.append(getConfiguration().getFile("mortality.fishing.spatial.distrib.file.sp" + iSpec));
                        msg.append(" must be equal to one.");
                        error(msg.toString(), null);
                    }
                }
            }
        }

    }

    /**
     * Find the fishing scenario defined for the given species. Osmose accepts
     * exactly one fishing scenario. The function throws an error if no scenario
     * or several scenarios are defined.
     *
     * @param iSpecies, the index of the species
     * @return the fishing scenario for this species
     */
    private Scenario findScenario(int iSpecies) {

        List<Scenario> scenarios = new ArrayList();
        // List the fishing scenarios listed in the current configuration file
        for (Scenario scenario : Scenario.values()) {
            if (!getConfiguration().isNull(scenario.key + iSpecies)) {
                scenarios.add(scenario);
            }
        }

        // No fishing scenario has been defined
        if (scenarios.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Set a fishing scenario among ");
            msg.append(Arrays.toString(Scenario.values()));
            error("No fishing scenario has been defined for species " + getSpecies(iSpecies).getName(), new NullPointerException(msg.toString()));
        }

        // Several fishing scenarios have been defined
        if (scenarios.size() > 1) {
            StringBuilder msg = new StringBuilder();
            msg.append("Osmose found several fishing scenarios defined for species ");
            msg.append(getSpecies(iSpecies).getName());
            msg.append(": ");
            msg.append(Arrays.toString(scenarios.toArray()));
            error(msg.toString(), new UnsupportedOperationException("Only one fishing scenario per species can be defined."));
        }

        // return the scenario
        return scenarios.get(0);
    }

    public void setMPA() {

        boolean isUpToDate = true;
        int iStep = getSimulation().getIndexTimeSimu();
        for (MPA mpa : mpas) {
            isUpToDate &= (mpa.isActive(iStep - 1) == mpa.isActive(iStep));
        }
        if (!isUpToDate) {
            mpaFactor = new GridMap(1);
            int nCellMPA = 0;
            for (MPA mpa : mpas) {
                if (mpa.isActive(iStep)) {
                    for (Cell cell : mpa.getCells()) {
                        mpaFactor.setValue(cell, 0.f);
                        nCellMPA++;
                    }
                }
            }

            // barrier.n: this correction seems to mean that if we have MPA, then 
            // we have greater pressure in non MPA cells. If 150 cells and 30 MPA,
            // corr = 1.25 and (nocean - npa) * corr = 150
            int nOceanCell = getGrid().getNOceanCell();
            float correction = (float) nOceanCell / (nOceanCell - nCellMPA);
            for (Cell cell : getGrid().getCells()) {
                if (mpaFactor.getValue(cell) > 0.f) {
                    mpaFactor.setValue(cell, correction);
                }
            }
        }
    }

    /**
     * Assess fishable biomass for fishing scenarios based on catches
     */
    void assessFishableBiomass() {

        // fishable biomass only has to be updated for catches
        boolean[] catches = new boolean[getNSpecies()];
        for (int i = 0; i < getNSpecies(); i++) {
            catches[i] = (Type.CATCHES == fishingMortality[i].getType());
        }

        // loop over all the schools
        for (School school : getSchoolSet().getSchools()) {
            int i = school.getSpeciesIndex();
            if (catches[i]) {
                // increment fishable biomass
                if (!school.isUnlocated() && fishingMortality[i].isFishable(school)) {
                    fishingMortality[i].incrementFishableBiomass(school);
                }
            }
        }
    }

    /**
     * Gets the absolute fishing mortality rate for a given school at current
     * time step of the simulation.
     *
     * @param school, a given school
     * @return the fishing mortality rate for the given school at current time
     * step of the simulation, expressed in dt^-1
     */
    @Override
    public double getRate(School school) {
        int iSpec = school.getSpeciesIndex();
        if (null != spatialFactor[iSpec]) {
            return fishingMortality[iSpec].getRate(school)
                    * mpaFactor.getValue(school.getCell())
                    * spatialFactor[iSpec].getValue(school.getCell());
        } else {
            return fishingMortality[iSpec].getRate(school)
                    * mpaFactor.getValue(school.getCell());
        }
    }

    /**
     * Returns the instantaneous level of catches, in tonne, for a given school
     * at current time step of the simulation.
     *
     * @param school, a given school
     * @return the instantaneous level of catches for the given school at
     * current time step of the simulation
     */
    public double getCatches(School school) {
        int iSpec = school.getSpeciesIndex();
        double catches;
        if (null != spatialFactor[iSpec]) {
            catches = fishingMortality[iSpec].getCatches(school)
                    * mpaFactor.getValue(school.getCell())
                    * spatialFactor[iSpec].getValue(school.getCell());
        } else {
            catches = fishingMortality[iSpec].getCatches(school)
                    * mpaFactor.getValue(school.getCell());
        }
        return Math.min(catches, school.getInstantaneousBiomass());
    }

    public Type getType(int iSpecies) {
        return fishingType[iSpecies];
    }

    /**
     * Fishing scenario
     */
    public enum Scenario {

        RATE_ANNUAL("mortality.fishing.rate.sp", Type.RATE),
        RATE_BY_YEAR("mortality.fishing.rate.byYear.file.sp", Type.RATE),
        RATE_BY_DT_BY_AGE("mortality.fishing.rate.byDt.byAge.file.sp", Type.RATE),
        RATE_BY_DT_BY_SIZE("mortality.fishing.rate.byDt.bySize.file.sp", Type.RATE),
        CATCHES_ANNUAL("mortality.fishing.catches.sp", Type.CATCHES),
        CATCHES_BY_YEAR("mortality.fishing.catches.byYear.file.sp", Type.CATCHES),
        CATCHES_BY_DT_BY_AGE("mortality.fishing.catches.byDt.byAge.file.sp", Type.CATCHES),
        CATCHES_BY_DT_BY_SIZE("mortality.fishing.catches.byDt.bySize.file.sp", Type.CATCHES);

        private final String key;
        private final Type type;

        private Scenario(String key, Type type) {
            this.key = key;
            this.type = type;
        }

        @Override
        public String toString() {
            return key + "#";
        }
    }

    /**
     * Type of fishing scenario. Fishing parameters are either rates or catches.
     */
    public enum Type {

        /**
         * Fishing mortality rates
         */
        RATE,
        /**
         * Catches
         */
        CATCHES;
    }
}
