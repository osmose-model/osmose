/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.fishing;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.AbstractMortalityScenario;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class AnnualFSeasonScenario extends AbstractMortalityScenario {

    private float annualF;
    private float[] season;
    private int recruitmentAge;
    private float recruitmentSize;

    public AnnualFSeasonScenario(int iSimulation, Species species) {
        super(iSimulation, species);
    }

    @Override
    public void init() {
        int nStepYear = getConfiguration().getNStepYear();
        int iSpec = getIndexSpecies();
        annualF = getConfiguration().getFloat("mortality.fishing.rate.sp" + iSpec);
        if (!getConfiguration().isNull("mortality.fishing.recruitment.age.sp" + iSpec)) {
            float age = getConfiguration().getFloat("mortality.fishing.recruitment.age.sp" + iSpec);
            recruitmentAge = Math.round(age * nStepYear);
            recruitmentSize = 0.f;
        } else if (!getConfiguration().isNull("mortality.fishing.recruitment.size.sp" + iSpec)) {
            recruitmentSize = getConfiguration().getFloat("mortality.fishing.recruitment.size.sp" + iSpec);
            recruitmentAge = 0;
        } else {
            recruitmentAge = 0;
            recruitmentSize = 0.f;
            getLogger().log(Level.WARNING, "Could not find any fishing recruitment threshold (neither age nor size) for species {0}. Osmose assumes every school can be catched.", getSpecies().getName());
        }


        season = new float[nStepYear];
        String filename = getConfiguration().getFile("mortality.fishing.season.distrib.file.sp" + iSpec);
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();
            if ((lines.size() - 1) != nStepYear) {
                throw new IOException("Wrong number of time steps in file. Found " + (lines.size() - 1) + " and expected " + nStepYear);
            }
            for (int t = 0; t < nStepYear; t++) {
                season[t] = Float.valueOf(lines.get(t + 1)[1]);
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading fishing seasonality file " + filename, ex);
        }


    }

    @Override
    public float getInstantaneousRate(School school) {
        return (school.getAgeDt() >= recruitmentAge) && (school.getLength() >= recruitmentSize)
                ? annualF * season[getSimulation().getIndexTimeYear()]
                : 0.f;
    }

    @Override
    public float getAnnualRate() {
        return annualF;
    }
}
