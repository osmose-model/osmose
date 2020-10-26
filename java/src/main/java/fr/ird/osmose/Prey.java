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

package fr.ird.osmose;

/**
 * This class provides a record of predation events on marine organisms (refer
 * to IMarineOrganism interface) that share the same species index, trophic
 * level, age and length. A record keep tracks of the species index of the prey,
 * the preyed biomass, the age, the length and the trophic level of the prey. It
 * implements the IMarineOrganism interface so the Prey can be handled by the
 * Stages classes (AgeStage, LengthStage, etc.) in the outputs for instance. The
 * Prey class does not represent a specific school or plankton swarm. It must be
 * seen as a record of preyed biomass by a predator, from potentially distinct
 * schools that share the same species, age, length and trophic level. That is
 * why the biomass variable is not final: predation events from a predator on
 * different schools with same species, age, length and trophic level will just
 * increase the preyed biomass of a single Prey. The class has been designed in
 * such a way to avoid the creation of hundred of thousands of Prey objects (as
 * I did initially, i.e. one predation event, one prey record which lead to heap
 * memory problem).
 */
public class Prey implements IMarineOrganism {

    /**
     * The preyed biomass, in tonne. Biomass is not final because
     */
    private double biomass;
    /**
     * The trophic level of the prey.
     */
    private final float trophicLevel;
    /**
     * The index of the species or resource group of the prey. In order to
     * distinguish a school from a resource, the index follows the following
     * convention: index = index species if the prey is an instance of
     * {@code School} and index = nSpecies + index resource group if the prey is
     * an instance of {@code ResourceSpecies}
     */
    private final int index;
    /**
     * The age of the prey, in year.
     */
    private final float age;
    /**
     * The length of the prey, in centimetre.
     */
    private final float length;

    /**
     * Creates a new prey.
     *
     * @param index of the prey. In this case, global index is used.
     * @param trophicLevel of the prey
     * @param age of the prey
     * @param length of the prey
     * @param biomass, the preyed biomass, in tonne
     */
    Prey(int index, float trophicLevel, float age, float length, double biomass) {
        this.index = index;
        this.trophicLevel = trophicLevel;
        this.biomass = biomass;
        this.age = age;
        this.length = length;
    }

    /**
     * Returns the preyed biomass, in tonne.
     *
     * @return the preyed biomass, in tonne
     */
    public double getBiomass() {
        return biomass;
    }

    public void incrementBiomass(double biomass) {
        this.biomass += biomass;
    }

    /**
     * Returns the trophic level of the prey.
     *
     * @return the trophic level of the prey
     */
    @Override
    public float getTrophicLevel() {
        return trophicLevel;
    }

    /**
     * Returns the index of the prey. In order to distinguish a school from a
     * resource group, the index follows the following convention: index = index
     * species if the prey is an instance of {@code School} and index = nSpecies
     * + index resource group if the prey is an instance of
     * {@code ResourceSpecies}
     *
     * @return the index of the prey
     */
    @Override
    public int getSpeciesIndex() {
        return index;
    }

    @Override
    public float getAge() {
        return age;
    }

    @Override
    public float getLength() {
        return length;
    }

    @Override
    public int hashCode() {
        // Hash code based on species index, trophic level, age and length
        int hash = 3;
        hash = 97 * hash + Float.floatToIntBits(this.trophicLevel);
        hash = 97 * hash + this.index;
        hash = 97 * hash + Float.floatToIntBits(this.age);
        hash = 97 * hash + Float.floatToIntBits(this.length);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        /* Two Preys are equals if they share same species index, trophic level,
        age and length. */
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Prey other = (Prey) obj;
        if (this.trophicLevel != other.trophicLevel) {
            return false;
        }
        if (this.index != other.index) {
            return false;
        }
        if (this.age != other.age) {
            return false;
        }
        if (this.length != other.length) {
            return false;
        }
        return true;
    }

    /**
     * PhV: So far the weight of the prey has never been needed by any output so
     * instead of storing unnecessary variables, I return zero. It can be
     * reconsidered at any moment.
     *
     * @return zero.
     */
    @Override
    public float getWeight() {
        return 0.f;
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
    public double[] getAccessibility() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void preyedUpon(int indexPrey, float trophicLevel, float age, float length, double preyedBiomass, boolean keepRecord) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Cell getCell() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getBetaBioen() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String getSpeciesName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getGlobalSpeciesIndex() {
        return index;
    }

    @Override
    public int getGlobalSpeciesIndex(boolean applyOff) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
