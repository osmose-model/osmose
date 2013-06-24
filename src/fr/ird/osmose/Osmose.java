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
package fr.ird.osmose;

import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.util.OsmoseLogFormatter;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Osmose {

    /*
     * Static instance of Osmose
     */
    private static Osmose osmose = new Osmose();
    public final String fileSeparator = System.getProperty("file.separator");
    /*
     * Sets of simulations
     */
    private Simulation[] simulation;
    /*
     * Configuration
     */
    private Configuration configuration;
    private String outputPathName;
    private List<String> configurationFiles;
    /**
     * The application logger
     */
    final private static Logger logger = Logger.getLogger(Osmose.class.getName());

    private void setupLogger() {

        // setup the logger
        logger.setUseParentHandlers(false);
        OsmoseLogFormatter formatter = new OsmoseLogFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
    }

    public void readArgs(String[] args) {

        configurationFiles = new ArrayList();

        // Get command line arguments
        if (args.length > 0) {
            configurationFiles.add(new File(args[0]).getAbsolutePath());
        } else {
            // This will not have trailing file separator - no idea if this is a problem
            configurationFiles.addAll(readFilepath());
            //logger.log(Level.INFO, "Main configuration file: {0}", mainFilename);
        }

        if (args.length > 1) {
            outputPathName = args[1];
            if (!outputPathName.endsWith(fileSeparator)) {
                outputPathName += fileSeparator;
            }
        }
    }

    public void init() {
        configuration = new Configuration(configurationFiles.get(0), outputPathName);
        configuration.init();

        simulation = new Simulation[configuration.getNSimulation()];
        for (int i = 0; i < configuration.getNSimulation(); i++) {
            simulation[i] = new Simulation(i);
        }
    }

    private void run() {
        for (String configurationFile : configurationFiles) {
            logger.log(Level.INFO, "Running configuration {0}", configurationFile);
            osmose.run(configurationFile);
            logger.info("*****************************************");
        }
    }

    private void run(String configurationFile) {

        // Initialize the configuration
        configuration = new Configuration(configurationFile, outputPathName);
        configuration.init();

        simulation = new Simulation[configuration.getNSimulation()];
        for (int i = 0; i < configuration.getNSimulation(); i++) {
            simulation[i] = new Simulation(i);
        }

        // Loop over the number of replica
        long begin = System.currentTimeMillis();
        logger.info("Simulation started...");
        int nProcs = Math.min(configuration.getNCpu(), Runtime.getRuntime().availableProcessors());
        //int nProcs = 1;
        int nBatch = (int) Math.ceil((float) configuration.getNSimulation() / nProcs);
        int indexSimulation = 0;
        for (int iBatch = 0; iBatch < nBatch; iBatch++) {
            int nworker = Math.min(nProcs, configuration.getNSimulation() - indexSimulation);
            CountDownLatch doneSignal = new CountDownLatch(nworker);
            Worker[] workers = new Worker[nworker];
            for (int iworker = 0; iworker < nworker; iworker++) {
                workers[iworker] = new Worker(simulation[indexSimulation], doneSignal);
                indexSimulation++;
            }
            for (int iworker = 0; iworker < nworker; iworker++) {
                new Thread(workers[iworker]).start();
            }
            try {
                doneSignal.await();
            } catch (InterruptedException ex) {
                Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (int iworker = 0; iworker < nworker; iworker++) {
                simulation[iBatch * nProcs + iworker] = null;
            }
        }
        int time = (int) ((System.currentTimeMillis() - begin) / 1000);
        logger.log(Level.INFO, "Simulation completed (time ellapsed:  {0} seconds)", time);
    }

    private class Worker implements Runnable {

        private final Simulation simulation;
        private final CountDownLatch doneSignal;

        public Worker(Simulation simulation, CountDownLatch doneSignal) {
            this.simulation = simulation;
            this.doneSignal = doneSignal;
        }

        @Override
        public void run() {
            long begin = System.currentTimeMillis();
            try {
                simulation.getLogger().info("Started...");
                simulation.init();
                simulation.run();
                int time = (int) ((System.currentTimeMillis() - begin) / 1000);
                simulation.getLogger().log(Level.INFO, "Completed (time ellapsed:  {0} seconds)", time);
            } finally {
                doneSignal.countDown();
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public List<String> readFilepath() {

        // Look for filepath.txt
        String[] names = new String[]{"filePath.txt", "FilePath.txt", "Filepath.txt", "filepath.txt"};
        String filename = null;
        for (String name : names) {
            if (new File(name).exists()) {
                filename = name;
                break;
            }
        }

        FileInputStream filepath = null;
        try {
            filepath = new FileInputStream(new File(filename));
        } catch (FileNotFoundException ex) {
            String wd = new File("").getAbsolutePath();
            logger.log(Level.SEVERE, "Did not find either {filePath.txt|FilePath.txt|Filepath.txt|filepath.txt} in current directory " + wd, ex);
            System.exit(1);
        }

        logger.info("Reading filePath.txt");
        BufferedReader bfIn = new BufferedReader(new InputStreamReader(filepath));
        String line;
        List<String> cfgFiles = new ArrayList();
        try {
            while ((line = bfIn.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") & !line.startsWith("//") & !(line.length() < 1)) {
                    String file = line;
                    if (line.startsWith(";") & line.endsWith(";")) {
                        file = line.substring(1, line.length() - 1);
                    }
                    cfgFiles.add(new File(file).getAbsolutePath());
                }
            }
            bfIn.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error reading " + filename, ex);
        }
        return cfgFiles;
    }

    /*
     * Point d'entrée du programme
     */
    public static void main(String... args) {
        osmose.setupLogger();
        logger.info("*****************************************");
        logger.info("*   Osmose v3.0b - Copyright 2013 IRD   *");
        logger.info("*****************************************");
        logger.info(new Date().toString());
        osmose.readArgs(args);
        osmose.run();
        logger.info(new Date().toString());
        logger.info("*   Osmose v3.0b - Exit");
        logger.info("*****************************************");
    }

    public static Osmose getInstance() {
        return osmose;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Simulation getSimulation(int replica) {
        return simulation[replica];
    }

    public IGrid getGrid() {
        return configuration.getGrid();
    }

    public LTLForcing getForcing() {
        return configuration.getForcing();
    }
}
