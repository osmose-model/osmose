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

package fr.ird.osmose.output;

import fr.ird.osmose.AbstractSchool;

import fr.ird.osmose.process.mortality.fishery.FisheryMapSet;

/**
 *
 * @author pverley
 */
public class OutputRegion extends AbstractOutputRegion {

    private FisheryMapSet mapSet;
    private boolean cutoffEnabled;
    private float[] cutoffAge;

    public OutputRegion(int index) {
        super(index);
    }

    @Override
    public void init() {

        // Recovering the Survey index;
        int index = this.getIndex();

        // Setting the name of the Survey region.
        this.setName(getConfiguration().getString("output.regions.name.rg" + index));

        mapSet = new FisheryMapSet(this.getName(), "output.regions.movement", "region");
        mapSet.init();

        // Cutoff
        cutoffEnabled = getConfiguration().getBoolean("output.cutoff.enabled");
        cutoffAge = new float[getNSpecies()];
        if (cutoffEnabled) {
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                cutoffAge[iSpec] = getConfiguration().getFloat("output.cutoff.age.sp" + iSpec);
            }
        }

        /*
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
         */
    }

    /**
     * Implementation of the Point On Polygon algorithm. In this implementation,
     * infinite beams are oriented northward. See
     * https://stackoverflow.com/questions/12083093/how-to-define-if-a-determinate-point-is-inside-a-region-lat-long
     */
    /*
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
     */
    /**
     * Check if school is given the region at a given time step.
     *
     * @param timeStep
     * @param school
     * @return
     */
    @Override
    public boolean contains(int timeStep, AbstractSchool school) {
        return (this.mapSet.getValue(timeStep, school.getCell()) > 0);
    }

    @Override
    public String toString() {
        return "Output Region " + this.getIndex();
    }

    @Override
    public double getSelectivity(int timeStep, AbstractSchool school) {
        double sel = this.include(school) ? 1 : 0;
        return sel;
    }

    boolean include(AbstractSchool school) {
        return ((!cutoffEnabled) || (school.getAge() >= cutoffAge[school.getFileSpeciesIndex()]));
    }

}
