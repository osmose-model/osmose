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
import fr.ird.osmose.util.IOTools;
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
    private OldConfiguration oldConfiguration;
    /**
     * The application logger
     */
    final private static Logger logger = Logger.getLogger(Osmose.class.getName());

    /*
     * Function for dealing with command line arguments From David K. for the GA
     */
    public void init(String[] args) {
        
        // setup the logger
        logger.setUseParentHandlers(false);
        OsmoseLogFormatter formatter = new OsmoseLogFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);

        String inputPathName, outputPathName, inputTxtName;

        // Get command line arguments
        if (args.length > 0) {
            inputPathName = args[0];
        } else {
            // This will not have trailing file separator - no idea if this is a problem
            inputPathName = readPathFile();
            logger.log(Level.INFO, "Input path: {0}", inputPathName);
        }

        if (args.length > 1) {
            outputPathName = args[1];
            if (!outputPathName.endsWith(fileSeparator)) {
                outputPathName += fileSeparator;
            }
        } else {
            outputPathName = inputPathName + fileSeparator + "output" + fileSeparator;
        }

        if (args.length > 2) {
            inputTxtName = args[2];
        } else {
            inputTxtName = "INPUT.txt";
        }

        oldConfiguration = new OldConfiguration(inputPathName, outputPathName, inputTxtName);
        oldConfiguration.init();
    }

    public void run() {
        // Delete existing output directory
        File targetPath = new File(oldConfiguration.getOutputPathname() + oldConfiguration.getOutputFolder());
        if (targetPath.exists()) {
            IOTools.deleteDirectory(targetPath);
        }

        // Loop over the number of replica
        simulation = new Simulation[oldConfiguration.getNSimulation()];
        long begin = System.currentTimeMillis();
        logger.info("Simulation started...");
        int nProcs = Runtime.getRuntime().availableProcessors();
        //int nProcs = 1;
        int nBatch = (int) Math.ceil((float) oldConfiguration.getNSimulation() / nProcs);
        int replica = 0;
        for (int iBatch = 0; iBatch < nBatch; iBatch++) {
            int nworker = Math.min(nProcs, oldConfiguration.getNSimulation() - replica);
            CountDownLatch doneSignal = new CountDownLatch(nworker);
            Worker[] workers = new Worker[nworker];
            for (int iworker = 0; iworker < nworker; iworker++) {
                simulation[replica] = new Simulation(replica);
                workers[iworker] = new Worker(simulation[replica], doneSignal);
                replica++;
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

    public String readPathFile() // read the file situated within the source code directory
    {
        FileInputStream pathFile = null;
        try {
            pathFile = new FileInputStream(new File("filePath.txt"));
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "Initial path file doesn't exist", ex);
        }

        Reader r = new BufferedReader(new InputStreamReader(pathFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        logger.info("1. Reading the filePath.txt");
        try {
            st.nextToken();
            File inputFile = new File(st.sval);
            return inputFile.getAbsolutePath();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Reading error of path file", ex);
            return null;
        }
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

    public OldConfiguration getOldConfiguration() {
        return oldConfiguration;
    }

    public Simulation getSimulation(int replica) {
        return simulation[replica];
    }

    public IGrid getGrid() {
        return oldConfiguration.getGrid();
    }

    public LTLForcing getForcing() {
        return oldConfiguration.getForcing();
    }
}
