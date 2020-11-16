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
 * This interface gathers the traits that are shared between all marine
 * organisms in Osmose model, either explictly modelled (schools) or in the
 * background (plankton). Schools or swarms of plankton or any other future
 * marine organism included in Osmose must implement the IMarineOrganism
 * interface. Many classes of Osmose do not need to know whether they are
 * dealing with schools, plankton, top predators, etc. and only need access to
 * some characteristics of the organism, such as species index, age, length,
 * trophic level or weight. Generic classes such as AgeStage, LengthStage,
 * PredPreyStage rely on the sole information provided by IMarineOrganism
 * interface and as a consequence will work with any new Object that implements
 * this interface.
 *
 * @author P. VERLEY (philippe.verley@ird.fr)
 */
public interface IMarineOrganism {

    /**
     * The index of the species in Osmose configuration. The index should be
     * unique for every species, no matter whether the marine organism is a
     * school of fish or a swarm of plankton. Adopted convention so far: for
     * school species index is the index of the species in the configuration
     * file (species.name.sp#, index is the #). For LTL, the species index is
     * nSpecies + index of the LTL group in the configuration file
     * (resource.name.rsc# the species index is nSpecies + #). This is error
     * prone as every object that implements IMarineOrganism must carefully
     * check all the other objects that already implements IMarineOrganism for
     * generating a unique species index. For instance a new class Birds that
     * implements IMarineOrganism could have the following index nSpecies + nLTL
     * + # (# being the bird species index)
     *
     * @return the index of the species.
     */
    public int getFileSpeciesIndex();
    public int getSpeciesIndex();
    
    /**
     * Age of the organism in year.
     *
     * @return the age in year.
     */
    public float getAge();

    /**
     * Length of the organism in centimetre.
     *
     * @return the length in centimetre.
     */
    public float getLength();

    /**
     * Trophic level of the organism.
     *
     * @return the trophic level (scalar)
     */
    public float getTrophicLevel();

    /**
     * The weight of the organism in tonne. The weight has been historically
     * expressed in tonne in the School class in order to save computation time
     * for converting the biomass from gramme to tonne.
     *
     * @return the weight in tonne.
     */
    public float getWeight();
    
    public void incrementPredSuccessRate(float drate);

    /**
     * Age of the organism in year.
     *
     * @return the age in year.
     */
    public int getAgeDt();

    public double[] getAccessibility();

    public void preyedUpon(int indexPrey, int globalindex, float trophicLevel, float age, float length, double preyedBiomass, boolean keepRecord);
    
    public Cell getCell();
    
    public String getSpeciesName();
   
    public double getBetaBioen();
    
}
