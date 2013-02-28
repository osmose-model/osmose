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
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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
    
    /*
     * Function for dealing with command line arguments From David K. for the GA
     */
    public void init(String[] args) {
        
        String inputPathName, outputPathName, inputTxtName;
        
        // Get command line arguments
        if (args.length > 0) {
            inputPathName = args[0];
        } else {
            // This will not have trailing file separator - no idea if this is a problem
            inputPathName = readPathFile();
            System.out.println("Input path ==> " + inputPathName);
        }

        if (args.length > 1) {
            outputPathName = args[1];
        } else {
            outputPathName = inputPathName + fileSeparator + "output" + fileSeparator;
        }

        if (args.length > 2) {
            inputTxtName = args[2];
        } else {
            inputTxtName = "INPUT.txt";
        }
        
        configuration = new Configuration(inputPathName, outputPathName, inputTxtName);
        configuration.init();
    }

    public void run() {
        // Delete existing output directory
        File targetPath = new File(configuration.outputPathName + configuration.outputFileNameTab);
        if (targetPath.exists()) {
            IOTools.deleteDirectory(targetPath);
        }

        // Loop over the number of replica
        simulation = new Simulation[configuration.nbLoopTab];
        long begin = System.currentTimeMillis();
        System.out.println("\nSimulation started...");
        int nProcs = Runtime.getRuntime().availableProcessors();
        //int nProcs = 1;
        int nBatch = (int) Math.ceil((float) configuration.nbLoopTab / nProcs);
        int replica = 0;
        for (int iBatch = 0; iBatch < nBatch; iBatch++) {
            int nworker = Math.min(nProcs, configuration.nbLoopTab - replica);
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
        System.out.println("End of simulation [OK] (time ellapsed:  " + time + " seconds)");
    }

    private class Worker implements Runnable {

        private final Simulation simulation;
        private final int replica;
        private final CountDownLatch doneSignal;

        public Worker(Simulation simulation, CountDownLatch doneSignal) {
            this.simulation = simulation;
            this.doneSignal = doneSignal;
            replica = simulation.getReplica();
        }

        @Override
        public void run() {
            long begin = System.currentTimeMillis();
            try {
                System.out.println("  Simulation#" + (replica + 1) + " started...");
                simulation.init();
                simulation.run();
                int time = (int) ((System.currentTimeMillis() - begin) / 1000);
                System.out.println("  Simulation#" + (replica + 1) + " [OK] (time ellapsed:  " + time + " seconds)");
            } finally {
                doneSignal.countDown();
            }
        }
    }

    public Logger getLogger() {
        return Logger.getLogger(Osmose.class.getName());
    }

    public String readPathFile() // read the file situated within the source code directory
    {
        FileInputStream pathFile = null;
        try {
            pathFile = new FileInputStream(new File("filePath.txt"));
        } catch (FileNotFoundException ex) {
            System.err.println("initial path file doesn't exist");
        }

        Reader r = new BufferedReader(new InputStreamReader(pathFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        System.out.println("1. Reading the filePath.txt");
        try {
            st.nextToken();
            File inputFile = new File(st.sval);
            return inputFile.getAbsolutePath();
        } catch (IOException ex) {
            System.err.println("Reading error of path file");
            return null;
        }
    }

    /*
     * Point d'entrée du programme
     */
    public static void main(String... args) {
        System.out.println("*****************************************");
        System.out.println("*   Osmose v3.0b - Copyright 2013 IRD   *");
        System.out.println("*****************************************");
        System.out.println(new Date());
        System.out.println();
        osmose.init(args);
        osmose.run();
        System.out.println();
        System.out.println(new Date());
        System.out.println("*   Osmose v3.0b - Exit");
        System.out.println("*****************************************");
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
