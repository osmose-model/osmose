/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class Indicators {

    // Biomass
    private static double[] biomassTot;
    private static double[] biomassNoJuv;
    // Abundance
    private static double[] abundanceTot;
    private static double[] abundanceNoJuv;
    // Size
    private static double[] meanSize;
    // Trophic Level
    private static double[] meanTL;
    // Yields
    public static double[] yield, yieldN;

    public static void updateAndWriteIndicators() {

        int year = getSimulation().getYear();
        int index = getSimulation().getIndexTime();
        int nStepsYear = getSimulation().getNbTimeStepsPerYear();
        int nStepsRecord = getOsmose().savingDtMatrix[getOsmose().numSerie];
        //
        // UPDATE
        if (year >= getOsmose().timeSeriesStart) {
            // Biomass & abundance
            monitorBiomassAndAbundance();
            // Mean size
            if (getSimulation().meanSizeOutput) {
                monitorMeanSizes();
            }
            // Trophic level
            if (getSimulation().TLoutput) {
                monitorMeanTL();
            }
            //
            // WRITE
            if (((index + 1) % nStepsRecord) == 0) {
                float time = getSimulation().getYear() + (getSimulation().getIndexTime() + 1f) / (float) nStepsYear;
                // Mean size
                if (getSimulation().meanSizeOutput) {
                    writeMeanSizes(time);
                }
                // Trophic level
                if (getSimulation().TLoutput) {
                    writeMeanTL(time);
                }
                // Biomass & abundance
                writeBiomassAndAbundance(time);
                // Yields
                writeYields(time);
                //
                // RESET
                reset();
            }
        }
    }

    public static void reset() {
        int nSpec = getSimulation().getNbSpecies();
        // biomass & abundance
        biomassNoJuv = new double[nSpec];
        abundanceNoJuv = new double[nSpec];
        if (getSimulation().outputClass0 || getSimulation().calibration) {
            biomassTot = new double[nSpec];
            abundanceTot = new double[nSpec];
        }
        // yield
        yield = new double[nSpec];
        yieldN = new double[nSpec];
        // size
        if (getSimulation().meanSizeOutput) {
            meanSize = new double[nSpec];
        }
        // Trophic level
        if (getSimulation().TLoutput) {
            meanTL = new double[nSpec];
        }
    }

    public static void monitorBiomassAndAbundance() {

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            for (int j = 0; j < species.getNumberCohorts(); j++) {
                if (getSimulation().outputClass0 || getSimulation().calibration) {
                    biomassTot[i] += species.getCohort(j).getBiomass();
                    abundanceTot[i] += species.getCohort(j).getAbundance();
                }
                if (j >= species.indexAgeClass0) {
                    biomassNoJuv[i] += species.getCohort(j).getBiomass();
                    abundanceNoJuv[i] += species.getCohort(j).getAbundance();
                }
            }
        }
    }

    public static void monitorMeanSizes() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            double abundance = 0.d;
            for (int j = species.indexAgeClass0; j < species.getNumberCohorts(); j++) {
                abundance += species.getCohort(j).getAbundance();
            }
            meanSize[i] += species.meanSizeSpe * abundance;
        }

    }
    
    public static void writeYields(float time) {
        
        StringBuilder filename;
        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_yield_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        String description = "cumulative catch (tons per time step of saving). ex: if time step of saving is the year, then annual catches are saved";
        writeVariable(time, yield, filename.toString(), description);        
        
        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_yieldN_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "cumulative catch (number of fish caught per time step of saving). ex: if time step of saving is the year, then annual catches in fish numbers are saved";
        writeVariable(time, yieldN, filename.toString(), description);    
    }

    /*
     * Writes mean size per species. It must come before
     * writeBiomassAndAbundance since the mean size is pondered by the abundance
     * without juveniles.
     *
     */
    public static void writeMeanSizes(float time) {

        StringBuilder filename;

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            if (abundanceNoJuv[i] != 0) {
                meanSize[i] = (float) (meanSize[i] / abundanceNoJuv[i]);
            } else {
                meanSize[i] = 0.f;
            }
        }

        filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanSize_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, meanSize, filename.toString(), "Mean size of fish species in cm, weighted by fish numbers, and including/excluding first ages specified in input (in calibration file)");
    }

    public static void monitorMeanTL() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            double biomass = 0.d;
            for (int j = species.indexAgeClass0; j < species.getNumberCohorts(); j++) {
                biomass += species.getCohort(j).getBiomass();
            }
            meanTL[i] += species.meanTLSpe * biomass;
        }
    }

    /*
     * Writes mean TL per species. It must come before writeBiomassAndAbundance
     * since the mean size is pondered by the biomass without juveniles.
     *
     */
    public static void writeMeanTL(float time) {

        StringBuilder filename;

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            if (biomassNoJuv[i] != 0.d) {
                meanTL[i] = (float) (meanTL[i] / biomassNoJuv[i]);
            } else {
                meanTL[i] = 0.f;
            }
        }

        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanTL_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, meanTL, filename.toString(), "Mean Trophic Level of fish species, weighted by fish biomass, and including/excluding first ages specified in input (in calibration file)");
    }

    public static void writeVariable(float time, double[] variable, String filename, String description) {
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        path.mkdirs();
        File file = new File(path, filename);
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                pr.print("// ");
                pr.println(description);
                pr.print("Time");
                for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
                    pr.print(";");
                    pr.print(getSimulation().getSpecies(i).getName());
                }
                pr.println();
            }
            pr.print(time);
            for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
                pr.print(";");
                pr.print(variable[i]);
                //pr.print((long) variable[i]);
                //System.out.println(filename + " " + time + " spec" + i + " " + variable[i]);
            }
            pr.println();
            pr.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Indicators.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(Indicators.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void writeVariable(float time, double[][] variable, String filename, String[] headers, String description) {
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        File file = new File(path, filename);
        file.getParentFile().mkdirs();
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                if (null != description || !description.isEmpty()) {
                    pr.print("// ");
                    pr.println(description);
                }
                pr.print("Time");
                for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
                    for (int j = 0; j < variable[i].length; j++) {
                        pr.print(";");
                        pr.print(getSimulation().getSpecies(i).getName());
                    }
                }
                pr.println();
                if (null != headers) {
                    pr.print("Headers");
                    for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
                        for (int j = 0; j < headers.length; j++) {
                            pr.print(";");
                            pr.print(headers[j]);
                        }
                    }
                }
                pr.println();
            }
            pr.print(time);
            for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
                for (int j = 0; j < variable[i].length; j++) {
                    pr.print(";");
                    pr.print(variable[i][j]);
                    //pr.print((long) variable[i][j]);
                    //System.out.println(filename + " " + time + " spec" + i + " " + variable[i]);
                }
            }
            pr.println();
            pr.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Indicators.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(Indicators.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void writeBiomassAndAbundance(float time) {

        StringBuilder filename;

        double nsteps = getOsmose().savingDtMatrix[getOsmose().numSerie];
        int year = getSimulation().getYear();
        int indexSaving = (int) (getSimulation().getIndexTime() / nsteps);
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            if (getSimulation().outputClass0 || getSimulation().calibration) {
                abundanceTot[i] = Math.floor(abundanceTot[i] / nsteps);
                biomassTot[i] /= nsteps;
            }
            abundanceNoJuv[i] = Math.floor(abundanceNoJuv[i] / nsteps);
            biomassNoJuv[i] /= nsteps;
            if (getSimulation().calibration) {
                getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassNoJuv[i];
                getOsmose().BIOMQuadri[getOsmose().numSimu][i][1][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassTot[i];
            }
        }

        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_biomass_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, biomassNoJuv, filename.toString(), "Mean biomass (tons), excluding first ages specified in input (typically in calibration file)");

        if (getSimulation().outputClass0) {

            filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
            filename.append("_biomass-total_Simu");
            filename.append(getOsmose().numSimu);
            filename.append(".csv");
            writeVariable(time, biomassTot, filename.toString(), "Mean biomass (tons), including first ages specified in input (typically in calibration file)");
        }
    }

///////////////////////////
    // UTIL
    public static IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }

    public static Simulation getSimulation() {
        return Osmose.getInstance().getSimulation();
    }
}
