/********************************************************************************
 * <p>Titre : Species class</p>
 *
 * <p>Description : groups species specificities - biological processes </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.0
 ******************************************************************************** 
 */

import java.util.*;

class Species
{

	Simulation simulation;
	int numSerie;

	//DESCRIPTORS

	int number;  		//num of the species [1...12]
	String name;			//name of the species
	long abundance;
	long nbEggs;
	double biomass;		//total biomass in tonnes
	Cohort[] tabCohorts;       //[nb cohorts=(int)Math.round((longevity+1)*simulation.nbDt)
	int nbCohorts;              //=(int)Math.round((longevity+1)*simulation.nbDt
	double SSB;

	//MORGANE 07-2004
	//those attributes are used for the calculation of indicators
	int nbSchoolsTot;    //nb of schools per species
	School[] tabSchoolsRanked, schoolsRanked;  //schools sorted according to their size, dim = nbSchoolsTot
	int nbSchoolsTotCatch;	//nb of schools fished per species
	int[] cumulCatch;           //dim of 3 tables=nbSchoolsTotCatch
	float[] nbSchoolCatch;	//fished schools sorted according to their size
	float[] sizeSchoolCatch;

	float meanSizeSpe;	//mean size per species in the ecosystem
	float meanSizeSpeCatch;	//mean size per species in catches

	float meanTLSpe;
	float[] meanTLperAge;
	float TLeggs = 3f;

	//LIFE HISTORY PARAMETERS

	float D;//D0;		//mortality rates year-1
	float F;
	float longevity;              //in years
	float lInf, K, t0, c, bPower;	//von bertalanffy growth parameters
	float alpha;  		//nb of eggs per gram of mature female
	float sizeMat;
	float recruitAge;            //year
	float supAgeOfClass0;        // year
	int indexAgeClass0;          // index for the table tabCohort, in nbDt
	float recruitSize;
	float larvalSurvival;
	float[] seasonSpawning, seasonFishing; //according to nbDt

	float sexRatio, eggSize, eggWeight, growthAgeThreshold,
	predationRate, criticalPredSuccess, starvMaxRate;
	float[] predPreySizesMax, predPreySizesMin;
	float[] tabMeanWeight;      //tab of mean weights at nbDt*ages
	float[] tabMeanLength;	//tab of mean lengths at nbDt*ages
	float[] minDelta;
	float[] maxDelta;

	float[] deltaMeanLength;

	long[] tabAbdIni;	//tab of abd for initializing the simulation nbDt*age
	double[] tabBiomIni;

	int nbStages;  // stage indirectly correponds to size classes:
	float[] sizeFeeding;

	public Species(Simulation simulation,int number,String name,float D,float F,
			float longevity, float lInf, float K, float t0, float c, float bPower,
			float alpha, float sizeMat, int nbStages, float[] sizeFeeding, float recruitAge, float recruitSize,
			float[] seasonFishing, float[] seasonSpawning, float supAgeOfClass0,
			float larvalSurvival, float sexRatio, float eggSize, float eggWeight, float growthAgeThreshold, 
			float predationRate, float[] predPreySizesMax, float[] predPreySizesMin,float criticalPredSuccess, float starvMaxRate)
	{
		this.simulation=simulation;
		this.numSerie = simulation.numSerie;

		// INITIALISATION of PARAM
		this.number=number;
		this.name=name;
		this.D=D;
		this.F=F;
		this.longevity = longevity;
		this.lInf=lInf;
		this.K=K;
		this.t0=t0;
		this.c = c;
		this.bPower = bPower;
		this.alpha=alpha;
		this.sizeMat=sizeMat;
		this.nbStages = nbStages;
		this.sizeFeeding = sizeFeeding;
		this.recruitAge = recruitAge;
		this.recruitSize = recruitSize;
		this.seasonFishing = seasonFishing;
		this.seasonSpawning = seasonSpawning;
		this.supAgeOfClass0 = supAgeOfClass0;//age from which the species biomass-0 is calculated
		this.indexAgeClass0 = (int)Math.ceil(supAgeOfClass0*simulation.nbDt);      // index of supAgeOfClass0 used in tabCohorts table
		this.larvalSurvival = larvalSurvival;
		this.sexRatio = sexRatio;
		this.eggSize = eggSize;
		this.eggWeight = eggWeight;
		this.growthAgeThreshold = growthAgeThreshold;

		this.predationRate = predationRate;
		this.predPreySizesMax = predPreySizesMax;
		this.predPreySizesMin = predPreySizesMin;
		this.criticalPredSuccess = criticalPredSuccess;
		this.starvMaxRate = starvMaxRate;

		// START INITIALISATION of COHORTS
		nbCohorts = (int)Math.round((longevity+1)*simulation.nbDt);
		tabAbdIni = new long[nbCohorts];
		tabBiomIni = new double[nbCohorts];
		tabCohorts = new Cohort[nbCohorts];
		cumulCatch = new int[nbCohorts];

		// INITIALISATION of TAB for LENGTH and MINMAX of DELTA LENGTH
		tabMeanLength = new float[nbCohorts];
		tabMeanWeight = new float[nbCohorts];

		float decimalAge = 0.0f;
		tabMeanLength[0] = eggSize;
		tabMeanWeight[0] = eggWeight;

		for(int i=1; i<nbCohorts;i++)
		{
			decimalAge = i/(float)simulation.nbDt;
			if(decimalAge<growthAgeThreshold)
			{
				float lengthAtAgePart=(float) (lInf*(1-Math.exp(-K*(growthAgeThreshold-t0))));
				if(lengthAtAgePart<eggSize)
					lengthAtAgePart = eggSize;
				tabMeanLength[i]= decimalAge*(float)(lengthAtAgePart-eggSize)+eggSize ;    // linear growth for the 1st year as von Bertalanffy is not well adapted for the 1st year
			}
			else
				tabMeanLength[i] = (float) (lInf*(1-Math.exp(-K*(decimalAge-t0))));   // von Bertalnffy growth after the first year

			tabMeanWeight[i] = (float)(c*(Math.pow(tabMeanLength[i],bPower)));
			if (tabMeanWeight[i]<eggWeight)
				tabMeanWeight[i]=eggWeight;
		}

		minDelta=new float[nbCohorts];
		maxDelta=new float[nbCohorts];
		deltaMeanLength = new float[nbCohorts];

		for(int i=0; i<nbCohorts-1;i++)
		{
			decimalAge = i/(float)simulation.nbDt;
			deltaMeanLength[i] = tabMeanLength[i+1] - tabMeanLength[i];

			minDelta[i]=deltaMeanLength[i]-deltaMeanLength[i];
			maxDelta[i]=deltaMeanLength[i]+deltaMeanLength[i];
		}


		meanTLperAge = new float[nbCohorts];
	}

	public void growth()   //****NEW: comes from growth1 and growth2
	{

		for(int i=0;i<nbCohorts;i++)
		{
			//this had to be taken into account before the decision to take a straight line between eggs size and size at age 1
			if(i==0)        // eggs
				for(int k=0;k<tabCohorts[0].nbSchools;k++)
				{
					School school0k = (School)tabCohorts[0].vectSchools.elementAt(k);
					school0k.length += deltaMeanLength[i];
					school0k.weight = (float)(c*Math.pow(school0k.length,bPower));
					school0k.biomass=((double)school0k.abundance)*school0k.weight/1000000.;
				}

			if((tabCohorts[i].outOfZoneCohort[simulation.dt]))//||(i==0))

			{
				for(int k=0;k<tabCohorts[i].nbSchools;k++)
				{
					School schoolk = ((School)tabCohorts[i].vectSchools.elementAt(k));
					schoolk.length += deltaMeanLength[i];
					schoolk.weight = (float)(c*Math.pow(schoolk.length,bPower));
					schoolk.biomass = ((double)schoolk.abundance)*schoolk.weight/1000000.;
				}
			}
			else
			{
				if(tabCohorts[i].abundance!=0)
					tabCohorts[i].growth(minDelta[i],maxDelta[i],c,bPower);
			}
		}
	}



	public void reproduce()
	{
		//CALCULATION of Spawning Stock Biomass (SSB) with an update of cohorts biomass
		for(int i=0;i<tabCohorts.length;i++)
		{
			tabCohorts[i].biomass=0;
			for(int j=0;j<tabCohorts[i].nbSchools;j++)
				tabCohorts[i].biomass+=
					((School)tabCohorts[i].vectSchools.elementAt(j)).biomass;
		}
		SSB=0;
		float tempTL=0f;
		int indexMin = 0;
		while ((indexMin < tabSchoolsRanked.length) &&
				(tabSchoolsRanked[indexMin].length < sizeMat))
			indexMin++;
		for (int i = indexMin; i < tabSchoolsRanked.length; i++)
		{
			SSB += tabSchoolsRanked[i].biomass;
			tempTL += tabSchoolsRanked[i].trophicLevel[tabSchoolsRanked[i].cohort.ageNbDt]* tabSchoolsRanked[i].biomass;
		}

		nbEggs=Math.round(sexRatio*alpha*seasonSpawning[simulation.dt]*SSB*1000000);
		if(nbEggs>9100000000000000000l)
			System.out.println("beyond long format for eggs numbers");

		// REMOVING THE OLDEST COHORT from the system

		/*         long nbDeadSenescence = tabCohorts[nbCohorts-1].abundance;           //*************** Pas pris en compte pour l'instant
                                                                                //***************** pas ok
        QCell[][] matrix = simulation.osmose.grid.matrix;
        if(!tabCohorts[nbCohorts-1].outOfZoneCohort[simulation.dt])
            for(int k=0;k<tabCohorts[nbCohorts-1].nbSchools;k++)
            {
                QSchool oldSchoolk = (QSchool)tabCohorts[nbCohorts-1].vectSchools.elementAt(k);
                matrix[oldSchoolk.posi][oldSchoolk.posj].vectPresentSchools.removeElement(oldSchoolk);
                matrix[oldSchoolk.posi][oldSchoolk.posj].nbPresentSchools--;
            }

        for(int i=0;i<simulation.osmose.grid.nbLines;i++)
            for(int j=0;j<simulation.osmose.grid.nbColumns;j++)
                matrix[i][j].vectPresentSchools.trimToSize();

        tabCohorts[nbCohorts-1].vectSchools.removeAllElements();
        tabCohorts[nbCohorts-1].nbSchools=0;
        tabCohorts[nbCohorts-1].vectSchools.trimToSize();
		 */
		//MAKING COHORTS GOING UP to the UPPER AGE CLASS
		//species, age, caseLeftUpAireCoh, tabCasesAireCoh do not change
		for(int i=tabCohorts.length-1;i>0;i--)
		{
			tabCohorts[i].nbSchools=tabCohorts[i-1].nbSchools;
			tabCohorts[i].vectSchools=tabCohorts[i-1].vectSchools;
			tabCohorts[i].abundance=tabCohorts[i-1].abundance;
			tabCohorts[i].biomass=tabCohorts[i-1].biomass;
			tabCohorts[i].nbDead = tabCohorts[i-1].nbDead;
			tabCohorts[i].nbDeadDd = tabCohorts[i-1].nbDeadDd;
			tabCohorts[i].nbDeadPp = tabCohorts[i-1].nbDeadPp;
			tabCohorts[i].nbDeadSs = tabCohorts[i-1].nbDeadSs;
			tabCohorts[i].nbDeadFf = tabCohorts[i-1].nbDeadFf;
			tabCohorts[i].Z = tabCohorts[i-1].Z;
			tabCohorts[i].Dd = tabCohorts[i-1].Dd;
			tabCohorts[i].Pp = tabCohorts[i-1].Pp;
			tabCohorts[i].Ss = tabCohorts[i-1].Ss;
			tabCohorts[i].Ff = tabCohorts[i-1].Ff;
			tabCohorts[i].oldAbundance = tabCohorts[i-1].oldAbundance;

			for(int j=0;j<tabCohorts[i].nbSchools;j++)
				((School)tabCohorts[i].vectSchools.elementAt(j)).cohort=tabCohorts[i];
		}

		//UPDATE AGE CLASS 0
		Cohort coh0 = tabCohorts[0];
		coh0.abundance=nbEggs;
		coh0.biomass=((double)nbEggs)*eggWeight/1000000.;
		coh0.vectSchools=new Vector(simulation.osmose.nbSchools[numSerie]);
		if(nbEggs==0)
			coh0.nbSchools=0;
		else if(nbEggs<simulation.osmose.nbSchools[numSerie])
		{
			coh0.nbSchools=1;
			coh0.vectSchools.addElement(new School(coh0,nbEggs,eggSize,eggWeight));
			((School)coh0.vectSchools.elementAt(0)).biomass=coh0.biomass;
		}
		else if(nbEggs>=simulation.osmose.nbSchools[numSerie])
		{
			coh0.nbSchools=simulation.osmose.nbSchools[numSerie];
			for(int i=0;i<coh0.nbSchools;i++)
				coh0.vectSchools.addElement(new School(coh0,Math.round(((double)coh0.abundance)/(float)coh0.nbSchools),eggSize,eggWeight));
			int surplus=(int)coh0.abundance%coh0.nbSchools;
			((School)(coh0.vectSchools.elementAt(0))).abundance+=surplus;
			for(int i=0;i<coh0.nbSchools;i++)
				((School)coh0.vectSchools.elementAt(i)).biomass=((double)((School)coh0.vectSchools.elementAt(i)).abundance)*eggWeight/1000000.;
		}
		coh0.vectSchools.trimToSize();
		for(int i=0; i<coh0.nbSchools; i++)
			((School)coh0.vectSchools.elementAt(i)).trophicLevel[0] = TLeggs; //tempTL/(float)SSB;
	}

	public void update()
	{
		//UPDATE ABD and BIOMASS of SPECIES
		abundance=0;
		biomass=0;

		/*	for(int i=0; i<tabCohorts.length;i++)
	{
	    abundance+=tabCohorts[i].abundance;
	    biomass+=tabCohorts[i].biomass;
	}
		 */
		for(int i=0; i<tabCohorts.length;i++)
		{
			for (int k=0; k<tabCohorts[i].nbSchools; k++)
			{
				abundance+=((School)tabCohorts[i].vectSchools.elementAt(k)).abundance;
				biomass+=((School)tabCohorts[i].vectSchools.elementAt(k)).biomass;
			}
		}



		if(abundance>9100000000000000000l)
			System.out.println("beyond long format for species abd n�"+name);

		for(int i=0; i<tabCohorts.length;i++)
		{
			for(int j=0; j<tabCohorts[i].nbSchools; j++)
				((School)tabCohorts[i].vectSchools.elementAt(j)).updateStage(sizeFeeding,nbStages);
		}

		if(!(nbCohorts==(int)Math.round((longevity+1)*simulation.nbDt)))
		{
			nbCohorts=(int)Math.round((longevity+1)*simulation.nbDt);
			System.out.println("PB of number of cohorts at the update stage: not equals to the initial number of cohorts");
			// Morgane 03-2007
		}
//		nbCohorts=(longevity+1)*simulation.nbDt;
//		for(int i=0;i<tabCohorts.length;i++)
//		if(tabCohorts[i].abundance==0)
//		this.nbCohorts--;


		// UPDATE LENGTHS and MEAN WEIGHTS of AGE CLASSES
		for(int i=0;i<tabCohorts.length;i++)
			tabCohorts[i].calculMeanGrowth();
	}

	public void fishingA()      // with recruit AGE as metric, calendar age
	{
		long nbSurplusDead = 0;
		int indexRecruitAge = Math.round(recruitAge*simulation.nbDt);

		if(simulation.targetFishing)  //different F per species
		{
			for(int i=indexRecruitAge;i<tabCohorts.length;i++)
				if(tabCohorts[i].abundance!=0)
					nbSurplusDead += tabCohorts[i].fishing1(F*seasonFishing[simulation.dt]);

			if(nbSurplusDead != 0)    //case not enough fish in a cohort -> surplus affected to other cohorts
			{
				Vector vectCohCatchable = new Vector(nbCohorts);
				long abdCatchableTot = 0;
				for(int i=indexRecruitAge;i<tabCohorts.length;i++)
					if(tabCohorts[i].abundance!=0)
					{
						vectCohCatchable.addElement(tabCohorts[i]);
						abdCatchableTot += tabCohorts[i].abundanceCatchable;
					}
				if(nbSurplusDead>=abdCatchableTot)   // not enough fish even in other cohorts
				{
					for(int i=indexRecruitAge;i<tabCohorts.length;i++)
					{
						for(int k=tabCohorts[i].schoolsCatchable.size()-1;k>=0;k--)
						{
							School schoolCatchk = (School)tabCohorts[i].schoolsCatchable.elementAt(k);
							if((simulation.t)>=simulation.osmose.timeSeriesStart){
								simulation.savingYield[number-1] +=
									((float) (schoolCatchk.abundance*schoolCatchk.weight / 1000000));
								simulation.tabTLCatch[number-1] += schoolCatchk.trophicLevel[schoolCatchk.cohort.ageNbDt]*((float) (schoolCatchk.abundance*schoolCatchk.weight / 1000000));
							}
							if(!(tabCohorts[i].outOfZoneCohort[simulation.dt]))
							{
								simulation.osmose.grid.matrix[schoolCatchk.posi]
								                               [schoolCatchk.posj]. vectPresentSchools.removeElement(schoolCatchk);
								simulation.osmose.grid.matrix[schoolCatchk.posi]
								                               [schoolCatchk.posj].nbPresentSchools --;
							}
							tabCohorts[i].vectSchools.removeElement(schoolCatchk);
							tabCohorts[i].nbSchools --;
						}
						tabCohorts[i].abundance -= tabCohorts[i].abundanceCatchable;
						tabCohorts[i].nbDeadFf +=tabCohorts[i].abundanceCatchable;
					}

				}
				else   // enough fish in other cohorts
				{
					long abdToCatch;
					for(int i=0;i<vectCohCatchable.size();i++)
					{
						abdToCatch=Math.round(((double)nbSurplusDead)*
								((Cohort)vectCohCatchable.elementAt(i)).abundanceCatchable/abdCatchableTot);
						((Cohort)vectCohCatchable.elementAt(i)).fishingSurplus(abdToCatch);
					}
				}
			}
		}
		else     //same F per species
			for(int i=indexRecruitAge;i<tabCohorts.length;i++)
				if((tabCohorts[i].abundance!=0)&&(simulation.RS != 1.))
					tabCohorts[i].fishing2((float)(F*seasonFishing[simulation.dt]/(1-simulation.RS)));
	}

	public void fishingL()
	{
//		with recruit LENGTH as metric   -> NEED FISHING1 and FISHING2 IN QSCHOOL (not in QCohort)
//		Not programmed for the moment. If recruitment size in input, transformed at initialisation into recruitment age
	}



	public void rankSchools()      // NEW    morgane 02-2007
	{
		//Sort schools of all ages in the ecosystem according to their length
		nbSchoolsTot = 0;
		for(int j=0;j<tabCohorts.length;j++)
			nbSchoolsTot += tabCohorts[j].nbSchools;
		tabSchoolsRanked = new School[nbSchoolsTot];
		int dummy;
		int index = 0;
		for(int j=0;j<tabCohorts.length;j++)
			for(int k=0;k<tabCohorts[j].nbSchools;k++)
			{
				tabSchoolsRanked[index] = (School)tabCohorts[j].vectSchools.elementAt(k);
				index++;
			}
		int[] indexSchoolsSizes=new int[nbSchoolsTot];
		for(int k=0;k<nbSchoolsTot;k++)
			indexSchoolsSizes[k]=k;
		for(int k1=0;k1<nbSchoolsTot;k1++)
			for(int k2=k1+1;k2<nbSchoolsTot;k2++)
				if(((School)tabSchoolsRanked[indexSchoolsSizes[k1]]).length>
				((School)tabSchoolsRanked[indexSchoolsSizes[k2]]).length)
				{
					dummy=indexSchoolsSizes[k1];
					indexSchoolsSizes[k1]=indexSchoolsSizes[k2];
					indexSchoolsSizes[k2]=dummy;
				}
		School[] tabSchoolsTemp = new School[nbSchoolsTot];
		for(int k=0;k<tabSchoolsTemp.length;k++)
			tabSchoolsTemp[k]=(School)tabSchoolsRanked[indexSchoolsSizes[k]];
		for(int k=0;k<tabSchoolsTemp.length;k++)
			tabSchoolsRanked[k]=(School)tabSchoolsTemp[k];
	}


	// ---------MORGANE 07-2004-----------

	public void calculSizes()		// allows to calculate some SBIs in the ecosystem...   A verifier
	{
		/*	//Sort schools of age 1+ in the ecosystem according to their length
	nbSchoolsTot = 0;
	for(int j=indexAgeClass0;j<tabCohorts.length;j++)
	    nbSchoolsTot += tabCohorts[j].nbSchools;
	schoolsRanked = new QSchool[nbSchoolsTot];
	int dummy;
	int index = 0;
	for(int j=1;j<tabCohorts.length;j++)
	    for(int k=0;k<tabCohorts[j].nbSchools;k++)
	    {
		schoolsRanked[index] = (QSchool)tabCohorts[j].vectSchools.elementAt(k);
		index++;
	    }
	int[] indexSchoolsSizes=new int[nbSchoolsTot];
	for(int k=0;k<nbSchoolsTot;k++)
	    indexSchoolsSizes[k]=k;
	for(int k1=0;k1<nbSchoolsTot;k1++)
	    for(int k2=k1+1;k2<nbSchoolsTot;k2++)
		if(((QSchool)schoolsRanked[indexSchoolsSizes[k1]]).length>
		    ((QSchool)schoolsRanked[indexSchoolsSizes[k2]]).length)
		{
		    dummy=indexSchoolsSizes[k1];
		    indexSchoolsSizes[k1]=indexSchoolsSizes[k2];
		    indexSchoolsSizes[k2]=dummy;
		}
	QSchool[] tabSchoolsTemp = new QSchool[nbSchoolsTot];
	for(int k=0;k<tabSchoolsTemp.length;k++)
	    tabSchoolsTemp[k]=(QSchool)schoolsRanked[indexSchoolsSizes[k]];
	for(int k=0;k<tabSchoolsTemp.length;k++)
		schoolsRanked[k]=(QSchool)tabSchoolsTemp[k];
		 */
		//Calculation of mean size per species
		float abdWithout0;
		meanSizeSpe = 0;
		float sum = 0;
		abdWithout0 = 0;
		for(int j=indexAgeClass0;j<tabCohorts.length;j++)   //we don't consider age class 0 in the calculation of the mean
			for(int k=0;k<tabCohorts[j].nbSchools;k++)
			{
				sum += ((School)tabCohorts[j].vectSchools.elementAt(k)).abundance * ((School)tabCohorts[j].vectSchools.elementAt(k)).length;
				abdWithout0 += ((School)tabCohorts[j].vectSchools.elementAt(k)).abundance;
			}

		if (abdWithout0 != 0)
			meanSizeSpe = sum/abdWithout0;

		//Calculation of max size (according to abd percentages) per species
		/*
	float limit = 0;
	for (int p=0;p<simulation.osmose.nbPercent;p++)
	{
	    float sum2 = 0;
	    int j= schoolsRanked.length;
	    limit = (1- 0.99f + (p*0.01f))*abdWithout0;   //abd representing p% of the total abd without age0
	    while ((sum2<limit)&&(j>0))
	    {
		j--;
		sum2 += ((QSchool)schoolsRanked[j]).abundance;
	    }
	    limit = 0;
	    if((simulation.t>=simulation.osmose.timeSeriesStart)&&(j<schoolsRanked.length))
		simulation.osmose.maxSize[simulation.osmose.numSimu][number-1][p][simulation.t-simulation.osmose.timeSeriesStart] = ((QSchool)schoolsRanked[j]).length;
	}
		 */
		/*
	//Calculation of the proportion of mature fish
	float lMatTemp1 = 0;
	for (int i=0;i<simulation.osmose.nbPercent;i++)
	{
	    float sumMat = 0;
	    lMatTemp1 = (simulation.osmose.lMat[simulation.numSerie][number-1])*(1f+(i*0.1f));
	    for(int j=0;j<nbSchoolsTot;j++)
	    {
		if((((QSchool)schoolsRanked[j]).length) > lMatTemp1)
		{
		    sumMat += ((QSchool)schoolsRanked[j]).abundance;
		}

	    }
	    lMatTemp1 = 0;
	    if((simulation.t>=simulation.osmose.timeSeriesStart)&&(abdWithout0!=0))
		simulation.osmose.propLargeFish[simulation.osmose.numSimu][number-1][i][simulation.t-simulation.osmose.timeSeriesStart] = sumMat/abdWithout0;
	}
		 */    }

	//---------------MORGANE  07-2004

	public void calculSizesCatch()	    //same as previously, but for fished schools
	{
		nbSchoolCatch = new float[nbSchoolsTotCatch];
		sizeSchoolCatch = new float[nbSchoolsTotCatch];
		int dummy;
		int index = 0;
		for(int k=0;k<nbSchoolsTotCatch;k++)
		{
			nbSchoolCatch[index] = simulation.tabNbCatch[number-1][k];
			sizeSchoolCatch[index] = simulation.tabSizeCatch[number-1][k];
			index++;
		}



		/*	int[] indexSchoolsSizes=new int[nbSchoolsTotCatch];
	for(int k=0;k<nbSchoolsTotCatch;k++)
	    indexSchoolsSizes[k]=k;
	for(int k1=0;k1<nbSchoolsTotCatch;k1++)
	    for(int k2=k1+1;k2<nbSchoolsTotCatch;k2++)
		if(sizeSchoolCatch[k1]>sizeSchoolCatch[k2])
		{
		    dummy=indexSchoolsSizes[k1];
		    indexSchoolsSizes[k1]=indexSchoolsSizes[k2];
		    indexSchoolsSizes[k2]=dummy;
		}
	float[][] tabSchoolsCatchTemp = new float[nbSchoolsTotCatch][];
	for (int i=0;i<nbSchoolsTotCatch;i++)
	    tabSchoolsCatchTemp[i] = new float[2];

	for(int k=0;k<tabSchoolsCatchTemp.length;k++)
	{
	    tabSchoolsCatchTemp[k][0]=nbSchoolCatch[indexSchoolsSizes[k]];
	    tabSchoolsCatchTemp[k][1]=sizeSchoolCatch[indexSchoolsSizes[k]];
	}
	for(int k=0;k<tabSchoolsCatchTemp.length;k++)
	{
	    nbSchoolCatch[k]=tabSchoolsCatchTemp[k][0];
	    sizeSchoolCatch[k]=tabSchoolsCatchTemp[k][1];
	}
		 */
		//Calculation of mean size per species
		float abdCatch;
		meanSizeSpeCatch = 0;
		float sumCatch = 0;
		abdCatch = 0;
		for(int j=0;j<nbSchoolsTotCatch;j++)
		{
			sumCatch += nbSchoolCatch[j]*sizeSchoolCatch[j];
			abdCatch += nbSchoolCatch[j];
		}
		if (abdCatch!=0)
			meanSizeSpeCatch = sumCatch/abdCatch;

//		calcul of length frequency in catch
		int sizeTemp = 0;
		for(int j=0;j<nbSchoolsTotCatch;j++)
		{
			if(sizeSchoolCatch[j]<simulation.osmose.spectrumMaxSize)
			{
				sizeTemp = (int) Math.floor(sizeSchoolCatch[j] / 5f);
				simulation.spectrumTemp[1][number -
				                           1][sizeTemp] += nbSchoolCatch[j];
			}
		}


		/*	//Calculation of max size (according to abd percentages) per species

	float limiteCatch = 0;
	for (int p=0;p<simulation.osmose.nbPercent;p++)
	{
	    float sumCatch2 = 0;
	    int j= nbSchoolsTotCatch;
	    limiteCatch = (1 -0.99f +(p*0.01f))*abdCatch;
	    while ((sumCatch2<limiteCatch)&&(j>0))
	    {
		j--;
		sumCatch2 += nbSchoolCatch[j];
	    }
	    limiteCatch = 0;
	    if((simulation.t>=simulation.osmose.timeSeriesStart)&&(j<nbSchoolsTotCatch))
		simulation.osmose.maxSizeCatch[simulation.osmose.numSimu][number-1][p][simulation.t-simulation.osmose.timeSeriesStart] = sizeSchoolCatch[j];
	}

	//Calculation of the proportion of mature fish

	float lMatTemp = 0;
	for (int i=0;i<simulation.osmose.nbPercent;i++)
	{
	    float sumMatCatch = 0;
	    lMatTemp = (simulation.osmose.lMat[simulation.numSerie][number-1])*(1+(i*0.1f));
	    for(int j=0;j<nbSchoolsTotCatch;j++)
	    {
		if((sizeSchoolCatch[j]) > lMatTemp)
		{
		    sumMatCatch += nbSchoolCatch[j];
		}
	    }
	    lMatTemp =0;
	    if((simulation.t>=simulation.osmose.timeSeriesStart)&&(abdCatch!=0))
		simulation.osmose.propLargeFishCatch[simulation.osmose.numSimu][number-1][i][simulation.t-simulation.osmose.timeSeriesStart] = sumMatCatch/abdCatch;
	}*/
	}

	public void calculTL()
	{
		float biomWithout0 = 0;
		float abd = 0;
		float sum = 0;

		// ********** Calcul of mean trophic level of the species, without age 0

		meanTLSpe = 0;

		for(int j=indexAgeClass0;j<tabCohorts.length;j++)   //we don't consider age class 0 in the calculation of the mean
			for(int k=0;k<tabCohorts[j].nbSchools;k++)
				if(((School)tabCohorts[j].vectSchools.elementAt(k)).trophicLevel[j]!=0)
				{
					sum += ((School)tabCohorts[j].vectSchools.elementAt(k)).biomass * ((School)tabCohorts[j].vectSchools.elementAt(k)).trophicLevel[j];
					biomWithout0 += ((School)tabCohorts[j].vectSchools.elementAt(k)).biomass;
				}

		if (biomWithout0 != 0)
			meanTLSpe = sum/biomWithout0;

		// ********* Calcul of mean trophic level per age

		for (int j= 0;j<nbCohorts;j++)
			meanTLperAge[j]=0;

		for(int j=0;j<tabCohorts.length;j++)
		{
			abd = 0;
			sum = 0;
			for(int k=0;k<tabCohorts[j].nbSchools;k++)
				if(((School)tabCohorts[j].vectSchools.elementAt(k)).trophicLevel[j]!=0)
				{
					sum += ((School)tabCohorts[j].vectSchools.elementAt(k)).abundance * ((School)tabCohorts[j].vectSchools.elementAt(k)).trophicLevel[j];
					abd += ((School)tabCohorts[j].vectSchools.elementAt(k)).abundance;
				}
			if (abd != 0)
				meanTLperAge[j] = sum/abd;
		}
		if(nbCohorts!=tabCohorts.length)
			System.out.println("nb= "+nbCohorts+" ,   length = "+tabCohorts.length);
	}

	private void jbInit() throws Exception {
	}
}
