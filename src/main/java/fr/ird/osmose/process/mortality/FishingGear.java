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
import fr.ird.osmose.process.mortality.fishery.sizeselect.KnifeEdgeSelectivity;
import fr.ird.osmose.process.mortality.fishery.sizeselect.SigmoSelectivity;
import fr.ird.osmose.process.mortality.fishery.sizeselect.GaussSelectivity;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.fishery.AccessMatrix;
import fr.ird.osmose.process.mortality.fishery.FishingMapSet;
import fr.ird.osmose.process.mortality.fishery.sizeselect.SizeSelectivity;
import fr.ird.osmose.process.mortality.fishery.TimeVariability;
import fr.ird.osmose.util.GridMap;
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

    /**
     * Fishery time-variability.
     */
    private TimeVariability timeVar;

    /**
     * Fishery map set.
     */
    private FishingMapSet fMapSet;
    
    /**
     * Fishery accessibility.
     */
    private double[] accessibility;
    
    /** Array of l50 values. One value per time step. */
    private double[] l50_array;
    
    /** Array of l50 values. One value per time step. */
    private double[] l75_array;
    
    /** Value below which selectivity is forced to 0. */
    private double tiny = 0;
    
    /** Array of seelectivity types. One value per time step. */
    private int[] selectType_array;
    
    /** Array of selectivity type. */
    private SizeSelect select[];
    
    private interface SizeSelect { 
        double getSelectivity(AbstractSchool school);
    }

    public FishingGear(int rank, int findex) {
        super(rank);
        fIndex = findex;
    }

    @Override
    public void init() {

        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        // Recover the fishery name
        this.name = cfg.getString("fishery.name.fsh" + fIndex);
        
        // if tiny parameter exists, set tiny. Else, use default
        if (cfg.canFind("fishery.selectivity.tiny.fsh" + fIndex)) {
            this.tiny = cfg.getFloat("fishery.selectivity.tiny.fsh" + fIndex);
        }
        
        this.l50_array = this.getConfiguration().getArrayDouble("fishery.l50.fsh" + fIndex);
        this.l75_array = this.getConfiguration().getArrayDouble("fishery.l75.fsh" + fIndex);
        this.selectType_array = this.getConfiguration().getArrayInt("fishery.selectivity.fsh" + fIndex);

        // Initialize the time varying array
        timeVar = new TimeVariability(this);
        timeVar.init();

        // fishery spatial maps
        fMapSet = new FishingMapSet(fIndex, "fishery.movement");
        fMapSet.init();

        // accessibility matrix
        // (it provides the percentage of fishes that are going to be captured)
        AccessMatrix accessMatrix = new AccessMatrix();
        accessMatrix.read(getConfiguration().getFile("fishery.catch.matrix.file"));
        accessibility = accessMatrix.getValues(fIndex);  // accessibility for one gear (nspecies).
        
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
    public double getRate(AbstractSchool school) {

        // If the map index is -1 (no map defined), it is assumed that no
        // fishing rate is associated with the current fisherie.
        if (fMapSet.getIndexMap(getSimulation().getIndexTimeSimu()) == -1) {
            return 0;
        }

        double speciesAccessibility = accessibility[school.getSpeciesIndex()];
        if (speciesAccessibility == 0.d) {
            return 0.d;
        }

        // Recovers the school cell (used to recover the map factor)
        Cell cell = school.getCell();

        int selectIndex =  this.getSimulation().getIndexTimeSimu() % selectType_array.length;

        // recovers the time varying rate of the fishing mortality
        double timeSelect = timeVar.getTimeVar(getSimulation().getIndexTimeSimu());

        // Recovers the size/age fishery selectivity factor [0, 1]
        double sizeSelect = select[selectIndex].getSelectivity(school);

        GridMap map = fMapSet.getMap(fMapSet.getIndexMap(getSimulation().getIndexTimeSimu()));
        double spatialSelect;
        if (map != null) {
            spatialSelect = Math.max(0, map.getValue(cell));  // this is done because if value is -999, then no fishery is applied here.
        } else {
            spatialSelect = 0.0;
        }

        return speciesAccessibility * timeSelect * sizeSelect * spatialSelect;
    }

    /**
     * Returns the fishery index.
     *
     * @return the fishery index
     */
    public int getFIndex() {
        return this.fIndex;
    }
    
    /** Returns the fishery name. 
     * 
     * @return 
     */
    public String getName() {
        return this.name;
    }
    
    /** Sets the name of the fishery.
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
    
    /** Computes the knife edge selectivity.
     * @param school
     * @return 
     */
    public double getKnifeEdgeSelectivity(AbstractSchool school) {
        
        double l50 = this.getArrayVal(this.l50_array);
        
        double output = (school.getLength() < l50) ? 0 : 1;
        return output;
    }

    /** Computes the Gaussian selectivity.
     * 
     * @param school
     * @return 
     */
    public double getGaussianSelectivity(AbstractSchool school) {
        
        double l50 = this.getArrayVal(this.l50_array);
        double l75 = this.getArrayVal(this.l75_array);
        
        NormalDistribution norm = new NormalDistribution();
        
        double sd = (l75 - l50) / norm.inverseCumulativeProbability(0.75);  // this is the qnorm function
        // initialisation of the distribution used in selectity calculation
        NormalDistribution distrib = new NormalDistribution(l50, sd);
        
        double output;
        // calculation of selectivity. Normalisation by the maximum value 
        // (i.e. the value computed with x = mean).
        // If L75 > 0, assumes Ricardo Formulation should be used
        output = distrib.density(school.getLength()) / distrib.density(l50);

        if (output < tiny) {
            output = 0.0;
        }

        return output;
        
    }

    /** Computes the sigmoid selectivity. 
     * 
     * @param school
     * @return 
     */
    public double getSigmoidSelectivity(AbstractSchool school) {
        
        double l50 = this.getArrayVal(this.l50_array);
        double l75 = this.getArrayVal(this.l75_array);

        double s1 = (l50 * Math.log(3)) / (l75 - l50);
        double s2 = s1 / l50;

        double output;

        output = 1 / (1 + Math.exp(s1 - (s2 * school.getLength())));

        if (output < tiny) {
            output = 0.0;
        }

        return output;

    }
    
    
    /** Recovers the value of an array by recycling values. 
     * 
     * @param input Input array (l50 for instance)
     * @return 
     */
    private double getArrayVal(double[] input) {      
        int length = input.length;
        int index = this.getSimulation().getIndexTimeSimu() % length;
        return input[index];    
    }
    
}
