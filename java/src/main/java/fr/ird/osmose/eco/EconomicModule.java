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
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import fr.ird.osmose.Configuration;

public class EconomicModule extends AbstractProcess {

    // sizeClasses used to determine variables for fishing economy (costs, etc.)
    private SchoolStage sizeClasses;
    private boolean isCalibrationEnabled = true;

    /** Number of species */
    private int nSpecies;

    /** Stock elasticity - chi_i. Dim = [nSpecies] */
    private double[] stockElasticity;

    /** Baseline costs at t0 from calibration - c0_i. Dim = [Species] */
    private double[] baselineCostst0;

    /** Time trend on prices - tau_i - from calibration. Dim = [Species] */
    private double[] timetrend;

    /** Baseline costs time series - c(i,t). Dim = [Species] */
    private double[] baselineCosts;

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

    public EconomicModule(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        this.sizeClasses = new SchoolStage("economic.output.stage");
        this.sizeClasses.init();

        int cpt;
        int nSpecies = this.getNSpecies();

        // Recovers the index of fisheries - UNUSED
        int[] fisheryIndex = this.getConfiguration().findKeys("fisheries.name.fsh*").stream()
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).sorted().toArray();

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

        // Compute baseline cost time series
        int time = getSimulation().getIndexTimeSimu();
        baselineCosts = new double[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            baselineCosts[i] = baselineCostst0[i] * Math.exp(timetrend[i] * time);
        }

        // Initialisation of species consumption elasticity alpha_i
        cpt = 0;
        speciesConsumptionElasticity = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            this.speciesConsumptionElasticity[cpt] = this.getConfiguration()
                    .getDouble("species.consumption.elasticity.sp" + i);
            cpt++;
        }

        // Initialisation of species size consumption elasticity mu_i
        cpt = 0;
        sizeConsumptionElasticity = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            this.sizeConsumptionElasticity[cpt] = this.getConfiguration()
                    .getDouble("species.sizeconsumption.elasticity.sp" + i);
            cpt++;
        }

        // Initialisation of species size preference beta_i
        cpt = 0;
        speciesSizePreference = new double[nSpecies][];
        for (int i : getConfiguration().getFocalIndex()) {
            this.speciesSizePreference[cpt] = this.getConfiguration().getArrayDouble("species.size.preference.sp" + i);
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

    public void clearHarvestingCosts() {
        int nSpecies = this.getNSpecies();
        this.harvestingCosts = new double[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            this.harvestingCosts = new double[i];
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
        int nSpecies = getConfiguration().getNSpecies();
        int iClass = sizeClasses.getNStage(nSpecies);
        double[][] accesBiomass = new double[nSpecies][iClass];
        double[][] harvestBiomass = new double[nSpecies][iClass];
        double[] sumHarvest = new double[nSpecies];
        double[] sumAccess = new double[nSpecies];
        this.harvestingCosts = new double[nSpecies];

        for (int i = 0; i < getConfiguration().getNFisheries(); i++) {
            for (int j = 0; j < nSpecies; j++) {
                for (int k = 0; k < iClass; k++) {
                    // accessible biomass and harvested biomass for each species per size class
                    accesBiomass[j][k] += this.accessibleBiomass[i][j][k];
                    harvestBiomass[j][k] += this.harvestedBiomass[i][j][k];

                    // accsessible and harvested biomass summed over size class
                    sumHarvest[j] += harvestBiomass[j][k];
                    sumAccess[j] += accesBiomass[j][k];
                    if (sumHarvest[j] == 0) {
                        this.harvestingCosts[j] = 0;
                    } else {
                        this.harvestingCosts[j] += this.baselineCosts[j] * sumHarvest[j]
                                / (Math.pow(sumAccess[j], this.stockElasticity[j]));
                    }
                }
            }
        }
        return this.harvestingCosts[iSpecies];
    }

    // nothing is checked starting from here
    /** Computation of Utility of fish cosumption v(t) */
    public double computeUtility() {
        int nSpecies = getConfiguration().getNSpecies();
        int iClass = this.sizeClasses.getNStage(nSpecies);
        double[][] harvestBiomass = new double[nSpecies][iClass];
        double[][] sizePrefHarvest = new double[nSpecies][iClass];
        double[] sumbetah = new double[nSpecies];
        double[] consumerpref = new double[nSpecies];
        double sumparenthesis = 0;
        this.Utility = 0;
        for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
            for (int i = 0; i < nSpecies; i++) {
                for (int j = 0; j < iClass; j++) {
                    // harvested biomass per species per size class
                    harvestBiomass[i][j] += this.harvestedBiomass[iFishery][i][j];
                    // Step one = beta_i * harvested biomass
                    sizePrefHarvest[i][j] += this.speciesSizePreference[i][j] * Math.pow(harvestBiomass[i][j],
                            ((this.sizeConsumptionElasticity[i] - 1) / this.sizeConsumptionElasticity[i]));
                    // sum over size classes
                    sumbetah[i] += sizePrefHarvest[i][j];
                    // alpha_i * sum(beta_i*harvest^power)^power
                    consumerpref[i] += this.speciesConsumptionElasticity[i] * Math.pow(sumbetah[i],
                            (this.sizeConsumptionElasticity[i] * (this.ElasticitySubstitutionSpecies - 1)
                                    / this.ElasticitySubstitutionSpecies * (this.sizeConsumptionElasticity[i] - 1)));
                    // integrates over species
                    sumparenthesis += consumerpref[i];
                    this.Utility += Math.pow(sumparenthesis,
                            this.ElasticitySubstitutionSpecies / (this.ElasticitySubstitutionSpecies - 1));
                }
            }
        }
        return this.Utility;
    }

    /** Computation of prices p(i,s,t) */
    public double getPrices(int iSpecies, int nClass) {
        int nSpecies = getConfiguration().getNSpecies();
        int iClass = this.sizeClasses.getNStage(nSpecies);
        double[][] harvestBiomass = new double[nSpecies][iClass];
        double[][] partone = new double[nSpecies][iClass];
        double[][] betaharvestk = new double[nSpecies][iClass];
        double[] parttwo = new double[nSpecies];
        double[] partthree = new double[nSpecies];
        double partfour = 0.0;
        this.Prices = new double[nSpecies][iClass];
        for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
            for (int i = 0; i < nSpecies; i++) {
                for (int j = 0; j < iClass; j++) {
                    // harvested biomass over size
                    harvestBiomass[i][j] += harvestedBiomass[iFishery][i][j];
                    // Part 1 - gamma(beta*harvest^(1/mu))
                    partone[i][j] += this.weightFishConsumption * (this.speciesSizePreference[i][j]
                            * Math.pow(harvestBiomass[i][j], (-1 / this.sizeConsumptionElasticity[i])));
                    // Part 2 - sum(beta*harvest^((mu-1)/mu))
                    for (int kClass = 0; kClass < iClass; kClass++) {
                        if (kClass == j) {
                            continue;
                        }
                        betaharvestk[i][kClass] += this.speciesSizePreference[i][kClass]
                                * Math.pow(harvestBiomass[i][kClass],
                                        (this.sizeConsumptionElasticity[i] - 1) / this.sizeConsumptionElasticity[i]);

                        // sum over size class k
                        parttwo[i] += betaharvestk[i][kClass];
                    }
                    // Part 3 - alpha*(Part 2)^((mu*(sigma-1)/(mu-1)*sigma)-1)
                    partthree[i] += this.speciesConsumptionElasticity[i] * Math.pow(parttwo[i],
                            (this.ElasticitySubstitutionSpecies - this.sizeConsumptionElasticity[i])
                                    / (this.ElasticitySubstitutionSpecies * (this.sizeConsumptionElasticity[i] - 1)));
                    // Part 4 - utility^(1/sigma - 1/nu)
                    if (this.Utility == 0){
                    partfour = 0;
                    } else {
                    partfour += Math.pow(this.Utility,
                            (1 / this.ElasticitySubstitutionSpecies) - (1 / this.ElasticityDemand));
                    }


                    this.Prices[i][j] += partone[i][j] * parttwo[i] * partthree[i] * partfour;
                }
            }
        }
        return this.Prices[iSpecies][nClass];
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
