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

    /** Baseline costs at t0 from calibration [Species] */
    private double[] baselineCostst0;

    /** Time trend on prices tau [Species] */
    private double[] timetrend;

    /** Baseline costs. [Species] */
    private double[] baselineCosts;

    /** Total accessible biomass. Dims=[fisheries, species] */
    private double[][][] accessibleBiomass;

    /**
     * Accessible biomass ponderated by the price of the species. Dims=[fisheries][species]*/
    private double[][][] priceAccessibleBiomass;

    /**
     * Total harvested biomass. Depends on fisheries, species and size-class. Dims=[fisheries][species][size-class]*/
    private double[][][] harvestedBiomass;

    /* Computed harvested costs. [species] */
    private double[] harvestingCosts;

    /** Substitution elasticity between species (alpha_i). Dim = [Species]*/
    private double[] speciesConsumptionElasticity;

    /** Substitution elasticity between sizes within a species (mu_i). Dim = [Species] */
    private double[] sizeConsumptionElasticity;

    /** Species size preference (beta_i), Dim = [Species][Size-class] */
    private double[][] speciesSizePreference; 

    /** Weight of fish consumption in total utility (gamma) */
    private double weightFishConsumption;

    /**  Elasticity of substitution between species (sigma) */
    private double ElasticitySubstitutionSpecies;

    /** Elasticity of demand for fish (nu) */
    private double ElasticityDemand; 

    /** Computation step for v(t) with beta_i*harvested biomass, Dim = [Species][size-class] */
    private double[][] sizePrefHarvest;

    /** Computation step for v(t) with alpha_i*sizePrefHarvest, Dim = [Species] */
    private double[] consumerpref;

    /** Utility of fish consumption v(t) */
    private double Utility;

    /** computation step for prices - part 2 */
    public double[] parttwo;

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

     // Recovers the index of fisheries
     int[] fisheryIndex = this.getConfiguration().findKeys("fisheries.name.fsh*").stream()
             .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).sorted().toArray();
          
    // Initialisation of stock elasticity.
        cpt = 0;
        stockElasticity = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            this.stockElasticity[cpt] = this.getConfiguration().getDouble("species.stock.elasticity.sp" + i);
            cpt++;
        }
    
    // Initialisation of baseline costs t0 from calibration c_i0
        cpt = 0;
        baselineCostst0 = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            this.baselineCostst0[cpt] = this.getConfiguration().getDouble("baseline.costs.t0.sp" + i);
            cpt++;
        }

    // Initialisation of time trend of fish prices tau
        cpt = 0;
        timetrend = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            this.timetrend[cpt] = this.getConfiguration().getDouble("price.time.trend.sp" + i);
            cpt++;
        }

        /** I calculated the time series later on so might not need to read a file */
    // Reads the time series of baseline costs. Reads one cost per simulation time step.
      //  cpt = 0;
       // baselineCosts = new double[nSpecies][];
        //for (int i : fisheryIndex) {
             // String filename = getConfiguration().getFile("baseline.costs.file.fsh" + i);
          //   SingleTimeSeries ts = new SingleTimeSeries();
             //ts.read(filename);
            // ts.read(getConfiguration().getFile("baseline.costs.file.fsh" + i));
             //baselineCosts[cpt] = ts.getValues();
             //cpt++;
        //}

    // Initialisation of species consumption elasticity alpha_i
        cpt = 0;
        speciesConsumptionElasticity = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            this.speciesConsumptionElasticity[cpt] = this.getConfiguration().getDouble("species.consumption.elasticity.sp" + i );
            cpt++;
        }
    
    // Initialisation of species size consumption elasticity mu_i
        cpt = 0;
        sizeConsumptionElasticity = new double[nSpecies];
        for (int i : getConfiguration().getFocalIndex()) {
            this.sizeConsumptionElasticity[cpt] = this.getConfiguration().getDouble("species.sizeconsumption.elasticity.sp" + i);
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

/** Computation of baseline cost time series */
    public void computeBaselineCosts() {
        int cpt = 0;
        int time = this.getSimulation().getIndexTimeSimu();
        for (int i : getFocalIndex()) {
            this.baselineCosts[cpt] = this.baselineCostst0[cpt]*Math.exp(this.timetrend[cpt]*time); 
        }
    }


/** Computation of harvesting costs. */
    public void computeHarvestingCosts() {
        int time = this.getSimulation().getIndexTimeSimu();
        // Loop over fisheries
        for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {

        // accessible biomass over size
            double[][] accesBiomass = accessibleBiomass[iFishery]; // species
        
        // harvested biomass over size
            double[][] harvestBiomass = harvestedBiomass[iFishery]; // species

          for (int iSpecies = 0; iSpecies < this.getNSpecies(); iSpecies++) {

        // integrates harvested biomass over time
                double[] sumHarvest = new double[iSpecies];
                sumHarvest = harvestBiomass[iSpecies];
                double[] sumAccess = new double[iSpecies];
                sumAccess = accesBiomass[iSpecies];

               this.harvestingCosts[iSpecies] = this.baselineCosts[iSpecies] * sumHarvest[iSpecies]
                         / (Math.pow(sumAccess[iSpecies], this.stockElasticity[iSpecies]));
            }
        }
    }

/** Computation of Utility of fish cosumption v(t) */
    public void computeUtility() {
        int time = this.getSimulation().getIndexTimeSimu();
        AbstractSchool school;
        for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
        int cpt = 0;
            for (int i : getFocalIndex()) {
                for (int iClass = 0; iClass < this.sizeClasses.getNStage(cpt); iClass++) {
            // harvested biomass over size
                double[][] harvestBiomass = harvestedBiomass[iFishery];
            // Step one = beta_i * harvested biomass
                this.sizePrefHarvest[i][iClass] = this.speciesSizePreference[i][iClass]
                * Math.pow(harvestBiomass[i][iClass],((this.sizeConsumptionElasticity[i]-1)/this.sizeConsumptionElasticity[i]));
           }
                // integrates over size class
                double[] sumbetah = sizePrefHarvest[cpt];
                this.consumerpref[i] = this.speciesConsumptionElasticity[i]
                *Math.pow(sumbetah[i],(this.sizeConsumptionElasticity[i]*(this.ElasticitySubstitutionSpecies-1)/this.ElasticitySubstitutionSpecies*(this.sizeConsumptionElasticity[i]-1)));
            // integrates over species
                double sumparenthesis = 0;
                sumparenthesis = consumerpref[i];
                this.Utility =  Math.pow(sumparenthesis,this.ElasticitySubstitutionSpecies/(this.ElasticitySubstitutionSpecies-1));
                cpt++;
        }
    }
}

/** Computation of prices p(i,s,t) */
    public void computePrices() {
        int time = this.getSimulation().getIndexTimeSimu();
        for (int iFishery = 0; iFishery < getConfiguration().getNFisheries(); iFishery++) {
        int cpt = 0;
        for (int i : getFocalIndex()) {
            for (int iClass = 0; iClass < this.sizeClasses.getNStage(cpt); iClass++)  {
            // harvested biomass over size
            double[][] harvestBiomass = harvestedBiomass[iFishery];
            // Part 1 - gamma(beta*harvest^(1/mu))
                double[][] partone = new double[i][iClass];
                partone[i][iClass] = this.weightFishConsumption*(this.speciesSizePreference[i][iClass]*
                Math.pow(harvestBiomass[i][iClass],(-1/this.sizeConsumptionElasticity[i])));
            // Part 2 - sum(beta*harvest^((mu-1)/mu))
                for (int kClass = 0; kClass < this.sizeClasses.getNStage(cpt); kClass++) {
                           if(kClass == iClass) {
                           continue;
                           }
                           double[][] betaharvestk = new double[i][kClass];
                            betaharvestk[i][kClass] = this.speciesSizePreference[i][kClass]*Math.pow(harvestBiomass[i][kClass],
                           (this.sizeConsumptionElasticity[i]-1)/this.sizeConsumptionElasticity[i]);
                           
                           //double[] parttwo = new double[i];
                           this.parttwo = betaharvestk[i];
                        }
            // Part 3 - alpha*(Part 2)^((mu*(sigma-1)/(mu-1)*sigma)-1)
                double[]partthree = new double[i];
                partthree[i] = this.speciesConsumptionElasticity[i]*Math.pow(this.parttwo[i],
                (this.ElasticitySubstitutionSpecies-this.sizeConsumptionElasticity[i])/(this.ElasticitySubstitutionSpecies
                *(this.sizeConsumptionElasticity[i]-1)));
            // Part 4 - utility^(1/sigma - 1/nu)
                double partfour = 0;
                partfour = Math.pow(this.Utility,(1/this.ElasticitySubstitutionSpecies)-(1/this.ElasticityDemand));

                this.Prices[i][iClass] = partone[i][iClass]*this.parttwo[i]*partthree[i]*partfour;
             }  
            }
            cpt++;
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
