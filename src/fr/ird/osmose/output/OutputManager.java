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

import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public class OutputManager extends SimulationLinker {

    // List of the indicators
    final private List<IOutput> indicators;

    // Default CSV separator
    public final static String SEPARATOR = ",";

    public OutputManager(int rank) {
        super(rank);
        indicators = new ArrayList();
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
        /*
         * Instantiate indicators
         */
        // Biomass
        indicators.add(new BiomassOutput(rank, "output.biomass.enabled"));
        // Abundance
        indicators.add(new AbundanceOutput(rank, "output.abundance.enabled"));
        // Mortality
        indicators.add(new MortalityOutput(rank, "output.mortality.enabled"));
        if (getConfiguration().getBoolean("output.mortality.perSpecies.perAge.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                indicators.add(new MortalitySpeciesOutput(rank, "output.mortality.perSpecies.perAge.enabled", getSpecies(i), AbstractSpectrumOutput.Type.AGE));
            }
        }
        if (getConfiguration().getBoolean("output.mortality.perSpecies.perSize.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                indicators.add(new MortalitySpeciesOutput(rank, "output.mortality.perSpecies.perSize.enabled", getSpecies(i), AbstractSpectrumOutput.Type.SIZE));
            }
        }
        // Yield
        indicators.add(new YieldOutput(rank, "output.yield.biomass.enabled"));
        indicators.add(new YieldNOutput(rank, "output.yield.abundance.enabled"));
        // Size
        indicators.add(new MeanSizeOutput(rank, "output.size.enabled"));
        indicators.add(new MeanSizeCatchOutput(rank, "output.size.catch.enabled"));
        indicators.add(new SizeSpectrumOutput(rank, "output.size.spectrum.enabled"));
        indicators.add(new SizeSpectrumSpeciesNOutput(rank, "output.size.spectrum.perSpecies.N.enabled"));
        indicators.add(new SizeSpectrumSpeciesYieldNOutput(rank, "output.size.spectrum.perSpecies.N.enabled"));
        indicators.add(new SizeSpectrumSpeciesBOutput(rank, "output.size.spectrum.perSpecies.B.enabled"));
        indicators.add(new SizeSpectrumSpeciesYieldOutput(rank, "output.size.spectrum.perSpecies.B.enabled"));
        indicators.add(new MeanSizeSpeciesOutput(rank, "output.size.perSpecies.enabled"));
        // Age
        if (getConfiguration().getBoolean("output.age.spectrum.perSpecies.N.enabled")) {
            indicators.add(new AgeSpectrumSpeciesNOutput(rank, "output.age.spectrum.perSpecies.N.enabled"));
            indicators.add(new AgeSpectrumSpeciesYieldNOutput(rank, "output.age.spectrum.perSpecies.N.enabled"));
        }
        if (getConfiguration().getBoolean("output.age.spectrum.perSpecies.B.enabled")) {
            indicators.add(new AgeSpectrumSpeciesBOutput(rank, "output.age.spectrum.perSpecies.B.enabled"));
            indicators.add(new AgeSpectrumSpeciesYieldOutput(rank, "output.age.spectrum.perSpecies.B.enabled"));
        }
        // TL
        indicators.add(new MeanTrophicLevelOutput(rank, "output.tl.enabled"));
        indicators.add(new MeanTrophicLevelCatchOutput(rank, "output.tl.catch.enabled"));
        indicators.add(new TrophicLevelSpectrumOutput(rank, "output.tl.spectrum.enabled"));
        indicators.add(new MeanTrophicLevelSizeOutput(rank, "output.tl.perSize.enabled"));
        indicators.add(new MeanTrophicLevelAgeOutput(rank, "output.tl.perAge.enabled"));
        // Predation
        indicators.add(new DietOutput(rank, "output.diet.composition.enabled"));
        if (getConfiguration().getBoolean("output.diet.composition.perSpecies.perAge.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                indicators.add(new DietSpeciesOutput(rank, "output.diet.composition.perSpecies.perAge.enabled", getSpecies(i), AbstractSpectrumOutput.Type.AGE));
            }
        }
        if (getConfiguration().getBoolean("output.diet.composition.perSpecies.perSize.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                indicators.add(new DietSpeciesOutput(rank, "output.diet.composition.perSpecies.perSize.enabled", getSpecies(i), AbstractSpectrumOutput.Type.SIZE));
            }
        }
        indicators.add(new PredatorPressureOutput(rank, "output.diet.pressure.enabled"));
        indicators.add(new BiomassDietStageOutput(rank, "output.diet.pressure.enabled"));
        if (getConfiguration().getBoolean("output.diet.pressure.perSpecies.perAge.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                indicators.add(new PredatorPressureSpeciesOutput(rank, "output.diet.pressure.perSpecies.perAge.enabled", getSpecies(i), AbstractSpectrumOutput.Type.AGE));
            }
        }
        if (getConfiguration().getBoolean("output.diet.pressure.perSpecies.perSize.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                indicators.add(new PredatorPressureSpeciesOutput(rank, "output.diet.pressure.perSpecies.perSize.enabled", getSpecies(i), AbstractSpectrumOutput.Type.SIZE));
            }
        }
        // Spatialized
        indicators.add(new SpatialOutput(rank, "output.spatial.enabled"));
        indicators.add(new LTLOutput(rank, "output.spatial.ltl.enabled"));

        /*
         * Initialize indicators
         */
        for (IOutput indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.init();
                indicator.reset();
            }
        }
    }

    public void close() {
        for (IOutput indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.close();
            }
        }
    }

    public void initStep() {
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            for (IOutput indicator : indicators) {
                if (indicator.isEnabled()) {
                    indicator.initStep();
                }
            }
        }
    }

    public void update(int iStepSimu) {

        // UPDATE
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            for (IOutput indicator : indicators) {
                if (indicator.isEnabled()) {
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
    }
}
