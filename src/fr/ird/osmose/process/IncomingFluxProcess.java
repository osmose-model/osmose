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
import fr.ird.osmose.Species;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import fr.ird.osmose.util.timeseries.SpeciesTimeSeries;

/**
 *
 * @author pverley
 */
public class IncomingFluxProcess extends AbstractProcess {

    /**
     * Distribution of the spawning throughout the year
     */
    private float[][] seasonFlux;
    /*
     * Annual flux of incoming biomass in tons
     */
    private double[] biomassFluxIn;
    /*
     * Mean length of incomimg fish
     */
    private float[] meanLengthIn;
    /*
     * Mean weight of incoming fish
     */
    private int[] ageMeanIn;

    public IncomingFluxProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        biomassFluxIn = new double[nSpecies];
        meanLengthIn = new float[nSpecies];
        ageMeanIn = new int[nSpecies];
        if (!getConfiguration().isNull("flux.incoming.season.file")) {
            SpeciesTimeSeries ts = new SpeciesTimeSeries(getRank());
            ts.read(getConfiguration().getFile("flux.incoming.season.file"));
            seasonFlux = ts.getValues();
        } else {
            seasonFlux = new float[nSpecies][];
            for (int i = 0; i < nSpecies; i++) {
                if (!getConfiguration().isNull("flux.incoming.season.file.sp" + i)) {
                    SingleTimeSeries ts = new SingleTimeSeries(getRank());
                    ts.read(getConfiguration().getFile("flux.incoming.season.file.sp" + i));
                     seasonFlux[i] = ts.getValues();
                } else {
                    seasonFlux[i] = new float[0];
                }
            }
        }

        for (int i = 0; i < nSpecies; i++) {
            double sum = 0;
            for (double d : seasonFlux[i]) {
                sum += d;
            }
            if (sum > 0.d) {
                biomassFluxIn[i] = getConfiguration().getFloat("flux.incoming.annual.biomass.sp" + i);
                meanLengthIn[i] = getConfiguration().getFloat("flux.incoming.size.sp" + i);
                ageMeanIn[i] = (int) Math.round(getConfiguration().getFloat("flux.incoming.age.sp" + i) * getConfiguration().getNStepYear());
            }
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (biomassFluxIn[i] == 0.d) {
                continue;
            }
            Species species = getSpecies(i);
            /*
             * Incoming flux
             */
            double season = getSeason(getSimulation().getIndexTimeSimu(), species);
            double biomassIn = biomassFluxIn[i] * season;
            float meanWeigthIn = (float) species.computeWeight(meanLengthIn[i]);
            long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
            int nSchool = getConfiguration().getNSchool(i);
            if (abundanceIn > 0 && abundanceIn < nSchool) {
                getSchoolSet().add(new School(species, abundanceIn, meanLengthIn[i], meanWeigthIn, ageMeanIn[i]));
            } else if (abundanceIn >= nSchool) {
                double abdSchool = abundanceIn / nSchool;
                for (int s = 0; s < nSchool; s++) {
                    getSchoolSet().add(new School(species, abdSchool, meanLengthIn[i], meanWeigthIn, ageMeanIn[i]));
                }
            }
        }
    }

    private double getSeason(int iStepSimu, Species species) {
        int iSpec = species.getIndex();
        return seasonFlux[iSpec][iStepSimu];
    }
}
