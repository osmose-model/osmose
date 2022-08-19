
#' Set up a new calibration for an OSMOSE model
#'
#' @param type Type or calibration. Currently only 'simple' is available.
#' @param bioen Are we calibrating a Bioen-OSMOSE model?
#' @param control A list with additional control arguments. Notably, control$dir to
#' choose the name of the calibration folder and control$run for the scratch folder 
#' (to 'run' OSMOSE during the calibration. Full paths are ignored, only basename is kept).
#' @param test Boolean, test the calibration? FALSE by default. You can run the same test later using
#' the function \code{\link{osmose_calibration_test}}.
#' @param ... Additional arguments of current no use.
#'
#' @return Side effects: Create all the files you need for your calibration. Returns, invisibily,
#' the path to the calibration directory.
#' @export
#' @inheritParams run_osmose
osmose_calibration_setup = function(input, osmose, type="simple", bioen=FALSE, test=FALSE, 
                                    control=list(), version="4.3.3", ...) {
  
  wd = getwd()
  on.exit(setwd(wd))
  
  type = match.arg(type, choices=c("simple"))
  
  control$method = type
  conf = read_osmose(input=input)
  
  # Create dir and file names -----------------------------------------------
  
  if(is.null(control$dir)) control$dir = "calibration"
  if(is.null(control$run)) control$run = "run"
  
  irun = basename(control$run)
  idir = basename(control$dir)
  
  if(irun!=control$run) {
    warning(sprintf("Ignoring path in 'run', control$run set to %s", irun))
    control$run = irun
  }
  
  if(idir!=control$dir) {
    warning(sprintf("Ignoring path in 'dir', control$dir set to %s", idir))
    control$dir = idir
  }
  
  dir_master = file.path(control$dir, "master")  
  dir_data   = file.path(control$dir, "data_templates")
  
  if(!dir.exists(dir_master)) dir.create(dir_master, recursive=TRUE)
  if(!dir.exists(control$run)) dir.create(control$run, recursive=TRUE)
  if(!dir.exists(dir_data)) dir.create(dir_data, recursive=TRUE)
  
  bsn = sprintf("osmose-%s", get_par(conf, "output.file.prefix"))
  guess_file = file.path(control$dir, paste(bsn, "-parguess.osm", sep=""))
  min_file   = file.path(control$dir, paste(bsn, "-parmin.osm", sep=""))
  max_file   = file.path(control$dir, paste(bsn, "-parmax.osm", sep=""))
  phase_file = file.path(control$dir, paste(bsn, "-parphase.osm", sep=""))
  control_file = file.path(control$dir, paste(bsn, "-calibration_settings.csv", sep=""))
  
  allfiles = c(guess_file, min_file, max_file, phase_file)
  
  # Get general parameters from configuration -------------------------------
  
  nspp = get_par(conf, par="simulation.nspecies")
  nyear = get_par(conf, "simulation.time.nyear")
  
  # Create new main configuration file --------------------------------------
  
  calib = list()
  calib$osmose.configuration.calibration.parameters = "calibration_parameters.osm"
  calib$osmose.configuration.outputs = "output-configuration.osm"
  calib$osmose.configuration.main = file.path("../..", input)
  class(calib) = "osmose.configuration"
  write_osmose(calib, file=file.path(dir_master, "osmose-calibration.osm"))
  
  # Create guess, min, max and phase files. ---------------------------------
  
  mcat("\n#------- Parameters to be calibrated -------\n", file=allfiles)
  
  
  # Resource parameters -----------------------------------------------------
  
  
  mcat("\n#---- Resources species\n", file=allfiles, append=TRUE)
  
  pars = get_par(conf, "species.accessibility2fish", as.is=TRUE)
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, -20), file=min_file, append = TRUE)
  write_osmose(set_par(pars, +20), file=max_file, append = TRUE)
  write_osmose(set_par(pars, 1), file=phase_file, append = TRUE)
  
  nbkg = get_par(conf, "nbackground")
  if(!is.null(nbkg)) {
    mcat("\n#---- Background species\n", file=allfiles, append=TRUE)
    pars = get_par(conf, "species.multiplier.log.sp", as.is=TRUE)
    write_osmose(pars, file=guess_file, append = TRUE)
    write_osmose(set_par(pars, 0), file=min_file, append = TRUE)
    write_osmose(set_par(pars, 15), file=max_file, append = TRUE)
    write_osmose(set_par(pars, 1), file=phase_file, append = TRUE)
  }
  
  
  # Focal species parameters ------------------------------------------------
  
  mcat("\n#---- Focal species\n", file=allfiles, append=TRUE)
  
  pars = get_par(conf, "mortality.additional.rate.log", as.is=TRUE)
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, -9), file=min_file, append = TRUE)
  write_osmose(set_par(pars, +5), file=max_file, append = TRUE)
  write_osmose(set_par(pars, 1), file=phase_file, append = TRUE)
  
  pars = get_par(conf, "mortality.additional.larva.rate.log", as.is=TRUE)
  pars = get_par(pars, par="seasonality", invert = TRUE, as.is=TRUE)
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, -9), file=min_file, append = TRUE)
  write_osmose(set_par(pars, +9), file=max_file, append = TRUE)
  write_osmose(set_par(pars, 1), file=phase_file, append = TRUE)
  
  knots = get_par(conf, "mortality.additional.larva.knots")
  
  pars = list()
  for(isp in get_species(conf, code=TRUE)) {
    iknots = get_par(knots, sp=as.numeric(isp), as.is=TRUE)
    if(is.null(iknots)) iknots = nyear + 1
    nn = sprintf("osmose.user.larval.deviate.log.sp%s", isp)
    pars[[nn]] = rep(0, iknots)
  }
  class(pars) = "osmose.configuration"
  
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, -4), file=min_file, append = TRUE)
  write_osmose(set_par(pars, +4), file=max_file, append = TRUE)
  write_osmose(set_par(pars, 3), file=phase_file, append = TRUE)
  
  
  # Growth parameters -------------------------------------------------------
  
  pars = list()
  for(isp in get_species(conf, code=TRUE)) {
    nn = sprintf("species.delta.lmax.factor.sp%s", isp)
    pars[[nn]] = 2
  }
  class(pars) = "osmose.configuration"
  
  write_osmose(pars, file=guess_file, append = TRUE)
  write_osmose(set_par(pars, 1), file=min_file, append = TRUE)
  write_osmose(set_par(pars, 5), file=max_file, append = TRUE)
  write_osmose(set_par(pars, 2), file=phase_file, append = TRUE)
  
  # here to add case for Bioen-OSMOSE  
  
  # Fisheries parameters ----------------------------------------------------
  
  mcat("\n#---- Fisheries\n", file=allfiles, append=TRUE)
  
  nmf = get_fisheries(conf)
  nmc = get_fisheries(conf, code=TRUE)
  
  for(i in seq_along(nmc)) {
    
    ifsh = nmc[i]
    msg = sprintf("\n-- Fishery %s: %s\n", nmc[i], nmf[i])
    mcat(msg, file=allfiles, append=TRUE)
    this = get_par(conf, sprintf("fsh%s", ifsh))
    pars = get_par(get_par(this, "fisheries.rate.base", as.is=TRUE), "shift", invert=TRUE, as.is=TRUE)
    write_osmose(pars, file=guess_file, append=TRUE)
    write_osmose(set_par(pars, -9), file=min_file, append = TRUE)
    write_osmose(set_par(pars, +3), file=max_file, append = TRUE)
    write_osmose(set_par(pars, 1), file=phase_file, append = TRUE)
    
    pars = get_par(this, "fisheries.rate.byperiod", as.is=TRUE)
    write_osmose(pars, file=guess_file, append=TRUE)
    write_osmose(set_par(pars, -4), file=min_file, append = TRUE)
    write_osmose(set_par(pars, +4), file=max_file, append = TRUE)
    write_osmose(set_par(pars, 3), file=phase_file, append = TRUE)
    
    # make difference for each one: l50, l75
    pars = get_par(this, "fisheries.selectivity.l50", as.is=TRUE)
    L50 = as.numeric(unlist(pars))
    write_osmose(pars, file=guess_file, append=TRUE)
    write_osmose(set_par(pars, 0), file=min_file, append = TRUE)
    write_osmose(set_par(pars, 200), file=max_file, append = TRUE)
    write_osmose(set_par(pars, 3), file=phase_file, append = TRUE)
    
    pars75 = get_par(this, "fisheries.selectivity.l75", as.is=TRUE)
    
    if(!is.null(pars75)) {
      
      L75 = as.numeric(unlist(pars75))
      pars = list()
      nn = sprintf("osmose.user.selectivity.delta75.fsh%s", ifsh)
      pars[[nn]] = L75 - L50
      
      class(pars) = "osmose.configuration"
      
      write_osmose(pars, file=guess_file, append = TRUE)
      write_osmose(set_par(pars, 0), file=min_file, append = TRUE)
      write_osmose(set_par(pars, ceiling(0.3*L50)), file=max_file, append = TRUE)
      write_osmose(set_par(pars, 3), file=phase_file, append = TRUE)
      
    }
    
  } # end of fisheries parameters
  
  
  # Template files ----------------------------------------------------------
  
  # RStudio project
  rstudioproj = system.file("calibration/master.Rproj", package="osmose")
  file.copy(from=rstudioproj, to=dir_master, overwrite = TRUE)
  # launch_calibration
  launcher = system.file("calibration/launch_calibration.R", package="osmose")
  file.copy(from=launcher, to=control$dir, overwrite = TRUE)
  # runModel
  runtmp = sprintf("calibration/runModel_%s.R", control$method)
  runmodeltmp = system.file(runtmp, package="osmose")
  file.copy(from=runmodeltmp, to=file.path(control$dir, "runModel.R"), overwrite = TRUE)
  # output configuration file
  outtmp = sprintf("calibration/output-configuration_%s.osm", control$method)
  outputtmp = system.file(outtmp, package="osmose")
  file.copy(from=outputtmp, to=file.path(dir_master, "output-configuration.osm"), overwrite = TRUE)  
  
  source(file.path(control$dir, "runModel.R"))
  
  par_guess   = read_osmose(input=guess_file)
  par_min   = read_osmose(input=min_file)
  par_max   = read_osmose(input=max_file)
  par_phase = read_osmose(input=phase_file)
  
  message("Testing is we can run OSMOSE...\n")
  message("Running OSMOSE with ", osmose)
  setwd(dir_master)
  simulated = runModel(par=par_guess, conf=conf, osmose=osmose, is_a_test=FALSE, version=version)
  setwd(wd)
  
  if(file.exists(file.path(dir_master, "osmose.log")))
    file.remove(file.path(dir_master, "osmose.log"))
  
  message("Pre-calibration test passed: we can run the model (via the runModel function).\n")
  saveRDS(simulated, file=file.path(control$dir, "simulated.rds"))
  
  # Create data templates ---------------------------------------------------
  
  output = read_osmose(path = file.path(dir_master, "output"), version='4.3.3')
  
  biomass_file = .write_biomass_files(output, conf, path=dir_data)
  yield_files  = .write_yield_files(output, conf, path=dir_data)
  cal_files    = .write_cal_files(output, conf, path=dir_data)
  
  observed_files = c(biomass_file, yield_files, cal_files)
  
  cal_settings = .create_calibration_settings(output=simulated, files=observed_files)
  write_osmose(cal_settings, file=control_file, row.names = FALSE, col.names = TRUE)
  
  setup = calibration_setup(file=control_file)
  observed = suppressMessages(calibration_data(setup, path=dir_data))
  saveRDS(observed, file=file.path(control$dir, "observed.rds"))
  
  # Calibration test --------------------------------------------------------
  
  obs_check = lapply(observed, FUN=.dim_or_length)
  sim_check = lapply(simulated, FUN=.dim_or_length)
  sim_check[!setup$use_data] = 1L
  test1 = identical(obs_check, sim_check)
  test2 = identical(names(obs_check), names(sim_check))
  
  if(test1 & test2) {
    message("Pre-calibration test passed: simulated and observed data are compatible.\n")
  }
  
  fn = calibration_objFn(model=runModel, setup=setup, observed=observed, 
                         conf=conf, osmose=osmose, is_a_test=TRUE)
  setwd(dir_master)
  value = try(fn(par_guess))
  setwd(wd)
  test3 = !inherits(value, "try-error")
  if(test3) {
    message("Pre-calibration test passed: Objective function is properly created.\n")
  } else {
    stop("Pre-calibration test failed: your model function (i.e. runModel) is not working properly.")
  }
  
  message(sprintf("The data templates provided in '%s' need to be filled with your data.", dir_data))
  
  setwd(wd)
  if(isTRUE(test)) osmose_calibration_test(control$dir)
  
  return(invisible(control$dir))
  
}

#' Run an OSMOSE calibration test.
#'
#' @param path Path to the main calibration directory.
#' @param script Name of the calibratio script. The default is 'launch_calibration.R' as
#' automatically created by \code{\link{osmose_calibration_setup}}.
#'
#' @return TRUE is the test is completed.
#' @export
#'
osmose_calibration_test = function(path, script="launch_calibration.R") {
  
  wd = getwd()
  on.exit(setwd(wd))
  
  setwd(path)
  message("Running a calibration test...")
  Sys.sleep(2)
  message("It may take a few minutes.\n")
  Sys.sleep(4)
  message("If there are no errors, you will be ready to launch your calibration.\n")
  Sys.sleep(5)
  message("This automatic setup provides generic templates, you MUST check and modify all the configuration parameters before running a calibration.\n")
  
  if(!file.exists(script)) stop(sprintf("Calibration script file '%s' does not exist in '%s'.", script, path))
  
  source(script)
  
  message("CALIBRATION TEST PASSED! Everything seems to be in order.\n\n")
  
  return(invisible(TRUE))
  
}





