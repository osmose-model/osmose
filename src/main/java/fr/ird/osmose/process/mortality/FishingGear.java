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
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.fishery.FisheryFBase;
import fr.ird.osmose.process.mortality.fishery.FisherySeason;
import fr.ird.osmose.process.mortality.fishery.FisherySeasonality;
import fr.ird.osmose.process.mortality.fishery.FishingMapSet;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.timeseries.BySpeciesTimeSeries;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import java.util.Arrays;
import org.apache.commons.math3.distribution.NormalDistribution;


/**
 *
 * @author pverley
 */
public class FishingGear extends AbstractMortality {

    private String name;

    /**
     * Fishery index.
     */
    private final int fIndex;

    // Initialize the time varying array
    private FisheryFBase fBase;
    private FisherySeason fSeason;
    private FisherySeasonality fSeasonality;

    /**
     * Fishery map set.
     */
    private FishingMapSet fMapSet;

    /**
     * Fishery accessibility file. Provides the catchability for the given
     * fishery. Function of time and species.
     */
    private BySpeciesTimeSeries catchability;

    /**
     * Array of l50 values. One value per time step.
     */
    private double[] l50_array;

    /**
     * Array of l75 values. One value per time step.
     */
    private double[] l75_array;

    /**
     * Value below which selectivity is forced to 0.
     */
    private double tiny = 0;

    /**
     * Array of seelectivity types. One value per time step.
     */
    private double[] selectType_array;

    /**
     * Array of selectivity methods. 0 = knife edge. 1 = Sigmoid 2 = Guaussian
     */
    private SizeSelect select[];

    private interface SizeSelect {

        double getSelectivity(AbstractSchool school);
    }

    /**
     * Interface to allow recovering the size/age of species. Both background
     * and focal.
     */
    private VarGetter varGetter;

    private interface VarGetter {

        public double getVariable(AbstractSchool school);
    }

    public FishingGear(int rank, int findex) {
        super(rank);
        fIndex = findex;
    }

    @Override
    public void init() {
        
        Configuration cfg = Osmose.getInstance().getConfiguration();

        if (cfg.canFind("fishery.selectivity.variable.fsh" + fIndex)) {
            String selVar = cfg.getString("fishery.selectivity.variable.fsh" + fIndex);
            if (selVar.equals("age")) {
                varGetter = (school) -> (school.getAge());
            } else {
                varGetter = (school) -> (school.getLength());
            }
        } else {
            varGetter = (school) -> (school.getLength());
        }

        // Recover the fishery name
        this.name = cfg.getString("fishery.name.fsh" + fIndex);

        // if tiny parameter exists, set tiny. Else, use default
        if (cfg.canFind("fishery.selectivity.tiny.fsh" + fIndex)) {
            this.tiny = cfg.getFloat("fishery.selectivity.tiny.fsh" + fIndex);
        }

        SingleTimeSeries ts = new SingleTimeSeries();
        ts.read(this.getConfiguration().getFile("fishery.l50.file.fsh" + fIndex));
        this.l50_array = ts.getValues();

        ts.read(this.getConfiguration().getFile("fishery.selectivity.type.file.fsh" + fIndex));
        this.selectType_array = ts.getValues();
        
        double sum = Arrays.stream(this.selectType_array).sum();
        if (sum != 0) {
            ts.read(this.getConfiguration().getFile("fishery.l75.file.fsh" + fIndex));
            this.l75_array = ts.getValues();
        }

        // Initialize the time varying array
        FisheryFBase fBase = new FisheryFBase(fIndex);
        FisherySeason fSeason = new FisherySeason(fIndex);
        FisherySeasonality fSeasonality = new FisherySeasonality(fIndex);

        // fishery spatial maps
        fMapSet = new FishingMapSet(fIndex, "fishery.movement");
        fMapSet.init();

        // accessibility matrix
        // (it provides the percentage of fishes that are going to be captured)
        this.catchability = new BySpeciesTimeSeries();
        catchability.read(getConfiguration().getFile("fishery.catchability.file.fsh" + fIndex));

        // Init the size selectivity array
        select = new SizeSelect[3];
        select[0] = (sch -> this.getKnifeEdgeSelectivity(sch));  // knife edge selectivity
        select[1] = (sch -> this.getSigmoidSelectivity(sch));    // Sigmoid selectivity
        select[2] = (sch -> this.getGaussianSelectivity(sch));   // Gaussian selectivity
        
    }

    /**
     * Returns the fishing mortality rate associated with a given fishery. It is
     * the product of the time varying fishing rate, of the size selectivity and
     * of the spatial factor.
     *
     * @param school
     * @return The fishing mortality rate.
     */
    public double getRate(AbstractSchool school) throws Exception {

        int index = getSimulation().getIndexTimeSimu();

        // If the map index is -1 (no map defined), it is assumed that no
        // fishing rate is associated with the current fisherie.
        if (fMapSet.getIndexMap(index) == -1) {
            return 0;
        }

        double speciesCatchability = this.catchability.getValue(index, school.getSpeciesName());
        if (speciesCatchability == 0.d) {
            return 0.d;
        }

        // Recovers the school cell (used to recover the map factor)
        Cell cell = school.getCell();

        // recovers the time varying rate of the fishing mortality
        // as a product of FBase, FSeason and FSeasonality
        double timeSelect = fBase.getFBase(index);
        timeSelect *= this.fSeason.getSeasonFishMort(index) ;
        timeSelect *= this.fSeasonality.getSeasonalityFishMort(index);

        int selectIndex = (int) this.selectType_array[index];

        // Recovers the size/age fishery selectivity factor [0, 1]
        double sizeSelect = select[selectIndex].getSelectivity(school);

        GridMap map = fMapSet.getMap(fMapSet.getIndexMap(getSimulation().getIndexTimeSimu()));
        double spatialSelect;
        if (map != null) {
            spatialSelect = Math.max(0, map.getValue(cell));  // this is done because if value is -999, then no fishery is applied here.
        } else {
            spatialSelect = 0.0;
        }

        return speciesCatchability * timeSelect * sizeSelect * spatialSelect;
    }

    /**
     * Returns the fishery index.
     *
     * @return the fishery index
     */
    public int getFIndex() {
        return this.fIndex;
    }

    /**
     * Returns the fishery name.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of the fishery.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public double getRate(School school) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Computes the knife edge selectivity.
     *
     * @param school
     * @return
     */
    public double getKnifeEdgeSelectivity(AbstractSchool school) {

        int index = this.getSimulation().getIndexTimeSimu();

        double l50 = this.l50_array[index];

        double output = (varGetter.getVariable(school) < l50) ? 0 : 1;
        return output;
    }

    /**
     * Computes the Gaussian selectivity.
     *
     * @param school
     * @return
     */
    public double getGaussianSelectivity(AbstractSchool school) {

        int index = this.getSimulation().getIndexTimeSimu();

        double l50 = this.l50_array[index];
        double l75 = this.l75_array[index];

        NormalDistribution norm = new NormalDistribution();

        double sd = (l75 - l50) / norm.inverseCumulativeProbability(0.75);  // this is the qnorm function
        // initialisation of the distribution used in selectity calculation
        NormalDistribution distrib = new NormalDistribution(l50, sd);

        double output;
        // calculation of selectivity. Normalisation by the maximum value 
        // (i.e. the value computed with x = mean).
        // If L75 > 0, assumes Ricardo Formulation should be used
        output = distrib.density(varGetter.getVariable(school)) / distrib.density(l50);

        if (output < tiny) {
            output = 0.0;
        }

        return output;

    }
 
    /**
     * Computes the sigmoid selectivity.
     *
     * @param school
     * @return
     */
    public double getSigmoidSelectivity(AbstractSchool school) {

        int index = this.getSimulation().getIndexTimeSimu();
        double l50 = this.l50_array[index];
        double l75 = this.l75_array[index];

        double s1 = (l50 * Math.log(3)) / (l75 - l50);
        double s2 = s1 / l50;

        double output;

        output = 1 / (1 + Math.exp(s1 - (s2 * varGetter.getVariable(school))));

        if (output < tiny) {
            output = 0.0;
        }

        return output;

    }
}
