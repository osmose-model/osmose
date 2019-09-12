#' Plot the dietMatrix output
#'
#' @param x osmose.dietMatrix output
#' @param time.mean True if time averaged should be drawn
#' @param species  Species name (string or list). If NULL, al species are drawn.
#' @param thres Threshold (in percentage) below which preys are concatenated.
#' @param label_size Size of the xlabel of histogram (time.mean=T)
#' @param add_text True if values sjopu
#' @param color Histogram colors (if NULL, gray). Only used if time.mean=T
#' @param legsize Size of the legend (if time.mean=F)
#' @param ... Additional arguments to the barplot or line function
#'
#' @export
#'
plot.osmose.dietMatrix = function(x, time.mean=FALSE, species=NULL, 
                                  thres=1, label_size=1, add_text=TRUE, 
                                  color=NULL, legsize=0.5, ...) {
  
  #  if species is not null, plot figure for each species
  if(is.null(species)) { 
    species = names(x)
  }
  
  for (spec in species) { 
    par(oma=c(5, 5, 5, 5))
    .plot_osmose_dietMatrix(x, time.mean=time.mean, species=spec, plot_name='DietMatrix (%)s', 
                            thres=thres, label_size=label_size, add_text=add_text, color=color, legsize=legsize, ...)
  }
}



# Plots diet matrix
#
# @param x Diet matrix
# @param time.mean If TRUE, the time mean diet matrix is computed is displayed.
# @param species Species name
# @param colors GGplot2 colors (see for instance scale_fill_manual)
# @param thres Thresholds (in percentage). Time-average predation rates below this threashold 
# are binned together ("other" column).
# @param ... 
#
# @return None
# @method plot osmose.output.dietMatrix
.plot_osmose_dietMatrix = function(x, time.mean, species, thres, label_size, color, legsize, add_text, plot_name, ...) {
  
  x = process.dietMatrix(x, species=species, time.mean=time.mean, thres=thres)
  
  if(time.mean) {
    # If using a time-averaged diet matrix, then 
    # a barplot is drawn.
    temp = as.vector(x)
    names(temp) = names(x)
    osmose.barplot(temp, xlab="", ylab=plot_name, main=species, 
                   label_size=label_size, add_text=add_text, color=color, ...)
    return(invisible())
  } 
  
  # format the diet matrix to have stacked format for ggplot2
  data = .osmose.format_data_stacked(x)
  xlim = c(min(data$time, na.rm=TRUE), max(data$time, na.rm=TRUE) * (1 + 0.5))
  ylim = c(min(data$value, na.rm=TRUE), max(data$value, na.rm=TRUE))
  
  ncolors = length(levels(data$specie))
  cl = rainbow(ncolors)
  
  plot(0, 0, xlim=xlim, ylim=ylim, type='n', main=species, xlab='Time', ylab=plot_name, 
       cex.axis=0.5, cex.main=0.5, cex.lab=0.5)
  
  cpt = 1
  for (prey in levels(data$specie)) {
    temp = data[data$specie == prey, ]
    lines(temp[['time']], temp[['value']], type='l', col=cl[cpt], ...)
    cpt = cpt + 1
  }
  
  legend("topright", legend=levels(data$specie), col=cl, cex=legsize, lty=1)
  
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
plot.osmose.mortalityRate = function(x, time.mean=FALSE, species=NULL, norm=TRUE, ...) {
  
  #  if species is not null, plot figure for each species
  if(is.null(species)) { 
    species = names(x)
  }
  
  for(spec in species) { 
    par(oma=rep(5, 4), mar=rep(2, 4))
    .plot_osmose_mortalityRate(x, time.mean=time.mean, species=spec, norm=norm, ...)
  }
}


#' Plot mortality rates for a given species.
#'
#' @param data 
#' @param species 
#' @param time.mean 
#' @param norm 
#' @param ... 
.plot_osmose_mortalityRate = function(data, species=NULL, time.mean=TRUE, norm=TRUE, ...) {
  
  data = process.mortalityRate(data, species=species, time.mean=time.mean, ...)
  
  message = "You must provide a life stade among 'eggs', 'juveniles' or 'adults'"
  
  if(time.mean==FALSE)  {
    
    par(mfrow=c(3, 1))
    for (stade in names(data)) {
      datatmp = data[[stade]]
      
      if(norm) { 
        datatmp = apply(datatmp, 1, norm_func)
        datatmp = t(datatmp)
      }
      
      datatmp = osmose:::.osmose.format_data_stacked(datatmp)
      xlim = c(min(datatmp$time, na.rm=TRUE), max(datatmp$time, na.rm=TRUE) * (1 + 0.5))
      ylim = c(min(datatmp$value, na.rm=TRUE), max(datatmp$value, na.rm=TRUE))
      
      ncolors = length(levels(datatmp$specie))
      cl = rainbow(ncolors)
      print(ncolors)
      plot(0, 0, xlim=xlim, ylim=ylim, type='n', main=paste0(species, ", ", stade),  
           xlab='Time', ylab='Predation rate', 
           cex.axis=0.5, cex.main=0.5, cex.lab=0.5)
      
      cpt = 1
      for (prey in levels(datatmp$specie)) {
        temp = datatmp[datatmp$specie == prey, ]
        lines(temp[['time']], temp[['value']], type='l', col=cl[cpt], ...)
        cpt = cpt + 1
      }
      
      legend("topright", legend=levels(datatmp$specie), col=cl, lty=1, cex=0.5)
      
    }
    
    return(invisible())
    
  }
  
  # if normalize, display mortality rates into percentage instead of absolutes.
  if(norm) {
    # apply the normalize function to all the elements of the list.
    data = lapply(data, norm_func)
  }
  
  # convert the list into a matrix
  data = do.call(cbind.data.frame, data)
  data = as.matrix(data)
  
  xlabel = "Stage"
  if(norm) {
    ylabel = "Mortality rate (%)"
  } else {
    ylabel = "Mortality rate"
  }
  
  osmose.stackedpcent(data, xlab=xlabel, main=species, ylab=ylabel, ...)
  return(invisible())
  
}

#' Plots biomass by size class
#'
#' @param data Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param lwd Line width
#' @param ... 
#'
#' @export
#' @method plot osmose.biomassDistribBySize
plot.osmose.biomassDistribBySize = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plots biomass by age class
#'
#' @param data Biomass distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.biomassDistribByAge
plot.osmose.biomassDistribByAge = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Age", ylab="Biomass", ...)
  }
  
  return(invisible())
  
}



#' Plots the biomass distribution by trophic level
#'
#' @param data Biomass by trophic level data
#' @param species  Species name
#' @param time.mean If TRUE, the time-mean biomass is plotted for each TL class
#' @param lwd Line width
#' @param thres  Threshold below which data are not plotted. If NULL, all data are plotted. Only if time.mean=TRUE
#' @param colors GGplot2 colors
#' @param ... 
#'
#' @export
#' @method plot osmose.biomassDistribByTL
plot.osmose.biomassDistribByTL = function(data, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Trophic Level", ylab="Biomass", ...)
  }
  
  return(invisible())
  
}



#' Plots predator pressure.
#'
#' @param data Predator pressure.
#' @param time.mean If TRUE, plots the time-average predator pressure 
#' sorted in decreasing order 
#' @param species Species names
#' @param colors  GGplot2 colors
#' @param nmax Maximum number of values to draw. If NULL, all data are plotted.
#' @param ... 
#
#' @export
#' @method plot osmose.predatorPressure
plot.osmose.predatorPressure = function(data, species=NULL, time.mean=TRUE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Trophic Level", ylab="Biomass", ...)
  }
  
  return(invisible())
  
}



# Plots the prey biomass before the mortality rate is applied
#
# @param  Data array
# @param x osmose.output.object
# @param start First simulation year
# @param conf  Confidence interval
# @param factor Multiplication factor
# @param replicates Draws the time-series for each replicate
# @param freq Output frequency (used to convert from time-step into years)
# @param alpha Transparency for confidence interval
# @param col  Line color
# @param xlim  Xaxis limit
# @param ylim  Yaxis limit
# @param nrep  Maximum number of replicate to draw. If NULL, all the replicates
# are drawn. Only used if replicates=TRUE
# @param time.mean If FALSE, the mean biomass is drawn for each specie as a barplot.
# @param nmax Maximum number of species to draw (only used if time.mean=TRUE)
# @param species Name of the species to draw (only if time.mean=FALSE)
# @param ... 
#
# @return Nothing
# @export
# @method plot osmose.output.biomassPredPreyIni
# plot.osmose.output.biomassPredPreyIni = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
#                                       freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=NULL,
#                                       time.mean=FALSE, nmax=NULL, species=NULL, ...) {
#   
#   opar = par(no.readonly = TRUE)
#   on.exit(par(opar))
#   
#   if(isTRUE(time.mean)) {
#     .plotAverageBiomass(x, col=col, nmax=nmax, factor=factor, ylab="Biomass before mort.", title="Mean biomass before mortality", ...)
#     return(invisible())
#   }
#   
#   if(!species %in% colnames(x) | is.null(species))
#   {
#     stop("You should proper a species name")
#   }
#   
#   start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
#   
#   .plotBiomass(x=x, sp=species, start=start, conf=conf, factor=factor, 
#                replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
#                xlim=xlim, ylim=xlim) 
#   
#   title(xlab="Time (years)", ylab="Biomass before pred.", main=species)
#   
#   return(invisible())
# }

#' Plots mortality rates by size class.
#'
#' @param mort Mortality object
#' @param species Species name
#' @param time.mean If time-average should be plotted. If TRUE, mortality rates by
#' for each age class and for each mortality types are plotted. Else, the time series for
#' a specific type of mortality is plotted. 
#' @param norm Wheter mortality should be normalized (only if time.mean=TRUE)
#' @param mtype Mortality type ("Mtot", Mpred", "Mstar", "Mnat", "F" or "Z")
#' @param ... Additional arguments of the function.
#'
#' @return If time.mean=FALSE, returns a ggplot2 object
#' @export
#' @method plot osmose.mortalityRateDistribBySize
plot.osmose.mortalityRateDistribBySize = function(mort, species=NULL, time.mean=TRUE, norm=TRUE, mtype="Mtot", ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for(spec in species) { 
    mort = process.mortalityRate(mort, species=species, time.mean=time.mean, norm=norm)
    plot.mort.byclass.tmean(mort, species, norm, class="size")
  }
  
  return(invisible())
  
}

#' Plots mortality rates by age class.
#'
#' @param mort Mortality object
#' @param species Species name
#' @param time.mean If time-average should be plotted. If TRUE, mortality rates by
#' for each age class and for each mortality types are plotted. Else, the time series for
#' a specific type of mortality is plotted. 
#' @param norm Wheter mortality should be normalized (only if time.mean=TRUE)
#' @param mtype Mortality type ("Mtot", Mpred", "Mstar", "Mnat", "F" or "Z")
#' @param ... Additional arguments of the function.
#'
#' @return If time.mean=FALSE, returns a ggplot2 object
#' @export
#' @method plot osmose.mortalityRateDistribByAge
plot.osmose.mortalityRateDistribByAge = function(mort, species=NULL, time.mean=TRUE, norm=TRUE, mtype="Mtot", ...) {
  
  if(is.null(species)) { 
    species = names(x)
  }
  
  for(spec in species) {
    mort = process.mortalityRate(mort, species=species, time.mean=time.mean, norm=norm)
    plot.mort.byclass.tmean(mort, species, norm, class="Age")
  }
  
  return(invisible())
  
}

# Extract the mortality array providing the 
# mortality type.
# extract_mort = function(mort, mtype){
#   
#   if(mtype == "Mtot")
#   {
#     mort = mort[["Mpred"]] + mort[["Mstar"]] + mort[["Mnat"]] + mort[["F"]] + mort[["Z"]]
#   } else {
#     
#     if(!(mtype %in% names(mort)))
#     {
#       stop('Mortality type should be "Mtot", Mpred", "Mstar", "Mnat", "F" or "Z"')
#     }
#     
#     # extracts the mortality type
#     mort = mort[[mtype]]
#     
#   }
#   
#   return(mort)
# 
# }
# 
# normalize function. 
# returns percentage instead of raw values
norm_func = function(data) {
  output = 100 * data / (sum(data, na.rm=TRUE) + .Machine$double.xmin)
  dimnames(output) = dimnames(data)
  return(output)
}