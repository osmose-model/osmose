
#' Creates OSMOSE configuration templates
#'
#' @param file File to write the template.
#' @param what Type of template desired. Currently: "initialisation".
#' @param control Additional arguments provided for each type of template. See details.
#' @param ... Extra arguments passed to each method.
#'
#' @return Called by its side effects, it creates a file with the desired template.
#' @details
#' For the template for the initialisation (\code{what='initialisation'}), control arguments
#' can be supplied as a list containing the named elements \code{'file_biomass'}, \code{'file_yield'},
#' providing the full path to the file containing the biomass and yield. If ommited, a dummy
#' value will be used in the template (that can be replaced programmatically after the file has
#' been created). Additionally, an element named \code{'path_cal'} can be used to provide a path
#' to the catch-at-length files, but each line will need to be modified to add the species-specific
#' file, as one file per species is expected. 
#' 
#' @examples 
#' \dontrun{
#' osmose_template(input=configFile, file="initialisation_template.osm")
#' osmose_template(input=configFile, file="initialisation_template.osm",
#'   control=list(file_biomass="dummy_biomass.csv", 
#'   file_yield="dummy_yield.csv", path_cal="mypath"))}
#' @export
#'
#' @inheritParams run_osmose
osmose_template = function(input, file, what="initialisation", control=list(), ...) {
  
  input = suppressWarnings(normalizePath(input, mustWork=TRUE))
  
  if(missing(file)) stop("A 'file' to create the template must be provided.")
  
  if(!is.null(attr(file, "path"))) file = file.path(attr(file, "path"), file)
  file  = suppressWarnings(normalizePath(file))
  
  if(identical(input, file)) stop("Input configuration file and template output file cannot be identical.")
  
  conf = read_osmose(input=input)
  
  out = switch(what,
               "initialisation"  = .osmose_template_init(conf, file=file, control=control),
               "initialization"  = .osmose_template_init(conf, file=file, control=control),
               stop(sprintf("what='%s' is not supported.", type))
  )  
  
  return(invisible(out))
  
}


#' Create a tidy configuration compatible with OSMOSE v3.0.0 or newer
#'
#' @param key Optional argument providing a name for this particular configuration.
#' By default, "reference" is used.
#'
#' @return Invisibily, the path to the new tidy configuration. Used mostly for the side
#' effect of reorganising an old configuration.
#' @export
#'
#' @inheritParams run_osmose
#' @examples 
#' \dontrun{
#' osmose_tidy(input=configFile)}
osmose_tidy = function(input, key="reference") {
  
  on.exit(if(file.exists(".testx.R")) file.remove(".testx.R"))
  
  osmose:::.tidy_conf(input=input, file = ".testx.R", checks=FALSE)
  test = read_osmose(input=".testx.R")
  test = get_par(test, par="^osmose.configuration", invert=TRUE, as.is=TRUE)
  model = tolower(get_par(test, "prefix"))
  version = tolower(get_par(test, "version"))
  version = gsub(gsub(version, pattern='[[:space:]]', replacement=""), 
                 pattern="update|release", replacement=".")
  version = gsub(version, pattern="osmose", replacement="")
  
  the_files = test[sapply(test, FUN=.is_path)]
  
  output = sprintf("osmose-%s", model)
  ref = sprintf("osmose-%s-v%s-%s", model, version, key)
  outDir = file.path(output, ref)
  if(dir.exists(output)) stop(sprintf("Directory '%s' already exists, change the key.", outDir))
  dir.create(outDir, recursive=TRUE)
  dir.create(file.path(output, "forcing"), recursive=TRUE)
  dir.create(file.path(output, "utils"), recursive=TRUE)
  dir.create(file.path(output, "data"), recursive=TRUE)
  
  dir.create(file.path(outDir, "output"), recursive=TRUE)
  dir.create(file.path(outDir, "input", "fisheries"), recursive=TRUE)
  dir.create(file.path(outDir, "input", "flux"), recursive=TRUE)
  dir.create(file.path(outDir, "input", "reproduction"), recursive=TRUE)
  dir.create(file.path(outDir, "input", "mortality"), recursive=TRUE)
  
  dir.create(file.path(output, "forcing", "distribution"), recursive=TRUE)
  dir.create(file.path(output, "forcing", "resources"), recursive=TRUE)
  
  # resources: species.file
  files = unique(unlist(get_par(the_files, "species.file")))
  file.copy(from=files, to=file.path(output, "forcing", "resources"))
  test = .update_path(config=test, par="species.file", 
                      path=file.path("..", "forcing", "resources"))
  # movement: movement.file
  files = unique(unlist(get_par(the_files, "^movement.file")))
  file.copy(from=files, to=file.path(output, "forcing", "distribution"))
  test = .update_path(config=test, par="^movement.file", 
                      path=file.path("..", "forcing", "distribution"))
  # reproduction: reproduction.season.file
  files = unique(unlist(get_par(the_files, "^reproduction.season.file")))
  file.copy(from=files, to=file.path(outDir, "input", "reproduction"))
  test = .update_path(config=test, par="^reproduction.season.file", 
                      path=file.path("input", "reproduction"))
  # fisheries: fisheries.catchability + fisheries.discards
  files = unique(unlist(get_par(the_files, "^fisheries.catchability.file")))
  file.copy(from=files, to=file.path(outDir, "input", "fisheries"))
  files = unique(unlist(get_par(the_files, "^fisheries.discards")))
  file.copy(from=files, to=file.path(outDir, "input", "fisheries"))
  test = .update_path(config=test, par="^fisheries.catchability", 
                      path=file.path("input", "fisheries"))
  test = .update_path(config=test, par="^fisheries.discards.file", 
                      path=file.path("input", "fisheries"))
  # fisheries: fisheries.movement
  files = unique(unlist(get_par(the_files, "^fisheries.movement.file")))
  file.copy(from=files, to=file.path(outDir, "input", "fisheries"))
  test = .update_path(config=test, par="^fisheries.movement.file", 
                      path=file.path("input", "fisheries"))
  # fisheries: mortality.fishing.season.distrib.file
  dir.create(file.path(outDir, "input", "fisheries", "seasonality"), recursive=TRUE)
  files = unique(unlist(get_par(the_files, "^mortality.fishing.season.distrib.file")))
  file.copy(from=files, to=file.path(outDir, "input", "fisheries", "seasonality"))
  test = .update_path(config=test, par="^mortality.fishing.season.distrib.file", 
                      path=file.path("input", "fisheries", "seasonality"))
  # fisheries: mortality.fishing.spatial.distrib.file
  dir.create(file.path(outDir, "input", "fisheries", "spatial"), recursive=TRUE)
  files = unique(unlist(get_par(the_files, "^mortality.fishing.spatial.distrib.file")))
  file.copy(from=files, to=file.path(outDir, "input", "fisheries", "spatial"))
  test = .update_path(config=test, par="^mortality.fishing.spatial.distrib.file", 
                      path=file.path("input", "fisheries", "spatial"))
  
  # remaining files
  the_files = test[sapply(test, FUN=.is_path)]
  files = unique(unlist(the_files))
  file.copy(from=files, to=file.path(outDir, "input"))
  for(iname in names(the_files)) {
    test = .update_path(config=test, par=iname, 
                        path=file.path("input"))
  }
  
  
  write_osmose(test, file=".testx.R")
  
  osmose:::.tidy_conf(input=".testx.R", file = file.path(outDir, paste0(output, ".R")), 
                      absolute=FALSE)
  
  return(invisible(output))
  
}


# Internal ----------------------------------------------------------------


# Template for initialisation
.osmose_template_init = function(conf, file, control, ...) {
  
  file_biomass = if(is.null(control$file_biomass)) "../data/path/to/biomass/file/biomass.csv" else control$file_biomass
  file_yield = if(is.null(control$file_yield)) "../data/path/to/landings/file/landings.csv" else control$file_yield
  path_cal = if(is.null(control$path_cal)) "../data/path/to/catch_at_length/file/catchatlength.csv" else control$path_cal
  ndt = if(is.null(control$ndt)) 12 else control$ndt
  
  if(length(ndt)==1) ndt = rep(ndt, 3)
  if(length(ndt)!=3) stop("Argument 'ndt' must be of length 1 or 3.")
  
  spp = sort(as.numeric(get_species(conf, type="focal", code=TRUE)))
  
  code = c("species.egg.stage.duration.sp%d", 
           "observed.biomass.guess.sp%d",
           "observed.biomass.file.sp%d",
           "observed.biomass.ndtPerYear.sp%d",
           "observed.biomass.cutoff.size.sp%d",
           "fisheries.yield.file.sp%d",
           "fisheries.yield.ndtPerYear.sp%d",
           "fisheries.catchatlength.file.sp%d",
           "fisheries.catchatlength.ndtPerYear.sp%d",
           "fisheries.selectivity.type.sp%d",
           "fisheries.selectivity.l50.sp%d",
           "fisheries.selectivity.l75.sp%d")
  
  dn = dirname(file)
  if(!dir.exists(dn)) dir.create(dn, recursive=TRUE)
  if(file.exists(file)) file.copy(from=file, to=paste(file, ".bck", sep=""))
  
  msg = sprintf("# OSMOSE initialization setup parameters (created %s)\n", date())
  cat(msg, file=file)
  
  tcat("Instructions", file=file, section=TRUE)
  
  msg = c(
    "species.egg.stage.duration         : duration of the egg stage, in days.", 
    "observed.biomass.guess             : estimated biomass at the start of the simulation, in tonnes.", 
    "observed.biomass.file              : path to a file with time series of biomass for the simulation period.",
    "                                     The name of the column matching the species name will be look for.", 
    "                                     The parameter 'observed.biomass.guess' has precedence.", 
    "observed.biomass.ndtPerYear        : Number of time steps per year in the biomass file.", 
    "observed.biomass.cutoff.size       : Cutoff size to include biomass in the computation of initial conditions.", 
    "                                     Biomass from schools with size lower than the cutoff, are not counted,.", 
    "                                     to reflect the sampling bias in the source of biomass estimates (e.g. survey selectivity).", 
    "fisheries.yield.file               : Mandatory file providing the landings for the species.", 
    "                                     The name of the column matching the species name will be look for.", 
    "                                     For non-exploited species, fill the column with zeros.", 
    "fisheries.yield.ndtPerYear         : Number of time steps per year in the yield file.", 
    "fisheries.catchatlength.file       : Optional file, providing catch-at-length information at the species level,", 
    "                                     i.e. combining all fisheries targeting the species. Optionally, catch-at-length", 
    "                                     from the main fishery can be used, if landings are dominant or minor differences", 
    "                                     in the selectivity with other fleets are expected.", 
    "fisheries.catchatlength.ndtPerYear : Number of time steps per year in the catch-at-length file.", 
    "fisheries.selectivity.type         : If no catch-at-length information is provided, a selectivity function should be assumed.", 
    "                                     0: knife-edge, 1: logistic, 2: normal, 3: log-normal", 
    "fisheries.selectivity.l50          : L50 parameter for the assumed selectivity.", 
    "fisheries.selectivity.l75          : L75 parameter for the assumed selectivity.")
  
  msg = paste("#", msg, collapse="\n")
  cat(msg, file=file, append=TRUE)
  cat("\n", file=file, append=TRUE)
  
  
  for(ispp in spp) {
    
    nm = sprintf(code, ispp)
    
    xspp = get_par(conf, sp=ispp)
    sp_name = get_par(xspp, "species.name")
    
    sb = gpar(xspp, par="seeding.biomass", as.is = FALSE, default=0)
    Linf = gpar(xspp, par="linf", as.is = FALSE, default=0)
    A = gpar(xspp, par="lifespan", as.is = FALSE, default=0)
    smat = gpar(xspp, par="maturity.size", as.is = FALSE)
    if(is.null(smat)) smat = gpar(xspp, par="maturity.l50", as.is = FALSE)
    amat = unlist(gpar(xspp, par="maturity.age", as.is=TRUE, default=ifelse(A<10, 1, 2)))
    if(is.null(smat)) smat = osmose:::VB(amat, this=xspp)
    
    s1 = min(1.5*smat, 0.5*Linf)
    s2 = min(1.75*smat, 0.9*Linf)
    ss = sort(c(s1, s2))
    
    par = list(egg_stage = 2,
               biomass_guess = sb,
               biomass_file = file_biomass,
               biomass_ndt = ndt[1],
               biomass_cutoff = 5,
               yield_file = file_yield,
               yield_ndt = ndt[2],
               cal_file = path_cal,
               cal_ndt = ndt[3],
               cal_sel_type = 3,
               cal_sel_l50 = round(ss[1], 1),
               cal_sel_l75 = round(ss[2], 1))
    
    names(par) = nm
    class(par) = "osmose.configuration"
    
    tcat(sprintf("sp%d: %s", ispp, sp_name), file=file, section=TRUE)
    write_osmose(par, file=file, append = TRUE, justify=TRUE)
    
  }
  
  message(sprintf("Initialisation setup template written in '%s'.", file))
  
  return(invisible())
  
}



# Auxiliar ----------------------------------------------------------------

.is_path = function(x) {
  if(!is.character(x)) return(FALSE)
  return(file.exists(x))
}

.update_path = function(config, par, path) {
  ind = grep(names(config), pattern=par)
  for(i in ind) {
    if(is.null(config[[i]])) next
    config[[i]] = file.path(path, basename(config[[i]]))
  }
  return(config)
}

.add_full_path = function(x) {
  path = attr(x, "path") 
  if(is.null(path)) return(x)
  return(file.path(path, x))
}

tcat = function(x, file, section=TRUE) {
  nx = paste(rep("-", 72 - nchar(as.character(x))), collapse="")
  if(!section) nx = ""
  cat(sprintf("\n# %s %s%s", x, nx, ifelse(section, "\n", "")), file=file, append = TRUE)
  return(invisible())
}

gpar = function(conf, par=NULL, sp=NULL, fsh=NULL, sr=NULL, invert=FALSE, as.is=TRUE, 
                unlist=FALSE, linear=FALSE, default=NULL, sort=FALSE) {
  
  pars = get_par(conf=conf, par=par, sp=sp, fsh=fsh, sr=sr, invert=invert, as.is=as.is, 
                 unlist=unlist, linear=linear)
  
  if(is.null(pars) & is.null(default)) return(invisible(NULL))
  if(!invert & is.null(pars) & !is.null(default)) {
    pars = list(default)
    names(pars) = par
    class(pars) = "osmose.configuration"
  } 
  
  if(isTRUE(sort)) {
    ind = as.numeric(gsub("\\D", "", names(pars)))
    pars = pars[order(ind)]
    class(pars) = "osmose.configuration"
  }
  
  return(pars)
  
}

.print_and_remove = function(par, conf, file) {
  pars = NULL
  for(ipar in par) {
    pars = c(pars, gpar(conf, par=ipar, as.is=TRUE))
    conf = gpar(conf, par=ipar, invert = TRUE, as.is=TRUE)
  }
    if(!is.null(pars)) class(pars) = "osmose.configuration"
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  return(invisible(conf))
}


.tidy_conf = function(input, file, absolute=TRUE, checks=TRUE) {
  
  if(file.exists(file)) {
    file.copy(from=file, to=paste0(file, ".bck"))
    file.remove(file)
  }
  
  conf = read_osmose(input=input)
  if(isTRUE(absolute)) {
    conf = lapply(conf, .add_full_path)
    class(conf) = "osmose.configuration"
  }

  if(isTRUE(checks)) {
    .check_density(conf)
  }
  
  model = toupper(gpar(conf, "output.file.prefix", default="NA"))
  version = gpar(conf, "osmose.version", default = "4.3.3")
  ecosystem = gpar(conf, "osmose.model.ecosystem", default = "Ecosystem name")
  ref = gpar(conf, "osmose.model.reference", default = "Reference, year")
  
  msg = c(sprintf("# OSMOSE-%s MAIN CONFIGURATION FILE", model), 
          sprintf("# %s (%s)", ecosystem, ref),
          sprintf("# OSMOSE version %s", version),
          sprintf("# Last update: %s", date()))
  
  cat(msg, sep="\n", file=file)
  
  
  # Setting the model -------------------------------------------------------
  
  tcat("Setting the model", file=file)
  
  pars = gpar(conf, "simulation.n", as.is=TRUE)
  pars = gpar(pars, "nschool", invert = TRUE, as.is=TRUE)
  pars = gpar(pars, "ncpu", invert = TRUE, as.is=TRUE)
  pars = gpar(pars, "nsimulation", invert = TRUE, as.is=TRUE)
  
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("fisheries module", file=file, section=FALSE)
  par0 = gpar(conf, "process.multispecies.fisheries.enabled", default=TRUE)
  par1 = gpar(conf, "fisheries.check.enabled", default=FALSE)
  write_osmose(c(par0, par1), file=file, append = TRUE)
  
  tcat("bioenergetics module", file=file, section=FALSE)
  par0 = gpar(conf, "module.bioenergetics.enabled", default=FALSE)
  par1 = gpar(conf, "module.genetics.enabled", default=FALSE)
  write_osmose(c(par0, par1), file=file, append = TRUE)
  
  tcat("bioeconomics module", file=file, section=FALSE)
  par0 = gpar(conf, "module.bioeconomics.enabled", default=FALSE)
  write_osmose(par0, file=file, append = TRUE)
  
  tcat("Species", file=file, section=FALSE)
  pars = gpar(conf, "species.name", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.type", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Fisheries", file=file, section=FALSE)
  pars = gpar(conf, "fisheries.name", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Time configuration", file=file, section=FALSE)
  par0 = gpar(conf, "simulation.time", sort=TRUE)
  par1 = gpar(conf, "simulation.ncpu")
  par2 = gpar(conf, "simulation.nsimulation")
  par3 = gpar(conf, "simulation.time.start", default=1900)
  
  write_osmose(c(par0, par1, par2, par3), file=file, append = TRUE, justify=TRUE)
  
  tcat("School number", file=file, section=FALSE)
  pars = gpar(conf, "simulation.nschool", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Spatial configuration", file=file, section=FALSE)
  pars = gpar(conf, "grid", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  
  # Resource configuration --------------------------------------------------
  
  tcat("Resource configuration", file=file)
  
  pars = gpar(conf, "species.file", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.biomass.nsteps.year", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.accessibility2fish", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Resources sizes", file=file, section=FALSE)
  pars = gpar(conf, "species.size", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Resources TL", file=file, section=FALSE)
  pars = gpar(conf, "species.TL", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  
  # Species configuration ---------------------------------------------------
  
  tcat("Species configuration", file=file)
  
  pars = gpar(conf, "species.growth.model", sort=TRUE)
  if(is.null(pars)) {
    pars = gpar(conf, "species.type", sort=TRUE)
    pars = pars[pars=="focal"]
    names(pars) = gsub(names(pars), pattern="type", replacement="growth.model")
    pars[] = "VonBertalanffy"
    class(pars) = "osmose.configuration"
  }
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Growth", file=file, section=FALSE)
  
  pars = gpar(conf, "species.lInf", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE, digits=2)
  pars = gpar(conf, "species.K", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE, digits=3)
  pars = gpar(conf, "species.t0", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE, digits=3)
  pars = gpar(conf, "species.vonbertalanffy.threshold.age", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  pars = gpar(conf, "species.length2weight.condition.factor", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  pars = gpar(conf, "species.length2weight.allometric.power", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  pars = gpar(conf, "species.delta.lmax.factor", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Reproduction: number of eggs per grame of female per time step", file=file, section=FALSE)
  pars = gpar(conf, "reproduction.season.file", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Reproduction: relative fecundity", file=file, section=FALSE)
  pars = gpar(conf, "species.relativefecundity", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Reproduction: species egg size", file=file, section=FALSE)
  pars = gpar(conf, "species.egg.size", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Reproduction: species egg weigth", file=file, section=FALSE)
  pars = gpar(conf, "species.egg.weight", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE) 
  
  tcat("Reproduction: sex ratio", file=file, section=FALSE)
  pars = gpar(conf, "species.sexratio", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE) 
  
  tcat("Reproduction: size at maturity", file=file, section=FALSE)
  pars = gpar(conf, "species.maturity.size", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE) 
  
  tcat("Reproduction: lifespan", file=file, section=FALSE)
  pars = gpar(conf, "species.lifespan", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Survival: additionality mortality rate", file=file, section=FALSE)
  pars = gpar(conf, "mortality.additional.rate", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE) 
  
  tcat("Survival: larval mortality rate (config_larval-historical)", file=file, section=FALSE)
  pars = gpar(conf, "mortality.additional.larva.rate", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE) 
  
  tcat("Survival: starvation mortality", file=file, section=FALSE)
  pars = gpar(conf, "mortality.starvation.rate.max", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  pars = gpar(conf, "population.seeding.biomass", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Migration configuration", file=file)
  
  tcat("Flux incoming", file=file, section=FALSE)
  
  pars = gpar(conf, "flux.incoming.byDt.byAge.file", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE) 
  
  
  # Predation configuration -------------------------------------------------
  
  tcat("Predation configuration", file=file)
  
  pars = gpar(conf, "predation.accessibility.file")
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.accessibility.stage.structure", default="size")
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.efficiency.critical", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.ingestion.rate.max", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.predPrey.sizeRatio.m", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.predPrey.stage.structure", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.predPrey.stage.threshold", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  # Fisheries configuration -------------------------------------------------
  
  tcat("Fisheries configuration", file=file)
  
  tcat("fisheries module", file=file, section=FALSE)
  par0 = gpar(conf, "simulation.fishing.mortality.enabled", default=TRUE)
  par1 = gpar(conf, "fisheries.movement.netcdf.enabled", default=TRUE)
  write_osmose(c(par0, par1), file=file, append = TRUE)
  
  tcat("fisheries module", file=file, section=FALSE)
  par0 = gpar(conf, "fisheries.catchability.file")
  par1 = gpar(conf, "fisheries.discards.file")
  write_osmose(c(par0, par1), file=file, append = TRUE)
  
  ind = as.integer(get_fisheries(conf, code = TRUE))
  find = get_fisheries(conf)
  fm_nm = unlist(gpar(conf, "fisheries.movement.fishery.map"))
  fpars = gpar(conf, "fisheries.movement.*.map")
  
  for(i in seq_along(ind)) {
    tcat(sprintf("Fisheries %d (%s):", ind[i], find[i]), file=file, section=FALSE)
    pars = gpar(conf, "fisheries", fsh=ind[i], as.is=TRUE)
    pars = gpar(pars, "fisheries.name", invert = TRUE, as.is=TRUE)
    write_osmose(pars, file=file, append = TRUE, justify=TRUE)
    
    res = gsub(names(which(fm_nm == find[i])), 
               pattern="fisheries.movement.fishery.", replacement="")
    if(length(res) > 0) {
      for(ires in res) {
        this_maps = gpar(fpars, ires)
        write_osmose(this_maps, file=file, append = TRUE, justify=TRUE)
      }
    }
  }
  
  
  # Survey configuration ----------------------------------------------------
  
  tcat("Survey configuration", file=file)
  
  par0 = gpar(conf, "surveys.movement.netcdf.enabled", default=TRUE)
  write_osmose(par0, file=file, append = TRUE)
  
  ind = as.integer(get_surveys(conf, code = TRUE))
  find = get_surveys(conf)
  fm_nm = unlist(gpar(conf, "surveys.movement.survey.map"))
  fpars = gpar(conf, "surveys.movement.*.map")
  
  for(i in seq_along(ind)) {
    tcat(sprintf("Survey %d (%s):", ind[i], find[i]), file=file, section=FALSE)
    pars = gpar(conf, "surveys", sr=ind[i], as.is=TRUE)
    pars = gpar(pars, "surveys.movement", invert = TRUE, as.is=TRUE)
    write_osmose(pars, file=file, append = TRUE, justify=TRUE)
    
    res = gsub(names(which(fm_nm == find[i])), 
               pattern="surveys.movement.survey.", replacement="")
    if(length(res) > 0) {
      for(ires in res) {
        this_maps = gpar(fpars, ires)
        write_osmose(this_maps, file=file, append = TRUE, justify=TRUE)
      }
    }
  }
  
  
  # Movement configuration --------------------------------------------------
  
  tcat("Movement configuration", file=file)
  
  par0 = gpar(conf, "movement.netcdf.enabled", default=TRUE)
  write_osmose(par0, file=file, append = TRUE)
  
  pars = gpar(conf, "^movement.", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE) 
  
  
  # Survival configuration --------------------------------------------------
  
  tcat("Survival configuration", file=file)
  par0 = gpar(conf, "mortality.subdt", default=TRUE)
  write_osmose(par0, file=file, append = TRUE)
  
  
  # Bioenergetics configuration ---------------------------------------------
  
  tcat("Bioenergetics configuration", file=file)
  
  tcat("physical configuration", file=file, section=FALSE)
  
  pars = gpar(conf, "^temperature", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "^oxygen", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "^species.zlayer", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("bionergetic", file=file, section=FALSE)
  
  pars = gpar(conf, "species.beta", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.assimilation", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.ingestion.rate.max.bioen", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.c.bioen", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "predation.coef.ingestion.rate.max.larvae.bioen", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.larvae.growth.threshold.age", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.forage.k_for", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.forage.k1_for", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.forage.k2_for", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  
  tcat("Temperature", file=file, section=FALSE)
  
  pars = gpar(conf, "species.bioen.maint.energy.c_m", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.maint.e.maint", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.mobilized.e.d", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.mobilized.e.mobi", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.mobilized", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Oxygen", file=file, section=FALSE)
  
  pars = gpar(conf, "species.oxygen.c1", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.oxygen.c2", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  tcat("Maturity", file=file, section=FALSE)
  pars = gpar(conf, "species.bioen.maturity.eta", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.maturity.m0", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.maturity.m1", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "species.bioen.maturity.r", sort=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  
  # Output configuration ----------------------------------------------------
  tcat("Output configuration", file=file)
  
  oconf = gpar(conf, par="^output.")
  
  oconf = .print_and_remove(par=c("output.start.year", "output.restart.enabled", 
                                  "output.file.prefix", "output.step0.include"), 
                            conf=oconf, file=file)
  
  oconf = .print_and_remove(par=c("output.recordfrequency.ndt", "output.cutoff.enabled", 
                                  "output.fishery.enabled"), 
                            conf=oconf, file=file)
  
  oconf = .print_and_remove(par=c("output.fishing.accessible.biomass",
                                  "output.fishing.harvested.biomass"), 
                            conf=oconf, file=file)
  
  oconf = .print_and_remove(par="output.yield", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.biomass", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.abundance", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.meanSize", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.meanWeight", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.diet.", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.spatial", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.mortality", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.age.at.death", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.distrib.bySize", conf=oconf, file=file)
  oconf = .print_and_remove(par="output.distrib.byAge", conf=oconf, file=file)
  
  write_osmose(oconf, file=file, append = TRUE, justify=TRUE)
  
  tcat("Model initialisation", file=file)
  
  pars = gpar(conf, "population.seeding.year.max", sort=TRUE, default=0)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "population.initialization.relativebiomass.enabled", default=TRUE)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  
  # Advanced parameters -----------------------------------------------------
  
  tcat("Advanced parameters", file=file)
  
  pars = gpar(conf, "osmose.version", sort=TRUE, default=0)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "simulation.restart.recordfrequency.ndt", sort=TRUE, default=1)
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "output.netcdf.format", sort=TRUE, default="netcdf3")
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "output.netcdf.format", sort=TRUE, default="netcdf3")
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "ltl.java.classname", sort=TRUE, default="netcdf3")
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "simulation.incoming.flux.enabled", sort=TRUE, default="netcdf3")
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  pars = gpar(conf, "movement.checks.enabled", sort=TRUE, default="netcdf3")
  write_osmose(pars, file=file, append = TRUE, justify=TRUE)
  
  
  # Other parameters --------------------------------------------------------
  
  tcat("Other parameters", file=file)
  
  xconf = read_osmose(input=file)
  
  ind = match(names(xconf), names(conf))
  iconf = conf
  iconf = iconf[-na.omit(ind)]
  if(length(iconf) > 0) class(iconf) = "osmose.configuration"
  iconf = gpar(iconf, par="^osmose.configuration", invert=TRUE, as.is=TRUE)
  write_osmose(iconf, file=file, append = TRUE, justify=TRUE)
  
  return(invisible(TRUE))
  
}





