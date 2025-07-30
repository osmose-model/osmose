#!/usr/bin/env Rscript
#  
# Arguments for the script
#
# --ncores=n                 : 'n' is the number of cores. If ommited, it will take it from the PBS.
#                              Useful to set it up when using OMP (avoid using logical) or using replicates
#                              from OSMOSE. In MPI, matches the number of MPI processes, sub OMP are for
#                              OSMOSE replicates.
# --omp                      : Use OMP.
# --nht                      : Do not use hyper-threading. Only half of cores are effectively used.
# --test                     : boolean, is it a test? Does not run OSMOSE if is a test (save time!).
# --run_model=file           : 'file' is sourced to find the 'run_model' function.
# --replicates=n             : 'n' is the number of replicates, controlled from the 'calibrar' package.
# --calibration.control=file : options are taken from 'file' (no spaces in 'file' are allowed).

if(!exists(".args", mode = "character")) .args = commandArgs(trailingOnly=TRUE)

message("\n--------- ", date(), "\n")
message(R.version.string)
message(sprintf("\tR_LIBS      = %s\n\tR_LIBS_USER = %s", Sys.getenv("R_LIBS"), Sys.getenv("R_LIBS_USER")))
library("calibrar")
message(sprintf("\tcalibrar version %s loaded.", packageVersion("calibrar")))
library("osmose")
message(sprintf("\tosmose version %s loaded.", packageVersion("osmose")))

nodefile = Sys.getenv("PBS_NODEFILE")
nodes = if(nodefile=="") "localhost" else readLines(nodefile)

message(sprintf("\nUsing nodes: %s\n", paste(nodes, collapse=", ")))

control_file   = .get_command_argument(.args, "calibration.control", default=".calibrarrc")
if(is.null(control_file)) stop("A control file must be specified ('--calibration.control=file')")
is_a_test      = .get_command_argument(.args, "test")
run_model_file = .get_command_argument(.args, "run_model", default="run_model.R")
ncores         = .get_command_argument(.args, "ncores", default=length(nodes))
OMP            = .get_command_argument(.args, "omp")
nht            = .get_command_argument(.args, "nht")
replicates     = .get_command_argument(.args, "replicates", default=1)
MPI            = !OMP
if(isTRUE(MPI)) nht = FALSE # hyper-threading FALSE by default when using MPI
nht = ifelse(nht, 2, 1)

control  = .read_configuration(control_file)
osmose   = ".osmose.jar"
obs_file = "observed.rds"

# check for mandatory arguments
msg = "You must provide a '%s' argument in '%s'"

root = basename(getwd()) # here
run_path = sprintf("../.run_%s", root)
if(!dir.exists(run_path)) dir.create(run_path)
if(!file.exists(osmose)) stop("OSMOSE executable '.osmose.jar' not found.")

.cancopy = file.copy(from=osmose, to=file.path(run_path, osmose), overwrite = TRUE)
message(sprintf("Copying OSMOSE executable: %s", as.character(.cancopy)))
if(isFALSE(.cancopy)) stop("Cannot copy OSMOSE executable.")

# check for some defaults if missing
if(is.null(control$verbose)) control$verbose = TRUE
control$parallel = (ncores > 1)

message(sprintf("Running calibration in %s with %d cores.", 
                ifelse(control$parallel, "parallel", "sequential"), ncores))

conf = read_osmose(input = "master/osmose-calibration.osm")
model = get_par(conf, "output.file.prefix")
osm_ncpu = get_par(conf, "simulation.ncpu")
if(is.null(osm_ncpu) || !is.numeric(osm_ncpu)) osm_ncpu=1
if(osm_ncpu > 1) nht=1 # if OSMOSE runs in parallel, use hyper-threading
  
# explicit some variables
verbose   = control$verbose
parallel  = control$parallel
method    = control$method

source(run_model_file, local=TRUE)
if(!exists("run_model", mode="function"))
  stop(sprintf("We couldn't find the 'run_model' function in '%s'.", run_model_file))

# we are fixing the following names, 'cause too many arguments.  
setup = calibration_setup(file = sprintf("osmose-%s-calibration_settings.csv", model))

par_guess = read_osmose(input=sprintf("osmose-%s-parguess.osm", model))
par_min   = read_osmose(input=sprintf("osmose-%s-parmin.osm", model))
par_max   = read_osmose(input=sprintf("osmose-%s-parmax.osm", model))
par_phase = read_osmose(input=sprintf("osmose-%s-parphase.osm", model))

# read observed data
if(!file.exists(obs_file)) stop(sprintf("Observed data ('%s') not found.", obs_file))
observed = readRDS(obs_file)

# create objective function
osmose = file.path("..", osmose) # to make it relative to master
objfn = calibration_objFn(model=run_model, setup=setup, observed=observed, 
                          conf=conf, osmose=osmose, is_a_test=is_a_test)

if(!is.null(control$master)) {
  if(control$master!="master") {
    msg0 = "This script relays on using a 'master' folder right here. \nRename the folder '%s' to master and put it here."
    stop(sprintf(msg0, control$master))
  }  
}

if(!is.null(control$run)) {
  if(control$run!=run_path) {
    msg0 = "This script relays on using an specific '%s' folder.. \nIgnoring the line 'run = %s' from '%s'."
    warning(sprintf(msg0, run_path, control$run, control_file))
  }  
}

restart_file = if(is_a_test) "osmose-test" else sprintf("osmose-%s", model)
control$master = "master" # directory that will be copied
control$run = run_path   # run directory
control$restart.file = restart_file # name of the restart file
eff_cores = floor((ncores - 1)/nht/osm_ncpu)
control$ncores = eff_cores # the actual number of cores you have

.ncore = function(n) if(n>1) "cores" else "core"
msg = sprintf("Running calibration with %d %s, OSMOSE is using %d %s per simulation.",
              eff_cores, .ncore(eff_cores), osm_ncpu, .ncore(osm_ncpu))
message(msg)

if(isTRUE(parallel)) {
  
  if(!isTRUE(MPI)) {
    if(!require("doParallel")) stop("Package 'doParallel' not found")
    message(sprintf("Using doParallel for parallelisation (%s).", parallel:::getClusterOption("type")))
    cl = makeCluster(control$ncores)
    registerDoParallel(cl)
  } else {
    if(!require("doSNOW")) stop("Package 'doSNOW' not found")
    message("Using MPI for parallelisation. If you want to use OMP for running OSMOSE, configure it manually.")
    cl = makeCluster()
    registerDoSNOW(cl)
  }
  
  e = new.env()
  e$conf      = conf
  e$is_a_test = is_a_test
  e$osmose    = osmose
  
  clusterExport(cl, c("conf", "is_a_test", "osmose"), envir = e)
  message("Loading calibrar in the cluster...")
  l0 = clusterEvalQ(cl, library("calibrar"))
  message("Loading OSMOSE in the cluster...")
  l1 = clusterEvalQ(cl, library("osmose"))
  
}

# launch the calibration!
opt = try(calibrate(par=par_guess, fn=objfn, method=method,
                lower=par_min, 
                upper=par_max, 
                phases=par_phase,
                replicates=replicates,
                control=control,
                parallel=parallel))

if(inherits(opt, "try-error")) 
  message("Error while running the calibration.")
# tidy up
if(isTRUE(parallel)) stopCluster(cl)
