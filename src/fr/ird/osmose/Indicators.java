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
    public static double[][] biomPerStage;
    // Abundance
    private static double[] abundanceTot;
    private static double[] abundanceNoJuv;
    // Size
    private static double[] meanSize;
    private static double[] meanSizeCatch;
    private static double[][] sizeSpectrum;
    // Trophic Level
    private static double[] meanTL;
    private static double[][] distribTL;
    public static double[] tabTLCatch;
    // Yields
    public static double[] yield, yieldN;
    // Diets
    private static double[][][][] diet, predatorPressure;
    private static double[][] nbStomachs;
    // Mortality
    private static double[][][] mortalityRates;
    
    public static void updateAndWriteIndicators() {
        
        int year = getSimulation().getYear();
        int index = getSimulation().getIndexTimeYear();
        int nStepsYear = getSimulation().getNbTimeStepsPerYear();
        int nStepsRecord = getOsmose().savingDtMatrix[getOsmose().numSerie];
        //
        // UPDATE
        if (year >= getOsmose().timeSeriesStart) {
            // Biomass & abundance
            monitorBiomassAndAbundance();
            // Yields
            monitorYields();
            // Mortality
            monitorMortality();
            // Mean size
            if (getOsmose().isMeanSizeOutput()) {
                monitorMeanSizes();
            }
            if (getOsmose().isSizeSpectrumOutput() || getOsmose().isSizeSpectrumSpeciesOutput()) {
                monitorSizeSpectrum();
            }
            // Trophic level
            if (getOsmose().isTLOutput()) {
                monitorMeanTL();
            }
            if (getOsmose().isTLDistribOutput()) {
                monitorTLDistribution();
            }
            // Diets
            if (getOsmose().isDietOuput()) {
                monitorPredation();
            }
            //
            // WRITE
            if (((index + 1) % nStepsRecord) == 0) {
                float time = getSimulation().getYear() + (getSimulation().getIndexTimeYear() + 1f) / (float) nStepsYear;
                // Mean size
                if (getOsmose().isMeanSizeOutput()) {
                    writeMeanSizes(time);
                }
                // Size spectrum
                if (getOsmose().isSizeSpectrumOutput()) {
                    writeSizeSpectrum(time);
                }
                // Trophic level
                if (getOsmose().isTLOutput()) {
                    writeMeanTL(time);
                }
                if (getOsmose().isTLDistribOutput()) {
                    writeTLDistribution(time);
                }
                // Diets
                if (getOsmose().isDietOuput()) {
                    writeDiet(time);
                    writePredatorPressure(time);
                }
                // Biomass & abundance
                writeBiomassAndAbundance(time);
                // Yields
                writeYields(time);
                // Mortality
                writeMortality(time);
                //
                // RESET
                reset();
            }
        }
    }
    
    public static void reset() {
        int nSpec = getSimulation().getNbSpecies();
        int nPrey = nSpec + getSimulation().getForcing().getNbPlanktonGroups();
        // biomass & abundance
        biomassNoJuv = new double[nSpec];
        abundanceNoJuv = new double[nSpec];
        if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
            biomassTot = new double[nSpec];
            abundanceTot = new double[nSpec];
        }
        biomPerStage = new double[nPrey][];
        for (int i = 0; i < nSpec; i++) {
            biomPerStage[i] = new double[getSimulation().getSpecies(i).nbDietStages];
        }
        for (int i = nSpec; i < nPrey; i++) {
            biomPerStage[i] = new double[1];
        }
        // yield
        yield = new double[nSpec];
        yieldN = new double[nSpec];
        // size
        if (getOsmose().isMeanSizeOutput()) {
            meanSize = new double[nSpec];
            meanSizeCatch = new double[nSpec];
        }
        if (getOsmose().isSizeSpectrumOutput() || getOsmose().isSizeSpectrumSpeciesOutput()) {
            sizeSpectrum = new double[nSpec][getOsmose().tabSizes.length];
        }
        // Trophic level
        if (getOsmose().isTLOutput()) {
            meanTL = new double[nSpec];
            distribTL = new double[nSpec][getOsmose().tabTL.length];
            tabTLCatch = new double[nSpec];
        }
        // Diets
        if (getOsmose().isDietOuput()) {
            diet = new double[nSpec][][][];
            predatorPressure = new double[nSpec][][][];
            nbStomachs = new double[nSpec][];
            for (int i = 0; i < nSpec; i++) {
                diet[i] = new double[getSimulation().getSpecies(i).nbDietStages][][];
                predatorPressure[i] = new double[getSimulation().getSpecies(i).nbDietStages][][];
                nbStomachs[i] = new double[getSimulation().getSpecies(i).nbDietStages];
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                    diet[i][s] = new double[nPrey][];
                    predatorPressure[i][s] = new double[nPrey][];
                    for (int ipr = 0; ipr < nPrey; ipr++) {
                        if (ipr < nSpec) {
                            diet[i][s][ipr] = new double[getSimulation().getSpecies(ipr).nbDietStages];
                            predatorPressure[i][s][ipr] = new double[getSimulation().getSpecies(ipr).nbDietStages];
                        } else {
                            diet[i][s][ipr] = new double[1];
                            predatorPressure[i][s][ipr] = new double[1];
                        }
                    }
                }
            }
        }

        // Mortality
        // [4] = mortalities ==> Predation / Starvation / Other / Fishing
        // [3] = stages ==> Eggs & larvae / Pre-recruits / Recruits
        mortalityRates = new double[nSpec][4][3];
    }
    
    public static void monitorMortality() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            for (int iDeath = 0; iDeath < 4; iDeath++) {
                for (int iStage = 0; iStage < 3; iStage++) {
                    mortalityRates[i][iDeath][iStage] += species.mortalityRate[iDeath][iStage];
                }
            }
        }
    }
    
    public static void writeMortality(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        int nSpec = getSimulation().getNbSpecies();
        
        for (int i = 0; i < nSpec; i++) {
            filename = new StringBuilder("Mortality");
            filename.append(File.separatorChar);
            filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
            filename.append("_mortalityRate_");
            filename.append(getSimulation().getSpecies(i).getName());
            filename.append("_Simu");
            filename.append(getOsmose().numSimu);
            filename.append(".csv");
            description = "Predation (Fpred), Starvation (Fstarv), Other Natural mortality (Fnat) & Fishing (Ffish) mortality rates per time step of saving. To get annual mortality rates, sum the mortality rates within one year.";
            // Write the file
            File file = new File(path, filename.toString());
            file.getParentFile().mkdirs();
            boolean isNew = !file.exists();
            try {
                fos = new FileOutputStream(file, true);
                pr = new PrintWriter(fos, true);
                if (isNew) {
                    pr.print("// ");
                    pr.println(description);
                    pr.print("Time");
                    pr.print(';');
                    pr.print("Fpred;Fpred;Fpred;");
                    pr.print("Fstarv;Fstarv;Fstarv;");
                    pr.print("Fnat;Fnat;Fnat;");
                    pr.println("Ffish;Ffish;Ffish");
                    pr.print(";");
                    for (int iDeath = 0; iDeath < 4; iDeath++) {
                        pr.print("Eggs&larvae;Pre-recruits;Recruits;");
                    }
                    pr.println();
                }
                pr.print(time);
                pr.print(";");
                for (int iDeath = 0; iDeath < 4; iDeath++) {
                    for (int iStage = 0; iStage < 3; iStage++) {
                        pr.print(mortalityRates[i][iDeath][iStage]);
                        pr.print(";");
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
    
    public static void monitorBiomassAndAbundance() {
        
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            for (int j = 0; j < species.getNumberCohorts(); j++) {
                if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
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
            meanSize[i] += species.meanSize * abundance;
            meanSizeCatch[i] += species.meanSizeCatch * species.yieldN;
        }
    }
    
    public static void monitorYields() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            yield[i] += species.yield;
            yieldN[i] += species.yieldN;
        }
    }
    
    public static void monitorSizeSpectrum() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            for (School school : getSimulation().getSpecies(i).getSchools()) {
                sizeSpectrum[i][getSizeRank(school)] += school.getAbundance();
            }
        }
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
    
    public static void monitorTLDistribution() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            for (School school : getSimulation().getSpecies(i).getSchools()) {
                int ageClass1 = (int) Math.max(1, school.getSpecies().supAgeOfClass0);
                if ((school.getBiomass() > 0) && (school.getAge() >= ageClass1)) {
                    distribTL[i][getTLRank(school)] += school.getBiomass();
                }
            }
        }
    }
    
    private static int getTLRank(School school) {
        
        int iTL = getOsmose().tabTL.length - 1;
        while (school.trophicLevel[school.getAge()] <= getOsmose().tabTL[iTL] && (iTL > 0)) {
            iTL--;
        }
        return iTL;
    }
    
    public static void monitorPredation() {
        for (School school : getSimulation().getSchools()) {
            int iSpec = school.getSpecies().getIndex();
            nbStomachs[iSpec][school.dietOutputStage] += school.getAbundance();
            for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
                for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                    predatorPressure[iSpec][school.dietOutputStage][i][s] += school.dietTemp[i][s];
                    if (school.getSumDiet() > 0) {
                        diet[iSpec][school.dietOutputStage][i][s] += school.getAbundance() * school.dietTemp[i][s] / school.getSumDiet();
                    }
                }
            }
            for (int i = getSimulation().getNbSpecies(); i < getSimulation().getNbSpecies() + getSimulation().getForcing().getNbPlanktonGroups(); i++) {
                predatorPressure[iSpec][school.dietOutputStage][i][0] += school.dietTemp[i][0];
                if (school.getSumDiet() > 0) {
                    diet[iSpec][school.dietOutputStage][i][0] += school.getAbundance() * school.dietTemp[i][0] / school.getSumDiet();
                }
            }
        }
    }
    
    public static void writeDiet(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        int nSpec = getSimulation().getNbSpecies();
        
        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_dietMatrix_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "% of prey species (in rows) in the diet of predator species (in col)";
        // Write the file
        File file = new File(path, filename.toString());
        file.getParentFile().mkdirs();
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                pr.print("// ");
                pr.println(description);
                pr.print("Time");
                pr.print(';');
                pr.print("Prey");
                for (int i = 0; i < nSpec; i++) {
                    Species species = getSimulation().getSpecies(i);
                    for (int s = 0; s < species.nbDietStages; s++) {
                        pr.print(";");
                        if (species.nbDietStages == 1) {
                            pr.print(species.getName());    // Name predators
                        } else {
                            if (s == 0) {
                                pr.print(species.getName() + " < " + species.dietStagesTab[s]);    // Name predators
                            } else {
                                pr.print(species.getName() + " >" + species.dietStagesTab[s - 1]);    // Name predators
                            }
                        }
                    }
                }
                pr.println();
            }
            for (int j = 0; j < nSpec; j++) {
                Species species = getSimulation().getSpecies(j);
                for (int st = 0; st < species.nbDietStages; st++) {
                    pr.print(time);
                    pr.print(';');
                    if (species.nbDietStages == 1) {
                        pr.print(species.getName());    // Name predators
                    } else {
                        if (st == 0) {
                            pr.print(species.getName() + " < " + species.dietStagesTab[st]);    // Name predators
                        } else {
                            pr.print(species.getName() + " >" + species.dietStagesTab[st - 1]);    // Name predators
                        }
                    }
                    pr.print(";");
                    for (int i = 0; i < nSpec; i++) {
                        for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                            if (nbStomachs[i][s] >= 1) {
                                pr.print((float) (diet[i][s][j][st] / nbStomachs[i][s]));
                            } else {
                                pr.print("NaN");
                            }
                            pr.print(";");
                        }
                    }
                    pr.println();
                }
            }
            for (int j = nSpec; j < (nSpec + getSimulation().getForcing().getNbPlanktonGroups()); j++) {
                pr.print(time);
                pr.print(";");
                pr.print(getSimulation().getForcing().getPlanktonName(j - nSpec));
                pr.print(";");
                for (int i = 0; i < nSpec; i++) {
                    for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                        if (nbStomachs[i][s] >= 1) {
                            pr.print((float) (diet[i][s][j][0] / nbStomachs[i][s]));
                        } else {
                            pr.print("NaN");
                        }
                        pr.print(";");
                    }
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
    
    public static void writePredatorPressure(float time) {
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        int nSpec = getSimulation().getNbSpecies();
        int dtRecord = getOsmose().getRecordFrequency();
        
        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_predatorPressure_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Biomass of prey species (in tons per time step of saving, in rows) eaten by a predator species (in col). The last column reports the biomass of prey at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)";
        // Write the file
        File file = new File(path, filename.toString());
        file.getParentFile().mkdirs();
        boolean isNew = !file.exists();
        try {
            fos = new FileOutputStream(file, true);
            pr = new PrintWriter(fos, true);
            if (isNew) {
                pr.print("// ");
                pr.println(description);
                pr.print("Time");
                pr.print(';');
                pr.print("Prey");
                for (int i = 0; i < nSpec; i++) {
                    Species species = getSimulation().getSpecies(i);
                    for (int s = 0; s < species.nbDietStages; s++) {
                        pr.print(";");
                        if (species.nbDietStages == 1) {
                            pr.print(species.getName());    // Name predators
                        } else {
                            if (s == 0) {
                                pr.print(species.getName() + " < " + species.dietStagesTab[s]);    // Name predators
                            } else {
                                pr.print(species.getName() + " >" + species.dietStagesTab[s - 1]);    // Name predators
                            }
                        }
                    }
                }
                pr.print(";");
                pr.print("Biomass");
                pr.println();
            }
            for (int j = 0; j < nSpec; j++) {
                Species species = getSimulation().getSpecies(j);
                for (int st = 0; st < species.nbDietStages; st++) {
                    pr.print(time);
                    pr.print(';');
                    if (species.nbDietStages == 1) {
                        pr.print(species.getName());    // Name predators
                    } else {
                        if (st == 0) {
                            pr.print(species.getName() + " < " + species.dietStagesTab[st]);    // Name predators
                        } else {
                            pr.print(species.getName() + " >" + species.dietStagesTab[st - 1]);    // Name predators
                        }
                    }
                    pr.print(";");
                    for (int i = 0; i < nSpec; i++) {
                        for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                            pr.print((float) (predatorPressure[i][s][j][st] / dtRecord));
                            pr.print(";");
                        }
                    }
                    pr.print((float) (biomPerStage[j][st] / dtRecord));
                    pr.println();
                }
            }
            for (int j = nSpec; j < (nSpec + getSimulation().getForcing().getNbPlanktonGroups()); j++) {
                pr.print(time);
                pr.print(";");
                pr.print(getSimulation().getForcing().getPlanktonName(j - nSpec));
                pr.print(";");
                for (int i = 0; i < nSpec; i++) {
                    for (int s = 0; s < getSimulation().getSpecies(i).nbDietStages; s++) {
                        pr.print((float) (predatorPressure[i][s][j][0] / dtRecord));
                        pr.print(";");
                    }
                }
                pr.print(biomPerStage[j][0] / dtRecord);
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
    
    public static void writeTLDistribution(float time) {
        
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
                pr.print("// ");
                pr.println(description);
                pr.print("Time");
                pr.print(';');
                pr.print("TL");
                pr.print(';');
                for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
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
                for (int iSpec = 0; iSpec < getSimulation().getNbSpecies(); iSpec++) {
                    pr.print((float) (distribTL[iSpec][iTL] / getOsmose().getRecordFrequency()));
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
    
    public static void writeSizeSpectrum(float time) {
        
        StringBuilder filename;
        String description;
        PrintWriter pr;
        FileOutputStream fos = null;
        File path = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[getOsmose().numSerie]);
        
        if (getOsmose().isSizeSpectrumOutput()) {
            filename = new StringBuilder("SizeIndicators");
            filename.append(File.separatorChar);
            filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
            filename.append("_SizeSpectrum_Simu");
            filename.append(getOsmose().numSimu);
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
                    pr.print("// ");
                    pr.println(description);
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
                    for (int iSpec = 0; iSpec < getSimulation().getNbSpecies(); iSpec++) {
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
            filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
            filename.append("_SizeSpectrumSpecies_Simu");
            filename.append(getOsmose().numSimu);
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
                    pr.print("// ");
                    pr.println(description);
                    pr.print("Time");
                    pr.print(';');
                    pr.print("size");
                    pr.print(';');
                    for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
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
                    for (int iSpec = 0; iSpec < getSimulation().getNbSpecies(); iSpec++) {
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
    
    public static void writeYields(float time) {
        
        StringBuilder filename;
        String description;
        
        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_yield_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "cumulative catch (tons per time step of saving). ex: if time step of saving is the year, then annual catches are saved";
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
        String description;
        
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            if (abundanceNoJuv[i] > 0) {
                meanSize[i] = (float) (meanSize[i] / abundanceNoJuv[i]);
            } else {
                meanSize[i] = Double.NaN;
            }
            if (yieldN[i] > 0) {
                meanSizeCatch[i] = meanSizeCatch[i] / yieldN[i];
            } else {
                meanSizeCatch[i] = Double.NaN;
            }
        }
        
        filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanSize_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean size of fish species in cm, weighted by fish numbers, and excluding first ages specified in input (in calibration file)";
        writeVariable(time, meanSize, filename.toString(), description);
        
        filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanSizeCatch_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean size of fish species in cm, weighted by fish numbers in the catches";
        writeVariable(time, meanSizeCatch, filename.toString(), description);
    }
    
    public static void monitorMeanTL() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
            double biomass = 0.d;
            for (int j = species.indexAgeClass0; j < species.getNumberCohorts(); j++) {
                biomass += species.getCohort(j).getBiomass();
            }
            meanTL[i] += species.meanTLSpe * biomass;
            tabTLCatch[i] += species.tabTLCatch;
        }
    }

    /*
     * Writes mean TL per species. It must come before writeBiomassAndAbundance
     * since the mean size is pondered by the biomass without juveniles.
     */
    public static void writeMeanTL(float time) {
        
        StringBuilder filename;
        String description;
        
        double[] meanTLCatch = new double[getSimulation().getNbSpecies()];
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            if (biomassNoJuv[i] != 0.d) {
                meanTL[i] = (float) (meanTL[i] / biomassNoJuv[i]);
            } else {
                meanTL[i] = 0.f;
            }
            if (yield[i] > 0) {
                meanTLCatch[i] = tabTLCatch[i] / yield[i];
            } else {
                meanTLCatch[i] = Double.NaN;
            }
        }

        // Mean TL
        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanTL_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean Trophic Level of fish species, weighted by fish biomass, and including/excluding first ages specified in input (in calibration file)";
        writeVariable(time, meanTL, filename.toString(), description);

        // Mean TL for catches
        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanTLCatch-tmp_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean Trophic Level of fish species, weighted by fish catch";
        writeVariable(time, meanTLCatch, filename.toString(), description);
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
    
    public static void writeBiomassAndAbundance(float time) {
        
        StringBuilder filename;
        int nSpec = getSimulation().getNbSpecies();
        
        double nsteps = getOsmose().savingDtMatrix[getOsmose().numSerie];
        int year = getSimulation().getYear();
        int indexSaving = (int) (getSimulation().getIndexTimeYear() / nsteps);
        for (int i = 0; i < nSpec; i++) {
            if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
                abundanceTot[i] = Math.floor(abundanceTot[i] / nsteps);
                biomassTot[i] /= nsteps;
            }
            abundanceNoJuv[i] = Math.floor(abundanceNoJuv[i] / nsteps);
            biomassNoJuv[i] /= nsteps;
        }
        
        if (getOsmose().isCalibrationOutput()) {
            for (int i = 0; i < nSpec; i++) {
                getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassNoJuv[i];
                getOsmose().BIOMQuadri[getOsmose().numSimu][i][1][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassTot[i];
            }
            for (int i = nSpec; i < nSpec + getSimulation().getForcing().getNbPlanktonGroups(); i++) {
                getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][year - getOsmose().timeSeriesStart][indexSaving] = (float) (biomPerStage[i][0] / nsteps);
            }
        }
        
        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_biomass_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        writeVariable(time, biomassNoJuv, filename.toString(), "Mean biomass (tons), excluding first ages specified in input (typically in calibration file)");
        
        if (getOsmose().isIncludeClassZero()) {
            
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
