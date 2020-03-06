
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
#' @param freq A \code{numeric} value to indicate the steps by year used in the 
#' time series. Default values, see Details.
#' @param xlim,ylim numeric vectors of length 2, giving the x and y coordinates 
#' ranges.
#' @param col A vector with the color names for the plots. By default 
#' \code{col = NULL} and the colors will be chosen by the function.  
#' @param axes a \code{logical} value indicating whether both axes should be 
#' drawn on the plot. 
#' @param border the color to draw the border of CI polygons, bar plots and 
#' box plots. By default, \code{border = NA}, which means that no border will be 
#' drawn.
#' @param legend \code{logical} do you want to show a legend of species? (valid
#' only for those plot types TS-2)
#' @param addElements A \code{character} vector indicating extra graphical 
#' elements that can be included.
#' @param n \code{numeric} value indicating the number of steps that are going
#' to be used to plot the growth curve (\code{n = 100} as default). The larger
#' is n, the more resolution the curve will have.
#' @param ... Extra arguments of the function.
#' 
#' @details Plot types will depend on the class of \code{x}, which is defined
#' by \code{what}. Thereby,
#' \itemize{
#' \item{\code{what = predation}: }{Generates a plot of size range as shadows
#' for a selected species .}
#'  \item{\code{what = reproduction}: }{Generates a single plots of seasonality 
#'  of reproduction whether as lines (\code{type = 1}) or bars (\code{type = 2})}
#'  \item{\code{what = species}: }{Generates a plot of growth curve following the
#'  VB parameters defined on configuration files (\code{type = 1}).}
#' }
#' 
#' \code{species} argument follows the indexation way of java: starting in zero,
#' as osmose-java returns species outputs.
#' 
#' Default value for \code{freq} will be calculated from \code{x}: 
#' \eqn{freq = 1/x$model$start}.
#' 
#' Extra arguments can be passed using \code{...} and depending on the type, you
#' can modify arguments like: \code{cex},  \code{cex.axis}, \code{border} 
#' (useful for \code{polygon} and \code{barplot}s), etc.
#' 
#' @note Ellipsis (\code{...}) must be used carefully, since it will pass the
#' arguments to different generic plot functions. For instance, \code{type = 2} 
#' of \code{osmose.config.reproduction} method will use \code{...} to pass 
#' arguments to \link{barplot}, so some arguments like \code{cex} may match with 
#' many formal arguments (e.g. \code{cex.axis} and \code{cex.names}), so it may 
#' cause errors.
#' 
#' @author Criscely Lujan Paredes
#' 
#' @method plot osmose.config.reproduction
#' @export
plot.osmose.config.reproduction = function(x, type = 1, species = 0, 
                                           speciesNames = NULL, start = NULL, 
                                           end = NULL, initialYear = NULL,
                                           freq = 12, xlim = NULL, ylim = NULL,
                                           col = "black", axes = TRUE, 
                                           border = NA, legend = TRUE, ...){
  
  # species indexation
  if(is.null(species) || is.na(species)) stop("'species' argument must be specified.")
  if(length(species) > 1) stop("The value of 'species' must be of length 1.")
  if(min(species) < 0 || max(species) > (length(x) - 1)) stop("Incorrect value for 'species'.")
  
  x = x[[species + 1]]
  
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
  
  switch(type,
         "1" = plotReproductionType1(x = x, times = times, 
                                     xlim = xlim, ylim = ylim,
                                     speciesNames = speciesNames, axes = axes,
                                     legend = legend, col = col, ...),
         "2" = plotReproductionType2(x = x, ylim = ylim, 
                                     speciesNames = speciesNames,
                                     axes = axes, border = border, 
                                     legend = legend, col = col, ...),
         stop("Not defined plot for 'type = ", type, "'."))
  
  return(invisible())
}


#' @rdname plot.osmose.config
#' @method plot osmose.config.species
#' @export
plot.osmose.config.species = function(x, n = 100, type = 1, species = 0, 
                                      speciesNames = NULL, 
                                      addElements = c("segments", "points", "polygon", "text"),
                                      axes = TRUE, border = NA, 
                                      xlim = NULL, ylim = NULL, 
                                      legend = TRUE, col = "black", ...){
  
  if(is.null(species) || is.na(species)) stop("'species' argument must be specified.")
  if(length(species) > 1) stop("The value of 'species' must be of length 1.")
  if(min(species) < 0 || max(species) > (length(x) - 1)) stop("Incorrect value for 'species'.")
  
  switch(type,
         "1" = plotGrowthType1(x = x, n = n, species = species, 
                               speciesNames = speciesNames, 
                               addElements = addElements, axes = axes,
                               border = border, xlim = xlim, ylim = ylim, 
                               legend = legend, col = col, ...),
         stop("Not defined plot for 'type = ", type, "'."))
  
  return(invisible())
}


#' @rdname plot.osmose.config
#' @method plot osmose.config.predation
#' @export
plot.osmose.config.predation = function(x, type = 1, species = 0, 
                                        speciesNames = NULL, 
                                        addElements = c("segments", "points", "text"),
                                        axes = TRUE, border = NA, 
                                        xlim = NULL, ylim = NULL, col = "gray70", 
                                        legend = TRUE, ...){
  
  if(is.null(species) || is.na(species)) stop("'species' argument must be specified.")
  if(length(species) > 1) stop("The value of 'species' must be of length 1.")
  if(min(species) < 0 || max(species) > (length(x$speciesNames) - 1)) stop("Incorrect value for 'species'.")
  
  switch(type,
         "1" = plotPredationType1(x = x, species = species, 
                                  speciesNames = speciesNames, 
                                  addElements = addElements, axes = axes, 
                                  border = border, xlim = xlim, ylim = ylim, 
                                  col = col, legend = legend, ...),
         stop("Not defined plot for 'type = ", type, "'."))
  
  return(invisible())
}

