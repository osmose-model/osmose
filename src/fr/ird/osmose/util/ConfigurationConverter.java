package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.Cell;
import fr.ird.osmose.process.MovementProcess.SpatialDistribution;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class ConfigurationConverter {

    /**
     * Filename of the INPUT.txt file from osmose v2. Use slash '/' as separator
     * char. Example: outputPath =
     * "/home/philippe/osmose/benguela/ben_v2/INPUT.txt" outputPath =
     * "C:/user/philippe/Mes documents/osmose/benguela/ben_v2/INPUT.txt"
     */
    final public String inputFile = "C:\\Users\\philippe\\Documents\\osmose\\dev\\config\\osm\\osm_v3\\osm_old_input\\INPUT.txt";
    /**
     * Path of the folder for saving the new format of input Use slash '/' as
     * separator char. Example: outputPath =
     * "/home/philippe/osmose/benguela/ben_v3" outputPath =
     * "C:/user/philippe/Mes documents/osmose/benguela/ben_v3"
     */
    final public String outputPath = "C:\\Users\\philippe\\Documents\\osmose\\dev\\config\\osm\\osm_v3";
    //
    private OldConfiguration cfg;
    private Properties prop;
    
    ConfigurationConverter(String[] args) {

        // Get old configuration
        String inputPathName = new File(inputFile).getParentFile().getAbsolutePath();
        String inputTxtName = new File(inputFile).getName();
        String fileSeparator = System.getProperty("file.separator");
        String outputPathName = inputPathName + fileSeparator + "output" + fileSeparator;
        cfg = new OldConfiguration(inputPathName, outputPathName, inputTxtName);
        cfg.readParameters();

        // Create new Properties
        prop = new Properties();

        // convert
        System.out.println("Converting old configuration file to new format...");
        convert();
        //writeAsText();
        writeAsCSV();
        //writeAsXML();
        System.out.println("Conversion completed successfully.");
    }

    private void convert() {

        new File(resolveFile(getFilename("csv"))).getParentFile().mkdirs();

        int nSpecies = cfg.getNSpecies();
        int nPlankton = cfg.getNPlankton();
        // OUTPUT
        prop.setProperty("output.dir.path", resolveFile("output/"));
        prop.setProperty("output.file.prefix", cfg.getOutputPrefix());
        prop.setProperty("output.recordfrequency.ndt", String.valueOf(cfg.getRecordFrequency()));
        prop.setProperty("output.start.year", String.valueOf(cfg.yearStartSaving));
        prop.setProperty("output.cutoff.enabled", String.valueOf(!cfg.outputClass0));
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("output.cutoff.age.sp" + i, String.valueOf(cfg.supAgeOfClass0Matrix[i]));
        }

        // INDICATORS
        prop.setProperty("output.abundance.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.biomass.enabled", String.valueOf(true));
        prop.setProperty("output.yield.biomass.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.yield.abundance.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.mortality.enabled", String.valueOf(!cfg.outputCalibration));

        prop.setProperty("output.diet.composition.enabled", String.valueOf(cfg.outputDiet));
        prop.setProperty("output.diet.pressure.enabled", String.valueOf(cfg.outputDiet));
        prop.setProperty("output.diet.stage.structure", String.valueOf(cfg.getDietOutputMetrics()));
        for (int i = 0; i < nSpecies; i++) {
            String key = "output.diet.stage.threshold.sp" + i;
            prop.setProperty(key, String.valueOf(toString(cfg.dietStageThreshold[i])));
        }
        prop.setProperty("output.TL.catch.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.TL.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.TL.spectrum.enabled", String.valueOf(cfg.outputTLSpectrum));
        prop.setProperty("output.TL.perSize.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.TL.perAge.enabled", String.valueOf(cfg.outputTL));

        prop.setProperty("output.spatial.ltl.enabled", String.valueOf(cfg.outputPlanktonBiomass));
        prop.setProperty("output.spatial.enabled", String.valueOf(cfg.outputSpatialized));

        prop.setProperty("output.size.catch.enabled", String.valueOf(cfg.outputMeanSize));
        prop.setProperty("output.size.enabled", String.valueOf(cfg.outputMeanSize));
        prop.setProperty("output.size.perSpecies.enabled", String.valueOf(cfg.outputMeanSize));
//        prop.setProperty("output.size.spectrum.unit", "cm");
        prop.setProperty("output.size.spectrum.enabled", String.valueOf(cfg.outputSizeSpectrum));
        prop.setProperty("output.size.spectrum.perSpecies.N.enabled", String.valueOf(cfg.outputSizeSpectrumSpecies));
        prop.setProperty("output.size.spectrum.perSpecies.B.enabled", String.valueOf(cfg.outputSizeSpectrumSpecies));
        prop.setProperty("output.size.spectrum.size.min", String.valueOf(cfg.getSpectrumMinSize()));
        prop.setProperty("output.size.spectrum.size.max", String.valueOf(cfg.getSpectrumMaxSize()));
        prop.setProperty("output.size.spectrum.size.range", String.valueOf(cfg.getSpectrumClassRange()));

        // SIMULATION
        prop.setProperty("simulation.time.ndtPerYear", String.valueOf(cfg.getNumberTimeStepsPerYear()));
        prop.setProperty("simulation.time.nyear", String.valueOf(cfg.getNYear()));
        prop.setProperty("simulation.nsimulation", String.valueOf(cfg.getNSimulation()));
        prop.setProperty("simulation.nschool", String.valueOf(cfg.nSchool));
        prop.setProperty("simulation.ncpu", String.valueOf(Runtime.getRuntime().availableProcessors()));
        prop.setProperty("simulation.nspecies", String.valueOf(cfg.getNSpecies()));
        prop.setProperty("simulation.nplankton", String.valueOf(cfg.getNPlankton()));
        prop.setProperty("simulation.restart.recordfrequency.ndt", "null");
        prop.setProperty("simulation.restart.file", "null");

        // SPECIES
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("species.name.sp" + i, String.valueOf(cfg.speciesName[i]));
            prop.setProperty("species.lifespan.sp" + i, String.valueOf(cfg.speciesLifespan[i]));
            prop.setProperty("species.lInf.sp" + i, String.valueOf(cfg.lInf[i]));
            prop.setProperty("species.K.sp" + i, String.valueOf(cfg.K[i]));
            prop.setProperty("species.t0.sp" + i, String.valueOf(cfg.t0[i]));
            prop.setProperty("species.length2weight.condition.factor.sp" + i, String.valueOf(cfg.c[i]));
            prop.setProperty("species.length2weight.allometric.power.sp" + i, String.valueOf(cfg.bPower[i]));
            prop.setProperty("species.egg.size.sp" + i, String.valueOf(cfg.eggSize[i]));
            if (cfg.ageMaturity[i] >= 0) {
                prop.setProperty("species.maturity.age.sp" + i, String.valueOf(cfg.ageMaturity[i]));
            } else {
                prop.setProperty("species.maturity.size.sp" + i, String.valueOf(cfg.sizeMaturity[i]));
            }
            prop.setProperty("species.egg.weight.sp" + i, String.valueOf(cfg.eggWeight[i]));
            prop.setProperty("species.vonbertalanffy.threshold.age.sp" + i, String.valueOf(cfg.growthAgeThreshold[i]));
            prop.setProperty("species.sexratio.sp" + i, String.valueOf(cfg.sexRatio[i]));
            prop.setProperty("species.relativefecundity.sp" + i, String.valueOf(cfg.alpha[i]));
        }

        // PLANKTON
        for (int i = 0; i < nPlankton; i++) {
            prop.setProperty("plankton.name.plk" + i, String.valueOf(cfg.planktonName[i]));
            prop.setProperty("plankton.TL.plk" + i, String.valueOf(cfg.ltlTrophicLevel[i]));
//            prop.setProperty("plankton.size.unit", "cm");
            prop.setProperty("plankton.size.min.plk" + i, String.valueOf(cfg.ltlMinSize[i]));
            prop.setProperty("plankton.size.max.plk" + i, String.valueOf(cfg.ltlMaxSize[i]));
            if (cfg.ltlTotalBiomass[i] >= 0) {
                prop.setProperty("plankton.biomass.total.plk" + i, String.valueOf(cfg.ltlTotalBiomass[i]));
            } else {
                prop.setProperty("plankton.conversion2tons.plk" + i, String.valueOf(cfg.ltlConversionFactor[i]));
            }
            prop.setProperty("plankton.accessibility2fish.plk" + i, String.valueOf(cfg.planktonAccessibility[i]));
        }

        // LTL MODEL
        String className = cfg.getLTLClassName();
        prop.setProperty("ltl.java.classname", String.valueOf(className));
        prop.setProperty("ltl.nstep", String.valueOf(cfg.getNumberLTLSteps()));
        if (className.equals("fr.ird.osmose.ltl.LTLFastForcingRomsPisces")
                || className.equals("fr.ird.osmose.ltl.LTLForcingRomsPisces")
                || className.equals("fr.ird.osmose.ltl.LTLForcingLaure")) {
            String[] files = cfg.planktonFileListNetcdf;
            for (int i = 0; i < files.length; i++) {
                prop.setProperty("ltl.netcdf.file.t" + i, cfg.resolveFile(files[i]));
            }
            prop.setProperty("ltl.integration.depth", String.valueOf(cfg.getIntegrationDepth()));
            String[] names = cfg.planktonNetcdfNames;
            for (int i = 0; i < nPlankton; i++) {
                prop.setProperty("ltl.netcdf.var.plankton.plk" + i, names[i]);
            }
            prop.setProperty("ltl.netcdf.grid.file", files[0]);
            prop.setProperty("ltl.netcdf.var.lat", cfg.strLat);
            prop.setProperty("ltl.netcdf.var.lon", cfg.strLon);
            prop.setProperty("ltl.netcdf.var.bathy", cfg.strH);
            prop.setProperty("ltl.netcdf.var.hc", cfg.strHC);
            prop.setProperty("ltl.netcdf.var.csr", cfg.strCs_r);
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcingECO3M")
                || className.equals("fr.ird.osmose.ltl.LTLForcingECO3M")) {
            String[] files = cfg.planktonFileListNetcdf;
            for (int i = 0; i < files.length; i++) {
                prop.setProperty("ltl.netcdf.file.t" + i, cfg.resolveFile(files[i]));
            }
            prop.setProperty("ltl.integration.depth", String.valueOf(cfg.getIntegrationDepth()));
            String[] names = cfg.planktonNetcdfNames;
            for (int i = 0; i < nPlankton; i++) {
                prop.setProperty("ltl.netcdf.var.plankton.plk" + i, names[i]);
            }
            prop.setProperty("ltl.netcdf.var.zlevel", cfg.zlevelName);
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcingBFM")
                || className.equals("fr.ird.osmose.ltl.LTLForcingBFM")) {
            String[] files = cfg.planktonFileListNetcdf;
            for (int i = 0; i < files.length; i++) {
                prop.setProperty("ltl.netcdf.file.t" + i, cfg.resolveFile(files[i]));
            }
            prop.setProperty("ltl.integration.depth", String.valueOf(cfg.getIntegrationDepth()));
            String[] names = cfg.planktonNetcdfNames;
            for (int i = 0; i < nPlankton; i++) {
                prop.setProperty("ltl.netcdf.var.plankton.plk" + i, names[i]);
            }
            prop.setProperty("ltl.netcdf.bathy.file", cfg.bathyFile);
            prop.setProperty("ltl.netcdf.var.bathy", cfg.bathyName);
            prop.setProperty("ltl.netcdf.var.zlevel", cfg.zlevelName);
            prop.setProperty("ltl.netcdf.dim.ntime", String.valueOf(cfg.timeDim));
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcing")) {
            prop.setProperty("ltl.netcdf.file", cfg.resolveFile(cfg.ltlForcingFilename));
        }

        // GRID
        prop.setProperty("grid.java.classname", String.valueOf(cfg.gridClassName));
        className = cfg.gridClassName;
        if (className.equals("fr.ird.osmose.grid.OriginalGrid")) {
            prop.setProperty("grid.upleft.lat", String.valueOf(cfg.upLeftLat));
            prop.setProperty("grid.upleft.lon", String.valueOf(cfg.upLeftLon));
            prop.setProperty("grid.lowright.lat", String.valueOf(cfg.lowRightLat));
            prop.setProperty("grid.lowright.lon", String.valueOf(cfg.lowRightLon));
            prop.setProperty("grid.nline", String.valueOf(cfg.nLine));
            prop.setProperty("grid.ncolumn", String.valueOf(cfg.nColumn));
            String filename = resolveFile("grid-mask.csv");
            writeMaskAsCSV(filename);
            prop.setProperty("grid.mask.file", filename);
        } else {
            prop.setProperty("grid.netcdf.file", String.valueOf(cfg.gridFileTab));
            prop.setProperty("grid.var.lat", String.valueOf(cfg.latField));
            prop.setProperty("grid.var.lon", String.valueOf(cfg.lonField));
            prop.setProperty("grid.var.mask", String.valueOf(cfg.maskField));
            prop.setProperty("grid.stride", String.valueOf(cfg.stride));
        }

        // MOVEMENT
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("movement.distribution.method.sp" + i, String.valueOf(cfg.spatialDistribution[i]));
            prop.setProperty("movement.randomwalk.range.sp" + i, String.valueOf(cfg.range[i]));
            if (cfg.spatialDistribution[i].equals(SpatialDistribution.RANDOM)) {
                prop.setProperty("movement.distribution.ncell.sp" + i, String.valueOf(cfg.randomAreaSize[i]));
            }
        }
        int nmap = cfg.maps.length;
        int indexMap = 0;
        for (int iMap = 0; iMap < nmap; iMap++) {
            if (null == cfg.mapFile[iMap] || cfg.mapFile[iMap].isEmpty()) {
                continue;
            }
            String map = "movement.map" + indexMap;
            String value;
            StringBuilder key = new StringBuilder(map);
            key.append(".species");
            value = cfg.speciesName[cfg.speciesMap[iMap]];
            prop.setProperty(key.toString(), value);
            key = new StringBuilder(map);
            key.append(".age.min");
            value = String.valueOf(cfg.agesMap[iMap][0]);
            prop.setProperty(key.toString(), value);
            key = new StringBuilder(map);
            key.append(".age.max");
            value = String.valueOf(cfg.agesMap[iMap][cfg.agesMap[iMap].length - 1] + 1);
            prop.setProperty(key.toString(), value);
            key = new StringBuilder(map);
            key.append(".season");
            value = toString(cfg.seasonMap[iMap]);
            prop.setProperty(key.toString(), value);
            key = new StringBuilder(map);
            key.append(".file");
            value = cfg.mapFile[iMap];
            prop.setProperty(key.toString(), value);
            if (cfg.spatialDistribution[cfg.speciesMap[iMap]].equals(SpatialDistribution.CONNECTIVITY)) {
                key = new StringBuilder(map);
                key.append(".connectivity.file");
                value = cfg.connectivityFile[iMap];
                prop.setProperty(key.toString(), value);
            }
            indexMap++;
        }

        // PREDATION
//        prop.setProperty("predation.ingestion.rate.max.unit", "grams of food per gram of fish and per year");
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("predation.predPrey.stage.threshold.sp" + i, String.valueOf(toString(cfg.feedingStageThreshold[i])));
            prop.setProperty("predation.predPrey.sizeRatio.min.sp" + i, String.valueOf(toString(cfg.predPreySizeRatioMin[i])));
            prop.setProperty("predation.predPrey.sizeRatio.max.sp" + i, String.valueOf(toString(cfg.predPreySizeRatioMax[i])));
            prop.setProperty("predation.ingestion.rate.max.sp" + i, String.valueOf(cfg.maxPredationRate[i]));
            prop.setProperty("predation.efficiency.critical.sp" + i, String.valueOf(cfg.criticalPredSuccess[i]));
            prop.setProperty("predation.accessibility.stage.structure", "age");
            prop.setProperty("predation.accessibility.stage.threshold.sp" + i, String.valueOf(toString(cfg.accessStageThreshold[i])));
        }
        String accessibilityFile = resolveFile("predation-accessibility.csv");
        prop.setProperty("predation.accessibility.file", accessibilityFile);
        writeAccessibilityAsCSV(accessibilityFile);

        // STARVATION
//        prop.setProperty("mortality.starvation.rate.max.unit", "year^-1");
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("mortality.starvation.rate.max.sp" + i, String.valueOf(cfg.starvMaxRate[i]));
        }

        // NATURAL MORTALITY
//        prop.setProperty("mortality.natural.larva.rate.unit", "dt^-1");
//        prop.setProperty("mortality.natural.rate.unit", "year^-1");
        if (null == cfg.larvalMortalityFile) {
            String larvalMortalityFile = resolveFile("larval-mortality-rates.csv");
            prop.setProperty("mortality.natural.larva.rate.file", larvalMortalityFile);
            writeLarvalMortalityRateAsCSV(larvalMortalityFile);
        } else {
            prop.setProperty("mortality.natural.larva.rate.file", cfg.resolveFile(cfg.larvalMortalityFile));
        }
        String naturalMortalityFile = resolveFile("natural-mortality-rates.csv");
        prop.setProperty("mortality.natural.rate.file", naturalMortalityFile);
        writeNaturalMortalityRateAsCSV(naturalMortalityFile);
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("mortality.natural.larva.rate.sp" + i, String.valueOf(cfg.larvalMortalityRates[i][0]));
            prop.setProperty("mortality.natural.rate.sp" + i, String.valueOf(cfg.D[i]));
        }

        // FISHING
        for (int i = 0; i < nSpecies; i++) {
            new File(resolveFile("fishing/")).mkdirs();
            String filename = "fishing/fishing-seasonality-byAge-" + cfg.speciesName[i] + ".csv";
            prop.setProperty("mortality.fishing.season.distrib.byAge.file.sp" + i, resolveFile(filename));

            filename = "fishing/fishing-seasonality-bySize-" + cfg.speciesName[i] + ".csv";
            prop.setProperty("mortality.fishing.season.distrib.bySize.file.sp" + i, resolveFile(filename));

            filename = "fishing/fishing-seasonality-" + cfg.speciesName[i] + ".csv";
            prop.setProperty("mortality.fishing.season.distrib.file.sp" + i, resolveFile(filename));

            filename = "fishing/F-rate-byAge-" + cfg.speciesName[i] + ".csv";
            prop.setProperty("mortality.fishing.rate.byAge.file.sp" + i, resolveFile(filename));
            prop.setProperty("mortality.fishing.rate.byAge.byYear.file.sp" + i, "null");

            filename = "fishing/F-rate-bySize-" + cfg.speciesName[i] + ".csv";
            prop.setProperty("mortality.fishing.rate.bySize.file.sp" + i, resolveFile(filename));
            prop.setProperty("mortality.fishing.rate.bySize.byYear.file.sp" + i, "null");

            filename = "fishing/F-rate-byYear" + cfg.speciesName[i] + ".csv";
            prop.setProperty("mortality.fishing.rate.byYear.file.sp" + i, resolveFile(filename));

            filename = "fishing/F-rate-byDt" + cfg.speciesName[i] + ".csv";
            prop.setProperty("mortality.fishing.rate.byDt.file.sp" + i, resolveFile(filename));
            prop.setProperty("mortality.fishing.rate.bySize.byDt.file.sp" + i, "null");
            prop.setProperty("mortality.fishing.rate.byAge.byDt.file.sp" + i, "null");

            writeFishingAsCSV(i);
            float F = sum(cfg.fishingRates[i]);
            prop.setProperty("mortality.fishing.rate.sp" + i, String.valueOf(F));
            prop.setProperty("mortality.fishing.recruitment.age.sp" + i, String.valueOf(cfg.recruitmentAge[i]));
            prop.setProperty("mortality.fishing.recruitment.size.sp" + i, "null");

            prop.setProperty("mortality.fishing.spatial.distrib.map" + i + ".file", "null");
            prop.setProperty("mortality.fishing.spatial.distrib.map" + i + ".species", cfg.speciesName[i]);
            prop.setProperty("mortality.fishing.spatial.distrib.map" + i + ".age.min", "0");
            prop.setProperty("mortality.fishing.spatial.distrib.map" + i + ".age.max", String.valueOf(cfg.speciesLifespan[i]));
            float[] season = new float[cfg.getNumberTimeStepsPerYear()];
            for (int t = 0; t < season.length; t++) {
                season[t] = t;
            }
            prop.setProperty("mortality.fishing.spatial.distrib.map" + i + ".season", toString(season));
            prop.setProperty("mortality.fishing.spatial.distrib.map" + i + ".year.min", "0");
            prop.setProperty("mortality.fishing.spatial.distrib.map" + i + ".year.max", String.valueOf(cfg.getNYear()));
        }

        // MPA
        if (cfg.mpaFilename.equalsIgnoreCase("default")) {
            prop.setProperty("mpa.file.mpa0", "null");
        } else {
            prop.setProperty("mpa.file.mpa0", cfg.mpaFilename);
        }
        prop.setProperty("mpa.start.year.mpa0", String.valueOf(cfg.yearStartMPA));
        prop.setProperty("mpa.end.year.mpa0", String.valueOf(cfg.yearEndMPA));

        // MIGRATION
        for (int i = 0; i < nSpecies; i++) {
            if (cfg.ageMigration[i] != null) {
                prop.setProperty("migration.ageclass.sp" + i, String.valueOf(toString(cfg.ageMigration[i])));
                prop.setProperty("migration.season.sp" + i, String.valueOf(toString(cfg.seasonMigration[i])));
                prop.setProperty("migration.mortality.rate.sp" + i, String.valueOf(toString(cfg.migrationTempMortality[i])));
            } else {
                prop.setProperty("migration.ageclass.sp" + i, "null");
                prop.setProperty("migration.season.sp" + i, "null");
                prop.setProperty("migration.mortality.rate.sp" + i, "null");
            }
        }

        // BIOMASS INITIALIZATION
        prop.setProperty("population.initialization.method", cfg.calibrationMethod);
        if (cfg.calibrationMethod.equalsIgnoreCase("biomass")) {
            for (int i = 0; i < nSpecies; i++) {
                prop.setProperty("population.initialization.biomass.sp" + i, String.valueOf(cfg.targetBiomass[i]));
            }
            prop.setProperty("population.initialization.spectrum.slope", "null");
            prop.setProperty("population.initialization.spectrum.intercept", "null");
            prop.setProperty("population.initialization.spectrum.range", "null");
        } else if (cfg.calibrationMethod.equalsIgnoreCase("spectrum")) {
            prop.setProperty("population.initialization.spectrum.slope", String.valueOf(cfg.sizeSpectrumSlope));
            prop.setProperty("population.initialization.spectrum.intercept", String.valueOf(cfg.sizeSpectrumIntercept));
            prop.setProperty("population.initialization.spectrum.range", String.valueOf(cfg.getSpectrumClassRange()));
            for (int i = 0; i < nSpecies; i++) {
                prop.setProperty("population.initialization.biomass.sp" + i, "null");
            }
        }

        // REPRODUCTION
        String filename = resolveFile("reproduction-seasonality.csv");
        writeReproductionSeasonalityAsCSV(filename);
        prop.setProperty("reproduction.season.file", filename);

        // INCOMING FLUX
        filename = resolveFile("incoming-flux-seasonality.csv");
        writeFluxSeasonalityAsCSV(filename);
        prop.setProperty("flux.incoming.season.file", filename);
        for (int i = 0; i < nSpecies; i++) {
            //prop.setProperty("flux.incoming.enabled.sp" + i, String.valueOf(!cfg.reproduceLocally[i]));
            if (!cfg.reproduceLocally[i]) {
                prop.setProperty("flux.incoming.biomass.sp" + i, String.valueOf(cfg.biomassFluxIn[i]));
                prop.setProperty("flux.incoming.age.sp" + i, String.valueOf(cfg.meanAgeFishIn[i]));
                prop.setProperty("flux.incoming.size.sp" + i, String.valueOf(cfg.meanLengthFishIn[i]));
            } else {
                prop.setProperty("flux.incoming.biomass.sp" + i, String.valueOf(0.f));
                prop.setProperty("flux.incoming.age.sp" + i, "null");
                prop.setProperty("flux.incoming.size.sp" + i, "null");
            }
        }


        //prop.setProperty("", String.valueOf());
    }

    private void writeAsCSV() {
        try {
            FileOutputStream fos = new FileOutputStream(resolveFile(getFilename("csv")));
            PrintWriter prw = new PrintWriter(fos, true);
            Iterator it = prop.keySet().iterator();
            while (it.hasNext()) {
                String key = String.valueOf(it.next());
                prw.print(key);
                prw.print(";");
                prw.println(prop.getProperty(key));
            }
            prw.close();
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeAsXML() {
        try {
            prop.storeToXML(new FileOutputStream(resolveFile(getFilename("xml"))), null);
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeAsText() {
        try {
            prop.store(new FileWriter(resolveFile(getFilename("txt"))), null);
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    String getFilename(String ext) {
        StringBuilder filename = new StringBuilder(outputPath);
        filename.append("/");
        filename.append(cfg.getOutputPrefix());
        filename.append("_all-parameters.");
        filename.append(ext);
        return filename.toString();
    }

    private String toString(float[] array) {
        if (array != null && array.length > 0) {
            StringBuilder str = new StringBuilder();
            for (float f : array) {
                str.append(f);
                str.append("; ");
            }
            int l = str.length();
            str.delete(l - 2, l - 1);
            return str.toString();
        }
        return "null";
    }

    private String toString(int[] array) {
        if (null == array) {
            return "null";
        }
        StringBuilder str = new StringBuilder();
        str.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            str.append("; ");
            str.append(array[i]);
        }
        return str.toString();
    }

    private void writeReproductionSeasonalityAsCSV(String filename) {

        new File(filename).getParentFile().mkdirs();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[cfg.getNSpecies() + 1];
            header[0] = "Time (year)";
            System.arraycopy(cfg.speciesName, 0, header, 1, cfg.getNSpecies());
            writer.writeNext(header);
            for (int t = 0; t < cfg.seasonSpawning[0].length; t++) {
                String[] entries = new String[header.length];
                entries[0] = String.valueOf((float) t / cfg.getNumberTimeStepsPerYear());
                for (int i = 0; i < cfg.getNSpecies(); i++) {
                    if (cfg.reproduceLocally[i]) {
                        entries[i + 1] = String.valueOf(cfg.seasonSpawning[i][t] * 100.f);
                    } else {
                        entries[i + 1] = "0";
                    }
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeFluxSeasonalityAsCSV(String filename) {

        new File(filename).getParentFile().mkdirs();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[cfg.getNSpecies() + 1];
            header[0] = "Time (year)";
            System.arraycopy(cfg.speciesName, 0, header, 1, cfg.getNSpecies());
            writer.writeNext(header);
            for (int t = 0; t < cfg.seasonSpawning[0].length; t++) {
                String[] entries = new String[header.length];
                entries[0] = String.valueOf((float) t / cfg.getNumberTimeStepsPerYear());
                for (int i = 0; i < cfg.getNSpecies(); i++) {
                    if (!cfg.reproduceLocally[i]) {
                        entries[i + 1] = String.valueOf(cfg.seasonSpawning[i][t] * 100.f);
                    } else {
                        entries[i + 1] = "0";
                    }
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeLarvalMortalityRateAsCSV(String filename) {

        new File(filename).getParentFile().mkdirs();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[cfg.getNSpecies() + 1];
            header[0] = "Time (year)";
            System.arraycopy(cfg.speciesName, 0, header, 1, cfg.getNSpecies());
            writer.writeNext(header);
            for (int t = 0; t < cfg.larvalMortalityRates[0].length; t++) {
                String[] entries = new String[header.length];
                entries[0] = String.valueOf((float) t / cfg.getNumberTimeStepsPerYear());
                for (int i = 0; i < cfg.getNSpecies(); i++) {
                    entries[i + 1] = String.valueOf(cfg.larvalMortalityRates[i][t]);
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeNaturalMortalityRateAsCSV(String filename) {

        new File(filename).getParentFile().mkdirs();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[cfg.getNSpecies() + 1];
            header[0] = "Time (year)";
            System.arraycopy(cfg.speciesName, 0, header, 1, cfg.getNSpecies());
            writer.writeNext(header);
            String[] entries = new String[header.length];
            entries[0] = "0";
            for (int i = 0; i < cfg.getNSpecies(); i++) {
                entries[i + 1] = String.valueOf(cfg.D[i]);
            }
            writer.writeNext(entries);
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeFishingAsCSV(int ispec) {

        int stepsize = 10;
        float stepage = 0.5f;

        String filename = "fishing/F-rate-byDt-" + cfg.speciesName[ispec] + ".csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(filename)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[]{"Time step", "F"};
            writer.writeNext(header);
            for (int t = 0; t < cfg.fishingRates[ispec].length; t++) {
                String[] entries = new String[2];
                entries[0] = String.valueOf(t);
                entries[1] = String.valueOf(cfg.fishingRates[ispec][t]);
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Fishing rate interannual
        float F = sum(cfg.fishingRates[ispec]);
        filename = "fishing/F-rate-byYear-" + cfg.speciesName[ispec] + ".csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(filename)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            writer.writeNext(new String[]{"Time", "Annual F"});
            for (int iYear = 0; iYear < cfg.getNYear(); iYear++) {
                writer.writeNext(new String[]{String.valueOf(iYear), String.valueOf(F)});
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Fishing seasonality
        filename = "fishing/fishing-seasonality-" + cfg.speciesName[ispec] + ".csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(filename)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            writer.writeNext(new String[]{"Time", "Season"});
            for (int t = 0; t < cfg.fishingRates[ispec].length; t++) {
                String[] entries = new String[2];
                entries[0] = String.valueOf((float) t / cfg.getNumberTimeStepsPerYear());
                if (F > 0) {
                    entries[1] = String.valueOf(100 * cfg.fishingRates[ispec][t] / F);
                } else {
                    entries[1] = "0";
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Fishing seasonality per age class
        filename = "fishing/fishing-seasonality-byAge-" + cfg.speciesName[ispec] + ".csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(filename)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[(int) Math.ceil(Math.min(cfg.speciesLifespan[ispec], 10) / stepage) + 1];
            header[0] = "v Time / Age class >";
            for (int i = 1; i < header.length; i++) {
                header[i] = String.valueOf((i - 1) * stepage);
            }
            if ((header.length * stepage) < cfg.speciesLifespan[ispec]) {
                header[header.length - 1] += "+";
            }
            writer.writeNext(header);
            for (int t = 0; t < cfg.fishingRates[ispec].length; t++) {
                String[] entries = new String[header.length];
                entries[0] = String.valueOf((float) t / cfg.getNumberTimeStepsPerYear());
                for (int i = 1; i < header.length; i++) {
                    if (F > 0) {
                        entries[i] = String.valueOf(100 * cfg.fishingRates[ispec][t] / F);
                    } else {
                        entries[i] = "0";
                    }
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Fishing rate per age class
        filename = "fishing/F-rate-byAge-" + cfg.speciesName[ispec] + ".csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(filename)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[(int) Math.ceil(cfg.speciesLifespan[ispec] / stepage) + 1];
            header[0] = "v Time / Age class >";
            for (int i = 1; i < header.length; i++) {
                header[i] = String.valueOf((i - 1) * stepage);
            }
            writer.writeNext(header);
            String[] entries = new String[header.length];
            entries[0] = "0";
            for (int i = 1; i < header.length; i++) {
                entries[i] = String.valueOf(F);
            }
            writer.writeNext(entries);
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Fishing rate per size class
        filename = "fishing/F-rate-bySize-" + cfg.speciesName[ispec] + ".csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(filename)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[(int) Math.ceil(cfg.lInf[ispec] / stepsize) + 1];
            header[0] = "v Time / Size class >";
            for (int i = 1; i < header.length; i++) {
                header[i] = String.valueOf((i - 1) * stepsize);
            }
            header[header.length - 1] += "+";
            writer.writeNext(header);
            String[] entries = new String[header.length];
            entries[0] = "0";
            for (int i = 1; i < header.length; i++) {
                entries[i] = String.valueOf(F);
            }
            writer.writeNext(entries);
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Fishing seasonality per size class
        filename = "fishing/fishing-seasonality-bySize-" + cfg.speciesName[ispec] + ".csv";
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(resolveFile(filename)), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[(int) Math.ceil(cfg.lInf[ispec] / stepsize) + 1];
            header[0] = "v Time / Size class >";
            for (int i = 1; i < header.length; i++) {
                header[i] = String.valueOf((i - 1) * stepsize);
            }
            header[header.length - 1] += "+";
            writer.writeNext(header);
            for (int t = 0; t < cfg.fishingRates[ispec].length; t++) {
                String[] entries = new String[header.length];
                entries[0] = String.valueOf((float) t / cfg.getNumberTimeStepsPerYear());
                for (int i = 1; i < header.length; i++) {
                    if (F > 0) {
                        entries[i] = String.valueOf(100 * cfg.fishingRates[ispec][t] / F);
                    } else {
                        entries[i] = "0";
                    }
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeAccessibilityAsCSV(String filename) {

        new File(filename).getParentFile().mkdirs();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[sum(cfg.nAccessStage) + 1];
            int k = 0;
            header[k] = "v Prey / Predator >";
            for (int i = 0; i < cfg.getNSpecies(); i++) {
                for (int s = 0; s < cfg.nAccessStage[i]; s++) {
                    k++;
                    if (cfg.nAccessStage[i] == 1) {
                        header[k] = cfg.speciesName[i];    // Name predators
                    } else {
                        if (s == 0) {
                            header[k] = cfg.speciesName[i] + " < " + cfg.accessStageThreshold[i][s] + " year";  // Name predators
                        } else {
                            header[k] = cfg.speciesName[i] + " > " + cfg.accessStageThreshold[i][s - 1] + " year";     // Name predators
                        }
                    }
                }
            }
            writer.writeNext(header);

            for (int i = 0; i < cfg.getNSpecies(); i++) {
                for (int s = 0; s < cfg.nAccessStage[i]; s++) {
                    k = 0;
                    String[] entries = new String[header.length];
                    if (cfg.nAccessStage[i] == 1) {
                        entries[k] = cfg.speciesName[i];    // Name predators
                    } else {
                        if (s == 0) {
                            entries[k] = cfg.speciesName[i] + " < " + cfg.accessStageThreshold[i][s] + " year";  // Name predators
                        } else {
                            entries[k] = cfg.speciesName[i] + " > " + cfg.accessStageThreshold[i][s - 1] + " year";     // Name predators
                        }
                    }
                    k++;
                    for (int ii = 0; ii < cfg.getNSpecies(); ii++) {
                        for (int ss = 0; ss < cfg.nAccessStage[ii]; ss++) {
                            entries[k] = String.valueOf(cfg.accessibilityMatrix[i][s][ii][ss]);
                            k++;
                        }
                    }
                    writer.writeNext(entries);
                }
            }
            for (int i = 0; i < cfg.getNPlankton(); i++) {
                k = 0;
                String[] entries = new String[header.length];
                entries[k] = cfg.planktonName[i];
                k++;
                for (int ii = 0; ii < cfg.getNSpecies(); ii++) {
                    for (int ss = 0; ss < cfg.nAccessStage[ii]; ss++) {
                        entries[k] = String.valueOf(cfg.accessibilityMatrix[cfg.getNSpecies() + i][0][ii][ss]);
                        k++;
                    }
                }
                writer.writeNext(entries);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean isLand(int i, int j) {
        if (null != cfg.icoordLand) {
            for (int k = 0; k < cfg.icoordLand.length; k++) {
                if (((cfg.nLine - j - 1) == cfg.icoordLand[k]) && (i == cfg.jcoordLand[k])) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeMaskAsCSV(String filename) {

        new File(filename).getParentFile().mkdirs();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            for (int j = 0; j < cfg.nLine; j++) {
                String[] line = new String[cfg.nColumn];
                for (int i = 0; i < cfg.nColumn; i++) {
                    line[i] = isLand(i, cfg.nLine - j - 1)
                            ? String.valueOf(Cell.LAND_VALUE)
                            : "0";
                }
                writer.writeNext(line);
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int sum(int[] array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    private float sum(float[] array) {
        float sum = 0;
        for (float f : array) {
            sum += f;
        }
        return sum;
    }

    public String resolveFile(String filename) {
        try {
            File file = new File(outputPath);
            String pathname = new File(file.toURI().resolve(filename)).getCanonicalPath();
            return pathname;
        } catch (Exception e) {
            return filename;
        }
    }

    public static void main(String[] args) {
        ConfigurationConverter configurationConverter = new ConfigurationConverter(args);
    }
}
