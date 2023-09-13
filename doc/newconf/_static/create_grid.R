rm(list=ls())

library("osmose")
setwd("newconf/_static")

ltl_file = system.file(package="osmose", "extdata", "gog_v4", 
                       "corr_eco3m_gog44_conversion_factor.nc")

# extract the grid parameters
gridpar = extract_grid_param(ltl_file, lonname="longitude", 
                             latname="latitude", csv="grid_param.csv")
gridpar

# extract the grid mask file
mask = extract_grid_mask(ltl_file, varname="COPC", output_file="grid_mask.csv", fillVal=0)

# revert data for the plot
mask = apply(mask, 2, rev)

png("mask.png")
par(mar = c(2, 2, 2, 2)) # Set the margin on all sides to 2
image(t(mask), useRaster=TRUE, col = c("black", "white"), xaxt="n", yaxt="n")
