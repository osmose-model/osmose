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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import fr.ird.osmose.util.YearParameters;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestYearsParam {

    @Test
    public void TestParam1() {

        int[] expected = new int[120];

        for (int i = 0; i < 120; i++) {
            expected[i] = i;
        }

        YearParameters param1 = new YearParameters("year.params.test1", "tst1");
        param1.init();

        assertArrayEquals(expected, param1.getYears());

    }
    
    @Test
    public void TestParam2() {

        int[] expected = new int[] { 5, 7, 50, 78, 119 };

        YearParameters param2 = new YearParameters("years.params.test2", "tst2");
        param2.init();

        assertArrayEquals(expected, param2.getYears());

    }

    @Test
    public void TestParam3() {

        int[] expected = new int[] { 0, 7, 50, 78 };

        YearParameters param3 = new YearParameters("years.params.test3", "tst3");
        param3.init();

        assertArrayEquals(expected, param3.getYears());

    }
    
    @Test
    public void TestParam4() {

        int N = 73 - 54 + 1;
        int[] expected = new int[N];

        int cpt = 0;
        for (int i = 54; i <= 73; i++) {
            expected[cpt++] = i;
        }

        YearParameters param4 = new YearParameters("years.params.test4", "tst4");
        param4.init();

        assertArrayEquals(expected, param4.getYears());

    }
    
    @Test
    public void TestParam5() {

        int N = 119 - 79 + 1;
        int[] expected = new int[N];

        int cpt = 0;
        for (int i = 79; i <= 119; i++) {
            expected[cpt++] = i;
        }

        YearParameters param5 = new YearParameters("years.params.test5", "tst5");
        param5.init();

        assertArrayEquals(expected, param5.getYears());

    }
    
    
    
    @BeforeAll
    public void prepareData() throws Exception {

        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        
        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        
        //test1: no parameters provided, returns all the years in the simulation
        
        // setting for test2
        // years provided as an array of years
        cmd.put("years.params.test2.years.tst2", "5;7;50;78;119");
        
        // settings for test3
        //years provided as an array of years but last two years must me discarded
        cmd.put("years.params.test3.years.tst3", "0;7;50;78;120;125");
        
        // settings for test4
        // years provided as first and last (inclusive) years
        cmd.put("years.params.test4.initialyear.tst4", "54");
        cmd.put("years.params.test4.lastyear.tst4", "73");
        
        // settings for test5 
        // same as test4 but last years must be discared
        cmd.put("years.params.test5.initialyear.tst5", "79");
        cmd.put("years.params.test5.lastyear.tst5", "125");
        
        osmose.readConfiguration(configurationFile, cmd);
        osmose.getConfiguration().init();

    }

}
