/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.util.logging.OLogger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.math3.distribution.LogNormalDistribution;

/**
 *
 * @author pverley
 */
public class Indiseas extends OLogger {

    // Name of the scenarii
    private final String[] scenarii = new String[]{"ltl", "htl", "all"};
    // Index of the species for every scenario
    private int[][] species;
    // Fmsy, maximum sustainable yield
    private float[] fmsy;
    // Osmose main configuration file
    private String osmoseMainCfg;
    // Indiseas working directory
    private String wdPath;

    /**
     * 
     */
    public void init() {

        osmoseMainCfg = getConfiguration().getFile("osmose.configuration.main");

        species = new int[scenarii.length][];
        for (int is = 0; is < scenarii.length; is++) {
            species[is] = getConfiguration().getArrayInt("indiseas.species." + scenarii[is]);
        }

        int nspecies = getConfiguration().getInt("simulation.nspecies");
        fmsy = new float[nspecies];
        for (int ispec = 0; ispec < nspecies; ispec++) {
            fmsy[ispec] = getConfiguration().getFloat("indiseas.fmsy.sp" + ispec);
        }

        wdPath = getConfiguration().getFile("indiseas.output.path");

        try (FileWriter fw = new FileWriter(getConfiguration().getFile("indiseas.simulation.file"), false)) {
            fw.write("# ");
            fw.write(Calendar.getInstance().getTime().toString());
            fw.write('\n');
        } catch (IOException ex) {
            error("Failed to append command", ex);
        }
    }

    /**
     * Create simulation launchers for three types of experience: sensitivity,
     * specificity and responsiveness of the indicators.
     */
    public void run() {
        sensitivity();
        specificity();
        responsiveness();
    }

    /**
     * Sensitivity: does the indicator vary significantly under fishing
     * pressure.
     */
    private void sensitivity() {

        // Sensitivity Fmsy multiplier
        float[] svFx = getConfiguration().getArrayFloat("indiseas.sensitivy.fmsy.multiplier");

        for (int is = 0;
                is < scenarii.length;
                is++) {
            for (int iF = 0; iF < svFx.length; iF++) {
                HashMap<String, String> options = new HashMap();
                // Output folder
                StringBuilder destination = new StringBuilder();
                destination.append(wdPath);
                destination.append(File.separator);
                destination.append("sv_");
                destination.append(scenarii[is]);
                destination.append("_fx");
                destination.append(svFx[iF]);
                options.put("output.dir.path", destination.toString());
                // Fishing mortality rates
                for (int ispec = 0; ispec < species[is].length; ispec++) {
                    int ispecies = species[is][ispec];
                    float F = svFx[iF] * fmsy[ispecies];
                    options.put("mortality.fishing.rate.sp" + ispecies, String.valueOf(F));
                }
                // Annual output
                int dtOut = getConfiguration().getInt("simulation.time.ndtPerYear") * 10;
                options.put("output.recordfrequency.ndt", String.valueOf(dtOut));
                // General options
                addGeneralOptions(options);
                // Create java command
                appendSimulation(options, destination.toString());
            }
        }
    }

    /**
     * Specificity: does the indicator respond primarily to fishing compared to
     * other drivers. Two types of environmental variability: random
     * environmental variation, and directional environmental forcing. These two
     * types of forcing intend to represent interannual variability and longer
     * term, directional climate change.
     */
    private void specificity() {

        //// Directional
        // Specificity Fmsy multiplier
        float[] spFx = getConfiguration().getArrayFloat("indiseas.specificity.fmsy.multiplier");
        // Specificity directional plankton multiplier
        float[] spPx = getConfiguration().getArrayFloat("indiseas.specificity.plankton.multiplier");

        for (int is = 0; is < scenarii.length; is++) {
            for (int iF = 0; iF < spFx.length; iF++) {
                for (int iP = 0; iP < spPx.length; iP++) {
                    HashMap<String, String> options = new HashMap();
                    // Output folder
                    StringBuilder destination = new StringBuilder();
                    destination.append(wdPath);
                    destination.append(File.separator);
                    destination.append("sp_");
                    destination.append(scenarii[is]);
                    destination.append("_fx");
                    destination.append(spFx[iF]);
                    destination.append("_plx");
                    destination.append(spPx[iP]);
                    options.put("output.dir.path", destination.toString());
                    // Fishing mortality rates
                    for (int ispec = 0; ispec < species[is].length; ispec++) {
                        int ispecies = species[is][ispec];
                        float F = spFx[iF] * fmsy[ispecies];
                        options.put("mortality.fishing.rate.sp" + ispecies, String.valueOf(F));
                    }
                    // Plankton multplier
                    for (int ipl = 0; ipl < getConfiguration().getInt("simulation.nplankton"); ipl++) {
                        options.put("plankton.multiplier.plk" + ipl, String.valueOf(spPx[iP]));
                    }
                    // Annual output
                    int dtOut = getConfiguration().getInt("simulation.time.ndtPerYear") * 10;
                    options.put("output.recordfrequency.ndt", String.valueOf(dtOut));
                    // Add general options
                    addGeneralOptions(options);
                    // Create java command
                    appendSimulation(options, destination.toString());
                }
            }
        }

        //// Random
        // Phytoplankton biomass
        double bm = getConfiguration().getDouble("indiseas.specificity.phytoplankton.biomass");
        // Standard deviation
        double[] sd = getConfiguration().getArrayDouble("indiseas.specificity.random.sd");
        // Number of draws for a given standard deviation
        int ndraw = getConfiguration().getInt("indiseas.specificity.random.ndraw");
        for (int is = 0; is < scenarii.length; is++) {
            for (int iF = 0; iF < spFx.length; iF++) {
                for (int isd = 0; isd < sd.length; isd++) {
                    double scale = Math.log(Math.pow(bm, 2) / Math.sqrt(Math.pow(sd[isd] * bm, 2) + Math.pow(bm, 2)));
                    double shape = Math.sqrt(Math.log(Math.pow(sd[isd], 2) + 1));
                    LogNormalDistribution lnd = new LogNormalDistribution(scale, shape);
                    for (int id = 0; id < ndraw; id++) {
                        HashMap<String, String> options = new HashMap();
                        // Output folder
                        StringBuilder destination = new StringBuilder();
                        destination.append(wdPath);
                        destination.append(File.separator);
                        destination.append("sp_");
                        destination.append(scenarii[is]);
                        destination.append("_fx");
                        destination.append(spFx[iF]);
                        destination.append("_sd");
                        destination.append(sd[isd]);
                        destination.append("_");
                        destination.append(id);
                        options.put("output.dir.path", destination.toString());
                        // Fishing mortality rates
                        for (int ispec = 0; ispec < species[is].length; ispec++) {
                            int ispecies = species[is][ispec];
                            float F = spFx[iF] * fmsy[ispecies];
                            options.put("mortality.fishing.rate.sp" + ispecies, String.valueOf(F));
                        }
                        double nbm = lnd.inverseCumulativeProbability(Math.random());
                        double plx = nbm / bm;
                        // Plankton multplier
                        for (int ipl = 0; ipl < getConfiguration().getInt("simulation.nplankton"); ipl++) {
                            options.put("plankton.multiplier.plk" + ipl, String.valueOf((float) plx));
                        }
                        // Annual output
                        int dtOut = getConfiguration().getInt("simulation.time.ndtPerYear") * 10;
                        options.put("output.recordfrequency.ndt", String.valueOf(dtOut));
                        // General options
                        addGeneralOptions(options);
                        // Create java command
                        appendSimulation(options, destination.toString());
                    }
                }
            }
        }
    }

    /**
     * Responsiveness: does the indicator respond rapidly to fishing.
     */
    private void responsiveness() {

        // Fmsy multiplier
        float[] rvFx = getConfiguration().getArrayFloat("indiseas.responsiveness.fmsy.multiplier");
        // Number of years of spinup with F = Fcurrent
        int nyearBurnIn = getConfiguration().getInt("indiseas.responsiveness.burnin.nyear");
        // Number of years with F = Fmsy * multiplier
        int nyearChange = getConfiguration().getInt("indiseas.responsiveness.change.nyear");

        // Simulation duration
        int nyearTot = nyearBurnIn + nyearChange;
        // Monthly output
        int dtOut = getConfiguration().getInt("simulation.time.ndtPerYear") / 12;

        for (int is = 0; is < scenarii.length; is++) {
            for (int iF = 0; iF < rvFx.length; iF++) {
                HashMap<String, String> options = new HashMap();
                // Output folder
                StringBuilder destination = new StringBuilder();
                destination.append(wdPath);
                destination.append(File.separator);
                destination.append("rv_");
                destination.append(scenarii[is]);
                destination.append("_fx");
                destination.append(rvFx[iF]);
                options.put("output.dir.path", destination.toString());
                new File(destination.toString()).mkdirs();
                for (int ispec = 0; ispec < species[is].length; ispec++) {
                    int ispecies = species[is][ispec];
                    float F1 = getConfiguration().getFloat("mortality.fishing.rate.sp" + ispec);
                    float F2 = rvFx[iF] * fmsy[ispecies];
                    StringBuilder csvfile = new StringBuilder();
                    csvfile.append(destination);
                    csvfile.append(File.separator);
                    csvfile.append("fishing-rate-by-year-sp");
                    csvfile.append(ispec);
                    csvfile.append(".csv");
                    try (CSVWriter writer = new CSVWriter(new FileWriter(csvfile.toString()), ';')) {
                        for (int iy = 0; iy < nyearTot; iy++) {
                            float F = (iy < nyearBurnIn) ? F1 : F2;
                            String[] newline = new String[]{String.valueOf(iy), String.valueOf(F)};
                            writer.writeNext(newline);
                        }
                        info("Created fishing mortality file by year for species " + ispecies + " " + csvfile);
                    } catch (IOException ex) {
                        error("Error writing CSV file " + csvfile, ex);
                    }
                    options.put("mortality.fishing.rate.byYear.file.sp" + ispecies, csvfile.toString());
                }
                // Monthly output
                options.put("output.recordfrequency.ndt", String.valueOf(dtOut));
                // Simulation duration
                options.put("simulation.time.nyear", String.valueOf(nyearTot));
                // General options
                addGeneralOptions(options);
                // Create java command
                appendSimulation(options, destination.toString());
            }
        }
    }

    private void addGeneralOptions(HashMap<String, String> options) {

        // Number of replicated simulations
        options.put("simulation.nsimulation", getConfiguration().getString("simulation.nsimulation"));
    }

    private void appendSimulation(HashMap<String, String> options, String destination) {

        String cmd = createCommand(options);

        // Append the new simulation command in the indiseas simulation file
        try (FileWriter fw = new FileWriter(getConfiguration().getFile("indiseas.simulation.file"), true)) {
            fw.write(cmd);
            fw.write("\n\n");
        } catch (IOException ex) {
            error("Failed to append command", ex);
        }

        // Create output folder
        File output = new File(destination);
        output.mkdirs();

        // Write summary text file in output folder
        File summaryFile = new File(output, output.getName() + ".txt");
        try (FileWriter fw = new FileWriter(summaryFile, true)) {
            fw.write(optionsToString(options));
            fw.write('\n');
            fw.write("Java command:\n");
            fw.write(cmd);
        } catch (IOException ex) {
            error("Failed to write summary file", ex);
        }
    }

    private String optionsToString(HashMap<String, String> options) {
        StringBuilder txt = new StringBuilder();
        txt.append("Overriden parameters:\n");
        SortedSet<String> keys = new TreeSet<>(options.keySet());
        for (String key : keys) {
            txt.append(key);
            txt.append(" = ");
            txt.append(options.get(key));
            txt.append('\n');
        }
        return txt.toString();
    }

    private String createCommand(HashMap<String, String> options) {

        StringBuilder cmd = new StringBuilder();
        cmd.append(getConfiguration().getString("java.command"));
        cmd.append(" -jar ");
        cmd.append(getConfiguration().getString("osmose.jar"));
        // Resolve option
        cmd.append(" -resolve=");
        cmd.append(getConfiguration().getString("osmose.path.resolve"));
        cmd.append(" ");
        // Osmose parameters 
        for (Map.Entry<String, String> argument : options.entrySet()) {
            cmd.append("-P");
            cmd.append(argument.getKey());
            cmd.append("=");
            cmd.append(argument.getValue());
            cmd.append(" ");
        }
        // main Osmose configuration file
        cmd.append(osmoseMainCfg);
        // Send the log to the output folder
        cmd.append(" &> ");
        cmd.append(options.get("output.dir.path"));
        cmd.append(File.separator);
        cmd.append("osmose.log");
        return cmd.toString();
    }

    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

}
