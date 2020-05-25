/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    private final OxygenFunction oxygen_function;
    /**
     * Parameters for the kappa function.
     */
    private double[] r;
    private double[] Imax;

    public EnergyBudget(int rank) throws IOException {

        super(rank);
        temp_function = new TempFunction(rank);
        temp_function.init();
        

        oxygen_function = new OxygenFunction(rank);
        oxygen_function.init();

    }
    

    @Override
    public void init() {

        String key;

        // Redundant with the beta of the BioenPredationMortality class.
        int nBack = this.getNBkgSpecies();
        int nspec = this.getNSpecies();

        // Recovers the beta coefficient for focal + background species
        r = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.r.sp%d", i);
            r[i] = this.getConfiguration().getDouble(key);
        }

        // Recovers the beta coefficient for focal + background species
        m0 = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.m0.sp%d", i);
            m0[i] = this.getConfiguration().getDouble(key);   // barrier.n: conversion from mm to cm
        }

        // Recovers the beta coefficient for focal + background species
        m1 = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maturity.m1.sp%d", i);
            m1[i] = this.getConfiguration().getDouble(key);  // barrier.n: conversion from mm to cm
        }

        // Recovers the beta coefficient for focal + background species
        csmr = new double[nspec];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("bioen.maint.energy.csmr.sp%d", i);
            csmr[i] = this.getConfiguration().getDouble(key);
        }

        // Recovers the beta coefficient for focal + background species
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
            school.updateIngestionTot(school.getIngestion(),school.getInstantaneousAbundance());

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
        double output = this.csmr[ispec] * Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * temp_function.get_Arrhenius(school);
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
        school.setEGross(school.getIngestion() * temp_function.get_phiT(school) * oxygen_function.compute_fO2(school));
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
        double kappa = (!school.isMature()) ? 1 : 1 - (r_temp / (imax_temp - csmr[ispec])) * Math.pow(school.getWeight() * 1e6f, 1 - school.getBetaBioen()); //Function in two parts according to maturity state
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
