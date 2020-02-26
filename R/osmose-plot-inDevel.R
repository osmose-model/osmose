# osmose-plot (IN DEVELOP) ------------------------------------------------

#' Plots yield abundance by size class
#'
#' @param x Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... Extra arguments passed to the method.
#'
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
#' 
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
#' 
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
#' 
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
plot.osmose.meanTLByAge = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Age", ylab="Trophic level", ...)
  }
  
  return(invisible())
  
}


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

#' Plots the biomass distribution by trophic level
#'
#' @param x Biomass by trophic level data
#' @param species  Species name
#' @param time.mean If TRUE, the time-mean biomass is plotted for each TL class
#' @param ...  Additional plot arguments
#' 
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
plot.osmose.abundanceDistribByAge = function(x, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(x)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(x, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}