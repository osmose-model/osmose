/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2015
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
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
package fr.ird.osmose;

import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.util.OsmoseLinker;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
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
public class Indiseas extends OsmoseLinker {

    // Name of the scenarii
    private final String[] scenarii = new String[]{"ltl", "htl", "all"};
    // Index of the species for every scenario
    private int[][] species;
    // Fmsy, maximum sustainable yield
    private float[] fmsy;
    // Indiseas working directory
    private String wdPath;

    /**
     *
     */
    public void init() {

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
        if (!wdPath.endsWith(File.separator)) {
            wdPath += File.separator;
        }

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
        info("Running Indiseas simulation generator");
        sensitivity();
        specificity();
        responsiveness();
        info("Indiseas simulation launchers listed in file " + getConfiguration().getFile("indiseas.simulation.file"));
        info("Indiseas file tree generated in folder " + wdPath);
    }

    /**
     * Sensitivity: does the indicator vary significantly under fishing
     * pressure.
     */
    private void sensitivity() {

        info("Indiseas SENSITIVITY");

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
                destination.append("sv_");
                destination.append(scenarii[is]);
                destination.append("_fx");
                destination.append(svFx[iF]);
                options.put("output.dir.path", destination.toString());
                info(new File(destination.toString()).getName());
                // Fishing mortality rates
                for (int ispec = 0; ispec < species[is].length; ispec++) {
                    int ispecies = species[is][ispec];
                    float F = svFx[iF] * fmsy[ispecies];
                    options.put("mortality.fishing.rate.sp" + ispecies, String.valueOf(F));
                }
                // Annual output
                int dtOut = getConfiguration().getInt("simulation.time.ndtPerYear");
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
        info("Indiseas SPECIFICITY directional");
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
                    destination.append("spd_");
                    destination.append(scenarii[is]);
                    destination.append("_fx");
                    destination.append(spFx[iF]);
                    destination.append("_plx");
                    destination.append(spPx[iP]);
                    options.put("output.dir.path", destination.toString());
                    info(new File(destination.toString()).getName());
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
                    int dtOut = getConfiguration().getInt("simulation.time.ndtPerYear");
                    options.put("output.recordfrequency.ndt", String.valueOf(dtOut));
                    // Add general options
                    addGeneralOptions(options);
                    // Create java command
                    appendSimulation(options, destination.toString());
                }
            }
        }

        //// Random
        info("Indiseas SPECIFICITY random");
        // Phytoplankton biomass
        double bm = getConfiguration().getDouble("indiseas.specificity.phytoplankton.biomass");
        // Standard deviation
        double[] sd = getConfiguration().getArrayDouble("indiseas.specificity.random.sd");
        // Number of draws for a given standard deviation
        int ndraw = getConfiguration().getInt("indiseas.specificity.random.ndraw");
        NumberFormat fmt = NumberFormat.getIntegerInstance();
        fmt.setMinimumIntegerDigits(String.valueOf(ndraw).length());
        // Phytoplankton biomass
        double[][][] pbm = new double[spFx.length][sd.length][ndraw];
        // Create CSV file for saving phytoplankton biomass
        StringBuilder csvfile = new StringBuilder();
        csvfile.append(wdPath);
        csvfile.append(File.separator);
        csvfile.append("specificity-phytoplankton-biomass.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvfile.toString()), ',')) {
            writer.writeNext(new String[]{"Fmsy multiplier", "Standard deviation multiplier", "Input phytoplankton biomass"});
            for (int iF = 0; iF < spFx.length; iF++) {
                for (int isd = 0; isd < sd.length; isd++) {
                    double scale = Math.log(Math.pow(bm, 2) / Math.sqrt(Math.pow(sd[isd] * bm, 2) + Math.pow(bm, 2)));
                    double shape = Math.sqrt(Math.log(Math.pow(sd[isd], 2) + 1));
                    LogNormalDistribution lnd = new LogNormalDistribution(scale, shape);
                    for (int id = 0; id < ndraw; id++) {
                        pbm[iF][isd][id] = lnd.inverseCumulativeProbability(Math.random());
                        // Add new phytoplankton biomass to CSV file
                        String[] newline = new String[]{String.valueOf(spFx[iF]), String.valueOf(sd[isd]), String.valueOf(Math.round(pbm[iF][isd][id]))};
                        writer.writeNext(newline);
                    }
                }
            }
        } catch (IOException ex) {
            error("Error writing phytoplankton biomass CSV file " + csvfile.toString(), ex);
        }
        for (int is = 0; is < scenarii.length; is++) {
            for (int iF = 0; iF < spFx.length; iF++) {
                for (int isd = 0; isd < sd.length; isd++) {
                    for (int id = 0; id < ndraw; id++) {
                        HashMap<String, String> options = new HashMap();
                        // Output folder
                        StringBuilder destination = new StringBuilder();
                        destination.append(wdPath);
                        destination.append("spr_");
                        destination.append(scenarii[is]);
                        destination.append("_fx");
                        destination.append(spFx[iF]);
                        destination.append("_sd");
                        destination.append(sd[isd]);
                        destination.append("_");
                        destination.append(fmt.format(id));
                        options.put("output.dir.path", destination.toString());
                        info(new File(destination.toString()).getName());
                        // Fishing mortality rates
                        for (int ispec = 0; ispec < species[is].length; ispec++) {
                            int ispecies = species[is][ispec];
                            float F = spFx[iF] * fmsy[ispecies];
                            options.put("mortality.fishing.rate.sp" + ispecies, String.valueOf(F));
                        }
                        double plx = pbm[iF][isd][id] / bm;
                        // Plankton multplier
                        for (int ipl = 0; ipl < getConfiguration().getInt("simulation.nplankton"); ipl++) {
                            options.put("plankton.multiplier.plk" + ipl, String.valueOf((float) plx));
                        }
                        // Annual output
                        int dtOut = getConfiguration().getInt("simulation.time.ndtPerYear");
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

        info("Indiseas RESPONSIVENESS");
        // Fmsy multiplier
        float[] rvFx = getConfiguration().getArrayFloat("indiseas.responsiveness.fmsy.multiplier");
        // Number of years of spinup with F = Fmsy
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
                destination.append("rv_");
                destination.append(scenarii[is]);
                destination.append("_fx");
                destination.append(rvFx[iF]);
                options.put("output.dir.path", destination.toString());
                info(new File(destination.toString()).getName());
                new File(destination.toString()).mkdirs();
                for (int ispec = 0; ispec < species[is].length; ispec++) {
                    int ispecies = species[is][ispec];
                    StringBuilder csvfile = new StringBuilder();
                    csvfile.append(destination);
                    csvfile.append(File.separator);
                    csvfile.append("fishing-rate-by-year-sp");
                    csvfile.append(ispecies);
                    csvfile.append(".csv");
                    try (CSVWriter writer = new CSVWriter(new FileWriter(csvfile.toString()), ';')) {
                        for (int iy = 0; iy < nyearTot; iy++) {
                            float F = (iy < nyearBurnIn) ? fmsy[ispecies] : (rvFx[iF] * fmsy[ispecies]);
                            String[] newline = new String[]{String.valueOf(iy), String.valueOf(F)};
                            writer.writeNext(newline);
                        }
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

        // Start year for output set to zero
        options.put("output.start.year", String.valueOf(0));
        // Abundance output
        options.put("output.abundance.enabled", String.valueOf(true));
        // Biomass output
        options.put("output.biomass.enabled", String.valueOf(true));
        // Yield output
        options.put("output.yield.biomass.enabled", String.valueOf(true));
        // Trophic level output
        options.put("output.TL.enabled", String.valueOf(true));
        // Mean size output
        options.put("output.size.enabled", String.valueOf(true));
        // Biomass size distribution output
        options.put("output.biomass.bysize.enabled", String.valueOf(true));
        // Ncpu must be set to 1
        options.put("simulation.ncpu", String.valueOf(1));
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
        try (FileWriter fw = new FileWriter(summaryFile)) {
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
        // Quiet option (only print error message in log file
        cmd.append(" -quiet ");
        // Osmose parameters 
        for (Map.Entry<String, String> argument : options.entrySet()) {
            cmd.append("-P");
            cmd.append(argument.getKey());
            boolean hasBlank = argument.getValue().contains(" ");
            cmd.append(hasBlank ? "=\"" : "=");
            cmd.append(argument.getValue());
            cmd.append(hasBlank ? "\" " : " ");
        }
        // main Osmose configuration file
        cmd.append(getConfiguration().getMainFile().replaceAll("\\s", "\\\\ "));
        // Send the log to the output folder
        cmd.append(" > ");
        cmd.append(options.get("output.dir.path").replaceAll("\\s", "\\\\ "));
        cmd.append(File.separator);
        cmd.append("osmose.log");
        return cmd.toString();
    }

}
