rm(list=ls())
require("osmose")
require("calibrar")
require("doParallel")

# Recovering of the runModel function associated with the reference configuration
dirin = getwd()
filename = file.path(dirin, "runModel.R")
source(filename)

# creates a user defined likelyhood function
minmaxt = function(obs, sim) {
    output = -1e+6*sum(log(pmin((sim+1)/(obs[, 1]+1), 1)), na.rm=TRUE) + 
              1e+6*sum(log(pmax((sim+1)/(obs[, 2]+1), 1)), na.rm=TRUE)
    return(output)
}

# reads calibration informations
calib_path = dirin
calib_file = "calibration_settings.csv"
calInfo = getCalibrationInfo(path=calib_path, file=calib_file)

# loads the observed data (path is the data directory path, 
# data.folder is the data directory name)
observed = getObservedData(calInfo, path=calib_path, data.folder="DATA")

# load calibration parameters
param_to_calib = file.path(dirin, "parameters_to_calib.csv")
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
# control$maxgen = c(2, 2, 2, 2)   # maximum number of generations (former gen.max parameter)
control$maxgen = 2   # maximum number of generations (former gen.max parameter)
control$master = "gog_v4" # directory that will be copied
control$run = "RUN"   # run directory
control$restart.file = "calib_restart"   # name of the restart file
control$REPORT = 1    # number of generations to run before saving a restart
control$parallel = TRUE
control$nCores = 2
control$popsize = 15   # population  size (former seed parameter)

cl = makeCluster(control$nCores)
registerDoParallel(cl)

# send the variables and loaded libraries defined in the above to the nodes
clusterExport(cl, c("objfn", "calibData", "calInfo", "observed", "minmaxt"))
clusterEvalQ(cl, library("osmose"))
clusterEvalQ(cl, library("calibrar"))

cal1 = calibrate(calibData['paropt'], fn=objfn, method='default',
                 lower=calibData['parmin'], upper=calibData['parmax'], 
                 phases=calibData['parphase'], replicates=1, control=control)

# stopCluster(cl)
