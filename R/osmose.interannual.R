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

.writeFishingFile = function(sp, fishing, species, T, ndt, output) {
  
  if(is.null(fishing$rate$method[[sp]])) return(NULL)
    
  fileCode = file.path(output, "fishing_%s_%s.csv")
  
  f           = getFishingMortality(sp, fishing, T, ndt) 
  write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
  
  selectivity = getSelectivity(sp, fishing, T, ndt)
  if(!is.null(selectivity)) {
    Fs          = f %o% selectivity
    write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
    
  }
  
  fishing = cbind(fishing, f) 
  write.osmose(Fs, file=file.path(output, paste0("F-", isp, ".csv")))
  
}

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


# Auxiliar ----------------------------------------------------------------

.getFishingFrequency = function(sp, fishing, ndt) {
  periods = fishing$periodsperyear[[sp]]
  if(periods%%1!=0) stop(sprintf("periodsPerYear.%s must be an integer.", sp))
  freq = ndt/periods
  if(freq%%1!=0) stop(sprintf("simulation.time.ndtPerYear must be a multiple of periodsPerYear.%s.", sp))
  return(freq)
}

.normalizeByFreq = function(x, freq) {
  if(any(is.na(x))) stop("x must not contain NA.")
  if(any(x<0)) stop("x must not contain negative values")
  if(sum(x)==0) return(x)
  ind = rep(seq_along(x), each=freq, length=length(x))
  xNorm = tapply(x, INDEX = ind, FUN= function(x) x/sum(x))
  xNorm = setNames(unlist(xNorm), nm = names(x))
  return(xNorm)
}

.calculateDeviates = function(x, freq, ndt) {
  x = .normalizeByFreq(x=x, freq=freq)
  deviates = log(x) + log(freq)
  return(deviates)
}

calculateSeasonalPattern = function(x, ndt) {
  rowMeans(matrix(x, nrow=ndt))
}

