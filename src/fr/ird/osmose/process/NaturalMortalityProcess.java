package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.naturalmortality.AnnualNaturalMortalityScenario;
import fr.ird.osmose.process.naturalmortality.ByDtByAgeSizeNaturalMortalityScenario;
import fr.ird.osmose.process.naturalmortality.ByDtLarvaMortalityScenario;
import fr.ird.osmose.process.naturalmortality.ConstantLarvaMortalityScenario;
import fr.ird.osmose.util.GridMap;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class NaturalMortalityProcess extends AbstractProcess {

    private AbstractMortalityScenario[] larvaMortality;
    private AbstractMortalityScenario[] naturalMortality;
    /**
     * Spatial factor for natural mortality [0, 1]
     */
    private GridMap[] spatialD;

    public NaturalMortalityProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        int iSimu = getIndexSimulation();

        // Larva mortality
        larvaMortality = new AbstractMortalityScenario[getNSpecies()];
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSpecies(iSpec);
            // Larva mortality by Dt
            if (!getConfiguration().isNull("mortality.natural.larva.rate.bytDt.file.sp" + iSpec)) {
                larvaMortality[iSpec] = new ByDtLarvaMortalityScenario(iSimu, species);
                continue;
            }
            // Constant larva mortality
            if (!getConfiguration().isNull("mortality.natural.larva.rate.sp" + iSpec)) {
                larvaMortality[iSpec] = new ConstantLarvaMortalityScenario(iSimu, species);
                continue;
            }
            // Did not find any scenario for larva mortality, Osmose assumes larva mortality = 0.
            // Warning only because some species might not reproduce in the 
            // simulated area and therefore have no need to define larva mortality
            larvaMortality[iSpec] = new ConstantLarvaMortalityScenario(iSimu, species, 0.f);
            getLogger().log(Level.WARNING, "Could not find any parameters for larva mortality (mortality.natural.larva.rate.bytDt.file.sp# or mortality.natural.larva.rate.sp#) for species {0}. Osmose assumes larva mortality = 0", species.getName());
        }

        // Natural mortality
        naturalMortality = new AbstractMortalityScenario[getNSpecies()];

        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            Species species = getSpecies(iSpec);
            // Natural mortality by Dt, by Age or Size
            if (!getConfiguration().isNull("mortality.natural.rate.byDt.byAge.file.sp" + iSpec)) {
                naturalMortality[iSpec] = new ByDtByAgeSizeNaturalMortalityScenario(iSimu, species);
                continue;
            }
            // Annual natural mortality
            if (!getConfiguration().isNull("mortality.natural.rate.sp" + iSpec)) {
                naturalMortality[iSpec] = new AnnualNaturalMortalityScenario(iSimu, species);
                continue;
            }
            // Did not find any scenario for natural mortality. Throws error.
            getLogger().log(Level.SEVERE, "Could not find any parameters for natural mortality (mortality.natural.rate.byDt.byAge.file.sp# or mortality.natural.rate.sp#) for species {0}", species.getName());
            System.exit(1);
        }

        // Initialize mortality scenarii
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            larvaMortality[iSpec].init();
            naturalMortality[iSpec].init();
        }

        // Patch for Ricardo to include space variability in natural mortality
        // Need to think of a better parametrization before including it
        // formally in Osmose
        spatialD = new GridMap[getNSpecies()];
        List<String> keys = getConfiguration().findKeys("mortality.natural.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
                if (!getConfiguration().isNull("mortality.natural.spatial.distrib.file.sp" + iSpec)) {
                    spatialD[iSpec] = getConfiguration().readCSVMap(getConfiguration().getFile("mortality.natural.spatial.distrib.file.sp" + iSpec));
                }
            }
        }
    }

    @Override
    public void run() {
        // Natural mortality (due to other predators)
        for (School school : getSchoolSet()) {
            double M = getInstantaneousRate(school, 1);
            double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-M));
            if (nDead > 0.d) {
                school.setNdeadNatural(nDead);
            }
        }
    }

    /**
     * For all species, D is due to other predators (seals, seabirds) for
     * migrating species, we add mortality because absents during a time step so
     * they don't undergo mortalities due to predation and starvation Additional
     * mortalities for ages 0: no-fecundation of eggs, starvation more
     * pronounced than for sup ages (rel to CC), predation by other species are
     * not explicit.
     */
    public double getInstantaneousRate(School school, int subdt) {
        double M;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            M = larvaMortality[school.getSpeciesIndex()].getInstantaneousRate(school) / (float) subdt;
        } else {
            if (null != spatialD[spec.getIndex()] && !school.isUnlocated()) {
                M = (spatialD[spec.getIndex()].getValue(school.getCell()) * naturalMortality[school.getSpeciesIndex()].getInstantaneousRate(school)) / (float) subdt;
            } else {
                M = naturalMortality[school.getSpeciesIndex()].getInstantaneousRate(school) / (float) (subdt);
            }
        }
        return M;
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the larval mortality rates over the years.
     */
    public double getLarvalAnnualRate(Species species) {
        return larvaMortality[species.getIndex()].getAnnualRate();
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the natural mortality rates over the years.
     */
    public double getAnnualRate(Species species) {
        return naturalMortality[species.getIndex()].getAnnualRate();
    }
}
