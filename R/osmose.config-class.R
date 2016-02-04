#' @export
readOsmoseConfiguration = function(file, config=NULL, absolute=TRUE) {
  
  L0 = .readOsmoseConfiguration(input=file, absolute=absolute)
  
  if(!is.null(config)) {
    config = .getConfig(config)
    L0 = c(config, L0)
    L0 = L0[!duplicated(names(L0))]
  }
  
  L1 = .createParameterList(L0)
  class(L1) = c("osmose.config", class(L1))
  
  return(L1)
}



configureCalibration = function(L1) {
  
  nameCal  = names(unlist(L1$calibration))
  valueCal = unname(unlist(L1$calibration))
  
  #guess List
  guessList = .createCalibrationList(nameCal,valueCal,"\\.max|\\.min|\\.phase",TRUE)
  
  #max List
  maxList   = .createCalibrationList(nameCal,valueCal,"\\.max",FALSE)
  
  #phase List
  phaseList = .createCalibrationList(nameCal,valueCal,"\\.phase",FALSE)
  
  #min List
  minList   = .createCalibrationList(nameCal,valueCal,"\\.min",FALSE)
  
  L2 = list(guess=guessList, max=maxList, min=minList, phase=phaseList)
  
  return(L2)
  
}


# Methods -----------------------------------------------------------------

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


