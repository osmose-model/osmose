
/*
 *OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 *http://www.osmose-model.org
 *
 *Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-today
 *
 *Osmose is a computer program whose purpose is to simulate fish
 *populations and their interactions with their biotic and abiotic environment.
 *OSMOSE is a spatial, multispecies and individual-based model which assumes
 *size-based opportunistic predation based on spatio-temporal co-occurrence
 *and size adequacy between a predator and its prey. It represents fish
 *individuals grouped into schools, which are characterized by their size,
 *weight, age, taxonomy and geographical location, and which undergo major
 *processes of fish life cycle (growth, explicit predation, additional and
 *starvation mortalities, reproduction and migration) and fishing mortalities
 *(Shin and Cury 2001, 2004).
 *
 *Contributor(s):
 *Yunne SHIN (yunne.shin@ird.fr),
 *Morgane TRAVERS (morgane.travers@ifremer.fr)
 *Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 *Philippe VERLEY (philippe.verley@ird.fr)
 *Laure VELEZ (laure.velez@ird.fr)
 *Nicolas Barrier (nicolas.barrier@ird.fr)
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation (version 3 of the License). Full description
 *is provided on the LICENSE file.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package fr.ird.osmose;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import fr.ird.osmose.output.OutputManager;
import ucar.ma2.InvalidRangeException;
/**
 * Class for testing some basic parameters (number of species, number of
 * longitudes, latitudes, time-steps, etc.)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class TestWritting {

    private static Configuration cfg;
    private OutputManager outputManager;

    HashMap<String, String> cmd = new HashMap<>();

    @BeforeAll
    public void prepareData() {

        this.setCMD();

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }

        Osmose.getInstance().initSimulation();
        Osmose.getInstance().getSimulation(0).init();

        outputManager = new OutputManager(0);

    }


    /** Test that all the outputs can be written */
    @Test
    @Order(1)
    public void testInit() {
        outputManager.init();
    }

    @Test
    @Order(2)
    public void testRun() {
        Osmose.getInstance().getSimulation(0).run();
    }

    private void setCMD() {

        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory(null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String bool = "true";

        cmd.put("output.dir.path", tmpDir.toString());
        cmd.put("output.start.year", "0");
        cmd.put("output.restart.recordfrequency.ndt", "1");
        cmd.put("output.recordfrequency.ndt", "1");
        cmd.put("simulation.nsimulation", "1");
        cmd.put("simulation.time.nyear", "1");

        cmd.put("economy.enabled", "true");
        cmd.put("economic.output.stage.structure", "weight");

        cmd.put("output.biomass.enabled", "true");
        cmd.put("output.abundance.enabled", "true");

        cmd.put("output.abundance.netcdf.enabled", bool);
        cmd.put("output.biomass.netcdf.enabled", bool);
        cmd.put("output.yield.biomass.netcdf.enabled", bool);
        cmd.put("output.yield.abundance.netcdf.enabled", bool);
        cmd.put("output.biomass.bysize.netcdf.enabled", bool);
        cmd.put("output.biomass.bytl.netcdf.enabled", bool);
        cmd.put("output.biomass.byage.netcdf.enabled", bool);
        cmd.put("output.abundance.bysize.netcdf.enabled", bool);
        cmd.put("output.abundance.byage.netcdf.enabled", bool);
        cmd.put("output.diet.pressure.netcdf.enabled", bool);
        cmd.put("output.diet.composition.netcdf.enabled", bool);
        cmd.put("output.mortality.perSpecies.byage.netcdf.enabled", bool);
        cmd.put("output.diet.composition.byage.netcdf.enabled", bool);
        cmd.put("output.diet.composition.bysize.netcdf.enabled", bool);
        cmd.put("output.yield.biomass.bySize.netcdf.enabled", bool);
        cmd.put("output.yield.biomass.byage.netcdf.enabled", bool);
        cmd.put("output.yield.abundance.bySize.netcdf.enabled", bool);
        cmd.put("output.yield.abundance.byage.netcdf.enabled", bool);
        cmd.put("output.meanSize.byAge.netcdf.enabled", bool);
        cmd.put("output.diet.pressure.netcdf.enabled", bool);
        cmd.put("output.spatial.abundance.enabled", bool);
        cmd.put("output.spatial.biomass.enabled", bool);
        cmd.put("output.spatial.size.enabled", bool);
        cmd.put("output.spatial.enet.enabled", bool);
        cmd.put("output.spatial.enet.larvae.enabled", bool);
        cmd.put("output.spatial.enet.juv.enabled", bool);
        cmd.put("output.spatial.mstarv.enabled", bool);
        cmd.put("output.spatial.mpred.enabled", bool);
        cmd.put("output.spatial.dg.enabled", bool);
        cmd.put("output.spatial.egg.enabled", bool);
        cmd.put("output.spatial.yield.biomass.enabled", bool);
        cmd.put("output.spatial.yield.abundance.enabled", bool);
        cmd.put("output.spatialtl.enabled", bool);
        cmd.put("output.spatialsizespecies.enabled", bool);
        cmd.put("output.spatialagespecies.enabled", bool);
        cmd.put("output.fishing.accessible.biomass", bool);
        cmd.put("output.fishing.harvested.biomass", bool);
        cmd.put("output.fishery.enabled", bool);
        cmd.put("output.fishery.byage.enabled", bool);
        cmd.put("output.fishery.bysize.enabled", bool);
        cmd.put("output.biomass.bysize.enabled", bool);
        cmd.put("output.biomass.byage.enabled", bool);
        cmd.put("output.abundance.age1.enabled", bool);
        cmd.put("output.meanWeight.byAge.enabled", bool);
        cmd.put("output.meanWeight.bySize.enabled", bool);
        cmd.put("output.meanWeight.byWeight.enabled", bool);
        cmd.put("output.abundance.bysize.enabled", bool);
        cmd.put("output.abundance.byweight.enabled", bool);
        cmd.put("output.biomass.byweight.enabled", bool);
        cmd.put("output.abundance.byage.enabled", bool);
        cmd.put("output.abundance.bytl.enabled", bool);
        cmd.put("output.mortality.enabled", bool);
        cmd.put("output.mortality.perSpecies.byage.enabled", bool);
        cmd.put("output.mortality.perSpecies.bysize.enabled", bool);
        cmd.put("output.mortality.additional.bySize.enabled", bool);
        cmd.put("output.mortality.additional.byAge.enabled", bool);
        cmd.put("output.mortality.additional.byAge.enabled", bool);
        cmd.put("output.mortality.additionalN.bySize.enabled", bool);
        cmd.put("output.mortality.additionalN.byAge.enabled", bool);
        cmd.put("output.yield.biomass.enabled", bool);
        cmd.put("output.yield.abundance.enabled", bool);
        cmd.put("output.size.enabled", bool);
        cmd.put("output.weight.enabled", bool);
        cmd.put("output.size.catch.enabled", bool);
        cmd.put("output.yield.abundance.bySize.enabled", bool);
        cmd.put("output.yield.biomass.bySize.enabled", bool);
        cmd.put("output.yield.abundance.byWeight.enabled", bool);
        cmd.put("output.yield.biomass.byWeight.enabled", bool);
        cmd.put("output.meanSize.byAge.enabled", bool);
        cmd.put("output.yield.abundance.byAge.enabled", bool);
        cmd.put("output.yield.biomass.byAge.enabled", bool);
        cmd.put("output.tl.enabled", bool);
        cmd.put("output.tl.catch.enabled", bool);
        cmd.put("output.biomass.bytl.enabled", bool);
        cmd.put("output.meanTL.bySize.enabled", bool);
        cmd.put("output.meanTL.byAge.enabled", bool);
        cmd.put("output.diet.composition.enabled", bool);
        cmd.put("output.diet.composition.byage.enabled", bool);
        cmd.put("output.diet.composition.bysize.enabled", bool);
        cmd.put("output.diet.pressure.enabled", bool);
        cmd.put("output.diet.pressure.enabled", bool);
        cmd.put("output.diet.pressure.byage.enabled", bool);
        cmd.put("output.diet.pressure.bysize.enabled", bool);
        cmd.put("output.diet.success.enabled", bool);
        cmd.put("output.age.at.death.enabled", bool);
        cmd.put("output.spatial.enabled", bool);
        cmd.put("output.spatial.ltl.enabled", bool);
        cmd.put("output.ssb.enabled", bool);
        cmd.put("output.nschool.enabled", bool);
        cmd.put("output.nschool.byage.enabled", bool);
        cmd.put("output.nschool.bysize.enabled", bool);
        cmd.put("output.ndeadschool.enabled", bool);
        cmd.put("output.ndeadschool.byage.enabled", bool);
        cmd.put("output.ndeadschool.bysize.enabled", bool);
        cmd.put("output.individual.enabled", bool);
        cmd.put("output.fecondity.bysize.enabled", bool);
        cmd.put("output.fecondity.byage.enabled", bool);
        cmd.put("output.bioen.mature.age.enabled", bool);
        cmd.put("output.bioen.ingest.enabled", bool);
        cmd.put("output.bioen.ingest.tot.enabled", bool);
        cmd.put("output.bioen.maint.enabled", bool);
        cmd.put("output.bioen.enet.enabled", bool);
        cmd.put("output.bioen.sizeInf.enabled", bool);
        cmd.put("output.bioen.kappa.enabled", bool);
        cmd.put("output.ingest.byAge.enabled", bool);
        cmd.put("output.ingest.bySize.enabled", bool);
        cmd.put("output.kappa.byAge.enabled", bool);
        cmd.put("output.kappa.bySize.enabled", bool);
        cmd.put("output.enet.byAge.enabled", bool);
        cmd.put("output.enet.bySize.enabled", bool);
        cmd.put("output.maintenance.byAge.enabled", bool);
        cmd.put("output.maintenance.bySize.enabled", bool);
        cmd.put("output.meanSomaticWeight.byAge.enabled", bool);
        cmd.put("output.meanGonadWeight.byAge.enabled", bool);
        cmd.put("output.evolvingtraits.enabled", bool);
        cmd.put("output.meanTraits.newborn.enabled", bool);
        cmd.put("output.meanTraits.parents.enabled", bool);

    }



}