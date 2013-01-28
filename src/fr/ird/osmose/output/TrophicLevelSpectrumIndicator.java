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
public class TrophicLevelSpectrumIndicator extends AbstractIndicator {

    /*
     * Trophic level distribution [SPECIES][TL_SPECTRUM]
     */
    private double[][] trophicLevelSpectrum;
    
    @Override
    public void init() {
        // Nothing to do
    }

    @Override
    public void reset() {
        trophicLevelSpectrum = new double[getNSpecies()][getOsmose().tabTL.length];
    }

    @Override
    public void update(School school) {
        int ageClass1 = (int) Math.max(1, school.getSpecies().indexAgeClass0);
        if ((school.getBiomass() > 0) && (school.getAgeDt() >= ageClass1)) {
            trophicLevelSpectrum[school.getSpeciesIndex()][getTLRank(school)] += school.getBiomass();
        }
    }
    
    private static int getTLRank(School school) {

        int iTL = getOsmose().tabTL.length - 1;
        while (school.trophicLevel <= getOsmose().tabTL[iTL] && (iTL > 0)) {
            iTL--;
        }
        return iTL;
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isTLOutput();
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);

        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_TLDistrib_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Distribution of species biomass (tons) by 0.1 TL class, and excluding first ages specified in input (in calibration file)";
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
                pr.print("TL");
                pr.print(';');
                for (int i = 0; i < getNSpecies(); i++) {
                    pr.print(getSimulation().getSpecies(i).getName());
                    pr.print(';');
                }
                pr.println();
            }
            for (int iTL = 0; iTL < getOsmose().nbTLClass; iTL++) {
                pr.print(time);
                pr.print(';');
                pr.print((getOsmose().tabTL[iTL]));
                pr.print(';');
                for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                    pr.print((float) (trophicLevelSpectrum[iSpec][iTL] / getOsmose().getRecordFrequency()));
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
