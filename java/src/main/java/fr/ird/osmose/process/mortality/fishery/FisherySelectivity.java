/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
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
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.process.mortality.fishery;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Configuration;

import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.ByRegimeTimeSeries;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import org.apache.commons.math3.distribution.LogNormalDistribution;
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

    /**
     * Prefix used to define parameters.
     */
    private final String selPrefix;

    private final String selSuffix;

    /**
     * Array of l50 values. One value per time step.
     */
    private double[] l50Array;

    /**
     * Array of l75 values. One value per time step.
     */
    private double[] l75Array;

    /**
     * Value below which selectivity is forced to 0.
     */
    private double tiny = 0;

    /**
     * Array of seelectivity types. One value per time step.
     */
    private double[] selectTypeArray;

    /**
     * Array of selectivity methods. 0 = knife edge. 1 = Sigmoid 2 = Gaussian 3 = logNormal
     */
    private SizeSelect sizeSelectMethods[];
    
    /** Object for the normal distribution of selectivities. */
    private NormalDistribution normDistrib;
    
    /** Object for the lognormal distribution of selectivites. */
    private LogNormalDistribution logNormDistrib;
    
    /** True if selectivity should be initialized. True only if selectivity type changes. */
    private boolean initSelectivity;

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
        this.initSelectivity = true;
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
        sizeSelectMethods = new SizeSelect[4];
        sizeSelectMethods[0] = (index, sch) -> this.getKnifeEdgeSelectivity(index, sch);  // knife edge selectivity
        sizeSelectMethods[1] = (index, sch) -> this.getSigmoidSelectivity(index, sch);    // Sigmoid selectivity
        sizeSelectMethods[2] = (index, sch) -> this.getGaussianSelectivity(index, sch);   // Gaussian selectivity
        sizeSelectMethods[3] = (index, sch) -> this.getLogNormalSelectivity(index, sch);   // Log-normal selectivity

    }

    /**
     * Init the variables need to compute age selectivity. Only knife-edge can
     * be used with age.
     */
    private void initByAge() {

        String prefix = selPrefix + ".a50";
        this.l50Array = this.initArray(prefix);

        this.selectTypeArray = new double[l50Array.length];
        for (int i = 0; i < selectTypeArray.length; i++) {
            selectTypeArray[i] = 0.d;
        }
    }

    public void initByLength() {

        String prefix;

        prefix = selPrefix + ".l50";
        this.l50Array = this.initArray(prefix);

        prefix = selPrefix + ".type";
        this.selectTypeArray = this.initArray(prefix);

        double sum = 0.;
        for (double v : this.selectTypeArray) {
            sum += v;
        }

        if (sum != 0) {
            prefix = selPrefix + ".l75";
            this.l75Array = this.initArray(prefix);
        }

    }

    /**
     * Computes the selectivity for a given school.
     *
     * @param school
     * @return
     */
    public double getSelectivity(int index, AbstractSchool school) {
        int selType = (int) this.selectTypeArray[index];
        return sizeSelectMethods[selType].getSelectivity(index, school);
    }
    
    public void checkIfUpdatedSel(int index) {
        
        if(this.initSelectivity) {
            // when init is true (first simulated time step), nothing is done 
            return;
        }
        
        if(this.selectTypeArray[index] != this.selectTypeArray[index - 1]) {
            // if the selectivity type has changed, reinit the objects
            this.initSelectivity = true;
            return;
        }
        
        if(this.l50Array[index] != this.l50Array[index - 1]) {
            // if the l50 value has changed, reinit the objects
            this.initSelectivity = true;
            return;
        }
        
        if ((this.l75Array != null) && ((this.l75Array[index] != this.l75Array[index - 1]))) {
            // if the l75 value has changed, reinit the objects
            this.initSelectivity = true;
            return;
        }
    }

    /**
     * Computes the knife edge selectivity.
     *
     * @param school
     * @return
     */
    public double getKnifeEdgeSelectivity(int index, AbstractSchool school) {
        double l50 = this.l50Array[index];
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

        double l50 = this.l50Array[index];
        double l75 = this.l75Array[index];
        double q75 = 0.674489750196082; // declare constant to save time
        // this is the qnorm(0.75)
        double sd = (l75 - l50) / q75; // this is the qnorm function
        if (this.initSelectivity) {
            // initialisation of the distribution used in selectity calculation
            normDistrib = new NormalDistribution(l50, sd);
            this.initSelectivity = false;
        }

        // calculation of selectivity. Normalisation by the maximum value 
        // (i.e. the value computed with x = mean).
        // If L75 > 0, assumes Ricardo Formulation should be used
        double output = normDistrib.density(varGetter.getVariable(school)) / normDistrib.density(l50);

        if (output < tiny) {
            output = 0.0;
        }

        return output;

    }
    
    /**
     * Computes the Log-normal selectivity.
     *
     * @param school
     * @return
     */
  public double getLogNormalSelectivity(int index, AbstractSchool school) {
      double l50 = this.l50Array[index];
      double l75 = this.l75Array[index];
      double q75 = 0.674489750196082; // declare constant to save time
      // this is the qnorm(0.75), qnorm has to be used here
      double mean = Math.log(l50);
      double sd   = Math.log(l75/l50) / q75;  
      
      if (this.initSelectivity) {
          // initialisation of the distribution used in selectity calculation
          logNormDistrib = new LogNormalDistribution(mean, sd);
          this.initSelectivity = false;
      }
    
    double output;
    // calculation of selectivity. Normalisation by the maximum value 
    // (i.e. the value computed with x = mode = exp(mean - sd^2).
    double mode = Math.exp(mean - Math.pow(sd, 2));
    output = logNormDistrib.density(varGetter.getVariable(school)) / logNormDistrib.density(mode);
    
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

        double l50 = this.l50Array[index];
        double l75 = this.l75Array[index];

        double s1 = (l50 * Math.log(3)) / (l75 - l50);
        double s2 = s1 / l50;

        double output;

        output = 1 / (1 + Math.exp(s1 - (s2 * varGetter.getVariable(school))));

        if (output < tiny) {
            output = 0.0;
        }

        return output;

    }

    /**
     * Init an array either from file (by dt) or shifts.
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
