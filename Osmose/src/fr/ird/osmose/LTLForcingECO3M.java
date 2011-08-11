/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D3;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class LTLForcingECO3M implements LTLForcing {

    private int nbPlankton, nbForcingDt;
    private String[] planktonFileListNetcdf;
    private String[] planktonNames;		// list of names of plankton groups
    private float[] trophicLevel;			// list of TL of plankton groups
    private float[] minSize, maxSize;		// list of min and max sizes of plankton groups
    private float[] conversionFactors;		// list of conversionFactors of plankton groups
    private float[] prodBiomFactors;		// list of prod/biom ratios of plankton groups
    private Plankton[] planktonList;     // list of plankton groups (here 4)
    private int nbDimensionsGrid;
    private int planktonDimX;      // dimension of LTL model, here ROMS Plume (144 * 65 * 20)
    private int planktonDimY;
    private int planktonDimZ;	// vertical dimension (20)
    private float[][][] depthOfLayer;       // table of height of layers of ROMS model used in vertical integration
    private float integrationDepth;   // latitude and longitude of each cell of the LTL grid, used for interpolation
    private String[] plktonNetcdfNames;
    private int numSerie;
    private String zlevelName;

    @Override
    public void readLTLConfigFile1(String planktonStructureFileName) // ******** read LTL basic file with name of plankton, sizes, format of files...
    {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getOsmose().inputPathName, planktonStructureFileName));
        } catch (FileNotFoundException ex) {
            System.out.println("LTL file " + planktonStructureFileName + " doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(LTLFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            nbPlankton = (new Integer(st.sval)).intValue();
            if (!(nbPlankton == getOsmose().nbPlanktonGroupsTab[numSerie])) {
                System.out.println("The number of plankton group in plankton structure file does not match the one from config file");
            }
            st.nextToken();
            nbForcingDt = (new Integer(st.sval)).intValue();
            if (!(nbForcingDt == getSimulation().getNbTimeStepsPerYear())) {
                System.out.println("In the current version, the time step of plankton biomass should match the time step of osmose config");
            }

            // initializing tables
            planktonNames = new String[nbPlankton];
            trophicLevel = new float[nbPlankton];
            minSize = new float[nbPlankton];
            maxSize = new float[nbPlankton];
            conversionFactors = new float[nbPlankton];
            prodBiomFactors = new float[nbPlankton];

            for (int i = 0; i < nbPlankton; i++) {
                // filling tables
                st.nextToken();
                planktonNames[i] = st.sval;
                getOsmose().planktonNamesTab[numSerie][i] = st.sval;
                st.nextToken();
                minSize[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                maxSize[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                trophicLevel[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                conversionFactors[i] = (new Float(st.sval)).floatValue();
                st.nextToken();
                prodBiomFactors[i] = (new Float(st.sval)).floatValue();
            }

            st.nextToken();
            nbDimensionsGrid = new Integer(st.sval).intValue();
            if ((nbDimensionsGrid > 3) || (nbDimensionsGrid < 2)) {
                System.out.println("The dimension " + nbDimensionsGrid + " cannot be consider - should be 2 or 3");
            }
            st.nextToken();
            planktonDimX = new Integer(st.sval).intValue();
            st.nextToken();
            planktonDimY = new Integer(st.sval).intValue();
            if (nbDimensionsGrid == 3) {
                st.nextToken();
                planktonDimZ = new Integer(st.sval).intValue();
                st.nextToken();
                integrationDepth = new Float(st.sval).floatValue();
            }
        } catch (IOException ex) {
            System.out.println("Reading error of LTL structure file");
            return;
        }
    }

    @Override
    public void readLTLConfigFile2(String planktonFileName) {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getOsmose().inputPathName, planktonFileName));
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
            ncGrid = NetcdfFile.open(gridFilename, null);
        } catch (IOException ex) {
            System.err.println("Failed to open plankton grid file " + gridFilename);
            ex.printStackTrace();
        }

        int[] shape = ncGrid.findVariable(zlevelName).getShape();
        planktonDimZ = shape[0];
        planktonDimY = shape[1];
        planktonDimX = shape[2];

        depthOfLayer = new float[planktonDimX][planktonDimY][planktonDimZ];

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

            int stride = getGrid().getStride();
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    for (int ii = 0; ii < stride; ii++) {
                        for (int jj = 0; jj < stride; jj++) {
                            getGrid().getCell(i, j).icoordLTLGrid.addElement(j * stride + jj);
                            getGrid().getCell(i, j).jcoordLTLGrid.addElement(i * stride + ii);
                        }
                    }
                }
            }

            planktonList = new Plankton[nbPlankton];

            // Initialisation plankton and table of data
            for (int i = 0; i < nbPlankton; i++) {
                planktonList[i] = new Plankton(planktonNames[i], minSize[i], maxSize[i], trophicLevel[i], conversionFactors[i], prodBiomFactors[i], getOsmose().planktonAccessCoeffMatrix[i]);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void updatePlankton(int dt) {
        for (int i = 0; i < nbPlankton; i++) {
            planktonList[i].clearPlankton();      // put the biomass tables of plankton to 0
        }
        String nameTemp = getOsmose().inputPathName + File.separator + planktonFileListNetcdf[dt];
        int timeIndex = 0;
        readNetCDFFile(nameTemp, timeIndex);
        mapInterpolation();
        if (getSimulation().getYear() >= getOsmose().timeSeriesStart) {
            saveForDiet();       // save biomass of plankton before predation
        }
    }

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
                            /*if (p == 0) {
                                System.out.println("osmose cell (" + i + ", " + j + ") contains ECO3M cell (" + tempX + ", " + tempY + ")");
                            }*/
                            // interpolate the plankton concentrations from the LTL cells
                            ((Plankton) planktonList[p]).addCell(i, j, tempX, tempY, getGrid().getCell(i, j).getNbCellsLTLGrid());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void saveForDiet() {

        for (int p = 0; p < nbPlankton; p++) {
            getSimulation().biomPerStage[getSimulation().getNbSpecies() + p][0] = 0; //biomPerStage[][0] because just 1 stage per plankton group
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    if (!getGrid().getCell(i, j).isLand()) {
                        getSimulation().biomPerStage[getSimulation().getNbSpecies() + p][0] += ((Plankton) planktonList[p]).biomass[i][j];
                    }
                }
            }
        }
    }

    @Override
    public int getNbPlanktonGroups() {
        return nbPlankton;
    }

    @Override
    public String getPlanktonName(int indexGroup) {
        return planktonNames[indexGroup];
    }

    @Override
    public Plankton getPlankton(int indexGroup) {
        return planktonList[indexGroup];
    }

    @Override
    public int getPlanktonDimX() {
        return planktonDimX;
    }

    @Override
    public int getPlanktonDimY() {
        return planktonDimY;
    }

    @Override
    public int getPlanktonDimZ() {
        return planktonDimZ;
    }

    public IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    Osmose getOsmose() {
        return Osmose.getInstance();
    }

    Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    @Override
    public void savePlanktonBiomass() {
        File targetPath;
        File targetFile;
        PrintWriter pr;

        String mortalityFile = getOsmose().outputFileNameTab[numSerie] + "_planktonBiomassMatrix_Simu" + getOsmose().numSimu + ".csv";

        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "planktonBiomass");
        targetPath.mkdirs();

        FileOutputStream planktonTime;
        try {
            targetFile = new File(targetPath, mortalityFile);
            planktonTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(planktonTime, true);

        for (int j = 0; j < getGrid().getNbLines(); j++) {
            pr.print(getSimulation().getYear() + getSimulation().getIndexTime() / (float) getSimulation().getNbTimeStepsPerYear());
            pr.print(';');
            for (int p = 0; p < nbPlankton; p++) {
                for (int i = 0; i < getGrid().getNbColumns(); i++) {
                    pr.print(planktonList[p].biomass[j][i]);
                    pr.print(";");

                }
            }
            pr.println();
        }
        pr.close();
    }
}
