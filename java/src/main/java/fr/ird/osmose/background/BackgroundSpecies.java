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

package fr.ird.osmose.background;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.OsmoseLinker;     
import fr.ird.osmose.util.timeseries.ByClassTimeSeries;
import java.io.IOException;
import ucar.ma2.InvalidRangeException;
import fr.ird.osmose.ISpecies;

/**
 *
 * @author nbarrier
 */
public class BackgroundSpecies extends OsmoseLinker implements ISpecies {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Index of the species. [0 : number of background - 1]
     */
    private final int fileindex;

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
    
    private final ByClassTimeSeries timeSeries;
    
    private final int index;
    private final int offset;

    /**
     * Constructor of background species.
     *
     * @param fileindex
     * @throws java.io.IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    public BackgroundSpecies(int fileindex, int index) throws IOException, InvalidRangeException {

        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        boolean isOk = true;
        String message = "";

        this.offset = cfg.getNSpecies();
        this.index = index + this.offset;
        
        // Initialiaze the index of the Background species
        this.fileindex = fileindex;

        // Initialization of parameters
        name = cfg.getString("species.name.sp" + fileindex);

        nClass = cfg.getInt("species.nclass.sp" + fileindex);

        // Reads allometric variables to obtain weight from size
        c = cfg.getFloat("species.length2weight.condition.factor.sp" + fileindex);
        bPower = cfg.getFloat("species.length2weight.allometric.power.sp" + fileindex);

        //trophicLevel = cfg.getFloat("species.trophiclevel.sp" + index);
        trophicLevel = cfg.getArrayFloat("species.trophic.level.sp" + fileindex);

        age = cfg.getArrayFloat("species.age.sp" + fileindex);
        ageDt = new int[age.length];
        for (int i = 0; i < age.length; i++) {
            ageDt[i] = (int) age[i] * getConfiguration().getNStepYear();
        }
        
        if (cfg.canFind("species.size.proportion.file.sp" + fileindex)) {
            
            String filename = cfg.getFile("species.size.proportion.file.sp" + fileindex);
            this.timeSeries = new ByClassTimeSeries();
            this.timeSeries.read(filename);
            length = this.timeSeries.getClasses();
            this.classProportion = null;
            
        } else {
            
            this.timeSeries = null;
            
            // Proportion of the different size classes
            classProportion = cfg.getArrayFloat("species.size.proportion.sp" + fileindex);
            // Get the array of species length
            length = cfg.getArrayFloat("species.length.sp" + fileindex);

            if (classProportion.length != nClass) {
                message = String.format("Length of species.size.proportion.sp%d is "
                        + "not consistent with species.nclass.cp%d", fileindex, fileindex);
                isOk = false;
            }
            
            // check that the classProportion sums to 1.
            float sum = 0.f;
            for (int i = 0; i < classProportion.length; i++) {
                sum += classProportion[i];
            }

            if (sum != 1.f) {
                message = String.format("species.size.proportion.sp%d must sum to 1.0", fileindex);
                isOk = false;
            }
        
        }

        if (trophicLevel.length != nClass) {
            message = String.format("Length of species.trophic.level.sp%d is "
                    + "not consistent with species.nclass.cp%d", fileindex, fileindex);
            isOk = false;
        }

        if (age.length != nClass) {
            message = String.format("Length of species.age.sp%d is "
                    + "not consistent with species.nclass.cp%d", fileindex, fileindex);
            isOk = false;
        }

        if (length.length != nClass) {
            message = String.format("Length of species.length.sp%d is "
                    + "not consistent with species.nclass.cp%d", fileindex, fileindex);
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
    public int getFileSpeciesIndex() {
        return this.fileindex;
    }

    /**
     * Return the global index of the species.
     *
     * @return
     */
    public int getSpeciesIndex(boolean applyOffset) {
        if (applyOffset) {
            return this.index;
        } else {
            return this.index - this.offset;
        }
    }
    
    @Override
    public int getSpeciesIndex() {
        return this.getSpeciesIndex(true);
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

    public double getProportion(int iClass, int step) {
        if (this.classProportion == null) {
            return this.timeSeries.getValue(step, iClass);
        } else {
            return this.classProportion[iClass];
        }
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
