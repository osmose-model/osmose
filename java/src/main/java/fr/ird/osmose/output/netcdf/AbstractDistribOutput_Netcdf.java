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

package fr.ird.osmose.output.netcdf;

import fr.ird.osmose.IMarineOrganism;
import fr.ird.osmose.output.distribution.OutputDistribution;
import fr.ird.osmose.output.distribution.DistributionType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public abstract class AbstractDistribOutput_Netcdf extends AbstractOutput_Netcdf {

    // Output values distributed by species and by class
    double[][] values;
    // Distribution
    private final OutputDistribution distrib;

    public AbstractDistribOutput_Netcdf(int rank, OutputDistribution distrib) {
        super(rank);
        this.distrib = distrib;
    }

    @Override
    public void reset() {
        values = new double[getNSpecies()][distrib.getNClass()];
    }

    int getClass(IMarineOrganism school) {
        return distrib.getClass(school);
    }


    @Override
    public void write(float time) {

        int nClass = distrib.getNClass();
        double[][] array = new double[nClass][getNSpecies()];
        for (int iClass = 0; iClass < nClass; iClass++) {
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                array[iClass][iSpec] = values[iSpec][iClass] / getRecordFrequency();
            }
        }
        writeVariable(time, array);
    }


    float getClassThreshold(int iClass) {
        return distrib.getThreshold(iClass);
    }

    int getNClass() {
        return distrib.getNClass();
    }

    DistributionType getType() {
        return distrib.getType();
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    @Override
    void init_nc_dims_coords() {

        Dimension speciesDim = getBNc().addDimension("species", getNSpecies());
        Dimension classDim = getBNc().addDimension(this.getDisName(), this.distrib.getNClass());
        Variable.Builder<?> speciesVar = getBNc().addVariable("species", DataType.INT, "species");
        getBNc().addVariable(this.getDisName(), DataType.FLOAT, this.getDisName());

        this.createSpeciesAttr(speciesVar);

        // Initialize the outdims (time, class, species) as a NetCDF file
        List<Dimension> outdims = new ArrayList<>(Arrays.asList(getTimeDim(), classDim, speciesDim));
        this.setDims(outdims);

    }

    @Override
    public void write_nc_coords() {
        try {

            // Writes variable trait (trait names) and species (species names)
            ArrayInt arrSpecies = new ArrayInt(new int[] {this.getNSpecies()}, false);
            ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.distrib.getNClass());
            Index index = arrSpecies.getIndex();

            for (int i = 0; i < this.getNSpecies(); i++) {
                index.set(i);
                arrSpecies.set(index, i);
            }

            Variable varspec = this.getNc().findVariable("species");
            getNc().write(varspec, arrSpecies);

            for (int i = 0; i < this.distrib.getNClass(); i++) {
                arrClass.set(i, this.getClassThreshold(i));
            }
            Variable vardis = this.getNc().findVariable(this.getDisName());
            getNc().write(vardis, arrClass);

        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(AbundanceOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String getDisName() {
        return this.distrib.getType().toString();
    }

}
