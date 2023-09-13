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
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.MortalityCause;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class MortalityOutput_Netcdf extends AbstractOutput_Netcdf {

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
     * Stage of the schools at the beginning of the time step.
     */
    private int[] stage_init;

    public MortalityOutput_Netcdf(int rank, Species species) {
        super(rank);
        this.species = species;
        getConfiguration().getOutputSeparator();
    }

    public void init() {
        // Get the age of recruitment
        int iSpecies = this.species.getFileSpeciesIndex();
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
                    // instantenous mortality rate for eggs additional mortality
                    mortalityRates[iDeath][iStage] /= recordFrequency;
                }
            }
        }

        this.writeVariable(time, mortalityRates);

    }

    private int getStage(School school) {

        int iStage;

        if (this.getConfiguration().isBioenEnabled()) {

            if (school.isEgg()) {
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

            if (school.isEgg()) {
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

        Dimension stageDim = getBNc().addDimension("stage", STAGES);
        Variable.Builder<?> stagevarBuilder = getBNc().addVariable("stage", DataType.INT, stageDim.getName());
        String[] stages_str = new String[]{"Egg", "Pre-recruit", "Recruit"};
        for (int i = 0; i < STAGES; i++) {
            String attrname = String.format("stage%d", i);
            String attval = stages_str[i];
            stagevarBuilder.addAttribute(new Attribute(attrname, attval));
        }

        Dimension mortDim = getBNc().addDimension("mortality_cause", MortalityCause.values().length);
        Variable.Builder<?> mortvarBuilder = getBNc().addVariable("mortality_cause", DataType.INT, mortDim.getName());
        for (int i = 0; i < MortalityCause.values().length; i++) {
            String attrname = String.format("mortality_cause%d", i);
            String attval = MortalityCause.values()[i].name();
            mortvarBuilder.addAttribute(new Attribute(attrname, attval));
        }

        //this.createSpeciesAttr();
        //outDims = new Dimension[]{timeDim, speciesDim};
        this.setDims(new ArrayList<>(Arrays.asList(this.getTimeDim(), mortDim, stageDim)));
    }

    @Override
    public void write_nc_coords() {

        // Writes variable trait (trait names) and species (species names)
        ArrayInt arrMort = new ArrayInt(new int[] {MortalityCause.values().length}, false);
        Index index = arrMort.getIndex();

        for (int i = 0; i < MortalityCause.values().length; i++) {
            index.set(i);
            arrMort.set(index, i);
        }

        Variable mortvar = this.getNc().findVariable("mortality_cause");
        try {
            getNc().write(mortvar, arrMort);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MortalityOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Writes variable trait (trait names) and species (species names)
        ArrayInt arrStage = new ArrayInt(new int[] {STAGES}, false);
        index = arrStage.getIndex();

        for (int i = 0; i < STAGES; i++) {
            index.set(i);
            arrStage.set(index, i);
        }
        Variable stagevar = this.getNc().findVariable("stage");
        try {
            getNc().write(stagevar, arrStage);
        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(MortalityOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
