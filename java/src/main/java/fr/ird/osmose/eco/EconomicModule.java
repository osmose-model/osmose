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
import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.output.distribution.AgeDistribution;
import fr.ird.osmose.output.distribution.SizeDistribution;
import fr.ird.osmose.output.distribution.WeightDistribution;
import fr.ird.osmose.process.AbstractProcess;

public class EconomicModule extends AbstractProcess {
    
    // sizeClasses used to determine variables for fishing economy (costs, etc.)
    private AbstractDistribution sizeClasses;
    private boolean isCalibrationEnabled = true;

    /** Stock elasticity. [nSpecies] */
    private double[] stockElasticity;
    
    /** Baseline costs. [gear, time] */
    private double[][] baselineCosts;
    
    /** Number of fishing gears. If old fisheries used, equals to nSpecies. */
    private int nFisheries;
    
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
    
    /** Number of species */
    private int nSpecies;
    
    private String[] namesFisheries;
    
    // /* Computed harvested costs. [gear, species] */
    // private double[][] harvestingCosts;
    
    // /** Substitution elasticity between species (alpha). */
    // private double speciesConsumptionElasticity;
    
    // /** Substitution elasticity between sizes within a species (mu_i). */
    // private double[] sizeConsumptionElasticity;
    
    public EconomicModule(int rank) {  
        super(rank);
    }
    
    @Override
    public void init() {
                
        if (getConfiguration().isFisheryEnabled()) {
            nFisheries = getConfiguration().getNFishery();
            namesFisheries = new String[nFisheries];
            for (int iFishery = 0; iFishery < nFisheries; iFishery++) {
                namesFisheries[iFishery] = String.format("fishery%03d", iFishery);
            }
        } else {
            nFisheries = getConfiguration().getNSpecies();
            namesFisheries = new String[nFisheries];
            for (int iSpecies = 0; iSpecies < nFisheries; iSpecies++) {
                namesFisheries[iSpecies] = String.format("fishery%03d", iSpecies);
            }
        }
        
        String key = "economic.distribution.type";
        if (getConfiguration().canFind(key)) {
            String type = getConfiguration().getString(key);
            switch (type) {
                case ("weight"):
                    this.sizeClasses = new WeightDistribution();
                    break;
                case ("age"):
                    this.sizeClasses = new AgeDistribution();
                    break;
                case ("size"):
                    this.sizeClasses = new SizeDistribution();
                    break;
                default:
                    this.sizeClasses = new SizeDistribution();
                    break;
            }
        } else {
            this.sizeClasses = new SizeDistribution();
        }

        this.sizeClasses.init();
        
    }
    
    public void clearAccessibleBiomass() {
        int nSpecies = this.getNSpecies();
        int nClass = sizeClasses.getNClass();
        this.accessibleBiomass = new double[nFisheries][nSpecies][nClass];
        this.priceAccessibleBiomass = new double[nFisheries][nSpecies][nClass];
        this.harvestedBiomass = new double[nFisheries][nSpecies][nClass];
    }

    public void incrementAccessibleBiomass(int iFishery, AbstractSchool school, double increment) {
        int iSpecies = school.getSpeciesIndex();
        int iClass = this.sizeClasses.getClass(school);
        this.accessibleBiomass[iFishery][iSpecies][iClass] += increment;
    }

    public void incrementHarvestedBiomass(int iFishery, AbstractSchool school, double nDead) {
        int iSpecies = school.getSpeciesIndex();
        int iClass = this.sizeClasses.getClass(school);
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
        return sizeClasses.getClass(school);
    }
    

        // int cpt;
        // // Recovers the index of fisheries
        // int[] fisheryIndex = this.getConfiguration().findKeys("fisheries.name.fsh*").stream()
        //         .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).sorted().toArray(); 
        
        // // Initialisation of stock elasticity.
        // cpt = 0;
        // for (int i : getFocalIndex()) {
        //     this.stockElasticity[cpt] = getConfiguration().getDouble("species.stock.elasticity.sp" + i);
        //     cpt++;
        // }
        
        // // Reads the time series of baseline costs. Reads one cost per simulation time step.
        // cpt = 0;
        // for (int i : fisheryIndex) {
        //     String filename = getConfiguration().getFile("baseline.costs.file.fsh" + i);
        //     SingleTimeSeries ts = new SingleTimeSeries();
        //     ts.read(filename);
        //     this.baselineCosts[i] = ts.getValues();
        //     cpt++;
        // }
        
        // this.speciesConsumptionElasticity = getConfiguration().getDouble("consumption.elasticity");
        
        // cpt = 0;
        // for (int i : getFocalIndex()) {
        //     this.sizeConsumptionElasticity[cpt] = getConfiguration().getDouble("species.sizeconsumption.elasticity.sp" + i);
        //     cpt++;
        // }
 
    // }
    
    
    // /** Computation of harvesting costs. */
    // public void computeHarvestingCosts() {
    //     int time = this.getSimulation().getIndexTimeSimu();
    //     // Loop over fisheries
    //     for (int iFishery = 0; iFishery < nFisheries; iFishery++) {

    //         double baseCost = this.baselineCosts[iFishery][time]; // get base costs

    //         // accessible biomass over size
    //         double[] accesBiomass = getSimulation().getAccessibleBiomass()[iFishery]; // species
    //         double sumAccess = 0;

    //         // integrate accessible biomass over size
    //         double[] harvestBiomass = getSimulation().getHarvestedBiomass()[iFishery]; // species
    //         double sumHarvest = 0;

    //         for (int iSpecies = 0; iSpecies < this.getNSpecies(); iSpecies++) {

    //             // integrates harvested biomass over time
    //             sumHarvest = harvestBiomass[iSpecies];
    //             sumAccess = accesBiomass[iSpecies];

    //             this.harvestingCosts[iFishery][iSpecies] = baseCost * sumHarvest
    //                     / (Math.pow(sumAccess, this.stockElasticity[iSpecies]));

    //         }
    //     }
    // }
            
    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }
    
    public AbstractDistribution getSizeClass() {
        return this.sizeClasses;
    }
    
    public int getNFisheries() { 
        return this.nFisheries;   
    }
    
    public String[] getFisheriesNames() {
        return this.namesFisheries;   
    }

}
