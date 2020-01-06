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
package fr.ird.osmose.output.netcdf;

import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.MortalityCause;
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanTrophicLevelCatchOutput_Netcdf extends AbstractOutput_Netcdf {

    private double[] meanTLCatch;
    private double[] yield;

    public MeanTrophicLevelCatchOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        meanTLCatch = new double[getNSpecies()];
        yield = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int i = school.getSpeciesIndex();
            meanTLCatch[i] += school.getTrophicLevel() * school.abd2biom(school.getNdead(MortalityCause.FISHING));
            yield[i] += school.abd2biom(school.getNdead(MortalityCause.FISHING));
        }
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (yield[i] > 0) {
                meanTLCatch[i] = meanTLCatch[i] / yield[i];
            } else {
                meanTLCatch[i] = Double.NaN;
            }
        }
        writeVariable(time, meanTLCatch);
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanTLCatch_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean Trophic Level of fish species, weighted by fish catch, and including first ages specified in input";
    }

    @Override
    String getUnits() {
        return(""); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return("trophic_level"); //To change body of generated methods, choose Tools | Templates.
    }
}
