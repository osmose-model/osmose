package fr.ird.osmose;

/**
 * *****************************************************************************
 * <p>Titre : Osmose </p>
 *
 * <p>Description : Main class of the Osmose model - reads the input files and
 * initialize the series and simulations - save the biomass file in case of
 * calibration - run the simulations </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * ******************************************************************************
 */
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
    
    public void init(String[] args) {

        // setup the logger
        logger.setUseParentHandlers(false);
        OsmoseLogFormatter formatter = new OsmoseLogFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
        
        readArgs(args);
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
    
    public void preLoad() {
        configuration = new Configuration(configurationFiles.get(0), outputPathName);
        configuration.init();
        
        simulation = new Simulation[configuration.getNSimulation()];
        for (int i = 0; i < configuration.getNSimulation(); i++) {
            simulation[i] = new Simulation(i);
        }
    }
    
    private void run() {
        for (String configurationFile : configurationFiles) {
            logger.info("Running configuration " + configurationFile);
            osmose.run(configurationFile);
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
        int nProcs = Runtime.getRuntime().availableProcessors();
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
        System.err.println("*****************************************");
        System.err.println("*   Osmose v3.0b - Copyright 2013 IRD   *");
        System.err.println("*****************************************");
        System.err.println(new Date());
        System.err.println();
        osmose.init(args);
        osmose.run();
        System.err.println();
        System.err.println(new Date());
        System.err.println("*   Osmose v3.0b - Exit");
        System.err.println("*****************************************");
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
