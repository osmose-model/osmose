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

package fr.ird.osmose;

/**
 * This class represents a species. It is characterised by the following
 * variables:
 * <ul>
 * <li>name</li>
 * <li>lifespan</li>
 * <li>Allometric parameters</li>
 * <li>Egg weight and size</li>
 * <li>Size at maturity</li>
 * </ul>
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Species implements ISpecies {

    ///////////////////////////////
    // Declaration of the variables
    ///////////////////////////////

    /**
     * Index of the species. From 0 to Nspecies - 1 for focal species
     */
    private final int index;

    /**
     * Trophic level of an egg.
     */
    public static final float TL_EGG = 3f;
    /**
     * Index of the species in the configuration file.
     */
    private final int fileIndex;
    /**
     * Name of the species. Parameter <i>species.name.sp#</i>
     */
    private final String name;
    /**
     * Lifespan expressed in number of time step. A lifespan of 5 years means that a
     * fish will die as soon as it turns 5 years old. Parameter
     * <i>species.lifespan.sp#</i>
     */
    private final int lifespan;
    /**
     * Allometric parameters. Parameters
     * <i>species.length2weight.condition.factor.sp#</i> and
     * <i>species.length2weight.allometric.power.sp#</i>
     */
    private final float c, bPower;
    /**
     * Size (cm) at maturity. Parameter <i>species.maturity.size.sp#</i>
     */
    private final float sizeMaturity;
    /**
     * Age (year) at maturity. Parameter <i>species.maturity.age.sp#</i>
     */
    private final float ageMaturity;
    /**
     * Size (cm) of eggs. Parameter <i>species.egg.size.sp#</i>
     */
    private final float eggSize;
    /**
     * Weight (gram) of eggs. Parameter <i>species.egg.weight.sp#</i>
     */
    private final float eggWeight;

    private int zlayer = 0;

    /**
     * Threshold (number of time-steps) at which a species move from larva to
     * adults. Expressed in time steps.
     */
    private final int firstFeedingAgeDt;

    /** Threshold for moving from larvaeToAdults. */
    private final int larvaeToAdultsAgeDt;

    private double beta_bioen;

    private interface StarvationInterface  {
        public boolean isStarvationEnabled(School school);
    }

    private StarvationInterface starvationInterface;

    // private ByClassTimeSeries prices;

    //////////////
    // Constructor
    //////////////
    /**
     * Create a new species
     *
     * @param fileIndex, an integer, the index of the species
     *                   {@code [0, nbTotSpecies - 1]}
     * @param index
     */
    public Species(int fileIndex, int index) {

        this.index = index;

        Configuration cfg = Osmose.getInstance().getConfiguration();
        this.fileIndex = fileIndex;

        // Initialization of parameters
        name = cfg.getString("species.name.sp" + fileIndex).replaceAll("_", "").replaceAll("-", "");

        c = cfg.getFloat("species.length2weight.condition.factor.sp" + fileIndex);
        bPower = cfg.getFloat("species.length2weight.allometric.power.sp" + fileIndex);

        // If the economic module is on, then we read the file containing the prices for different
        // size classes.
        // if(cfg.isEconomyEnabled()) {
        //     prices = new ByClassTimeSeries();
        //     prices.read(cfg.getFile("species.prices.file.sp" + fileIndex));
        // }

        if (!cfg.isBioenEnabled()) {

            // If not bioen, initialize age at maturity
            // used for reproduction process and egg size
            // used for growth
            if (!cfg.isNull("species.maturity.size.sp" + fileIndex)) {
                sizeMaturity = cfg.getFloat("species.maturity.size.sp" + fileIndex);
                ageMaturity = Float.MAX_VALUE;
            } else {
                ageMaturity = cfg.getFloat("species.maturity.age.sp" + fileIndex);
                sizeMaturity = Float.MAX_VALUE;
            }

            starvationInterface = (School sch) -> this.isStarvationEnabledNoBioen(sch);
            eggSize = cfg.getFloat("species.egg.size.sp" + fileIndex);

        } else {
            // if Bioen module enabled, use maximum values
            sizeMaturity = Float.MAX_VALUE;
            ageMaturity = Float.MAX_VALUE;
            eggSize = Float.MAX_VALUE;
            starvationInterface = (School sch) -> this.isStarvationEnabledBioen(sch);
        }

        eggWeight = cfg.getFloat("species.egg.weight.sp" + fileIndex);
        float agemax = cfg.getFloat("species.lifespan.sp" + fileIndex);
        lifespan = (int) Math.round(agemax * cfg.getNStepYear());

        // barrier.n: added for bioenergetic purposes.
        if (cfg.isBioenEnabled()) {
            zlayer = cfg.getInt("species.zlayer.sp" + fileIndex);
            String key = String.format("species.beta.sp%d", fileIndex);
            beta_bioen = cfg.getDouble(key);
        }

        // If the key is found, then the age switch in years is converted into
        // time-step.
        String key = "species.first.feeding.age.sp" + fileIndex;
        if (cfg.canFind(key)) {
            float age_adult = cfg.getFloat(key);
            this.firstFeedingAgeDt = (int) Math.round(age_adult * cfg.getNStepYear());
        } else {
            // if no parameter exists, species become larva when ageDt = 1
            this.firstFeedingAgeDt = 1;
        }

        // If bioen is on, use the parameter to get the age at which
        // a species moves from larvae to adult.
        if (cfg.isBioenEnabled()) {
            key = "species.larvae.growth.threshold.age.sp" + fileIndex;
            if (cfg.canFind(key)) {
                float age_adult = cfg.getFloat(key);
                this.larvaeToAdultsAgeDt = (int) Math.round(age_adult * cfg.getNStepYear());
            } else {
                // if no parameter exists, species become larva when ageDt = 1
                this.larvaeToAdultsAgeDt = 1;
            }
        } else {
            this.larvaeToAdultsAgeDt = 0;
        } // end of test on bioen.
    }

    /** Starvation method for bioenergetics. */
    private boolean isStarvationEnabledBioen(School school) {
        return (school.getAgeDt() > this.getFirstFeedingAgeDt());
    }

    /** Starvation method for non-bioenergetics. */
    private boolean isStarvationEnabledNoBioen(School school) {
        return (school.getAgeDt() >= this.getFirstFeedingAgeDt());
    }

    public boolean isStarvationEnabled(School school) {
        return starvationInterface.isStarvationEnabled(school);
    }

    public int getFirstFeedingAgeDt() {
        return this.firstFeedingAgeDt;
    }

    public double getBetaBioen() {
        return this.beta_bioen;
    }

    public int getDepthLayer() {
        return this.zlayer;
    }

    public int getLarvaeThresDt() {
        return this.larvaeToAdultsAgeDt;
    }

    //////////////////////////////
    // Definition of the functions
    //////////////////////////////
    /**
     * Computes the weight, in gram, corresponding to the given length, in
     * centimetre.
     *
     * @param length, the length in centimetre
     * @return the weight in gram for this {@code length}
     */
    public float computeWeight(float length) {
        return (float) (c * (Math.pow(length, bPower)));
    }

    /**
     * Computes the length, in centimeter, corresponding to the given weight, in
     * gram.
     *
     * @param weight, the weight in gram
     * @return the length in centimetre for this {@code weight}
     */
    public float computeLength(float weight) {
        return (float) (Math.pow(weight / c, (1 / bPower)));
    }

    /**
     * Returns the lifespan of the species. Parameter <i>species.lifespan.sp#</i>
     *
     * @return the lifespan, in number of time step
     */
    public int getLifespanDt() {
        return lifespan;
    }

    /**
     * Returns the index of the species.
     *
     * @return the index of the species
     */
    @Override
    public int getFileSpeciesIndex() {
        return fileIndex;
    }

    /**
     * Return the global index of the species.
     *
     * Index between [0, Nspec - 1].
     *
     *
     * @return
     */
    @Override
    public int getSpeciesIndex() {
        return index;
    }

    /**
     * Returns the name of the species. Parameter <i>species.name.sp#</i>
     *
     * @return the name of the species
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the size of an egg. Parameter <i>species.egg.size.sp#</i>
     *
     * @return the size of an egg in centimeter
     */
    public float getEggSize() {
        Configuration cfg = Osmose.getInstance().getConfiguration();
        float output = cfg.isBioenEnabled() ? this.computeLength(eggWeight) : this.eggSize;
        return output;
    }

    /**
     * Returns the weight of an egg in gram. Parameter <i>species.egg.weight.sp#</i>
     *
     * @return the weight of an egg in gram
     */
    public float getEggWeight() {
        return eggWeight;
    }

    public boolean isSexuallyMature(School school) {
        if (Osmose.getInstance().getConfiguration().isBioenEnabled()) {
            throw new UnsupportedOperationException("isSexualluMature not supported in Osmose-PHYSIO");
        } else {
            return (school.getLength() >= sizeMaturity) || (school.getAge() >= ageMaturity);
        }
    }

    /**
     * Just keep it as a reminder for a future vulnerability function
     *
     * @param biomass
     * @return
     */

    /*
     * private boolean isVulnerable(double biomass) { double Bv = 0.d; double Sv =
     * 1.d; return (Math.random() > (1.d / (1.d + Math.exp(Sv * (Bv - biomass)))));
     * }
     */

    // /* Returns the prices for the given species. */
    // public ByClassTimeSeries getPrices() {
    //     return prices;
    // }

}
