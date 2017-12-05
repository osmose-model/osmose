/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.version;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.util.Separator;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author pverley
 */
public class Version3Update2 extends AbstractVersion {

    public Version3Update2() {
        super(3, 2, 2015, Calendar.MARCH, 1);
    }

    @Override
    void updateParameters() {

        int nSpecies = getConfiguration().getInt("simulation.nspecies");

        // Renamed population.initialization.biomass.sp# into population.seeding.biomass.sp#
        for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
            updateKey("population.initialization.biomass.sp" + iSpec, "population.seeding.biomass.sp" + iSpec);
        }

        // Deprecated population.initialization.method
        deprecateParameter("population.initialization.method");

        // As we deprecate parameter 'population.initialization.method' make sure
        // that 'population.initialization.file' does not interfere accidentaly 
        // in the initialisation process.
        if (!getConfiguration().isNull("population.initialization.method")) {
            if (getConfiguration().getString("population.initialization.method").equalsIgnoreCase("biomass")
                    && (!getConfiguration().isNull("population.initialization.file"))) {
                commentParameter("population.initialization.file", "Commented in order to avoid any interference with the new default seeding process");
            }
        }

        // Deprecated the spectrum initialisation method
        deprecateParameter("population.initialization.spectrum.intercept");
        deprecateParameter("population.initialization.spectrum.range");
        deprecateParameter("population.initialization.spectrum.slope");

        // Deprecated flux.incoming.season.file
        deprecateParameter("flux.incoming.season.file");

        for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
            // Deprecated flux.incoming.season.file.sp#
            deprecateParameter("flux.incoming.season.file.sp" + iSpec);
            // Deprecated flux.incoming.annual.biomass.sp#
            deprecateParameter("flux.incoming.biomass.sp" + iSpec);
            // Deprecated flux.incoming.size.sp#
            deprecateParameter("flux.incoming.size.sp" + iSpec);
            // Deprecated flux.incoming.age.sp#
            deprecateParameter("flux.incoming.age.sp" + iSpec);
            if (!getConfiguration().isNull("flux.incoming.biomass.sp" + iSpec) && getConfiguration().getFloat("flux.incoming.biomass.sp" + iSpec) > 0.f) {
                // Added parameter flux.incoming.byDt.byAge.file.sp#
                addParameter("flux.incoming.byDt.byAge.file.sp" + iSpec, "null");
                // Added parameter flux.incoming.byDt.bySize.file.sp#
                addParameter("flux.incoming.byDt.bySize.file.sp" + iSpec, "null");
                // Warning because the incoming flux cannot be updated automatically
                StringBuilder msg = new StringBuilder();
                msg.append("Parameters for configuring the flux of ");
                msg.append(getConfiguration().getString("species.name.sp" + iSpec));
                msg.append(" biomass have changed. ");
                msg.append("You must provide time series of incoming biomass by time step, structured in age or size class, in a CSV file.\n");
                msg.append("Osmose cannot generate automatically this CSV file from the deprecated 'flux.incoming.*' parameters. Please create it manually and update the pathname of parameters flux.incoming.byDt.byAge/bySize.file.sp");
                msg.append(iSpec);
                msg.append("\nMore information is available on the Osmose website http://www.osmose-model.org/documentation/changes-osmose-3-update-2");
                warning(msg.toString());
            }
        }

        // Deprecate reproduction.season.file
        deprecateParameter("reproduction.season.file");

        // Replace previous reproduction seasonality by individual files
        if (getConfiguration().canFind("reproduction.season.file")) {
            if (getConfiguration().isNull("reproduction.season.file")) {
                for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                    addParameter("reproduction.season.file.sp" + iSpec, "null");
                }
            } else {
                // Split CSV file 'flux.incoming.season.file' into several CSV files
                String absFilename = getConfiguration().getFile("reproduction.season.file");
                String absNewname = absFilename.substring(0, absFilename.lastIndexOf(".")) + "-sp";
                String relFilename = getConfiguration().getString("reproduction.season.file");
                String relNewname = relFilename.substring(0, relFilename.lastIndexOf(".")) + "-sp";
                try {
                    // Read the 'flux.incoming.season.file' CSV
                    CSVReader reader = new CSVReader(new FileReader(absFilename), Separator.guess(absFilename).getSeparator());
                    List<String[]> lines = reader.readAll();
                    reader.close();
                    for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                        String csvfile = absNewname + String.valueOf(iSpec) + ".csv";
                        try {
                            CSVWriter writer = new CSVWriter(new FileWriter(csvfile), ';');
                            for (String[] line : lines) {
                                String[] newline = new String[]{line[0], line[iSpec + 1]};
                                writer.writeNext(newline);
                            }
                            writer.close();
                            info("  Created reproduction seasonality file for species " + iSpec + " " + csvfile);
                        } catch (IOException ex) {
                            error("Error writing CSV file " + csvfile, ex);
                        }
                        String filename = relNewname + String.valueOf(iSpec) + ".csv";
                        addParameter("reproduction.season.file.sp" + iSpec, filename);
                    }
                } catch (IOException ex) {
                    error("Error reading CSV file " + absFilename, ex);
                }
            }
        }
    }

}
