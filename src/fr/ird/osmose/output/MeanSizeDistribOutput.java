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
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.output.distribution.AbstractDistribution;
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanSizeDistribOutput extends AbstractMeanDistribOutput {

    public MeanSizeDistribOutput(int rank, AbstractDistribution distrib) {
        super(rank, distrib);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getType().toString());
        filename.append("Indicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanSizeDistribBy");
        filename.append(getType().toString());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Mean size of fish (centimeter) by ");
        description.append(getType().getDescription());
        description.append(". For class i, the mean size in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int iSpec = school.getSpeciesIndex();
            int iClass = getClass(school);
            if (iClass >= 0) {
                values[iSpec][iClass] += school.getInstantaneousAbundance() * school.getLength();
                denominator[iSpec][iClass] += school.getInstantaneousAbundance();
            }
        }
    }
}
