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

import fr.ird.osmose.School;
import fr.ird.osmose.Prey;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.distribution.AbstractDistribution;
import java.io.File;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class DietDistribOutput extends AbstractDistribOutput {

    private final Species species;

    public DietDistribOutput(int rank, Species species, AbstractDistribution distrib) {
        super(rank, distrib);
        this.species = species;
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
    }
    
    @Override
    public void reset() {
        values = new double[getNSpecies() + getConfiguration().getNPlankton()][getNClass()];
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_dietMatrixby");
        filename.append(getType().toString());
        filename.append("-");
        filename.append(species.getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Distribution of the biomass (tonne) of prey species (in columns) in the diet of ");
        description.append(species.getName());
        description.append(" by ");
        description.append(getType().getDescription());
        description.append(". For class i, the preyed biomass in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + getConfiguration().getNPlankton() + 1];
        headers[0] = getType().toString();
        for (int i = 0; i < getNSpecies(); i++) {
            headers[i + 1] = getSpecies(i).getName();
        }
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            headers[i + getNSpecies() + 1] = getConfiguration().getPlankton(i).getName();
        }
        return headers;
    }

    @Override
    public void update() {

        for (School predator : getSchoolSet().getSchools(species, false)) {
            double preyedBiomass = predator.getPreyedBiomass();
            if (preyedBiomass > 0) {
                for (Prey prey : predator.getPreys()) {
                    int classPredator = getClass(predator);
                    if (classPredator >= 0) {
                        values[prey.getSpeciesIndex()][classPredator] += prey.getBiomass();
                    }
                }
            }
        }
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    String getRegionalFilename(int idom) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
