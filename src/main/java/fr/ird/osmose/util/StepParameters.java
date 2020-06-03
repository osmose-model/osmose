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
package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;

/** 
 * Class that manages the initialisation of step parameters.
 * 
 * Seasons can be provided as a list of steps
 * 
 * Seasons that exceed the number of steps per year are discarded.
 *
 * @author Nicolas
 */
public class StepParameters extends OsmoseLinker {

    private final String prefix;
    private final String suffix;
    private int[] season;

    public StepParameters(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.init();
    }

    public void init() {
        
        int ymax, ymin;
        int[] tempYears;

        Configuration conf = this.getConfiguration();

        int nseason = getConfiguration().getNStepYear();
        String key = String.format("%s.steps.%s", this.prefix, this.suffix);
        
        int[] tmpSeason;
        if (!conf.isNull(key)) {
            tmpSeason = conf.getArrayInt(key);
        } else {
            tmpSeason = new int[nseason];
            for (int s = 0; s < nseason; s++) {
                tmpSeason[s] = s;
            }
        }
        
        int goodseason = 0;
        for(int s : tmpSeason) {
            if(s < nseason) {
                goodseason++;
            }
        }
        
        season = new int[goodseason];
        int cpt = 0;
        for (int s : tmpSeason) {
            if (s < nseason) {
                season[cpt] = s;
                cpt++;
            }
        }
        
    }

    public int[] getSeasons() {
        return this.season;
    }

}
