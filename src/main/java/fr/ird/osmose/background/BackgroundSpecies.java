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
package fr.ird.osmose.background;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.OsmoseLinker;     
import java.io.IOException;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author nbarrier
 */
public class BackgroundSpecies extends OsmoseLinker {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Index of the species. [0 : number of background - 1]
     */
    private final int index;

    /**
     * Name of the species. Parameter <i>species.name.sp#</i>
     */
    private final String name;

    /**
     * Allometric parameters. Parameters
     * <i>species.length2weight.condition.factor.sp#</i> and
     * <i>species.length2weight.allometric.power.sp#</i>
     */
    private final float c, bPower;

    /**
     * Trophic Level.
     *
     * @todo Use TL by stage instead?
     */
    private final float[] trophicLevel;

    private final float[] length;

    private final float[] classProportion;

    private final float[] age;

    private final int[] ageDt;

    private final int nClass;

    /**
     * Constructor of background species.
     *
     * @param index
     * @throws java.io.IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    public BackgroundSpecies(int index) throws IOException, InvalidRangeException {

        Configuration cfg = Osmose.getInstance().getConfiguration();

        // Initialiaze the index of the Background species
        this.index = index;

        // Initialization of parameters
        name = cfg.getString("species.name.sp" + index);

        nClass = cfg.getInt("species.nclass.sp" + index);

        // Reads allometric variables to obtain weight from size
        c = cfg.getFloat("species.length2weight.condition.factor.sp" + index);
        bPower = cfg.getFloat("species.length2weight.allometric.power.sp" + index);

        //trophicLevel = cfg.getFloat("species.trophiclevel.sp" + index);
        trophicLevel = cfg.getArrayFloat("species.trophic.level.sp" + index);

        // Proportion of the different size classes
        classProportion = cfg.getArrayFloat("species.size.proportion.sp" + index);

        age = cfg.getArrayFloat("species.age.sp" + index);
        ageDt = new int[age.length];
        for (int i = 0; i < age.length; i++) {
            ageDt[i] = (int) age[i] * getConfiguration().getNStepYear();
        }

        // Get the array of species length
        length = cfg.getArrayFloat("species.length.sp" + index);

        boolean isOk = true;
        String message = "";

        if (trophicLevel.length != nClass) {
            message = String.format("Length of species.trophic.level.sp%d is "
                    + "not consistent with species.nclass.cp%d", index, index);
            isOk = false;
        }

        if (age.length != nClass) {
            message = String.format("Length of species.age.sp%d is "
                    + "not consistent with species.nclass.cp%d", index, index);
            isOk = false;
        }

        if (classProportion.length != nClass) {
            message = String.format("Length of species.size.proportion.sp%d is "
                    + "not consistent with species.nclass.cp%d", index, index);
            isOk = false;
        }

        if (length.length != nClass) {
            message = String.format("Length of species.length.sp%d is "
                    + "not consistent with species.nclass.cp%d", index, index);
            isOk = false;
        }
        
        // check that the classProportion sums to 1.
        float sum = 0.f;
        for (int i = 0; i < classProportion.length; i++) {
            sum += classProportion[i];
        }

        if (sum != 1.f) {
            message = String.format("species.size.proportion.sp%d must sum to 1.0", index);
            isOk = false;
        }
        
        if(!isOk) {
            error(message, new IOException());
        }

    }

    /**
     * Returns the index of the background species.
     *
     * @return
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Returns the trophic level of the current background species.
     *
     * @todo Do this by class?
     * @return
     */
    public float getTrophicLevel(int iClass) {
        return this.trophicLevel[iClass];
    }

    /**
     * Computes the weight, in gram, corresponding to the given length, in
     * centimeter.
     *
     * @param length, the length in centimeter
     * @return the weight in gram for this {@code length}
     */
    public float computeWeight(float length) {
        return (float) (c * (Math.pow(length, bPower)));
    }

    /**
     * Returns the name of the background species.
     *
     * @return The species name
     */
    public String getName() {
        return name;
    }

    public float getLength(int iClass) {
        return this.length[iClass];
    }

    public float getProportion(int iClass) {
        return this.classProportion[iClass];
    }

    public float getAge(int iClass) {
        return this.age[iClass];
    }

    public int getAgeDt(int iClass) {
        return this.ageDt[iClass];
    }
    
    public int getNClass() {
        return this.nClass;
    }

}
