/*
*OSMOSE (Object-oriented Simulator of Marine Ecosystems)
*http://www.osmose-model.org
*
*Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-today
*
*Osmose is a computer program whose purpose is to simulate fish
*populations and their interactions with their biotic and abiotic environment.
*OSMOSE is a spatial, multispecies and individual-based model which assumes
*size-based opportunistic predation based on spatio-temporal co-occurrence
*and size adequacy between a predator and its prey. It represents fish
*individuals grouped into schools, which are characterized by their size,
*weight, age, taxonomy and geographical location, and which undergo major
*processes of fish life cycle (growth, explicit predation, additional and
*starvation mortalities, reproduction and migration) and fishing mortalities
*(Shin and Cury 2001, 2004).
*
*Contributor(s):
*Yunne SHIN (yunne.shin@ird.fr),
*Morgane TRAVERS (morgane.travers@ifremer.fr)
*Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
*Philippe VERLEY (philippe.verley@ird.fr)
*Laure VELEZ (laure.velez@ird.fr)
*Nicolas Barrier (nicolas.barrier@ird.fr)
*
*This program is free software: you can redistribute it and/or modify
*it under the terms of the GNU General Public License as published by
*the Free Software Foundation (version 3 of the License). Full description
*is provided on the LICENSE file.
*
*This program is distributed in the hope that it will be useful,
*but WITHOUT ANY WARRANTY; without even the implied warranty of
*MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*GNU General Public License for more details.
*
*You should have received a copy of the GNU General Public License
*along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package fr.ird.osmose.eco;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.stage.SchoolStage;

public class EconomicModule extends AbstractProcess {

    // sizeClasses used to determine variables for fishing economy (costs, etc.)
    private SchoolStage sizeClasses;

    /** Stock elasticity - chi_i. Dim = [nSpecies] */
    private double[] stockElasticity;

    /** Baseline costs at t0 from calibration - c0_i. Dim = [Species] */
    private double[] baselineCostst0;

    /** Time trend on prices - tau_i - from calibration. Dim = [Species] */
    private double[] timetrend;

    /** Total accessible biomass. Dims = [fisheries, species, size-class] */
    private double[][][] accessibleBiomass;

    /**
     * Total harvested biomass. Depends on fisheries, species and size-class. Dims =
     * [fisheries][species][size-class]
     */
    private double[][][] harvestedBiomass;

    /** Computed harvesting costs. Dim = [species] */
    private double[] harvestingCosts;

    /** Substitution elasticity between species - alpha_i. Dim = [Species] */
    private double[] speciesConsumptionElasticity;

    /**
     * Substitution elasticity between sizes within a species - mu_i. Dim =
     * [Species]
     */
    private double[] sizeConsumptionElasticity;

    /** Species size preference - beta_i,s. Dims = [Species][Size-class] */
    private double[][] speciesSizePreference;

    /** Weight of fish consumption in total utility - gamma */
    private double weightFishConsumption;

    /** Elasticity of substitution between species - sigma */
    private double ElasticitySubstitutionSpecies;

    /** Elasticity of demand for fish - nu */
    private double ElasticityDemand;

    /** Utility of fish consumption - v(t) */
    private double Utility;

    /** Prices of species p(i,s,t), Dim = [Species][size-class] */
    private double[][] Prices;

    /** Fisherman's Profit Pi over time, Dim = [Species] */
    private double[] FishermanProfit;

    /** Profit margin pi. Dim = [Species] */
    private double[] ProfitMargin;

    public EconomicModule(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        this.sizeClasses = new SchoolStage("economic.output.stage");
        this.sizeClasses.init();

        int cpt;
        int nSpecies = this.getNSpecies();

        // Initialisation of stock elasticity.
        stockElasticity = new double[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            stockElasticity[i] = this.getConfiguration().getDouble("species.stock.elasticity.sp" + i);
        }

        // Initialisation of baseline costs t0 from calibration c_i0
        baselineCostst0 = new double[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            baselineCostst0[i] = this.getConfiguration().getDouble("baseline.costs.t0.sp" + i);
        }

        // Initialisation of time trend of fish prices tau
        timetrend = new double[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            timetrend[i] = this.getConfiguration().getDouble("price.time.trend.sp" + i);
        }



        // Initialisation of species consumption elasticity alpha_i
        cpt = 0;
        speciesConsumptionElasticity = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            speciesConsumptionElasticity[cpt] = this.getConfiguration()
                    .getDouble("species.consumption.elasticity.sp" + i);
            cpt++;
        }

        // Initialisation of species size consumption elasticity mu_i
        cpt = 0;
        sizeConsumptionElasticity = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            sizeConsumptionElasticity[cpt] = this.getConfiguration()
                    .getDouble("species.sizeconsumption.elasticity.sp" + i);
            cpt++;
        }

        // Initialisation of species size preference beta_i
        cpt = 0;
        speciesSizePreference = new double[nSpecies][];
        for (int i : getConfiguration().getFocalIndex()) {
            speciesSizePreference[cpt] = this.getConfiguration().getArrayDouble("species.size.preference.sp" + i);
            cpt++;
        }

        // Initialisation of weight of fish consumption gamma
        this.weightFishConsumption = getConfiguration().getDouble("weight.fish.consumption");

        // Initialisation of substitution elasticity between species sigma
        this.ElasticitySubstitutionSpecies = getConfiguration().getDouble("substitution.elasticity");

        // Initialisation elasticity of demand for fish nu
        this.ElasticityDemand = getConfiguration().getDouble("elasticity.demand.fish");

    }

    public void clearAccessibleBiomass() {
        int nSpecies = this.getNSpecies();
        this.accessibleBiomass = new double[getConfiguration().getNFisheries()][nSpecies][];
        this.harvestedBiomass = new double[getConfiguration().getNFisheries()][nSpecies][];
        for (int i = 0; i < getConfiguration().getNFisheries(); i++) {
            for (int j = 0; j < nSpecies; j++) {
                int nClass = sizeClasses.getNStage(j);
                this.accessibleBiomass[i][j] = new double[nClass];
                this.harvestedBiomass[i][j] = new double[nClass];
            }
        }
    }

    public void incrementAccessibleBiomass(int iFishery, AbstractSchool school, double increment) {
        int iSpecies = school.getSpeciesIndex();
        int iClass = this.sizeClasses.getStage(school);
        this.accessibleBiomass[iFishery][iSpecies][iClass] += increment;
    }

    public void incrementHarvestedBiomass(int iFishery, AbstractSchool school, double nDead) {
        int iSpecies = school.getSpeciesIndex();
        int iClass = this.sizeClasses.getStage(school);
        double biomass = school.abd2biom(nDead);
        this.harvestedBiomass[iFishery][iSpecies][iClass] += biomass;
    }

    public double getAccessibleBiomass(int iFishery, int iSpecies, int iClass) {
        return this.accessibleBiomass[iFishery][iSpecies][iClass];
    }

    public double getHarvestedBiomass(int iFishery, int iSpecies, int iClass) {
        return this.harvestedBiomass[iFishery][iSpecies][iClass];
    }

    public int getSizeClass(AbstractSchool school) {
        return this.sizeClasses.getStage(school);
    }

    /** Computation of harvesting costs. */
    public double getHarvestingCosts(int iSpecies) {
        // make sure all the parameters and variables are defined
        int time = getSimulation().getIndexTimeSimu();
        double timestart = getConfiguration().getDouble("output.start.year")*24;
        int nSpecies = getConfiguration().getNSpecies();
        double[] sumHarvest = new double[nSpecies];
        double[] sumAccess = new double[nSpecies];
        double[][] accesBiomass = new double[nSpecies][];
        double[][] harvestBiomass = new double[nSpecies][];
        double[] baselineCosts = new double[nSpecies];
        this.harvestingCosts = new double[nSpecies];
        // Loop over species
        for (int i = 0; i < nSpecies; i++) {
            // Compute baseline cost time series
            baselineCosts[i] = baselineCostst0[i] * Math.exp(timetrend[i] * (time - timestart));
            // Define size-class for each species
            int iClass = sizeClasses.getNStage(i);
            // Gives the size of the size-class dimension based on each species
            accesBiomass[i] = new double[iClass];
            harvestBiomass[i] = new double[iClass];
            // Loop over size-class
            for (int j = 1; j < iClass; j++) {
                // Loop over fisheries since accessible and harvested biomass are defined with
                // fisheries
                for (int k = 0; k < getConfiguration().getNFisheries(); k++) {
                    // accessible biomass and harvested biomass summed over fisheries
                    accesBiomass[i][j] += this.accessibleBiomass[k][i][j];
                    harvestBiomass[i][j] += this.harvestedBiomass[k][i][j];
                }
                // accessible and harvested biomass summed over size class
                sumHarvest[i] += harvestBiomass[i][j];
                sumAccess[i] += accesBiomass[i][j];
                // Make sure harvesting cost is 0 when harvesting biomass is null, NaN otherwise
                if (sumHarvest[i] == 0) {
                    this.harvestingCosts[i] = 0;
                } else {
                    this.harvestingCosts[i] = baselineCosts[i] * (sumHarvest[i]
                            / (Math.pow(sumAccess[i], stockElasticity[i])));
                }
            }
        }
        return this.harvestingCosts[iSpecies];
    }

    /** Computation of Utility of fish cosumption v(t) */
    public double getUtility() {
        // make sure all the parameters and variables are defined
        int nSpecies = getConfiguration().getNSpecies();
        double[] sumbetah = new double[nSpecies];
        double[] consumerpref = new double[nSpecies];
        double sumparenthesis = 0;
        double[][] harvestBiomass = new double[nSpecies][];
        double[][] sizePrefHarvest = new double[nSpecies][];
        double[] power = new double[nSpecies];
        this.Utility = 0;
        // Loop over species
        for (int i = 0; i < nSpecies; i++) {
            // Recover the number of size-class for each species
            int iClass = sizeClasses.getNStage(i);
            // Define the dimension size-class, different for each species
            harvestBiomass[i] = new double[iClass];
            sizePrefHarvest[i] = new double[iClass];
            // Loop over size-class - loop starts at 1 to exclude size class 0
            for (int j = 1; j < iClass; j++) {
                // Loop over fisheries
                for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
                    // harvested biomass summed over fisheries
                    harvestBiomass[i][j] += this.harvestedBiomass[iFishery][i][j];
                }
                // Formula for v(t) is divided in multiple steps
                // beta_i * harvested biomass
                sizePrefHarvest[i][j] = this.speciesSizePreference[i][j - 1] * Math.pow(harvestBiomass[i][j],
                        ((this.sizeConsumptionElasticity[i] - 1) / this.sizeConsumptionElasticity[i]));
                // sum over size classes
                sumbetah[i] += sizePrefHarvest[i][j];
            }
            // alpha_i * sumbetah^power
            power[i] = (this.sizeConsumptionElasticity[i] * (this.ElasticitySubstitutionSpecies - 1))
                    / (this.ElasticitySubstitutionSpecies * (this.sizeConsumptionElasticity[i] - 1));
            consumerpref[i] = this.speciesConsumptionElasticity[i] * Math.pow(sumbetah[i], power[i]);
            // sum over species
            sumparenthesis += consumerpref[i];
            this.Utility = Math.pow(sumparenthesis,
                    (this.ElasticitySubstitutionSpecies / (this.ElasticitySubstitutionSpecies - 1)));

        }
        return this.Utility;
    }

    /** Computation of prices p(i,s,t) */
    public double getPrices(int iSpecies, int nClass) {
        // make sure all the parameters and variables are defined
        int nSpecies = getConfiguration().getNSpecies();
        double[] parttwo = new double[nSpecies];
        double[] partthree = new double[nSpecies];
        double[][] partone = new double[nSpecies][];
        double[][] betaharvestk = new double[nSpecies][];
        double[][] harvestBiomass = new double[nSpecies][];
        double partfour = 0;
        this.Prices = new double[nSpecies][];
        this.Utility = getUtility();
        // Loop over species
        for (int i = 0; i < nSpecies; i++) {
            // Recover number of size classes for each species
            int iClass = sizeClasses.getNStage(i);
            harvestBiomass[i] = new double[iClass];
            partone[i] = new double[iClass];
            betaharvestk[i] = new double[iClass];
            this.Prices[i] = new double[iClass];
            // Loop over size-class - loop starts at 1 to exclude size class 0 that is not
            // sold
            for (int j = 1; j < iClass; j++) {
                // Loop over fisheries
                for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
                    // harvested biomass over size
                    harvestBiomass[i][j] += this.harvestedBiomass[iFishery][i][j];
                }
                // Equation for p(i,s,t) divided in calculation steps
                // Part 1 - gamma(beta*harvest^(1/mu))
                if (harvestBiomass[i][j] == 0) {
                    partone[i][j] = 0.0;
                } else {
                    partone[i][j] = this.weightFishConsumption * (this.speciesSizePreference[i][j - 1]
                            * Math.pow(harvestBiomass[i][j], (-1 / this.sizeConsumptionElasticity[i])));
                }
                // Part 2 - sum(beta*harvest^((mu-1)/mu))
                betaharvestk[i][j] = this.speciesSizePreference[i][j - 1] * Math.pow(harvestBiomass[i][j],
                        ((this.sizeConsumptionElasticity[i] - 1) / this.sizeConsumptionElasticity[i]));
                // sum over size-class k
                parttwo[i] += betaharvestk[i][j];

                // Part 3 - alpha*(Part 2)^((mu*(sigma-1)/(mu-1)*sigma)-1)
                partthree[i] = this.speciesConsumptionElasticity[i]
                        * Math.pow(parttwo[i], (this.ElasticitySubstitutionSpecies - this.sizeConsumptionElasticity[i])
                                / (this.ElasticitySubstitutionSpecies * (this.sizeConsumptionElasticity[i] - 1)));
                // Part 4 - utility^(1/sigma - 1/nu)
                if (this.Utility == 0.0) {
                    partfour = 0.0;
                } else {
                    partfour = Math.pow(this.Utility,
                            ((1 / this.ElasticitySubstitutionSpecies) - (1 / this.ElasticityDemand)));
                }

                this.Prices[i][j] = partone[i][j] * partthree[i] * partfour;
            }
        }
        return this.Prices[iSpecies][nClass];
    }

    /** Computation of Fisherman's profit Pi */
    public double getFishermanProfit(int iSpecies) {
        int nSpecies = getConfiguration().getNSpecies();
        double[][] priceharvest = new double[nSpecies][];
        this.FishermanProfit = new double[nSpecies];
        double[] sumpriceharvest = new double[nSpecies];
        double[][] harvestBiomass = new double[nSpecies][];
        // Loop over species
        for (int i = 0; i < nSpecies; i++) {
            // Define the number of size-class for each species
            int iClass = sizeClasses.getNStage(i);
            harvestBiomass[i] = new double[iClass];
            priceharvest[i] = new double[iClass];
            // Loop over size-class
            for (int j = 0; j < iClass; j++) {
                for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
                    // sum over fisheries to get harvested biomass over size
                    harvestBiomass[i][j] += this.harvestedBiomass[iFishery][i][j];
                }
                // Make sure we don't have NaN in output
                if (harvestBiomass[i][j] > 0) {
                    priceharvest[i][j] = harvestBiomass[i][j] * this.Prices[i][j];
                } else {
                    priceharvest[i][j] = 0;
                }
                // Sum p(i,s,t)*h(i,s,t) over size-class
                sumpriceharvest[i] += priceharvest[i][j];

                this.FishermanProfit[i] = sumpriceharvest[i] - this.harvestingCosts[i];
            }
        }
        return this.FishermanProfit[iSpecies];
    }

    /** Computation of profit margin pi */
    public double getProfitMargin(int iSpecies) {
        int nSpecies = getConfiguration().getNSpecies();
        double[][] priceharvest = new double[nSpecies][];
        this.ProfitMargin = new double[nSpecies];
        double[] sumpriceharvest = new double[nSpecies];
        // Loop over species
        for (int i = 0; i < nSpecies; i++) {
            // Define the number of size-class for each species
            int iClass = sizeClasses.getNStage(i);
            double[][] harvestBiomass = new double[nSpecies][iClass];
            priceharvest[i] = new double[iClass];
            // Loop over size-class
            for (int j = 0; j < iClass; j++) {
                for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
                    // sum over fisheries to get harvested biomass over size
                    harvestBiomass[i][j] += this.harvestedBiomass[iFishery][i][j];
                }
                this.Prices[i][j] = getPrices(i, j);
                // Make sure we don't have NaN in output
                if (harvestBiomass[i][j] == 0) {
                    priceharvest[i][j] = 0;
                } else {
                    priceharvest[i][j] = harvestBiomass[i][j] * this.Prices[i][j];
                }
                // Sum p(i,s,t)*h(i,s,t) over size-class
                sumpriceharvest[i] += priceharvest[i][j];
            }
            this.ProfitMargin[i] = (sumpriceharvest[i] - this.harvestingCosts[i]) / sumpriceharvest[i];
        }
        return this.ProfitMargin[iSpecies];
    }

    @Override
    public void run() {
    }

    public SchoolStage getSizeClass() {
        return this.sizeClasses;
    }

    public int getNFisheries() {
        return this.getConfiguration().getNFisheries();
    }

    public String[] getFisheriesNames() {
        return this.getConfiguration().getFisheriesNames();
    }

}
