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



#' Plot mortality rates
#'
#' @param x Object of class osmose.mortalityRate
#' @param time.mean True if time-average plot should be ok 
#' @param species List of species to plot. If NULL, all species are drawn.
#' @param norm True if percentage is returned instead of raw mort. rates
#' @param ... Additional parameters to the barplot/lines functions
#' @param type 1 if stacked barplot, 2 if pie chart
#' @method plot osmose.mortalityRate
#' @export 
plot.osmose.mortalityRate = function(x, species = NULL, speciesNames = NULL,  
                                     norm = TRUE, type = 1, draw_legend = TRUE,  
                                     plotargs = list(), parargs = list(), 
                                     legargs = list(), axisargs = list(), ... ) {
  
  # extract the values for a given list of species
  x = .extract_species_from_list(x, species)
  
  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
    stop("'speciesNames' has an incorrect length.")
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  # computes the time and replicate mean for all the mortalities type class (eggs, juveniles, ...)
  outlist = lapply(x, process.mortalityRate, time.mean=TRUE, species=NULL)
  outlist = lapply(outlist, as.data.frame)  # converts list of list into data.frame

  # draws plots
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plotMortRateType1(x = outlist, norm = norm, 
                                 speciesNames = speciesNames, parargs = parargs, 
                                 legargs = legargs, axisargs = axisargs, 
                                 plotargs = plotargs, ...),
         "2" = plotMortRateType2(outlist, speciesNames = speciesNames, 
                                 parargs = parargs, legargs = legargs, 
                                 axisargs = axisargs, plotargs = plotargs, 
                                 draw_legend = draw_legend, ...),
         stop(msg))
  
  return(invisible())
  
}

# Plots the mortality rate as a stacked plot
plotMortRateType1 = function(x, norm, speciesNames, parargs = list(), 
                             plotargs = list(), legargs = list(), 
                             axisargs = list(), ...) { 
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  do.call(par, parargs)
  
  i = 1
  
  # Loop over all the species
  for (data in x) {  

    if(norm) {
      data = apply(data, 2, norm_func)
    }

    plotargs$args.legend = legargs
    plotargs$main = speciesNames[i]

    .generic_staked_barplot(data, plotargs=plotargs, axisargs=axisargs)
    i = i + 1
    
  }
  
  return(invisible())
  
}

# Plots the mortality as pie charts. One plot per class (size, age or status)
plotMortRateType2 = function(x, speciesNames, draw_legend = TRUE, parargs = list(), 
                             plotargs = list(), legargs = list(), axisargs = list(), 
                             ...) { 
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  do.call(par, parargs)
  
  i = 1
  
  # loop over the species to extract 
  for (data in x) { 

    rnames = rownames(data)
    cnames = colnames(data)
    
    # loop over the class (eggs, juveniles, adults)
    for (class in colnames(data)) {
      
      temp = data[, class] # extracts the data for the given species
      names(temp) = rnames
      plotargs$x= temp
      plotargs$main = paste(speciesNames[i], class, sep=', ')
      
      .generic_pieplot(temp, draw_legend=draw_legend, plotargs=plotargs, legargs=legargs)
    
    }
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
plot.mortalityRate = function(x, species = NULL, speciesNames = NULL, norm = TRUE, 
                              type = 1, parargs = list(), plotargs = list(), 
                              legargs = list(), axisargs = list(),  
                              draw_legend = TRUE, ...) {
  
  # extract the values for a given list of species
  x = .extract_species_from_list(x, species)
  x = lapply(x, .process_mort_rate_dis)
  
  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
    stop("'speciesNames' has an incorrect length.")
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plotMortRateType1(x, norm = norm, speciesNames = speciesNames, 
                                 parargs = parargs, plotargs = plotargs, 
                                 legargs = legargs, axisargs = axisargs, ...),
         "2" = plotMortRateType2(x, speciesNames = speciesNames, parargs = parargs, 
                                 plotargs = plotargs, legargs = legargs, 
                                 axisargs = axisargs, draw_legend = draw_legend, 
                                 ...),
         stop(msg))
  
  return(invisible())

}


# Process the mortality rate distribution. Computes the mean over replicates
# Removes the sizes where the mortality contains only 0.
.process_mort_rate_dis = function(x) {
  
  x = lapply(x, apply, mean, MARGIN=2, na.rm=TRUE) # computes the mean over replicates and time
  mort = as.data.frame(x)
  mort = as.matrix(mort)
  
  # Counts the total mortality rate for each size class
  tot = apply(mort, 1, sum, na.rm=TRUE)

  # Extracts the size class with mortality > 0
  mort = mort[tot>0, ]
  tot = tot[tot>0]

  # Transpose the dataframe
  mort = as.data.frame(t(mort))
  
}

#' Plot mortality rates
#'
#' @param x Object of class osmose.mortalityRate
#' @param time.mean True if time-average plot should be ok 
#' @param species List of species to plot. If NULL, all species are drawn.
#' @param norm True if percentage is returned instead of raw mort. rates
#' @param ... Additional parameters to the barplot/lines functions
#' @method plot osmose.dietMatrix
#' @export 
plot.osmose.dietMatrix = function(x, species = NULL, speciesNames = NULL, type = 1, 
                                  thres = 1, parargs = list(), plotargs = list(), 
                                  legargs = list(), axisargs = list(), 
                                  draw_legend = TRUE, norm = TRUE, ...) {
  
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
         "1" = plotDietType1(outlist = outlist, speciesNames = speciesNames, 
                             parargs = parargs, plotargs = plotargs, 
                             legargs = legargs, axisargs = axisargs, norm = norm, 
                             ...),
         "2" = plotDietType2(outlist = outlist, speciesNames = speciesNames, 
                             parargs = parargs, plotargs = plotargs, 
                             legargs = legargs, axisargs = axisargs, 
                             draw_legend = draw_legend, ...),
         stop(msg))
  
  return(invisible())
  
}

# Plots diet matrix as a stacked plot.
plotDietType1 = function(outlist, speciesNames, parargs, plotargs, legargs, 
                         axisargs, norm, ... ) {
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  do.call(par, parargs)
  
  # Conversion of list into data frame
  cnames = names(outlist)
  outlist = as.matrix(as.data.frame(outlist))
  colnames(outlist) = cnames
  
  if(norm) {
    message('THe normalization is not implemented yet')
    
  }
  
  plotargs$args.legend = legargs
  
  .generic_staked_barplot(outlist, plotargs=plotargs, axisargs=axisargs)
  
  return(invisible())
  
}

plotDietType2 = function(outlist, speciesNames, parargs, plotargs, legargs, 
                         axisargs, draw_legend = TRUE, ...) {
  
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  do.call(par, parargs)
  
  i = 1  # species counter
  
  # loop over the species to extract 
  for (temp in outlist) { 
    print('####################')
    print(temp)
    
    plotargs$x= temp
    plotargs$main = paste(speciesNames[i])
    
    .generic_pieplot(temp, draw_legend = draw_legend, plotargs = plotargs, 
                     legargs = legargs)
    
    i = i + 1
    
  }
  
  return(invisible())
  
}

plot.diet = function(x, species = NULL, speciesNames = NULL, norm = TRUE, type = 1, 
                     parargs = list(), plotargs = list(), legargs = list(), 
                     axisargs = list(),  draw_legend = TRUE, ...) {
  
  # extract the values for a given list of species
  x = .extract_species_from_list(x, species)
  
  .process_dietDis = function(x) { 
    # computes the mean for all the prey
    lapply(x, apply, 2, mean, na.rm=TRUE)  
  }
  
  # computes the mean for all the preys
  x = lapply(x, .process_dietDis)
  
  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
    stop("'speciesNames' has an incorrect length.")
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  msg = sprintf("3D plot type %d is not implemented yet.", type)
  switch(type,
         "1" = plotDietDisType1(x = x, speciesNames = speciesNames, 
                                parargs = parargs, plotargs = plotargs, 
                                legargs = legargs, axisargs = axisargs, 
                                norm = norm, ...),
         "2" = plotDietDisType2(x, speciesNames = speciesNames, parargs = parargs, 
                                plotargs = plotargs, legargs = legargs, 
                                axisargs = axisargs, draw_legend = draw_legend, ...),
         stop(msg))
  
  return(invisible())
  
}

# Plot diet distribution as a stacked plot.
plotDietDisType1 = function(x, speciesNames = speciesNames, parargs = parargs, 
                            plotargs = plotargs, legargs = legargs, 
                            axisargs = axisargs, norm = norm, ...) { 
  
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  do.call(par, parargs)
  
  i = 1
  
  # Loop over all the species
  for (data in x) {  
    
    data = as.data.frame(data)  # class x prey
    keep = apply(data, 1, sum, na.rm=TRUE) # remove class with only NAN values
    data = data[keep > 0, ]

    if(norm) {
      data = apply(data, 1, norm_func)
    }

    plotargs$args.legend = legargs
    plotargs$main = speciesNames[i]
    
    .generic_staked_barplot(data, plotargs = plotargs, axisargs = axisargs)
    i = i + 1
    
  }
  
  return(invisible())
  
}

# Plot diet distribution as a stacked plot.
plotDietDisType2 = function(x, speciesNames = speciesNames, parargs = parargs, 
                            plotargs = plotargs, legargs = legargs, 
                            axisargs = axisargs, draw_legend = TRUE, ...) { 
  
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  do.call(par, parargs)
  
  i = 1
  
  # Loop over all the species
  for (data in x) {  
    
    data = as.matrix(as.data.frame(data))  # class x prey

    keep = apply(data, 2, sum, na.rm=TRUE)  # prey 
    data = data[, keep>0]  # remove prey columns with only 0s
    
    keep = apply(data, 1, sum, na.rm=TRUE)  # class
    data = data[keep>0, ]  # remove class rows with only 0s
    
    data = t(data) 

    # loop over the class (eggs, juveniles, adults)
    for (class in colnames(data)) {
      temp = data[, class] # extracts the data for the given species
      plotargs$x= temp
      plotargs$main = paste(speciesNames[i], class, sep=', ')
      
      .generic_pieplot(temp, draw_legend=draw_legend, plotargs=plotargs, legargs=legargs)
      
    } 
    
    i = i + 1
    
  }
  
  return(invisible())
  
}


# Generic stacked plot function
# outlist = data to plot. should have a names argument
# parargs = additionnal arguments to the par function
# plotargs = additionnal arguments to the pie function
.generic_staked_barplot = function(data, plotargs = list(), axisargs = list()) {
  
  # prepare the arguments for the pie arguments
  plotargs$height = data
  plotargs$xaxt ="n"
  
  xpos = do.call(barplot, plotargs)
  
  axisargs$side=1
  axisargs$at=xpos
  axisargs$labels=colnames(data)
  
  # call the axis method
  do.call(axis, axisargs)

  return(invisible())
  
}


# Generic pieplot function
# outlist = data to plot. should have a names argument
# draw_legend = True -> legend, else labels
# legargs = additionnal arguments to the legend function
# parargs = additionnal arguments to the par function
# plotargs = additionnal arguments to the pie function
.generic_pieplot = function(outlist, draw_legend = TRUE, legargs = list(), 
                            plotargs = list()) {
  
  plotargs$x = outlist
  if(draw_legend) {
    plotargs$labels = NA
    if(is.null(plotargs$col)) plotargs$col = rainbow(length(outlist))
    legargs$fill = plotargs$col
  } else {
    plotargs$labels = names(outlist)
  }
  
  do.call(pie, plotargs)
  
  if(draw_legend) {
    if(is.null(legargs$x)) legargs$x = 'topright'
    legargs$legend = names(outlist)
    do.call(legend, legargs)
  }
  
  return(invisible())
  
}
  