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
package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.stage.DietOutputStage;
import fr.ird.osmose.stage.IStage;
import java.util.HashMap;

/**
 *
 * @author pverley
 */
public class BiomassDietStageOutput extends AbstractOutput {

    private int nColumns;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private HashMap<Integer, double[]> biomassStage = new HashMap();

    private IStage dietOutputStage;

    public BiomassDietStageOutput(int rank) {
        super(rank, "Trophic", "biomassPredPreyIni");
    }

    @Override
    public void init() {

        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();

        nColumns = 0;
        // Sum-up diet stages
        for (int iSpec : getConfiguration().getFocalIndex()) {
            nColumns += dietOutputStage.getNStage(iSpec);
        }
        
        nColumns += getConfiguration().getNRscSpecies() + getConfiguration().getNBkgSpecies();

        super.init();
    }

    @Override
    String getDescription() {
        return "\\ Biomass (tons) of preys at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)";
    }

    @Override
    String[] getHeaders() {

        int k = 0;
        String[] headers = new String[nColumns];
        for (int iSpec : getConfiguration().getFocalIndex()) {
            String name = getSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStage = dietOutputStage.getNStage(iSpec);
            for (int s = 0; s < nStage; s++) {
                if (nStage == 1) {
                    headers[k] = name;    // Name predators
                } else {
                    if (s == 0) {
                        headers[k] = name + " < " + threshold[s];    // Name predators
                    } else {
                        headers[k] = name + " >=" + threshold[s - 1];    // Name predators
                    }
                }
                k++;
            }
        }
        
        for (int iSpec : getConfiguration().getBkgIndex()) {
            headers[k] = getConfiguration().getBkgSpecies(iSpec).getName();
            k++;
        }

        for (int iSpec : getConfiguration().getRscIndex()) {
            headers[k] = getConfiguration().getResourceSpecies(iSpec).getName();
            k++;
        }
        return headers;
    }

    @Override
    public void initStep() {
        getSchoolSet().getPresentSchools().forEach(school -> {
            biomassStage.get(school.getSpeciesIndex())[dietOutputStage.getStage(school)] += school.getBiomass();
        });
                
        for (int iRsc : getConfiguration().getBkgIndex()) {
            biomassStage.get(iRsc)[0] += getTotalBiomass(iRsc);
        }
        
            for (int iRsc : getConfiguration().getRscIndex()) {
            biomassStage.get(iRsc)[0] += getTotalBiomass(iRsc);
        }
        
    }

    @Override
    public void reset() {
        int nSpec = getNSpecies();
        int nPrey = nSpec + getConfiguration().getNRscSpecies();
        biomassStage.clear();
        for (int iSpec : getConfiguration().getFocalIndex()) {
            biomassStage.put(iSpec, new double[dietOutputStage.getNStage(iSpec)]);
        }
        for (int i : getConfiguration().getPreyIndex()) {
            // we consider just 1 stage per resource group
            biomassStage.put(i, new double[1]);
        }
    }

    @Override
    public void update() {
        // nothing to do
    }

    @Override
    public void write(float time) {
        double[] biomass = new double[nColumns];
        double nsteps = getRecordFrequency();
        int k = 0;
        int nSpec = getNSpecies();
        for (int iSpec = 0; iSpec < nSpec; iSpec++) {
            for (int s = 0; s < dietOutputStage.getNStage(iSpec); s++) {
                biomass[k] = biomassStage.get(iSpec)[s] / nsteps;
                k++;
            }
        }
        for (int j : getConfiguration().getFocalIndex()) {
            biomass[k] = biomassStage.get(j)[0] / nsteps;
            k++;
        }
        writeVariable(time, biomass);
    }

    /**
     * Gets the total biomass of the resource groups over the grid.
     *
     * @return the cumulated biomass over the domain in tonne
     */
    private double getTotalBiomass(int iRsc) {
        double biomTot = 0.d;
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                biomTot += getSimulation().getResourceForcing(iRsc).getBiomass(cell);
            }
        }
        return biomTot;
    }

}
