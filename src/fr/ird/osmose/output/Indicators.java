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

        indicators.add(new BiomassIndicator());
        indicators.add(new AbundanceIndicator());
        indicators.add(new MortalityIndicator());
        indicators.add(new YieldIndicator());
        indicators.add(new MeanSizeIndicator());
        indicators.add(new SizeSpectrumIndicator());
        indicators.add(new MeanTrophicLevelIndicator());
        indicators.add(new TrophicLevelSpectrumIndicator());
        indicators.add(new PredationIndicator());
        indicators.add(new SpatialIndicator());
        indicators.add(new BiomassCalibrationIndicator());
        indicators.add(new LTLIndicator());

        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.reset();
            }
        }
    }
    
    public static void initStep() {
        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.init();
            }
        }
    }

    public static void updateAndWriteIndicators() {

        int year = getSimulation().getYear();
        int index = getSimulation().getIndexTimeYear();
        int nStepsYear = getSimulation().getNumberTimeStepsPerYear();
        int nStepsRecord = getOsmose().savingDtMatrix[getOsmose().numSerie];
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
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
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
                for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
                    pr.print(";");
                    pr.print(getSimulation().getSpecies(i).getName());
                }
                pr.println();
            }
            pr.print(time);
            for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
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
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
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
                for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
                    for (int j = 0; j < variable[i].length; j++) {
                        pr.print(";");
                        pr.print(getSimulation().getSpecies(i).getName());
                    }
                }
                pr.println();
                if (null != headers) {
                    pr.print("Headers");
                    for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
                        for (int j = 0; j < headers.length; j++) {
                            pr.print(";");
                            pr.print(headers[j]);
                        }
                    }
                }
                pr.println();
            }
            pr.print(time);
            for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
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
