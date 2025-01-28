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

public class EconomicModule extends AbstractProcess {

    // sizeClasses used to determine variables for fishing economy (costs, etc.)
    private SchoolStage sizeClasses;
    private boolean isCalibrationEnabled = true;

    /** Number of species */
    private int nSpecies;

    /** Stock elasticity. [nSpecies] */
    private double[] stockElasticity;

    /** Baseline costs. [gear, time] */
    private double[][] baselineCosts;

    /** Total accessible biomass. Dims=[fisheries, species] */
    private double[][][] accessibleBiomass;

    /**
     * Accessible biomass ponderated by the price of the species. Dims=[fisheries,
     * species]
     */
    private double[][][] priceAccessibleBiomass;

    /**
     * Total harvested biomass. Depends on fisheries, species and size-class. Dims=[fisheries,
     * species, size-class]
     */
    private double[][][] harvestedBiomass;

    /* Computed harvested costs. [gear, species] */
    private double[][] harvestingCosts;

    /** Substitution elasticity between species (alpha_i). 
     * Dim = [Species]
    */
    private double[] speciesConsumptionElasticity;

    /** Substitution elasticity between sizes within a species (mu_i). 
     * Dim = [Species]
    */
    private double[] sizeConsumptionElasticity;

    /** Species size preference (beta_i) 
     * Dim = [Species][Size-class]
    */
    private double[][] speciesSizePreference; 

    /** Weight of fish consumption in total utility (gamma) */
    private double weightFishConsumption;

    /**  Elasticity of substitution between species (sigma) */
    private double ElasticitySubstitutionSpecies;

    /** Elasticity of demand for fish (nu) */
    private double ElasticityDemand; 

    /** Computation step for v(t) with beta_i*harvested biomass 
     * Dim = [Species][size-class]
    */
    private double[][] sizePrefHarvest;

    /** Computation step for v(t) with alpha_i*sizePrefHarvest 
     * Dim = [Species] 
    */
    private double[] consumerpref;

    /** Utility of fish consumption v(t) */
    private double Utility;

    /** Computation steps for Prices
     * Part 1 - gamma(beta*harvest^(1/mu))
     * Dim = [Species][size-class]
     */
    private double[][] partone; 

    /** Part 2 - sum(beta*harvest^((mu-1)/mu)) 
     * Dim = [Species]
    */
    private double[] parttwo;

    /** Part 3 - alpha*(Part 2)^((mu*(sigma-1)/(mu-1)*sigma)-1) 
     * Dim = [Species]
    */
    private double[] partthree;

    /** Part 4 - Utility^(1/sigma - 1/nu) */
    private double partfour;

    /** Prices of species p(i,s,t)
     * Dim = [Species][size-class]
     */
    private double[][] Prices;

    public EconomicModule(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        this.sizeClasses = new SchoolStage("economic.output.stage");
        this.sizeClasses.init();

    int cpt;
     // Recovers the index of fisheries
     int[] fisheryIndex = this.getConfiguration().findKeys("fisheries.name.fsh*").stream()
             .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).sorted().toArray();
          
    // Initialisation of stock elasticity.
        cpt = 0;
        for (int i : getFocalIndex()) {
            stockElasticity[cpt] = this.getConfiguration().getDouble("species.stock.elasticity.sp" + i);
            cpt++;
        }
    
    // Reads the time series of baseline costs. Reads one cost per simulation time step.
        cpt = 0;
        for (int i : fisheryIndex) {
             String filename = getConfiguration().getFile("baseline.costs.file.fsh" + i);
             SingleTimeSeries ts = new SingleTimeSeries();
             ts.read(filename);
             this.baselineCosts[i] = ts.getValues();
             cpt++;
        }

    // Initialisation of species consumption elasticity alpha_i
        cpt = 0;
        for (int i : getFocalIndex()) {
            this.speciesConsumptionElasticity[cpt] = getConfiguration().getDouble("species.consumption.elasticity.sp" + i );
            cpt++;
        }
    
    // Initialisation of species size consumption elasticity mu_i
        cpt = 0;
        for (int i : getFocalIndex()) {
            this.sizeConsumptionElasticity[cpt] = getConfiguration().getDouble("species.sizeconsumption.elasticity.sp" + i);
            cpt++;
        }

    // Initialisation of species size preference beta_i
        cpt = 0;
        for (int i : getFocalIndex()) {
            this.speciesSizePreference[cpt] = getConfiguration().getArrayDouble("species.size.preference.sp" + i);
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
        this.priceAccessibleBiomass = new double[getConfiguration().getNFisheries()][nSpecies][];
        this.harvestedBiomass = new double[getConfiguration().getNFisheries()][nSpecies][];
        for (int i = 0; i < getConfiguration().getNFisheries(); i++) {
            for (int j = 0; j < nSpecies; j++) {
                int nClass = sizeClasses.getNStage(j);
                this.accessibleBiomass[i][j] = new double[nClass];
                this.priceAccessibleBiomass[i][j] = new double[nClass];
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

    // public void incrementPriceAccessibleBiomass(int iFishery, int iSpecies, double increment) {
    //     this.priceAccessibleBiomass[iFishery][iSpecies] += increment;
    // }

    public double getAccessibleBiomass(int iFishery, int iSpecies, int iClass) {
        return accessibleBiomass[iFishery][iSpecies][iClass];
    }

    public double getPriceAccessibleBiomass(int iFishery, int iSpecies, int iClass) {
        return priceAccessibleBiomass[iFishery][iSpecies][iClass];
    }

    public double getHarvestedBiomass(int iFishery, int iSpecies, int iClass) {
        return this.harvestedBiomass[iFishery][iSpecies][iClass];
    }

    public int getSizeClass(AbstractSchool school)  {
        return this.sizeClasses.getStage(school);
    }


/** Computation of Utility of fish cosumption v(t) */
    public void computeUtility() {
        int time = this.getSimulation().getIndexTimeSimu();
        for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
            // harvested biomass over size
            double[][] harvestedBiomass = getSimulation().getEconomicModule().getHarvestedBiomass()[iFishery]; // species
        }
        int cpt = 0;
        for (int i : getFocalIndex()) {
            int iClass = 0;
            for (int j : getSizeClass()) {
            // Step one = beta_i * harvested biomass
                this.sizePrefHarvest = this.speciesSizePreference[iClass]
                *Math.pow(this.harvestedBiomass[cpt][iClass],((this.sizeConsumptionElasticity[cpt]-1)/this.sizeConsumptionElasticity[cpt]));
            // integrates over size class
                double[] sumbetah = 0;
                sumbetah = sizePrefHarvest[iClass];
                this.consumerpref[cpt] = this.speciesConsumptionElasticity[cpt]
                *Math.pow(this.sumbetah[cpt],(this.sizeConsumptionElasticity[cpt]*(this.ElasticitySubstitutionSpecies-1)/this.ElasticitySubstitutionSpecies*(this.sizeConsumptionElasticity[cpt]-1)));
            // integrates over species
                double sumparenthesis = 0;
                sumparenthesis = consumerpref[cpt];
                this.Utility =  Math.pow(this.sumparenthesis,this.ElasticitySubstitutionSpecies/(this.ElasticitySubstitutionSpecies-1));
                iClass++;
                cpt++;
            }
        }
    }

/** Computation of prices p(i,s,t) */
    public void computePrices() {
        int time = this.getSimulation().getIndexTimeSimu();
        for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
            // harvested biomass over size
            double[][] harvestBiomass = getSimulation().getHarvestedBiomass()[iFishery]; // species
        }
        int cpt = 0;
        for (int i : getFocalIndex()) {
            int sclass = 0;
            for (int j : getSizeClass())  {
            // Part 1 - gamma(beta*harvest^(1/mu))
                this.partone[cpt][sclass] = this.weightFishConsumption*(this.speciesSizePreference[sclass]*
                Math.pow(this.harvestBiomass[cpt][sclass],(-1/this.sizeConsumptionElasticity[cpt])));
            // Part 2 - sum(beta*harvest^((mu-1)/mu))
                for (int k : getSizeClass()) {
                           if(k == j) {
                           continue;
                           }
                           double[][] betaharvestk = this.speciesSizePreference[k]*Math.pow(this.harvestBiomass[cpt][k],
                           (this.sizeConsumptionElasticity[cpt]-1)/this.sizeConsumptionElasticity[cpt]);
                           this.parttwo = betaharvestk[k];
                        }
                }   
            // Part 3 - alpha*(Part 2)^((mu*(sigma-1)/(mu-1)*sigma)-1)
                this.partthree = this.speciesConsumptionElasticity[cpt]*Math.pow(this.parttwo[cpt],
                (this.ElasticitySubstitutionSpecies-this.sizeConsumptionElasticity[cpt])/(this.ElasticitySubstitutionSpecies
                *(this.sizeConsumptionElasticity[cpt]-1)));
            // Part 4 - utility^(1/sigma - 1/nu)
                this.partfour = Math.pow(this.Utility,(1/this.ElasticitySubstitutionSpecies)-(1/this.ElasticityDemand));

                this.Prices = partone[cpt][sclass]*parttwo[cpt]*partthree[cpt]*partfour;
            }
        
    }

    // /** Computation of harvesting costs. */
    public void computeHarvestingCosts() {
         int time = this.getSimulation().getIndexTimeSimu();
         // Loop over fisheries
         for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {

             double baseCost = this.baselineCosts[iFishery][time]; // get base costs

            // accessible biomass over size
             double[] accesBiomass = getSimulation().getAccessibleBiomass()[iFishery]; // species
             double sumAccess = 0;

            // harvested biomass over size
             double[] harvestBiomass = getSimulation().getHarvestedBiomass()[iFishery]; // species
             double sumHarvest = 0;

             for (int iSpecies = 0; iSpecies < this.getNSpecies(); iSpecies++) {

                 // integrates harvested biomass over time
                 sumHarvest = harvestBiomass[iSpecies];
                 sumAccess = accesBiomass[iSpecies];

                 this.harvestingCosts[iFishery][iSpecies] = baseCost * sumHarvest
                         / (Math.pow(sumAccess, this.stockElasticity[iSpecies]));

             }
         }
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
