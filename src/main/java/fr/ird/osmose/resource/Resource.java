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
        this.index = species.getIndex() + getNSpecies() + this.getNBkgSpecies();
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
    public int getSpeciesIndex() {
        return index;
    }

    @Override
    public float getAge() {
        return Float.NaN;
    }

    @Override
    public float getLength() {
        return Float.NaN;
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

    public int getRscIndex() {
        return species.getIndex();
    }

    @Override
    public void incrementPredSuccessRate(float drate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getAgeDt() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public double getAlphaBioen() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void fishedBy(int fisheryIndex, double fishedBiomass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
