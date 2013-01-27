/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class YieldIndicator extends AbstractIndicator {

    public double[] yield, yieldN;

    @Override
    public void reset() {
        int nSpec = getSimulation().getNumberSpecies();
        yield = new double[getNSpecies()];
        yieldN = new double[getNSpecies()];

    }

    @Override
    public void update(School school) {
        int i = school.getSpeciesIndex();
        yield[i] += school.adb2biom(school.nDeadFishing);
        yieldN[i] += school.nDeadFishing;
    }

    @Override
    public boolean isEnabled() {
        return !getOsmose().isCalibrationOutput();
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        String description;

        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_yield_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "cumulative catch (tons per time step of saving). ex: if time step of saving is the year, then annual catches are saved";
        Indicators.writeVariable(time, yield, filename.toString(), description);

        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_yieldN_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "cumulative catch (number of fish caught per time step of saving). ex: if time step of saving is the year, then annual catches in fish numbers are saved";
        Indicators.writeVariable(time, yieldN, filename.toString(), description);
    }
}
