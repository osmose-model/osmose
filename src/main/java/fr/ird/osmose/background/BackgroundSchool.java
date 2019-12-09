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
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getAlphaBioen() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSpeciesName() {
        return this.bkgSpecies.getName();
    }

}
