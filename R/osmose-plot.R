
# Plot methods ------------------------------------------------------------

#' @title Plot methods for osmose-like objects
#' 
#' @rdname plot.osmose
#' 
#' @description This function implements a plot method for different osmose 
#' classes.
#' 
#' @param x \code{osmose} like object. 
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots 
#' are going to be time series where the x-axis is the time. For plots where the 
#' x-axis is not the time \code{ts = FALSE}.
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
#' @param replicates \code{logical}. It controls to show whether the values in 
#' each simulation (\code{TRUE}) or the median of the values along the time 
#' (\code{FALSE}).
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show 
#' on the plots. This parameter is used only when \code{replicates = TRUE}.
#' @param freq A \code{numeric} value to indicate the steps by year used in the 
#' time series. Default values, see Details.
#' @param horizontal a \code{logical} value. If \code{FALSE}, plot types that do 
#' not represent time series (boxplots and barplots) will be drawn horizontally.
#' @param conf \code{numeric} single value indicating the confidence interval 
#' showed in the plot as shadow. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. 
#' By default \code{factor = 1e-3}.
#' @param xlim,ylim numeric vectors of length 2, giving the x and y coordinates 
#' ranges.
#' @param col A vector with the color names for the plots. By default 
#' \code{col = NULL} and the colors will be chosen by the function.  
#' @param alpha factor modifying the opacity alpha (typically in [0,1], by 
#' default 1) given to confidence interval shadow (check 
#' \code{\link{adjustcolor}}).
#' @param lty The line type (check \code{\link{par}}).
#' @param lwd The line width (check \code{\link{par}}).
#' @param axes a \code{logical} value indicating whether both axes should be 
#' drawn on the plot. 
#' @param legend \code{logical} do you want to show a legend of species? (valid
#' only for those plot types TS-2)
#' @param units String with units of the variable that defines the label set 
#' on top of the plot. If \code{NULL} (default), the value will depend on the
#' class. For instance, if plot if about biomass or abundance, units will be set
#' on 'tonnes' or 'individuals', respectively.
#' @param ... Extra arguments of the function.
#' 
#' @details Describe types-ts
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
plot.osmose.biomass = function(x, ts = TRUE, type = 1, species = NULL, 
                               speciesNames = NULL, start = NULL, end = NULL,  
                               initialYear = NULL, replicates = TRUE, nrep = 3, 
                               freq = 12, horizontal = FALSE, conf = 0.95, 
                               factor = 1e-3, xlim = NULL, ylim = NULL, 
                               col = NULL, alpha = 1, lty = 1, lwd = 1, 
                               axes = TRUE, legend = TRUE, units = "tonnes", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend,
                units = units, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @export
plot.osmose.abundance = function(x, species = NULL, speciesNames = NULL, 
                                 start = NULL, end = NULL, 
                                 initialYear = NULL, ts = TRUE, type = 1, 
                                 replicates = TRUE, nrep = 3, freq = 12, 
                                 horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                                 xlim = NULL, ylim = NULL, col = NULL, alpha = 1,
                                 lty = 1, lwd = 1, axes = TRUE, legend = TRUE,
                                 units = "individuals", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend,
                units = units, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @export
plot.osmose.yield = function(x, species = NULL, speciesNames = NULL, 
                             start = NULL, end = NULL, 
                             initialYear = NULL, ts = TRUE, type = 1, 
                             replicates = TRUE, nrep = 3, freq = 12, 
                             horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                             xlim = NULL, ylim = NULL, col = NULL, alpha = 1,
                             lty = 1, lwd = 1, axes = TRUE, legend = TRUE, 
                             units = "tonnes", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend,
                units = units, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @export
plot.osmose.yieldN = function(x, species = NULL, speciesNames = NULL, 
                              start = NULL, end = NULL, 
                              initialYear = NULL, ts = TRUE, type = 1, 
                              replicates = TRUE, nrep = 3, freq = 12, 
                              horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                              xlim = NULL, ylim = NULL, col = NULL, alpha = 1,
                              lty = 1, lwd = 1, axes = TRUE, legend = TRUE, 
                              units = "individuals", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend,
                units = units, ...)
  
  return(invisible())
}
