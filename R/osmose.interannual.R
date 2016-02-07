# 
# file = "data/config.csv"
# L1 = readOsmoseConfiguration(file)
# 
# species = L1$species
# fishing = L1$mortality$fishing
# 
# selectivity = fishing$selectivity
# vector > file
# configureCalibration read files and create vectors, fill missings, defaults. 
# sorts all the calibration information in one file to check. "calibration-config.csv"
# calibration have a vector/list, pass it to buildConfiguration
# buildConfiguration creates the proper file (vector has precedence but normally file is used)
# mortality.fishing.deviate.byYear.useFiles;true # default is false, but changed after buildConfiguration!
# mortality.fishing.deviate.season.useFiles;true # default is false, but changed after buildConfiguration!
# 
# F = getFishingMortality()
# # 
# Fm  = getFishingRate()
# dy  = getDeviatesByYear()
# ds  = getDeviatesBySeason()
# 
# F = Fm*exp(dy+ds)

writeFishingFiles = function(L1, outputPath) {
  
  nsp = getOsmoseParameter(L1, "simulation", "nspecies")
  T   = getOsmoseParameter(L1, "simulation", "time", "nyear")
  ndt = getOsmoseParameter(L1, "simulation", "time", "ndtPerYear")

  spp = .getSpecies(L1$fishing$rate$method)
  
  if(length(spp)==0) return(NULL)
  
  ofiles = unlist(sapply(spp, FUN=.writeFishingFile, 
                         fishing=L1$mortality$fishing,
                         species=L1$species, T=T, ndt=ndt,
                         output=outputPath))

  return(ofiles)  
}

