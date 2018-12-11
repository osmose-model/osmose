rm(list=ls())
require("osmose")
require("calibrar")
require("doParallel")

# Recovering of the runModel function associated with the reference configuration
filename = system.file(package="osmose", "extdata", "calib", "runModel.R")
source(filename)

# creates a user defined likelyhood function
minmaxt = function(obs, sim) {
    output = -1e+6*sum(log(pmin((sim+1)/(obs[, 1]+1), 1)), na.rm=TRUE) + 
              1e+6*sum(log(pmax((sim+1)/(obs[, 2]+1), 1)), na.rm=TRUE)
    return(output)
}

# reads calibration informations
master_path = 
calib_path = system.file(package="osmose", "extdata", "calib")
calib_file = "calibration_settings.csv"
calInfo = getCalibrationInfo(path=calib_path, file=calib_file)
print(calInfo)

# loads the observed data (path is the data directory path, 
# data.folder is the data directory name)
# todo: merge data.folder and path
observed = getObservedData(calInfo, path=calib_path, data.folder="DATA")

# load calibration parameters
param_to_calib = system.file(package="osmose", "extdata", "calib", "parameters_to_calib.csv")
calibData = read.csv(file=param_to_calib, 
                     header=TRUE, 
                     sep=",", 
                     row.names=1)

# create an objective function
# additional arguments to the runModel function
# are provided here.
objfn = createObjectiveFunction(runModel=runModel, 
                                info=calInfo, 
                                observed=observed, 
                                aggFn=calibrar:::.weighted.sum,
                                aggregate=FALSE,
                                names=row.names(calibData))

control = list()
control$maxgen = c(150, 200, 250, 300)   # maximum number of generations (former gen.max parameter)
control$master = system.file(package="osmose", "extdata", "master")   # directory that will be copied
control$run = "RUN"   # run directory
control$restart.file = "calib_restart"   # name of the restart file
control$REPORT = 1    # number of generations to run before saving a restart
control$parallel = FALSE
control$nCores = 2
control$maxgen = 1   # maximum number of generations (former gen.max parameter)
control$popsize = 15   # population  size (former seed parameter)

cl = makeCluster(control$nCores)
registerDoParallel(cl)

cal1 = calibrate(calibData['paropt'], fn=objfn, method='default',
                 lower=calibData['parmin'], upper=calibData['parmax'], 
                 phases=calibData['parphase'], control=control, replicates=2)

stopCluster(cl)
