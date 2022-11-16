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

import fr.ird.osmose.IMarineOrganism;
import fr.ird.osmose.util.OsmoseLinker;
/**
 *
 * If no thresholds are provided, then assume one single class, of index 0.
 * If N thresholds are provided, assumes that N + 1 classes are provided.
 * The threshold provide the upper limit of the threshold, so that a school belongs
 * to size class s if:
 *
 * T(s - 1) <= V < T(s)
 *
 * @author pverley
 */
public class SchoolStage extends OsmoseLinker {

    private float[][] thresholds;

    private final String key;
    private ClassGetter[] classGetter;

    public SchoolStage(String key) {
        this.key = key;
    }

    public void init() {

        int nSpecies = this.getNSpecies();
        int nBkgSpecies = this.getNBkgSpecies();
        int nResources = this.getNRscSpecies();
        int nTot = nSpecies + nBkgSpecies + nResources;
        thresholds = new float[nTot][];
        classGetter = new ClassGetter[nSpecies + nBkgSpecies];

        // Set values for focal and background species species.
        int cpt = 0;
        for (int i : getConfiguration().getPredatorIndex()) {
            int nStage = !getConfiguration().isNull(key + ".thresholds.sp" + i)
                    ? getConfiguration().getArrayString(key + ".thresholds.sp" + i).length + 1
                    : 1;
            if (nStage > 1) {
                thresholds[cpt] = getConfiguration().getArrayFloat(key + ".thresholds.sp" + i);
            } else {
                // if no threshold, init an empty array of length 0, to insure that
                // stage index is always 0.
                thresholds[cpt] = new float[0];
            }

            String structure = getConfiguration().getString(key + ".structure.sp" + i);
            switch (structure) {
                case "age":
                    classGetter[cpt] = (school -> school.getAge());
                    break;
                case "size":
                    classGetter[cpt] = (school -> school.getLength());
                    break;
                case "weight":
                    classGetter[cpt] = (school -> school.getWeight() * 1e-3);
                    break;
                case "tl":
                    classGetter[cpt] = (school -> school.getTrophicLevel());
                    break;
            }

            cpt++;

        }

        // Set values for resource species.
        for (cpt = 0; cpt < getConfiguration().getNRscSpecies(); cpt++) {
            thresholds[cpt + nSpecies + nBkgSpecies] = new float[0];
        }

    }

    public int getNStage(int iSpecies) {
        return thresholds[iSpecies].length + 1;
    }

    public float[] getThresholds(int iSpecies) {
        return thresholds[iSpecies];
    }

    public int getStage(IMarineOrganism school) {
        int stage = 0;
        int iSpec = school.getSpeciesIndex();
        for (float threshold : this.getThresholds(iSpec)) {
            if (classGetter[iSpec].getVariable(school) < threshold) {
                break;
            }
            stage++;
        }
        return stage;
    }

}
