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

import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.distribution.OutputDistribution;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class MortalitySpeciesOutput_Netcdf extends AbstractDistribOutput_Netcdf {

    private final Species species;
    /*
     * Abundance per stages [STAGES]
     */
    private double[] abundanceStage;

    // mortality rates por souces and per stages
    private double[][] mortalityRates;

    public MortalitySpeciesOutput_Netcdf(int rank, Species species, OutputDistribution distrib) {
        super(rank, distrib);
        this.species = species;
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append("Mortality");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_mortalityRateDistribBy");
        filename.append(getType().toString());
        filename.append("-");
        filename.append(species.getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Predation (Mpred), Starvation (Mstarv), Additional mortality (Madd), Fishing (F) & Out-of-domain (Z) mortality rates per time step of saving and per size class. Z is the total mortality for migratory fish outside the simulation grid. To get annual mortality rates, sum the mortality rates within one year.";
    }

    @Override
    public void reset() {
        mortalityRates = new double[MortalityCause.values().length][getNClass()];
    }

    @Override
    public void update() {

        int iClass;
        int nCause = MortalityCause.values().length;
        double[][] nDead = new double[nCause][getNClass()];
        // Loop on all the schools to be sure we don't discard dead schools
        for (School school : getSchoolSet().getSchools()) {
            if (school.getFileSpeciesIndex() != species.getFileSpeciesIndex()) {
                continue;
            }
            iClass = getClass(school);
            // Update number of deads
            for (MortalityCause cause : MortalityCause.values()) {
                nDead[cause.index][iClass] += school.getNdead(cause);
            }
        }

        // Cumulate the mortality rates
        for (iClass = 0; iClass < getNClass(); iClass++) {
            if (abundanceStage[iClass] > 0) {
                double nDeadTot = 0;
                for (int iDeath = 0; iDeath < nCause; iDeath++) {
                    nDeadTot += nDead[iDeath][iClass];
                }
                double Z = Math.log(abundanceStage[iClass] / (abundanceStage[iClass] - nDeadTot));
                for (int iDeath = 0; iDeath < nCause; iDeath++) {
                    mortalityRates[iDeath][iClass] += Z * nDead[iDeath][iClass] / nDeadTot;
                }
            }
        }
    }

    @Override
    public void write(float time) {

        int nCause = MortalityCause.values().length;
        double[][] array = new double[getNClass()][nCause];
        for (int iClass = 0; iClass < getNClass(); iClass++) {
            // Mortality rates
            for (int iDeath = 0; iDeath < nCause; iDeath++) {
                array[iClass][iDeath] = mortalityRates[iDeath][iClass];
            }
        }

        writeVariable(time, array);

    }

    @Override
    public void initStep() {
        // Reset abundance array
        abundanceStage = new double[getNClass()];

        // save abundance at the beginning of the time step
        for (School school : getSchoolSet().getSchools(species, false)) {
            int iClass = getClass(school);
            if (iClass >= 0) {
                abundanceStage[iClass] += school.getAbundance();
            }
        }
    }

    @Override
    String getUnits() {
        return (""); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return ("mortality"); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    @Override
    void init_nc_dims_coords() {

        Dimension classDim = getBNc().addDimension(this.getDisName(), this.getNClass());
        getBNc().addVariable(this.getDisName(), DataType.FLOAT, classDim.getName());

        Dimension mortDim = getBNc().addDimension("mortality_cause", MortalityCause.values().length);
        Variable.Builder<?> mortvarBuilder = getBNc().addVariable("mortality_cause", DataType.INT, mortDim.getName());
        for (int i = 0; i < MortalityCause.values().length; i++) {
            String attrname = String.format("mortality_cause%d", i);
            String attval = MortalityCause.values()[i].name();
            mortvarBuilder.addAttribute(new Attribute(attrname, attval));
        }

        //this.createSpeciesAttr();
        //outDims = new Dimension[]{timeDim, speciesDim};
        this.setDims(new ArrayList<>(Arrays.asList(this.getTimeDim(), classDim, mortDim)));
    }

    @Override
    public void write_nc_coords() {
        try {

            // Writes variable trait (trait names) and species (species names)
            ArrayInt arrMort = new ArrayInt(new int[] {MortalityCause.values().length}, false);
            Index index = arrMort.getIndex();
            for (int i = 0; i < MortalityCause.values().length; i++) {
                index.set(i);
                arrMort.set(index, i);
            }
            Variable mortvar = this.getNc().findVariable("mortality_cause");
            getNc().write(mortvar, arrMort);

            ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.getNClass());
            arrClass.set(0, 0);
            for (int i = 1; i < this.getNClass(); i++) {
                arrClass.set(i, this.getClassThreshold(i - 1));
            }
            Variable disvar = this.getNc().findVariable(this.getDisName());
            getNc().write(disvar, arrClass);

        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbundanceOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
