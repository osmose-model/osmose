/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;
import java.io.IOException;

/**
 *
 * @author nbarrier
 */
public class EnergyBudget extends AbstractProcess {

    private double[] alpha;
    private double[] csmr;

    private double[] m0, m1;

    private final TempFunction temp_function;

    /**
     * Parameters for the kappa function.
     */
    private double[] r;
    private double[] growth_pot;

    
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
        alpha = new double[nspec + nBack];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("species.alpha.sp%d", i);
            alpha[i] = this.getConfiguration().getDouble(key);
        }

        for (int i = 0; i < nBack; i++) {
            key = String.format("species.alpha.bkg%d", i);
            alpha[i + nspec] = this.getConfiguration().getDouble(key);
        }

        // Recovers the alpha coefficient for focal + background species
        m0 = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.m0.sp%d", i);
            m0[i] = this.getConfiguration().getDouble(key) * 1e-2;   // barrier.n: conversion from mm to cm
        }

        // Recovers the alpha coefficient for focal + background species
        m1 = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.m1.sp%d", i);
            m1[i] = this.getConfiguration().getDouble(key) * 1e-2;  // barrier.n: conversion from mm to cm
        }

        // Recovers the alpha coefficient for focal + background species
        csmr = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maint.energy.csmr.sp%d", i);
            csmr[i] = this.getConfiguration().getDouble(key);
        }
        
        // Recovers the alpha coefficient for focal + background species
        growth_pot = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            double lInf = getConfiguration().getDouble("species.linf.sp" + i);
            growth_pot[i] = r[i] * Math.pow(this.getSpecies(i).getBPower(), 1 - alpha[i]) * Math.pow(lInf, 3 * (1 - alpha[i]));;
        }
    }

    /**
     * Runs all the steps of the bioenergetic module.
     */
    @Override
    public void run() {
        
        // Loop over all the alive schools
        for (School school : getSchoolSet().getAliveSchools()) {
            this.get_egross(school);   // computes E_gross, stored in the attribute.
            this.get_maintenance(school);   // computes E_maintanance
            this.get_maturation(school);   // computes maturation properties for the species.
            school.setENet(school.getEGross() - school.getEMaint());
            this.get_kappa(school);   // computes the kappa function
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
        double output = this.csmr[ispec] * Math.pow(school.getBiomass(), alpha[ispec]) * temp_function.get_Arrhenius(school);
        output /= this.getConfiguration().getNStepYear();   // if csmr is in year^-1, convert back into time step value
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
    }

    /**
     * Determines the maturity state. Equation 8
     *
     * @param school
     * @return
     */
    public int get_maturation(School school) {

        // If the school is mature, nothing is done and returns 1
        if (school.isMature()) {
            return 1;
        }

        int ispec = school.getSpeciesIndex();
        // If the school is not mature yet, maturation is computed following equation 8
        double age = school.getAge();  // returns the age in years
        double length = school.getLength();   // warning: length in cm.
        double llim = this.m0[ispec] * age + this.m1[ispec];   // computation of a maturity

        int output = (length >= llim) ? 1 : 0;
        if (output == 1) {
            school.setAgeMat(age);
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
        // increments the weight
        school.incrementWeight((float) dgrowth);
    }

    /**
     * Returns the gonadic weight increment (Equation 12).
     * In this function, only positive increments of gonad weights 
     * (Enet > 0) are considered. Gonad removal if (Enet < 0)
     * is implemented on starvation mortality.
     *
     * @param school
     * @return
     */
    public void get_dg(School school) {
        
        double output = 0;
        double enet = school.getENet();
        double kappa = school.getKappa();
        if (enet > 0) {
            output = (1 - kappa) * enet;
            school.incrementGonadWeight((float) output);
        }
    }

    /**
     * Returns the proportion of net energy allocated to somatic growth
     * (equation 10').
     *
     * @param school
     * @return
     */
    public void get_kappa(School school) {
        int ispec = school.getSpeciesIndex();
        // If the organism is imature, all the net energy goes to the somatic growth.
        // else, only a kappa fraction goes to somatic growth
        double kappa = (!school.isMature()) ? 1 : 1 - (r[ispec] / growth_pot[ispec]) * Math.pow(school.getWeight(), 1 - alpha[ispec]); //Function in two parts according to maturity state
        school.setKappa(kappa);
    }

}
