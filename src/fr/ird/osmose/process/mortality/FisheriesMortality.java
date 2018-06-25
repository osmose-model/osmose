/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Nicolas BARRIER (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.output.FisheriesOutput;
import fr.ird.osmose.process.mortality.fisheries.AccessMatrix;
import fr.ird.osmose.process.mortality.fisheries.SingleFisheriesMortality;
import fr.ird.osmose.process.mortality.StochasticMortalityProcess;

/**
 *
 * @author Nicolas Barrier
 */
public class FisheriesMortality extends AbstractMortality {
    
    /** List of fisheries classes. One per fisherie type. */
    private SingleFisheriesMortality[] fisheriesMortality;
    
    /** Total number of fisheries. */
    private int nFisheries;
    
    /** Accessibility matrix. */
    private AccessMatrix accessMatrix;
    
    // Number of predation time-steps
    private final int subdt;
    
    public FisheriesMortality(int rank, int subdt) {
        super(rank);
        this.subdt = subdt;
    }
    
    public SingleFisheriesMortality[] getFisheries()
    {
        return this.fisheriesMortality;
    }

    @Override
    public void init() {
        
        // Initialize the accessbility matrix, which provides the percentage of fishes that are going to be captured.
        accessMatrix = new AccessMatrix();
        accessMatrix.read(getConfiguration().getFile("fisheries.catch.matrix.file"));
        
        // Recovers the total number of fisheries and initialize the fisheries array
        nFisheries = getConfiguration().findKeys("fisheries.select.curve.fis*").size();
        fisheriesMortality = new SingleFisheriesMortality[nFisheries];
        
        // Loop over all the fisheries and initialize them.
        int cpt = 0;
        for (int i = 0; i < nFisheries; i++) {
            while (!getConfiguration().canFind("fisheries.select.curve.fis" + cpt)) {
                cpt++;
            }
            fisheriesMortality[i] = new SingleFisheriesMortality(this.getRank(), cpt);
            fisheriesMortality[i].init();
            cpt++;
        }
    }

    /** Calculation of the fishing mortality rate for a given school.
     * A loop is performed over all the defined fisheries. 
     * If the analysed fisherie doesn't catch the defined fish, nothing is done.
     * Else, the total mortality rate is updated.
     * @param school
     * @return 
     */
    @Override
    public double getRate(School school) {
       
        // Initialize an array of fisheries index
        Integer[] seqFisheries = new Integer[nFisheries];
        for (int i = 0; i < nFisheries; i++) {
            seqFisheries[i] = i;
        }
        
        // Initialize the number of dead individuals
        double nDead;
        
        // shuffle the fisheries index array.
        StochasticMortalityProcess.shuffleArray(seqFisheries);
        
        // Initializes the fishing mortality rate and sets it to 0.
        double output = 0.0;
        
        // Recover the species index
        int iSpecies = school.getSpeciesIndex();
       
        // Loop over all the fisheries index in a random way
        for(int fIndex : seqFisheries)
        {
             
            // recover the current fisherie
            SingleFisheriesMortality fish = this.getFisheries()[fIndex];
            
            // computes the accessibility: 100% for the targeted species, >0 for bycatches
            double accessVal = accessMatrix.getValues(fIndex, iSpecies);
            
            // if the current specie is not targeted by the fisherie, nothing is done
            if(accessVal == 0) {
                continue;
            }
            
            // Computation of the mortality fishing rate.
            // F is the fishing rate for the current specie (i.e.
            // spatial * timevar * size select) time *  
            double F = fish.getRate(school) * accessVal / subdt;
            
            // updates the number of dead individuals within the school
            nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-F));
            
            // Increment the fisheries Outputs for the given species and the given
            // fisherie.
            if (FisheriesOutput.saveFisheries()) {
                FisheriesOutput.incrementFish(nDead, iSpecies, fIndex);
            }
            
            // increments the number of DEAD individuals by fishing.
            school.incrementNdead(MortalityCause.FISHING, nDead);
           
        }
        
        return output;
        
    }
    
}
