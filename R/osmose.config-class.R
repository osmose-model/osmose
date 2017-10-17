#' Reads Osmose configuration files.
#' 
#' @param file Main configuration file
#' @param config Configuration object to which file parameters are appended
#' @param absolute Whether the path is absolute (TRUE) or relative (FALSE)
#' @return A list tree.
#' @usage config = readOsmoseConfiguration(fileName)
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



#' Reads calibration parameters from an osmose.config list
#'
#' @param L1 osmose.config list (see \code{\link{readOsmoseConfiguration}})
#'
#' @return A list of parameters to calibrate ("guess", "max", "min", "phase")
#' @usage 
#' config = readOsmoseConfiguration(fileName)
#' calpar = configureCalibration(config)
#' @export
configureCalibration = function(L1) {
  
  nameCal  = names(unlist(L1$calibration))
  valueCal = unname(unlist(L1$calibration))
  
  #guess List
  guessList = .createCalibrationList(nameCal, valueCal, "\\.max|\\.min|\\.phase", TRUE)
  
  #max List
  maxList   = .createCalibrationList(nameCal, valueCal, "\\.max", FALSE)
  
  #phase List
  phaseList = .createCalibrationList(nameCal, valueCal, "\\.phase", FALSE)
  
  #min List
  minList   = .createCalibrationList(nameCal, valueCal, "\\.min", FALSE)
  
  L2 = list(guess=guessList, max=maxList, min=minList, phase=phaseList)
  
  return(L2)
  
}


# Methods -----------------------------------------------------------------

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


