
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ucar.ma2.InvalidRangeException;
/**
 * Class for testing some basic parameters (number of species, number of
 * longitudes, latitudes, time-steps, etc.)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Test1 {
    
    private static Configuration cfg;

    @Test
    public void testNx() {
        assertEquals(45, cfg.getGrid().get_nx());
    }

    @Test
    public void testNy() {
        assertEquals(22, cfg.getGrid().get_ny());
    }

    @Test
    public void testNStepYears() {
        assertEquals(24, cfg.getNStepYear());
    }

    @Test
    public void testNSteps() {
        assertEquals(24 * 120, cfg.getNStep());
    }
    
    @Test
    public void testNYears() {
        assertEquals(120, cfg.getNYears());
    }
    
    @Test
    public void testNSpecies() {
        assertEquals(14, cfg.getNSpecies());
    }
    
    @Test
    public void testNResources() {
        assertEquals(10, cfg.getNRscSpecies());
    }

    public Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }
    
    @BeforeAll
    public void prepareData() {
    
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String dirIn = System.getenv("OSMOSE_TEST_DIR");
        String fileIn = System.getenv("OSMOSE_TEST_FILE");
        String configurationFile = new File(dirIn, fileIn).getAbsolutePath();
        osmose.readConfiguration(configurationFile);
        cfg = osmose.getConfiguration();
        try {
            cfg.init();
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
        }
    }
    


}