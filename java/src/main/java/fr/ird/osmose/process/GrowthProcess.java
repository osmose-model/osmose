/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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

    private HashMap<Integer, AbstractGrowth> growth;
    private HashMap<Integer, double[]> minDelta;
    private HashMap<Integer, double[]> maxDelta;
    private HashMap<Integer, double[]> deltaMeanLength;
    private HashMap<Integer, Double> criticalPredSuccess;
    /**
     * Maximum length for every species. Infinity by default. Parameter
     * species.lmax.sp#
     */
    private HashMap<Integer, Float> lmax;

    public GrowthProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        growth = new HashMap();
        criticalPredSuccess = new HashMap();
        minDelta = new HashMap();
        maxDelta = new HashMap();
        deltaMeanLength = new HashMap();
        lmax = new HashMap();

        for (int i : getConfiguration().getFocalIndex()) {
            // Initialize growth function
            String growthClassName = getConfiguration().isNull("growth.java.classname.sp" + i)
                    ? "fr.ird.osmose.process.growth.VonBertalanffyGrowth"
                    : getConfiguration().getString("growth.java.classname.sp" + i);
            String errMsg = "Failed to instantiate Growth function " + growthClassName + " for species " + getSpecies(i).getName();
            try {
                growth.put(i, (AbstractGrowth) Class.forName(growthClassName).getConstructor(Integer.TYPE, Species.class).newInstance(getRank(), getSpecies(i)));
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
            growth.get(i).init();

            criticalPredSuccess.put(i, getConfiguration().getDouble("predation.efficiency.critical.sp" + i));
            Species species = getSpecies(i);
            int lifespan = species.getLifespanDt();
            minDelta.put(i, new double[lifespan]);
            maxDelta.put(i, new double[lifespan]);
            deltaMeanLength.put(i, new double[lifespan]);
                
            // barrier.n: patch for Fabien to limit the maximum grow rate
            double delta_lmax_factor = (getConfiguration().isNull("species.delta.lmax.factor.sp" + i)) ? 2 : getConfiguration().getDouble("species.delta.lmax.factor.sp" + i); 
            
            double meanLength1 = growth.get(i).ageToLength(0);
            for (int ageDt = 0; ageDt < lifespan - 1; ageDt++) {
                double meanLength0 = meanLength1;
                meanLength1 = growth.get(i).ageToLength((ageDt + 1) / (double) getConfiguration().getNStepYear());
                deltaMeanLength.get(i)[ageDt] = meanLength1 - meanLength0;

                // barrier.n: patch for Fabien to limit the maximum grow rate
                //maxDelta[i][ageDt] = deltaMeanLength[i][ageDt] + deltaMeanLength[i][ageDt];
                minDelta.get(i)[ageDt] = deltaMeanLength.get(i)[ageDt] - deltaMeanLength.get(i)[ageDt];
                maxDelta.get(i)[ageDt] = delta_lmax_factor * deltaMeanLength.get(i)[ageDt];
            }
            // Read maximal length
            if (!getConfiguration().isNull("species.lmax.sp" + i)) {
                lmax.put(i, getConfiguration().getFloat("species.lmax.sp" + i));
            } else {
                lmax.put(i, Float.POSITIVE_INFINITY);
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
                school.incrementLength((float) deltaMeanLength.get(i)[age]);
            } else {
                // Growth based on predation success
                if (school.getLength() < lmax.get(i)) {
                    grow(school, minDelta.get(i)[age], maxDelta.get(i)[age]);
                }
            }
        }
    }

    private void grow(School school, double minDelta, double maxDelta) {

        int iSpec = school.getSpeciesIndex();
        //calculation of lengths according to predation efficiency
        if (school.getPredSuccessRate() >= criticalPredSuccess.get(iSpec)) {
                double dlength = (minDelta + (maxDelta - minDelta) * ((school.getPredSuccessRate() - criticalPredSuccess.get(iSpec)) / (1 - criticalPredSuccess.get(iSpec))));
            school.incrementLength((float) dlength);
        }
    }

    public AbstractGrowth getGrowth(int indexSpecies) {
        return growth.get(indexSpecies);
    }
}
    