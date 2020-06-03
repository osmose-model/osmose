/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
package fr.ird.osmose.process.mortality;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.stage.ClassGetter;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.Separator;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Class that manages the reading and use of accesibility matrix.
 *
 * @author Nicolas Barrier
 */
public class AccessMatrix extends OsmoseLinker {

    /** Interface to the recovery of class variable (age or size). */
    private final ClassGetter classGetter;

    /**
     * Number of preys (lines in the file).
     */
    private int nPreys;

    /**
     * Number of predators (columns in the file).
     */
    private int nPred;

    /**
     * Accessibility values of dimension (nprey, nclass).
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

    /**
     * Class constructor. The reading of the file is done here
     *
     * @param filename
     */
    AccessMatrix(String filename, ClassGetter classGetter) {
        this.filename = filename;
        this.classGetter = classGetter;
        this.read();
    }

    /**
     * Reads the accessibility file. The first column and the header are now
     * used to reconstruct the upper size class
     */
    private void read() {

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
            for (int ipred = 1; ipred < header.length; ipred++) {
                String predString = header[ipred];
                int index = predString.lastIndexOf('<');
                if (index < 0) {
                    classPred[ipred - 1] = Float.MAX_VALUE;
                    namesPred[ipred - 1] = predString.trim();
                } else {
                    namesPred[ipred - 1] = predString.substring(0, index - 1).trim();
                    classPred[ipred - 1] = Float.valueOf(predString.substring(index + 1, predString.length()));
                }
            }

            // Initialize the data matrix
            this.accessibilityMatrix = new double[nPreys][nPred];

            // Loop over all the lines of the file, avoiding header
            for (int iprey = 1; iprey < lines.size(); iprey++) {

                // Read the line for the given prey
                String[] lineStr = lines.get(iprey);

                // Recovering the column name to get prey names and class
                String preyString = lineStr[0];
                int index = preyString.lastIndexOf('<');
                if (index < 0) {
                    classPrey[iprey - 1] = Float.MAX_VALUE;
                    namesPrey[iprey - 1] = preyString;
                } else {
                    namesPrey[iprey - 1] = preyString.substring(0, index - 1).trim();
                    classPrey[iprey - 1] = Float.valueOf(preyString.substring(index + 1, preyString.length()));
                }

                for (int ipred = 1; ipred < lineStr.length; ipred++) {
                    this.accessibilityMatrix[iprey - 1][ipred - 1] = Double.valueOf(lineStr[ipred]);
                }

            }

        } catch (IOException ex) {
            error("Error loading accessibility matrix from file " + filename, ex);
        }
    }

    /** Recovers the name of the accessibility file. */
    public String getFile() {
        return this.filename;
    }

    /** Extracts the matrix column for the given predator.
     * 
     * Based on full correspondance of the name (class < thres).
     * 
     * @param pred
     * @return 
     */
    public int getIndexPred(IAggregation pred) {        
        for (int i = 0; i < this.nPred; i++) {
            if (pred.getSpeciesName().equals(this.namesPred[i]) && (classGetter.getVariable(pred) < this.classPred[i])) {
                return i;
            }
        }
        String message = String.format("No accessibility found for predator %s class %f", pred.getSpeciesName(), classGetter.getVariable(pred));
        throw new IllegalArgumentException(message);
    }

    /** Extracts the matrix column for the given prey.
     * 
     * Based on full correspondance of the name (class < thres).
     * 
     * @param prey
     * @return 
     */
    public int getIndexPrey(IAggregation prey) {
        for (int i = 0; i < this.nPred; i++) {
            if (prey.getSpeciesName().equals(this.namesPrey[i]) && (classGetter.getVariable(prey) < this.classPrey[i])) {
                return i;
            }
        }
        String message = String.format("No accessibility found for prey %s class %f", prey.getSpeciesName(), classGetter.getVariable(prey));
        throw new IllegalArgumentException(message);
    }

    public double getValue(int iprey, int ipred) {
        return this.accessibilityMatrix[iprey][ipred];
    }

}
