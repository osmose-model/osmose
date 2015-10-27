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

import fr.ird.osmose.util.logging.OLogger;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.util.logging.StdoutHandler;
import fr.ird.osmose.util.version.VersionManager;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Level;
import ml.options.OptionData;
import ml.options.OptionSet;
import ml.options.Options;
import ml.options.Options.Multiplicity;
import ml.options.Options.Separator;

/**
 * This class is the entry point of the program. It contains the
 * {@code main(String... args)} method.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Osmose extends OLogger {

    /**
     * Static instance of Osmose.
     */
    private final static Osmose osmose = new Osmose();
    /**
     * Set of simulations with the same set of parameters (replicates).
     * Controlled by parameter <i>simulation.nsimu</i>
     */
    private Simulation[] simulation;
    /**
     * {@link Configuration} object that stores the current set of parameters.
     */
    private Configuration configuration;
    /**
     * List of configuration files.
     */
    private List<String> configurationFiles;
    /**
     * Set of command line options
     */
    private HashMap<String, String> cmd;
    /**
     * Whether to update the configuration files
     */
    private boolean updateConfiguration = false;
    /**
     * Run Indiseas simulation generator
     */
    private boolean flagIndiseas = false;

    /**
     * Read input arguments. If no argument are provided, Osmose assumes that it
     * will find in the current directory a file called <i>filepath.txt</i> that
     * provides the list of configuration files (refer to
     * {@link #readFilepath()}). The first argument is a path to a configuration
     * file. The second argument is the path for writing the output files. If
     * not provided Osmose assumes it will be given in the configuration file by
     * parameter <i>output.dir.path</i>. Providing input arguments to Osmose
     * allows you to run only one configuration. If you were to run several
     * configurations you should use the <i>filepath.txt</i>.
     *
     * @param args, arguments from {@link #main(java.lang.String...)}.
     */
    public void readArgs(String... args) {

        // Sets of command line options
        Options opt = new Options(args);
        // Set 1: Osmose configuration files are listed from a file
        opt.addSet("Usage1", 0)
                .addOption("F", Separator.BLANK)
                .addOption("update", Multiplicity.ZERO_OR_ONE);
        // Set 2: Osmose configuration files are given as arguments
        opt.addSet("Usage2", 1, Integer.MAX_VALUE)
                .addOption("update", Multiplicity.ZERO_OR_ONE);
        // Set 3: Set up Indiseas simulations
        opt.addSet("Usage3", 0)
                .addOption("indiseas", Separator.BLANK);
        // For all sets, user can specify how to resolve pathnames
        opt.addOptionAllSets("resolve", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        // For all sets, user can specify parameter values that will overwrite
        // the values defined in the configuration files
        opt.addOptionAllSets("P", true, Separator.EQUALS, Multiplicity.ZERO_OR_MORE);
        // For all sets, enable verbose output
        opt.addOptionAllSets("verbose", Multiplicity.ZERO_OR_ONE);
        // For all sets, enable quiet output (only error)
        opt.addOptionAllSets("quiet", Multiplicity.ZERO_OR_ONE);

        // Get the matching set and throw error if none found
        OptionSet set = opt.getMatchingSet(false, false);
        if (set == null) {
            info(getCmdUsage());
            error("Invalid command line usage.", new IllegalArgumentException(opt.getCheckErrors()));
        }

        configurationFiles = new ArrayList();

        // Usage1 & Usage2, OSMOSE
        if (set.getSetName().equals("Usage1")) {
            configurationFiles.addAll(readFilepath(set.getOption("F").getResultValue(0)));
        }

        if (set.getSetName().equals("Usage2")) {
            configurationFiles.addAll(set.getData());
        }

        // Option for updating configuration file, only Usage1 & Usage2
        if (set.getSetName().matches("Usage[12]")) {
            if (set.isSet("update")) {
                if (!set.isSet("P")) {
                    updateConfiguration = true;
                } else {
                    info(getCmdUsage());
                    error("Invalid command line options.", new IllegalArgumentException("-update and -P options are mutually exclusive."));
                }
            }
        }

        // Usage3, INDISEAS
        if (set.getSetName().equals("Usage3")) {
            configurationFiles.add(set.getOption("indiseas").getResultValue(0));
            flagIndiseas = true;
        }

        // Initialises the set of command line options
        cmd = new HashMap();

        // Resolve option -resolve=global|local
        if (set.isSet("resolve")) {
            String resolve = set.getOption("resolve").getResultValue(0);
            if (resolve.matches("^.*?(local|global).*$")) {
                cmd.put("resolve", resolve);
            } else {
                info(getCmdUsage());
                error("Invalid command line option -resolve=" + resolve, new IllegalArgumentException("-resolve=global or -resolve=local only"));
            }
        }

        // Parameters option -Pkey=value
        if (set.isSet("P")) {
            OptionData optParam = set.getOption("P");
            for (int i = 0; i < optParam.getResultCount(); i++) {
                String key = optParam.getResultDetail(i).toLowerCase();
                // Remove leading and trailing double quotes
                String value = optParam.getResultValue(i).replaceAll("^\"|\"$", "");
                cmd.put(key, value);
            }
        }

        // Verbose and quiet options are exclusive
        if (set.isSet("verbose") && set.isSet("quiet")) {
            info(getCmdUsage());
            error("Invalid command usage, -verbose and -quiet options are exclusive", new IllegalArgumentException("Osmose logging cannot be both verbose and quiet."));
        }
        if (set.isSet("verbose")) {
            getLogger().setLevel(Level.FINE);
        }
        if (set.isSet("quiet")) {
            getLogger().setLevel(Level.SEVERE);
        }
    }

    private void setupLogger() {
        Handler[] handlers = getLogger().getHandlers();
        for (Handler handler : handlers) {
            getLogger().removeHandler(handler);
        }
        getLogger().setUseParentHandlers(false);
        getLogger().addHandler(new StdoutHandler());
        getLogger().setLevel(Level.INFO);
    }

    /**
     * Reads the command line usage from resource file resources/cmd-usage.txt
     *
     * @return the command line usage as a String
     */
    private String getCmdUsage() {
        // Read usage from ressources/cmd-usage.txt
        StringBuilder usage = new StringBuilder();
        try {
            BufferedReader bfIn = new BufferedReader(new InputStreamReader(Osmose.class.getResourceAsStream("resources/cmd-usage.txt")));
            String line;
            while ((line = bfIn.readLine()) != null) {
                usage.append(line);
                usage.append("\n");
            }
        } catch (IOException ex) {
            warning("Failed to print command line usage.");
        }
        return usage.toString();
    }

    /**
     * Loads a configuration file and creates a new set of simulation. It is
     * only used externally by Osmose UI tools to access the parameters of
     * configuration.
     */
    public void init() {
        configuration = new Configuration(configurationFiles.get(0), cmd);
        configuration.init();

        simulation = new Simulation[configuration.getNSimulation()];
        for (int i = 0; i < configuration.getNSimulation(); i++) {
            simulation[i] = new Simulation(i);
        }
    }

    /**
     * Loops on the list of configuration files (listed in the
     * <i>filepath.txt</i>, refer to {@link #readFilepath()} for details) and
     * run the configurations in sequential order.
     */
    public void runAll() {

        if (flagIndiseas) {
            info("Creating Indiseas simulation batch from {0}", configurationFiles.get(0));
            osmose.indiseas();
            info("*********************************************");
        } else if (updateConfiguration) {
            for (String configurationFile : configurationFiles) {
                info("Updating configuration {0}", configurationFile);
                osmose.update(configurationFile);
                info("*********************************************");
            }
        } else {
            for (String configurationFile : configurationFiles) {
                info("Running configuration {0}", configurationFile);
                osmose.run(configurationFile);
                info("*********************************************");
            }
        }
    }

    public void indiseas() {
        configuration = new Configuration(configurationFiles.get(0), cmd);
        configuration.load();
        Indiseas indiseas = new Indiseas();
        indiseas.init();
        indiseas.run();
    }

    /**
     * Loads a configuration file and updates it if necessary.
     *
     * @param configurationFile, the path of the configuration file.
     */
    public void update(String configurationFile) {
        // Initialize the configuration
        configuration = new Configuration(configurationFile, cmd);
        if (!configuration.load()) {
            VersionManager.getInstance().updateConfiguration();
        }
    }

    /**
     * Run a specified configuration in multi-threads mode. It initialises a new
     * {@link Configuration} and launches concurrently batches of simulations.
     * The number of simulations is controlled by parameter
     * <i>simulation.nsimu</i>. The user can control how many CPUs are allocated
     * to running the simulations with parameter <i>simulation.ncpu</i>. If this
     * parameter is not provided in the configuration file or the value exceed
     * the number of available processors in the system, Osmose will set it
     * automatically to the number of available processors
     * ({@code Runtime.getRuntime().availableProcessors()}). If the number of
     * simulations exceed the number of processors allocated to running the
     * configuration, Osmose will launch sequentially batches of simulations,
     * each batch running concurrently as many simulations as the number of
     * allocated CPUs.
     *
     * @see java.util.concurrent.CountDownLatch
     *
     * @param configurationFile, the path of the configuration file.
     */
    public void run(String configurationFile) {

        // Initialize the configuration
        configuration = new Configuration(configurationFile, cmd);
        if (!configuration.load()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Your configuration file must be updated. Please run osmose with the -update option.\n");
            msg.append("Example: java -jar osmose.jar -update config.csv");
            error(msg.toString(), null);
        }
        configuration.init();

        // Disable logging in multithread environment
        Level lvl = getLogger().getLevel();
        if (configuration.getNCpu() > 1 && configuration.getNSimulation() > 1) {
            info("Logging is disabled in multithreading mode. Set ncpu or nsimulation to 1 for full logging.");
            getLogger().setLevel(Level.SEVERE);
        }

        simulation = new Simulation[configuration.getNSimulation()];
        for (int i = 0; i < configuration.getNSimulation(); i++) {
            simulation[i] = new Simulation(i);
        }

        // Loop over the number of replica
        long begin = System.currentTimeMillis();
        int nProcs = Math.min(configuration.getNCpu(), Runtime.getRuntime().availableProcessors());
        //int nProcs = 1;
        int nBatch = (int) Math.ceil((float) configuration.getNSimulation() / nProcs);
        int rank = 0;
        for (int iBatch = 0; iBatch < nBatch; iBatch++) {
            int nworker = Math.min(nProcs, configuration.getNSimulation() - rank);
            CountDownLatch doneSignal = new CountDownLatch(nworker);
            Worker[] workers = new Worker[nworker];
            for (int iworker = 0; iworker < nworker; iworker++) {
                workers[iworker] = new Worker(rank, doneSignal);
                rank++;
            }
            for (int iworker = 0; iworker < nworker; iworker++) {
                new Thread(workers[iworker]).start();
            }
            try {
                doneSignal.await();
            } catch (InterruptedException ex) {
                error("Simulation " + rank + " terminated unexpectedly.", ex);
            }
            for (int iworker = 0; iworker < nworker; iworker++) {
                simulation[iBatch * nProcs + iworker] = null;
            }
        }
        getLogger().setLevel(lvl);
        if (configuration.getNSimulation() > 1) {
            int time = (int) ((System.currentTimeMillis() - begin) / 1000);
            info("All simulations completed (time ellapsed:  {0} seconds)", time);
        }
    }

    /**
     * Inner class that initializes and runs one simulation in a dedicated
     * thread. It informs the {@link java.util.concurrent.CountDownLatch} when
     * the simulation has completed (with or without error).
     */
    private class Worker implements Runnable {

        /**
         * The rank of the simulation that will run in this dedicated thread.
         */
        private final int rank;
        /**
         * The {@link java.util.concurrent.CountDownLatch} that will wait for
         * this {@link Simulation} to complete before decrementing the count of
         * the latch.
         */
        private final CountDownLatch doneSignal;

        /**
         * Initializes a new {@code Worker} object for running the
         * {@code Simulation} given as an argument and decrements the
         * {@code CountDownLatch} given as an argument when the simulation has
         * completed.
         *
         * @param rank, the rank of the simulation to run in this thread
         * @param doneSignal, the {@link java.util.concurrent.CountDownLatch}
         * that will wait for {@code simulation} to complete before decrementing
         * the count of the latch.
         */
        public Worker(int rank, CountDownLatch doneSignal) {
            this.rank = rank;
            this.doneSignal = doneSignal;
        }

        /**
         * Initializes and runs the current {@code Simulation} object in this
         * thread and decrements the count of the latch when it is completed.
         */
        @Override
        public void run() {
            long begin = System.currentTimeMillis();
            try {
                info("Simulation {0} started...", rank);
                simulation[rank].init();
                simulation[rank].run();
                int time = (int) ((System.currentTimeMillis() - begin) / 1000);
                info("Simulation {0} completed (time ellapsed: {1} seconds)", new Object[]{rank, time});
                simulation[rank].destroy();
            } finally {
                doneSignal.countDown();
            }
        }
    }

    /**
     * Returns a list of configuration files listed from a text file. Lines
     * starting with # or // or empty lines are ignored. The function does not
     * check whether the paths to the configuration files are correct. It is
     * handled in the constructor of the {@link Configuration} object.
     *
     * @param filename, the file that contains the list of Osmose configuration
     * files
     * @return a list of configuration files.
     */
    public List<String> readFilepath(String filename) {

        FileInputStream filepath = null;
        try {
            filepath = new FileInputStream(new File(filename));
        } catch (FileNotFoundException ex) {
            String wd = new File("").getAbsolutePath();
            error("Did not find file " + filename + " in current directory " + wd, ex);
        }

        // Read filepath.txt
        info("Listing configuration files from " + filename);
        BufferedReader bfIn = new BufferedReader(new InputStreamReader(filepath));
        String line;
        List<String> cfgFiles = new ArrayList();
        try {
            while ((line = bfIn.readLine()) != null) {
                line = line.trim();
                // Ignore lines starting with # or // or empty lines
                if (!line.startsWith("#") & !line.startsWith("//") & !(line.length() <= 1)) {
                    String file = line;
                    if (line.startsWith(";") & line.endsWith(";")) {
                        file = line.substring(1, line.length() - 1);
                    }
                    cfgFiles.add(new File(file).getAbsolutePath());
                }
            }
            bfIn.close();
        } catch (IOException ex) {
            error("Error reading file " + filename, ex);
        }
        // return the list of configuration files
        return cfgFiles;
    }

    /**
     * Entry point of the program. Read input arguments and run the Osmose
     * configurations.
     *
     * @param args, the input arguments. Refer to
     * {@link #readArgs(java.lang.String...)} for details about accepted
     * arguments.
     */
    public static void main(String... args) {
        osmose.setupLogger();
        osmose.readArgs(args);
        osmose.info("*********************************************");
        osmose.info("OSMOSE - Modelling Marin Exploited Ecosystems");
        osmose.info("http://www.osmose-model.org");
        osmose.info("*********************************************");
        osmose.info("Software version: " + VersionManager.getInstance().OSMOSE_VERSION.toString());
        osmose.runAll();
        osmose.info("OSMOSE Model copyright © IRD");
        osmose.info("*********************************************");
    }

    /**
     *
     * @return
     */
    public static Osmose getInstance() {
        return osmose;
    }

    /**
     * Gets the current {@link Configuration} object.
     *
     * @return the current {@link Configuration} object.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Gets the {@link Simulation} object at specified rank.
     *
     * @param rank, the rank of the simulation, ranging from 0 to
     * <i>simulation.nsimu</i>
     * @return the {@link Simulation} object of rank {@code rank}
     */
    public Simulation getSimulation(int rank) {
        return simulation[rank];
    }

    /**
     * Gets the grid ({@link IGrid}) of the current Osmose configuration.
     *
     * @return the current grid of the configuration.
     */
    public IGrid getGrid() {
        return configuration.getGrid();
    }
}
