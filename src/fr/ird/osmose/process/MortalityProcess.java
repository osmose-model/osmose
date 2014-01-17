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
import fr.ird.osmose.Prey;
import fr.ird.osmose.School;
import fr.ird.osmose.Prey.MortalityCause;
import fr.ird.osmose.School.PreyRecord;
import fr.ird.osmose.process.FishingProcess.FishingType;
import fr.ird.osmose.stage.DietOutputStage;
import fr.ird.osmose.stage.IStage;
import fr.ird.osmose.util.XSRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * @author pverley
 */
public class MortalityProcess extends AbstractProcess {

    /*
     * Random generator
     */
    private static Random random;
    /*
     * Subdivise the main time step in smaller time steps for applying
     * mortality. Should only be 1 so far, still problems to fix.
     */
    private int subdt;
    /*
     * Private instance of the natural mortality process
     */
    private NaturalMortalityProcess naturalMortalityProcess;
    /*
     * Private instance of the fishing process
     */
    private FishingProcess fishingProcess;
    /*
     * Private instance of the starvation process
     */
    private StarvationProcess starvationProcess;
    /*
     * Private instance of the predation process
     */
    private PredationProcess predationProcess;
    /*
     * Private instance of the out of zone mortality process
     */
    private OutMortalityProcess outMortalityProcess;
    /*
     * Diet output stage
     */
    private IStage dietOutputStage;
    /**
     * Epsilon constant for numerical purpose to avoid zeros at denominator when
     * calculating mortality rates.
     */
    private final double epsilon = 0.01d;

    /**
     * Several mortality algorithms have been implemented at the time of coding
     * Osmose version 3.
     */
    private enum MortalityAlgorithm {

        /**
         * Mortality rates are obtained through an iterative process.
         * <ul>
         * <li>It is assumed that every cause is independant and
         * concomitant.</li>
         * <li>No stochasticity neither competition within predation process:
         * every predator sees preys as they are at the begining of the
         * time-step.</li>
         * <li>Synchromous updating of school biomass.</li>
         * </ul>
         */
        ITERATIVE,
        /**
         * Mortality processes compete stochastically.
         * <ul>
         * <li>It is assumed that every cause compete with each other.</li>
         * <li>Stochasticity and competition within predation process.</li>
         * <li>Asynchronous updating of school biomass (it means biomass are
         * updated on the fly).</li>
         * </ul>
         */
        STOCHASTIC;
    }
    /**
     * Sets the mortality algorithm. Change carefully as it will affect the
     * whole dynamics of the model.
     *
     * @see MortalityAlgorithm for details.
     */
    private MortalityAlgorithm mortalityAlgorithm;

    public MortalityProcess(int rank) {
        super(rank);
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
    }

    @Override
    public void init() {
        random = new XSRandom(System.nanoTime());

        naturalMortalityProcess = new NaturalMortalityProcess(getRank());
        naturalMortalityProcess.init();

        fishingProcess = new FishingProcess(getRank());
        fishingProcess.init();

        starvationProcess = new StarvationProcess(getRank());
        starvationProcess.init();

        predationProcess = new PredationProcess(getRank());
        predationProcess.init();

        outMortalityProcess = new OutMortalityProcess(getRank());
        outMortalityProcess.init();

        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();

        // Sets the mortality algorithm
        try {
            mortalityAlgorithm = MortalityAlgorithm.valueOf(getConfiguration().getString("mortality.algorithm").toUpperCase());
        } catch (Exception ex) {
            mortalityAlgorithm = MortalityAlgorithm.STOCHASTIC;
            warning("Default mortality algorithm set to " + mortalityAlgorithm.toString());
        }

        // Subdt for case3 FULLY_STOCHASTIC
        if (!getConfiguration().isNull("mortality.subdt")) {
            subdt = getConfiguration().getInt("mortality.subdt");
        } else {
            subdt = 1;
            if (mortalityAlgorithm.equals(MortalityAlgorithm.STOCHASTIC)) {
                warning("Did not find parameter 'mortality.subdt' for stochastic mortality algorithm. Osmose assumes mortality.subdt = 1");
            }
        }
    }

    @Override
    public void run() {

        // Update fishing process (for MPAs)
        fishingProcess.setMPA();

        switch (mortalityAlgorithm) {
            case ITERATIVE:
                run_itarative();
                break;
            case STOCHASTIC:
                run_stochastic();
                break;
            default:
                throw new UnsupportedOperationException("Mortality algortithm '" + mortalityAlgorithm + "' not supported in mortality process.");
        }

        // Update predation success rate, starvation mortality rate and trophic level
        for (School school : getSchoolSet()) {
            if (school.getPreyedBiomass() > 0) {
                // Update predation success rate
                double maxPreyedBiomass = school.getBiomass() * predationProcess.getMaxPredationRate(school);
                school.setPredSuccessRate(predationProcess.computePredSuccessRate(maxPreyedBiomass, school.getPreyedBiomass()));
                // Calculate starvation mortality rate that will be apply at next time step
                school.setStarvationRate(starvationProcess.getStarvationMortalityRate(school));
                // Update trophic level
                double trophicLevel = 0.d;
                for (PreyRecord preyRecord : school.getPreyRecords()) {
                    trophicLevel += preyRecord.getTrophicLevel() * preyRecord.getBiomass() / school.getPreyedBiomass();
                }
                trophicLevel += 1;
                school.setTrophicLevel((float) trophicLevel);
            }
        }

        // Apply Z mortality on schools out of the simulated domain
        for (School school : getSchoolSet().getOutSchools()) {
            double Z = outMortalityProcess.getZ(school);
            double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-Z));
            if (nDead > 0.d) {
                school.setNdead(MortalityCause.OUT, nDead);
            }
        }

    }

    public void run_stochastic() {

        // Update fishing process (for MPAs)
        fishingProcess.setMPA();

        // Assess accessibility for this time step
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getSchoolSet().getSchools(cell);
            if (cell.isLand() || schools.isEmpty()) {
                continue;
            }
            // Create the list of preys by gathering the schools and the plankton group
            List<Prey> preys = new ArrayList();
            preys.addAll(schools);
            for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
                preys.add(getSimulation().getPlankton(i).asPrey(cell, subdt));
            }
            for (School school : schools) {
                school.setAccessibilities(predationProcess.getAccessibility(school, preys));
            }
        }

        for (int idt = 0; idt < subdt; idt++) {
            fishingProcess.assessFishableBiomass();
            for (Cell cell : getGrid().getCells()) {
                if (!cell.isLand()) {
                    computeMortality_stochastic(subdt, cell);
                }
            }
            // Reset LTL preys
            for (int iLTL = 0; iLTL < getConfiguration().getNPlankton(); iLTL++) {
                getSimulation().getPlankton(iLTL).resetPreys();
            }
        }
    }

    /**
     * New function that encompasses all kind of mortality faced by the schools:
     * natural mortality, predation, fishing and starvation. we assume all
     * mortality sources are independent, compete against each other but act
     * simultaneously.
     */
//    @Override
    public void run_itarative() {

        // Update fishing process (for MPAs)
        fishingProcess.setMPA();
        fishingProcess.assessFishableBiomass();

        int nspec = getConfiguration().getNSpecies();
        boolean keepRecord = getSimulation().isPreyRecord();
        // Loop over cells
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getSchoolSet().getSchools(cell);
            if (!(cell.isLand() || schools.isEmpty())) {
                int ns = schools.size();
                int npl = getConfiguration().getNPlankton();

                double[][] nDeadMatrix = computeMortality_iterative(cell);

                // Apply mortalities and update prey records
                for (int is = 0; is < ns; is++) {
                    School school = schools.get(is);
                    // 1. Predation
                    school.resetNdead(MortalityCause.PREDATION);
                    for (int ipd = 0; ipd < ns; ipd++) {
                        school.incrementNdead(MortalityCause.PREDATION, nDeadMatrix[is][ipd]);
                    }
                    // 2. Starvation
                    school.setNdead(MortalityCause.STARVATION, nDeadMatrix[is][ns]);
                    // 3. Natural mortality
                    school.setNdead(MortalityCause.NATURAL, nDeadMatrix[is][ns + 1]);
                    // 4. Fishing
                    school.setNdead(MortalityCause.FISHING, nDeadMatrix[is][ns + 2]);

                    // Prey record
                    for (int ipr = 0; ipr < (ns + npl); ipr++) {
                        if (nDeadMatrix[ipr][is] > 0) {
                            if (ipr < ns) {
                                // Prey is School
                                School prey = schools.get(ipr);
                                school.addPreyRecord(prey, prey.adb2biom(nDeadMatrix[ipr][is]), dietOutputStage.getStage(prey), keepRecord);
                            } else {
                                // Prey is Plankton
                                int index = ipr - ns + nspec;
                                school.addPreyRecord(index, getSimulation().getPlankton(ipr - ns).getTrophicLevel(), nDeadMatrix[ipr][is], 0, keepRecord);
                            }
                        }
                    }

                }
            }
        }
    }
    /*
     * CASE1
     * > It is assumed that every cause is independant and concomitant.
     * > No stochasticity neither competition within predation process: every
     * predator sees preys as they are at the begining of the time-step.
     * > Synchromous updating of school biomass.
     */

    public double[][] computeMortality_iterative(Cell cell) {

        int ITER_MAX = 50;
        double ERR_MAX = 1.e-5d;

        List<School> schools = getSchoolSet().getSchools(cell);
        int nSchool = schools.size();
        int nPlankton = getConfiguration().getNPlankton();
        int nMortality = nSchool + 3;
        double[][] nDeadMatrix = new double[nSchool + nPlankton][nMortality];
        double[][] mortalityRateMatrix = new double[nSchool + nPlankton][nMortality];
        double[] totalMortalityRate = new double[nSchool + nPlankton];
        double[] correctionFactor = new double[nSchool];

        //
        // Initialize the number of deads and the mortality rates
        // 1. Predation
        double[][] predationMatrix = predationProcess.computePredationMatrix(cell, false, 1);
        for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
            for (int iPredator = 0; iPredator < nSchool; iPredator++) {
                double predationMortalityRate;
                if (iPrey < nSchool) {
                    School school = schools.get(iPrey);
                    nDeadMatrix[iPrey][iPredator] = school.biom2abd(predationMatrix[iPredator][iPrey]);
                    predationMortalityRate = Math.log(school.getAbundance() / (school.getAbundance() - nDeadMatrix[iPrey][iPredator]));
                } else {
                    nDeadMatrix[iPrey][iPredator] = predationMatrix[iPredator][iPrey];
                    double planktonAbundance = getSimulation().getPlankton(iPrey - nSchool).getBiomass(cell);
                    if (planktonAbundance > 0) {
                        predationMortalityRate = Math.log(planktonAbundance / (planktonAbundance - predationMatrix[iPredator][iPrey]));
                    } else {
                        predationMortalityRate = 0;
                    }
                }
                mortalityRateMatrix[iPrey][iPredator] = predationMortalityRate;
            }
        }
        for (int is = 0; is < nSchool; is++) {
            School school = schools.get(is);
            // 2. Starvation
            // computes preyed biomass by school ipr
            double preyedBiomass = 0;
            for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
                preyedBiomass += predationMatrix[is][iPrey];
            }
            double biomassToPredate = school.getBiomass() * predationProcess.getMaxPredationRate(school);
            school.setPredSuccessRate(predationProcess.computePredSuccessRate(biomassToPredate, preyedBiomass));
            mortalityRateMatrix[is][nSchool] = starvationProcess.getStarvationMortalityRate(school);

            // 3. Natural mortality
            mortalityRateMatrix[is][nSchool + 1] = naturalMortalityProcess.getInstantaneousRate(school);

            // 4. Fishing mortality
            switch (fishingProcess.getType()) {
                case RATE:
                    mortalityRateMatrix[is][nSchool + 2] = fishingProcess.getInstantaneousRate(school);
                    break;
                case CATCHES:
                    /* Even though we call instantenous catches, since ITERATIVE
                     * case does not increment any ndead, it is equivalent to 
                     * getting the catches at the beginning of the time step.
                     */
                    double catches = fishingProcess.getInstantaneousCatches(school);
                    if (school.getBiomass() - catches < epsilon) {
                        mortalityRateMatrix[is][nSchool + 2] = Math.log(school.getAbundance() / epsilon);
                    } else {
                        mortalityRateMatrix[is][nSchool + 2] = Math.log(school.getAbundance() / (school.getAbundance() - school.biom2abd(catches)));
                    }
                    break;
            }
        }

        //
        // Compute total mortality rate for schools and plankton
        // Sum mortality rates from every source for every school and
        // every plankton group
        for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
            for (int iMortality = 0; iMortality < nMortality; iMortality++) {
                totalMortalityRate[iPrey] += mortalityRateMatrix[iPrey][iMortality];
            }
        }

        //
        // Begining of iteration
        int iteration = 0;
        double error = Double.MAX_VALUE;
        while ((iteration < ITER_MAX) && error > ERR_MAX) {

            // Update number of deads
            for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
                double abundance;
                if (iPrey < nSchool) {
                    abundance = schools.get(iPrey).getAbundance();
                } else {
                    abundance = getSimulation().getPlankton(iPrey - nSchool).getBiomass(cell);
                }
                for (int iMortality = 0; iMortality < nMortality; iMortality++) {
                    if (totalMortalityRate[iPrey] > 0) {
                        nDeadMatrix[iPrey][iMortality] = (mortalityRateMatrix[iPrey][iMortality] / totalMortalityRate[iPrey]) * (1 - Math.exp(-totalMortalityRate[iPrey])) * abundance;
                    } else {
                        nDeadMatrix[iPrey][iMortality] = 0.d;
                    }
                }
            }

            // Compute correction factor
            for (int iPredator = 0; iPredator < nSchool; iPredator++) {
                School predator = schools.get(iPredator);
                double preyedBiomass = 0;
                for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
                    if (iPrey < nSchool) {
                        preyedBiomass += schools.get(iPrey).adb2biom(nDeadMatrix[iPrey][iPredator]);
                    } else {
                        preyedBiomass += nDeadMatrix[iPrey][iPredator];
                    }
                    //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                }
                double biomassToPredate = predator.getBiomass() * predationProcess.getMaxPredationRate(predator);
                predator.setPredSuccessRate(predationProcess.computePredSuccessRate(biomassToPredate, preyedBiomass));
                if (preyedBiomass > 0) {
                    correctionFactor[iPredator] = Math.min(biomassToPredate / preyedBiomass, 1.d);
                } else {
                    correctionFactor[iPredator] = 1;
                }
            }

            // Update mortality rates
            for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
                // 1. Predation
                for (int iPredator = 0; iPredator < nSchool; iPredator++) {
                    mortalityRateMatrix[iPrey][iPredator] *= correctionFactor[iPredator];
                }
            }
            for (int iPrey = 0; iPrey < nSchool; iPrey++) {
                School school = schools.get(iPrey);
                // 2. Starvation
                // computes preyed biomass by school ipr
                mortalityRateMatrix[iPrey][nSchool] = starvationProcess.getStarvationMortalityRate(school);
                // 3. Natural mortality, unchanged
                // 4. Fishing, unchanged
            }

            // Convergence test
            double[] oldTotalMortalityRate = Arrays.copyOf(totalMortalityRate, totalMortalityRate.length);
            error = 0.d;
            for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
                totalMortalityRate[iPrey] = 0.d;
                for (int iMortality = 0; iMortality < nMortality; iMortality++) {
                    totalMortalityRate[iPrey] += mortalityRateMatrix[iPrey][iMortality];
                }
                error = Math.max(error, Math.abs(totalMortalityRate[iPrey] - oldTotalMortalityRate[iPrey]));
            }
            iteration++;
        }

        //
        // return the number of deads matrix
        return nDeadMatrix;
    }

    /*
     * CASE3
     * > It is assumed that every cause compete with each other.
     * > Stochasticity and competition within predation process.
     * > Asynchronous updating of school biomass (it means biomass are updated
     * on the fly).
     */
    public void computeMortality_stochastic(int subdt, Cell cell) {

        List<School> schools = getSchoolSet().getSchools(cell);
        int ns = schools.size();
        if (ns == 0) {
            return;
        }
        // Create the list of preys by gathering the schools and the plankton group
        List<Prey> preys = new ArrayList();
        preys.addAll(schools);
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            preys.add(getSimulation().getPlankton(i).asPrey(cell, subdt));
        }

        Integer[] seqPred = new Integer[ns];
        for (int i = 0; i < ns; i++) {
            seqPred[i] = i;
        }
        Integer[] seqFish = Arrays.copyOf(seqPred, ns);
        Integer[] seqNat = Arrays.copyOf(seqPred, ns);
        Integer[] seqStarv = Arrays.copyOf(seqPred, ns);
        MortalityCause[] mortalityCauses = MortalityCause.values();

        FishingType fishingType = fishingProcess.getType();
        shuffleArray(seqPred);
        shuffleArray(seqFish);
        shuffleArray(seqNat);
        shuffleArray(seqStarv);
        int nspec = getConfiguration().getNSpecies();
        boolean keepRecord = getSimulation().isPreyRecord();
        for (int i = 0; i < ns; i++) {
            shuffleArray(mortalityCauses);
            for (MortalityCause cause : mortalityCauses) {
                School school;
                double nDead = 0;
                switch (cause) {
                    case PREDATION:
                        // Predation mortality
                        School predator = schools.get(seqPred[i]);
                        double[] preyUpon = predationProcess.computePredation(predator, preys, predator.getAccessibilities(), subdt);
                        for (int ipr = 0; ipr < preys.size(); ipr++) {
                            Prey prey = preys.get(ipr);
                            nDead = prey.biom2abd(preyUpon[ipr]);
                            prey.incrementNdead(MortalityCause.PREDATION, nDead);

                            if (prey instanceof School) {
                                predator.addPreyRecord((School) prey, preyUpon[ipr], dietOutputStage.getStage((School) prey), keepRecord);
                            } else {
                                int index = ipr - ns + nspec;
                                predator.addPreyRecord(index, prey.getTrophicLevel(), preyUpon[ipr], 0, keepRecord);
                            }

                        }
                        break;
                    case STARVATION:
                        // Starvation mortality
                        school = schools.get(seqStarv[i]);
                        double M = school.getStarvationRate() / subdt;
                        nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-M));
                        school.incrementNdead(MortalityCause.STARVATION, nDead);
                        break;
                    case NATURAL:
                        // Natural mortality
                        school = schools.get(seqNat[i]);
                        double D = naturalMortalityProcess.getInstantaneousRate(school) / subdt;
                        nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
                        school.incrementNdead(MortalityCause.NATURAL, nDead);
                        break;
                    case FISHING:
                        // Fishing Mortality
                        school = schools.get(seqFish[i]);
                        switch (fishingType) {
                            case RATE:
                                double F = fishingProcess.getInstantaneousRate(school) / subdt;
                                nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-F));
                                break;
                            case CATCHES:
                                nDead = school.biom2abd(fishingProcess.getInstantaneousCatches(school) / subdt);
                                break;
                        }
                        school.incrementNdead(MortalityCause.FISHING, nDead);
                        break;
                }
            }
        }
    }

    private static <T> void shuffleArray(T[] a) {
        // Shuffle array
        for (int i = a.length; i > 1; i--) {
            T tmp = a[i - 1];
            int j = random.nextInt(i);
            a[i - 1] = a[j];
            a[j] = tmp;
            //swap(a, i - 1, random.nextInt(i));
        }
    }

    private static <T> void swap(T[] a, int i, int j) {
        T tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }
}
