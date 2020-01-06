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

import fr.ird.osmose.process.mortality.FishingGear;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.OsmoseLinker;

/**
 * 
 * @todo Eventually Move the selectivity into Interface, with three different classes (Step, Gaussian and Sigmo) 
 * @author nbarrier
 */
public abstract class SizeSelectivity extends OsmoseLinker {
    
    /**
     * Type of the selectivity variable (age or size).
     */
    private SizeSelectivity.Variable variable;
    
    /** L50 size. Used in all three types of selectivities. */
    private double l50;
    
     /** Tiny size. Size below which no fish is captured. */
    private double tiny = 1.e-6;
    
    /** Pointer to the fishery mortality array. 
     * Allows to recover the fishery index and the MPI rank.
     */
    private final FishingGear mort;
    
    /**
     * Public constructor. Initialize the FisheryMortality pointer.
     *
     * @param fmort
     */
    public SizeSelectivity(FishingGear fmort) {
        this.mort = fmort;
        this.init_var();
    }
    
    /** Initializes the selectivity class. The selectivity parameters
     * are initialized from the configuration file.
     * The number of parameters depends on the selectivity curve.
     */
    public final void init_var() {
        
        int index = mort.getFIndex();
        
        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        // Initialize the selectivity variable 
        String var = cfg.getString("fishery.selectivity.structure.fsh" + index);
        if (var.equals("age")) {
            this.variable = SizeSelectivity.Variable.AGE;
        } else {
            this.variable = SizeSelectivity.Variable.SIZE;
        }
        
        // Init the l50 variable
        this.l50 = cfg.getFloat("fishery.selectivity.l50.fsh" + index);

        // if tiny parameter exists, set tiny. Else, use default
        if(cfg.canFind("fishery.selectivity.tiny.fsh" + index)) {
            this.tiny  = cfg.getFloat("fishery.selectivity.tiny.fsh" + index);
        }
    }
    
    /**
     * Returns a selectivity value. It depends on the size of the specieand
     * on the selectivity curve and parameters. Output value is 
     * between 0 and 1.
     * @param size Specie size
     * @return A selectivity value (0<output<1)
     */
    public abstract double getSelectivity(double size);
    
    
    /** Abstract init method. */
    public abstract void init();
   
    /** Returns the selectivity variable.
     * @return  */
    public Variable getVariable() {
        return this.variable;
    }
    
    public enum  Variable {
        SIZE,
        AGE,
    }
    
    public FishingGear getGear() {
        return this.mort;
    }
    
    public double getL50() {
        return this.l50;
    }

    public double getTiny() {
        return this.tiny;
    }
}
