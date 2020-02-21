
# Plot methods ------------------------------------------------------------

#' @title Plot methods for osmose-like objects
#' 
#' @rdname plot.osmose
#' 
#' @description This function implements a plot method for different osmose 
#' classes.
#' 
#' @param x \code{osmose} like object. 
#' @param species A \code{numeric} vector specifying the species that will be 
#' showed. If \code{NULL} (default), all the available species time series will 
#' be plotted. 
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
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots 
#' are going to be time series where the x-axis is the time. For plots where the 
#' x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value, indicating the type of plot to be used 
#' (\code{type = 1}, by default). See Details.
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
#' default 0.5) given to confidence interval shadow (check 
#' \code{\link{adjustcolor}}).
#' @param lty The line type (check \code{\link{par}}).
#' @param lwd The line width (check \code{\link{par}}).
#' @param axes a \code{logical} value indicating whether both axes should be 
#' drawn on the plot. 
#' @param legend \code{logical} do you want to show a legend of species? (valid
#' only for those plot types TS-2)
#' @param ... Extra arguments of the function.
#' 
#' @details Describe types-ts
#' 
#' Default value for \code{freq} will be calculated from \code{x}: 
#' \code{freq = 1/x$model$start}.
#' 
#' Extra arguments can be passed from \code{plot.default}: \code{xlim}, 
#' \code{ylim}, \code{col}, \code{cex}, \code{cex.axis}, etc.
#' 
#' @author Criscely Lujan Paredes
#' 
#' @export
plot.osmose.biomass = function(x, species = NULL, speciesNames = NULL, 
                               start = NULL, end = NULL, 
                               initialYear = NULL, ts = TRUE, type = 1, 
                               replicates = TRUE, nrep = 3, freq = 12, 
                               horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                               xlim = NULL, ylim = NULL, col = NULL, alpha = 0.5,
                               lty = 1, lwd = 1, axes = TRUE, legend = TRUE, ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend,
                units = "tonnes", ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @export
plot.osmose.abundance = function(x, species = NULL, speciesNames = NULL, 
                                 start = NULL, end = NULL, 
                                 initialYear = NULL, ts = TRUE, type = 1, 
                                 replicates = TRUE, nrep = 3, freq = 12, 
                                 horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                                 xlim = NULL, ylim = NULL, col = NULL, alpha = 0.5,
                                 lty = 1, lwd = 1, axes = TRUE, legend = TRUE, ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend,
                units = "individuals", ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @export
plot.osmose.yield = function(x, species = NULL, speciesNames = NULL, 
                             start = NULL, end = NULL, 
                             initialYear = NULL, ts = TRUE, type = 1, 
                             replicates = TRUE, nrep = 3, freq = 12, 
                             horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                             xlim = NULL, ylim = NULL, col = NULL, alpha = 0.5,
                             lty = 1, lwd = 1, axes = TRUE, legend = TRUE, 
                             units = "tonnes", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend, ...)
  
  return(invisible())
}

#' @rdname plot.osmose
#' @export
plot.osmose.yieldN = function(x, species = NULL, speciesNames = NULL, 
                              start = NULL, end = NULL, 
                              initialYear = NULL, ts = TRUE, type = 1, 
                              replicates = TRUE, nrep = 3, freq = 12, 
                              horizontal = FALSE, conf = 0.95, factor = 1e-3, 
                              xlim = NULL, ylim = NULL, col = NULL, alpha = 0.5,
                              lty = 1, lwd = 1, axes = TRUE, legend = TRUE, 
                              units = "individuals", ...){
  
  # Run the plot
  osmosePlots2D(x = x, species = species, speciesNames = speciesNames, 
                start = start, end = end, 
                initialYear = initialYear, ts = ts, type = type, 
                replicates = replicates, nrep = nrep, freq = freq, 
                horizontal = horizontal, conf = conf, factor = factor,
                xlim = xlim, ylim = ylim, col = col, alpha = alpha, 
                lty = lty, lwd = lwd, axes = axes, legend = legend, ...)
  
  return(invisible())
}


# osmose-plot-nico (prev) -------------------------------------------------

#' Plots yield abundance by size class
#'
#' @param x Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... Extra arguments passed to the method.
#'
#' @export
#' @method plot osmose.yieldNBySize
plot.osmose.yieldNBySize = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Yield Abundance", ...)
  }
  
  return(invisible())
}


#' Plots yield abundance by age class
#'
#' @param x Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ... Extra arguments passed to the method.
#'
#' @export
#' @method plot osmose.yieldNByAge
plot.osmose.yieldNByAge = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Age", ylab="Yield Abundance", ...)
  }
  
  return(invisible())
  
}

#' Plots yield biomass by size class
#'
#' @param x Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ... Extra arguments passed to the method.
#'
#' @export
#' @method plot osmose.yieldBySize
plot.osmose.yieldBySize = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Yield Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plots yield biomass by age class
#'
#' @param x Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ... Extra arguments passed to the method.
#'
#' @export
#' @method plot osmose.yieldByAge
plot.osmose.yieldByAge = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Age", ylab="Yield Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @method plot osmose.predatorPressureDistribBySize
#' @export
plot.osmose.predatorPressureDistribBySize = function(x, species=NULL, ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for (spec in species) {
    .plot.osmose.dietMatrixbyDis(x, spec, "Size (cm)", "Predator pressure", ...)
  }
  
}

#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @method plot osmose.predatorPressureDistribByAge
#' @export
plot.osmose.predatorPressureDistribByAge = function(x, species=NULL, ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for (spec in species) {
    .plot.osmose.dietMatrixbyDis(x, spec, "Age", "Predator pressure", ...)
  }
  
}

#' Plots predator pressure.
#'
#' @param x Predator pressure.
#' @param time.mean If TRUE, plots the time-average predator pressure 
#' sorted in decreasing order 
#' @param species Species names
#' @param ...  Additional plot arguments
#
#' @export
#' @method plot osmose.predatorPressure
plot.osmose.predatorPressure = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Trophic Level", ylab="Biomass", ...)
  }
  
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
#'
#' @return None
#' @export
#' @method plot osmose.mortalityRateDistribBySize
plot.osmose.mortalityRateDistribBySize = function(x, species=NULL, time.mean=TRUE, norm=TRUE, mtype="Mtot", ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for(spec in species) { 
    mort = process.mortalityRate(x, species=species, time.mean=time.mean, norm=norm)
    plot.mort.byclass.tmean(mort, species, norm, class="size")
  }
  
  return(invisible())
  
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
#'
#' @return Nothing
#' @export
#' @method plot osmose.mortalityRateDistribByAge
plot.osmose.mortalityRateDistribByAge = function(x, species=NULL, time.mean=TRUE, norm=TRUE, mtype="Mtot", ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for(spec in species) {
    mort = process.mortalityRate(x, species=species, time.mean=time.mean, norm=norm)
    plot.mort.byclass.tmean(mort, species, norm, class="Age")
  }
  
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
#' @export
plot.osmose.mortalityRate = function(x, time.mean=TRUE, species=NULL, norm=TRUE, ...) {
  
  #  if species is not null, plot figure for each species
  if(is.null(species)) { 
    species = names(x)
  }
  
  for(spec in species) { 
    .plot_osmose_mortalityRate(x, time.mean = time.mean, species = spec, norm = norm, ...)
  }
}

#' Plots mean trophic level by size class
#'
#' @param x TL distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean TL is plotted
#' @param ... Extra arguments passed to the method.
#'
#' @export
#' @method plot osmose.meanTLBySize
plot.osmose.meanTLBySize = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Trophic level", ...)
  }
  
  return(invisible())
  
}

#' Plots mean trophic level by age class
#'
#' @param x Trophic level distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean TL is plotted
#' @param ... Extra arguments passed to the method.
#'
#' @export
#' @method plot osmose.meanTLByAge
plot.osmose.meanTLByAge = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Age", ylab="Trophic level", ...)
  }
  
  return(invisible())
  
}

#' Plot function for biomass
#' @description This function takes a object of the class \code{osmose.biomass} and 
#' produce useful plots for the analysis of this variable.
#' @param x Object of the class \code{osmose.biomass}. This object have three dimensions: 
#' species, time and size or age.
#' @param species A \code{numeric} vector. The species indexation of the object \code{x} is specified 
#' with this parameter. By defaul \code{species = NULL} and there is not species indexation, using 
#' all the species of \code{x} for the plots.
#' @param start A \code{numeric} value. The first element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{start = NULL} and the time indexation start 
#' with the first element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{end = NULL} and the time indexation finish 
#' with the last element of \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element that is going to be used on 
#' the x axis for the plots.
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots are going to be time series 
#' where the x-axis is the time. For plots where the x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value. This parameter specifies the type of the plot to be performed. By 
#' default \code{type = 1}. To see all the plot types available for this variable check the 
#' Osmose user manual.
#' @param replicates \code{logical} parameter. Parameter used for plots types: \code{type = 1} and \code{type = 2}.
#' When \code{replicates = TRUE} the plot show the values obtained in each replicates of object (\code{x}). 
#' code{replicates = FALSE} show the mean value of the variable over the time.
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show on the plots. This parameter is 
#' used only when \code{replicates = TRUE} and for the plot types where \code{replicates} is used.
#' @param ci TODO. remove this parameter on type 1 and 2 (ts = TRUE).
#' @param freq A \code{numeric} value to indicate the frequence on the time series plots. 
#' It is \code{freq = 12} (monthly) by default. This parameter is going to be used to create 
#' a vector of the time for the x-axis.
#' @param horizontal \code{logical} parameter. For plots types that are not time series (barplots or boxplots), 
#' this parameter allows represent verticaly the plot (\code{horizontal = FALSE}). \code{horizontal = TRUE} 
#' present the plots horizontaly.
#' @param conf A number to indicate the confidence interval to plot. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. By default \code{factor = 1e-3}.
#' @param xlim,ylim numeric vectors of length 2, giving the x and y coordinates ranges.
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
#' @param axes a logical value indicating whether both axes should be drawn on the plot. Use graphical parameter "xaxt" or "yaxt" to suppress just one of the axes.
#' @param ... Extra arguments of the function.
#' @return A graph of a object of the class \code{osmose.biomass}.
#' @author Criscely Lujan Paredes
#' @method plot osmose.meanSizeCatch
#' @export
plot.osmose.meanSizeCatch = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
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

#' Plot function for biomass
#' @description This function takes a object of the class \code{osmose.biomass} and 
#' produce useful plots for the analysis of this variable.
#' @param x Object of the class \code{osmose.biomass}. This object have three dimensions: 
#' species, time and size or age.
#' @param species A \code{numeric} vector. The species indexation of the object \code{x} is specified 
#' with this parameter. By defaul \code{species = NULL} and there is not species indexation, using 
#' all the species of \code{x} for the plots.
#' @param start A \code{numeric} value. The first element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{start = NULL} and the time indexation start 
#' with the first element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{end = NULL} and the time indexation finish 
#' with the last element of \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element that is going to be used on 
#' the x axis for the plots.
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots are going to be time series 
#' where the x-axis is the time. For plots where the x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value. This parameter specifies the type of the plot to be performed. By 
#' default \code{type = 1}. To see all the plot types available for this variable check the 
#' Osmose user manual.
#' @param replicates \code{logical} parameter. Parameter used for plots types: \code{type = 1} and \code{type = 2}.
#' When \code{replicates = TRUE} the plot show the values obtained in each replicates of object (\code{x}). 
#' code{replicates = FALSE} show the mean value of the variable over the time.
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show on the plots. This parameter is 
#' used only when \code{replicates = TRUE} and for the plot types where \code{replicates} is used.
#' @param ci TODO. remove this parameter on type 1 and 2 (ts = TRUE).
#' @param freq A \code{numeric} value to indicate the frequence on the time series plots. 
#' It is \code{freq = 12} (monthly) by default. This parameter is going to be used to create 
#' a vector of the time for the x-axis.
#' @param horizontal \code{logical} parameter. For plots types that are not time series (barplots or boxplots), 
#' this parameter allows represent verticaly the plot (\code{horizontal = FALSE}). \code{horizontal = TRUE} 
#' present the plots horizontaly.
#' @param conf A number to indicate the confidence interval to plot. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. By default \code{factor = 1e-3}.
#' @param xlim,ylim numeric vectors of length 2, giving the x and y coordinates ranges.
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
#' @param axes a logical value indicating whether both axes should be drawn on the plot. Use graphical parameter "xaxt" or "yaxt" to suppress just one of the axes.
#' @param ... Extra arguments of the function.
#' @return A graph of a object of the class \code{osmose.biomass}.
#' @author Criscely Lujan Paredes
#' @method plot osmose.meanSize
#' @export
plot.osmose.meanSize = function(x, species = NULL, start = NULL, end = NULL, initialYear = NULL,
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

#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @method plot osmose.dietMatrixbySize
#' @export
plot.osmose.dietMatrixbySize = function(x, species=NULL, ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for (spec in species) {
    .plot.osmose.dietMatrixbyDis(x, spec, "Size (cm)", "Diet Matrix", ...)
  }
  
}

#' Plot diet matrix by size class
#'
#' @param x Data list
#' @param species List of species name. If null, all species are plotted
#' @param ... Extra arguments of the graphics barplot function
#' @method plot osmose.dietMatrixbyAge
#' @export
plot.osmose.dietMatrixbyAge = function(x, species=NULL, ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for (spec in species) {
    .plot.osmose.dietMatrixbyDis(x, spec, "Age", "Diet Matrix", ...)
  }
  
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
#' @export
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

#' Plot function for biomass
#' @description This function takes a object of the class \code{osmose.biomass} and 
#' produce useful plots for the analysis of this variable.
#' @param x Object of the class \code{osmose.biomass}. This object have three dimensions: 
#' species, time and size or age.
#' @param species A \code{numeric} vector. The species indexation of the object \code{x} is specified 
#' with this parameter. By defaul \code{species = NULL} and there is not species indexation, using 
#' all the species of \code{x} for the plots.
#' @param start A \code{numeric} value. The first element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{start = NULL} and the time indexation start 
#' with the first element of \code{x}.
#' @param end A \code{numeric} value. The last element of the time indexation of the object \code{x} is
#' specified with this parameter. By default \code{end = NULL} and the time indexation finish 
#' with the last element of \code{x}.
#' @param initialYear A \code{numeric} value. It specifies the first element that is going to be used on 
#' the x axis for the plots.
#' @param ts \code{logical} parameter. By default \code{ts = TRUE} and the plots are going to be time series 
#' where the x-axis is the time. For plots where the x-axis is not the time \code{ts = FALSE}.
#' @param type A \code{numeric} value. This parameter specifies the type of the plot to be performed. By 
#' default \code{type = 1}. To see all the plot types available for this variable check the 
#' Osmose user manual.
#' @param replicates \code{logical} parameter. Parameter used for plots types: \code{type = 1} and \code{type = 2}.
#' When \code{replicates = TRUE} the plot show the values obtained in each replicates of object (\code{x}). 
#' code{replicates = FALSE} show the mean value of the variable over the time.
#' @param nrep A \code{numeric} value. Indicate the number of replicates to show on the plots. This parameter is 
#' used only when \code{replicates = TRUE} and for the plot types where \code{replicates} is used.
#' @param ci TODO. remove this parameter on type 1 and 2 (ts = TRUE).
#' @param freq A \code{numeric} value to indicate the frequence on the time series plots. 
#' It is \code{freq = 12} (monthly) by default. This parameter is going to be used to create 
#' a vector of the time for the x-axis.
#' @param horizontal \code{logical} parameter. For plots types that are not time series (barplots or boxplots), 
#' this parameter allows represent verticaly the plot (\code{horizontal = FALSE}). \code{horizontal = TRUE} 
#' present the plots horizontaly.
#' @param conf A number to indicate the confidence interval to plot. By default \code{conf = 0.95}.
#' @param factor A number to indicate the scale of the variable on the y-axis. By default \code{factor = 1e-3}.
#' @param xlim,ylim numeric vectors of length 2, giving the x and y coordinates ranges.
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
#' @param axes a logical value indicating whether both axes should be drawn on the plot. Use graphical parameter "xaxt" or "yaxt" to suppress just one of the axes.
#' @param ... Extra arguments of the function.
#' @return A graph of a object of the class \code{osmose.biomass}.
#' @author Criscely Lujan Paredes
#' @method plot osmose.biomassPredPreyIni
#' @export
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

#' Plots the biomass distribution by trophic level
#'
#' @param x Biomass by trophic level data
#' @param species  Species name
#' @param time.mean If TRUE, the time-mean biomass is plotted for each TL class
#' @param ...  Additional plot arguments
#'
#' @export
#' @method plot osmose.biomassDistribByTL
plot.osmose.biomassDistribByTL = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Trophic Level", ylab="Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plots biomass by size class
#'
#' @param x Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ...  Additional plot arguments
#'
#' @export
#' @method plot osmose.biomassDistribBySize
plot.osmose.biomassDistribBySize = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plots biomass by age class
#'
#' @param x Biomass distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ...  Additional plot arguments
#'
#' @export
#' @method plot osmose.biomassDistribByAge
plot.osmose.biomassDistribByAge = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Age", ylab="Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plots abundance by trophic level
#'
#' @param x Abundance distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ...  Additional plot arguments
#'
#' @export
#' @method plot osmose.abundanceDistribByTL
plot.osmose.abundanceDistribByTL = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Trophic level", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}

#' Plots abundance by size class
#'
#' @param x Abundance distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... Additional plot arguments
#'
#' @export
#' @method plot osmose.abundanceDistribBySize
plot.osmose.abundanceDistribBySize = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}

#' Plots abundance by age class
#'
#' @param x Abundance distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ...  Additional plot arguments
#'
#' @export
#' @method plot osmose.abundanceDistribByAge
plot.osmose.abundanceDistribByAge = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}
