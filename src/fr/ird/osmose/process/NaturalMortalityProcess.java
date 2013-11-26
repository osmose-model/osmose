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
package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.School.MortalityCause;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.naturalmortality.AnnualNaturalMortalityScenario;
import fr.ird.osmose.process.naturalmortality.ByDtByAgeSizeNaturalMortalityScenario;
import fr.ird.osmose.process.naturalmortality.ByDtLarvaMortalityScenario;
import fr.ird.osmose.process.naturalmortality.ByDtNaturalMortalityScenario;
import fr.ird.osmose.process.naturalmortality.ConstantLarvaMortalityScenario;
import fr.ird.osmose.util.GridMap;
import java.util.List;

/**
 *
 * @author pverley
 */
public class NaturalMortalityProcess extends AbstractProcess {

    private AbstractMortalityScenario[] larvaMortality;
    private AbstractMortalityScenario[] naturalMortality;
    /**
     * Spatial factor for natural mortality [0, 1]
     */
    private GridMap[] spatialD;

    public NaturalMortalityProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int rank = getRank();

        // Larva mortality
        larvaMortality = new AbstractMortalityScenario[getNSpecies()];
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSpecies(iSpec);
            // Larva mortality by Dt
            if (!getConfiguration().isNull("mortality.natural.larva.rate.bytDt.file.sp" + iSpec)) {
                larvaMortality[iSpec] = new ByDtLarvaMortalityScenario(rank, species);
                continue;
            }
            // Constant larva mortality
            if (!getConfiguration().isNull("mortality.natural.larva.rate.sp" + iSpec)) {
                larvaMortality[iSpec] = new ConstantLarvaMortalityScenario(rank, species);
                continue;
            }
            // Did not find any scenario for larva mortality, Osmose assumes larva mortality = 0.
            // Warning only because some species might not reproduce in the 
            // simulated area and therefore have no need to define larva mortality
            larvaMortality[iSpec] = new ConstantLarvaMortalityScenario(rank, species, 0.f);
            getSimulation().warning("Could not find any parameters for larva mortality (mortality.natural.larva.rate.bytDt.file.sp# or mortality.natural.larva.rate.sp#) for species {0}. Osmose assumes larva mortality = 0", species.getName());
        }

        // Natural mortality
        naturalMortality = new AbstractMortalityScenario[getNSpecies()];

        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSpecies(iSpec);
            // Natural mortality by Dt, by Age or Size
            if (!getConfiguration().isNull("mortality.natural.rate.byDt.byAge.file.sp" + iSpec)
                    || !getConfiguration().isNull("mortality.natural.rate.byDt.bySize.file.sp" + iSpec)) {
                naturalMortality[iSpec] = new ByDtByAgeSizeNaturalMortalityScenario(rank, species);
                continue;
            }
            // Natural mortality by Dt
            if (!getConfiguration().isNull("mortality.natural.rate.bytDt.file.sp" + iSpec)) {
                larvaMortality[iSpec] = new ByDtNaturalMortalityScenario(rank, species);
                continue;
            }
            // Annual natural mortality
            if (!getConfiguration().isNull("mortality.natural.rate.sp" + iSpec)) {
                naturalMortality[iSpec] = new AnnualNaturalMortalityScenario(rank, species);
                continue;
            }
            // Did not find any scenario for natural mortality. Throws error.
            getSimulation().error("Could not find any parameters for natural mortality (mortality.natural.rate.byDt.byAge.file.sp# or mortality.natural.rate.byDt.bySize.file.sp mortality.natural.rate.byDt.file.sp or mortality.natural.rate.sp#) for species " + species.getName(), null);
        }

        // Initialize mortality scenarii
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            larvaMortality[iSpec].init();
            naturalMortality[iSpec].init();
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

    @Override
    public void run() {
        // Natural mortality (due to other predators)
        for (School school : getSchoolSet()) {
            double M = getInstantaneousRate(school, 1);
            double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-M));
            if (nDead > 0.d) {
                school.setNdead(MortalityCause.NATURAL, nDead);
            }
        }
    }

    /**
     * For all species, D is due to other predators (seals, seabirds) for
     * migrating species, we add mortality because absents during a time step so
     * they don't undergo mortalities due to predation and starvation Additional
     * mortalities for ages 0: no-fecundation of eggs, starvation more
     * pronounced than for sup ages (rel to CC), predation by other species are
     * not explicit.
     */
    public double getInstantaneousRate(School school, int subdt) {
        double M;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            M = larvaMortality[school.getSpeciesIndex()].getInstantaneousRate(school) / (float) subdt;
        } else {
            if (null != spatialD[spec.getIndex()] && !school.isUnlocated()) {
                M = (spatialD[spec.getIndex()].getValue(school.getCell()) * naturalMortality[school.getSpeciesIndex()].getInstantaneousRate(school)) / (float) subdt;
            } else {
                M = naturalMortality[school.getSpeciesIndex()].getInstantaneousRate(school) / (float) (subdt);
            }
        }
        return M;
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the larval mortality rates over the years.
     */
    public double getLarvalAnnualRate(Species species) {
        return larvaMortality[species.getIndex()].getAnnualRate();
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the natural mortality rates over the years.
     */
    public double getAnnualRate(Species species) {
        return naturalMortality[species.getIndex()].getAnnualRate();
    }
}
