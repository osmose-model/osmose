/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.background;

import fr.ird.osmose.AbstractSchool;
import java.util.HashMap;

/**
 *
 * @author nbarrier
 */
public class BackgroundSchool extends AbstractSchool {

    /**
     * Backgroud species associated with the background School.
     */
    final private BackgroundSpecies bkgSpecies;

    /**
     * Class index associated with the background School.
     */
    private final int iClass;

    /**
     * Time step index.
     */
    private int iStep;

    /**
     * Public constructor. Initialisation from background species, class index
     * and time step.
     *
     * @param species
     * @param iClass
     */
    public BackgroundSchool(BackgroundSpecies species, int iClass) {
        this.bkgSpecies = species;
        this.iClass = iClass;
        abundanceHasChanged = false;
        preys = new HashMap();
    }

    public void setStep(int step) {
        this.iStep = step;
    }

    /**
     * Initialisation of background species school by the values (ts and maps)
     * provided in file.
     */
    public void init() {
        this.abundance = this.getAbundance();
        this.biomass = this.getBiomass();
        this.instantaneousAbundance = abundance;
        this.instantaneousBiomass = biomass;
        this.reset(nDead);
        // Reset diet variables
        preys.clear();
        preyedBiomass = 0.d;
        predSuccessRate = 0.f;
    }

    /**
     * Gets the biomass of school at the beginning of the time-step. It is the
     * value which is defined from time-series and maps.
     *
     * @return
     */
    @Override
    public double getBiomass() {
        return this.bkgSpecies.getBiomass(iStep, this.getLength(), Math.round(this.getX()), Math.round(this.getY()));
    }

    /**
     * Gets the abundance of school at the beginning of the time-step. It is the
     * value which is defined from time-series and maps.
     *
     * @return
     */
    @Override
    public double getAbundance() {
        return this.biom2abd(this.getBiomass());
    }

    @Override
    public void updateBiomAndAbd() {
        this.instantaneousAbundance = this.abundance - sum(nDead);
        if (instantaneousAbundance < 1.d) {
            instantaneousAbundance = 0.d;
        }
        this.instantaneousBiomass = this.abd2biom(instantaneousAbundance);
        abundanceHasChanged = false;
    }


    @Override
    public double biom2abd(double biomass) {
        return biomass / this.getWeight();
    }
    
    @Override
    public double abd2biom(double abund) {
        return abund * this.getWeight();
    }

    @Override
    public int getSpeciesIndex() {
        return this.bkgSpecies.getFinalIndex();
    }

    @Override
    public float getAge() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getLength() {
        return (float) this.bkgSpecies.getTimeSeries().getClass(iClass);
    }

    @Override
    public float getTrophicLevel() {
        return this.bkgSpecies.getTrophicLevel(iClass);
    }

    @Override
    public float getWeight() {
        return this.bkgSpecies.computeWeight(this.getLength());
    }   
    
    /** Returns age as a number of time steps. 
     * Hard coded to 1 (to insure > 0 for predation), 
     * but to see what should be done about it.
     * @return 
     */
    @Override
    public int getAgeDt() {
        // Note: here, age is virtually set as equal to 1
        // in order to be sure that the statement for predation
        // (computePredation in PredationMortality) is always true.
        // if (predator.getAgeDt() > 0) {
        // i.e. bkg species will always feed.
        return 1;
       
    }

    @Override
    public void incrementIngestion(double cumPreyUpon) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
