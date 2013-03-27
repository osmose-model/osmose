/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class MPAProcess extends AbstractProcess {

    private List<Cell> mpa;
    private int start, end;

    public MPAProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        mpa = new ArrayList();
        if (getConfiguration().canFind("mpa.file.mpa0")) {
            loadMPA(getConfiguration().getFile("mpa.file.mpa0"));
            start = getConfiguration().getInt("mpa.start.year.mpa0");
            end = getConfiguration().getInt("mpa.end.year.mpa0");
        } else {
            start = Integer.MAX_VALUE;
        }
    }

    @Override
    public void run() {
        int year = getSimulation().getYear();
        boolean active = (year >= start) && (year <= end);
        if (active) {
            for (Cell cell : mpa) {
                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        school.notCatchable();
                    }
                }
            }
        }
    }

    public boolean isMPA(Cell cell) {
        return mpa.contains(cell);
    }

    private void loadMPA(String csvFile) {
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();

            int ny = getGrid().get_ny();
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val > 0.f) {
                        mpa.add(getGrid().getCell(i, j));
                    }
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error loading MPA " + csvFile, ex);
        }
    }
}
