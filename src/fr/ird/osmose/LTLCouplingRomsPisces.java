package fr.ird.osmose;

/*******************************************************************************
 * <p>Titre : Coupling class </p>
 *
 * <p>Description : class dealing with plankton coupling - read plankton input files - makes interpolation - launch LTL model run </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1 
 ******************************************************************************* 
 */
import java.io.*;


//for the following packages, the jdk 5 or higher is required 

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;
import ucar.ma2.ArrayDouble;
import ucar.ma2.Index;

public class LTLCouplingRomsPisces implements LTLCoupling {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/07 phv
     * Osmose and Simulation are called with Osmose.getInstance()
     * and Osmose.getInstance().getSimulation()
     * Deleted the grid variable and replaced it by method getGrid() that calls
     * the Osmose grid.
     * ***
     */
    int nbPlankton, nbForcingDt;
    // Tables of plankton information
    String[][] planktonFileListTxt;		// List of the file names of plankton matrix provided as input
    String[] planktonFileListNetcdf;
    String[] planktonNames;		// list of names of plankton groups
    float[] trophicLevel;			// list of TL of plankton groups
    float[] minSize, maxSize;		// list of min and max sizes of plankton groups
    float[] conversionFactors;		// list of conversionFactors of plankton groups
    float[] prodBiomFactors;		// list of prod/biom ratios of plankton groups
    Plankton[] planktonList;     // list of plankton groups (here 4)
    int nbDimensionsGrid;
    int planktonDimX;      // dimension of LTL model, here ROMS Plume (144 * 65 * 20)
    int planktonDimY;
    int planktonDimZ;	// vertical dimension (20)
    float[][][] depthOfLayer;       // table of height of layers of ROMS model used in vertical integration
    float integrationDepth;
    float[][] latitude, longitude;     // latitude and longitude of each cell of the LTL grid, used for interpolation
    float[][] tableToFill;    // temporary table
    float[][][][] saveMortality;     // mortality field to apply to plankton groups
    FileOutputStream planktonTime;    // for saving
    String[] plktonNetcdfNames;
    ArrayDouble phyto1, phyto2, zoo1, zoo2;    // for reading NetCDF files
    String planktonFilesPath;
    String configFileName;
    String nameLatFile;
    String nameLongFile;
    String[] nameDepthFile;
    String filesFormat;	// "text" or "netcdf" - provided as input
    int startLTLModel;
    String nameFileCoupling, nameForcingFile;
    String[] cmd = {"/bin/sh", "-c", "./roms bengCoupling.in >toto.out"};
    private int numSerie;

    public LTLCouplingRomsPisces() {
        this(true);
    }

    public LTLCouplingRomsPisces(boolean isForcing) {
        planktonFilesPath = getOsmose().inputPathName;
        numSerie = getOsmose().numSerie;
        startLTLModel = getOsmose().simulationTimeTab[numSerie] + 1;
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
    public void readCouplingConfigFile(String couplingFileName) {
        FileInputStream couplingFile;
        try {
            couplingFile = new FileInputStream(new File(getOsmose().inputPathName, couplingFileName));
        } catch (FileNotFoundException ex) {
            System.out.println(" copuling configuration file doesn't exist: " + couplingFileName);
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(couplingFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            st.nextToken();
            startLTLModel = (new Integer(st.sval)).intValue();
            st.nextToken();
            cmd[2] = st.sval;
            st.nextToken();
            nameFileCoupling = st.sval;
            st.nextToken();
            nameForcingFile = st.sval;
            couplingFile.close();
        } catch (IOException ex) {
            System.out.println("Reading error of coupling configuration file");
            return;
        }
    }

    @Override
    public void readLTLConfigFile1(String planktonStructureFileName) // ******** read LTL basic file with name of plankton, sizes, format of files...
    {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(planktonFilesPath, planktonStructureFileName));
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
            if (!(nbForcingDt == getSimulation().getNumberTimeStepsPerYear())) {
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
            st.nextToken();
            if (!(((new Integer(st.sval)).intValue()) == nbPlankton)) {
                System.out.println("Error concerning the number of plankton group in plankton file");
            }

            st.nextToken();
            nameLatFile = st.sval;
            st.nextToken();
            nameLongFile = st.sval;

            st.nextToken();
            filesFormat = st.sval;

            if (filesFormat.equalsIgnoreCase("text")) /* ******* CASE OF BIOMASSE FILE IN TXT FORMAT ******* */ {
                planktonFileListTxt = new String[nbPlankton][];
                for (int i = 0; i < nbPlankton; i++) {
                    planktonFileListTxt[i] = new String[nbForcingDt];
                }
                for (int i = 0; i < nbPlankton; i++) {
                    for (int step = 0; step < nbForcingDt; step++) {
                        st.nextToken();
                        planktonFileListTxt[i][step] = st.sval;
                    }
                }
            } else if (filesFormat.equalsIgnoreCase("netcdf"))/* ******* CASE OF BIOMASSE FILE IN Netcdf FORMAT ******* */ {

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
                nameDepthFile = new String[planktonDimZ];
                for (int i = 0; i < planktonDimZ; i++) {
                    st.nextToken();
                    nameDepthFile[i] = st.sval;
                }
            } else {
                System.out.println("The file format for plankton files is not supported yet");
            }
        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            return;
        }

    }

    @Override
    public void initPlanktonMap() // initialize matrix - read latitude, longitude and depth files
    {
        latitude = new float[planktonDimX][];
        longitude = new float[planktonDimX][];
        if (filesFormat.equalsIgnoreCase("netcdf")) {
            depthOfLayer = new float[planktonDimX][][];
        }
        tableToFill = new float[planktonDimX][];
        saveMortality = new float[nbPlankton][][][];

        for (int p = 0; p < nbPlankton; p++) {
            saveMortality[p] = new float[planktonDimX][][];
        }

        for (int i = 0; i < planktonDimX; i++) {
            latitude[i] = new float[planktonDimY];
            longitude[i] = new float[planktonDimY];
            tableToFill[i] = new float[planktonDimY];
            if (filesFormat.equalsIgnoreCase("netcdf")) {
                depthOfLayer[i] = new float[planktonDimY][];
                for (int j = 0; j < planktonDimY; j++) {
                    depthOfLayer[i][j] = new float[planktonDimZ];
                }
            }
            for (int p = 0; p < nbPlankton; p++) {
                saveMortality[p][i] = new float[planktonDimY][];
            }
            for (int p = 0; p < nbPlankton; p++) {
                for (int y = 0; y < planktonDimY; y++) {
                    saveMortality[p][i][y] = new float[nbForcingDt];
                }
            }
        }

        readGridFile(nameLatFile);  // read latitude file and fill tableToFill
        for (int i = 0; i < planktonDimX; i++) {
            for (int j = 0; j < planktonDimY; j++) {
                latitude[i][j] = tableToFill[i][j];
            }
        }

        readGridFile(nameLongFile);  // read longitude file and fill tableToFill
        for (int i = 0; i < planktonDimX; i++) {
            for (int j = 0; j < planktonDimY; j++) {
                longitude[i][j] = tableToFill[i][j];
            }
        }

        findValidMapIndex();   // determined cell overlap for spatial integration

        if (filesFormat.equalsIgnoreCase("netcdf")) {
            for (int z = 0; z < planktonDimZ; z++) {
                readGridFile(nameDepthFile[z]);  // read depth file of layer z
                for (int i = 0; i < planktonDimX; i++) {
                    for (int j = 0; j < planktonDimY; j++) {
                        depthOfLayer[i][j][z] = tableToFill[i][j];
                    }
                }
            }
        }

        planktonList = new Plankton[nbPlankton];

        // Initialisation plankton and table of data
        for (int i = 0; i < nbPlankton; i++) {
            planktonList[i] = new Plankton(planktonNames[i], minSize[i], maxSize[i], trophicLevel[i], conversionFactors[i], prodBiomFactors[i], getOsmose().planktonAccessCoeffMatrix[i]);
        }

        if (filesFormat.equalsIgnoreCase("netcdf")) {		// for NetCDF file - Structure depends of netcdf file
            phyto1 = new ArrayDouble.D3(getSimulation().getNumberTimeStepsPerYear(), planktonDimX, planktonDimY);
            phyto2 = new ArrayDouble.D3(getSimulation().getNumberTimeStepsPerYear(), planktonDimX, planktonDimY);
            zoo1 = new ArrayDouble.D3(getSimulation().getNumberTimeStepsPerYear(), planktonDimX, planktonDimY);
            zoo2 = new ArrayDouble.D3(getSimulation().getNumberTimeStepsPerYear(), planktonDimX, planktonDimY);
        }
    }

    private void readGridFile(String FileToRead) // read latitude, longitude or depth files and fill tableTofill
    {
        FileInputStream gridFile;
        try {
            gridFile = new FileInputStream(new File(planktonFilesPath, FileToRead));
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening File " + FileToRead);
            return;
        }
        Reader r = new BufferedReader(new InputStreamReader(gridFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            for (int i = 0; i < planktonDimX; i++) {
                for (int j = 0; j < planktonDimY; j++) {
                    st.nextToken();
                    tableToFill[i][j] = new Float(st.sval).floatValue();
                }
            }

            gridFile.close();
        } catch (IOException ex) {
            System.out.println("Error while reading File " + FileToRead);
            return;
        }
    }

    private void readLTLTxtFile(String FileToRead, int pl) // read plankton files and fill integrateData of plankton classes
    {
        FileInputStream gridFile;
        try {
            gridFile = new FileInputStream(new File(planktonFilesPath, FileToRead));
        } catch (FileNotFoundException ex) {
            System.out.println("Error while opening File " + planktonFilesPath + FileToRead);
            return;
        }
        Reader r = new BufferedReader(new InputStreamReader(gridFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');
        try {
            for (int i = 0; i < planktonDimX; i++) {
                for (int j = 0; j < planktonDimY; j++) {
                    st.nextToken();
                    ((Plankton) planktonList[pl]).integratedData[i][j] = new Float(st.sval).floatValue();
                }
            }
            gridFile.close();
        } catch (IOException ex) {
            System.out.println("Error while reading File " + FileToRead);
            return;
        }
    }

    /* CASE SPECIFIC - name of variable, order of dimensions can change according to cases, depth of integration*/
    private void readNetCDFFile(String nameOfFile, int timeIndex) {
        NetcdfFile nc = null;
        String name = nameOfFile;
        Variable[] tempVar;
        int[] shape;
        ArrayFloat.D4[] tempArray;
        tempArray = new ArrayFloat.D4[nbPlankton];
        tempVar = new Variable[nbPlankton];

        try {
            nc = NetcdfFile.open(name);

            // atributes netcdf variables to local variables tempVar[]
            // "SPHYTO", "SZOO", ... are specific to the netcdf file used as input
            tempVar[0] = nc.findVariable(plktonNetcdfNames[0]);
            tempVar[1] = nc.findVariable(plktonNetcdfNames[1]);
            tempVar[2] = nc.findVariable(plktonNetcdfNames[2]);
            tempVar[3] = nc.findVariable(plktonNetcdfNames[3]);

            shape = tempVar[0].getShape();    // in ROMS benguela case : shape[0] = 1, shape[1] = 20, shape[2] = 144, shape[3] = 65

            // read data and put them in the local arrays
            for (int i = 0; i < nbPlankton; i++) {
                tempArray[i] = (ArrayFloat.D4) tempVar[i].read();
            }

            // fill dataInit of plankton classes from local arrays
            for (int i = 0; i < shape[1]; i++) {
                for (int j = 0; j < shape[2]; j++) {
                    for (int k = 0; k < shape[3]; k++) {
                        for (int p = 0; p < nbPlankton; p++) {
                            ((Plankton) planktonList[p]).dataInit[j][k][i] = tempArray[p].get(timeIndex, i, j, k);    // carreful, index not in the same order
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
    public void updatePlankton(int dt) // read files for the current time step
    {
        for (int i = 0; i < nbPlankton; i++) {
            planktonList[i].clearPlankton();      // put the biomass tables of plankton to 0
        }
        if (filesFormat.equalsIgnoreCase("text")) {
            for (int i = 0; i < nbPlankton; i++) {
                readLTLTxtFile(planktonFileListTxt[i][dt], i);       // fill integratedData table of plankton groups
            }
        } else {
            if (filesFormat.equalsIgnoreCase("netcdf")) {
                String nameTemp = getOsmose().inputPathName + getOsmose().fileSeparator + planktonFileListNetcdf[dt];
                int timeIndex = 0;
                if (getSimulation().getYear() >= startLTLModel) // two-way coupling mode
                {
                    readNetCDFFile(nameFileCoupling, timeIndex);
                } else {
                    readNetCDFFile(nameTemp, timeIndex);	// forcing mode
                }
            } else {
                System.out.println("LTL file format not supported yet");
            }
        }

        mapInterpolation();      // from LTL grid to Osmose grid
    }

    @Override
    public void saveForDiet() // save biomass of plankton before predation
    {
        for (int p = 0; p < nbPlankton; p++) {
            Indicators.biomPerStage[getSimulation().getNbSpecies() + p][0] = 0; //biomPerStage[][0] because just 1 stage per plankton group
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    if (!getGrid().getCell(i, j).isLand()) {
                        Indicators.biomPerStage[getSimulation().getNbSpecies() + p][0] += ((Plankton) planktonList[p]).biomass[i][j];
                    }
                }
            }
        }
    }

    @Override
    public void runLTLModel() {
        Process p;
        Runtime r;
        r = Runtime.getRuntime();

        try {
            p = r.exec(cmd);  // command launching the LTL model
            try {
                int exitVal = p.waitFor();
                System.out.println("Process exitValue: " + exitVal + " (0:success - 1:failure) ");
            } catch (InterruptedException e) {
                System.out.println("ERROR: waitFor Failure");
                System.exit(10);
            }
        } catch (IOException e) {
            System.out.println("ERROR exec failure");
            System.exit(11);
        }
    }

    @Override
    public void calculPlanktonMortality() // called if t>=startLTLModel, ie when in coupling mode
    {
        // compute mortality rate in day-1 for the Osmose getGrid()
        for (int p = 0; p < nbPlankton; p++) {
            for (int x = 0; x < getGrid().getNbLines(); x++) {
                for (int y = 0; y < getGrid().getNbColumns(); y++) {
                    if (!getGrid().getCell(x, y).isLand()) {
                        if (planktonList[p].iniBiomass[x][y] != 0) {
                            planktonList[p].mortalityRate[x][y] = (getSimulation().getNumberTimeStepsPerYear() / 365f) * (planktonList[p].iniBiomass[x][y] - planktonList[p].biomass[x][y]) / planktonList[p].iniBiomass[x][y];
                        } else {
                            planktonList[p].mortalityRate[x][y] = 0;
                        }
                    } else {
                        planktonList[p].mortalityRate[x][y] = (planktonList[p].getAccessibilityCoeff() / 2f) * (getSimulation().getNumberTimeStepsPerYear() / 365f);
                    }
                }
            }
        }

        // from osmose getGrid() to LTL grid
        mapInterpBack();

        // write and save in the forcing NetCDF file for two-way coupling
        saveNetCDFfile(nameForcingFile);

        // save in TXT format the mortality field per dt
        saveMortalityperTime();
    }

    private void saveMortalityperTime() {
        File targetPath;
        File targetFile;
        PrintWriter pr;

        String mortalityFile = getOsmose().outputPrefix[numSerie] + "_planktonMortalityMatrix_Simu" + getOsmose().numSimu + ".csv";

        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "planktonMortality");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, mortalityFile);
            planktonTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(planktonTime, true);

        for (int j = 0; j < getGrid().getNbLines(); j++) {
            pr.print(getSimulation().getYear() + getSimulation().getIndexTimeYear() / (float) getSimulation().getNumberTimeStepsPerYear());
            pr.print(';');
            for (int p = 0; p < nbPlankton; p++) {
                for (int i = 0; i < getGrid().getNbColumns(); i++) {
                    pr.print(planktonList[p].mortalityRate[j][i]);
                    pr.print(";");

                }
            }
            pr.println();
        }
        pr.close();
    }

    // CASE SPECIFIC - save plankton mortality rates within an existing forcing file
    private void saveNetCDFfile(String nameOfFile) {
        NetcdfFileWriteable ncfile = null;

        try {
            ncfile = NetcdfFileWriteable.openExisting(nameOfFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Index ima = phyto1.getIndex();

        for (int k = 0; k < getSimulation().getNumberTimeStepsPerYear(); k++) {
            for (int i = 0; i < planktonDimX; i++) {
                for (int j = 0; j < planktonDimY; j++) {
                    phyto1.setDouble(ima.set(k, i, j), (double) saveMortality[0][i][j][getSimulation().getIndexTimeYear()]);
                    phyto2.setDouble(ima.set(k, i, j), (double) saveMortality[1][i][j][getSimulation().getIndexTimeYear()]);
                    zoo1.setDouble(ima.set(k, i, j), (double) saveMortality[2][i][j][getSimulation().getIndexTimeYear()]);
                    zoo2.setDouble(ima.set(k, i, j), (double) saveMortality[3][i][j][getSimulation().getIndexTimeYear()]);
                }
            }
        }

        try {
            ncfile.write("MorP1", phyto1);	// "MorP1"... are specific names of the netcdf file
            ncfile.write("MorP2", phyto2);
            ncfile.write("MorZ1", zoo1);
            ncfile.write("MorZ2", zoo2);
        } catch (IOException e) {
            System.err.println("ERROR writing file");
        } catch (InvalidRangeException e) {
            e.printStackTrace();
        }

        try {
            ncfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // CASE SPECIFIC - uses easy relation between the grids Plume and Osmose
    private void findValidMapIndex() {
        int posiTemp, posjTemp;

        for (int i = 0; i < planktonDimX; i++) {
            for (int j = 0; j < planktonDimY; j++) // consider only the LTL cells included within the Osmose grid
            {
                if ((latitude[i][j] >= getGrid().getLatMin()) && (latitude[i][j] <= getGrid().getLatMax()) && (longitude[i][j] >= getGrid().getLongMin()) && (longitude[i][j] <= getGrid().getLongMax())) {
                    // equations giving the position of ROMS cells within the Osmose getGrid(), avoiding to read the whole matrix
                    /*
                     * WARNING, phv, 2011/04/07
                     * I changed the calculation of posiTemp & posjTemp
                     * but it should be double checked.
                     * 2011/08/04 phv, looks like the change in positemp led to
                     * different outputs... so I reverted the change.
                     */
                    posiTemp = (int) Math.floor(-(latitude[i][j] - getGrid().getLatMax()) / getGrid().getdLat());    //************** Attention sign minus & latMax depend on the sign of lat and long
                    posjTemp = (int) Math.floor((longitude[i][j] - getGrid().getLongMin()) / getGrid().getdLong());

                    // attach each LTL cells to the right Osmose cell (several LTL cells per Osmose cell is allowed)
                    if (!getGrid().getCell(posiTemp, posjTemp).isLand()) {
                        getGrid().getCell(posiTemp, posjTemp).icoordLTLGrid.addElement(new Integer(i));
                        getGrid().getCell(posiTemp, posjTemp).jcoordLTLGrid.addElement(new Integer(j));
                    }
                }
            }
        }
    }

    // CASE SPECIFIC - depends of the LTL grid
    @Override
    public void mapInterpolation() // from ROMS (vertically integrated) towards OSMOSE
    {
        int tempX, tempY;

        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    if (getGrid().getCell(i, j).getNbCellsLTLGrid() != 0) // if this osmose cell is composed of at least one LTL cell
                    {
                        for (int k = 0; k < getGrid().getCell(i, j).getNbCellsLTLGrid(); k++) {
                            for (int p = 0; p < nbPlankton; p++) {
                                tempX = ((Integer) getGrid().getCell(i, j).icoordLTLGrid.elementAt(k)).intValue();
                                tempY = ((Integer) getGrid().getCell(i, j).jcoordLTLGrid.elementAt(k)).intValue();
                                // interpolate the plankton concentrations from the LTL cells
                                ((Plankton) planktonList[p]).addCell(i, j, tempX, tempY, getGrid().getCell(i, j).getNbCellsLTLGrid());
                            }
                        }
                    } else // if no LTL cell is associated with this osmose cell (because of curvilinear grid of ROMS)
                    // -> then uses the neighbor cells to get the average plankton biomass
                    {
                        int nbCellsTemp = 0;
                        if (i > 0) {
                            if (!getGrid().getCell(i - 1, j).isLand()) {
                                nbCellsTemp += getGrid().getCell(i - 1, j).getNbCellsLTLGrid();
                            }
                        }
                        if (i < getGrid().getNbLines() - 1) {
                            if (!getGrid().getCell(i + 1, j).isLand()) {
                                nbCellsTemp += getGrid().getCell(i + 1, j).getNbCellsLTLGrid();
                            }
                        }
                        if (j > 0) {
                            if (!getGrid().getCell(i, j - 1).isLand()) {
                                nbCellsTemp += getGrid().getCell(i, j - 1).getNbCellsLTLGrid();
                            }
                        }
                        if (j < getGrid().getNbColumns() - 1) {
                            if (!getGrid().getCell(i, j + 1).isLand()) {
                                nbCellsTemp += getGrid().getCell(i, j + 1).getNbCellsLTLGrid();
                            }
                        }

                        if (i > 0) {
                            for (int k = 0; k < getGrid().getCell(i - 1, j).getNbCellsLTLGrid(); k++) {
                                for (int p = 0; p < nbPlankton; p++) {
                                    tempX = ((Integer) getGrid().getCell(i - 1, j).icoordLTLGrid.elementAt(k)).intValue();
                                    tempY = ((Integer) getGrid().getCell(i - 1, j).jcoordLTLGrid.elementAt(k)).intValue();
                                    ((Plankton) planktonList[p]).addCell(i, j, tempX, tempY, nbCellsTemp);
                                }
                            }
                        }

                        if (i < getGrid().getNbLines() - 1) {
                            for (int k = 0; k < getGrid().getCell(i + 1, j).getNbCellsLTLGrid(); k++) {
                                for (int p = 0; p < nbPlankton; p++) {
                                    tempX = ((Integer) getGrid().getCell(i + 1, j).icoordLTLGrid.elementAt(k)).intValue();
                                    tempY = ((Integer) getGrid().getCell(i + 1, j).jcoordLTLGrid.elementAt(k)).intValue();
                                    ((Plankton) planktonList[p]).addCell(i, j, tempX, tempY, nbCellsTemp);
                                }
                            }
                        }

                        if (j > 0) {
                            for (int k = 0; k < getGrid().getCell(i, j - 1).getNbCellsLTLGrid(); k++) {
                                for (int p = 0; p < nbPlankton; p++) {
                                    tempX = ((Integer) getGrid().getCell(i, j - 1).icoordLTLGrid.elementAt(k)).intValue();
                                    tempY = ((Integer) getGrid().getCell(i, j - 1).jcoordLTLGrid.elementAt(k)).intValue();
                                    ((Plankton) planktonList[p]).addCell(i, j, tempX, tempY, nbCellsTemp);
                                }
                            }
                        }

                        if (j < getGrid().getNbColumns() - 1) {
                            for (int k = 0; k < getGrid().getCell(i, j + 1).getNbCellsLTLGrid(); k++) {
                                for (int p = 0; p < nbPlankton; p++) {
                                    tempX = ((Integer) getGrid().getCell(i, j + 1).icoordLTLGrid.elementAt(k)).intValue();
                                    tempY = ((Integer) getGrid().getCell(i, j + 1).jcoordLTLGrid.elementAt(k)).intValue();
                                    ((Plankton) planktonList[p]).addCell(i, j, tempX, tempY, nbCellsTemp);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void savePlanktonBiomass(String format) {
        File targetPath;
        File targetFile;
        PrintWriter pr;

        String mortalityFile = getOsmose().outputPrefix[numSerie] + "_planktonBiomassMatrix_Simu" + getOsmose().numSimu + ".csv";

        targetPath = new File(getOsmose().outputPathName + getOsmose().outputFileNameTab[numSerie] + getOsmose().fileSeparator + "planktonBiomass");
        targetPath.mkdirs();

        try {
            targetFile = new File(targetPath, mortalityFile);
            planktonTime = new FileOutputStream(targetFile, true);
        } catch (IOException ie) {
            System.err.println(ie.getMessage());
            return;
        }

        pr = new PrintWriter(planktonTime, true);

        for (int j = 0; j < getGrid().getNbLines(); j++) {
            pr.print(getSimulation().getYear() + getSimulation().getIndexTimeYear() / (float) getSimulation().getNumberTimeStepsPerYear());
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

    private void mapInterpBack() // from Osmose towards ROMS
    {
        int xTemp, yTemp;

        // initialize mortality field with default value (median of accessCoeff)
        for (int x = 0; x < planktonDimX; x++) {
            for (int y = 0; y < planktonDimY; y++) {
                for (int p = 0; p < nbPlankton; p++) {
                    saveMortality[p][x][y][getSimulation().getIndexTimeYear()] = (planktonList[p].getAccessibilityCoeff() / 2f) * (getSimulation().getNumberTimeStepsPerYear() / 365f);
                }
            }
        }

        // realize the interpolation in the other way than mapInterpolation()
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                // phv 2013-01-02 Deleted condition getCell(i, j).getNbMapsConcerned() since function has been removed
                //if ((!getGrid().getCell(i, j).isLand()) && (getGrid().getCell(i, j).getNbMapsConcerned() != 0)) {
                if (!getGrid().getCell(i, j).isLand()) {
                    if (getGrid().getCell(i, j).getNbCellsLTLGrid() != 0) {
                        for (int k = 0; k < getGrid().getCell(i, j).getNbCellsLTLGrid(); k++) {
                            for (int p = 0; p < nbPlankton; p++) {
                                xTemp = ((Integer) getGrid().getCell(i, j).icoordLTLGrid.elementAt(k)).intValue();
                                yTemp = ((Integer) getGrid().getCell(i, j).jcoordLTLGrid.elementAt(k)).intValue();
                                saveMortality[p][xTemp][yTemp][getSimulation().getIndexTimeYear()] = planktonList[p].mortalityRate[i][j];
                            }
                        }
                    } else {
                        int nbCellsTemp = 0;
                        if (i > 0) {
                            if (!getGrid().getCell(i - 1, j).isLand()) {
                                nbCellsTemp += getGrid().getCell(i - 1, j).getNbCellsLTLGrid();
                            }
                        }
                        if (i < getGrid().getNbLines() - 1) {
                            if (!getGrid().getCell(i + 1, j).isLand()) {
                                nbCellsTemp += getGrid().getCell(i + 1, j).getNbCellsLTLGrid();
                            }
                        }
                        if (j > 0) {
                            if (!getGrid().getCell(i, j - 1).isLand()) {
                                nbCellsTemp += getGrid().getCell(i, j - 1).getNbCellsLTLGrid();
                            }
                        }
                        if (j < getGrid().getNbColumns() - 1) {
                            if (!getGrid().getCell(i, j + 1).isLand()) {
                                nbCellsTemp += getGrid().getCell(i, j + 1).getNbCellsLTLGrid();
                            }
                        }

                        if (i > 0) {
                            for (int p = 0; p < nbPlankton; p++) {
                                for (int k = 0; k < getGrid().getCell(i - 1, j).getNbCellsLTLGrid(); k++) {
                                    xTemp = ((Integer) getGrid().getCell(i - 1, j).icoordLTLGrid.elementAt(k)).intValue();
                                    yTemp = ((Integer) getGrid().getCell(i - 1, j).jcoordLTLGrid.elementAt(k)).intValue();

                                    saveMortality[p][xTemp][yTemp][getSimulation().getIndexTimeYear()] += planktonList[p].mortalityRate[i - 1][j] / (float) nbCellsTemp;
                                }
                            }
                        }
                        if (i < getGrid().getNbLines() - 1) {
                            for (int p = 0; p < nbPlankton; p++) {
                                for (int k = 0; k < getGrid().getCell(i + 1, j).getNbCellsLTLGrid(); k++) {
                                    xTemp = ((Integer) getGrid().getCell(i + 1, j).icoordLTLGrid.elementAt(k)).intValue();
                                    yTemp = ((Integer) getGrid().getCell(i + 1, j).jcoordLTLGrid.elementAt(k)).intValue();

                                    saveMortality[p][xTemp][yTemp][getSimulation().getIndexTimeYear()] += planktonList[p].mortalityRate[i + 1][j] / (float) nbCellsTemp;
                                }
                            }
                        }

                        if (j > 0) {
                            for (int p = 0; p < nbPlankton; p++) {
                                for (int k = 0; k < getGrid().getCell(i, j - 1).getNbCellsLTLGrid(); k++) {
                                    xTemp = ((Integer) getGrid().getCell(i, j - 1).icoordLTLGrid.elementAt(k)).intValue();
                                    yTemp = ((Integer) getGrid().getCell(i, j - 1).jcoordLTLGrid.elementAt(k)).intValue();

                                    saveMortality[p][xTemp][yTemp][getSimulation().getIndexTimeYear()] += planktonList[p].mortalityRate[i][j - 1] / (float) nbCellsTemp;
                                }
                            }
                        }

                        if (j < getGrid().getNbColumns() - 1) {
                            for (int p = 0; p < nbPlankton; p++) {
                                for (int k = 0; k < getGrid().getCell(i, j + 1).getNbCellsLTLGrid(); k++) {
                                    xTemp = ((Integer) getGrid().getCell(i, j + 1).icoordLTLGrid.elementAt(k)).intValue();
                                    yTemp = ((Integer) getGrid().getCell(i, j + 1).jcoordLTLGrid.elementAt(k)).intValue();

                                    saveMortality[p][xTemp][yTemp][getSimulation().getIndexTimeYear()] += planktonList[p].mortalityRate[i][j + 1] / (float) nbCellsTemp;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getStartYearLTLModel() {
        return startLTLModel;
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
}
