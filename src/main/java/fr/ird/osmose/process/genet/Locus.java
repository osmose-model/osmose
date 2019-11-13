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
package fr.ird.osmose.process.genet;

import fr.ird.osmose.util.OsmoseLinker;

/**
 *
 * @author nbarrier
 */
public class Locus extends OsmoseLinker {

    /**
     * Values of the two Locus (dim=N=2).
     */
    private final double value[];

    /**
     * Number of values for a given loci (alleles). 
     */
    private final int N = 2;
    
    /** Trait that is associated with the given loci. 
     This is a pointer to the trait associated with the given locus*/
    private final Trait trait;
    
    /** Locus index within the given trait. */
    private final int index;
    
    /** Species associated with the index. */
    private final int spec_index;
    
    /**
     * Locus constructor.
     *
     * @param rank
     * @param index
     * @param spec_index
     * @param trait
     */
    public Locus(int index, Trait trait, int spec_index) {

        this.index = index;
        this.trait = trait;
        this.spec_index = spec_index;
        value = new double[N];

    }
    
    /**
     * Random draft of an allele among the n_allele possible.
     */
    public void init_random_draft() {

        // value index in the trait diversity array.
        int valindex;
         
        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (Math.random() * this.trait.getNValues(spec_index));
        value[0] = this.trait.getDiv(spec_index, index, valindex);
        
        // Random draft of a given value in the diversity value for 1st allel
        valindex = (int) (Math.random() * this.trait.getNValues(spec_index));
        value[1] = this.trait.getDiv(spec_index, index, valindex);
        
    }

    /**
     * Set locus values from the parents locus (uniform draft of the parent's
     * locus).
     *
     * @param parent_a
     * @param parent_b
     */
    public void set_from_parents(Locus parent_a, Locus parent_b) {

        // Random draft of an allel (0 or 1) from parent a
        int i = (int) (2 * Math.random());
        value[0] = parent_a.getValue(i);

        // Random draft of an allel (0 or 1) from parent 
        i = (int) (2 * Math.random());
        value[1] = parent_b.getValue(i);

    }

    public double getValue(int i) {
        return this.value[i];
    }

    public double sum() {
        return (this.getValue(0) + this.getValue(1));
    }

}
