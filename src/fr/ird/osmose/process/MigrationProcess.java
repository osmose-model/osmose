/*
 # * To change this template, choose Tools | Templates
 # * and open the template in the editor.
 Copyright IRD (2013)
 Contributors: Philippe VERLEY

 philippe.verley@ird.fr

 This software is a computer program whose purpose is to [describe
 functionalities and technical features of your software].

 This software is governed by the CeCILL-B license under French law and
 abiding by the rules of distribution of free software.  You can  use, 
 modify and/ or redistribute the software under the terms of the CeCILL-B
 license as circulated by CEA, CNRS and INRIA at the following URL
 "http://www.cecill.info". 

 As a counterpart to the access to the source code and  rights to copy,
 modify and redistribute granted by the license, users are provided only
 with a limited warranty  and the software's author,  the holder of the
 economic rights,  and the successive licensors  have only  limited
 liability. 

 In this respect, the user's attention is drawn to the risks associated
 with loading,  using,  modifying and/or developing or reproducing the
 software by the user in light of its specific status of free software,
 that may mean  that it is complicated to manipulate,  and  that  also
 therefore means  that it is reserved for developers  and  experienced
 professionals having in-depth computer knowledge. Users are therefore
 encouraged to load and test the software's suitability as regards their
 requirements in conditions enabling the security of their systems and/or 
 data to be ensured and,  more generally, to use and operate it in the 
 same conditions as regards security. 

 The fact that you are presently reading this means that you have had
 knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class MigrationProcess extends AbstractProcess {

    /*
     * Migration
     */
    private boolean[][][] outOfZoneCohort;
    private static float[][][] outOfZoneMortality;

    public MigrationProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        // init migration
        outOfZoneCohort = new boolean[nSpecies][][];
        outOfZoneMortality = new float[getNSpecies()][][];
        for (int index = 0; index < nSpecies; index++) {
            int lifespan = getSpecies(index).getLifespanDt();
            outOfZoneMortality[index] = new float[lifespan][getConfiguration().getNumberTimeStepsPerYear()];
            outOfZoneCohort[index] = new boolean[lifespan][getConfiguration().getNumberTimeStepsPerYear()];

            if (getConfiguration().canFind("migration.ageclass.sp" + index)) {
                int nbStepYear = getConfiguration().getNumberTimeStepsPerYear();
                int[] ageclass = getConfiguration().getArrayInt("migration.ageclass.sp" + index);
                int[] season = getConfiguration().getArrayInt("migration.season.sp" + index);
                float[] rate = getConfiguration().getArrayFloat("migration.mortality.rate.sp" + index);
                for (int m = 0; m < ageclass.length; m++) {
                    for (int n = 0; n < season.length; n++) {
                        for (int h = 0; h < nbStepYear; h++) {
                            outOfZoneCohort[index][ageclass[m] * nbStepYear + h][season[n]] = true;
                            outOfZoneMortality[index][ageclass[m] * nbStepYear + h][season[n]] = rate[m];
                        }
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        // nothing to do
    }

    boolean isOut(School school) {
        return outOfZoneCohort[school.getSpeciesIndex()][school.getAgeDt()][getSimulation().getIndexTimeYear()];
    }

    float getOutMortality(School school) {
        return outOfZoneMortality[school.getSpeciesIndex()][school.getAgeDt()][getSimulation().getIndexTimeYear()];
    }
}
