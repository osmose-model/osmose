#' Reformat the data into the ggplot2 stacked plot format.
#' It returns a dataframe with specie, time and predation rate
#' in the columns.
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
#' @param ... Additional arguments to the barplot function (color, etc.)
#'
#' @export
osmose.barplot = function(x, label_size=1, add_text=TRUE, color=NULL, ...) {
  
  # format the data so that at the end we have
  # a table of dim [N. 1]
  col = rep(color, length(x))
  x = as.table(x)
  names = rownames(x)
  N = length(names)
  dim(x) <- c(length(x), 1)

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
  print(args)
  
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
  
  # Create a list of 22 colors to use for the lines.
  cl <- rainbow(ncol(y))
  plotcol = 1:ncol(y)
  
  # Now fill plot with the log transformed coverage data from the
  # files one by one.
  for(i in 1:ncol(y)) {
    lines(y[,i], col=cl[i], ...)
    plotcol[i] <- cl[i]
  }
  
  legend("topright", legend=colnames(y), col = plotcol, lwd=0.7, cex=0.7, title=legtitle, ncol=nlegend)
}


plot.osmose.output.ts.generic = function(data, species=NULL, time.mean=TRUE, legtitle, ylab, ...) {
  
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
  osmose.barplot(temp, xlab=legtitle, ylab=ylab, main=species, add_text=FALSE, ...)
  return(invisible())
}


# Geric function to plot mortality time mean as a function of class (size or age)
plot.mort.byclass.tmean = function(mort, species, norm, class="size") {
  
  # converts into matrix
  mort = do.call(cbind.data.frame, mort)
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
  
  .check_species(data, species)    
  
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







