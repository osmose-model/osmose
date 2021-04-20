 /*
 *OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 *http://www.osmose-model.org
 *
 *Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-today
 *
 *Osmose is a computer program whose purpose is to simulate fish
 *populations and their interactions with their biotic and abiotic environment.
 *OSMOSE is a spatial, multispecies and individual-based model which assumes
 *size-based opportunistic predation based on spatio-temporal co-occurrence
 *and size adequacy between a predator and its prey. It represents fish
 *individuals grouped into schools, which are characterized by their size,
 *weight, age, taxonomy and geographical location, and which undergo major
 *processes of fish life cycle (growth, explicit predation, additional and
 *starvation mortalities, reproduction and migration) and fishing mortalities
 *(Shin and Cury 2001, 2004).
 *
 *Contributor(s):
 *Yunne SHIN (yunne.shin@ird.fr),
 *Morgane TRAVERS (morgane.travers@ifremer.fr)
 *Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 *Philippe VERLEY (philippe.verley@ird.fr)
 *Laure VELEZ (laure.velez@ird.fr)
 *Nicolas Barrier (nicolas.barrier@ird.fr)
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation (version 3 of the License). Full description
 *is provided on the LICENSE file.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package fr.ird.osmose.eco;

import java.util.List;

import fr.ird.osmose.School;
import fr.ird.osmose.stage.SizeStage;
import fr.ird.osmose.util.SimulationLinker;

public class EconomicModule extends SimulationLinker {
    
    private double[][] availableBiomass;
    
    /** Size classes used to integrate fish biomass. 
     * [nSpecies, nClasses].
     * User gives nclasses, then the code add one more classes from 
     * L[-1] to Inf. At the end classes=[L0, L1, L2, INF].
    */
    private double[][] classes;
    
    private SizeStage stage;

    public EconomicModule(int rank) {
        super(rank);
        stage = new SizeStage("economic.size.class");
    }
    
    public void init() {
        
        // total number of species, including the  background species
        int nSpecies = this.getNSpecies();
        classes = new double[nSpecies][];
        availableBiomass = new double[nSpecies][];
        
        int cpt = 0;
        for(int i : this.getFocalIndex()) { 
            String key = String.format("economic.classes.sp%d", i); 
            double temp[] = this.getConfiguration().getArrayDouble(key);
            int nvalues = temp.length;
            classes[cpt] = new double[nvalues + 1];
            availableBiomass[cpt] = new double[nvalues + 1];
            for (int k=0; k<nvalues; k++) { 
                classes[cpt][k] = temp[k];
            }
            classes[cpt][nvalues] = Double.MAX_VALUE;
        }
        
    }

        
    public void assessAvailableBiomass() { 
     
       List<School> listSchool = this.getSchoolSet().getAliveSchools();
       for(School sch : listSchool) { 
            int iSpecies = sch.getSpeciesIndex();   
       }
        
    }
    
    }
