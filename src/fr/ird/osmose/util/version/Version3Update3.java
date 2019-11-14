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
public class Version3Update3 extends AbstractVersion {

    public Version3Update3() {
        super(3, 3, 2018, Calendar.JANUARY, 15);
    }

    @Override
    void updateParameters() {

        // Count the old version map parameters
        
        String prefix;

        prefix = "movement";

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
}