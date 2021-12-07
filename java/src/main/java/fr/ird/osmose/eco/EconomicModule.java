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

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.util.SimulationLinker;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

public class EconomicModule extends AbstractProcess {

    /** Stock elasticity. [nSpecies] */
    private double[] stockElasticity;
    
    /** Baseline costs. [gear, time] */
    private double[][] baselineCosts;
    
    /** Number of fishing gears */
    private int nFisheries;
    
    /** Number of species */
    private int nSpecies;
    
    /* Computed harvested costs. [gear, species] */
    private double[][] harvestingCosts;
    
    /** Substitution elasticity between species (alpha). */
    private double speciesConsumptionElasticity;
    
    /** Substitution elasticity between sizes within a species (mu_i). */
    private double[] sizeConsumptionElasticity;
    
    public EconomicModule(int rank) {
        super(rank);
        this.nSpecies = this.getNSpecies();
        this.nFisheries = this.getFishingGear().length; 
        this.stockElasticity = new double[nSpecies];
        this.baselineCosts = new double[nFisheries][];
        this.harvestingCosts = new double[nFisheries][nSpecies];     
        this.sizeConsumptionElasticity = new double[nSpecies];     
    }

    @Override
    public void init() {

        int cpt;
        // Recovers the index of fisheries
        int[] fisheryIndex = this.getConfiguration().findKeys("fisheries.name.fsh*").stream()
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).sorted().toArray();
        
        // Initialisation of stock elasticity.
        cpt = 0;
        for (int i : getFocalIndex()) {
            this.stockElasticity[cpt] = getConfiguration().getDouble("species.stock.elasticity.sp" + i);
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
        
        this.speciesConsumptionElasticity = getConfiguration().getDouble("consumption.elasticity");
        
        cpt = 0;
        for (int i : getFocalIndex()) {
            this.sizeConsumptionElasticity[cpt] = getConfiguration().getDouble("species.sizeconsumption.elasticity.sp" + i);
            cpt++;
        }
        
    }
    
    
    /** Computation of harvesting costs. */
    public void computeHarvestingCosts() {
        int time = this.getSimulation().getIndexTimeSimu();
        // Loop over fisheries
        for (int iFishery=0; iFishery<nFisheries; iFishery++) { 
            
            double baseCost = this.baselineCosts[iFishery][time];  // get base costs
            
            // accessible biomass over size
            double[] accesBiomass = getFishingGear()[iFishery].getAccessibleBiomass();  // species
            double sumAccess = 0;
            
            // integrate accessible biomass over size
            double[] harvestBiomass = getFishingGear()[iFishery].getHarvestedBiomass(); // species
            double sumHarvest = 0;
            
            for (int iSpecies = 0; iSpecies < nSpecies; iSpecies++) {
                // integrates harvested biomass over time
                sumHarvest = harvestBiomass[iSpecies];
                sumAccess = accesBiomass[iSpecies];
                
                this.harvestingCosts[iFishery][iSpecies] = baseCost * sumHarvest / (Math.pow(sumAccess, this.stockElasticity[iSpecies]));
                
            }
        }
        
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

}
