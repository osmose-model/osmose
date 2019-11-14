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
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nbarrier
 */
public class EnergyBudget extends AbstractProcess {

    private double[] csmr;

    private double[] m0, m1;

    private final TempFunction temp_function;

    /**
     * Parameters for the kappa function.
     */
    private double[] r;
    private double[] Imax;

    public EnergyBudget(int rank) throws IOException {

        super(rank);
        temp_function = new TempFunction(rank);
        temp_function.init();

    }

    @Override
    public void init() {

        String key;

        // Redundant with the alpha of the BioenPredationMortality class.
        int nBack = this.getNBkgSpecies();
        int nspec = this.getNSpecies();

        // Recovers the alpha coefficient for focal + background species
        r = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.r.sp%d", i);
            r[i] = this.getConfiguration().getDouble(key);
        }

        // Recovers the alpha coefficient for focal + background species
        m0 = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.m0.sp%d", i);
            m0[i] = this.getConfiguration().getDouble(key);   // barrier.n: conversion from mm to cm
        }

        // Recovers the alpha coefficient for focal + background species
        m1 = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.m1.sp%d", i);
            m1[i] = this.getConfiguration().getDouble(key);  // barrier.n: conversion from mm to cm
        }

        // Recovers the alpha coefficient for focal + background species
        csmr = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maint.energy.csmr.sp%d", i);
            csmr[i] = this.getConfiguration().getDouble(key);
        }

        // Recovers the alpha coefficient for focal + background species
        Imax = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("predation.ingestion.rate.max.bioen.sp%d", i);
            Imax[i] = this.getConfiguration().getDouble(key);
        }
    }

    /**
     * Runs all the steps of the bioenergetic module.
     */
    @Override
    public void run() {

        //System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        // Loop over all the alive schools
        for (School school : getSchoolSet().getAliveSchools()) {
            this.get_egross(school);   // computes E_gross, stored in the attribute.
            this.get_maintenance(school);   // computes E_maintanance

            try {
                this.get_maturation(school);   // computes maturation properties for the species.
            } catch (Exception ex) {
                Logger.getLogger(EnergyBudget.class.getName()).log(Level.SEVERE, null, ex);
            }

            school.setENet(school.getEGross() - school.getEMaint());
            try {
                this.get_kappa(school);   // computes the kappa function
            } catch (Exception ex) {
                Logger.getLogger(EnergyBudget.class.getName()).log(Level.SEVERE, null, ex);
            }

            this.get_dw(school);   // computes E_growth (somatic growth)
            this.get_dg(school);   // computes the increase in gonadic weight
        }
    }

    /**
     * Computes the maintenance coefficient. Equation 5
     *
     * @param school
     * @return
     */
    public void get_maintenance(School school) {

        int ispec = school.getSpeciesIndex();

        // computes the mantenance flow for one fish of the school for the current time step
        // barrier.n: weight is converted into g.
        double output = this.csmr[ispec] * Math.pow(school.getWeight() * 1e6f, school.getAlphaBioen()) * temp_function.get_Arrhenius(school);
        output /= this.getConfiguration().getNStepYear();   // if csmr is in year^-1, convert back into time step value

        // multiply the maintenance flow by the number of fish in the school
        // barrier.n: converted back into ton
        output *= school.getInstantaneousAbundance() * 1e-6f;
        school.setEMaint(output);

    }

    /**
     * Returns the gross energy. Equation 3
     *
     * @param school
     * @return
     */
    public void get_egross(School school) {
        school.setEGross(school.getIngestion() * temp_function.get_phiT(school));
        //System.out.println(school.getIngestion() + ", " + temp_function.get_phiT(school));
    }

    /**
     * Determines the maturity state. Equation 8
     *
     * @param school
     * @return
     */
    public int get_maturation(School school) throws Exception {

        // If the school is mature, nothing is done and returns 1
        if (school.isMature()) {
            return 1;
        }

        int ispec = school.getSpeciesIndex();

        String key = "m0";
        double m0_temp = school.existsTrait(key) ? school.getTrait(key) : m0[ispec];

        key = "m1";
        double m1_temp = school.existsTrait(key) ? school.getTrait(key) : m1[ispec];

        // If the school is not mature yet, maturation is computed following equation 8
        double age = school.getAge();  // returns the age in years
        double length = school.getLength();   // warning: length in cm.
        double llim = m0_temp * age + m1_temp;   // computation of a maturity

        int output = (length >= llim) ? 1 : 0;
        if (output == 1) {
            school.setAgeMat(age);
            school.setSizeMat(length);
            school.setIsMature(true);
        }

        return output;

    }

    /**
     * Returns the somatic weight increment (Equation 11).
     *
     * @param school
     * @return
     */
    public void get_dw(School school) {

        // computes the trend in structure weight dw/dt
        // note: dw should be in ton
        double dgrowth = (school.getENet() > 0) ? (school.getENet() * school.getKappa()) : 0;

        if (school.isAlive()) {
            dgrowth /= school.getInstantaneousAbundance();

            // increments the weight
            school.incrementWeight((float) dgrowth);
        }
    }

    /**
     * Returns the gonadic weight increment (Equation 12). In this function,
     * only positive increments of gonad weights (Enet > 0) are considered.
     * Gonad removal if (Enet < 0) is implemented on starvation mortality.
     *
     * @param school
     */
    public void get_dg(School school) {

        double output = 0;
        double enet = school.getENet();
        double kappa = school.getKappa();
        if ((enet > 0) && school.isAlive()) {
            output = (1 - kappa) * enet;
            output /= school.getInstantaneousAbundance();
            school.incrementGonadWeight((float) output);
        }
    }

    /**
     * Returns the proportion of net energy allocated to somatic growth
     * (equation 10').
     *
     * @param school
     */
    public void get_kappa(School school) throws Exception {
        int ispec = school.getSpeciesIndex();

        String key = "r";
        double r_temp = school.existsTrait(key) ? school.getTrait(key) : r[ispec];

        key = "imax";
        double imax_temp = school.existsTrait(key) ? school.getTrait(key) : Imax[ispec];

        // If the organism is imature, all the net energy goes to the somatic growth.
        // else, only a kappa fraction goes to somatic growth
        double kappa = (!school.isMature()) ? 1 : 1 - (r_temp / (imax_temp - csmr[ispec])) * Math.pow(school.getWeight() * 1e6f, 1 - school.getAlphaBioen()); //Function in two parts according to maturity state
        kappa = ((kappa < 0) ? 0 : kappa); //0 if kappa<0
        kappa = ((kappa > 1) ? 1 : kappa); //1 if kappa>1

        school.setKappa(kappa);
    }

//    public void get_kappa(School school) {
//        // int ispec = school.getSpeciesIndex();
//        // If the organism is imature, all the net energy goes to the somatic growth.
//        // else, only a kappa fraction goes to somatic growth
//        double kappa = (!school.isMature()) ? 1 : 0; //Function in two parts according to maturity state
//        
//        
//        school.setKappa(kappa);
//    }
}
