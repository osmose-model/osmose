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

package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.growth.AbstractGrowth;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 *
 * @author pverley
 */
public class GrowthProcess extends AbstractProcess {

    private AbstractGrowth[] growth;
    private double[][] minDelta;
    private double[][] maxDelta;
    private double[][]deltaMeanLength;
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

        int cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            // Initialize growth function
            String growthClassName = getConfiguration().isNull("growth.java.classname.sp" + i)
                    ? "fr.ird.osmose.process.growth.VonBertalanffyGrowth"
                    : getConfiguration().getString("growth.java.classname.sp" + i);
            String errMsg = "Failed to instantiate Growth function " + growthClassName + " for species " + getSpecies(cpt).getName();
            try {
                growth[cpt] = (AbstractGrowth) Class.forName(growthClassName).getConstructor(Integer.TYPE, Species.class).newInstance(getRank(), getSpecies(i));
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
                error(errMsg, ex);
            }
            // Initializes Growth function
            growth[cpt].init();

            criticalPredSuccess[cpt] = getConfiguration().getDouble("predation.efficiency.critical.sp" + i);
            Species species = getSpecies(cpt);
            int lifespan = species.getLifespanDt();
            minDelta[cpt] = new double[lifespan];
            maxDelta[cpt] = new double[lifespan];
            deltaMeanLength[cpt] = new double[lifespan];
                
            // barrier.n: patch for Fabien to limit the maximum grow rate
            double delta_lmax_factor = (getConfiguration().isNull("species.delta.lmax.factor.sp" + i)) ? 2 : getConfiguration().getDouble("species.delta.lmax.factor.sp" + i); 
            
            double meanLength1 = growth[cpt].ageToLength(0);
            for (int ageDt = 0; ageDt < lifespan - 1; ageDt++) {
                double meanLength0 = meanLength1;
                meanLength1 = growth[cpt].ageToLength((ageDt + 1) / (double) getConfiguration().getNStepYear());
                deltaMeanLength[cpt][ageDt] = meanLength1 - meanLength0;

                // barrier.n: patch for Fabien to limit the maximum grow rate
                //maxDelta[i][ageDt] = deltaMeanLength[i][ageDt] + deltaMeanLength[i][ageDt];
                minDelta[cpt][ageDt] = deltaMeanLength[cpt][ageDt] - deltaMeanLength[cpt][ageDt];
                maxDelta[cpt][ageDt] = delta_lmax_factor * deltaMeanLength[cpt][ageDt];
            }
            // Read maximal length
            if (!getConfiguration().isNull("species.lmax.sp" + i)) {
                lmax[cpt] = getConfiguration().getFloat("species.lmax.sp" + i);
            } else {
                lmax[cpt] =  Float.POSITIVE_INFINITY;
            }
            
            cpt++;
            
        }  // end of loop on focal species
    }

    @Override
    public void run() {
        for (School school : getSchoolSet().getAliveSchools()) {
            Species species = school.getSpecies();
            int i = species.getSpeciesIndex();
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
    