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
package fr.ird.osmose.background;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.ByClassTimeSeries;
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

    /** Movement map set for the current background species. */
    private final BackgroundMapSet maps;

    /**
     * Trophic Level.
     *
     * @todo Use TL by stage instead?
     */
    private final float[] trophicLevel;

    /**
     * By size class time series of background species biomass.
     */
    ByClassTimeSeries timeSerieBySize;

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

        // Reads the biomass by dt by size class for the current background species.
        timeSerieBySize = new ByClassTimeSeries();
        timeSerieBySize.read(cfg.getFile("biomass.byDt.bySize.file.bkg" + index));

        // Initialization of parameters
        name = cfg.getString("species.name.bkg" + index);

        // Reads allometric variables to obtain weight from size
        c = cfg.getFloat("species.length2weight.condition.factor.bkg" + index);
        bPower = cfg.getFloat("species.length2weight.allometric.power.bkg" + index);

        // Initialize movement maps for background species. For one bkg species, there
        // will be one map per size class
        maps = new BackgroundMapSet(index, "movement.bkgspecies", "bkg", timeSerieBySize.getNClass());
        maps.loadMaps();

        //trophicLevel = cfg.getFloat("species.trophiclevel.bkg" + index);
        trophicLevel = cfg.getArrayFloat("species.trophiclevel.bkg" + index);
        if(trophicLevel.length != timeSerieBySize.getNClass()) {
            error("The number of trophic levels for bkg " + index + " is inconsistent with then number of class", new Exception("Init of bkg failed"));
        }

    }

    /**
     * Returns the time series object.
     *
     * @return by class time series.
     */
    public ByClassTimeSeries getTimeSeries() {
        return this.timeSerieBySize;
    }

    /**
     * Returns the biomass value for the current step and the current size class.
     *
     * @param step
     * @param school
     * @return
     */
    public double getValues(int step, float school) {
        return getTimeSeries().getValue(step, school);
    }

    /** Returns the class index for an individual of
     * the current bkg species. 
     * @param length Length of the species.
     * @return 
     */
    public int getClass(float length) {
        return getTimeSeries().classOf(length);
    }
    
    /** Returns the global bkg species index (ibkg + nspecies).
     * @return 
     */
    public int getFinalIndex() {
        return (this.index + getConfiguration().getNSpecies());
    }

    /** Returns the index of the background species. */
    public int getIndex() {
        return this.index;
    }

    /** Returns the trophic level of the current background species.
     * @todo Do this by class?
     * @return 
     */
    public float[] getTrophicLevel() {
        return this.trophicLevel;
    }

    /**
     * Returns the trophic level of the current background species
     * and for a given size class
     *
     * @param k Size class
     * @return
     */
    public float getTrophicLevel(int k) {
        return this.trophicLevel[k];
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

    /** Returns the name of the background species.
     * @return  The species name*/
    public String getName() {
        return name;
    }

    /**
     * Recovers the biomass value from the time-series, eg from time-step and
     * length class and from the map coefficient.
     *
     * @param step
     * @param length
     * @param cell
     * @return
     */
    public double getBiomass(int step, float length, Cell cell) {
        // recovers the biomass value from the time-series, 
        // i.e. from time-step and length class. 
        int i = cell.get_igrid();
        int j = cell.get_igrid();
        return this.getBiomass(step, length, i, j);
    }

    /**
     * Recovers the biomass value from the time-series, eg from time-step and
     * length class and from the map coefficient.
     *
     * @param step
     * @param length
     * @param i
     * @param j
     * @return
     */
    public double getBiomass(int step, float length, int i, int j) {
        // recovers the biomass value from the time-series, 
        // i.e. from time-step and length class. 
        double mapVal = this.getMapVal(step, length, i, j);
        double output = this.getValues(step, length);
        return output * mapVal;
    }
    
    /** Return the map multiplication factor to convert biomass time-series
     * into biomass map.
     * @param step
     * @param length
     * @param i
     * @param j
     * @return 
     */
    public double getMapVal(int step, float length, int i, int j) {
        if (maps.getMap(this.getClass(length), step) != null) {
            return maps.getMap(this.getClass(length), step).getValue(i, j);
        } else {
            return 0.d;
        }
    }
}
