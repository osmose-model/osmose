/*******************************************************************************
 * <p>Titre : Simulation class</p>
 *
 * <p>Description : </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.0
 ******************************************************************************* 
 */
//essai
import java.io.*;

import java.util.*;

class Simulation
{
	Osmose osmose;
	Coupling couple;
	int numSerie, nbDt,savingDt;
	boolean randomDistribution = true;
	int t, dt, dtCount; // years, time steps, for saving

	int nbSpecies, nbSpeciesIni;
	Species[] species;
	School[] tabSchoolsRandom;

	String recruitMetric;
	//spectrum
//	float[] spectrumAbd, spectrumBiom;
	float[][] spectrumSpeciesAbd; //output of size spectrum per species - Stage MORGANE 07-2004
	float[][][] distribTL;  //output of TL distribution per species

	//tables for output of additional data, cf Morgane
	//float[] lMat;

	//table grouping the characteristics of schools caught by species
	float[][] tabSizeCatch, tabNbCatch;
	float[] tabTLCatch;

	long abdTot=0;
	double biomTot = 0;
	long nbDeadTot, nbDeadTotDd, nbDeadTotFf, nbDeadTotPp, nbDeadTotSs;//(for ages>=1)
	long nbDeadTot0, nbDeadTotDd0, nbDeadTotPp0, nbDeadTotSs0, nbDeadTotFf0;

	// for saving
	long[] abdTemp, abdTempWithout0, savingNbYield;
	double[] biomTemp, biomTempWithout0;
	float[] savingYield, meanSizeTemp,meanSizeCatchTemp, meanTLtemp;
	float[][][] spectrumTemp;
	float[][] meanTLperAgeTemp;
	int[][] countTemp;
	double[][] biomPerStage;

	float[][][][] dietsMatrix,predatorsPressureMatrix;
	long[][] nbStomachs;

	// initialisation param for species abd in function of an input size spectrum
	double a,b;	    //coeff of the relation nb=length^a * expb
	//in Rice : a=-5.8; b=35.5*/
	Vector[] specInSizeClass10;	//tab of vectors of species belonging to [0-10[....[140-150[
	long[] abdGapSizeClass10;	//tab of abd to fill in 10cm size class
	long abdIniMin;			//initial min abd of last age class of a species

	boolean targetFishing;
	double RS;		//ratio between mpa and total grid surfaces, RS for Relative Size of MPA

	FileOutputStream dietTime, biomTime, abdTime, TLDistTime, yieldTime, nbYieldTime, meanSizeTime, meanTLTime, SSperSpTime;
	Runtime r=Runtime.getRuntime();
	long freeMem;


//	*******Trophodynamics indicators
	boolean TLoutput;
	boolean TLDistriboutput;
	boolean dietsOutput;
//	*******Size-based indicators
	boolean meanSizeOutput;
	boolean sizeSpectrumOutput;
	boolean sizeSpectrumPerSpeOutput;
//	*******Mortalities data
	boolean planktonMortalityOutput;

	boolean outputClass0;
	boolean calibration;

	public Simulation(Osmose osmose,int nbDt, int savingDt,int nbSpecies,String[] nameSpecies,
			float[] D,float[] F, float[] longevity, float[] lInf,float[] K,float[] t0,float[] c, float[] bPower,
			float[] alpha,float[] sizeMat, int[] nbStages, float[][] sizeFeeding, float[] recruitAge, float[] recruitSize,
			float[][] seasonFishing, String recruitMetric, float[][] seasonSpawning, float[] supAgeOfClass0,
			float[] larvalSurvival, float[] sexRatio, float[] eggSize, float[] eggWeight, float[] growthAgeThreshold, float[] larvalLength, float[] larvalWeight,
			float[] predationRate, float[][] predPreySizesMax, float[][] predPreySizesMin, float[] criticalPredSuccess, float[] starvMaxRate,
			boolean TLoutput, boolean TLDistriboutput, boolean dietsOutput, boolean meanSizeOutput, 
			boolean sizeSpectrumOutput, boolean sizeSpectrumPerSpeOutput, 
			boolean planktonMortalityOutput, boolean calibration, boolean outputClass0, boolean isForcing)
	{


		t=0;
		dt=0;
		dtCount=1;
		osmose.simInitialized = true;
		this.osmose=osmose;
		this.numSerie = osmose.numSerie;
		this.nbDt = nbDt;
		this.savingDt = savingDt;
		this.nbSpecies = nbSpecies;
		this.recruitMetric = recruitMetric;
		nbSpeciesIni=nbSpecies;

		// Initialise plankton matrix
		iniPlanktonField(this, isForcing);

		//CREATION of the SPECIES
		species = new Species[nbSpecies];
		for(int i=0;i<nbSpecies;i++)
			species[i]=new Species(this,i+1,nameSpecies[i],D[i],F[i],longevity[i],lInf[i],K[i],t0[i],
					c[i],bPower[i],alpha[i],sizeMat[i],nbStages[i],sizeFeeding[i],recruitAge[i],recruitSize[i],
					seasonFishing[i], seasonSpawning[i], supAgeOfClass0[i],larvalSurvival[i],
					sexRatio[i],eggSize[i],eggWeight[i],growthAgeThreshold[i],larvalLength[i],larvalWeight[i],
					predationRate[i],predPreySizesMax[i],predPreySizesMin[i],criticalPredSuccess[i],starvMaxRate[i]);

		// determine if fishing is species-based or similar for all species
		targetFishing = false;
		for(int i=1;i<nbSpecies;i++)
			if(species[i].F != species[0].F)
				targetFishing = true;

		//INITIALISATION of SPECIES ABD ACCORDING TO SIZE SPECTRUM
		if(osmose.calibrationMethod[numSerie].equalsIgnoreCase("biomass"))
			iniBySpeciesBiomass();
		else if(osmose.calibrationMethod[numSerie].equalsIgnoreCase("spectrum"))
			iniBySizeSpectrum();
		else if(osmose.calibrationMethod[numSerie].equalsIgnoreCase("random"))
			iniRandomly();

		for(int i=0;i<nbSpecies;i++)
		{
			abdTot += species[i].abundance;
			biomTot += species[i].biomass;
		}

		this.calibration = calibration;

		this.TLoutput = TLoutput;
		this.TLDistriboutput = TLDistriboutput;
		this.dietsOutput = dietsOutput;
		this.meanSizeOutput = meanSizeOutput;
		this.sizeSpectrumOutput = sizeSpectrumOutput;
		this.sizeSpectrumPerSpeOutput = sizeSpectrumPerSpeOutput;
		this.planktonMortalityOutput = planktonMortalityOutput;
		this.outputClass0 = outputClass0;

		// Initialize all the tables required for saving output
		initSaving();

		//Initialisation indicators
		tabSizeCatch = new float[species.length][];
		tabNbCatch = new float[species.length][];
		if (sizeSpectrumPerSpeOutput)
			for(int i=0;i<species.length;i++)
				for(int j=0;j<species[i].tabCohorts.length;j++)
					for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
						((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).rankSize(osmose.tabSizes5,osmose.spectrumMaxSize);
	}

	public void step()
	{
		// screen display to check the period already simulated
		if(t%5==0)
			System.out.println("t"+t+" -> "+new Date());   // t is annual
		else
			System.out.println("t"+t);

		// calculation of relative size of MPA
		if((osmose.thereIsMPATab[numSerie])&&(t==osmose.MPAtStartTab[numSerie]))
		{
			RS = osmose.tabMPAiMatrix[numSerie].length/((osmose.grid.nbLines)*osmose.grid.nbColumns);
			for(int index=0;index<osmose.tabMPAiMatrix[numSerie].length;index++)
				osmose.grid.matrix[osmose.tabMPAiMatrix[numSerie][index]][osmose.tabMPAjMatrix[numSerie][index]].isMPA = true;
		}
		else if((!osmose.thereIsMPATab[numSerie])||(t>osmose.MPAtEndTab[numSerie]))
		{
			RS = 0;
			for(int index=0;index<osmose.tabMPAiMatrix[numSerie].length;index++)
				osmose.grid.matrix[osmose.tabMPAiMatrix[numSerie][index]][osmose.tabMPAjMatrix[numSerie][index]].isMPA = false;
		}


		while(dt <nbDt)   // for each time step dt of the year t
		{
			// clear tables
			for(int i=0;i<species.length;i++)
				for (int j = 0; j < species[i].tabCohorts.length; j++)
				{
					species[i].tabCohorts[j].oldAbundance = species[i].tabCohorts[j].abundance;
					species[i].tabCohorts[j].nbDeadPp = 0;
					species[i].tabCohorts[j].nbDeadSs = 0;
					species[i].tabCohorts[j].nbDeadDd = 0;
					species[i].tabCohorts[j].nbDeadFf = 0;
				}
			// update stages
			for(int i=0;i<species.length;i++)
				for (int j=0;j<species[i].tabCohorts.length;j++)
					for (int k=0; k<species[i].tabCohorts[j].vectSchools.size();k++)
						((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).updateStage(species[i].sizeFeeding,species[i].nbStages);


			// ***** SPATIAL DISTRIBUTION of the species *****

			if(!((dt==0)&&(t==0)))    // because distributeSpeciesIni() used at initialisation
			{
				for (int i=0;i<osmose.grid.nbLines;i++)               // remove all the schools because of the last age class
					for(int j=0;j<osmose.grid.nbColumns;j++)
					{
						osmose.grid.matrix[i][j].vectPresentSchools.removeAllElements();
						osmose.grid.matrix[i][j].nbPresentSchools=0;
					}
				distributeSpecies();      // update distribution
			}

			// ***** ADDITIONAL MORTALITY D *****
			for(int i=0;i<species.length;i++)
			{
				//for all species, D is due to other predators (seals, seabirds)
				//for migrating species, we add mortality because absents during a time step
				//so they don't undergo mortalities due to predation and starvation
				//Additional mortalities for ages 0: no-fecundation of eggs, starvation more pronounced
				//than for sup ages (rel. to CC), predation by other species are not explicit
				if(species[i].tabCohorts[0].abundance!=0)
					species[i].tabCohorts[0].surviveD(species[i].larvalSurvival+(species[i].tabCohorts[0].outOfZoneMortality[dt]/(float)nbDt));     //additional larval mortality
				for(int j=1; j<species[i].tabCohorts.length; j++)
					if(species[i].tabCohorts[j].abundance!=0)
						species[i].tabCohorts[j].surviveD((species[i].D+species[i].tabCohorts[j].outOfZoneMortality[dt])/(float)nbDt);
			}

			// ***** UPDATE LTL DATA *****
			r.gc();
			freeMem=r.freeMemory();

			if((couple.isForcing)&&(t>=couple.startLTLModel))             // if LTL model to be run, run it
			{
				System.out.print(". "+new Date()+"       Free mem = "+freeMem);
				couple.runLTLModel();
				System.out.println("      -> OK ");
			}
			couple.updatePlankton(dt);     // update plankton fields either from LTL run or from data

			// *** PREDATION ***
			randomOrder();
			rankSchoolsSizes();
			calculAccessibleBiom();

			if(t>=osmose.timeSeriesStart)    // save fish biomass before predation process for diets data
			{
				for (int i=0; i<nbSpecies; i++)
					for (int j=0; j< species[i].tabCohorts.length; j++)
						for (int k=0; k<species[i].tabCohorts[j].nbSchools; k++)
							biomPerStage[i][((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).stage] += ((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).biomass;
			}
			if((t>=couple.startLTLModel-1))       // save grid of plankton biomass one year before coupling so forcing mode is also saved
				couple.savePlanktonBiomass();

			for(int i=0;i<tabSchoolsRandom.length;i++)
				if(!tabSchoolsRandom[i].disappears)
				{
					if(!(((Cohort)tabSchoolsRandom[i].cohort).ageNbDt==0))   // eggs do not predate other organisms
						tabSchoolsRandom[i].predation();
				}

			if((!couple.isForcing)&&(t>=couple.startLTLModel))
				couple.calculPlanktonMortality();


			// *** STARVATION MORTALITY ***
			for(int i=0;i<tabSchoolsRandom.length;i++)
				if(!tabSchoolsRandom[i].disappears)
					tabSchoolsRandom[i].surviveP();

			// *** UPDATE of disappeared schools ***
			assessNbSchools();
			assessPresentSchools();
			assessAbdCohSpec();

			// *** GROWTH ***
			for(int i=0;i<species.length;i++)
				if(species[i].abundance!=0)
					species[i].growth();
			for(int i=0;i<species.length;i++)
				species[i].rankSchools();

			// *** FISHING ***
			assessCatchableSchools();

			//Initialisation of indicators tables
			for (int i=0;i<species.length;i++)
			{
				tabSizeCatch[i] = new float[species[i].nbSchoolsTotCatch];
				tabNbCatch[i] = new float[species[i].nbSchoolsTotCatch];
			}
			for(int i=0;i<species.length;i++)
			{
				for (int b=0;b<species[i].nbSchoolsTotCatch;b++)
				{
					tabSizeCatch[i][b] = 0;
					tabNbCatch[i][b] = 0;
				}
			}
			for(int i=0;i<species.length;i++)
				if((species[i].abundance!=0)&&(species[i].seasonFishing[dt] != 0)&&(species[i].F!=0))
				{
					species[i].fishingA();
				}


			// *** UPDATE ***
			nbSpecies=nbSpeciesIni;
			abdTot = 0;
			biomTot = 0;

			for(int i=0;i<species.length;i++)
			{
				species[i].update();
				abdTot += species[i].abundance;
				biomTot += species[i].biomass;
				if(species[i].abundance==0)
					nbSpecies--;
			}

			//update spectra
			if (sizeSpectrumPerSpeOutput)
				for(int i=0;i<osmose.nbSizeClass;i++)
				{
					//spectrumAbd[i]=0;
					//spectrumBiom[i]=0;
					for(int j=0;j<species.length;j++)
						spectrumSpeciesAbd[j][i]=0;
				}

			for(int i=0;i<species.length;i++)
				for(int j=0;j<species[i].tabCohorts.length;j++)
					for(int k=0;k<species[i].tabCohorts[j].vectSchools.size();k++)
					{
						if(sizeSpectrumPerSpeOutput)
							((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).rankSize(osmose.tabSizes5,osmose.spectrumMaxSize);
						if ((t>=osmose.timeSeriesStart)&&((TLoutput)||(TLDistriboutput)))
							((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).rankTL(osmose.tabTL);
					}
			//Stage Morgane - 07-2004  output of indicators
			for(int i=0;i<species.length;i++)
			{
				if (meanSizeOutput)
				{
					species[i].calculSizes();
					species[i].calculSizesCatch();
				}
				if((TLoutput)||(TLDistriboutput))
					species[i].calculTL();
			}

			// *** SAVE THE TIME STEP ***
			saveStep();

			// *** REPRODUCTION ***
			for(int i=0;i<species.length;i++)
				species[i].reproduce();

			if(t>=osmose.timeSeriesStart)
				dtCount++; // for saving
			dt++;
		}
		dt = 0;  //end of the year
		t ++; // go to following year
	}


	public void calculAccessibleBiom()
	{
		for(int i=0;i<species.length;i++)
			for(int j=0;j<species[i].tabCohorts.length;j++)
				if(!species[i].tabCohorts[j].outOfZoneCohort[dt])
					for(int k=0;k<species[i].tabCohorts[j].vectSchools.size();k++)
					{
						((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).biomAccessible=
							0.80*((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).biomass;
						if(((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).biomAccessible==0)
							((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).inaccessible=true;
						else
							((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).inaccessible=false;
					}
	}


	public void iniBySizeSpectrum()                     //************************************* A VERIFIER : � adapter eu nouveau pas de temps si besoin**************************
	//initialisation according to a spectrum [10cm], from 0 to 200cm
	{
		long[] tempSpectrumAbd = new long[20];
		specInSizeClass10 = new Vector[20];    //20 classes size 0 a 200
		for(int i=0;i<specInSizeClass10.length;i++)
			specInSizeClass10[i] = new Vector(nbSpeciesIni);
		abdIniMin = 100;
		//a=-5.8;
		//b=35.5;
		a=osmose.SSslope[numSerie];
		b=osmose.SSintercept[numSerie];
		//Calculation of abd lacking in each size class
		//calculation apart for first size class because minSize=0.05 (and not 0)
		tempSpectrumAbd[0]= Math.round(Math.pow(5.,a)*Math.exp(b));
		for(int i=1;i<20;i++)
			tempSpectrumAbd[i]= Math.round(Math.pow(osmose.tabSizes10[i]+5.,a)*Math.exp(b));
		//tabSizes10[i]+5 is mean length of [tabSizes10[i],tabSizes10[i+1][
		//Sort the Lmax of each species in each size class
		for(int i=0;i<nbSpeciesIni;i++)
		{
			int index1 = tempSpectrumAbd.length-1;
			while(species[i].tabMeanLength[species[i].nbCohorts-1]<osmose.tabSizes10[index1])
				index1 --;
			specInSizeClass10[index1].addElement(species[i]);
		}
		//calculate spectrumMaxIndex
		int spectrumMaxIndex = specInSizeClass10.length-1;
		while(specInSizeClass10[spectrumMaxIndex].size() == 0)
			spectrumMaxIndex --;

		//Calculate abd species and cohorts
		for(int i=spectrumMaxIndex;i>=0;i--)
		{
			for(int j=0;j<specInSizeClass10[i].size();j++)
			{
				Species speciesj = ((Species)specInSizeClass10[i].elementAt(j));
				speciesj.tabAbdIni[speciesj.nbCohorts-1]=Math.round(((double)tempSpectrumAbd[i])/
						specInSizeClass10[i].size());
				speciesj.tabBiomIni[speciesj.nbCohorts-1]=((double)speciesj.tabAbdIni[speciesj.
				                                                                      nbCohorts-1])*speciesj.tabMeanWeight[speciesj.nbCohorts-1]/1000000.;
				//we consider that D0->1 = 10 for the first age class (month or year, whatever nbDt), D0-1year->2 = 1 and D=0.4 otherwise
				//we calculate abd & biom of coh, and in parallel abd & biom of species & we create cohorts
				speciesj.abundance = 0;
				speciesj.biomass = 0;
				speciesj.abundance+=speciesj.tabAbdIni[speciesj.nbCohorts-1];
				speciesj.biomass+=speciesj.tabBiomIni[speciesj.nbCohorts-1];

				for(int k=speciesj.nbCohorts-2;k>=(2*nbDt);k--)
				{
					speciesj.tabAbdIni[k]=Math.round(speciesj.tabAbdIni[k+1]*Math.exp((0.5/(float)nbDt)));
					speciesj.tabBiomIni[k]=((double)speciesj.tabAbdIni[k])*speciesj.
					tabMeanWeight[k]/1000000.;
					speciesj.abundance+=speciesj.tabAbdIni[k];
					speciesj.biomass+=speciesj.tabBiomIni[k];
				}
				int kTemp;
				if(speciesj.longevity<=1)
					kTemp=speciesj.nbCohorts-2;
				else kTemp=(2*nbDt)-1;

				for(int k=kTemp;k>=1;k--)
				{
					speciesj.tabAbdIni[k]=Math.round(speciesj.tabAbdIni[k+1]*Math.exp((1./(float)nbDt)));
					speciesj.tabBiomIni[k]=((double)speciesj.tabAbdIni[k])*speciesj.
					tabMeanWeight[k]/1000000.;
					speciesj.abundance+=speciesj.tabAbdIni[k];
					speciesj.biomass+=speciesj.tabBiomIni[k];
				}

				speciesj.tabAbdIni[0]=Math.round(speciesj.tabAbdIni[1]*Math.exp(10.));
				speciesj.tabBiomIni[0]=((double)speciesj.tabAbdIni[0])*speciesj.
				tabMeanWeight[0]/1000000.;
				speciesj.nbEggs = speciesj.tabAbdIni[0];
				speciesj.abundance += speciesj.tabAbdIni[0];
				speciesj.biomass += speciesj.tabBiomIni[0];
				//creation of the cohorts
				for(int k=0;k<speciesj.nbCohorts;k++)
					speciesj.tabCohorts[k] = new Cohort(speciesj,k,speciesj.tabAbdIni[k],
							speciesj.tabBiomIni[k], speciesj.tabMeanLength[k], speciesj.tabMeanWeight[k]);
			}
		}
	}

	public void iniRandomly()          //************************** Nouvelle option : A faire
	{}

	public void iniBySpeciesBiomass()
	{
		float correctingFactor;
		double abdIni;

		for(int i=0;i<nbSpeciesIni;i++)
		{
			//We calculate abd & biom ini of cohorts, and in parallel biom of species
			Species speci = species[i];
			speci.abundance = 0;
			speci.biomass = 0;
			double sumExp = 0;
			abdIni = osmose.spBiomIniTab[numSerie][i]/(speci.tabMeanWeight[(int)Math.round(speci.nbCohorts/2)]/1000000);

			for(int j=speci.indexAgeClass0;j<speci.nbCohorts;j++)
				sumExp+=Math.exp(- ( j*(speci.D + speci.F + 0.5f)/(float)nbDt )); //0.5 = approximation of average natural mortality (by predation, senecence...)

			speci.tabAbdIni[0] =(long)((abdIni)/(Math.exp(-speci.larvalSurvival/(float)nbDt)*(1+sumExp)));
			speci.tabBiomIni[0]=((double)speci.tabAbdIni[0])*speci.tabMeanWeight[0]/1000000.;
			if (speci.indexAgeClass0<=0)
				speci.biomass+=speci.tabBiomIni[0];

			speci.tabAbdIni[1]=Math.round(speci.tabAbdIni[0]*Math.exp(-speci.larvalSurvival/(float)nbDt));
			speci.tabBiomIni[1]=((double)speci.tabAbdIni[1])*speci.tabMeanWeight[1]/1000000.;
			if (speci.indexAgeClass0<=1)
				speci.biomass+=speci.tabBiomIni[1];

			for(int j=2;j<speci.nbCohorts;j++)
			{
				speci.tabAbdIni[j]=Math.round(speci.tabAbdIni[j-1]*Math.exp(-(speci.D + 0.5f + speci.F)/(float)nbDt));
				speci.tabBiomIni[j]=((double)speci.tabAbdIni[j])*speci.tabMeanWeight[j]/1000000.;
				if (speci.indexAgeClass0<=j)
					speci.biomass+=speci.tabBiomIni[j];
			}
			correctingFactor = (float)(osmose.spBiomIniTab[numSerie][i]/speci.biomass);

			// we make corrections on initial abundance to fit the input biomass
			speci.biomass = 0;

			speci.tabAbdIni[0] =(long)((abdIni*correctingFactor)/(Math.exp(-speci.larvalSurvival/(float)nbDt)*(1+sumExp)));
			speci.tabBiomIni[0]=((double)speci.tabAbdIni[0])*speci.tabMeanWeight[0]/1000000.;
			speci.abundance+=speci.tabAbdIni[0];
			speci.biomass+=speci.tabBiomIni[0];
			speci.nbEggs = speci.tabAbdIni[0];

			speci.tabAbdIni[1]=Math.round(speci.tabAbdIni[0]*Math.exp(-speci.larvalSurvival/(float)nbDt));
			speci.tabBiomIni[1]=((double)speci.tabAbdIni[1])*speci.tabMeanWeight[1]/1000000.;
			speci.abundance+=speci.tabAbdIni[1];
			speci.biomass+=speci.tabBiomIni[1];

			for(int j=2;j<speci.nbCohorts;j++)
			{
				speci.tabAbdIni[j]=Math.round(speci.tabAbdIni[j-1]*Math.exp(-(speci.D + 0.5f + speci.F)/(float)nbDt));
				speci.tabBiomIni[j]=((double)speci.tabAbdIni[j])*speci.tabMeanWeight[j]/1000000.;
				speci.abundance+=speci.tabAbdIni[j];
				speci.biomass+=speci.tabBiomIni[j];
			}
			// and we create the cohorts
			for(int j=0;j<speci.nbCohorts;j++)
			{
				speci.tabCohorts[j] = new Cohort(speci,j,speci.tabAbdIni[j],
						speci.tabBiomIni[j], speci.tabMeanLength[j], speci.tabMeanWeight[j]);
			}
		}
	}

	public void iniPlanktonField(Simulation simu, boolean isForcing)
	{
		couple = new Coupling(simu, isForcing);
		couple.iniCouplingReading();
		if(couple.filesFormat.equalsIgnoreCase("text"))
			couple.configPlanktonTxt(couple.configFileName);
		else{
			if(couple.filesFormat.equalsIgnoreCase("netcdf"))
				couple.configPlanktonNetcdf(couple.configFileName);
			else
				System.out.println("The file format for plankton files is not supported yet");
		}
		couple.initPlanktonMap();
	}

	public void randomOrder()
	{
		//schools are sorted randomly for predation  ---- ALL SCHOOLS
		int capaIni = 30*10*species.length;
		Vector vectSchoolsRandom = new Vector(capaIni,1);
		for(int i=0;i<species.length;i++)
			for(int j=0;j<species[i].tabCohorts.length;j++)
				if(!species[i].tabCohorts[j].outOfZoneCohort[dt])
				{
					for (int k = 0; k < species[i].tabCohorts[j].nbSchools; k++)
						vectSchoolsRandom.addElement(species[i].tabCohorts[j].vectSchools.elementAt(k));
				}

		vectSchoolsRandom.trimToSize();
		tabSchoolsRandom = new School[vectSchoolsRandom.size()];
		int z=0;
		while(z<tabSchoolsRandom.length)
		{
			int random=(int)Math.round((vectSchoolsRandom.size()-1)*Math.random());
			tabSchoolsRandom[z]=(School)vectSchoolsRandom.elementAt(random);
			vectSchoolsRandom.removeElementAt(random);
			z++;
		}
		vectSchoolsRandom.removeAllElements();
		vectSchoolsRandom.trimToSize();

		//schools are sorted randomly for predation  except ages <piscivorousAge (biological age)
		/*        int capaIni = 30*10*species.length;
	Vector vectSchoolsRandom = new Vector(capaIni,1);
	for(int i=0;i<species.length;i++)
	    for(int j=Math.round(species[i].piscivorousAge*nbDt);j<species[i].tabCohorts.length;j++)
                if(!species[i].tabCohorts[j].outOfZoneCohort[dt])
                {
                    for (int k = 0; k < species[i].tabCohorts[j].nbSchools; k++)
                        vectSchoolsRandom.addElement(species[i].tabCohorts[j].vectSchools.elementAt(k));
                }

	vectSchoolsRandom.trimToSize();
	tabSchoolsRandom = new QSchool[vectSchoolsRandom.size()];
	int z=0;
	while(z<tabSchoolsRandom.length)
	{
	    int random=(int)Math.round((vectSchoolsRandom.size()-1)*Math.random());
	    tabSchoolsRandom[z]=(QSchool)vectSchoolsRandom.elementAt(random);
	    vectSchoolsRandom.removeElementAt(random);
	    z++;
	}
	vectSchoolsRandom.removeAllElements();
	vectSchoolsRandom.trimToSize();

	//tabSchoolsRandomCC only contents non pisci schools (age<piscivorous age)
	//no particular order, even not random
	    int nbSchoolsCC = 0;
	    for(int i=0;i<species.length;i++)
	    {
                for(int j=0;j<Math.round(species[i].piscivorousAge*nbDt);j++)
                    for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
                        if(!species[i].tabCohorts[j].outOfZoneCohort[dt])
                            nbSchoolsCC ++;
	    }
	    tabSchoolsRandomCC = new QSchool[nbSchoolsCC];
	    int indexTab=0;
	    for(int i=0;i<species.length;i++)
	    {
                for(int j=0;j<Math.round(species[i].piscivorousAge*nbDt);j++)
                    for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
                        if(!species[i].tabCohorts[j].outOfZoneCohort[dt])
                        {
                            tabSchoolsRandomCC[indexTab]=(QSchool)species[i].tabCohorts[j].vectSchools.elementAt(k);
                            indexTab ++;
                        }
	    }
		 */
	}

	public void rankSchoolsSizes()
	{
		Grid grid = osmose.grid;
		int dummy;
		for(int i=0;i<grid.nbLines;i++)
			for(int j=0;j<grid.nbColumns;j++)
			{
				int[] indexSchoolsSizes=new int[grid.matrix[i][j].nbPresentSchools];
				for(int k=0;k<grid.matrix[i][j].nbPresentSchools;k++)
					indexSchoolsSizes[k]=k;
				for(int k1=0;k1<grid.matrix[i][j].nbPresentSchools;k1++)
					for(int k2=k1+1;k2<grid.matrix[i][j].nbPresentSchools;k2++)
						if(((School)grid.matrix[i][j].vectPresentSchools.elementAt(indexSchoolsSizes[k1])).length >
						((School)grid.matrix[i][j].vectPresentSchools.elementAt(indexSchoolsSizes[k2])).length)
						{
							dummy=indexSchoolsSizes[k1];
							indexSchoolsSizes[k1]=indexSchoolsSizes[k2];
							indexSchoolsSizes[k2]=dummy;
						}
				School[] tabSchoolsTemp = new School[grid.matrix[i][j].nbPresentSchools];
				for(int k=0;k<tabSchoolsTemp.length;k++)
					tabSchoolsTemp[k]=(School)grid.matrix[i][j].vectPresentSchools.elementAt(indexSchoolsSizes[k]);
				grid.matrix[i][j].vectPresentSchools.removeAllElements();
				for(int k=0;k<tabSchoolsTemp.length;k++)
					grid.matrix[i][j].vectPresentSchools.addElement(tabSchoolsTemp[k]);
			}
	}

	//update remaining schools in coh.vectSchools & vectPresentSchools(according to disappears)
	public void assessNbSchools()
	{
		for(int i=0;i<species.length;i++)
			for(int j=0;j<species[i].tabCohorts.length;j++)
			{
				for(int k=species[i].tabCohorts[j].vectSchools.size()-1;k>=0;k--)
					if(((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).disappears)
					{
						species[i].tabCohorts[j].vectSchools.removeElementAt(k);
						species[i].tabCohorts[j].nbSchools--;
					}
				species[i].tabCohorts[j].vectSchools.trimToSize();
			}
	}

	public void assessPresentSchools()
	{
		for(int i=0;i<osmose.grid.nbLines;i++)
			for(int j=0;j<osmose.grid.nbColumns;j++)
			{
				for(int k=osmose.grid.matrix[i][j].vectPresentSchools.size()-1;k>=0;k--)
					if(((School)osmose.grid.matrix[i][j].vectPresentSchools.elementAt(k)).disappears)
					{
						osmose.grid.matrix[i][j].vectPresentSchools.removeElementAt(k);
						osmose.grid.matrix[i][j].nbPresentSchools--;
					}
				osmose.grid.matrix[i][j].vectPresentSchools.trimToSize();
			}
	}

	public void assessAbdCohSpec()
	{
		//update abd cohort and abd species
		for(int i=0;i<species.length;i++)
		{
			species[i].abundance=0;
			for(int j=0;j<species[i].tabCohorts.length;j++)
				species[i].tabCohorts[j].abundance=0;
		}
		for(int i=0;i<species.length;i++)
		{
			for(int j=0;j<species[i].tabCohorts.length;j++)
			{
				for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
				{
					species[i].tabCohorts[j].abundance+=
						((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).abundance;
				}
				species[i].abundance+=species[i].tabCohorts[j].abundance;
			}
		}
	}

	public void distributeSpeciesIni()   //***NEW: correspond to distributeSpecies for initialization
	{
		if(randomDistribution)
		{
			for(int i=0;i<species.length;i++)
			{
				Vector vectCells = new Vector(osmose.randomAreaCoordi[i].length);
				for(int m=0;m<osmose.randomAreaCoordi[i].length;m++)
					vectCells.addElement(osmose.grid.matrix[osmose.randomAreaCoordi[i][m]][osmose.randomAreaCoordj[i][m]]);
				for(int j=0;j<species[i].tabCohorts.length;j++)
					for(int k=0;k<species[i].tabCohorts[j].vectSchools.size();k++)
					{
						((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).distribute(vectCells);
						((School)species[i].tabCohorts[j].vectSchools.elementAt(k)).communicatePosition();
					}
			}
		}
		else//species areas given by file
		{
			for(int i=0;i<species.length;i++)
				for(int j=0;j<species[i].tabCohorts.length;j++)
					if(!species[i].tabCohorts[j].outOfZoneCohort[0])   // 0=at the first time step
					{
						Vector vectCells = new Vector(osmose.mapCoordi[osmose.numMap[i][j][0]].length);
						for (int m = 0; m < osmose.mapCoordi[osmose.numMap[i][j][0]].length; m++)
							vectCells.addElement(osmose.grid.matrix[osmose.mapCoordi[osmose.numMap[i][j][0]][m]][osmose.mapCoordj[osmose.numMap[i][j][0]][m]]);

						for (int k = 0;k < species[i].tabCohorts[j].vectSchools.size(); k++)
						{
							((School) species[i].tabCohorts[j].vectSchools.elementAt(k)).distribute(vectCells);
							((School) species[i].tabCohorts[j].vectSchools.elementAt(k)).communicatePosition();
						}
					}
		}//end file areas
	}

	public void distributeSpecies()
	{
		if(randomDistribution)
		{
			//distribute coh 0 & commPosition for 0 only, the others stay in the same cell
			for(int i=0;i<species.length;i++)
			{
				Vector vectCells = new Vector(osmose.randomAreaCoordi[i].length);
				for(int m=0;m<osmose.randomAreaCoordi[i].length;m++)
					vectCells.addElement(osmose.grid.matrix[osmose.randomAreaCoordi[i][m]][osmose.randomAreaCoordj[i][m]]);
				for(int k=0;k<species[i].tabCohorts[0].vectSchools.size();k++)
				{
					((School)species[i].tabCohorts[0].vectSchools.elementAt(k)).distribute(vectCells);
					((School)species[i].tabCohorts[0].vectSchools.elementAt(k)).communicatePosition();
				}
				for(int j=0;j<species[i].tabCohorts.length;j++)
					for(int k=1;k<species[i].tabCohorts[j].vectSchools.size();k++)
					{
						School thisSchool=(School)(species[i].tabCohorts[j].vectSchools.elementAt(k));
						//  ((QSchool)species[i].tabCohorts[j].vectSchools.elementAt(k)).communicatePosition();
						thisSchool.randomWalk();
						thisSchool.communicatePosition();
					}
			}
		}
		else//species areas given by file
		{
			for(int i=0;i<species.length;i++)
			{
				if(!species[i].tabCohorts[0].outOfZoneCohort[dt])  //distribute coh 0
				{
					Vector vectCellsCoh0 = new Vector();
					for(int j=0;j<osmose.mapCoordi[(osmose.numMap[i][0][dt])].length;j++)
						vectCellsCoh0.addElement(osmose.grid.matrix[osmose.mapCoordi[(osmose.numMap[i][0][dt])][j]]
						                                             [osmose.mapCoordj[(osmose.numMap[i][0][dt])][j]]);

					for(int k=0;k<species[i].tabCohorts[0].vectSchools.size();k++)
					{
						((School)species[i].tabCohorts[0].vectSchools.elementAt(k)).distribute(vectCellsCoh0);
						((School)species[i].tabCohorts[0].vectSchools.elementAt(k)).communicatePosition();
					}
				}

				//compare areas (ages to end): age a, sem2 with age a+1, sem 1
				// if diff, distribute
				for(int j=1;j<species[i].tabCohorts.length;j++)
				{
					int oldTime;
					if(dt==0)
						oldTime=nbDt-1;
					else
						oldTime=dt-1;

					boolean idem = false;
					if(osmose.numMap[i][j][dt]==osmose.numMap[i][j-1][oldTime])
						idem=true;

					if(!idem)// distribute in new area
					{
						if(!species[i].tabCohorts[j].outOfZoneCohort[dt])      //if is in the area during the time step
						{
							Vector vectCellsCoh = new Vector();
							for(int m=0;m<osmose.mapCoordi[(osmose.numMap[i][j][dt])].length;m++)
								vectCellsCoh.addElement(osmose.grid.matrix[osmose.mapCoordi[(osmose.numMap[i][j][dt])][m]]
								                                            [osmose.mapCoordj[(osmose.numMap[i][j][dt])][m]]);

							for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
							{
								School thisSchool=(School)(species[i].tabCohorts[j].vectSchools.elementAt(k));
								if(!species[i].tabCohorts[j-1].outOfZoneCohort[oldTime])//was in the area during the previous time step
								{
									//      osmose.grid.matrix[thisSchool.posi][thisSchool.posj].vectPresentSchools.removeElement(thisSchool);
									//      osmose.grid.matrix[thisSchool.posi][thisSchool.posj].nbPresentSchools --;
								}
								thisSchool.distribute(vectCellsCoh);
								//                          thisSchool.communicatePosition();
							}
						}

						else     //if is NOT in the area during the time step
						{
							if(!species[i].tabCohorts[j-1].outOfZoneCohort[oldTime])//was in the area during the previous time step
								for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
								{
									School thisSchool=(School)(species[i].tabCohorts[j].vectSchools.elementAt(k));
									//    osmose.grid.matrix[thisSchool.posi][thisSchool.posj].vectPresentSchools.removeElement(thisSchool);
									//    osmose.grid.matrix[thisSchool.posi][thisSchool.posj].nbPresentSchools --;
								}
						}
					}
					else    // stay in the same map
					{
						for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
						{
							School thisSchool=(School)(species[i].tabCohorts[j].vectSchools.elementAt(k));

							thisSchool.randomWalk();
							//       thisSchool.communicatePosition();
							boolean stillInMap = false;

							for(int p=0; p< osmose.grid.matrix[thisSchool.posi][thisSchool.posj].nbMapsConcerned;p++)
								if(((Integer)osmose.grid.matrix[thisSchool.posi][thisSchool.posj].numMapsConcerned.elementAt(p)).intValue()==osmose.numMap[i][j][dt])
									stillInMap = true;

							if(!stillInMap)
							{
								Vector vectCellsCoh = new Vector();
								for(int m=0;m<osmose.mapCoordi[(osmose.numMap[i][j][dt])].length;m++)
									vectCellsCoh.addElement(osmose.grid.matrix[osmose.mapCoordi[(osmose.numMap[i][j][dt])][m]]
									                                            [osmose.mapCoordj[(osmose.numMap[i][j][dt])][m]]);

								thisSchool.distribute(vectCellsCoh);
							}
						}

					}
					if(!species[i].tabCohorts[j].outOfZoneCohort[dt])      //if is in the area during the time step
					{
						for(int k=0;k<species[i].tabCohorts[j].nbSchools;k++)
						{
							School thisSchool=(School)(species[i].tabCohorts[j].vectSchools.elementAt(k));
							thisSchool.communicatePosition();
						}
					}

				}//end loop cohort
			}//end loop species
		}//end file areas
	}



	public void assessCatchableSchools()
	{
		if((!osmose.thereIsMPATab[numSerie])||(t<osmose.MPAtStartTab[numSerie])
				||(t>=osmose.MPAtEndTab[numSerie]))// case where no MPA
		{
			for(int i=0;i<species.length;i++)
			{
				species[i].nbSchoolsTotCatch = 0;
				for(int j=0;j<species[i].tabCohorts.length;j++)
				{
					Cohort cohij = species[i].tabCohorts[j];
					cohij.nbSchoolsCatchable = cohij.nbSchools;
					cohij.schoolsCatchable = new Vector(cohij.nbSchoolsCatchable);
					cohij.abundanceCatchable = 0;
					for(int k=0;k<cohij.vectSchools.size();k++)
					{
						School schoolk = (School)cohij.vectSchools.elementAt(k);
						cohij.schoolsCatchable.addElement(schoolk);
						cohij.abundanceCatchable += schoolk.abundance;
						schoolk.isCatchable = true;
					}
				}
				species[i].cumulCatch[0] = 0;
				species[i].cumulCatch[0] = species[i].tabCohorts[0].nbSchoolsCatchable;
				species[i].nbSchoolsTotCatch += species[i].tabCohorts[0].nbSchoolsCatchable;
				for(int j=1;j<species[i].tabCohorts.length;j++)
				{
					species[i].cumulCatch[j] = 0;
					species[i].cumulCatch[j] = species[i].cumulCatch[j-1] + species[i].tabCohorts[j].nbSchoolsCatchable;
					species[i].nbSchoolsTotCatch += species[i].tabCohorts[j].nbSchoolsCatchable;
				}
			}
		}
		else//case MPA
		{
			for(int i=0;i<species.length;i++)
			{
				species[i].nbSchoolsTotCatch = 0;
				for(int j=0;j<species[i].tabCohorts.length;j++)
				{
					Cohort cohij = species[i].tabCohorts[j];
					cohij.nbSchoolsCatchable = 0;
					cohij.schoolsCatchable = new Vector(osmose.nbSchools[numSerie]);
					cohij.abundanceCatchable = 0;
					for(int k=0;k<cohij.vectSchools.size();k++)
					{
						School schoolk = (School)cohij.vectSchools.elementAt(k);
						if(osmose.grid.matrix[schoolk.posi][schoolk.posj].isMPA)
							schoolk.isCatchable = false;
						else
						{
							schoolk.isCatchable = true;
							cohij.nbSchoolsCatchable ++;
							cohij.schoolsCatchable.addElement(schoolk);
							cohij.abundanceCatchable += schoolk.abundance;
						}
					}
					cohij.schoolsCatchable.trimToSize();
				}
				species[i].cumulCatch[0] = 0;
				species[i].cumulCatch[0] = species[i].tabCohorts[0].nbSchoolsCatchable;
				species[i].nbSchoolsTotCatch += species[i].tabCohorts[0].nbSchoolsCatchable;
				for(int j=1;j<species[i].tabCohorts.length;j++)
				{
					species[i].cumulCatch[j] = 0;
					species[i].cumulCatch[j] = species[i].cumulCatch[j-1] + species[i].tabCohorts[j].nbSchoolsCatchable;
					species[i].nbSchoolsTotCatch += species[i].tabCohorts[j].nbSchoolsCatchable;
				}
			}
		}
	}

	public void initSaving()
	{
		abdTemp = new long[species.length];
		abdTempWithout0 = new long[species.length];
		biomTemp = new double[species.length];
		biomTempWithout0 = new double[species.length];
		savingYield = new float[species.length];
		savingNbYield = new long[species.length];
		tabTLCatch = new float[species.length];
		biomPerStage = new double[nbSpecies + couple.nbPlankton][];

		if(meanSizeOutput)
		{
			meanSizeTemp = new float[species.length];
			meanSizeCatchTemp = new float[species.length];
		}
		if(TLoutput)
		{
			meanTLtemp = new float[species.length];
			for(int i=0;i<species.length;i++)
				tabTLCatch[i] = 0;
		}


		for (int i=0;i<species.length;i++)
		{
			abdTemp[i]=0;
			abdTempWithout0[i]=0;
			biomTemp[i]=0;
			biomTempWithout0[i]=0;
			savingYield[i]=0;
			savingNbYield[i]=0;
			biomPerStage[i] = new double[species[i].nbStages];
			for (int j=0; j<species[i].nbStages; j++)
				biomPerStage[i][j] = 0;
			if(meanSizeOutput)
			{
				meanSizeTemp[i] = 0f;
				meanSizeCatchTemp[i] = 0f;
			}
			if(TLoutput)
				meanTLtemp[i] = 0f;
		}
		for (int i=species.length; i<species.length+couple.nbPlankton; i++)
		{
			biomPerStage[i] = new double[1];                  // only & stage per plankton group
			biomPerStage[i][0] = 0;
		}


		if(dietsOutput)
		{
			nbStomachs = new long[species.length][];
			dietsMatrix = new float[species.length][][][];
			predatorsPressureMatrix = new float[species.length][][][];
			for (int i=0; i<species.length; i++)
			{
				nbStomachs[i] = new long[species[i].nbStages];
				dietsMatrix[i] = new float[species[i].nbStages][][];
				predatorsPressureMatrix[i] = new float[species[i].nbStages][][];
				for (int s=0; s<species[i].nbStages; s++)
				{
					nbStomachs[i][s] = 0;
					dietsMatrix[i][s] = new float[species.length+couple.nbPlankton][];
					predatorsPressureMatrix[i][s] = new float[species.length+couple.nbPlankton][];
					for (int j=0; j<species.length; j++)
					{
						dietsMatrix[i][s][j] = new float[species[j].nbStages];
						predatorsPressureMatrix[i][s][j] = new float[species[j].nbStages];
						for (int st=0; st<species[j].nbStages; st++)
						{
							dietsMatrix[i][s][j][st] = 0f;
							predatorsPressureMatrix[i][s][j][st] = 0f;
						}
					}
					for (int j=species.length; j<species.length+couple.nbPlankton; j++)
					{
						dietsMatrix[i][s][j] = new float[1];
						dietsMatrix[i][s][j][0] = 0f;
						predatorsPressureMatrix[i][s][j] = new float[1];
						predatorsPressureMatrix[i][s][j][0] = 0f;
					}
				}
			}
		}

		countTemp = new int[species.length][];
		for (int i = 0;i<species.length;i++)
		{
			countTemp[i] = new int[species[i].nbCohorts];
			for (int j=0;j<species[i].nbCohorts;j++)
				countTemp[i][j] = 0;
		}

		if(TLoutput)
		{
			meanTLperAgeTemp = new float[species.length][];
			for (int i =0; i<species.length;i++)
			{
				meanTLperAgeTemp[i] = new float[species[i].nbCohorts];
				for (int j=0;j<species[i].nbCohorts;j++)
				{
					meanTLperAgeTemp[i][j] = 0;
				}
			}
		}
		if(TLDistriboutput)
		{
			distribTL = new float[species.length][][];
			for (int i=0;i<species.length;i++)
			{
				distribTL[i] = new float[2][];
				distribTL[i][0] = new float[osmose.nbTLClass];    // age 0
				distribTL[i][1] = new float[osmose.nbTLClass];    // without age 0
				for (int j = 0; j < osmose.nbTLClass; j++)
				{
					distribTL[i][0][j] = 0;
					distribTL[i][1][j] = 0;
				}
			}
		}
		//ORGANIZING SIZE CLASSES of the spectrum at INITIALIZATION
		//spectrumAbd = new float[osmose.nbSizeClass];
		//spectrumBiom = new float[osmose.nbSizeClass];

		if(sizeSpectrumPerSpeOutput)
		{
			spectrumSpeciesAbd = new float[species.length][];
			spectrumTemp = new float[2][][];
			spectrumTemp[0] = new float[species.length][];
			spectrumTemp[1] = new float[species.length][];
			for (int i=0;i<species.length;i++)
			{
				spectrumSpeciesAbd[i] = new float[osmose.nbSizeClass];
				spectrumTemp[0][i] = new float[osmose.nbSizeClass];
				spectrumTemp[1][i] = new float[osmose.nbSizeClass];
			}
			//calculation of spectrum values
			for(int i=0;i<osmose.nbSizeClass;i++)
			{
				//   spectrumAbd[i]=0;
				//   spectrumBiom[i]=0;
				for(int j=0;j<species.length;j++)
				{
					spectrumSpeciesAbd[j][i] = 0;
					spectrumTemp[0][j][i] = 0;
					spectrumTemp[1][j][i] = 0;
				}
			}
		}

		initAbdFile();
		initBiomFile();
		initYieldFile();
		initNbYieldFile();

		if(outputClass0)
		{
			initAbd0File();
			initBiom0File();
		}
		if(meanSizeOutput)
		{
			initMeanSizeFile();
			initMeanSizeCatchFile();
		}

		if(TLoutput)
		{
			initMeanTLFile();
			initMeanTLCatchFile();
		}
		if(dietsOutput)
		{
			initDietFile();
			initPredatorPressureFile();
		}
		if(TLDistriboutput)
			initTLDistFile();

		if(sizeSpectrumPerSpeOutput)
		{
			initSizeSpecPerSpFile();
			initSizeSpecPerSpCatchFile();
		}
		if(sizeSpectrumOutput)
			initSizeSpecFile();
	}

	public void saveStep()
	{

		int indexSaving;
		indexSaving = (int)dt/savingDt;
		double biomNo0;
		long abdNo0;

		if(t>=osmose.timeSeriesStart)
		{
			for(int i=0;i<species.length;i++)
			{
				Species speci = species[i];

				abdTemp[i] += speci.abundance;
				biomTemp[i] += speci.biomass;

				if(sizeSpectrumPerSpeOutput)
					for(int j=0;j<osmose.nbSizeClass;j++)
						spectrumTemp[0][i][j]+=spectrumSpeciesAbd[i][j];

				//biom per stage rempli avant la prédation
				/*               for (int j=0; j< speci.tabCohorts.length; j++)
                    for (int k=0; k<speci.tabCohorts[j].nbSchools; k++)
                        biomPerStage[i][((QSchool)speci.tabCohorts[j].vectSchools.elementAt(k)).stage] += ((QSchool)speci.tabCohorts[j].vectSchools.elementAt(k)).biomass;
				 */
				abdNo0 = 0;biomNo0 = 0;
				for(int k=speci.indexAgeClass0;k<speci.tabCohorts.length;k++)
				{
					abdTempWithout0[i] += speci.tabCohorts[k].abundance;
					biomTempWithout0[i] += speci.tabCohorts[k].biomass;
					biomNo0 +=  speci.tabCohorts[k].biomass;
					abdNo0 += speci.tabCohorts[k].abundance;

				}

				if(meanSizeOutput)
				{
					meanSizeTemp[i] += speci.meanSizeSpe*(float)abdNo0;
					meanSizeCatchTemp[i] +=speci.meanSizeSpeCatch*(float)savingNbYield[i];
				}
				if(TLoutput)
				{
					meanTLtemp[i] += speci.meanTLSpe*(float)biomNo0;
					for (int j=0;j<speci.nbCohorts;j++)
						if (speci.meanTLperAge[j]!=0)
						{
							meanTLperAgeTemp[i][j] += speci.meanTLperAge[j];
							countTemp[i][j] += 1;
						}
				}
			}


			if(dtCount==savingDt)
			{
				float timeSaving = (float) t + (dt+(savingDt/2f)+1f)/(float)nbDt;
				timeSaving = t + (dt+1f)/(float)nbDt;
				saveABDperTime(timeSaving,abdTempWithout0);
				saveBIOMperTime(timeSaving, biomTempWithout0);

				if(outputClass0)
				{
					saveABD0perTime(timeSaving,abdTemp);
					saveBIOM0perTime(timeSaving, biomTemp);
				}
				saveYieldperTime(timeSaving,savingYield);
				saveNbYieldperTime(timeSaving,savingNbYield);
				if(meanSizeOutput)
				{
					saveMeanSizeperTime(timeSaving, meanSizeTemp, abdTempWithout0);
					saveMeanSizeCatchperTime(timeSaving, meanSizeCatchTemp, savingNbYield);
				}
				if(TLoutput)
				{
					saveMeanTLperTime(timeSaving, meanTLtemp, biomTempWithout0);
					saveMeanTLCatchperTime(timeSaving, tabTLCatch, savingYield);
					saveMeanTLperAgeperTime(timeSaving,meanTLperAgeTemp,countTemp);
					/*
                   if(osmose.TLoutput)
                   {
                       for (int j=0;j<species[i].nbCohorts;j++)
                           osmose.TLperAgeMatrix[osmose.numSimu][i][j][t-osmose.timeSeriesStart][indexSaving] = meanTLperAgeTemp[i][j]/countTemp[i][j];
                  }
                    if(osmose.TLoutput)
                  {
                      for (int j=0;j<species[i].nbCohorts;j++)
                      {
                          meanTLperAgeTemp[i][j]=0;
                          countTemp[i][j] = 0;
                      }
                  }
					 */
				}
				if(dietsOutput)
				{
					saveDietperTime(timeSaving, dietsMatrix, nbStomachs);
					savePredatorPressureperTime(timeSaving, predatorsPressureMatrix, biomPerStage);
				}
				if(TLDistriboutput)
					saveTLDistperTime(timeSaving, distribTL);

				if(sizeSpectrumOutput)
				{
					saveSizeSpecperTime(timeSaving, spectrumTemp[0]);
				}
				if(sizeSpectrumPerSpeOutput)
				{
					saveSizeSpecPerSpperTime(timeSaving, spectrumTemp[0]);
					saveSizeSpecPerSpperCatchTime(timeSaving, spectrumTemp[1]);
				}
				if(calibration)
					for(int i=0; i<species.length; i++)
					{
						osmose.BIOMQuadri[osmose.numSimu][i][0][t-osmose.timeSeriesStart][indexSaving] = (float)biomTempWithout0[i]/savingDt;
						osmose.BIOMQuadri[osmose.numSimu][i][1][t-osmose.timeSeriesStart][indexSaving] = (float)biomTemp[i]/savingDt;
					}
				for (int i=species.length; i<species.length+couple.nbPlankton; i++)
				{
					if(calibration)
						osmose.BIOMQuadri[osmose.numSimu][i][0][t-osmose.timeSeriesStart][indexSaving] = (float)biomPerStage[i][0]/savingDt;
					biomPerStage[i][0] = 0;
				}

				// clear all saving tables
				for(int i=0;i<species.length;i++)
				{
					abdTemp[i]=0;
					abdTempWithout0[i]=0;
					biomTemp[i]=0;
					biomTempWithout0[i]=0;
					savingYield[i] = 0;
					savingNbYield[i] = 0;
					tabTLCatch[i] = 0;

					if(meanSizeOutput)
					{
						meanSizeTemp[i] = 0;
						meanSizeCatchTemp[i] = 0;
					}

					if(TLoutput)
					{
						meanTLtemp[i] = 0;
						for (int j = 0; j < species[i].nbCohorts; j++)
						{
							meanTLperAgeTemp[i][j] = 0;
							countTemp[i][j] = 0;
						}
					}

					if(dietsOutput)
						for (int s=0; s<species[i].nbStages; s++)
						{
							nbStomachs[i][s] = 0;
							biomPerStage[i][s] = 0;
							for (int j=0; j<species.length; j++)
								for (int st=0; st<species[j].nbStages; st++)
								{
									dietsMatrix[i][s][j][st] = 0f;
									predatorsPressureMatrix[i][s][j][st] = 0f;
								}
							for (int j=species.length; j<species.length+couple.nbPlankton; j++)
							{
								dietsMatrix[i][s][j][0] = 0f;
								predatorsPressureMatrix[i][s][j][0] = 0f;
							}
						}
					if(TLDistriboutput)
						for(int j=0;j<osmose.nbTLClass;j++)
						{
							distribTL[i][0][j] = 0;
							distribTL[i][1][j] = 0;
						}
					if(sizeSpectrumPerSpeOutput)
						for(int j=0;j<osmose.nbSizeClass;j++)
						{
							spectrumTemp[0][i][j]=0;
							spectrumTemp[1][i][j]=0;
						}
				} // end clearing loop over species

				dtCount=0;

			}   // end loop dtcount=dtSaving
		}
	}

	public void initPredatorPressureFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String dietFile = osmose.outputFileNameTab[numSerie]+"_predatorPressureMatrix_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Diets");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, dietFile);
			dietTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(dietTime, true);
		pr.print("Time");pr.print(';');pr.print("Prey");
		for (int i=0;i<species.length;i++)
			for (int s=0;s<species[i].nbStages;s++)
			{
				pr.print(";");
				if(species[i].nbStages==1)
					pr.print(species[i].name);    // Name predators
				else
				{
					if(s==0)
						pr.print(species[i].name+" < "+species[i].sizeFeeding[s]);    // Name predators
					else
						pr.print(species[i].name+" >"+species[i].sizeFeeding[s-1]);    // Name predators
				}
			}
		pr.print(";");
		pr.print("Biomass");
		pr.println();
		pr.close();
	}

	public void initDietFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String dietFile = osmose.outputFileNameTab[numSerie]+"_dietMatrix_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Diets");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, dietFile);
			dietTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(dietTime, true);
		pr.print("Time");pr.print(';');pr.print("Prey");
		for (int i=0;i<species.length;i++)
			for (int s=0;s<species[i].nbStages;s++)
			{
				pr.print(";");
				if(species[i].nbStages==1)
					pr.print(species[i].name);    // Name predators
				else
				{
					if(s==0)
						pr.print(species[i].name+" < "+species[i].sizeFeeding[s]);    // Name predators
					else
						pr.print(species[i].name+" >"+species[i].sizeFeeding[s-1]);    // Name predators
				}
			}
		pr.print(";");
		pr.print("nbStomachs");
		pr.println();
		pr.close();
	}

	public void savePredatorPressureperTime(float time, float[][][][] diets, double[][] biom)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String dietFile = osmose.outputFileNameTab[numSerie]+"_predatorPressureMatrix_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Diets");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, dietFile);
			dietTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(dietTime, true);


		for (int j=0;j<species.length;j++)
			for (int st=0;st<species[j].nbStages;st++)
			{
				pr.print(time);
				pr.print(';');
				if(species[j].nbStages==1)
					pr.print(species[j].name);    // Name predators
					else
					{
						if(st==0)
							pr.print(species[j].name+" < "+species[j].sizeFeeding[st]);    // Name predators
						else
							pr.print(species[j].name+" >"+species[j].sizeFeeding[st-1]);    // Name predators
					}
				pr.print(";");
				for (int i=0;i<species.length;i++)
					for (int s=0;s<species[i].nbStages;s++)
					{
						pr.print(diets[i][s][j][st]/savingDt);
						pr.print(";");
					}
				pr.print(biom[j][st]/savingDt);
				pr.println();
			}
		for (int j=species.length; j<(species.length+couple.nbPlankton); j++)
		{
			pr.print(time); pr.print(";");
			pr.print(couple.planktonNames[j-species.length]);
			pr.print(";");
			for (int i=0;i<species.length;i++)
				for (int s=0;s<species[i].nbStages;s++)       // 4 Stages
				{
					pr.print(diets[i][s][j][0]/savingDt);
					pr.print(";");
				}
			pr.print(biom[j][0]/savingDt);
			pr.println();
		}
		pr.close();
	}

	public void saveDietperTime(float time, float[][][][] diets, long[][] nbStomachs)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String dietFile = osmose.outputFileNameTab[numSerie]+"_dietMatrix_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Diets");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, dietFile);
			dietTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(dietTime, true);


		for (int j=0;j<species.length;j++)
			for (int st=0;st<species[j].nbStages;st++)
			{
				pr.print(time);
				pr.print(';');
				if(species[j].nbStages==1)
					pr.print(species[j].name);    // Name predators
					else
					{
						if(st==0)
							pr.print(species[j].name+" < "+species[j].sizeFeeding[st]);    // Name predators
						else
							pr.print(species[j].name+" >"+species[j].sizeFeeding[st-1]);    // Name predators
					}
				pr.print(";");
				for (int i=0;i<species.length;i++)
					for (int s=0;s<species[i].nbStages;s++)
					{
						if(nbStomachs[i][s]!=0)
							pr.print(diets[i][s][j][st]/(float)nbStomachs[i][s]);
						else
							pr.print("NaN");
						pr.print(";");
					}
				pr.print(nbStomachs[j][st]);
				pr.println();
			}
		for (int j=species.length; j<(species.length+couple.nbPlankton); j++)
		{
			pr.print(time); pr.print(";");
			pr.print(couple.planktonNames[j-species.length]);
			pr.print(";");
			for (int i=0;i<species.length;i++)
				for (int s=0;s<species[i].nbStages;s++)       // 4 Stages
				{
					if(nbStomachs[i][s]!=0)
						pr.print(diets[i][s][j][0]/(float)nbStomachs[i][s]);
					else
						pr.print("NaN");
					pr.print(";");
				}
			pr.println();
		}
		pr.close();
	}

	public void initBiomFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String biomFile = osmose.outputFileNameTab[numSerie]+"_biomass_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, biomFile);
			biomTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(biomTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		/*        for (int i=0;i<couple.nbPlankton;i++)
        {
            pr.print(";");
            pr.print(couple.planktonList[i].name);
        }*/
		pr.println();
		pr.close();
	}

	public void initBiom0File()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String biomFile = osmose.outputFileNameTab[numSerie]+"_biomassClass0_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, biomFile);
			biomTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(biomTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}

		pr.println();
		pr.close();
	}


	public void initAbdFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String abdFile = osmose.outputFileNameTab[numSerie]+"_abundance_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, abdFile);
			abdTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(abdTime, true);

		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		pr.println();
		pr.close();
	}

	public void initAbd0File()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String abdFile = osmose.outputFileNameTab[numSerie]+"_abundanceClass0_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, abdFile);
			abdTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(abdTime, true);

		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		pr.println();
		pr.close();
	}


	public void initYieldFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String yieldFile = osmose.outputFileNameTab[numSerie]+"_yield_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, yieldFile);
			yieldTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(yieldTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		pr.println();
		pr.close();
	}

	public void initNbYieldFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String nbYieldFile = osmose.outputFileNameTab[numSerie]+"_yieldNB_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, nbYieldFile);
			nbYieldTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(nbYieldTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		pr.println();
		pr.close();
	}

	public void initMeanSizeFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanSizeFile = osmose.outputFileNameTab[numSerie]+"_meanSize_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanSizeFile);
			meanSizeTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanSizeTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		pr.println();
		pr.close();
	}

	public void initMeanSizeCatchFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanSizeFile = osmose.outputFileNameTab[numSerie]+"_meanSizeCatch_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanSizeFile);
			meanSizeTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanSizeTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		pr.println();
		pr.close();
	}

	public void initMeanTLFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanTLFile = osmose.outputFileNameTab[numSerie]+"_meanTL_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Trophic");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanTLFile);
			meanTLTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanTLTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}
		pr.println();
		pr.close();
	}

	public void initMeanTLCatchFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanTLFile = osmose.outputFileNameTab[numSerie]+"_meanTLCatch_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Trophic");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanTLFile);
			meanTLTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanTLTime, true);
		pr.print("Time");
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(species[i].name);
		}

		pr.println();
		pr.close();
	}

	public void saveABDperTime(float time, long[] A)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String abdFile = osmose.outputFileNameTab[numSerie]+"_abundance_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, abdFile);
			abdTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(abdTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(A[i]/(float)savingDt);
		}
		pr.println();
		pr.close();
	}
	public void saveABD0perTime(float time, long[] A)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String abdFile = osmose.outputFileNameTab[numSerie]+"_abundanceClass0_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, abdFile);
			abdTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(abdTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(A[i]/(float)savingDt);
		}
		pr.println();
		pr.close();
	}


	public void saveBIOMperTime(float time, double[] B)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String biomFile = osmose.outputFileNameTab[numSerie]+"_biomass_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();
		try {
			targetFile = new File(targetPath, biomFile);
			biomTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(biomTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(B[i]/(float)savingDt);
		}
		pr.println();
		pr.close();
	}

	public void saveBIOM0perTime(float time, double[] B)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String biomFile = osmose.outputFileNameTab[numSerie]+"_biomassClass0_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();
		try {
			targetFile = new File(targetPath, biomFile);
			biomTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(biomTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(B[i]/(float)savingDt);
		}
		pr.println();
		pr.close();
	}


	public void saveYieldperTime(float time, float[] Y)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String yieldFile = osmose.outputFileNameTab[numSerie]+"_yield_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, yieldFile);
			yieldTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(yieldTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(Y[i]/(float)savingDt);
		}
		pr.println();
		pr.close();
	}

	public void saveNbYieldperTime(float time, long[] nY)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String nbYieldFile = osmose.outputFileNameTab[numSerie]+"_yieldNB_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]);
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, nbYieldFile);
			nbYieldTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}
		pr = new PrintWriter(nbYieldTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			pr.print(nY[i]/(float)savingDt);
		}
		pr.println();
		pr.close();
	}

	public void saveMeanSizeperTime(float time, float[] mL, long[] abd)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanSizeFile = osmose.outputFileNameTab[numSerie]+"_meanSize_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanSizeFile);
			meanSizeTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanSizeTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			if(abd[i]!=0)
				pr.print((mL[i]/(float)abd[i]));
			else
				pr.print("NaN");
		}
		pr.println();
		pr.close();
	}

	public void saveMeanSizeCatchperTime(float time, float[] mLY, long[] abd)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanSizeFile = osmose.outputFileNameTab[numSerie]+"_meanSizeCatch_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanSizeFile);
			meanSizeTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanSizeTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			if(abd[i]!=0)
				pr.print((mLY[i]/(float)abd[i]));
			else
				pr.print("NaN");
		}
		pr.println();
		pr.close();
	}

	public void saveMeanTLperTime(float time, float[] mTL, double[] biom)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanTLFile = osmose.outputFileNameTab[numSerie]+"_meanTL_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Trophic");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanTLFile);
			meanTLTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanTLTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			if(biom[i]!=0)
				pr.print((mTL[i]/(float)biom[i]));
			else
				pr.print("NaN");
		}
		pr.println();
		pr.close();
	}

	public void saveMeanTLCatchperTime(float time, float[] mTL, float[] biom)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanTLFile = osmose.outputFileNameTab[numSerie]+"_meanTLCatch_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Trophic");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanTLFile);
			meanTLTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanTLTime, true);

		pr.print(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(";");
			if(biom[i]!=0)
				pr.print((mTL[i]/(float)biom[i]));
			else
				pr.print("NaN");
		}
		pr.println();
		pr.close();
	}

	public void saveMeanTLperAgeperTime(float time, float[][] mTL, int[][] nb)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String meanTLFile = osmose.outputFileNameTab[numSerie]+"_meanTLperAge_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Trophic");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, meanTLFile);
			meanTLTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(meanTLTime, true);

		pr.println(time);
		for (int i=0;i<species.length;i++)
		{
			pr.print(species[i].name);
			for (int j=0;j<species[i].nbCohorts;j++)
			{
				pr.print(";");
				if(nb[i][j]!=0)
					pr.print((mTL[i][j]/(float)nb[i][j]));
				else
					pr.print("NaN");
			}
			pr.println();
		}
		pr.println();
		pr.close();
	}

	public void initTLDistFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String TLDistFile = osmose.outputFileNameTab[numSerie]+"_TLDistrib_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Trophic");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, TLDistFile);
			TLDistTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(TLDistTime, true);

		pr.print("Time");pr.print(';');
		pr.print("TL");pr.print(';');
		for(int i=0;i<species.length;i++)
		{
			pr.print(species[i].name+" -0");
			pr.print(';');
			pr.print(species[i].name+" -1+");
			pr.print(';');
		}
		pr.println();
		pr.close();
	}

	public void saveTLDistperTime(float time, float[][][] TLdist)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String TLDistFile = osmose.outputFileNameTab[numSerie]+"_TLDistrib_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"Trophic");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, TLDistFile);
			TLDistTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(TLDistTime, true);

		for(int j=0;j<osmose.nbTLClass;j++)
		{
			pr.print(time);pr.print(';');
			pr.print((osmose.tabTL[j]));pr.print(';');
			for(int i=0;i<species.length;i++)
			{
				pr.print(TLdist[i][0][j]/(float)savingDt);pr.print(';');
				pr.print(TLdist[i][1][j]/(float)savingDt);pr.print(';');
			}
			pr.println();
		}
		pr.close();
	}

	public void initSizeSpecPerSpFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String SSperSpFile = osmose.outputFileNameTab[numSerie]+"_SizeSpectrumPerSpecies_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, SSperSpFile);
			SSperSpTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(SSperSpTime, true);

		pr.print("Time");pr.print(';');
		pr.print("size");pr.print(';');
		for(int i=0;i<species.length;i++)
		{
			pr.print(species[i].name);
			pr.print(';');
		}
		pr.print("LN(size)");pr.print(';');
		for(int i=0;i<species.length;i++)
		{
			pr.print(species[i].name);
			pr.print(';');
		}
		pr.println();
		pr.close();
	}

	public void saveSizeSpecPerSpperTime(float time, float[][] abdSize)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String SSperSpFile = osmose.outputFileNameTab[numSerie]+"_SizeSpectrumPerSpecies_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, SSperSpFile);
			SSperSpTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(SSperSpTime, true);

		for(int j=0;j<osmose.nbSizeClass;j++)
		{
			pr.print(time);pr.print(';');
			pr.print((osmose.tabSizes5[j]));pr.print(';');
			for(int i=0;i<species.length;i++)
			{
				pr.print(abdSize[i][j]/(float)savingDt);
				pr.print(';');
			}
			pr.print((osmose.tabSizesLn5[j]));pr.print(';');
			for(int i=0;i<species.length;i++)
			{
				pr.print(Math.log(abdSize[i][j]/(float)savingDt));
				pr.print(';');
			}
			pr.println();
		}
		pr.close();
	}

	public void initSizeSpecPerSpCatchFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String SSperSpFile = osmose.outputFileNameTab[numSerie]+"_SizeSpectrumPerSpeciesCatch_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, SSperSpFile);
			SSperSpTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(SSperSpTime, true);

		pr.print("Time");pr.print(';');
		pr.print("size");pr.print(';');
		for(int i=0;i<species.length;i++)
		{
			pr.print(species[i].name);
			pr.print(';');
		}
		pr.println();
		pr.close();
	}

	public void saveSizeSpecPerSpperCatchTime(float time, float[][] abdSize)
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String SSperSpFile = osmose.outputFileNameTab[numSerie]+"_SizeSpectrumPerSpeciesCatch_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, SSperSpFile);
			SSperSpTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(SSperSpTime, true);

		for(int j=0;j<osmose.nbSizeClass;j++)
		{
			pr.print(time);pr.print(';');
			pr.print((osmose.tabSizes5[j]));pr.print(';');
			for(int i=0;i<species.length;i++)
			{
				pr.print(abdSize[i][j]/(float)savingDt);
				pr.print(';');
			}
			pr.println();
		}
		pr.close();
	}


	public void initSizeSpecFile()
	{
		File targetPath, targetFile;
		PrintWriter pr;
		String SSperSpFile = osmose.outputFileNameTab[numSerie]+"_SizeSpectrum_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, SSperSpFile);
			SSperSpTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(SSperSpTime, true);

		pr.print("Time");pr.print(';');
		pr.print("size");pr.print(';');
		pr.print("Abundance");pr.print(';');
		pr.print("LN(size)");pr.print(';');
		pr.print("LN(Abd)");pr.print(';');
		pr.println();
		pr.close();
	}

	public void saveSizeSpecperTime(float time, float[][] abdSize)
	{
		float sum;
		File targetPath, targetFile;
		PrintWriter pr;
		String SSperSpFile = osmose.outputFileNameTab[numSerie]+"_SizeSpectrum_Simu"+osmose.numSimu+".csv";
		targetPath = new File(osmose.outputPathName+osmose.outputFileNameTab[numSerie]+osmose.fileSeparator+"SizeIndicators");
		targetPath.mkdirs();

		try {
			targetFile = new File(targetPath, SSperSpFile);
			SSperSpTime = new FileOutputStream(targetFile, true);
		}
		catch (IOException ie) {
			System.err.println(ie.getMessage());
			return;
		}

		pr = new PrintWriter(SSperSpTime, true);

		for(int j=0;j<osmose.nbSizeClass;j++)
		{
			sum = 0f;
			pr.print(time);pr.print(';');
			pr.print((osmose.tabSizes5[j]));pr.print(';');
			for(int i=0;i<species.length;i++)
			{
				sum+=abdSize[i][j]/(float)savingDt;
			}
			pr.print(sum);pr.print(';');
			pr.print((osmose.tabSizesLn5[j]));pr.print(';');
			pr.print(Math.log(sum));
			pr.println();
		}
		pr.close();
	}

}
