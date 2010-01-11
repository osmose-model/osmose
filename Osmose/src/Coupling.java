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


public class Coupling
{
	Simulation simulation;
	Grid grid;
	int nbPlankton, nbForcingDt;

	// Tables of plankton information
	String[][] planktonFileListTxt;		// List of the file names of plankton matrix provided as input
	String[] planktonFileListNetcdf;
	String[] planktonNames;		// list of names of plankton groups
	float[] trophicLevel;			// list of TL of plankton groups	
	float[] minSize, maxSize;		// list of min and max sizes of plankton groups
	float[] conversionFactors;		// list of conversionFactors of plankton groups
	float[] prodBiomFactors;		// list of prod/biom ratios of plankton groups

	Plankton[]  planktonList;     // list of plankton groups (here 4)
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
	boolean isForcing;
	String[] cmd = { "/bin/sh", "-c", "./roms bengCoupling.in >toto.out" };

	public Coupling(Simulation simulation, boolean isForcing)
	{
		this.simulation = simulation;
		this.grid = simulation.osmose.grid;
		planktonFilesPath = simulation.osmose.inputPathName;
		this.isForcing = isForcing;
		if(!isForcing)
			readCouplingConfigFile(simulation.osmose.couplingFileNameTab[simulation.numSerie]);
		else
		{
			startLTLModel = simulation.osmose.simulationTimeTab[simulation.numSerie] + 1;
		}
	}

	public void readCouplingConfigFile(String couplingFileName)
	{
		FileInputStream couplingFile;
		try
		{
			couplingFile=new FileInputStream(new File(simulation.osmose.inputPathName, couplingFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println(" copuling configuration file doesn't exist: "+couplingFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(couplingFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			startLTLModel = (new Integer(st.sval)).intValue();
			st.nextToken();
			cmd[2] = st.sval;
			st.nextToken();
			nameFileCoupling = st.sval;
			st.nextToken();
			nameForcingFile = st.sval;
			couplingFile.close();
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of coupling configuration file");
			return;
		}
	}

	public void iniCouplingReading(String planktonStructureFileName)         // ******** read LTL basic file with name of plankton, sizes, format of files...
	{
		FileInputStream LTLFile;
		try
		{LTLFile=new FileInputStream(new File(planktonFilesPath,planktonStructureFileName));}
		catch (FileNotFoundException ex)
		{System.out.println("LTL file "+planktonStructureFileName+" doesn't exist");return;}

		Reader r= new BufferedReader(new InputStreamReader(LTLFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			nbPlankton = (new Integer(st.sval)).intValue();
			if(!(nbPlankton==simulation.osmose.nbPlanktonGroupsTab[simulation.numSerie]))
				System.out.println("The number of plankton group in plankton structure file does not match the one from config file");
			st.nextToken();
			nbForcingDt = (new Integer(st.sval)).intValue();
			if(!(nbForcingDt==simulation.nbDt))
				System.out.println("In the current version, the time step of plankton biomass should match the time step of osmose config");
			
			// initializing tables
			planktonNames = new String[nbPlankton];
			trophicLevel = new float[nbPlankton];
			minSize = new float[nbPlankton];
			maxSize = new float[nbPlankton];
			conversionFactors = new float[nbPlankton];
			prodBiomFactors = new float[nbPlankton];

			for (int i=0; i<nbPlankton; i++)
			{
				// filling tables
				st.nextToken();
				planktonNames[i] = st.sval;
				simulation.osmose.planktonNamesTab[simulation.numSerie][i] = st.sval;
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
			if((nbDimensionsGrid>3)||(nbDimensionsGrid<2))
				System.out.println("The dimension "+nbDimensionsGrid+" cannot be consider - should be 2 or 3");
			st.nextToken();
			planktonDimX = new Integer(st.sval).intValue();
			st.nextToken();
			planktonDimY = new Integer(st.sval).intValue();
			if(nbDimensionsGrid==3)
			{
				st.nextToken();
				planktonDimZ = new Integer(st.sval).intValue();
				st.nextToken();
				integrationDepth = new Float(st.sval).floatValue();
			}
		}
		catch(IOException ex)
		{System.out.println("Reading error of LTL structure file");return;}
	}
	
	public void readInputPlanktonFiles(String planktonFileName)
	{
		FileInputStream LTLFile;
		try
		{LTLFile=new FileInputStream(new File(planktonFilesPath,planktonFileName));}
		catch (FileNotFoundException ex)
		{System.out.println("LTL file "+planktonFileName+" doesn't exist");return;}

		Reader r= new BufferedReader(new InputStreamReader(LTLFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			if(!(((new Integer(st.sval)).intValue())==nbPlankton))
				System.out.println("Error concerning the number of plankton group in plankton file");
			
			st.nextToken();
			nameLatFile = st.sval;
			st.nextToken();
			nameLongFile = st.sval;

			st.nextToken();
			filesFormat = st.sval;
			
			if(filesFormat.equalsIgnoreCase("text"))	/* ******* CASE OF BIOMASSE FILE IN TXT FORMAT ******* */
			{
				planktonFileListTxt = new String[nbPlankton][];
				for (int i=0; i<nbPlankton; i++)
					planktonFileListTxt[i] = new String[nbForcingDt];
				for (int i=0; i<nbPlankton; i++){
					for (int step=0;step<nbForcingDt;step++){
						st.nextToken();
						planktonFileListTxt[i][step] = st.sval;
					}
				}
			}
			else if(filesFormat.equalsIgnoreCase("netcdf"))/* ******* CASE OF BIOMASSE FILE IN Netcdf FORMAT ******* */
			{
				
				plktonNetcdfNames = new String[nbPlankton];
				for (int i=0; i<nbPlankton; i++){
					st.nextToken();
					plktonNetcdfNames[i] = st.sval;
				}
				
				planktonFileListNetcdf = new String[nbForcingDt];
				for (int step=0;step<nbForcingDt;step++){
					st.nextToken();
					planktonFileListNetcdf[step] = st.sval;
				}
				nameDepthFile = new String[planktonDimZ];
				for (int i=0; i<planktonDimZ; i++){
					st.nextToken();
					nameDepthFile[i] = st.sval;
				}		
			}
			else
				System.out.println("The file format for plankton files is not supported yet");
		}
		catch(IOException ex)
		{System.out.println("Reading error of LTL file");return;}
		
	}
	
	public void initPlanktonMap()	// initialize matrix - read latitude, longitude and depth files
	{
		latitude = new float[planktonDimX][];
		longitude = new float[planktonDimX][];
		if(filesFormat.equalsIgnoreCase("netcdf"))
			depthOfLayer = new float[planktonDimX][][];
		tableToFill = new float[planktonDimX][];
		saveMortality = new float[nbPlankton][][][];

		for (int p=0; p<nbPlankton; p++)
			saveMortality[p] = new float[planktonDimX][][];

		for (int i=0; i<planktonDimX; i++)
		{
			latitude[i] = new float[planktonDimY];
			longitude[i] = new float[planktonDimY];
			tableToFill[i] = new float[planktonDimY];
			if(filesFormat.equalsIgnoreCase("netcdf"))
			{	
				depthOfLayer[i] = new float[planktonDimY][];
				for (int j=0; j<planktonDimY; j++)
					depthOfLayer[i][j] = new float[planktonDimZ];
			}
			for (int p=0; p<nbPlankton; p++)
				saveMortality[p][i] = new float[planktonDimY][];
			for (int p=0; p<nbPlankton; p++)
				for(int y=0; y<planktonDimY; y++)
					saveMortality[p][i][y] = new float[nbForcingDt];
		}

		readGridFile(nameLatFile);  // read latitude file and fill tableToFill
		for(int i=0;i<planktonDimX;i++)
			for(int j=0; j<planktonDimY; j++)
				latitude[i][j] = tableToFill[i][j];

		readGridFile(nameLongFile);  // read longitude file and fill tableToFill
		for(int i=0;i<planktonDimX;i++)
			for(int j=0; j<planktonDimY; j++)
				longitude[i][j] = tableToFill[i][j];

		findValidMapIndex();   // determined cell overlap for spatial integration

		if(filesFormat.equalsIgnoreCase("netcdf")){
			for (int z=0; z<planktonDimZ; z++)
			{
				readGridFile(nameDepthFile[z]);  // read depth file of layer z
				for(int i=0;i<planktonDimX;i++)
					for(int j=0; j<planktonDimY; j++)
						depthOfLayer[i][j][z] = tableToFill[i][j];
			}
		}

		planktonList = new Plankton[nbPlankton];

		// Initialisation plankton and table of data
		for(int i=0; i<nbPlankton; i++) 
			planktonList[i] = new Plankton(this, planktonNames[i], minSize[i], maxSize[i], trophicLevel[i],conversionFactors[i], prodBiomFactors[i], simulation.osmose.planktonAccessCoeffMatrix[i]);

		if(filesFormat.equalsIgnoreCase("netcdf")){		// for NetCDF file - Structure depends of netcdf file
			phyto1 = new ArrayDouble.D3(simulation.nbDt,planktonDimX,planktonDimY);         
			phyto2 = new ArrayDouble.D3(simulation.nbDt,planktonDimX,planktonDimY);
			zoo1 = new ArrayDouble.D3(simulation.nbDt,planktonDimX,planktonDimY);
			zoo2 = new ArrayDouble.D3(simulation.nbDt,planktonDimX,planktonDimY);
		}
	}

	public void readGridFile(String FileToRead)		// read latitude, longitude or depth files and fill tableTofill
	{
		FileInputStream gridFile;
		try{
			gridFile=new FileInputStream(new File(planktonFilesPath, FileToRead));}
		catch(FileNotFoundException ex){
			System.out.println("Error while opening File "+FileToRead);return;}
		Reader r=new BufferedReader(new InputStreamReader(gridFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			for(int i=0;i<planktonDimX;i++)
				for(int j=0; j<planktonDimY; j++)
				{
					st.nextToken();
					tableToFill[i][j] = new Float(st.sval).floatValue();
				}

			gridFile.close();
		}
		catch(IOException ex){
			System.out.println("Error while reading File "+FileToRead);return;}
	}

	public void readLTLTxtFile(String FileToRead, int pl)  // read plankton files and fill integrateData of plankton classes
	{
		FileInputStream gridFile;
		try{
			gridFile=new FileInputStream(new File(planktonFilesPath, FileToRead));}
		catch(FileNotFoundException ex){
			System.out.println("Error while opening File "+planktonFilesPath+FileToRead);return;}
		Reader r=new BufferedReader(new InputStreamReader(gridFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);st.slashStarComments(true);
		st.quoteChar(';');
		try
		{
			for(int i=0;i<planktonDimX;i++)
				for(int j=0; j<planktonDimY; j++)
				{
					st.nextToken();
					((Plankton)planktonList[pl]).integratedData[i][j] = new Float(st.sval).floatValue();
				}
			gridFile.close();
		}
		catch(IOException ex){
			System.out.println("Error while reading File "+FileToRead);return;}
	}

	/* CASE SPECIFIC - name of variable, order of dimensions can change according to cases, depth of integration*/
	public void readNetCDFFile(String nameOfFile, int timeIndex)
	{
		NetcdfFile nc = null;
		String name = nameOfFile;
		Variable[] tempVar;
		int[] shape;
		ArrayFloat.D4[] tempArray;
		tempArray = new ArrayFloat.D4[nbPlankton];
		tempVar = new Variable[nbPlankton];

		try
		{
			nc = NetcdfFile.open(name);

			// atributes netcdf variables to local variables tempVar[]
			// "SPHYTO", "SZOO", ... are specific to the netcdf file used as input
			tempVar[0] = nc.findVariable(plktonNetcdfNames[0]);
			tempVar[1] = nc.findVariable(plktonNetcdfNames[1]);
			tempVar[2] = nc.findVariable(plktonNetcdfNames[2]);
			tempVar[3] = nc.findVariable(plktonNetcdfNames[3]);

			shape = tempVar[0].getShape();    // in ROMS benguela case : shape[0] = 1, shape[1] = 20, shape[2] = 144, shape[3] = 65

			// read data and put them in the local arrays
			for (int i=0; i<nbPlankton; i++)
				tempArray[i] = (ArrayFloat.D4)tempVar[i].read();

			// fill dataInit of plankton classes from local arrays
			for(int i=0;i<shape[1]; i++)
				for (int j=0; j<shape[2]; j++)
					for (int k=0; k<shape[3]; k++)
						for (int p=0; p<nbPlankton; p++)
							((Plankton)planktonList[p]).dataInit[j][k][i] = tempArray[p].get(timeIndex,i,j,k);    // carreful, index not in the same order

							// integrates vertically plankton biomass, using depth files
			for (int p=0; p<nbPlankton; p++)
				((Plankton)planktonList[p]).verticalIntegration(depthOfLayer,integrationDepth);

		}
		catch (IOException e){
			e.printStackTrace();}

		finally{
			if (nc !=null)
				try{
					nc.close();
				}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public void updatePlankton(int dt)		// read files for the current time step
	{
		for (int i=0; i<nbPlankton; i++)
			planktonList[i].clearPlankton();      // put the biomass tables of plankton to 0

		if(filesFormat.equalsIgnoreCase("text"))
			for (int i=0; i<nbPlankton; i++)
				readLTLTxtFile(planktonFileListTxt[i][dt],i);       // fill integratedData table of plankton groups  
		else
		{
			if(filesFormat.equalsIgnoreCase("netcdf"))
			{
				String nameTemp = simulation.osmose.inputPathName + simulation.osmose.fileSeparator +planktonFileListNetcdf[dt];
				int timeIndex = 0;
				if(simulation.t>=startLTLModel)	// two-way coupling mode
					readNetCDFFile(nameFileCoupling, timeIndex);
				else
					readNetCDFFile(nameTemp, timeIndex);	// forcing mode
			}
			else
				System.out.println("LTL file format not supported yet");
		}

		mapInterpolation();      // from LTL grid to Osmose grid

		if(simulation.t>=simulation.osmose.timeSeriesStart)
			saveForDiet();       // save biomass of plankton before predation
	}


	public void saveForDiet()       // save biomass of plankton before predation
	{
		for (int p=0; p<nbPlankton; p++)
		{
			simulation.biomPerStage[simulation.species.length+p][0] = 0; //biomPerStage[][0] because just 1 stage per plankton group
			for (int i=0; i<grid.nbLines; i++)
				for (int j=0; j<grid.nbColumns; j++)
					if(!grid.matrix[i][j].coast)
						simulation.biomPerStage[simulation.species.length+p][0] += ((Plankton)planktonList[p]).biomass[i][j];
		}
	}


	public void runLTLModel()
	{
		Process p;
		Runtime r;
		r = Runtime.getRuntime(); 

		try{
			p= r.exec(cmd);  // command launching the LTL model
			try{
				int exitVal = p.waitFor();
				System.out.println("Process exitValue: " + exitVal + " (0:success - 1:failure) ");
			} 
			catch(InterruptedException e) {
				System.out.println("ERROR: waitFor Failure");
				System.exit(10);
			}
		} 
		catch (IOException e){
			System.out.println("ERROR exec failure");
			System.exit(11);
		}
	}


	public void calculPlanktonMortality()   // called if t>=startLTLModel, ie when in coupling mode
	{
		// compute mortality rate in day-1 for the Osmose grid
		for (int p=0; p<nbPlankton; p++)
			for(int x=0; x<grid.nbLines; x++)
				for (int y=0; y<grid.nbColumns; y++)
					if (!grid.matrix[x][y].coast)
					{
						if(planktonList[p].iniBiomass[x][y]!=0)
							planktonList[p].mortalityRate[x][y] = (simulation.nbDt/365f)*(planktonList[p].iniBiomass[x][y]-planktonList[p].biomass[x][y])/planktonList[p].iniBiomass[x][y];
						else
							planktonList[p].mortalityRate[x][y] = 0;
					}
					else
					{
						planktonList[p].mortalityRate[x][y] = (planktonList[p].accessibilityCoeff/2f)*(simulation.nbDt/365f);
					}

		// from osmose grid to LTL grid
		mapInterpBack();

		// write and save in the forcing NetCDF file for two-way coupling
		saveNetCDFfile(nameForcingFile);

		// save in TXT format the mortality field per dt
		saveMortalityperTime();
	}

	public void saveMortalityperTime()
	{
		File targetPath;
		File targetFile;
		PrintWriter pr;

		String mortalityFile = simulation.osmose.outputFileNameTab[simulation.numSerie]+"_planktonMortalityMatrix_Simu"+simulation.osmose.numSimu+".csv";

		targetPath = new File(simulation.osmose.outputPathName+simulation.osmose.outputFileNameTab[simulation.numSerie]+simulation.osmose.fileSeparator+"planktonMortality");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, mortalityFile);
			planktonTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(planktonTime, true);

		for (int j=0;j<grid.nbLines;j++)
		{
			pr.print(simulation.t+simulation.dt/(float)simulation.nbDt);
			pr.print(';');
			for (int p=0; p<nbPlankton; p++)
			{
				for (int i=0;i<grid.nbColumns;i++)
				{
					pr.print(planktonList[p].mortalityRate[j][i]);
					pr.print(";");

				}
			}
			pr.println();
		}
		pr.close();
	}

	// CASE SPECIFIC - save plankton mortality rates within an existing forcing file
	public void saveNetCDFfile(String nameOfFile)
	{
		NetcdfFileWriteable ncfile = null;

		try {
			ncfile = NetcdfFileWriteable.openExisting(nameOfFile);} 
		catch (IOException e) {
			e.printStackTrace();
		}

		Index ima = phyto1.getIndex();

		for (int k=0; k<simulation.nbDt; k++){
			for (int i=0; i<planktonDimX; i++) {
				for (int j=0; j<planktonDimY; j++) {
					phyto1.setDouble(ima.set(k,i,j), (double) saveMortality[0][i][j][simulation.dt]);
					phyto2.setDouble(ima.set(k,i,j), (double) saveMortality[1][i][j][simulation.dt]);
					zoo1.setDouble(ima.set(k,i,j), (double) saveMortality[2][i][j][simulation.dt]);
					zoo2.setDouble(ima.set(k,i,j), (double) saveMortality[3][i][j][simulation.dt]);
				}
			}
		}

		try {
			ncfile.write("MorP1", phyto1);	// "MorP1"... are specific names of the netcdf file
			ncfile.write("MorP2", phyto2);
			ncfile.write("MorZ1", zoo1);
			ncfile.write("MorZ2", zoo2);
		} 
		catch (IOException e) {
			System.err.println("ERROR writing file");} 
		catch (InvalidRangeException e) {
			e.printStackTrace();
		}

		try {
			ncfile.close();} 
		catch (IOException e) {
			e.printStackTrace(); }
	}

	// CASE SPECIFIC - uses easy relation between the grids Plume and Osmose
	public void findValidMapIndex()
	{
		int posiTemp, posjTemp;

		for (int i=0; i<planktonDimX; i++)
			for (int j=0; j<planktonDimY; j++)
				// consider only the LTL cells included within the Osmose grid
				if((latitude[i][j]>=grid.latMin)&&(latitude[i][j]<=grid.latMax)&&(longitude[i][j]>=grid.longMin)&&(longitude[i][j]<=grid.longMax))
				{
					// equations giving the position of ROMS cells within the Osmose grid, avoiding to read the whole matrix
					posiTemp = (int)Math.floor(-(latitude[i][j] - grid.latMax)/grid.dLat);    //************** Attention sign minus & latMax depend on the sign of lat and long
					posjTemp = (int)Math.floor((longitude[i][j] - grid.longMin)/grid.dLong);

					// attach each LTL cells to the right Osmose cell (several LTL cells per Osmose cell is allowed)
					if(!grid.matrix[posiTemp][posjTemp].coast)
					{
						grid.matrix[posiTemp][posjTemp].nbCellsLTLGrid++;
						grid.matrix[posiTemp][posjTemp].icoordLTLGrid.addElement(new Integer(i));
						grid.matrix[posiTemp][posjTemp].jcoordLTLGrid.addElement(new Integer(j));
					}
				}
	}

	// CASE SPECIFIC - depends of the LTL grid
	public void mapInterpolation ()    // from ROMS (vertically integrated) towards OSMOSE
	{
		int tempX, tempY;

		for (int i=0; i<grid.nbLines; i++)
			for (int j=0; j<grid.nbColumns; j++)
				if(!grid.matrix[i][j].coast)
					if(grid.matrix[i][j].nbCellsLTLGrid!=0) // if this osmose cell is composed of at least one LTL cell 
					{
						for (int k=0; k<grid.matrix[i][j].nbCellsLTLGrid;k++)
							for (int p=0; p<nbPlankton; p++)
							{
								tempX = ((Integer)grid.matrix[i][j].icoordLTLGrid.elementAt(k)).intValue();
								tempY = ((Integer)grid.matrix[i][j].jcoordLTLGrid.elementAt(k)).intValue();
								// interpolate the plankton concentrations from the LTL cells
								((Plankton)planktonList[p]).addCell(i,j,tempX,tempY,grid.matrix[i][j].nbCellsLTLGrid);
							}
					}
					else  // if no LTL cell is associated with this osmose cell (because of curvilinear grid of ROMS)
						// -> then uses the neighbor cells to get the average plankton biomass 
					{
						int nbCellsTemp = 0;
						if(i>0)
							if(!grid.matrix[i-1][j].coast)
								nbCellsTemp += grid.matrix[i-1][j].nbCellsLTLGrid;
						if(i<grid.nbLines-1)
							if(!grid.matrix[i+1][j].coast)
								nbCellsTemp += grid.matrix[i+1][j].nbCellsLTLGrid;
						if(j>0)
							if(!grid.matrix[i][j-1].coast)
								nbCellsTemp += grid.matrix[i][j-1].nbCellsLTLGrid;
						if(j<grid.nbColumns-1)
							if(!grid.matrix[i][j+1].coast)
								nbCellsTemp += grid.matrix[i][j+1].nbCellsLTLGrid;

						if(i>0)
							for (int k=0; k<grid.matrix[i-1][j].nbCellsLTLGrid;k++)
								for (int p=0; p<nbPlankton; p++)
								{
									tempX = ((Integer)grid.matrix[i-1][j].icoordLTLGrid.elementAt(k)).intValue();
									tempY = ((Integer)grid.matrix[i-1][j].jcoordLTLGrid.elementAt(k)).intValue();
									((Plankton)planktonList[p]).addCell(i,j,tempX,tempY,nbCellsTemp);
								}

						if(i<grid.nbLines-1)
							for (int k=0; k<grid.matrix[i+1][j].nbCellsLTLGrid;k++)
								for (int p=0; p<nbPlankton; p++)
								{
									tempX = ((Integer)grid.matrix[i+1][j].icoordLTLGrid.elementAt(k)).intValue();
									tempY = ((Integer)grid.matrix[i+1][j].jcoordLTLGrid.elementAt(k)).intValue();
									((Plankton)planktonList[p]).addCell(i,j,tempX,tempY,nbCellsTemp);
								}

						if(j>0)
							for (int k=0; k<grid.matrix[i][j-1].nbCellsLTLGrid;k++)
								for (int p=0; p<nbPlankton; p++)
								{
									tempX = ((Integer)grid.matrix[i][j-1].icoordLTLGrid.elementAt(k)).intValue();
									tempY = ((Integer)grid.matrix[i][j-1].jcoordLTLGrid.elementAt(k)).intValue();
									((Plankton)planktonList[p]).addCell(i,j,tempX,tempY,nbCellsTemp);
								}

						if(j<grid.nbColumns-1)
							for (int k=0; k<grid.matrix[i][j+1].nbCellsLTLGrid;k++)
								for (int p=0; p<nbPlankton; p++)
								{
									tempX = ((Integer)grid.matrix[i][j+1].icoordLTLGrid.elementAt(k)).intValue();
									tempY = ((Integer)grid.matrix[i][j+1].jcoordLTLGrid.elementAt(k)).intValue();
									((Plankton)planktonList[p]).addCell(i,j,tempX,tempY,nbCellsTemp);
								}
					}
	}

	public void savePlanktonBiomass()
	{
		File targetPath;
		File targetFile;
		PrintWriter pr;

		String mortalityFile = simulation.osmose.outputFileNameTab[simulation.numSerie]+"_planktonBiomassMatrix_Simu"+simulation.osmose.numSimu+".csv";

		targetPath = new File(simulation.osmose.outputPathName+simulation.osmose.outputFileNameTab[simulation.numSerie]+simulation.osmose.fileSeparator+"planktonBiomass");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, mortalityFile);
			planktonTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(planktonTime, true);

		for (int j=0;j<grid.nbLines;j++)
		{
			pr.print(simulation.t+simulation.dt/(float)simulation.nbDt);
			pr.print(';');
			for (int p=0; p<nbPlankton; p++)
			{
				for (int i=0;i<grid.nbColumns;i++)
				{
					pr.print(planktonList[p].biomass[j][i]);
					pr.print(";");

				}
			}
			pr.println();
		}
		pr.close();
	}

	public void mapInterpBack()     // from Osmose towards ROMS
	{
		int xTemp, yTemp;

		// initialize mortality field with default value (median of accessCoeff)
		for (int x = 0; x < planktonDimX; x++)
			for (int y = 0; y < planktonDimY; y++)
				for (int p = 0; p < nbPlankton; p++)
					saveMortality[p][x][y][simulation.dt] = (planktonList[p].accessibilityCoeff / 2f) * (simulation.nbDt / 365f);

		// realize the interpolation in the other way than mapInterpolation()
		for (int i = 0; i < grid.nbLines; i++)
			for (int j = 0; j < grid.nbColumns; j++)
				if ((!grid.matrix[i][j].coast) && (grid.matrix[i][j].nbMapsConcerned != 0))
					if (grid.matrix[i][j].nbCellsLTLGrid != 0)
						for (int k = 0; k < grid.matrix[i][j].nbCellsLTLGrid; k++)
							for (int p = 0; p < nbPlankton; p++)
							{
								xTemp = ((Integer) grid.matrix[i][j].icoordLTLGrid.
										elementAt(k)).intValue();
								yTemp = ((Integer) grid.matrix[i][j].jcoordLTLGrid.
										elementAt(k)).intValue();
								saveMortality[p][xTemp][yTemp][simulation.dt] = planktonList[p].mortalityRate[i][j];
							}
					else
					{
						int nbCellsTemp = 0;
						if(i>0)
							if(!grid.matrix[i-1][j].coast)
								nbCellsTemp += grid.matrix[i-1][j].nbCellsLTLGrid;
						if(i<grid.nbLines-1)
							if(!grid.matrix[i+1][j].coast)
								nbCellsTemp += grid.matrix[i+1][j].nbCellsLTLGrid;
						if(j>0)
							if(!grid.matrix[i][j-1].coast)
								nbCellsTemp += grid.matrix[i][j-1].nbCellsLTLGrid;
						if(j<grid.nbColumns-1)
							if(!grid.matrix[i][j+1].coast)
								nbCellsTemp += grid.matrix[i][j+1].nbCellsLTLGrid;

						if(i>0)
							for (int p=0; p<nbPlankton; p++)
							{
								for (int k=0; k<grid.matrix[i-1][j].nbCellsLTLGrid;k++)
								{
									xTemp = ((Integer) grid.matrix[i-1][j].icoordLTLGrid.elementAt(k)).intValue();
									yTemp = ((Integer) grid.matrix[i-1][j].jcoordLTLGrid.elementAt(k)).intValue();

									saveMortality[p][xTemp][yTemp][simulation.dt] += planktonList[p].mortalityRate[i-1][j]/(float)nbCellsTemp;
								}
							}
						if(i<grid.nbLines-1)
							for (int p=0; p<nbPlankton; p++)
								for (int k=0; k<grid.matrix[i+1][j].nbCellsLTLGrid;k++)
								{
									xTemp = ((Integer) grid.matrix[i+1][j].icoordLTLGrid.elementAt(k)).intValue();
									yTemp = ((Integer) grid.matrix[i+1][j].jcoordLTLGrid.elementAt(k)).intValue();

									saveMortality[p][xTemp][yTemp][simulation.dt] += planktonList[p].mortalityRate[i+1][j]/(float)nbCellsTemp;
								}

						if(j>0)
							for (int p=0; p<nbPlankton; p++)
								for (int k=0; k<grid.matrix[i][j-1].nbCellsLTLGrid;k++)
								{
									xTemp = ((Integer) grid.matrix[i][j-1].icoordLTLGrid.elementAt(k)).intValue();
									yTemp = ((Integer) grid.matrix[i][j-1].jcoordLTLGrid.elementAt(k)).intValue();

									saveMortality[p][xTemp][yTemp][simulation.dt] += planktonList[p].mortalityRate[i][j-1]/(float)nbCellsTemp;
								}

						if(j<grid.nbColumns-1)
							for (int p=0; p<nbPlankton; p++)
								for (int k=0; k<grid.matrix[i][j+1].nbCellsLTLGrid;k++)
								{
									xTemp = ((Integer) grid.matrix[i][j+1].icoordLTLGrid.elementAt(k)).intValue();
									yTemp = ((Integer) grid.matrix[i][j+1].jcoordLTLGrid.elementAt(k)).intValue();

									saveMortality[p][xTemp][yTemp][simulation.dt] += planktonList[p].mortalityRate[i][j+1]/(float)nbCellsTemp;
								}
					}
	}

}
