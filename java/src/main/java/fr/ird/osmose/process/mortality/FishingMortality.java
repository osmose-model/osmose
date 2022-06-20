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
    private GridMap[] mpaFactor;
    private GridMap[] spatialFactor;
    private FishingMortality.Type[] fishingType;
    //private List<School>[] arrSpecies;

    public FishingMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nSpecies = this.getNSpecies();
        fishingMortality = new AbstractFishingMortality[nSpecies];
        fishingType = new FishingMortality.Type[nSpecies];
        // Find type of fishing scenario

        int cpt = 0;
        for (int fileSpeciesIndex : getConfiguration().getFocalIndex()) {
            int rank = getRank();
            Species species = getSpecies(cpt);
            // Find fishing scenario
            Scenario scenario = findScenario(fileSpeciesIndex);
            debug("Fishing scenario for " + species.getName() + " set to " + scenario.toString());
            fishingType[cpt] = scenario.type;
            switch (scenario) {
                case RATE_ANNUAL:
                    fishingMortality[cpt] = new RateBySeasonFishingMortality(rank, species);
                    break;
                case RATE_BY_YEAR:
                    fishingMortality[cpt] = new RateByYearBySeasonFishingMortality(rank, species);
                    break;
                case RATE_BY_DT_BY_AGE:
                    fishingMortality[cpt] = new RateByDtByClassFishingMortality(rank, species);
                    break;
                case RATE_BY_DT_BY_SIZE:
                    fishingMortality[cpt] = new RateByDtByClassFishingMortality(rank, species);
                    break;
                case CATCHES_ANNUAL:
                    fishingMortality[cpt] = new CatchesBySeasonFishingMortality(rank, species);
                    break;
                case CATCHES_BY_YEAR:
                    fishingMortality[cpt] = new CatchesByYearBySeasonFishingMortality(rank, species);
                    break;
                case CATCHES_BY_DT_BY_AGE:
                    fishingMortality[cpt] = new CatchesByDtByClassFishingMortality(rank, species);
                    break;
                case CATCHES_BY_DT_BY_SIZE:
                    fishingMortality[cpt] = new CatchesByDtByClassFishingMortality(rank, species);
                    break;
            }
            // Initialize fishing scenario
            fishingMortality[cpt].init();
            cpt++;
        }

        // Loads the MPAs
        int nMPA = getConfiguration().findKeys("mpa.file.mpa*").size();
        mpas = new ArrayList<>(nMPA);
        for (int iMPA = 0; iMPA < nMPA; iMPA++) {
            mpas.add(new MPA(getRank(), iMPA));
        }

        for (MPA mpa : mpas) {
            mpa.init();
        }

        // Initialize MPA correction factor array (one for each species)
        // initialize with values of ones.
        mpaFactor = new GridMap[nSpecies];
        for(int iSpecies=0; iSpecies < nSpecies; iSpecies++) {
            mpaFactor[iSpecies] = new GridMap(1);
        }

        // Initialize the spatialFactor. Initialize with ones everywhere.
        spatialFactor = new GridMap[nSpecies];
        for(int iSpecies=0; iSpecies < nSpecies; iSpecies++) {
            spatialFactor[iSpecies] = new GridMap(1);
        }

        List<String> keys = getConfiguration().findKeys("mortality.fishing.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            cpt = 0;
            for (int fileSpeciesIndex : getConfiguration().getFocalIndex()) {
                if (!getConfiguration().isNull("mortality.fishing.spatial.distrib.file.sp" + fileSpeciesIndex)) {
                    spatialFactor[cpt] = new GridMap(getConfiguration().getFile("mortality.fishing.spatial.distrib.file.sp" + fileSpeciesIndex));
                    // Make sure the sum of the values in ocean cells is equal to one
                    double sum = 0.d;
                    int nCells = 0;
                    for (Cell cell : getGrid().getCells()) {
                        if (!cell.isLand()) {
                            double value = spatialFactor[cpt].getValue(cell);
                            sum += value;
                            if(value > 0) {
                                nCells++;
                            }
                        }
                    }

                    sum /= nCells;

                    if (Math.abs(sum - 1.d) > 1e-2) {
                        StringBuilder msg = new StringBuilder();
                        msg.append("The means of the factors in spatial fishing distribution file ");
                        msg.append(getConfiguration().getFile("mortality.fishing.spatial.distrib.file.sp" + fileSpeciesIndex));
                        msg.append(" must be equal to one.");
                        error(msg.toString(), null);
                    }
                }  // end of existence test

                cpt++;

            }  // end of loop on species
        }

    }

    /**
     * Find the fishing scenario defined for the given species. Osmose accepts
     * exactly one fishing scenario. The function throws an error if no scenario
     * or several scenarios are defined.
     *
     * @param fileSpeciesIndex, the index of the species
     * @return the fishing scenario for this species
     */
    private Scenario findScenario(int fileSpeciesIndex) {

        List<Scenario> scenarios = new ArrayList<>();
        // List the fishing scenarios listed in the current configuration file
        for (Scenario scenario : Scenario.values()) {
            if (!getConfiguration().isNull(scenario.key + fileSpeciesIndex)) {
                scenarios.add(scenario);
            }
        }

        // No fishing scenario has been defined
        if (scenarios.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Set a fishing scenario among ");
            msg.append(Arrays.toString(Scenario.values()));
            error("No fishing scenario has been defined for species " + fileSpeciesIndex, new NullPointerException(msg.toString()));
        }

        // Several fishing scenarios have been defined
        if (scenarios.size() > 1) {
            StringBuilder msg = new StringBuilder();
            msg.append("Osmose found several fishing scenarios defined for species ");
            msg.append(fileSpeciesIndex);
            msg.append(": ");
            msg.append(Arrays.toString(scenarios.toArray()));
            error(msg.toString(), new UnsupportedOperationException("Only one fishing scenario per species can be defined."));
        }

        // return the scenario
        return scenarios.get(0);
    }

    public void setMPA() {

        int nX = getGrid().get_nx();
        int nY = getGrid().get_ny();
            
        boolean isUpToDate = true;
        int iStep = getSimulation().getIndexTimeSimu();
        for (MPA mpa : mpas) {
            isUpToDate &= (mpa.isActive(iStep - 1) == mpa.isActive(iStep));
        }

        if (!isUpToDate) {
            
            for (int iSpecies = 0; iSpecies < this.getNSpecies(); iSpecies++) {

                GridMap spatialMap = this.spatialFactor[iSpecies];

                // initialize the MPA correction factor. By default, it is 1 (no MPA).
                GridMap mpaGridMap = new GridMap(1);

                // Compute the initial MPA correction factor.
                // % of cell occupied not occupie by MPA (0 if 100% occupied by MPA).
                for (MPA mpa : mpas) {
                    if (mpa.isActive(iStep)) {
                        for (Cell cell : mpa.getCells()) {
                            float percentageMPA = mpa.getPercentageMPA(cell);
                            mpaGridMap.setValue(cell, 1 - percentageMPA);
                        }
                    }
                }

                // Force mpaFactor to 0 when no fishing in the cell
                for (Cell cell : getGrid().getCells()) {
                    if (spatialMap.getValue(cell) <= 0) {
                        mpaGridMap.setValue(cell, 0);
                    }
                }
                
                // compute the fishing effort by dividing fishing mort
                // by the total (excluding 0s). 
                // we also integrate the correction factor
                float correction = 0;
                           
                float totalEffort = spatialMap.count(true);
                GridMap effort = new GridMap();
                for (int j = 0; j < nY; j++) {
                    for (int i = 0; i < nX; i++) {
                        float value = spatialMap.getValue(i, j);
                        if ((value != 0) & !Float.isNaN(value)) {
                            effort.setValue(i, j, value / totalEffort);
                            correction += effort.getValue(i, j) * mpaGridMap.getValue(i, j);
                            // if effort is 0 in the MPA, no need to redistribute effort.
                            // factor is 0.
                        }
                    }
                }

                correction = 1 / correction;
                
                // apply the correction to the mpaFactor.
                for (int j = 0; j < nY; j++) {
                    for (int i = 0; i < nX; i++) {
                        mpaFactor[iSpecies].setValue(i, j, correction * mpaGridMap.getValue(i, j));
                    }
                }
            }
        }
    }

    /**
     * Assess fishable biomass for fishing scenarios based on catches
     */
    public void assessFishableBiomass() {

        // fishable biomass only has to be updated for catches
        Boolean catches[] = new Boolean[this.getNSpecies()];
        for (int cpt = 0; cpt < this.getNSpecies(); cpt++) {
            catches[cpt] = (Type.CATCHES == fishingMortality[cpt].getType());
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
        return fishingMortality[iSpec].getRate(school)
                    * mpaFactor[iSpec].getValue(school.getCell())
                    * spatialFactor[iSpec].getValue(school.getCell());
    }
    
    /** Returns the correction factor induced by the inclusion of MPA and
     * spatial factor. Used only for testing.
     */
    public float getFactor(int iSpec, Cell cell) {
        return mpaFactor[iSpec].getValue(cell) * spatialFactor[iSpec].getValue(cell);
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
        double catches = fishingMortality[iSpec].getCatches(school)
                    * mpaFactor[iSpec].getValue(school.getCell())
                    * spatialFactor[iSpec].getValue(school.getCell());
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

    public AbstractFishingMortality getFishingMortality(int index) {
        return this.fishingMortality[index];
    }

}
