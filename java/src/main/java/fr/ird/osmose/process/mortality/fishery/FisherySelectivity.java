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
 * Nicolas BARRIER (nicolas.barrier@ird.fr)
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
import fr.ird.osmose.IMarineOrganism;
import fr.ird.osmose.output.distribution.DistributionType;
import fr.ird.osmose.stage.ClassGetter;
import fr.ird.osmose.stage.SchoolStage;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.ByRegimeTimeSeries;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

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
     * Array of selectivity parameters. One value per time step.
     */
    private double[] l50Array;
    private double[] l75Array;
    private double[] l25Array;
    private double[] plateauArray;
    private double[] l0Array;
    private double[] l1Array;

    /**
     * Value below which selectivity is forced to 0.
     */
    private double tiny = 1E-6; // default consistent with R processing

    /**
     * Array of seelectivity types. One value per time step.
     */
    private double[] selectTypeArray;

    /**
     * Array of selectivity methods. 0:=knife edge. 1:= Sigmoid 2:= Gaussian
     * 3:=logNormal, 4=3-par double normal, 5=4-par double normal, 6=5-par double normal,
     * 7=6-par double normal, 8:= splines (not implemented yet), 9:=Non-parametric
     */
    private SizeSelect sizeSelectMethods[];

    /**
     * True if selectivity should be initialized. True only if selectivity type
     * changes.
     */
    private boolean initSelectivity;

    // used only in the case of discrete selectivities
    private float[] breaks;
    private double[] discreteSelectivities;
    private ClassGetter classGetter;

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
     * Interface to allow recovering the size/age of species. Both background and
     * focal.
     */
    private VarGetter varGetter;

    /**
     * Determines which School variable should be used to compute the selectivity.
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
            String msg = String.format("%d: Using %s.tiny.%s%d = %f", fIndex, selPrefix, selSuffix, fIndex, this.tiny);
            info(msg);
        } else {
            this.tiny = tiny;
            String msg = String.format("%d: Using default tiny (%f).", fIndex, this.tiny);
            info(msg);
        }

        key = String.format("%s.a50.%s%d", selPrefix, selSuffix, fIndex);
        if (!cfg.isNull(key)) {
            String msg0 = "Age selectivity is not compatible with multispecies fisheries and will be deprecated soon.";
            String msg1 = String.format("Please update fishing parameters for Fishery %d.", fIndex);
            warning(msg0 + " " + msg1);
            varGetter = (school) -> (school.getAge());
            this.initByAge();
          } else {
            varGetter = (school) -> (school.getLength());
              this.initByLength();
        }
          // Init the size selectivity array
        sizeSelectMethods = new SizeSelect[10];
        sizeSelectMethods[0] = (index, sch) -> this.getKnifeEdgeSelectivity(index, sch); // knife-edge selectivity
        sizeSelectMethods[1] = (index, sch) -> this.getSigmoidSelectivity(index, sch); // Sigmoid selectivity
        sizeSelectMethods[2] = (index, sch) -> this.getGaussianSelectivity(index, sch); // Gaussian selectivity
        sizeSelectMethods[3] = (index, sch) -> this.getLogNormalSelectivity(index, sch); // Log-normal selectivity
        sizeSelectMethods[4] = (index, sch) -> this.getDoubleNormalSelectivity3(index, sch); // 3-par double-normal selectivity
        sizeSelectMethods[5] = (index, sch) -> this.getDoubleNormalSelectivity4(index, sch); // 4-par double-normal selectivity
        sizeSelectMethods[6] = (index, sch) -> this.getDoubleNormalSelectivity5(index, sch); // 5-par double-normal selectivity
        sizeSelectMethods[7] = (index, sch) -> this.getDoubleNormalSelectivity6(index, sch); // 6-par double-normal selectivity
        sizeSelectMethods[8] = (index, sch) -> this.getSplineSelectivity(index, sch); // Spline selectivity
        sizeSelectMethods[9] = (index, sch) -> this.getNonParametricSelectivity(index, sch); // Non-parametric selectivity

    }

    /**
     * Init the variables need to compute age selectivity. Only knife-edge can be
     * used with age.
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

        String prefix, key, msg;

        boolean use_l50;
        boolean use_l75, use_l25, use_plateau, use_l0, use_l1;
        boolean use_nonpar, nonpar_only;

        use_l50 = true;
        use_l75 = use_l25 = use_plateau = use_l0 = use_l1 = false;
        use_nonpar = false;
        nonpar_only = true;

        prefix = selPrefix + ".type";
        this.selectTypeArray = this.initArray(prefix);

        for (double v : this.selectTypeArray) {
          if (v != 9) nonpar_only = false;
          if (v == 1) use_l75 = true;
          if (v == 2) use_l75 = true;
          if (v == 3) use_l75 = true;
          if (v == 4) use_l75 = use_l25 = true;
          if (v == 5) use_l75 = use_l25 = use_plateau = true;
          if (v == 6) use_l75 = use_l25 = use_plateau = use_l1 = true;
          if (v == 7) use_l75 = use_l25 = use_plateau = use_l1 = use_l0 = true;
          if (v == 9) use_nonpar = true;
        }

        if (nonpar_only)  use_l50 = false;

        if (use_l50) {
            prefix = selPrefix + ".l50";
            this.l50Array = this.initArray(prefix);
        }

        if (use_l75) {
            prefix = selPrefix + ".l75";
            this.l75Array = this.initArray(prefix);
        }

        if (use_l25) {
            prefix = selPrefix + ".l25";
            this.l25Array = this.initArray(prefix);
        }

        if (use_plateau) {
            prefix = selPrefix + ".plateau";
            this.plateauArray = this.initArray(prefix);
        }

        if (use_l1) {
            prefix = selPrefix + ".l1";
            this.l1Array = this.initArray(prefix);
        }

        if (use_l0) {
            prefix = selPrefix + ".l0";
            this.l0Array = this.initArray(prefix);
        }

        if (use_nonpar) {

          key = String.format("%s.breaks.%s%d", selPrefix, selSuffix, fIndex);

          if(getConfiguration().isNull(key)) {
            error("Could not find parameter " + key, new NullPointerException("Parameter " + key + " not found "));
          }

          int nStage = getConfiguration().getArrayString(key).length + 1;
          this.breaks = getConfiguration().getArrayFloat(key);

          key = String.format("%s.values.%s%d", selPrefix, selSuffix, fIndex);
          if(getConfiguration().isNull(key)) {
            error("Could not find parameter " + key, new NullPointerException("Parameter " + key + " not found "));
          }

          this.discreteSelectivities = getConfiguration().getArrayDouble(key);
          if(this.discreteSelectivities.length != nStage) {
            error("Wrong number of selectivity values.", null);
          }
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

    /**
     * 0: Computes the knife edge selectivity.
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
     * 1: Computes the sigmoid selectivity.
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

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }

    /**
     * 2: Computes the Gaussian selectivity.
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
        //checkIfUpdatedSel(index);
        //if (this.initSelectivity) {
        //    // initialisation of the distribution used in selectity calculation
        //    normDistrib = new NormalDistribution(l50, sd);
        //    this.initSelectivity = false;
        //}

        // calculation of selectivity. Normalisation by the maximum value
        // (i.e. the value computed with x = mean).
        // If L75 > 0, assumes Ricardo Formulation should be used
        double size = varGetter.getVariable(school);
        //double output = normDistrib.density(size) / normDistrib.density(l50);
        double output = Math.exp(-0.5*Math.pow(size - l50, 2)/Math.pow(sd, 2));

        //if(output >= 0) {
        //  String msg = String.format("Selectivity value =%.2f (%.9f/%.9f) at size %.2f, l50=%.3f (mean=%.3f), step %d.",
        //  output, normDistrib.density(size), normDistrib.density(l50), size, l50, normDistrib.getMean(), index);
        //  info(msg);
        //}

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }

    /**
     * 3: Computes the Log-normal selectivity.
     *
     * @param school
     * @return
     */
    public double getLogNormalSelectivity(int index, AbstractSchool school) {
        double l50 = this.l50Array[index];
        double l75 = this.l75Array[index];
        double q75 = 0.674489750196082; // declare constant to save time
        // this is the qnorm(0.75), qnorm has to be used here
        double mean = Math.log(l50); // l50 is the median
        double sd = Math.log(l75 / l50) / q75;

        //checkIfUpdatedSel(index);
        //if (this.initSelectivity) {
        //    // initialisation of the distribution used in selectity calculation
        //    logNormDistrib = new LogNormalDistribution(mean, sd);
        //    this.initSelectivity = false;
        //}

        double output;
        double size = varGetter.getVariable(school);
        // calculation of selectivity. Normalisation by the maximum value
        // (i.e. the value computed with x = mode = exp(mean - sd^2).
        //double mode = Math.exp(mean - Math.pow(sd, 2));
        //output = logNormDistrib.density(size) / logNormDistrib.density(mode);
        output = Math.exp(-Math.pow(Math.log(size)-mean, 2)/(2*Math.pow(sd,2)))*Math.exp(mean-0.5*Math.pow(sd, 2)) / size;

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }

   /**
     * 4: Computes the 3 parameter double-normal selectivity.
     *
     * @param school
     * @return
     */
    public double getDoubleNormalSelectivity3(int index, AbstractSchool school) {

        double l25 = this.l25Array[index];
        double l50 = this.l50Array[index];
        double l75 = this.l75Array[index];
        double q25 = -0.674489750196082; // declare constant to save time
        double q75 = 0.674489750196082; // declare constant to save time
        // this is the qnorm(0.75), qnorm has to be used here
        double mean1 = l50;
        double mean2 = mean1;
        double sd1 = (l25 - mean1)/q25;
        double sd2 = (l75 - mean2)/q75;

        double output;
        double size = varGetter.getVariable(school);
        if (size < mean1) {
          output = Math.exp(-0.5*Math.pow(size - mean1, 2)/Math.pow(sd1, 2));
        } else {
          output = Math.exp(-0.5*Math.pow(size - mean2, 2)/Math.pow(sd2, 2));
        }

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }

   /**
     * 5: Computes the 4 parameter double-normal selectivity.
     *
     * @param school
     * @return
     */
    public double getDoubleNormalSelectivity4(int index, AbstractSchool school) {

        double l25 = this.l25Array[index];
        double l50 = this.l50Array[index];
        double l75 = this.l75Array[index];
        double plateau = this.plateauArray[index];

        String msg = String.format("Fishery %d: L75 must be greater than L50 + plateau", fIndex);
        if (l75 < l50 + plateau) error(msg, null);

        double q25 = -0.674489750196082; // declare constant to save time
        double q75 = 0.674489750196082; // declare constant to save time
        // this is the qnorm(0.75), qnorm has to be used here
        double mean1 = l50;
        double mean2 = mean1 + plateau;
        double sd1 = (l25 - mean1)/q25;
        double sd2 = (l75 - mean2)/q75;

        double output = 1;
        double size = varGetter.getVariable(school);
        if (size < mean1) {
          output = Math.exp(-0.5*Math.pow(size - mean1, 2)/Math.pow(sd1, 2));
        }
        if (size > mean2) {
          output = Math.exp(-0.5*Math.pow(size - mean2, 2)/Math.pow(sd2, 2));
        }

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }

   /**
     * 6: Computes the 5 parameter double-normal selectivity.
     *
     * @param school
     * @return
     */
    public double getDoubleNormalSelectivity5(int index, AbstractSchool school) {

        double l25 = this.l25Array[index];
        double l50 = this.l50Array[index];
        double l75 = this.l75Array[index];
        double plateau = this.plateauArray[index];
        double l1 = this.l1Array[index];
        String msg;

        msg = String.format("Fishery %d: L75 must be greater than L50 + plateau", fIndex);
        if (l75 < l50 + plateau) error(msg, null);

        double q25 = -0.674489750196082; // declare constant to save time
        double q75 = 0.674489750196082; // declare constant to save time
        // this is the qnorm(0.75), qnorm has to be used here
        double mean1 = l50;
        double mean2 = mean1 + plateau;
        double sd1 = (l25 - mean1)/q25;
        double sd2 = (l75 - mean2)/q75;

        msg = String.format("Fishery %d: L1 must be greater than L50 + plateau", fIndex);
        if (l1 < mean2) error(msg, null);

        double output = 1;
        double size = varGetter.getVariable(school);
        if (size < mean1) {
          output = Math.exp(-0.5*Math.pow(size - mean1, 2)/Math.pow(sd1, 2));
        }
        if (size > mean2 && size <= l1) {
          output = Math.exp(-0.5*Math.pow(size - mean2, 2)/Math.pow(sd2, 2));
        }
        if (size > l1) {
          output = Math.exp(-0.5*Math.pow(l1 - mean2, 2)/Math.pow(sd2, 2));
          return output;
        }

        if (output < this.tiny) {
            output = 0.0;
        }

        return output;

    }

   /**
     * 7: Computes the 6 parameter double-normal selectivity.
     *
     * @param school
     * @return
     */
    public double getDoubleNormalSelectivity6(int index, AbstractSchool school) {

        double l25 = this.l25Array[index];
        double l50 = this.l50Array[index];
        double l75 = this.l75Array[index];
        double plateau = this.plateauArray[index];
        double l1 = this.l1Array[index];
        double l0 = this.l0Array[index];
        String msg;

        msg = String.format("Fishery %d: L75 must be greater than L50 + plateau", fIndex);
        if (l75 < l50 + plateau) error(msg, null);

        double q25 = -0.674489750196082; // declare constant to save time
        double q75 = 0.674489750196082; // declare constant to save time
        // this is the qnorm(0.75), qnorm has to be used here
        double mean1 = l50;
        double mean2 = mean1 + plateau;
        double sd1 = (l25 - mean1)/q25;
        double sd2 = (l75 - mean2)/q75;

        msg = String.format("Fishery %d: L1 must be greater than L50 + plateau", fIndex);
        if (l1 < mean2) error(msg, null);
        msg = String.format("Fishery %d: L0 must be lower than L50", fIndex);
        if (l0 > mean1) error(msg, null);

        double output = 1;
        double size = varGetter.getVariable(school);

        if (size < l0) {
          output = 0.0;
          return output;
        }
        if (size < mean1) {
          output = Math.exp(-0.5*Math.pow(size - mean1, 2)/Math.pow(sd1, 2));
        }
        if (size > mean2 && size <= l1) {
          output = Math.exp(-0.5*Math.pow(size - mean2, 2)/Math.pow(sd2, 2));
        }
        if (size > l1) {
          output = Math.exp(-0.5*Math.pow(l1 - mean2, 2)/Math.pow(sd2, 2));
        }

        if (output < this.tiny) {
            output = 0.0;
        }


        return output;

    }

    /**
     * 8: Computes the splines selectivity
     *
     * @param school
     * @return
     */
    public double getSplineSelectivity(int index, AbstractSchool school) {
        error("Spline selectivity not implemented yet.", null);
        double l50 = this.l50Array[index];
        double output = (varGetter.getVariable(school) < l50) ? 0 : 1;
        return output;
    }

    /**
     * 9: Reads non-parametric selectivity.
     *
     * @param school
     * @return
     */
    public double getNonParametricSelectivity(int index, AbstractSchool school) {

        int stage = 0;
        for (float ibreak : this.breaks) {
            if (varGetter.getVariable(school) < ibreak) {
                break;
            }
            stage++;
        }

        return discreteSelectivities[stage];
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
