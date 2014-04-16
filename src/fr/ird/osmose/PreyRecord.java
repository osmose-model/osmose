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
package fr.ird.osmose;

/**
 * This class provides a record of a predation event by the school on a given
 * prey. A prey can be either an other school or a plankton group. A record keep
 * tracks of: the type of prey (which species or plankton group), the biomass
 * preyed, the {@code DietOutputStage} of the prey and the trophic level of the
 * prey.
 *
 * @see Prey
 */
public class PreyRecord implements ISchool {

    /**
     * The preyed biomass, in tonne.
     */
    private final double biomass;
    /**
     * The trophic level of the prey.
     */
    private final float trophicLevel;
    /**
     * The index of the species or / plankton group of the prey. In order to
     * distinguish a school from a plankton, the index follows the following
     * convention: index = index species if the prey is an instance of
     * {@code School} and index = nSpecies + index plankton group if the prey is
     * an instance of {@code Plankton}
     */
    private final int index;
    private final float age;
    private final float length;

    /**
     * Creates a new prey record.
     *
     * @param index of the prey
     * @param trophicLevel of the prey
     * @param biomass, the preyed biomass, in tonne
     * @param dietOutputStage, the {@code DietOutputStage} of the prey
     */
    PreyRecord(int index, float trophicLevel, double biomass, float age, float length) {
        this.index = index;
        this.trophicLevel = trophicLevel;
        this.biomass = biomass;
        this.age = age;
        this.length = length;
    }

    PreyRecord(School prey, double biomass) {
        this.index = prey.getSpeciesIndex();
        this.trophicLevel = prey.getTrophicLevel();
        this.biomass = biomass;
        this.age = prey.getAge();
        this.length = prey.getLength();
    }

    /**
     * Returns the preyed biomass, in tonne.
     *
     * @return the preyed biomass, in tonne
     */
    @Override
    public double getBiomass() {
        return biomass;
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
     * plankton, the index follows the following convention: index = index
     * species if the prey is an instance of {@code School} and index = nSpecies
     * + index plankton group if the prey is an instance of {@code Plankton}
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
    public float getLengthIniStep() {
        throw new UnsupportedOperationException("Not supported for class PreyRecord.");
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Float.floatToIntBits(this.trophicLevel);
        hash = 97 * hash + this.index;
        hash = 97 * hash + Float.floatToIntBits(this.age);
        hash = 97 * hash + Float.floatToIntBits(this.length);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PreyRecord other = (PreyRecord) obj;
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
}
