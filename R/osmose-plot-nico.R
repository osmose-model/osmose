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
#' Osmose user manual \link{COMPLETE HERE}.
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
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
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
#' Osmose user manual \link{COMPLETE HERE}.
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
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
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
#' Osmose user manual \link{COMPLETE HERE}.
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
#' @param col A vector with the color names for the plots. By default \code{col = NULL} and the colors
#'  are chosen by the function. If the length of this parameter is shorter than the number of species, 
#'  the colors are recycled and repeated for the species that have not a specific color. 
#' @param alpha A number between 0 and 1. Indicate the transparency with which the 
#'  confidence interval is colored. By default \code{alpha = 0.5}.
#' @param speciesNames A vector with the names for each species. By default \code{speciesNames = NULL} and the 
#' names inside the object \code{x} are used.
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



#' Plots yield biomass by age class
#'
#' @param data Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.yieldByAge
plot.osmose.yieldByAge = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Age", ylab="Yield Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plots yield abundance by age class
#'
#' @param data Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.yieldNByAge
plot.osmose.yieldNByAge = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Age", ylab="Yield Abundance", ...)
  }
  
  return(invisible())
  
}

#' Plots yield biomass by size class
#'
#' @param data Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean biomass is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.yieldBySize
plot.osmose.yieldBySize = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Yield Biomass", ...)
  }
  
  return(invisible())
  
}

#' Plots yield abundance by size class
#'
#' @param data Biomass distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.yieldNBySize
plot.osmose.yieldNBySize = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Yield Abundance", ...)
  }
  
  return(invisible())
  
}

#' Plots mean trophic level by size class
#'
#' @param data TL distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean TL is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.meanTLBySize
plot.osmose.meanTLBySize = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Trophic level", ...)
  }
  
  return(invisible())
  
}

#' Plots mean trophic level by age class
#'
#' @param data Trophic level distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean TL is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.meanTLByAge
plot.osmose.meanTLByAge = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Age", ylab="Trophic level", ...)
  }
  
  return(invisible())
  
}


#' Plots abundance by size class
#'
#' @param data Abundance distribution by size
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.abundanceDistribBySize
plot.osmose.abundanceDistribBySize = function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}


#' Plots abundance by age class
#'
#' @param data Abundance distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.abundanceDistribByAge
plot.osmose.abundanceDistribByAge= function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Size (cm)", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}

#' Plots abundance by trophic level
#'
#' @param data Abundance distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.abundanceDistribByTL
plot.osmose.abundanceDistribByTL= function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Trophic level", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}


#' Plots abundance by trophic level
#'
#' @param data Abundance distribution by age
#' @param species Species name
#' @param time.mean If true, time.mean abundance is plotted
#' @param ... 
#'
#' @export
#' @method plot osmose.abundanceDistribByTL
plot.osmose.abundanceDistribByTL= function(data, species=NULL, time.mean=FALSE, ...) {
  
  if(is.null(species)) {
    species = names(data)
  }
  
  for(spec in species) {
    plot.osmose.output.ts.generic(data, species=spec, time.mean=time.mean, legtitle="Trophic level", ylab="Abundance", ...)
  }
  
  return(invisible())
  
}


#' Title
#'
#' @param data 
#'
#'
#' @method plot osmose.dietMatrixbySize
#' @export
plot.osmose.dietMatrixbySize = function(data, species=NULL, ...) {
  
  if(is.null(species)) { 
      species = names(species)
  }
  
  for (spec in species) {
    .plot.osmose.dietMatrixbySize(data, spec, "Size (cm)", "Diet Matrix", ...)
  }
  
}
 
.plot.osmose.dietMatrixbySize = function(data, spec, xlab, ylab, ...) {
    
  # recover the diet Matrix for the given species
  # data has one list element per prey
  data = data[[spec]]
  
  # computes the time-average for each prey
  data = lapply(data, apply, mean, MARGIN=2, na.rm=TRUE)
  # converts list into array ready for barplot
  data = as.data.frame(data)
  data[is.na(data)] = 0
  data = t(data.matrix(data))
  
  col = rainbow(nrow(data))
  
  barplot(data, legend=rownames(data), col=col, xlab=xlab, ylab=ylab, ...)
}
  
  
  
