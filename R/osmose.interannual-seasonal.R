
# getDeviatesBySeason -----------------------------------------------------

getDeviatesBySeason = function(sp, fishing, T, ndt) {
  useFiles = .getBoolean(fishing$deviate$season$useFiles, FALSE)
  method = fishing$season$method[[sp]]
  if(is.null(method)) method = "default"
  out = switch(method,
               "default"      = getDeviatesBySeason.default(sp, fishing, T, ndt, useFiles),
               "periodic"     = getDeviatesBySeason.periodic(sp, fishing, T, ndt, useFiles),
               "non-periodic" = getDeviatesBySeason.nonPeriodic(sp, fishing, T, ndt, useFiles))
  return(out)
}

# getDeviatesBySeason.default

# getDeviatesBySeason.periodic
getDeviatesBySeason.periodic = function(sp, fishing, T, ndt, useFiles=FALSE) {

  freq = .getFishingFrequency(sp, fishing, ndt)
  
  if(useFiles) {
    deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
    if(is.null(deviates)) deviates = fishing$deviate$season[[sp]]    
  } else {
    deviates = fishing$deviate$season[[sp]]        
    if(is.null(deviates)) deviates = .getFileAsVector(fishing$deviate$season$file[[sp]])
  }
  
  if(is.null(deviates)) {
    season = .getFileAsVector(fishing$season$distrib$file[[sp]])
    if(is.null(season)) {
      season = .getFileAsVector(fishing$season$byDt$file[[sp]]) 
      if(is.null(season)) stop(sprintf("No seasonality information for %s", sp))
    }
    stopifnot(length(season)%%ndt==0)
    deviates = .calculateDeviates(x=season, freq, ndt)  
  }
  
  stopifnot(length(deviates)%%ndt==0)
  deviates = calculateSeasonalPattern(x=deviates, ndt=ndt)
  
  return(deviates)  
  
}


# getDeviatesBySeason.nonPeriodic
