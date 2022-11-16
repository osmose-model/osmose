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

import fr.ird.osmose.Cell;
import fr.ird.osmose.stage.SchoolStage;

/**
 *
 * @author pverley
 */
public class BiomassDietStageOutput extends AbstractOutput {

    private int nColumns;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private double[][] biomassStage;

    private SchoolStage dietOutputStage;

    public BiomassDietStageOutput(int rank) {
        super(rank, "Trophic", "biomassPredPreyIni");
    }

    @Override
    public void init() {

        dietOutputStage = new SchoolStage("output.diet.stage");
        dietOutputStage.init();

        nColumns = 0;

        int nAll = this.getNBkgSpecies() + this.getNSpecies() + this.getNRscSpecies();

        // Sum-up diet stages
        for (int cpt = 0; cpt < nAll; cpt++) {
            nColumns += dietOutputStage.getNStage(cpt);
        }

        super.init();
    }

    @Override
    String getDescription() {
        return "\\ Biomass (tons) of preys at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)";
    }

    @Override
    public String[] getHeaders() {

        int nAll = this.getNBkgSpecies() + this.getNSpecies() + this.getNRscSpecies();

        int k = 0;
        String[] headers = new String[nColumns];
        for (int cpt = 0; cpt < nAll; cpt++) {
            String name = getISpecies(cpt).getName();
            float[] threshold = dietOutputStage.getThresholds(cpt);
            int nStage = dietOutputStage.getNStage(cpt);
            for (int s = 0; s < nStage; s++) {
                if (nStage == 1) {
                    headers[k] = name;    // Name predators
                } else {
                    if (s == 0) {
                        headers[k] = name + "[0 " + threshold[s] + "[";    // Name predators
                        headers[k] = String.format("%s in [%f, %f[", name, 0.f, threshold[s]);
                    } else if (s == nStage - 1) {
                        headers[k] = String.format("%s in [%f, inf[", name, threshold[s - 1]);
                    } else {
                        headers[k] = name + " >=" + threshold[s - 1];    // Name predators
                        headers[k] = String.format("%s in [%f, %f[", name, threshold[s - 1], threshold[s]);
                    }
                }
                k++;
            }
        } // end of species loop

        return headers;
    }

    @Override
    public void initStep() {

        getSchoolSet().getPresentSchools().forEach(school -> {
            biomassStage[school.getSpeciesIndex()][dietOutputStage.getStage(school)] += school.getBiomass();
        });

        this.getBkgSchoolSet().getAllSchools().forEach(school -> {
            biomassStage[school.getSpeciesIndex()][dietOutputStage.getStage(school)] += school.getBiomass();
        });

        int nSpecies = this.getNSpecies();
        int nBkg = this.getNBkgSpecies();
        for (int cpt = 0; cpt < this.getNRscSpecies(); cpt++) {
            biomassStage[nSpecies + nBkg + cpt][0] += getTotalBiomass(cpt + nBkg);
        }

    }

    @Override
    public void reset() {

        int nSpecies = this.getNSpecies();
        int nBkg = this.getNBkgSpecies();
        int nRsc = this.getNRscSpecies();
        int nAll = nSpecies + nBkg + nRsc;
        biomassStage = new double[nAll][];

        for (int cpt = 0; cpt < nAll; cpt++) {
            biomassStage[cpt] = new double[dietOutputStage.getNStage(cpt)];
        }
    }

    @Override
    public void update() {
        // nothing to do
    }

    @Override
    public void write(float time) {
        double[] biomass = new double[nColumns];
        double nsteps = getRecordFrequency();
        for (int k = 0; k < this.getNAllSpecies(); k++) {
            for (int s = 0; s < dietOutputStage.getNStage(k); s++) {
                biomass[k] = biomassStage[k][s] / nsteps;
            }
        } // end of species loop
        writeVariable(time, biomass);
    }

    /**
     * Gets the total biomass of the resource groups over the grid.
     * Note that the index in the argument is offseted, since
     * resource forcing starts with background species.
     * @return the cumulated biomass over the domain in tonne
     */
    private double getTotalBiomass(int iRsc) {
        double biomTot = 0.d;
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                biomTot += getSimulation().getResourceForcing(iRsc).getBiomass(cell);
            }
        }
        return biomTot;
    }

}
