rm(list=ls())

library(osmose)

#runOsmose("osm_all-parameters.csv")
run_osmose("osm_all-parameters.csv", osmose='/home/nbarrier/Modeles/osmose/svn-osmose/branches/stable-3/dist/osmose_stable_3.jar', version="3.3.3")
