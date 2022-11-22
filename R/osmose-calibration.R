
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
osmose_calibration_setup = function(input, osmose, name=NULL, data_path=NULL, type="simple", bioen=FALSE,  
                                    control=list(), version="4.3.3", ...) {
  
  if(is.null(data_path)) {
    message("\nNo observed data has been provided, this setup will create templates for you to fill.\n")
  } else {
    message(sprintf("\nObserved data has been provided in %s, the calibration setup will use it.\n", data_path))
  }
  
  wd = getwd()
  on.exit(setwd(wd))
  
  type = match.arg(type, choices=c("simple", "survey"))
  
  control$method = type
  conf = read_osmose(input=input)
  
  if(!file.exists(osmose)) stop(sprintf("Could not find your 'osmose' executable (%s)", osmose))
  if(!is.null(name)) {
    if(basename(name)!=name) stop(sprintf("Argument 'name' (%s) cannot be a directory.", name))
  }
  
  run = if(is.null(name)) ".run" else sprintf(".run_%s", name)
  dir = if(is.null(name)) "calibration" else sprintf("calibration_%s", name)
  if(!dir.exists(dir)) dir.create(dir)
  # make a local copy of osmose into the calibration directory
  nosmose = ".osmose.jar"
  file.copy(from=osmose, to=file.path(dir, nosmose))
  osmose = file.path("..", nosmose)
  
  # Create dir and file names -----------------------------------------------
  
  control$dir = dir
  control$run = run
  
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

  # Create new main OSMOSE configuration file -------------------------------
  
  calib = list()
  calib$osmose.configuration.calibration.parameters = "calibration_parameters.osm"
  calib$osmose.configuration.outputs = "output-configuration.osm"
  calib$osmose.configuration.main = file.path("../..", input)
  class(calib) = "osmose.configuration"
  write_osmose(calib, file=file.path(dir_master, "osmose-calibration.osm"))
  
  # Get general parameters from configuration -------------------------------
  
  # nspp = get_par(conf, par="simulation.nspecies")
  # nyear = get_par(conf, "simulation.time.nyear")
  
  bsn = sprintf("osmose-%s", get_par(conf, "output.file.prefix"))
  guess_file = file.path(control$dir, paste(bsn, "-parguess.osm", sep=""))
  min_file   = file.path(control$dir, paste(bsn, "-parmin.osm", sep=""))
  max_file   = file.path(control$dir, paste(bsn, "-parmax.osm", sep=""))
  phase_file = file.path(control$dir, paste(bsn, "-parphase.osm", sep=""))
  settings_file = file.path(control$dir, paste(bsn, "-calibration_settings.csv", sep=""))
  
  allfiles = c(guess=guess_file, min=min_file, max=max_file, phase=phase_file)
  
  if(!all(file.exists(allfiles))) {

    # Create guess, min, max and phase files. ---------------------------------
    
    mcat("\n#------- Parameters to be calibrated -------\n", file=allfiles)
    
    .set_param_resources(allfiles, conf)
    
    .set_param_background(allfiles, conf)
    
    .set_param_focal(allfiles, conf)
    
    .set_param_growth(allfiles, conf, bioen)
    
    # here to add case for Bioen-OSMOSE  
    if(isTRUE(bioen)) .set_param_bioen(allfiles, conf)
    
    .set_param_fisheries(allfiles, conf)
    
    .set_param_user(allfiles, conf)
    
  } else {
    
    message("Previous guess, min, max and phase files found. Conserving them.\n")
    
  }
  
  # Template files ----------------------------------------------------------
  
  # copy this files as they are
  files = c("calibration_MPI.pbs", "calibration_OMP.pbs", "calibration_sequentiel.pbs")
  for(ifile in files) {
    ofile = system.file(file.path("calibration", ifile), package="osmose")
    file.copy(from=ofile, to=control$dir, overwrite = TRUE)
  }
  
  # copy this files as hidden
  files = c("calibration.R", "calibrartest", "calibrarrc")
  for(ifile in files) {
    ofile = system.file(file.path("calibration", ifile), package="osmose")
    file.copy(from=ofile, to=file.path(control$dir, sprintf(".%s", ifile)), overwrite = TRUE)
  }
  
  # copy files according to calibration type
  # run_model:
  if(!file.exists(file.path(control$dir, "run_model.R"))) {
    runmodeltmp = system.file(sprintf("calibration/run_model_%s.R", control$method), package="osmose")
    file.copy(from=runmodeltmp, to=file.path(control$dir, "run_model.R"), overwrite = TRUE)
  } else {
    message("A 'run_model.R' script was found, conserving it.\n")
  }
  
  # output configuration file:
  outtmp = sprintf("calibration/output-configuration_%s.osm", control$method)
  outputtmp = system.file(outtmp, package="osmose")
  file.copy(from=outputtmp, to=file.path(dir_master, "output-configuration.osm"), overwrite = TRUE)  
  
  source(file.path(control$dir, "run_model.R"), local=TRUE)
  
  par_guess = read_osmose(input=guess_file)
  par_min   = read_osmose(input=min_file)
  par_max   = read_osmose(input=max_file)
  par_phase = read_osmose(input=phase_file)
  
  message("Testing is we can run OSMOSE...\n")
  # message("Running OSMOSE with ", osmose, "\n")
  setwd(dir_master)
  simulated = try(run_model(par=par_guess, conf=conf, osmose=osmose, is_a_test=FALSE, version=version))
  setwd(wd)
  
  if(!inherits(simulated, "try-error")) {
    message("Pre-calibration test: PASSED. We can run the model (via the run_model function).\n")
    saveRDS(simulated, file=file.path(control$dir, ".simulated.rds"))
  } else {
    message("Pre-calibration test: FAILED. We CANNOT run the model (via the run_model function).\n")
    stop("Test failed.")
  }
  
  if(file.exists(file.path(dir_master, "osmose.log")))
    file.remove(file.path(dir_master, "osmose.log"))
  
  # Create data templates ---------------------------------------------------
  
  output = read_osmose(path = file.path(dir_master, "output"), version=version)
  if(is.null(data_path)) {
    # if not data_path provided, create templates.
    if(!dir.exists(dir_data)) dir.create(dir_data, recursive=TRUE)
    biomass_file = .write_biomass_files(output, conf, path=dir_data, what="biomass")
    yield_files  = .write_yield_files(output, conf, path=dir_data)
    cal_files    = .write_cal_files(output, conf, path=dir_data)

    survey_files = NULL
    surveyNames = grep(names(output), pattern="biomass\\.", value=TRUE)
    if(length(surveyNames)!=0) {
      for(iSurvey in surveyNames) {
        survey_files = c(survey_files, 
                         .write_biomass_files(output, conf, path=dir_data, what=iSurvey))
      }
    }
    observed_files = c(biomass_file, survey_files, yield_files, cal_files)
    # create calibration settings
    cal_settings = .create_calibration_settings(output=simulated, files=observed_files, type=control$method)
    if(file.exists(settings_file)) {
      message(sprintf("Settings file (%s) found. Conserving it.", settings_file))      
    } else {
      write_osmose(cal_settings, file=settings_file, row.names = FALSE, col.names = TRUE)
    }
  } else {
    dir_data = data_path
    if(!file.exists(settings_file)) 
      stop(sprintf("Settings file for the calibration (%s) not found.", settings_file))
    message(sprintf("Settings file (%s) found. Conserving it.", settings_file))
  }

  setup = calibration_setup(file=settings_file)
  observed = calibration_data(setup, path=dir_data) # it will read from data_path if provided
  saveRDS(observed, file=file.path(control$dir, "observed.rds")) # saving the new object
  
  # Calibration test --------------------------------------------------------
  
  .calibration_test_1(simulated, observed, setup)
  
  fn = calibration_objFn(model=run_model, setup=setup, observed=observed, 
                         conf=conf, osmose=osmose, is_a_test=TRUE)
  setwd(dir_master)
  value = try(fn(par_guess))
  setwd(wd)
  test3 = !inherits(value, "try-error")
  if(test3) {
    message("Pre-calibration test: PASSED. Objective function is properly created.\n")
  } else {
    message("Pre-calibration test: FAILED. Your model function (i.e. run_model) is not working properly.")
    stop("Test failed.")
  }
  
  if(is.null(data_path))
    message(sprintf("The data templates provided in '%s' need to be filled with your data.", dir_data))
  
  setwd(wd)

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
osmose_calibration_test = function(path, setup=NULL) {
  
  wd = getwd()
  on.exit(setwd(wd))
  
  msg = sprintf("We couldn't find your calibration directory ('%s') in the current working directory ('%s').",
                path, wd)
  
  if(!dir.exists(path)) stop(msg)
  setwd(path)
  
  script  = ".calibration.R"
  options = ".calibrartest"
  
  if(!file.exists(script)) stop(sprintf("Calibration script file '%s' does not exist in '%s'.", script, path))
  
  if(is.null(setup)) {
    setup = dir(pattern="calibration_settings.csv")
    if(length(setup)!=1) stop("Must indicated the calibration settings file ('setup' argument)")
  }

  settings = calibration_setup(file=setup)
  # dir_data = .getDataFolder(options)
  # observed = suppressMessages(calibration_data(settings, path=dir_data))
  observed  = readRDS("observed.rds")
  simulated = readRDS(".simulated.rds")

  message("Running a calibration test...")
  Sys.sleep(2)
  message("It may take a few minutes.\n")
  Sys.sleep(4)
  message("If there are no errors, you will be ready to launch your calibration.\n")
  Sys.sleep(5)
  message("This automatic setup provides generic templates, you MUST check and modify all the configuration parameters before running a calibration.\n")

  arg0 = sprintf("--calibration.control=%s", options)
  
  message("CALIBRATION TEST 1: Is your observed data compatible with the simulation outputs?.\n\n")
  # calibration test 1: compatibility of data
  .calibration_test_1(simulated, observed, setup=settings)
  message("CALIBRATION TEST 1: PASSED! \n  Observed data is compatible.\n\n")
  
  # calibration test 2: sequential  
  
  message("CALIBRATION TEST 2: Can we run a calibration in sequential mode (single-thread)?.\n\n")
  .args = c(arg0, "--test", "--ncores=1")
  source(script, local=TRUE)
  message("CALIBRATION TEST 2: PASSED! \n  Calibration is running in sequential mode.\n\n")
  message("This means all the files and inputs are in the right place.")
  
  # calibration test 3: parallel
  
  message("CALIBRATION TEST 2: Can we run a calibration in parallel mode (multi-thread)?.\n\n")
  .args = c(arg0, "--test", "--ncores=2")
  source(script, local=TRUE)
  message("CALIBRATION TEST 3: PASSED! \n  Calibration is running in parallel.\n\n")
  message("This means all the files and inputs are being properly exported to the virtual cluster.")
  
  Sys.sleep(5)
  message("ALL CALIBRATION TESTS PASSED! You can go grab SOMBEERS.\n\n")
  
  return(invisible(TRUE))
  
}




