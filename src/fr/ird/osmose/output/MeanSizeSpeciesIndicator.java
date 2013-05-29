package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class MeanSizeSpeciesIndicator extends SimulationLinker implements Indicator {

    // IO
    private FileOutputStream[] fos;
    private PrintWriter[] prw;
    private double[][] meanSize;
    private double[][] abundance;
    /**
     * Whether the indicator should be enabled or not.
     */
    private boolean enabled;

    public MeanSizeSpeciesIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation);
        enabled = getConfiguration().getBoolean(keyEnabled);
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {

        int nSpecies = getNSpecies();
        meanSize = new double[nSpecies][];
        abundance = new double[nSpecies][];
        for (int i = 0; i < nSpecies; i++) {
            meanSize[i] = new double[getSpecies(i).getLifespanDt()];
            abundance[i] = new double[getSpecies(i).getLifespanDt()];
        }
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int i = school.getSpeciesIndex();
            meanSize[i][school.getAgeDt()] += school.getInstantaneousAbundance() * school.getLength();
            abundance[i][school.getAgeDt()] += school.getInstantaneousAbundance();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void write(float time) {

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            prw[iSpecies].print(time);
            for (int iAge = 0; iAge < getSpecies(iSpecies).getLifespanDt(); iAge++) {
                prw[iSpecies].print(";");
                if (abundance[iSpecies][iAge] > 0) {
                    meanSize[iSpecies][iAge] = (float) (meanSize[iSpecies][iAge] / abundance[iSpecies][iAge]);
                } else {
                    meanSize[iSpecies][iAge] = Double.NaN;
                }
                prw[iSpecies].print((float) meanSize[iSpecies][iAge]);
            }
            prw[iSpecies].println();
        }
    }

    @Override
    public void init() {
        fos = new FileOutputStream[getNSpecies()];
        prw = new PrintWriter[getNSpecies()];
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("SizeIndicators");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_meanSize-");
            filename.append(getSimulation().getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getSimulation().getReplica());
            filename.append(".csv");
            File file = new File(path, filename.toString());
            boolean fileExists = file.exists();
            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos[iSpecies] = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                getLogger().log(Level.SEVERE, "Failed to create indicator file " + file.getAbsolutePath(), ex);
            }
            prw[iSpecies] = new PrintWriter(fos[iSpecies], true);
            if (!fileExists) {
                // Write headers
                prw[iSpecies].print("\"");
                prw[iSpecies].print("Mean size of fish species by age class in cm, weighted by fish numbers");
                prw[iSpecies].println("\"");
                prw[iSpecies].print("Time");
                for (int iAge = 0; iAge < getSpecies(iSpecies).getLifespanDt(); iAge++) {
                    prw[iSpecies].print(";Age class ");
                    prw[iSpecies].print(iAge);
                }
                prw[iSpecies].println();
            }
        }
    }

    @Override
    public void close() {
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            if (null != prw) {
                prw[iSpecies].close();
            }
            if (null != fos) {
                try {
                    fos[iSpecies].close();
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
