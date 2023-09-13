rm(list=ls())

library("osmose")

# copy all that is needed for the calibration
demo = osmose_calib_demo(path = "calib/_static/")

# move to the calibration folder
setwd(demo$path)

# run the calibration script
#source(demo$file)
