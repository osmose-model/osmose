/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.fishing;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class AnnualFScenario extends AbstractFishingScenario {

    private float instantaneousF;
    private int recruitmentAge;
    private float recruitmentSize;

    public AnnualFScenario(int iSimulation, Species species) {
        super(iSimulation, species);
    }

    @Override
    public void init() {
        int nStepYear = getConfiguration().getNStepYear();
        int iSpec = getIndexSpecies();
        instantaneousF = getConfiguration().getFloat("mortality.fishing.rate.sp" + iSpec) / nStepYear;
        if (!getConfiguration().isNull("mortality.fishing.recruitment.age.sp" + iSpec)) {
            float age = getConfiguration().getFloat("mortality.fishing.recruitment.age.sp" + iSpec);
            recruitmentAge = Math.round(age * nStepYear);
            recruitmentSize = 0.f;
        } else if (!getConfiguration().isNull("mortality.fishing.recruitment.size.sp" + iSpec)) {
            recruitmentSize = getConfiguration().getFloat("mortality.fishing.recruitment.size.sp" + iSpec);
            recruitmentAge = 0;
        } else {
            recruitmentAge = 0;
            recruitmentSize = 0.f;
            getLogger().log(Level.WARNING, "Could not find any fishing recruitment threshold (neither age nor size) for species {0}. Osmose assumes every school can be catched.", getSpecies().getName());
        }
    }

    @Override
    public float getInstantaneousRate(School school) {
        return (school.getAgeDt() >= recruitmentAge) && (school.getLength() >= recruitmentSize)
                ? instantaneousF
                : 0.f;
    }

    @Override
    public float getAnnualRate() {
        return instantaneousF * getConfiguration().getNStepYear();
    }
}
