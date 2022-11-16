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
import fr.ird.osmose.output.distribution.DistributionType;
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

    /** Upper bounds of the threshold. Dimensions are [nSpecies][nClass - 1]. */
    private float[][] thresholds;

    private final String key;
    private ClassGetter[] classGetter;
    private DistributionType[] distributionTypes;

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
        distributionTypes = new DistributionType[nSpecies + nBkgSpecies];

        // Set values for focal and background species species.
        int cpt = 0;
        for (int i : getConfiguration().getPredatorIndex()) {
            int nStage = !getConfiguration().isNull(key + ".threshold.sp" + i)
                    ? getConfiguration().getArrayString(key + ".threshold.sp" + i).length + 1
                    : 1;
            if (nStage > 1) {
                thresholds[cpt] = getConfiguration().getArrayFloat(key + ".threshold.sp" + i);
            } else {
                // if no threshold, init an empty array of length 0, to insure that
                // stage index is always 0.
                thresholds[cpt] = new float[0];
            }

            String keysp = key + ".structure.sp" + i;
            String structure;
            if (!getConfiguration().isNull(keysp)) {
                structure = getConfiguration().getString(keysp);
            } else {
                structure = getConfiguration().getString(key + ".structure");
            }

            switch (structure) {
                case "age":
                    distributionTypes[cpt] = DistributionType.AGE;
                    classGetter[cpt] = (school -> school.getAge());
                    break;
                case "size":
                    distributionTypes[cpt] = DistributionType.SIZE;
                    classGetter[cpt] = (school -> school.getLength());
                    break;
                case "weight":
                    distributionTypes[cpt] = DistributionType.WEIGHT;
                    // converts getWeight (in tons) to grams
                    classGetter[cpt] = (school -> school.getWeight() * 1e6);
                    break;
                case "tl":
                    distributionTypes[cpt] = DistributionType.TL;
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

    /** Return the threshold value. Here they are considered
     * as the upper bound of the size class.*/
    public float[] getThresholds(int iSpecies) {
        return thresholds[iSpecies];
    }

    public float getThresholds(int iSpecies, int iClass) {
        return thresholds[iSpecies][iClass];
    }

    public int getStage(IMarineOrganism school) {
        int stage = 0;
        int iSpec = school.getSpeciesIndex();
        for (float threshold : this.thresholds[iSpec]) {
            if (classGetter[iSpec].getVariable(school) < threshold) {
                break;
            }
            stage++;
        }
        return stage;
    }

    public DistributionType getType(int iSpecies) {
        return distributionTypes[iSpecies];
    }


    public int getNValues(boolean includeResource) {

        int nColumns = 0;

        int nAll = this.getNBkgSpecies() + this.getNSpecies();

        if (includeResource) {
            nAll +=  this.getNRscSpecies();
        }

        // Sum-up diet stages
        for (int cpt = 0; cpt < nAll; cpt++) {
            nColumns += this.getNStage(cpt);
        }

        return nColumns;

    }

    public String[] getHeaders(boolean includeResource) {

        int nAll = this.getNBkgSpecies() + this.getNSpecies();
        if (includeResource) {
            nAll +=  this.getNRscSpecies();
        }

        int nColumns = this.getNValues(includeResource);

        int k = 0;
        String[] headers = new String[nColumns];
        for (int cpt = 0; cpt < nAll; cpt++) {
            String name = getISpecies(cpt).getName();
            float[] threshold = this.getThresholds(cpt);
            int nStage = this.getNStage(cpt);
            for (int s = 0; s < nStage; s++) {
                if (nStage == 1) {
                    headers[k] = name;    // Name predators
                } else {
                    if (s == 0) {
                        // Name predators
                        headers[k] = String.format("%s [%f, %f[", name, 0.f, threshold[s]);
                    } else if (s == nStage - 1) {
                        headers[k] = String.format("%s [%f, inf[", name, threshold[s - 1]);
                    } else {
                        headers[k] = String.format("%s [%f, %f[", name, threshold[s - 1], threshold[s]);
                    }
                }
                k++;
            }
        } // end of species loop

        return headers;

    }


}
