/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Cell;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import fr.ird.osmose.Prey;
import fr.ird.osmose.background.BackgroundSchool;
import fr.ird.osmose.process.bioen.BioenPredationMortality;
import fr.ird.osmose.process.bioen.BioenStarvationMortality;
import fr.ird.osmose.process.mortality.AbstractMortality;
import fr.ird.osmose.process.mortality.AdditionalMortality;
import fr.ird.osmose.process.mortality.FishingMortality;
import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.process.mortality.OutMortality;
import fr.ird.osmose.process.mortality.ForagingMortality;
import fr.ird.osmose.process.mortality.PredationMortality;
import fr.ird.osmose.process.mortality.StarvationMortality;
import fr.ird.osmose.process.mortality.FishingGear;
import fr.ird.osmose.resource.Resource;
import fr.ird.osmose.util.AccessibilityManager;
import fr.ird.osmose.util.Matrix;
import fr.ird.osmose.util.XSRandom;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mortality processes compete stochastically.
 * <ul>
 * <li>It is assumed that every cause compete with each other.</li>
 * <li>Stochasticity and competition within predation process.</li>
 * <li>Asynchronous updating of school biomass (it means biomass are updated on
 * the fly).</li>
 * </ul>
 */
public class MortalityProcess extends AbstractProcess {

    /*
     * Private instance of mortality for particles outside the simulated domain
     */
    private AbstractMortality outsideMortality;
    /*
     * Private instance of the starvation mortality
     */
    private AbstractMortality starvationMortality;
    /*
     * Private instance of the additional mortality
     */
    private AbstractMortality additionalMortality;
    /*
     * Private instance of the fishing mortality
     */
    private FishingMortality fishingMortality;
    /*
     * Private instance of the predation mortality
     */
    private PredationMortality predationMortality;
    /*
     * Private instance of fishery mortality
     */
    private FishingGear[] fisheriesMortality;
    /**
     * Private instance of bioenergetic starvation mortality
     */
    private BioenStarvationMortality bioenStarvationMortality;
    /*
     * Private instance of bioenergetic oxidative mortality
     */
    private ForagingMortality foragingMortality;

    /** Variables to manage fishery catchabilities. */
    private AccessibilityManager fisheryCatchability;
    
    /** Variables to manage fishery discards. */
    private AccessibilityManager fisheryDiscards;

    /**
     * Whether the Osmose v4 fishery implementation is enabled
     */
    private boolean fisheryEnabled = false;
    /**
     * Number of fisheries
     */
    private int nfishery;
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
     * The set of resource aggregations
     */
    private HashMap<Integer, List<Resource>> resourcesSet;

    public MortalityProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        // Possibility to use a seed in the definition of mortality algorithm
        String key = "stochastic.mortality.seed";
        if (getConfiguration().canFind(key)) {
            random = new XSRandom(getConfiguration().getLong(key));
        } else {
            random = new XSRandom(System.nanoTime());
        }

        fisheryEnabled = getConfiguration().isFisheryEnabled();
        nfishery = getConfiguration().getNFishery();

        additionalMortality = new AdditionalMortality(getRank());
        additionalMortality.init();

        // If not use of bioen, use the traditional predation mort.
        // class. If bioen, use the dedicated class.
        if (!getConfiguration().isBioenEnabled()) {
            predationMortality = new PredationMortality(getRank());
            predationMortality.init();
        } else {
            try {
                predationMortality = new BioenPredationMortality(getRank());
                predationMortality.init();
            } catch (IOException ex) {
                error("Failed to initialize bioenergetic predation mortality", ex);
            }
        }

        // Subdt 
        if (!getConfiguration().isNull("mortality.subdt")) {
            subdt = getConfiguration().getInt("mortality.subdt");
        } else {
            subdt = 10;
            warning("Did not find parameter 'mortality.subdt' for stochastic mortality algorithm. Osmose set it to {0}.", subdt);
        }

        // fishery (Osmose 4) vs fishing mortality (Osmose 3)
        if (fisheryEnabled) {
            fisheriesMortality = new FishingGear[nfishery];
            int count = 0;
            for (int i = 0; i < nfishery; i++) {
                while (!getConfiguration().canFind("fisheries.name.fsh" + count)) {
                    count++;
                }
                fisheriesMortality[i] = new FishingGear(getRank(), count);
                fisheriesMortality[i].init();
                count++;
            }

            fisheryCatchability = new AccessibilityManager(getRank(), "fisheries.catchability", "cat", null);
            fisheryCatchability.init();

            fisheryDiscards = new AccessibilityManager(getRank(), "fisheries.discards", "dis", null);
            fisheryDiscards.init();

        } else {
            fishingMortality = new FishingMortality(getRank());
            fishingMortality.init();
        }

        // Create a new resources set, empty at the moment
        resourcesSet = new HashMap();

        // barrier.n: init the bioenergetic module
        if (this.getConfiguration().isBioenEnabled()) {
            // starvation mortality
            bioenStarvationMortality = new BioenStarvationMortality(getRank());
            bioenStarvationMortality.init();
            // oxidative mortality
            foragingMortality = new ForagingMortality(getRank());
            foragingMortality.init();
        }

        // Mortality that occurs outside the simulated domain is handled separatly
        outsideMortality = new OutMortality(getRank());
        outsideMortality.init();

        // The starvation process is needed to update the starvation mortality
        // rate at the end of the mortality algorithm
        if (!this.getConfiguration().isBioenEnabled()) {
            starvationMortality = new StarvationMortality(getRank());
            starvationMortality.init();
        }
    }

    @Override
    public void run() {

        // Update fishing process (for MPAs)
        if (!fisheryEnabled) {
            fishingMortality.setMPA();
        }

        // Assess accessibility for this time step
        for (Cell cell : getGrid().getOceanCells()) {
            List<School> schools = getSchoolSet().getSchools(cell);
            if (null == schools) {
                continue;
            }
            // Create the list of preys by gathering the schools and the resource groups
            List<IAggregation> preys = new ArrayList();
            preys.addAll(schools);
            preys.addAll(getResources(cell));

            // recovers the list of schools for background species and
            // for the current cell. add this to the list of preys 
            // for the current cell
            preys.addAll(this.getBackgroundSchool(cell));

            // NOTE: at this stage, rsc and bkg biomass is not initialized but 
            // it does not matter: only size is used to define access.
            // Loop over focal schools, which are viewed here as predators.
            // Consider predation over resource, background and focal species.
            for (School school : schools) {
                school.setAccessibility(predationMortality.getAccessibility(school, preys));
                school.setPredSuccessRate(0);
                if (school.isLarva()) {
                    // Egg loss, not accessible to predation process
                    double D = additionalMortality.getRate(school);
                    double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
                    school.incrementNdead(MortalityCause.ADDITIONAL, nDead);
                    school.retainEgg();
                }
            }

            // Loop over background species, which are this time predators.
            for (BackgroundSchool bkg : this.getBackgroundSchool(cell)) {
                bkg.setAccessibility(predationMortality.getAccessibility(bkg, preys));
                bkg.setPredSuccessRate(0);
            }

        } // end of cell loop

        // Update resources biomass
        int iStepSimu = getSimulation().getIndexTimeSimu();
        for (List<Resource> resources : resourcesSet.values()) {    // loop over the cells
            for (Resource resource : resources) {    // loop over the resources
                int iRsc = resource.getSpeciesIndex();
                double accessibleBiom = getConfiguration().getResourceSpecies(iRsc).getAccessibility(iStepSimu)
                        * getResourceForcing(iRsc).getBiomass(resource.getCell());
                resource.setBiomass(accessibleBiom);
            }
        }

        // Init the biomass of background species by using the ResourceForcing class
        for (List<BackgroundSchool> bkgSchoolList : this.getBkgSchoolSet().getValues()) {    // loop over the cells
            for (BackgroundSchool bkg : bkgSchoolList) {    // loop over the resources
                int ibkg = bkg.getSpeciesIndex();
                double accessibleBiom = getResourceForcing(ibkg).getBiomass(bkg.getCell());
                // note that here, the multiplication by proportion value is made in the setbiomass method
                bkg.setBiomass(accessibleBiom);
                bkg.init();  // reset ndead prior predation
            }
        }

        int[] ncellBatch = dispatchCells();
        int nbatch = ncellBatch.length;
        for (int idt = 0; idt < subdt; idt++) {
            if (!fisheryEnabled) {
                fishingMortality.assessFishableBiomass();
            }
            CountDownLatch doneSignal = new CountDownLatch(nbatch);
            int iStart = 0, iEnd = 0;
            for (int ibatch = 0; ibatch < nbatch; ibatch++) {
                iEnd += ncellBatch[ibatch];
                new Thread(new MortalityWorker(iStart, iEnd, doneSignal)).start();
                iStart += ncellBatch[ibatch];
            }
            try {
                doneSignal.await();
            } catch (InterruptedException ex) {
                error("Multithread mortality process terminated unexpectedly.", ex);
            }
        }

        // Update starvation mortality rate and trophic level
        for (School school : getSchoolSet().getSchools()) {
            // Calculate starvation mortality rate that will be apply at next time step
            if (!getConfiguration().isBioenEnabled()) {
                school.setStarvationRate(starvationMortality.getRate(school));
            }
            // Update trophic level
            if (school.getPreyedBiomass() > 0) {
                Collection<Prey> preys = school.getPreys();
                if (!preys.isEmpty()) {
                    double trophicLevel = 0.d;
                    for (Prey prey : preys) {
                        trophicLevel += prey.getTrophicLevel() * prey.getBiomass() / school.getPreyedBiomass();
                    }
                    trophicLevel += 1;
                    school.setTrophicLevel((float) trophicLevel);
                }
            }
        }

        // Apply Zout mortality on schools out of the simulated domain
        for (School school : getSchoolSet().getOutSchools()) {
            double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-outsideMortality.getRate(school)));
            if (nDead > 0.d) {
                school.setNdead(MortalityCause.OUT, nDead);
            }
        }
    }

    /**
     * Stochastic mortality algorithm > It is assumed that every cause compete
     * with each other. > Stochasticity and competition within predation
     * process. > Asynchronous updating of school biomass (it means biomass are
     * updated on the fly).
     */
    private void computeMortality(int subdt, Cell cell) throws Exception {

        List<School> schools = getSchoolSet().getSchools(cell);
        if (null == schools) {
            return;
        }
        int ns = schools.size();

        // Create the list of preys by gathering the schools and the resource groups
        List<IAggregation> preys = new ArrayList();

        // add focal schools to prey
        preys.addAll(schools);

        schools.stream().filter((prey) -> (prey.isLarva())).forEachOrdered((prey) -> {
            prey.releaseEgg(subdt);
        }); // Release some eggs for current subdt (initial abundance / subdt)

        // add resource swarms to preys
        preys.addAll(getResources(cell));

        // Recover the list of background schools for the current cell
        List<BackgroundSchool> bkgSchool = this.getBackgroundSchool(cell);
        int nBkg = bkgSchool.size();

        // barrier.n: adding background species to the list of possible preys.
        preys.addAll(bkgSchool);

        // preys contains focal species + resources + bkg species
        // Arrays for loop over schools are initialised with nfocal + nbackgroud
        Integer[] seqPred = new Integer[ns + nBkg];
        for (int i = 0; i < ns + nBkg; i++) {
            seqPred[i] = i;
        }

        Integer[] seqFish = Arrays.copyOf(seqPred, ns + nBkg);
        Integer[] seqNat = Arrays.copyOf(seqPred, ns + nBkg);
        Integer[] seqStarv = Arrays.copyOf(seqPred, ns + nBkg);
        Integer[] seqFor = Arrays.copyOf(seqPred, ns + nBkg);

        // init a list of mortality causes, containing all the original mortality causes
        List<MortalityCause> causes = new ArrayList();
        causes.addAll(Arrays.asList(MortalityCause.values()));

        // add all the fisheries in the cause list
        if (fisheryEnabled) {
            // every fishery accounts as an independant fishing mortality source
            // note that we start at 1 since the addAll already include one fishing
            // mortality source
            for (int i = 1; i < nfishery; i++) {
                causes.add(MortalityCause.FISHING);
            }
        }
        MortalityCause[] mortalityCauses = causes.toArray(new MortalityCause[causes.size()]);

        if (fisheryEnabled) {
            Matrix catchability = this.fisheryCatchability.getMatrix();
            for (FishingGear gear : this.fisheriesMortality) {
                gear.setCatchability(catchability);
            }

            Matrix discards = this.fisheryDiscards.getMatrix();
            for (FishingGear gear : this.fisheriesMortality) {
                gear.setDiscards(discards);
            }

        }

        // distinct random fishery sequences for every school
        Integer[] singleSeqFishery = new Integer[nfishery];
        for (int i = 0; i < nfishery; i++) {
            singleSeqFishery[i] = i;
        }

        // dimension [spec+bkg][seqFishery]
        // order, for each fished school, of the fishery attacks
        Integer[][] seqFishery = new Integer[ns + nBkg][];
        for (int i = 0; i < ns + nBkg; i++) {
            seqFishery[i] = Arrays.copyOf(singleSeqFishery, nfishery);
            shuffleArray(seqFishery[i]);
        }

        int[] indexFishery = new int[ns + nBkg];

        // Initialisation of list of predators, which contains both
        // background species and focal species.
        // pred contains focal + bkg species
        ArrayList<AbstractSchool> listPred = new ArrayList<>();
        listPred.addAll(schools);
        listPred.addAll(bkgSchool);

        shuffleArray(seqPred);
        shuffleArray(seqFish);
        shuffleArray(seqNat);
        shuffleArray(seqStarv);
        shuffleArray(seqFor);

        boolean keepRecord = getSimulation().isPreyRecord();
        for (int i = 0; i < ns + nBkg; i++) {               // loop over all the school (focal and bkg) as predators.
            shuffleArray(mortalityCauses);
            for (MortalityCause cause : mortalityCauses) {   // random loop over all the mortality causes
                School school;
                double nDead = 0;
                switch (cause) {

                    // barrier.n: adding the 
                    case FORAGING:
                        if ((seqFor[i] >= ns) || (!getConfiguration().isBioenEnabled())) {
                            // oxidative mortality for bion module and focal species only
                            break;
                        }
                        school = schools.get(seqFor[i]);
                        // oxidative mortality rate at current sub time step                      
                        double Mo = foragingMortality.getRate(school) / subdt;
                        if (Mo > 0.d) {
                            nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-Mo));
                            school.incrementNdead(MortalityCause.FORAGING, nDead);
                        }
                        break;
                    case PREDATION:
                        // Predation mortality
                        IAggregation predator = listPred.get(seqPred[i]);   // recover one predator (background or focal species)
                        // compute predation from predator to all the possible preys
                        // preyUpon is the total biomass easten by predator
                        double[] preyUpon = predationMortality.computePredation(predator, preys, predator.getAccessibility(), subdt);
                        for (int ipr = 0; ipr < preys.size(); ipr++) {
                            if (preyUpon[ipr] > 0) {
                                // Loop over all the preys. If they are eaten by the predator,
                                // the biomass of the prey is updted
                                IAggregation prey = preys.get(ipr);
                                nDead = prey.biom2abd(preyUpon[ipr]);   // total biomass that has been eaten
                                prey.incrementNdead(MortalityCause.PREDATION, nDead);
                                predator.preyedUpon(prey.getSpeciesIndex(), prey.getTrophicLevel(), prey.getAge(), prey.getLength(), preyUpon[ipr], keepRecord);
                            }
                        }
                        break;
                    case STARVATION:

                        if (seqStarv[i] >= ns) {
                            break;   // if background school, nothing is done
                        }

                        school = schools.get(seqStarv[i]);
                        nDead = 0.d;
                        if (!this.getConfiguration().isBioenEnabled()) {
                            // Starvation mortality when no use of bioen module.                           
                            double M = school.getStarvationRate() / subdt;
                            nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-M));
                        } else if (school.getAgeDt() > 0) {
                            // computation of the starvation mortality
                            // which is updated directly from the BioenMortality class.
                            // computes starv.mortality only for species greater than 0 years old
                            nDead = bioenStarvationMortality.computeStarvation(school, subdt);
                        }
                        if (nDead > 0.d) {
                            school.incrementNdead(MortalityCause.STARVATION, nDead);
                        }

                        break;
                    case ADDITIONAL:
                        if (seqNat[i] >= ns) {
                            break;    // if background school, nothing is done
                        }
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

                        // Osmose 4 fishery mortality
                        if (fisheryEnabled) {

                            AbstractSchool fishedSchool = listPred.get(seqFish[i]);

                            // determine the index of the fishery to read.
                            // here, we use [i] and not seq[i] because it does not matter much
                            int iFishery = seqFishery[i][indexFishery[i]];
                            double F = fisheriesMortality[iFishery].getRate(fishedSchool) / subdt;
                            nDead = fishedSchool.getInstantaneousAbundance() * (1.d - Math.exp(-F));

                            // Percentage values of discarded fish. The remaining go to fishery.
                            double discardRate = fisheriesMortality[iFishery].getDiscardRate(fishedSchool);

                            fishedSchool.fishedBy(iFishery, fishedSchool.abd2biom((1 - discardRate) * nDead));
                            fishedSchool.discardedBy(iFishery, fishedSchool.abd2biom(discardRate * nDead));

                            fishedSchool.incrementNdead(MortalityCause.FISHING, nDead);

                            // make sure a different fishery is called every time
                            // it is just a trick since we do not have case FISHERY1,
                            // case FISHERY2, etc. like the other mortality sources.
                            indexFishery[i]++;
                        } else {

                            // Possibility to fish background species?????
                            if (seqFish[i] >= ns) {
                                break;
                            }

                            // recovers the current school
                            school = schools.get(seqFish[i]);

                            // Osmose 3 fishing Mortality
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

                        }
                        break;
                }  // end of switch (cause
            }   // end of mort cause loop
        }   // end of school loop species loop
    }    // end of function

    private List<Resource> getResources(Cell cell) {
        if (!resourcesSet.containsKey(cell.getIndex())) {
            List<Resource> resources = new ArrayList();
            for (int iRsc : getConfiguration().getRscIndex()) {
                resources.add(new Resource(getConfiguration().getResourceSpecies(iRsc), cell));
            }
            resourcesSet.put(cell.getIndex(), resources);
        }
        return resourcesSet.get(cell.getIndex());
    }

    /**
     * Shuffles an input array.
     *
     * @param <T> type of array
     * @param a input array
     */
    public static <T> void shuffleArray(T[] a) {
        // Shuffle array
        for (int i = a.length; i > 1; i--) {
            T tmp = a[i - 1];
            int j = random.nextInt(i);
            a[i - 1] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * Split the ocean cells in batches that will run on concurrent threads.
     * Distribute them evenly considering number of schools per batches of ocean
     * cells.
     *
     * @return integer array, the number of ocean cells for every batch
     */
    private int[] dispatchCells() {

        // number of school in a batch
        int nschoolBatch = 0;
        // number of procs available for multithreading
        int ncpu = Math.max(1, getConfiguration().getNCpu() / getConfiguration().getNSimulation());
        // number of schools to be handled by each proc
        int nschoolPerCPU = getSchoolSet().getSchools().size() / ncpu;
        // array of number of cells in every batch
        int[] ncellBatch = new int[ncpu];
        // index of current batch [0, ncpu - 1]
        int ibatch = 0;
        for (Cell cell : getGrid().getOceanCells()) {
            // number of schools in current cell
            List<School> schools = getSchoolSet().getSchools(cell);
            int nschoolCell = (null == schools) ? 0 : schools.size();
            // check whether the batch reaches expected number of schools
            if (nschoolBatch + nschoolCell > nschoolPerCPU) {
                // current batch reached expected number of schools
                // check whether the batch with or without current cell is
                // closer to average number of schools per CPU
                if (nschoolBatch + nschoolCell - nschoolPerCPU > nschoolPerCPU - nschoolBatch) {
                    // batch without current cell is closer to nschoolPerCPU, so
                    // schools of current cell go to next batch.
                    // current cell counts as 1st cell of next batch
                    ncellBatch[Math.min(ibatch + 1, ncpu - 1)] += 1;
                    // schools of current cell go to next batch
                    nschoolBatch = nschoolCell;
                } else {
                    // current cell is attached to current batch
                    // set final number of ocean cells in current batch
                    ncellBatch[ibatch] += 1;
                    nschoolBatch = 0;
                }
                // increment batch index
                ibatch = Math.min(ibatch + 1, ncpu - 1);
            } else {
                // current batch not full yet
                // increment number of schools in current batch
                nschoolBatch += nschoolCell;
                ncellBatch[ibatch] += 1;
            }
        }

        return ncellBatch;
    }

    /**
     * Implementation of the Fork/Join algorithm for splitting the set of cells
     * in several subsets.
     */
    private class MortalityWorker implements Runnable {

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
         * @param doneSignal, the CountDownLatch object
         */
        MortalityWorker(int iStart, int iEnd, CountDownLatch doneSignal) {
            this.iStart = iStart;
            this.iEnd = iEnd;
            this.doneSignal = doneSignal;
        }

        /**
         * Loop over the subset of cells and apply the
         * {@link fr.ird.osmose.process.mortality.StochasticMortalityProcess#computeMortality(int, fr.ird.osmose.Cell)}
         * function.
         */
        @Override
        public void run() {
            try {
                List<Cell> cells = getGrid().getOceanCells();
                for (int iCell = iStart; iCell < iEnd; iCell++) {
                    try {
                        computeMortality(subdt, cells.get(iCell));
                    } catch (Exception ex) {
                        Logger.getLogger(MortalityProcess.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } finally {
                doneSignal.countDown();
            }
        }
    }

//    /**
//     * Recovers the list of background schools for the current cell. If the
//     * current cell does not contain any background school yet, they are added.
//     * This is the same as for the getResources method.
//     *
//     * @param cell
//     * @return
//     */
//    private List<BackgroundSchool> getBackgroundSchool(Cell cell) {
//        if (!bkgSet.containsKey(cell.getIndex())) {
//            // If the cell does not contain any background school
//            // initialisation of a list of cells.
//            List<BackgroundSchool> output = new ArrayList<>();
//            // Loop over all the background species
//            for (int iBkg = 0; iBkg < getConfiguration().getNBkgSpecies(); iBkg++) {
//                BackgroundSpecies bkgSpec = getConfiguration().getBkgSpecies(iBkg);
//                // Loop over all the classes of the background species.
//                for (int iClass = 0; iClass < bkgSpec.getTimeSeries().getNClass(); iClass++) {
//                    // Init a background school of species bkgSpec and of class iClass
//                    BackgroundSchool BkgSchTmp = new BackgroundSchool(bkgSpec, iClass);
//                    // Move the bkg school to cell (set x and y)
//                    BkgSchTmp.moveToCell(cell);
//                    // add to output
//                    output.add(BkgSchTmp);
//                }   // end of iClass loop
//            }   // end of bkg loop
//            // add the list to the hash map
//            bkgSet.put(cell.getIndex(), output);
//        }   // end of contains test
//        return bkgSet.get(cell.getIndex());
//    }   // end of function
    /**
     * Recovers the list of background schools for the current cell. If the
     * current cell does not contain any background school yet, they are added.
     * This is the same as for the getResources method.
     *
     * @param cell
     * @return
     */
    private List<BackgroundSchool> getBackgroundSchool(Cell cell) {
        return this.getBkgSchoolSet().getBackgroundSchool(cell);
    }
}
