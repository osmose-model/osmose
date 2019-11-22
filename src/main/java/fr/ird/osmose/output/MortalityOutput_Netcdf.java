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
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class MortalityOutput_Netcdf extends AbstractOutput_Netcdf {

    // IO
    private FileOutputStream[] fos;
    private PrintWriter[] prw;
    private int recordFrequency;
    private final Species species;

    /*
     * Mortality rates Stages: 1. eggs & larvae 2. Pre-recruits 3. Recruits
     */
    final private int STAGES = 3;
    final private int EGG = 0;
    final private int PRE_RECRUIT = 1;
    final private int RECRUIT = 2;
    /*
     * Mortality rates array [SPECIES][CAUSES][STAGES]
     */
    private double[][] mortalityRates;
    /*
     * Abundance per stages [SPECIES][STAGES]
     */
    private double[] abundanceStage;
    /**
     * Age of recruitment (expressed in number of time steps) [SPECIES]
     */
    private int recruitmentAge;
    /**
     * Size of recruitment (expressed in centimetre) [SPECIES]
     */
    private float recruitmentSize;
    /**
     * CSV separator
     */
    private final String separator;

    /**
     * Stage of the schools at the beginning of the time step.
     */
    private int[] stage_init;

    public MortalityOutput_Netcdf(int rank, Species species) {
        super(rank);
        this.species = species;
        separator = getConfiguration().getOutputSeparator();
    }

    public void init() {
        // Get the age of recruitment
        int iSpecies = this.species.getIndex();
        if (!getConfiguration().isNull("mortality.fishing.recruitment.age.sp" + iSpecies)) {
            float age = getConfiguration().getFloat("mortality.fishing.recruitment.age.sp" + iSpecies);
            recruitmentAge = Math.round(age * getConfiguration().getNStepYear());
            recruitmentSize = -1.f;
        } else if (!getConfiguration().isNull("mortality.fishing.recruitment.size.sp" + iSpecies)) {
            recruitmentSize = getConfiguration().getFloat("mortality.fishing.recruitment.size.sp" + iSpecies);
            recruitmentAge = -1;
        } else {
            warning("Could not find parameters mortality.fishing.recruitment.age/size.sp{0}. Osmose assumes it is one year.", new Object[]{iSpecies});
            recruitmentAge = getConfiguration().getNStepYear();
            recruitmentSize = -1.f;
        }
        super.init();
    }

    @Override
    public void initStep() {

        // Reset the nDead array used to compute the mortality rates of current
        // time step
        abundanceStage = new double[STAGES];
        List<School> school_list = this.getSchoolSet().getSchoolsAll(this.species);

        stage_init = new int[school_list.size()];

        int cpt = 0;

        // save abundance at the beginning of the time step
        for (School school : school_list) {
            int stage = this.getStage(school);
            stage_init[cpt] = stage;
            abundanceStage[stage] += school.getAbundance();
            cpt += 1;
        }
    }

    @Override
    public void reset() {

        // Reset mortality rates
        mortalityRates = new double[MortalityCause.values().length][STAGES];
    }

    @Override
    public void update() {
        int iStage, cpt = 0;
        int nCause = MortalityCause.values().length;
        double[][] nDead = new double[nCause][STAGES];
        for (School school : getSchoolSet().getSchoolsAll(this.species)) {
            //iStage = getStage(school);
            iStage = stage_init[cpt];
            // Update number of deads
            for (MortalityCause cause : MortalityCause.values()) {
                nDead[cause.index][iStage] += school.getNdead(cause);
            }
            cpt += 1;
        }
        // Cumulate the mortality rates
        for (iStage = 0; iStage < STAGES; iStage++) {
            if (abundanceStage[iStage] > 0) {
                double nDeadTot = 0;
                for (int iDeath = 0; iDeath < nCause; iDeath++) {
                    nDeadTot += nDead[iDeath][iStage];
                }
                double Ftot = Math.log(abundanceStage[iStage] / (abundanceStage[iStage] - nDeadTot));

                if (Ftot != 0) {
                    for (int iDeath = 0; iDeath < nCause; iDeath++) {
                        mortalityRates[iDeath][iStage] += Ftot * nDead[iDeath][iStage] / ((1 - Math.exp(-Ftot)) * abundanceStage[iStage]);
                    }
                }
            }
        }
    }

    @Override
    public void write(float time) {

        for (int iDeath = 0; iDeath < MortalityCause.values().length; iDeath++) {
            for (int iStage = 0; iStage < STAGES; iStage++) {
                if (iDeath == MortalityCause.ADDITIONAL.index && iStage == EGG) {
                    // instantenous mortality rate for eggs natural mortality 
                    mortalityRates[iDeath][iStage] /= recordFrequency;
                }
            }
        }

        this.writeVariable(time, mortalityRates);

    }

    private int getStage(School school) {

        int iStage;

        if (this.getConfiguration().isBioenEnabled()) {

            if (school.getAgeDt() == 0) {
                // Eggss
                iStage = EGG;

            } else if (!school.isMature()) {
                // Pre-recruits
                iStage = PRE_RECRUIT;

            } else {
                // Recruits
                iStage = RECRUIT;
            }

        } else {

            if (school.getAgeDt() == 0) {
                // Eggss
                iStage = EGG;

            } else if (school.getAgeDt() < recruitmentAge
                    || school.getLength() < recruitmentSize) {
                // Pre-recruits
                iStage = PRE_RECRUIT;

            } else {
                // Recruits
                iStage = RECRUIT;
            }
        }
        return iStage;
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append("Mortality");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_mortalityRate-");
        filename.append(this.species.getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return ("Mortality rate"); //To change body of generated methods, choose Tools | Templates.
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

        Dimension stageDim = getNc().addDimension(null, "stage", STAGES);
        Variable stagevar = getNc().addVariable(null, "stage", DataType.INT, stageDim.getFullName());
        String[] stages_str = new String[]{"Egg", "Pre-recruit", "Recruit"};
        for (int i = 0; i < STAGES; i++) {
            String attrname = String.format("stage%d", i);
            String attval = stages_str[i];
            stagevar.addAttribute(new Attribute(attrname, attval));
        }

        Dimension mortDim = getNc().addDimension(null, "mortality_cause", MortalityCause.values().length);
        Variable mortvar = getNc().addVariable(null, "mortality_cause", DataType.INT, mortDim.getFullName());
        for (int i = 0; i < MortalityCause.values().length; i++) {
            String attrname = String.format("mortality_cause%d", i);
            String attval = MortalityCause.values()[i].name();
            mortvar.addAttribute(new Attribute(attrname, attval));
        }

        //this.createSpeciesAttr();
        //outDims = new Dimension[]{timeDim, speciesDim};
        this.setDims(new ArrayList<>(Arrays.asList(this.getTimeDim(), mortDim, stageDim)));
    }

    @Override
    public void write_nc_coords() {

        // Writes variable trait (trait names) and species (species names)
        ArrayInt.D1 arrMort = new ArrayInt.D1(MortalityCause.values().length);

        for (int i = 0; i < MortalityCause.values().length; i++) {
            arrMort.set(i, i);
        }

        Variable mortvar = this.getNc().findVariable("mortality_cause");
        try {
            getNc().write(mortvar, arrMort);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MortalityOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Writes variable trait (trait names) and species (species names)
        ArrayInt.D1 arrStage = new ArrayInt.D1(STAGES);

        for (int i = 0; i < STAGES; i++) {
            arrStage.set(i, i);
        }
        Variable stagevar = this.getNc().findVariable("stage");
        try {
            getNc().write(stagevar, arrStage);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MortalityOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
