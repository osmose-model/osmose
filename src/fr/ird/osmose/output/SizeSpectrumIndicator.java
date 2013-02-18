/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class SizeSpectrumIndicator extends SchoolBasedIndicator {

    private double[][] sizeSpectrum;
    
    @Override
    public void init() {
        // Nothing to do
    }

    @Override
    public void reset() {
        sizeSpectrum = new double[getNSpecies()][getOsmose().tabSizes.length];
    }

    @Override
    public void update(School school) {
        sizeSpectrum[school.getSpeciesIndex()][getSizeRank(school)] += school.getAbundance();
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isSizeSpectrumOutput() || getOsmose().isSizeSpectrumSpeciesOutput();
    }

    private static int getSizeRank(School school) {

        int iSize = getOsmose().tabSizes.length - 1;
        if (school.getLength() <= getOsmose().spectrumMaxSize) {
            while (school.getLength() < getOsmose().tabSizes[iSize]) {
                iSize--;
            }
        }
        return iSize;
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab);

        if (getOsmose().isSizeSpectrumOutput()) {
            filename = new StringBuilder("SizeIndicators");
            filename.append(File.separatorChar);
            filename.append(getOsmose().outputPrefix);
            filename.append("_SizeSpectrum_Simu");
            filename.append(getSimulation().getReplica());
            filename.append(".csv");
            description = "Distribution of fish abundance in size classes (cm). For size class i, the number of fish in [i,i+1[ is reported. In logarithm, we consider the median of the size class, ie Ln(size [i]) = Ln((size [i]+size[i+1])/2)";
            // Write the file
            File file = new File(path, filename.toString());
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
                    pr.print(';');
                    pr.print("size");
                    pr.print(';');
                    pr.print("Abundance");
                    pr.print(';');
                    pr.print("LN(size)");
                    pr.print(';');
                    pr.print("LN(Abd)");
                    pr.print(';');
                    pr.println();
                }
                for (int iSize = 0; iSize < getOsmose().nbSizeClass; iSize++) {
                    double sum = 0f;
                    pr.print(time);
                    pr.print(';');
                    pr.print((getOsmose().tabSizes[iSize]));
                    pr.print(';');
                    for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                        sum += sizeSpectrum[iSpec][iSize] / getOsmose().getRecordFrequency();
                    }
                    pr.print((float) sum);
                    pr.print(';');
                    pr.print((getOsmose().tabSizesLn[iSize]));
                    pr.print(';');
                    pr.print((float) Math.log(sum));
                    pr.println();
                }
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

        if (getOsmose().isSizeSpectrumSpeciesOutput()) {
            filename = new StringBuilder("SizeIndicators");
            filename.append(File.separatorChar);
            filename.append(getOsmose().outputPrefix);
            filename.append("_SizeSpectrumSpecies_Simu");
            filename.append(getSimulation().getReplica());
            filename.append(".csv");
            description = "Distribution of fish species abundance in size classes (cm). For size class i, the number of fish in [i,i+1[ is reported.";
            // Write the file
            File file = new File(path, filename.toString());
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
                    pr.print(';');
                    pr.print("size");
                    pr.print(';');
                    for (int i = 0; i < getNSpecies(); i++) {
                        pr.print(getSimulation().getSpecies(i).getName());
                        pr.print(';');
                    }
                    pr.println();
                }
                for (int iSize = 0; iSize < getOsmose().nbSizeClass; iSize++) {
                    pr.print(time);
                    pr.print(';');
                    pr.print((getOsmose().tabSizes[iSize]));
                    pr.print(';');
                    for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                        pr.print((float) (sizeSpectrum[iSpec][iSize] / getOsmose().getRecordFrequency()));
                        pr.print(';');
                    }
                    pr.println();
                }
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
}
