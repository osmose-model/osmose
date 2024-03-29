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

import fr.ird.osmose.School;
import fr.ird.osmose.Prey;
import fr.ird.osmose.stage.SchoolStage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
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
public class DietOutput_Netcdf extends AbstractOutput_Netcdf {

    private double[][][][] diet;
    private double[][] abundanceStage;
    // Diet output stage
    private SchoolStage dietOutputStage;

    private int nPreys;
    private int nPred;

    public DietOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies() + getNBkgSpecies();
        int nPrey = nSpec + getConfiguration().getNRscSpecies();
        diet = new double[nSpec][][][];
        abundanceStage = new double[nSpec][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            int nStage = dietOutputStage.getNStage(iSpec);
            diet[iSpec] = new double[nStage][][];
            abundanceStage[iSpec] = new double[nStage];
            for (int iStage = 0; iStage < nStage; iStage++) {
                diet[iSpec][iStage] = new double[nPrey][];
                for (int iPrey = 0; iPrey < nPrey; iPrey++) {
                    if (iPrey < nSpec) {
                        diet[iSpec][iStage][iPrey] = new double[dietOutputStage.getNStage(iPrey)];
                    } else {
                        diet[iSpec][iStage][iPrey] = new double[1];
                    }
                }
            }
        }
    }

    @Override
    public void update() {

        for (School school : getSchoolSet().getPresentSchools()) {
            double preyedBiomass = school.getPreyedBiomass();
            int iSpec = school.getSpeciesIndex();
            if (preyedBiomass > 0) {
                abundanceStage[iSpec][dietOutputStage.getStage(school)] += school.getAbundance();
                for (Prey prey : school.getPreys()) {
                    diet[iSpec][dietOutputStage.getStage(school)][prey.getSpeciesIndex()][dietOutputStage.getStage(prey)] += school.getAbundance() * prey.getBiomass() / preyedBiomass;
                }
            }
        }
    }

    @Override
    public void write(float time) {

        int nSpec = getConfiguration().getNSpecies() + getNBkgSpecies();
//        double[][] sum = new double[getNSpecies()][];
//        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
//            sum[iSpec] = new double[nDietStage[iSpec]];
//        }

        // Write the step in the file
        int iprey = 0;
        int ipred = 0;

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        ArrayFloat.D3 arrOut = new ArrayFloat.D3(1, this.nPred, this.nPreys);

        arrTime.set(0, time);
        int index = this.getTimeIndex();
        try {
            Variable tvar = this.getNc().findVariable("time");
            getNc().write(tvar, new int[]{index}, arrTime);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            int nStagePred = dietOutputStage.getNStage(iSpec);
            for (int st = 0; st < nStagePred; st++) {
                iprey = 0;
                for (int i = 0; i < nSpec; i++) {
                    int nStagePrey = dietOutputStage.getNStage(i);
                    for (int s = 0; s < nStagePrey; s++) {
                        arrOut.set(0, ipred, iprey, (float) (100.d * diet[iSpec][st][i][s] / abundanceStage[iSpec][st]));
                        iprey++;
                    }  // end of Prey Stage loop
                }   // end of loop on species prey

                for (int j = nSpec; j < (nSpec + getConfiguration().getNRscSpecies()); j++) {
                    arrOut.set(0, ipred, iprey, (float) (100.d * diet[iSpec][st][j][0] / abundanceStage[iSpec][st]));
                    iprey++;
                }  // end of loop of resources as preys
                ipred++;
            }  // end of predator stage loop
        }  // end of predator species loop

        try {
            Variable outvar = this.getNc().findVariable(this.getVarname());
            getNc().write(outvar, new int[]{index, 0, 0}, arrOut);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.incrementIndex();


    }

    @Override
    public void init() {

        // Init diet output stage
        dietOutputStage = new SchoolStage("output.diet.stage");
        dietOutputStage.init();
        super.init();

    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    @Override
    void init_nc_dims_coords() {

        int nSpec = this.getNSpecies() + this.getNBkgSpecies();

        this.nPred = 0;
        List<String> listPred = new ArrayList<>();
        List<String> listPrey = new ArrayList<>();
        // Init the predator coordinates
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            String name = getISpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStage = dietOutputStage.getNStage(iSpec);
            for (int iStage = 0; iStage < nStage; iStage++) {
                String outname;
                if (nStage == 1) {
                    outname = name;    // Name predators
                } else {
                    if (iStage == 0) {
                        outname = name + " < " + threshold[iStage];    // Name predators
                    } else {
                        outname = name + " >=" + threshold[iStage - 1];   // Name predators
                    }
                }
                listPred.add(outname);
                listPrey.add(outname);
                this.nPred++;
                this.nPreys++;
            }
        }

        Dimension predDim = getBNc().addDimension("pred_index", this.nPred);
        Variable.Builder<?> predvarBuilder = getBNc().addVariable("pred_index", DataType.INT, predDim.getName());
        int cpt = 0;
        for (String name : listPred) {
            predvarBuilder.addAttribute(new Attribute(String.format("pred_index%d", cpt), name));
            cpt++;
        }

        for (int j = nSpec; j < (nSpec + getConfiguration().getNRscSpecies()); j++) {
            listPrey.add(this.getConfiguration().getResourceSpecies(j - nSpec).getName());
            this.nPreys++;
        }

        Dimension preyDim = getBNc().addDimension("prey_index", this.nPreys);
        Variable.Builder<?> preyvarBuilder = getBNc().addVariable("prey_index", DataType.INT, preyDim.getName());
        cpt = 0;
        for (String name : listPrey) {
            preyvarBuilder.addAttribute(new Attribute(String.format("prey_index%d", cpt), name));
            cpt++;
        }

        this.setDims(new ArrayList<>(Arrays.asList(this.getTimeDim(), predDim, preyDim)));

    }

    @Override
    public void write_nc_coords() {

    }

    @Override
    String getFilename() {
        // Create parent directory
        StringBuilder filename = this.initFileName();
        filename.append("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_dietMatrix_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "% of prey species (in rows) in the diet of predator species (in col)";
    }

    @Override
    String getUnits() {
        return "%";
    }

    @Override
    String getVarname() {
        return ("diet");
    }

}
