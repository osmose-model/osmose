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
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.growth.AbstractGrowth;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author pverley
 */
public class GrowthProcess extends AbstractProcess {

    private AbstractGrowth[] growth;
    private double[][] minDelta;
    private double[][] maxDelta;
    private double[][] deltaMeanLength;
    private double[] criticalPredSuccess;
    /**
     * Maximum length for every species. Infinity by default. Parameter
     * species.lmax.sp#
     */
    private float[] lmax;

    public GrowthProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        growth = new AbstractGrowth[nSpecies];
        criticalPredSuccess = new double[nSpecies];
        minDelta = new double[nSpecies][];
        maxDelta = new double[nSpecies][];
        deltaMeanLength = new double[nSpecies][];
        lmax = new float[nSpecies];

        for (int i = 0; i < nSpecies; i++) {
            // Initialize growth function
            String growthClassName = getConfiguration().isNull("growth.java.classname.sp" + i)
                    ? "fr.ird.osmose.process.growth.VonBertalanffyGrowth"
                    : getConfiguration().getString("growth.java.classname.sp" + i);
            String errMsg = "Failed to instantiate Growth function " + growthClassName + " for species " + getSpecies(i).getName();
            try {
                growth[i] = (AbstractGrowth) Class.forName(growthClassName).getConstructor(Integer.TYPE, Species.class).newInstance(getRank(), getSpecies(i));
            } catch (InstantiationException ex) {
                error(errMsg, ex);
            } catch (IllegalAccessException ex) {
                error(errMsg, ex);
            } catch (IllegalArgumentException ex) {
                error(errMsg, ex);
            } catch (InvocationTargetException ex) {
                error(errMsg, ex);
            } catch (NoSuchMethodException ex) {
                error(errMsg, ex);
            } catch (SecurityException ex) {
                error(errMsg, ex);
            } catch (ClassNotFoundException ex) {
                error(errMsg, ex);
            }
            // Initializes Growth function
            growth[i].init();

            criticalPredSuccess[i] = getConfiguration().getFloat("predation.efficiency.critical.sp" + i);
            Species species = getSpecies(i);
            int lifespan = species.getLifespanDt();
            minDelta[i] = new double[lifespan];
            maxDelta[i] = new double[lifespan];
            deltaMeanLength[i] = new double[lifespan];

            double meanLength1 = growth[i].ageToLength(0);
            for (int ageDt = 0; ageDt < lifespan - 1; ageDt++) {
                double meanLength0 = meanLength1;
                meanLength1 = growth[i].ageToLength((ageDt + 1) / (double) getConfiguration().getNStepYear());
                deltaMeanLength[i][ageDt] = meanLength1 - meanLength0;

                minDelta[i][ageDt] = deltaMeanLength[i][ageDt] - deltaMeanLength[i][ageDt];
                maxDelta[i][ageDt] = deltaMeanLength[i][ageDt] + deltaMeanLength[i][ageDt];
            }
            // Read maximal length
            if (!getConfiguration().isNull("species.lmax.sp" + i)) {
                lmax[i] = getConfiguration().getFloat("species.lmax.sp" + i);
            } else {
                lmax[i] = Float.POSITIVE_INFINITY;
            }
        }
    }

    @Override
    public void run() {
        for (School school : getSchoolSet().getAliveSchools()) {
            Species species = school.getSpecies();
            int i = species.getIndex();
            int age = school.getAgeDt();
            if ((age == 0) || school.isUnlocated()) {
                // Linear growth for eggs and migrating schools
                school.incrementLength((float) deltaMeanLength[i][age]);
            } else {
                // Growth based on predation success
                if (school.getLength() < lmax[i]) {
                    grow(school, minDelta[i][age], maxDelta[i][age]);
                }
            }
        }
    }

    private void grow(School school, double minDelta, double maxDelta) {

        int iSpec = school.getSpeciesIndex();
        //calculation of lengths according to predation efficiency
        if (school.getPredSuccessRate() >= criticalPredSuccess[iSpec]) {
            double dlength = (minDelta + (maxDelta - minDelta) * ((school.getPredSuccessRate() - criticalPredSuccess[iSpec]) / (1 - criticalPredSuccess[iSpec])));
            school.incrementLength((float) dlength);
        }
    }

    public AbstractGrowth getGrowth(int indexSpecies) {
        return growth[indexSpecies];
    }
}
