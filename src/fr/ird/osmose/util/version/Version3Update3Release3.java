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
public class Version3Update3Release3 extends AbstractVersion {

    public Version3Update3Release3() {
        super(3, 3, 2, 2018, Calendar.NOVEMBER, 28);
    }

    @Override
    void updateParameters() {
        // Nothing to be done for this version
        this.updateKey("grid.ncolumn", "grid.nlon");
        this.updateKey("grid.nline", "grid.nlat");

    }
}