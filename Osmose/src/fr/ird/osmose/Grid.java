package fr.ird.osmose;


/********************************************************************************
 * <p>Title : Grid class</p>
 *
 * <p>Description : grid of Osmose model, divided into cells (Cell) 
 * Include a function defining neighbors of each cell </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1 
 ******************************************************************************** 
 */
import java.util.*;

class Grid {

    Osmose osmose;
    Cell[][] matrix;
    int nbLines, nbColumns;
    float latMax, latMin, longMax, longMin;
    float dLat, dLong;

    public Grid(Osmose osmose, int nbl, int nbc, float upleftLat, float lowrightLat, float upleftLong, float lowrightLong) {
        this.osmose = osmose;
        this.nbLines = nbl;
        this.nbColumns = nbc;

        this.latMax = upleftLat;
        this.latMin = lowrightLat;
        this.longMax = lowrightLong;
        this.longMin = upleftLong;

        dLat = (latMax - latMin) / (float) nbLines;
        dLong = (longMax - longMin) / (float) nbColumns;
        matrix = new Cell[nbLines][nbColumns];

        // create the grid and specify latitude and longitude of each cell
        float latitude, longitude;
        for (int i = 0; i < nbLines; i++) {
            latitude = latMax - (float) (i + 0.5f) * dLat;
            for (int j = 0; j < nbColumns; j++) {
                longitude = longMin + (float) (j + 0.5) * dLong;
                matrix[i][j] = new Cell(i, j, latitude, longitude);
            }
        }

        identifyNeighbors(nbLines);
        identifySpatialGroups();
    }

    public void identifySpatialGroups() {
        for (int i = 0; i < nbLines; i++) {
            for (int j = 0; j < nbColumns; j++) {
                if (matrix[i][j].getLat() >= (matrix[i][j].getLon() - 52.5)) {
                    matrix[i][j].setSpatialGroup(Cell.SpatialGroup.UPWELLING);
                } else {
                    matrix[i][j].setSpatialGroup(Cell.SpatialGroup.AGULHAS_BANK);
                }
            }
        }
    }

    public void identifyNeighbors(int nbl) {
        //adjacent cells are sorted in random order for non bias when species.iniRepartitionAleat
        if (nbl == 1) {
            matrix[0][0].neighbors = new Cell[1];
            matrix[0][0].neighbors[0] = matrix[0][0];
        } else if (nbl == 2) {
            int[] tabIndexRandom = new int[3];
            Vector vectInd = new Vector();
            int rand;

            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[0][0].neighbors = new Cell[3];
            matrix[0][0].neighbors[tabIndexRandom[0]] = matrix[0][1];
            matrix[0][0].neighbors[tabIndexRandom[1]] = matrix[1][0];
            matrix[0][0].neighbors[tabIndexRandom[2]] = matrix[1][1];

            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[0][1].neighbors = new Cell[3];
            matrix[0][1].neighbors[tabIndexRandom[0]] = matrix[0][0];
            matrix[0][1].neighbors[tabIndexRandom[1]] = matrix[1][0];
            matrix[0][1].neighbors[tabIndexRandom[2]] = matrix[1][1];

            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[1][0].neighbors = new Cell[3];
            matrix[1][0].neighbors[tabIndexRandom[0]] = matrix[0][0];
            matrix[1][0].neighbors[tabIndexRandom[1]] = matrix[0][1];
            matrix[1][0].neighbors[tabIndexRandom[2]] = matrix[1][1];

            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[1][1].neighbors = new Cell[3];
            matrix[1][1].neighbors[tabIndexRandom[0]] = matrix[0][1];
            matrix[1][1].neighbors[tabIndexRandom[1]] = matrix[1][0];
            matrix[1][1].neighbors[tabIndexRandom[2]] = matrix[0][0];
        } else if (nbl > 2) {
            int[] tabIndexRandom;
            Vector vectInd = new Vector();
            int rand;

            for (int i = 1; i < nbLines - 1; i++) {
                for (int j = 1; j < nbColumns - 1; j++) {
                    tabIndexRandom = new int[8];
                    for (int ind = 0; ind < tabIndexRandom.length; ind++) {
                        vectInd.addElement(new Integer(ind));
                    }
                    for (int ind = 0; ind < tabIndexRandom.length; ind++) {
                        rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                        tabIndexRandom[ind] = ((Integer) vectInd.elementAt(rand)).intValue();
                        vectInd.removeElementAt(rand);
                    }
                    matrix[i][j].neighbors = new Cell[8];
                    matrix[i][j].neighbors[tabIndexRandom[0]] = matrix[i - 1][j - 1];
                    matrix[i][j].neighbors[tabIndexRandom[1]] = matrix[i - 1][j];
                    matrix[i][j].neighbors[tabIndexRandom[2]] = matrix[i - 1][j + 1];
                    matrix[i][j].neighbors[tabIndexRandom[3]] = matrix[i][j - 1];
                    matrix[i][j].neighbors[tabIndexRandom[4]] = matrix[i][j + 1];
                    matrix[i][j].neighbors[tabIndexRandom[5]] = matrix[i + 1][j - 1];
                    matrix[i][j].neighbors[tabIndexRandom[6]] = matrix[i + 1][j];
                    matrix[i][j].neighbors[tabIndexRandom[7]] = matrix[i + 1][j + 1];
                }
            }

            for (int j = 1; j < nbColumns - 1; j++) {
                tabIndexRandom = new int[5];
                for (int i = 0; i < tabIndexRandom.length; i++) {
                    vectInd.addElement(new Integer(i));
                }
                for (int i = 0; i < tabIndexRandom.length; i++) {
                    rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                    tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                    vectInd.removeElementAt(rand);
                }
                matrix[0][j].neighbors = new Cell[5];
                matrix[0][j].neighbors[tabIndexRandom[0]] = matrix[0][j - 1];
                matrix[0][j].neighbors[tabIndexRandom[1]] = matrix[0][j + 1];
                matrix[0][j].neighbors[tabIndexRandom[2]] = matrix[1][j - 1];
                matrix[0][j].neighbors[tabIndexRandom[3]] = matrix[1][j];
                matrix[0][j].neighbors[tabIndexRandom[4]] = matrix[1][j + 1];
            }

            for (int j = 1; j < nbColumns - 1; j++) {
                tabIndexRandom = new int[5];
                for (int i = 0; i < tabIndexRandom.length; i++) {
                    vectInd.addElement(new Integer(i));
                }
                for (int i = 0; i < tabIndexRandom.length; i++) {
                    rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                    tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                    vectInd.removeElementAt(rand);
                }
                matrix[nbLines - 1][j].neighbors = new Cell[5];
                matrix[nbLines - 1][j].neighbors[tabIndexRandom[0]] = matrix[nbLines - 2][j - 1];
                matrix[nbLines - 1][j].neighbors[tabIndexRandom[1]] = matrix[nbLines - 2][j];
                matrix[nbLines - 1][j].neighbors[tabIndexRandom[2]] = matrix[nbLines - 2][j + 1];
                matrix[nbLines - 1][j].neighbors[tabIndexRandom[3]] = matrix[nbLines - 1][j - 1];
                matrix[nbLines - 1][j].neighbors[tabIndexRandom[4]] = matrix[nbLines - 1][j + 1];
            }

            for (int i = 1; i < nbLines - 1; i++) {
                tabIndexRandom = new int[5];
                for (int ind = 0; ind < tabIndexRandom.length; ind++) {
                    vectInd.addElement(new Integer(ind));
                }
                for (int ind = 0; ind < tabIndexRandom.length; ind++) {
                    rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                    tabIndexRandom[ind] = ((Integer) vectInd.elementAt(rand)).intValue();
                    vectInd.removeElementAt(rand);
                }
                matrix[i][0].neighbors = new Cell[5];
                matrix[i][0].neighbors[tabIndexRandom[0]] = matrix[i - 1][0];
                matrix[i][0].neighbors[tabIndexRandom[1]] = matrix[i - 1][1];
                matrix[i][0].neighbors[tabIndexRandom[2]] = matrix[i][1];
                matrix[i][0].neighbors[tabIndexRandom[3]] = matrix[i + 1][0];
                matrix[i][0].neighbors[tabIndexRandom[4]] = matrix[i + 1][1];
            }
            for (int i = 1; i < nbLines - 1; i++) {
                tabIndexRandom = new int[5];
                for (int ind = 0; ind < tabIndexRandom.length; ind++) {
                    vectInd.addElement(new Integer(ind));
                }
                for (int ind = 0; ind < tabIndexRandom.length; ind++) {
                    rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                    tabIndexRandom[ind] = ((Integer) vectInd.elementAt(rand)).intValue();
                    vectInd.removeElementAt(rand);
                }
                matrix[i][nbColumns - 1].neighbors = new Cell[5];
                matrix[i][nbColumns - 1].neighbors[tabIndexRandom[0]] = matrix[i - 1][nbColumns - 2];
                matrix[i][nbColumns - 1].neighbors[tabIndexRandom[1]] = matrix[i - 1][nbColumns - 1];
                matrix[i][nbColumns - 1].neighbors[tabIndexRandom[2]] = matrix[i][nbColumns - 2];
                matrix[i][nbColumns - 1].neighbors[tabIndexRandom[3]] = matrix[i + 1][nbColumns - 2];
                matrix[i][nbColumns - 1].neighbors[tabIndexRandom[4]] = matrix[i + 1][nbColumns - 1];
            }
            tabIndexRandom = new int[3];
            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[0][0].neighbors = new Cell[3];
            matrix[0][0].neighbors[tabIndexRandom[0]] = matrix[0][1];
            matrix[0][0].neighbors[tabIndexRandom[1]] = matrix[1][0];
            matrix[0][0].neighbors[tabIndexRandom[2]] = matrix[1][1];

            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[0][nbColumns - 1].neighbors = new Cell[3];
            matrix[0][nbColumns - 1].neighbors[tabIndexRandom[0]] = matrix[0][nbColumns - 2];
            matrix[0][nbColumns - 1].neighbors[tabIndexRandom[1]] = matrix[1][nbColumns - 2];
            matrix[0][nbColumns - 1].neighbors[tabIndexRandom[2]] = matrix[1][nbColumns - 1];

            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[nbLines - 1][0].neighbors = new Cell[3];
            matrix[nbLines - 1][0].neighbors[tabIndexRandom[0]] = matrix[nbLines - 2][0];
            matrix[nbLines - 1][0].neighbors[tabIndexRandom[1]] = matrix[nbLines - 2][1];
            matrix[nbLines - 1][0].neighbors[tabIndexRandom[2]] = matrix[nbLines - 1][1];

            for (int i = 0; i < tabIndexRandom.length; i++) {
                vectInd.addElement(new Integer(i));
            }
            for (int i = 0; i < tabIndexRandom.length; i++) {
                rand = (int) Math.round(Math.random() * (vectInd.size() - 1));
                tabIndexRandom[i] = ((Integer) vectInd.elementAt(rand)).intValue();
                vectInd.removeElementAt(rand);
            }
            matrix[nbLines - 1][nbColumns - 1].neighbors = new Cell[3];
            matrix[nbLines - 1][nbColumns - 1].neighbors[tabIndexRandom[0]] = matrix[nbLines - 2][nbColumns - 2];
            matrix[nbLines - 1][nbColumns - 1].neighbors[tabIndexRandom[1]] = matrix[nbLines - 2][nbColumns - 1];
            matrix[nbLines - 1][nbColumns - 1].neighbors[tabIndexRandom[2]] = matrix[nbLines - 1][nbColumns - 2];
        }
    }
}

