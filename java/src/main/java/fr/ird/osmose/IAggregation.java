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

import fr.ird.osmose.process.mortality.MortalityCause;

/**
 * This interface provides functions to characterise marine aggregations in
 * Osmose, such as schools of fish or swarms of plankton. The interface extends
 * the IMarineOrganism and equips the object with biomass and abundance
 * basically. The idea is to offer common basic features in Osmose to objects
 * that share characteristics though they might play different roles in the
 * model. So far we have explicit schools or plankton swarms, but in future we
 * could easily include background schools (not explicitely modelled) and they
 * would benefit from all the features of the IAggregation, making the
 * integration in the mortality algorithm easier, for instance.
 *
 * @author P. VERLEY (philippe.verley@ird.fr)
 */
public interface IAggregation extends IMarineOrganism {

    /**
     * The biomass of the aggregation at the beginning of the current time step,
     * in tonne.
     *
     * @return the biomass in tonne.
     */
    public double getBiomass();

    /**
     * The instantaneous biomass of the aggregation during the time step. The
     * Instantaneous biomass is the biomass at the beginning of the time step
     * minus current death toll. It varies during the time step.
     *
     * @return the instantaneous biomass in tonne during the current time step.
     */
    public double getInstantaneousBiomass();

    /**
     * The abundance of the aggregation at the beginning of the current time
     * step.
     *
     * @return the number of individuals in the aggregation.
     */
    public double getAbundance();

    /**
     * The instantaneous abundance of the aggregation during the time step. The
     * instantaneous abundance is the number of individuals in the aggregation
     * at the beginning of the time step minus the dead individuals.
     *
     * @return the instantaneous number of individuals in the aggregation during
     * the current time step.
     */
    public double getInstantaneousAbundance();

    /**
     * Increment the number of dead individuals at current time step, for a
     * given mortality cause.
     *
     * @param cause, the mortality cause (predation, fishing, starvation, etc.)
     * @param nDead, the number of dead individuals
     */
    public void incrementNdead(MortalityCause cause, double nDead);

    /**
     * Converts biomass, in tonne, into abundance.
     *
     * @param biomass of the aggregation in tonne
     * @return the corresponding abundance.
     */
    public double biom2abd(double biomass);

    /**
     * Converts abundance into biomass (in ton).
     *
     * @param biomass of the aggregation in tonne
     * @return the corresponding abundance.
     */
    public double abd2biom(double biomass);

    public void incrementIngestion(double cumPreyUpon);

    /**
     * Records the amount of biomass fished by a given fishery.
     *
     * @param fisheryIndex, the fishery index
     * @param fishedBiomass, fished biomass in tons
     */
    public void fishedBy(int fisheryIndex, double fishedBiomass);
    
    public void discardedBy(int fisheryIndex, double fishedBiomass);

}
