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

import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.distribution.AbstractDistribution;
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

    public MortalitySpeciesOutput_Netcdf(int rank, Species species, AbstractDistribution distrib) {
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
        return "Predation (Mpred), Starvation (Mstarv), Other Natural mortality (Mnat), Fishing (F) & Out-of-domain (Z) mortality rates per time step of saving and per size class. Z is the total mortality for migratory fish outside the simulation grid. To get annual mortality rates, sum the mortality rates within one year.";
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
            if (school.getSpeciesIndex() != species.getIndex()) {
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

        Dimension classDim = getNc().addDimension(this.getDisName(), this.getNClass());
        getNc().addVariable(this.getDisName(), DataType.FLOAT, new Dimension[]{classDim});

        Dimension mortDim = getNc().addDimension("mortality_cause", MortalityCause.values().length);
        getNc().addVariable("mortality_cause", DataType.INT, new Dimension[]{mortDim});
        for (int i = 0; i < MortalityCause.values().length; i++) {
            String attrname = String.format("mortality_cause%d", i);
            String attval = MortalityCause.values()[i].name();
            getNc().addVariableAttribute("mortality_cause", attrname, attval);
        }

        //this.createSpeciesAttr();
        //outDims = new Dimension[]{timeDim, speciesDim};
        this.setDims(new Dimension[]{this.getTimeDim(), classDim, mortDim});
    }

    @Override
    public void write_nc_coords() {
        try {

            // Writes variable trait (trait names) and species (species names)
            ArrayInt.D1 arrMort = new ArrayInt.D1(MortalityCause.values().length);
            for (int i = 0; i < MortalityCause.values().length; i++) {
                arrMort.set(i, i);
            }
            getNc().write("mortality_cause", arrMort);

            ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.getNClass());
            for (int i = 0; i < this.getNClass(); i++) {
                arrClass.set(i, this.getClassThreshold(i));
            }
            getNc().write(this.getDisName(), arrClass);

        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbundanceOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
