/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Simulation;
import fr.ird.osmose.Species;
import java.util.Arrays;
import java.util.Collections;
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
    private final int subdt = 1;
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
    /**
     * Whether school diet should be recorded
     */
    private boolean recordDiet;

    public MortalityProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {
        random = new Random();

        naturalMortalityProcess = new NaturalMortalityProcess(getReplica());
        naturalMortalityProcess.init();

        fishingProcess = new FishingProcess(getReplica());
        fishingProcess.init();

        starvationProcess = new StarvationProcess(getReplica());
        starvationProcess.init();

        predationProcess = new PredationProcess(getReplica());
        predationProcess.init();
        
        recordDiet = getConfiguration().getBoolean("output.diet.composition.enabled")
                || getConfiguration().getBoolean("output.diet.pressure.enabled");
    }

    /**
     * New function that encompasses all kind of mortality faced by the schools:
     * natural mortality, predation, fishing and starvation. we assume all
     * mortality sources are independent, compete against each other but act
     * simultaneously.
     */
    @Override
    public void run() {

        // Loop over cells
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getPopulation().getSchools(cell);
            if (!(cell.isLand() || schools.isEmpty())) {
                int ns = schools.size();
                int npl = getConfiguration().getNPlankton();

                // Update stages
                for (School school : schools) {
                    predationProcess.updateAccessibilityStage(school);
                    predationProcess.updatePredPreyStage(school);
                    predationProcess.updateDietOutputStage(school);
                }

                double[][] nDeadMatrix = null;
                switch (Simulation.VERSION) {
                    case CASE1:
                        nDeadMatrix = computeMortality_case1(subdt, cell);
                        break;
                    case CASE2:
                        nDeadMatrix = computeMortality_case2(subdt, cell);
                        break;
                    case CASE3:
                        nDeadMatrix = computeMortality_case3(subdt, cell);
                        break;
                    default:
                        throw new UnsupportedOperationException("Version " + Simulation.VERSION + " not supported in computeMortality() function.");
                }

                // Apply mortalities
                float[] tmpTL = new float[ns];
                for (int is = 0; is < ns; is++) {
                    School school = schools.get(is);
                    // 1. Predation
                    school.resetNdeadPredation();
                    double preyedBiomass = 0.d;
                    for (int ipd = 0; ipd < ns; ipd++) {
                        school.incrementNdeadPredation(nDeadMatrix[is][ipd]);
                    }
                    for (int ipr = 0; ipr < ns + npl; ipr++) {
                        if (ipr < ns) {
                            preyedBiomass += schools.get(ipr).adb2biom(nDeadMatrix[ipr][is]);
                        } else {
                            preyedBiomass += nDeadMatrix[ipr][is];
                        }
                    }
                    // update TL
                    tmpTL[is] = 0;
                    if (preyedBiomass > 0.d) {
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School prey = schools.get(ipr);
                                double biomPrey = prey.adb2biom(nDeadMatrix[ipr][is]);
                                if (recordDiet) {
                                    school.diet[prey.getSpeciesIndex()][prey.getDietOutputStage()] += biomPrey;
                                }
                                float TLprey = (prey.getAgeDt() == 0) || (prey.getAgeDt() == 1)
                                        ? Species.TL_EGG
                                        : prey.getTrophicLevel();
                                tmpTL[is] += TLprey * biomPrey / preyedBiomass;
                            } else {
                                tmpTL[is] += getSimulation().getPlankton(ipr - ns).getTrophicLevel() * nDeadMatrix[ipr][is] / preyedBiomass;
                                if (recordDiet) {
                                    int iltl = getNSpecies() + ipr - ns;
                                    school.diet[iltl][0] += nDeadMatrix[ipr][is];
                                }
                            }
                            //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                        }
                        tmpTL[is] += 1;
                    } else {
                        tmpTL[is] = school.getTrophicLevel();
                    }

                    // 2. Starvation
                    school.setNdeadStarvation(nDeadMatrix[is][ns]);
                    // 3. Natural mortality
                    school.setNdeadNatural(nDeadMatrix[is][ns + 1]);
                    // 4. Fishing
                    school.setNdeadFishing(nDeadMatrix[is][ns + 2]);
                }
                // Update TL
                for (int is = 0; is < ns; is++) {
                    schools.get(is).setTrophicLevel(tmpTL[is]);
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
    public double[][] computeMortality_case1(int subdt, Cell cell) {

        int ITER_MAX = 50;
        double ERR_MAX = 1.e-5d;

        List<School> schools = getPopulation().getSchools(cell);
        int nSchool = schools.size();
        int nPlankton = getConfiguration().getNPlankton();
        int nMortality = nSchool + 3;
        double[][] nDeadMatrix = new double[nSchool + nPlankton][nMortality];
        double[][] mortalityRateMatrix = new double[nSchool + nPlankton][nMortality];
        double[] totalMortalityRate = new double[nSchool + nPlankton];
        double[] correctionFactor = new double[nSchool];

        //
        // Initialize the number of deads and the mortality rates
        double[][] predationMatrix = predationProcess.computePredationMatrix(cell, School.INISTEP_BIOMASS, subdt);
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
            double biomassToPredate = school.getBiomass() * predationProcess.getPredationRate(school, subdt);
            school.predSuccessRate = predationProcess.computePredSuccessRate(biomassToPredate, preyedBiomass);
            mortalityRateMatrix[is][nSchool] = starvationProcess.getStarvationMortalityRate(school, subdt);

            // 3. Natural mortality
            mortalityRateMatrix[is][nSchool + 1] = naturalMortalityProcess.getNaturalMortalityRate(school, subdt);

            // 4. Fishing mortality
            mortalityRateMatrix[is][nSchool + 2] = fishingProcess.getFishingMortalityRate(school, subdt);
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
                double biomassToPredate = predator.getBiomass() * predationProcess.getPredationRate(predator, subdt);
                predator.predSuccessRate = predationProcess.computePredSuccessRate(biomassToPredate, preyedBiomass);
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
                mortalityRateMatrix[iPrey][nSchool] = starvationProcess.getStarvationMortalityRate(school, subdt);
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
     * CASE2
     * > It is assumed that every cause is independant and concomitant.
     * > Stochasticity and competition within predation process: prey and
     * predator biomass are being updated on the fly virtually (indeed the
     * update is not effective outside the predation process,
     * it is just temporal).
     * > Synchronous updating of school biomass.
     */
    public double[][] computeMortality_case2(int subdt, Cell cell) {

        List<School> schools = getPopulation().getSchools(cell);
        int ns = schools.size();
        int npl = getConfiguration().getNPlankton();
        double[][] mortalityRateMatrix = new double[ns + npl][ns + 3];
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];
        double[] totalMortalityRate = new double[ns + npl];


        //
        // Assess all mortality independently from each other
        Collections.shuffle(schools);
        for (int ipd = 0; ipd < ns; ipd++) {
            // Predation mortality 
            School predator = schools.get(ipd);
            double[] preyUpon = predationProcess.computePredation(predator, School.INSTANTANEOUS_BIOMASS, subdt);
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                double predationMortalityRate;
                if (ipr < ns) {
                    School school = schools.get(ipr);
                    nDeadMatrix[ipr][ipd] = school.biom2abd(preyUpon[ipr]);
                    predationMortalityRate = Math.log(school.getInstantaneousAbundance() / (school.getInstantaneousAbundance() - nDeadMatrix[ipr][ipd]));
                    school.incrementNdeadPredation(nDeadMatrix[ipr][ipd]);
                } else {
                    nDeadMatrix[ipr][ipd] = preyUpon[ipr];
                    double planktonAbundance = getSimulation().getPlankton(ipr - ns).getAccessibleBiomass(cell);
                    predationMortalityRate = Math.log(planktonAbundance / (planktonAbundance - preyUpon[ipr]));
                }
                mortalityRateMatrix[ipr][ipd] = predationMortalityRate;

            }
            double biomassToPredate = predator.getInstantaneousBiomass() * predationProcess.getPredationRate(predator, subdt);
            predator.predSuccessRate = predationProcess.computePredSuccessRate(biomassToPredate, sum(preyUpon));
        }

        for (int is = 0; is < ns; is++) {
            School school = schools.get(is);
            // reset ndeadpredation so to ensure processes are independant
            school.resetNdeadPredation();
            // 2. Starvation
            double M = starvationProcess.getStarvationMortalityRate(school, subdt);
            nDeadMatrix[is][ns] = school.getInstantaneousAbundance() * (1 - Math.exp(-M));
            mortalityRateMatrix[is][ns] = M;

            // 3. Natural mortality
            double D = naturalMortalityProcess.getNaturalMortalityRate(school, subdt);
            nDeadMatrix[is][ns + 1] = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
            mortalityRateMatrix[is][ns + 1] = D;

            // 4. Fishing mortality
            double F = fishingProcess.getFishingMortalityRate(school, subdt);
            nDeadMatrix[is][ns + 2] = school.getInstantaneousAbundance() * (1 - Math.exp(-F));
            mortalityRateMatrix[is][ns + 2] = F;
        }

        //
        // Compute total mortality rate for schools and plankton
        // Sum mortality rates from every source for every school and
        // every plankton group
        for (int ipr = 0; ipr < (ns + npl); ipr++) {
            for (int imort = 0; imort < (ns + 3); imort++) {
                totalMortalityRate[ipr] += mortalityRateMatrix[ipr][imort];
            }
        }

        //
        // Update number of deads
        for (int ipr = 0; ipr < (ns + npl); ipr++) {
            double abundance;
            if (ipr < ns) {
                abundance = schools.get(ipr).getAbundance();
            } else {
                abundance = getSimulation().getPlankton(ipr - ns).getAccessibleBiomass(cell);
            }
            for (int imort = 0; imort < ns + 3; imort++) {
                if (totalMortalityRate[ipr] > 0) {
                    nDeadMatrix[ipr][imort] = (mortalityRateMatrix[ipr][imort] / totalMortalityRate[ipr]) * (1 - Math.exp(-totalMortalityRate[ipr])) * abundance;
                } else {
                    nDeadMatrix[ipr][imort] = 0.d;
                }
            }
        }

        return nDeadMatrix;
    }

    /*
     * CASE3
     * > It is assumed that every cause compete with each other.
     * > Stochasticity and competition within predation process.
     * > Asynchronous updating of school biomass (it means biomass are updated
     * on the fly).
     */
    public double[][] computeMortality_case3(int subdt, Cell cell) {

        List<School> schools = getPopulation().getSchools(cell);
        int ns = schools.size();
        int npl = getConfiguration().getNPlankton();
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];

        int[] seqPred = new int[ns];
        boolean[] hasPredated = new boolean[ns];
        for (int i = 0; i < ns; i++) {
            seqPred[i] = i;
        }
        int[] seqFish = Arrays.copyOf(seqPred, ns);
        int[] seqNat = Arrays.copyOf(seqPred, ns);
        int[] seqStarv = Arrays.copyOf(seqPred, ns);
        shuffleArray(seqPred);
        shuffleArray(seqFish);
        shuffleArray(seqNat);
        shuffleArray(seqStarv);
        int[] mortalitySource = new int[]{0, 1, 2, 3};
        // 0 = predation
        // 1 = starvation
        // 2 = natural
        // 3 = fishing

        for (int i = 0; i < ns; i++) {
            shuffleArray(mortalitySource);
            for (int j = 0; j < mortalitySource.length; j++) {
                School school;
                switch (mortalitySource[j]) {
                    case 0:
                        // Predation mortality
                        school = schools.get(seqPred[i]);
                        double[] preyUpon = predationProcess.computePredation(school, School.INSTANTANEOUS_BIOMASS, subdt);
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School prey = schools.get(ipr);
                                nDeadMatrix[ipr][seqPred[i]] = prey.biom2abd(preyUpon[ipr]);
                                prey.incrementNdeadPredation(nDeadMatrix[ipr][seqPred[i]]);
                            } else {
                                nDeadMatrix[ipr][seqPred[i]] = preyUpon[ipr];
                            }

                        }
                        hasPredated[seqPred[i]] = true;
                        double biomassToPredate = school.getInstantaneousBiomass() * predationProcess.getPredationRate(school, subdt);
                        school.predSuccessRate = predationProcess.computePredSuccessRate(biomassToPredate, sum(preyUpon));
                        break;
                    case 1:
                        // Starvation mortality
                        if (hasPredated[seqStarv[i]]) {
                            school = schools.get(seqStarv[i]);
                            double M = starvationProcess.getStarvationMortalityRate(school, subdt);
                            nDeadMatrix[seqStarv[i]][ns] = school.getInstantaneousAbundance() * (1 - Math.exp(-M));
                            school.setNdeadStarvation(nDeadMatrix[seqStarv[i]][ns]);
                        }
                        break;
                    case 2:
                        // Natural mortality
                        school = schools.get(seqNat[i]);
                        double D = naturalMortalityProcess.getNaturalMortalityRate(school, subdt);
                        nDeadMatrix[seqNat[i]][ns + 1] = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
                        school.setNdeadNatural(nDeadMatrix[seqNat[i]][ns + 1]);
                        break;
                    case 3:
                        // Fishing Mortality
                        school = schools.get(seqFish[i]);
                        double F = fishingProcess.getFishingMortalityRate(school, subdt);
                        nDeadMatrix[seqFish[i]][ns + 2] = school.getInstantaneousAbundance() * (1 - Math.exp(-F));
                        school.setNdeadFishing(nDeadMatrix[seqFish[i]][ns + 2]);
                        break;
                }
            }
        }

        return nDeadMatrix;
    }

    private static void shuffleArray(int[] a) {
        // Shuffle array
        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, random.nextInt(i));
        }
    }

    private static void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    private double sum(double[] array) {
        double sum = 0.d;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }
}
