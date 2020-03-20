/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.fishery.FisheryCatchability;
import fr.ird.osmose.process.mortality.fishery.FisheryDiscards;
import fr.ird.osmose.process.mortality.fishery.FisheryFBase;
import fr.ird.osmose.process.mortality.fishery.FisherySeason;
import fr.ird.osmose.process.mortality.fishery.FisherySeasonality;
import fr.ird.osmose.process.mortality.fishery.FisheryMapSet;
import fr.ird.osmose.process.mortality.fishery.FisherySelectivity;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author pverley
 */
public class FishingGear extends AbstractMortality {

    private String name;

    /**
     * Fishery index.
     */
    private final int fIndex;

    // Initialize the time varying array
    private FisheryFBase fBase;
    private FisherySeason fSeason;
    private FisherySeasonality fSeasonality;

    /**
     * Fishery map set.
     */
    private FisheryMapSet fMapSet;

    /**
     * Fishery accessibility file. Provides the catchability for the given
     * fishery. Function of time and species.
     */
    private FisheryCatchability catchability;
    
    private FisherySelectivity selectivity;
    
    private FisheryDiscards discards;


    public FishingGear(int rank, int findex) {
        super(rank);
        fIndex = findex;
    }

    @Override
    public void init() {
        
        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        // set-up the name of the fishery
        name = cfg.getString("fisheries.name.fsh" + fIndex);

        // Initialize the time varying array
        fBase = new FisheryFBase(fIndex);
        fBase.init();
        
        fSeason = new FisherySeason(fIndex);
        fSeason.init();
        
        fSeasonality = new FisherySeasonality(fIndex);       
        fSeasonality.init();
        
        // fishery spatial maps
        fMapSet = new FisheryMapSet(name, "fisheries.movement");
        fMapSet.init();
        
        selectivity = new FisherySelectivity(fIndex);
        selectivity.init();

        // accessibility matrix
        // (it provides the percentage of fishes that are going to be captured)
        catchability = new FisheryCatchability(fIndex, "fisheries.catchability");
        try {
            catchability.init();
        } catch (IOException ex) {
            Logger.getLogger(FishingGear.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        discards = new FisheryDiscards(fIndex, "fisheries.discards");
        
    }

    /**
     * Returns the fishing mortality rate associated with a given fishery. It is
     * the product of the time varying fishing rate, of the size selectivity and
     * of the spatial factor.
     *
     * @param school
     * @return The fishing mortality rate.
     */
    public double getRate(AbstractSchool school) throws Exception {

        int index = getSimulation().getIndexTimeSimu();
        
        // Recovers the school cell (used to recover the map factor)
        Cell cell = school.getCell();
        
        double spatialSelect = this.fMapSet.getValue(index, cell);
        if(spatialSelect == 0.0) {
            return 0.0;
        }

        int speciesIndex = school.getSpeciesIndex();

        double speciesCatchability = this.catchability.getValues(index, speciesIndex);
        if (speciesCatchability == 0.d) {
            return 0.d;
        }

        // recovers the time varying rate of the fishing mortality
        // as a product of FBase, FSeason and FSeasonality
        double timeSelect = fBase.getFBase(index);
        timeSelect *= this.fSeason.getSeasonFishMort(index) ;
        timeSelect *= this.fSeasonality.getSeasonalityFishMort(index);

        // Recovers the size/age fishery selectivity factor [0, 1]
        double sizeSelect = selectivity.getSelectivity(index, school);

        return speciesCatchability * timeSelect * sizeSelect * spatialSelect;
    }
    
    /**
     * Get the percentage of discards for the given species and the given
     * time-step.
     *
     * @param school
     * @return
     */
    public double getDiscardRate(AbstractSchool school) {
        int index = getSimulation().getIndexTimeSimu();
        int speciesIndex = school.getSpeciesIndex();
        return this.discards.getValues(index, speciesIndex);
    }

    /**
     * Returns the fishery index.
     *
     * @return the fishery index
     */
    public int getFIndex() {
        return this.fIndex;
    }

    /**
     * Returns the fishery name.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of the fishery.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public double getRate(School school) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
