/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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

package fr.ird.osmose.resource;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

/**
 * This class represents a resource such as a plankton group or any other low
 * trophic level compartment of the ecosystem that is not explicitly represented
 * in Osmose. A resource provides a pool of biomass, spatially distributed, with
 * a given size range, accessible (to a certain level) to the schools of fish.
 * Resources are therefore the forcing of the model. A resource species is
 * defined by :
 * <ul>
 * <li>a trophic level, parameter <i>species.TL.sp#</i></li>
 * <li>a size min and max, parameters <i>species.size.min.sp#</i> and
 * <i>species.size.max.sp#</i></li>
 * <li>a conversion factor to wet weight [ton/km2], parameter
 * <i>species.conversion2tons.sp#</i></li>
 * <li>an accessibility coefficient, the percent of resource available to the
 * fish, parameter <i>species.accessibility2fish.sp</i></li>
 * </ul>
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 4.2 2019/11/25
 */
public class ResourceSpecies {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Index of the resource group
     */
    private final int index;
    /**
     * Trophic level of the resource group. Parameter <i>species.TL.sp#</i>
     */
    private final float trophicLevel;
    /**
     * Size range, in centimeter, of the resource group. Parameters
     * <i>species.size.min.sp#</i> and
     * <i>species.size.max.sp#</i>
     */
    private final double sizeMin, sizeMax;
    /**
     * Name of the resource group. (e.g. phytoplankton, diatoms, copepods).
     * Parameter <i>species.name.sp#</i>
     */
    private final String name;
    /**
     * Fraction of the resource biomass available to the fish, ranging [0, 1].
     * Parameter <i>species.accessibility2fish.sp#</i>
     */
    private final double[] accessibilityCoeff;
    /**
     * Maximum value for resource accessibility. It should never be one or
     * exceed one to avoid any numerical problem when converting from float to
     * double.
     */
    private final double accessMax = 0.99d;

///////////////
// Constructors
///////////////
    /**
     * Initializes a new resource species with characteristics given as
     * parameters.
     *
     * @param index, index of the resource group
     */
    public ResourceSpecies(int index) {

        Configuration cfg = Osmose.getInstance().getConfiguration();
        this.index = index;
        // Initialisation of parameters
        name = cfg.getString("species.name.sp" + index);
        sizeMin = cfg.getDouble("species.size.min.sp" + index);
        sizeMax = cfg.getDouble("species.size.max.sp" + index);
        trophicLevel = cfg.getFloat("species.tl.sp" + index);
        if (!cfg.isNull("species.accessibility2fish.file.sp" + index)) {
            SingleTimeSeries ts = new SingleTimeSeries();
            ts.read(cfg.getFile("species.accessibility2fish.file.sp" + index));
            accessibilityCoeff = ts.getValues();
        } else {
            double accessibility = cfg.getDouble("species.accessibility2fish.sp" + index);
            accessibilityCoeff = new double[cfg.getNStep()];
            for (int i = 0; i < accessibilityCoeff.length; i++) {
                accessibilityCoeff[i] = (accessibility >= 1) ? accessMax : accessibility;
            }
        }
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    public double getAccessibility(int iStepSimu) {
        return accessibilityCoeff[iStepSimu];
    }

    /**
     * Computes the fraction of the resource size range that is contained within
     * the given size range. The size range given as parameter represents the
     * minimal and maximal prey size that a predator can prey upon. Therefore
     * this function helps to determine the fraction of the resource biomass
     * available to a predator.
     *
     * @param accessibleSizeMin, the minimal prey size, in centimeter, that a
     * predator can prey upon
     * @param accessibleSizeMax, the maximal prey size, in centimeter, that a
     * predator can prey upon
     * @return the fraction of the resource size range that matches the size
     * range given as parameter.
     */
    public double computePercent(double accessibleSizeMin, double accessibleSizeMax) {
        double tempPercent;
        tempPercent = (Math.min(sizeMax, accessibleSizeMax) - Math.max(sizeMin, accessibleSizeMin)) / (sizeMax - sizeMin);
        return tempPercent;
    }

    /**
     * Returns the maximal size of the organisms in the resource group.
     * Parameter <i>species.size.max.sp#</i>
     *
     * @return the maximal size, in centimeter, of the organisms in the resource
     * group
     */
    public double getSizeMax() {
        return sizeMax;
    }

    /**
     * Returns the minimal size of the organisms in the resource group.
     * Parameter <i>species.size.min.sp#</i>
     *
     * @return the minimal size, in centimeter, of the organisms in the resource
     * group
     */
    public double getSizeMin() {
        return sizeMin;
    }

    /**
     * Returns the name of the resource group. Parameter
     * <i>species.name.sp#</i>
     *
     * @return the name of the resource group
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the resource group.
     *
     * @see #getName()
     * @return the name of the resource group
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the index of the resource group.
     *
     * @return the index of the resource group
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the averaged trophic level of the resource group. Parameter
     * <i>species.TL.sp#</i>
     *
     * @return the averaged trophic level of the resource group
     */
    public float getTrophicLevel() {
        return trophicLevel;
    }
}
