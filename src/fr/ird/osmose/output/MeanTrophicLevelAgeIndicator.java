package fr.ird.osmose.output;

import fr.ird.osmose.School;
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanTrophicLevelAgeIndicator extends AbstractIndicator {

    private double[][] meanTL;
    private double[][] biomass;

     public MeanTrophicLevelAgeIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation, keyEnabled);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanTLPerAge_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean Trophic Level of fish species by age class.";
    }

    @Override
    String[] getHeaders() {

        int classmax = 0;
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            classmax = (int) Math.max(Math.ceil(getConfiguration().getFloat("species.lifespan.sp" + iSpecies)) , classmax);
        }
        String[] headers = new String[classmax + 1];
        headers[0] = "Species index";
        for (int i = 0; i < classmax; i++) {
            headers[i + 1] = "Age class " + i;
        }
        return headers;
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        meanTL = new double[getNSpecies()][];
        biomass = new double[getNSpecies()][];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            meanTL[iSpecies] = new double[(int) Math.ceil(getConfiguration().getFloat("species.lifespan.sp" + iSpecies))];
            biomass[iSpecies] = new double[(int) Math.ceil(getConfiguration().getFloat("species.lifespan.sp" + iSpecies))];
        }
    }

    @Override
    public void update() {
        int nstep = getConfiguration().getNStepYear();
        for (School school : getSchoolSet().getAliveSchools()) {
            int i = school.getSpeciesIndex();
            double biom = school.getInstantaneousBiomass();
            int ageClass = school.getAgeDt() / nstep;
            meanTL[i][ageClass] += biom * school.getTrophicLevel();
            biomass[i][ageClass] += biom;
        }
    }

    @Override
    public void write(float time) {

        double[][] values = new double[getNSpecies()][];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            values[iSpecies] = new double[meanTL[iSpecies].length + 1];
            values[iSpecies][0] = iSpecies;
            for (int ageClass = 0; ageClass < meanTL[iSpecies].length; ageClass++) {
                if (biomass[iSpecies][ageClass] > 0.d) {
                    meanTL[iSpecies][ageClass] = (float) (meanTL[iSpecies][ageClass] / biomass[iSpecies][ageClass]);
                } else {
                    meanTL[iSpecies][ageClass] = Double.NaN;
                }
                values[iSpecies][ageClass + 1] = meanTL[iSpecies][ageClass];
            }
        }
        
        writeVariable(time, values);
    }
}
