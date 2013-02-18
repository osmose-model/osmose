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

    @Override
    public void init() {
        random = new Random();
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
                int npl = getForcing().getNbPlanktonGroups();

                // Reset nDeads
                for (School school : schools) {
                    school.nDeadPredation = 0;
                    school.nDeadStarvation = 0;
                    school.nDeadNatural = 0;
                    school.nDeadFishing = 0;
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
                for (int is = 0; is < ns; is++) {
                    School school = schools.get(is);
                    // 1. Predation
                    school.nDeadPredation = 0.d;
                    double preyedBiomass = 0.d;
                    for (int ipd = 0; ipd < ns; ipd++) {
                        school.nDeadPredation += nDeadMatrix[is][ipd];
                    }
                    for (int ipr = 0; ipr < ns + npl; ipr++) {
                        if (ipr < ns) {
                            preyedBiomass += schools.get(ipr).adb2biom(nDeadMatrix[ipr][is]);
                        } else {
                            preyedBiomass += nDeadMatrix[ipr][is];
                        }
                    }
                    school.preyedBiomass += preyedBiomass;
                    // update TL
                    school.tmpTL = 0;
                    if (preyedBiomass > 0.d) {
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School prey = schools.get(ipr);
                                double biomPrey = prey.adb2biom(nDeadMatrix[ipr][is]);
                                if (getOsmose().isDietOuput()) {
                                    school.diet[prey.getSpeciesIndex()][prey.dietOutputStage] += biomPrey;
                                }
                                float TLprey = (prey.getAgeDt() == 0) || (prey.getAgeDt() == 1)
                                        ? Species.TL_EGG
                                        : prey.trophicLevel;
                                school.tmpTL += TLprey * biomPrey / preyedBiomass;
                            } else {
                                school.tmpTL += getForcing().getPlankton(ipr - ns).trophicLevel * nDeadMatrix[ipr][is] / preyedBiomass;
                                if (getOsmose().isDietOuput()) {
                                    school.diet[getOsmose().getNumberSpecies() + (ipr - ns)][0] += nDeadMatrix[ipr][is];
                                }
                            }
                            //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                        }
                        school.tmpTL += 1;
                    } else {
                        school.tmpTL = school.trophicLevel;
                    }

                    // 2. Starvation
                    school.nDeadStarvation = nDeadMatrix[is][ns];
                    // 3. Natural mortality
                    school.nDeadNatural = nDeadMatrix[is][ns + 1];
                    // 4. Fishing
                    school.nDeadFishing = nDeadMatrix[is][ns + 2];

                    // Update abundance
                    double nDeadTotal = school.nDeadPredation
                            + school.nDeadStarvation
                            + school.nDeadNatural
                            + school.nDeadFishing;

                    school.setAbundance(school.getAbundance() - nDeadTotal);
                    if (school.getAbundance() < 1.d) {
                        school.setAbundance(0.d);
                    }
                }
                for (School school : schools) {
                    school.trophicLevel = school.tmpTL;
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
        int ns = schools.size();
        int npl = getForcing().getNbPlanktonGroups();
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];
        double[][] mortalityRateMatrix = new double[ns + npl][ns + 3];
        double[] totalMortalityRate = new double[ns + npl];
        double[] correctionFactor = new double[ns];

        //
        // Initialize the number of deads and the mortality rates
        double[][] predationMatrix = PredationProcess.computePredationMatrix(cell, subdt);
        for (int ipr = 0; ipr < (ns + npl); ipr++) {
            for (int ipd = 0; ipd < ns; ipd++) {
                double predationMortalityRate;
                if (ipr < ns) {
                    School school = schools.get(ipr);
                    nDeadMatrix[ipr][ipd] = school.biom2abd(predationMatrix[ipd][ipr]);
                    predationMortalityRate = Math.log(school.getAbundance() / (school.getAbundance() - nDeadMatrix[ipr][ipd]));
                } else {
                    nDeadMatrix[ipr][ipd] = predationMatrix[ipd][ipr];
                    double planktonAbundance = getForcing().getPlankton(ipr - ns).biomass[cell.get_igrid()][cell.get_jgrid()];
                    if (planktonAbundance > 0) {
                        predationMortalityRate = Math.log(planktonAbundance / (planktonAbundance - predationMatrix[ipd][ipr]));
                    } else {
                        predationMortalityRate = 0;
                    }
                }
                mortalityRateMatrix[ipr][ipd] = predationMortalityRate;
            }
        }
        for (int is = 0; is < ns; is++) {
            School school = schools.get(is);
            // 2. Starvation
            // computes preyed biomass by school ipr
            double preyedBiomass = 0;
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                preyedBiomass += predationMatrix[is][ipr];
            }
            school.predSuccessRate = PredationProcess.computePredSuccessRate(PredationProcess.computeBiomassToPredate(school, subdt), preyedBiomass);
            mortalityRateMatrix[is][ns] = StarvationProcess.getStarvationMortalityRate(school, subdt);

            // 3. Natural mortality
            mortalityRateMatrix[is][ns + 1] = NaturalMortalityProcess.getNaturalMortalityRate(school, subdt);

            // 4. Fishing mortality
            mortalityRateMatrix[is][ns + 2] = FishingProcess.getFishingMortalityRate(school, subdt);
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
        // Begining of iteration
        int iteration = 0;
        double error = Double.MAX_VALUE;
        while ((iteration < ITER_MAX) && error > ERR_MAX) {

            // Update number of deads
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                double abundance;
                if (ipr < ns) {
                    abundance = schools.get(ipr).getAbundance();
                } else {
                    abundance = getForcing().getPlankton(ipr - ns).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
                }
                for (int imort = 0; imort < ns + 3; imort++) {
                    if (totalMortalityRate[ipr] > 0) {
                        nDeadMatrix[ipr][imort] = (mortalityRateMatrix[ipr][imort] / totalMortalityRate[ipr]) * (1 - Math.exp(-totalMortalityRate[ipr])) * abundance;
                    } else {
                        nDeadMatrix[ipr][imort] = 0.d;
                    }
                }
            }

            // Compute correction factor
            for (int ipd = 0; ipd < ns; ipd++) {
                School predator = schools.get(ipd);
                double preyedBiomass = 0;
                for (int ipr = 0; ipr < (ns + npl); ipr++) {
                    if (ipr < ns) {
                        preyedBiomass += schools.get(ipr).adb2biom(nDeadMatrix[ipr][ipd]);
                    } else {
                        preyedBiomass += nDeadMatrix[ipr][ipd];
                    }
                    //System.out.println("pred" + ipd + " py:" + ipr + " " + nbDeadMatrix[ipr][ipd] + " " + mortalityRateMatrix[ipr][ipd] + " " + totalMortalityRate[ipr]);
                }
                double biomassToPredate = PredationProcess.computeBiomassToPredate(predator, subdt);
                predator.predSuccessRate = PredationProcess.computePredSuccessRate(biomassToPredate, preyedBiomass);
                if (preyedBiomass > 0) {
                    correctionFactor[ipd] = Math.min(biomassToPredate / preyedBiomass, 1.d);
                } else {
                    correctionFactor[ipd] = 1;
                }
            }

            // Update mortality rates
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                // 1. Predation
                for (int ipd = 0; ipd < ns; ipd++) {
                    mortalityRateMatrix[ipr][ipd] *= correctionFactor[ipd];
                }
            }
            for (int ipr = 0; ipr < ns; ipr++) {
                School school = schools.get(ipr);
                // 2. Starvation
                // computes preyed biomass by school ipr
                mortalityRateMatrix[ipr][ns] = StarvationProcess.getStarvationMortalityRate(school, subdt);
                // 3. Natural mortality, unchanged
                // 4. Fishing, unchanged
            }

            // Convergence test
            double[] oldTotalMortalityRate = Arrays.copyOf(totalMortalityRate, totalMortalityRate.length);
            error = 0.d;
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                totalMortalityRate[ipr] = 0.d;
                for (int imort = 0; imort < (ns + 3); imort++) {
                    totalMortalityRate[ipr] += mortalityRateMatrix[ipr][imort];
                }
                error = Math.max(error, Math.abs(totalMortalityRate[ipr] - oldTotalMortalityRate[ipr]));
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
        int npl = getForcing().getNbPlanktonGroups();
        double[][] mortalityRateMatrix = new double[ns + npl][ns + 3];
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];
        double[] totalMortalityRate = new double[ns + npl];


        //
        // Assess all mortality independently from each other
        Collections.shuffle(schools);
        for (int ipd = 0; ipd < ns; ipd++) {
            // Predation mortality 
            School predator = schools.get(ipd);
            double[] preyUpon = PredationProcess.computePredation(predator, subdt);
            for (int ipr = 0; ipr < (ns + npl); ipr++) {
                double predationMortalityRate;
                if (ipr < ns) {
                    School school = schools.get(ipr);
                    nDeadMatrix[ipr][ipd] = school.biom2abd(preyUpon[ipr]);
                    predationMortalityRate = Math.log(school.getInstantaneousAbundance() / (school.getInstantaneousAbundance() - nDeadMatrix[ipr][ipd]));
                    school.nDeadPredation += nDeadMatrix[ipr][ipd];
                } else {
                    nDeadMatrix[ipr][ipd] = preyUpon[ipr];
                    double planktonAbundance = getForcing().getPlankton(ipr - ns).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
                    predationMortalityRate = Math.log(planktonAbundance / (planktonAbundance - preyUpon[ipr]));
                }
                mortalityRateMatrix[ipr][ipd] = predationMortalityRate;

            }
            predator.predSuccessRate = PredationProcess.computePredSuccessRate(PredationProcess.computeBiomassToPredate(predator, subdt), sum(preyUpon));
        }

        for (int is = 0; is < ns; is++) {
            School school = schools.get(is);
            school.nDeadPredation = 0.d;
            // 2. Starvation
            nDeadMatrix[is][ns] = StarvationProcess.computeStarvationMortality(school, subdt);
            mortalityRateMatrix[is][ns] = StarvationProcess.getStarvationMortalityRate(school, subdt);

            // 3. Natural mortality
            nDeadMatrix[is][ns + 1] = NaturalMortalityProcess.computeNaturalMortality(school, subdt);
            mortalityRateMatrix[is][ns + 1] = NaturalMortalityProcess.getNaturalMortalityRate(school, subdt);

            // 4. Fishing mortality
            nDeadMatrix[is][ns + 2] = FishingProcess.computeFishingMortality(school, subdt);
            mortalityRateMatrix[is][ns + 2] = FishingProcess.getFishingMortalityRate(school, subdt);
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
                abundance = getForcing().getPlankton(ipr - ns).accessibleBiomass[cell.get_igrid()][cell.get_jgrid()];
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
        int npl = getForcing().getNbPlanktonGroups();
        double[][] nDeadMatrix = new double[ns + npl][ns + 3];

        int[] seqPred = new int[ns];
        for (int i = 0; i < ns; i++) {
            schools.get(i).hasPredated = false;
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
                School predator;
                switch (mortalitySource[j]) {
                    case 0:
                        // Predation mortality
                        predator = schools.get(seqPred[i]);
                        double[] preyUpon = PredationProcess.computePredation(predator, subdt);
                        for (int ipr = 0; ipr < (ns + npl); ipr++) {
                            if (ipr < ns) {
                                School school = schools.get(ipr);
                                nDeadMatrix[ipr][seqPred[i]] = school.biom2abd(preyUpon[ipr]);
                                school.nDeadPredation += nDeadMatrix[ipr][seqPred[i]];
                            } else {
                                nDeadMatrix[ipr][seqPred[i]] = preyUpon[ipr];
                            }

                        }
                        predator.hasPredated = true;
                        predator.predSuccessRate = PredationProcess.computePredSuccessRate(PredationProcess.computeBiomassToPredate(predator, subdt), sum(preyUpon));
                        break;
                    case 1:
                        // Starvation mortality
                        predator = schools.get(seqStarv[i]);
                        if (predator.hasPredated) {
                            nDeadMatrix[seqStarv[i]][ns] = StarvationProcess.computeStarvationMortality(predator, subdt);
                            predator.nDeadStarvation = nDeadMatrix[seqStarv[i]][ns];
                        }
                        break;
                    case 2:
                        // Natural mortality
                        nDeadMatrix[seqNat[i]][ns + 1] = NaturalMortalityProcess.computeNaturalMortality(schools.get(seqNat[i]), subdt);
                        schools.get(seqNat[i]).nDeadNatural = nDeadMatrix[seqNat[i]][ns + 1];
                        break;
                    case 3:
                        // Fishing Mortality
                        nDeadMatrix[seqFish[i]][ns + 2] = FishingProcess.computeFishingMortality(schools.get(seqFish[i]), subdt);
                        schools.get(seqFish[i]).nDeadFishing = nDeadMatrix[seqFish[i]][ns + 2];
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
