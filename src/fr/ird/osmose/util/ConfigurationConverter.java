package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Configuration.SpatialDistribution;
import fr.ird.osmose.Osmose;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class ConfigurationConverter {

    private Configuration cfg;
    private Properties prop;

    ConfigurationConverter(String[] args) {

        // Get old configuration
        Osmose osmose = Osmose.getInstance();
        osmose.init(args);
        cfg = osmose.getConfiguration();

        // Create new Properties
        prop = new Properties();

        // convert
        System.out.println("Converting old configuration file to new format...");
        convert();
        write();
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

        // INDICATORS
        prop.setProperty("output.abundance.nojuv.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.abundance.tot.enabled", String.valueOf(cfg.outputClass0));
        prop.setProperty("output.biomass.nojuv.enabled", String.valueOf(true));
        prop.setProperty("output.biomass.tot.enabled", String.valueOf(cfg.outputClass0));
        prop.setProperty("output.yield.biomass.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.yield.abundance.enabled", String.valueOf(!cfg.outputCalibration));
        prop.setProperty("output.mortality.enabled", String.valueOf(!cfg.outputCalibration));

        prop.setProperty("output.trophic.diet.enabled", String.valueOf(cfg.outputDiet));
        prop.setProperty("output.trophic.pressure.enabled", String.valueOf(cfg.outputDiet));
        prop.setProperty("output.trophic.diet.metrics", String.valueOf(cfg.getDietOutputMetrics()));
        for (int i = 0; i < nSpecies; i++) {
            String key = "output.trophic.diet.stage.sp" + i;
            prop.setProperty(key, String.valueOf(toString(cfg.dietStageThreshold[i])));
        }
        prop.setProperty("output.trophic.tl.catch.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.trophic.tl.mean.enabled", String.valueOf(cfg.outputTL));
        prop.setProperty("output.trophic.tl.spectrum.enabled", String.valueOf(cfg.outputTLSpectrum));

        prop.setProperty("output.spatial.ltl.enabled", String.valueOf(cfg.outputPlanktonBiomass));
        prop.setProperty("output.spatial.enabled", String.valueOf(cfg.outputSpatialized));

        prop.setProperty("output.size.catch.enabled", String.valueOf(cfg.outputMeanSize));
        prop.setProperty("output.size.mean.enabled", String.valueOf(cfg.outputMeanSize));
        prop.setProperty("output.size.spectrum.mean.enabled", String.valueOf(cfg.outputSizeSpectrum));
        prop.setProperty("output.size.spectrum.species.enabled", String.valueOf(cfg.outputSizeSpectrumSpecies));
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
            prop.setProperty("species.longevity.sp" + i, String.valueOf(cfg.speciesLongevity[i]));
            prop.setProperty("species.lInf.sp" + i, String.valueOf(cfg.lInf[i]));
            prop.setProperty("species.K.sp" + i, String.valueOf(cfg.K[i]));
            prop.setProperty("species.t0.sp" + i, String.valueOf(cfg.t0[i]));
            prop.setProperty("species.c.sp", String.valueOf(cfg.c[i]));
            prop.setProperty("species.bPower.sp" + i, String.valueOf(cfg.bPower[i]));
            prop.setProperty("species.size.egg.sp" + i, String.valueOf(cfg.eggSize[i]));
            prop.setProperty("species.size.maturity.sp" + i, String.valueOf(cfg.sizeMaturity[i]));
            prop.setProperty("species.weight.egg.sp" + i, String.valueOf(cfg.eggWeight[i]));
            prop.setProperty("species.age.recruitment.sp" + i, String.valueOf(cfg.recruitmentAge[i]));
            prop.setProperty("species.age.class0.sp" + i, String.valueOf(cfg.supAgeOfClass0Matrix[i]));
        }

        // PLANKTON
        for (int i = 0; i < nPlankton; i++) {
            prop.setProperty("plankton.name.pl" + i, String.valueOf(cfg.planktonName[i]));
            prop.setProperty("plankton.tl.pl" + i, String.valueOf(cfg.ltlTrophicLevel[i]));
            prop.setProperty("plankton.size.min.pl" + i, String.valueOf(cfg.ltlMinSize[i]));
            prop.setProperty("plankton.size.max.pl" + i, String.valueOf(cfg.ltlMaxSize[i]));
            prop.setProperty("plankton.conversion.pl" + i, String.valueOf(cfg.ltlConversionFactor[i]));
            prop.setProperty("plankton.accessibility.pl" + i, String.valueOf(cfg.planktonAccessibility[i]));
        }

        // LTL MODEL
        prop.setProperty("ltl.depth.integration", String.valueOf(cfg.getIntegrationDepth()));
        for (int i = 0; i < nPlankton; i++) {
            prop.setProperty("ltl.netcdf.plankton.name.pl" + i, "null");
        }
        String className = cfg.getLTLClassName();
        prop.setProperty("ltl.classname", String.valueOf(className));
        if (className.equals("fr.ird.osmose.ltl.LTLFastForcing")) {
            prop.setProperty("ltl.forcing.file", String.valueOf(cfg.ltlForcingFilename));
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcingRomsPisces")
                || className.equals("fr.ird.osmose.ltl.LTLForcingRomsPisces")
                || className.equals("fr.ird.osmose.ltl.LTLForcingLaure")) {
            prop.setProperty("ltl.forcing.grid.file", "null");
            prop.setProperty("ltl.forcing.field.lat", "lat_rho");
            prop.setProperty("ltl.forcing.field.lon", "lon_rho");
            prop.setProperty("ltl.forcing.field.bathy", "h");
            prop.setProperty("ltl.forcing.field.hc", "hc");
            prop.setProperty("ltl.forcing.field.csr", "Cs_r");
            prop.setProperty("ltl.forcing.file", "null");
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcingECO3M")
                || className.equals("fr.ird.osmose.ltl.LTLForcingECO3M")) {
            prop.setProperty("ltl.forcing.field.zlevel", "levels_ZHL");
            prop.setProperty("ltl.forcing.file", "null");
        } else if (className.equals("fr.ird.osmose.ltl.LTLFastForcingBFM")
                || className.equals("fr.ird.osmose.ltl.LTLForcingBFM")) {
            prop.setProperty("ltl.forcing.field.bathy", "h");
            prop.setProperty("ltl.forcing.field.zlevel", "zz");
            prop.setProperty("ltl.forcing.dim.ntime", "2");
            prop.setProperty("ltl.forcing.file", "null");
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
                    key.append(".csv");
                    value = cfg.mapFile[iMap];
                    prop.setProperty(key.toString(), value);
                    if (cfg.spatialDistribution[i].equals(SpatialDistribution.CONNECTIVITY)) {
                        key = new StringBuilder(map);
                        key.append(".connectivity");
                        value = cfg.connectivityFile[iMap];
                        prop.setProperty(key.toString(), value);
                    }
                }
            }
        }
        
        
        
        //prop.setProperty("", String.valueOf());
    }

    private void write() {
        try {
            prop.store(new FileWriter(cfg.resolveFile(getFilename())), null);
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    String getFilename() {
        StringBuilder filename = new StringBuilder(cfg.getOutputPrefix());
        filename.append("_all-parameters.cfg");
        return filename.toString();
    }

    private String toString(float[] array) {
        if (array.length > 0) {
            StringBuilder str = new StringBuilder();
            for (float f : array) {
                str.append(f);
                str.append(", ");
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
            str.append(", ");
            str.append(array[i]);
        }
        return str.toString();
    }

    public static void main(String[] args) {
        new ConfigurationConverter(args).convert();
    }
}
