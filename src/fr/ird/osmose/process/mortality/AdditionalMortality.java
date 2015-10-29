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
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.additional.AnnualAdditionalMortality;
import fr.ird.osmose.process.mortality.additional.ByDtByClassAdditionalMortality;
import fr.ird.osmose.process.mortality.additional.ByDtLarvaMortality;
import fr.ird.osmose.process.mortality.additional.ByDtAdditionalMortality;
import fr.ird.osmose.process.mortality.additional.ConstantLarvaMortality;
import fr.ird.osmose.util.GridMap;
import java.util.List;

/**
 *
 * @author pverley
 */
public class AdditionalMortality extends AbstractMortality {

    private AbstractMortalitySpecies[] eggMortality;
    private AbstractMortalitySpecies[] additionalMortality;
    /**
     * Spatial factor for natural mortality [0, 1]
     */
    private GridMap[] spatialD;

    public AdditionalMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int rank = getRank();

        // Egg mortality
        eggMortality = new AbstractMortalitySpecies[getNSpecies()];
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSpecies(iSpec);
            // Egg mortality by Dt
            if (!getConfiguration().isNull("mortality.natural.larva.rate.bytDt.file.sp" + iSpec)) {
                eggMortality[iSpec] = new ByDtLarvaMortality(rank, species);
                continue;
            }
            // Constant Egg mortality
            if (!getConfiguration().isNull("mortality.natural.larva.rate.sp" + iSpec)) {
                eggMortality[iSpec] = new ConstantLarvaMortality(rank, species);
                continue;
            }
            // Did not find any scenario for Egg mortality, Osmose assumes Egg mortality = 0.
            // Warning only because some species might not reproduce in the 
            // simulated area and therefore have no need to define larva mortality
            eggMortality[iSpec] = new ConstantLarvaMortality(rank, species, 0.f);
            warning("Could not find any parameters for egg mortality (mortality.natural.larva.rate.bytDt.file.sp# or mortality.natural.larva.rate.sp#) for species {0}. Osmose assumes egg mortality = 0", species.getName());
        }

        // Additional mortality
        additionalMortality = new AbstractMortalitySpecies[getNSpecies()];

        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSpecies(iSpec);
            // Additional mortality by Dt, by Age or Size
            if (!getConfiguration().isNull("mortality.natural.rate.byDt.byAge.file.sp" + iSpec)
                    || !getConfiguration().isNull("mortality.natural.rate.byDt.bySize.file.sp" + iSpec)) {
                additionalMortality[iSpec] = new ByDtByClassAdditionalMortality(rank, species);
                continue;
            }
            // Additional mortality by Dt
            if (!getConfiguration().isNull("mortality.natural.rate.bytDt.file.sp" + iSpec)) {
                additionalMortality[iSpec] = new ByDtAdditionalMortality(rank, species);
                continue;
            }
            // Annual natural mortality
            if (!getConfiguration().isNull("mortality.natural.rate.sp" + iSpec)) {
                additionalMortality[iSpec] = new AnnualAdditionalMortality(rank, species);
                continue;
            }
            // Did not find any scenario for natural mortality. Throws error.
            error("Could not find any parameters for natural mortality (mortality.natural.rate.byDt.byAge.file.sp# or mortality.natural.rate.byDt.bySize.file.sp mortality.natural.rate.byDt.file.sp or mortality.natural.rate.sp#) for species " + species.getName(), null);
        }

        // Initialize mortality scenarii
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            eggMortality[iSpec].init();
            additionalMortality[iSpec].init();
        }

        // Patch for Ricardo to include space variability in natural mortality
        // Need to think of a better parametrization before including it
        // formally in Osmose
        spatialD = new GridMap[getNSpecies()];
        List<String> keys = getConfiguration().findKeys("mortality.natural.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
                if (!getConfiguration().isNull("mortality.natural.spatial.distrib.file.sp" + iSpec)) {
                    spatialD[iSpec] = new GridMap(getConfiguration().getFile("mortality.natural.spatial.distrib.file.sp" + iSpec));
                }
            }
        }
    }

    /**
     * Additional mortality rate due to other predators (seals, sea birds, etc.) or
     * disease. For school of age 0 it returns the egg mortality rate at current
     * time step.
     *
     * @param school, a school of the system
     * @return the natural mortality rate for the current time step, and the 
     * larva mortality rate for school of age 0.
     */
    @Override
    public double getRate(School school) {
        double D;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            // Egg stage
            D = eggMortality[school.getSpeciesIndex()].getRate(school);
        } else {
            if (null != spatialD[spec.getIndex()] && !school.isUnlocated()) {
                D = (spatialD[spec.getIndex()].getValue(school.getCell()) * additionalMortality[school.getSpeciesIndex()].getRate(school));
            } else {
                D = additionalMortality[school.getSpeciesIndex()].getRate(school);
            }
        }
        return D;
    }
}
