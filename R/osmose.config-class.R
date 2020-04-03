#' @title Plot method for \code{osmose.config} objects
#' @description This method takes a \code{osmose.config} object and produce 
#' useful plots.
#' 
#' @param x Object of \code{osmose.config} class. See the \code{\link{read_osmose}} 
#' function for more details.
#' @param what Variable name to plot. By default is \code{what = "predation"}. 
#' See Details.
#' @param ... Additional arguments for the plot function.
#' 
#' @export
#' @method plot osmose.config
plot.osmose.config = function(x, what = "predation", ...) {
  
  x = get_var.osmose.config(x, what = what)
  plot(x, ...)
  
  return(invisible())
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
  
  x = switch(what,
             reproduction  = getReproductionData(x, var = "season.file", ...),
             species       = getSpeciesData(x, ...),
             predation     = getPredationData(x, object = object, ...),
             x)
  
  class(x) = c(paste("osmose.config", what, sep = "."), class(x))
  
  return(x)
}

# Methods -----------------------------------------------------------------

# print.osmose.config = function(x, ...)

# summary.osmose.config = function(x, ...)

# print.summary.osmose.config = function(x, ...)


