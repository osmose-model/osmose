# OSMOSE Release Notes

## Osmose 4.3.3

### New features

- Adding a weight distribution for the outputs (abundance by weight class)
- Adding outputs for the economic module (accessible and harvested biomass distribution).
- Adding the possibility to save CSV files that allow to verify the proper definition of fish movements.
- Now the parameters `lastYear` and `lastAge` are inclusive. For instance, if `Yinit=2` and `Ylast=5`, the years are `[2, 3, 4, 5]` instead of `[2, 3, 4]` in the previous version.
- Possibility to use NetCDF parameterization for the definition of fish movements.
- Improvement in the management of physical and LTL forcing file (new generic class, `ForcingFile.java`)
- Possibility to deactivate computation of $\F_{O2}$ in the bioen module (`simulation.bioen.fo2.enabled` parameter)
- Possibility to deactivate computation of $\Phi_T$ in the bioen module (`simulation.bioen.phit.enabled` parameter)
- Possibility to deactivate fishing/fishery mortalities (`simulation.fishing.mortality.enabled` parameter)
- Possibility to read fishing period rates from a file (new class for the management of time series, without any constraints on the number of values).
- New parameterization of fish movements using NetCDF (one netcdf per species and age class). Old param has been removed.
- Possibility to use `log` definition of fishing period mortality rates.
- Adding a `simulation.nfisheries` parameter.
- Adding the output of aging mortality
- Adding a test on the number of iterations for MapDistributions (programs stops if school cannot be moved after 1000 tries)
- New computation of the PhiT values
- **New parameterization of genetics parameters. No more based on arrays but species by species, consistently with the new Osmose versions**
- `species.larva2adults.agethres.sp` parameter renamed into `species.first.feeding.age.sp`
- `fisheries.rate.bySeason.fsh%d` parameter renamed into `fisheries.rate.byperiod.fsh%d`
- Normalisation of season spawning to 1.
- Trimming of parameter name in the `Release.findLine` method.
- Reduction of computation time for new fisheries implementation
- Adding the possibility to use a size threshold (``output.cutoff.size.sp#`` parameter


### Bug fixes

- Correction of a bug in the `lengthToAge` method of the `VonBertalanffyGrowth.java` class. Age for `L > Linf` was returned in dt instead of years.
- Correction of a bug in the map movements. At the first time-step within a year, the map was compared with the one of index `nstepyear - 1`. At step=48, map was compared with map 23 instead of map 47.
- Correction of `CatchesByDtByClassFishingMortality` (`fishableBiomass` was not initialized, hence causing malloc errors)
- Correction of the starvation mortality in bioen mode. Starvation applied only if species is older than first feeding age.
- NetCDF output format forced to NetCDF3 instead of NetCDF4 when running in multithread mode (NetCDF4 causes an error and it does not seem possible to do that)
- Correction of a bug in the use of cutoff ages. Cutoff were not properly working with `OutputWholeRegion.java` class.

## Osmose 4.3.2

### New features

- Adding new parameter in bioenergetic module (assimilation)
- Use of life-integrated Enet in the computation of reproduction instead of instantaneous Enet, in order to prevent irrealistic growth.
- Automatic testing of R package (build and check) and Java code (Maven build) using GitHub actions
- Adding the possibility to use plankton variables of dims (time, lat, lon) to init mask variable. Masked if variable is NaN or <= 0
- Adding the possibility to use `ByClassTimeSeries` for proportion in background species.
- Adding an `osmose_calib_demo` function in the R package`
- Adding options to force the model to run an outdated configuration. It avoids running creating a new configuration version for minor releases
- **Remove the use of `grid.java.classname` and force the use of `NcGrid.java` class**
- Adding a species interface (`ISpecies`) to recover some variables that are shared among species (name for instance).
- Adding of all species (preys + background + focal) in the `DietDistribOutput` file.
- Adding of background species in the `DietOutput` and `PredatorPressure` file.
- Adding of lognormal distribution for fishery selectivity (Ricardo)
- Adding some tools to help converting parameter names to version 4.# does this parameter exist?ckage -f "/home/barrier/Codes/osmose/git-osmose/java/pom.xml"
3.0 (i.e adding `species.type`, replace `plankton.plk` by `species.sp`, etc)
- Replace use of hashmaps (costly) by use of arrays, as done before.


### Bugfix

- Correction of a bug in the init. of accessibility manager when varying over time. The index array was hardcoded to `acc` instead of suffix
- Correction of bugs in the init. and use of some arrays (mixing hashmap and indices). **Has no effect if configurations are setup in the old way (focal ranging from 0 to N)**
- Correction of a bug in the saving of ingestion (egross was saved instead of energy)
- Correction of a bug in the saving of fisheries: mean instead of cumulated sum was saved. Revealed by Ghassen.
- Correction of a bug in the `AbstractDistribOutput` and `DietDistribOutput` (`cpt` increment not initialized in the right place).
- Correction of a bug in the extraction of OutMortality (problem due to hashmap init).
- Correction of a bug in the saving of SpatialOutputs for LTL (problem with indexing)
- Correction of bugs in the reading of resources: problem with file path reconstruction
- Correction of bugs in the reading of resources: bad recovery of the nc indes. Corrected by adding a parameter (ncstep / year). Revealed by Ekin.
- Correction of a bug in the ResourceOutput class. HashMap variables were not initialized + problem of indexing
- Put the `initTimeMapping` method of `ResourceForcing.java` inside the condition. Caused an error if no NetCDF used.
- Correct a bug in the accessibility recovery (mixing preys and predators)
- Set the `compile.on.save` Netbeans parameter to `false`. When `true`, compilation may work even if code is bugged.
- Correct a potential bug in `eliminateTwinAccess` for `AccessibilityManager.java`. Did work only for index ranging from [0, N-1]. Now should work for all indexing


### Misc.

- Correction of ref. config in the R comments.
- Update of README to add vignettes
- Change build name from osmose**-**X.Y.Z.jar to osmose**_**X.Y.Z.jar
- Change of LICENSE (move from CECILL to GPL-3) for future connection with Zenodo + change in License Java headers

## Osmose 4.3.1

### Bugfix

- Correction of a bug in the position of background species. Position was not init. so all the schools were at cell (0, 0)

## Osmose 4.3.0

### New features

- Moving Java compilation to Maven
- New parameterization of fisheries (fishing period, discards, catchability matrix)
- Standardized parameterization of resource and background species forcings (netcdf file containing biomass)
- New parameterization of background and resource species (species.XXX.sp instead of plankton.YYY.plk for instance)
- Removing of iterative mortality processes (no more used)
- Removing of all grid types except NCGrid
- Possibility to read file using regular expressions (in order to use multiple files).
- New parameterization of accessibility matrixes (thres. read from files, no more as parameters).
- All species (focal, background and resource) must have different indexes
- Extensive use of HashMaps in order to allow easy switch of a species from one type to another
- Use of the updated NetCDF Java library (associated with Netcdf-C)

# Older versions

## Changes from Osmose 3 Update 1 to Osmose 3 Update 2

This new release is an update of  Osmose 3 and,  as such,  does not  lead to major changes in the parameters. This document  highlights the most  important  changes from Osmose 3 Update 1 to Osmose 3 Update 2, and lists all the changes, additions and deprecations.

The release improves the initialisation of the model in order to avoid oscillation (that may lead to premature species collapse) and make it lighter and faster. It also provides a new parametrisation for managing the input flux of incoming species more accurately. This release automatically updates the configuration file from previous versions (Osmose 3 and Osmose 3 Update 1). Osmose 3 Update 2 also comes will clear guidelines for the calibration procedure (separate document).

You can retrieve Osmose 3 Update 2 and the default configuration either from the Downloads section or from the subversion server (contact us and ask for the login if you whish to use SVN).

Source code https://svn.mpl.ird.fr/osmose/code/branches/stable-3
Configuration https://svn.mpl.ird.fr/osmose/code/branches/config/osm/osm_v3u2
### Release history
This section provides of  brief  history  of  previous Osmose versions.  Source code and default configuration files are all available from the subversion server.

#### Osmose 3 Update 1
This release updates and clarifies the mortality algorithms, hereafter called ITERATIVE and STOCHASTIC algorithms (see section below). Both stochastic and iterative algorithms now guarantee Finput=Foutput (so no need to back calculate fishing mortality from output as we used to do before). Fishing mortality and natural mortality accept any type of time variability (constant, seasonal, interannual, interannual + seasonal). The release introduces the option of parameterizing fishing by input catches (this could be useful if you would like to simulate fishing quotas for example). Effort for unifying the output format has been made (not finished though).

You can retrieve Osmose 3 Update 1 and the default configuration either from the Downloads section or from the subversion server (contact us and ask for the login if you whish to use SVN).

Source code https://svn.mpl.ird.fr/osmose/code/ tags/ov3u1
Configuration https://svn.mpl.ird.fr/osmose/code/branches/config/osm/osm_v3u1
#### Osmose 3
This is the first public release of Osmose 3. It is a stabilized and debugged version of Osmose that has been presented during the Osmose Workshop 2013. It uses the ITERATIVE (previously called Case1) mortality algorithm by default (iterative algorithm presented by Ricardo at the 2013 workshop). Most input parameters can be in the form of time series so that the application works on interannual mode.

You can retrieve Osmose 3 and the default configuration either from the Downloads section or from the subversion server (contact us and ask for the login if you whish to use SVN).

Source code https://svn.mpl.ird.fr/osmose/code/tags/ov3
Configuration https://svn.mpl.ird.fr/osmose/code/branches/config/osm/osm_v3
#### Osmose 2, 2013
This version gathers together all the work done from February 2013 (limit being the email sent for MEECE simulations early February 2013) until mid-year 2013. Both mortality algorithm CASE1 and CASE3 are implemented (what will become ITERATIVE and STOCHASTIC in Osmose 3 Update 1), conversely to ov2_2012. It fixes several bugs from ov2_2012 and improves input formats for fish spatial distribution maps.

You can retrieve Osmose 2, 2013 and the default configuration either from the Downloads section or from the subversion server (contact us and ask for the login if you whish to use SVN).

Source code https://svn.mpl.ird.fr/osmose/code/tags/ov2_2013
Configuration https://svn.mpl.ird.fr/osmose/code/branches/config/osm/osm_v2_2013
#### Osmose 2, 2012
This version gathers together all the work done from mid-2011 (start of Philippe's contract) to beginning of 2013 (limit being the email sent for MEECE simulations early February 2013). It incorporates the new mortality algorithms but only CASE3 (what will become the stochastic mortality algorithm in Osmose 3 Update 1) seems to work properly. As for the input files, it takes the old format (even though the habitat/area file accepts some CSV files). It is a big update from Osmose WS2009, with a lot of bug fixing and the first steps for improving the mortality algorithm, without major changes in the parametrization.

You can retrieve Osmose 2, 2012 and the default configuration either from the Downloads section or from the subversion server (contact us and ask for the login if you whish to use SVN).

Source code https://svn.mpl.ird.fr/osmose/code/tags/ov2_2012
Configuration https://svn.mpl.ird.fr/osmose/code/branches/config/osm/osm_v2_2012
#### Osmose 2, 2009
Osmose 2, workshop May 2009, Cape Town, South Africa. This version gathers all the work done by Morgane, Yunne and collaborators during Morgane PhD (2006-2009). Main additions concern the coupling of Osmose to the
biogeochemical model ROMS-NPZD.

You can retrieve Osmose 2, 2009 and the default configuration either from the Downloads section or from the subversion server (contact us and ask for the login if you whish to use SVN).

Source code https://svn.mpl.ird.fr/osmose/code/tags/ws2009
Configuration https://svn.mpl.ird.fr/osmose/code/branches/config/ben/afs_ws2009
#### Osmose 1
Yunne PhD and early career (refer to Shin and Cury 2001, 2004). Not archived on subversion server.

### Update configuration file

#### From Osmose 2 to Osmose 3

From Osmose 2, 2012 or Osmose 2, 2009, you need to update manually your configuration files to be compatible with Osmose 2, 2013. They are no major changes though.

From Osmose 2, 2013 to Osmose 3, there is a utility that will convert automatically your configuration file to the new Osmose 3 format. Open Osmose 3 Update 1 with Netbeans and expand package fr.ird.osmose.util. Open class ConfigurationConverter.java. There is two variables to set up:

inputFile that provides the path to the Osmose 2, 2013 INPUT.txt
outputPath that provides the path of the directory for the converted Osmose 3 configuration.
Right click on ConfigurationConverter.java from the “Projects” panel and click on “Run File”. The conversion starts and should last one or two seconds.

This converter has been written as a convenience for the developer to upgrade quickly several configurations and was not intended at first to be shared. Even though it should work without any problem for the end user, be aware that it is not a fancy tool and you must double check the new configuration files and look for any inconsistency.

#### From Osmose 3 to current release
Since Osmose 3 Update 1, the configuration manager automatically updates any configuration (no older than Osmose 3) to the current release. The update proceeds incrementally:

detect the version of the configuration (check parameter 'osmose.version', if the parameter does not exist, Osmose assumes this is Osmose 3)
loop and update from one version to the following one until the newest release
Any configuration file that is modified by the update is backed up, for instance osm_param-output.csv is copied as osm_param-out.csv.bakyyyyMMddHHmm (with yyyyMMddHHmm the time of the backup). Every changes is commented in the configuration file and detailed in the log of the simulation.

Please pay careful attention to the log after the update process as it will give valuable information about missing parameters or assumption made by Osmose in order to run the new version.

#### CSV input file separator
Many Osmose parameters are paths to CSV file, for instance:

movement.map0.file
mortality.fishing.rate.byDt.byAge.file.sp#
reproduction.season.file.sp#
In Osmose 3 and Osmose 3 Update 1 these CSV input files had to be semi-colon separated. Since Osmose 3 Update 2, CSV input file separators can be any of the following characters:

equals =
semi-colon ;
coma ,
colon :
tab \t
Osmose will detect the separator automatically and independently for every CSV file. It means that one CSV input file may be coma separated and an other one may be tab-separated, this is perfectly fine since Osmose 3 Update 2.

#### Decimal separator
Osmose is quite flexible in terms of separators for the configuration files (automatically detected among = , ; : \t ), the CSV output files (user-defined by parameter output.csv.separator) and the CSV input files (automatically detected among = , ; : \t ). On the contrary it restricts the decimal separator to dot, and only dot.

Exemple given: 3.14159265 or 1.618

Any other decimal separator (COMA for instance as in French locale) will be misunderstood and will unmistakably lead to errors. One must be careful when editing CSV input files (either parameters or time series) with tools such as spreadsheets that may automatically replace decimal separator depending on the locale settings. Future Osmose release might allow the use of specific locale but for now remember that DOT is the only accepted decimal separator.

Population initialisation
#### Seeding approach
Until Osmose 3 Update 1, the main way of initialising the population consisted in providing target biomass for every species. Osmose would create an age-structured population following an exponential decay by estimating the total annual mortality from the fishing and natural mortality parameters. This initialisation method shows several drawbacks:

it provides a fully structured population though no assumption should be made about this structure. Indeed we want the model to build up this structure given some basic laws at individual level.
it often leaves the system in a highly unstable state and therefore leads to premature and artificial species collapses or explosion in the first year of the simulation.
it slows down the simulation because the initial population contains a big number of schools (it creates nschool for every age class of the species, from eggs to old schools).
We must first acknowledge that there is no ideal solution for initialising Osmose but it should be done buy making as little assumptions as possible, keeping the spin-up time as short as possible and leave the model dynamics as untouched as possible. Keeping this three points in mind, Osmose 3 Update 2 proposes a new "seeding" mechanism for initialising the population. It works the following way: the system starts from a pristine state, with no schools in the domain. For a few years (user-defined) Osmose will spawn some eggs for every species. As soon as the eggs reach sexual maturity, the reproduction process takes over. Osmose stops the seeding, unless the spawning stock
biomass gets depleted. In that case Osmose resumes the seeding by releasing some eggs until there are again mature individuals in the system for carrying on the reproduction process. Osmose completely ceases the seeding when the simulation reaches the maximal number of year for seeding, defined in the configuration file.

By following this approach

it does not make any assumption about the structure of the population
it gives full command to the individual based model for building up the structure of the population
it keeps the model light-weight as the population grows from scratch (the first years are the fastest to run, conversely to the target biomass approach)
it minimizes the amplitude of the population oscillations
it makes the spin-up period shorter
#### Seeding parameters
The new initialisation process is controlled by two parameters, the seeding biomass and the seeding duration.

population.seeding.biomass.sp#
This parameter is the SSB that Osmose guarantees during the initialisation process when there are no mature adults to ensure the reproduction process. The number of eggs to be released in the reproduction process are computed the following way neggs = sex_ratio * alpha * season * SSB with SSB = sum(biomass of mature individuals) or population.seeding.biomass.sp# if sum(biomass of mature individuals) is equal to zero.

As a first estimate population.seeding.biomass.sp# can take the same value as the previous parameter population.initialisation.biomass.sp#. This parameter could be calibrated.

population.seeding.year.max
The number of years for running the seeding process. From year 0 to year seeding max, Osmose will guarantee that some eggs will be release even though there are no mature individuals in the system. From year seeding max to the end of the simulation, the seeding ceases completely. If the parameter does not exist, Osmose will set it by default to the lifespan of the longest lived species of the system.

#### Deprecated parameters
The seeding mechanism replaces the initialisation from biomass. As a consequence the following parameters are deprecated

population.initialisation.biomass.sp#
population.initialisation.method
Indeed we also deleted the initialisation from spectrum as it has never been used since Osmose 2 and therefore the only way to initialise the population is the seeding mechanism. One can still start the simulation from a NetCDF file though. Refer to the next section.

#### Initialisation from NetCDF file
The population can be initialised by providing a NetCDF file that contains a complete description of every single school of the population. This approach may be useful for defining initial condition for an inter-annual run, for instance. Refer to the user manual for details.

population.initialization.file
Be aware that initialisation from NetCDF file and seeding mechanism are independent one from an other and they may interfere if set up inconsistently. If one initialises the simulation from a NetCDF file, then the seeding process should be disabled (by setting seeding biomass or seeding maximal year to zero).

### Migration
Osmose 3 Update 2 redefines how to input incoming flux of biomass in the system. Since Osmose 2 2012, the incoming flux of biomass was defined by a biomass, an incoming age or length and a seasonality file. Since Osmose 3 Update 2, the user provides time series of biomass by size/age class, in CSV files.

#### Incoming flux process
Deprecated parameters
flux.incoming.season.file
flux.incoming.season.file.sp#
flux.incoming.biomass.sp#
flux.incoming.size.sp#
flux.incoming.age.sp#
New parameters
flux.incoming.byDt.byAge.file.sp#
flux.incoming.byDt.bySize.file.sp#
One or the other.

Format of the CSV file

Time step / Age;0;2;3;4
0;0;500;800;0
1;0;500;800;0
2;0;400;700;0
3;0;400;700;0
...
The age classes (year) are automatically scanned by Osmose. In this case there are 4 classes: [0 2[, [2 3[, [3 4[ and [4 lifespan[. Osmose will sets the incoming age at the middle of the interval: 1 year, 2.5 year, 3.5 year, etc. The value of the time step does not matter, Osmose assumes there is one line per time step. The number of time steps in the CSV file must be a multiple of the number of time steps per year. If the time series is shorter than the duration of the simulation, Osmose will loop over it. If the time series is longer than the duration of the simulation, Osmose will ignore the exceeding steps.

In the above example, for the first time step, Osmose will input 500 tonnes of 2.5 year old school and 800 tonnes of 3.5 year school. The incoming biomass should be calibrated. Size classes are handled the same way than age classes.

simulation.nschool.sp#
This parameter takes a slightly different meaning for the incoming flux process. It still controls the number of schools created during the reproduction process (which may occur independently of the incoming flux process, depending on your configuration parameters) but it also controls the number of schools created for each age/size class and time step. The meanings are close enough so as not to worry about the value of this parameter and its order of magnitude depending on whether it controls reproduction, incoming flux or both.

#### Several processes to account for migration
In your simulated domain, some species might not fulfil a complete life cycle inside the domain but are too significant in terms of biomass or impact upon the other species to be ignored by the simulation. Such species will be considered as migrating species in Osmose and several mechanisms co-exists in order to depict a broad range of situation. Careful parametrisation (and combination) of incoming flux process, reproduction process, movement process and lifespan should allow the user to represent any type of migration.

Incoming flux process
Some schools come from outside the simulated domain at a given age/size and time. Osmose does not control where and when these schools have been spawned neither what happened to them in previous stages. This process brings them in. What happens to these schools inside the simulated domain is then up to the movement process and reproduction process.

Temporarily out of the simulated domain
Osmose can relocate some schools outside the simulated domain, at given age and time. What happens outside the simulated domain is not explicitly represented, Osmose applies a user-defined total mortality rate and a species specific growth rate and the reproduction process is disabled. The movement parameters may be such that some schools never come back in the simulated domain. It is not advisable though because such schools would unnecessarily clutter the memory.

Permanent departure
Some species may leave the simulated domain at a given age and never come back. As mentioned in the previous point, this could be achieved in Osmose with the movement process by leaving them permanently out of the simulated domain, but it is not recommended for memory reason. Osmose does not offer yet a specific process to trigger the permanent departure of some schools at a given age/size. The only trick to render this behaviour would be to purposefully shorten the lifespan of the species. It works but it is not satisfactory because it corrupts the meaning of the lifespan parameter.

#### Reproduction process
The process controls which schools can spawn inside the simulated domain.

Reproduction process
Updated ReproductionProcess.java so that the seasonality files can only be provided by species.

Deprecated parameter:

reproduction.season.file
Valid parameter:

reproduction.season.file.sp#

## Changes from Osmose 3 to Osmose 3 Update 1

Osmose 3 update 1, as the name indicates, is an update of Osmose 3 and, as such, does not lead to major changes in the parameters. This document highlights the most important changes from Osmose 3 to Osmose 3 update 1 in terms of features, and lists all the changes of parameters, additions and deprecations.

This release updates and clarifies the mortality algorithms, hereafter called ITERATIVE and STOCHASTIC algorithms (see section below). Both stochastic and iterative algorithms now guarantee Finput=Foutput (so no need to back calculate fishing mortality from output as we used to do before). Fishing mortality and natural mortality accept any type of time variability (constant, seasonal, interannual, interannual + seasonal). The release introduces the option of parameterizing fishing by input catches (this could be useful if you would like to simulate fishing quotas for example). Effort for unifying the output format has been made (not finished though). This new release of Osmose also comes will clear guidelines for the calibration procedure (separate document).

You can retrieve Osmose 3 Update 1 and the default configuration either from the download section or from the subversion server (contact us and ask for the login if you whish to use SVN):

Source code https://svn.mpl.ird.fr/osmose/code/branches/stable-3
Default configuration template https://svn.mpl.ird.fr/osmose/code/branches/config/osm/osm_v3u1

### Update configuration file

The update of an old configuration to Osmose 3 Update 1 must be done by steps, from one version to the next one.

#### Osmose 2 to Osmose 3
From Osmose 2, 2012 or Osmose 2, 2009, you need to update manually your configuration files to be compatible with Osmose 2, 2013. They are no major changes though.
From Osmose 2, 2013 to Osmose 3, there is a utility that will convert automatically your configuration file to the new Osmose 3 format. Open Osmose 3 Update 1 with Netbeans and expand package fr.ird.osmose.util. Open class ConfigurationConverter.java. There is two variables to set up:

inputFile that provides the path to the Osmose 2, 2013 INPUT.txt
outputPath that provides the path of the directory for the converted Osmose 3 configuration.
Right click on ConfigurationConverter.java from the “Projects” panel and click on “Run File”. The conversion starts and should last one or two seconds.

This converter has been written as a convenience for the developer to upgrade quickly several configurations and was not intended at first to be shared. Even though it should work without any problem for the end user, be aware that it is not a fancy tool and you must double check the new configuration files and look for any inconsistency.

### Osmose 3 to Osmose 3 update 1
From Osmose 3 to Osmose 3 update 1, there is unfortunately not yet any automatised updater (on its way though, it will be included in Osmose 3 update 2). Nonetheless a basic Osmose 3 configuration should run out of the box with Osmose 3 update 1 and any missing parameters will be reported in the logs. The main visible changes concern the names of the output parameters but Osmose will mention it in the logs. Moreover this document details all the new available features and changes.

### Mortality algorithm
Most of the work on this update concerns the mortality algorithm. Detailed documentation on the new mortality algorithm will be published soon. In the meantime, here is a brief history of the work on the mortality algorithm for the last 12 months.

#### History
The initial problem we wanted to address is that in Osmose 2, Finput mismatched Foutput, which is very annoying when one is supposed to do batches of scenarii with different Finput. Simulation results could still be reported in function of Foutput, but the problem was the absence of control in the fishing driver in input. We came up with two different solutions to the problem: an iterative approach and a purely stochastic approach. At first we observed that:

The iterative algorithm works consistently with relatively low values of F but does not behave satisfactorily with higher values of F. Looking at it closely we understood that it is because we handle side by side mortality via a rate (fishing mortality rate F) and mortality via biomass removal (predation) so that from a certain threshold value, F is outcompeted by predation pressure.
Stochastic case was not working well at all, high variability in the simulations and calibration failures.
Here are the solutions we implemented to address the problems mentioned above:

For iterative algorithm: added fishing by catches so that both predation and fishing deal with biomasses, which solved the problem. But it disqualified the algorithm when implementing scenarii of Finput.
For stochastic algorithm: added a subdt for ensuring that the random order of the mortality sources within a cell does not bias the mortality outcome. And this was the beginning of series of problems that we've been dealing with in 2014.
As said previously, the idea behind the sub time step is to get rid of the variability due to the random order of the mortality sources. The main problem we faced was that it created trends in the state variables depending on the number of sub time steps. This means the processes at sub time step level were not consistent any more, with the processes expressed at time step level. So we had to update several parts of the code, namely predation, eggs release and LTL forcing, to make sure that the meaning of the processes at sub time step level remains consistent as expressed at time step level.

Here comes the list of the modifications we made in the code in order to achieve consistency of the stochastic algorithm:

starvation: at the end of time step t, we can calculate the starvation mortality rate based on what a school has been eating during time step t. At time step t + 1, the starvation mortality is applied based on the starvation mortality rate estimated at the end of time step t. For sub time step level, we apply starvation mortality rate / subdt
the predation success rate is calculated as the average predation success rate over the sub time steps. We did so after noticing that calculating the predation success rate at the end of the time step (ie eaten B[end of dt]/maxBiomassToPrey[beginning of dt]) would systematically underestimate the predation success (as the maximum biomass to prey upon at sub time step level is calculated from the instantaneous biomass of the predator, we always have maxBiomassToPrey(sub time step) < maxBiomassToPrey(dt) / n_subdt)
the meaning of the plankton accessibility coefficient (estimated by EA) has been clarified. It removes from the system a fraction of the LTL biomass beforehand. accessible_LTL_biomass = accessibility_coefficient * LTL_biomass is what is available at current time step dt for the predators.
the meaning of the larval mortality (estimated by EA) has been clarified. Similarly to the plankton accessibility coefficient, the larval mortality will remove some eggs from the system beforehand. It stands for eggs and larvae mortality before even being available to predation. In concrete terms, the larval mortality is applied before entering the stochastic algorithm. This is meant to represent the high loss of eggs from the system modelled (non fecundation, sinking, exportation from system due to drifting).
eggs are released evenly over the sub time steps (instead of releasing all the eggs at the beginning of the time step, which led to a dramatic increase of the egg mortality due to predation). Every sub time step, the biomass (and the abundance) of the school of eggs is incremented by (spawned_biomass - egg_loss) / n_subdt, egg_loss accounting for the larval mortality.
a LTL group is handled as any other preys. The accessible_LTL_biomass is updated on the fly as it is preyed upon by predators.
#### New parameters
mortality.algorithm = stochastic
mortality.subdt = 10
### Low trophic level
plankton.multiplier.plk0 = 1
Added a parameter 'plankton.multiplier.plk#' for multiplying plankton biomass, in order to run different scenarios of plankton forcing in relation to a reference state (plankton.multiplier.plk0 = 1 by default for the reference state). For instance plankton.multiplier.plk0 = 2 means that Osmose will multiply by two the biomass of the plankton group zero of the LTL dataset.

### Output
#### File format and naming convention
Modifications of the Osmose outputs aim to provide data in a systematic way, always abiding to the same format, so that it can be easily interpreted and analysed by post-processing tools (such as Osmose2R for instance). The name of the Osmose parameters for enabling the outputs tend to follow the pattern:

output.variable(.byAge/bySize/byTL)(.perSpecies).enabled
The byAge/bySize/byTL means that the outut data will be split by age, size or trophic level classes. The perSpecies means that the information is divided into one file per species.

Osmose outputs are provided in CSV files and can have the following formats:

Time serie for all species.
For instance:

output.biomass.enabled
output.yield.abundance.enabled
output.size.enabled
Time series by age, size or trophic level classes, for all species.
For instance:

output.yield.bySize.enabled
output.meanSize.byAge.enabled
output.biomass.byTL.enabled
Time series of trophic interaction by age or size class, per species (one file per species)
output.diet.composition.byAge.enabled
output.diet.pressure.bySize.enabled
Time series of trophic interaction, for all species
For instance

output.diet.pressure.enabled
output.diet.composition.enabled
Mortality files
For instance:

output.mortality.enabled
output.mortality.perSpecies.byAge.enabled
Trophic interactions and mortality rates cannot be satisfactorily written in a CSV file as they are time series of 2D arrays (or more if we include age or size classes). A NetCDF file would suit better the purpose but would make the use of post-processing routines compulsory, which can be a hindrance for the end-user. We have not reached yet a satisfactory answer to the problem and output formats will keep evolving in future releases.

#### CSV separator
output.csv.separator = COMA (or SEMICOLON, EQUALS, TAB, COLON)
Added parameter 'output.csv.separator' that controls the CSV separator in the CSV output files. Coma by default.

#### Restart
output.restart.enabled = true
Added parameter 'output.restart.enabled' to be able to deactivate completely the writing of the NetCDF restart file. True by default.

output.restart.spinup = 0
Sets the number of years before starting to write the restart file (assuming that the record frequency of the restart file is one year for instance). Zero by default.

output.restart.recordFrequency.ndt = 24
Renamed parameter 'simulation.restart.recordFrequency.ndt'. Only use this parameter if you would like Osmose to save more restart files rather than just one at the end of the simulation.

#### Diets
output.diet.composition.enabled
output.diet.composition.byAge.enabled
output.diet.composition.bySize.enabled
output.diet.pressure.enabled
output.diet.pressure.enabled
output.diet.pressure.byAge.enabled
output.diet.pressure.bySize.enabled
Added new outputs PredatorPressureSpeciesOutput.java and DietSpeciesOutput.java that gives information about the predation, either from the prey or predator perspective, per age/size class.

#### Size-based or age-based distributed parameters
output.biomass.bySize.enabled
output.biomass.byAge.enabled
output.abundance.bySize.enabled
output.abundance.byAge.enabled
output.mortality.perSpecies.byAge.enabled
output.mortality.perSpecies.bySize.enabled
output.yieldN.bySize.enabled
output.yield.bySize.enabled
output.yieldN.byAge.enabled
output.yield.byAge.enabled
output.meanSize.byAge.enabled
output.biomass.byTL.enabled
output.meanTL.bySize.enabled
output.meanTL.byAge.enabled
Updated list of size/age based outputs.

### Fishing
Included fishing mortality by catches. All the parameters that do exist for “rate” have the equivalent for “catches”. Example : parameter 'mortality.fishing.rate.sp#' and its equivalent for catches 'mortality.fishing.catches.sp#'

mortality.fishing.type = catches (or rate)
Added parameter 'mortality.fishing.type' either 'catches' or 'rate'. Rate by default.

Possible ways of parametrizing fishing in Osmose (rates and catches):

Fishing mortality by dt, by age class
mortality.fishing.rate.byDt.byAge.file.sp#
or mortality.fishing.catches.byDt.byAge.file.sp#
Fishing mortality by dt, by size class
mortality.fishing.rate.byDt.bySize.file.sp#
or mortality.fishing.catches.byDt.bySize.file.sp#
Annual rates with seasonality file per species
mortality.fishing.rate.byYear.file.sp#
or mortality.fishing.catches.byYear.file.sp#
mortality.fishing.season.distrib.file.sp#
Constant annual rates by species with seasonality file per species
mortality.fishing.rate.sp#
or mortality.fishing.catches.sp#
mortality.fishing.season.distrib.file.sp#
Constant annual rate by species
mortality.fishing.rate.sp#
or mortality.fishing.catches.sp#
Spatial F not taken into account yet.

### Natural mortality
In a similar way to fishing, the parametrization of the natural mortality has been extended to take into account time variability at different levels.

#### Larval mortality
Larval mortality by dt
mortality.natural.larva.rate.byDt.file.sp#
Constant larval mortality
mortality.natural.larva.rate.sp#

#### Natural mortality
Natural mortality rate by dt and by age or size class
mortality.natural.rate.byDt.byAge.file.sp
mortality.natural.rate.byDt.bySize.file.sp
Natural mortality rate by dt
mortality.natural.rate.bytDt.file.sp
Annual natural mortality rate
mortality.natural.rate.sp
### Marine Protected Area
The user can defined as many MPA as he wishes.

mpa.file.mpa0 = maps/mpa0.csv
mpa.start.year.mpa0 = 10
mpa.end.year.mpa0 = 15
An MPA is defined by a map of 0 and 1 and by a time span. Parameters  'mpa.file.mpa#'  'mpa.start.year.mpa#' and 'mpa.end.year.mpa#'

The MPA are handled within the Fishing process. Every time there is a new MPA to be activated or deactivated, Osmose updates the correction factor that will be applied to the fishing mortality rates in order to take into account the uniform redistribution of the fishing effort outside the MPAs.

### Reproduction and incoming flux
Updated ReproductionProcess.java and IncomingFluxProcess.java so that the seasonality files can be provided by species. Parameters 'reproduction.season.file' and 'flux.incoming.season.file' are still valid (and they have priority) but user can also choose to specify instead parameters such as:

reproduction.season.file.sp#
flux.incoming.season.file.sp#

# Release notes of Osmose-R

From now on, Osmose R and Osmose Java are one, so everything is merged in a single release note

## osmose 3.3.3
* Several improvements on methods: print, summary, plot, report.
* Improved documentation: help files, demo scripts, vignettes.
* Bug corrections.

## osmose 0.1.0
* First release to CRAN

## osmose 3.3.3
* Corrections and modifications in order to submit to CRAN.
