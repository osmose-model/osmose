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
package fr.ird.osmose.output;

import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.output.distribution.AgeDistribution;
import fr.ird.osmose.output.distribution.SizeDistribution;
import fr.ird.osmose.output.distribution.TLDistribution;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.util.ArrayList;
import java.util.List;
import fr.ird.osmose.output.spatial.SpatialAbundanceOutput;
import fr.ird.osmose.output.spatial.SpatialBiomassOutput;
import fr.ird.osmose.output.spatial.SpatialYieldOutput;
import fr.ird.osmose.output.spatial.SpatialYieldNOutput;
import fr.ird.osmose.output.spatial.SpatialTLOutput;
import fr.ird.osmose.output.spatial.SpatialSizeOutput;

/**
 *
 * @author pverley
 */
public class OutputManager extends SimulationLinker {

    // List of the indicators
    final private List<IOutput> outputs;
    /**
     * Object that is able to take a snapshot of the set of schools and write it
     * in a NetCDF file. Osmose will be able to restart on such a file.
     */
    final private SchoolSetSnapshot snapshot;
    /**
     * Record frequency for writing restart files, in number of time step.
     */
    private int restartFrequency;
    /**
     * Whether the restart files should be written or not
     */
    private boolean writeRestart;
    /**
     * Number of years before writing restart files.
     */
    private int spinupRestart;

    private boolean useNetcdf = false;

    public OutputManager(int rank) {
        super(rank);
        outputs = new ArrayList();
        snapshot = new SchoolSetSnapshot(rank);
    }

    public void init() {

        int rank = getRank();
        /*
         * Delete existing outputs from previous simulation
         */
        if (!getSimulation().isRestart()) {
            // Delete previous simulation of the same name
            String pattern = getConfiguration().getString("output.file.prefix") + "*_Simu" + rank + "*";
            IOTools.deleteRecursively(getConfiguration().getOutputPathname(), pattern);
        }

        AbstractDistribution sizeDistrib = new SizeDistribution();
        AbstractDistribution ageDistrib = new AgeDistribution();

        useNetcdf = getConfiguration().getBoolean("output.use.netcdf");
        /*
        outputs.add(new AbundanceOutput_Netcdf(rank));
        outputs.add(new BiomassOutput_Netcdf(rank));
        outputs.add(new YieldOutput_Netcdf(rank));
        outputs.add(new YieldNOutput_Netcdf(rank));
        outputs.add(new BiomassDistribOutput_Netcdf(rank, sizeDistrib));
        outputs.add(new BiomassDistribOutput_Netcdf(rank, ageDistrib));
        outputs.add(new AbundanceDistribOutput_Netcdf(rank, sizeDistrib));
        outputs.add(new AbundanceDistribOutput_Netcdf(rank, ageDistrib));
        outputs.add(new BiomassDietStageOutput_Netcdf(rank));
        outputs.add(new DietOutput_Netcdf(rank));
        
        for (int i = 0; i < getNSpecies(); i++) {
            //outputs.add(new DietDistribOutput_Netcdf(rank, getSpecies(i), ageDistrib));
            //outputs.add(new MortalityOutput_Netcdf(rank, getSpecies(i)));
            outputs.add(new MortalitySpeciesOutput_Netcdf(rank, getSpecies(i), ageDistrib));
            break;
        }
         */

 /*
         * Instantiate indicators
         */
        if (getConfiguration().getBoolean("output.spatialabundance.enabled")) {
            outputs.add(new SpatialAbundanceOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialbiomass.enabled")) {
            outputs.add(new SpatialBiomassOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialsize.enabled")) {
            outputs.add(new SpatialSizeOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialyield.enabled")) {
            outputs.add(new SpatialYieldOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialyieldN.enabled")) {
            outputs.add(new SpatialYieldNOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialtl.enabled")) {
            outputs.add(new SpatialTLOutput(rank));
        }

        // Barrier.n: Saving of spatial, class (age or size) structure abundance
        if (getConfiguration().getBoolean("output.spatialsizespecies.enabled")) {
            outputs.add(new SpatialSizeSpeciesOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.spatialagespecies.enabled")) {
            outputs.add(new SpatialSizeSpeciesOutput(rank, ageDistrib));
        }
        // Barrier.n: Fisheries saving
        if (getConfiguration().getBoolean("output.fisheries.enabled")) {
            outputs.add(new FisheriesOutput(rank));
        }
        // Biomass
        if (getConfiguration().getBoolean("output.biomass.enabled")) {
            outputs.add(new BiomassOutput(rank));
        }
        if (getConfiguration().getBoolean("output.biomass.bysize.enabled")) {
            outputs.add(new BiomassDistribOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.biomass.byage.enabled")) {
            outputs.add(new BiomassDistribOutput(rank, ageDistrib));
        }
        // Abundance
        if (getConfiguration().getBoolean("output.abundance.enabled")) {
            outputs.add(new AbundanceOutput(rank));
        }
        if (getConfiguration().getBoolean("output.abundance.bysize.enabled")) {
            outputs.add(new AbundanceDistribOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.abundance.byage.enabled")) {
            outputs.add(new AbundanceDistribOutput(rank, ageDistrib));
        }
        // Mortality
        if (getConfiguration().getBoolean("output.mortality.enabled")) {
            outputs.add(new MortalityOutput(rank));
        }
        if (getConfiguration().getBoolean("output.mortality.perSpecies.byage.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new MortalitySpeciesOutput(rank, getSpecies(i), ageDistrib));
            }
        }
        // phv 20150413, it should be size distribution at the beginning of the
        // time step. To be fixed
        if (getConfiguration().getBoolean("output.mortality.perSpecies.bysize.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new MortalitySpeciesOutput(rank, getSpecies(i), sizeDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.mortality.natural.bySize.enabled")) {
            outputs.add(new AdditionalMortalityDistribOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.mortality.natural.byAge.enabled")) {
            outputs.add(new AdditionalMortalityDistribOutput(rank, ageDistrib));
        }
        if (getConfiguration().getBoolean("output.mortality.naturalN.bySize.enabled")) {
            outputs.add(new AdditionalMortalityNDistribOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.mortality.naturalN.byAge.enabled")) {
            outputs.add(new AdditionalMortalityNDistribOutput(rank, ageDistrib));
        }
        // Yield
        if (getConfiguration().getBoolean("output.yield.biomass.enabled")) {
            outputs.add(new YieldOutput(rank));
        }
        if (getConfiguration().getBoolean("output.yield.abundance.enabled")) {
            outputs.add(new YieldNOutput(rank));
        }
        // Size
        if (getConfiguration().getBoolean("output.size.enabled")) {
            outputs.add(new MeanSizeOutput(rank));
        }
        if (getConfiguration().getBoolean("output.size.catch.enabled")) {
            outputs.add(new MeanSizeCatchOutput(rank));
        }
        if (getConfiguration().getBoolean("output.yieldN.bySize.enabled")) {
            outputs.add(new YieldNDistribOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.yield.bySize.enabled")) {
            outputs.add(new YieldDistribOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.meanSize.byAge.enabled")) {
            outputs.add(new MeanSizeDistribOutput(rank, ageDistrib));
        }
        // Age
        if (getConfiguration().getBoolean("output.yieldN.byAge.enabled")) {
            outputs.add(new YieldNDistribOutput(rank, ageDistrib));
        }
        if (getConfiguration().getBoolean("output.yield.byAge.enabled")) {
            outputs.add(new YieldDistribOutput(rank, ageDistrib));
        }
        // TL
        if (getConfiguration().getBoolean("output.tl.enabled")) {
            outputs.add(new MeanTrophicLevelOutput(rank));
        }
        if (getConfiguration().getBoolean("output.tl.catch.enabled")) {
            outputs.add(new MeanTrophicLevelCatchOutput(rank));
        }
        if (getConfiguration().getBoolean("output.biomass.bytl.enabled")) {
            outputs.add(new BiomassDistribOutput(rank, new TLDistribution()));
        }
        if (getConfiguration().getBoolean("output.meanTL.bySize.enabled")) {
            outputs.add(new MeanTrophicLevelDistribOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.meanTL.byAge.enabled")) {
            outputs.add(new MeanTrophicLevelDistribOutput(rank, ageDistrib));
        }
        // Predation
        if (getConfiguration().getBoolean("output.diet.composition.enabled")) {
            outputs.add(new DietOutput(rank));
        }
        if (getConfiguration().getBoolean("output.diet.composition.byage.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput(rank, getSpecies(i), ageDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.diet.composition.bysize.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput(rank, getSpecies(i), sizeDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.diet.pressure.enabled")) {
            outputs.add(new PredatorPressureOutput(rank));
        }
        if (getConfiguration().getBoolean("output.diet.pressure.enabled")) {
            outputs.add(new BiomassDietStageOutput(rank));
        }
        if (getConfiguration().getBoolean("output.diet.pressure.byage.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new PredatorPressureDistribOutput(rank, getSpecies(i), ageDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.diet.pressure.bysize.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new PredatorPressureDistribOutput(rank, getSpecies(i), sizeDistrib));
            }
        }
        // Spatialized
        if (getConfiguration().getBoolean("output.spatial.enabled")) {
            outputs.add(new SpatialOutput(rank));
        }
        if (getConfiguration().getBoolean("output.spatial.ltl.enabled")) {
            outputs.add(new LTLOutput(rank));
        }

        // Debugging outputs
        boolean NO_WARNING = false;
        if (getConfiguration().getBoolean("output.ssb.enabled", NO_WARNING)) {
            outputs.add(new SSBOutput(rank));
        }
        if (getConfiguration().getBoolean("output.nschool.enabled", NO_WARNING)) {
            outputs.add(new NSchoolOutput(rank));
        }
        if (getConfiguration().getBoolean("output.nschool.byage.enabled", NO_WARNING)) {
            outputs.add(new NSchoolDistribOutput(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.nschool.bysize.enabled", NO_WARNING)) {
            outputs.add(new NSchoolDistribOutput(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.ndeadschool.enabled", NO_WARNING)) {
            outputs.add(new NDeadSchoolOutput(rank));
        }

        if (getConfiguration().getBoolean("output.ndeadschool.byage.enabled", NO_WARNING)) {
            outputs.add(new NDeadSchoolDistribOutput(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.ndeadschool.bysize.enabled", NO_WARNING)) {
            outputs.add(new NDeadSchoolDistribOutput(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.bioen.maturesize.enabled", NO_WARNING)) {
            outputs.add(new BioenSizeMatureOutput(rank));
        }

        if (getConfiguration().getBoolean("output.bioen.matureage.enabled", NO_WARNING)) {
            outputs.add(new BioenAgeMatureOutput(rank));
        }

        if (getConfiguration().getBoolean("output.bioen.ingest.enabled", NO_WARNING)) {
            outputs.add(new BioenIngestOutput(rank));
        }

        if (getConfiguration().getBoolean("output.bioen.maint.enabled", NO_WARNING)) {
            outputs.add(new BioenMaintOutput(rank));
        }

        if (getConfiguration().getBoolean("output.bioen.growthpot.enabled", NO_WARNING)) {
            outputs.add(new BioenGrowthPot(rank));
        }

        if (getConfiguration().getBoolean("output.bioen.sizeInf.enabled", NO_WARNING)) {
            outputs.add(new BioenSizeInfOutput(rank));
        }

        if (getConfiguration().getBoolean("output.regional.biomass.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new RegionalOutputsBiomass(rank, getSpecies(i)));
            }
        }

        if (getConfiguration().getBoolean("output.regional.abundance.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new RegionalOutputsAbundance(rank, getSpecies(i)));
            }
        }

        // warning: simulation init is called after output init.
        List<String> genet_keys = this.getConfiguration().findKeys("*.trait.mean");
        if (genet_keys.size() > 0) {
            if (getConfiguration().getBoolean("output.evolvingtraits.enabled")) {
                outputs.add(new VariableTraitOutput(rank));
            }
        }

        /*
         * Initialize indicators
         */
        for (IOutput indicator : outputs) {
            indicator.init();
            indicator.reset();
        }

        // Initialize the restart maker
        restartFrequency = Integer.MAX_VALUE;
        if (!getConfiguration().isNull("output.restart.recordfrequency.ndt")) {
            restartFrequency = getConfiguration().getInt("output.restart.recordfrequency.ndt");
        }

        writeRestart = true;
        if (!getConfiguration().isNull("output.restart.enabled")) {
            writeRestart = getConfiguration().getBoolean("output.restart.enabled");
        } else {
            warning("Could not find parameter 'output.restart.enabled'. Osmose assumes it is true and a NetCDF restart file will be created at the end of the simulation (or more, depending on parameters 'simulation.restart.recordfrequency.ndt' and 'simulation.restart.spinup').");
        }

        spinupRestart = 0;
        if (!getConfiguration().isNull("output.restart.spinup")) {
            spinupRestart = getConfiguration().getInt("output.restart.spinup") - 1;
        }
    }

    public void close() {
        for (IOutput indicator : outputs) {
            indicator.close();
        }
    }

    public void initStep() {
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            for (IOutput indicator : outputs) {
                indicator.initStep();
            }
        }
    }

    public void update(int iStepSimu) {

        // UPDATE
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            for (IOutput indicator : outputs) {
                indicator.update();
                // WRITE
                if (indicator.isTimeToWrite(iStepSimu)) {
                    float time = (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
                    indicator.write(time);
                    indicator.reset();
                }
            }
        }
    }

    public void writeRestart(int iStepSimu) {
        // Create a restart file
        boolean isTimeToWrite = writeRestart;
        isTimeToWrite &= (getSimulation().getYear() >= spinupRestart);
        isTimeToWrite &= ((iStepSimu + 1) % restartFrequency == 0);
        isTimeToWrite |= (iStepSimu >= (getConfiguration().getNStep() - 1));

        if (isTimeToWrite) {
            snapshot.makeSnapshot(iStepSimu);
        }
    }
}
