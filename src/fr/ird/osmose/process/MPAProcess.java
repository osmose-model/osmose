/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.Cell;
import fr.ird.osmose.GridMap;

/**
 *
 * @author pverley
 */
public class MPAProcess extends AbstractProcess {
    
    private GridMap mpa;
    private int start, end;
    private boolean active; 

    @Override
    public void init() {
        mpa = new GridMap();
        for (int index = 0; index < getOsmose().tabMPAiMatrix.length; index++) {
            mpa.setValue(getOsmose().tabMPAiMatrix[index], getOsmose().tabMPAjMatrix[index], 1);
        }
        start = getOsmose().MPAtStartTab;
        end = getOsmose().MPAtEndTab;
    }

    @Override
    public void run() {
        int year = getSimulation().getYear();
        active = (year >= start) && (year <= end);
    }
    
    public boolean isMPA(Cell cell) {
        return active && (mpa.getValue(cell) > 0);
    }
    
}
