/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
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
package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.stage.DietOutputStage;
import fr.ird.osmose.stage.IStage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;

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

    private IStage dietOutputStage;

    public BiomassDietStageOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();

        nColumns = 0;
        // Sum-up diet stages
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            nColumns += dietOutputStage.getNStage(iSpec);
        }
        nColumns += getConfiguration().getNPlankton();

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
        for (School school : getSchoolSet().getPresentSchools()) {
            biomassStage[school.getSpeciesIndex()][dietOutputStage.getStage(school)] += school.getBiomass();
        }
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        for (int i = nSpec; i < nPrey; i++) {
            int iPlankton = i - nSpec;
            biomassStage[i][0] += getTotalBiomass(iPlankton);
        }
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        biomassStage = new double[nPrey][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            biomassStage[iSpec] = new double[dietOutputStage.getNStage(iSpec)];
        }
        for (int i = nSpec; i < nPrey; i++) {
            // we consider just 1 stage per plankton group
            biomassStage[i] = new double[1];
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
        int k = 0;
        int nSpec = getNSpecies();
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            for (int s = 0; s < dietOutputStage.getNStage(iSpec); s++) {
                biomass[k] = biomassStage[iSpec][s] / nsteps;
                k++;
            }
        }
        for (int j = nSpec; j < (nSpec + getConfiguration().getNPlankton()); j++) {
            biomass[k] = biomassStage[j][0] / nsteps;
            k++;
        }
        writeVariable(time, biomass);
    }

    /**
     * Gets the total biomass of the plankton group over the grid.
     *
     * @return the cumulated biomass over the domain in tonne
     */
    private double getTotalBiomass(int iPlankton) {
        double biomTot = 0.d;
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                biomTot += getSimulation().getForcing().getBiomass(iPlankton, cell);
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

        Dimension classDim = getNc().addDimension("class_prey", nColumns);
        StringBuilder bld = new StringBuilder();
        
        getNc().addVariable("class_prey", DataType.FLOAT, new Dimension[]{classDim});
        this.setDims(new Dimension[]{getTimeDim(), classDim});
        
        int nSpec = getNSpecies();
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
                getNc().addVariableAttribute("class_prey", attrname, outname);
                k++;
            }
        }

    }

    @Override
    public void write_nc_coords() {
        try {

            // Writes variable trait (trait names) and species (species names)
            ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.nColumns);

            for (int i = 0; i < this.nColumns; i++) {
                arrClass.set(i, i);
            }
            getNc().write("class_prey", arrClass);

        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbundanceOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
