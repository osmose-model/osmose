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

import fr.ird.osmose.School;
import java.io.File;

/**
 *
 * @author pverley
 */
public class SizeSpectrumIndicator extends AbstractIndicator {

    private double[][] sizeSpectrum;
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

    public SizeSpectrumIndicator(int rank, String keyEnabled) {
        super(rank, keyEnabled);
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
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        sizeSpectrum = new double[getNSpecies()][tabSizes.length];
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            sizeSpectrum[school.getSpeciesIndex()][getSizeRank(school)] += school.getInstantaneousAbundance();
        }
    }
    
    private int getSizeRank(School school) {

        int iSize = tabSizes.length - 1;
        if (school.getLength() <= spectrumMaxSize) {
            while (school.getLength() < tabSizes[iSize]) {
                iSize--;
            }
        }
        return iSize;
    }

    @Override
    public void write(float time) {

        double[][] values = new double[nSizeClass][2];
        for (int iSize = 0; iSize < nSizeClass; iSize++) {
            // Size
            values[iSize][0] = tabSizes[iSize];
            // Abundance
            double sum = 0f;
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                sum += sizeSpectrum[iSpec][iSize] / getRecordFrequency();
            }
            values[iSize][1] = sum;
        }
        
        writeVariable(time, values);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_SizeSpectrum_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();

    }

    @Override
    String getDescription() {
        return "Distribution of fish abundance in size classes (cm). For size class i, the number of fish in [i,i+1[ is reported.";
    }

    @Override
    String[] getHeaders() {
        return new String[]{"Size", "Abundance"};
    }
}
