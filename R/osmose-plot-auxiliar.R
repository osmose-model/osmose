
# PLOT 2D -----------------------------------------------------------------

osmosePlots2D = function(x, species, speciesNames, start, end, initialYear, ts, 
                         type, replicates, freq, horizontal, conf, factor, 
                         xlim, ylim, col, alpha, border, lty, lwd, axes, legend, 
                         units, ci = TRUE, ...){
  
  # CHECK ARGUMENTS
  if(!is.null(species)){
    # Check species I
    message1 = "'species' must be whether a numeric or character vector without NA or duplicated values."
    if(!is.vector(species) || # isn't it a vector?
       all(!is.element(c("character", "numeric"), mode(species))) || # is it character or numeric?
       length(species) < 1 || # its length is greater than 1?
       sum(is.na(species)) > 0 || # is there any NA?
       any(duplicated(species))){ # is there any duplicated value?
      stop(message1)
    }
    
    # Check species II
    if(is.numeric(species)){
      if(any(species > ncol(x))){
        stop("'species' must be between 1 and ", ncol(x))  
      }
    }else if(is.character(species)){
      if(is.null(dimnames(x))){
        stop("Is not possible to define species as character due to 'x' has not species names defined.")
      }
      
      if(any(!is.element(species, colnames(x)))){
        stop("Some values of 'species' does not exist.")
      }
      
      species = match(species, colnames(x)) - 1
    }
    
    x = x[ , species + 1, , drop = FALSE]
  }
  
  if(!is.null(speciesNames) && length(speciesNames) != ncol(x)){
    stop("'speciesNames' has an incorrect length.")
  }
  
  # Check start and end args
  if(is.null(start)) start = 1
  if(is.null(end)) end = dim(x)[1]
  
  if(start < 1 | start > end) stop("Incorrect value for 'start' argument")
  if(end > dim(x)[1] | end < start) stop("Incorrect value for 'end' argument")
  
  x = x[seq(start, end), , ,drop = FALSE]
  
  # xlim 
  if(is.null(initialYear)) initialYear = 0
  
  times = seq(from = initialYear + start/freq, by = 1/freq, 
              length.out = nrow(x))
  
  msg = sprintf("Not defined method for ts = %s and type = %s", isTRUE(ts), type)
  if(isTRUE(ts)){
    
    switch(type,
           "1" = plot2DTsType1(x = x, replicates = replicates, 
                               ci = ci, times = times, xlim = xlim, ylim = ylim,
                               conf = conf, factor = factor, col = col, 
                               alpha = alpha, speciesNames = speciesNames, 
                               lty = lty, lwd = lwd, axes = axes, units = units,
                               border = border, ...),
           "2" = plot2DTsType2(x = x, replicates = replicates, 
                               ci = ci, times = times, xlim = xlim, ylim = ylim,
                               conf = conf, factor = factor, col = col, 
                               alpha = alpha, speciesNames = speciesNames, 
                               lty = lty, lwd = lwd, axes = axes, 
                               legend = legend, units = units, border = border, 
                               ...),
           "3" = plot2DTsType3(x = x, times = times, xlim = xlim, ylim = ylim, 
                               factor = factor, col = col, alpha = alpha, 
                               legend = legend, speciesNames = speciesNames, 
                               axes = axes, units = units, border = border, ...),
           "4" = plot2DTsType4(x = x, times = times, xlim = xlim, ylim = ylim, 
                               factor = factor, lty = lty, lwd = lwd, col = col, 
                               alpha = alpha, legend = legend, 
                               speciesNames = speciesNames, axes = axes, 
                               units = units, ...),
           stop(msg))
  }else{
    
    switch(type,
           "1" = plot2DType1(x, ci = ci, horizontal = horizontal, col = col,
                             factor = factor, speciesNames = speciesNames, 
                             xlim = xlim, ylim = ylim, axes = axes, units = units,
                             border = border, conf = conf, ...),
           "2" = plot2DType2(x, horizontal = horizontal, col = col, 
                             factor = factor, speciesNames = speciesNames, 
                             xlim = xlim, ylim = ylim, axes = axes, units = units, 
                             border = border, ...),
           "3" = plot2DType3(x, horizontal = horizontal, col = col, 
                             factor = factor, speciesNames = speciesNames, 
                             xlim = xlim, ylim = ylim, axes = axes, units = units, 
                             border = border, ...),
           stop(msg))
  }
  
  return(invisible())
}


# Plot types with TS = TRUE -----------------------------------------------

plot2DTsType1 = function(x, replicates, ci, times, xlim, ylim, conf, 
                         factor, col, alpha, speciesNames, lty, lwd, axes, 
                         units, border, ...){
  
  # Define name of species
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  } 
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  # Define multiplot array if there're more than 1 species
  if(length(x) > 1){
    mar = rep(0, 4)
    oma = c(3, 4, 3, 4)
    
    par(mar = mar, oma = oma)
    
    mfrow = getmfrow(ncol(x))
  }else{
    mfrow = c(1, 1)
  }
  
  par(mfrow = mfrow)
  
  # Extract args related with line customization
  col = rep(x = if(is.null(col)) "black" else col, length.out = ncol(x))
  lty = rep(x = if(is.null(lty)) "solid" else lty, length.out = ncol(x))
  lwd = rep(x = if(is.null(lwd)) 1 else lwd, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define default value for alpha
  if(is.null(alpha)) alpha = 0.3
  
  # Define default value for border
  if(is.null(border)) border = NA
  
  # Define xlim & ylim if NULL
  if(is.null(xlim)) xlim = range(times)
  if(is.null(ylim)) ylim = range(as.numeric(x))*factor #pending: ylim flexible for the users
  
  # Generate plots by spp
  for(i in seq_len(ncol(x))){
    # Extract values for spp i
    xsp = factor*x[, i, ,drop = FALSE]
    
    # Set an empty canvas
    plot.new()
    plot.window(xlim = xlim, ylim = ylim)
    
    # Draw the plot
    plotCI(x = times, y = xsp, replicates = replicates, ci = ci,  
           prob = 1 - conf, col = col[i], alpha = alpha, lty = lty[i], 
           lwd = lwd[i], border = border, ...)
    
    # Add spp names
    mtext(text = speciesNames[i], side = 3, line = -1.5, adj = 0.05, cex = cex)
    
    # Add factor label at topleft
    if(i == 1){
      legendFactor = -log10(factor)
      legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
      mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)  
    }
    
    # Add axis
    if(isTRUE(axes)){
      las = list(...)$las
      las = ifelse(is.null(las), 1, las)
      
      line = list(...)$line
      line = ifelse(is.null(line), NA, line)
      
      cex.axis = list(...)[["cex.axis"]]
      cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
      
      if(is.element(i %% (mfrow[2]*2), c(0, 1))){
        axis(side = ifelse(i %% 2 == 1, 2, 4), las = las, line = line, 
             cex.axis = cex.axis)  
      }
      
      if(mfrow[2] > 1 && is.element(i, seq(2, mfrow[2], 2))){
        axis(side = 3, las = las, line = line, cex.axis = cex.axis)
      }
      
      index = c(seq(from = ncol(x) - mfrow[2] + 1, by = 2, 
                    to = prod(mfrow) - mfrow[2] + 1),
                seq(from = prod(mfrow), by = -2, length.out = mfrow[2] - 1))
      if(is.element(i, index)){
        axis(side = 1, las = las, line = line, cex.axis = cex.axis)
      }
      
      box()
    }
  }
  
  return(invisible())
}

plot2DTsType2 = function(x, replicates, ci, times, xlim, ylim, conf, 
                         factor, col, alpha, speciesNames, lty, lwd, axes,  
                         legend, units, border, ...) {
  
  # Define name of species
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  # Define xlim & ylim if NULL
  if(is.null(xlim)) xlim = range(times)
  if(is.null(ylim)) ylim = range(as.numeric(x))*factor
  
  # Extract args related with line customization
  col = if(is.null(col)) rainbow(n = ncol(x)) else rep(x = col, length.out = ncol(x))
  lty = rep(x = if(is.null(lty)) "solid" else lty, length.out = ncol(x))
  lwd = rep(x = if(is.null(lwd)) 1 else lwd, length.out = ncol(x))
  
  # Set default values for cex
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Set default values for xaxs/yaxs
  xaxs = list(...)[["xaxs"]]
  if(is.null(xaxs)) xaxs = "r"
  
  yaxs = list(...)[["yaxs"]]
  if(is.null(yaxs)) yaxs = "r"
  
  # Define default value for alpha
  if(is.null(alpha)) alpha = 0.3
  
  # Define default value for border
  if(is.null(border)) border = NA
  
  # Set an empty canvas
  plot.new()
  plot.window(xlim = xlim, ylim = ylim, xaxs = xaxs, yaxs = yaxs)
  
  # Generate plots by spp
  for(sp in seq(ncol(x))) {
    # Extract values for spp i
    xsp = factor*x[, sp, ,drop = FALSE]
    
    # Draw the plot
    plotCI(x = times, y = xsp, replicates = replicates, ci = ci, 
           prob = 1 - conf, col = col[sp], alpha = alpha, lty = lty[sp], 
           lwd = lwd[sp], border = border, ...)
  }
  
  # Add axes
  if(isTRUE(axes)){
    las = list(...)$las
    las = ifelse(is.null(las), 1, las)
    
    line = list(...)$line
    line = ifelse(is.null(line), NA, line)
    
    cex.axis = list(...)[["cex.axis"]]
    cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
    
    axis(side = 1, las = las, line = line, cex.axis = cex.axis)
    axis(side = 2, las = las, line = line, cex.axis = cex.axis)
    box()
  }
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  if(isTRUE(legend)){
    legend("topleft", legend = speciesNames, col = col, bty = "n", cex = cex, 
           lty = lty, lwd = lwd)
  }
  
  return(invisible())
}

plotCI = function(x, y, replicates, ci, prob, col, alpha, lty, lwd, border, 
                  ...){
  
  if(dim(y)[3] == 1){
    lines(x = x, y = apply(y, 1, median, na.rm = TRUE), col = col, lty = lty, 
          lwd = lwd)
    
    return(invisible())
  }
  
  x.50  = apply(y, 1, median)
  
  if(isTRUE(replicates)) {
    x.inf = apply(y, 1, quantile, prob = prob/2)
    x.sup = apply(y, 1, quantile, prob = 1 - prob/2)
    
    x.pol = c(x, rev(x), x[1])
    y.pol = c(x.inf, rev(x.sup), x.inf[1])
    
    polygon(x = x.pol, y = y.pol, col = adjustcolor(col = col, alpha.f = alpha),
            border = border, ...)
  }
  
  lines(x = x, y = x.50, col = col, lty = lty, lwd = lwd)
  
  return(invisible())
}

plot2DTsType3 = function(x, times, xlim, ylim, factor, col, alpha, speciesNames, 
                         legend, axes, units, border, ...){
  
  if(length(dim(x)) > 2){
    x = apply(x, c(1, 2), mean, na.rm = TRUE)
  }
  
  x = factor*x
  orderData = order(apply(x, 2, sum, na.rm = TRUE), decreasing = FALSE)
  x = x[, orderData]
  
  dataSpecies = NULL
  for (sp in seq(ncol(x))){
    xsp  = apply(as.data.frame(x[, c(sp:(dim(x))[2])]), 1, sum, na.rm = TRUE)
    dataSpecies = cbind(dataSpecies, xsp)
  }
  colnames(dataSpecies) = colnames(x)
  
  # Define xlim & ylim if NULL
  if(is.null(xlim)) xlim = range(times)
  if(is.null(ylim)) ylim = range(dataSpecies[, 1])
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(dataSpecies))
  }else{
    speciesNames = speciesNames[orderData]
  }
  
  col = if(is.null(col)) rainbow(n = ncol(dataSpecies)) else rep(x = col, length.out = ncol(x))
  
  # Set default value for cex
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Set default values for xaxs/yaxs
  xaxs = list(...)[["xaxs"]]
  if(is.null(xaxs)) xaxs = "r"
  
  yaxs = list(...)[["yaxs"]]
  if(is.null(yaxs)) yaxs = "r"
  
  # Define default value for alpha
  if(is.null(alpha)) alpha = 1
  
  # Define default value for border
  if(is.null(border)) border = TRUE
  
  # Draw an empty canvas
  plot.new()
  plot.window(xlim = xlim, ylim = ylim, xaxs= xaxs, yaxs = yaxs)
  
  for(sp in seq(ncol(x))){
    x.pol = c(times, rev(times))
    y.pol = c(dataSpecies[, sp], rep(0, times = nrow(x)))
    
    polygon(x = x.pol, y = y.pol, border = border, 
            col = adjustcolor(col = col[sp], alpha.f = alpha), ...)
  }
  
  if(isTRUE(axes)){
    las = list(...)$las
    las = ifelse(is.null(las), 1, las)
    
    line = list(...)$line
    line = ifelse(is.null(line), NA, line)
    
    cex.axis = list(...)[["cex.axis"]]
    cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
    
    axis(side = 1, las = las, line = line, cex.axis = cex.axis)
    axis(side = 2, las = las, line = line, cex.axis = cex.axis)
    box()
  }
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  if(isTRUE(legend)){
    legend("topleft", legend = speciesNames, col = col, bty = "n", cex = cex, 
           lty = 1)
  }
  
  return(invisible())
}

#plot for only one species using bars
plot2DTsType4 = function(x, times, xlim, ylim, factor, lty, lwd, col, alpha, 
                         speciesNames, legend, axes, units, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  } 
  
  if(ncol(x) > 1) stop("Plot ts = TRUE and type = 4 is only for one species")
  
  col = rep(x = if(is.null(col)) "black" else col, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define default value for alpha
  if(is.null(alpha)) alpha = 1
  
  x = apply(x, 1, mean, na.rm = TRUE)*factor
  
  # Define xlim & ylim if NULL
  if(is.null(xlim)) xlim = range(times)
  if(is.null(ylim)) ylim = range(x)
  
  plot(x = times, y = x, col = adjustcolor(col = col, alpha.f = alpha), 
       lty = lty, lwd = lwd, type = "h", axes = FALSE, xlim = xlim, ylim = ylim, ...)
  
  if(isTRUE(axes)){
    las = list(...)$las
    las = ifelse(is.null(las), 1, las)
    
    line = list(...)$line
    line = ifelse(is.null(line), NA, line)
    
    cex.axis = list(...)[["cex.axis"]]
    cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
    
    axis(side = 1, las = las, line = line, cex.axis = cex.axis)
    axis(side = 2, las = las, line = line, cex.axis = cex.axis)
    box()
  }
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  if(isTRUE(legend)){
    legend("topleft", legend = speciesNames, col = col, bty = "n", cex = cex, 
           lty = lty, lwd = lwd)
  }
  
  return(invisible())
}


# Plot types with TS = FALSE ----------------------------------------------

plot2DType1 = function(x, ci, horizontal, col, factor, speciesNames, axes, 
                       xlim, ylim, units, border, conf, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  if(is.null(col)) col = "gray"
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define default value for border
  if(is.null(border)) border = TRUE
  
  if(isTRUE(ci)){
    barplotCI(x, horizontal = horizontal, speciesNames = speciesNames, 
              col = col, factor = factor, axes = axes, conf = conf, 
              xlim = xlim, ylim = ylim, border = border, ...)
  }else{
    x = apply(x, 2, mean, na.rm = TRUE) #mean over the replicates
    x = x * factor
    
    if(isTRUE(horizontal)){
      if(is.null(xlim)){
        xlim = c(0, max(x)*1.1)
      }
    }else{
      if(is.null(ylim)){
        ylim = c(0, max(x)*1.1)
      }
    }
    
    barplot(x, horiz = horizontal, names.arg = speciesNames, col = col,
            ylim = ylim, xlim = xlim, axes = axes, border = border, ...)
  }
  
  box()
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}  

barplotCI = function(x, horizontal, speciesNames, col, factor, axes, conf, 
                     xlim, ylim, border, ...){
  
  y.mean = apply(x*factor, 2, mean, na.rm = TRUE)
  y.sd   = apply(x*factor, 2, sd, na.rm = TRUE)
  
  conf = qnorm(conf/2 + 0.5)
  
  if(isTRUE(horizontal)){
    if(is.null(xlim)){
      xlim = c(0, max(y.mean) + conf*max(y.sd)/10)*1.1
    }
  }else{
    if(is.null(ylim)){
      ylim = c(0, max(y.mean) + conf*max(y.sd)/10)*1.1
    }
  }
  
  barx = barplot(y.mean, horiz = horizontal, names.arg = speciesNames, col = col,
                 ylim = ylim, xlim = xlim, axes = axes, border = border, ...)
  
  angle   = ifelse(is.null(list(...)[["angle"]]), 90, list(...)[["angle"]]) 
  code    = ifelse(is.null(list(...)[["code"]]), 3, list(...)[["code"]])
  length  = ifelse(is.null(list(...)[["length"]]), 0.1, list(...)[["length"]])
  
  arrowLimits = list(x = as.numeric(barx),
                     min = y.mean - conf*y.sd/10,
                     max = y.mean + conf*y.sd/10)
  
  if(isTRUE(horizontal)){
    suppressWarnings(arrows(x0 = arrowLimits$min, y0 = arrowLimits$x, 
                            x1 = arrowLimits$max, y1 = arrowLimits$x, 
                            angle = angle, code = code, length = length))
  }else{
    suppressWarnings(arrows(x0 = arrowLimits$x, y0 = arrowLimits$min, 
                            x1 = arrowLimits$x, y1 = arrowLimits$max, 
                            angle = angle, code = code, length = length))
  }
  
  return(invisible())
}

# boxplot with mean over the replicates
plot2DType2 = function(x, horizontal, col, factor, speciesNames, axes,
                       xlim, ylim, units, border, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex) 
  
  # Define default value for border
  if(is.null(border)) border = TRUE
  
  # Get the mean over the replicates
  x = apply(x*factor, c(1, 2), mean, na.rm = TRUE)
  
  if(is.null(ylim)){
    ylim = c(0, max(x)*1.1)
  }
  
  boxplot(x, horizontal = horizontal, names = speciesNames, col = col, 
          axes = axes, xlim = xlim, ylim = ylim, border = border, ...)
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}

# boxplot with mean over the time
plot2DType3 = function(x, horizontal, col, factor, speciesNames, axes, 
                       xlim, ylim, units, border, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  if(is.null(col)) col = "gray"
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define default value for border
  if(is.null(border)) border = TRUE
  
  # Get mean over the time
  x = apply(x*factor, c(3, 2), mean, na.rm = TRUE)
  
  if(is.null(ylim)){
    ylim = c(0, max(x)*1.1)
  }
  
  boxplot(x, horizontal = horizontal, names = speciesNames, col = col, 
          axes = axes, xlim = xlim, ylim = ylim, border = border, ...)
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}


# normalize function. 
# returns percentage instead of raw values
norm_func = function(data) {
  output = 100 * data / (sum(data, na.rm=TRUE) + .Machine$double.xmin)
  return(output)
}


# PLOT 3D -----------------------------------------------------------------

osmosePlots3D = function(x, type, by, species, speciesNames, start, end, 
                         initialYear, freq, horizontal, factor,
                         xlim, ylim, col, border, lty, lwd, axes, 
                         units, ci, log, ...) {
  
  x = .extract_species_from_list(x, species)
  
  if(!is.null(speciesNames) && length(speciesNames) != length(x)){
    stop("'speciesNames' has an incorrect length.")
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(names(x))
  }
  
  # getting time array (not used so far)
  index = .get_start_end(x[[1]], start, end)
  start = index[1]
  end = index[2]
  
  if(is.null(initialYear)) initialYear = 0
  times = seq(from = initialYear + start/freq, by = 1/freq, 
              length.out = nrow(x[[1]]))
  
  # apply processing on the time-series (so far, replicate and time-mean)
  x = sapply(x, .process_3d_fields, start, end, ts, replicates, ...) 
  
  switch(type,
         "1" = plot3DType1(x, by = by, horizontal = horizontal, col = col,
                           factor = factor, speciesNames = speciesNames,
                           ylim = ylim, axes = axes, units = units,
                           border = border, ...),
         "2" = plot3DType2(x, by = by, col = col, factor = factor, 
                           speciesNames = speciesNames,
                           axes = axes, units = units, ...),
         stop(sprintf("3D plot type %d is not implemented yet.", type)))
  
  return(invisible())
  
}

# Displays a histogram showing one plot per species.
plot3DType1 = function(x, by, horizontal, col, factor, 
                       speciesNames, axes, ylim, units, border, ...){
  
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  # Define multiplot array if there're more than 1 species
  if(ncol(x) > 1){
    # Modify oma & mar
    mar = rep(0, 4)
    oma = c(3, 4, 2, 4)
    par(mar = mar, oma = oma)
    
    # Set pane distribution
    mfrow = getmfrow(ncol(x))
  }else{
    mfrow = c(1, 1)
  }
  
  par(mfrow = mfrow)
  
  # Extract args related with line customization
  col = rep(x = if(is.null(col)) "black" else col, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define default value for border
  if(is.null(border)) border = NA
  
  # recovering the size-class
  byNames = rownames(x)
  
  # Define xlim & ylim if NULL
  if(is.null(ylim)) { 
    ymin = Reduce(min, lapply(x, min)) * factor
    ymax = Reduce(max, lapply(x, max)) * factor
    ylim = c(ymin, ymax*1.1)
  }
  
  # Generate plots by spp
  for(i in seq(ncol(x))){
    
    # Extract values for spp i
    xsp = factor*x[,i]
    
    # Draw barplot
    bp <- barplot(xsp, horiz = horizontal, names.arg = rep(NA, nrow(x)), 
                  col = col, ylim = ylim, border = border, 
                  xlab = NA, ylab = NA, axes = FALSE, ...)
    
    bp <- as.numeric(bp)
    
    # Add label of factor
    if(i == 1){
      legendFactor = -log10(factor)
      legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units$y))
      mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)  
    }
    
    # Add name of species
    mtext(text = speciesNames[i], side = 3, line = -1.5, adj = 1, cex = cex)
    
    # Add axis
    if(isTRUE(axes)){
      las = list(...)$las
      las = ifelse(is.null(las), 1, las)
      
      line = list(...)$line
      line = ifelse(is.null(line), NA, line)
      
      cex.axis = list(...)[["cex.axis"]]
      cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
      
      if(is.element(i %% (mfrow[2]*2), c(0, 1))){
        axis(side = ifelse(i %% 2 == 1, 2, 4), las = las, line = line, 
             cex.axis = cex.axis)  
      }
      
      if(mfrow[2] > 1 && is.element(i, seq(2, mfrow[2], 2))){
        axis(side = 3, at = bp, labels = byNames, las = las, line = line, 
             cex.axis = cex.axis)
      }
      
      index = c(seq(from = ncol(x) - mfrow[2] + 1, by = 2, 
                    to = prod(mfrow) - mfrow[2] + 1),
                seq(from = prod(mfrow), by = -2, length.out = mfrow[2] - 1))
      if(is.element(i, index)){
        axis(side = 1, at = bp, labels = byNames, las = las, line = line, 
             cex.axis = cex.axis)
      }
      
      box()
      
      if(i == 1){
        mtext(text = paste0(by, ' (', units$x, ')'), side = 1, line = 2, 
              outer = TRUE, cex = cex)
      }
    }
  }
  
  return(invisible())
}

# Showing biomass as a function of class as a raster file.
plot3DType2 = function(x, col, factor, speciesNames, axes, units, by, ...){
  
  # List of the form x, y & z
  x <- list(x = seq(nrow(x)),
            y = seq(ncol(x)),
            z = x*factor)
  
  # Modify margins to give space for species names
  par(mar = c(3, 10, 1, 1))
  
  # Define default color palette
  if(is.null(col)) col <- tim.colors(1e3)
  
  # Draw image plot
  image.plot(x, axes = FALSE, legend.lab = units$y, col = col, ...)
  
  # Add axis
  if(isTRUE(axes)){
    axis(side = 1, at = x$x, labels = rownames(x$z))
    axis(side = 2, at = x$y, labels = colnames(x$z), las = 1)
    
    box()
    
    mtext(text = paste0(by, ' (', units$x, ')'), side = 1, line = 2)
  }
  
  return(invisible())
}

.get_start_end = function(x, start, end) {
  
  # Check start and end args
  if(is.null(start)) start = 1
  if(is.null(end)) end = dim(x)[1]
  
  if(start < 1 | start > end) stop("Incorrect value for 'start' argument")
  if(end > dim(x)[1] | end < start) stop("Incorrect value for 'end' argument")
  
  return(c(start, end))
}

# Process 3D fields (fields by class). Computes replicates
# or time means. Called through lapply (to loop over all the species)
.process_3d_fields = function(x, start, end, ts, replicates, ...) {
  
  index = .get_start_end(x, start, end)
  start = index[1]
  end = index[2]
  
  if(length(dim(x)) == 3) { 
    x = x[seq(start, end), , , drop = FALSE]  # time, class, replicates
  } else {
    x = x[seq(start, end), , drop = FALSE]  # time, class, replicates  
  }
  x = apply(x, 2, mean, na.rm = TRUE)
  
}