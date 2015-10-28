/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import java.util.Arrays;
import java.util.List;

/**
 * Mortality rates are obtained through an iterative process.
 * <ul>
 * <li>It is assumed that every cause is independent and concomitant.</li>
 * <li>No stochasticity neither competition within predation process: every
 * predator sees preys as they are at the beginning of the time-step.</li>
 * <li>Synchronous updating of school biomass.</li>
 * </ul>
 */
public class IterativeMortalityProcess extends AbstractProcess {

    /*
     * Private instance of the additional mortality 
     */
    private AdditionalMortality additionalMortality;
    /*
     * Private instance of the fishing mortality
     */
    private FishingMortality fishingMortality;
    /*
     * Private instance of the starvation mortality
     */
    private StarvationMortality starvationMortality;
    /*
     * Private instance of the predation mortality
     */
    private PredationMortality predationMortality;
    /**
     * Epsilon constant for numerical purpose to avoid zeros at denominator when
     * calculating mortality rates.
     */
    private final double epsilon = 0.01d;
    /**
     * Variables to monitor the iterative algorithm and record how many
     * iterations (min, max, average) are necessary to converge.
     */
    private int iterMin, iterMax, iterMean, nIterProcess;

    public IterativeMortalityProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        additionalMortality = new AdditionalMortality(getRank());
        additionalMortality.init();

        fishingMortality = new FishingMortality(getRank());
        fishingMortality.init();

        starvationMortality = new StarvationMortality(getRank());
        starvationMortality.init();

        predationMortality = new PredationMortality(getRank());
        predationMortality.init();

        iterMin = Integer.MAX_VALUE;
        iterMax = 0;
        iterMean = 0;
        nIterProcess = 0;
    }

    @Override
    public void run() {
        
        // Update fishing process (for MPAs)
        fishingMortality.setMPA();
        fishingMortality.assessFishableBiomass();

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
                    // 3. Additional mortality
                    school.setNdead(MortalityCause.ADDITIONAL, nDeadMatrix[is][ns + 1]);
                    // 4. Fishing
                    school.setNdead(MortalityCause.FISHING, nDeadMatrix[is][ns + 2]);

                    // Prey record
                    for (int ipr = 0; ipr < (ns + npl); ipr++) {
                        if (nDeadMatrix[ipr][is] > 0) {
                            if (ipr < ns) {
                                // Prey is School
                                School prey = schools.get(ipr);
                                school.addPreyRecord(prey.getSpeciesIndex(), prey.getTrophicLevel(), prey.getAge(), prey.getLength(), prey.adb2biom(nDeadMatrix[ipr][is]), keepRecord);
                            } else {
                                // Prey is Plankton
                                int index = ipr - ns + nspec;
                                school.addPreyRecord(index, getSimulation().getPlankton(ipr - ns).getTrophicLevel(), -1, -1, nDeadMatrix[ipr][is], keepRecord);
                            }
                        }
                    }

                    // Predation success rate
                    double maxPreyedBiomass = school.getBiomass() * predationMortality.getMaxPredationRate(school);
                    school.setPredSuccessRate(predationMortality.computePredSuccessRate(maxPreyedBiomass, school.getPreyedBiomass()));
                }
            }
        }
        performanceIterative();
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
        double[] planktonBiomass = new double[nPlankton];
        for (int iPlk = 0; iPlk < nPlankton; iPlk++) {
            planktonBiomass[iPlk] = getSimulation().getForcing().getBiomass(iPlk, cell);
        }

        //
        // Initialize the number of deads and the mortality rates
        // 1. Predation
        double[][] predationMatrix = predationMortality.computePredationMatrix(cell, false, 1);
        for (int iPrey = 0; iPrey < (nSchool + nPlankton); iPrey++) {
            for (int iPredator = 0; iPredator < nSchool; iPredator++) {
                double predationMortalityRate;
                if (iPrey < nSchool) {
                    School school = schools.get(iPrey);
                    nDeadMatrix[iPrey][iPredator] = school.biom2abd(predationMatrix[iPredator][iPrey]);
                    predationMortalityRate = Math.log(school.getAbundance() / (school.getAbundance() - nDeadMatrix[iPrey][iPredator]));
                } else {
                    nDeadMatrix[iPrey][iPredator] = predationMatrix[iPredator][iPrey];
                    double planktonAbundance = planktonBiomass[iPrey - nSchool];
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
            double biomassToPredate = school.getBiomass() * predationMortality.getMaxPredationRate(school);
            school.setPredSuccessRate(predationMortality.computePredSuccessRate(biomassToPredate, preyedBiomass));
            mortalityRateMatrix[is][nSchool] = starvationMortality.getRate(school);

            // 3. Additional mortality
            mortalityRateMatrix[is][nSchool + 1] = additionalMortality.getRate(school);

            // 4. Fishing mortality
            switch (fishingMortality.getType()) {
                case RATE:
                    mortalityRateMatrix[is][nSchool + 2] = fishingMortality.getRate(school);
                    break;
                case CATCHES:
                    /* Even though we call instantenous catches, since ITERATIVE
                     * case does not increment any ndead, it is equivalent to 
                     * getting the catches at the beginning of the time step.
                     */
                    double catches = fishingMortality.getCatches(school);
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
                    abundance = planktonBiomass[iPrey - nSchool];
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
                double biomassToPredate = predator.getBiomass() * predationMortality.getMaxPredationRate(predator);
                predator.setPredSuccessRate(predationMortality.computePredSuccessRate(biomassToPredate, preyedBiomass));
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
                mortalityRateMatrix[iPrey][nSchool] = starvationMortality.getRate(school);
                // 3. Additional mortality, unchanged
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
        if (iteration < iterMin) {
            iterMin = iteration;
        }
        if (iteration > iterMax) {
            iterMax = iteration;
        }
        iterMean += iteration;
        nIterProcess += 1;

        //
        // return the number of deads matrix
        return nDeadMatrix;
    }

    /**
     * Send debugging info about the performance of the iterative algorithm.
     */
    private void performanceIterative() {
        float mean = iterMean / (float) nIterProcess;
        debug("Iterative mortality algorithm. Iteration min " + iterMin + " iteration max " + iterMax + " iteration mean " + mean);
    }

}
