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

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Prey.MortalityCause;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.fishing.AnnualFByYearSeasonScenario;
import fr.ird.osmose.process.fishing.AnnualFScenario;
import fr.ird.osmose.process.fishing.AnnualFSeasonScenario;
import fr.ird.osmose.process.fishing.ByDtByAgeSizeScenario;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.MPA;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public class FishingProcess extends AbstractProcess {
    
    private AbstractMortalityScenario[] fishingScenario;
    private List<MPA> mpas;
    private GridMap mpaFactor;
    
    public FishingProcess(int rank) {
        super(rank);
    }
    
    @Override
    public void init() {
        fishingScenario = new AbstractMortalityScenario[getNSpecies()];
        // Find fishing scenario
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            int rank = getRank();
            Species species = getSpecies(iSpec);
            // Fishing rate by Dt, by Age or Size
            if (!getConfiguration().isNull("mortality.fishing.rate.byDt.byAge.file.sp" + iSpec)
                    || !getConfiguration().isNull("mortality.fishing.rate.byDt.bySize.file.sp" + iSpec)) {
                fishingScenario[iSpec] = new ByDtByAgeSizeScenario(rank, species);
                continue;
            }
            // Annual fishing rate by Year
            if (!getConfiguration().isNull("mortality.fishing.rate.byYear.file.sp" + iSpec)) {
                fishingScenario[iSpec] = new AnnualFByYearSeasonScenario(rank, species);
                continue;
            }
            // Annual fishing rate
            if (!getConfiguration().isNull("mortality.fishing.rate.sp" + iSpec)) {
                if (!getConfiguration().isNull("mortality.fishing.season.distrib.file.sp" + iSpec)) {
                    fishingScenario[iSpec] = new AnnualFSeasonScenario(rank, species);
                } else {
                    fishingScenario[iSpec] = new AnnualFScenario(rank, species);
                }
            }
        }

        // Initialize fishing scenario
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            fishingScenario[iSpec].init();
        }

        // Loads the MPAs
        int nMPA = getConfiguration().findKeys("mpa.file.mpa*").size();
        mpas = new ArrayList(nMPA);
        for (int iMPA = 0; iMPA < nMPA; iMPA++) {
            mpas.add(new MPA(getRank(), iMPA));
        }
        for (MPA mpa : mpas) {
            mpa.init();
        }
        // Initialize MPA correction factor
        mpaFactor = new GridMap(1);
    }
    
    @Override
    public void run() {
        update();
        for (School school : getSchoolSet().getPresentSchools()) {
            if (school.getAbundance() != 0.d) {
                double F = getInstantaneousRate(school, 1);
                double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-F));
                if (nDead > 0.d) {
                    school.setNdead(MortalityCause.FISHING, nDead);
                }
            }
        }
    }
    
    public void update() {
        
        boolean isUpToDate = true;
        int iStep = getSimulation().getIndexTimeSimu();
        for (MPA mpa : mpas) {
            isUpToDate &= (mpa.isActive(iStep - 1) == mpa.isActive(iStep));
        }
        if (!isUpToDate) {
            mpaFactor = new GridMap(1);
            int nCellMPA = 0;
            for (MPA mpa : mpas) {
                if (mpa.isActive(iStep)) {
                    for (Cell cell : mpa.getCells()) {
                        mpaFactor.setValue(cell, 0.f);
                        nCellMPA++;
                    }
                }
            }
            int nOceanCell = getGrid().getNOceanCell();
            float correction = (float) nOceanCell / (nOceanCell - nCellMPA);
            for (Cell cell : getGrid().getCells()) {
                if (mpaFactor.getValue(cell) > 0.f) {
                    mpaFactor.setValue(cell, correction);
                }
            }
        }
    }
    
    public double getInstantaneousRate(School school, int subdt) {
        if (!school.isUnlocated()) {
            return fishingScenario[school.getSpeciesIndex()].getInstantaneousRate(school) * mpaFactor.getValue(school.getCell());
        } else {
            return 0.d;
        }
    }

    /*
     * F the annual mortality rate is calculated as the annual average
     * of the fishing rates over the years. 
     */
    public double getAnnualRate(Species species) {
        return fishingScenario[species.getIndex()].getAnnualRate();
    }
}
