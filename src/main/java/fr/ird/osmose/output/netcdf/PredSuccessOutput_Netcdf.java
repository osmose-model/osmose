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

/**
 *
 * @author pverley
 */
public class PredSuccessOutput_Netcdf extends AbstractOutput_Netcdf {

    private double[] predSuccess;
    private double[] nschool;

    public PredSuccessOutput_Netcdf(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        predSuccess = new double[getNSpecies()];
        nschool = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
//            if (school.getPredSuccessRate() >= 0.57) {
//                predSuccess[school.getSpeciesIndex()] += 1;
//            }
            predSuccess[school.getSpeciesIndex()] += school.getPredSuccessRate();
            nschool[school.getSpeciesIndex()] += 1;
        }
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < predSuccess.length; i++) {
            predSuccess[i] /= (nschool[i]);
        }
        writeVariable(time, predSuccess);
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
         filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_predsuccess_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Predation success rate per species.";
    }

    @Override
    String getUnits() {
        throw new UnsupportedOperationException(""); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        throw new UnsupportedOperationException("predation_success"); //To change body of generated methods, choose Tools | Templates.
    }

}
