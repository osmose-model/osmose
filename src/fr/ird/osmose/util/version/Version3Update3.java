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

        for (int iSpec = 0; iSpec < nMapMax; iSpec++) {
            updateKey(prefix + ".map" + iSpec + ".species", prefix + ".species.map" + iSpec);
            updateKey(prefix + ".map" + iSpec + ".age.min", prefix + ".age.min.map" + iSpec);
            updateKey(prefix + ".map" + iSpec + ".age.max", prefix + ".age.max.map" + iSpec);
            updateKey(prefix + ".map" + iSpec + ".season", prefix + ".season.map" + iSpec);
            updateKey(prefix + ".map" + iSpec + ".year.min", prefix + ".year.min.map" + iSpec);
            updateKey(prefix + ".map" + iSpec + ".year.max", prefix + ".year.max.map" + iSpec);
            updateKey(prefix + ".map" + iSpec + ".file", prefix + ".file.map" + iSpec);
        }

    }

}
