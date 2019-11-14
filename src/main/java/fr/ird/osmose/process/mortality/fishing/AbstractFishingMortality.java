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
package fr.ird.osmose.process.mortality.fishing;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.AbstractMortalitySpecies;
import fr.ird.osmose.process.mortality.FishingMortality;

/**
 * This class extends {@code AbstractMortalityScenario} for fishing as it gives
 * the option to specify directly the catches (instead of a fishing mortality
 * rate).
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public abstract class AbstractFishingMortality extends AbstractMortalitySpecies {

    private int recruitmentAge;
    private float recruitmentSize;
    private double fishableBiomass;
    private final FishingMortality.Type fishingType;

    public AbstractFishingMortality(int rank, Species species, FishingMortality.Type fishingType) {
        super(rank, species);
        this.fishingType = fishingType;
    }

    /**
     * Returns the instantaneous level of catches, in tonne, for a given school
     * at current time step of the simulation.
     *
     * @param school, a given school
     * @return the the instantaneous level of catches for the given school at
     * current time step of the simulation
     */
    abstract public double getCatches(School school);

    abstract void readParameters();

    @Override
    public void init() {

        readParameters();

        int nStepYear = getConfiguration().getNStepYear();
        int iSpec = getIndexSpecies();

        if (!getConfiguration().isNull("mortality.fishing.recruitment.age.sp" + iSpec)) {
            float age = getConfiguration().getFloat("mortality.fishing.recruitment.age.sp" + iSpec);
            recruitmentAge = Math.round(age * nStepYear);
            recruitmentSize = 0.f;
        } else if (!getConfiguration().isNull("mortality.fishing.recruitment.size.sp" + iSpec)) {
            recruitmentSize = getConfiguration().getFloat("mortality.fishing.recruitment.size.sp" + iSpec);
            recruitmentAge = 0;
        } else {
            recruitmentAge = 0;
            recruitmentSize = 0.f;
            warning("Could not find any fishing recruitment threshold (neither age nor size) for species {0}. Osmose sets recruitment age and size to zero.", getSpecies().getName());
        }
    }

    public boolean isFishable(School school) {
        return (school.getAgeDt() >= recruitmentAge) && (school.getLength() >= recruitmentSize);
    }

    public void resetFishableBiomass() {
        fishableBiomass = 0.d;
    }

    /*
     * Increment the fishable biomass, in tonne, of the species.
     */
    public void incrementFishableBiomass(School school) {
        fishableBiomass += school.getInstantaneousBiomass();
    }

    double getFishableBiomass() {
        return fishableBiomass;
    }

    public FishingMortality.Type getType() {
        return fishingType;
    }
}
