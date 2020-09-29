/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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

package fr.ird.osmose.background;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Cell;
import java.util.HashMap;

/**
 *
 * @author nbarrier
 */
public class BackgroundSchool extends AbstractSchool {

    /**
     * Backgroud species associated with the background School.
     */
    final private BackgroundSpecies bkgSpecies;
    
    /** Size class index. */
    private final int classIndex;

    /**
     * Public constructor.Initialisation from background species, class index
 and time step.
     *
     * @param species
     * @param classIndex
     * @param cell
     */
    public BackgroundSchool(BackgroundSpecies species, int classIndex, Cell cell) {
        this.bkgSpecies = species;
        abundanceHasChanged = false;
        preys = new HashMap();
        fishedBiomass = new double[getConfiguration().getNFishery()];
        discardedBiomass = new double[getConfiguration().getNFishery()];
        this.classIndex = classIndex;
        this.moveToCell(cell);
    }

    /**
     * Initialisation of background species school by the values (ts and maps)
     * provided in file.
     */
    public void init() {
        this.reset(nDead);
        // Reset diet variables
        preys.clear();
        preyedBiomass = 0.d;
        predSuccessRate = 0.f;
        reset(fishedBiomass);
        reset(discardedBiomass);
    }

    /**
     * Gets the biomass of school at the beginning of the time-step. It is the
     * value which is defined from time-series and maps.
     *
     * @return
     */
    @Override
    public double getBiomass() {
        return this.biomass;
    }

    /**
     * Gets the abundance of school at the beginning of the time-step. It is the
     * value which is defined from time-series and maps.
     *
     * @return
     */
    @Override
    public double getAbundance() {
        return this.biom2abd(this.getBiomass());
    }

    @Override
    public void updateBiomAndAbd() {
        this.instantaneousAbundance = this.abundance - sum(nDead);
        if (instantaneousAbundance < 1.d) {
            instantaneousAbundance = 0.d;
        }
        this.instantaneousBiomass = this.abd2biom(instantaneousAbundance);
        abundanceHasChanged = false;
    }


    @Override
    public double biom2abd(double biomass) {
        return biomass / this.getWeight();
    }
    
    @Override
    public double abd2biom(double abund) {
        return abund * this.getWeight();
    }

    @Override
    public int getSpeciesIndex() {
        return this.bkgSpecies.getIndex();
    }
    
    @Override
    public float getAge() {
       return this.bkgSpecies.getAge(classIndex);
    }

    @Override
    public float getLength() {
        //return (float) this.bkgSpecies.getTimeSeries().getClass(iClass);
        return this.bkgSpecies.getLength(classIndex);
    }

    @Override
    public float getTrophicLevel() {
        return this.bkgSpecies.getTrophicLevel(classIndex);
    }

    @Override
    public float getWeight() {
        return this.bkgSpecies.computeWeight(this.getLength());
    }   
    
    /** Returns age as a number of time steps. 
     * Hard coded to 1 (to insure > 0 for predation), 
     * but to see what should be done about it.
     * @return 
     */
    @Override
    public int getAgeDt() {
        return this.bkgSpecies.getAgeDt(classIndex);
    }

    @Override
    public void incrementIngestion(double cumPreyUpon) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSpeciesName() {
        return this.bkgSpecies.getName();
    }
    
    public float getProportion() {
        return this.bkgSpecies.getProportion(this.classIndex);
    }
    
    public void setBiomass(double biomass) {
        this.biomass = biomass * this.getProportion();
    }

    @Override
    public double getBetaBioen() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
