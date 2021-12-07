/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.AbstractSchool;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import fr.ird.osmose.process.mortality.fishery.FisheryBase;
import fr.ird.osmose.process.mortality.fishery.FisheryPeriod;
import fr.ird.osmose.process.mortality.fishery.FisherySeasonality;
import fr.ird.osmose.process.mortality.fishery.FisheryMapSet;
import fr.ird.osmose.process.mortality.fishery.FisherySelectivity;
import fr.ird.osmose.stage.SizeStage;
import fr.ird.osmose.util.Matrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class FishingGear extends AbstractMortality {

    private String name;

    /**
     * Fishery index.
     */
    private final int fileFisheryIndex;

    // Initialize the time varying array
    private FisheryBase fishingBase;
    private FisheryPeriod fishingPeriod;
    private FisherySeasonality fishingSeasonality;
    
    // If economy is on, define a size stage array for fisheries.
    private boolean isEconomyEnabled;
    
    // sizeClasses used to determine variables for fishing economy (costs, etc.)
    private SizeStage sizeClasses; 
    
    /** Total accessible biomass. Depends on species and size-class. */
    private double[] accessibleBiomass;
    
    /** Accessible biomass ponderated by the price of the species. */
    private double[] priceAccessibleBiomass;
    
    /** Total harvested biomass. Depends on species and size-class. */
    private double[][] harvestedBiomass;
    
    /**
     * Fishery map set.
     */
    private FisheryMapSet fisheryMapSet;

    private double[] catchability;
    private double[] discards;

    private FisherySelectivity selectivity;
    private boolean checkFisheryEnabled;

    public FishingGear(int rank, int fisheryIndex) {
        super(rank);
        this.fileFisheryIndex = fisheryIndex;
    }

    @Override
    public void init() {

        Configuration cfg = Osmose.getInstance().getConfiguration();
        
        int nspecies = cfg.getNSpecies();
        int nbackground = cfg.getNBkgSpecies();
        this.isEconomyEnabled = cfg.isEconomyEnabled();
        
        catchability = new double[nspecies + nbackground];
        discards = new double[nspecies + nbackground];

        // set-up the name of the fishery
        name = cfg.getString("fisheries.name.fsh" + fileFisheryIndex);

        // True if fishing mortality components are to be saved for inspection
        checkFisheryEnabled = cfg.getBoolean("fisheries.check.enabled");

        if (this.isEconomyEnabled) {
            // upper bounds of size classes. if 5 values provides, 6 classes:
            // [0, l1[, [l1, l2[, [l2, l3[, [l3, l4[, [l4, l5[, [l5, inf]
            this.sizeClasses = new SizeStage("fisheries.size.class.fsh" + fileFisheryIndex);
            this.sizeClasses.init();

            // Accessible biomass
            this.accessibleBiomass = new double[this.getNSpecies()];
            this.priceAccessibleBiomass = new double[this.getNSpecies()];
            
            this.harvestedBiomass = new double[this.getNSpecies()][];
            for (int iSpecies = 0; iSpecies < nspecies; iSpecies++) {
                int nSizeClass = this.sizeClasses.getNStage(iSpecies);
                this.harvestedBiomass[iSpecies] = new double[nSizeClass];
            }
        }
        
        // Initialize the time varying array
        fishingBase = new FisheryBase(fileFisheryIndex);
        fishingBase.init();

        fishingPeriod = new FisheryPeriod(fileFisheryIndex);
        fishingPeriod.init();

        fishingSeasonality = new FisherySeasonality(fileFisheryIndex);
        fishingSeasonality.init();

        // fishery spatial maps
        fisheryMapSet = new FisheryMapSet(name, "fisheries.movement", "fishery");
        fisheryMapSet.init();

        selectivity = new FisherySelectivity(fileFisheryIndex, "fisheries.selectivity", "fsh");
        selectivity.init();

        if (checkFisheryEnabled) {
            try {
                this.writeFisheriesTimeSeries();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FishingGear.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Returns the fishing mortality rate associated with a given fishery. It is
     * the product of the time varying fishing rate, of the size selectivity and
     * of the spatial factor.
     *
     * @param school
     * @return The fishing mortality rate.
     */
    public double getRate(AbstractSchool school) throws Exception {

        int index = getSimulation().getIndexTimeSimu();

        // Recovers the school cell (used to recover the map factor)
        Cell cell = school.getCell();

        double spatialSelect = this.fisheryMapSet.getValue(index, cell);
        if (spatialSelect == 0.0) {
            return 0.0;
        }

        int speciesIndex = school.getSpeciesIndex();

        double speciesCatchability = this.catchability[speciesIndex];
        if (speciesCatchability == 0.d) {
            return 0.d;
        }

        // recovers the time varying rate of the fishing mortality
        // as a product of FBase, FSeason and FSeasonality
        double timeSelect = fishingBase.getFisheryBase(index);
        timeSelect *= this.fishingPeriod.getFisheryPeriod(index);
        timeSelect *= this.fishingSeasonality.getFisherySeasonality(index);

        // Recovers the size/age fishery selectivity factor [0, 1]
        double sizeSelect = selectivity.getSelectivity(index, school);

        return speciesCatchability * timeSelect * sizeSelect * spatialSelect;
    }

    /**
     * Get the percentage of discards for the given species and the given
     * time-step.
     *
     * @param school
     * @return
     */
    public double getDiscardRate(AbstractSchool school) {
        int speciesIndex = school.getSpeciesIndex();
        return this.discards[speciesIndex];
    }

    /**
     * Returns the fishery index.
     *
     * @return the fishery index
     */
    public int getFisheryIndex() {
        return this.fileFisheryIndex;
    }

    /**
     * Returns the fishery name.
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of the fishery.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public double getRate(School school) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void writeFisheriesTimeSeries() throws FileNotFoundException {

        PrintWriter prw;

        // Create parent directory
        File file = new File(getFilename());
        file.getParentFile().mkdirs();
        try {
            // Init stream
            prw = new PrintWriter(file);
        } catch (FileNotFoundException ex) {
            error("Failed to create output file " + file.getAbsolutePath(), ex);
            throw (ex);
        }

        String separator = getConfiguration().getOutputSeparator();
        String[] headers = {"Time", "Fbase", "Fperiod", "Fseasonality", "Ftot"};

        // Write headers
        for (int i = 0; i < headers.length - 1; i++) {
            prw.print(headers[i]);
            prw.print(separator);
        }

        prw.print(headers[headers.length - 1]);
        prw.println();

        for (int i = 0; i < this.getConfiguration().getNStep(); i++) {

            double fbase = this.fishingBase.getFisheryBase(i);
            double fseason = this.fishingPeriod.getFisheryPeriod(i);
            double fseasonality = this.fishingSeasonality.getFisherySeasonality(i);
            double ftot = fbase * fseason * fseasonality;

            prw.print(i);
            prw.print(separator);

            prw.print(fbase);
            prw.print(separator);

            prw.print(fseason);
            prw.print(separator);

            prw.print(fseasonality);
            prw.print(separator);

            prw.print(ftot);
            prw.println();
        }

        prw.close();

    }

    final String getFilename() {
        StringBuilder filename = new StringBuilder();
        String subfolder = "fisheries_checks";
        filename.append(subfolder).append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_").append(name).append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();

    }

    public void setCatchability(Matrix matrix) {

        int fishIndex = matrix.getIndexPred(this.name);
        int nTot = this.getNSpecies() + this.getNBkgSpecies();
        for (int cpt = 0; cpt < nTot; cpt++) {
            String speciesName = getISpecies(cpt).getName();
            int speciesIndex = matrix.getIndexPrey(speciesName);
            catchability[cpt] = matrix.getValue(speciesIndex, fishIndex);
        }
    }

    public void setDiscards(Matrix matrix) {

        int fishIndex = matrix.getIndexPred(this.name);
        int nTot = this.getNSpecies() + this.getNBkgSpecies();
        for (int cpt = 0; cpt < nTot; cpt++) {
            String speciesName = getISpecies(cpt).getName();
            int speciesIndex = matrix.getIndexPrey(speciesName);
            discards[cpt] = matrix.getValue(speciesIndex, fishIndex);
        }
    }
    
    /** Returns the gear selectivity. 
     * Used to compute available biomass.
     */
    public double getSelectivity(int index, AbstractSchool school) {
        return selectivity.getSelectivity(index, school);
    }
    
    public int getSizeClass(AbstractSchool school)  {
        return sizeClasses.getStage(school);
    }
       
    /** Init computation of total accessible biomass. */
    public void initAccessBiomass() {
        int index = this.getSimulation().getIndexTimeSimu();
        this.accessibleBiomass = new double[getNSpecies()];
        this.priceAccessibleBiomass = new double[getNSpecies()];

        for (School school : this.getSchoolSet().getAliveSchools()) {
            int iSpecies = school.getSpeciesIndex();
            double sel = this.getSelectivity(index, school);
            // double cat = this.catchability[iSpecies];

            // get the price that corresponds to the given length of the school
            double prices = getSpecies(iSpecies).getPrices().getValue(index, school.getLength());

            double temp = school.getInstantaneousBiomass() * sel;
            this.accessibleBiomass[iSpecies] += temp;
            this.priceAccessibleBiomass[iSpecies] += temp * prices;
        }
    }
    
    /** Recovers the value of total accessible biomass. **/ 
    public double[] getAccessibleBiomass() {
        return this.accessibleBiomass;
    }
    
    /** Recovers the value of total accessible biomass for a given species. **/
    public double getAccessibleBiomass(int iSpecies) {
        return this.accessibleBiomass[iSpecies];
    }
    
    /** Recovers the value of total priced accessible biomass. **/
    public double[] getPriceAccessibleBiomass() {
        return this.priceAccessibleBiomass;
    }

    /** Recovers the value of total priced accessible biomass for a given species. **/
    public double getPriceAccessibleBiomass(int iSpecies) {
        return this.priceAccessibleBiomass[iSpecies];
    }

    /** Recovers the value of total accessible biomass. **/
    public double[][] getHarvestedBiomass() {
        return this.harvestedBiomass;
    }

    /** Increment the harvested biomass.
     * @param nDead Number of fished individuals.
     * @param school School object of the school that has been fished. */
    public void incrementHarvestedBiomass(double nDead, AbstractSchool school) {
        int iSpecies = school.getSpeciesIndex();   
        int iClass = this.getSizeClass(school);
        this.harvestedBiomass[iSpecies][iClass] += school.abd2biom(nDead);
    }
    
    /** Reinitialize the harbvested biomass. */
    public void resetHarvestedBiomass() {
        int nspecies = getNSpecies();
        this.harvestedBiomass = new double[nspecies][];
        for (int iSpecies = 0; iSpecies < nspecies; iSpecies++) {
            int nSizeClass = this.sizeClasses.getNStage(iSpecies);
            this.harvestedBiomass = new double[this.getNSpecies()][nSizeClass];
        }
    }
}
