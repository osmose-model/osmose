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
package fr.ird.osmose;

import fr.ird.osmose.util.SimulationLinker;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

/**
 * This class represents a plankton group or any other low trophic level
 * compartment of the ecosystem that is not explicitly represented in Osmose. A
 * plankton group provides a pool of biomass, spatially distributed, with a
 * given size range, accessible (to a certain level) to the schools of fish.
 * Plankton groups are therefore the forcing of the model. A plankton group is
 * defined by :
 * <ul>
 * <li>a trophic level, parameter <i>plankton.TL.plk#</i></li>
 * <li>a size min and max, parameters <i>plankton.size.min.plk#</i> and
 * <i>plankton.size.max.plk#</i></li>
 * <li>a conversion factor to wet weight [ton/km2], parameter
 * <i>plankton.conversion2tons.plk#</i></li>
 * <li>an accessibility coefficient, the percent of plankton available to the
 * fish, parameter <i>plankton.accessibility2fish.plk</i></li>
 * </ul>
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Plankton extends SimulationLinker {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Index of the plankton group
     */
    final private int index;
    /**
     * Trophic level of the plankton group. Parameter <i>plankton.TL.plk#</i>
     */
    private float trophicLevel;
    /**
     * Size range, in centimeter, of the plankton group. Parameters
     * <i>plankton.size.min.plk#</i> and
     * <i>plankton.size.max.plk#</i>
     */
    private float sizeMin, sizeMax;
    /**
     * Name of the plankton group. (e.g. phytoplankton, diatoms, copepods).
     * Parameter <i>plankton.name.plk#</i>
     */
    private String name;
    /**
     * Fraction of plankton biomass available to the fish, ranging [0, 1].
     * Parameter <i>plankton.accessibility2fish.plk#</i>
     */
    private float[] accessibilityCoeff;
    /**
     * Multiplier of the plankton biomass. Parameter 'plankton.multiplier.plk#'
     * for virtually increasing or decreasing plankton biomass.
     */
    private float multiplier;

    private Prey[][] preys;

///////////////
// Constructors
///////////////
    /**
     * Initializes a new plankton group with characteristics given as
     * parameters.
     *
     * @param rank, the simulation rank
     * @param index, index of the plankton group
     */
    public Plankton(int rank, int index) {
        super(rank);
        this.index = index;
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Intializes the parameters of the plankton group
     */
    public void init() {

        name = getConfiguration().getString("plankton.name.plk" + index);
        sizeMin = getConfiguration().getFloat("plankton.size.min.plk" + index);
        sizeMax = getConfiguration().getFloat("plankton.size.max.plk" + index);
        trophicLevel = getConfiguration().getFloat("plankton.tl.plk" + index);
        if (!getConfiguration().isNull("plankton.accessibility2fish.file.plk" + index)) {
            SingleTimeSeries ts = new SingleTimeSeries(getRank());
            ts.read(getConfiguration().getFile("plankton.accessibility2fish.file.plk" + index));
            accessibilityCoeff = ts.getValues();
        } else {
            float accessibility = getConfiguration().getFloat("plankton.accessibility2fish.plk" + index);
            accessibilityCoeff = new float[getConfiguration().getNStepYear() * getConfiguration().getNYear()];
            for (int i = 0; i < accessibilityCoeff.length; i++) {
                accessibilityCoeff[i] = accessibility;
            }
        }
        if (!getConfiguration().isNull("plankton.multiplier.plk" + index)) {
            multiplier = getConfiguration().getFloat("plankton.multiplier.plk" + index);
            warning("Plankton biomass for plankton group " + name + " will be multiplied by " + multiplier + " accordingly to parameter 'plankton.multiplier.plk'" + index + " from file " + getConfiguration().getSource("plankton.multiplier.plk" + index));
        } else {
            multiplier = 1.f;
        }
        resetPreys();
    }

    /**
     * Gets the biomass of the plankton group in a specific cell of the grid.
     *
     * @param cell, a {@link Cell} of the grid
     * @return the biomass of the plankton group, in tonne, in the given
     * {@code cell}
     */
    public float getBiomass(Cell cell) {
        return multiplier * getForcing().getBiomass(index, cell);
    }

    /**
     * Gets the accessible biomass of the plankton group by the fish in a
     * specific cell of the grid.
     * {@code accessible biomass = biomass(cell) * accessibility coefficient}
     *
     * @param cell, a {@link Cell} of the grid
     * @param iStepSimu, the current time step of the simulation
     * @return the accessible biomass of the plankton group, in tonne, in the
     * given {@code cell}
     */
    public float getAccessibleBiomass(Cell cell, int iStepSimu) {
        return accessibilityCoeff[iStepSimu] * getBiomass(cell);
    }

    /**
     * Gets the total biomass of the plankton group over the grid.
     *
     * @return the cumulated biomass over the domain in tonne
     */
    public double getTotalBiomass() {
        double biomTot = 0.d;
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                biomTot += getBiomass(cell);
            }
        }
        return biomTot;
    }

    /**
     * Computes the fraction of the plankton size range that is contained within
     * the given size range. The size range given as parameter represents the
     * minimal and maximal prey size that a predator can prey upon. Therefore
     * this function helps to determine the fraction of the plankton biomass
     * available to a predator.
     *
     * @param accessibleSizeMin, the minimal prey size, in centimeter, that a
     * predator can prey upon
     * @param accessibleSizeMax, the maximal prey size, in centimeter, that a
     * predator can prey upon
     * @return the fraction of the plankton size range that matches the size
     * range given as parameter.
     */
    public float computePercent(float accessibleSizeMin, float accessibleSizeMax) {
        float tempPercent;
        tempPercent = (Math.min(sizeMax, accessibleSizeMax) - Math.max(sizeMin, accessibleSizeMin)) / (sizeMax - sizeMin);
        return tempPercent;
    }

    /**
     * Returns the maximal size of the organisms in the plankton group.
     * Parameter <i>plankton.size.max.plk#</i>
     *
     * @return the maximal size, in centimeter, of the organisms in the plankton
     * group
     */
    public float getSizeMax() {
        return sizeMax;
    }

    /**
     * Returns the minimal size of the organisms in the plankton group.
     * Parameter <i>plankton.size.min.plk#</i>
     *
     * @return the minimal size, in centimeter, of the organisms in the plankton
     * group
     */
    public float getSizeMin() {
        return sizeMin;
    }

    /**
     * Returns the name of the plankton group. Parameter
     * <i>plankton.name.plk#</i>
     *
     * @return the name of the plankton group
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the plankton group.
     *
     * @see #getName()
     * @return the name of the plankton group
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the index of the plankton group.
     *
     * @return the index of the plankton group
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the averaged trophic level of the plankton group. Parameter
     * <i>plankton.TL.plk#</i>
     *
     * @return the averaged trophic level of the plankton group
     */
    public float getTrophicLevel() {
        return trophicLevel;
    }

    public Prey asPrey(Cell cell, int iStepSimu) {
        if (null == preys[cell.get_jgrid()][cell.get_igrid()]) {
            preys[cell.get_jgrid()][cell.get_igrid()] = new Prey(index, // index
                    cell.get_igrid(), // x
                    cell.get_jgrid(), // y
                    getAccessibleBiomass(cell, iStepSimu), // abundance (assumes that abundance <==> biomass for Plankton)
                    1e6f, // weight set to 1 ton to have abundance <==> biomass
                    trophicLevel); // trophic level
        }
        return preys[cell.get_jgrid()][cell.get_igrid()];
    }

    public void resetPreys() {
        preys = new Prey[getGrid().get_ny()][getGrid().get_nx()];
    }
}
