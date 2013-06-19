/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.stage.AbstractStage;
import fr.ird.osmose.stage.DietOutputStage;
import java.io.File;

/**
 *
 * @author pverley
 */
public class BiomassDietStageIndicator extends AbstractIndicator {

    private int nColumns;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private double[][] biomassStage;
    
    private AbstractStage dietOutputStage;

    public BiomassDietStageIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation, keyEnabled);
    }

    @Override
    public void init() {
        
        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();

        nColumns = 0;
        // Sum-up diet stages
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            nColumns += dietOutputStage.getNStage(iSpec);
        }
        nColumns += getConfiguration().getNPlankton();
        
        super.init();
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getOutputPathname());
        filename.append(File.separatorChar);
        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_biomassPredPreyIni_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "\\ Biomass (tons) of preys at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)";
    }

    @Override
    String[] getHeaders() {

        int nSpec = getNSpecies();
        String[] headers = new String[nColumns];
        int k = 0;
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            String name = getSimulation().getSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStage = dietOutputStage.getNStage(iSpec);
            for (int s = 0; s < nStage; s++) {
                if (nStage == 1) {
                    headers[k] = name;    // Name predators
                } else {
                    if (s == 0) {
                        headers[k] = name + " < " + threshold[s];    // Name predators
                    } else {
                        headers[k] = name + " >=" + threshold[s - 1];    // Name predators
                    }
                }
                k++;
            }
        }

        for (int j = nSpec; j < (nSpec + getConfiguration().getNPlankton()); j++) {
            headers[k] = getSimulation().getPlankton(j - nSpec).getName();
            k++;
        }
        return headers;
    }

    @Override
    public void initStep() {
        for (School school : getSchoolSet().getPresentSchools()) {
            biomassStage[school.getSpeciesIndex()][dietOutputStage.getStage(school)] += school.getBiomass();
        }
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        for (int i = nSpec; i < nPrey; i++) {
            int iPlankton = i - nSpec;
            biomassStage[i][0] += getSimulation().getPlankton(iPlankton).getBiomass();
        }
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNPlankton();
        biomassStage = new double[nPrey][];
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            biomassStage[iSpec] = new double[dietOutputStage.getNStage(iSpec)];
        }
        for (int i = nSpec; i < nPrey; i++) {
            // we consider just 1 stage per plankton group
            biomassStage[i] = new double[1];
        }
    }

    @Override
    public void update() {
        // nothing to do
    }

    @Override
    public void write(float time) {
        double[] biomass = new double[nColumns];
        double nsteps = getRecordFrequency();
        int k = 0;
        int nSpec = getNSpecies();
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            for (int s = 0; s < dietOutputStage.getNStage(iSpec); s++) {
                biomass[k] = biomassStage[iSpec][s] / nsteps;
                k++;
            }
        }
        for (int j = nSpec; j < (nSpec + getConfiguration().getNPlankton()); j++) {
            biomass[k] = biomassStage[j][0] / nsteps;
            k++;
        }
        writeVariable(time, biomass);
    }
}
