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
import fr.ird.osmose.util.timeseries.ByClassTimeSeries;
import java.util.HashMap;

/**
 * This class simulates a flux of individuals (biomass) of given age and length
 * in the simulated domain throughout time. Therefore it allows to take into
 * account some species that were not spawned inside the simulated domain and
 * only that enter the domain at a later stage. It acts as a migrating process
 * (for species that do not fulfil a complete life cycle inside the simulated
 * domain but are too significant in terms of biomass or impact upon the other
 * species to be ignored) except that it only controls how a species enters the
 * domain. This is why the class has been called IncomingFluxProcess rather than
 * MigrationProcess, as it does not control when a species leaves temporarily or
 * permanently the domain (refer to MovementProcess for such feature).
 * <br />
 * For each species, the user can provide time series of incoming biomass by
 * time step, structured in age or size class. The incoming flux is provided in
 * a CSV file and is formatted as follow:
 * <br />
 * <table>
 * <tr>
 * <td>time/class</td> <td>class0</td> <td>class1</td> <td>...</td>
 * <td>classn</td>
 * </tr>
 * <tr>
 * <td>t0</td> <td>biomass_class0_t0</td> <td>biomass_class1_t0</td>
 * <td>...</td> <td>biomass_classn_t0</td>
 * </tr>
 * <tr>
 * <td>t1</td> <td>biomass_class0_t1</td> <td>biomass_class1_t1</td>
 * <td>...</td> <td>biomass_classn_t1</td>
 * </tr>
 * <tr>
 * <td>...</td> <td>...</td> <td>...</td> <td>...</td> <td>...</td>
 * </tr>
 * <tr>
 * <td>tm</td> <td>biomass_class0_tm</td> <td>biomass_class1_tm</td>
 * <td>...</td> <td>biomass_classn_tm</td>
 * </tr>
 * </table>
 * The first row of the file details the age or size structure. Each column of
 * the first row is the lower bound of the age/size class. Age is expressed in
 * year and size in centimetre. There may be as much classes as needed. Osmose
 * will set the age (or size) of the incoming individual as the middle of the
 * age classes. For the last class, Osmose will set the age as the middle of the
 * last age class and the lifespan (or the last size class and the length
 * infinity). For instance if age_class0 is 0, age_class1 is 1 and age_class2 is
 * 2 for a species whose lifespan is 5 years, incoming individuals will be
 * respectively 0.5, 1.5 and 3.5 years old. The biomass are expressed in tonnes.
 * For a given time step and for every age (or size) class with a non-zero
 * incoming biomass, Osmose will distribute the biomass over n new schools, n
 * being the number of schools defined by parameter 'simulation.nschool.sp#'
 * <br />
 * The time series must contain a number of steps that is a multiple of the
 * number of steps per year. If the time series is shorter than the simulation
 * Osmose will loop over it. If the time series is longer than the simulation
 * Osmose will ignore the exceeding years.
 * <br />
 * The class accepts two parameters (one or the other, cannot be both for one
 * species)
 * <ul>
 * <li>flux.incoming.byDt.byAge.file.sp#</li>
 * <li>flux.incoming.byDt.bySize.file.sp#</li>
 * </ul>
 */
public class IncomingFluxProcess extends AbstractProcess {

    /*
     * Flux of incoming biomass in tonne, by dt and by age/size class
     */
    private HashMap<Integer, double[][]> biomassIn;
    /*
     * Length of incomimg fish, in centimeter, per species and by size/age class
     */
    private HashMap<Integer, float[]> lengthIn;
    /*
     * Age of incoming fish, in number of time steps, per species and by size/age class
     */
    private  HashMap<Integer,int[]> ageIn;

    public IncomingFluxProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        biomassIn = new HashMap();
        lengthIn = new HashMap();
        ageIn = new HashMap();
        // Call the growth process to be able to calculate meanAgeIn or meanLengthIn
        GrowthProcess growthProcess = new GrowthProcess(getRank());
        growthProcess.init();
        for (int iSpec : getConfiguration().getFocalIndex()) {
            if (!getConfiguration().isNull("flux.incoming.byDt.byAge.file.sp" + iSpec)) {
                ByClassTimeSeries timeSerieByAge = new ByClassTimeSeries();
                timeSerieByAge.read(getConfiguration().getFile("flux.incoming.byDt.byAge.file.sp" + iSpec));
                biomassIn.put(iSpec, timeSerieByAge.getValues());
                // Read age from file, and set ageIn as the middle of the age classes
                ageIn.put(iSpec, new int[timeSerieByAge.getNClass()]);
                for (int iAge = 0; iAge < ageIn.get(iSpec).length - 1; iAge++) {
                    ageIn.get(iSpec)[iAge] = (int) (getConfiguration().getNStepYear() * (0.5 * (timeSerieByAge.getClass(iAge) + timeSerieByAge.getClass(iAge + 1))));
                }
                // Last ageIn as the middle of interval between last class and lifespan
                int lifespan = getSpecies(iSpec).getLifespanDt();
                ageIn.get(iSpec)[ageIn.get(iSpec).length - 1] = (int) (0.5 * (getConfiguration().getNStepYear() * timeSerieByAge.getClass(ageIn.get(iSpec).length - 1) + lifespan));
                // Compute corresponding length in with Von Bertallanfy
                lengthIn.put(iSpec, new float[timeSerieByAge.getNClass()]);
                for (int iAge = 0; iAge < ageIn.get(iSpec).length; iAge++) {
                    double age = ageIn.get(iSpec)[iAge] / (double) getConfiguration().getNStepYear();
                    lengthIn.get(iSpec)[iAge] = (float) growthProcess.getGrowth(iSpec).ageToLength(age);   
                }
            } else if (!getConfiguration().isNull("flux.incoming.byDt.bySize.file.sp" + iSpec)) {
                ByClassTimeSeries timeSerieBySize = new ByClassTimeSeries();
                timeSerieBySize.read(getConfiguration().getFile("flux.incoming.byDt.bySize.file.sp" + iSpec));
                biomassIn.put(iSpec, timeSerieBySize.getValues());
                // Read length from file and set lengthIn as middle of the length classes
                lengthIn.put(iSpec, new float[timeSerieBySize.getNClass()]);
                for (int iLength = 0; iLength < lengthIn.get(iSpec).length - 1; iLength++) {
                    lengthIn.get(iSpec)[iLength] = (float) (0.5 * (timeSerieBySize.getClass(iLength) + timeSerieBySize.getClass(iLength + 1)));
                }
                // Last lengthIn as the middle of interval between last class and length infinity
                float lInf = getConfiguration().getFloat("species.linf.sp" + iSpec);
                lengthIn.get(iSpec)[lengthIn.get(iSpec).length - 1] = (float) (0.5 * (timeSerieBySize.getClass(lengthIn.get(iSpec).length - 1) + lInf));
                // Compute corresponding age in with Von Bertallanfy
                ageIn.put(iSpec, new int[timeSerieBySize.getNClass()]);
                for (int iLength = 0; iLength < ageIn.get(iSpec).length; iLength++) {
                    double age = growthProcess.getGrowth(iSpec).lengthToAge(lengthIn.get(iSpec)[iLength]);
                    ageIn.get(iSpec)[iLength] = (int) (age * getConfiguration().getNStepYear());
                }
            } else {
                // Nothing to do, means there is no incoming flux for this species
            }
        }
    }

    @Override
    public void run() {
        int iTime = getSimulation().getIndexTimeSimu();
        for (int iSpec : getConfiguration().getFocalIndex()) {
            // No incoming flux for this species, skip
            if (biomassIn.get(iSpec) == null) {
                continue;
            }
            Species species = getSpecies(iSpec);
            // number of school gets a peculiar meaning here: it is the number
            // of school created per age/size class
            // Should be changed in future to match original meaning
            int nSchool = getConfiguration().getNSchool(iSpec);
            // Loop over age/size class
            for (int iClass = 0; iClass < biomassIn.get(iSpec)[iTime].length; iClass++) {
                // Compute corresponding weight to estimate abundance from biomassIn
                float meanWeigthIn = (float) species.computeWeight(lengthIn.get(iSpec)[iClass]);
                long abundanceIn = (long) Math.round(biomassIn.get(iSpec)[iTime][iClass] * 1000000.d / meanWeigthIn);
                // Abundance smaller than number of schools, then create only one school
                if (abundanceIn > 0 && abundanceIn < nSchool) {
                    getSchoolSet().add(new School(species, abundanceIn, lengthIn.get(iSpec)[iClass], meanWeigthIn, ageIn.get(iSpec)[iClass]));
                } else if (abundanceIn >= nSchool) {
                    double abdSchool = abundanceIn / nSchool;
                    for (int s = 0; s < nSchool; s++) {
                        getSchoolSet().add(new School(species, abdSchool, lengthIn.get(iSpec)[iClass], meanWeigthIn, ageIn.get(iSpec)[iClass]));
                    }
                }
            }
        }
    }
}
