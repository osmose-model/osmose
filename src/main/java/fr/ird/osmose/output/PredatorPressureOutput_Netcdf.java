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
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.Prey;
import fr.ird.osmose.stage.DietOutputStage;
import fr.ird.osmose.stage.IStage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
public class PredatorPressureOutput_Netcdf extends AbstractOutput_Netcdf {

    // IO
    private FileOutputStream fos;
    private PrintWriter prw;

    //
    private double[][][][] predatorPressure;
    // Diet output stage
    private IStage dietOutputStage;
    private int nPreys;
    private int nPred;

    private final String separator;

    public PredatorPressureOutput_Netcdf(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNRscSpecies();
        predatorPressure = new double[nSpec][][][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            int nStage = dietOutputStage.getNStage(iSpec);
            predatorPressure[iSpec] = new double[nStage][][];
            for (int s = 0; s < nStage; s++) {
                predatorPressure[iSpec][s] = new double[nPrey][];
                for (int iPrey = 0; iPrey < nPrey; iPrey++) {
                    if (iPrey < nSpec) {
                        predatorPressure[iSpec][s][iPrey] = new double[dietOutputStage.getNStage(iPrey)];
                    } else {
                        predatorPressure[iSpec][s][iPrey] = new double[1];
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int iSpec = school.getSpeciesIndex();
            int stage = dietOutputStage.getStage(school);
            for (Prey prey : school.getPreys()) {
                predatorPressure[iSpec][stage][prey.getSpeciesIndex()][dietOutputStage.getStage(prey)] += prey.getBiomass();
            }
        }
    }

    @Override
    public void write(float time) {

        // Write the step in the file
        int iprey = 0;
        int ipred = 0;
        int index = this.getTimeIndex();

        ArrayDouble.D1 arrTime = new ArrayDouble.D1(1);
        arrTime.set(0, time);
        ArrayFloat.D3 arrOut = new ArrayFloat.D3(1, this.nPred, this.nPreys);

        int nSpec = getNSpecies();
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            int nStagePred = dietOutputStage.getNStage(iSpec);
            for (int iStage = 0; iStage < nStagePred; iStage++) {
                iprey = 0;
                for (int i = 0; i < nSpec; i++) {
                    int nStage = dietOutputStage.getNStage(i);
                    for (int s = 0; s < nStage; s++) {
                        float val = (float) (predatorPressure[iSpec][iStage][i][s] / this.getRecordFrequency());
                        arrOut.set(0, ipred, iprey, val);
                        iprey++;
                    }  // end of prey  stage
                }  // end of prey species

                for (int j = nSpec; j < (nSpec + getConfiguration().getNRscSpecies()); j++) {
                    arrOut.set(0, ipred, iprey, (float) (predatorPressure[iSpec][iStage][j][0] / this.getRecordFrequency()));
                    iprey++;
                }

                ipred++;

            }  // end of pred stage 
        }  // end of pred species

        try {
            Variable outvar = this.getNc().findVariable(this.getVarname());
            getNc().write(outvar, new int[]{index, 0, 0}, arrOut);
            Variable tvar = this.getNc().findVariable("time");
            getNc().write(tvar, new int[]{index, 0, 0}, arrTime);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.incrementIndex();

    }

    @Override
    public void init() {

        // Init diet output stage
        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();
        super.init();

    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_predatorPressure_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return (filename.toString());
    }

    @Override
    public String getDescription() {
        return ("Biomass of prey species (in tons per time step of saving, in rows) eaten by a predator species (in col). The last column reports the biomass of prey at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)");
    }

    @Override
    String getUnits() {
        return ("tons"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return ("predator_pressure"); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    @Override
    void init_nc_dims_coords() {

        int nSpec = this.getNSpecies();

        this.nPred = 0;
        List<String> listPred = new ArrayList<>();
        List<String> listPrey = new ArrayList<>();
        // Init the predator coordinates
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            String name = getSpecies(iSpec).getName();
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

        Dimension predDim = getNc().addDimension(null, "pred_index", this.nPred);
        Variable predvar = getNc().addVariable(null, "pred_index", DataType.INT, predDim.getFullName());
        int cpt = 0;
        for (String name : listPred) {
            predvar.addAttribute(new Attribute(String.format("pred_index%d", cpt), name));
            cpt++;
        }

        for (int j = nSpec; j < (nSpec + getConfiguration().getNRscSpecies()); j++) {
            listPrey.add(this.getConfiguration().getResourceSpecies(j - nSpec).getName());
            this.nPreys++;
        }

        Dimension preyDim = getNc().addDimension(null, "prey_index", this.nPreys);
        Variable preyvar = getNc().addVariable(null, "prey_index", DataType.INT, preyDim.getFullName());
        cpt = 0;
        for (String name : listPrey) {
            preyvar.addAttribute(new Attribute(String.format("prey_index%d", cpt), name));
            cpt++;
        }

        this.setDims(new ArrayList<>(Arrays.asList(this.getTimeDim(), predDim, preyDim)));

    }

    @Override
    public void write_nc_coords() { 
    }
    
}
