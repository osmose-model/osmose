/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.SimulationLinker;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class Indicators extends SimulationLinker {

    // List of the indicators
    private static List<Indicator> indicators;

    public static void init() {
        indicators = new ArrayList();

        // Biomass
        indicators.add(new BiomassNoJuvIndicator());
        indicators.add(new BiomassTotIndicator());
        // Abundance
        indicators.add(new AbundanceNoJuvIndicator());
        indicators.add(new AbundanceTotIndicator());
        // Mortality
        indicators.add(new MortalityIndicator());
        // Yield
        indicators.add(new YieldIndicator());
        indicators.add(new YieldNIndicator());
        // Size
        indicators.add(new MeanSizeIndicator());
        indicators.add(new MeanSizeCatchIndicator());
        indicators.add(new SizeSpectrumIndicator());
        indicators.add(new SizeSpectrumSpeciesIndicator());
        // TL
        indicators.add(new MeanTrophicLevelIndicator());
        indicators.add(new MeanTrophicLevelCatchIndicator());
        indicators.add(new TrophicLevelSpectrumIndicator());
        // Predation
        indicators.add(new DietIndicator());
        indicators.add(new PredatorPressureIndicator());
        // Spatialized
        indicators.add(new SpatialIndicator());
        indicators.add(new LTLIndicator());
        // Temporary indicator for calib that will be deleted soon
        indicators.add(new BiomassCalibrationIndicator());

        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.init();
                indicator.reset();
            }
        }
    }

    public static void close() {
        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.close();
            }
        }
    }

    public static void initStep() {
        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.initStep();
            }
        }
    }

    public static void updateAndWriteIndicators() {

        int year = getSimulation().getYear();
        int index = getSimulation().getIndexTimeYear();
        int nStepsYear = getOsmose().getNumberTimeStepsPerYear();
        int nStepsRecord = getOsmose().savingDtMatrix;
        //
        // UPDATE
        if (year >= getOsmose().timeSeriesStart) {
            for (Indicator indicator : indicators) {
                if (indicator.isEnabled()) {
                    indicator.update();
                    if (((index + 1) % nStepsRecord) == 0) {
                        float time = getSimulation().getYear() + (getSimulation().getIndexTimeYear() + 1f) / (float) nStepsYear;
                        indicator.write(time);
                        indicator.reset();
                    }
                }
            }
        }
    }

    public static void writeVariable(float time, double[] variable, String filename, String description) {
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab);
        File file = new File(path, filename);
        file.getParentFile().mkdirs();
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                pr.print("\"");
                pr.print(description);
                pr.println("\"");
                pr.print("Time");
                for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
                    pr.print(";");
                    pr.print(getSimulation().getSpecies(i).getName());
                }
                pr.println();
            }
            pr.print(time);
            for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
                pr.print(";");
                pr.print((float) variable[i]);
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
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab);
        File file = new File(path, filename);
        file.getParentFile().mkdirs();
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                if (null != description || !description.isEmpty()) {
                    pr.print("\"");
                    pr.print(description);
                    pr.println("\"");
                }
                pr.print("Time");
                for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
                    for (int j = 0; j < variable[i].length; j++) {
                        pr.print(";");
                        pr.print(getSimulation().getSpecies(i).getName());
                    }
                }
                pr.println();
                if (null != headers) {
                    pr.print("Headers");
                    for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
                        for (int j = 0; j < headers.length; j++) {
                            pr.print(";");
                            pr.print(headers[j]);
                        }
                    }
                }
                pr.println();
            }
            pr.print(time);
            for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
                for (int j = 0; j < variable[i].length; j++) {
                    pr.print(";");
                    pr.print((float) variable[i][j]);
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
}
