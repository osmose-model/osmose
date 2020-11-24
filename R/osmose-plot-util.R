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



#' Reformat the data into the ggplot2 stacked plot format.
#' It returns a dataframe with specie, time and predation rate
#' in the columns.
#'
#' @param data Data to plot
#' @param time Time vector
.osmose.format_data_stacked = function(data, time=NULL)
{
  
  ntime = dim(data)[1]
  npecie = dim(data)[2]
  if(is.null(time)) time = 1:ntime  # extracts the time array
  
  # Initialize the output array
  output = data.frame()
  
  # loop over all the specie and update the output array.
  for (spec in colnames(data)) {
    temp = data.frame(specie=rep(spec, ntime), time=time, value=data[, spec])
    output = rbind(output, temp)
  }
  
  # converts specie into a factor
  output$specie = factor(output$specie)
  
  return(output)
  
}

#' Plots a barplot, with xlabels rotated with a 45degree angle
#'
#' @param x Data array
#' @param add_text True if text should be added.
#' @param color Color of the barplot
#' @param ... Additional arguments to the barplot function (color, etc.)
.osmose.barplot = function(x, add_text = TRUE, color=NULL, ...) {
  
  # format the data so that at the end we have
  # a table of dim [N. 1]
  col = rep(color, length(x))
  x = as.table(x)
  names = rownames(x)
  N = length(names)
  dim(x) = c(length(x), 1)

  # recover the arguments of varplot 
  # and modify xlim and legend pos.
  args = list(...)
  args[["args.legend"]]['x'] = N * (1  + 0.5)
  xlim = c(0, N * (1 + 0.5))
  args[['xlim']] = xlim
  
  # updates the list of barplot arguments
  # to use with do.call
  args$height = x
  args$col = rainbow(N)
  args$legend = names
  args$beside = TRUE

  #barplot(x, col=col, legend = names, beside=TRUE, args) 
  do.call(barplot, args)
  
  if(add_text) { 
    ytext = max(x) * (1 - 0.1) 
    text(1:N + 0.5, ytext, round(x, digits=1), cex=0.5)
  }
  
}

# Plot a stacked percent plot.
#
# @param data 
# @param legPosX X position of legend (if NULL, no legend is plotted)
# @param legPosY Y position of legend
# @param ... Additional arguments to the barplot function.
# @export
osmose.stackedpcent = function(data, ...)
{
  
  # cheks that the data frame contains labeled rows and columns
  if(length(colnames(data)) == 0) {
    stop("You must provide column names to your dataframe")
  }
  
  if(length(rownames(data)) == 0) {
    stop("You must provide row names to your dataframe")
  }
  
  N = nrow(data)
  col = rainbow(N)
  
  barplot(data, space=1, col=col, ...)
  
  # to do: check that data has colnames and rownames#
  N = nrow(data)
  legend("right", "top", 
         legend = rownames(data), 
         ncol=1, xpd=TRUE, inset=c(0.2, 0.),
         fill=col, bg="white")
  
}

# extract the default ggplot2 color table for fill plots
# it takes as argument the discrete level array 
# .make_default_ggplot_col = function(levels)
# {
#   colors = scale_fill_manual(values=topo.colors(length(levels)))
#   return(colors)
# }


# Plots time-series plots.
.osmose.plot_ts = function(y, xlab, ylab, title, legtitle, ...) 
{
  
  nlegend = as.integer(ncol(y) / 5)
  time = 1:nrow(y)
  
  # First create an empty plot.
  plot(1, type = 'n', xlim = c(min(time), max(time)), ylim = c(min(y), max(y)),
       xlab=xlab, ylab=ylab, main=title)
  
  args = list(...)
  lwd = args[['lwd']]
  
  # Create a list of 22 colors to use for the lines.
  cl = rainbow(ncol(y))
  plotcol = 1:ncol(y)
  
  # Now fill plot with the log transformed coverage data from the
  # files one by one.
  for(i in 1:ncol(y)) {
    lines(y[,i], col=cl[i], ...)
    plotcol[i] = cl[i]
  }
  
  legend("topright", legend=colnames(y), col = plotcol, lwd=lwd, cex=0.7, title=legtitle, ncol=nlegend)
}


plot.osmose.output.ts.generic = function(data, species = NULL, time.mean = TRUE, 
                                         legtitle, ylab, ...) {
  
  .check_species(data, species)
  
  y = data[[species]]
  
  # computes the replicate mean 
  y = apply(y, c(1, 2), mean)
  
  if(time.mean == FALSE) {
    .osmose.plot_ts(y, xlab='Time', ylab=ylab, title=species, legtitle=legtitle, ...)
    return(invisible())
  }
  
  # Computes the time-mean
  y = apply(y, 2, mean)
  temp = as.vector(y)
  names(temp) = names(y)
  .osmose.barplot(temp, xlab=legtitle, ylab=ylab, main=species, add_text=FALSE, ...)
  return(invisible())
}


# Geric function to plot mortality time mean as a function of class (size or age)
plot.mort.byclass.tmean = function(x, species, norm, class="size") {
  
  # converts into matrix
  mort = do.call(cbind.data.frame, x)
  mort = as.matrix(mort)
  
  # Counts the total mortality rate for each size class
  tot = apply(mort, 1, sum)
  
  # Extracts the size class with mortality > 0
  mort = mort[tot>0, ]
  tot = tot[tot>0]
  
  # If data should be normalized, then it is divided by the total 
  # mortality
  if(norm) mort = (mort / tot) * 100
  
  # Transpose the dataframe
  mort = t(mort)
  
  if(norm) {
    ylab = "Mortality (%)"
  } else {
    ylab = "Mortality"
  }
  
  if(class=="size") {
    osmose.stackedpcent(mort, xlab="Size (cm)", main=species, ylab=ylab)
  } else {
    osmose.stackedpcent(mort, xlab="Age", main=species, ylab=ylab)
  }
  return(invisible())
}

.plot.osmose.dietMatrixbyDis = function(data, spec, xlab, ylab, ...) {
  
  .check_species(data, spec)    
  
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

# Plot mortality rates for a given species.
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
      
      cnames = colnames(datatmp)
      time = as.numeric(rownames(datatmp))
      
      xlim = c(min(time, na.rm=TRUE), max(time, na.rm=TRUE) * (1 + 0.5))
      ylim = c(min(datatmp, na.rm=TRUE), max(datatmp, na.rm=TRUE))
      
      ncolors = length(cnames)
      cl = rainbow(ncolors)
      plot(0, 0, xlim=xlim, ylim=ylim, type='n', main=paste0(species, ", ", stade),  
           xlab='Time', ylab='Predation rate', 
           cex.axis=0.5, cex.main=0.5, cex.lab=0.5)
      
      cpt = 1
      for (cpt in 1:ncolors) {
        temp = datatmp[, cpt]
        lines(time, temp, type='l', col=cl[cpt], ...)
        cpt = cpt + 1
      }
      
      legend("topright", legend=cnames, col=cl, lty=1, cex=0.5)
      
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

#' Plots diet matrix
#'
#' @param x Diet matrix
#' @param time.mean If TRUE, the time mean diet matrix is computed is displayed.
#' @param species Species name
#' @param thres Thresholds (in percentage). Time-average predation rates below this threashold 
#' are binned together ("other" column).
#' @param color Color of the barplot.
#' @param add_text \code{TRUE} if text should be added.
#' @param plot_name Label for ylab in diet matrix plot.
#' @param legsize Size of the legend (default 1), if time.mean is \code{FALSE}.
#' @param ... Additional plot arguments passed to barplot.
#'
#' @return None
.plot_osmose_dietMatrix = function(x, time.mean, species, thres, color, add_text, plot_name,
                                   legsize = 1, ...) {
  
  x = process.dietMatrix(x, species=species, time.mean=time.mean, thres=thres)
  
  if(time.mean) {
    # If using a time-averaged diet matrix, then 
    # a barplot is drawn.
    temp = as.vector(x)
    names(temp) = names(x)
    .osmose.barplot(temp, xlab="", ylab=plot_name, main=species, 
                    add_text = add_text, color=color, ...)
    return(invisible())
  } 
  
  
  # format the diet matrix to have stacked format for ggplot2
  time = rownames(x)
  time = as.numeric(time)
  cnames = colnames(x)
  xlim = c(min(time, na.rm=TRUE), max(time, na.rm=TRUE) * (1 + 0.5))
  ylim = c(min(x, na.rm=TRUE), max(x, na.rm=TRUE))
  
  ncolors = length(cnames)
  cl = rainbow(ncolors)
  
  x = apply(x, c(1, 2), mean, na.rm=TRUE)
  plot(0, 0, xlim=xlim, ylim=ylim, type='n', main=species, xlab='Time', ylab=plot_name, 
       cex.axis=0.5, cex.main=0.5, cex.lab=0.5)
  
  cpt = 1
  for (cpt in 1:ncolors) {
    temp = x[, cpt]
    lines(time, temp, type='l', col=cl[cpt])
    cpt = cpt + 1
  }
  
  legend("topright", legend=cnames, col=cl, cex=legsize, lty=1)
  
  return(invisible())
}
