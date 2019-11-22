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
package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.util.GridMap;
import java.io.File;
import java.util.List;
import fr.ird.osmose.util.OsmoseLinker;

/**
 *
 * @author nbarrier
 */
public class Regions extends OsmoseLinker {

    private String[] domain_names;
    private static int i_dom[][];   // ndomain, npoints
    private static int j_dom[][];   // ndomain, npoints
    private static int nregion;

    public void init() {

        nregion = getConfiguration().findKeys("output.region.file*").size();
        domain_names = new String[nregion];
        i_dom = new int[nregion][];
        j_dom = new int[nregion][];

        int cpt = 0;
        for (int i = 0; i < nregion; i++) {
            String key = ("output.region.file" + cpt);
            if (!getConfiguration().canFind(key)) {
                cpt++;
                continue;
            }
            String file = getConfiguration().getFile(key);
            key = ("output.region.name" + cpt);
            String name = getConfiguration().getString(key);
            GridMap domain_mask = new GridMap(file);
            init_indexes(i, domain_mask);
            domain_names[i] = name;
            cpt++;
        }

    }

    public void init_indexes(int idom, GridMap map) {

        int ii, jj;
        int ny = getConfiguration().getGrid().get_ny();
        int nx = getConfiguration().getGrid().get_nx();
        int npoints = 0;   // number of points for the sum

        // count the number of points for which point is in ocean and set for regional outputs
        for (jj = 0; jj < ny; jj++) {
            for (ii = 0; ii < nx; ii++) {
                Cell cell = getGrid().getCell(ii, jj);
                if ((map.getValue(cell) > 0) && (!cell.isLand())) {
                    npoints += 1;
                }
            }
        }

        // init the indexes for the domain
        i_dom[idom] = new int[npoints];
        j_dom[idom] = new int[npoints];

        // redo the same loop but with setting the indexes. cpt at the end should equal npoints
        int cpt = 0;
        for (jj = 0; jj < ny; jj++) {
            for (ii = 0; ii < nx; ii++) {
                Cell cell = getGrid().getCell(ii, jj);
                if ((map.getValue(cell) > 0) && (!cell.isLand())) {
                    i_dom[idom][cpt] = ii;
                    j_dom[idom][cpt] = jj;
                    cpt++;
                }
            }
        }
    }

    public static int[] getIdom(int idom) {
        return i_dom[idom];
    }

    public static int[] getJdom(int idom) {
        return j_dom[idom];
    }

    public static int getNRegions() {
        return nregion;
    }
    
    /** Search whether the given school belongs to the idom region.
     * @param idom Index of the output region
     * @param school School
     * @return      
     */
    public static boolean isInRegion(int idom, School school) {

        int ischool = school.getCell().get_igrid();
        int jschool = school.getCell().get_jgrid();
        int npoints = i_dom[idom].length;

        for (int k = 0; k < npoints; k++) {
            if ((i_dom[idom][k] == ischool) && (j_dom[idom][k] == jschool)) {
                return true;
            }
        }

        return false;
    }
}
