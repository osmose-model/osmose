/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.Cell;
import fr.ird.osmose.GridMap;
import fr.ird.osmose.School;
import java.util.ArrayList;
import java.util.List;

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
        GridMap mpaMap = new GridMap();
        for (int index = 0; index < getOsmose().tabMPAiMatrix.length; index++) {
            mpaMap.setValue(getOsmose().tabMPAiMatrix[index], getOsmose().tabMPAjMatrix[index], 1);
        }
        mpa = new ArrayList();
        for (Cell cell : getGrid().getCells()) {
            if (mpaMap.getValue(cell) > 0) {
                mpa.add(cell);
            }
        }
        start = getOsmose().MPAtStartTab;
        end = getOsmose().MPAtEndTab;
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
}
