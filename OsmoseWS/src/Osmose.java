/*******************************************************************************
 * <p>Titre : Osmose </p>
 *
 * <p>Description : Main class of the Osmose model - reads the input files and initialize the series and simulations
 * - save the biomass file in case of calibration - run the simulations </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.0
 ******************************************************************************* 
 */

import java.io.*;
import java.util.*;

public class Osmose
{

	String OS_NAME = System.getProperty("os.name");
	String fileSeparator = System.getProperty("file.separator");
	String inputPathName, outputPathName;
	
	String initialPathFile = "filePath.txt";

	Simulation simulation;
	Grid grid;
	int nbSizeClass10 = 15;
	boolean simInitialized = false;
	int numSerie, numSimu;

	//INFORMATION in FILE PARAM SERIES
	//tab.length = nb of Series (of simulations)
	//for 1 serie of simus, same species parameters and options
	int nbSeriesSimus;
	int[] nbLoopTab;
	String[] speciesFileNameTab, optionFileNameTab, predationFileNameTab, outputFileNameTab, migrationFileNameTab,
	configFileNameTab,calibrationFileNameTab,reproductionFileNameTab,fishingFileNameTab,planktonFileNameTab,
	indicatorsFileNameTab, couplingFileNameTab;
	boolean[] isForcing;

	//INFOS in SPECIES PARAMETERS FILE
	int[] nbSpeciesTab;
	String[][] nameSpecMatrix,maturityMetricMatrix;
	String[] recruitMetricMatrix;
	float[][] DMatrix, larvalSurvivalMatrix, starvMaxRateMatrix;
	float[][] longevityMatrix;
	float[][] lInfMatrix, KMatrix, t0Matrix, cMatrix, bPowerMatrix;
	float[][] alphaMatrix, sizeMatMatrix;
	float[][][] seasonSpawningMatrix;
	float[][][] sizeFeedingMatrix, predPreySizesMinMatrix, predPreySizesMaxMatrix;
	int[][] nbStagesMatrix;
	float[][] recruitAgeMatrix;
	float[][] predationRateMatrix, criticalPredSuccessMatrix;
	float[][] eggSizeMatrix, eggWeightMatrix, sexRatioMatrix, growthAgeThresholdMatrix;
	float[][] supAgeOfClass0Matrix;

	// INFOS for FISHING
	String[] globalOrCohortTab;
	float[][] FMatrix, recruitSizeMatrix;
	float[][][] seasonFishingMatrix;

	// INFOS for PLANKTON groups
	int[] nbPlanktonGroupsTab;
	String[][] planktonNamesTab;

	//INFOS for CONFIG & OPTIONS
	int[] gridLinesTab, gridColumnsTab;
    float[] upLeftLatTab, lowRightLatTab, upLeftLongTab, lowRightLongTab;
    
	int[] simulationTimeTab,nbDtMatrix,savingDtMatrix,nbDtSavePerYear;

	//Parameters
	int[] nbSchools;
	float[] accessibilityCoeffMatrix;

	// SPECIES AREAS FILE
	String[] areasFileNameTab;	              //choice between "Random" or fileName
	int[] speciesAreasSizeTab;	    //used only for Qsimulation.iniRepartitionAleat() ie for random distribution
	int[][] randomAreaCoordi, randomAreaCoordj;//species areas in random cases [species][cell]
	int[][][] numMap;        //gives a number of map for[species][cohort][dt]
	int[][] mapCoordi,mapCoordj;      //    coord of maps [numMap][cell]

	//coastline
	String[] coastFileNameTab;	              //choice between "None" or fileName
	int[][] tabCoastiMatrix, tabCoastjMatrix;   //coordinates of the cells representing land
	int[] nbCellsCoastTab;

	//MPAs coordinates
	String[] mpaFileNameTab;
	int[][] tabMPAiMatrix, tabMPAjMatrix;     //coord i et j of the matrix delimiting a mpa
	boolean[] thereIsMPATab;		      //signify that 1 mpa is implemented even if t<tStart
	int[] MPAtStartTab, MPAtEndTab;	      //start and end of MPA in years

	//tables for output storage by series of simulations
	//4 dimensions : simu, species,val(total OR total-0), step t
	//for mortalities, 3 dim, the last is for the mean on the simulation period
	float[][][][][] BIOMQuadri;   //[numSimu][species][with or without age 0][t][dt]
	float[][] iniBiomass; //used for saving the biomass after initialization

	// FOR INDICATORS OUTPUT
	boolean[] TLoutputMatrix, TLDistriboutputMatrix, dietsOutputMatrix, meanSizeOutputMatrix, 
	sizeSpectrumOutputMatrix, sizeSpectrumPerSpeOutputMatrix,  
	planktonMortalityOutputMatrix, calibrationMatrix, outputClass0Matrix;

	//Initial abundances
	String[] calibrationMethod;
	double[][] spBiomIniTab;
	double[] SSslope,SSintercept;

	//size spectrum
	float spectrumMinSize, spectrumMaxSize;
	float[] tabSizes5, tabSizes10;//size classes of 10cm are used for ini and output
	float[] tabSizesLn5, tabSizesLn10;//for output
	int nbSizeClass;

	// TL distrib
	int nbTLClass;
	float minTL;
	float maxTL;
	float[] tabTL;

	//param allowing to adapt the size of output tables depending on simulation time
	int timeSeriesLength, timeSeriesStart;
	int[] startingSavingTimeTab;
	boolean timeSeriesIsShortened;

	public Osmose()
	{

		readPathFile();
		
		if (OS_NAME.startsWith("Linux")) {
			outputPathName = inputPathName + "/output/";    	//Name of the path of output folder (!!with path separator at the end!!)	
		}
		else if (OS_NAME.startsWith("Windows")) {
			outputPathName =  inputPathName + ".\\output\\"; 
		}
		
		readInputFile();

		for(int x=0;x<nbSeriesSimus;x++)
		{
			readConfigurationFile(configFileNameTab[x],x);
			readOptionFile(optionFileNameTab[x],x);
			readSpeciesFile(speciesFileNameTab[x],x);
			readPredationFile(predationFileNameTab[x],x);
			readCalibrationFile(calibrationFileNameTab[x],x);
			readSeasonalityReproFile(reproductionFileNameTab[x],x);
			readFishingFile(fishingFileNameTab[x],x);
			readIndicatorsFile(indicatorsFileNameTab[x],x);
		}
		//initialisation of the size spectrum features
		spectrumMinSize = 0.05f;
		spectrumMaxSize = 200f;
		nbSizeClass = (int)(spectrumMaxSize/5);//size classes of 5 cm

		tabSizes5 = new float[nbSizeClass];
		tabSizes5[0] = spectrumMinSize;
		for(int i=1;i<nbSizeClass;i++)
			tabSizes5[i] = i * 5;
		tabSizes10 = new float[nbSizeClass/2];
		tabSizes10[0] = spectrumMinSize;
		for(int i=1;i<tabSizes10.length;i++)
			tabSizes10[i] = i * 10;

		tabSizesLn5 = new float[nbSizeClass];
		tabSizesLn10 = new float[nbSizeClass/2];
		tabSizesLn5[0] =(float)( Math.log(2.5));
		for(int i=1;i<nbSizeClass;i++)
			tabSizesLn5[i] = (float)(Math.log(tabSizes5[i]+2.5));
		tabSizesLn10[0] = (float)(Math.log(5.));
		for(int i=1;i<tabSizesLn10.length;i++)
			tabSizesLn10[i] =(float)( Math.log(i*10+5.));

		minTL = 1;
		maxTL = 6;
		nbTLClass = (int)(1+((6-1)/0.1f));   // TL classes of 0.1
		tabTL = new float[nbTLClass];
		tabTL[0] = minTL;
		for (int i=1;i<nbTLClass;i++)
			tabTL[i] = tabTL[i-1] + 0.1f;
	}

	public void runSeriesSimulations()
	{
		Runtime r = Runtime.getRuntime();
		long freeMem = r.freeMemory();
		for(int x=0;x<nbSeriesSimus;x++)
		{
			numSerie = x;
			System.out.println("SERIE "+x);
			for(int xx=0;xx<nbLoopTab[x];xx ++)
			{
				numSimu = xx;
				r.gc();
				freeMem = r.freeMemory();
				System.out.println("Simulation "+xx+"        **** FREE MEMORY = "+freeMem);

				if(numSimu == 0)
				{
					initializeOptions();
					System.out.println("options initialized");

					simulation= new Simulation(this,nbDtMatrix[x],savingDtMatrix[x],nbSpeciesTab[x],nameSpecMatrix[x],DMatrix[x],FMatrix[x],
							longevityMatrix[x],lInfMatrix[x],KMatrix[x],t0Matrix[x],cMatrix[x],bPowerMatrix[x],alphaMatrix[x],
							sizeMatMatrix[x],nbStagesMatrix[x],sizeFeedingMatrix[x],recruitAgeMatrix[x],recruitSizeMatrix[x],
							seasonFishingMatrix[x], recruitMetricMatrix[x], seasonSpawningMatrix[x], supAgeOfClass0Matrix[x],
							larvalSurvivalMatrix[x],sexRatioMatrix[x],eggSizeMatrix[x],eggWeightMatrix[x],growthAgeThresholdMatrix[x],
							predationRateMatrix[x],predPreySizesMaxMatrix[x],predPreySizesMinMatrix[x],criticalPredSuccessMatrix[x],starvMaxRateMatrix[x],
							TLoutputMatrix[x], TLDistriboutputMatrix[x], dietsOutputMatrix[x], meanSizeOutputMatrix[x], 
							sizeSpectrumOutputMatrix[x], sizeSpectrumPerSpeOutputMatrix[x], planktonMortalityOutputMatrix[x], calibrationMatrix[x], outputClass0Matrix[x], isForcing[x]);

					System.out.println("simulation initialized");

					initializeOutOfZoneCarac();
					System.out.println("migration caracteristics initialized");

					initializeSpeciesAreas();
					System.out.println("areas initialized");

					//in initialiserSpeciesareas, save in tabTemp the areas by cohort
					//do not distribute the species in simulation()
					initializeOutputData();
					System.out.println("output data initialized");
				}
				else
				{
					grid = new Grid(this,gridLinesTab[numSerie],gridColumnsTab[numSerie],upLeftLatTab[numSerie], 
							upLeftLongTab[numSerie], lowRightLatTab[numSerie], lowRightLongTab[numSerie]);
					if(!coastFileNameTab[numSerie].equalsIgnoreCase("None"))
						updateCoastCells(numSerie);

					simulation= new Simulation(this,nbDtMatrix[x],savingDtMatrix[x],nbSpeciesTab[x],nameSpecMatrix[x],DMatrix[x],FMatrix[x],
							longevityMatrix[x],lInfMatrix[x],KMatrix[x],t0Matrix[x],cMatrix[x],bPowerMatrix[x],alphaMatrix[x],
							sizeMatMatrix[x],nbStagesMatrix[x],sizeFeedingMatrix[x],recruitAgeMatrix[x],recruitSizeMatrix[x],
							seasonFishingMatrix[x], recruitMetricMatrix[x], seasonSpawningMatrix[x], supAgeOfClass0Matrix[x],
							larvalSurvivalMatrix[x],sexRatioMatrix[x],eggSizeMatrix[x],eggWeightMatrix[x],growthAgeThresholdMatrix[x],
							predationRateMatrix[x],predPreySizesMaxMatrix[x],predPreySizesMinMatrix[x],criticalPredSuccessMatrix[x],starvMaxRateMatrix[x],
							TLoutputMatrix[x], TLDistriboutputMatrix[x], dietsOutputMatrix[x], meanSizeOutputMatrix[x], 
							sizeSpectrumOutputMatrix[x], sizeSpectrumPerSpeOutputMatrix[x],  
							planktonMortalityOutputMatrix[x], calibrationMatrix[x], outputClass0Matrix[x], isForcing[x]);

					initializeOutOfZoneCarac();
					initializeSpeciesAreas();
				}

				runSimulation();
				System.out.println();
				System.out.println("simu "+numSimu+" end");
				System.out.println(new Date());
			}

			saveSerieSimulations(numSerie);
			simulation = null;
			grid = null;
		}
	}

	public void readPathFile()
	{
		
		FileInputStream pathFile;
		try
		{
			pathFile=new FileInputStream(new File(initialPathFile));
		}
		catch (FileNotFoundException ex)
		{System.out.println("initial path file doesn't exist");return;}

		Reader r= new BufferedReader(new InputStreamReader(pathFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			inputPathName = st.sval;
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of path file");
			return;
		}
	}
	
	public void readInputFile()
	{
		/*STRUCTURE OF FILE "INPUT.txt"
		;nb of Series (of simulations);
		;nb of loops or simulations for serie1; for serie2; ----->
		;"Random" OR SpeciesFileName;----->
		;configFileName;----->
        ;optionFileName;----->
        ;calibrationFileName for serie 1;------->
        ;fishingFileName for serie 1;------->
        ;reproductionFileName for serie 1;------->
        ;areasFileName for serie 1 or "Random";------->
        ;migrationFileName for serie 1;------->
        ;coastFileName for serie 1 or "None";------->
        ;mpaFileName for serie 1 or "None";------->
		;OutputFileName for serie 1;------->	"name of Serie"+"A"+n�simu=name of file abd output
		 */
		FileInputStream inputFile;
		try
		{
			inputFile=new FileInputStream(new File(inputPathName, "INPUT.txt"));
		}
		catch (FileNotFoundException ex)
		{System.out.println("INPUT file doesn't exist");return;}

		Reader r= new BufferedReader(new InputStreamReader(inputFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			nbSeriesSimus = (new Integer(st.sval)).intValue();
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of INPUT file");
			return;
		}
		//****initialisation of tables dimensions****
		//--- INPUT file---
		nbLoopTab = new int[nbSeriesSimus];
		speciesFileNameTab = new String[nbSeriesSimus];
		configFileNameTab = new String[nbSeriesSimus];
		optionFileNameTab = new String[nbSeriesSimus];
		predationFileNameTab = new String[nbSeriesSimus];
		calibrationFileNameTab = new String[nbSeriesSimus];
		fishingFileNameTab = new String[nbSeriesSimus];
		reproductionFileNameTab = new String[nbSeriesSimus];
		areasFileNameTab = new String[nbSeriesSimus];
		migrationFileNameTab = new String[nbSeriesSimus];
		coastFileNameTab = new String[nbSeriesSimus];
		mpaFileNameTab = new String[nbSeriesSimus];
		outputFileNameTab = new String[nbSeriesSimus];
		planktonFileNameTab = new String[nbSeriesSimus];
		indicatorsFileNameTab = new String[nbSeriesSimus];
		couplingFileNameTab = new String[nbSeriesSimus];        
		//--- CONFIGURATION file---
		gridLinesTab = new int[nbSeriesSimus];
		gridColumnsTab = new int[nbSeriesSimus];
		simulationTimeTab = new int[nbSeriesSimus];
		nbDtMatrix = new int[nbSeriesSimus];
		savingDtMatrix = new int[nbSeriesSimus];
		nbDtSavePerYear = new int[nbSeriesSimus];
		startingSavingTimeTab = new int[nbSeriesSimus];
		nbSpeciesTab = new int[nbSeriesSimus];
		isForcing = new boolean[nbSeriesSimus];
		//--- COAST file---
		upLeftLatTab = new float[nbSeriesSimus];
		lowRightLatTab = new float[nbSeriesSimus];					
		upLeftLongTab = new float[nbSeriesSimus];					
		lowRightLongTab = new float[nbSeriesSimus];					
		//--- OPTION file---
		nbSchools = new int[nbSeriesSimus];
		eggSizeMatrix = new float[nbSeriesSimus][];
		eggWeightMatrix = new float[nbSeriesSimus][];
		growthAgeThresholdMatrix = new float[nbSeriesSimus][];
		predationRateMatrix = new float[nbSeriesSimus][];
		predPreySizesMinMatrix = new float[nbSeriesSimus][][];
		predPreySizesMaxMatrix = new float[nbSeriesSimus][][];
		criticalPredSuccessMatrix = new float[nbSeriesSimus][];
		starvMaxRateMatrix = new float[nbSeriesSimus][];

		//--- SPECIES file---

		nbPlanktonGroupsTab= new int[nbSeriesSimus];
		planktonNamesTab= new String[nbSeriesSimus][];
		nameSpecMatrix = new String[nbSeriesSimus][];
		DMatrix = new float[nbSeriesSimus][];
		longevityMatrix = new float[nbSeriesSimus][];
		lInfMatrix = new float[nbSeriesSimus][];
		KMatrix = new float[nbSeriesSimus][];
		t0Matrix = new float[nbSeriesSimus][];
		cMatrix = new float[nbSeriesSimus][];
		bPowerMatrix = new float[nbSeriesSimus][];
		alphaMatrix = new float[nbSeriesSimus][];
		maturityMetricMatrix = new String[nbSeriesSimus][];
		sizeMatMatrix = new float[nbSeriesSimus][];
		sexRatioMatrix = new float[nbSeriesSimus][];
		sizeFeedingMatrix = new float[nbSeriesSimus][][];
		nbStagesMatrix = new int[nbSeriesSimus][];
		//--- CALIBRATION file---
		calibrationMethod = new String[nbSeriesSimus];
		spBiomIniTab = new double[nbSeriesSimus][];
		larvalSurvivalMatrix = new float[nbSeriesSimus][];
		supAgeOfClass0Matrix = new float[nbSeriesSimus][];
		SSslope = new double[nbSeriesSimus];
		SSintercept = new double[nbSeriesSimus];
		accessibilityCoeffMatrix = new float[4];
		//--- REPRODUCTION file---
		seasonSpawningMatrix = new float[nbSeriesSimus][][];
		//--- FISHING file---
		globalOrCohortTab = new String[nbSeriesSimus];
		FMatrix = new float[nbSeriesSimus][];
		recruitSizeMatrix = new float[nbSeriesSimus][];
		recruitAgeMatrix = new float[nbSeriesSimus][];
		recruitMetricMatrix = new String[nbSeriesSimus];
		seasonFishingMatrix = new float[nbSeriesSimus][][];

		//--- SPATIAL initialisations---
		speciesAreasSizeTab = new int[nbSeriesSimus];

		tabCoastiMatrix = new int[nbSeriesSimus][];
		tabCoastjMatrix = new int[nbSeriesSimus][];
		nbCellsCoastTab = new int[nbSeriesSimus];

		tabMPAiMatrix = new int[nbSeriesSimus][];
		tabMPAjMatrix = new int[nbSeriesSimus][];
		thereIsMPATab = new boolean[nbSeriesSimus];;
		MPAtStartTab = new int[nbSeriesSimus];
		MPAtEndTab = new int[nbSeriesSimus];

		//--- OUTPUT INDICATORS ---

		TLoutputMatrix = new boolean[nbSeriesSimus]; 
		TLDistriboutputMatrix = new boolean[nbSeriesSimus];  
		dietsOutputMatrix = new boolean[nbSeriesSimus];  
		meanSizeOutputMatrix = new boolean[nbSeriesSimus];  
		sizeSpectrumOutputMatrix = new boolean[nbSeriesSimus];  
		sizeSpectrumPerSpeOutputMatrix = new boolean[nbSeriesSimus];    
		planktonMortalityOutputMatrix = new boolean[nbSeriesSimus];  
		calibrationMatrix = new boolean[nbSeriesSimus];  
		outputClass0Matrix = new boolean[nbSeriesSimus]; 

		try
		{
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				nbLoopTab[x]=(new Integer(st.sval)).intValue();
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				speciesFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				configFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				optionFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				predationFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				calibrationFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				fishingFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				reproductionFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				areasFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				migrationFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				coastFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				mpaFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				outputFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				planktonFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				indicatorsFileNameTab[x] = st.sval;
			}
			for(int x=0;x<nbSeriesSimus;x++)
			{
				st.nextToken();
				couplingFileNameTab[x] = st.sval;
			}
			inputFile.close();
		}
		catch(IOException ex)
		{System.out.println("Reading error of INPUT file");return;}
	}

	public void readSpeciesFile(String speciesFileName, int numSerie)
	{
		/*Structure of file speciesFile
		;nbSpecies;
		;speciesName;	    ------>*nbSpecies
		;D;		    ------>*nbSpecies
        ;longevity;	    ------>*nbSpecies
		;lInf;		    ------>*nbSpecies
		;K;		    ------>*nbSpecies
		;t0;		    ------>*nbSpecies
		;c;		    ------>*nbSpecies
       	;b;		    ------>*nbSpecies
		;alpha;		    ------>*nbSpecies
		;maturity metric;	    ------>*nbSpecies
        ;age or size Mat;	    ------>*nbSpecies
        ;sex ratio;
		 */

		FileInputStream speciesFile;
		try
		{
			speciesFile=new FileInputStream(new File(inputPathName,speciesFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("Species file "+speciesFileName+" doesn't exist");return;}

		Reader r= new BufferedReader(new InputStreamReader(speciesFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			if(new Integer(st.sval).intValue()==nbSpeciesTab[numSerie])
			{
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					nameSpecMatrix[numSerie][i]=st.sval;
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					DMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					longevityMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					lInfMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					KMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					t0Matrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					cMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					bPowerMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					alphaMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					maturityMetricMatrix[numSerie][i]= st.sval;
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					if(maturityMetricMatrix[numSerie][i].equalsIgnoreCase("age"))   //conversion from maturity age to maturity size through von Bertalanffy equation
					{
						st.nextToken();
						sizeMatMatrix[numSerie][i] = lInfMatrix[numSerie][i]*(float)(1-Math.exp(-KMatrix[numSerie][i]
						                                                                                           *((new Float(st.sval).floatValue())-t0Matrix[numSerie][i])));             //***** to checked if not too big fish (same test than for recruit age)
					}
					else
					{
						st.nextToken();
						sizeMatMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
					}
				}
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					sexRatioMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
				}
			}
			else
			{
				System.out.println("Uncorrect number of species in species file");return;
			}
			speciesFile.close();
		}
		catch(IOException ex)
		{System.out.println("Reading error of species file");return;}
	}

	public void readSeasonalityReproFile(String reproductionFileName, int numSerie)
	{
		/*Structure of file seasonalityFile (IN CAPITAL default options)
            ;nb time step;
            sp 1
            ;% of spawning; --->*nb time step
            sp 2
            ;% of spawning; --->*nb time step
            ...
		 */

		FileInputStream reproductionFile;
		try
		{
			reproductionFile=new FileInputStream(new File(inputPathName, reproductionFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("reproduction file doesn't exist: "+reproductionFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(reproductionFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		float tempSum;

		try
		{
			st.nextToken();
			if(new Integer(st.sval).intValue()==nbDtMatrix[numSerie])
			{
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					tempSum=0;
					seasonSpawningMatrix[numSerie][i] = new float[nbDtMatrix[numSerie]];
					for(int j=0;j<nbDtMatrix[numSerie];j++)
					{
						st.nextToken();
						seasonSpawningMatrix[numSerie][i][j] = (new Float(st.sval)).floatValue()/100; //percentage
						tempSum+=(new Float(st.sval)).floatValue();
					}
					if(!((tempSum>99.f)&&(tempSum<101.f)))
						System.out.println("ERROR: sum of percents does not equal 100% in spawning seasonality file");
				}
			}
			else
				System.out.println("Error in nb time steps defined in the reproduction seasonality file");
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of reproduction seasonality file");
			return;
		}
	}

	public void readFishingFile(String fishingFileName, int numSerie)
	{
		/*Structure of file seasonalityFile (IN CAPITAL default options)
                ;global or cohort;
                ;F;
                ;fishing metric "age or size"
                ;age of recrutment; (or size)
                seasonnality % of F per dt
                ...
		 */

		FileInputStream fishingFile;
		try
		{
			fishingFile=new FileInputStream(new File(inputPathName, fishingFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("fishing file doesn't exist: "+fishingFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(fishingFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		float tempSum;

		try
		{
			st.nextToken();
			globalOrCohortTab[numSerie] = st.sval;

			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				FMatrix[numSerie][i]=(new Float(st.sval)).floatValue();    //annual F mortality
			}

			st.nextToken();
			recruitMetricMatrix[numSerie] = st.sval;

			if(recruitMetricMatrix[numSerie].equalsIgnoreCase("age"))
			{
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					recruitAgeMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
				}

			}
			else
			{
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					recruitSizeMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
					if(recruitSizeMatrix[numSerie][i]<lInfMatrix[numSerie][i])
						recruitAgeMatrix[numSerie][i]=(float)(-((Math.log(1-(recruitSizeMatrix[numSerie][i]/lInfMatrix[numSerie][i])))/KMatrix[numSerie][i]))+t0Matrix[numSerie][i];
					else
						recruitAgeMatrix[numSerie][i]=longevityMatrix[numSerie][i]+1;
					if(recruitAgeMatrix[numSerie][i]<0.6)//due to inverse von Bert transformation
					recruitAgeMatrix[numSerie][i]=(float)0.6; // >0.5 to avoid Math.round() problems
					if(recruitAgeMatrix[numSerie][i]>longevityMatrix[numSerie][i])
						recruitAgeMatrix[numSerie][i]=longevityMatrix[numSerie][i]+1;
				}
			}

			if(globalOrCohortTab[numSerie].equalsIgnoreCase("cohort"))  //*******************TO DETAIL************************
			{
			}

			st.nextToken();
			if(new Integer(st.sval).intValue()==nbDtMatrix[numSerie])

			{
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					tempSum = 0;
					seasonFishingMatrix[numSerie][i] = new float[nbDtMatrix[numSerie]];
					for(int j=0;j<nbDtMatrix[numSerie];j++)
					{
						st.nextToken();
						seasonFishingMatrix[numSerie][i][j] = (new Float(st.sval)).floatValue()/100;   //percentage
						tempSum+=(new Float(st.sval)).floatValue();
					}
					if(!((tempSum>99.f)&&(tempSum<101.f)))
						System.out.println("ERROR: sum of percents does not equal 100% in fishing seasonality file");
				}
			}
			else
				System.out.println("Error in nb time steps defined in the fishing seasonality file");
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of fishing seasonality file");
			return;
		}
	}

	public void readCalibrationFile(String calibrationFileName, int numSerie)
	{
		/*Structure of file calibration
		;biomass;
      	;val;;val;;-------->nbSpecies of seriex
		---OR---
		;spectrum;
        ;...;
        ---OR---
        ;random;
        ;...;

        ;larval mortalities;-------->nb Species
        ;supAgeOfClass0Matrix;-------->nb Species

        ;coeff accessibility;-------->nb Plankton

		 */
		FileInputStream calibFile;
		try
		{
			calibFile=new FileInputStream(new File(inputPathName,calibrationFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("abdIni file doesn't exist: "+calibrationFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(calibFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			calibrationMethod[numSerie] = st.sval;
			if(calibrationMethod[numSerie].equalsIgnoreCase("biomass"))
			{
				spBiomIniTab[numSerie] = new double[nbSpeciesTab[numSerie]];
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					st.nextToken();
					spBiomIniTab[numSerie][i]=(new Double(st.sval)).doubleValue();
				}
			}
			else if(calibrationMethod[numSerie].equalsIgnoreCase("spectrum"))
			{
				st.nextToken();
				SSslope[numSerie]=(new Double(st.sval)).doubleValue();
				st.nextToken();
				SSintercept[numSerie]=(new Double(st.sval)).doubleValue();
			}
			else if(calibrationMethod[numSerie].equalsIgnoreCase("random"))     //*****************TO DETAIL****************************
			{
			}

			supAgeOfClass0Matrix[numSerie] = new float[nbSpeciesTab[numSerie]];
			larvalSurvivalMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];

			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				larvalSurvivalMatrix[numSerie][i]=(new Float(st.sval)).floatValue();
			}
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				supAgeOfClass0Matrix[numSerie][i]=(new Float(st.sval)).floatValue();
			}
			for(int i=0;i<4;i++)
			{
				st.nextToken();
				accessibilityCoeffMatrix[i]=(new Float(st.sval)).floatValue();
			}
			calibFile.close();
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of calibration file");
			return;
		}
	}

	public void readConfigurationFile(String configFileName, int numSerie)
	{
		/*Structure of file optionFile (IN CAPITAL default options)
            ;nb lines of the grid;
            ;nb columns of the grid;
            ;simulation time (in years);
            ;nb of time subdivisons per year;
            ;nb of time steps for saving;
            ;nbSpecies;
		 */

		FileInputStream configFile;
		try
		{
			configFile=new FileInputStream(new File(inputPathName, configFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("configuration file doesn't exist: "+configFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(configFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			gridLinesTab[numSerie] = (new Integer(st.sval)).intValue();
			st.nextToken();
			gridColumnsTab[numSerie] = (new Integer(st.sval)).intValue();
			st.nextToken();
			simulationTimeTab[numSerie] = (new Integer(st.sval)).intValue();
			st.nextToken();
			nbDtMatrix[numSerie] = (new Integer(st.sval)).intValue();
			st.nextToken();
			savingDtMatrix[numSerie] = (new Integer(st.sval)).intValue();
			if(!((nbDtMatrix[numSerie]%savingDtMatrix[numSerie])==0))
				System.out.println("The number of time steps per year is not a multiple of the number of time steps for saving");
			nbDtSavePerYear[numSerie] = (int) nbDtMatrix[numSerie]/savingDtMatrix[numSerie];
			st.nextToken();
			startingSavingTimeTab[numSerie] = (new Integer(st.sval)).intValue();
			st.nextToken();
			nbSpeciesTab[numSerie] = (new Integer(st.sval)).intValue();
			st.nextToken();
			speciesAreasSizeTab[numSerie] = (new Integer(st.sval)).intValue();
			st.nextToken();
			if((st.sval).equalsIgnoreCase("forcing"))
				isForcing[numSerie] = true;
			else if((st.sval).equalsIgnoreCase("coupling")) 
				isForcing[numSerie] = false;
			else
				System.out.println("In configuration file you have to specify either COUPLING or FORCING");
			configFile.close();
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of configuration file");
			return;
		}




		// initialisation of tables length
		//----OPTION file------
		eggSizeMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		eggWeightMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		growthAgeThresholdMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		predationRateMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		predPreySizesMaxMatrix[numSerie] = new float[nbSpeciesTab[numSerie]][];
		predPreySizesMinMatrix[numSerie] = new float[nbSpeciesTab[numSerie]][];
		criticalPredSuccessMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		starvMaxRateMatrix[numSerie] =  new float[nbSpeciesTab[numSerie]];
		//----SPECIES file------
		nameSpecMatrix[numSerie] = new String[nbSpeciesTab[numSerie]];
		DMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		longevityMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		lInfMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		KMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		t0Matrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		cMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		bPowerMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		alphaMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		maturityMetricMatrix[numSerie] = new String[nbSpeciesTab[numSerie]];
		sizeMatMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		sexRatioMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		sizeFeedingMatrix[numSerie] = new float[nbSpeciesTab[numSerie]][];
		nbStagesMatrix[numSerie] = new int[nbSpeciesTab[numSerie]];
		//----REPRODUCTION file------
		seasonSpawningMatrix[numSerie] = new float[nbSpeciesTab[numSerie]][];
		//----FISHING file------
		recruitSizeMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		recruitAgeMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];
		seasonFishingMatrix[numSerie] = new float[nbSpeciesTab[numSerie]][];
		FMatrix[numSerie] = new float[nbSpeciesTab[numSerie]];

	}

	public void readOptionFile(String optionFileName, int numSerie)
	{
		/*Structure of file optionFile (IN CAPITAL default options)
        ;nbSchools;
        ;eggSizeMatrix ---->*nbSpecies;
        ;eggWeightMatrix ---->*nbSpecies;
        ;growthAgeThresholdMatrix
        ;larvalLength ---->*nbSpecies;
        ;larvalWeightMatrix ---->*nbSpecies;
        ;predationRateMatrix ---->*nbSpecies;
        ;predPreySizeMaxMatrix ---->*nbSpecies;
        ;predPreySizeMinMatrix ---->*nbSpecies;
        ;criticalPredSuccessMatrix ---->*nbSpecies;
        ;stravMaxRateMatrix ---->*nbSpecies;
		 */

		FileInputStream optionFile;
		try
		{
			optionFile=new FileInputStream(new File(inputPathName, optionFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("option file doesn't exist: "+optionFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(optionFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			nbSchools[numSerie] = 1 + Math.round(((new Integer(st.sval)).intValue())/nbDtMatrix[numSerie]);
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				eggSizeMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
			}
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				eggWeightMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
			}
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				growthAgeThresholdMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
			}
			optionFile.close();
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of option file");
			return;
		}
	}

	public void readPredationFile(String predationFileName, int numSerie)
	{
		/*Structure of file predationFile
            ;predationRateMatrix ---->*nbSpecies;
            ;criticalPredSuccessMatrix ---->*nbSpecies;
            ;starvMaxRateMatrix ---->*nbSpecies;
            {
            ; number of stage considered by species;
            ;size threshold between stages;
            ;min and max size ratio per stage ---> * nb stages;
            }---------------> * nbSpecies

            ; predation matrix;

		 */

		FileInputStream predationFile;
		try
		{
			predationFile=new FileInputStream(new File(inputPathName, predationFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("predation file doesn't exist: "+predationFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(predationFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				predationRateMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
			}
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				criticalPredSuccessMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
			}
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				starvMaxRateMatrix[numSerie][i] = (new Float(st.sval)).floatValue();
			}
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				st.nextToken();
				nbStagesMatrix[numSerie][i] = (new Integer(st.sval)).intValue();
				sizeFeedingMatrix[numSerie][i] = new float[nbStagesMatrix[numSerie][i]-1];
				predPreySizesMinMatrix[numSerie][i] = new float[nbStagesMatrix[numSerie][i]];
				predPreySizesMaxMatrix[numSerie][i] = new float[nbStagesMatrix[numSerie][i]];
				for (int s=0; s<nbStagesMatrix[numSerie][i]-1; s++)
				{
					st.nextToken();
					sizeFeedingMatrix[numSerie][i][s] = (new Float(st.sval)).floatValue();
				}
				for (int s=0; s<nbStagesMatrix[numSerie][i]; s++)
				{
					st.nextToken();
					predPreySizesMinMatrix[numSerie][i][s] = (new Float(st.sval)).floatValue();
					st.nextToken();
					predPreySizesMaxMatrix[numSerie][i][s] = (new Float(st.sval)).floatValue();
				}
			}
			predationFile.close();
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of predation file");
			return;
		}
	}

	public void readIndicatorsFile(String indicatorsFileName, int numSerie)
	{
		FileInputStream indicFile;
		try
		{
			indicFile=new FileInputStream(new File(inputPathName, indicatorsFileName));
		}
		catch (FileNotFoundException ex)
		{System.out.println("option file doesn't exist: "+indicatorsFileName);return;}

		Reader r= new BufferedReader(new InputStreamReader(indicFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		try
		{
			st.nextToken();
			calibrationMatrix[numSerie] = false;
			if (st.sval.equalsIgnoreCase("calibration"))
				calibrationMatrix[numSerie] = true;

			st.nextToken();
			TLoutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
			st.nextToken();
			TLDistriboutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
			st.nextToken();
			dietsOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
			st.nextToken();
			meanSizeOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
			st.nextToken();
			sizeSpectrumOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
			st.nextToken();
			sizeSpectrumPerSpeOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
			st.nextToken();
			planktonMortalityOutputMatrix[numSerie] = (new Boolean(st.sval)).booleanValue();
			st.nextToken();
			outputClass0Matrix[numSerie] = (new Boolean(st.sval)).booleanValue();

			indicFile.close();
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of option file");
			return;
		}
	}

	public void initializeOptions()
	{
		//grid = new Grid(this,gridLinesTab[numSerie],gridColumnsTab[numSerie]);    ////**************AJOUTER LES COORDONEES
		grid = new Grid(this,gridLinesTab[numSerie],gridColumnsTab[numSerie],upLeftLatTab[numSerie], 
				upLeftLongTab[numSerie], lowRightLatTab[numSerie], lowRightLongTab[numSerie]);
		if(coastFileNameTab[numSerie].equalsIgnoreCase("None"))
			nbCellsCoastTab[numSerie] = 0;
		else
			initializeCoast();
		initializeMPA();
	}

	public void initializeCoast()
	{
		//read info in file coast
		FileInputStream coastFile;

		try
		{
			coastFile=new FileInputStream(new File(inputPathName, coastFileNameTab[numSerie]));
		}
		catch(FileNotFoundException ex)
		{
			System.out.println("Error while opening coastFile");
			return;
		}
		//read nb of cells and compare to options
		Reader r=new BufferedReader(new InputStreamReader(coastFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);st.slashStarComments(true);
		st.quoteChar(';');
		/*structure of file:
        nb lines;
        nb columns;
        nb Cells for coast(land);
        in lines coord i of coast;
        in lines coord j of coast;
		 */
		try
		{
			st.nextToken();
			if(new Integer(st.sval).intValue()==gridLinesTab[numSerie])
			{
				st.nextToken();
				if (new Integer(st.sval).intValue() == gridColumnsTab[numSerie]) {
					st.nextToken();
					upLeftLatTab[numSerie] = new Float(st.sval).floatValue();
					st.nextToken();
					lowRightLatTab[numSerie] = new Float(st.sval).floatValue();				
					st.nextToken();
					upLeftLongTab[numSerie] = new Float(st.sval).floatValue();					
					st.nextToken();
					lowRightLongTab[numSerie] = new Float(st.sval).floatValue();				
					
					st.nextToken();
					tabCoastiMatrix[numSerie] = new int[new Integer(st.sval).
					                                    intValue()];
					tabCoastjMatrix[numSerie] = new int[new Integer(st.sval).
					                                    intValue()];

					nbCellsCoastTab[numSerie] = new Integer(st.sval).intValue();

					for (int i = 0; i < tabCoastiMatrix[numSerie].length; i++) {

						st.nextToken();
						tabCoastiMatrix[numSerie][i] = (new Integer(st.sval).intValue());
					}

					for (int i = 0; i < tabCoastjMatrix[numSerie].length; i++) {

						st.nextToken();
						tabCoastjMatrix[numSerie][i] = (new Integer(st.sval).intValue());
					}
					for (int i = 0; i < tabCoastiMatrix[numSerie].length; i++)
					{
						grid.matrix[tabCoastiMatrix[numSerie][i]][
						                                          tabCoastjMatrix[numSerie][i]].coast = true;
					}
				}
				else
					System.out.println("Error while reading coastFile for nb columns match");
			}
			else
				System.out.println("Error while reading coastFile for nb lines match");

			coastFile.close();
		}
		catch(IOException ex)
		{
			System.out.println("Error while reading coastFile");
			return;
		}
	}

	public void updateCoastCells(int numSerie)
	{
		for(int i=0;i<tabCoastiMatrix[numSerie].length;i++)
			grid.matrix[tabCoastiMatrix[numSerie][i]][tabCoastjMatrix[numSerie][i]].coast=true;
	}

	public void initializeMPA()
	{
		if(mpaFileNameTab[numSerie].equalsIgnoreCase("None"))
		{
			thereIsMPATab[numSerie] = false;
			tabMPAiMatrix[numSerie]= new int[0];
			tabMPAjMatrix[numSerie]= new int[0];
			MPAtStartTab[numSerie] = 0;
			MPAtEndTab[numSerie] = 0;
		}
		else
		{
			//read info in file mpa
			FileInputStream mpaFile;
			int[] tabi, tabj;
			try
			{
				mpaFile=new FileInputStream(new File(inputPathName, mpaFileNameTab[numSerie]));
			}
			catch(FileNotFoundException ex)
			{
				System.out.println("Error while opening mpaFile");
				return;
			}
			Reader r = new BufferedReader(new InputStreamReader(mpaFile));
			StreamTokenizer st = new StreamTokenizer(r);
			st.slashSlashComments(true);st.slashStarComments(true);
			st.quoteChar(';');
			/*structure of file: (diff with osmose: MPAtStart et MPAtEnd in addition)
	    nb lines;
            nb columns;
	    nbCells of mpa;
	    MPAtstart;MPAtEnd;
	    in lines, coord i of mpa;
	    in lines coord j of mpa;
			 */
			try
			{
				st.nextToken();
				if(new Integer(st.sval).intValue()==gridLinesTab[numSerie])
				{
					st.nextToken();
					if (new Integer(st.sval).intValue() ==gridColumnsTab[numSerie]) {
						st.nextToken();
						tabi = new int[new Integer(st.sval).intValue()];
						tabj = new int[new Integer(st.sval).intValue()];
						st.nextToken();
						MPAtStartTab[numSerie] = new Integer(st.sval).intValue();
						st.nextToken();
						MPAtEndTab[numSerie] = new Integer(st.sval).intValue();
						for (int i = 0; i < tabi.length; i++) {
							st.nextToken();
							tabi[i] = new Integer(st.sval).intValue();
						}
						for (int j = 0; j < tabj.length; j++) {
							st.nextToken();
							tabj[j] = new Integer(st.sval).intValue();
						}
						boolean okForCoast = true;
						for (int i = 0; i < tabi.length; i++)
							if (grid.matrix[tabi[i]][tabj[i]].coast) {
								okForCoast = false;
								break;
							}
						if (okForCoast) {
							tabMPAiMatrix[numSerie] = new int[tabi.length];
							tabMPAjMatrix[numSerie] = new int[tabj.length];
							for (int i = 0; i < tabi.length; i++) {
								tabMPAiMatrix[numSerie][i] = tabi[i];
								tabMPAjMatrix[numSerie][i] = tabj[i];
							}
						} else
							System.out.println(
									"Error while reading mpaFile for coast cells");
						mpaFile.close();
					}
					else
						System.out.println("Error while reading mpaFile for nb columns match");
				}
				else
					System.out.println("Error while reading mpaFile for nb lines match");
			}
			catch(IOException ex)
			{
				System.out.println("Error while reading mpaFile");
				return;
			}
			thereIsMPATab[numSerie] = true;
		}
	}

	public void initializeSpeciesAreas()
	{//0
		//must be done after coast

		//#######################
		//##   RANDOM CASE
		//#######################

		if(areasFileNameTab[numSerie].equalsIgnoreCase("Random"))
		{//1

			simulation.randomDistribution = true;
			if(numSimu==0)
			{//2
				//creation of randomAreaCoordi and j
				randomAreaCoordi = new int[nbSpeciesTab[numSerie]][];
				randomAreaCoordj = new int[nbSpeciesTab[numSerie]][];
				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					randomAreaCoordi[i] = new int[speciesAreasSizeTab[numSerie]];
					randomAreaCoordj[i] = new int[speciesAreasSizeTab[numSerie]];
				}
				//initialise random sorting of distribution areas

				Cell[][] matrix = grid.matrix;
				int nbCasesDispos=((int)(gridLinesTab[numSerie]*gridColumnsTab[numSerie]))-
				nbCellsCoastTab[numSerie];

				//Case where random distribution on the whole (grid-coast)
				if(speciesAreasSizeTab[numSerie]>=nbCasesDispos)
				{//3
					speciesAreasSizeTab[numSerie] = nbCasesDispos;
					for(int i=0;i<nbSpeciesTab[numSerie];i++)
					{
						randomAreaCoordi[i] = new int[speciesAreasSizeTab[numSerie]];
						randomAreaCoordj[i] = new int[speciesAreasSizeTab[numSerie]];
					}
					int index = 0;
					for(int l=0;l<grid.nbLines;l++)
						for(int m=0;m<grid.nbColumns;m++)
							if(!matrix[l][m].coast)
							{
								for(int i=0;i<nbSpeciesTab[numSerie];i++)
								{
									randomAreaCoordi[i][index] = matrix[l][m].posi;
									randomAreaCoordj[i][index] = matrix[l][m].posj;
								}
								index ++;
							}
				}//3

				//case where random disribution on speciesAreasSize cells
				//random sorting of connex cells for each species
				else
				{//4
					for(int i=0;i<nbSpeciesTab[numSerie];i++)
					{//5
						for(int l=0;l<grid.nbLines;l++)
							for(int m=0;m<grid.nbColumns;m++)
								matrix[l][m].alreadyChosen = false;
						Cell[] tabCellsArea = new Cell[speciesAreasSizeTab[numSerie]];
						int coordi, coordj;
						coordi = (int)Math.round(Math.random()*(grid.nbLines-1));
						coordj = (int)Math.round(Math.random()*(grid.nbColumns-1));
						while(matrix[coordi][coordj].coast)
						{
							coordi = (int)Math.round(Math.random()*(grid.nbLines-1));
							coordj = (int)Math.round(Math.random()*(grid.nbColumns-1));
						}
						tabCellsArea[0] = matrix[coordi][coordj];
						matrix[coordi][coordj].alreadyChosen = true;
						//from initial cell, successive random sorting of the adjacent cells..
						//until tabCellsArea is full
						int indice1 = 0;
						int indice2 = 0;
						int index = 0;
						while(index<(tabCellsArea.length-1))
						{//6
							for(int x=indice1;x<=indice2;x++)
								//for each new added cell we test neighbour cells
							{
								int indexNeighbor=0;
								while((index<(tabCellsArea.length-1))&&
										(indexNeighbor<tabCellsArea[x].neighbors.length))
								{
									if((!tabCellsArea[x].neighbors[indexNeighbor].coast)&&
											(!tabCellsArea[x].neighbors[indexNeighbor].alreadyChosen))
									{
										index ++;
										tabCellsArea[index] = tabCellsArea[x].neighbors[indexNeighbor];
										tabCellsArea[x].neighbors[indexNeighbor].alreadyChosen = true;
									}
									indexNeighbor ++;
								}
							}
							indice1 = indice2+1;
							indice2 = index;
						}//6
						for(int m=0;m<tabCellsArea.length;m++)
						{
							randomAreaCoordi[i][m]=tabCellsArea[m].posi;
							randomAreaCoordj[i][m]=tabCellsArea[m].posj;
						}
					}//5
				}//4
			}//2
		}//1

		//#######################
		//##   CASE FILE AREAS
		//#######################

		else {if(numSimu==0)
		{//1
			//areas data are read from file areasFile
			FileInputStream areasFile;

			try
			{
				areasFile=new FileInputStream(new File(inputPathName, areasFileNameTab[numSerie]));
			}
			catch(FileNotFoundException ex)
			{
				System.out.println("Error while opening areasFile");
				return;
			}
			//nbCells are read and compared to options
			Reader r=new BufferedReader(new InputStreamReader(areasFile));
			StreamTokenizer st = new StreamTokenizer(r);
			st.slashSlashComments(true);st.slashStarComments(true);
			st.quoteChar(';');

			/*
	    structure file:
	    nbLines, nbColumns, nbSpecies
	    nbAgeClasses by species----->
            total nb of maps

            n� species
            nb age classes concerned
            nb time steps
            n� of the age classes
            n� of the time steps
            nb cells
            coordinates i
            coordinates j

            etc * nb maps
			 */

			try
			{//2
				st.nextToken();
				if(new Integer(st.sval).intValue()==gridLinesTab[numSerie])
				{//3
					st.nextToken();
					if(new Integer(st.sval).intValue()==gridColumnsTab[numSerie])
					{//3
						st.nextToken();

						if(new Integer(st.sval).intValue()==nbSpeciesTab[numSerie])
						{//4
							boolean okForLongevity=true;
							for(int i=0;i<nbSpeciesTab[numSerie];i++)
							{
								st.nextToken();
								if(new Float(st.sval).floatValue()!=longevityMatrix[numSerie][i]+1)
									okForLongevity=false;
							}

							if(okForLongevity)
							{//5
								numMap = new int[nbSpeciesTab[numSerie]][][];
								int nbMaps;
								st.nextToken();
								nbMaps = new Integer(st.sval).intValue();
								mapCoordi = new int[nbMaps][];
								mapCoordj = new int[nbMaps][];

								int numSpForMap,nbAgePerMap,nbDtPerMap;
								int[] tempAge;
								int[] tempDt;


								for(int i=0;i<nbSpeciesTab[numSerie];i++)
								{
									Species speci = simulation.species[i];
									numMap[i] = new int[speci.tabCohorts.length][];
									for(int j=0;j<speci.tabCohorts.length;j++)
										numMap[i][j] = new int[nbDtMatrix[numSerie]];
								}

								for(int i=0;i<nbMaps;i++)
								{
									st.nextToken();
									numSpForMap = new Integer(st.sval).intValue() - 1;   //because species number between 1 and nbSpecies
									st.nextToken();
									nbAgePerMap = new Integer(st.sval).intValue();
									tempAge = new int[nbAgePerMap];
									st.nextToken();
									nbDtPerMap = new Integer(st.sval).intValue();
									tempDt = new int[nbDtPerMap];
									for(int k=0;k<nbAgePerMap;k++)
									{
										st.nextToken();
										tempAge[k] = new Integer(st.sval).intValue();
									}
									for(int k=0;k<nbDtPerMap;k++)
									{
										st.nextToken();
										tempDt[k] = new Integer(st.sval).intValue() -1;
									}
									st.nextToken();

									mapCoordi[i] = new int[(new Integer(st.sval).intValue())];
									mapCoordj[i] = new int[(new Integer(st.sval).intValue())];

									for(int m=0;m<nbAgePerMap;m++)
										for(int n=0;n<nbDtPerMap;n++)
											for(int h=0;h<nbDtMatrix[numSerie];h++)
											{
												numMap[numSpForMap][tempAge[m]*nbDtMatrix[numSerie]+h][tempDt[n]]=i;
												if(mapCoordi[i].length==0)
													if(!simulation.species[numSpForMap].tabCohorts[(tempAge[m]*nbDtMatrix[numSerie])+h].outOfZoneCohort[tempDt[n]])
														System.out.println("Match error between species areas and migration file");
											}
									for(int m=0;m<mapCoordi[i].length;m++)
									{
										st.nextToken();
										mapCoordi[i][m] = (new Integer(st.sval).intValue());
									}
									for(int m=0;m<mapCoordj[i].length;m++)
									{
										st.nextToken();
										mapCoordj[i][m] = (new Integer(st.sval).intValue());
									}
									for(int m=0;m<mapCoordj[i].length;m++)
									{
										if (grid.matrix[mapCoordi[i][m]][mapCoordj[i][m]].coast)
											System.out.println("Problem of coast in species area file for map "+ m +" - "+ i+" - "+mapCoordi[i][m]+"  "+mapCoordj[i][m]);
									}
									for(int m=0;m<mapCoordj[i].length;m++)
									{
										grid.matrix[mapCoordi[i][m]][mapCoordj[i][m]].nbMapsConcerned++;
										grid.matrix[mapCoordi[i][m]][mapCoordj[i][m]].numMapsConcerned.add(new Integer(m));
									}
								}
								simulation.randomDistribution=false;

							}//5
							else
								System.out.println("Error while reading areasFile for longevities match");
						}//4
						else
							System.out.println("Spatial error while reading file for species number match");
					}//3
					else
						System.out.println("Error while reading areasFile for nb columns match");
				}//3
				else
					System.out.println("Error while reading areasFile for nb lines match");
				areasFile.close();
			}//2
			catch(IOException ex)
			{
				System.out.println("Error while reading areasFile");
				return;
			}
		}//1
		if(numSimu>0)
			simulation.randomDistribution=false;
		}

		//################################################
		//## FOR ALL CASES/ sim0 or not, random or not
		//################################################

		simulation.distributeSpeciesIni();
	}//0

	public void initializeOutOfZoneCarac()
	{

		/*Structure of file seasonalityFile (IN CAPITAL default options)
               ;nb species migrating;
               ;num of species
               ;number of ages;
               ;number of time steps
               ;ages
               ;time steps
               ;mortality
               ...  * nb species migrating
		 */

		FileInputStream outOfZoneFile;
		try
		{
			outOfZoneFile=new FileInputStream(new File(inputPathName, migrationFileNameTab[numSerie]));
		}
		catch (FileNotFoundException ex)
		{System.out.println("migration file doesn't exist: "+migrationFileNameTab[numSerie]);return;}

		Reader r= new BufferedReader(new InputStreamReader(outOfZoneFile));
		StreamTokenizer st = new StreamTokenizer(r);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.quoteChar(';');

		int nbSpOutOfZone,numSpOutOfZone;
		int nbAgePerCase,nbDtPerCase;
		float tempMortality;
		int[] tempAge,tempDt;

		try
		{
			st.nextToken();
			nbSpOutOfZone = (new Integer(st.sval)).intValue();
			if(nbSpOutOfZone==0)
				System.out.println("no species out of zone to be initialized");
			else
			{
				for(int i=0;i<nbSpOutOfZone;i++)
				{
					st.nextToken();
					numSpOutOfZone = (new Integer(st.sval)).intValue();
					st.nextToken();
					nbAgePerCase = new Integer(st.sval).intValue();
					tempAge = new int[nbAgePerCase];
					st.nextToken();
					nbDtPerCase = new Integer(st.sval).intValue();
					tempDt = new int[nbDtPerCase];

					for(int k=0;k<nbAgePerCase;k++)
					{
						st.nextToken();
						tempAge[k] = new Integer(st.sval).intValue();
					}
					for(int k=0;k<nbDtPerCase;k++)
					{
						st.nextToken();
						tempDt[k] = new Integer(st.sval).intValue() -1;
					}

					for(int m=0;m<nbAgePerCase;m++)
					{
						st.nextToken();
						tempMortality = (new Float(st.sval)).floatValue();
						for(int n=0;n<nbDtPerCase;n++)
							for(int h=0;h<nbDtMatrix[numSerie];h++)
							{
								simulation.species[numSpOutOfZone-1].tabCohorts[tempAge[m]*nbDtMatrix[numSerie]+h].outOfZoneCohort[tempDt[n]] = true;
								simulation.species[numSpOutOfZone-1].tabCohorts[tempAge[m]*nbDtMatrix[numSerie]+h].outOfZoneMortality[tempDt[n]] = tempMortality;
							}
					}
				}
			}
		}
		catch(IOException ex)
		{
			System.out.println("Reading error of out of zone species file");
			return;
		}
	}

	public void initializeOutputData()
	{
		//these param are function of the length of the temporal series saved
		if(startingSavingTimeTab[numSerie]>simulationTimeTab[numSerie])
		{
			System.out.println("Starting time for saving higher than simulation time -> set to 0");
			startingSavingTimeTab[numSerie] = 0;    		
		}

		if(startingSavingTimeTab[numSerie]==0)
		{
			timeSeriesLength=simulationTimeTab[numSerie];
			timeSeriesStart=0;
			timeSeriesIsShortened = false;		
		}
		else
		{
			timeSeriesStart=startingSavingTimeTab[numSerie];
			timeSeriesLength=simulationTimeTab[numSerie]-startingSavingTimeTab[numSerie];
			timeSeriesIsShortened=true;	
		}

		if(calibrationMatrix[numSerie])
			BIOMQuadri = new float[nbLoopTab[numSerie]][][][][];

		iniBiomass = new float[nbSeriesSimus][];
		for(int x=0; x<nbSeriesSimus; x++)
			iniBiomass[x] = new float[nbSpeciesTab[numSerie]];
		/*
        if(mortalityOutput)
        {
            nbDTri = new float[nbLoopTab[numSerie]][][][];
            nbFTri = new float[nbLoopTab[numSerie]][][][];
            nbPTri = new float[nbLoopTab[numSerie]][][][];
            nbSTri = new float[nbLoopTab[numSerie]][][][];
        }
		 */
		for(int xx=0;xx<nbLoopTab[numSerie];xx ++)
		{
			if(calibrationMatrix[numSerie])
				BIOMQuadri[xx] = new float[nbSpeciesTab[numSerie]+4][][][];

			iniBiomass[xx] = new float[nbSpeciesTab[numSerie]];
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
				iniBiomass[xx][i] = 0;
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
				for(int j=simulation.species[i].indexAgeClass0;j<simulation.species[i].nbCohorts;j++)
					iniBiomass[xx][i] += (float) simulation.species[i].tabCohorts[j].biomass;
			/*
            if(mortalityOutput)
            {
                nbDTri[xx] = new float[nbSpeciesTab[numSerie]][][];
                nbFTri[xx] = new float[nbSpeciesTab[numSerie]][][];
                nbPTri[xx] = new float[nbSpeciesTab[numSerie]][][];
                nbSTri[xx] = new float[nbSpeciesTab[numSerie]][][];
            }
			 */
			int tempIndex = (int) nbDtMatrix[numSerie]/savingDtMatrix[numSerie];

			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				/*
                if(mortalityOutput)
                {
                    nbDTri[xx][i] = new float[2][];
                    nbFTri[xx][i] = new float[2][];
                    nbPTri[xx][i] = new float[2][];
                    nbSTri[xx][i] = new float[2][];
                }
				 */
				if (calibrationMatrix[numSerie])
				{
					BIOMQuadri[xx][i] = new float[2][][];
					BIOMQuadri[xx][i][0] = new float[timeSeriesLength][];   //without age 0
					BIOMQuadri[xx][i][1] = new float[timeSeriesLength][];   // with age 0
				}
				/*               if(mortalityOutput)
                {
                    nbDTri[xx][i][0] = new float[timeSeriesLength];
                    nbDTri[xx][i][1] = new float[timeSeriesLength];
                    nbFTri[xx][i][0] = new float[timeSeriesLength];
                    nbFTri[xx][i][1] = new float[timeSeriesLength];
                    nbPTri[xx][i][0] = new float[timeSeriesLength];
                    nbPTri[xx][i][1] = new float[timeSeriesLength];
                    nbSTri[xx][i][0] = new float[timeSeriesLength];
                    nbSTri[xx][i][1] = new float[timeSeriesLength];
                }
				 */
				if(calibrationMatrix[numSerie])
					for(int tt=0;tt<timeSeriesLength;tt++)
					{

						BIOMQuadri[xx][i][0][tt] = new float[tempIndex];
						BIOMQuadri[xx][i][1][tt] = new float[tempIndex];

					}
			}
			if(calibrationMatrix[numSerie])
				for(int i=nbSpeciesTab[numSerie];i<nbSpeciesTab[numSerie]+4;i++)
				{
					BIOMQuadri[xx][i] = new float[1][][];
					BIOMQuadri[xx][i][0] = new float[timeSeriesLength][];   //without age 0
					for(int tt=0;tt<timeSeriesLength;tt++)
						BIOMQuadri[xx][i][0][tt] = new float[tempIndex];
				}
		}
	}

	public void saveSerieSimulations(int nSerie)                            // ************************** seuls les fichiers biomasses, abundances, yield,
	//**************************size, mortalites et size spectrum per species sont OK � 100%
	{
		//save in output files
		File targetPath;

		String inputFileName = outputFileNameTab[nSerie]+"_I";
		String biomFileName = outputFileNameTab[nSerie]+"_B.csv";

		targetPath = new File(outputPathName+outputFileNameTab[nSerie]);
		targetPath.mkdirs();

		saveInputParameters(targetPath,inputFileName, nSerie);
		if(calibrationMatrix[nSerie])
			saveBIOMData(targetPath,inputFileName, biomFileName);

	}

	public void saveInputParameters(File targetPath, String inputFileName, int nSerie)
	{

		FileOutputStream inputFile;
		File targetFile;
		PrintWriter pw;
		try
		{
			targetFile = new File(targetPath,inputFileName);
			inputFile = new FileOutputStream(targetFile);
		}
		catch(IOException e)
		{
			System.out.println("Error of input file creation");
			return;
		}
		pw = new PrintWriter(inputFile, true);

		pw.println("// ---- simulation OPTIONS ---- ");
		pw.print("// Lattice Size ");pw.print(';');pw.print(gridLinesTab[nSerie]);
		pw.print('*');pw.println(gridColumnsTab[nSerie]);
		pw.print("//Fish movement");pw.print(';');

		pw.print("//Species areas file");pw.print(';');pw.println(areasFileNameTab[nSerie]);
		if(areasFileNameTab[nSerie].equalsIgnoreCase("Random"))
		{
			pw.print("//Species areas size");pw.print(';');pw.println(speciesAreasSizeTab[nSerie]);
		}
		pw.print("//Coast file");pw.print(';');pw.println(coastFileNameTab[nSerie]);
		if(!coastFileNameTab[nSerie].equalsIgnoreCase("None"))
		{
			pw.print("//Coast size");pw.print(';');pw.println(tabCoastiMatrix[nSerie].length);
		}
		pw.print("//MPA file");pw.print(';');pw.println(mpaFileNameTab[nSerie]);
		if(!mpaFileNameTab[nSerie].equalsIgnoreCase("None"))
		{
			pw.print("//MPA size");pw.print(';');pw.println(tabMPAiMatrix[nSerie].length);
		}
		pw.println();
		pw.println("// ----- BIOLOGICAL PARAMETERS -----   ");
		pw.print("Nb species ");pw.print(';');pw.print(nbSpeciesTab[nSerie]);pw.println(';');
		pw.print("Name ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].name);
		}
		pw.println();
		pw.print("D ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].D);
		}
		pw.println();
		pw.print("F ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].F);
		}
		pw.println();
		pw.print("recruitment age ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].recruitAge);
		}
		pw.println();
		pw.print("longevity ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].longevity);
		}
		pw.println();
		pw.print("lInf ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].lInf);
		}
		pw.println();
		pw.print("K ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].K);
		}
		pw.println();
		pw.print("t0 ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].t0);
		}
		pw.println();
		pw.print("c ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].c);
		}
		pw.println();
		pw.print("b ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].bPower);
		}
		pw.println();

		pw.print("relFecund ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].alpha);
		}
		pw.println();
		pw.print("sizeMat ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].sizeMat);
		}
		pw.println();
		pw.print("eggSize ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].eggSize);
		}
		pw.println();
		pw.print("eggWeight ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].eggWeight);
		}
		pw.println();
		pw.print("sex ratio ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].sexRatio);
		}
		pw.println();

		pw.println("// -----  INITIALISATION ---- ");
		pw.println();

		pw.print("Initial biomass for serie : ");

		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(iniBiomass[nSerie][i]);
		}
		pw.println();
		pw.println("// -----  CALIBRATION ---- ");
		pw.print("Larval mortality ");
		for(int i=0;i<simulation.species.length;i++)
		{
			pw.print(';');pw.print(simulation.species[i].larvalSurvival);
		}
		pw.println();
		pw.print("accessibility coefficient ");
		for(int i=0;i<simulation.couple.nbPlankton;i++)
		{
			pw.print(';');pw.print(simulation.couple.planktonList[i].accessibilityCoeff);
		}
		pw.close();
		System.out.print("Input data saved, ");
	}

	public void saveBIOMData(File targetPath, String inputFileName, String biomFileName)
	{

		FileOutputStream biomFile;
		File targetFile;
		PrintWriter pw;

		float[][][] tabMean,tabCv;
		tabMean = new float[nbSpeciesTab[numSerie]+1+4][][];
		tabCv = new float[nbSpeciesTab[numSerie]+1+4][][];

		for(int i=0;i<=nbSpeciesTab[numSerie]+4;i++)
		{
			tabMean[i] = new float[2][];
			tabMean[i][0] = new float[nbLoopTab[numSerie]];
			tabMean[i][1] = new float[nbLoopTab[numSerie]];
			tabCv[i] = new float[2][];
			tabCv[i][0] = new float[nbLoopTab[numSerie]];
			tabCv[i][1] = new float[nbLoopTab[numSerie]];
		}

		float MEAN,STD,CV;//mean, std et cv des moyennes de chaque simu


		try
		{
			targetFile = new File(targetPath,biomFileName);
			biomFile = new FileOutputStream(targetFile);
		}
		catch(IOException e)
		{
			System.out.println("Error of biomass file creation");
			return;
		}
		pw = new PrintWriter(biomFile, true);
		pw.println("//File containing the set of input parameters "+inputFileName);
		pw.println("//BIOMASS in tonnes");
		for(int xx=0;xx<nbLoopTab[numSerie];xx ++)
		{
			float std;
			float[][] sum = new float[timeSeriesLength][];
			pw.println("SIMULATION n "+xx);
			pw.println();
			//results for tot-0
			for(int t=0;t<timeSeriesLength;t++)
			{
				sum[t] = new float[nbDtSavePerYear[numSerie]];
				for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++)
					sum[t][dt]=0;
			}
			pw.print("TOTAL-0");
			pw.println(';');
			pw.print("Time");
			pw.print(';');

			for(int t=timeSeriesStart;t<simulationTimeTab[numSerie];t++)
				for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
				{
					pw.print((float)(t+dt/(float)nbDtSavePerYear[numSerie]));
					pw.print(';');
				}
			pw.println();

			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				pw.print(nameSpecMatrix[numSerie][i]);pw.print(';');
				for(int t=0;t<timeSeriesLength;t++)
					for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
					{
						sum[t][dt]+=BIOMQuadri[xx][i][0][t][dt];
						pw.print( BIOMQuadri[xx][i][0][t][dt] );
						pw.print(';');
					}
				tabMean[i][0][xx]=mean2(BIOMQuadri[xx][i][0]);
				std=std2(BIOMQuadri[xx][i][0]);
				tabCv[i][0][xx]=std/tabMean[i][0][xx];
				pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[i][0][xx]);pw.print(';');
				pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
				pw.print("cv-0");pw.print(';');pw.print(tabCv[i][0][xx]);pw.print(';');
				pw.println();
			}
			/*   for(int i=nbSpeciesTab[numSerie]; i<nbSpeciesTab[numSerie]+4; i++)
           {
               pw.print("Plankton Gr"+(i-nbSpeciesTab[numSerie]+1));pw.print(';');
               for(int t=0;t<timeSeriesLength;t++)
                   for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
                   {
                       pw.print( BIOMQuadri[xx][i][0][t][dt] );
                       pw.print(';');
                   }
               tabMean[i][0][xx]=mean2(BIOMQuadri[xx][i][0]);
               std=std2(BIOMQuadri[xx][i][0]);
               tabCv[i][0][xx]=std/tabMean[i][0][xx];
               pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[i][0][xx]);pw.print(';');
               pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
               pw.print("cv-0");pw.print(';');pw.print(tabCv[i][0][xx]);pw.print(';');
               pw.println();

           }
			 */
			pw.print("sys-0");pw.print(';');
			for(int t=0;t<timeSeriesLength;t++)
				for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
				{
					pw.print(sum[t][dt]);
					pw.print(';');
				}
			tabMean[nbSpeciesTab[numSerie]+4][0][xx]=mean2(sum);
			std=std2(sum);
			tabCv[nbSpeciesTab[numSerie]+4][0][xx]=std/tabMean[nbSpeciesTab[numSerie]+4][0][xx];
			pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[nbSpeciesTab[numSerie]+4][0][xx]);pw.print(';');
			pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
			pw.print("cv-0");pw.print(';');pw.print(tabCv[nbSpeciesTab[numSerie]+4][0][xx]);pw.print(';');
			pw.println();pw.println();

			//bloc resultats pour total = with age 0
			if(outputClass0Matrix[numSerie])
			{
				for(int t=0;t<timeSeriesLength;t++)
				{
					sum[t] = new float[nbDtSavePerYear[numSerie]];
					for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++)
						sum[t][dt]=0;
				}
				pw.print("TOTAL");
				pw.println(';');
				pw.print("Time");
				pw.print(';');
				for(int t=timeSeriesStart;t<simulationTimeTab[numSerie];t++)
					for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
					{
						pw.print((float)(t+dt/(float)nbDtSavePerYear[numSerie]));
						pw.print(';');
					}
				pw.println();

				for(int i=0;i<nbSpeciesTab[numSerie];i++)
				{
					pw.print(nameSpecMatrix[numSerie][i]);pw.print(';');
					for(int t=0;t<timeSeriesLength;t++)
						for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++)
						{
							sum[t][dt]+=BIOMQuadri[xx][i][1][t][dt];
							pw.print( BIOMQuadri[xx][i][1][t][dt] );
							pw.print(';');
						}
					tabMean[i][1][xx]=mean2(BIOMQuadri[xx][i][1]);
					std=std2(BIOMQuadri[xx][i][1]);
					tabCv[i][1][xx]=std/tabMean[i][1][xx];
					pw.print(';');pw.print("mean");pw.print(';');pw.print(tabMean[i][1][xx]);pw.print(';');
					pw.print("std");pw.print(';');pw.print(std);pw.print(';');
					pw.print("cv");pw.print(';');pw.print(tabCv[i][1][xx]);pw.print(';');
					pw.println();
				}
				/*   for(int i=nbSpeciesTab[numSerie]; i<nbSpeciesTab[numSerie]+4; i++)
               {
                   pw.print("Plankton Gr"+(i-nbSpeciesTab[numSerie]+1));pw.print(';');
                   for(int t=0;t<timeSeriesLength;t++)
                       for(int dt=0;dt<nbDtSavePerYear[numSerie];dt++)
                       {
                           pw.print( BIOMQuadri[xx][i][0][t][dt] );
                           pw.print(';');
                       }
                   tabMean[i][0][xx]=mean2(BIOMQuadri[xx][i][0]);
                   std=std2(BIOMQuadri[xx][i][0]);
                   tabCv[i][0][xx]=std/tabMean[i][0][xx];
                   pw.print(';');pw.print("mean-0");pw.print(';');pw.print(tabMean[i][0][xx]);pw.print(';');
                   pw.print("std-0");pw.print(';');pw.print(std);pw.print(';');
                   pw.print("cv-0");pw.print(';');pw.print(tabCv[i][0][xx]);pw.print(';');
                   pw.println();

               }
				 */
				pw.print("sys");pw.print(';');
				for(int t=0;t<timeSeriesLength;t++)
					for (int dt = 0; dt < nbDtSavePerYear[numSerie]; dt++)
					{
						pw.print(sum[t][dt]);
						pw.print(';');
					}
				tabMean[nbSpeciesTab[numSerie]+4][1][xx]=mean2(sum);
				std=std2(sum);
				tabCv[nbSpeciesTab[numSerie]+4][1][xx]=std/tabMean[nbSpeciesTab[numSerie]+4][1][xx];
				pw.print(';');pw.print("mean");pw.print(';');pw.print(tabMean[nbSpeciesTab[numSerie]+4][1][xx]);pw.print(';');
				pw.print("std");pw.print(';');pw.print(std);pw.print(';');
				pw.print("cv");pw.print(';');pw.print(tabCv[nbSpeciesTab[numSerie]+4][1][xx]);pw.print(';');
				pw.println();pw.println();
			}
		}
		pw.println("SYNTHESIS");
		//tot-0
		pw.print("TOT-0");pw.print(';');pw.print("MEAN(mean-0)");pw.print(';');pw.print("STD(mean-0)");
		pw.print(';');pw.print("CV(mean-0)");pw.print(';');pw.print("MEAN(cv-0)");pw.println(';');
		for(int i=0;i<nbSpeciesTab[numSerie];i++)
		{
			MEAN = mean(tabMean[i][0]);
			STD = std(tabMean[i][0]);
			CV = STD/MEAN;
			pw.print(nameSpecMatrix[numSerie][i]);pw.print(';');pw.print(MEAN);pw.print(';');
			pw.print(STD);pw.print(';');
			pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
		}
		/*     for(int i=nbSpeciesTab[numSerie];i<nbSpeciesTab[numSerie]+4;i++)
       {
           MEAN = mean(tabMean[i][0]);
           STD = std(tabMean[i][0]);
           CV = STD/MEAN;
           pw.print("Plankton group "+(i+1-nbSpeciesTab[numSerie]));pw.print(';');pw.print(MEAN);pw.print(';');
           pw.print(STD);pw.print(';');
           pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
       }

		 */
		MEAN = mean(tabMean[nbSpeciesTab[numSerie]+4][0]);
		STD = std(tabMean[nbSpeciesTab[numSerie]+4][0]);
		CV = STD/MEAN;
		pw.print("SYS-0");pw.print(';');pw.print(MEAN);pw.print(';');pw.print(STD);pw.print(';');
		pw.print(CV);pw.print(';');pw.print(mean(tabCv[nbSpeciesTab[numSerie]+4][0]));pw.println(';');
		pw.println();

		//bloc tot
		if(outputClass0Matrix[numSerie])
		{
			pw.print("TOT");pw.print(';');pw.print("MEAN(mean)");pw.print(';');pw.print("STD(mean)");
			pw.print(';');pw.print("CV(mean)");pw.print(';');pw.print("MEAN(cv)");pw.println(';');
			for(int i=0;i<nbSpeciesTab[numSerie];i++)
			{
				MEAN = mean(tabMean[i][1]);
				STD = std(tabMean[i][1]);
				CV = STD/MEAN;
				pw.print(nameSpecMatrix[numSerie][i]);pw.print(';');pw.print(MEAN);pw.print(';');
				pw.print(STD);pw.print(';');
				pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][1]));pw.println(';');
			}
			/*     for(int i=nbSpeciesTab[numSerie];i<nbSpeciesTab[numSerie]+4;i++)
           {
               MEAN = mean(tabMean[i][0]);
               STD = std(tabMean[i][0]);
               CV = STD/MEAN;
               pw.print("Plankton group "+(i+1-nbSpeciesTab[numSerie]));pw.print(';');pw.print(MEAN);pw.print(';');
               pw.print(STD);pw.print(';');
               pw.print(CV);pw.print(';');pw.print(mean(tabCv[i][0]));pw.println(';');
           }
			 */
			MEAN = mean(tabMean[nbSpeciesTab[numSerie]+4][1]);
			STD = std(tabMean[nbSpeciesTab[numSerie]+4][1]);
			CV = STD/MEAN;
			pw.print("SYS");pw.print(';');pw.print(MEAN);pw.print(';');pw.print(STD);pw.print(';');
			pw.print(CV);pw.print(';');pw.print(mean(tabCv[nbSpeciesTab[numSerie]+4][1]));pw.println(';');

		}
		pw.close();

		System.out.println("biom data saved");
	}

	public float mean(float[] val)
	{
		float mean;
		float sum=0;
		int n=val.length;
		for(int i=0;i<val.length;i++)
			sum+=val[i];
		mean = sum/n;
		return mean;
	}

	public float mean2(float[][] val)
	{
		float mean;
		float sum=0;
		int n=0;
		for(int i=0;i<val.length;i++)
			for(int j=0;j<val[i].length;j++)
			{
				sum += val[i][j];
				n++;
			}
		mean = sum/n;
		return mean;
	}

	public float mean2d(double[][] val)
	{
		float mean;
		float sum=0;
		int n=0;
		for(int i=0;i<val.length;i++)
			for(int j=0;j<val[i].length;j++)
			{
				sum += val[i][j];
				n++;
			}
		mean = sum/n;
		return mean;
	}

	public float std(float[] val)
	{
		float std;
		double sum,sum2;
		sum=0;
		sum2=0;
		int n=val.length;
		for(int i=0;i<val.length;i++)
		{
			sum+=(double)val[i];
			sum2+=(Math.pow((double)val[i],2));
		}
		double numerator,denominator;
		numerator=n*sum2-Math.pow(sum,2);
		denominator=(double)n*(n-1);
		std=(float)Math.sqrt(numerator/denominator);
		return std;
	}

	public float std2(float[][] val)
	{
		float std;
		double sum,sum2;
		sum=0;
		sum2=0;
		int n=0;
		for(int i=0;i<val.length;i++)
			for(int j=0;j<val[i].length;j++)
			{
				sum+=(double)val[i][j];
				sum2+=(Math.pow((double)val[i][j],2));
				n++;
			}
		double numerator,denominator;
		numerator=n*sum2-Math.pow(sum,2);
		denominator=(double)n*(n-1);
		std=(float)Math.sqrt(numerator/denominator);
		return std;
	}

	//Methods of means and std for calculating SBIs, without null values

	public float meanWithout0(float[][] val, float[][] abd)
	{
		float mean;
		float sum=0;
		int n=0;
		for(int i=0;i<val.length;i++)
			for(int j=0;j<val[i].length;j++)
				if (abd[i][j]!=0)
				{
					sum+=val[i][j];
					n++;
				}
		mean = sum/n;
		return mean;
	}

	public float stdWithout0(float[][] val, float[][] abd)
	{
		float std;
		double sum,sum2;
		sum=0;
		sum2=0;
		int n=0;
		for(int i=0;i<val.length;i++)
			for (int j=0;j<val[i].length;j++)
			{
				if (abd[i][j]!=0)
				{
					sum+=(double)val[i][j];
					sum2+=(Math.pow((double)val[i][j],2));
					n++;
				}
			}
		double numerator,denominator;
		numerator=n*sum2-Math.pow(sum,2);
		denominator=(double)n*(n-1);
		std=(float)Math.sqrt(numerator/denominator);
		return std;
	}

	public void runSimulation()
	{

		while(simulation.t < simulationTimeTab[numSerie])
			simulation.step();
	}

	public static void main(String args[])
	{
		System.out.println(new Date());
		Osmose osmose = new Osmose();
		osmose.runSeriesSimulations();
		System.out.println(new Date());
	}
}
