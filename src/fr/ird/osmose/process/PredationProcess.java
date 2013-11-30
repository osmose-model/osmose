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
package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Prey;
import fr.ird.osmose.Prey.MortalityCause;
import fr.ird.osmose.School;
import fr.ird.osmose.stage.AccessibilityStage;
import fr.ird.osmose.stage.IStage;
import fr.ird.osmose.stage.PredPreyStage;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author pverley
 */
public class PredationProcess extends AbstractProcess {

    private float[][] predPreySizesMax, predPreySizesMin;
    private float[] predationRate;
    /**
     * Accessibility matrix.
     * Array[nSpecies+nPlankton][nAccessStage][nSpecies][nAccessStage]
     */
    private float[][][][] accessibilityMatrix;
    /*
     * Accessibility stages
     */
    private IStage accessStage;
    /*
     * Feeding stages
     */
    private IStage predPreyStage;

    public PredationProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nspec = getNSpecies();
        int nPlankton = getConfiguration().getNPlankton();
        predPreySizesMax = new float[nspec][];
        predPreySizesMin = new float[nspec][];
        predationRate = new float[nspec];

        for (int i = 0; i < nspec; i++) {
            predPreySizesMax[i] = getConfiguration().getArrayFloat("predation.predPrey.sizeRatio.max.sp" + i);
            predPreySizesMin[i] = getConfiguration().getArrayFloat("predation.predPrey.sizeRatio.min.sp" + i);
            predationRate[i] = getConfiguration().getFloat("predation.ingestion.rate.max.sp" + i);
        }

        // Accessibility stages
        accessStage = new AccessibilityStage();
        accessStage.init();

        // Feeding stages
        predPreyStage = new PredPreyStage();
        predPreyStage.init();

        // accessibility matrix
        if (!getConfiguration().isNull("predation.accessibility.file")) {
            String filename = getConfiguration().getFile("predation.accessibility.file");
            try {
                CSVReader reader = new CSVReader(new FileReader(filename), ';', CSVWriter.NO_QUOTE_CHARACTER);
                List<String[]> lines = reader.readAll();
                int l = 1;
                accessibilityMatrix = new float[nspec + nPlankton][][][];
                for (int i = 0; i < nspec + nPlankton; i++) {
                    int nStagePrey = accessStage.getNStage(i);
                    accessibilityMatrix[i] = new float[nStagePrey][][];
                    for (int j = 0; j < nStagePrey; j++) {
                        String[] line = lines.get(l);
                        int ll = 1;
                        accessibilityMatrix[i][j] = new float[nspec][];
                        for (int k = 0; k < nspec; k++) {
                            int nStagePred = accessStage.getNStage(k);
                            accessibilityMatrix[i][j][k] = new float[nStagePred];
                            for (int m = 0; m < nStagePred; m++) {
                                accessibilityMatrix[i][j][k][m] = Float.valueOf(line[ll]);
                                ll++;
                            }
                        }
                        l++;
                    }
                }
                reader.close();
            } catch (IOException ex) {
                getSimulation().error("Error loading accessibility matrix from file " + filename, ex);
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
                Collections.shuffle(schools);
                int ns = schools.size();
                double[] preyedBiomass = new double[ns];
                // Compute predation
                for (int ipred = 0; ipred < ns; ipred++) {
                    School predator = schools.get(ipred);
                    double[] preyUpon = computePredation(predator, true, 1);
                    for (int iprey = 0; iprey < ns; iprey++) {
                        if (iprey < ns) {
                            School prey = schools.get(iprey);
                            prey.incrementNdead(MortalityCause.PREDATION, prey.biom2abd(preyUpon[iprey]));
                        }
                    }
                    preyedBiomass[ipred] = sum(preyUpon);
                }
                // Apply predation mortality
                for (int is = 0; is < ns; is++) {
                    School school = schools.get(is);
                    double biomassToPredate = school.getBiomass() * getMaxPredationRate(school);
                    school.setPredSuccessRate(computePredSuccessRate(biomassToPredate, preyedBiomass[is]));
                }
            }
        }

    }

    /**
     * Computes the biomass preyed by predator upon the list of preys. The
     * function considers instantaneous biomass for both preys and predator.
     *
     * @param predator
     * @param preys
     * @param accessibility
     * @param subdt
     * @return the array of biomass preyed by the predator upon the preys
     */
    public double[] computePredation(School predator, List<Prey> preys, double[] accessibility, int subdt) {

        double[] preyUpon = new double[preys.size()];
        // egg do not predate
        if (predator.getAgeDt() > 0) {
            // Compute accessible biomass
            // 1. from preys
            double[] accessibleBiomass = new double[preys.size()];
            for (int i = 0; i < preys.size(); i++) {
                accessibleBiomass[i] = accessibility[i] * preys.get(i).getInstantaneousBiomass();
            }
            double biomAccessibleTot = sum(accessibleBiomass);

            // Compute the potential biomass that predators could prey upon
            double biomassToPredate = getMaxPredationRate(predator) * predator.getInstantaneousBiomass() / subdt;

            // Distribute the predation over the preys
            if (biomAccessibleTot != 0) {
                // There is less prey available than the predator can
                // potentially prey upon. Predator will feed upon the total
                // accessible biomass
                if (biomAccessibleTot <= biomassToPredate) {
                    biomassToPredate = biomAccessibleTot;
                }

                // Assess the loss for the preys caused by this predator
                for (int i = 0; i < preys.size(); i++) {
                    // ratio of prey i (among available preys) preyed upon by predator
                    double ratio = accessibleBiomass[i] / biomAccessibleTot;
                    preyUpon[i] = ratio * biomassToPredate;
                }
            } else {
                // Case 2: there is no prey available
                // preyUpon[i] = 0; no need to do it since initialization already set it to zero
            }
        }
        // Return the array of biomass preyed by the predator
        return preyUpon;
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
            float tempAccess = accessibilityMatrix[getConfiguration().getNSpecies() + i][0][predator.getSpeciesIndex()][accessStage.getStage(predator)];
            biomAccessibleTot += percentPlankton[i] * tempAccess * getSimulation().getPlankton(i).getAccessibleBiomass(cell);
        }

        // Compute the potential biomass that predators could prey upon
        double biomassToPredate = instantaneous
                ? getMaxPredationRate(predator) * predator.getInstantaneousBiomass() / subdt
                : getMaxPredationRate(predator) * predator.getBiomass() / subdt;

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
                float tempAccess = accessibilityMatrix[getConfiguration().getNSpecies() + i][0][predator.getSpeciesIndex()][accessStage.getStage(predator)];
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
        int iStage = predPreyStage.getStage(predator);
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][iStage];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][iStage];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            if ((preySizeMin > getSimulation().getPlankton(i).getSizeMax()) || (preySizeMax < getSimulation().getPlankton(i).getSizeMin())) {
                percentPlankton[i] = 0.0f;
            } else {
                percentPlankton[i] = getSimulation().getPlankton(i).computePercent(preySizeMin, preySizeMax);
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
        int iStage = predPreyStage.getStage(predator);
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][iStage];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][iStage];
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
     * @return
     */
    public double getMaxPredationRate(School predator) {
        return predationRate[predator.getSpeciesIndex()] / (double) (getConfiguration().getNStepYear());
    }

    /*
     * Get the accessible biomass that predator can feed on prey
     */
    private double getAccessibility(School predator, School prey) {
        return accessibilityMatrix[prey.getSpeciesIndex()][accessStage.getStage(prey)][predator.getSpeciesIndex()][accessStage.getStage(predator)];
    }

    /**
     * Get the accessibility of a list of preys for a given predator. Zero means
     * that the prey is not accessible to this predator. Accessibility ranges
     * from zero to one.
     *
     * @param predator, the predator in a cell
     * @param preys a list of preys that are in the same cell that the predator
     * @return an array of accessibility of the preys to this predator.
     */
    public double[] getAccessibility(School predator, List<Prey> preys) {

        double[] accessibility = new double[preys.size()];
        int iSpecPred = predator.getSpeciesIndex();
        int iPredPreyStage = predPreyStage.getStage(predator);
        float preySizeMax = predator.getLength() / predPreySizesMax[iSpecPred][iPredPreyStage];
        float preySizeMin = predator.getLength() / predPreySizesMin[iSpecPred][iPredPreyStage];
        float[] percentPlankton = getPercentPlankton(predator);
        int iStagePred = accessStage.getStage(predator);

        for (int iPrey = 0; iPrey < preys.size(); iPrey++) {
            int iSpecPrey = preys.get(iPrey).getSpeciesIndex();
            int iStagePrey;
            // The prey is an other school
            if (preys.get(iPrey) instanceof School) {
                School prey = (School) preys.get(iPrey);
                if (prey.equals(predator)) {
                    continue;
                }
                if (prey.getLength() >= preySizeMin && prey.getLength() < preySizeMax) {
                    iStagePrey = accessStage.getStage(prey);
                    accessibility[iPrey] = accessibilityMatrix[iSpecPrey][iStagePrey][iSpecPred][iStagePred];
                }
                // else accessibility[iPrey] = 0.d; no need to do it since initialization already set it to zero
            } else {
                // The prey is a plankton group
                iStagePrey = 0;
                accessibility[iPrey] = accessibilityMatrix[iSpecPrey+getConfiguration().getNSpecies()][iStagePrey][iSpecPred][iStagePred]
                        * getSimulation().getPlankton(iSpecPrey).getAccessibility()
                        * percentPlankton[iSpecPrey];
            }
        }
        return accessibility;
    }
}
