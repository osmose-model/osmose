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

    public static void updateAndWriteIndicators() {

        int year = getSimulation().getYear();
        int index = getSimulation().getIndexTime();
        int nStepsYear = getSimulation().getNbTimeStepsPerYear();
        int nStepsRecord = getSimulation().getRecordFrequency();
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
        if (getSimulation().outputClass0) {
            biomassTot = new double[nSpec];
            abundanceTot = new double[nSpec];
        }
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
                if (getSimulation().outputClass0) {
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

    /*
     * Writes mean size per species. It must come before writeBiomassAndAbundance
     * since the mean size is pondered by the abundance without juveniles.
     *
     */
    public static void writeMeanSizes(float time) {

        StringBuilder filename;

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            meanSize[i] = (float) (meanSize[i] / abundanceNoJuv[i]);
        }

        filename = new StringBuilder(getOsmose().outputFileNameTab[getOsmose().numSerie]);
        filename.append("_meanSize_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, meanSize, filename.toString());
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
            meanTL[i] = (float) (meanTL[i] / biomassNoJuv[i]);
        }

        filename = new StringBuilder(getOsmose().outputFileNameTab[getOsmose().numSerie]);
        filename.append("_meanTL_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, meanTL, filename.toString());
    }

    private static void writeVariable(float time, double[] variable, String filename) {
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie] + "/new");
        path.mkdirs();
        File file = new File(path, filename);
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
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

    public static void writeBiomassAndAbundance(float time) {

        StringBuilder filename;

        double nsteps = getSimulation().getRecordFrequency();
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            if (getSimulation().outputClass0) {
                abundanceTot[i] = Math.floor(abundanceTot[i] / nsteps);
                biomassTot[i] /= nsteps;
            }
            abundanceNoJuv[i] = Math.floor(abundanceNoJuv[i] / nsteps);
            biomassNoJuv[i] /= nsteps;
        }

        filename = new StringBuilder(getOsmose().outputFileNameTab[getOsmose().numSerie]);
        filename.append("_abundance_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, abundanceNoJuv, filename.toString());

        filename = new StringBuilder(getOsmose().outputFileNameTab[getOsmose().numSerie]);
        filename.append("_biomass_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, biomassNoJuv, filename.toString());

        if (getSimulation().outputClass0) {
            filename = new StringBuilder(getOsmose().outputFileNameTab[getOsmose().numSerie]);
            filename.append("_abundanceClass0_Simu");
            filename.append(getOsmose().numSimu);
            filename.append(".csv");
            writeVariable(time, abundanceTot, filename.toString());

            filename = new StringBuilder(getOsmose().outputFileNameTab[getOsmose().numSerie]);
            filename.append("_biomassClass0_Simu");
            filename.append(getOsmose().numSimu);
            filename.append(".csv");
            writeVariable(time, biomassTot, filename.toString());
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
