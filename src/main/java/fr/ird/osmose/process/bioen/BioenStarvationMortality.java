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
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;

/**
 *
 * @author nbarrier
 */
public class BioenStarvationMortality extends AbstractProcess {

    

    public BioenStarvationMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        // nothing to do
        // Bioen starvation mortality does not directly rely on user-defined parameters
    }

   public double computeStarvation(School school, int subdt) {

        if (school.getENet() >= 0) {
            // If Enet > 0, maintenance needs have been paid, no starvation mortality
            return 0.d;
        }
        
        // dead individuals (zero by default)
        double ndead = 0.d;
        
        // fraction of ENet deficit at current sub time step (turned into positive value)
        double eNetSubDt = Math.abs(school.getENet()) / subdt;
        
        // check whether ENet deficit can be compensated with gonadic energy
        if (school.getGonadWeight() >= eNetSubDt) {
            // 1. enough gonadic energy
            // pay maintenance with gonadic energy and decrease gonadic energy accordingly
            school.incrementGonadWeight((float) -eNetSubDt);
            school.incrementEnet(eNetSubDt); 
        } else {
            // 2. not enough gonadic energy
            // flush gonadic energy
            school.incrementGonadWeight(-school.getGonadWeight());
            // partially repay ENet with available gonadic energy
            school.incrementEnet(school.getGonadWeight());
            // starvation occurs, as a fraction of energy deficit
            double deathToll = eNetSubDt - school.getGonadWeight();
            ndead = deathToll / school.getWeight();
        }
        
        // return number of dead fish in school at current sub time step
        return ndead;
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
