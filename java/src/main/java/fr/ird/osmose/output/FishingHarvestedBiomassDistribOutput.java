package fr.ird.osmose.output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.util.SimulationLinker;

public class FishingHarvestedBiomassDistribOutput extends SimulationLinker implements IOutput {
    
    /** Output is the harvested biomass. Dimensions are [species][sizeClass][fisheries] */ 
    private double[][][] output;
    private AbstractDistribution sizeClasses;
    private int nFisheries, nSpecies;
    private FileOutputStream fos[];
    private PrintWriter prw[];
    private int recordFrequency;
    private int nClass;
    
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
        int nClass = this.sizeClasses.getNClass();
        output = new double[nSpecies][nFisheries][nClass];
    }

    @Override
    public void update() {
        // get accessible biomass (nfisheries, nspecies)
        for (int iFishery = 0; iFishery < nFisheries; iFishery++) {
            for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
                for (int iClass = 0; iClass < nClass; iClass++) {
                    output[iSpecies][iFishery][iClass] += getSimulation().getEconomicModule().getHarvestedBiomass(iFishery, iSpecies, iClass);
                }
            }
        }
    }

    @Override
    public void write(float time) {
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            for (int iClass = 0; iClass < nClass; iClass++) {
                prw[iSpecies].print(time);
                prw[iSpecies].print(separator);
                prw[iSpecies].print(this.sizeClasses.getThreshold(iClass));
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
        nSpecies = getConfiguration().getNSpecies();
        
        this.sizeClasses = getSimulation().getEconomicModule().getSizeClass();
        
        String[] namesFisheries = getSimulation().getEconomicModule().getFisheriesNames();
        nClass = this.sizeClasses.getNClass();
        
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            // Create parent directory
            File path = new File(getConfiguration().getOutputPathname());
            StringBuilder filename = new StringBuilder("Econ");
            filename.append(File.separatorChar);
            filename.append(getConfiguration().getString("output.file.prefix"));
            filename.append("_HarvestedBiomassBy");
            filename.append(sizeClasses.getType());
            filename.append("-");
            filename.append(getSpecies(iSpecies).getName());
            filename.append("_Simu");
            filename.append(getRank());
            filename.append(".csv");
            File file = new File(path, filename.toString());
            boolean fileExists = file.exists();
            file.getParentFile().mkdirs();
            try {
                // Init stream
                fos[iSpecies] = new FileOutputStream(file, true);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MortalityOutput.class.getName()).log(Level.SEVERE, null, ex);
            }
            prw[iSpecies] = new PrintWriter(fos[iSpecies], true);
            if (!fileExists) {
                // Write headers
                prw[iSpecies].print(quote("Time"));
                prw[iSpecies].print(separator);
                prw[iSpecies].print(quote("Class"));
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
