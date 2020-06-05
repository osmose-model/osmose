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
import fr.ird.osmose.util.Matrix;
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
public class AccessMatrix extends Matrix {

    /** Interface to the recovery of class variable (age or size). */
    private final ClassGetter classGetter;

    /**
     * Class constructor.The reading of the file is done here
     *
     * @param filename
     * @param classGetter
     */
    public AccessMatrix(String filename, ClassGetter classGetter) {
        super(filename);
        this.classGetter = classGetter;
        this.read();
    }

    /** Extracts the matrix column for the given predator.
     * 
     * Based on full correspondance of the name (class < thres).
     * 
     * @param pred
     * @return 
     */
    @Override
    public int getIndexPred(IAggregation pred) {        
        for (int i = 0; i < this.getNPred(); i++) {
            if (pred.getSpeciesName().equals(this.getPredName(i)) && (classGetter.getVariable(pred) < this.getPredClass(i))) {
                return i;
            }
        }
        String message = String.format("No accessibility found for predator %s class %f", pred.getSpeciesName(), classGetter.getVariable(pred));
        error(message, new IllegalArgumentException());       
        return -1;
    }

    /** Extracts the matrix column for the given prey.
     * 
     * Based on full correspondance of the name (class < thres).
     * 
     * @param prey
     * @return 
     */
    @Override
    public int getIndexPrey(IAggregation prey) {
        for (int i = 0; i < this.getNPrey(); i++) {
            if (prey.getSpeciesName().equals(this.getPreyName(i)) && (classGetter.getVariable(prey) < this.getPreyClass(i))) {
                return i;
            }
        }
        String message = String.format("No accessibility found for prey %s class %f", prey.getSpeciesName(), classGetter.getVariable(prey));
        error(message, new IllegalArgumentException());       
        return -1;
    }

    @Override
    public int getIndexPred(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getIndexPrey(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
