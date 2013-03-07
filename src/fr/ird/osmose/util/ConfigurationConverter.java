package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.OldConfiguration;
import fr.ird.osmose.OldConfiguration.SpatialDistribution;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.ltl.AbstractLTLForcing;
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

    private OldConfiguration cfg;
    private Properties prop;

    ConfigurationConverter(String[] args) {

        // Get old configuration
        Osmose osmose = Osmose.getInstance();
        osmose.init(args);
        cfg = osmose.getOldConfiguration();

        // Create new Properties
        prop = new Properties();

        // convert
        System.out.println("Converting old configuration file to new format...");
        convert();
        writeAsText();
        writeAsCSV();
        writeAsXML();
        System.out.println("Conversion completed successfully.");
    }

    private void convert() {

        int nSpecies = cfg.getNSpecies();
        int nPlankton = cfg.getNPlankton();
        // OUTPUT
        prop.setProperty("output.path", cfg.getOutputPathname() + cfg.getOutputFolder());
        prop.setProperty("output.prefix", cfg.getOutputPrefix());
        prop.setProperty("output.recordfrequency", String.valueOf(cfg.getRecordFrequency()));
        prop.setProperty("output.yearstart", String.valueOf(cfg.yearStartSaving));
        prop.setProperty("output.cutoff.enabled", String.valueOf(cfg.outputClass0));
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("output.cutoff.age.sp" + i, String.valueOf(cfg.supAgeOfClass0Matrix[i]));
        }

        // INDICATORS
        prop.setProperty("output.abundance.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.biomass.enabled", String.valueOf(true));
        prop.setProperty("output.yield.biomass.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.yield.abundance.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.mortality.enabled", String.valueOf(!cfg.outputCalibration));

        prop.setProperty("output.trophic.diet.enabled", String.valueOf(cfg.outputDiet));
        prop.setProperty("output.trophic.pressure.enabled", String.valueOf(cfg.outputDiet));
        prop.setProperty("output.trophic.diet.metrics", String.valueOf(cfg.getDietOutputMetrics()));
        for (int i = 0; i < nSpecies; i++) {
            String key = "output.trophic.diet.stage.threshold.sp" + i;
            prop.setProperty(key, String.valueOf(toString(cfg.dietStageThreshold[i])));
        }
        prop.setProperty("output.trophic.TL.catch.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.trophic.TL.mean.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.trophic.TL.spectrum.enabled", String.valueOf(cfg.outputTLSpectrum));
        prop.setProperty("output.trophic.TL.mean.perSize.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.trophic.TL.mean.perAge.enabled", String.valueOf(cfg.outputTL));

        prop.setProperty("output.spatial.ltl.enabled", String.valueOf(cfg.outputPlanktonBiomass));
        prop.setProperty("output.spatial.enabled", String.valueOf(cfg.outputSpatialized));

        prop.setProperty("output.size.catch.enabled", String.valueOf(cfg.outputMeanSize));
        prop.setProperty("output.size.mean.enabled", String.valueOf(cfg.outputMeanSize));
        prop.setProperty("output.size.mean.perSpecies.enabled", String.valueOf(cfg.outputMeanSize));
        prop.setProperty("output.size.spectrum.mean.enabled", String.valueOf(cfg.outputSizeSpectrum));
        prop.setProperty("output.size.spectrum.species.N.enabled", String.valueOf(cfg.outputSizeSpectrumSpecies));
        prop.setProperty("output.size.spectrum.species.B.enabled", String.valueOf(cfg.outputSizeSpectrumSpecies));
        prop.setProperty("output.size.spectrum.minsize", String.valueOf(cfg.getSpectrumMinSize()));
        prop.setProperty("output.size.spectrum.maxsize", String.valueOf(cfg.getSpectrumMaxSize()));
        prop.setProperty("output.size.spectrum.classrange", String.valueOf(cfg.getSpectrumClassRange()));

        // SIMULATION
        prop.setProperty("simulation.time.nstepyear", String.valueOf(cfg.getNumberTimeStepsPerYear()));
        prop.setProperty("simulation.time.nyear", String.valueOf(cfg.getNYear()));
        prop.setProperty("simulation.nsimulation", String.valueOf(cfg.getNSimulation()));
        prop.setProperty("simulation.nschool", String.valueOf(cfg.nSchool));
        prop.setProperty("simulation.ncpu", String.valueOf(Runtime.getRuntime().availableProcessors()));
        prop.setProperty("simulation.nspecies", String.valueOf(cfg.getNSpecies()));
        prop.setProperty("simulation.nplankton", String.valueOf(cfg.getNPlankton()));

        // SPECIES
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("species.name.sp" + i, String.valueOf(cfg.speciesName[i]));
            prop.setProperty("species.lifespan.sp" + i, String.valueOf(cfg.speciesLifespan[i]));
            prop.setProperty("species.lInf.sp" + i, String.valueOf(cfg.lInf[i]));
            prop.setProperty("species.K.sp" + i, String.valueOf(cfg.K[i]));
            prop.setProperty("species.t0.sp" + i, String.valueOf(cfg.t0[i]));
            prop.setProperty("species.c.sp", String.valueOf(cfg.c[i]));
            prop.setProperty("species.bPower.sp" + i, String.valueOf(cfg.bPower[i]));
            prop.setProperty("species.size.egg.sp" + i, String.valueOf(cfg.eggSize[i]));
            prop.setProperty("species.size.maturity.sp" + i, String.valueOf(cfg.sizeMaturity[i]));
            prop.setProperty("species.weight.egg.sp" + i, String.valueOf(cfg.eggWeight[i]));
            prop.setProperty("species.age.recruitment.sp" + i, String.valueOf(cfg.recruitmentAge[i]));
        }

        // PLANKTON
        for (int i = 0; i < nPlankton; i++) {
            prop.setProperty("plankton.name.plk" + i, String.valueOf(cfg.planktonName[i]));
            prop.setProperty("plankton.TL.plk" + i, String.valueOf(cfg.ltlTrophicLevel[i]));
            prop.setProperty("plankton.size.min.plk" + i, String.valueOf(cfg.ltlMinSize[i]));
            prop.setProperty("plankton.size.max.plk" + i, String.valueOf(cfg.ltlMaxSize[i]));
            prop.setProperty("plankton.conversion2tons.plk" + i, String.valueOf(cfg.ltlConversionFactor[i]));
            prop.setProperty("plankton.accessibility.plk" + i, String.valueOf(cfg.planktonAccessibility[i]));
        }

        // LTL MODEL
        String className = cfg.getLTLClassName();
        prop.setProperty("ltl.classname", String.valueOf(className));
        String[] files = ((AbstractLTLForcing) cfg.getForcing()).getNetcdfFile();
        for (int i = 0; i < files.length; i++) {
            prop.setProperty("ltl.netcdf.file" + i, cfg.resolveFile(files[i]));
        }
        if (className.equals("fr.ird.osmose.ltl.LTLFastForcingRomsPisces")
                || className.equals("fr.ird.osmose.ltl.LTLForcingRomsPisces")
                || className.equals("fr.ird.osmose.ltl.LTLForcingLaure")) {
            prop.setProperty("ltl.integration.depth", String.valueOf(cfg.getIntegrationDepth()));
            String[] names = ((AbstractLTLForcing) cfg.getForcing()).getPlanktonFieldName();
            for (int i = 0; i < nPlankton; i++) {
                prop.setProperty("ltl.netcdf.field.plankton.plk" + i, names[i]);
            }
            prop.setProperty("ltl.netcdf.grid.file", "null");
            prop.setProperty("ltl.netcdf.field.lat", "lat_rho");
            prop.setProperty("ltl.netcdf.field.lon", "lon_rho");
            prop.setProperty("ltl.netcdf.field.bathy", "h");
            prop.setProperty("ltl.netcdf.field.hc", "hc");
            prop.setProperty("ltl.netcdf.field.csr", "Cs_r");
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcingECO3M")
                || className.equals("fr.ird.osmose.ltl.LTLForcingECO3M")) {
            prop.setProperty("ltl.integration.depth", String.valueOf(cfg.getIntegrationDepth()));
            String[] names = ((AbstractLTLForcing) cfg.getForcing()).getPlanktonFieldName();
            for (int i = 0; i < nPlankton; i++) {
                prop.setProperty("ltl.netcdf.field.plankton.plk" + i, names[i]);
            }
            prop.setProperty("ltl.netcdf.field.zlevel", "levels_ZHL");
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcingBFM")
                || className.equals("fr.ird.osmose.ltl.LTLForcingBFM")) {
            prop.setProperty("ltl.integration.depth", String.valueOf(cfg.getIntegrationDepth()));
            String[] names = ((AbstractLTLForcing) cfg.getForcing()).getPlanktonFieldName();
            for (int i = 0; i < nPlankton; i++) {
                prop.setProperty("ltl.netcdf.field.plankton.plk" + i, names[i]);
            }
            prop.setProperty("ltl.netcdf.field.bathy", "h");
            prop.setProperty("ltl.netcdf.field.zlevel", "zz");
            prop.setProperty("ltl.netcdf.dim.ntime", "2");
        }

        // GRID
        prop.setProperty("grid.classname", String.valueOf(cfg.gridClassName));
        className = cfg.gridClassName;
        if (className.equals("fr.ird.osmose.grid.OriginalGrid")) {
            prop.setProperty("grid.upleft.lat", String.valueOf(cfg.upLeftLat));
            prop.setProperty("grid.upleft.lon", String.valueOf(cfg.upLeftLon));
            prop.setProperty("grid.lowright.lat", String.valueOf(cfg.lowRightLat));
            prop.setProperty("grid.lowright.lat", String.valueOf(cfg.lowRightLon));
            prop.setProperty("grid.nline", String.valueOf(cfg.nLine));
            prop.setProperty("grid.ncolumn", String.valueOf(cfg.nColumn));
        } else {
            prop.setProperty("grid.netcdf", String.valueOf(cfg.gridFileTab));
            prop.setProperty("grid.field.lat", String.valueOf(cfg.latField));
            prop.setProperty("grid.field.lon", String.valueOf(cfg.lonField));
            prop.setProperty("grid.field.mask", String.valueOf(cfg.maskField));
            prop.setProperty("grid.stride", String.valueOf(cfg.stride));
        }

        // MOVEMENT
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("movement.distribution.method.sp" + i, String.valueOf(cfg.spatialDistribution[i]));
            prop.setProperty("movement.randomwalk.range.sp" + i, String.valueOf(cfg.range[i]));
            if (cfg.spatialDistribution[i].equals(SpatialDistribution.RANDOM)) {
                prop.setProperty("movement.distribution.ncell.sp" + i, String.valueOf(cfg.randomAreaSize[i]));
            } else {
                int nmap = cfg.maps.length;
                for (int iMap = 0; iMap < nmap; iMap++) {
                    String map = "movement.map" + iMap;
                    String value;
                    StringBuilder key = new StringBuilder(map);
                    key.append(".species");
                    value = cfg.speciesName[cfg.speciesMap[iMap]];
                    prop.setProperty(key.toString(), value);
                    key = new StringBuilder(map);
                    key.append(".agemin");
                    value = String.valueOf(cfg.agesMap[iMap][0]);
                    prop.setProperty(key.toString(), value);
                    key = new StringBuilder(map);
                    key.append(".agemax");
                    value = String.valueOf(cfg.agesMap[iMap][cfg.agesMap[iMap].length - 1] + 1);
                    prop.setProperty(key.toString(), value);
                    key = new StringBuilder(map);
                    key.append(".season");
                    value = toString(cfg.seasonMap[iMap]);
                    prop.setProperty(key.toString(), value);
                    key = new StringBuilder(map);
                    key.append(".area.file");
                    value = cfg.mapFile[iMap];
                    prop.setProperty(key.toString(), value);
                    if (cfg.spatialDistribution[i].equals(SpatialDistribution.CONNECTIVITY)) {
                        key = new StringBuilder(map);
                        key.append(".connectivity.file");
                        value = cfg.connectivityFile[iMap];
                        prop.setProperty(key.toString(), value);
                    }
                }
            }
        }

        // PREDATION
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("predation.predPrey.stage.threshold.sp" + i, String.valueOf(toString(cfg.feedingStageThreshold[i])));
            prop.setProperty("predation.predPrey.sizeRatio.min.sp" + i, String.valueOf(toString(cfg.predPreySizeRatioMin[i])));
            prop.setProperty("predation.predPrey.sizeRatio.max.sp" + i, String.valueOf(toString(cfg.predPreySizeRatioMax[i])));
            prop.setProperty("predation.ingestion.rate.max.sp" + i, String.valueOf(cfg.maxPredationRate[i]));
            prop.setProperty("predation.efficiency.critical.sp" + i, String.valueOf(cfg.criticalPredSuccess[i]));
            prop.setProperty("predation.accessibility.stage.metrics", "age");
            prop.setProperty("predation.accessibility.stage.threshold.sp" + i, String.valueOf(toString(cfg.accessStageThreshold[i])));
        }
        String accessibilityFile = cfg.resolveFile("predation-accessibility.csv");
        prop.setProperty("predation.accessibility.file", accessibilityFile);
        writeAccessibilityAsCSV(accessibilityFile);

        // STARVATION
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("mortality.starvation.rate.max.sp" + i, String.valueOf(cfg.starvMaxRate[i]));
        }

        // NATURAL MORTALITY
        if (null == cfg.larvalMortalityFile) {
            String larvalMortalityFile = cfg.resolveFile("larval-mortality-rates.csv");
            prop.setProperty("mortality.natural.rate.larva.file", larvalMortalityFile);
            writeLarvalMortalityRateAsCSV(larvalMortalityFile);
        } else {
            prop.setProperty("predation.accessibility.file", cfg.resolveFile(cfg.larvalMortalityFile));
        }
        String naturalMortalityFile = cfg.resolveFile("natural-mortality-rates.csv");
        prop.setProperty("mortality.natural.rate.file", naturalMortalityFile);
        writeNaturalMortalityRateAsCSV(naturalMortalityFile);

        // FISHING
        for (int i = 0; i < nSpecies; i++) {
            prop.setProperty("fishing.rate.sp" + i, String.valueOf(toString(cfg.fishingRates[i])));
        }

        // MPA
        if (cfg.mpaFilename.equalsIgnoreCase("default")) {
            prop.setProperty("mpa.file.m0", "null");
        } else {
            prop.setProperty("mpa.file.m0", cfg.mpaFilename);
        }
        prop.setProperty("mpa.year.start.m0", String.valueOf(cfg.yearStartMPA));
        prop.setProperty("mpa.year.end.m0", String.valueOf(cfg.yearEndMPA));

        // MIGRATION
        for (int i = 0; i < nSpecies; i++) {
            if (cfg.ageMigration[i] != null) {
                prop.setProperty("migration.ageclass.sp" + i, String.valueOf(toString(cfg.ageMigration[i])));
                prop.setProperty("migration.season.sp" + i, String.valueOf(toString(cfg.seasonMigration[i])));
                prop.setProperty("migration.mortality.rate.sp" + i, String.valueOf(toString(cfg.migrationTempMortality[i])));
            } else {
                prop.setProperty("migration.year.sp" + i, "null");
                prop.setProperty("migration.season.sp" + i, "null");
                prop.setProperty("migration.mortality.rate.sp" + i, "null");
            }
        }

        // BIOMASS INITIALIZATION
        prop.setProperty("population.initialization.method", cfg.calibrationMethod);
        if (cfg.calibrationMethod.equalsIgnoreCase("biomass")) {
            for (int i = 0; i < nSpecies; i++) {
                prop.setProperty("population.initalization.biomass.sp" + i, String.valueOf(cfg.targetBiomass[i]));
            }
            prop.setProperty("population.initalization.spectrum.slope", "null");
            prop.setProperty("population.initalization.biomass.intercept", "null");
        } else if (cfg.calibrationMethod.equalsIgnoreCase("spectrum")) {
            prop.setProperty("population.initalization.spectrum.slope", String.valueOf(cfg.sizeSpectrumSlope));
            prop.setProperty("population.initalization.biomass.intercept", String.valueOf(cfg.sizeSpectrumIntercept));
            for (int i = 0; i < nSpecies; i++) {
                prop.setProperty("population.initalization.biomass.sp" + i, "null");
            }
        }


        //prop.setProperty("", String.valueOf());
    }

    private void writeAsCSV() {
        try {
            FileOutputStream fos = new FileOutputStream(cfg.resolveFile(getFilename("csv")));
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
            prop.storeToXML(new FileOutputStream(cfg.resolveFile(getFilename("xml"))), null);
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeAsText() {
        try {
            prop.store(new FileWriter(cfg.resolveFile(getFilename("txt"))), null);
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    String getFilename(String ext) {
        StringBuilder filename = new StringBuilder(cfg.getOutputPrefix());
        filename.append("_all-parameters.");
        filename.append(ext);
        return filename.toString();
    }

    private String toString(float[] array) {
        if (array.length > 0) {
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
            return null;
        }
        StringBuilder str = new StringBuilder();
        str.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            str.append("; ");
            str.append(array[i]);
        }
        return str.toString();
    }

    private void writeLarvalMortalityRateAsCSV(String filename) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[cfg.getNSpecies() + 1];
            header[0] = "Time (year)";
            for (int i = 0; i < cfg.getNSpecies(); i++) {
                header[i + 1] = cfg.speciesName[i];
            }
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
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
            String[] header = new String[cfg.getNSpecies() + 1];
            header[0] = "Time (year)";
            for (int i = 0; i < cfg.getNSpecies(); i++) {
                header[i + 1] = cfg.speciesName[i];
            }
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

    private void writeAccessibilityAsCSV(String filename) {

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

    private int sum(int[] array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    public static void main(String[] args) {
        Osmose.getInstance().init(args);
        ConfigurationConverter configurationConverter = new ConfigurationConverter(args);
    }
}
