/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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

import fr.ird.osmose.process.genet.Genotype;
import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.util.GridPoint;
import java.util.Collection;
import java.util.HashMap;
import ucar.ma2.ArrayFloat;

/**
 * This class represents a school of fish, it is the individual of the
 * Individual Based Model. A school is constituted by a pool of identical fish.
 * This feature allows Osmose to attribute to the school a set of state
 * variables characterising the typical fish of the school:
 * <ul>
 * <li>species<li>
 * <li>age</li>
 * <li>length</li>
 * <li>weight</li>
 * <li>trophic level</li>
 * </ul>
 * The school also has proper state variables:
 * <ul>
 * <li>abundance</li>
 * <li>biomass</li>
 * </ul>
 * Stricto sensus, the fish should be the individual of the IBM but it is
 * unrealistic in terms of computational power to manage such large number of
 * individuals. The concept of school made of fish with identical
 * characteristics offers a good comprise between computational cost and
 * biological relevancy. Later on, when the documentation refers to "fish" it
 * must be understood as the typical fish constituting the school.<br>
 * The {@code School} extends a {@code GridPoint}. It means the school is
 * located with both grid coordinates and geographical coordinates. The way
 * Osmose presently handles spatial movements, it is unnecessarily "fancy" for a
 * school to extends a GridPoint. Indeed Osmose locates the schools in grid
 * cells and does not need more precise coordinates. A mere Cell attribute in
 * the School object would be enough at the moment. The GridPoint opens avenue
 * for future improvement or refinement in the spatial movements of the schools.
 *
 * @see GridPoint
 * @author P.Verley (philippe.verley@ird.fr)
 */
public class School extends AbstractSchool {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * {@link Species} of the fish.
     */
    final private Species species;
    
    /** Genotype variable. */
    private Genotype genotype;
    
    /**
     * The index of the species. From [0; nSpec-1] for fish and from [nSpec;
     * nSpec+nLTL-1] for resource.
     */
    private final int index;
    /**
     * Weight of the fish in tonne. The unit has been set to tonne just because
     * it saves computation time for converting the biomass from gramme to tonne
     */
    private float weight;

    /**
     * Weight of gonads of the fish in tonne.
     */
    private float gonadWeight;
    /**
     * Trophic level of the fish.
     */
    private float trophicLevel;

    /**
     * A buffer variable that will temporarily retain some eggs inside a time
     * step. The reproduction process makes all the eggs available at the
     * beginning of the time step. The stochastic mortality algorithm uses a sub
     * time step and we want the eggs to be made available progressively
     * throughout the sub time step (instead of having them all accessible at
     * the 1st sub time step). This variable represents the amount of eggs that
     * must be subtracted to the abundance at the beginning of the time step for
     * getting the instantaneous accessible abundance of eggs.
     */
    private double eggRetained;
    /**
     * Age of the fish expressed in number of time step.
     */
    private int ageDt;
    /**
     * Age of the fish in year.
     */
    private float age;
    /**
     * Length of the fish in centimetre.
     */
    private float length;
    /**
     * Length of the fish in centimetre at the beginning of the time step.
     */
    private float lengthi;

    /**
     * Starvation mortality rate.
     */
    private double starvationRate;
    /**
     * Whether the school is out of the simulated domain at current time step.
     */
    private boolean out;

    /**
     * Variables that are used in the bioenergetic version of the code.
     */
    private double e_gross;    // gross energy (assimilated)
    private double e_maint;    // energy used for maintenance.
    private double e_net;      // net energy (gross - maintenance)
    private double ingestion;   // total ingestion
    private double kappa;    // kappa value
    double ingestionTot = 0; // sum of all the food ingested during life of the 
                             // school
    
    /** Mortality rates associated with the bioen module. */
    private double mort_oxy_rate = 0;
    private double mort_starv_rate = 0;

    // Initialisation of maturity variables.
    // by default the school is imature.
    private double ageMature = 0;
    private double sizeMature = 0;
    private boolean isMature = false;
    
    
///////////////
// Constructors
///////////////
    /**
     * Create a new school at egg stage with a given number of eggs. State
     * variables are preset:
     * <ul>
     * <li>age set to zero</li>
     * <li>length set from parameter <i>species.egg.size.sp#</i></li>
     * <li>weight set from parameter <i>species.egg.weight.sp#</i></li>
     * <li>trophic level set from constant {@link Species#TL_EGG}</li>
     * <li>the school is not yet located on the grid</li>
     * </ul>
     *
     * @param species, the {@link Species} of the fish
     * @param abundance, the number of eggs in the school
     */
    public School(Species species, double abundance) {
        // constructor with length=eggSize, weight=EggWeight, age=0
        this(species, abundance, species.getEggSize(), species.getEggWeight(), 0);
    }

    /**
     * Create a new school, with given species, abundance, length, weight,
     * gonadWeight and age. Trophic level is preset to {@link Species#TL_EGG}
     * and the school is not located on the grid.
     *
     * @param species, the {@link Species} of the fish
     * @param abundance, the number of fish in the school
     * @param length, the length of the fish in centimeter
     * @param weight, the weight of the fish in gram
     * @param age, the age of the fish in number of time step
     */
    public School(Species species, double abundance, float length, float weight, int age) {
        // call the complete cons. with x=y=-1, TL = Species.TL_EGG
        this(species, -1, -1, abundance, length, weight, age, Species.TL_EGG);
    }

    public School(Species species, float x, float y, double abundance, float length, float weight, int ageDt, float trophicLevel) {
        // call the complete constructor with gonadWeight = 0.f
        this(species, x, y, abundance, length, weight, ageDt, trophicLevel, 0.f);
    }

    /**
     * Create a new school, with given species, grid coordinates, abundance,
     * length weight, gonad weight, age and trophic level.
     *
     * @param species, the {@link Species} of the fish
     * @param x, x coordinate of the school on the grid
     * @param y, y coordinate of the school on the grid
     * @param abundance, the number of fish in the school
     * @param length, the length of the fish in centimetre
     * @param weight, the weight of the fish in gramme
     * @param gonadWeight, the weight of the fish in gramme
     * @param ageDt, the age of the fish in number of time step
     * @param trophicLevel, the trophic level of the fish
     */
    public School(Species species, float x, float y, double abundance, float length, float weight, int ageDt, float trophicLevel, float gonadWeight) {
        this.index = species.getIndex();
        this.abundance = abundance;
        instantaneousAbundance = abundance;
        this.weight = weight * 1.e-6f;
        this.gonadWeight = gonadWeight * 1.e-6f;
        biomass = instantaneousBiomass = abundance * (this.weight + this.gonadWeight);
        abundanceHasChanged = false;
        this.trophicLevel = trophicLevel;
        if (x >= 0 && x < getGrid().get_nx() && y >= 0 && y < getGrid().get_ny()) {
            moveToCell(getGrid().getCell(Math.round(x), Math.round(y)));
        } else {
            setOffGrid();
        }
        eggRetained = 0.d;
        this.species = species;
        this.length = length;
        this.ageDt = ageDt;
        this.age = ageDt / (float) getConfiguration().getNStepYear();
        out = false;
        preys = new HashMap();
        starvationRate = 0.d;
        fishedBiomass = new double[getConfiguration().getNFishery()];
        discardedBiomass = new double[getConfiguration().getNFishery()];
        
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Initialises and reset some state variables
     */
    public void init() {

        // Update abundance
        abundance = getInstantaneousAbundance();
        // Update biomass
        biomass = abundance * (weight);
        // Rest number of dead fish
        reset(nDead);
        // Reset diet variables
        preys.clear();
        preyedBiomass = 0.d;
        predSuccessRate = 0.f;
        // by default the school is in the simulated area, and migration might
        // change this state.
        out = false;
        // Set length at the beginning of the time step
        lengthi = length;
        // reset ingestion at beginning of time step;
        ingestion = 0.d;
        // reset fished biomass
        reset(fishedBiomass);
        reset(this.discardedBiomass);
    }
    
    /**
     * Make some more eggs accessible. This function assumes that the initial
     * abundance of egg at the beginning of the predation process (abundance -
     * eggLoss) is homogeneously released throughout every sub time step. Every
     * sub time step, the amount of egg to be released is equal to
     * (abundance-eggLoss) / subdt.
     *
     * @param subdt, the sub time step of the mortality algorithm
     */
    public void releaseEgg(int subdt) {
        eggRetained = Math.max(0.d, eggRetained - (abundance - nDead[MortalityCause.ADDITIONAL.index]) / (double) subdt);
        abundanceHasChanged = true;
    }

    /**
     * Retain all the eggs left available for predation process. The eggs
     * available for predation means the initial abundance minus the egg loss.
     * After calling this function the instantaneous abundance is zero. One must
     * call the {@link #releaseEgg} function to release some eggs.
     */
    public void retainEgg() {
        eggRetained = abundance - nDead[MortalityCause.ADDITIONAL.index];
        abundanceHasChanged = true;
    }

    /**
     * Converts the specified biomass [tonne] into abundance [number of fish]
     *
     * @param biomass, the biomass of the school, in tonne
     * @return the number of fish weighting {@code weight} corresponding to this
     * level of biomass. {@code abundance = biomass / (gonadWeight+weight)}
     */
    @Override
    public double biom2abd(double biomass) {
        return biomass / (weight);
    }

    /**
     * Converts the specified abundance [number of fish] into biomass [tonne]
     *
     * @param abundance, a number of fish
     * @return the corresponding biomass of this number of fish weighting
     * {@code weight}. {@code biomass = abundance * (weight+gonadWeight)}
     */
    @Override
    public double abd2biom(double abundance) {
        return abundance * (weight);
    }

    /**
     * Returns the trophic level of the fish.
     *
     * @return the trophic level of fish
     */
    @Override
    public float getTrophicLevel() {
        return trophicLevel;
    }

    /**
     * Sets the trophic level of the fish.
     *
     * @param trophicLevel, the new trophic level of the fish
     */
    public void setTrophicLevel(float trophicLevel) {
        this.trophicLevel = trophicLevel;
    }

    /**
     * Returns the index of the species
     *
     * @return the index of the species
     */
    @Override
    public int getSpeciesIndex() {
        return index;
    }

    /**
     * Sets the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     * @param nDead, the number of dead fish for this mortality cause
     */
    public void setNdead(MortalityCause cause, double nDead) {
        this.nDead[cause.index] = nDead;
                
        double factor = 1;
        if (this.getInstantaneousAbundance() != 0) {
            factor = (this.getInstantaneousAbundance() - nDead) / this.getInstantaneousAbundance();
        }

        this.ingestion *= factor;
        this.e_net *= factor;
        
        abundanceHasChanged = true;
    }

    @Override
    public void incrementNdead(MortalityCause cause, double nDead) {
        
        this.nDead[cause.index] += nDead;
        double factor = 1;
        
        if (this.getInstantaneousAbundance() != 0) {
            factor = (this.getInstantaneousAbundance() - nDead) / this.getInstantaneousAbundance();
        }

        this.ingestion *= factor;
        this.e_net *= factor;

        abundanceHasChanged = true;
    }

    /**
     * Resets the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     */
    public void resetNdead(MortalityCause cause) {
        nDead[cause.index] = 0;
        abundanceHasChanged = true;
    }

    /**
     * The weight of the fish (not the whole school), in tonne.
     *
     * @return the weight on the fish, in tonne.
     */
    @Override
    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * Returns a list of the prey records at current time step.
     *
     * @return a list of the prey records at current time step.
     */
    public Collection<Prey> getPreys() {
        return preys.values();
    }

    /**
     * Sets the school out of the simulated domain.
     */
    public void out() {
        out = true;
        setOffGrid();
    }

    /**
     * Checks whether the school is out of the simulated domain.
     *
     * @return {@code true} if the school is out of the simulated domain.
     */
    public boolean isOut() {
        return out;
    }

    /**
     * Checks whether the school is alive. A school is alive if it fulfills both
     * conditions: {@code instantaneous abundance > 0} and null
     * {@code age <= lifespan - 1}
     *
     * @return whether the school is alive or not
     */
    public boolean isAlive() {
        return (getInstantaneousAbundance() > 0) && (ageDt <= species.getLifespanDt() - 1);
    }

    /**
     * Returns a string representation of the school (species, cohort and
     * location).
     *
     * @return a string representation of the school
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("School");
        str.append("\n  Species: ");
        str.append(getSpecies().getName());
        str.append("\n  Cohort: ");
        str.append(getAge());
        str.append(" [year]");
        str.append("\n  Cell: ");
        str.append(getCell().getIndex());
        return str.toString();
    }

    /**
     * Returns the species of the school.
     *
     * @see Species
     * @return the species of the school
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Returns the age of the fish in number of time step.
     *
     * @return the age of the fish in number of time step
     */
    @Override
    public int getAgeDt() {
        return ageDt;
    }

    @Override
    public float getAge() {
        return age;
    }

    /**
     * Increments the age of the fish of one time step.
     */
    public void incrementAge() {
        ageDt += 1;
        age = ageDt / (float) getConfiguration().getNStepYear();
    }

    /**
     * Returns the length of the fish, in centimetre.
     *
     * @return the length of the fish, in centimetre
     */
    @Override
    public float getLength() {
        return length;
    }

    /**
     * @param length
     */
    public void setLength(float length) {
        this.length = length;
    }

    /**
     * Increments the length of the fish from given number of centimetre.
     *
     * @param dlength, the length increment in centimetre
     */
    public void incrementLength(float dlength) {
        if (dlength != 0.f) {
            length += dlength;
            setWeight(species.computeWeight(length) * 1e-6f);
        }
    }

    /**
     * Get the starvation rate at current time step.
     *
     * @return the starvation rate
     */
    public double getStarvationRate() {
        return starvationRate;
    }

    /**
     * @param starvationRate the starvationRate to set
     */
    public void setStarvationRate(double starvationRate) {
        this.starvationRate = starvationRate;
    }

    /**
     * Length of the fish at the beginning of the time step, in centimetre.
     *
     * @return the length at the beginning of the time step, in centimetre.
     */
    public float getLengthIniStep() {
        return lengthi;
    }

    @Override
    public void updateBiomAndAbd() {
        instantaneousAbundance = (abundance - eggRetained) - sum(nDead);
        if (instantaneousAbundance < 1.d) {
            instantaneousAbundance = 0.d;
        }
        instantaneousBiomass = instantaneousAbundance * weight;
        abundanceHasChanged = false; 
    }

    /**
     * Returns the ingestion (I term, equation 1).
     */
    public double getIngestion() {
        return this.ingestion;
    }
    
    
    // Calculate the total ingestion of food during life of the school 
    public double getIngestionTot(){
        return this.ingestionTot;
    }
    
    public void updateIngestionTot(double ingestion, double abundance) {
        this.ingestionTot += (ingestion/abundance);
    }

    /**
     * The gonadic weight of the fish (not the whole school), in tonne.
     *
     * @return the gonadic weight on the fish, in tonne.
     */
    public float getGonadWeight() {
        return gonadWeight;
    }

    public void setGonadWeight(float gonadWeight) {
        this.gonadWeight = gonadWeight;
    }

    /**
     * Increments the weight of the fish from given number of tons. Length is
     * recomputed thereafter from the new weigth
     *
     * @param dw Weight increment (in ton)
     */
    public void incrementWeight(float dw) {
        if (dw > 0.f) {
            this.weight += dw;
            // Update in the length calculation
            // weight is in ton, should be converted in gram
            this.setLength(species.computeLength(this.weight * 1e6f));
        }

    }

    /**
     * Increments the gonad weight of the fish from given number of tons. In
     * this case, the length of the fish is not updated.
     *
     * @param dg Weight increment (in ton)
     */
    public void incrementGonadWeight(float dg) {
        this.gonadWeight += dg;
        if(this.gonadWeight < 0.d) { 
            warning("Gonad weight is negative");
        }
    }

    /**
     * Returns true if the individual is sexually mature.
     */
    public boolean isMature() {
        return this.isMature;
    }

    /**
     * Set whether the individual is sexually mature or not.
     */
    public void setIsMature(boolean mature) {
        this.isMature = mature;
    }

    /**
     * Returns the age at maturity (only used for outputs).
     */
    public double getAgeMat() {
        return this.ageMature;
    }

    /**
     * Returns the age at maturity (only used for outputs).
     */
    public double getSizeMat() {
        return this.sizeMature;
    }
    
    /**
     * Sets the age at maturity (only used for outputs).
     */
    public void setAgeMat(double agemature) {
        this.ageMature = agemature;
    }
    
    /**
     * Sets the age at maturity (only used for outputs).
     */
    public void setSizeMat(double sizemat) {
        this.sizeMature = sizemat;
    }

    /**
     * Sets the value of assimilated energy (ingestion x phiT, equation 3).
     */
    public void setEGross(double value) {
        this.e_gross = value;
    }

    /**
     * Returns the value of assimilated energy (ingestion x phiT, equation 3).
     */
    public double getEGross() {
        return this.e_gross;
    }

    /**
     * Sets the value of maintenance energy (ingestion x phiT, equation 5).
     */
    public void setEMaint(double value) {
        this.e_maint = value;
    }

    /**
     * Gets the value of maintenance energy (ingestion x phiT, equation 5).
     */
    public double getEMaint() {
        return this.e_maint;
    }

    /**
     * Returns the net energy, which is the difference between gross and
     * maintenance energy.
     *
     * @return
     */
    public double getENet() {
        return this.e_net;
    }

     /**
     * Returns the net energy, which is the difference between gross and
     * maintenance energy.
     *
     * @return
     */
    public void setENet(double enet) {
        this.e_net = enet;
    }
    
    /**
     * Increments the ingested energy.
     */
    @Override
    public void incrementIngestion(double cumPreyUpon) {
        this.ingestion += cumPreyUpon;
    }
    
    /**
     * Gets the value of kappa 
     */
    public double getKappa() {
        return this.kappa;
    }

    /**
     * Returns the net energy, which is the difference between gross and
     * maintenance energy.
     *
     * @return
     */
    public void setKappa(double value) {
        this.kappa = value;
    }
    
    /**
     * Gets the value of maintenance energy (ingestion x phiT, equation 5).
     */
    public double getOxyMort() {
        return this.mort_oxy_rate;
    }

    /**
     * Returns the net energy, which is the difference between gross and
     * maintenance energy.
     *
     * @return
     */
    public void setOxyMort(double value) {
        this.mort_oxy_rate = value;
    }

    /**
     * Gets the value of maintenance energy (ingestion x phiT, equation 5).
     */
    public double getStarvMort() {
        return this.mort_starv_rate;
    }

    /**
     * Returns the net energy, which is the difference between gross and
     * maintenance energy.
     *
     * @param value
     * @return
     */
    public void setStarvMort(double value) {
        this.mort_starv_rate = value;
    }

    public void incrementEnet(double d) {
        this.e_net += d;
    }

    @Override 
    
    public double getBetaBioen() {
        return this.getSpecies().getBetaBioen();
    }
    
    public Genotype getGenotype() {
        return this.genotype;
    }

    public void instance_genotype(int rank) {
        genotype = new Genotype(rank, this.getSpecies());
        genotype.init();
    }
    
    public double getTrait(String key) throws Exception { 
        return this.getGenotype().getTrait(key);
    }
    
    public boolean existsTrait(String key) throws Exception {
        return this.getGenotype().existsTrait(key);
    }

    @Override
    public String getSpeciesName() {
        return this.species.getName();
    }
    
    /** Returns true if the school is considered as a larva. 
     * Based on the comparison of ageDt with threshold age by dt (default 1).
     * @return True if larva, False if adult
     */
    public boolean isLarva() { 
        return (this.getAgeDt() < this.getSpecies().getThresAge());
    }
    
    /** Reads the genotype from restart file. 
     * 
     * @param rank Simulation rank
     * @param index School index
     * @param genArray Netcdf Array (school, itrait, ilocus, 2)
     * @param envNoise
     */
    public void restartGenotype(int rank, int index, ArrayFloat.D4 genArray, ArrayFloat.D2 envNoise) {
        
        // Instanciate (i.e. init arrays) genotype for the current school
        this.instance_genotype(rank);
        
        // Sets the genotype values from NetCDF files
        int ntrait = this.getGenotype().getNEvolvingTraits();
        for(int itrait=0; itrait<ntrait; itrait++) {
            int nlocus = this.getGenotype().getNLocus(itrait);
            for(int iloc=0; iloc<nlocus; iloc++) {
                // Recovers the NetCDF values
                double val0 = genArray.get(index, itrait, iloc, 0);
                double val1 = genArray.get(index, itrait, iloc, 1);
                this.getGenotype().setLocusVal(itrait, iloc, val0, val1);
                
                // Sets the value for the environmental noise
                double envnoise = envNoise.get(index, itrait);
                this.getGenotype().setEnvNoise(itrait, envnoise);
            }
        }   
    }

}
