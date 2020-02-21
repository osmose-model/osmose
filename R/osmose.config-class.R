#' Reads Osmose configuration files.
#' 
#' @param file Main configuration file
#' @param config Configuration object to which file parameters are appended
#' @param absolute Whether the path is absolute (TRUE) or relative (FALSE)
#' @return A list tree.
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



#' Reads calibration parameters from an osmose.config list.
#' 
#' The configuration argument must contain a "calibration" entry to work.
#'
#' @param L1 osmose.config object (see \code{\link{readOsmoseConfiguration}})
#'
#' @return A list of parameters to calibrate ("guess", "max", "min", "phase")
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


#' get_var method for osmose configuration objects
#' @description Get the configuration files from 
#' @param object Object of \code{osmose.config} class. 
#' See the \code{\link{read_osmose}} and \code{\link{readOsmoseConfiguration}} functions
#'  for more information about this object.
#' @param what Name of the variable to extract from the configuration file.
#' @param ... Extra arguments for plotting method.
#' @return An object of \code{list} class containing all the relevant information about 
#' the variable extracted.
#' @export
#' @method get_var osmose.config
get_var.osmose.config = function(object, what, ...) {
  
  what = getWhats(x = what)
  
  x = object[[what[1]]]
  
  if(is.null(x)){
    message = paste("The", sQuote(what[1]),
                    "variable doesn't exist on the configuration file.", sep = "")
    stop(message)
  }
  
  getConfigVar = c("reproduction", "species", "predation")
  
  if(what %in% getConfigVar){
    x = switch(what,
               reproduction  = getReproductionData(x, var = "season.file", ...),
               species       = getSpeciesData(x, ...),
               predation     = getPredationData(x, object = object, ...))
  }
  
  class(x) = c(paste("osmose.config", what, sep = "."), class(x))
  
  return(x)
}

# Methods -----------------------------------------------------------------

#' Title
#'
#' @param x TODO
#' @param what TODO
#' @param ... TODO
#'
#' @return TODO
#' @export 
#' @method plot osmose.config
plot.osmose.config = function(x, what = NULL, ...) {
  
  if(!is.null(what)){
    x = get_var.osmose.config(x, what = what)
    plot(x, ...)
    
  } else {
    message("An variable of the configuration file have to be chosen for the plot")
  }
  
  return(invisible())
}

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


