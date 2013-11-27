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
package fr.ird.osmose.step;

import fr.ird.osmose.School;
import fr.ird.osmose.output.OutputManager;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.FishingProcess;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.IncomingFluxProcess;
import fr.ird.osmose.process.MovementProcess;
import fr.ird.osmose.process.NaturalMortalityProcess;
import fr.ird.osmose.process.OutMortalityProcess;
import fr.ird.osmose.process.PredationProcess;
import fr.ird.osmose.process.ReproductionProcess;
import fr.ird.osmose.process.StarvationProcess;

/**
 *
 * @author pverley
 */
public class SequentialMortalityStep extends AbstractStep {

    /*
     * Growth process
     */
    private AbstractProcess growthProcess;
    /*
     * Reproduction process
     */
    private AbstractProcess reproductionProcess;
    /*
     * Incoming flux of biomass
     */
    private AbstractProcess incomingFLuxProcess;
    /*
     * Fishing process
     */
    private AbstractProcess fishingProcess;
    /*
     * Natural mortality process
     */
    private AbstractProcess naturalMortalityProcess;
    /*
     * Starvation mortality process
     */
    private AbstractProcess starvationProcess;
    /*
     * Predation mortality process
     */
    private AbstractProcess predationProcess;
    /*
     * 
     */
    private AbstractProcess outMortalityProcess;
    /*
     * Movement process
     */
    private AbstractProcess movementProcess;
    /*
     * List of indicators
     */
    private OutputManager indicators;

    public SequentialMortalityStep(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        // initialize natural mortality process
        naturalMortalityProcess = new NaturalMortalityProcess(getRank());
        naturalMortalityProcess.init();

        // initialize starvation process
        predationProcess = new PredationProcess(getRank());
        predationProcess.init();

        // initialize starvation process
        starvationProcess = new StarvationProcess(getRank());
        starvationProcess.init();

        // initialize fishing process
        fishingProcess = new FishingProcess(getRank());
        fishingProcess.init();

        outMortalityProcess = new OutMortalityProcess(getRank());
        outMortalityProcess.init();

        // initiliaza growth process
        growthProcess = new GrowthProcess(getRank());
        growthProcess.init();

        // Reproduction processes
        reproductionProcess = new ReproductionProcess(getRank());
        reproductionProcess.init();

        // Incoming flux
        incomingFLuxProcess = new IncomingFluxProcess(getRank());
        incomingFLuxProcess.init();

        // Movement of the schools
        movementProcess = new MovementProcess(getRank());
        movementProcess.init();

        // Indicators
        indicators = new OutputManager(getRank());
        indicators.init();
    }

    @Override
    public void step(int iStepSimu) {

        // Incoming flux
        incomingFLuxProcess.run();

        // Reset some school state variables 
        for (School school : getSchoolSet()) {
            school.init();
        }

        // Update LTL biomass
        getForcing().update(iStepSimu);

        // Some indicators might need a snapshot of the population
        // at the beginning of the step
        indicators.initStep();

        // Spatial distribution
        movementProcess.run();

        // Total mortality for schools out of simulated domain
        outMortalityProcess.run();

        // Natural mortality (due to other predators)
        naturalMortalityProcess.run();

        // Predation
        predationProcess.run();

        // Starvation
        starvationProcess.run();

        // Growth
        growthProcess.run();

        // Fishing
        fishingProcess.run();

        // Save steps
        indicators.update(iStepSimu);

        // Reproduction
        reproductionProcess.run();

        // Remove dead school
        getSchoolSet().removeDeadSchools();
    }

    @Override
    public void end() {
        // close indicators on last step
        indicators.close();
    }
}
