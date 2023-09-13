# OSMOSE (Object-oriented Simulator of Marine Ecosystems)
# http://www.osmose-model.org
#
# Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
#
# Osmose is a computer program whose purpose is to simulate fish
# populations and their interactions with their biotic and abiotic environment.
# OSMOSE is a spatial, multispecies and individual-based model which assumes
# size-based opportunistic predation based on spatio-temporal co-occurrence
# and size adequacy between a predator and its prey. It represents fish
# individuals grouped into schools, which are characterized by their size,
# weight, age, taxonomy and geographical location, and which undergo major
# processes of fish life cycle (growth, explicit predation, additional and
# starvation mortalities, reproduction and migration) and fishing mortalities
# (Shin and Cury 2001, 2004).
#
# Contributor(s):
# Yunne SHIN (yunne.shin@ird.fr),
# Morgane TRAVERS (morgane.travers@ifremer.fr)
# Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
# Philippe VERLEY (philippe.verley@ird.fr)
# Laure VELEZ (laure.velez@ird.fr)
# Nicolas Barrier (nicolas.barrier@ird.fr)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation (version 3 of the License). Full description
# is provided on the LICENSE file.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.


# Plot methods ------------------------------------------------------------

#' @title Plot methods for \code{osmose} objects
#' 
#' @rdname plot.osmose
#' 
#' @description This function implements a plot method for different osmose 
#' classes.
#' 
#' @param x \code{osmose} object. 
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
#' @param alpha factor modifying the opacity alpha given to confidence interval 
#' (CI) polygons (check \code{\link{adjustcolor}}). By default (\code{NULL}), 
#' its value will depend on the plot type. See Details.
#' @param border the color to draw the border of CI polygons, bar plots and 
#' box plots. The default value (\code{NULL}) will depend on the type, see Details.
#' @param lty The line type (check \code{\link{par}}).
#' @param lwd The line width (check \code{\link{par}}).
#' @param axes a \code{logical} value indicating whether both axes should be 
#' drawn on the plot. 
#' @param legend \code{logical} do you want to show a legend of species? (valid
#' only for those plot types TS-2)
#' @param units String with units of the variable that defines the label set 
#' on top of the plot. If \code{NULL} (default), the value will depend on the
#' class, see Details.
#' @param ... Extra arguments of the function. See Details.
#' 
#' @details User can select within different plot types depending on the osmose
#' class. So, for classes \code{biomass}, \code{abundance}, \code{yield} and 
#' \code{yieldN} (\strong{Group 1}) the user must use \code{ts} and \code{type} 
#' arguments to specify the type of plot to draw.
#' 
#' So, for the \strong{Group 1} and \code{ts = TRUE}, user will select within:
#' \itemize{
#'  \item{\code{type = 1}: }{Generates a matrix plot of the selected variable 
#'  plotted independently.}
#'  \item{\code{type = 2}: }{Generates a single plot with overlaped time series.
#'  You can include confidence interval shadow around lines.}
#'  \item{\code{type = 3}: }{Generates a single plot with cumulated time series.
#'  So, the method will sort and overlap the time series values for the selected 
#'  variable}
#'  \item{\code{type = 4}: }{ONLY valid for a single species. It generates a bar 
#'  plot of the time series.}
#' }
#' 
#' For the Group 1 and \code{ts = FALSE}, user will select within:
#' \itemize{
#'  \item{\code{type = 1}: }{Generates a bar plot of the selected variable by 
#'  species, including interval confidence bars.}
#'  \item{\code{type = 2}: }{Generates a boxplot of the selected variable by 
#'  species.}
#' }
#' 
#' The \strong{Group 2} refers classes of \strong{Group 1} with \strong{By} 
#' specification (e.g. \code{yieldNBySize}, \code{abundanceByAge},
#' \code{biomassByTL}). There are 2 available plot types:
#' 
#' \itemize{
#'  \item{\code{type = 1}: }{Generates a bar plot of the selected variable by 
#'  species. The \code{...} will be passed to the internal \link{barplot} 
#'  function.}
#'  \item{\code{type = 2}: }{Generates a plot using \link{image.plot} function,
#'  placing the species in the left axis, the By-variable (e.g. Size) on the 
#'  bottom and the main variable (e.g. biomass) in a color scale. The \code{...}
#'  will be passed to the internal \link{image.plot} function.}
#' }
#' 
#' Both \code{alpha} and \code{border} controls the opacity and the color of 
#' border, respectively, for polygons, bar plots and box plots. So depending on 
#' the \code{ts}-\code{type}, they will have different default values: 
#' \itemize{
#'  \item{\code{alpha = 0.3} & \code{border = NA} for }{\code{TS = TRUE} with 
#'  \code{type = 1 or 2}.}
#'  \item{\code{alpha = 1} & \code{border = TRUE} for }{Everything else.}
#' }
#' 
#' \code{species} argument follows the indexation way of java: starting in zero,
#' as osmose-java returns species outputs.
#' 
#' Default value for \code{freq} will be calculated from \code{x}: 
#' \eqn{freq = 1/x$model$start}.
#' 
#' \code{units} could be whether a single character vector (currently applied on
#' classes \strong{without By}, e.g. \code{osmose.biomass}, 
#' \code{osmose.yieldN}) or a list of length 2 specifying the units for x and y 
#' (currently used with classes \strong{with By}, e.g. 
#' \code{plot.osmose.biomassBySize}, \code{plot.osmose.yieldNByAge}).
#' 
#' Extra arguments can be passed from \link{plot.default} using \code{...}: 
#' \code{cex}, \code{cex.axis}, \code{border} (useful for \link{polygon}, 
#' \link{boxplot}s and \link{barplot}s), etc. It is important to notice that,
#' depending on where the ellipsys is passed, it allows and refuse the use of
#' certain arguments. For instance, if the selected plot is a bar plot, the 
#' \code{...} will be passed to the internal \code{barplot} call, so if 
#' \code{cex} argument is used, it will create a conflict and an error (a 
#' typical error of using \code{cex} in \link{barplot} function).
#' 
#' @author Criscely Lujan Paredes
#' 
#' @export
#' @method plot osmose.biomass
plot.osmose.biomass = function(x, ts = TRUE, type = 1, species = NULL, 
                               speciesNames = NULL, start = NULL, end = NULL,  
                               initialYear = NULL, replicates = TRUE, 
                               freq = 12, horizontal = FALSE, conf = 0.95, 
                               factor = 1e-3, xlim = NULL, ylim = NULL, 
                               col = NULL, alpha = NULL, border = NULL, 
                               lty = 1, lwd = 1,  axes = TRUE, legend = TRUE, 
                               units = "tonnes", zero=FALSE, ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                legend = legend, units = units, zero=zero, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.abundance
plot.osmose.abundance = function(x, species = NULL, speciesNames = NULL, 
                                 start = NULL, end = NULL, 
                                 initialYear = NULL, ts = TRUE, type = 1, 
                                 replicates = TRUE, freq = 12, 
                                 horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                                 xlim = NULL, ylim = NULL, col = NULL, 
                                 alpha = NULL, border = NULL, lty = 1, lwd = 1, 
                                 axes = TRUE,  legend = TRUE, 
                                 units = "individuals", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                legend = legend, units = units, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.yield
plot.osmose.yield = function(x, species = NULL, speciesNames = NULL, 
                             start = NULL, end = NULL, 
                             initialYear = NULL, ts = TRUE, type = 1, 
                             replicates = TRUE, freq = 12, 
                             horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                             xlim = NULL, ylim = NULL, col = NULL, alpha = NULL, 
                             border = NULL, lty = 1, lwd = 1, axes = TRUE, 
                             legend = TRUE, units = "tonnes", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                legend = legend, units = units, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.yieldN
plot.osmose.yieldN = function(x, species = NULL, speciesNames = NULL, 
                              start = NULL, end = NULL, 
                              initialYear = NULL, ts = TRUE, type = 1, 
                              replicates = TRUE, freq = 12, 
                              horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                              xlim = NULL, ylim = NULL, col = NULL, alpha = NULL, 
                              border = NULL, lty = 1, lwd = 1, axes = TRUE, 
                              legend = TRUE, units = "individuals", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                legend = legend, units = units, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.biomassByTL
plot.osmose.biomassByTL = function(x, type = 1, species = NULL, 
                                   speciesNames = NULL, start = NULL, end = NULL,  
                                   freq = 12, horizontal = FALSE, 
                                   factor = 1e-3, xlim = NULL, ylim = NULL, 
                                   col = NULL, border = NULL, legend = TRUE, 
                                   lty = 1, lwd = 1,  axes = TRUE,
                                   units = list(x = "", y = "tonnes"), ...) {
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, ci = TRUE, by = "Trophic Level", ...)
  
}

#' @rdname plot.osmose
#' @method plot osmose.biomassBySize
plot.osmose.biomassBySize = function(x, type = 1, species = NULL, 
                                     speciesNames = NULL, start = NULL, end = NULL,  
                                     freq = 12, horizontal = FALSE, 
                                     factor = 1e-3, xlim = NULL, ylim = NULL, 
                                     col = NULL, border = NULL, legend = TRUE, 
                                     lty = 1, lwd = 1,  axes = TRUE,  
                                     units = list(x = "cm", y = "tonnes"), ...) {
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Size", ...)
  
  return(invisible())
  
}

#' @rdname plot.osmose
#' @method plot osmose.biomassByAge
plot.osmose.biomassByAge = function(x, type = 1, species = NULL, 
                                    speciesNames = NULL, start = NULL, end = NULL,  
                                    freq = 12, horizontal = FALSE, 
                                    factor = 1e-3, xlim = NULL, ylim = NULL, 
                                    col = NULL, border = NULL, legend = TRUE, 
                                    lty = 1, lwd = 1,  axes = TRUE,
                                    units = list(x = "years", y = "tonnes"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes,  
                units = units, by = "Age", ...)
  
  return(invisible())
  
}

#' @rdname plot.osmose
#' @method plot osmose.abundanceByTL
plot.osmose.abundanceByTL = function(x, type = 1, species = NULL, 
                                     speciesNames = NULL, start = NULL, end = NULL,  
                                     freq = 12, horizontal = FALSE, 
                                     factor = 1e-3, xlim = NULL, ylim = NULL, 
                                     col = NULL, border = NULL, legend = TRUE, 
                                     lty = 1, lwd = 1,  axes = TRUE,
                                     units = list(x = "", y = "individuals"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Trophic Level", ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.abundanceBySize
plot.osmose.abundanceBySize = function(x, type = 1, species = NULL, 
                                       speciesNames = NULL, start = NULL, end = NULL,  
                                       freq = 12, horizontal = FALSE, 
                                       factor = 1e-3, xlim = NULL, ylim = NULL, 
                                       col = NULL, border = NULL, legend = TRUE, 
                                       lty = 1, lwd = 1,  axes = TRUE,
                                       units = list(x = "cm", y = "individuals"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Size", ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.abundanceByAge
plot.osmose.abundanceByAge = function(x, type = 1, species = NULL, 
                                      speciesNames = NULL, start = NULL, end = NULL,  
                                      freq = 12, horizontal = FALSE, 
                                      factor = 1e-3, xlim = NULL, ylim = NULL, 
                                      col = NULL, border = NULL, legend = TRUE, 
                                      lty = 1, lwd = 1,  axes = TRUE,
                                      units = list(x = "years", y = "individuals"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Age", ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.yieldNBySize
plot.osmose.yieldNBySize = function(x, type = 1, species = NULL, 
                                    speciesNames = NULL, start = NULL, end = NULL,  
                                    freq = 12, horizontal = FALSE, 
                                    factor = 1e-3, xlim = NULL, ylim = NULL, 
                                    col = NULL, border = NULL, legend = TRUE, 
                                    lty = 1, lwd = 1,  axes = TRUE,
                                    units = list(x = "cm", y = "individuals"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Size", ...)
  
  return(invisible())
}


#' @rdname plot.osmose
#' @method plot osmose.yieldNByAge
plot.osmose.yieldNByAge = function(x, type = 1, species = NULL, 
                                   speciesNames = NULL, start = NULL, end = NULL,  
                                   freq = 12, horizontal = FALSE, 
                                   factor = 1e-3, xlim = NULL, ylim = NULL, 
                                   col = NULL, border = NULL, legend = TRUE, 
                                   lty = 1, lwd = 1,  axes = TRUE,
                                   units = list(x = "years", y = "individuals"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Age", ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.yieldBySize
plot.osmose.yieldBySize = function(x, type = 1, species = NULL, 
                                   speciesNames = NULL, start = NULL, end = NULL,  
                                   freq = 12, horizontal = FALSE, 
                                   factor = 1e-3, xlim = NULL, ylim = NULL, 
                                   col = NULL, border = NULL, legend = TRUE, 
                                   lty = 1, lwd = 1,  axes = TRUE,
                                   units = list(x = "cm", y = "tonnes"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Size", ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @method plot osmose.yieldByAge
plot.osmose.yieldByAge = function(x, type = 1, species = NULL, 
                                  speciesNames = NULL, start = NULL, end = NULL,  
                                  freq = 12, horizontal = FALSE, 
                                  factor = 1e-3, xlim = NULL, ylim = NULL, 
                                  col = NULL, border = NULL, legend = TRUE, 
                                  lty = 1, lwd = 1,  axes = TRUE,
                                  units = list(x = "years", y = "tonnes"), ...){
  
  osmosePlots3D(x = x, type = type, species = species, speciesNames = speciesNames, 
                start = start, end = end, freq = freq, horizontal = horizontal, 
                factor = factor, legend = legend,
                xlim = xlim, ylim = ylim, col = col, 
                border = border, lty = lty, lwd = lwd, axes = axes, 
                units = units, by = "Age", ...)
  
  return(invisible())
}
