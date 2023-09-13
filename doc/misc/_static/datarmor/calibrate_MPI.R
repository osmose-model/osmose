# Need to load the doSNOW package, which is 
# installed in Nicolas Barrier's home
require("doSNOW")

# call the RMPI/Snow make cluster (note here that there are no arguments!)
cl <- makeCluster()

# call the registerDoSNOW function instead of the registerDoParallel
registerDoSNOW(cl)

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
