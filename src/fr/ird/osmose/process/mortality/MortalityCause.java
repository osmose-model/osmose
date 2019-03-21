/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.mortality;

/**
 * A list of mortality causes.
 */
public enum MortalityCause {

    /**
     * Predation mortality
     *
     * @see fr.ird.osmose.process.PredationProcess
     *//**
     * Predation mortality
     *
     * @see fr.ird.osmose.process.PredationProcess
     */
    PREDATION(0),
    /**
     * Starvation mortality
     *
     * @see fr.ird.osmose.process.StarvationProcess
     */
    STARVATION(1),
    /**
     * Additional mortality
     *
     * @see fr.ird.osmose.process.AdditionalMortality
     */
    ADDITIONAL(2),
    /**
     * Fishing mortality
     *
     * @see fr.ird.osmose.process.FishingProcess
     */
    FISHING(3),
    /**
     * Out of domain mortality
     *
     * @see fr.ird.osmose.process.OutMortalityProcess
     */
    OUT(4),
    
    /** Oxydation mortality (bioenergetic module) */
    OXY(5);
    
    /**
     * Index of the mortality cause
     */
    public final int index;

    /**
     * Initialises a mortality cause with a given index.
     *
     * @param index, the index of the mortality cause
     */
    private MortalityCause(int index) {
        this.index = index;
    }
}
