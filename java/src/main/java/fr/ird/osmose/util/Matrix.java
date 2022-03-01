/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.stage.ClassGetter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Class that manages the reading and use of accesibility matrix.
 *
 * @author Nicolas Barrier
 */
public class Matrix extends OsmoseLinker {

    /**
     * Number of preys (lines in the file).
     */
    private int nPreys;

    /**
     * Number of predators (columns in the file).
     */
    private int nPred;

    /**
     * Accessibility values of dimension (npreys, npred).
     */
    private double[][] accessibilityMatrix;

    /**
     * Accessibility filename.
     */
    private final String filename;

    /**
     * Upper bounds of the prey size class. Read in the file.
     */
    private float[] classPrey;

    /**
     * Names of the preys.
     */
    private String[] namesPrey;

    /**
     * Upper bounds of the pred size class.
     */
    private float[] classPred;

    /**
     * Names of the predators.
     */
    private String[] namesPred;

    private final ClassGetter classGetter;
    
    private boolean sortMatrix;
    
    private float[][] predClasses;
    private float[][] preyClasses;
    private int[][] predIndex;
    private int[][] preyIndex;

    /** Functional interface for the finding of predator index in access. matrix */
    private interface IndexPred {
        public int getIndexPred(IAggregation pred);
    }
    
    /** Functional interface for the finding of prey index in access. matrix */
    private interface IndexPrey {
        public int getIndexPrey(IAggregation prey);
    }
    
    IndexPrey indexingPrey;
    IndexPred indexingPred;
    
    /**
     * Class constructor.The reading of the file is done here
     *
     * @param filename
     * @param classGetter
     */
    public Matrix(String filename, ClassGetter classGetter) {
        this(filename, classGetter, true);
    }
    
    public Matrix(String filename, ClassGetter classGetter, boolean sortMatrix) {
        this.filename = filename;
        this.classGetter = classGetter;
        this.sortMatrix = sortMatrix;
        this.read();
    }

    /**
     * Reads the accessibility file. The first column and the header are now
     * used to reconstruct the upper size class
     */
    public void read() {

        try (CSVReader reader = new CSVReader(new FileReader(filename), Separator.guess(filename).getSeparator())) {

            // Read all the lines
            List<String[]> lines = reader.readAll();

            // extract the  number of preys (removing the header)
            nPreys = lines.size() - 1;

            namesPrey = new String[nPreys];
            classPrey = new float[nPreys];

            // process the header, i.e defines characteristics for predators.
            String[] header = lines.get(0);

            // extracts the number of pred (nheaders minus first element)
            nPred = header.length - 1;

            classPred = new float[nPred];
            namesPred = new String[nPred];

            // Process the header to extract the properties of predator (class, etc.)
            for (int ipred = 0; ipred < header.length - 1; ipred++) {
                String predString = header[ipred + 1];
                int index = predString.lastIndexOf('<');
                if (index < 0) {
                    classPred[ipred] = Float.MAX_VALUE;
                    namesPred[ipred] = predString.trim();
                } else {
                    namesPred[ipred] = predString.substring(0, index - 1).trim();
                    classPred[ipred] = Float.valueOf(predString.substring(index + 1, predString.length()));
                }
            }

            // Initialize the data matrix
            this.accessibilityMatrix = new double[nPreys][nPred];

            // Loop over all the lines of the file, avoiding header
            for (int iprey = 0; iprey < lines.size() - 1; iprey++) {

                // Read the line for the given prey
                String[] lineStr = lines.get(iprey + 1);

                // Recovering the column name to get prey names and class
                String preyString = lineStr[0];
                int index = preyString.lastIndexOf('<');
                if (index < 0) {
                    classPrey[iprey] = Float.MAX_VALUE;
                    namesPrey[iprey] = preyString.trim();
                } else {
                    namesPrey[iprey] = preyString.substring(0, index - 1).trim();
                    classPrey[iprey] = Float.valueOf(preyString.substring(index + 1, preyString.length()));
                }

                for (int ipred = 0; ipred < lineStr.length - 1; ipred++) {
                    this.accessibilityMatrix[iprey][ipred] = Double.valueOf(lineStr[ipred + 1]);
                }
            }

        } catch (IOException ex) {
            error("Error loading accessibility matrix from file " + filename, ex);
        } 
        
        if (this.sortMatrix) {
            this.sortMatrix();
        }
        debug(this.toString());
        
        if(classGetter == null) { 
            // If class getter is null, then the NoClass methods are used.
            this.indexingPred = (school -> this.getIndexPredNoClass(school));
            this.indexingPrey = (school -> this.getIndexPreyNoClass(school));
        } else {
            if(this.sortMatrix) {
                // If class getter is not null, then the Class methods are used.
                this.indexingPred = (school -> this.getIndexPredClassSorted(school));
                this.indexingPrey = (school -> this.getIndexPreyClassSorted(school));
            } else {
                // If class getter is not null, then the Class methods are used.
                this.indexingPred = (school -> this.getIndexPredClass(school));
                this.indexingPrey = (school -> this.getIndexPreyClass(school));
            }
        }       
        
        if (this.sortMatrix) {
            // Init an array with the prey classes for each species
            // size is [nPrey][nSizePrey]
            this.preyClasses = new float[this.getNAllSpecies()][];
            this.preyIndex = new int[this.getNAllSpecies()][];
            // We loop over all the species (focal, resource, bkg)
            for (int iSpecies = 0; iSpecies < this.getNAllSpecies(); iSpecies++) {
                int cpt = 0;
                // Species name that we consider
                String speciesName = getISpecies(iSpecies).getName();
                // We count the number of entries for the species (i.e the number of size-classes)
                for (String name : this.namesPrey) {
                    if (name.equals(speciesName)) {
                        cpt++;
                    }
                }

                // We initialize the number of classes for the prey
                this.preyClasses[iSpecies] = new float[cpt];
                this.preyIndex[iSpecies] = new int[cpt];
                cpt = 0;
                int i = 0;
                for (String name : this.namesPrey) {
                    if (name.equals(speciesName)) {
                        this.preyClasses[iSpecies][i] = this.classPrey[cpt];
                        this.preyIndex[iSpecies][i] = cpt;
                        i++;
                    }
                    cpt++;
                }
            } // end of prey loop

            // Init an array with the prey classes for each species
            // size is [nPrey][nSizePrey]
            this.predClasses = new float[this.getNPredSpecies()][];
            this.predIndex = new int[this.getNPredSpecies()][];
            // We loop over all the species (focal, resource, bkg)
            for (int iSpecies = 0; iSpecies < this.getNPredSpecies(); iSpecies++) {
                int cpt = 0;
                // Species name that we consider
                String speciesName = getISpecies(iSpecies).getName();
                // We count the number of entries for the species
                for (String name : this.namesPred) {
                    if (name.equals(speciesName)) {
                        cpt++;
                    }
                }

                // We initialize the number of classes for the pred
                this.predClasses[iSpecies] = new float[cpt];
                this.predIndex[iSpecies] = new int[cpt];
                cpt = 0;
                int i = 0;
                for (String name : this.namesPred) {
                    if (name.equals(speciesName)) {
                        this.predClasses[iSpecies][i] = this.classPred[cpt];
                        this.predIndex[iSpecies][i] = cpt;
                        i++;
                    }
                    cpt++;
                }
            }  // end of predator loop
        } // end of issorted test
    }
    
    /**
     * Recovers the name of the accessibility file.
     * @return 
     */
    public String getFile() {
        return this.filename;
    }

    public double getValue(int iprey, int ipred) {
        return this.accessibilityMatrix[iprey][ipred];
    }
    
    public double[][] getValues() {
        return this.accessibilityMatrix;   
    }

    public int getNPred() {
        return this.nPred;
    }

    public int getNPrey() {
        return this.nPreys;
    }
    
    public String[] getPreyNames() {
        return namesPrey;
    }

    public String[] getPredNames() {
        return namesPred;
    }

    public float[] getPreyClasses() {
        return classPrey;
    }

    public float[] getPredClasses() {
        return classPred;
    }

    public String getPreyName(int i) {
        return namesPrey[i];
    }

    public String getPredName(int i) {
        return namesPred[i];
    }

    public double getPreyClass(int i) {
        return classPrey[i];
    }

    public double getPredClass(int i) {
        return classPred[i];
    }
    
    /** Method that returns the column of predator in access matrix. It calls
     * the function in the functional interface. */
    public int getIndexPred(IAggregation pred) {
        return this.indexingPred.getIndexPred(pred);   
    }
    
     /**
     * Extracts the matrix column for the given predator.
     *
     * Based on full correspondance of the name and (class < thres).
     *
     * @param pred
     * @return
     */
     public int getIndexPredClass(IAggregation pred) {

         String predname = pred.getSpeciesName();
         double classVal = classGetter.getVariable(pred);
         for (int i = 0; i < this.getNPred(); i++) {
             if (predname.equals(this.getPredName(i)) && (classVal < this.getPredClass(i))) {
                 return i;
             }
         }

         String message = String.format("No accessibility found for predator %s class %f", pred.getSpeciesName(),
                 classGetter.getVariable(pred));
         error(message, new IllegalArgumentException());
         return -1;
     }

    /**
     * Extracts the matrix column for the given predator.
     *
     * Based on partial correspondance of the name (used in fisheries).
     *
     * @param pred
     * @return
     */
    public int getIndexPredNoClass(IAggregation pred) {

        String predname = pred.getSpeciesName();
        for (int i = 0; i < this.getNPred(); i++) {
            if (predname.equals(this.getPredName(i))) {
                return i;
            }
        }
        String message = String.format("No accessibility found for predator %s class %f", pred.getSpeciesName(), classGetter.getVariable(pred));
        error(message, new IllegalArgumentException());
        return -1;
    }
    
    /** Get the prey row in the accessibility matrix. Based on the 
     * functional interface that defines which value is used. */
    public int getIndexPrey(IAggregation prey) {
        return this.indexingPrey.getIndexPrey(prey);   
    }
    
    /**
     * Extracts the matrix column for the given prey.
     *
     * Based on full correspondance of the name (class < thres).
     *
     * @param prey
     * @return
     */
    public int getIndexPreyClass(IAggregation prey) {
        
        String preyname = prey.getSpeciesName();
        double classVal = classGetter.getVariable(prey);
        for (int i = 0; i < this.getNPrey(); i++) {
            if (preyname.equals(this.getPreyName(i)) && (classVal < this.getPreyClass(i))) {
                return i;
            }
        }

        String message = String.format("No accessibility found for prey %s class %f", prey.getSpeciesName(), classGetter.getVariable(prey));
        error(message, new IllegalArgumentException());
        return -1;

    }
    
    /**
     * Extracts the matrix column for the given prey.
     *
     * Based on full correspondance of the name (class < thres).
     *
     * @param prey
     * @return
     */
    public int getIndexPreyClassSorted(IAggregation prey) {
        
        int iSpecies = prey.getSpeciesIndex();
        for(int i = 0; i < this.preyClasses[iSpecies].length; i++) {
            if(classGetter.getVariable(prey) < this.preyClasses[iSpecies][i]) { 
                return this.preyIndex[iSpecies][i];   
            }
        }

        String message = String.format("No accessibility found for prey %s class %f", prey.getSpeciesName(), classGetter.getVariable(prey));
        error(message, new IllegalArgumentException());
        return -1;
    }

    /**
     * Extracts the matrix column for the given prey.
     *
     * Based on full correspondance of the name (class < thres).
     *
     * @param prey
     * @return
     */
    public int getIndexPredClassSorted(IAggregation pred) {
        
        int iSpecies = pred.getSpeciesIndex();
        for(int i = 0; i < this.predClasses[iSpecies].length; i++) {
            if(classGetter.getVariable(pred) < this.predClasses[iSpecies][i]) { 
                return this.predIndex[iSpecies][i];   
            }
        }

        String message = String.format("No accessibility found for pred %s class %f", pred.getSpeciesName(), classGetter.getVariable(pred));
        error(message, new IllegalArgumentException());
        return -1;
    }
    
    public int getIndexPreyNoClass(IAggregation prey) {
        
        String preyname = prey.getSpeciesName();

        for (int i = 0; i < this.getNPrey(); i++) {
            if (preyname.equals(this.getPreyName(i))) {
                return i;
            }
        }

        String message = String.format("No accessibility found for prey %s class %f", prey.getSpeciesName(),
                classGetter.getVariable(prey));
        error(message, new IllegalArgumentException());
        return -1;

    }
    
    /** *  Extracts the matrix column for the given predator.Based on full correspondance of the name (class < thres).
     * 
     *
     * @param name 
     * @param pred
     * @return 
     */
    public int getIndexPred(String name) { 
        for (int i = 0; i < this.getNPred(); i++) {
            if (name.equals(this.getPredName(i))) {
                return i;
            }
        }
        String message = String.format("No catchability found for fishery %s", name);
        error(message, new IllegalArgumentException());       
        return -1;
    }

    /** *  Extracts the matrix column for the given prey.Based on full correspondance of the name (class < thres).
     * 
     *
     * @param name 
     * @param prey
     * @return 
     */
    public int getIndexPrey(String name) {
        for (int i = 0; i < this.getNPrey(); i++) {
            if (name.equals(this.getPreyName(i))) {
                return i;
            }
        }
        String message = String.format("No catchability found for prey %s", name);
        error(message, new IllegalArgumentException());       
        return -1;
    }

    /** Sort the matrix. 
     * 
     * Rows and columns are sorted in alphabetical order. Then in increasing class order.
     * 
     */
    private void sortMatrix() {
        
        // Copy the accessibility matrix
        double[][] accessMatrixTemp = new double[nPreys][nPred];
        for(int i = 0; i < nPreys; i++) {
            for(int j = 0; j < nPred; j++) { 
                accessMatrixTemp[i][j] = this.accessibilityMatrix[i][j];
            }
        }
        
        // copy the input arrays of pred names and class;
        float[] classPredTemp = Arrays.copyOf(classPred, nPred);
        String[] namesPredTemp = Arrays.copyOf(namesPred, nPred);
        
        // Sort the predators matrix by growing name and class order
        MatrixSorter[] predSorter = new MatrixSorter[this.nPred];
        for (int iPred = 0; iPred < nPred; iPred++) {
            predSorter[iPred] = new MatrixSorter(namesPredTemp[iPred], classPredTemp[iPred]);
        }
        Arrays.sort(predSorter, (MatrixSorter m1, MatrixSorter m2) -> m1.compareTo(m2));
         
        // Copy the input arrays  of prey names and class
        float[] classPreyTemp = Arrays.copyOf(classPrey, nPreys);
        String[] namesPreyTemp = Arrays.copyOf(namesPrey, nPreys);
        
        // Sort the preys matrix by growing name and class order
        MatrixSorter[] preySorter = new MatrixSorter[this.nPreys];
        for (int iPrey = 0; iPrey < nPreys; iPrey++) {
            preySorter[iPrey] = new MatrixSorter(namesPreyTemp[iPrey], classPreyTemp[iPrey]);
        }
        
        Arrays.sort(preySorter, (MatrixSorter m1, MatrixSorter m2) -> m1.compareTo(m2));
        
        // Loop over the sorted Prey accessibility
        int iPrey = 0;
        for(MatrixSorter preyMat : preySorter) { 
            
            String preyName = preyMat.getName();
            float preyClass = preyMat.getClassVal();
            int oldPreyIndex = this.findIndex(preyName, preyClass, namesPreyTemp, classPreyTemp);
            namesPrey[iPrey] = preyName;
            classPrey[iPrey] = preyClass;
            
            int iPred = 0;
            for (MatrixSorter predMat : predSorter) {

                String predName = predMat.getName();
                float predClass = predMat.getClassVal();
                int oldPredIndex = this.findIndex(predName, predClass, namesPredTemp, classPredTemp);
                namesPred[iPred] = predName;
                classPred[iPred] = predClass;
                
                accessibilityMatrix[iPrey][iPred] = accessMatrixTemp[oldPreyIndex][oldPredIndex];
                iPred++;
                 
            }
                
            iPrey++;
            
        }
    }
    
    /** Find the index of a (name, class) tuple within a list of names and class.
     * 
     * @param name
     * @param classVal
     * @param listNames
     * @param listClassVal
     * @return 
     */
    private int findIndex(String name, float classVal, String[] listNames, float[] listClassVal) { 
        
        int NVal = listNames.length;
        for(int i = 0; i < NVal; i++) {
            if((name == listNames[i]) && (classVal == listClassVal[i])) {
                return i;
            }
        }
        
        return -1;
        
    }
    
    
    /** Converts Matrix to string. */
    @Override
    public String toString() {
        String output;
        StringBuilder bld = new StringBuilder(";");
        for(int iPred = 0; iPred < this.nPred; iPred++) {
            output = String.format("%s < %f", namesPred[iPred], classPred[iPred]);
            bld.append(output).append(";");
        }
        bld.append("\n");

        for (int iPrey = 0; iPrey < this.nPreys; iPrey++) {
            output = String.format("%s < %f", namesPrey[iPrey], classPrey[iPrey]);
            bld.append(output).append(";");
            for (int iPred = 0; iPred < this.nPred; iPred++) {
                bld.append(this.accessibilityMatrix[iPrey][iPred]).append(";");
            } 
            bld.append("\n");
        }
        
        // Remove infinite class from string output
        output =  bld.toString();
        String toRemove = String.format("< %f", Float.MAX_VALUE);
        output = output.replaceAll(toRemove, "");
        return output;
                
    } 
    
    /**
     * Class to sort Accessibility Matrix.
     * Sorts columns/rows based on names and class.
     *
     * @author barrier
     */
    private class MatrixSorter {

        private final String names;
        private final float classVal;

        public MatrixSorter(String names, float classVal) {
            this.names = names;
            this.classVal = classVal;
        }

        /** Comparator.
         * 
         * First compares the names, then the class.
         * 
         * @param otherelement
         * @return 
         */
        public int compareTo(MatrixSorter otherelement) {
            if (this.names != otherelement.names) {
                return this.names.compareToIgnoreCase(otherelement.names);
            }

            if (classVal != otherelement.classVal) {
                return Float.compare(classVal, otherelement.classVal);
            }

            return 0;
        }

        /** Returns the class value.
         * 
         * @return 
         */
        public float getClassVal() {
            return this.classVal;
        }

        /** Returns the name.
         * 
         * @return 
         */
        public String getName() {
            return this.names;
        }

    }

}
