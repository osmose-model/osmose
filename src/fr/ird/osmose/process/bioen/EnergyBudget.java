/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;

/**
 *
 * @author nbarrier
 */
public class EnergyBudget extends AbstractProcess {
    
    private double tmin, tmax, topt;
    private double a, b, c;
    private double ap, bp, cp;  // a prime, b prime, cprime

    private double[] alpha;
    private double csmr;

    private double m0, m1;

    /**
     * Parameters for the energy maintenance.
     */
    private double c_t1, c_t2;

    /**
     * Parameters for the kappa function.
     */
    private double r, growth_pot;

    PhysicalData temperature_input;

    public EnergyBudget(int rank) {

        super(rank);
        temperature_input = new PhysicalData(rank, "temperature");

    }

    @Override
    public void init() {
        
        String key;

        key = "bioen.gross.energy.tmin";
        tmin = getConfiguration().getDouble(key);

        key = "bioen.gross.energy.tmax";
        tmax = getConfiguration().getDouble(key);

        key = "bioen.gross.energy.topt";
        topt = getConfiguration().getDouble(key);

        key = "bioen.arrh.ct1";
        c_t1 = getConfiguration().getDouble(key);

        key = "bioen.arrh.ct2";
        c_t2 = getConfiguration().getDouble(key);

        key = "bioen.maint.energy.csmr";
        csmr = getConfiguration().getDouble(key);

        key = "bioen.maturity.m0";
        m0 = getConfiguration().getDouble(key);

        key = "bioen.maturity.m1";
        m1 = getConfiguration().getDouble(key);

        key = "bioen.maturity.r";
        r = getConfiguration().getDouble(key);

        key = "bioen.maturity.growth_pot";
        growth_pot = getConfiguration().getDouble(key);

        this.compute_abc();

        // Redundant with the alpha of the BioenPredationMortality class.
        int nBack = this.getNBkgSpecies();
        int nspec = this.getNSpecies();

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

    }

    /**
     * Computes the phiT coefficients function for gross energy. Equation 4
     */
    public void compute_abc() {
        this.b = 2 / (topt * (tmin / topt - 1));
        this.a = -this.b / (2 * topt);
        this.c = 1 - this.b / 2.d * topt;

        this.bp = 2 / (topt * (tmax / topt - 1));
        this.ap = -this.bp / (2 * topt);
        this.cp = 1 - this.bp / 2.d * topt;

    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Returns the phiT function for gross energy. Equation 4
     *
     * @param school
     * @return
     */
    public double get_phiT(School school) {

        // Recovers the temperature of the school cell
        double temp = temperature_input.getValue(school);

        if ((temp <= tmin) || (temp > tmax)) {
            return 0;
        }

        double output = (temp <= topt) ? this.a * Math.pow(temp, 2) + this.b * Math.pow(temp, 1) + this.c : this.ap * Math.pow(temp, 2) + this.bp * Math.pow(temp, 1) + this.cp;
        return output;

    }

    /**
     * Returns the Arrhenius function for a given school. Cf. equation 6
     *
     * @param school
     * @return
     */
    public double get_Arrhenius(School school) {

        // Recovers the temperature of the school cell
        double temp = temperature_input.getValue(school);
        return Math.exp(this.c_t1 - (this.c_t2 / temp));

    }

    /**
     * Computes the maintenance coefficient. Equation 5
     *
     * @param school
     * @return
     */
    public double get_maintenance(School school) {

        int ispec = school.getSpeciesIndex();
        double output = this.csmr * Math.pow(school.getBiomass(), alpha[ispec]) * this.get_Arrhenius(school);
        return output;

    }

    /**
     * Returns the gross energy. Equation 3
     *
     * @param school
     * @return
     */
    public double get_egross(School school) {
        return school.getIngestion() * this.get_phiT(school);
    }

    /**
     * Returns the net energy. Equation 7
     *
     * @param school
     * @return
     */
    public double get_enet(School school) {
        return this.get_egross(school) - this.get_maintenance(school);
    }

    /**
     * Determines the maturity state. Equation 8
     *
     * @param school
     * @return
     */
    public int get_maturation(School school) {
        
        // If the school is mature, nothing is done and returns 1
        if(school.isMature()) {
            return 1;
        }
        
        // If the school is not mature yet, maturation is computed following equation 8
        double age = school.getAge();  // returns the age in years
        double length = school.getLength();   // warning: length in cm.
        double llim = this.m0 * age + this.m1;   // computation of a maturity

        int output = (length >= llim) ? 1 : 0;
        if(output == 1) {
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
        double dgrowth = (this.get_enet(school) >= 0) ? this.get_enet(school) : 0;

        // increments the weight
        school.incrementWeight((float) dgrowth);
    }

    /**
     * Returns the gonadic weight increment Equation 12
     *
     * @param school
     * @return
     */
    public void get_dg(School school) {
        double trend = 0;
        double enet = this.get_enet(school);
        if(enet > 0) {
            trend = (1 - this.get_kappa(school)) * enet;
        } else {
            // if enet is negative, gonad weight is removed
            // all the g is removed if enet is greater in absolute than g
            trend = Math.max(enet, -school.getGonadWeight());
        }
        
        school.incrementGonadWeight((float)trend);
        
    }

    /**
     * Returns the proportion of net energy allocated to somatic growth (equation
     * 10').
     *
     * @param school
     * @return
     */
    public double get_kappa(School school) {
        int ispec = school.getSpeciesIndex();
        // If the organism is imature, all the net energy goes to the somatic growth.
        // else, only a kappa fraction goes to somatic growth
        double kappa = (!school.isMature()) ? 1 : 1 - (r / growth_pot) * Math.pow(school.getWeight(), 1 - alpha[ispec]); //Function in two parts according to maturity state
        return kappa;
    }

}
