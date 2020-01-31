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
package fr.ird.osmose.process.mortality.fishery.sizeselect;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.FishingGear;

/**
 *
 * @todo Eventually Move the selectivity into Interface, with three different
 * classes (Step, Gaussian and Sigmo)
 * @author nbarrier
 */
public class KnifeEdgeSelectivity extends SizeSelectivity {

    /**
     * Public constructor. Initialize the FisheryMortality pointer.
     *
     * @param fmort
     */
    public KnifeEdgeSelectivity(FishingGear fmort) {
        super(fmort);
    }

    /**
     * Initializes the selectivity class. For step, only L50 is used. Hence
     * no more init is used.
     * 
     */
    @Override
    public void init() {

    }

    /**
     * Returns a selectivity value. It depends on the size of the specieand on
     * the selectivity curve and parameters. Output value is between 0 and 1.
     *
     * @param size Specie size
     * @return A selectivity value (0<output<1)
     */
    @Override
    public double getSelectivity(AbstractSchool school) {

        double output = school.getLength() < this.getL50() ? 0 : 1;
        return output;
    }
}
