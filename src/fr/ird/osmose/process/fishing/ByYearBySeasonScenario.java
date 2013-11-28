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
package fr.ird.osmose.process.fishing;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.FishingProcess.FishingType;
import fr.ird.osmose.util.timeseries.ByYearTimeSeries;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

/**
 *
 * @author pverley
 */
public class ByYearBySeasonScenario extends AbstractFishingScenario {

    private float[] annualF;
    private float[] annualCatches;
    private float[] season;
    private int recruitmentAge;
    private float recruitmentSize;
    private final FishingType type;
    private double fishableBiomass;

    public ByYearBySeasonScenario(int rank, Species species, FishingType type) {
        super(rank, species);
        this.type = type;
    }

    @Override
    public void init() {
        int nStepYear = getConfiguration().getNStepYear();
        int iSpec = getIndexSpecies();

        String filename;
        ByYearTimeSeries yts;
        switch (type) {
            case RATE:
                // Read annual F by year
                filename = getConfiguration().getFile("mortality.fishing.rate.byYear.file.sp" + iSpec);
                yts = new ByYearTimeSeries(getRank());
                yts.read(filename);
                annualF = yts.getValues();
                annualCatches = new float[annualF.length];
                break;
            case CATCHES:
                // Read annual F by year
                filename = getConfiguration().getFile("mortality.fishing.catches.byYear.file.sp" + iSpec);
                yts = new ByYearTimeSeries(getRank());
                yts.read(filename);
                annualCatches = yts.getValues();
                annualF = new float[annualCatches.length];
                break;
        }

        // Read recruitment size or age
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
            getSimulation().warning("Could not find any fishing recruitment threshold (neither age nor size) for species {0}. Osmose assumes every school can be catched.", getSpecies().getName());
        }

        // Read seasonality
        SingleTimeSeries sts = new SingleTimeSeries(getRank());
        filename = getConfiguration().getFile("mortality.fishing.season.distrib.file.sp" + iSpec);
        sts.read(filename, nStepYear, nStepYear);
        season = sts.getValues();
    }

    @Override
    public float getInstantaneousRate(School school) {
        return (school.getAgeDt() >= recruitmentAge) && (school.getLength() >= recruitmentSize)
                ? annualF[getSimulation().getYear()] * season[getSimulation().getIndexTimeYear()]
                : 0.f;
    }

    @Override
    public float getAnnualRate() {
        double F = 0;
        for (int iYear = 0; iYear < annualF.length; iYear++) {
            F += annualF[iYear];
        }
        F = F / getConfiguration().getNYear();
        return (float) F;
    }

    @Override
    public float getInstantaneousCatches(School school) {
        if (isFishable(school) && (fishableBiomass > 0.d)) {
            return (float) (school.getInstantaneousBiomass() / fishableBiomass)
                    * annualCatches[getSimulation().getYear()]
                    * season[getSimulation().getIndexTimeYear()];
        }
        return 0.f;
    }

    @Override
    public void assessFishableBiomass() {
        fishableBiomass = 0.d;
        for (School school : getSchoolSet().getSchools(getSpecies(), false)) {
            if (!school.isUnlocated() && isFishable(school)) {
                fishableBiomass += school.getInstantaneousBiomass();
            }
        }
    }

    private boolean isFishable(School school) {
        return ((school.getAgeDt() >= recruitmentAge) && (school.getLength() >= recruitmentSize));
    }
}
