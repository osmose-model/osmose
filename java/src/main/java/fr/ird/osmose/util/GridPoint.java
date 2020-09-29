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

import fr.ird.osmose.Cell;

/**
 * 
 * @author pverley
 */
public class GridPoint extends OsmoseLinker {

    private float x, y;
    private float lon, lat;

    public boolean isInWater() {
        return !getCell().isLand();
    }

    public boolean isOnEdge() {
        return ((x > (getGrid().get_nx() - 2.0f))
                || (x < 1.0f)
                || (y > (getGrid().get_ny() - 2.0f))
                || (y < 1.0f));
    }

    /**
     * @return the x
     */
    public float getX() {
        return x;
    }

    /**
     * @return the y
     */
    public float getY() {
        return y;
    }

    /**
     * @return the lon
     */
    public float getLon() {
        return lon;
    }

    /**
     * @return the lat
     */
    public float getLat() {
        return lat;
    }

    public boolean isUnlocated() {
        return ((x < 0) || (y < 0));
    }
    
    public void setOffGrid() {
        x = y = -1;
    }
    
    /**
     * Gets the current location of the school
     *
     * @return the cell where is located the school
     */
    public Cell getCell() {
        return getGrid().getCell(Math.round(x), Math.round(y));
    }
    
    public void moveToCell(Cell cell) {
        x = cell.get_igrid();
        y = cell.get_jgrid();
    }
}
