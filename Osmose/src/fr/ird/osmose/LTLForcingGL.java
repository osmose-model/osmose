/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D3;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author pverley
 */
public class LTLForcingGL extends Coupling {

    private String zlevelName;

    public LTLForcingGL() {
        filesFormat = "netcdf";
    }

    @Override
    public void readInputPlanktonFiles(String planktonFileName) {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(planktonFilesPath, planktonFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("LTL file " + planktonFileName + " doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(LTLFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            plktonNetcdfNames = new String[nbPlankton];
            for (int i = 0; i < nbPlankton; i++) {
                st.nextToken();
                plktonNetcdfNames[i] = st.sval;
            }

            planktonFileListNetcdf = new String[nbForcingDt];
            for (int step = 0; step < nbForcingDt; step++) {
                st.nextToken();
                planktonFileListNetcdf[step] = st.sval;
            }

            st.nextToken();
            zlevelName = st.sval;

        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            return;
        }
    }

    @Override
    public void initPlanktonMap() {

        NetcdfFile ncGrid = null;
        String gridFilename = getOsmose().inputPathName + getOsmose().fileSeparator + planktonFileListNetcdf[0];
        try {
            ncGrid = NetcdfDataset.openFile(gridFilename, null);
        } catch (IOException ex) {
            System.err.println("Failed to open plankton grid file " + gridFilename);
            ex.printStackTrace();
        }

        int[] shape = ncGrid.findVariable(zlevelName).getShape();
        planktonDimZ = shape[0];
        planktonDimY = shape[1];
        planktonDimX = shape[2];

        depthOfLayer = new float[planktonDimX][planktonDimY][planktonDimZ];
        saveMortality = new float[nbPlankton][planktonDimX][planktonDimY][planktonDimZ];

        try {
            ArrayDouble.D3 arrDepth = (D3) ncGrid.findVariable(zlevelName).read();
            for (int i = 0; i < planktonDimX; i++) {
                for (int j = 0; j < planktonDimY; j++) {
                    for (int z = 0; z < planktonDimZ; z++) {
                        depthOfLayer[i][j][z] = (float) arrDepth.get(z, j, i);
                    }
                }
            }
            ncGrid.close();

            for (int x = 0; x < planktonDimX; x++) {
                for (int y = 0; y < planktonDimY; y++) {
                    if (!getGrid().getCell(y, x).isLand()) {
                        getGrid().getCell(y, x).icoordLTLGrid.addElement(y);
                        getGrid().getCell(y, x).jcoordLTLGrid.addElement(x);
                    }
                }
            }

            planktonList = new Plankton[nbPlankton];

            // Initialisation plankton and table of data
            for (int i = 0; i < nbPlankton; i++) {
                planktonList[i] = new Plankton(this, planktonNames[i], minSize[i], maxSize[i], trophicLevel[i], conversionFactors[i], prodBiomFactors[i], getOsmose().planktonAccessCoeffMatrix[i]);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void runLTLModel() {
        throw new UnsupportedOperationException("Not available in forcing mode.");
    }

    @Override
    public void updatePlankton(int dt) {
        for (int i = 0; i < nbPlankton; i++) {
            planktonList[i].clearPlankton();      // put the biomass tables of plankton to 0
        }
        String nameTemp = getOsmose().inputPathName + planktonFileListNetcdf[dt];
        int timeIndex = 0;
        readNetCDFFile(nameTemp, timeIndex);
        mapInterpolation();
        if (getSimulation().getYear() >= getOsmose().timeSeriesStart) {
            saveForDiet();       // save biomass of plankton before predation
        }
    }

    @Override
    public void readNetCDFFile(String nameOfFile, int timeIndex) {

        //System.out.println("Reading " + nameOfFile);

        NetcdfFile nc = null;
        String name = nameOfFile;
        Variable[] tempVar;
        int[] shape;
        ArrayDouble.D3[] tempArray;
        tempArray = new ArrayDouble.D3[nbPlankton];
        tempVar = new Variable[nbPlankton];

        try {
            nc = NetcdfFile.open(name);
            // read data and put them in the local arrays
            for (int i = 0; i < nbPlankton; i++) {
                tempVar[i] = nc.findVariable(plktonNetcdfNames[i]);
                tempArray[i] = (ArrayDouble.D3) tempVar[i].read();
            }
            shape = tempVar[0].getShape();

            // fill dataInit of plankton classes from local arrays
            for (int i = 0; i < shape[2]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    for (int k = 0; k < shape[0]; k++) {
                        for (int p = 0; p < nbPlankton; p++) {
                            ((Plankton) planktonList[p]).dataInit[i][j][k] = (float) tempArray[p].get(k, j, i);    // carreful, index not in the same order
                        }
                    }
                }
            }
            // integrates vertically plankton biomass, using depth files
            for (int p = 0; p < nbPlankton; p++) {
                ((Plankton) planktonList[p]).verticalIntegration(depthOfLayer, integrationDepth);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    @Override
    public void calculPlanktonMortality() {
        throw new UnsupportedOperationException("Not available in forcing mode.");
    }

    // CASE SPECIFIC - depends of the LTL grid
    // from ECO3M (vertically integrated) towards OSMOSE
    @Override
    public void mapInterpolation() {

        int tempX, tempY;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    for (int k = 0; k < getGrid().getCell(i, j).getNbCellsLTLGrid(); k++) {
                        for (int p = 0; p < nbPlankton; p++) {
                            tempY = ((Integer) getGrid().getCell(i, j).icoordLTLGrid.elementAt(k)).intValue();
                            tempX = ((Integer) getGrid().getCell(i, j).jcoordLTLGrid.elementAt(k)).intValue();
                            // interpolate the plankton concentrations from the LTL cells
                            ((Plankton) planktonList[p]).addCell(i, j, tempX, tempY, getGrid().getCell(i, j).getNbCellsLTLGrid());
                        }
                    }
                }
            }
        }
    }
    
}
