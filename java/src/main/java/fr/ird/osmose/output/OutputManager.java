/*
 *
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 *
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 *
 * Osmose is a computer program whose purpose is to simulate fish
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
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package fr.ird.osmose.output;

import fr.ird.osmose.output.spatial.SpatialSizeSpeciesOutput;
import fr.ird.osmose.output.spatial.SpatialOutput;
import fr.ird.osmose.output.netcdf.AbundanceDistribOutput_Netcdf;
import fr.ird.osmose.output.netcdf.BiomassDietStageOutput_Netcdf;
import fr.ird.osmose.output.netcdf.BiomassOutput_Netcdf;
import fr.ird.osmose.output.netcdf.BiomassDistribOutput_Netcdf;
import fr.ird.osmose.output.netcdf.MeanSizeDistribOutput_Netcdf;
import fr.ird.osmose.output.netcdf.MortalitySpeciesOutput_Netcdf;
import fr.ird.osmose.output.netcdf.DietDistribOutput_Netcdf;
import fr.ird.osmose.output.netcdf.YieldNDistribOutput_Netcdf;
import fr.ird.osmose.output.netcdf.DietOutput_Netcdf;
import fr.ird.osmose.output.netcdf.PredatorPressureOutput_Netcdf;
import fr.ird.osmose.output.netcdf.AbundanceOutput_Netcdf;
import fr.ird.osmose.output.netcdf.YieldDistribOutput_Netcdf;
import fr.ird.osmose.output.netcdf.YieldNOutput_Netcdf;
import fr.ird.osmose.output.netcdf.YieldOutput_Netcdf;
import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.output.distribution.AgeDistribution;
import fr.ird.osmose.output.distribution.SizeDistribution;
import fr.ird.osmose.output.distribution.WeightDistribution;
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
import fr.ird.osmose.output.spatial.SpatialEnetOutput;
import fr.ird.osmose.output.spatial.SpatialEnetOutputlarvae;
import fr.ird.osmose.output.spatial.SpatialEnetOutputjuv;
import fr.ird.osmose.output.spatial.SpatialdGOutput;
import fr.ird.osmose.output.spatial.SpatialEggOutput;
import fr.ird.osmose.output.spatial.SpatialMortaPredOutput;
import fr.ird.osmose.output.spatial.SpatialMortaStarvOutput;
import fr.ird.osmose.process.mortality.MortalityCause;

/**
 *
 * @author pverley
 */
public class OutputManager extends SimulationLinker {

    // List of the indicators
    final private List<IOutput> outputs;

    /**
     * Whether first age class is discarded or not from output.
     */
    private boolean cutoff;
    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators when
     * parameter <i>output.cutoff.enabled</i> is set to {@code true}. Parameter
     * <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;

    private final static boolean NO_WARNING = false;

    public OutputManager(int rank) {
        super(rank);
        outputs = new ArrayList<>();
    }

    public void init() {

        int rank = getRank();
        int ndtPerYear = this.getConfiguration().getNStepYear();
        /*
         * Delete existing outputs from previous simulation
         */
        if (!getSimulation().isRestart()) {
            // Delete previous simulation of the same name
            String pattern = getConfiguration().getString("output.file.prefix") + "*_Simu" + rank + "*";
            IOTools.deleteRecursively(getConfiguration().getOutputPathname(), pattern);
        }

        AbstractDistribution sizeDistrib = new SizeDistribution();
        sizeDistrib.init();
        AbstractDistribution ageDistrib = new AgeDistribution();
        ageDistrib.init();
        AbstractDistribution weightDistrib = new WeightDistribution();
        weightDistrib.init();
        AbstractDistribution tl_distrib = new TLDistribution();
        tl_distrib.init();

        cutoff = getConfiguration().getBoolean("output.cutoff.enabled");
        cutoffAge = new float[getNSpecies()];
        int cpt = 0;
        if (cutoff) {
            for (int iSpec : this.getFocalIndex()) {
                cutoffAge[cpt++] = getConfiguration().getFloat("output.cutoff.age.sp" + iSpec);
            }
        }

        if (getConfiguration().getBoolean("output.abundance.netcdf.enabled")) {
            outputs.add(new AbundanceOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.biomass.netcdf.enabled")) {
            outputs.add(new BiomassOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.yield.biomass.netcdf.enabled")) {
            outputs.add(new YieldOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.yield.abundance.netcdf.enabled")) {
            outputs.add(new YieldNOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.biomass.bysize.netcdf.enabled")) {
            outputs.add(new BiomassDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.biomass.bytl.netcdf.enabled")) {
            outputs.add(new BiomassDistribOutput_Netcdf(rank, tl_distrib));
        }

        if (getConfiguration().getBoolean("output.biomass.byage.netcdf.enabled")) {
            outputs.add(new BiomassDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.bysize.netcdf.enabled")) {
            outputs.add(new AbundanceDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.byage.netcdf.enabled")) {
            outputs.add(new AbundanceDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.diet.pressure.netcdf.enabled")) {
            outputs.add(new BiomassDietStageOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.diet.composition.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new DietOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.mortality.perSpecies.byage.netcdf.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new MortalitySpeciesOutput_Netcdf(rank, getSpecies(i), ageDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.composition.byage.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput_Netcdf(rank, getSpecies(i), ageDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.composition.bysize.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput_Netcdf(rank, getSpecies(i), sizeDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.yield.biomass.bySize.netcdf.enabled")) {
            outputs.add(new YieldDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.yield.biomass.byage.netcdf.enabled")) {
            outputs.add(new YieldDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.yield.abundance.bySize.netcdf.enabled")) {
            outputs.add(new YieldNDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.yield.abundance.byage.netcdf.enabled")) {
            outputs.add(new YieldNDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.meanSize.byAge.netcdf.enabled")) {
            outputs.add(new MeanSizeDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.diet.pressure.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new PredatorPressureOutput_Netcdf(rank));
        }

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

        if (getConfiguration().getBoolean("output.spatialenet.enabled")) {
            outputs.add(new SpatialEnetOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialenetlarvae.enabled")) {
            outputs.add(new SpatialEnetOutputlarvae(rank));
        }

        if (getConfiguration().getBoolean("output.spatialenetjuv.enabled")) {
            outputs.add(new SpatialEnetOutputjuv(rank));
        }

        if (getConfiguration().getBoolean("output.spatialMstarv.enabled")) {
            outputs.add(new SpatialMortaStarvOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialMpred.enabled")) {
            outputs.add(new SpatialMortaPredOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialdg.enabled")) {
            outputs.add(new SpatialdGOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialegg.enabled")) {
            outputs.add(new SpatialEggOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatial.yield.biomass.enabled")) {
            outputs.add(new SpatialYieldOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatial.yield.abundance.enabled")) {
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

        if(getConfiguration().getBoolean("output.fishing.accessible.biomass")){
            outputs.add(new FishingAccessBiomassOutput(rank));
        }

        if(getConfiguration().getBoolean("output.fishing.harvested.biomass")){
            outputs.add(new FishingHarvestedBiomassDistribOutput(rank));
        }

        // Fisheries output
        if (getConfiguration().isFisheryEnabled() && getConfiguration().getBoolean("output.fishery.enabled")) {
            outputs.add(new FisheryOutput(rank));
        }
        // Biomass
        if (getConfiguration().getBoolean("output.biomass.enabled")) {
            outputs.add(new SpeciesOutput(rank, null, "biomass",
                    "Mean biomass (tons), " + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    (school) -> school.getInstantaneousBiomass()));
        }
        if (getConfiguration().getBoolean("output.biomass.bysize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "biomass", "Distribution of fish species biomass (tonne)",
                    school -> school.getInstantaneousBiomass(), sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.biomass.byage.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "biomass", "Distribution of fish species biomass (tonne)",
                    school -> school.getInstantaneousBiomass(), ageDistrib));
        }

        // Abundance
        if (getConfiguration().getBoolean("output.abundance.enabled")) {
            outputs.add(
                    new SpeciesOutput(rank, null, "abundance",
                            "Mean abundance (number of fish), " + (cutoff ? "excluding" : "including")
                                    + " first ages specified in input",
                            (school) -> school.getInstantaneousAbundance()));
        }

        if (getConfiguration().getBoolean("output.abundance.age1.enabled")) {
            outputs.add(new AbundanceOutput_age1(rank, "Bioen", "AbundAge1"));
        }

        if (getConfiguration().getBoolean("output.meanWeight.byAge.enabled")) {
            outputs.add(new WeightedDistribOutput(rank, "Indicators", "meanWeight", "Mean Weight of fish (kg)",
                    school -> (school.getWeight() * 1e3), school -> school.getInstantaneousAbundance(), ageDistrib));
        }

        if (getConfiguration().getBoolean("output.meanWeight.bySize.enabled")) {
            outputs.add(new WeightedDistribOutput(rank, "Indicators", "meanWeight", "Mean Weight of fish (kg)",
                    school -> (school.getWeight() * 1e3), school -> school.getInstantaneousAbundance(), sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.meanWeight.byWeight.enabled")) {
            outputs.add(new WeightedDistribOutput(rank, "Indicators", "meanWeight", "Mean Weight of fish (kg)",
                    school -> (school.getWeight() * 1e3), school -> school.getInstantaneousAbundance(), weightDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.bysize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "abundance",
                    "Distribution of fish abundance (number of fish)", school -> school.getInstantaneousAbundance(),
                    sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.byweight.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "abundance",
                    "Distribution of fish abundance (number of fish)", school -> school.getInstantaneousAbundance(),
                    weightDistrib));
        }

        if (getConfiguration().getBoolean("output.biomass.byweight.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "biomass",
                    "Distribution of fish biomass (tons)", school -> school.getInstantaneousBiomass(),
                    weightDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.byage.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "abundance",
                    "Distribution of fish abundance (number of fish)", school -> school.getInstantaneousAbundance(),
                    ageDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.bytl.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "abundance",
                    "Distribution of fish abundance (number of fish)", school -> school.getInstantaneousAbundance(),
                    tl_distrib));
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

        if (getConfiguration().getBoolean("output.mortality.additional.bySize.enabled")
                || getConfiguration().getBoolean("output.mortality.additional.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortality",
                    "Distribution of additional mortality biomass (tonne of fish dead from unexplicited cause per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.ADDITIONAL)), sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.mortality.additional.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortality",
                    "Distribution of additional mortality biomass (tonne of fish dead from unexplicited cause per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.ADDITIONAL)), ageDistrib));
        }

        if (getConfiguration().getBoolean("output.mortality.additionalN.bySize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortalityN",
                    "Distribution of additional mortality biomass (number of fish dead from unexplicited cause per time step of saving)",
                    school -> school.getNdead(MortalityCause.ADDITIONAL), sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.mortality.additionalN.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortalityN",
                    "Distribution of additional mortality biomass (number of fish dead from unexplicited cause per time step of saving)",
                    school -> school.getNdead(MortalityCause.ADDITIONAL), ageDistrib));
        }

        // Yield
        if (getConfiguration().getBoolean("output.yield.biomass.enabled")) {
            outputs.add(new SpeciesOutput(rank, null, "yield",
                    "cumulative catch (tons per time step of saving). ex: if time step of saving is the year, then annual catches are saved",
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING)), false));
        }

        if (getConfiguration().getBoolean("output.yield.abundance.enabled")) {
            outputs.add(new SpeciesOutput(rank, null, "yieldN",
                    "cumulative catch (number of fish caught per time step of saving). ex: if time step of saving is the year, then annual catches in fish numbers are saved",
                    school -> school.getNdead(MortalityCause.FISHING), false));
        }

        // Size
        if (getConfiguration().getBoolean("output.size.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "SizeIndicators", "meanSize",
                    "Mean size of fish species in cm, weighted by fish numbers, and "
                            + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    school -> school.getAge() >= cutoffAge[school.getSpeciesIndex()], school -> school.getLength(),
                    school -> school.getInstantaneousAbundance()));
        }

        // Size
        if (getConfiguration().getBoolean("output.weight.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "SizeIndicators", "meanWeight",
                    "Mean weight of fish species in kilogram, weighted by fish numbers, and "
                            + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    school -> school.getAge() >= cutoffAge[school.getSpeciesIndex()],
                    school -> 1E3 * school.getWeight(), school -> school.getInstantaneousAbundance()));
        }

        if (getConfiguration().getBoolean("output.size.catch.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "SizeIndicators", "meanSizeCatch",
                    "Mean size of fish species in cm, weighted by fish numbers in the catches, and including first ages specified in input.",
                    school -> school.getLength(), school -> school.getNdead(MortalityCause.FISHING)));
        }

        if (getConfiguration().getBoolean("output.yield.abundance.bySize.enabled")) {
            outputs.add(new DistribOutput(rank, "SizeIndicators", "yieldN",
                    "Distribution of cumulative catch (number of fish per time step of saving)",
                    school -> school.getNdead(MortalityCause.FISHING), sizeDistrib, false));
        }

        if (getConfiguration().getBoolean("output.yield.biomass.bySize.enabled")) {
            outputs.add(new DistribOutput(rank, "SizeIndicators", "yield",
                    "Distribution of cumulative catch (tonne per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING)), sizeDistrib, false));
        }

        if (getConfiguration().getBoolean("output.yield.abundance.byWeight.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "yieldN",
                    "Distribution of cumulative catch (number of fish per time step of saving)",
                    school -> school.getNdead(MortalityCause.FISHING), weightDistrib, false));
        }

        if (getConfiguration().getBoolean("output.yield.biomass.byWeight.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "yield",
                    "Distribution of cumulative catch (tonne per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING)), weightDistrib, false));
        }

        if (getConfiguration().getBoolean("output.meanSize.byAge.enabled")) {
            outputs.add(new WeightedDistribOutput(rank, "AgeIndicators", "meanSize", "Mean size of fish (centimeter)",
                    school -> school.getLength(), school -> school.getInstantaneousAbundance(), ageDistrib));
        }

        // Age
        if (getConfiguration().getBoolean("output.yield.abundance.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "AgeIndicators", "yieldN",
                    "Distribution of cumulative catch (number of fish per time step of saving)",
                    school -> school.getNdead(MortalityCause.FISHING), ageDistrib, false));
        }

        if (getConfiguration().getBoolean("output.yield.biomass.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "AgeIndicators", "yield",
                    "Distribution of cumulative catch (tonne per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING)), ageDistrib, false));
        }

        // TL
        if (getConfiguration().getBoolean("output.tl.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedSpeciesOutput(rank, "Trophic", "meanTL",
                    "Mean Trophic Level of fish species, weighted by fish biomass, and "
                            + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    school -> school.getAge() >= cutoffAge[school.getSpeciesIndex()],
                    school -> school.getTrophicLevel(), school -> school.getInstantaneousBiomass()));
        }

        if (getConfiguration().getBoolean("output.tl.catch.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedSpeciesOutput(rank, "Trophic", "meanTLCatch",
                    "Mean Trophic Level of fish species, weighted by fish catch, and including first ages specified in input",
                    school -> school.getTrophicLevel(),
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING))));
        }

        if (getConfiguration().getBoolean("output.biomass.bytl.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "biomass", "Distribution of fish biomass (tonne)",
                    school -> school.getInstantaneousBiomass(), tl_distrib));
        }

        if (getConfiguration().getBoolean("output.meanTL.bySize.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedDistribOutput(rank, "Trophic", "meanTL", "Mean trophic level of fish species",
                    school -> school.getTrophicLevel(), school -> school.getInstantaneousBiomass(), sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.meanTL.byAge.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedDistribOutput(rank, "Trophic", "meanTL", "Mean trophic level of fish species",
                    school -> school.getTrophicLevel(), school -> school.getInstantaneousBiomass(), ageDistrib));
        }

        // Predation
        if (getConfiguration().getBoolean("output.diet.composition.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new DietOutput(rank));
        }

        if (getConfiguration().getBoolean("output.diet.composition.byage.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput(rank, getSpecies(i), ageDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.composition.bysize.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput(rank, getSpecies(i), sizeDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.pressure.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new PredatorPressureOutput(rank));
        }

        if (getConfiguration().getBoolean("output.diet.pressure.enabled")) {
            outputs.add(new BiomassDietStageOutput(rank));
        }

        if (getConfiguration().getBoolean("output.diet.pressure.byage.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new PredatorPressureDistribOutput(rank, getSpecies(i), ageDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.pressure.bysize.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new PredatorPressureDistribOutput(rank, getSpecies(i), sizeDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.success.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "Trophic", "predationSuccess",
                    "Predation success rate per species", school -> school.getPredSuccessRate(), nschool -> 1.d));
        }

        // Adding saving of age at death.
        if (getConfiguration().getBoolean("output.age.at.death.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new AgeAtDeathOutput(rank, getSpecies(i)));
            }
        }

        // Spatialized
        if (getConfiguration().getBoolean("output.spatial.enabled")) {
            outputs.add(new SpatialOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatial.ltl.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new ResourceOutput(rank));
        }

        // Debugging outputs
        if (getConfiguration().getBoolean("output.ssb.enabled", NO_WARNING)) {
            outputs.add(new SpeciesOutput(rank, null, "SSB", "Spawning Stock Biomass (tonne)",
                    school -> school.isSexuallyMature() ? school.getInstantaneousBiomass() : 0.d));
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

        if (getConfiguration().getBoolean("output.individual.enabled", NO_WARNING)) {
            ModularSchoolSetSnapshot modOutput = new ModularSchoolSetSnapshot(rank);
            outputs.add(modOutput);
        }

        if (getConfiguration().isBioenEnabled()) {

            if (getConfiguration().getBoolean("output.bioen.mature.size.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "sizeMature", "Size at maturity (centimeter)",
                        school -> school.isMature(), school -> school.getSizeMat(),
                        school -> school.getInstantaneousAbundance()));
            }

            if (getConfiguration().getBoolean("output.bioen.mature.age.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "ageMature", "Age at maturity (year)",
                        school -> school.isMature(), school -> school.getAgeMat(),
                        school -> school.getInstantaneousAbundance()));
            }

            // Correct the output of bioen ingestion
            if (getConfiguration().getBoolean("output.bioen.ingest.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "ingestion", "Ingestion rate (grams.grams^-alpha)",
                        (school -> (school.getAgeDt() >= school.getSpecies().getFirstFeedingAgeDt())),
                        school -> school.getIngestion() * 1e6f / (Math.pow(school.getWeight() * 1e6f, school.getBetaBioen())),
                        school -> school.getInstantaneousAbundance()));
            }

            if (getConfiguration().getBoolean("output.bioen.ingest.tot.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "ingestionTot", "Cumulated ingestion (grams)",
                        school -> school.getIngestionTot() * 1e6f, school -> school.getInstantaneousAbundance()));
            }

            if (getConfiguration().getBoolean("output.bioen.maint.enabled", NO_WARNING)) {
                outputs.add(
                        new WeightedSpeciesOutput(rank, "Bioen", "maintenance", "Maintenance rate (grams.grams^-alpha)",
                                school -> (school.getEMaint() / school.getInstantaneousAbundance() * 1e6f
                                        / (Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()))),
                                nshool -> 1.d));
            }

            if (getConfiguration().getBoolean("output.bioen.enet.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "meanEnet",
                        "Mean energy net rate (grams.grams^-beta) (grams net usable per gram of predator)",
                        school -> school.getENet() / school.getInstantaneousAbundance() * 1e6f
                                / (Math.pow(school.getWeight() * 1e6f, school.getBetaBioen())),
                        nshool -> 1.d));
            }

            if (getConfiguration().getBoolean("output.bioen.sizeInf.enabled", NO_WARNING)) {
                outputs.add(new BioenSizeInfOutput(rank));
            }

            if (getConfiguration().getBoolean("output.bioen.kappa.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "kappa", "Kappa (rate [0-1])",
                        school -> school.getKappa(), school -> school.getInstantaneousAbundance()));
            }

            // Alaia's outputs in the new format
            if (getConfiguration().getBoolean("output.ingest.byAge.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanIngestDistribBy",
                        "Mean ingestion per g.g^-beta.y-1 of fish (centimeter)",
                        school -> (school.getIngestion() * 1e6f
                                / Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * ndtPerYear),
                        school -> school.getInstantaneousAbundance(), ageDistrib, false));
            }

            // Alaia's outputs in the new format
            if (getConfiguration().getBoolean("output.ingest.bySize.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanIngestDistribBy",
                        "Mean ingestion per g.g^-beta.y-1 of fish (centimeter)",
                        school -> (school.getIngestion() * 1e6f
                                / Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * ndtPerYear),
                        school -> school.getInstantaneousAbundance(), sizeDistrib, false));
            }

            // Alaia's outputs in the new format
            if (getConfiguration().getBoolean("output.kappa.byAge.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanKappaDistribBy",
                        "Mean kappa of fish", school -> (school.getKappa()),
                        school -> school.getInstantaneousAbundance(), ageDistrib));
            }

            // Alaia's outputs in the new format
            if (getConfiguration().getBoolean("output.kappa.bySize.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanKappa",
                        "Mean kappa of fish", school -> (school.getKappa()),
                        school -> school.getInstantaneousAbundance(), sizeDistrib));
            }

            // Alaia's outputs in the new format
            if (getConfiguration().getBoolean("output.enet.byAge.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanEnet",
                        "Mean Enet per g.g^-beta.y-1 of fish", school -> ((school.getENet() * 1e6f
                                / Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * ndtPerYear)),
                        school -> school.getInstantaneousAbundance(), ageDistrib, false));
            }

            // Alaia's outputs in the new format
            if (getConfiguration().getBoolean("output.enet.bySize.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanEnet",
                        "Mean Enet per g.g^-beta.y-1 of fish", school -> ((school.getENet() * 1e6f
                                / Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * ndtPerYear)),
                        school -> school.getInstantaneousAbundance(), sizeDistrib, false));
            }

            // Alaia's outputs in the new format
            if (getConfiguration().getBoolean("output.maintenance.byAge.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanMaint",
                        "Mean maintenance per g.g^-beta.y-1 of fish",
                        school -> ((school.getEMaint() * 1e6f
                                / Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * ndtPerYear)),
                        school -> school.getInstantaneousAbundance(), ageDistrib, false));
            }

            if (getConfiguration().getBoolean("output.maintenance.bySize.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanMaint",
                        "Mean maintenance per g.g^-beta.y-1 of fish",
                        school -> ((school.getEMaint() * 1e6f
                                / Math.pow(school.getWeight() * 1e6f, school.getBetaBioen()) * ndtPerYear)),
                        school -> school.getInstantaneousAbundance(), sizeDistrib, false));
            }

            if (getConfiguration().getBoolean("output.meanSomaticWeight.byAge.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanSomaticWeight",
                        "Mean somatic weight of fish (centimeter)",
                        school -> (school.getWeight()),
                        school -> school.getInstantaneousAbundance(), ageDistrib)
                );
            }

            if (getConfiguration().getBoolean("output.meanGonadWeight.byAge.enabled")) {
                outputs.add(new WeightedDistribOutput(rank, "BioenIndicators", "meanGonadWeight",
                        "Mean gonad weight of fish per gram of individual (centimeter) by ",
                        school -> (school.getGonadWeight() / (school.getWeight())),
                        school -> school.getInstantaneousAbundance(), ageDistrib)
                );
            }

        }

        // warning: simulation init is called after output init.
        // List<String> genet_keys = this.getConfiguration().findKeys("*.trait.mean");
        if (this.getConfiguration().isGeneticEnabled()) {
            if (getConfiguration().getBoolean("output.evolvingtraits.enabled")) {
                outputs.add(new VariableTraitOutput(rank));
            }
        }

        /*
         * Initialize indicators
         */
        outputs.forEach(indicator -> {
            indicator.init();
            indicator.reset();
        });

    }

    public void close() {
        outputs.forEach(IOutput::close);
    }

    public void initStep() {
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            outputs.forEach(IOutput::initStep);
        }
    }

    public void update(int iStepSimu) {

        // UPDATE
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            outputs.forEach(indicator -> {
                indicator.update();
                // WRITE
                if (indicator.isTimeToWrite(iStepSimu)) {
                    float time = (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
                    indicator.write(time);
                    indicator.reset();
                }
            });
        }
    }
}
