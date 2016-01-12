/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.SimulationLinker;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author pverley
 */
public class AbstractLTLFastForcing extends SimulationLinker implements LTLForcing {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * The LTL biomass [TIME][PLANKTON][NY][NX]
     */
    private double[][][][] biomass;
    /**
     * The name of the Java class 
     */
    private final String ltlClassName;

/////////////////////////////////////
// Definition of the abstract methods
/////////////////////////////////////
//////////////
// Constructor
//////////////
    /**
     * Creates a new LTLForcing associated to a specified simulation.
     *
     * @param rank, the rank of the simulation
     */
    AbstractLTLFastForcing(int rank, String ltlClassName) {
        super(rank);
        this.ltlClassName = ltlClassName;
    }

    @Override
    public void init() {

        // Read number of LTL steps
        int nLTLStep = getConfiguration().getInt("ltl.nstep");
        int nPlk = getConfiguration().getNPlankton();

        // Initialises biomass variable
        biomass = new double[nLTLStep][nPlk][getGrid().get_ny()][getGrid().get_nx()];

        // Set biomass variable
        info("Loading LTL data...");
        loadBiomass();

        // Uniform biomass. Check AbstractLTLForcing Javadoc for details.
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {
            if (!getConfiguration().isNull("plankton.biomass.total.plk" + iPlk)) {
                double uBiomass = getConfiguration().getDouble("plankton.biomass.total.plk" + iPlk) / getGrid().getNOceanCell();
                for (int iTime = 0; iTime < nLTLStep; iTime++) {
                    for (Cell cell : getGrid().getCells()) {
                        if (!cell.isLand()) {
                            biomass[iTime][iPlk][cell.get_jgrid()][cell.get_igrid()] = uBiomass;
                        }
                    }
                }
            }
        }

        // Biomass multiplier. Check Javadoc of AbstractLTLForcing for details.
        for (int iPlk = 0; iPlk < nPlk; iPlk++) {
            if (!getConfiguration().isNull("plankton.multiplier.plk" + iPlk)) {
                double multiplier = getConfiguration().getFloat("plankton.multiplier.plk" + iPlk);
                if (multiplier != 1.) {
                    for (int iTime = 0; iTime < nLTLStep; iTime++) {
                        for (Cell cell : getGrid().getCells()) {
                            if (!cell.isLand()) {
                                biomass[iTime][iPlk][cell.get_jgrid()][cell.get_igrid()] *= multiplier;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Set the LTL biomass for every LTL group in the {@code biomass} variable.
     */
    private void loadBiomass() {

        // Initialises parent LTLForcing
        AbstractLTLForcing forcing = null;
        try {
            debug("LTLForcing class " + ltlClassName);
            forcing = (AbstractLTLForcing) Class.forName(ltlClassName).getConstructor(Integer.TYPE).newInstance(getRank());
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            error("Failed to create new LTLForcing instance", ex);
        }
        forcing.init();

        // Set biomass variable
        for (int iStep = 0; iStep < biomass.length; iStep++) {
            forcing.update(iStep);
            for (int iLTL = 0; iLTL < getConfiguration().getNPlankton(); iLTL++) {
                for (Cell cell : getGrid().getCells()) {
                    if (!cell.isLand()) {
                        int i = cell.get_igrid();
                        int j = cell.get_jgrid();
                        biomass[iStep][iLTL][j][i] = forcing.getBiomass(iLTL, cell);
                    }
                }
            }
        }
    }

    @Override
    public double getBiomass(int iLTL, Cell cell) {
        int iLTLStep = getSimulation().getIndexTimeSimu() % biomass.length;
        return biomass[iLTLStep][iLTL][cell.get_jgrid()][cell.get_igrid()];
    }

    @Override
    public void update(int iStepSimu) {
        // nothing to do, all the LTL data is loaded at once at the beginning of the simulation
    }
}
