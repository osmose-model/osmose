/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.util.GridMap;
import fr.ird.osmose.School;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class MPAProcess extends AbstractProcess {

    private List<Cell> mpa;
    private int start, end;

    public MPAProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        mpa = new ArrayList();
        if (getConfiguration().canFind("mpa.file.mpa0")) {
            loadMPA(getConfiguration().resolveFile(getConfiguration().getString("mpa.file.mpa0")));
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
                for (School school : getPopulation().getSchools(cell)) {
                    school.notCatchable();
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

            int ny = lines.size();
            for (int j = ny; j-- > 0;) {
                String[] line = lines.get(j);
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val > 0.f) {
                        mpa.add(getGrid().getCell(i, j));
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Osmose.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
