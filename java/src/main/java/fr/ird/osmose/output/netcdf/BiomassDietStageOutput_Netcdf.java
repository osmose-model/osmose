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

package fr.ird.osmose.output.netcdf;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.background.BackgroundSchool;
import fr.ird.osmose.stage.SchoolStage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class BiomassDietStageOutput_Netcdf extends AbstractOutput_Netcdf {

    private int nColumns;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private double[][] biomassStage;

    private SchoolStage dietOutputStage;

    public BiomassDietStageOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        dietOutputStage = new SchoolStage("output.diet.stage");
        dietOutputStage.init();

        nColumns = 0;
        // Sum-up diet stages
        for (int iSpec = 0; iSpec < getNSpecies() + getNBkgSpecies(); iSpec++) {
            nColumns += dietOutputStage.getNStage(iSpec);
        }
        nColumns += getConfiguration().getNRscSpecies();

        super.init();
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_biomassPredPreyIni_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Biomass (tons) of preys at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)";
    }

    @Override
    public void initStep() {

        // Init step for all the focal schools
        for (School school : getSchoolSet().getPresentSchools()) {
            biomassStage[school.getSpeciesIndex()][dietOutputStage.getStage(school)] += school.getBiomass();
        }

        // Init step for all the background schools
        for (BackgroundSchool school : this.getBkgSchoolSet().getAllSchools()) {
            biomassStage[school.getSpeciesIndex()][dietOutputStage.getStage(school)] += school.getBiomass();
        }

        int nBkg = getNBkgSpecies();
        int nSpec = getNSpecies();
        int nRsc = getConfiguration().getNRscSpecies();
        for (int i = 0; i < nRsc; i++) {
            int iRsc = i + nBkg + nSpec;  // index from a species prospective (focal, back, res)
            biomassStage[iRsc][0] += getTotalBiomass(nBkg + i);  // index from a resource perspective (back, res)
        }
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies() + getNBkgSpecies();
        int nPrey = nSpec + getConfiguration().getNRscSpecies();
        biomassStage = new double[nPrey][];
        int cpt = 0;
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            biomassStage[cpt++] = new double[dietOutputStage.getNStage(iSpec)];
        }

        for (int i = 0; i < getConfiguration().getNRscSpecies(); i++) {
            // we consider just 1 stage per resource group
            biomassStage[cpt++] = new double[1];
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
        int nSpecies = getNSpecies();
        int nBkg = getNBkgSpecies();
        int nRsc = getNRscSpecies();
        int k = 0;
        for (int iSpec = 0; iSpec < nSpecies + nBkg; iSpec++) {
            for (int s = 0; s < dietOutputStage.getNStage(iSpec); s++) {
                biomass[k] = biomassStage[iSpec][s] / nsteps;
                k++;
            }
        }
        for (int j = 0; j < nRsc; j++) {
            biomass[k] = biomassStage[j + nBkg + nSpecies][0] / nsteps;
            k++;
        }
        writeVariable(time, biomass);
    }

    /**
     * Gets the total biomass of the resource group over the grid.
     *
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

    @Override
    String getUnits() {
        return("tons"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return("biomass"); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    @Override
    void init_nc_dims_coords() {

        Dimension classDim = getBNc().addDimension("class_prey", nColumns);

        Variable.Builder<?> varBuilder = getBNc().addVariable("class_prey", DataType.FLOAT, "class_prey");
        this.setDims(new ArrayList<>(Arrays.asList(getTimeDim(), classDim)));

        int nSpec = getNSpecies() + getNBkgSpecies() + getNRscSpecies();
        int k = 0;
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            String name = getSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStage = dietOutputStage.getNStage(iSpec);
            String outname;
            for (int s = 0; s < nStage; s++) {
                if (nStage == 1) {
                    outname = name;    // Name predators
                } else {
                    if (s == 0) {
                        outname = name + " < " + threshold[s];    // Name predators
                    } else {
                        outname = name + " >=" + threshold[s - 1];    // Name predators
                    }
                }
                String attrname = String.format("%d", k);
                varBuilder.addAttribute(new Attribute(attrname, outname));
                k++;
            }
        }

    }

    @Override
    public void write_nc_coords() {

        // Writes variable trait (trait names) and species (species names)
        ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.nColumns);

        for (int i = 0; i < this.nColumns; i++) {
            arrClass.set(i, i);
        }

        Variable var = this.getNc().findVariable("class_prey");
        try {
            getNc().write(var, arrClass);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(BiomassDietStageOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
