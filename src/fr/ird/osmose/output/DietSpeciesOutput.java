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
import fr.ird.osmose.School.PreyRecord;
import fr.ird.osmose.Species;
import java.io.File;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class DietSpeciesOutput extends AbstractSpectrumOutput {

    private final Species species;
    /**
     * Diet of a predator per stages and per prey [STAGES][SPECIES+PLANKTON+1]
     */
    private double[][] diet;

    public DietSpeciesOutput(int rank, Species species, Type type) {
        super(rank, type);
        this.species = species;
        // Ensure that prey records will be made during the simulation
        getSimulation().requestPreyRecord();
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_dietMatrixPer");
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
        return "Biomass, in tonne, of prey species (in rows) in the diet of predator species per age/size class(in col)";
    }

    @Override
    public void reset() {
        diet = new double[getNClass()][getNSpecies() + getConfiguration().getNPlankton() + 1];
        for (int iClass = 0; iClass < getNClass(); iClass++) {
            diet[iClass][0] = getClassThreshold(iClass);
        }
    }

    @Override
    public void write(float time) {

        writeVariable(time, diet);
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + getConfiguration().getNPlankton() + 1];
        headers[0] = getType().toString();
        for (int i = 0; i < getNSpecies(); i++) {
            headers[i + 1] = getSimulation().getSpecies(i).getName();
        }
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            headers[i + getNSpecies() + 1] = getSimulation().getPlankton(i).getName();
        }
        return headers;
    }

    @Override
    public void update() {

        for (School predator : getSchoolSet().getSchools(species, false)) {
            double preyedBiomass = predator.getPreyedBiomass();
            if (preyedBiomass > 0) {
                for (PreyRecord prey : predator.getPreyRecords()) {
                    int classSchool = getClass(predator);
                    if (classSchool >= 0) {
                        diet[classSchool][prey.getIndex() + 1] += prey.getBiomass();
                    }
                }
            }
        }
    }

    @Override
    public void initStep() {
        // nothing to do
    }

}
