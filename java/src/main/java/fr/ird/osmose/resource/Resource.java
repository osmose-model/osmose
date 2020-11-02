/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
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

package fr.ird.osmose.resource;

import fr.ird.osmose.Cell;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.util.OsmoseLinker;

/**
 * The Resource is the equivalent of the School in Osmose for the resource
 * group. It is simplified though as the Resource is only used in the predation
 * process. We only need to keep track of the preyed biomass to provide the
 * instantaneous biomass. In this class we make no distinction between abundance
 * and biomass
 *
 *
 * @author P. Verley
 */
public class Resource extends OsmoseLinker implements IAggregation {

    /**
     * Pointer to the resource group
     */
    final private ResourceSpecies species;
    /**
     * Pointer to the cell where this swarm is located
     */
    final private Cell cell;
    /**
     * Species index of the swarm, which ranges from nSpecies to nSpecis + nLTL
     */
    final private int index;
    
    /**
     * Number of organisms in the swarm at the beginning of the time step
     */
    private double abundance;
    /**
     * Number of dead organisms in the current time step, killed by predation
     */
    private double nDead;

    public Resource(ResourceSpecies species, Cell cell) {
        this.species = species;
        this.cell = cell;
        this.index = species.getFileSpeciesIndex();
    }

    /**
     * Gets the abundance of the school at the beginning of the time step.
     *
     * @return the abundance of the school at the beginning of the time step
     */
    @Override
    public double getAbundance() {
        return abundance;
    }

    /**
     * Evaluates the instantaneous abundance of the school.
     *
     * @return the instantaneous abundance of the school. {@code instantaneous
     * abundance = abundance at the beginning of the time step - ndead fish}
     * (due to any source of mortality, {@link MortalityCause}) at the time the
     * function is called. It is a snapshot of the abundance of the school
     * within the current time step.
     */
    @Override
    public double getInstantaneousAbundance() {
        return Math.max(0.d, abundance - nDead);
    }

    public void setBiomass(double biomass) {

        // Update abundance
        // (for resource Osmose makes no difference between abundance and biomass)
        abundance = biomass;
        // Rest number of dead fish
        nDead = 0.d;
    }

    /**
     * Gets the biomass of the school, in tonne, at the beginning of the time
     * step.
     *
     * @return the biomass of the school at the beginning of the time step in
     * tonne
     */
    @Override
    public double getBiomass() {
        return abundance;
    }

    /**
     * Evaluates the instantaneous biomass of the school.
     *
     * @return the instantaneous biomass of the school. {@code instantaneous
     * biomass = biomass at the beginning of the time step - biomass dead fish}
     * (due to any source of mortality, {@link MortalityCause}) at the time the
     * function is called. It is a snapshot of the biomass of the school within
     * the current time step.
     */
    @Override
    public double getInstantaneousBiomass() {
        return getInstantaneousAbundance();
    }

    /**
     * Increments the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     * @param nDead, the number of dead fish to be incremented for this
     * mortality cause
     */
    @Override
    public void incrementNdead(MortalityCause cause, double nDead) {
        if (cause != MortalityCause.PREDATION) {
            throw new UnsupportedOperationException("MortalityCause for Swarm must be PREDATION only.");
        }
        this.nDead += nDead;
    }

    @Override
    public double biom2abd(double biomass) {
        return biomass;
    }

    @Override
    public double abd2biom(double abund) {
        return abund;
    }

    @Override
    public int getFileSpeciesIndex() {
        return this.species.getFileSpeciesIndex();
    }

    @Override
    public float getAge() {
        return 0;
    }

    @Override
    public float getLength() {
        return 0;
    }

    @Override
    public float getTrophicLevel() {
        return species.getTrophicLevel();
    }

    @Override
    public float getWeight() {
        return 1.f;
    }

    @Override
    public Cell getCell() {
        return cell;
    }

    @Override
    public void incrementPredSuccessRate(float drate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getAgeDt() {
        return 0;
    }

    @Override
    public void preyedUpon(int indexPrey, float trophicLevel, float age, float length, double preyedBiomass, boolean keepRecord) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] getAccessibility() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void incrementIngestion(double cumPreyUpon) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getBetaBioen() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void fishedBy(int fisheryIndex, double fishedBiomass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSpeciesName() {
        return this.species.getName();
    }

    @Override
    public void discardedBy(int fisheryIndex, double fishedBiomass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getSpeciesIndex() {
        return this.species.getSpeciesIndex();
    }
    
    @Override
    public int getSpeciesIndex(boolean off) {
        return this.species.getGlobalSpeciesIndex(off);
    }

}
