/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class PredationProcess extends AbstractProcess {

    private float[][] predPreySizesMax, predPreySizesMin;
    private float[] predationRate;
    /**
     * Threshold age (year) between accessibility stages.
     * Array[nSpecies][nAccessStage]
     */
    private float[][] accessStageThreshold;
    /**
     * Threshold size (cm) of feeding stages. Array[nSpecies][nFeedingStage-1]
     */
    private float[][] predPreyStageThreshold;
    /**
     * Metrics used for splitting the stages (either age or size).
     */
    private String dietOutputMetrics;
    /**
     * Metrics used for splitting the stages (either age or size).
     */
    private String accessStageMetrics;
    /**
     * Threshold age (year) or size (cm) between the diet stages.
     */
    private float[][] dietOutputStageThreshold;
    /**
     * Accessibility matrix.
     * Array[nSpecies+nPlankton][nAccessStage][nSpecies][nAccessStage]
     */
    private float[][][][] accessibilityMatrix;
    /**
     * Whether school diet should be recorded
     */
    private boolean recordDiet;

    public PredationProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        int nspec = getNSpecies();
        int nPlankton = getConfiguration().getNPlankton();
        predPreySizesMax = new float[nspec][];
        predPreySizesMin = new float[nspec][];
        predationRate = new float[nspec];

        accessStageThreshold = new float[nspec + nPlankton][];
        predPreyStageThreshold = new float[nspec][];
        dietOutputStageThreshold = new float[nspec][];
        dietOutputMetrics = getConfiguration().getString("output.diet.stage.structure");
        accessStageMetrics = getConfiguration().getString("predation.accessibility.stage.structure");

        recordDiet = getConfiguration().getBoolean("output.diet.composition.enabled")
                || getConfiguration().getBoolean("output.diet.pressure.enabled");

        for (int i = 0; i < nspec; i++) {
            predPreySizesMax[i] = getConfiguration().getArrayFloat("predation.predPrey.sizeRatio.max.sp" + i);
            predPreySizesMin[i] = getConfiguration().getArrayFloat("predation.predPrey.sizeRatio.min.sp" + i);
            predationRate[i] = getConfiguration().getFloat("predation.ingestion.rate.max.sp" + i);

            // accessibility stage
            int nAccessStage = getConfiguration().canFind("predation.accessibility.stage.threshold.sp" + i)
                    ? getConfiguration().getArrayString("predation.accessibility.stage.threshold.sp" + i).length + 1
                    : 1;
            if (nAccessStage > 1) {
                accessStageThreshold[i] = getConfiguration().getArrayFloat("predation.accessibility.stage.threshold.sp" + i);
            } else {
                accessStageThreshold[i] = new float[0];
            }

            // predPrey stage
            int nPredPreyStage = getConfiguration().canFind("predation.predPrey.stage.threshold.sp" + i)
                    ? getConfiguration().getArrayString("predation.predPrey.stage.threshold.sp" + i).length + 1
                    : 1;
            if (nPredPreyStage > 1) {
                predPreyStageThreshold[i] = getConfiguration().getArrayFloat("predation.predPrey.stage.threshold.sp" + i);
            } else {
                predPreyStageThreshold[i] = new float[0];
            }

            // diet output stage
            int nDietOutputStage = getConfiguration().canFind("output.diet.stage.threshold.sp" + i)
                    ? getConfiguration().getArrayString("output.diet.stage.threshold.sp" + i).length + 1
                    : 1;
            if (nDietOutputStage > 1) {
                dietOutputStageThreshold[i] = getConfiguration().getArrayFloat("output.diet.stage.threshold.sp" + i);
            } else {
                dietOutputStageThreshold[i] = new float[0];
            }
        }
        for (int i = 0; i < nPlankton; i++) {
            accessStageThreshold[nspec + i] = new float[0];
        }

        // accessibility matrix
        if (getConfiguration().canFind("predation.accessibility.file")) {
            String filename = getConfiguration().getFile("predation.accessibility.file");
            try {
                CSVReader reader = new CSVReader(new FileReader(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
                List<String[]> lines = reader.readAll();
                int l = 1;
                accessibilityMatrix = new float[nspec + nPlankton][][][];
                for (int i = 0; i < nspec + nPlankton; i++) {
                    accessibilityMatrix[i] = new float[accessStageThreshold[i].length + 1][][];
                    for (int j = 0; j < accessStageThreshold[i].length + 1; j++) {
                        String[] line = lines.get(l);
                        int ll = 1;
                        accessibilityMatrix[i][j] = new float[nspec][];
                        for (int k = 0; k < nspec; k++) {
                            accessibilityMatrix[i][j][k] = new float[accessStageThreshold[k].length + 1];
                            for (int m = 0; m < accessStageThreshold[k].length + 1; m++) {
                                accessibilityMatrix[i][j][k][m] = Float.valueOf(line[ll]);
                                ll++;
                            }
                        }
                        l++;
                    }
                }
                reader.close();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Error loading accessibility matrix from file " + filename, ex);
            }
        } else {
            for (int i = 0; i < nspec; i++) {
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nspec][];
                for (int j = 0; j < nspec; j++) {
                    accessibilityMatrix[i][0][j] = new float[]{0.8f};
                }
            }
            for (int i = nspec; i < nspec + nPlankton; i++) {
                accessibilityMatrix[i] = new float[1][][];
                accessibilityMatrix[i][0] = new float[nspec][];
                for (int j = 0; j < nspec; j++) {
                    accessibilityMatrix[i][0][j] = new float[]{0.8f};
                }
            }
        }
    }

    @Override
    public void run() {
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getSchoolSet().getSchools(cell);
            if (!(cell.isLand() || schools.isEmpty())) {
                for (School school : schools) {
                    updateAccessibilityStage(school);
                    updatePredPreyStage(school);
                    updateDietOutputStage(school);
                }
                Collections.shuffle(schools);
                int ns = schools.size();
                double[] preyedBiomass = new double[ns];
                // Compute predation
                for (int ipred = 0; ipred < ns; ipred++) {
                    School predator = schools.get(ipred);
                    double[] preyUpon = computePredation(predator, School.INSTANTANEOUS_BIOMASS, 1);
                    for (int iprey = 0; iprey < ns; iprey++) {
                        if (iprey < ns) {
                            School prey = schools.get(iprey);
                            prey.incrementNdeadPredation(prey.biom2abd(preyUpon[iprey]));
                        }
                    }
                    preyedBiomass[ipred] = sum(preyUpon);
                }
                // Apply predation mortality
                for (int is = 0; is < ns; is++) {
                    School school = schools.get(is);
                    double biomassToPredate = school.getBiomass() * getPredationRate(school, 1);
                    school.predSuccessRate = computePredSuccessRate(biomassToPredate, preyedBiomass[is]);
                }
            }
        }

    }

    /**
     * Returns the matrix of predation for a given predator.
     *
     * @param predator
     * @param instantaneous, whether we should consider the instantaneous
     * biomass of the schools or the biomass at the beginning of the time step.
     * @param subdt, one by default
     * @return the matrix of predation
     */
    public double[] computePredation(School predator, boolean instantaneous, int subdt) {

        Cell cell = predator.getCell();
        List<School> schools = getSchoolSet().getSchools(predator.getCell());
        int nFish = schools.size();
        double[] preyUpon = new double[schools.size() + getConfiguration().getNPlankton()];
        // egg do not predate
        if (predator.getAgeDt() == 0) {
            return preyUpon;
        }
        // find the preys
        int[] indexPreys = findPreys(predator);

        // Compute accessible biomass
        // 1. from preys
        double[] accessibleBiomass = new double[indexPreys.length];
        for (int i = 0; i < indexPreys.length; i++) {
            School prey = schools.get(indexPreys[i]);
            accessibleBiomass[i] = (instantaneous)
                    ? getAccessibility(predator, prey) * prey.getInstantaneousBiomass()
                    : getAccessibility(predator, prey) * prey.getBiomass();
        }
        double biomAccessibleTot = sum(accessibleBiomass);
        // 2. from plankton
        float[] percentPlankton = getPercentPlankton(predator);
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            float tempAccess = accessibilityMatrix[getConfiguration().getNSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
            biomAccessibleTot += percentPlankton[i] * tempAccess * getSimulation().getPlankton(i).getAccessibleBiomass(cell);
        }

        // Compute the potential biomass that predators could prey upon
        double biomassToPredate = instantaneous
                ? getPredationRate(predator, subdt) * predator.getInstantaneousBiomass()
                : getPredationRate(predator, subdt) * predator.getBiomass();

        // Distribute the predation over the preys
        if (biomAccessibleTot != 0) {
            // There is less prey available than the predator can
            // potentially prey upon. Predator will feed upon the total
            // accessible biomass
            if (biomAccessibleTot <= biomassToPredate) {
                biomassToPredate = biomAccessibleTot;
            }

            // Assess the loss for the preys caused by this predator
            // Assess the gain for the predator from preys
            for (int i = 0; i < indexPreys.length; i++) {
                double ratio = accessibleBiomass[i] / biomAccessibleTot;
                preyUpon[indexPreys[i]] = ratio * biomassToPredate;
            }
            // Assess the gain for the predator from plankton
            // Assess the loss for the plankton caused by the predator
            for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
                float tempAccess = accessibilityMatrix[getConfiguration().getNSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
                double ratio = percentPlankton[i] * tempAccess * getSimulation().getPlankton(i).getAccessibleBiomass(cell) / biomAccessibleTot;
                preyUpon[nFish + i] = ratio * biomassToPredate;
            }

        } else {
            // Case 2: there is no prey available
            // No loss !
        }
        return preyUpon;
    }

    private double sum(double[] array) {
        double sum = 0.d;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    /**
     *
     * @param cell
     * @param instantaneous
     * @param subdt
     * @return
     */
    public double[][] computePredationMatrix(Cell cell, boolean instantaneous, int subdt) {

        List<School> schools = getSchoolSet().getSchools(cell);
        double[][] preyUpon = new double[schools.size() + getConfiguration().getNPlankton()][schools.size() + getConfiguration().getNPlankton()];
        // Loop over the schools of the cell
        for (int iPred = 0; iPred < schools.size(); iPred++) {
            preyUpon[iPred] = computePredation(schools.get(iPred), instantaneous, subdt);
        }
        return preyUpon;
    }

    /**
     * Compute the rate of predation success.
     *
     * @param biomassToPredate, the max biomass [ton] that a school can prey.
     * @param preyedBiomass, the biomass [ton] effectively preyed.
     * @return
     */
    public float computePredSuccessRate(double biomassToPredate, double preyedBiomass) {

        // Compute the predation success rate
        return Math.min((float) (preyedBiomass / biomassToPredate), 1.f);
    }

    private float[] getPercentPlankton(School predator) {
        float[] percentPlankton = new float[getConfiguration().getNPlankton()];
        int iPred = predator.getSpeciesIndex();
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][predator.getPredPreyStage()];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][predator.getPredPreyStage()];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            if ((preySizeMin > getSimulation().getPlankton(i).getSizeMax()) || (preySizeMax < getSimulation().getPlankton(i).getSizeMin())) {
                percentPlankton[i] = 0.0f;
            } else {
                percentPlankton[i] = getSimulation().getPlankton(i).calculPercent(preySizeMin, preySizeMax);
            }
        }
        return percentPlankton;
    }

    /**
     * Returns a list of preys for a given predator.
     *
     * @param predator
     * @return the list of preys for this predator
     */
    private int[] findPreys(School predator) {

        int iPred = predator.getSpeciesIndex();
        List<School> schoolsInCell = getSchoolSet().getSchools(predator.getCell());
        //schoolsInCell.remove(predator);
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][predator.getPredPreyStage()];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][predator.getPredPreyStage()];
        List<Integer> indexPreys = new ArrayList();
        for (int iPrey = 0; iPrey < schoolsInCell.size(); iPrey++) {
            School prey = schoolsInCell.get(iPrey);
            if (prey.equals(predator)) {
                continue;
            }
            if (prey.getLength() >= preySizeMin && prey.getLength() < preySizeMax) {
                indexPreys.add(iPrey);
            }
        }
        int[] index = new int[indexPreys.size()];
        for (int iPrey = 0; iPrey < indexPreys.size(); iPrey++) {
            index[iPrey] = indexPreys.get(iPrey);
        }
        return index;
    }

    /**
     * Gets the maximum predation rate of a predator per time step
     *
     * @param predator
     * @param subdt
     * @return
     */
    public double getPredationRate(School predator, int subdt) {
        return predationRate[predator.getSpeciesIndex()] / (double) (getConfiguration().getNStepYear() * subdt);
    }

    /*
     * Get the accessible biomass that predator can feed on prey
     */
    private double getAccessibility(School predator, School prey) {
        return accessibilityMatrix[prey.getSpeciesIndex()][prey.getAccessibilityStage()][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
    }

    void updateAccessibilityStage(School school) {

        int iSpec = school.getSpeciesIndex();
        if (accessStageMetrics.equalsIgnoreCase("size")) {
            for (int i = school.getAccessibilityStage(); i < accessStageThreshold[iSpec].length; i++) {
                if (school.getLength() >= accessStageThreshold[iSpec][i]) {
                    school.incrementAccessibilityStage();
                } else {
                    return;
                }
            }
        } else if (accessStageMetrics.equalsIgnoreCase("age")) {
            for (int i = school.getAccessibilityStage(); i < accessStageThreshold[iSpec].length; i++) {
                int tempAge = Math.round(accessStageThreshold[iSpec][i] * getConfiguration().getNStepYear());
                if (school.getAgeDt() >= tempAge) {
                    school.incrementAccessibilityStage();
                } else {
                    return;
                }
            }
        }
    }

    void updatePredPreyStage(School school) {

        int iSpec = school.getSpeciesIndex();
        for (int i = school.getPredPreyStage(); i < predPreyStageThreshold[iSpec].length; i++) {
            if (school.getLength() >= predPreyStageThreshold[iSpec][i]) {
                school.icrementPredPreyStage();
            } else {
                return;
            }
        }
    }

    void updateDietOutputStage(School school) {

        if (!recordDiet) {
            return;
        }

        int iSpec = school.getSpeciesIndex();
        if (dietOutputMetrics.equalsIgnoreCase("size")) {
            for (int i = school.getDietOutputStage(); i < dietOutputStageThreshold[iSpec].length; i++) {
                if (school.getLength() >= dietOutputStageThreshold[iSpec][i]) {
                    school.incrementDietOutputStage();
                } else {
                    return;
                }
            }
        } else if (dietOutputMetrics.equalsIgnoreCase("age")) {
            for (int i = school.getDietOutputStage(); i < dietOutputStageThreshold[iSpec].length; i++) {
                int tempAge = Math.round(dietOutputStageThreshold[iSpec][i] * getConfiguration().getNStepYear());
                if (school.getAgeDt() >= tempAge) {
                    school.incrementDietOutputStage();
                } else {
                    return;
                }
            }
        }
    }
}
