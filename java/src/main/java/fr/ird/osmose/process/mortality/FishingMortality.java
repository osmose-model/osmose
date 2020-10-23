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
    private GridMap mpaFactor;
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
        for (int iSpec : getConfiguration().getFocalIndex()) {
            int rank = getRank();
            Species species = getSpecies(iSpec);
            // Find fishing scenario
            Scenario scenario = findScenario(iSpec);
            debug("Fishing scenario for " + species.getName() + " set to " + scenario.toString());
            fishingType[cpt] = scenario.type;
            switch (scenario) {
                case RATE_ANNUAL:
                    fishingMortality[cpt]  =new RateBySeasonFishingMortality(rank, species);
                    break;
                case RATE_BY_YEAR:
                    fishingMortality[cpt]  =new RateByYearBySeasonFishingMortality(rank, species);
                    break;
                case RATE_BY_DT_BY_AGE:
                    fishingMortality[cpt]  =new RateByDtByClassFishingMortality(rank, species);
                    break;
                case RATE_BY_DT_BY_SIZE:
                    fishingMortality[cpt]  =new RateByDtByClassFishingMortality(rank, species);
                    break;
                case CATCHES_ANNUAL:
                    fishingMortality[cpt]  =new CatchesBySeasonFishingMortality(rank, species);
                    break;
                case CATCHES_BY_YEAR:
                    fishingMortality[cpt]  =new CatchesByYearBySeasonFishingMortality(rank, species);
                    break;
                case CATCHES_BY_DT_BY_AGE:
                    fishingMortality[cpt]  =new CatchesByDtByClassFishingMortality(rank, species);
                    break;
                case CATCHES_BY_DT_BY_SIZE:
                    fishingMortality[cpt]  =new CatchesByDtByClassFishingMortality(rank, species);
                    break;
            }
            // Initialize fishing scenario
            fishingMortality[cpt].init();
            cpt++;
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

        // Patch for Virginie to include space variability in fishing mortality
        // Need to think of a better way to include it to Osmose
        spatialFactor = new GridMap[nSpecies];
        List<String> keys = getConfiguration().findKeys("mortality.fishing.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            cpt = 0;
            for (int iSpec : getConfiguration().getFocalIndex()) {
                if (!getConfiguration().isNull("mortality.fishing.spatial.distrib.file.sp" + iSpec)) {
                    spatialFactor[cpt] = new GridMap(getConfiguration().getFile("mortality.fishing.spatial.distrib.file.sp" + iSpec));
                    // Make sure the sum of the values in ocean cells is equal to one
                    double sum = 0.d;
                    for (Cell cell : getGrid().getCells()) {
                        if (!cell.isLand()) {
                            sum += spatialFactor[cpt].getValue(cell);
                        }
                    }
                    if (Math.abs(sum - 1.d) > 1e-2) {
                        StringBuilder msg = new StringBuilder();
                        msg.append("The sum of the factors in spatial fishing distribution file ");
                        msg.append(getConfiguration().getFile("mortality.fishing.spatial.distrib.file.sp" + iSpec));
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
    public void assessFishableBiomass() {

        // fishable biomass only has to be updated for catches
        Boolean catches[] = new Boolean[this.getNSpecies()];
        int cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            catches[cpt] = (Type.CATCHES == fishingMortality[cpt].getType());
        }

        // loop over all the schools
        for (School school : getSchoolSet().getSchools()) {
            int i = school.getGlobalSpeciesIndex();
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
        int iSpec = school.getGlobalSpeciesIndex();
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
        int iSpec = school.getGlobalSpeciesIndex();
        double catches;
        if (null != spatialFactor[iSpec]){
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
