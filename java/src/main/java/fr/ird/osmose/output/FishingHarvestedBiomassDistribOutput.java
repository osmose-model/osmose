package fr.ird.osmose.output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.stage.SchoolStage;
import fr.ird.osmose.util.SimulationLinker;

public class FishingHarvestedBiomassDistribOutput extends SimulationLinker implements IOutput {

    /** Output is the harvested biomass. Dimensions are [species][sizeClass][fisheries] */
    private double[][][] output;
    private SchoolStage sizeClasses;
    private int nFisheries;
    private FileOutputStream fos[];
    private PrintWriter prw[];
    private int recordFrequency;

    /**
     * CSV separator
     */
    private final String separator;

    public FishingHarvestedBiomassDistribOutput(int rank) {
        super(rank);
        separator = getConfiguration().getOutputSeparator();

    }

    @Override
    public void initStep() {
        // TODO Auto-generated method stub

    }

    @Override
    public void reset() {
        // initialisation of the accessible biomass
        output = new double[getNSpecies()][nFisheries][];
        for (int i = 0; i < getNSpecies(); i++) {
            int nClass = this.sizeClasses.getNStage(i);
            for (int j = 0; j < nFisheries; j++) {
                output[i][j] = new double[nClass];
            }
        }
    }

    @Override
    public void update() {
        // get accessible biomass (nfisheries, nspecies)
        for (int iFishery = 0; iFishery < nFisheries; iFishery++) {
            for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
                for (int iClass = 0; iClass < this.sizeClasses.getNStage(iSpecies); iClass++) {
                    output[iSpecies][iFishery][iClass] += getSimulation().getEconomicModule().getHarvestedBiomass(iFishery, iSpecies, iClass);
                }
            }
        }
    }

    @Override
    public void write(float time) {
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            for (int iClass = 0; iClass < this.sizeClasses.getNStage(iSpecies); iClass++) {
                prw[iSpecies].print(time);
                prw[iSpecies].print(separator);
                prw[iSpecies].print(iClass == 0 ? 0 : this.sizeClasses.getThresholds(iSpecies, iClass - 1));
                prw[iSpecies].print(separator);
                for (int iFishery = 0; iFishery < nFisheries - 1; iFishery++) {
                    // instantenous mortality rate for eggs additional mortality
                    prw[iSpecies].print(output[iSpecies][iFishery][iClass]);
                    prw[iSpecies].print(separator);
                }
                int iFishery = nFisheries - 1;
                prw[iSpecies].print(output[iSpecies][iFishery][iClass]);
                prw[iSpecies].println();
            }
        }
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }

    @Override
    public void init() {

        fos = new FileOutputStream[getNSpecies()];
        prw = new PrintWriter[getNSpecies()];
        nFisheries = getSimulation().getEconomicModule().getNFisheries();

        this.sizeClasses = getSimulation().getEconomicModule().getSizeClass();

        String[] namesFisheries = getSimulation().getEconomicModule().getFisheriesNames();

        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("Econ");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_HarvestedBiomassBy");
            filename.append(sizeClasses.getType(iSpecies));
            filename.append("-");
            filename.append(getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getRank());
            filename.append(".csv");
            File file = new File(path, filename.toString());

            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos[iSpecies] = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MortalityOutput.class.getName()).log(Level.SEVERE, null, ex);
            }
            prw[iSpecies] = new PrintWriter(fos[iSpecies], true);

            // Write headers
            prw[iSpecies].print(quote("Time"));
            prw[iSpecies].print(separator);
            prw[iSpecies].print(quote("Class Lower Bound"));
            prw[iSpecies].print(separator);
            for (int iFishery = 0; iFishery < nFisheries - 1; iFishery++) {
                String fishingName = namesFisheries[iFishery];
                prw[iSpecies].print(quote(fishingName));
                prw[iSpecies].print(separator);
            }
            int iFishery = nFisheries - 1;
            String fishingName = namesFisheries[iFishery];
            prw[iSpecies].print(quote(fishingName));
            prw[iSpecies].println();

        }

        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");


    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }

}
