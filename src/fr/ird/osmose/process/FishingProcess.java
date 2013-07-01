package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.fishing.AbstractFishingScenario;
import fr.ird.osmose.process.fishing.AnnualFScenario;
import fr.ird.osmose.process.fishing.AnnualFSeasonScenario;
import fr.ird.osmose.process.fishing.ByDtByAgeSizeScenario;

/**
 *
 * @author pverley
 */
public class FishingProcess extends AbstractProcess {

    private AbstractFishingScenario[] fishingScenario;

    public FishingProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {
        fishingScenario = new AbstractFishingScenario[getNSpecies()];
        // Find fishing scenario
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            if (!getConfiguration().isNull("mortality.fishing.rate.byDt.byAge.file.sp" + iSpec)
                    || !getConfiguration().isNull("mortality.fishing.rate.byDt.bySize.file.sp" + iSpec)) {
                fishingScenario[iSpec] = new ByDtByAgeSizeScenario(getIndexSimulation(), getSpecies(iSpec));
                continue;
            }
            if (!getConfiguration().isNull("mortality.fishing.rate.sp" + iSpec)) {
                if (!getConfiguration().isNull("mortality.fishing.season.distrib.file.sp" + iSpec)) {
                    fishingScenario[iSpec] = new AnnualFSeasonScenario(getIndexSimulation(), getSpecies(iSpec));
                } else {
                    fishingScenario[iSpec] = new AnnualFScenario(getIndexSimulation(), getSpecies(iSpec));
                }
                continue;
            }
        }
        // Initialize fishing scenario
        
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            fishingScenario[iSpec].init();
        }
    }

    @Override
    public void run() {
        for (School school : getSchoolSet().getPresentSchools()) {
            if (school.getAbundance() != 0.d) {
                double F = getInstantaneousRate(school, 1);
                double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-F));
                if (nDead > 0.d) {
                    school.setNdeadFishing(nDead);
                }
            }
        }
    }

    public double getInstantaneousRate(School school, int subdt) {
        if (isFishable(school)) {
            return fishingScenario[school.getSpeciesIndex()].getInstantaneousRate(school);
        } else {
            return 0.d;
        }
    }

    private boolean isFishable(School school) {
        // Test whether fishing applies to this school
        // 1. School is catchable (no MPA)
        // 2. School is not out of simulated domain
        return school.isCatchable() && !school.isUnlocated();
    }

    /*
     * F the annual mortality rate is calculated as the annual average
     * of the fishing rates over the years. 
     */
    public double getAnnualRate(Species species) {
        return fishingScenario[species.getIndex()].getAnnualRate();
    }
}
