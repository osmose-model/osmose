
# Internal plot functions -------------------------------------------------

osmosePlots2D = function(x, species, speciesNames, start, end, initialYear, ts, 
                         type, replicates, nrep, freq, horizontal, conf, factor, 
                         xlim, ylim, col, alpha, lty, lwd, axes, legend, 
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
      
      species = match(species, colnames(x))
    }
    
    x = x[ , species + 1, , drop = FALSE]
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
  
  if(is.null(xlim)) xlim = range(times)
  
  if(!(type %in% c(1:4))){
    warning("The type argument selected is not correct. The value by default is used (type = 1)")
    type = 1
  } 
  
  if(isTRUE(ts)){
    
    switch(type,
           "1" = plot2DTsType1(x = x, replicates = replicates, nrep = nrep, 
                               ci = ci, times = times, xlim = xlim, ylim = ylim,
                               conf = conf, factor = factor, col = col, 
                               alpha = alpha, speciesNames = speciesNames, 
                               lty = lty, lwd = lwd, axes = axes, units = units, 
                               ...),
           "2" = plot2DTsType2(x = x, replicates = replicates, nrep = nrep, 
                               ci = ci, times = times, xlim = xlim, ylim = ylim,
                               conf = conf, factor = factor, col = col, 
                               alpha = alpha, speciesNames = speciesNames, 
                               lty = lty, lwd = lwd, axes = axes, 
                               legend = legend, units = units, ...),
           "3" = plot2DTsType3(x = x, times = times, xlim = xlim, ylim = ylim, 
                               factor = factor, col = col, alpha = alpha, 
                               legend = legend, speciesNames = speciesNames, 
                               axes = axes, units = units, ...),
           "4" = plot2DTsType4(x = x, times = times, xlim = xlim, ylim = ylim, 
                               factor = factor, lty = lty, col = col, 
                               alpha = alpha, legend = legend, 
                               speciesNames = speciesNames, axes = axes, 
                               units = units, ...))
  }else{
    
    switch(type,
           "1" = plot2DType1(x, ci = ci, horizontal = horizontal, col = col,
                             factor = factor, speciesNames = speciesNames, 
                             axes = axes, units = units, ...),
           "2" = plot2DType2(x, horizontal = horizontal, col = col, 
                             factor = factor, speciesNames = speciesNames, 
                             axes = axes, units = units, ...),
           "3" = plot2DType3(x, horizontal = horizontal, col = col, 
                             factor = factor, speciesNames = speciesNames, 
                             axes = axes, units = units, ...))
  }
  
  return(invisible())
}


# Plot types with TS = TRUE -----------------------------------------------

plot2DTsType1 = function(x, replicates, nrep, ci, times, xlim, ylim, conf, 
                         factor, col, alpha, speciesNames, lty, lwd, axes, 
                         units, ...){
  
  # Define name of species
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  } 
  
  # To keep the plot params as the beggining
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  # Define multiplot array if there're more than 1 species
  if(ncol(x) > 1){
    mar <- rep(0, 4)
    oma <- c(2, 3, 2, 3)
    
    par(mar = mar, oma = oma)
    
    mfrow <- getmfrow(ncol(x))
  }else{
    mfrow <- c(1, 1)
  }
  
  par(mfrow = mfrow)
  
  # Extract args related with line customization
  col = rep(x = if(is.null(col)) "black" else col, length.out = ncol(x))
  lty = rep(x = if(is.null(lty)) "solid" else lty, length.out = ncol(x))
  lwd = rep(x = if(is.null(lwd)) 1 else lwd, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # Define ylim
  if(is.null(ylim)){
    ylim = range(as.numeric(x))*factor
  }  #pending: ylim flexible for the users
  
  # Generate plots by spp
  for(i in seq_len(ncol(x))){
    # Extract values for spp i
    xsp = factor*x[, i, ,drop = FALSE]
    
    # Set an empty canvas
    plot.new()
    plot.window(xlim = xlim, ylim = ylim)
    
    # Draw the plot
    plotCI(x = times, y = xsp, replicates = replicates, ci = ci, nrep = nrep, 
           prob = 1 - conf, col = col[i], alpha = alpha, lty = lty[i], 
           lwd = lwd[i], ...)
    
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
      las <- list(...)$las
      las <- ifelse(is.null(las), 1, las)
      
      line <- list(...)$line
      line <- ifelse(is.null(line), NA, line)
      
      cex.axis = list(...)[["cex.axis"]]
      cex.axis = ifelse(is.null(cex.axis), 1, cex.axis)
      
      if(is.element(i %% (mfrow[2]*2), c(0, 1))){
        axis(side = ifelse(i %% 2 == 1, 2, 4), las = las, line = line, 
             cex.axis = cex.axis)  
      }
      
      if(ncol(x) > 1 && is.element(i, seq(2, mfrow[2], 2))){
        axis(side = 3, las = las, line = line, cex.axis = cex.axis)
      }
      
      index <- c(seq(from = ncol(x) - mfrow[2] + 1, by = 2, 
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

plot2DTsType2 = function(x, replicates, nrep, ci, times, xlim, ylim, conf, 
                         factor, col, alpha, speciesNames, lty, lwd, axes, cex, 
                         legend, units, ...) {
  
  # Define name of species
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  if(is.null(ylim)){
    ylim = range(as.numeric(x))*factor
  }
  
  # Extract args related with line customization
  col = if(is.null(col)) rainbow(n = ncol(x)) else rep(x = col, length.out = ncol(x))
  lty = rep(x = if(is.null(lty)) "solid" else lty, length.out = ncol(x))
  lwd = rep(x = if(is.null(lwd)) 1 else lwd, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # To keep the plot params as the beggining
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  # Set an empty canvas
  plot.new()
  plot.window(xlim = xlim, ylim = ylim)
  
  # Generate plots by spp
  for(sp in seq(ncol(x))) {
    # Extract values for spp i
    xsp = factor*x[, sp, ,drop = FALSE]
    
    # Draw the plot
    plotCI(x = times, y = xsp, replicates = replicates, ci = ci, nrep = nrep, 
           prob = 1 - conf, col = col[sp], alpha = alpha, lty = lty[sp], 
           lwd = lwd[sp], ...)
  }
  
  if(isTRUE(axes)){
    las <- list(...)$las
    las <- ifelse(is.null(las), 1, las)
    
    line <- list(...)$line
    line <- ifelse(is.null(line), NA, line)
    
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
           lty = lty)
  }
  
  return(invisible())
}

plotCI = function(x, y, replicates, ci, nrep, prob, col, alpha, lty, lwd, ...){
  
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
    
    polygon(x = x.pol, y = y.pol, col = adjustcolor(col = col, alpha.f = alpha), ...)
  }
  
  lines(x = x, y = x.50, col = col, lty = lty, lwd = lwd)
  
  return(invisible())
}

plot2DTsType3 = function(x, times, xlim, ylim, factor, col, alpha, speciesNames, 
                         legend, axes, units, ...){
  
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
  
  if(is.null(ylim)){
    ylim = c(0.75, 1.25)*range(dataSpecies[, 1])
  }
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(dataSpecies))
  }else{
    speciesNames = speciesNames[orderData]
  }
  
  col = if(is.null(col)) rainbow(n = ncol(dataSpecies)) else rep(x = col, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # To keep the plot params as the beggining
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  plot.new()
  plot.window(xlim=xlim, ylim=ylim)
  
  for(sp in seq(ncol(x))){
    x.pol = c(times, rev(times))
    y.pol = c(dataSpecies[, sp], rep(0, times = nrow(x)))
    
    polygon(x = x.pol, y = y.pol, 
            col = adjustcolor(col = col[sp], alpha.f = alpha), ...)
  }
  
  if(isTRUE(axes)){
    las <- list(...)$las
    las <- ifelse(is.null(las), 1, las)
    
    line <- list(...)$line
    line <- ifelse(is.null(line), NA, line)
    
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
plot2DTsType4 = function(x, times, xlim, ylim, factor, lty, col, alpha, 
                         speciesNames, legend, axes, units, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  } 
  
  if(ncol(x) > 1) stop("Plot ts = TRUE and type = 4 is only for one species")
  
  col = rep(x = if(is.null(col)) "black" else col, length.out = ncol(x))
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  x = apply(x, 1, mean, na.rm = TRUE)*factor
  if(is.null(ylim)){
    ylim =  c(0, 1.25)*range(x)
  } 
  
  plot(x = times, y = x, col = adjustcolor(col = col, alpha.f = alpha), 
       lty = lty, type = "h", axes = FALSE, xlim = xlim, ylim = ylim, ...)
  
  if(isTRUE(axes)){
    las <- list(...)$las
    las <- ifelse(is.null(las), 1, las)
    
    line <- list(...)$line
    line <- ifelse(is.null(line), NA, line)
    
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
           lty = lty)
  }
  
  return(invisible())
}


# Plot types with TS = FALSE ----------------------------------------------

plot2DType1 = function(x, ci, horizontal, col, factor, speciesNames, axes, 
                       units, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  if(is.null(col)) col = "gray"
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # To keep the plot params as the beginning
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  if(isTRUE(ci)){
    barplotCI(x, horizontal = horizontal, speciesNames = speciesNames, 
              col = col, factor = factor, axes = axes, ...)
  }else{
    x = apply(x, 2, mean, na.rm = TRUE) #mean over the replicates
    x = x * factor
    if(!isTRUE(horizontal)){
      ylim = c(0, 1.2*max(x))
      xlim = NULL
    }else{
      xlim = c(0, 1.2*max(x))
      ylim = NULL
    } 
    
    barplot(x, horiz = horizontal, names.arg = speciesNames, col = col,
            ylim = ylim, xlim = xlim, axes = axes, ...)
  }
  
  box()
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}  

barplotCI = function(x, horizontal, speciesNames, col, factor, axes, ...){
  
  y.mean = apply(x*factor, 2, mean, na.rm = TRUE)
  y.sd   = apply(x*factor, 2, sd, na.rm = TRUE)
  
  if(!isTRUE(horizontal)){
    ylim = c(0, 1.2*(max(y.mean)+1.96*max(y.sd)/10))
    xlim = NULL
  } else {
    xlim = c(0, 1.2*(max(y.mean)+1.96*max(y.sd)/10))
    ylim = NULL
  }
  
  barx = barplot(y.mean, horiz = horizontal, names.arg = speciesNames, col = col,
                 ylim = ylim, xlim = xlim, axes = axes, ...)
  
  angle   = ifelse(is.null(list(...)[["angle"]]), 90, list(...)[["angle"]]) 
  code    = ifelse(is.null(list(...)[["code"]]), 3, list(...)[["code"]])
  length  = ifelse(is.null(list(...)[["length"]]), 0.1, list(...)[["length"]])
  
  arrowLimits = list(x = as.numeric(barx),
                     min = y.mean - 1.96*y.sd/10,
                     max = y.mean + 1.96*y.sd/10)
  
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
                       units, ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex) 
  
  # To keep the plot params as the beggining
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  x = apply(x*factor, c(1, 2), mean, na.rm = TRUE) #mean over the replicates
  
  boxplot(x, horizontal = horizontal, names = speciesNames, col = col, 
          axes = axes, ...)
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}

# boxplot with mean over the time
plot2DType3 = function(x, horizontal, col, factor, speciesNames, axes, units, 
                       ...){
  
  if(is.null(speciesNames)){
    speciesNames = toupper(colnames(x))
  }
  
  if(is.null(col)) col = "gray"
  
  cex = list(...)[["cex"]]
  cex = ifelse(is.null(cex), 0.8, cex)
  
  # To keep the plot params as the beggining
  op = par(no.readonly = TRUE)
  on.exit(par(op))
  
  x = apply(x*factor, c(3, 2), mean, na.rm = TRUE) #mean over the time
  
  boxplot(x, horizontal = horizontal, names = speciesNames, col = col, 
          axes = axes, ...)
  
  legendFactor = -(log10(factor))
  legendFactor = bquote("x" ~ 10^.(legendFactor) ~ .(units))
  mtext(text = legendFactor, side = 3, line = 0, adj = 0, cex = cex)
  
  return(invisible())
}


# normalize function. 
# returns percentage instead of raw values
norm_func = function(data) {
  output = 100 * data / (sum(data, na.rm=TRUE) + .Machine$double.xmin)
  dimnames(output) = dimnames(data)
  return(output)
}
