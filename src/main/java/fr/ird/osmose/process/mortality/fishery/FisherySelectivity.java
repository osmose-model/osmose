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
package fr.ird.osmose.process.mortality.fishery;

import fr.ird.osmose.process.mortality.*;
import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.fishery.FisheryFBase;
import fr.ird.osmose.process.mortality.fishery.FisherySeason;
import fr.ird.osmose.process.mortality.fishery.FisherySeasonality;
import fr.ird.osmose.process.mortality.fishery.FisheryMapSet;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.BySpeciesTimeSeries;
import fr.ird.osmose.util.timeseries.ByRegimeTimeSeries;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import fr.ird.osmose.util.version.VersionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 *
 * @author pverley
 */
public class FisherySelectivity extends OsmoseLinker {

    /**
     * Fishery index.
     */
    private final int fIndex;
    
    /** Prefix used to define parameters. */
    private String selPrefix;

    private String selSuffix;
    
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

    /**
     * Determines which function should be used to compute the selectivity.
     */
    private interface SizeSelect {

        double getSelectivity(int index, AbstractSchool school);
    }

  
    public FisherySelectivity(int findex, String prefix, String suffix) {
        this.fIndex = findex;
        this.selPrefix = prefix;
        this.selSuffix = suffix;
    }

    /**
     * Interface to allow recovering the size/age of species. Both background
     * and focal.
     */
    private VarGetter varGetter;

    /**
     * Determines which School variable should be used to compute the
     * selectivity.
     */
    private interface VarGetter {

        public double getVariable(AbstractSchool school);
    }

    public void init() {

        Configuration cfg = this.getConfiguration();
        String key;
        
        key = String.format("%s.tiny.%s%d", selPrefix, selSuffix, fIndex);
        // if tiny parameter exists, set tiny. Else, use default
        if (!cfg.isNull(key)) {
            this.tiny = cfg.getFloat(key);
        }
        
        key = String.format("%s.a50.%s%d", selPrefix, selSuffix, fIndex);
        if (!cfg.isNull(key)) {
            varGetter = (school) -> (school.getAge());
            this.initByAge();
        } else {
            varGetter = (school) -> (school.getLength());
            this.initByLength();
        }

        // Init the size selectivity array
        select = new SizeSelect[3];
        select[0] = (index, sch) -> this.getKnifeEdgeSelectivity(index, sch);  // knife edge selectivity
        select[1] = (index, sch) -> this.getSigmoidSelectivity(index, sch);    // Sigmoid selectivity
        select[2] = (index, sch) -> this.getGaussianSelectivity(index, sch);   // Gaussian selectivity

    }

    /** Init the variables need to compute age selectivity.
     * Only knife-edge can be used with age.
     */
    private void initByAge() {

        String prefix = selPrefix + ".a50";
        this.l50_array = this.initArray(prefix);

        this.selectType_array = new double[l50_array.length];
        for (int i = 0; i < selectType_array.length; i++) {
            selectType_array[i] = 0.d;
        }
    }

    public void initByLength() {

        String prefix;

        prefix = selPrefix + ".l50";
        this.l50_array = this.initArray(prefix);

        prefix = selPrefix + ".type";
        this.selectType_array = this.initArray(prefix);

        double sum = 0.;
        for (double v : this.selectType_array) {
            sum += v;
        }

        if (sum != 0) {
            prefix = selPrefix + ".l75";
            this.l75_array = this.initArray(prefix);
        }

    }

    /** Computes the selectivity for a given school.
     * 
     * @param school
     * @return 
     */
    public double getSelectivity(int index, AbstractSchool school) {
        
        int selType = (int) this.selectType_array[index];
        return (select[selType].getSelectivity(index, school));

    }

    /**
     * Computes the knife edge selectivity.
     *
     * @param school
     * @return
     */
    public double getKnifeEdgeSelectivity(int index, AbstractSchool school) {

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
    public double getGaussianSelectivity(int index, AbstractSchool school) {

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
    public double getSigmoidSelectivity(int index, AbstractSchool school) {

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

    /** Init an array either from file (by dt) or shifts.
     * 
     * @param prefix
     * @return 
     */
    private double[] initArray(String prefix) {
        
        String keyVal, keyShift;
        Configuration cfg = this.getConfiguration();
        double[] array;
        
        keyVal = String.format("%s.file.%s%d", prefix, this.selSuffix, fIndex);
        if (cfg.canFind(keyVal)) {
            SingleTimeSeries ts = new SingleTimeSeries();
            ts.read(cfg.getFile(keyVal));
            array = ts.getValues();
        } else {
            keyShift = String.format("%s.shift.%s%d", prefix, this.selSuffix, fIndex);
            keyVal = String.format("%s.%s%d", prefix, this.selSuffix, fIndex);
            ByRegimeTimeSeries rts = new ByRegimeTimeSeries(keyShift, keyVal);
            rts.init();
            array = rts.getValues();
        }
        
        return array;
        
    }
        
} // end of class
