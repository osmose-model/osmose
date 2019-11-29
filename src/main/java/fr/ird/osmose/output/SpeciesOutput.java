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
import java.io.File;

/**
 *
 * @author nbarrier
 */
public class SpeciesOutput extends AbstractOutput {

    protected double[] value;
    protected double[][] valueReg;
    private final String name;
    private final String description;
    private final SchoolVariableGetter schoolVariable;

    public SpeciesOutput(int rank, String name, String description, SchoolVariableGetter schoolVariable) {
        this(rank, false, name, description, schoolVariable);
    }

    public SpeciesOutput(int rank, boolean regional, String name, String description, SchoolVariableGetter schoolVariable) {
        super(rank, regional);
        this.name = name;
        this.description = description;
        this.schoolVariable = schoolVariable;
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        value = new double[getNSpecies()];
        if (this.saveRegional()) {
            int nregion = Regions.getNRegions();
            valueReg = new double[nregion][];
            for (int i = 0; i < nregion; i++) {
                valueReg[i] = new double[getNSpecies()];
            }
        }
    }

    @Override
    public void update() {

        for (School school : getSchoolSet().getAliveSchools()) {
            if (include(school)) {
                value[school.getSpeciesIndex()] += schoolVariable.getVariable(school);
            }
        }

        if (this.saveRegional()) {
            for (int idom = 0; idom < Regions.getNRegions(); idom++) {
                for (School school : getSchoolSet().getRegionSchools(idom)) {
                    valueReg[idom][school.getSpeciesIndex()] += schoolVariable.getVariable(school);
                }
            }
        }
    }

    @Override
    public void write(float time) {

        double nsteps = getRecordFrequency();
        for (int i = 0; i < value.length; i++) {
            value[i] /= nsteps;
        }
        writeVariable(time, value);

        if (this.saveRegional()) {
            for (int idom = 0; idom < valueReg.length; idom++) {
                for (int i = 0; i < valueReg[idom].length; i++) {
                    valueReg[idom][i] /= nsteps;
                }
                writeVariable(idom + 1, time, valueReg[idom]);
            }
        }
    }

    @Override
    final String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getString("output.file.prefix"));
        filename.append("_").append(name).append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    final String[] getHeaders() {
        String[] species = new String[getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = getSpecies(i).getName();
        }
        return species;
    }

    @Override
    final String getRegionalFilename(int idom) {
        StringBuilder filename = new StringBuilder(getConfiguration().getOutputPathname());
        filename.append(File.separatorChar);
        filename.append("Regional");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_");
        filename.append(Regions.getRegionName(idom));
        filename.append("_").append(name).append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return description;
    }
}
