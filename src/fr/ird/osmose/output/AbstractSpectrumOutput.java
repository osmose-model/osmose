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
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public abstract class AbstractSpectrumOutput extends AbstractOutput {

    // Indicator distribution by species and by size classes
    double[][] spectrum;
    // Minimal size (cm) of the size spectrum.
    private float min;
    // Maximal size (cm) of the size spectrum.
    private float max;
    // Range (cm) of size classes.
    private float range;
    // discrete size spectrum
    private float[] classes;
    // Number of size classes in the discrete spectrum
    private int nClass;
    //
    private final Type type;

    public AbstractSpectrumOutput(int rank, String keyEnabled, Type type) {
        super(rank, keyEnabled);
        this.type = type;
        initializeSizeSpectrum(type);
    }

    private void initializeSizeSpectrum(Type type) {

        if (!isEnabled()) {
            return;
        }

        switch (type) {
            case SIZE:
                if (!getConfiguration().isNull("output.size.spectrum.size.min")) {
                    min = getConfiguration().getFloat("output.size.spectrum.size.min");
                } else {
                    min = 0;
                    warning("Did not find parameter 'output.size.spectrum.size.min'. Default value set to " + min + " cm");
                }
                if (!getConfiguration().isNull("output.size.spectrum.size.max")) {
                    max = getConfiguration().getFloat("output.size.spectrum.size.max");
                } else {
                    max = 200.f;
                    warning("Did not find parameter 'output.size.spectrum.size.max'. Default value set to " + max + " cm");
                }
                if (!getConfiguration().isNull("output.size.spectrum.size.range")) {
                    range = getConfiguration().getFloat("output.size.spectrum.size.range");
                } else {
                    range = 5.f;
                    warning("Did not find parameter 'output.size.spectrum.size.range'. Default value set to " + range + " cm");
                }
                break;
            case AGE:
                if (!getConfiguration().isNull("output.age.spectrum.age.min")) {
                    min = getConfiguration().getFloat("output.age.spectrum.age.min");
                } else {
                    min = 0;
                    warning("Did not find parameter 'output.age.spectrum.age.min'. Default value set to " + min + " year");
                }
                if (!getConfiguration().isNull("output.age.spectrum.age.max")) {
                    max = getConfiguration().getFloat("output.age.spectrum.age.max");
                } else {
                    max = 0.f;
                    for (int i = 0; i < getNSpecies(); i++) {
                        max = Math.max(max, getSpecies(i).getLifespanDt());
                    }
                    max = Math.round(max / getConfiguration().getNStepYear()) - 1.f;
                    warning("Did not find parameter 'output.age.spectrum.age.max'. Default value set to " + max + " year");
                }
                if (!getConfiguration().isNull("output.age.spectrum.age.range")) {
                    range = getConfiguration().getFloat("output.age.spectrum.age.range");
                } else {
                    range = 1.f;
                    warning("Did not find parameter 'output.age.spectrum.age.range'. Default value set to " + range + " year");
                }
                break;
        }

        nClass = (int) Math.ceil(max / range);

        classes = new float[nClass];
        classes[0] = min;
        for (int i = 1; i < nClass; i++) {
            classes[i] = i * range;
        }
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        spectrum = new double[getNSpecies()][classes.length];
    }

    int getClass(School school) {

        float value = (type == Type.SIZE)
                ? school.getLength()
                : (float) school.getAgeDt() / getConfiguration().getNStepYear();

        return getClass(value);
    }

    int getClass(float value) {
        int iClass = classes.length - 1;
        if (value <= max) {
            while (value < classes[iClass]) {
                iClass--;
            }
        }
        return iClass;
    }

    @Override
    public void write(float time) {

        double[][] values = new double[nClass][getNSpecies() + 1];
        for (int iSize = 0; iSize < nClass; iSize++) {
            values[iSize][0] = classes[iSize];
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                values[iSize][iSpec + 1] = spectrum[iSpec][iSize] / getRecordFrequency();
            }
        }
        writeVariable(time, values);
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + 1];
        headers[0] = (type == Type.SIZE)
                ? "Size"
                : "Age";
        for (int i = 0; i < getNSpecies(); i++) {
            headers[i + 1] = getSimulation().getSpecies(i).getName();
        }
        return headers;
    }

    float getClassThreshold(int iClass) {
        return classes[iClass];
    }

    int getNClass() {
        return classes.length;
    }

    Type getType() {
        return type;
    }

    public enum Type {

        SIZE, AGE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
