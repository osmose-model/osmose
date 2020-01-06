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
package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.OsmoseLinker;
import java.util.Arrays;

/**
 *
 * @author pverley
 */
public class OutputRegion extends OsmoseLinker {
    
    private final int index;
    private int[] cells;
    
    public OutputRegion(int index) {
        this.index = index;
    }
    
    public void init() {
        
        if (!getConfiguration().isNull("output.region.file.rg" + index)) {
            // region defined by a grid map
            String file = getConfiguration().getFile("output.region.file.rg" + index);
            GridMap map = new GridMap(file);
            cells = getConfiguration().getGrid().getCells().stream()
                    .filter(cell -> !cell.isLand() && map.getValue(cell) > 0)
                    .mapToInt(Cell::getIndex)
                    .toArray();
        } else {
            // region defined by lon/lat min & max
            double lonmin = getConfiguration().getDouble("output.region.lon.min.rg" + index);
            double lonmax = getConfiguration().getDouble("output.region.lon.max.rg" + index);
            double latmin = getConfiguration().getDouble("output.region.lat.min.rg" + index);
            double latmax = getConfiguration().getDouble("output.region.lat.max.rg" + index);
            double lat[] = new double[]{latmin, latmax, latmax, latmin, latmin};
            double lon[] = new double[]{lonmin, lonmin, lonmax, lonmax, lonmax};
            cells = getConfiguration().getGrid().getCells().stream()
                    .filter(cell -> !cell.isLand() && isInside(cell.getLat(), cell.getLon(), lat, lon))
                    .mapToInt(Cell::getIndex)
                    .toArray();
        }
        Arrays.sort(cells);
    }
    
    /*
    https://stackoverflow.com/questions/12083093/how-to-define-if-a-determinate-point-is-inside-a-region-lat-long
     */
    private boolean isInside(double lat0, double lon0, double[] lat, double[] lon) {
        int i, j;
        boolean inside = false;
        int sides = lat.length;
        for (i = 0, j = sides - 1; i < sides; j = i++) {
            //verifying if your coordinate is inside your region
            double dxi0 = substract(lon0, lon[i]);
            double dxj0 = substract(lon0, lon[j]);
            double dxji = substract(lon[j], lon[i]);
            if ((((dxi0 >= 0) && (dxj0 < 0)) || ((dxj0 >= 0) && (dxi0 < 0)))
                    && (lat0 < ((lat[j] - lat[i]) * dxi0 / dxji + lat[i]))) {
                inside = !inside;
            }
        }
        return inside;
    }

    private double substract(double lon1, double lon2) {
        double dx;
        if ((lon1 - lon2) > 180.d) {
            dx = lon1 - lon2 - 360.d;
        } else if ((lon1 - lon2) < -180.d) {
            dx = 360.d + lon1 - lon2;
        } else {
            dx = lon1 - lon2;
        }
        return dx;
    }
    
    public int getIndex() {
        return index;
    }
    
    public boolean contains(School school) {
        return Arrays.binarySearch(cells, school.getCell().getIndex()) >= 0;
    }
    
    @Override
    public String toString() {
        return "Region " + index + ", " + cells.length + " cells";
    }
    
}
