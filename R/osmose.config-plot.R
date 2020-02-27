
# Plot methods ------------------------------------------------------------

#' @title Plot methods for osmose-like objects
#' 
#' @rdname plot.osmose.config
#' 
#' @description This function implements a plot method for different osmose 
#' classes.
#' 
#' @param x \code{osmose} like object.
#' @param type A \code{numeric} value, indicating the type of plot to be used 
#' (\code{type = 1}, by default). See Details. 
#' @param species A \code{numeric} vector specifying the species that will be 
#' showed. If \code{NULL} (default), all the available species time series will 
#' be plotted. See Details.
#' @param speciesNames A vector with the names for each species. If \code{NULL}
#' (default) the names of \code{x} will be used.
#' @param start A \code{numeric} value indicating the first element of the time 
#' indexation of the object \code{x} is specified with this parameter. By 
#' default \code{start = NULL} and the time indexation start with the first 
#' element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of
#' the object \code{x} is specified with this parameter. By default 
#' \code{end = NULL} and the time indexation finish with the last element of 
#' \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element 
#' that is going to be used on the x axis for the plots.
#' @param type A \code{numeric} value, indicating the type of plot to be used 
#' (\code{type = 1}, by default). See Details.
#' @param freq A \code{numeric} value to indicate the steps by year used in the 
#' time series. Default values, see Details.
#' @param xlim,ylim numeric vectors of length 2, giving the x and y coordinates 
#' ranges.
#' @param col A vector with the color names for the plots. By default 
#' \code{col = NULL} and the colors will be chosen by the function.  
#' @param axes a \code{logical} value indicating whether both axes should be 
#' drawn on the plot. 
#' @param legend \code{logical} do you want to show a legend of species? (valid
#' only for those plot types TS-2)
#' @param names.arg A vector of names to be plotted below each bar or group of 
#' bars (check \code{\link{barplot}}).
#' @param addElements A \code{character} vector indicating extra graphical 
#' elements that can be included.
#' @param n \code{numeric} value indicating the number of steps that are going
#' to be used to plot the growth curve (\code{n = 100} as default). The larger
#' is n, the more resolution the curve will have.
#' @param ... Extra arguments of the function.
#' 
#' @details Describe types
#' 
#' \code{species} argument follows the indexation way of java: starting in zero,
#' as osmose-java returns species outputs.
#' 
#' Default value for \code{freq} will be calculated from \code{x}: 
#' \eqn{freq = 1/\code{x$model$start}}.
#' 
#' Extra arguments can be passed from \code{plot.default} using \code{...}: 
#' \code{cex},  \code{cex.axis}, \code{border} (useful for \code{polygon}, 
#' \code{boxplot}s and \code{barplot}s), etc.
#' 
#' @author Criscely Lujan Paredes
#' 
#' @export
plot.osmose.config.reproduction = function(x, type = 1, species = NULL, 
                                           speciesNames = NULL, start = NULL, 
                                           end = NULL, initialYear = NULL,
                                           freq = 12, xlim = NULL, ylim = NULL,
                                           col = "black", axes = TRUE, legend = TRUE,
                                           names.arg = NULL, ...){
  
  # species indexation
  if(is.null(species)){
    x = x[[1]] 
  }else{
    if(length(species) > 1) stop("The value of 'species' must be of length 1.")
    if(min(species) < 0 || max(species) > (length(x) - 1)) stop("Incorrect value for 'species'.")
    
    x = x[[species + 1]]
  }
  
  # Set default values for start and end
  if(is.null(start)) start = 1
  if(is.null(end)) end = dim(x)[1]
  
  # Check start and end values
  if(start < 1 | start > end) stop("Incorrect value for 'start' argument")
  if(end > dim(x)[1] | end < start) stop("Incorrect value for 'end' argument")
  
  # Index species data
  x = x[seq(start, end),]
  
  # Define default value for initialYear
  if(is.null(initialYear)) initialYear = 0
  
  # Define time vector
  times = seq(from = initialYear + start/freq, by = 1/freq, 
              length.out = nrow(x))
  
  # Define default values for xlim and ylim
  if(is.null(xlim)) xlim = range(times)
  if(is.null(ylim)) ylim = c(0, max(x))
  # ylim = if(is.null(ylim)) c(0, range(x[2])[2]*1.2)
  
  switch(type,
         "1" = plotReproductionType1(x = x, times = times, xlim = xlim, ylim = ylim,
                                     speciesNames = speciesNames, axes = axes,
                                     legend = legend, col = col, ...),
         "2" = plotReproductionType2(x = x, ylim = ylim, speciesNames = speciesNames,
                                     axes = axes, legend = legend, names.arg = names.arg,
                                     col = col, ...),
         stop("Not defined plot for 'type = ", type, "'."))
  
  return(invisible())
}


#' @rdname plot.osmose.config
#' @export
plot.osmose.config.species = function(x, n = 100, type = 1, species = NULL, speciesNames = NULL, 
                                      addElements = c("segments", "points", "polygon", "text"),
                                      axes = TRUE, xlim = NULL, ylim = NULL, 
                                      legend = TRUE, col = "black", ...){
  
  switch(type,
         "1" = plotGrowthType1(x = x, n = n, species = species, 
                               speciesNames = speciesNames, 
                               addElements = addElements, axes = axes,
                               xlim = xlim, ylim = ylim, legend = legend,
                               col = col, ...),
         stop("Not defined plot for 'type = ", type, "'."))
  
  return(invisible())
}


#' @rdname plot.osmose.config
#' @export
plot.osmose.config.predation = function(x, type = 1, species = NULL, speciesNames = NULL, 
                                        addElements = c("segments", "points", "text"),
                                        axes = TRUE, xlim = NULL, ylim = NULL, col = "black", 
                                        legend = TRUE, ...){
  
  switch(type,
         "1" = plotPredationType1(x = x, species = species, speciesNames = speciesNames, 
                                  addElements = addElements, axes = axes, 
                                  xlim = xlim, ylim = ylim, 
                                  col = col, legend = legend, ...),
         stop("Not defined plot for 'type = ", type, "'."))
  
  return(invisible())
}

