/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
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
package fr.ird.osmose.process.mortality.fisheries;

import fr.ird.osmose.process.mortality.fisheries.sizeselect.StepSelectivity;
import fr.ird.osmose.process.mortality.fisheries.sizeselect.SigmoSelectivity;
import fr.ird.osmose.process.mortality.fisheries.sizeselect.GaussSelectivity;
import fr.ird.osmose.process.mortality.*;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import fr.ird.osmose.util.GridMap;

/**
 *
 * @author pverley
 */
public class SingleFisheriesMortality extends AbstractMortality {

    /**
     * Fisheries index.
     */
    private final int fIndex;

    /**
     * Fisherie selectivity function.
     */
    private SizeSelectivity select;

    /**
     * Fisherie time-variability.
     */
    private TimeVariability timeVar;

    /**
     * Fisherie map set.
     */
    private FishingMapSet fMapSet;

    public SingleFisheriesMortality(int rank, int findex) {
        super(rank);
        fIndex = findex;
    }

    @Override
    public void init() {
        Configuration cfg = Osmose.getInstance().getConfiguration();
        String type = cfg.getString("fisheries.select.curve.fis" + fIndex);
        if (type.equals("step")) {
            select = new StepSelectivity(this);
        } else if (type.equals("gauss")) {
            select = new GaussSelectivity(this);
        } else if (type.equals("sigmo")) {
            select = new SigmoSelectivity(this);
        } else {
            error("Selectivity curve " + type + "is not implemented. Choose 'step', 'gauss' or 'sigmo'.", new Exception());
        }

        // Initialize the selectivity curve.
        select.init();

        // Initialize the time varying array
        timeVar = new TimeVariability(this);
        timeVar.init();

        fMapSet = new FishingMapSet(fIndex, "fisheries.fishmap");
        fMapSet.init();

    }

    /**
     * Returns the fishing mortality rate associated with a given fisherie. It
     * is the product of the time varying fishing rate, of the size selectivity
     * and of the spatial factor.
     *
     * @param school
     * @return The fishing mortality rate.
     */
    @Override
    public double getRate(School school) {

        // If the map index is -1 (no map defined), it is assumed that no
        // fishing rate is associated with the current fisherie.
        if (fMapSet.getIndexMap(getSimulation().getIndexTimeSimu()) == -1) {
            return 0;
        }

        // Recovers the school cell (used to recover the map factor)
        Cell cell = school.getCell();

        // Used to recover the selectivity value.
        float selVar;

        // Looks for the selectivity variable.
        switch (select.getVariable()) {
            case AGE:
                selVar = school.getAge();
                break;
            case LEN:
                selVar = school.getLength();
                break;
            default:
                selVar = school.getLength();
                error("Selectivity curve is not implemented.", new Exception());
                break;
        }

        // recovers the time varying rate of the fishing mortality
        double timeSelect = timeVar.getTimeVar(getSimulation().getIndexTimeSimu());

        // Recovers the size/age fisheries selectivity factor [0, 1]
        double sizeSelect = select.getSelectivity(selVar);

        GridMap map = fMapSet.getMap(fMapSet.getIndexMap(getSimulation().getIndexTimeSimu()));
        double spatialSelect;
        if (map != null) {
            spatialSelect = Math.max(0, map.getValue(cell));  // this is done because if value is -999, then no fisheries is applied here.
        } else {
            spatialSelect = 0.0;
        }
        
        // modulates the mortality rate by the the size selectivity and the spatial factor.
        double output = timeSelect * sizeSelect * spatialSelect;

        return output;

    }

    /**
     * Returns the index of the current fisherie.
     * @return The fisherines index
     */
    public int getFIndex() {
        return this.fIndex;
    }

}
