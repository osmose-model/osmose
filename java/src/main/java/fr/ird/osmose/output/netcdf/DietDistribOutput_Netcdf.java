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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ird.osmose.Prey;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.output.distribution.OutputDistribution;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class DietDistribOutput_Netcdf extends AbstractDistribOutput_Netcdf {

    private final Species species;

    public DietDistribOutput_Netcdf(int rank, Species species, OutputDistribution distrib) {
        super(rank, distrib);
        this.species = species;
    }

    @Override
    public void reset() {
        values = new double[getConfiguration().getNAllSpecies()][getNClass()];
    }

    @Override
    String getFilename() {

        StringBuilder filename = this.initFileName();
        filename.append("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_dietMatrixby");
        filename.append(getType().toString());
        filename.append("-");
        filename.append(species.getName());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Distribution of the biomass (tonne) of prey species (in columns) in the diet of ");
        description.append(species.getName());
        description.append(" by ");
        description.append(getType().getDescription());
        description.append(". For class i, the preyed biomass in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    public void update() {

        for (School predator : getSchoolSet().getSchools(species, false)) {
            double preyedBiomass = predator.getPreyedBiomass();
            if (preyedBiomass > 0) {
                for (Prey prey : predator.getPreys()) {
                    int classPredator = getClass(predator);
                    if (classPredator >= 0) {
                        values[prey.getFileSpeciesIndex()][classPredator] += prey.getBiomass();
                    }
                }
            }
        }
    }

    /**
     * Init the NetCDF file. Intitialize the output files by setting the NetCDF
     * dimension array + setting coordinates.
     */
    @Override
    void init_nc_dims_coords() {

        // Defines the prey dimension and coordinate.
        Dimension preyDim = getBNc().addDimension("prey_index", getNSpecies() + getConfiguration().getNRscSpecies());

        Variable.Builder<?> preyvarBuilder = getBNc().addVariable("prey_index", DataType.INT, preyDim.getName());
        int k = 0;
        for (int i = 0; i < getNAllSpecies(); i++) {
            String name = String.format("prey%d", k);
            preyvarBuilder.addAttribute(new Attribute(name, getISpecies(i).getName()));
            k++;
        }

        // Defines the prey dimension and coordinate.
        Dimension classDim = getBNc().addDimension(this.getDisName(), this.getNClass());
        getBNc().addVariable(this.getDisName(), DataType.FLOAT, classDim.getName());

        this.setDims(new ArrayList<>(Arrays.asList(getTimeDim(), classDim, preyDim)));

        getBNc().addAttribute(new Attribute("Species: ", this.species.getName()));

    }

    @Override
    public void write_nc_coords() {

        try {
            // Writes variable trait (trait names) and species (species names)
            ArrayInt arrSpecies = new ArrayInt(new int[] {this.getNSpecies() + getConfiguration().getNRscSpecies()}, false);
            Index index = arrSpecies.getIndex();
            for (int i = 0; i < this.getNSpecies(); i++) {
                index.set(i);
                arrSpecies.set(index, i);
            }

            Variable preyvar = this.getNc().findVariable("prey_index");
            getNc().write(preyvar, arrSpecies);

            // Writes variable trait (trait names) and species (species names)
            ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.getNClass());

            arrClass.set(0, 0);
            for (int i = 0; i < this.getNClass() - 1; i++) {
                arrClass.set(i + 1, this.getClassThreshold(i));
            }

            Variable disvar = this.getNc().findVariable(this.getDisName());
            getNc().write(disvar, arrClass);

        } catch (IOException | InvalidRangeException ex) {
            Logger.getLogger(DietDistribOutput_Netcdf.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    String getUnits() {
        return ("tons"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return ("prey_biomass"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNColumns() {
        return getNAllSpecies();
    }
}
