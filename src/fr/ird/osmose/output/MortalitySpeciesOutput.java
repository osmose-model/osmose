/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.Prey.MortalityCause;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.File;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class MortalitySpeciesOutput extends AbstractOutput {

    private final Species species;
    /*
     * Abundance per stages [STAGES]
     */
    private double[] abundanceStage;
    // mortality rates por souces and per stages
    private double[][] mortalityRates;
    // Minimal size (cm) of the size spectrum.
    public float spectrumMinSize;
    // Maximal size (cm) of the size spectrum.
    public float spectrumMaxSize;
    // Range (cm) of size classes.
    private float classRange;
    // discrete size spectrum
    private float[] tabSizes;
    // Number of size classes in the discrete spectrum
    private int nSizeClass;

    public MortalitySpeciesOutput(int rank, String keyEnabled, Species species) {
        super(rank, keyEnabled);
        this.species = species;
        initializeSizeSpectrum();
    }

    private void initializeSizeSpectrum() {

        if (!isEnabled()) {
            return;
        }

        spectrumMinSize = getConfiguration().getFloat("output.size.spectrum.size.min");
        spectrumMaxSize = getConfiguration().getFloat("output.size.spectrum.size.max");
        classRange = getConfiguration().getFloat("output.size.spectrum.size.range");

        //initialisation of the size spectrum features
        nSizeClass = (int) Math.ceil(spectrumMaxSize / classRange);//size classes of 5 cm

        tabSizes = new float[nSizeClass];
        tabSizes[0] = spectrumMinSize;
        for (int i = 1; i < nSizeClass; i++) {
            tabSizes[i] = i * classRange;
        }
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Mortality");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_mortalityRatePerSize-");
        filename.append(species.getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Predation (Mpred), Starvation (Mstarv), Other Natural mortality (Mnat), Fishing (F) & Out-of-domain (Z) mortality rates per time step of saving and per size class. Z is the total mortality for migratory fish outside the simulation grid. To get annual mortality rates, sum the mortality rates within one year.";
    }

    @Override
    public void reset() {
        mortalityRates = new double[MortalityCause.values().length][tabSizes.length];
    }

    @Override
    public void update() {

        int iStage;
        int nCause = MortalityCause.values().length;
        double[][] nDead = new double[nCause][nSizeClass];
        for (School school : getSchoolSet().getSchools(species, false)) {
            iStage = getSizeRank(school);
            // Update number of deads
            for (MortalityCause cause : MortalityCause.values()) {
                nDead[cause.index][iStage] += school.getNdead(cause);
            }
        }
        // Cumulate the mortality rates

        for (iStage = 0; iStage < nSizeClass; iStage++) {
            double nDeadTot = 0;
            for (int iDeath = 0; iDeath < nCause; iDeath++) {
                nDeadTot += nDead[iDeath][iStage];
            }
            double Ftot = Math.log(abundanceStage[iStage] / (abundanceStage[iStage] - nDeadTot));
            for (int iDeath = 0; iDeath < nCause; iDeath++) {
                mortalityRates[iDeath][iStage] += Ftot * nDead[iDeath][iStage] / ((1 - Math.exp(-Ftot)) * abundanceStage[iStage]);
            }
        }
    }

    private int getSizeRank(School school) {

        int iSize = tabSizes.length - 1;
        if (school.getLengthi() <= spectrumMaxSize) {
            while (school.getLengthi() < tabSizes[iSize]) {
                iSize--;
            }
        }
        return iSize;
    }

    @Override
    public void write(float time) {

        int nCause = MortalityCause.values().length;
        double[][] values = new double[nSizeClass][nCause + 1];
        for (int iSize = 0; iSize < nSizeClass; iSize++) {
            // Size
            values[iSize][0] = tabSizes[iSize];
            // Mortality rates
            for (int iDeath = 0; iDeath < nCause; iDeath++) {
                values[iSize][iDeath + 1] = mortalityRates[iDeath][iSize];
            }
        }

        writeVariable(time, values);
    }

    @Override
    String[] getHeaders() {
        return new String[]{"Size", "Mpred", "Mstar", "Mnat", "F", "Z"};
    }

    @Override
    public void initStep() {
        // Reset abundance array 
        abundanceStage = new double[tabSizes.length];

        // save abundance at the beginning of the time step
        for (School school : getSchoolSet().getSchools(species, false)) {
            int iStage = getSizeRank(school);
            abundanceStage[iStage] += school.getAbundance();
        }
    }
}
