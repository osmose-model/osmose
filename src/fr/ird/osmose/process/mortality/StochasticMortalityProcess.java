/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.Cell;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import fr.ird.osmose.util.XSRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Mortality processes compete stochastically.
 * <ul>
 * <li>It is assumed that every cause compete with each other.</li>
 * <li>Stochasticity and competition within predation process.</li>
 * <li>Asynchronous updating of school biomass (it means biomass are updated on
 * the fly).</li>
 * </ul>
 */
public class StochasticMortalityProcess extends AbstractProcess {

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
     * Private instance of the additional mortality
     */
    private AdditionalMortality additionalMortality;
    /*
     * Private instance of the fishing mortality
     */
    private FishingMortality fishingMortality;
    /*
     * Private instance of the predation mortality
     */
    private PredationMortality predationMortality;

    public StochasticMortalityProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        random = new XSRandom(System.nanoTime());

        additionalMortality = new AdditionalMortality(getRank());
        additionalMortality.init();

        fishingMortality = new FishingMortality(getRank());
        fishingMortality.init();

        predationMortality = new PredationMortality(getRank());
        predationMortality.init();

        // Subdt 
        if (!getConfiguration().isNull("mortality.subdt")) {
            subdt = getConfiguration().getInt("mortality.subdt");
        } else {
            subdt = 10;
            warning("Did not find parameter 'mortality.subdt' for stochastic mortality algorithm.");
        }

        info("Mortality subdt set to " + subdt);

    }

    @Override
    public void run() {

        // Update fishing process (for MPAs)
        fishingMortality.setMPA();

        // Assess accessibility for this time step
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getSchoolSet().getSchools(cell);
            if (cell.isLand() || schools.isEmpty()) {
                continue;
            }
            // Create the list of preys by gathering the schools and the plankton group
            List<IAggregation> preys = new ArrayList();
            preys.addAll(schools);
            for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
                preys.add(getSimulation().getPlankton(i).getSwarm(cell));
            }
            for (School school : schools) {
                school.setAccessibilities(predationMortality.getAccessibility(school, preys));
                school.setPredSuccessRate(0);
                if (school.getAgeDt() == 0) {
                    // Egg loss, not accessible to predation process
                    double D = additionalMortality.getRate(school);
                    double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
                    school.incrementNdead(MortalityCause.ADDITIONAL, nDead);
                    school.retainEgg();
                }
            }
        }

        // Update biomass of the swarms
        int iStepSimu = getSimulation().getIndexTimeSimu();
        for (int iLTL = 0; iLTL < getConfiguration().getNPlankton(); iLTL++) {
            getSimulation().getPlankton(iLTL).updateSwarms(iStepSimu);
        }

        for (int idt = 0; idt < subdt; idt++) {
            fishingMortality.assessFishableBiomass();
            for (Cell cell : getGrid().getCells()) {
                if (!cell.isLand()) {
                    computeMortality_stochastic(subdt, cell);
                }
            }
        }
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
        List<IAggregation> preys = new ArrayList();
        preys.addAll(schools);
        for (School prey : schools) {
            // Release some eggs for current subdt (initial abundance / subdt)
            if (prey.getAgeDt() == 0) {
                prey.releaseEgg(subdt);
            }
        }
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            preys.add(getSimulation().getPlankton(i).getSwarm(cell));
        }

        Integer[] seqPred = new Integer[ns];
        for (int i = 0; i < ns; i++) {
            seqPred[i] = i;
        }
        Integer[] seqFish = Arrays.copyOf(seqPred, ns);
        Integer[] seqNat = Arrays.copyOf(seqPred, ns);
        Integer[] seqStarv = Arrays.copyOf(seqPred, ns);
        MortalityCause[] mortalityCauses = MortalityCause.values();

        FishingMortality.Type fishingType = fishingMortality.getType();
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
                        double[] preyUpon = predationMortality.computePredation(predator, preys, predator.getAccessibilities(), subdt);
                        for (int ipr = 0; ipr < preys.size(); ipr++) {
                            if (preyUpon[ipr] > 0) {
                                IAggregation prey = preys.get(ipr);
                                nDead = prey.biom2abd(preyUpon[ipr]);
                                prey.incrementNdead(MortalityCause.PREDATION, nDead);
                                predator.addPreyRecord(prey.getSpeciesIndex(), prey.getTrophicLevel(), prey.getAge(), prey.getLength(), preyUpon[ipr], keepRecord);
                            }
                        }
                        break;
                    case STARVATION:
                        // Starvation mortality
                        school = schools.get(seqStarv[i]);
                        double M = school.getStarvationRate() / subdt;
                        nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-M));
                        school.incrementNdead(MortalityCause.STARVATION, nDead);
                        break;
                    case ADDITIONAL:
                        // Additional mortality
                        school = schools.get(seqNat[i]);
                        // Egg mortality is handled separately and beforehand, 
                        // assuming that the egg loss is not available to predation
                        // and thus these mortality causes should not compete
                        if (school.getAgeDt() > 0) {
                            double D = additionalMortality.getRate(school) / subdt;
                            nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
                            school.incrementNdead(MortalityCause.ADDITIONAL, nDead);
                        }
                        break;
                    case FISHING:
                        // Fishing Mortality
                        school = schools.get(seqFish[i]);
                        switch (fishingType) {
                            case RATE:
                                double F = fishingMortality.getRate(school) / subdt;
                                nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-F));
                                break;
                            case CATCHES:
                                nDead = school.biom2abd(fishingMortality.getCatches(school) / subdt);
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
        }
    }

}
