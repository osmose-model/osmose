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

    private double[] c_m;

    private double[] m0, m1;
    
    private double[] eta;

    private final TempFunction temp_function;

    private final OxygenFunction oxygen_function;
    /**
     * Parameters for the rho function.
     */
    private double[] r;
    private double[] larvaePredationRateBioen;
    
    private double[] assimilation;

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
        int cpt;
        // Redundant with the beta of the BioenPredationMortality class.
        int nSpecies = this.getNSpecies();
        
        // Recovers the eta parameters for focal + background species
        cpt = 0;
        eta = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            key = String.format("species.bioen.maturity.eta.sp%d", i);
            eta[cpt] = this.getConfiguration().getDouble(key);
            cpt++;
        }
        
        // Recovers the r parameters for focal + background species
        cpt = 0;
        r = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            key = String.format("species.bioen.maturity.r.sp%d", i);
            r[cpt] = this.getConfiguration().getDouble(key);
            cpt++;
        }

        // Recovers the m0 parameters for focal + background species
        cpt = 0;
        m0 = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            key = String.format("species.bioen.maturity.m0.sp%d", i);
            m0[cpt] = this.getConfiguration().getDouble(key);   // barrier.n: conversion from mm to cm
            cpt++;
        }

        // Recovers the m1 parameters for focal + background species
        m1 = new double[nSpecies];
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            key = String.format("species.bioen.maturity.m1.sp%d", i);
            m1[cpt] = this.getConfiguration().getDouble(key);  // barrier.n: conversion from mm to cm
            cpt++;
        }

        // Recovers the c_m parameters for focal + background species
        c_m = new double[nSpecies];
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            key = String.format("species.bioen.maint.energy.c_m.sp%d", i);
            c_m[cpt] = this.getConfiguration().getDouble(key);
            cpt++;
        }

        // Recovers the larvae factor of ingestion for focal + background species
        cpt = 0;
        larvaePredationRateBioen = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            key = String.format("predation.coef.ingestion.rate.max.larvae.bioen.sp%d", i);
            larvaePredationRateBioen[cpt] = this.getConfiguration().getDouble(key);
            cpt++;
        }
        
        // Recovers the assimilation parameter  for focal + background species
        assimilation = new double[nSpecies];
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            key = String.format("species.bioen.assimilation.sp%d", i);
            assimilation[cpt] = this.getConfiguration().getDouble(key);
            cpt++;
        }
        
    }

    /**
     * Runs all the steps of the bioenergetic module.
     */
    @Override
    public void run() {
        
        // Updates the temperature and oxygen variables. 
        this.temp_function.run();
        this.oxygen_function.run();

        // Loop over all the alive schools
        for (School school : getSchoolSet().getAliveSchools()) {
            this.getEgross(school);   // computes E_gross, stored in the attribute.
            this.getMaintenance(school);   // computes E_maintanance

            school.updateIngestionTot(school.getIngestion(), school.getInstantaneousAbundance());

            try {
                this.getMaturation(school);   // computes maturation properties for the species.
            } catch (Exception ex) {
                Logger.getLogger(EnergyBudget.class.getName()).log(Level.SEVERE, null, ex);
            }

            school.setENet(school.getEGross() - school.getEMaint());
            this.computeEnetFaced(school);
            
            try {
                this.getRho(school);   // computes the rho function
            } catch (Exception ex) {
                Logger.getLogger(EnergyBudget.class.getName()).log(Level.SEVERE, null, ex);
            }

            this.getDw(school);   // computes E_growth (somatic growth)
            this.getDg(school);   // computes the increase in gonadic weight
        }
    }

    /**
     * Computes the maintenance coefficient. Equation 5
     *
     * @param school
     * @return
     */
    public void getMaintenance(School school) {

        int ispec = school.getSpeciesIndex();

        // computes the mantenance flow for one fish of the school for the current time step
        // barrier.n: weight is converted into g.
        double output = this.c_m[ispec] * Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * temp_function.get_Arrhenius(school);
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
    public void getEgross(School school) {
        int ispec = school.getSpeciesIndex();
        school.setEGross(school.getIngestion() * this.assimilation[ispec] * temp_function.getPhiT(school) * oxygen_function.getFO2(school));
    }

    /**
     * Determines the maturity state. Equation 8
     *
     * @param school
     * @return
     */
    public int getMaturation(School school) throws Exception {

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
        double llim = m0_temp + m1_temp * age ;   // computation of a maturity

        int output = (length >= llim) ? 1 : 0;
        if (output == 1) {
            school.setAgeMat(age);
            school.setSizeMat(length);
            school.setIsMature(true);
        }

        return output;
        
    }

    public void computeEnetFaced(School school) {
        int ispec = school.getSpeciesIndex();
        int nStepYear = this.getConfiguration().getNStepYear();
        
        double output;
        if (school.getAgeDt() < school.getSpecies().getFirstFeedingAgeDt()) {
            // No feeding.
            output = 0;
        } else if (school.getAgeDt() == school.getSpecies().getFirstFeedingAgeDt()) {
            // First speeding (age = 1 dt)
            output = school.getENet()/larvaePredationRateBioen[ispec]*nStepYear / school.getInstantaneousAbundance() * 1e6f / (Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()));
        } else if ((school.getAgeDt() > school.getSpecies().getFirstFeedingAgeDt()) && (school.getAgeDt() < school.getSpecies().getLarvaeThresDt())) {
            // Next feedings as larvae and post-larvae
            double enet = school.getENet() / larvaePredationRateBioen[ispec] * nStepYear / school.getInstantaneousAbundance() * 1e6f / (Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()));
            output = (enet + school.get_enet_faced() * school.getAgeDt()) / (school.getAgeDt() + 1);
        } else { // Next feeding as juvenile and adult
            double enet = school.getENet() * nStepYear / school.getInstantaneousAbundance() * 1e6f / (Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()));
            output = (enet + school.get_enet_faced() * school.getAgeDt()) / (school.getAgeDt() + 1);
        }
        school.set_enet_faced(output);
    }

    /**
     * Returns the somatic weight increment (Equation 11).
     *
     * @param school
     * @return
     */
    public void getDw(School school) {

        // computes the trend in structure weight dw/dt
        // note: dw should be in ton
        double dgrowth = (school.getENet() > 0) ? (school.getENet() * (1-school.getRho())) : 0;

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
    public void getDg(School school) {

        double output = 0;
        double enet = school.getENet();
        double rho = school.getRho();
        if ((enet > 0) && school.isAlive()) {
            output = rho * enet;
            output /= school.getInstantaneousAbundance();
            school.incrementGonadWeight((float) output);
        }
    }

    /**
     * Returns the proportion of net energy allocated to somatic growth
     * (equation 10').
     *
     * @param school
     * @throws java.lang.Exception
     */
    
    public void getRho(School school) throws Exception {
        int ispec = school.getSpeciesIndex();

        String key = "r";
        double r_temp = school.existsTrait(key) ? school.getTrait(key) : r[ispec];
        double etaSpecies = eta[ispec];

        // If the organism is imature, all the net energy goes to the somatic growth.
        // else, only a 1-rho fraction goes to somatic growth
        double rho = (!school.isMature()) ? 0 : r_temp / (etaSpecies * school.get_enet_faced()) * Math.pow(school.getWeight() * 1e6f, 1 - school.getBetaBioen());
        rho = ((rho < 0) ? 0 : rho); //0 if rho<0
        rho = ((rho > 1) ? 1 : rho); //1 if rho>1

        school.setRho(rho);
    }


}
