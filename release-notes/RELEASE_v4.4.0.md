# 🌊 OSMOSE 4.4.0 — Java Release Notes

## 1. 🎯 Overview

This release introduces **major architectural improvements**, **new biological capabilities**, **expanded economic modeling**, and **significant refactoring** of internal processes, especially:

-   **Background species system overhaul**
-   **Region‑aware mortality**
-   **Fishing & discards tracked in numbers**
-   **Maturity ogive (stochastic reproduction)** (see [here](https://github.com/osmose-model/osmose/wiki/Reproduction#stochastic-maturity) for details)
-   **New egg size bioenergetics formulation**
-   **Reworked restart logic**
-   **Large configuration key migration**
-   **Simplified bioenergetics computation when lack of data** (see [Simplified Bioenergetics](https://github.com/osmose-model/osmose/wiki/Bioenergetic-module:-Simplified-equations))
-   **Post-reproduction mortality** (see [here](https://github.com/osmose-model/osmose/wiki/Mortality:-Post%E2%80%90reproduction-mortality))

These changes modernize the modeling framework and extend its biological, ecological, and economic realism.

------------------------------------------------------------------------

## 1. ⚙️ Configuration Changes (User Level)

### 1.1. Renamed (breaking) configuration keys

#### Module toggles

| Old Key                      | New Key                                 |
|------------------------------|-----------------------------------------|
| `simulation.bioen.enabled`   | `module.bioenergetics.enabled`          |
| `simulation.genetic.enabled` | `module.genetics.enabled`               |
| `fisheries.enabled`          | `module.multispecies.fisheries.enabled` |
| `economy.enabled`            | `module.bioeconomics.enabled`           |

#### Restart system

| Old Key | New Key |
|---------------------------------|---------------------------------------|
| `output.restart.enabled` | `simulation.restart.enabled` |
| `output.restart.recordfrequency.ndt` | `simulation.restart.recordfrequency.ndt` |
| `output.restart.spinup` | `simulation.restart.spinup.nstep` / `.nyear` |

#### Bioenergetic module

| Old Key | New Key |
|------------------------------------|------------------------------------|
| `predation.ingestion.rate.max.bioen.spX` | `predation.ingestion.rate.max.bioen.spX` |
| `predation.coef.ingestion.rate.max.larvae.bioen.sp` | `predation.larval.ingestion.rate.increase.ratio.spX` |
| `species.bioen.maturity.eta.spX` | `species.maturity.eta.spX` |
| `species.bioen.maturity.r.spX` | `species.maturity.r.spX` |
| `species.bioen.maturity.m0.spX` | `species.bioen.maturity.m0.spX` |
| `species.bioen.maturity.m1.spXX` | `species.maturity.m1.spX` |

These renames are **mandatory for existing configurations**.

------------------------------------------------------------------------

### 1.2. New configuration keys

#### Post-reproduction mortality

-   `species.reproduction.strategy.sp#` = `iteroparous` or `semelparous`
-   `species.reproduction.postspawning.survivaltime.sp#` = post-reproduction survival time (in years). Used only for `semelparous`

#### Simulation & reproducibility

-   `simulation.fixed.seed.enabled`: used to control the stochasticity of a simulation and allow replicable runs
-   `simulation.nschool.multiplier`: Parameter to multiply the number of schools for all species (default one):

#### Maturity ogive

See [here](https://github.com/osmose-model/osmose/wiki/Reproduction#stochastic-maturity) for details
- `species.maturity.mode`: controls whether stochastic (`stochastic`) or deterministic (`legacy`) production is used.
- `species.maturity.l50.sp#`: mean of the gaussian - `species.maturity.l75.sp#`

#### Egg size (bioenergetics)

-   `species.egg.density.sp#`

#### Background species & biomass scaling

-   `species.multiplier.sp#`
-   `species.multiplier.log.sp#`

#### Background movement & map handling

-   `movement.checks.enabled`
-   `movement.randomwalk.range.sp#`
-   `movement.netcdf.enabled`

#### Bioeconomics module

-   `species.stock.elasticity.sp#`
-   `baseline.costs.t0.sp#`
-   `price.time.trend.sp#`
-   `species.consumption.elasticity.sp#`
-   `species.sizeconsumption.elasticity.sp#`
-   `species.size.preference.sp#`
-   `weight.fish.consumption`
-   `substitution.elasticity`
-   `elasticity.demand.fish`

#### Gradient based movements

-   `movement.random.walk.coef.sp#`
-   `movement.base.search.radius.sp#`

#### Predation on LTL

-   `simulation.resources.computePercent.legacy`

#### Bioenergetics

-   `species.bioenergetics.model.sp`: `full` or `simple`
-   `predation.efficiency.critical.spX`
-   `species.temperature.tmin.spX`
-   `species.temperature.tmax.spX`
-   `species.temperature.topt.spX`
-   `predation.ingestion.rate.max.spX`

------------------------------------------------------------------------

## 3. ⭐ User‑Facing Changes

### 3.1. Major New Features

#### Gradient-based movement distribution

-   Gradient based spatial distribution has been included ([Gradient based movements](https://github.com/osmose-model/osmose/wiki/Movement:-gradient-based-movements))

#### LTL predation

-   Possibility to use a log version of the `computePercent` method

#### ✔ Region‑specific mortality

Mortality is now tracked **per output region**. Arrays such as `nDead` and `ageDeath` are now 2‑dimensional: `[region][mortality_cause]`.

#### ✔ Fishing & discards tracked in numbers

Fishing/discard tracking moved from **biomass** to **abundance**:

-   `fishedBiomass` → `fishedAbundance`
-   `discardedBiomass` → `discardedAbundance`
-   Biomass now computed on demand using abundance × mass functions.

#### ✔ Background species & maps overhaul

A new full subsystem handles background species:

-   Background schools are now real “schools” integrated into the simulation
-   Background movement powered by maps (CSV/NetCDF)
-   New classes for background distribution and filtering
-   Background schools populate the grid using class-based biomass and spatial maps
-   Full per-timestep movement/redistribution support
-   Consistent inclusion of background schools in outputs

------------------------------------------------------------------------

### 3.2. Biological Process Enhancements

#### ✔ Stochastic maturity ogive

Optional probabilistic (not deterministic) maturity (see [here](https://github.com/osmose-model/osmose/wiki/Reproduction#stochastic-maturity) for details):

-   Uses parameters `species.maturity.l50.sp#` & `species.maturity.l75.sp#`
-   A normal CDF calculates probability of maturing between timesteps
-   Once mature, always mature

#### ✔ Improved egg size under bioenergetics

Under the bioenergetics module:

-   Egg size uses a **spherical egg** model based on density
-   New config key: `species.egg.density.sp#`

This replaces the adult-coefficient-based egg mass–length conversion.

------------------------------------------------------------------------

### 3.3. Expanded Bioeconomics Module

Economic modelling expanded significantly:

-   Harvesting costs
-   Consumer utility of fish consumption
-   Species & size‑class prices
-   Profit and profit margin
-   Multi‑elasticity modeling
-   Multi‑dimensional harvested/accessible biomass arrays: `[fishery][species][size-class]`

New parameters include elasticities, cost time trends, size preferences and more.

------------------------------------------------------------------------

### 3.4. Simulation & Restart Enhancements

#### ✔ More robust restart system

-   Restart keys migrated to `simulation.restart.*`
-   Intelligent fallback between plain and per-rank files
-   Required global attribute in restart NetCDF: `step`
-   Improved error messages

#### ✔ Deterministic simulation mode

`simulation.fixed.seed.enabled` enforces reproducible random number generation for:

-   Movement
-   Reproduction
-   Genetics
-   Resource forcing
-   Fishing selectivity

------------------------------------------------------------------------

### 3.5. Output Behavior Improvements

#### ✔ NetCDF chunking default changed

Default chunking now uses the **standard NetCDF-4 chunking strategy** for improved performance.

#### ✔ Background species always included in outputs

Refactor ensures consistent treatment of background schools in spatial, fisheries, mortality, and predator-prey outputs.

------------------------------------------------------------------------

## 4. 💥 Breaking Changes

#### ⚠ Fishing/discards API changed

-   `fishedBy()` / `discardedBy()` replaced by `fishedNBy()` / `discardedNBy()`
-   Fishing inputs/outputs now primarily use **numbers**, not **biomass**

#### ⚠ Mortality API changed

`incrementNdead()` now requires the timestep:

``` java
incrementNdead(MortalityCause cause, double nDead, int timeStep)
```

#### ⚠ Region indexing mandatory

`nDead`, `ageDeath`, caught fish, discards, mortality outputs and other arrays now include a **region dimension**.

#### ⚠ Restart NetCDF must include global attribute `step`

Restart files missing this field will cause the simulation to abort.

#### ⚠ Many configuration keys renamed or restructured

Old keys **will not work** without migration.

#### ⚠ Species background biomass reading and scaling logic changed

Existing background configurations relying on older semantics may need adjustment.

------------------------------------------------------------------------

## 5. 👨‍💻 Developer‑Facing Notes

### 5.1. Background Species Refactor

A very large subsystem rewrite:

-   `BackgroundProcess` executes each timestep
-   `BackgroundMapDistribution` controls school movement
-   `BackgroundMapSet` handles map loading (CSV/NetCDF)
-   Schools moved to cells via probability maps
-   Schools reinitialized/reset when maps change
-   Background species are tracked with real biomass × class distributions

This will impact any tool, plugin, or analysis code accessing background species.

------------------------------------------------------------------------

### 5.2. Aggregation Interface Changes

New abstract methods required in `AbstractSchool` / `IAggregation`:

-   `isMature()`
-   `isAlive()`
-   `getClassIndex()`
-   `isOut()`

Plus new utilities:

-   `resetAccessiblePreyIndex()`
-   `addAccessiblePreyIndex()`
-   `getAccessiblePreyIndex()`

Internal arrays:

-   `nDead` and `ageDeath` become 2D arrays `[region][cause]`.

------------------------------------------------------------------------

### 5.3. Configuration Parsing Changes

-   Many key comparisons now use `equalsIgnoreCase()`
-   Path resolution uses `getAbsolutePath()` instead of `getCanonicalPath()` (fix for Windows)
-   Restart key structure updated
-   More defensive parsing for command-line arguments

------------------------------------------------------------------------

### 5.4. Bioeconomics Engine Rewrite

Developers integrating with the economy module must update:

-   New matrices for biomass accessibility and harvest
-   New cost and price calculations
-   Multi-parameter elasticity and preference models
-   Utility & profit functions rewritten
-   Many new configuration points

------------------------------------------------------------------------

### 5.5. Initialization & Restart Logic

-   Genetic diversity and trait values may now be restored from restart files
-   Population initialization more modular
-   Schools created during reproduction have improved genetic initialization logic

------------------------------------------------------------------------

## 6. 🐞 Bug Fixes

-   Correction of the `BiomassDietStageOutput.write` method. There was a bug in the indexing during the conversion from 2D to 1D.
-   In `SimulationStep`, `movementProcess.run()` is now called before `indicators.initStep()`. This in order to make sure that the initial biomass is always less than the total predated biomass.
-   In spatial outputs (`SpatialAbundanceOutput.java` and `SpatialBiomassOutput.java`), only cut-off ages were working, not cut-off length. This has been corrected.
-   Correction in MPA initialization when using restart files. In some cases, the `isUpToDate` variable of the `setMPA` method was always `true`, therefore the fishing effort was never modified. Besides, MPA index as defined in the configuration files (`.mpaXX`) were expected to start from 0 onward. Now any index can work, as for species.
-   Correction in the reading of files from regular expressions. Canonical path was replaced by absolute paths to make it work on Windows
-   Correct a bug in `Surveys` and `OutputRegion`. When school was out of the domain, the code crashed. Fixed by assuming that outside schools are out of the survey region.
-   Correct a bug in the `NetcdfPopulator.java` when genetic is enabled. The genotype was not instanciated.
-   Correct a bug in the `RelativeBiomassPopulator.java` when genetic and/or bioenegetic was on. Genotype was not instanciated and maturity and gonadic weight were not initialized
-   Species name validation now supports hyphens

------------------------------------------------------------------------

## 7. 📦 Summary Table

| Category | Summary |
|--------------------|----------------------------------------------------|
| **New Features** | Region‑specific mortality, stochastic maturity ogive, density-based egg size, background movement maps, expanded bioeconomics |
| **Breaking Changes** | New fishing/discard APIs, mortality API requires timestep, config key renames, restart requirements, region dimension added |
| **Behavioral Changes** | NetCDF chunking change, background school process rewrite, new maturity logic, numbers-based fishing |
| **Developer Refactors** | Background species subsystem, economy engine, school interface changes, initialization/restart logic |
| **Bug Fixes** | Improved validation, safer parsing, corrected map logic, more robust biomass and egg handling |

------------------------------------------------------------------------

## 8. ✔ Short Summary

-   Region‑specific mortality
-   Fishing/discards tracked in numbers
-   Background school overhaul (movement maps, biomass classes)
-   Stochastic maturity ogive (L50/L75)
-   Egg size from spherical-density model
-   Bioeconomics: costs, utility, prices, profit
-   Config key migration to module.\* and simulation.restart.\*
-   Restart requires global attribute 'step'
-   NetCDF chunking uses 'standard' by default

------------------------------------------------------------------------

# 🌊 OSMOSE 4.4.0 — R Release Notes

## 🚀 Highlights

*   **New end-to-end calibration workflow**: add setup, test, run-model, and check utilities for OSMOSE calibrations, including automatic generation of parameter bounds/templates and observed-data scaffolding. [\[2\]](?web=1)
*   **Much richer output handling**: improved reading/structuring of fishery & survey outputs, including by-fishery-by-species arrays, time/class aggregation, and functional group rollups. [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   **Major `get_var()` upgrade**: new subsetting, safer behavior, and replicate sampling features make extraction of outputs more flexible and robust. [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

## ✨ New Features

### 1) Calibration framework (new public API)

A complete set of calibration utilities was added, centered around:

*   **`osmose_calibration_setup()`**
    Creates a calibration directory structure, writes guess/min/max/phase parameter files, installs scripts/templates, runs pre-flight model checks, and creates (or consumes) observed-data templates plus calibration settings. [\[2\]](?web=1), [\[?web=1\]](?web=1)

*   **`osmose_calibration_test()`**
    Runs automated tests (data compatibility + sequential and parallel “smoke tests”) to validate that the calibration environment is correctly configured. [\[2\]](?web=1), [\[?web=1\]](?web=1)

*   **`osmose_calibration_runmodel()`**
    Runs the calibration-ready model using the calibration directory and parameter sets, with safeguards against mixing calibrated vs non-calibrated parameters. [\[2\]](?web=1), [\[?web=1\]](?web=1)

*   **`osmose_calibration_check()`**
    Re-runs the model with best parameters from results/restart and returns a calibration object combining simulated/observed/settings/cv. [\[2\]](?web=1), [\[?web=1\]](?web=1)

*   **`osmose_calibration_outputs()`**
    Produces a curated list of calibration-ready outputs (surveys, landings, catch-at-length, penalties for growth/mortality/collapse/outburst, etc.). [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

### 2) Calibration parameter helpers (new internal + exported utilities)

New helpers make it easier to transform, replicate, and write parameter sets:

*   Parameter manipulation & transformations:
    *   `set_par()`, `transform_par()`, `replicate_par()` [\[2\]](?web=1), [\[?web=1\]](?web=1)
    *   `logit()`, `ilogit()` helpers [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   Parameter extraction logic for calibration/grouped parameters:
    *   `get_par2()`, `get_par_phase()`, `get_calibration_parameters()`, `get_osmose_parameter()` [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   Writers:
    *   `write_osmose_parameter()` (writes guess/min/max/phase entries for a parameter block) [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   Calibration settings creators:
    *   `.create_calibration_settings()` plus type-specific helpers (simple/survey) [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

### 3) New S3 methods for usability (`head()`/`tail()` on OSMOSE outputs)

New `head.*` and `tail.*` methods preserve OSMOSE output class when slicing:

*   `tail.osmose.biomass`, `tail.osmose.abundance`, `tail.osmose.yield`, `tail.osmose.yieldN` [\[?web=1\]](?web=1), [\[2\]](?web=1)
*   `head.osmose.biomass`, `head.osmose.abundance`, `head.osmose.yield`, `head.osmose.yieldN` [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

### 4) Configuration checks: egg density validation

A new internal check warns when egg density appears unrealistic:

*   `.check_density()` computes density from egg weight and egg size and emits warnings for “lower than seawater”, “abnormal”, or “wrong” density ranges. [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

## 📈 Output & Data Handling Improvements

### 1) `readOsmoseFiles()` gains survey-aware grouping and safer behavior

*   New argument **`bySurvey`** (in addition to `bySpecies`) supports more structured output folder parsing. [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   Combined grouping (`bySpecies + bySurvey`) is supported, with logic based on how many “grouping dimensions” are requested. [\[?web=1\]](?web=1), [\[2\]](?web=1)
*   Returns **`NULL` when no matching files exist**, avoiding downstream failures. [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

### 2) Fishery outputs expanded and reshaped

New NetCDF-based fishery outputs are supported and integrated:

*   Added reading/representation of:
    *   `yieldByFisheryBySpecies`, `yieldNByFisheryBySpecies`, `accessibleBiomassByFishery` [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   New reshaping and aggregation helpers:
    *   `.reshapeFishery()` (with `by="fishery"|"species"` and `aggregate=TRUE/FALSE`) [\[2\]](?web=1), [\[?web=1\]](?web=1)
    *   Aggregations across time/class/year:
        *   `.aggregate_catch_bytime()`, `.aggregate_catch_byclass()`, `.aggregate_catch_byyear()` [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   New “observed.\*” fields are produced from reshaped fishery catches (e.g., `observed.landings`, `observed.discards`) to feed calibration workflows. [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

### 3) Surveys are treated as first-class outputs

*   Survey outputs now support `bySurvey` and include additional survey datasets (e.g., mortality/predator pressure) where available. [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   Survey outputs can be filtered to their target species based on config. [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

### 4) More robust CSV/NetCDF reading

*   CSV readers now fail gracefully and return `NULL` on read errors instead of erroring. [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   NetCDF reader `.read_osmose_ncdf()` was hardened:
    *   uses `try(nc_open(...))` and returns `NULL` if opening fails [\[?web=1\]](?web=1), [\[2\]](?web=1)
    *   adds dimension names (`time`, `species`, `fishery`, `replicates`) and handles `species_names` / `fisheries_names` attributes [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

## 🧰 API Enhancements & Behavior Changes

### 1) `get_var()` is significantly enhanced

`get_var.osmose()` now supports:

*   hierarchical subsetting:
    *   `sp`, `srv`, `fsh` arguments for species / survey / fishery selection [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   safer behavior:
    *   `no.error=TRUE` returns `NULL` instead of throwing when variables are missing [\[?web=1\]](?web=1), [\[2\]](?web=1)
*   output shaping and replicate sampling:
    *   `drop`, `size`, `random`, `replace` for slicing/sampling replicates [\[?web=1\]](?web=1), [\[2\]](?web=1)
*   new generic helpers/methods:
    *   `get_var.default()`, `get_var.osmose.calibration()`, `get_var.list`, `get_var.NULL` [\[2\]](?web=1), [\[?web=1\]](?web=1)

> **Potential breaking change**: code that relied on `get_var()` erroring on missing variables may need updates if `no.error=TRUE` is used or propagated. [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

### 2) Plotting behavior improved for `plot.osmose()`

*   `plot.osmose()` now passes derived metadata (`initialYear`, `freq`) when plotting extracted variables, improving plot correctness for time axes. [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

### 3) Printing `osmose` objects is more informative

*   `print.osmose()` now prints model name/version from configuration, replicate count, species list, and a compact list of available outputs (excluding empty/system fields). [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

## 🛠️ Refactoring, Maintenance, and Internal Changes

### Cache management cleanup

*   `cacheManager`, `cachePath`, `flushCache`, `updateCache` were refactored:
    *   consistent `TRUE/FALSE`, clearer assignments, explicit `return(invisible(NULL))` [\[?web=1\]](?web=1), [\[2\]](?web=1)
    *   updated jar URL naming consistency for 4.3.2/4.3.3 entries [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

### Version parsing supports 4-part versions

*   Version validation now accepts **`X.Y.Z.W`** in addition to `X`, `X.Y`, `X.Y.Z`. [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

### Output trimming helper

*   New `.trim_matrix()` drops trailing all-`NA` columns more safely when reading some outputs. [\[2\]](?web=1), [\[?web=1\]](?web=1)

***

### Minor consistency/documentation improvements

*   Author name capitalization normalized (e.g., “Nicolas BARRIER”). [\[?web=1\]](?web=1), [\[2\]](?web=1)
*   Roxygen imports expanded (notably `utils::relist/as.relistable` and several `calibrar` functions). [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

## ⚠️ Notes for Upgraders

### Potential breaking/behavioral changes

*   **Fishery outputs**: previous `yieldByFishery` structures are replaced/extended by new arrays and reshaped derivatives (`landingsBy*`, `discardsBy*`, etc.). If you depended on legacy fields, update to the new names. [\[2\]](?web=1), [\[?web=1\]](?web=1)
*   **`readOsmoseFiles()` grouping**: introducing `bySurvey` changes how files can be grouped/parsed; calling code may need to specify `bySpecies`/`bySurvey` explicitly. [\[?web=1\]](?web=1), [\[2\]](?web=1)
*   **`get_var()` signature expanded**: if you were using `...` forwarding into `get_var.osmose()` you may want to ensure no name collisions with new formal args (`sp`, `srv`, `fsh`, `size`, etc.). [\[?web=1\]](?web=1), [\[2\]](?web=1)

***

<!--
























# OSMOSE Release Notes - Version 4.4.0

Release notes capturing changes from OSMOSE 4.3.3 to 4.4.0

## New Features

### Model Capabilities

-   **Single Cell Mode**: Added possibility to run simulations on a single grid cell via `grid.single.cell.enabled` parameter
-   **Simplified Bioenergetics Module**: Support for simplified bioenergetics for data-poor species (e.g., invertebrates)
-   **Gradient-based Spatial Distribution**: New spatial distribution method for fish movements
-   **Marine Ecological Outputs**: Enhanced outputs for Marine project simulations
-   **Background Species Management**: Improved initialization and management of background and school species using biomass and `species.biomass.nsteps.year.spX` parameters
-

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

-   Homogenization of module names:
    -   `simulation.bioen.enabled` → `module.bioenergetics.enabled`
    -   `simulation.genetic.enabled` → `module.genetics.enabled`
    -   `economy.enabled` → `module.bioeconomics.enabled`
    -   `fisheries.enabled` → `module.multispecies.fisheries.enabled`
-   Bioen-Osmose parameters for consistency with classical Osmose:
    -   `predation.ingestion.rate.max.bioen.spX` → `predation.ingestion.rate.max.spX` (consistency)
    -   `predation.coef.ingestion.rate.max.larvae.bioen.sp` → `predation.larval.ingestion.rate.increase.ratio.spX`
    -   `species.bioen.maturity.eta.spX` → `species.maturity.eta.spX`
    -   `species.bioen.maturity.r.spX` → `species.maturity.r.spX`
-   `output.fishery.*` → `output.fisheries.*` (output parameters)
-   `output.restart.*` → `simulation.restart.*` (output restart parameters)
-   `fishing.mortality.enabled` → `simulation.fishing.mortality.enabled` (clarified)
-   `species.file.spX` renamed in `species.biomass.file.spX`

### New Parameters

-   `grid.single.cell.enabled`: Enable single-cell mode simulation
-   `output.cpu.performance.enabled`: Output CPU performance metrics
-   Background species:
    -   `species.biomass.spX`: Initialize background species biomass by providing a time-series
    -   `species.biomass.nsteps.year.spX`: Initialize background species biomass by providing a time-series
-   `mortality.additional.rate.log.spX`: Additional mortality in log format
-   `simulation.nschool.multiplier`: Scale schools across focal species
-   `population.initialization.biomass.log.sp#`: Population initialization biomass in log mode.
-   `reproduction.mode`: Specify reproduction mode (oviparous/viviparous/etc.)
-   `species.weight.at.larvae.growth.threshold.age` (W0): Optional (default 0)
-   `predation.success.disabled.sp`: Disable predation success for specific species
-   `simulation.resource.computePercent.legacy`: Legacy resource computation mode
-   `simulation.fixed.seed.enabled`: fix the seed for all the stochastic process (easy control for replicability)
-   Fisheries grouping parameters for disaggregated analysis
-   Survey selectivity and other survey-specific parameters

### Deprecated Parameters

-   Paramters to fix seeds are now deprecated and replaced by `simulation.fixed.seed.enabled`:
    -   `population.initialization.randomseed.fixed`
    -   `genetics.randomseed.fixed`
    -   `movement.randomseed.fixed`
    -   `reproduction.randomseed.fixed`
    -   `stochastic.mortality.randomseed.fixed`

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

------------------------------------------------------------------------

# Manually written release notes for OSMOSE 4.4.0

## Parameter changes

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

## New features

-   Outputs:
    -   Add the bioenergetic output of size at maturity.
    -   Adding computation performance outputs (`output.cpu.performance.enabled`)
    -   Adding additional genetic outputs (allele frequency outputs, observed and expected heterotrophy)
    -   Output of fishery accessible biomass, yields in abundance and biomass
    -   Averaged genotype outputs instead of instantaneous outputs.
    -   Mortality outputs can now be recorded on different regions
-   Possibility to have simplified bioenergetics for species with lack of data (invertebrates).
-   Adding some patches to run Osmose on a single cell. This mode is activated by setting `grid.single.cell.enabled` to `True`. **This mode may be much slower than 2d simulations**
-   Adding gradient based spatial distribution
-   New management of background species and schools. Their biomass is now initialized with the `species.biomass.spX` and `species.biomass.nsteps.year.spX` parameters. And their schools are managed in the same way as focal species schools, contrary to having one school per grid cell as in the original implementation.
-   Fishing by fisheries is now computed in abundance rather than biomass (Ricardo Oliveros-Ramos change).
-   **New**: fully functional economic module (Emy Cottrant's post-doct)

## Bug fix

-   Correction of the `BiomassDietStageOutput.write` method. There was a bug in the indexing during the conversion from 2D to 1D.
-   In `SimulationStep`, `movementProcess.run()` is now called before `indicators.initStep()`. This in order to make sure that the initial biomass is always less than the total predated biomass.
-   In spatial outputs (`SpatialAbundanceOutput.java` and `SpatialBiomassOutput.java`), only cut-off ages were working, not cut-off length. This has been corrected.
-   Correction in MPA initialization when using restart files. In some cases, the `isUpToDate` variable of the `setMPA` method was always `true`, therefore the fishing effort was never modified. Besides, MPA index as defined in the configuration files (`.mpaXX`) were expected to start from 0 onward. Now any index can work, as for species.
-   Correction in the reading of files from regular expressions. Canonical path was replaced by absolute paths to make it work on Windows
-   Correct a bug in `Surveys` and `OutputRegion`. When school was out of the domain, the code crashed. Fixed by assuming that outside schools are out of the survey region.
-   Added the background schools in the computation of diets.
-   Correct a bug in resource output: background species are no longer managed with `ResourceForcing`.

-->
