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

package fr.ird.osmose.stage;

import fr.ird.osmose.util.OsmoseLinker;
/**
 *
 * @author pverley
 */
abstract class AbstractStage extends OsmoseLinker implements IStage {

    private float[][] thresholds;

    private final String key;

    AbstractStage(String key) {
        this.key = key;
    }

    @Override
    public void init() {

        int nSpecies = this.getNSpecies();
        int nBkgSpecies = this.getNBkgSpecies();
        int nResources = this.getNRscSpecies();
        int nTot = nSpecies + nBkgSpecies + nResources;
        thresholds = new float[nTot][];
        
        // Set values for focal and background species species.
        int cpt = 0;
        for (int i : getConfiguration().getPredatorIndex()) {
            int nStage = !getConfiguration().isNull(key + i)
                    ? getConfiguration().getArrayString(key + i).length + 1
                    : 1;
            if (nStage > 1) {
                thresholds[cpt] = getConfiguration().getArrayFloat(key + i);
            } else {
                thresholds[cpt] = new float[0];
            }
            
            cpt++;
            
        }
        
        // Set values for resource species.
        for (int i : getConfiguration().getResourceIndex()) {
            thresholds[cpt] = new float[0];
            cpt++;
        }
        
    }

    @Override
    public int getNStage(int iSpecies) {
        return thresholds[iSpecies].length + 1;
    }

    @Override
    public float[] getThresholds(int iSpecies) {
        return thresholds[iSpecies];
    }
}
