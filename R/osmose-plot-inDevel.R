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



# osmose-plot (IN DEVELOP) ------------------------------------------------


#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @export
#' @method plot osmose.predatorPressureBySize
plot.osmose.predatorPressureBySize = function(x, species = NULL, speciesNames = NULL, 
                                              type = 1, ...) {
  
  args = list(...)
  if(is.null(args$draw_legend)) args$draw_legend = TRUE
  if(is.null(args$norm)) args$norm = TRUE
  if(is.null(args$parargs)) args$parargs = list()
  if(is.null(args$plotargs)) args$plotargs = list()
  if(is.null(args$legargs)) args$legargs = list()
  if(is.null(args$axisargs)) args$axisargs = list()
  
  plot.diet(x, species = species, speciesNames = speciesNames, type = type, 
            norm = args$norm, parargs = args$parargs, plotargs = args$plotargs, 
            legargs = args$legargs, axisargs = args$axisargs, 
            draw_legend = args$draw_legend) 
  
}

#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @export
#' @method plot osmose.predatorPressureByAge
plot.osmose.predatorPressureByAge = function(x, species = NULL, 
                                             speciesNames = NULL, type = 1, ...){
  
  args = list(...)
  if(is.null(args$draw_legend)) args$draw_legend = TRUE
  if(is.null(args$norm)) args$norm = TRUE
  if(is.null(args$parargs)) args$parargs = list()
  if(is.null(args$plotargs)) args$plotargs = list()
  if(is.null(args$legargs)) args$legargs = list()
  if(is.null(args$axisargs)) args$axisargs = list()
  
  plot.diet(x, species = species, speciesNames = speciesNames, type = type, 
            norm = args$norm, parargs = args$parargs, plotargs = args$plotargs, 
            legargs = args$legargs, axisargs = args$axisargs, 
            draw_legend = args$draw_legend) 
  
}


#' Plots predator pressure.
#'
#' @param x Predator pressure.
#' @param time.mean If TRUE, plots the time-average predator pressure 
#' sorted in decreasing order 
#' @param species Species names
#' @param ...  Additional plot arguments
#' @method plot osmose.predatorPressure
#' @export
#' @details Thres > 0 does'nt work, since it returns negative values. To check what is going on.
plot.osmose.predatorPressure = function(x, species = NULL, norm = TRUE, 
                                        speciesNames = NULL, type = 1, thres = 0, 
                                        parargs = list(), plotargs = list(), 
                                        legargs = list(), axisargs = list(), 
                                        draw_legend = TRUE, ...) {
  
  # extract the values for a given list of species
  x = .extract_species_from_list(x, species)
  
  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
    stop("'speciesNames' has an incorrect length.")
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  if (type == 1) { 
    thres = 0
  }
  
  # computes the time and replicate mean for all the mortalities type class (eggs, juveniles, ...)
  outlist = lapply(x, process.dietMatrix, time.mean=TRUE, species=NULL, thres=thres)
  
  # draws plots 
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plotDietType1(outlist, speciesNames = speciesNames, 
                             parargs = parargs, plotargs = plotargs, 
                             legargs = legargs, axisargs = axisargs, norm = norm, 
                             ...),
         "2" = plotDietType2(outlist, speciesNames = speciesNames, 
                             parargs = parargs, plotargs = plotargs, 
                             legargs = legargs, axisargs = axisargs, 
                             draw_legend = draw_legend, ...),
         stop(msg))
  
  return(invisible())
  
}

#' Plots mortality rates by size class.
#'
#' @param x Mortality object
#' @param species Species name
#' @param time.mean If time-average should be plotted. If TRUE, mortality rates by
#' for each age class and for each mortality types are plotted. Else, the time series for
#' a specific type of mortality is plotted. 
#' @param norm Wheter mortality should be normalized (only if time.mean=TRUE)
#' @param mtype Mortality type ("Mtot", Mpred", "Mstar", "Mnat", "F" or "Z")
#' @param ... Additional arguments of the function.
#' @method plot osmose.mortalityRateBySize
#' @export
plot.osmose.mortalityRateBySize = function(x, species = NULL, speciesNames = NULL, 
                                           norm = TRUE, type = 1, parargs = list(), 
                                           plotargs = list(), legargs = list(), 
                                           axisargs = list(), draw_legend = TRUE, 
                                           ...) {
  
  plot.mortalityRate(x, species = species, norm = norm, type = type, 
                     speciesNames = speciesNames, parargs = parargs, 
                     plotargs = plotargs, legargs = legargs, axisargs = axisargs, 
                     draw_legend = draw_legend, ...)
}

#' Plots mortality rates by age class.
#'
#' @param x Mortality object
#' @param species Species name
#' @param time.mean If time-average should be plotted. If TRUE, mortality rates by
#' for each age class and for each mortality types are plotted. Else, the time series for
#' a specific type of mortality is plotted. 
#' @param norm Wheter mortality should be normalized (only if time.mean=TRUE)
#' @param mtype Mortality type ("Mtot", Mpred", "Mstar", "Mnat", "F" or "Z")
#' @param ... Additional arguments of the function.
#' @method plot osmose.mortalityRateByAge
#' @export
plot.osmose.mortalityRateByAge = function(x, species = NULL, speciesNames = NULL, 
                                          norm = TRUE, type = 1, parargs = list(), 
                                          plotargs = list(), legargs = list(), 
                                          axisargs = list(), draw_legend = TRUE, ...) {
  
  plot.mortalityRate(x, species = species, norm = norm, type = type, 
                     speciesNames = speciesNames, plotargs = plotargs, 
                     legargs = legargs, axisargs = axisargs, 
                     draw_legend = draw_legend, ...)
  
  return(invisible())
  
}

#' Plot mortality rates
#'
#' @param x Object of class osmose.mortalityRate
#' @param time.mean True if time-average plot should be ok 
#' @param species List of species to plot. If NULL, all species are drawn.
#' @param norm True if percentage is returned instead of raw mort. rates
#' @param ... Additional parameters to the barplot/lines functions
#'
plot.osmose.mortalityRate = function(x, time.mean=TRUE, species=NULL, norm=TRUE, ...) {
  
  #  if species is not null, plot figure for each species
  if(is.null(species)) { 
    species = names(x)
  }
  
  for(spec in species) { 
    .plot_osmose_mortalityRate(x, time.mean = time.mean, species = spec, norm = norm, ...)
  }
}

#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @method plot osmose.dietMatrixbySize
#' @export 
plot.osmose.dietMatrixbySize = function(x, species = NULL, speciesNames = NULL, 
                                        norm = TRUE, type = 1, parargs = list(), 
                                        plotargs = list(), legargs = list(), 
                                        axisargs = list(), draw_legend = TRUE, ...) {
  
  plot.diet(x, species = species, speciesNames = speciesNames, norm = norm, 
            type = type, parargs = parargs, plotargs = plotargs, legargs = legargs, 
            axisargs = axisargs, draw_legend = draw_legend, ...) 
  
}

#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @method plot osmose.dietMatrixbyAge
#' @export 
plot.osmose.dietMatrixbyAge = function(x, species = NULL, speciesNames = NULL, 
                                       norm = TRUE, type = 1, parargs = list(), 
                                       plotargs = list(), legargs = list(), 
                                       axisargs = list(), draw_legend = TRUE, 
                                       ...) {
  
  plot.diet(x, species = species, speciesNames = speciesNames, norm = norm, 
            type = type, parargs = parargs, plotargs = plotargs, legargs = legargs, 
            axisargs = axisargs, draw_legend = draw_legend, ...) 
  
}


#' Plot the dietMatrix output
#'
#' @param x osmose.dietMatrix output
#' @param time.mean True if time averaged should be drawn
#' @param species  Species name (string or list). If NULL, al species are drawn.
#' @param thres Threshold (in percentage) below which preys are concatenated.
#' @param add_text True if values sjopu
#' @param color Histogram colors (if NULL, gray). Only used if time.mean=T
#' @param ... Additional arguments to the barplot or line function
#'
plot.osmose.dietMatrix = function(x, time.mean=TRUE, species=NULL, 
                                  thres=1, add_text=TRUE, 
                                  color=NULL, ...) {
  
  #  if species is not null, plot figure for each species
  if(is.null(species)) { 
    species = names(x)
  }
  
  for (spec in species) { 
    .plot_osmose_dietMatrix(x, time.mean=time.mean, species=spec, plot_name='DietMatrix (%)', 
                            thres=thres, add_text=add_text, color=color, ...)
  }
}


plot.osmose.biomassPredPreyIni = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
                                          ts = TRUE, type = 1, replicates = TRUE, nrep = 3,
                                          ci = TRUE, freq = 12, horizontal = FALSE, 
                                          conf = 0.95, factor = 1e-3, xlim = NULL, ylim = NULL,
                                          col = NULL, alpha = 0.5, speciesNames = NULL, axes = TRUE, ...) {
  
  osmosePlots2D(x = x, species = species, start = start, end = end, initialYear = initialYear,
                ts = ts, type = type, replicates = replicates, nrep = nrep, ci = ci,
                freq = freq, horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, speciesNames = speciesNames,
                axes = axes, ...)
  
  return(invisible())
}


