# OSMOSE Release Notes - Version 4.4.0

Release notes capturing changes from OSMOSE 4.3.3 to 4.4.0

## New Features

### Model Capabilities

-   **Single Cell Mode**: Added possibility to run simulations on a single grid cell via `grid.single.cell.enabled` parameter
-   **Simplified Bioenergetics Module**: Support for simplified bioenergetics for data-poor species (e.g., invertebrates)
-   **Gradient-based Spatial Distribution**: New spatial distribution method for fish movements
-   **Marine Ecological Outputs**: Enhanced outputs for Marine project simulations
-   **Background Species Management**: Improved initialization and management of background and school species using biomass and `species.biomass.nsteps.year.spX` parameters

### Genetics and Allele Frequency

-   **Size Class in Allele Frequency Outputs**: Allele frequency outputs now include size class information
-   **Observed Heterozygosity (Htz) Output**: Size class tracking in observed heterozygosity outputs
-   **Early Genetic Transmission**: When no coding traits exist, genetic transmission can now occur earlier than seeding date
-   **Mean Genotype Outputs**: Averaged genotype outputs with recording frequency control

### Mortality and Energy Dynamics

-   **Mortality Outputs by Region**: Ability to record mortality outputs on different spatial regions
-   **Additional Mortality Rates in Log Format**: Support for `mortality.additional.rate.log.spX` parameter for flexible mortality specification
-   **Imax Correction**: Fixed foraging mortality calculation when traits are not present
-   **Age and Size at Maturity Outputs**: New outputs for tracking maturity dynamics
-   **CPU Performance Outputs**: Added `output.cpu.performance.enabled` for monitoring computational performance

### Fisheries and Economic Module

-   **Economic Module**: Fully functional economic module implementation (bioeconomics) with:
    -   Fish price variation outputs
    -   Harvesting costs computation
    -   Profit margin calculations
    -   Fisherman's profit equations
    -   Net present value (NPV) calculations
    -   Social optimum analyses
-   **Fisheries Accessibility Biomass**: Output of fishery accessible biomass and yields in abundance
-   **Catch by Numbers**: Fishing computation now in abundance rather than biomass (catch, landings, and discards)
-   **Fishery Outputs by Class**: New outputs by size/TL class with temporal and spatial disaggregation

### Calibration Improvements

-   **New Calibration Functions**:
    -   `osmose_calibration_outputs`: Process outputs for calibration
    -   `osmose_calibration_runmodel`: Run OSMOSE as part of calibration workflow
    -   `osmose_calibration_setup`, `osmose_calibration_test`, `osmose_calibration_demo`
-   **Multi-phase Calibration**: Support for multiple calibration phases with independent parameter sets
-   **Penalty Functions**: New penalties for outburst and collapse detection
-   **Group-based Parameterization**: Parameters can be modeled by species or fishery groups
-   **Cluster Computing Support**: Scripts for PBS/SLURM job schedulers (DATARMOR support)
-   **Template-based Workflow**: Survey calibration templates for streamlined setup

### Reproduction and Growth

-   **New Reproduction Modes**: Separate handling of oviparity (SSB-based) and viviparity (SSN-based) modes
-   **Dynamic Maturity Check**: Maturation check now performed within school objects
-   **Stochastic Reproduction**: Enhanced stochastic reproduction processes
-   **Genetic Transmission in Reproduction**: Full integration of genetic transmission during reproduction
-   **Bioen Spawning Seasonality**: New spawning seasonality mechanisms for bioenergetic module

### Configuration and Parameterization

-   **Class Definition in Outputs**: Flexible class threshold definitions for output distribution
-   **Species Names with Special Characters**: Support for dashes (`-`) in species names
-   **Simulation Start and Stop Parameters**: `simulation.time.start` and parameters for controlling temporal scope
-   **Nschool Multiplier**: New `simulation.nschool.multiplier` parameter to scale schools across focal species
-   **Dynamic Assignment**: `simulation.nschool` provides default for species not explicitly specified

### NetCDF and Output Processing

-   **NetCDF-based Parameterization**: Support for NetCDF files for movement distribution and species forcing
-   **Improved Output Format**: Better organization and processing of NetCDF outputs with independent files by variable
-   **2D Output Support**: Enhanced 2D outputs (by species and size class)
-   **Output by Regions/Surveys**: Support for regional and survey-based filtering in outputs

### Data Input and File Handling

-   **Regular Expression File Matching**: Use regex patterns to match input files from directories
-   **Flexible CSV Input Formats**: Better handling of various CSV separators in input files
-   **Absolute Path Handling**: Windows compatibility fix using absolute paths instead of canonical paths
-   **Time Series Management**: New `ByClassTimeSeries` for proportions in background species

### Selectivity and Fishery Modeling

-   **Lognormal Distribution for Selectivity**: Support for lognormal selectivity curves
-   **Discrete Selectivity Values**: Ability to define selectivity using discrete values instead of curves
-   **Age Selectivity**: Enhanced support for age-based selectivity (though not for calibration)
-   **Selectivity Type 9**: New selectivity type implementation (non-parametric)
-   **Relaxed Selectivity Thresholds**: More flexible deltaL75 thresholds (up to 100% of selectivity)

### Data Analysis and R Package Features

-   **Enhanced Output Reading**: `read_osmose` can handle multi-level disaggregation (surveys and species)
-   **Survey-based Analysis**: `get_var` and other functions accept survey as additional subsetting argument
-   **Predator Pressure by Survey/Region**: Regional predator pressure output capability
-   **Calibration Folder Management**: `read_osmose` handles calibration folders with multiple restart/results files
-   **New Templates**: Function to create R templates for initialization, calibration, and analysis

## Parameter Changes

### Renamed Parameters

-   `output.fishery.*` → `output.fisheries.*` (output parameters)
-   `simulation.bioen.enabled` → `module.bioenergetics.enabled`
-   `simulation.genetic.enabled` → `module.genetics.enabled`
-   `simulation.fishing.mortality.enabled` → `simulation.fishing.mortality.enabled` (clarified)
-   `economy.enabled` → `module.bioeconomics.enabled`
-   `fisheries.enabled` → `process.multispecies.fisheries.enabled`
-   `predation.ingestion.rate.max.bioen.spX` → `predation.ingestion.rate.max.bioen.spX` (consistency)
-   `predation.coef.ingestion.rate.max.larvae.bioen.sp` → `predation.larval.ingestion.rate.increase.ratio.spX`
-   `species.bioen.maturity.eta.spX` → `species.maturity.eta.spX`
-   `species.bioen.maturity.r.spX` → `species.maturity.r.spX`
-   `species.bioen.maturity.m0.spX` and `m1` parameter swap (m0 is now intercept, m1 is slope)
-   `fisheries.rate.bySeason.fsh%d` → `fisheries.rate.byperiod.fsh%d`
-   `species.larva2adults.agethres.sp` → `species.first.feeding.age.sp`
-   `stochastic.mortality.seed` → `stochastic.mortality.randomseed.fixed` (boolean)
-   `population.initialization.seed` → `population.initialization.randomseed.fixed` (boolean)
-   Fisheries class outputs renamed for consistency
-   Grid NetCDF file reading now uses `NcGrid.java` class (removal of `grid.java.classname`)

### New Parameters

-   `grid.single.cell.enabled`: Enable single-cell mode simulation
-   `module.bioenergetics.enabled`: Control bioenergetics module
-   `module.genetics.enabled`: Control genetics module
-   `module.bioeconomics.enabled`: Control economic module
-   `simulation.cpu.performance.enabled`: Enable CPU performance tracking
-   `output.cpu.performance.enabled`: Output CPU performance metrics
-   `species.biomass.spX`: Initialize background/school species biomass
-   `species.biomass.nsteps.year.spX`: Control biomass initialization timing
-   `mortality.additional.rate.log.spX`: Additional mortality in log format
-   `simulation.bioen.fo2.enabled`: Control oxygen limitation computation
-   `simulation.bioen.phit.enabled`: Control thermal correction computation
-   `simulation.nfisheries`: Explicit number of fisheries parameter
-   `simulation.nschool.multiplier`: Scale schools across focal species
-   `output.cutoff.size.sp#`: Size-based output filtering
-   `output.cutoff.age.sp#`: Age-based output filtering (now working correctly)
-   `genetics.randomseed.fixed`: Boolean to control stochasticity in genetics (Ev-OSMOSE)
-   `population.initialisation.biomass.sp#`: Population initialization biomass
-   `population.seeding.biomass.sp#`: Seeding biomass guarantee
-   `population.seeding.year.max`: Maximum seeding duration (years)
-   `simulation.restart`: Improved restart parameter handling
-   `reproduction.mode`: Specify reproduction mode (oviparous/viviparous/etc.)
-   `output.restart`: Moved to `simulation.restart` with time-step resolution
-   `species.weight.at.larvae.growth.threshold.age` (W0): Optional (default 0)
-   `predation.success.disabled.sp`: Disable predation success for specific species
-   `module.population.initialisation.enabled`: Control population initialization
-   `simulation.resource.computePercent.legacy`: Legacy resource computation mode
-   `simulation.nschool.sp%d`: Species-specific school counts
-   Fisheries grouping parameters for disaggregated analysis
-   Survey selectivity and other survey-specific parameters

### Deprecated Parameters

-   `population.initialisation.method`: Replaced by unified seeding mechanism
-   `population.initialisation.biomass.sp#`: Deprecated (use seeding mechanism)
-   `flux.incoming.season.file`, `flux.incoming.season.file.sp#`: Replaced by time-series approach
-   `flux.incoming.biomass.sp#`, `flux.incoming.size.sp#`, `flux.incoming.age.sp#`: Replaced by:
    -   `flux.incoming.byDt.byAge.file.sp#` OR
    -   `flux.incoming.byDt.bySize.file.sp#`
-   `simulation.onestep`: Removed (use debugging tools instead)

### Modified Parameter Behavior

-   **Annual Larval Mortality**: Now provided in rates/year instead of rates/time-step (multiply original rates by `simulation.time.ndtperyear`)
-   **Output Parameters**: `lastYear` and `lastAge` are now inclusive (previously exclusive upper bounds)
-   **Stochasticity Control**: Simulation output now depends on simulation rank index but is reproducible within replicates
-   **Species Initialization**: All species (focal, background, resource) must have different indexes
-   **Sex Ratio**: Default value of 0.5
-   **Year Max Seeding**: Set to zero when population initialization is true

## Bug Fixes

### Core Simulation

-   **Restart Initialization**: Corrected initialization of restarts from NetCDF files
-   **Trait Reinitialization**: Fixed trait reinitialization from restart files
-   **Time Index**: Corrected time index initialization from NetCDF
-   **Time Step Initialization**: Fixed time-step initialization handling
-   **Mortality with Infinity**: Fixed numerical issues with infinite mortality values
-   **Dead School Biomass**: Corrected biomass computation for dead schools, now using initial weight
-   **School Loop**: Simplified and corrected school iteration logic

### Genetics and Traits

-   **Genetic Key Definition**: Fixed key definition in restart diversity loading
-   **Genotype Instantiation**: Corrected genotype instantiation in relative biomass populator and NetcdfPopulator
-   **Maturity Check**: Fixed `isMature` vs `isSexuallyMature` inconsistencies
-   **Foraging Mortality**: Corrected imax calculation when traits are not present

### Movement and Distribution

-   **Movement Process Ordering**: `movementProcess.run()` now called before `indicators.initStep()` for consistent initial biomass
-   **Spatial Output Index**: Fixed wrong index in write_nc_coords for spatial outputs
-   **Cutoff Application**: Spatial outputs now correctly apply cutoff by length (previously only age worked)
-   **Out-of-Domain Schools**: Fixed crashes when schools moved outside domain (Surveys and OutputRegion)
-   **Map Distribution**: Corrected legacy_fixed map distribution method to avoid indexing bugs
-   **Movement Map Timing**: Fixed first time-step map comparison indexing

### Mortality and Feeding

-   **Larval Mortality**: Starvation mortality now only applies if species is older than first feeding age
-   **Foraging Accessibility**: Fixed accessibility matrix reading with non-standard formatting
-   **Natural Mortality**: Corrected incrementNDead for resources and background species
-   **Mortality Outputs**: Numerical stability improved for mortality outputs by stage (EGG, JUVENILE, ADULT)
-   **Mortality Output Indexing**: Fixed `BiomassDietStageOutput.write` method indexing from 2D to 1D conversion

### Fisheries and Catches

-   **Fished Biomass**: Corrected `getFishedBiomass` function to avoid null pointer exceptions
-   **Discard Biomass**: Fixed calculation of `discardedBiomass` (no longer exists as separate variable)
-   **Fishing Effort**: Corrected MPA initialization to properly update fishing effort based on restart files
-   **Fishing Normalization**: Flag to disable fisheries map normalization works with old fishery class
-   **Catch by Class**: Fixed temporal and spatial output computation for catch by trophic level

### Background and Resource Species

-   **Background Species Position**: Fixed initialization of background species position (was all at 0,0)
-   **Background Schools in Diet**: Added background schools to diet computation
-   **Resource Output**: Fixed bug in resource output where background species were managed with ResourceForcing
-   **Background Species Initialization**: Corrected `getAllSchools` usage, replaced by `getSchools`

### Output and Netcdf

-   **NetCDF Output Tests**: Corrected outputs (especially NetCDF) with comprehensive JUnit testing
-   **Spatial Output Consistency**: Removed dead schools from spatial outputs to match aggregated CSV outputs
-   **Relative Biomass Output**: Fixed when genetic and/or bioenergetic modules are enabled
-   **Netcdf Multithread Mode**: Forced NetCDF3 format (NetCDF4 causes errors in multithread mode)
-   **Netcdf Chunking**: Updated chunking strategy with standard as default
-   **Output Distribution**: Fixed `cutOff` parameter not being taken into account in `OutputWholeRegion`

### Configuration and Initialization

-   **Maturity and Gonad Weight**: Corrected initialization of gonad weight and maturity in relative biomass initialization
-   **Growth and Mortality Penalties**: Reconfiguration of calibration penalty estimates
-   **Catch-at-Length Validation**: Improved error handling for null or zero catch-at-length data
-   **File Path Reconstruction**: Fixed resource reading with proper file path handling
-   **Array Indexing**: Corrected multiple array indexing issues throughout codebase
-   **Viviparous Initialization**: Corrected initialization logic for viviparous fish species

### R Package Functions

-   **Configuration Reading**: Improved configuration validation and reading
-   **Parameter Retrieval**: Fixed `.getPar` to `get_par` with proper species offset handling
-   **Output Reading**: Enhanced robustness when reading calibration results with multiple files
-   **Yield Calculations**: Corrected yield computations for different output formats
-   **Predator Pressure**: Fixed predator pressure calculation by region/survey

## Technical Improvements

-   **GitHub Actions**: Automatic testing of R package (build and check) and Java code (Maven build)
-   **Code Organization**: Extensive refactoring for consistency across modules
-   **Documentation**: Improved Quarto documentation with MyST parser migration
-   **Memory Management**: Replaced HashMaps with arrays where possible for performance
-   **Java Compilation**: Full migration to Maven build system
-   **NetCDF Library**: Updated to latest NetCDF Java library with compression features
-   **Error Messages**: Enhanced error reporting and validation messages
-   **Performance**: Reduced computation time for new fisheries implementation and school management

## Version Information

-   **Java**: Maven-based compilation with updated NetCDF Java library
-   **R Package**: Enhanced with new calibration functions and analysis capabilities
-   **Configuration**: Automatic incremental updates from Osmose 3 and 4.x versions

## Manually written release notes for OSMOSE 4.4.0

### Parameter changes

-   Renaming of output fisheries parameters (`output.fishery.*` to `output.fisheries.*`).
-   Multispecies fisheries are activated using `process.multispecies.fisheries.enabled` (replaces fisheries.enabled).
-   **Annual larval mortality is now provided in rates/year instead of rates/time-step. So make sure to update your configuration file accordingly (multiply your original rates by** `simulation.time.ndtperyear`)
-   Possiblity to provide additional mortality rates in log format using `"mortality.additional.rate.log.spX` and
-   Parameter `predation.ingestion.rate.max.bioen.spX` has been renamed `predation.ingestion.rate.max.bioen.spX` to be consistent with the classical Osmose parameter names
-   Parameter `predation.coef.ingestion.rate.max.larvae.bioen.sp` has been renamed `predation.larval.ingestion.rate.increase.ratio.spX`
-   Parameter `species.bioen.maturity.eta.spX` has been renamed `species.maturity.eta.spX`
-   Parameter `species.bioen.maturity.r.spX` has been replaced by `species.maturity.r.spX`
-   Parameter `species.bioen.maturity.m0.spX` has been replaced by `species.bioen.maturity.m0.spX`
-   Parameter `species.bioen.maturity.m1.spXX` has been replaced by `species.maturity.m1.spX`
-   Parameter `fisheries.enabled` has been replaced by `module.multispecies.fisheries.enabled`
-   Parameter `simulation.bioen.enabled` has been replaced by `module.bioenergetics.enabled`
-   Parameter `simulation.genetic.enabled` has been replaced by `module.genetics.enabled`
-   Parameter `economy.enabled` has been replaced by `module.bioeconomics.enabled`

### New features

-   Outputs:
    -   Add the bioenergetic output of size at maturity.
    -   Adding computation performance outputs (`output.cpu.performance.enabled`)
    -   Adding additional genetic outputs (allele frequency outputs, observed and expected heterotrophy)
    -   Output of fishery accessible biomass, yields in abundance
    -   Averaged genotype outputs instead of instantaneous outputs.
    -   Mortality outputs can now be recorded on different regions
-   Possibility to have simplified bioenergetics for species with lack of data (invertebrates).
-   Adding some patches to run Osmose on a single cell. This mode is activated by setting `grid.single.cell.enabled` to `True`. **This mode may be much slower than 2d simulations**
-   Adding gradient based spatial distribution
-   New management of background species and schools. Their biomass is now initialized with the `species.biomass.spX` and `species.biomass.nsteps.year.spX` parameters. And their schools are managed in the same way as focal species schools, contrary to having one school per grid cell as in the original implementation.
-   Fishing by fisheries is now computed in abundance rather than biomass (Ricardo Oliveros-Ramos change).
-   **New**: fully functional economic module (Emy Cottrant's post-doct)

### Bug fix

-   Correction of the `BiomassDietStageOutput.write` method. There was a bug in the indexing during the conversion from 2D to 1D.
-   In `SimulationStep`, `movementProcess.run()` is now called before `indicators.initStep()`. This in order to make sure that the initial biomass is always less than the total predated biomass.
-   In spatial outputs (`SpatialAbundanceOutput.java` and `SpatialBiomassOutput.java`), only cut-off ages were working, not cut-off length. This has been corrected.
-   Correction in MPA initialization when using restart files. In some cases, the `isUpToDate` variable of the `setMPA` method was always `true`, therefore the fishing effort was never modified. Besides, MPA index as defined in the configuration files (`.mpaXX`) were expected to start from 0 onward. Now any index can work, as for species.
-   Correction in the reading of files from regular expressions. Canonical path was replaced by absolute paths to make it work on Windows
-   Correct a bug in `Surveys` and `OutputRegion`. When school was out of the domain, the code crashed. Fixed by assuming that outside schools are out of the survey region.
-   Added the background schools in the computation of diets.
-   Correct a bug in resource output: background species are no longer managed with `ResourceForcing`.
-   Correct a bug in the `NetcdfPopulator.java` when genetic is enabled. The genotype was not instanciated.
-   Correct a bug in the `RelativeBiomassPopulator.java` when genetic and/or bioenegetic was on. Genotype was not instanciated and maturity and gonadic weight were not initialized