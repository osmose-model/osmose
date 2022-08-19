library("osmose")
library("calibrar")
library("doParallel")

data_path = "data_templates" # replace for the real folder
is_a_test = TRUE # change to FALSE for a real calibration.
parallel  = FALSE # change to TRUE for a real calibration
method    = "AHR-ES" # (Adaptative Hierarchical Recombination Evolutionary Strategy, 
#' Oliveros-Ramos & Shin, 2016). This is the current default method for OSMOSE.
#' It's NOT a Genetic Algorithm. It's an Evolutionary Strategy (algorithm).
verbose   = FALSE # to keep it quiet, unless you are nervous and want to know everything.

conf = read_osmose("master/osmose-calibration.osm")
model = get_par(conf, "output.file.prefix")

source("runModel.R")

setup = calibration_setup(file = sprintf("osmose-%s-calibration_settings.csv", model))

par_guess = read_osmose(input=sprintf("osmose-%s-parguess.osm", model))
par_min   = read_osmose(input=sprintf("osmose-%s-parmin.osm", model))
par_max   = read_osmose(input=sprintf("osmose-%s-parmax.osm", model))
par_phase = read_osmose(input=sprintf("osmose-%s-parphase.osm", model))

observed = calibration_data(setup=setup, path=data_path, verbose=verbose)

objfn = calibration_objFn(model=runModel, setup=setup, observed=observed, 
                          conf=conf, is_a_test=is_a_test)

control = list()
control$maxgen = 2   # maximum number of generations (former gen.max parameter)
control$master = "master" # directory that will be copied
control$run = "../run"   # run directory
control$restart.file = sprintf("osmose-%s-restart", model)   # name of the restart file
control$REPORT = 1    # number of generations to run before saving a restart
control$parallel = parallel
control$nCores = 1 # the actual number of cores you have

if(isTRUE(parallel)) {
  
  cl = makeCluster(control$nCores)
  registerDoParallel(cl)
  clusterExport(cl, c("objfn", "observed", "conf", "control", "par_min", 
                      "par_max", "par_guess", "par_phase"))
  clusterEvalQ(cl, library("osmose"))
  clusterEvalQ(cl, library("calibrar"))
  
}

opt = calibrate(par=par_guess, fn=objfn, method="default",
                lower=par_min, 
                upper=par_max, 
                phases=par_phase,
                replicates=1,
                control=control)

if(isTRUE(parallel)) stopCluster(cl)
