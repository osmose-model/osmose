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
package fr.ird.osmose.util.version;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.Configuration;
import fr.ird.osmose.util.Separator;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author pverley
 */
public class Releases {

    final public static Release[] ALL = new Release[]{
        // 2014/06/01
        new Release("3.1") {
            @Override
            void updateParameters() {
                // Renamed simulation.restart.recordfrequency.ndt into output.restart.recordfrequency.ndt
                updateKey("simulation.restart.recordfrequency.ndt", "output.restart.recordfrequency.ndt");

                // Renamed simulation.restart.spinup into output.restart.spinup
                updateKey("simulation.restart.spinup", "output.restart.spinup");

                // Deleted output output.size.perSpecies.enabled
                deprecateParameter("output.size.perSpecies.enabled");

                // Deleted output output.size.spectrum.perSpecies.B.enabled
                deprecateParameter("output.size.spectrum.perSpecies.B.enabled");

                // Deleted output output.size.spectrum.perSpecies.N.enabled
                deprecateParameter("output.size.spectrum.perSpecies.N.enabled");

                // Renamed output.size.spectrum.enabled into output.abundance.bySize.enabled
                updateKey("output.size.spectrum.enabled", "output.abundance.bySize.enabled");

                // Renamed output.size.spectrum.size.min into output.distrib.bySize.min
                updateKey("output.size.spectrum.size.min", "output.distrib.bySize.min");

                // Renamed output.size.spectrum.size.max into output.distrib.bySize.max
                updateKey("output.size.spectrum.size.max", "output.distrib.bySize.max");

                // Renamed output.size.spectrum.size.range into output.distrib.bySize.incr
                updateKey("output.size.spectrum.size.range", "output.distrib.bySize.incr");

                // Renamed output.TL.perAge.enabled into output.meanTL.byAge.enabled
                updateKey("output.TL.perAge.enabled", "output.meanTL.byAge.enabled");

                // Renamed output.TL.perSize.enabled into output.meanTL.bySize.enabled
                updateKey("output.TL.perSize.enabled", "output.meanTL.bySize.enabled");

                // Renamed output.TL.spectrum.enabled into output.biomass.byTL.enabled
                updateKey("output.TL.spectrum.enabled", "output.biomass.byTL.enabled");
            }
        },
        // 2015/03/01
        new Release("3.2") {
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
                        try ( CSVReader reader = new CSVReader(new FileReader(absFilename), Separator.guess(absFilename).getSeparator())) {
                            // Read the 'flux.incoming.season.file' CSV

                            List<String[]> lines = reader.readAll();
                            for (int iSpec = 0; iSpec < nSpecies; iSpec++) {
                                String csvfile = absNewname + String.valueOf(iSpec) + ".csv";
                                try ( CSVWriter writer = new CSVWriter(new FileWriter(csvfile), ';')) {
                                    for (String[] line : lines) {
                                        String[] newline = new String[]{line[0], line[iSpec + 1]};
                                        writer.writeNext(newline);
                                    }
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
        },
        // 2018/01/15
        new Release("3.3") {
            @Override
            void updateParameters() {

                // Count the old version map parameters
                String prefix = "movement";
                int nMapMax = getConfiguration().findKeys(prefix + ".map*.species").size();
                int cpt = 0;
                int value = 0;
                while (cpt < nMapMax) {
                    if (getConfiguration().canFind(prefix + ".map" + value + ".file")) {
                        updateKey(prefix + ".map" + value + ".species", prefix + ".species.map" + cpt);
                        updateKey(prefix + ".map" + value + ".age.min", prefix + ".age.min.map" + cpt);
                        updateKey(prefix + ".map" + value + ".age.max", prefix + ".age.max.map" + cpt);
                        updateKey(prefix + ".map" + value + ".season", prefix + ".season.map" + cpt);
                        updateKey(prefix + ".map" + value + ".year.min", prefix + ".year.min.map" + cpt);
                        updateKey(prefix + ".map" + value + ".year.max", prefix + ".year.max.map" + cpt);
                        updateKey(prefix + ".map" + value + ".file", prefix + ".file.map" + cpt);
                        cpt++;
                    }
                    value++;
                }
            }
        },
        // 2018/11/28
        new Release("3.3.3") {
            @Override
            void updateParameters() {

                this.updateKey("grid.ncolumn", "grid.nlon");
                this.updateKey("grid.nline", "grid.nlat");

            }
        },
        // 2019/09/19
        new Release("4.2.1") {
            @Override
            void updateParameters() {

                updateKey("output.yieldN.enabled", "output.yield.abundance.enabled");
                updateKey("output.yield.enabled", "output.yield.biomass.enabled");

            }
        },
        // 2019/10/11
        new Release("4.2.2") {
            @Override
            void updateParameters() {

                updateKey("simulation.use.bioen", "simulation.bioen.enabled");

                // got rid of iterative mortality process, so only one mortality algorithm left
                deprecateParameter("mortality.algorithm");
            }
        },
        // 2019/11/25
        new Release("4.2.3") {
            @Override
            void updateParameters() {

                // time length of the resource groups is defined in the NetCDF file
                // consistency of the tiem length can be done with value from NetCDF
                // file, no need for overparametrisation.
                deprecateParameter("ltl.nstep");

                // rename simulation.nplankton into simulation.nresource
                updateKey("simulation.nplankton", "simulation.nresource");

                // update every plankton.*.plk# parameter into resource.*.rsc#
                int nRsc = getConfiguration().findKeys("plankton.name.plk*").size();
                for (int index = 0; index < nRsc; index++) {
                    updateKey("plankton.name.plk" + index, "resource.name.rsc" + index);
                    updateKey("plankton.TL.plk" + index, "resource.TL.rsc" + index);
                    updateKey("plankton.size.min.plk" + index, "resource.size.min.rsc" + index);
                    updateKey("plankton.size.max.plk" + index, "resource.size.max.rsc" + index);
                    updateKey("plankton.accessibility2fish.file.plk" + index, "resource.accessibility2fish.file.rsc" + index);
                    updateKey("plankton.accessibility2fish.plk" + index, "resource.accessibility2fish.rsc" + index);
                    updateKey("plankton.biomass.total.plk" + index, "resource.biomass.total.rsc" + index);
                    updateKey("plankton.file.plk" + index, "resource.file.rsc" + index);
                    updateKey("plankton.multiplier.plk" + index, "resource.multiplier.rsc" + index);
                    deprecateParameter("plankton.conversion2tons.plk" + index);
                }

                // deprecate every other old parameters related to ltl management
                deprecateParameter("ltl.netcdf.grid.file");
                deprecateParameter("ltl.netcdf.var.lon");
                deprecateParameter("ltl.netcdf.var.lat");
                deprecateParameter("ltl.netcdf.var.bathy");
                deprecateParameter("ltl.netcdf.var.csr");
                deprecateParameter("ltl.netcdf.var.hc");
                deprecateParameter("ltl.integration.depth");
                deprecateParameter("ltl.netcdf.dim.ntime");
                deprecateParameter("ltl.netcdf.var.zlevel");
                deprecateParameter("ltl.netcdf.var.bathy");
                deprecateParameter("ltl.netcdf.grid.file");
                for (int index = 0; index < nRsc; index++) {
                    deprecateParameter("ltl.netcdf.var.plankton.plk" + index);
                }
                for (int t = 0; t < getConfiguration().findKeys("ltl.netcdf.file.t*").size(); t++) {
                    deprecateParameter("ltl.netcdf.file.t" + t);
                }

            }
        },
        // 2019/11/25
        new Release("4.2.4") {
            @Override
            void updateParameters() {
                deprecateParameter("ltl.java.classname");
            }
        },
        // 2019/11/26
        new Release("4.2.5") {
            @Override
            void updateParameters() {

                int nSpecies = getConfiguration().getInt("simulation.nspecies");

                // outputs
                updateKey("output.mortality.natural.bySize.enabled", "output.mortality.additional.bySize.enabled");
                updateKey("output.mortality.natural.byAge.enabled", "output.mortality.additional.byAge.enabled");
                updateKey("output.mortality.naturalN.bySize.enabled", "output.mortality.additionalN.bySize.enabled");
                updateKey("output.mortality.naturalN.byAge.enabled", "output.mortality.additionalN.byAge.enabled");

                // rename *mortality.natural* into *mortality.additional*
                for (int i = 0; i < nSpecies; i++) {
                    // time series
                    updateKey("mortality.natural.rate.sp" + i, "mortality.additional.rate.sp" + i);
                    updateKey("mortality.natural.rate.bytDt.file.sp" + i, "mortality.additional.rate.bytDt.file.sp" + i);
                    updateKey("mortality.natural.rate.byDt.byAge.file.sp" + i, "mortality.additional.rate.byDt.byAge.file.sp" + i);
                    updateKey("mortality.natural.rate.byDt.bySize.file.sp" + i, "mortality.additional.rate.byDt.bySize.file.sp" + i);
                    updateKey("mortality.natural.larva.rate.sp" + i, "mortality.additional.larva.rate.sp" + i);
                    updateKey("mortality.natural.larva.rate.bytDt.file.sp" + i, "mortality.additional.larva.rate.bytDt.file.sp" + i);
                    // spatial 
                    updateKey("mortality.natural.spatial.distrib.file.sp" + i, "mortality.additional.spatial.distrib.file.sp" + i);
                }
            }
        },
        new Release("4.2.6") {
            @Override
            void updateParameters() {

                Configuration cfg = this.getConfiguration();

                cfg.findKeys("movement.age.min.map*").stream().mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".map") + 4))).forEach(i -> updateKey("movement.age.min.map" + i, "movement.initialAge.map" + i));
                cfg.findKeys("movement.age.max.map*").stream().mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".map") + 4))).forEach(i -> updateKey("movement.age.max.map" + i, "movement.lastAge.map" + i));
                cfg.findKeys("movement.season.map*").stream().mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".map") + 4))).forEach(i -> updateKey("movement.season.map" + i, "movement.steps.map" + i));

            }
        },
        new Release("4.3.0") {
            @Override
            void updateParameters() {

                Configuration cfg = this.getConfiguration();
                cfg.findKeys("plankton.conversion2tons*").stream().forEach(key -> deprecateParameter(key));

                if (cfg.findKeys("predation.accessibility.stage.threshold*").size() > 0) {
                    String message = "The accessibility stage are now set directly from the accessibility matrix";
                    this.getLogger().info(message);
                    cfg.findKeys("predation.accessibility.stage.threshold*").stream().forEach(key -> deprecateParameter(key));
                }

                int nspecies = cfg.getInt("simulation.nspecies");
                int nresources = cfg.getInt("simulation.nresource");

                int nbackground;
                if (cfg.canFind("simulation.nbackground")) {
                    nbackground = cfg.getInt("simulation.nbackground");
                } else {
                    nbackground = 0;
                }

                // Add the parameters relative to species types
                for (int i = 0; i < nspecies; i++) {
                    this.addParameter("species.type.sp" + i, "focal");
                }

                for (int i = 0; i < nbackground; i++) {
                    this.addParameter("species.type.sp" + (i + nspecies), "background");
                }

                for (int i = 0; i < nresources; i++) {
                    this.addParameter("species.type.sp" + (i + nspecies + nbackground), "resource");
                }

                for (int index = 0; index < nresources; index++) {
                    int newindex = index + nspecies + nbackground;
                    updateKey("resource.name.rsc" + index, "species.name.sp" + newindex);
                    updateKey("resource.TL.rsc" + index, "species.TL.sp" + newindex);
                    updateKey("resource.size.min.rsc" + index, "species.size.min.sp" + newindex);
                    updateKey("resource.size.max.rsc" + index, "species.size.max.sp" + newindex);
                    updateKey("resource.accessibility2fish.file.rsc" + index, "species.accessibility2fish.file.sp" + newindex);
                    updateKey("resource.accessibility2fish.rsc" + index, "species.accessibility2fish.sp" + newindex);
                    updateKey("resource.biomass.total.rsc" + index, "species.biomass.total.sp" + newindex);
                    updateKey("resource.file.rsc" + index, "species.file.sp" + newindex);
                    updateKey("resource.multiplier.rsc" + index, "species.multiplier.sp" + newindex);
                }

                this.deprecateParameter("grid.java.classname");

            } // end of update parameters
        }, // end of release constructor

        new Release("4.3.1") {
            @Override
            void updateParameters() {
            }
        },
        
        new Release("4.3.2") {
            @Override
            void updateParameters() {
            }
        }, 
        
        new Release("4.3.3") {
            @Override
            void updateParameters() {
                
                Configuration cfg = this.getConfiguration();
                
                for (int index : this.getFocalIndex()) {
                    updateKey("species.larva2adults.agethres.sp" + index, "species.first.feeding.age.sp" + index);
                }
                
                cfg.findKeys("fisheries.rate.bySeason.fsh*").stream().mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).forEach(i -> updateKey("fisheries.rate.bySeason" + i, "fisheries.rate.byPeriod.fsh" + i));

                // Update in parameters from Alaia 
                    for (int index : this.getFocalIndex()) {
                        updateKey("predation.ingestion.rate.max.larvae.bioen.sp" + index,
                                "predation.coef.ingestion.rate.max.larvae.bioen.sp" + index);
                    }

                    for (int index : this.getFocalIndex()) {
                        updateKey("bioen.maint.energy.csmr.sp" + index, "bioen.maint.energy.c_m.sp" + index);
                    }
                    
                    // Update for oxygen functions
                    for (int index : this.getFocalIndex()) {
                        updateKey("species.c1.sp" + index, "species.oxygen.c1.sp" + index);
                        updateKey("species.c2.sp" + index, "species.oxygen.c2.sp" + index);
                    }
                
                    // Deprecates old parameters for temperature function.
                    for (int index : this.getFocalIndex()) {
                        deprecateParameter("bioen.gross.energy.km.sp" + index);
                        deprecateParameter("bioen.gross.energy.gamma.sp" + index);
                        deprecateParameter("bioen.arrh.ct.sp" + index);
                        deprecateParameter("bioen.maint.energy.Tr.sp" + index);
                    }       
            }  // end of updatePara
        }  // end of Release(4.3.3)
        
    };  // end of ALL array
}  // end of class
