/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import fr.ird.osmose.util.logging.OLogger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pverley
 */
public class Indiseas extends OLogger {
    
    private final String[] scenarii = new String[]{"ltl", "htl", "all"};
    private int[][] species;
    // Fmsy, maximum sustainable yield
    private float[] fmsy;
    // Sensitivity Fmsy multiplier
    private float[] svFx;
    // Specificity Fmsy multiplier
    private float[] spFx;
    // Specificity directional plankton multiplier
    private float[] spPx;
    // Specificity random
    private double phytoBiomass;
    private double[] sd;
    private int ndraw;
    // Responsiveness
    private float[] rvFx;
    private int nyearBurnIn;
    private int nyearChange;
    // General indiseas parameters
    private String osmoseMainCfg;
    private String outputPath;
    
    public void init() {
        
        osmoseMainCfg = getConfiguration().getString("osmose.configuration.main");
        
        species = new int[scenarii.length][];
        for (int is = 0; is < scenarii.length; is++) {
            species[is] = getConfiguration().getArrayInt("indiseas.species." + scenarii[is]);
        }
        
        int nspecies = getConfiguration().getInt("simulation.nspecies");
        fmsy = new float[nspecies];
        for (int ispec = 0; ispec < nspecies; ispec++) {
            fmsy[ispec] = getConfiguration().getFloat("indiseas.fmsy.sp" + ispec);
        }
        
        outputPath = getConfiguration().getFile("indiseas.output.path");
        
        try (FileWriter fw = new FileWriter(getConfiguration().getFile("indiseas.simulation.file"), false)) {
            fw.write("# ");
            fw.write(Calendar.getInstance().getTime().toString());
            fw.write('\n');
        } catch (IOException ex) {
            error("Failed to append command", ex);
        }
    }
    
    public void run() {
        sensitivity();
        specificity();
    }
    
    private void sensitivity() {
        
        svFx = getConfiguration().getArrayFloat("indiseas.sensitivy.fmsy.multiplier");
        
        for (int is = 0; is < scenarii.length; is++) {
            for (int iF = 0; iF < svFx.length; iF++) {
                HashMap<String, String> options = new HashMap();
                // Output folder
                StringBuilder destination = new StringBuilder();
                destination.append(outputPath);
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
                addGeneralOptions(options);
                appendSimulation(options, destination.toString());
            }
        }
    }
    
    private void specificity() {

        // Directional
        spFx = getConfiguration().getArrayFloat("indiseas.specificity.fmsy.multiplier");
        spPx = getConfiguration().getArrayFloat("indiseas.specificity.plankton.multiplier");
        
        for (int is = 0; is < scenarii.length; is++) {
            for (int iF = 0; iF < spFx.length; iF++) {
                for (int iP = 0; iP < spPx.length; iP++) {
                    HashMap<String, String> options = new HashMap();
                    // Output folder
                    StringBuilder destination = new StringBuilder();
                    destination.append(outputPath);
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
                    addGeneralOptions(options);
                    appendSimulation(options, destination.toString());
                }
            }
        }
        
        // Random
        phytoBiomass = getConfiguration().getDouble("indiseas.specificity.phytoplankton.biomass");
        sd = getConfiguration().getArrayDouble("indiseas.specificity.random.sd");
        ndraw = getConfiguration().getInt("indiseas.specificity.random.ndraw");
        
        
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
            fw.write('\n');
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
        for (Map.Entry<String, String> argument : options.entrySet()) {
            txt.append(argument.getKey());
            txt.append(" = ");
            txt.append(argument.getValue());
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
            cmd.append("-D");
            cmd.append(argument.getKey());
            cmd.append("=");
            cmd.append(argument.getValue());
            cmd.append(" ");
        }
        // main Osmose configuration file
        cmd.append(osmoseMainCfg);
        // Send the log to the output folder
        cmd.append(" > ");
        cmd.append(options.get("output.dir.path"));
        cmd.append(File.separator);
        cmd.append("osmose.log");
        return cmd.toString();
    }
    
    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }
    
}
