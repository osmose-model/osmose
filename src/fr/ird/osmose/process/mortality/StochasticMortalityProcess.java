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
import fr.ird.osmose.Swarm;
import fr.ird.osmose.util.XSRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import static java.util.concurrent.ForkJoinTask.invokeAll;
import java.util.concurrent.RecursiveAction;

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
    /**
     * The set of plankton swarms
     */
    private HashMap<Integer, List<Swarm>> swarmSet;

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
            warning("Did not find parameter 'mortality.subdt' for stochastic mortality algorithm. Osmose set it to {0}.", subdt);
        }

        // Create a new swarm set, empty at the moment
        swarmSet = new HashMap();
    }

    @Override
    public void run() {

        // Update fishing process (for MPAs)
        fishingMortality.setMPA();

        // Assess accessibility for this time step
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getSchoolSet().getSchools(cell);
            if (cell.isLand() || (null == schools)) {
                continue;
            }
            // Create the list of preys by gathering the schools and the plankton group
            List<IAggregation> preys = new ArrayList();
            preys.addAll(schools);
            preys.addAll(getSwarms(cell));
            for (School school : schools) {
                school.setAccessibility(predationMortality.getAccessibility(school, preys));
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

        // Update swarms biomass
        int iStepSimu = getSimulation().getIndexTimeSimu();
        for (List<Swarm> swarms : swarmSet.values()) {
            for (Swarm swarm : swarms) {
                int iltl = swarm.getLTLIndex();
                double accessibleBiom = getConfiguration().getPlankton(iltl).getAccessibility(iStepSimu)
                        * getForcing().getBiomass(iltl, swarm.getCell());
                swarm.setBiomass(accessibleBiom);
            }
        }

        int ncpu = Math.max(1, getConfiguration().getNCpu() / getConfiguration().getNSimulation());
        int[] ncell = new int[ncpu];
        int nbasecell = getGrid().getNOceanCell() / ncpu;
        for (int k = 0; k < ncpu; k++) {
            ncell[k] = nbasecell;
        }
        int nleftcell = getGrid().getNOceanCell() - nbasecell * ncpu;
        for (int k = 0; k < nleftcell; k++) {
            ncell[k % nleftcell] += 1;
        }
        for (int idt = 0; idt < subdt; idt++) {
            fishingMortality.assessFishableBiomass();
            CountDownLatch doneSignal = new CountDownLatch(ncpu);
            int iStart = 0, iEnd = 0;
            for (int icpu = 0; icpu < ncpu; icpu++) {
                iEnd += ncell[icpu];
                new Thread(new Worker(iStart, iEnd, doneSignal)).start();
                iStart += ncell[icpu];
            }
            try {
                doneSignal.await();
            } catch (InterruptedException ex) {
                error("Multithread mortality process terminated unexpectedly.", ex);
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
        if (null == schools) {
            return;
        }
        int ns = schools.size();

        // Create the list of preys by gathering the schools and the plankton group
        List<IAggregation> preys = new ArrayList();
        preys.addAll(schools);
        for (School prey : schools) {
            // Release some eggs for current subdt (initial abundance / subdt)
            if (prey.getAgeDt() == 0) {
                prey.releaseEgg(subdt);
            }
        }
        preys.addAll(getSwarms(cell));

        Integer[] seqPred = new Integer[ns];
        for (int i = 0; i < ns; i++) {
            seqPred[i] = i;
        }
        Integer[] seqFish = Arrays.copyOf(seqPred, ns);
        Integer[] seqNat = Arrays.copyOf(seqPred, ns);
        Integer[] seqStarv = Arrays.copyOf(seqPred, ns);
        MortalityCause[] mortalityCauses = MortalityCause.values();

        shuffleArray(seqPred);
        shuffleArray(seqFish);
        shuffleArray(seqNat);
        shuffleArray(seqStarv);
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
                        double[] preyUpon = predationMortality.computePredation(predator, preys, predator.getAccessibility(), subdt);
                        for (int ipr = 0; ipr < preys.size(); ipr++) {
                            if (preyUpon[ipr] > 0) {
                                IAggregation prey = preys.get(ipr);
                                nDead = prey.biom2abd(preyUpon[ipr]);
                                prey.incrementNdead(MortalityCause.PREDATION, nDead);
                                predator.preyedUpon(prey.getSpeciesIndex(), prey.getTrophicLevel(), prey.getAge(), prey.getLength(), preyUpon[ipr], keepRecord);
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
                        switch (fishingMortality.getType(school.getSpeciesIndex())) {
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

    private List<Swarm> getSwarms(Cell cell) {
        if (!swarmSet.containsKey(cell.getIndex())) {
            List<Swarm> swarms = new ArrayList();
            for (int iLTL = 0; iLTL < getConfiguration().getNPlankton(); iLTL++) {
                swarms.add(new Swarm(getConfiguration().getPlankton(iLTL), cell));
            }
            swarmSet.put(cell.getIndex(), swarms);
        }
        return swarmSet.get(cell.getIndex());
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

    /**
     * Implementation of the Fork/Join algorithm for splitting the set of cells
     * in several subsets.
     */
    private class Worker implements Runnable {

        private final int iStart, iEnd;
        /**
         * The {@link java.util.concurrent.CountDownLatch} that will wait for
         * this {@link Simulation} to complete before decrementing the count of
         * the latch.
         */
        private final CountDownLatch doneSignal;

        /**
         * Creates a new {@code ForkStep} that will handle a subset of cells.
         *
         * @param iStart, index of the first cell of the subset
         * @param iEnd , index of the last cell of the subset
         */
        Worker(int iStart, int iEnd, CountDownLatch doneSignal) {
            this.iStart = iStart;
            this.iEnd = iEnd;
            this.doneSignal = doneSignal;
        }

        /**
         * Loop over the subset of cells and apply the
         * {@link fr.ird.osmose.process.mortality.StochasticMortalityProcess#computeMortality_stochastic(int, fr.ird.osmose.Cell)}
         * function.
         */
        @Override
        public void run() {
            try {
                List<Cell> cells = getGrid().getOceanCells();
                for (int iCell = iStart; iCell < iEnd; iCell++) {
                    computeMortality_stochastic(subdt, cells.get(iCell));
                }
            } finally {
                doneSignal.countDown();
            }
        }
    }
}
