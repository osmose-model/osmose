# With OMP, you need to load the doParallel library
require("doParallel")

# Initialisation of the cluster.
# BE SURE THAT THE NUMBER OF CORE HERE IS 
# CONSISTENT WITH THE NUMBER OF YOUR PBS FILE
cl <- makeCluster(control$nCores)

# register the doParallel so that for each is activated
registerDoParallel (cl)

# send the variables and loaded libraries defined in the above to the nodes
clusterExport(cl, c("objfn", "calibData", "calInfo", "observed", "minmaxt"))
clusterEvalQ(cl, library("osmose"))
clusterEvalQ(cl, library("calibrar"))

# run the calibration
cal1 = calibrate(calibData['paropt'], fn=objfn, method='default',
                 lower=calibData['parmin'], upper=calibData['parmax'], 
                 phases=calibData['parphase'], control=control, replicates=1)

# stop the cluster
stopCluster(cl)
